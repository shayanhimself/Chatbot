# 003 — Chat Engine

The `ChatEngine`: the app's sole interface to Claude. It streams a conversation to `POST /v1/messages` and emits the assistant's reply as it arrives. Lives in `:shared` commonMain (data layer); stateless — callers pass full history in, nothing is persisted here.

Scope is text streaming only. Tool use (`tool_use` / `tool_result`, agentic loop) is deferred to 008; memory to 009; persistence to 004; UI/ViewModel to 005; key storage to 006/007. See the deferrals at the bottom.

## Contract

```
interface ChatEngine {
  fun stream(request: ChatRequest): Flow<ChatStreamEvent>
}
```

One cold `Flow` per call. Main-safe (Ktor suspends on its own dispatchers). Emits exactly one terminal `Completed` or `Failed`, then completes. It never throws for API or domain errors — every outcome is an event a ViewModel folds into `UiState` (architecture: no ViewModel→UI event bus). Only structured cancellation propagates, cancelling the in-flight HTTP call.

## Types

All in commonMain.

```
data class ChatRequest(
  val messages: List<ChatMessage>,
  val model: ClaudeModel,          // existing enum: Sonnet default, Haiku, Opus
  val system: String?,
  val maxTokens: Int,              // required by the API; shared default constant
)
data class ChatMessage(val role: Role, val content: List<ContentBlock>)
enum class Role { User, Assistant }
sealed interface ContentBlock { data class Text(val text: String) : ContentBlock }
                                 // ToolUse / ToolResult added in 008

sealed interface ChatStreamEvent {
  data class Delta(val text: String) : ChatStreamEvent
  data class Completed(val stopReason: StopReason, val usage: TokenUsage) : ChatStreamEvent
  data class Failed(val error: ChatError) : ChatStreamEvent
}
enum class StopReason { EndTurn, MaxTokens, StopSequence, Refusal, Unknown }  // ToolUse added 008
data class TokenUsage(val inputTokens: Int, val outputTokens: Int)

sealed interface ChatError {                    // no user-facing text (architecture rule)
  data object Authentication : ChatError        // 401 / 403
  data class  RateLimited(val retryAfterSeconds: Int?) : ChatError  // 429
  data object Overloaded : ChatError            // 529
  data object InvalidRequest : ChatError        // 400
  data object Server : ChatError                // 5xx
  data object Network : ChatError               // no connectivity
  data object Timeout : ChatError               // stall between bytes
  data object Unexpected : ChatError            // malformed SSE / JSON
}

fun interface ApiKeyProvider { suspend fun apiKey(): String }
```

`ContentBlock` is a list rather than a bare `String` so 008 can add `ToolUse` / `ToolResult` without reshaping `ChatMessage`. Only `Text` exists in this spec.

`ChatError` carries no strings a user reads; feature ViewModels map each case to a string resource. `RateLimited` and `Overloaded` expose enough for a caller retry policy — the engine itself does not retry.

## Implementation

`ClaudeChatEngine(client: HttpClient, keyProvider: ApiKeyProvider)` is the one concrete engine. The `ChatEngine` interface is retained despite the single implementation for test fakes and a future iOS on-device engine (per `001`).

**Request.** `POST https://api.anthropic.com/v1/messages`. Headers: `x-api-key` (from `keyProvider.apiKey()` per request — never held or logged), `anthropic-version: 2023-06-01`, `content-type: application/json`, `accept: text/event-stream`. Body: `{ model, max_tokens, system?, stream: true, messages: [{ role, content: [{ type: "text", text }] }] }`. The `thinking` parameter is omitted.

**SSE.** Hand-rolled per `001` (no Ktor SSE plugin) — one parse path that extends to `input_json_delta` in 008. The response body is read as a stream, not buffered: `preparePost(...).execute { }` returns before the body is read, and `bodyAsChannel().readUTF8Line()` pulls frames as they arrive. Dispatch on the `data` JSON `type`:

- `message_start` — capture input token usage.
- `content_block_delta` with a `text_delta` — emit `Delta`. `thinking` blocks and any non-`text_delta` block are ignored.
- `message_delta` — capture `stop_reason` and cumulative output usage.
- `message_stop` — emit `Completed`.
- SSE `event: error` — emit `Failed`, then end.

**Errors.** A non-2xx status before streaming maps to a `ChatError` (the `retry-after` header populates `RateLimited`). Lost connectivity maps to `Network`; a byte-gap stall (`socketTimeout`) to `Timeout`; a malformed frame or JSON to `Unexpected`.

