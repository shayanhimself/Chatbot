package com.shayanaryan.chatbot.shared.chat

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val SOCKET_TIMEOUT_MILLIS = 60_000L

/**
 * Configuration shared by every platform's client. No request timeout: a streamed turn is
 * long-lived by design, so only connect and byte-gap stalls are bounded.
 */
internal fun HttpClientConfig<*>.installChatDefaults() {
    install(HttpTimeout) {
        requestTimeoutMillis = null
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

/** Builds the platform's Ktor client. The engine artifact is the only platform-specific piece. */
internal expect fun createChatHttpClient(): HttpClient
