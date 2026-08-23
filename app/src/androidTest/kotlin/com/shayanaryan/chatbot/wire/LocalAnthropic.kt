package com.shayanaryan.chatbot.wire

import androidx.test.platform.app.InstrumentationRegistry
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okhttp3.Protocol
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.rules.ExternalResource
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Must match `PROXY_PORT` in `scripts/instrumented.sh`, which points the device's proxy here. */
const val PROXY_PORT = 8099

private const val ANTHROPIC_HOST = "api.anthropic.com"
private const val CA_ASSET = "test_ca_with_key.pem"
private const val SSE_CONTENT_TYPE = "text/event-stream"
private const val CONTENT_TYPE_HEADER = "Content-Type"
private const val RETRY_AFTER_HEADER = "retry-after"
private const val CONNECT_METHOD = "CONNECT"
private const val OK = 200
private const val REQUEST_TIMEOUT_SECONDS = 10L

private const val REPLY_MESSAGE_ID = "msg_01LocalAnthropic"
private const val REPLY_MODEL = "claude-sonnet-5"

/**
 * Answers as `api.anthropic.com` for the duration of any instrumented test.
 *
 * The app's base URL is a compile-time constant, so nothing in the app is redirected or replaced.
 * The device's global proxy sends the request here instead, this server answers the `CONNECT` and
 * serves TLS inside the tunnel with a leaf for that hostname, and the app's own OkHttp, TLS stack
 * and SSE parser run untouched.
 *
 * The server is `mockwebserver3`, the artifact that carries OkHttp 5's server.
 */
class LocalAnthropic : ExternalResource() {
    private lateinit var server: MockWebServer
    private val dispatcher = TunnellingDispatcher()

    override fun before() {
        val authority = HeldCertificate.decode(caPem())
        val leaf =
            HeldCertificate
                .Builder()
                .addSubjectAlternativeName(ANTHROPIC_HOST)
                .signedBy(authority)
                .build()
        val certificates =
            HandshakeCertificates
                .Builder()
                .heldCertificate(leaf, authority.certificate)
                .build()

        server = MockWebServer()
        server.dispatcher = dispatcher
        // HTTP/1.1 only, so that dropping a reply part-way through is a socket closing under the
        // client rather than a stream reset the framing layer reports for it.
        server.protocols = listOf(Protocol.HTTP_1_1)
        server.useHttps(certificates.sslSocketFactory())
        server.start(PROXY_PORT)
    }

    override fun after() {
        server.close()
    }

    private fun caPem(): String =
        InstrumentationRegistry
            .getInstrumentation()
            .context
            .assets
            .open(CA_ASSET)
            .bufferedReader()
            .use { it.readText() }

    /** Answers the next request with a complete streamed reply carrying [text]. */
    fun enqueueReply(text: String) {
        dispatcher.enqueue(
            MockResponse
                .Builder()
                .code(OK)
                .setHeader(CONTENT_TYPE_HEADER, SSE_CONTENT_TYPE)
                .body(sseReply(text))
                .build(),
        )
    }

    /** Answers the next request with [code], and a `retry-after` header when one is given. */
    fun enqueueStatus(
        code: Int,
        retryAfterSeconds: Int? = null,
    ) {
        val response = MockResponse.Builder().code(code)
        retryAfterSeconds?.let { response.setHeader(RETRY_AFTER_HEADER, it.toString()) }
        dispatcher.enqueue(response.build())
    }

    /**
     * Starts a reply and drops the connection part-way through it, which is the failure a mocked
     * engine cannot produce: the app sees a real socket close mid-stream.
     */
    fun enqueueDisconnectMidStream(text: String) {
        dispatcher.enqueue(
            MockResponse
                .Builder()
                .code(OK)
                .setHeader(CONTENT_TYPE_HEADER, SSE_CONTENT_TYPE)
                .body(sseReply(text))
                .onResponseBody(SocketEffect.CloseSocket())
                .build(),
        )
    }

