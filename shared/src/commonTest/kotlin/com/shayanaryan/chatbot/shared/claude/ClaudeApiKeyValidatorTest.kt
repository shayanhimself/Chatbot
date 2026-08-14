package com.shayanaryan.chatbot.shared.claude
import com.shayanaryan.chatbot.shared.ApiError
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val API_KEY = "sk-ant-api03-not-a-real-key"
private const val MODELS_BODY = """{"data":[],"has_more":false}"""
private const val RETRY_AFTER_HEADER = "retry-after"
private const val RETRY_AFTER_SECONDS = 30
private const val STATUS_OVERLOADED = 529
private const val NO_NETWORK_MESSAGE = "no network"

// The query the validator is expected to send, as it appears on the wire. The count the validator
// asks for is private to it, so this is the test's own statement of what one model looks like.
private const val EXPECTED_MODELS_LIMIT = "1"

class ClaudeApiKeyValidatorTest {
    private suspend fun validate(
        status: HttpStatusCode,
        retryAfter: String? = null,
    ): KeyValidationResult =
        testApiKeyValidator { _ ->
            respond(
                content = MODELS_BODY,
                status = status,
                headers = retryAfter?.let { headersOf(RETRY_AFTER_HEADER, it) } ?: headersOf(),
            )
        }.validate(API_KEY)

    @Test
    fun `a 200 means the key is valid`() =
        runTest {
            assertEquals(KeyValidationResult.Valid, validate(HttpStatusCode.OK))
        }

    @Test
    fun `a 401 condemns the key`() =
        runTest {
            assertEquals(
                KeyValidationResult.Failed(ApiError.Authentication),
                validate(HttpStatusCode.Unauthorized),
            )
        }

    @Test
    fun `a 403 condemns the key`() =
        runTest {
            assertEquals(
                KeyValidationResult.Failed(ApiError.Authentication),
                validate(HttpStatusCode.Forbidden),
            )
        }

    @Test
    fun `a 429 is retryable and never condemns the key`() =
        runTest {
            val result =
                validate(HttpStatusCode.TooManyRequests, retryAfter = "$RETRY_AFTER_SECONDS")

            assertEquals(
                KeyValidationResult.Failed(ApiError.RateLimited(RETRY_AFTER_SECONDS)),
                result,
            )
            assertNotEquals(KeyValidationResult.Failed(ApiError.Authentication), result)
        }

    @Test
    fun `a 500 is retryable and never condemns the key`() =
        runTest {
            val result = validate(HttpStatusCode.InternalServerError)

            assertEquals(KeyValidationResult.Failed(ApiError.Server), result)
            assertNotEquals(KeyValidationResult.Failed(ApiError.Authentication), result)
        }

    @Test
    fun `a 529 is retryable and never condemns the key`() =
        runTest {
            val result = validate(HttpStatusCode.fromValue(STATUS_OVERLOADED))

            assertEquals(KeyValidationResult.Failed(ApiError.Overloaded), result)
            assertNotEquals(KeyValidationResult.Failed(ApiError.Authentication), result)
        }

    @Test
    fun `a connection failure is retryable and never condemns the key`() =
        runTest {
            val result =
                testApiKeyValidator { throw IOException(NO_NETWORK_MESSAGE) }.validate(API_KEY)

            assertEquals(KeyValidationResult.Failed(ApiError.Network), result)
            assertNotEquals(KeyValidationResult.Failed(ApiError.Authentication), result)
        }

    @Test
    fun `the request authenticates and asks for one model`() =
        runTest {
            var seen: HttpRequestData? = null
            testApiKeyValidator { request ->
                seen = request
                respond(MODELS_BODY, HttpStatusCode.OK)
            }.validate(API_KEY)

            val request = checkNotNull(seen)
            assertEquals(HttpMethod.Get, request.method)
            assertEquals(API_KEY, request.headers[API_KEY_HEADER])
            assertEquals(ANTHROPIC_VERSION, request.headers[ANTHROPIC_VERSION_HEADER])
            assertTrue(request.url.toString().startsWith(MODELS_URL))
            assertEquals(EXPECTED_MODELS_LIMIT, request.url.parameters[LIMIT_PARAM])
        }

    @Test
    fun `an unexpected status is not treated as a bad key`() =
        runTest {
            val result =
                testApiKeyValidator {
                    respondError(
                        HttpStatusCode.BadGateway,
                    )
                }.validate(API_KEY)

            assertNotEquals(KeyValidationResult.Failed(ApiError.Authentication), result)
        }
}
