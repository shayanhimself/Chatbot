package com.shayanaryan.chatbot.shared.chat.dto

import com.shayanaryan.chatbot.shared.chat.ChatRequest
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val THINKING_DISABLED = "disabled"
private const val TYPE_TEXT = "text"
private const val ROLE_USER = "user"
private const val ROLE_ASSISTANT = "assistant"

@Serializable
internal data class MessageRequestDto(
    val model: String,
    @SerialName("max_tokens") val maxTokens: Int,
    val messages: List<MessageDto>,
    val system: String? = null,
    val stream: Boolean = true,
    val thinking: ThinkingDto = ThinkingDto(),
)

@Serializable
internal data class ThinkingDto(
    val type: String = THINKING_DISABLED,
)

@Serializable
internal data class MessageDto(
    val role: String,
    val content: List<ContentBlockDto>,
)

@Serializable
internal data class ContentBlockDto(
    val type: String = TYPE_TEXT,
    val text: String,
)

internal fun ChatRequest.toDto(): MessageRequestDto =
    MessageRequestDto(
        model = model.id,
        maxTokens = maxTokens,
        system = system,
        messages =
            messages.map { message ->
                MessageDto(
                    role =
                        when (message.role) {
                            Role.User -> ROLE_USER
                            Role.Assistant -> ROLE_ASSISTANT
                        },
                    content =
                        message.content.map { block ->
                            when (block) {
                                is ContentBlock.Text -> ContentBlockDto(text = block.text)
                            }
                        },
                )
            },
    )
