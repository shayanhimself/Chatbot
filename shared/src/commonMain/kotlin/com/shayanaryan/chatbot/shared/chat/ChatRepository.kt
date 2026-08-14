package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/**
 * Owns chat history and the turn that produces it. The database is the source of truth for every
 * message; only the reply currently streaming is in memory, exposed as [TurnState].
 */
interface ChatRepository {
    companion object {
        /** How much of a first message becomes a chat's title. */
        const val MAX_TITLE_LENGTH: Int = 60
    }

    /** Every chat, most recently updated first. */
    fun getChatsFlow(): Flow<List<Chat>>

    /**
     * One chat, re-emitting whenever it or its messages change. Emits null for a
     * chat that does not exist or has been deleted.
     */
    fun getChatFlow(chatId: Long): Flow<Chat?>

    /** Every message in one chat in insertion order, whatever its status. */
    fun getMessagesFlow(chatId: Long): Flow<List<Message>>

    /**
     * The reply in flight for one chat. Emits [TurnState.Idle] for a chat with no
     * turn, and starts observing turns that begin after collection does.
     */
    fun getTurnFlow(chatId: Long): Flow<TurnState>

    /**
     * Appends [text] as a user message and starts a turn. Returns as soon as the message is
     * stored; the reply arrives on [getTurnFlow] and is persisted when the stream ends, whether
     * anything is still collecting.
     *
     * @param chatId null to start a new chat, which is created together with this
     *   first message so an abandoned empty chat never appears in the list.
     * @param model used only when creating a chat; afterward the chat's own model
     *   decides, and [setModel] changes it.
     * @return the chat ID the message landed in.
     * @throws IllegalStateException if that chat already has a turn in flight.
     * @throws IllegalArgumentException if [chatId] names a chat that does not
     *   exist, which a screen left open on a deleted chat can still ask for.
     */
    suspend fun send(
        chatId: Long?,
        text: String,
        model: ClaudeModel = ClaudeModel.Default,
    ): Long

    /**
     * Drops a trailing reply that failed or was cancelled and runs the turn again. Does nothing
     * when the last message is not an unfinished reply.
     */
    suspend fun retry(chatId: Long)

    /**
     * Stops the turn in flight, keeping whatever text arrived as a cancelled message. Returns once
     * that message is stored. Does nothing when no turn is in flight.
     */
    suspend fun cancel(chatId: Long)

    /** Changes which model this chat's later turns use. */
    suspend fun setModel(
        chatId: Long,
        model: ClaudeModel,
    )

    /** Removes the chat and its messages, stopping any turn in flight first. */
    suspend fun delete(chatId: Long)
}