    /**
     * Answers the next request with [text] arriving a piece at a time, slowly.
     *
     * The reply is split into [chunkLength] deltas, so the app accumulates a reply across many
     * events rather than receiving it whole, and the body is throttled so the pieces are spread
     * over time. Both halves are needed: throttling one delta only slows the bytes of a line the
     * parser cannot emit until it is complete.
     *
     * @param chunkLength characters per delta.
     * @param bytesPerPeriod how much of the body is released per [periodMillis].
     */
    fun enqueueSlowReply(
        text: String,
        chunkLength: Int,
        bytesPerPeriod: Long,
        periodMillis: Long,
    ) {
        dispatcher.enqueue(
            MockResponse
                .Builder()
                .code(OK)
                .setHeader(CONTENT_TYPE_HEADER, SSE_CONTENT_TYPE)
                .body(sseReply(text, chunkLength))
                .throttleBody(bytesPerPeriod, periodMillis, TimeUnit.MILLISECONDS)
                .build(),
        )
    }

    /**
     * The next request the app sent, for asserting path, headers and body.
     *
     * The server records the `CONNECT` that opens the tunnel alongside the requests sent inside
     * it, and how many of those there are depends on how the client pooled its connections. They
     * are dropped here so a caller sees only what the app meant to send.
     */
    fun lastRequest(): RecordedRequest {
        while (true) {
            val request =
                server.takeRequest(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    ?: error(
                        "no request reached the server within $REQUEST_TIMEOUT_SECONDS seconds",
                    )
            if (request.method != CONNECT_METHOD) return request
        }
    }
}

/** Answers `CONNECT` itself and serves the enqueued replies to everything else. */
private class TunnellingDispatcher : Dispatcher() {
    private val replies = LinkedBlockingQueue<MockResponse>()

    /**
     * Whether the next request to arrive opens a tunnel.
     *
     * Shared across connections, so this holds only while one connection is negotiated at a time:
     * two clients opening tunnels at once would read each other's state. The tests drive a single
     * request at a time.
     */
    @Volatile
    private var expectingConnect = true

    fun enqueue(response: MockResponse) {
        replies.add(response)
    }

    override fun peek(): MockResponse =
        when {
            expectingConnect -> TUNNEL_RESPONSE
            else -> replies.peek() ?: MockResponse()
        }

    /**
     * Answers one request, `CONNECT` included.
     *
     * The server decides whether a socket opens with a tunnel by peeking at the next response, and
     * a `CONNECT` consumes one. Queueing a tunnel response ahead of every reply would therefore
     * drift the moment the client reuses a connection, because one `CONNECT` then covers several
     * requests. Answering it here keeps the queue holding replies only.
     */
    override fun dispatch(request: RecordedRequest): MockResponse =
        when (request.method) {
            CONNECT_METHOD -> {
                expectingConnect = false
                TUNNEL_RESPONSE
            }

            else -> {
                expectingConnect = true
                replies.take()
            }
        }

    private companion object {
        val TUNNEL_RESPONSE: MockResponse = MockResponse.Builder().inTunnel().build()
    }
}

/**
 * An event stream carrying [text] as one reply, in deltas of [chunkLength] characters.
 *
 * The frames are the shapes `scripts/record-sse-fixture.sh` captured from the live API. The
 * capture itself belongs to another module's test source set and cannot be imported here, so the
 * shapes are reproduced rather than shared.
 *
 * @param chunkLength characters per delta. The whole reply in one delta is the degenerate case,
 *   and not what the API sends.
 */
private fun sseReply(
    text: String,
    chunkLength: Int = Int.MAX_VALUE,
): String =
    buildString {
        appendLine("event: message_start")
        appendLine(
            "data: {\"type\":\"message_start\",\"message\":{\"id\":\"$REPLY_MESSAGE_ID\"," +
                "\"type\":\"message\",\"role\":\"assistant\",\"model\":\"$REPLY_MODEL\"," +
                "\"content\":[],\"stop_reason\":null," +
                "\"usage\":{\"input_tokens\":1,\"output_tokens\":1}}}",
        )
        appendLine()
        text.chunked(chunkLength).forEach { chunk ->
            appendLine("event: content_block_delta")
            appendLine(
                "data: {\"type\":\"content_block_delta\",\"index\":0," +
                    "\"delta\":{\"type\":\"text_delta\",\"text\":\"${chunk.jsonEscaped()}\"}}",
            )
            appendLine()
        }
        appendLine("event: message_delta")
        appendLine(
            "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\"}," +
                "\"usage\":{\"output_tokens\":1}}",
        )
        appendLine()
        appendLine("event: message_stop")
        appendLine("data: {\"type\":\"message_stop\"}")
        appendLine()
    }

private fun String.jsonEscaped(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
