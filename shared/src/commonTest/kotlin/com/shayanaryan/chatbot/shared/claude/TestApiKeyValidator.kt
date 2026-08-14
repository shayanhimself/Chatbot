package com.shayanaryan.chatbot.shared.claude

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData

/** Builds a real [ClaudeApiKeyValidator] over a [MockEngine], so tests exercise the whole HTTP path. */
internal fun testApiKeyValidator(
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
): ClaudeApiKeyValidator =
    ClaudeApiKeyValidator(
        client = HttpClient(MockEngine { request -> handler(request) }) { installClaudeDefaults() },
    )
