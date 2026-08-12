package com.shayanaryan.chatbot.shared.chat

import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository

/**
 * Builds the production [ChatEngine].
 *
 * @param httpClient the module's shared client, so the engine and the key validator use one
 *   connection pool rather than one each.
 * @param repository consulted once per request for the user's API key.
 */
fun createChatEngine(
    httpClient: ChatHttpClient,
    repository: ApiKeyRepository,
): ChatEngine = ClaudeChatEngine(httpClient.client, repository)
