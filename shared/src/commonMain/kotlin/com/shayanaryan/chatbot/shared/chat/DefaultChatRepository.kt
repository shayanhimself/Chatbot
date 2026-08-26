package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.local.ChatDao
import com.shayanaryan.chatbot.shared.chat.local.ChatEntity
import com.shayanaryan.chatbot.shared.chat.local.MessageDao
import com.shayanaryan.chatbot.shared.chat.local.MessageEntity
import com.shayanaryan.chatbot.shared.chat.local.toClaudeMessages
import com.shayanaryan.chatbot.shared.chat.local.toDomain
import com.shayanaryan.chatbot.shared.claude.ClaudeEngine
import com.shayanaryan.chatbot.shared.claude.ClaudeMessageRequest
import com.shayanaryan.chatbot.shared.claude.ClaudeStreamEvent
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * How long a failed turn waits for a first collector before its entry is dropped unread. Long
 * enough to cover a screen being recreated, short enough that a failure nobody opens is not held
 * for the process's lifetime.
 */
private val unreadTurnTimeout = 30.seconds

/**
 * One reply in flight: the state a collector reads, and the coroutine filling it.
 */
private class Turn(
    val state: MutableStateFlow<TurnState>,
    val job: Job,
)

internal class DefaultChatRepository(
    private val engine: ClaudeEngine,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val externalScope: CoroutineScope,
    private val clock: Clock,
) : ChatRepository {
    /**
     * The latest turn per chat, live or terminal. An immutable map behind a state flow, so
     * reads need no lock and a collector sees turns that start after it does.
     */
    private val turns = MutableStateFlow<Map<Long, Turn>>(emptyMap())

    /**
     * Guards the writes. A single compare-and-set is already atomic, but the guard spans two
     * operations (test for a live turn, then start one) and without the lock two concurrent
     * sends both see none and both launch.
     */
    private val mutex = Mutex()

    override fun getChatsFlow(): Flow<List<Chat>> =
        chatDao.observeAllWithSnippet().map { rows -> rows.map { it.toDomain() } }

    override fun getChatFlow(chatId: Long): Flow<Chat?> =
        chatDao.observeByIdWithSnippet(chatId).map { it?.toDomain() }

    override fun getMessagesFlow(chatId: Long): Flow<List<Message>> =
        messageDao
            .observeForChat(chatId)
            .map { entities -> entities.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTurnFlow(chatId: Long): Flow<TurnState> =
        turns
            // Narrowed to this chat before the switch, so a turn starting on another chat does not
            // tear this collector's subscription down and put it back.
            .map { it[chatId] }
            .distinctUntilChanged()
            .flatMapLatest { it?.state ?: flowOf(TurnState.Idle) }
            .distinctUntilChanged()

    override suspend fun send(
        chatId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long =
        mutex.withLock {
            val now = clock.now().toEpochMilliseconds()
            val id =
                if (chatId == null) {
                    chatDao.createWithFirstMessage(
                        chat =
                            ChatEntity(
                                title = text.take(ChatRepository.MAX_TITLE_LENGTH),
                                model = model,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        message =
                            userMessage(
                                text = text,
                                createdAt = now,
                            ),
                    )
                } else {
                    requireExists(chatId)
                    requireIdle(chatId)
                    chatDao.appendMessage(
                        message =
                            userMessage(
                                text = text,
                                createdAt = now,
                                chatId = chatId,
                            ),
                        updatedAt = now,
                    )
                    chatId
                }
            launchTurn(id)
            id
        }

    override suspend fun retry(chatId: Long) {
        mutex.withLock {
            val last = messageDao.lastForChat(chatId) ?: return@withLock
            // Only a reply that stopped short is replayable. A turn in flight has the user
            // message last, so it falls outside this.
            if (last.role == Role.Assistant && last.status != MessageStatus.Complete) {
                // Deleting the row restores the state the turn started from.
                messageDao.deleteById(last.id)
                launchTurn(chatId)
            }
        }
    }

    override suspend fun cancel(chatId: Long) {
        // Not under the lock: the turn's own cancellation path takes it to clear its entry, and
        // joining while holding it would deadlock. Joining is what guarantees the cancelled
        // message is stored before this returns.
        turns.value[chatId]?.job?.cancelAndJoin()
    }

    override suspend fun setModel(
        chatId: Long,
        model: ClaudeModel,
    ) {
        chatDao.setModel(chatId, model)
    }

    override suspend fun delete(chatId: Long) {
        val turn = turns.value[chatId]
        turn?.job?.cancelAndJoin()
        chatDao.delete(chatId)
        if (turn != null) {
            clearTurn(chatId, turn.state)
        }
    }

    /**
     * @param chatId omitted for the first message of a new chat, whose id does not
     *   exist until the creating transaction runs and substitutes it.
     */
    private fun userMessage(
        text: String,
        createdAt: Long,
        chatId: Long = 0,
    ) = MessageEntity(
        chatId = chatId,
        role = Role.User,
        content = listOf(ContentBlock.Text(text)),
        status = MessageStatus.Complete,
        createdAt = createdAt,
    )

    /**
     * A screen left open on a chat the user has since deleted can still ask to send into
     * it. Rejecting here turns that into the documented failure rather than a foreign-key
     * constraint thrown from inside the insert.
     */
    private suspend fun requireExists(chatId: Long) {
        requireNotNull(chatDao.findById(chatId)) {
            "chat $chatId does not exist"
        }
    }

    /**
     * A turn is live while its job is running and its state still says so: testing the job
     * alone would let a turn that died unexpectedly block every later send, and testing the entry
     * alone would reject a send that lands between a finished turn going idle and its entry being
     * dropped.
     */
    private fun requireIdle(chatId: Long) {
        val turn = turns.value[chatId]
        check(turn == null || !turn.job.isActive || turn.state.value !is TurnState.Streaming) {
            "chat $chatId already has a turn in flight"
        }
    }

    /** Call under [mutex]: the entry must exist before the coroutine can clear it. */
    private fun launchTurn(chatId: Long) {
        val state = MutableStateFlow<TurnState>(TurnState.Streaming(""))
        val job =
            externalScope.launch(start = CoroutineStart.LAZY) {
                runTurn(chatId, state)
            }
        turns.update { it + (chatId to Turn(state, job)) }
        job.start()
    }

    /**
     * Streams one assistant reply and persists it. Deltas accumulate into [state] so a collector
     * renders the text as it arrives, and the finished reply is written as a single row, never a
     * write per token. The row is stored before the turn reports [TurnState.Idle], so the live
     * bubble is never dropped before the persisted message exists to replace it.
     *
     * @param state the turn's own state flow, also the handle used to clear its entry: a later
     *   turn may already have replaced it.
     */
    private suspend fun runTurn(
        chatId: Long,
        state: MutableStateFlow<TurnState>,
    ) {
        val chat = chatDao.findById(chatId)
        if (chat == null) {
            state.value = TurnState.Idle
            clearTurn(chatId, state)
            return
        }
        val messages = messageDao.completeForChat(chatId).toClaudeMessages()
        val reply = StringBuilder()
        var failure: ApiError? = null
        try {
            engine
                .stream(
                    ClaudeMessageRequest(
                        messages = messages,
                        model = chat.model,
                    ),
                ).collect { event ->
                    when (event) {
                        is ClaudeStreamEvent.Delta -> {
                            reply.append(event.text)
                            state.value = TurnState.Streaming(reply.toString())
                        }

                        is ClaudeStreamEvent.Failed -> {
                            failure = event.error
                            // The error is recorded here and applied once the stream ends: a delta
                            // arriving after would overwrite a failed state, and a cancellation
                            // landing after an early write would store a second row for this turn.
                        }

                        is ClaudeStreamEvent.Completed -> {
                            // Nothing to record: the reply came from the deltas, and the stop
                            // reason and usage are not stored. The completed turn is persisted
                            // below, once the stream ends with no failure.
                        }
                    }
                }
        } catch (cancellation: CancellationException) {
            // The turn owns every write, cancellation included, so exactly one assistant row is
            // ever produced per turn, and it is stored before the caller resumes.
            withContext(NonCancellable) {
                persistReply(chatId, reply.toString(), MessageStatus.Cancelled)
                state.value = TurnState.Idle
                clearTurn(chatId, state)
            }
            throw cancellation
        }
        // Copied into a val so the else branch below can read it as non-null.
        val error = failure
        // The stream has ended and this is the turn's only remaining work, so a cancel arriving
        // now must not stop it.
        withContext(NonCancellable) {
            if (error == null) {
                persistReply(chatId, reply.toString(), MessageStatus.Complete)
                state.value = TurnState.Idle
                clearTurn(chatId, state)
            } else {
                // The entry outlives the turn so the error is still readable when a collector
                // arrives late, and is dropped once nothing is reading it.
                persistReply(chatId, reply.toString(), MessageStatus.Failed)
                state.value = TurnState.Failed(error)
                clearTurnWhenUnread(chatId, state)
            }
        }
    }

    private suspend fun persistReply(
        chatId: Long,
        text: String,
        status: MessageStatus,
    ) {
        val now = clock.now().toEpochMilliseconds()
        chatDao.appendMessage(
            message =
                MessageEntity(
                    chatId = chatId,
                    role = Role.Assistant,
                    content = listOf(ContentBlock.Text(text)),
                    status = status,
                    createdAt = now,
                ),
            updatedAt = now,
        )
    }

    /**
     * Drops a failed turn's entry once nothing is reading it anymore.
     *
     * @param state the failed turn's state flow: both the subscriptions to watch and the handle
     *   [clearTurn] recognises the entry by.
     */
    private fun clearTurnWhenUnread(
        chatId: Long,
        state: MutableStateFlow<TurnState>,
    ) {
        // Runs on [externalScope], not the turn's own job, which ends as this returns.
        externalScope.launch {
            // Wait for a collector to arrive. Dropping the entry the moment the turn fails would
            // beat a screen that is still opening.
            withTimeoutOrNull(unreadTurnTimeout) {
                state.subscriptionCount.first { it > 0 }
            }
            // Then wait for it to leave. If none ever arrived the count is still zero, so this
            // returns at once.
            state.subscriptionCount.first { it == 0 }
            clearTurn(chatId, state)
        }
    }

    /**
     * Drops this turn's entry, and only this one: a later turn may already have replaced it, and
     * dropping that one would leave it running with nothing tracking it.
     */
    private suspend fun clearTurn(
        chatId: Long,
        state: MutableStateFlow<TurnState>,
    ) {
        mutex.withLock {
            turns.update { current ->
                if (current[chatId]?.state === state) current - chatId else current
            }
        }
    }
}
