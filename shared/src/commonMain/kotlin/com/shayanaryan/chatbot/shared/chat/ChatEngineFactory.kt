package com.shayanaryan.chatbot.shared.chat

/**
 * Builds the production [ChatEngine]. The only assembly seam this module exposes — the Ktor
 * client, its configuration, and the engine implementation all stay internal.
 *
 * @param keyProvider consulted once per request for the user's API key.
 */
fun createChatEngine(keyProvider: ApiKeyProvider): ChatEngine =
    ClaudeChatEngine(createChatHttpClient(), keyProvider)
