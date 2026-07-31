package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * In-memory [ConversationRepository] for tests above the data layer.
 *
 * There is no engine: a turn is opened by [send] or [retry] and moved by [emitDelta],
 * [completeTurn] and [failTurn], so a test decides exactly when a reply arrives. Turn state is
 * held independently of any collector, mirroring the production guarantee that a turn outlives
 * the screen that started it.
 */
class FakeConversationRepository(
    private val clock: Clock = FakeClock(autoAdvanceBy = 1.milliseconds),
) : ConversationRepository {
    private val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val turns = MutableStateFlow<Map<Long, TurnState>>(emptyMap())

    private var nextConversationId = 1L
    private var nextMessageId = 1L

    override fun getConversationsFlow(): Flow<List<Conversation>> = conversations

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        messages.map { it[conversationId].orEmpty() }.distinctUntilChanged()

    override fun getTurnFlow(conversationId: Long): Flow<TurnState> =
        turns.map { it[conversationId] ?: TurnState.Idle }.distinctUntilChanged()

    override suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long {
        if (conversationId != null) {
            require(conversations.value.any { it.id == conversationId }) {
                "conversation $conversationId does not exist"
            }
        }
        check(turns.value[conversationId] !is TurnState.Streaming) {
            "conversation $conversationId already has a turn in flight"
        }
        val id = conversationId ?: createConversation(text, model)
        appendMessage(id, Role.User, text, MessageStatus.Complete)
        turns.update { it + (id to TurnState.Streaming("")) }
        return id
    }

    override suspend fun retry(conversationId: Long) {
        val last = messages.value[conversationId]?.lastOrNull() ?: return
        if (last.role != Role.Assistant || last.status == MessageStatus.Complete) return
        messages.update { all ->
            all + (conversationId to all.getValue(conversationId).dropLast(1))
        }
        turns.update { it + (conversationId to TurnState.Streaming("")) }
    }

    override suspend fun cancel(conversationId: Long) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Cancelled)
        turns.update { it - conversationId }
    }

    override suspend fun setModel(
        conversationId: Long,
        model: ClaudeModel,
    ) {
        conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(model = model) else it }
        }
    }

    override suspend fun delete(conversationId: Long) {
        conversations.update { list -> list.filterNot { it.id == conversationId } }
        messages.update { it - conversationId }
        turns.update { it - conversationId }
    }

    /** Grows the reply in flight, as a stream delta would. */
    fun emitDelta(
        conversationId: Long,
        text: String,
    ) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        turns.update {
            it + (conversationId to TurnState.Streaming(streaming.text + text))
        }
    }

    fun completeTurn(conversationId: Long) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Complete)
        turns.update { it - conversationId }
    }

    /** Ends the turn in failure: the partial reply is stored and [error] stays readable. */
    fun failTurn(
        conversationId: Long,
        error: ChatError,
    ) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Failed)
        turns.update { it + (conversationId to TurnState.Failed(error)) }
    }

    private fun createConversation(
        text: String,
        model: ClaudeModel,
    ): Long {
        val now = clock.now()
        val id = nextConversationId++
        val conversation =
            Conversation(
                id = id,
                title = text.take(ConversationRepository.MAX_TITLE_LENGTH),
                model = model,
                createdAt = now,
                updatedAt = now,
            )
        conversations.update { (it + conversation).mostRecentFirst() }
        messages.update { it + (id to emptyList()) }
        return id
    }

    private fun appendMessage(
        conversationId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
    ) {
        val now = clock.now()
        val message =
            Message(
                id = nextMessageId++,
                conversationId = conversationId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = now,
            )
        messages.update { all ->
            all + (conversationId to (all[conversationId].orEmpty() + message))
        }
        conversations.update { list ->
            list
                .map { if (it.id == conversationId) it.copy(updatedAt = now) else it }
                .mostRecentFirst()
        }
    }

    /**
     * The order the conversation list query imposes, applied after every write that can change it
     * rather than left to the order rows happen to be inserted in.
     */
    private fun List<Conversation>.mostRecentFirst(): List<Conversation> =
        sortedByDescending { it.updatedAt }
}
