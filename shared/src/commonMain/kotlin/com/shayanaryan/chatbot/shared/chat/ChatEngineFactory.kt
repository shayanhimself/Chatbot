package com.shayanaryan.chatbot.shared.chat

/**
 * Builds the production [ChatEngine].
 *
 * @param httpClient the module's shared client, so the engine and the key validator use one
 *   connection pool rather than one each.
 * @param keyProvider consulted once per request for the user's API key.
 */
fun createChatEngine(
    httpClient: ChatHttpClient,
    keyProvider: ApiKeyProvider,
): ChatEngine = ClaudeChatEngine(httpClient.client, keyProvider)
