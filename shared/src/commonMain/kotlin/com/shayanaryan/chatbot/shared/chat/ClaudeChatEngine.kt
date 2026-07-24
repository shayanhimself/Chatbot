package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.chat.dto.MessageRequestDto
import com.shayanaryan.chatbot.shared.chat.dto.toDto
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val MESSAGES_URL = "https://api.anthropic.com/v1/messages"
private const val ANTHROPIC_VERSION_HEADER = "anthropic-version"
private const val ANTHROPIC_VERSION = "2023-06-01"
private const val API_KEY_HEADER = "x-api-key"
private const val SSE_CONTENT_TYPE = "text/event-stream"

/** The one production [ChatEngine]: Ktor over the Anthropic Messages API with hand-rolled SSE. */
internal class ClaudeChatEngine(
    private val client: HttpClient,
    private val keyProvider: ApiKeyProvider,
) : ChatEngine {
    override fun stream(request: ChatRequest): Flow<ChatStreamEvent> =
        flow {
            val apiKey = keyProvider.apiKey()
            val body = chatJson.encodeToString(MessageRequestDto.serializer(), request.toDto())
            client
                .preparePost(MESSAGES_URL) {
                    header(API_KEY_HEADER, apiKey)
                    header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                    header(HttpHeaders.Accept, SSE_CONTENT_TYPE)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.execute { }
        }
}
