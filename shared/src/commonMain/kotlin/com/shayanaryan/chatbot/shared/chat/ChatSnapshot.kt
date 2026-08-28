package com.shayanaryan.chatbot.shared.chat

/**
 * One chat with everything gathered at one instant.
 *
 * @property chat the stored thread.
 * @property messages its messages, in insertion order.
 * @property turn the reply in flight, [TurnState.Idle] when there is none.
 */
data class ChatSnapshot(
    val chat: Chat,
    val messages: List<Message>,
    val turn: TurnState,
)
