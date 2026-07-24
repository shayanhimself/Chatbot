package com.shayanaryan.chatbot.shared.chat

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createChatHttpClient(): HttpClient =
    HttpClient(OkHttp) {
        installChatDefaults()
    }
