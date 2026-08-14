package com.shayanaryan.chatbot.shared.claude

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val SOCKET_TIMEOUT_MILLIS = 60_000L

/**
 * The module's HTTP client, as an opaque handle. Everything that talks to Anthropic takes one of
 * these, so a single connection pool and a single set of timeouts serve the whole app, and no
 * caller outside `:shared` ever sees a Ktor type.
 */
class ClaudeHttpClient internal constructor(
    internal val client: HttpClient,
)

/** Builds the platform's client. The engine artifact is the only platform-specific piece. */
fun createClaudeHttpClient(): ClaudeHttpClient = ClaudeHttpClient(createPlatformHttpClient())

/**
 * Configuration shared by every platform's client. No request timeout: a streamed turn is
 * long-lived by design, so only connect and byte-gap stalls are bounded.
 */
internal fun HttpClientConfig<*>.installClaudeDefaults() {
    install(HttpTimeout) {
        requestTimeoutMillis = null
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

internal expect fun createPlatformHttpClient(): HttpClient
