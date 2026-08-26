package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.ApiError
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.time.Instant

/**
 * One chat thread.
 *
 * @property id assigned by the database on insert.
 * @property title the first message, truncated to [ChatRepository.MAX_TITLE_LENGTH].
 * @property model which Claude model this chat's turns use.
 * @property snippet the last complete message's text, projected by the list query rather than
 *   stored. Null for a chat with no complete message yet.
 * @property createdAt when the first message was sent.
 * @property updatedAt when the last message landed. The list's ordering key.
 */
data class Chat(
    val id: Long,
    val title: String,
    val model: ClaudeModel,
    val snippet: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * One message in a chat.
 *
 * @property id assigned by the database on insert.
 * @property chatId the chat this message belongs to.
 * @property role who wrote it.
 * @property content the message's blocks, in order.
 * @property status how the message ended.
 * @property error why the turn stopped, null unless [status] is [MessageStatus.Failed].
 * @property createdAt when the message was stored.
 */
data class Message(
    val id: Long,
    val chatId: Long,
    val role: Role,
    val content: List<ContentBlock>,
    val status: MessageStatus,
    val error: ApiError? = null,
    val createdAt: Instant,
)

/**
 * How a message ended. Only [Complete] messages are sent back to the model.
 */
enum class MessageStatus { Complete, Failed, Cancelled }
