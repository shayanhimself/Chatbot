package com.shayanaryan.chatbot.shared.chat

import kotlinx.serialization.json.Json

/** Shared codec for the Messages API. */
internal val chatJson =
    Json {
        // Keeps new server fields from breaking us.
        ignoreUnknownKeys = true
        // Emits `stream: true`.
        encodeDefaults = true
        // Omits an absent `system`.
        explicitNulls = false
    }
