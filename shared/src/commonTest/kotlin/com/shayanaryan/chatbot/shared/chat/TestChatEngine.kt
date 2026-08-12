package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.FakeApiKeyRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel

private const val TEST_API_KEY = "sk-ant-test"

/** Builds a real [ClaudeChatEngine] over a [MockEngine], so tests exercise the whole HTTP path. */
internal fun testChatEngine(
    repository: ApiKeyRepository = FakeApiKeyRepository(initialKey = TEST_API_KEY),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): ClaudeChatEngine =
    ClaudeChatEngine(
        client = HttpClient(MockEngine { request -> handler(request) }) { installChatDefaults() },
        repository = repository,
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
