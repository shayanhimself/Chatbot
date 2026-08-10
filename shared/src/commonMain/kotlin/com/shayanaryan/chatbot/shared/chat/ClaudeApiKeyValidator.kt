package com.shayanaryan.chatbot.shared.chat

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

private const val MODELS_LIMIT = 1

/**
 * The one production [ApiKeyValidator]. Lists a single model: the endpoint authenticates, consumes
 * no tokens, runs no inference and streams nothing, so a check costs the user nothing and needs
 * none of the engine's SSE machinery.
 */
internal class ClaudeApiKeyValidator(
    private val client: HttpClient,
) : ApiKeyValidator {
    override suspend fun validate(key: String): KeyValidationResult =
        try {
            val response =
                client.get(MODELS_URL) {
                    header(API_KEY_HEADER, key)
                    header(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
                    parameter(LIMIT_PARAM, MODELS_LIMIT)
                }
            if (response.status.isSuccess()) {
                KeyValidationResult.Valid
            } else {
                KeyValidationResult.Failed(response.toChatError())
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            KeyValidationResult.Failed(cause.toChatError())
        }
}

/** Builds the production validator over the module's shared client. */
fun createApiKeyValidator(httpClient: ChatHttpClient): ApiKeyValidator =
    ClaudeApiKeyValidator(httpClient.client)
