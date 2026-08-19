package com.shayanaryan.chatbot.wire

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.di.ApiKeyModule
import com.shayanaryan.chatbot.shared.ContentBlock
import com.shayanaryan.chatbot.shared.Role
import com.shayanaryan.chatbot.shared.apikey.ApiKeyRepository
import com.shayanaryan.chatbot.shared.apikey.TestApiKeyStore
import com.shayanaryan.chatbot.shared.claude.ApiKeyValidator
import com.shayanaryan.chatbot.shared.claude.ClaudeEngine
import com.shayanaryan.chatbot.shared.claude.ClaudeMessage
import com.shayanaryan.chatbot.shared.claude.ClaudeMessageRequest
import com.shayanaryan.chatbot.shared.claude.ClaudeStreamEvent
import com.shayanaryan.chatbot.shared.claude.StopReason
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals

private const val REACHABILITY_KEY = "sk-ant-api03-reachability"
private const val MODELS_PATH = "/v1/models"
private const val VERSION_HEADER = "anthropic-version"
private const val VERSION = "2023-06-01"
private const val OK = 200
private const val STORED_KEY = "sk-ant-api03-stored"
private const val QUESTION = "What does the wire carry?"
private const val ANSWER = "A stream of frames."

/**
 * The harness itself: the app's own client reaching the local server, over the proxy and through
 * the tunnel.
 *
 * This is the first thing to look at when a wire test fails, because it separates a broken harness
 * from a broken app. The path and version header it asserts on belong to the module that builds
 * the request, which keeps them private, so they are spelled again here.
 */
@HiltAndroidTest
@UninstallModules(ApiKeyModule::class)
@RunWith(AndroidJUnit4::class)
class LocalAnthropicTest {
    /** The order is 0 because Hilt must wrap everything, so the test component is set up before any rule that touches
     * injected state.
     */
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val anthropic = LocalAnthropic()

    @get:Rule(order = 2)
    val keyStore = TestApiKeyStore()

    /** The store the engine reads its key from, replacing the app's own for these tests. */
    @BindValue
    @JvmField
    val apiKeyRepository: ApiKeyRepository = keyStore.repository

    @Inject
    lateinit var validator: ApiKeyValidator

    @Inject
    lateinit var engine: ClaudeEngine

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun `the http client the app owns arrives here`() =
        runTest {
            anthropic.enqueueStatus(OK)

            validator.validate(REACHABILITY_KEY)

            val request = anthropic.lastRequest()
            assertEquals(MODELS_PATH, request.target.substringBefore('?'))
            assertEquals(VERSION, request.headers[VERSION_HEADER])
        }

    @Test
    fun `a queued reply parses through the stream reader the app owns`() =
        runTest {
            apiKeyRepository.save(STORED_KEY)
            anthropic.enqueueReply(ANSWER)

            val events =
                engine
                    .stream(
                        ClaudeMessageRequest(
                            messages =
                                listOf(
                                    ClaudeMessage(Role.User, listOf(ContentBlock.Text(QUESTION))),
                                ),
                        ),
                    ).toList()

            assertEquals(listOf(ClaudeStreamEvent.Delta(ANSWER)), events.dropLast(1))
            assertEquals(
                StopReason.EndTurn,
                (events.last() as ClaudeStreamEvent.Completed).stopReason,
            )
        }
}
