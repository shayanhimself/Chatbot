package com.shayanaryan.chatbot.feature.chat.detail

import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.chat.Message
import com.shayanaryan.chatbot.shared.chat.TurnState
import com.shayanaryan.chatbot.shared.textContent

/**
 * Maps stored messages and the turn in flight into the one list the message list renders.
 * Blank messages are dropped (an empty bubble is noise).
 *
 * The error comes from the trailing reply rather than from [turn], so a chat reopened after the
 * process died still offers the retry. Only a trailing failure is live: an earlier one was already
 * answered by the reply that follows it.
 *
 * @param turn the reply in flight, which contributes at most one trailing item.
 * @return every message worth rendering, oldest first, with the live item last when there is one.
 */
internal fun List<Message>.toChatDetailItems(turn: TurnState): List<ChatDetailItem> {
    val items = filter { it.content.textContent().isNotBlank() }.map(ChatDetailItem::Persisted)
    val last = lastOrNull()
    val failure = last?.error
    val awaitingReply = last?.role == Role.User
    val trailing =
        when {
            failure != null -> ChatDetailItem.Error(failure)

            // The reply already landed in the database, so any live text is stale.
            !awaitingReply -> null

            turn is TurnState.Streaming && turn.text.isEmpty() -> ChatDetailItem.Thinking

            turn is TurnState.Streaming -> ChatDetailItem.Streaming(turn.text)

            else -> null
        }
    return if (trailing == null) items else items + trailing
}
