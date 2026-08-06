package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/**
 * Owns chat history and the turn that produces it. The database is the source of truth for every
 * message; only the reply currently streaming is in memory, exposed as [TurnState].
 */
interface ConversationRepository {
    companion object {
        /** How much of a first message becomes a conversation's title. */
        const val MAX_TITLE_LENGTH: Int = 60
    }

    /** Every conversation, most recently updated first. */
    fun getConversationsFlow(): Flow<List<Conversation>>

    /**
     * One conversation, re-emitting whenever it or its messages change. Emits null for a
     * conversation that does not exist or has been deleted.
     */
    fun getConversationFlow(conversationId: Long): Flow<Conversation?>

    /** Every message in one conversation in insertion order, whatever its status. */
    fun getMessagesFlow(conversationId: Long): Flow<List<Message>>

    /**
     * The reply in flight for one conversation. Emits [TurnState.Idle] for a conversation with no
     * turn, and starts observing turns that begin after collection does.
     */
    fun getTurnFlow(conversationId: Long): Flow<TurnState>

    /**
     * Appends [text] as a user message and starts a turn. Returns as soon as the message is
     * stored; the reply arrives on [getTurnFlow] and is persisted when the stream ends, whether
     * anything is still collecting.
     *
     * @param conversationId null to start a new conversation, which is created together with this
     *   first message so an abandoned empty chat never appears in the list.
     * @param model used only when creating a conversation; afterward the conversation's own model
     *   decides, and [setModel] changes it.
     * @return the conversation ID the message landed in.
     * @throws IllegalStateException if that conversation already has a turn in flight.
     * @throws IllegalArgumentException if [conversationId] names a conversation that does not
     *   exist, which a screen left open on a deleted conversation can still ask for.
     */
    suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel = ClaudeModel.Default,
    ): Long

    /**
     * Drops a trailing reply that failed or was cancelled and runs the turn again. Does nothing
     * when the last message is not an unfinished reply.
     */
    suspend fun retry(conversationId: Long)

    /**
     * Stops the turn in flight, keeping whatever text arrived as a cancelled message. Returns once
     * that message is stored. Does nothing when no turn is in flight.
     */
    suspend fun cancel(conversationId: Long)

    /** Changes which model this conversation's later turns use. */
    suspend fun setModel(
        conversationId: Long,
        model: ClaudeModel,
    )

    /** Removes the conversation and its messages, stopping any turn in flight first. */
    suspend fun delete(conversationId: Long)
}