## Module and DI

`:shared` owns construction; `:app` owns only DI registration. `:shared` assembles the engine and keeps the Ktor `HttpClient` internal — nothing outside the module sees the client. It exposes exactly two public symbols: the `ChatEngine` interface and a factory:

```
// commonMain — client, config, and impl are all internal
internal fun HttpClientConfig<*>.installChatDefaults() { /* ContentNegotiation(json), HttpTimeout */ }
internal expect fun createChatHttpClient(): HttpClient
internal class ClaudeChatEngine(...) : ChatEngine

fun createChatEngine(keyProvider: ApiKeyProvider): ChatEngine =   // the only assembly seam
    ClaudeChatEngine(createChatHttpClient(), keyProvider)

// androidMain — the engine is the only platform-specific piece
internal actual fun createChatHttpClient(): HttpClient = HttpClient(OkHttp) { installChatDefaults() }
```

`HttpTimeout`: `requestTimeoutMillis = null` (the stream is long-lived), `connectTimeoutMillis = 15_000`, `socketTimeoutMillis = 60_000`. A future iosMain `actual` uses `HttpClient(Darwin)` with the same config — which is why the client factory is `:shared`, not `:app`. `ktor-client-core` is the multiplatform API used everywhere; only the engine artifact (`ktor-client-okhttp`) is androidMain.

`:app` contributes one Hilt binding — `@Provides fun chatEngine(keyProvider) = createChatEngine(keyProvider)` — and the `ApiKeyProvider` binding. It never references `HttpClient`. `:app` is irreducible only because Hilt cannot enter `:shared` and because `ApiKeyProvider`'s real implementation is assembled by Hilt from androidMain crypto (006).

`ApiKeyProvider` is defined here as an interface only; no production implementation ships in this spec.

## Testing

TDD, fakes not mocks (architecture).

- **`FakeChatEngine`** (commonTest) — emits scripted event lists so later feature specs test against it, never the network.
- **`ClaudeChatEngine`** — hermetic, via Ktor `MockEngine` feeding fixtures recorded from a real API response (not invented): happy path, `Refusal` stop, each error status → the correct `ChatError`, `retry-after` parsing, malformed SSE → `Unexpected`, a mid-stream `error` event, and cancellation mid-stream. `Json { ignoreUnknownKeys = true }`.
- **Gated integration test** — hits the real endpoint with a dev key read from the environment or `local.properties`, and is skipped when the key is absent so the default suite stays hermetic and CI-safe. Its captured SSE seeds the fixtures above; this is how the streaming risk is retired before any UI exists.

Real end-to-end verification (real key, on-device) happens at the M1 sideload checkpoint via journey XMLs (`001`'s E2E lane), owned by 005 onward.

## Deferred to later specs

This engine is deliberately incomplete. Later specs own the pieces that plug into it:

| Piece | Owner | What it adds |
|---|---|---|
| `DefaultApiKeyProvider` | 006 | Real `ApiKeyProvider`: reads Keystore-backed Tink AEAD ciphertext from DataStore, decrypts per call. Bound to `ApiKeyProvider` by a Hilt module in `:app`. |
| Dev-key `ApiKeyProvider` stub | 005 | Debug-build implementation returning the developer's own key from build config, so chat runs before onboarding exists. Replaced by 006. |
| `ContentBlock.ToolUse` / `ToolResult`, `StopReason.ToolUse` | 008 | Tool-use content blocks and the `input_json_delta` SSE path; the agentic loop (model emits tool call → app executes → resume) in commonMain. |
| Memory injection | 009 | System-prompt / message assembly that feeds stored memories into `ChatRequest`. Engine is unchanged; the caller composes the request. |
| Conversation persistence | 004 | Room storage of messages. The engine stays stateless; callers persist the streamed reply after `Completed`. |
| ViewModel and chat UI | 005 | Collects the `Flow`, folds `Delta` / `Completed` / `Failed` into `UiState`, renders streaming text and the in-conversation model picker. |
| iosMain `createChatHttpClient()` | future | `HttpClient(Darwin)` `actual`; the commonMain engine and SSE parse are unchanged. |
| Retry / backoff policy | future | A caller-level policy over `RateLimited` / `Overloaded`; the engine surfaces the typed error and does not retry. |
| `thinking` configuration | future | Adaptive-thinking request parameter and streaming of `thinking` blocks; currently omitted and skipped. |
