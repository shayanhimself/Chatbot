package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-memory [ChatRepository] for tests above the data layer.
 *
 * There is no engine: a turn is opened by [send] or [retry] and moved by [emitDelta],
 * [completeTurn] and [failTurn], so a test decides exactly when a reply arrives. Turn state is
 * held independently of any collector, mirroring the production guarantee that a turn outlives
 * the screen that started it.
 */
class FakeChatRepository(
    private val clock: Clock = FakeClock(autoAdvanceBy = 1.milliseconds),
) : ChatRepository {
    private val chats = MutableStateFlow<List<Chat>>(emptyList())
    private val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val turns = MutableStateFlow<Map<Long, TurnState>>(emptyMap())

    private var nextChatId = 1L
    private var nextMessageId = 1L

    override fun getChatsFlow(): Flow<List<Chat>> = chats

    override fun getChatFlow(chatId: Long): Flow<Chat?> =
        chats
            .map { list ->
                list.firstOrNull { it.id == chatId }
            }.distinctUntilChanged()

    override fun getMessagesFlow(chatId: Long): Flow<List<Message>> =
        messages.map { it[chatId].orEmpty() }.distinctUntilChanged()

    override fun getTurnFlow(chatId: Long): Flow<TurnState> =
        turns.map { it[chatId] ?: TurnState.Idle }.distinctUntilChanged()

    override suspend fun send(
        chatId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long {
        if (chatId != null) {
            require(chats.value.any { it.id == chatId }) {
                "chat $chatId does not exist"
            }
        }
        check(turns.value[chatId] !is TurnState.Streaming) {
            "chat $chatId already has a turn in flight"
        }
        val id = chatId ?: createChat(text, model)
        appendMessage(id, Role.User, text, MessageStatus.Complete)
        turns.update { it + (id to TurnState.Streaming("")) }
        return id
    }

    override suspend fun retry(chatId: Long) {
        val last = messages.value[chatId]?.lastOrNull() ?: return
        if (last.role != Role.Assistant || last.status == MessageStatus.Complete) return
        messages.update { all ->
            all + (chatId to all.getValue(chatId).dropLast(1))
        }
        turns.update { it + (chatId to TurnState.Streaming("")) }
    }

    override suspend fun cancel(chatId: Long) {
        val streaming = turns.value[chatId] as? TurnState.Streaming ?: return
        appendMessage(chatId, Role.Assistant, streaming.text, MessageStatus.Cancelled)
        turns.update { it - chatId }
    }

    override suspend fun setModel(
        chatId: Long,
        model: ClaudeModel,
    ) {
        chats.update { list ->
            list.map { if (it.id == chatId) it.copy(model = model) else it }
        }
    }

    override suspend fun delete(chatId: Long) {
        chats.update { list -> list.filterNot { it.id == chatId } }
        messages.update { it - chatId }
        turns.update { it - chatId }
    }

    /** Grows the reply in flight, as a stream delta would. */
    fun emitDelta(
        chatId: Long,
        text: String,
    ) {
        val streaming = turns.value[chatId] as? TurnState.Streaming ?: return
        turns.update {
            it + (chatId to TurnState.Streaming(streaming.text + text))
        }
    }

    fun completeTurn(chatId: Long) {
        val streaming = turns.value[chatId] as? TurnState.Streaming ?: return
        appendMessage(chatId, Role.Assistant, streaming.text, MessageStatus.Complete)
        turns.update { it - chatId }
    }

    /** Ends the turn in failure: the partial reply is stored and [error] stays readable. */
    fun failTurn(
        chatId: Long,
        error: ApiError,
    ) {
        val streaming = turns.value[chatId] as? TurnState.Streaming ?: return
        appendMessage(chatId, Role.Assistant, streaming.text, MessageStatus.Failed)
        turns.update { it + (chatId to TurnState.Failed(error)) }
    }

    private fun createChat(
        text: String,
        model: ClaudeModel,
    ): Long {
        val now = clock.now()
        val id = nextChatId++
        val chat =
            Chat(
                id = id,
                title = text.take(ChatRepository.MAX_TITLE_LENGTH),
                model = model,
                snippet = text,
                createdAt = now,
                updatedAt = now,
            )
        chats.update { (it + chat).mostRecentFirst() }
        messages.update { it + (id to emptyList()) }
        return id
    }

    private fun appendMessage(
        chatId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
    ) {
        val now = clock.now()
        val message =
            Message(
                id = nextMessageId++,
                chatId = chatId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = now,
            )
        messages.update { all ->
            all + (chatId to (all[chatId].orEmpty() + message))
        }
        chats.update { list ->
            list
                .map {
                    when {
                        it.id != chatId -> {
                            it
                        }

                        status == MessageStatus.Complete && text.isNotBlank() -> {
                            it.copy(updatedAt = now, snippet = text)
                        }

                        else -> {
                            it.copy(updatedAt = now)
                        }
                    }
                }.mostRecentFirst()
        }
    }

    /**
     * The order the chat list query imposes, applied after every write that can change it
     * rather than left to the order rows happen to be inserted in.
     */
    private fun List<Chat>.mostRecentFirst(): List<Chat> = sortedByDescending { it.updatedAt }
}
