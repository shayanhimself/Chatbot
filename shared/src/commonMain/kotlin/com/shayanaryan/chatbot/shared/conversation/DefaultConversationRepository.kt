package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.local.ConversationDao
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageDao
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity
import com.shayanaryan.chatbot.shared.conversation.local.toDomain
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * One reply in flight: the state a collector reads, and the coroutine filling it.
 */
private class Turn(
    val state: MutableStateFlow<TurnState>,
    val job: Job,
)

internal class DefaultConversationRepository(
    private val engine: ChatEngine,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val externalScope: CoroutineScope,
    private val clock: Clock,
) : ConversationRepository {
    /**
     * The latest turn per conversation, live or terminal. An immutable map behind a state flow, so
     * reads need no lock and a collector sees turns that start after it does.
     */
    private val turns = MutableStateFlow<Map<Long, Turn>>(emptyMap())

    /**
     * Guards the writes. A single compare-and-set is already atomic, but the guard spans two
     * operations — test for a live turn, then start one — and without the lock two concurrent
     * sends both see none and both launch.
     */
    private val mutex = Mutex()

    override fun getConversationsFlow(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        messageDao
            .observeForConversation(conversationId)
            .map { entities -> entities.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTurnFlow(conversationId: Long): Flow<TurnState> =
        turns
            .flatMapLatest { it[conversationId]?.state ?: flowOf(TurnState.Idle) }
            .distinctUntilChanged()

    override suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long =
        mutex.withLock {
            val now = clock.now().toEpochMilliseconds()
            val id =
                if (conversationId == null) {
                    conversationDao.createWithFirstMessage(
                        conversation =
                            ConversationEntity(
                                title = text.take(MAX_TITLE_LENGTH),
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
                    requireIdle(conversationId)
                    conversationDao.appendMessage(
                        message =
                            userMessage(
                                text = text,
                                createdAt = now,
                                conversationId = conversationId,
                            ),
                        updatedAt = now,
                    )
                    conversationId
                }
            launchTurn(id)
            id
        }

    override suspend fun retry(conversationId: Long) {
        // Filled in with the turn lifecycle.
    }

    override suspend fun cancel(conversationId: Long) {
        // Filled in with the turn lifecycle.
    }

    override suspend fun setModel(
        conversationId: Long,
        model: ClaudeModel,
    ) {
        conversationDao.setModel(conversationId, model)
    }

    override suspend fun delete(conversationId: Long) {
        turns.value[conversationId]?.job?.cancelAndJoin()
        conversationDao.delete(conversationId)
        mutex.withLock { turns.update { it - conversationId } }
    }

    /**
     * @param conversationId omitted for the first message of a new conversation, whose id does not
     *   exist until the creating transaction runs and substitutes it.
     */
    private fun userMessage(
        text: String,
        createdAt: Long,
        conversationId: Long = 0,
    ) = MessageEntity(
        conversationId = conversationId,
        role = Role.User,
        content = listOf(ContentBlock.Text(text)),
        status = MessageStatus.Complete,
        createdAt = createdAt,
    )

    /**
     * A turn is live while its job is running and its state still says so: testing the job
     * alone would let a turn that died unexpectedly block every later send, and testing the entry
     * alone would reject a send that lands between a finished turn going idle and its entry being
     * dropped.
     */
    private fun requireIdle(conversationId: Long) {
        val turn = turns.value[conversationId]
        check(turn == null || !turn.job.isActive || turn.state.value !is TurnState.Streaming) {
            "conversation $conversationId already has a turn in flight"
        }
    }

    /** Call under [mutex]: the entry must exist before the coroutine can clear it. */
    private fun launchTurn(conversationId: Long) {
        val state = MutableStateFlow<TurnState>(TurnState.Streaming(""))
        val job =
            externalScope.launch(start = CoroutineStart.LAZY) {
                runTurn(conversationId, state)
            }
        turns.update { it + (conversationId to Turn(state, job)) }
        job.start()
    }

    private suspend fun runTurn(
        conversationId: Long,
        state: MutableStateFlow<TurnState>,
    ) {
        state.value = TurnState.Idle
        clearTurn(conversationId, state)
    }

    /**
     * Drops this turn's entry, and only this one — a later turn may already have replaced it.
     */
    private suspend fun clearTurn(
        conversationId: Long,
        state: MutableStateFlow<TurnState>,
    ) {
        mutex.withLock {
            turns.update { current ->
                if (current[conversationId]?.state === state) current - conversationId else current
            }
        }
    }
}
