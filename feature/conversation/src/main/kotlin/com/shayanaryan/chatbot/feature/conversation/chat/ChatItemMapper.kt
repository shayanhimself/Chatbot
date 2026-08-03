package com.shayanaryan.chatbot.feature.conversation.chat

import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.textContent
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.TurnState

/**
 * Maps stored messages and the turn in flight into the one list the message list renders.
 * Blank messages are dropped (an empty bubble is noise).
 *
 * @param turn the reply in flight, which contributes at most one trailing item.
 * @return every message worth rendering, oldest first, with the live item last when there is one.
 */
internal fun List<Message>.toChatItems(turn: TurnState): List<ChatItem> {
    val items = filter { it.content.textContent().isNotBlank() }.map(ChatItem::Persisted)
    val awaitingReply = lastOrNull()?.role == Role.User
    val trailing =
        when {
            turn is TurnState.Failed -> ChatItem.Error(turn.error)
            // The reply already landed in the database, so any live text is stale.
            !awaitingReply -> null
            turn is TurnState.Streaming && turn.text.isEmpty() -> ChatItem.Thinking
            turn is TurnState.Streaming -> ChatItem.Streaming(turn.text)
            else -> null
        }
    return if (trailing == null) items else items + trailing
}
