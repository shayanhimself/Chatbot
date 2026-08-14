package com.shayanaryan.chatbot.shared.claude

import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository

/**
 * Builds the production [ClaudeEngine].
 *
 * @param httpClient the module's shared client, so the engine and the key validator use one
 *   connection pool rather than one each.
 * @param repository consulted once per request for the user's API key.
 */
fun createClaudeEngine(
    httpClient: ClaudeHttpClient,
    repository: ApiKeyRepository,
): ClaudeEngine = DefaultClaudeEngine(httpClient.client, repository)
