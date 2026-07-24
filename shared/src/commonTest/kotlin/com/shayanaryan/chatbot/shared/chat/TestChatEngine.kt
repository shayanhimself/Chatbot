package com.shayanaryan.chatbot.shared.chat

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel

/** Builds a real [ClaudeChatEngine] over a [MockEngine], so tests exercise the whole HTTP path. */
internal fun testChatEngine(
    apiKey: String = "sk-ant-test",
    keyProvider: ApiKeyProvider = ApiKeyProvider { apiKey },
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): ClaudeChatEngine =
    ClaudeChatEngine(
        client = HttpClient(MockEngine { request -> handler(request) }) { installChatDefaults() },
        keyProvider = keyProvider,
    )

/** Responds with [body] as a streamed `text/event-stream`. */
internal fun MockRequestHandleScope.respondSse(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): HttpResponseData =
    respond(
        content = ByteReadChannel(body.encodeToByteArray()),
        status = status,
        headers = headersOf("Content-Type", "text/event-stream"),
    )
