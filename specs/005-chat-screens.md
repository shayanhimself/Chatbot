# 005: Chat Screens

The app's first screens: a chat list and a chat screen, wired together by a Nav 3 back stack that adapts to a two-pane layout on wide windows. This is where 003's stream and 004's storage become something a person can use, and it is the last spec before the M1 sideload checkpoint.

Everything renders through `:core:ui` (002). All state comes from `ChatRepository` (004); this spec adds no persistence of its own beyond two additions to that contract. Chat runs on a debug-only dev key until 006 replaces it with onboarding.

## Scope

In: the chat list (populated, empty, loading), the chat screen (new-chat empty state, thinking, streaming, inline error with retry, model picker), chat delete, the Nav 3 back stack with list-detail two-pane, deep-link readiness, and the mechanism that lets feature modules consume `:shared`'s test fakes.

Out, with owners: tool-call chips (008 onward), search and the unread indicator (M4), suggested prompt chips and the model picker's blurbs (M4), settings and its entry point (007). Deferrals are listed in full at the bottom.

## Repository additions

Two additions to 004's contract, which that spec defers to this one.

```
fun getChatFlow(chatId: Long): Flow<Chat?>

data class Chat(id, title, model, snippet, createdAt, updatedAt)
```

The chat screen renders the chat's title, and 004 exposes chats only as a list. `getChatFlow` closes that gap; it emits null for a chat that does not exist yet or has been deleted.

`snippet: String?` is the list's second line. It comes from a correlated subquery on the query that already backs the list, so there is no new table, no denormalized column, and no schema version bump:

```sql
SELECT c.*, (SELECT m.content FROM messages m
             WHERE m.chatId = c.id AND m.status = 'Complete'
             ORDER BY m.id DESC LIMIT 1) AS snippet
FROM chats c ORDER BY c.updatedAt DESC
```

Nothing is stored: `snippet` is projected, not persisted, so there is no write path and no way for it to drift from the messages it summarizes. The DAO returns an internal projection that the repository maps to `Chat`. Room re-runs the flow whenever a table the query references is invalidated, and subqueries count toward that, so it observes `messages` as well as `chats`. That means twice per turn, since 004 writes the user row and then the assistant row and never writes per token, and only while something collects it. On a wide window that includes mid-chat, because the list sits beside the chat.

Content is a JSON column, so the existing `List<ContentBlock>` converter decodes the projected value and the repository flattens its `Text` blocks. The `status = 'Complete'` filter matches the history query for the same reason: a failed turn's partial text is not what the chat is about, and after a failed first turn the user's own message is the honest summary. That literal is coupled to an enum constant name with nothing in the compiler to check it, so it gets the same kind of test 004 wrote for the history query.

## Modules

### `:shared:testing`

004 left a `FakeChatRepository` in `:shared` commonTest that no other Gradle project can see, because test source sets are not published to consumers and Kotlin has no KMP equivalent of test fixtures. This spec is the first that needs it, so it owns the fix.

```
shared/
├── build.gradle.kts            :shared, unchanged
├── src/
│   ├── commonTest/             fakes removed, tests stay
│   └── …
└── testing/
    ├── build.gradle.kts        :shared:testing, new
    └── src/commonMain/         FakeChatRepository, FakeScriptedClaudeEngine,
                                FakeManualClaudeEngine, FakeClock
```

A KMP module with `androidTarget()` only, mirroring `:shared`. The fakes live in `commonMain`, a production source set, which is what makes them visible to other projects. Every consumer takes it as `testImplementation`, so nothing reaches the APK.

The dependency graph is acyclic because Gradle resolves classpaths per source set, not per project:

```
        :shared / commonMain                    (production code)
                 ▲
                 │ implementation
        :shared:testing / commonMain            (fakes)
                 ▲
                 │ testImplementation
        ┌────────┴─────────┐
:shared/commonTest   :feature:chat/test
```

Every edge points one way. The arrangement only looks circular if a project is treated as a single node; `:shared`'s test compilation and its main compilation are separate units with separate classpaths, and production code never depends on its own tests.

### Feature and app

`:feature:chat` gains Hilt and KSP, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `hilt-lifecycle-viewmodel-compose`, and the Compose screenshot plugin with the same `enableScreenshotTest` experimental property `:core:ui` sets. It takes **no Navigation 3 dependency**: the nav graph belongs to `:app`, so the feature exports composables and ViewModels and knows nothing about keys.

`hilt-lifecycle-viewmodel-compose` rather than `hilt-navigation-compose`: the latter declares `navigation-compose` as a compile dependency, which would put Navigation 2 on the classpath.

Each feature screen ships as a pair. `ChatDetailRoute` is stateful, resolves its ViewModel with `hiltViewModel()`, and is what `:app` calls. `ChatDetailScreen` is stateless, takes `uiState` plus event lambdas, and is what previews, screenshot tests, and Compose UI tests drive. The same split applies to the list.

`:app` gains `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3`, `adaptive-navigation3`, the kotlin-serialization plugin, and `buildConfig = true`.

## Navigation

```
@Serializable data object ChatListKey : NavKey
@Serializable data class ChatDetailKey(val chatId: Long? = null) : NavKey
```

A `NavDisplay` over `rememberNavBackStack(ChatListKey)`, which persists across process death because the keys are serializable. `rememberListDetailSceneStrategy()` supplies the adaptive layout, and the two entries carry `listPane()` and `detailPane()` metadata.

On a wide window both panes show and `ChatDetailScreen` receives a null `onBack`, which is what hides its back arrow; on a narrow window it is single-pane with the arrow shown. The back affordance is a parameter rather than an internal decision so the screen stays stateless and both states are screenshot-testable.

**A new chat keeps `ChatDetailKey(null)`.** The first `send` creates a chat, but the key is not rewritten: replacing the top of the back stack recreates the entry and therefore the ViewModel, mid-stream, resetting scroll. The ViewModel owns the live id instead.

It is seeded through an assisted-inject factory (`@HiltViewModel(assistedFactory = …)` with `hiltViewModel(creationCallback = …)`), since a Nav 3 key is not injected into `SavedStateHandle` the way a Nav 2 route argument was. After `send` returns a new id the ViewModel writes it to `SavedStateHandle`, which is what survives process death: the restored back stack still says `ChatDetailKey(null)`, and without that write the user would return to an empty new chat instead of the chat they were in. The saved value wins over the assisted parameter when present.

**The list's selected item reads that id, not the key.** Mockup 3k highlights the open chat in the list pane, so on a wide window something outside the chat screen does need the live id. `ChatDetailRoute` reports it upward through an `onChatIdChanged` lambda driven by its `UiState`, and `:app` hands the reported value to the list as `selectedChatId`. The key stays stable while the list still gets the truth, and `:app` needs no saved state of its own: the ViewModel's `SavedStateHandle` is the durable store, and the lambda fires again on the first composition after a restore.

Switching to a *different* chat is the opposite case and does rewrite the key, which is correct: a different chat should get a different ViewModel. Only the null-to-new-id transition is exempt, because there the ViewModel being replaced is the one that owns the in-flight turn.

**Deep-link readiness.** `MainActivity` reads `EXTRA_CHAT_ID` from its launch intent and from `onNewIntent`, and seeds the back stack as `[ChatListKey, ChatDetailKey(id)]`. Nothing sends that intent yet; 010's reminder notification is what eventually does, and this spec exists to make that a one-line addition rather than a nav rework.

## State

### Chat

Room and the in-memory turn are folded into one list so the `LazyColumn` has a single source and no composable has to reconcile two:

```
sealed interface ChatDetailItem {
  data class Persisted(val message: Message) : ChatDetailItem
  data object Thinking : ChatDetailItem
  data class Streaming(val text: String) : ChatDetailItem
  data class Error(val error: ApiError) : ChatDetailItem
}

data class ChatDetailUiState(
  val chatId: Long?,
  val title: String?,
  val model: ClaudeModel,
  val items: List<ChatDetailItem>,
  val isStreaming: Boolean,
  val deleteDialogVisible: Boolean,
  val deleted: Boolean,
)
```

`chatId` is null until the first send creates the chat, and is what hides the overflow menu until then. `deleted` turns true once a confirmed delete finishes, and is the state `:app` reads to pop the back stack or reset the detail pane, since a ViewModel raises no events.

A null `title` means a chat with no first message yet, which the screen renders as the new-chat copy. `getChatFlow` emitting null resolves to that same state: the ViewModel clears its id and falls back to a new chat. The delete flow does not need this, since it pops or resets the pane itself. The deep link does: `ChatDetailKey` can be seeded from an intent extra, so its id arrives from outside the app with no guarantee the row still exists, and 004's `send` rejects a missing chat with `IllegalArgumentException`. 010's notification for a chat deleted between scheduling and firing is that case in production.

Items are built by one rule:

```
items = messages.filter { it.text.isNotBlank() }.map(::Persisted) + trailing

trailing = when (turn) {
  Idle              -> nothing
  Streaming("")     -> Thinking      ┐ only when the last persisted
  Streaming(text)   -> Streaming     ┘ message is a user message
  Failed(error)     -> Error
}
```

The guard on the live items exists because 004 guarantees the assistant row is inserted **before** the turn returns to `Idle`, which means there is a window where Room has already emitted the finished message while the turn still reads `Streaming`. Rendering both would double the bubble for a frame. Keying off the trailing role instead of a timestamp makes it deterministic: once an assistant message is the last persisted item, any live text is stale by definition. `Failed` is exempt because 004 writes a `Failed` assistant row and keeps the turn entry until the next `send`, `retry`, or `delete`, so the error must render after that row rather than instead of it.

Blank messages are filtered out for the same reason 004 drops them on the way into a request: a turn that produced no text still stores its row, and an empty bubble is noise. Cancelled items keep their partial text and render as ordinary assistant messages, which is what the user already saw.

Events are ViewModel methods, never an event channel: `onSend`, `onCancel`, `onRetry`, `onModelSelected`, and the three delete-dialog methods. **Composer text is screen state**, held in a saveable `TextFieldState` rather than in `UiState`; it survives rotation on its own and the ViewModel only ever sees the finished string.

### List

```
data class ChatListUiState(
  val isLoading: Boolean,
  val chats: List<Chat>,
)
```

`isLoading` is true until the Room flow's first emission and drives the skeleton items. Relative timestamps ("2h", "1d", "1w") are formatted in the feature from an injected `Clock`, with the unit strings in the feature's resources.

Both states are produced with the architecture skill's `stateIn` recipe. The chat ViewModel combines `getChatFlow`, `getMessagesFlow`, and `getTurnFlow` through a `flatMapLatest` on the id, so a null id produces the new-chat state without any flow being subscribed to a chat that does not exist.

## Screens

Chat surfaces are built in :feature:chat rather than in `:core:ui`, per 002: their props are domain-shaped and they have exactly one consumer. `:core:ui` gains only the new `Glyphs` constants the screens reference.

Layout, spacing, color, and the composition of each state come from the mockups, not from this spec. Two design files, pulled with the `pull-design` skill:

| Design file | Frames |
|---|---|
| `Screen-Chat List.dc.html` | 2a–2e |
| `Screen-Chat.dc.html` | 3a–3k, the last being the tablet two-pane layout |

Composables to build in `:feature:chat`:

- **List:** `ChatListItem`, empty state, loading skeleton, new-chat button
- **Chat:** `MessageBubble` (user and assistant, with streaming caret), `ThinkingIndicator`, `ErrorItem`, `Composer`, `ModelPickerChip` and its menu, new-chat empty state, overflow menu, delete dialog

Where this spec diverges from what those files draw:

- **The list top bar's search and settings actions are not built.** Search is M4; settings has no destination until 007.
- **Suggested prompt chips (3a), picker blurbs (3e), and the unread dot are not built.** All M4.
- **Tool chips (3c, 3d) are not built.** 008 onward; everything else in those two frames is ordinary chat.
- **The selected-item treatment (3k) applies on a wide window only**, since a narrow one never shows the list beside a chat.

The overflow button opens a dropdown whose only item in this spec is "Delete chat"; tapping it opens a confirm dialog, and confirming calls `delete(id)` and pops to the list, or on a wide window returns the detail pane to the new-chat state. The button is hidden entirely while the chat id is null, since a chat with no first message has nothing to delete.

The composer's trailing button sends when there is non-blank text and no live turn, and becomes a stop button that calls `cancel` while streaming. The model picker chip opens a menu of the three models, checkmarking the current one, and is disabled during a turn for the same reason the composer is. Model names come from `ClaudeModel.displayName`; the per-model blurbs in mockup 3e are deferred to M4.

The message list follows the tail while tokens arrive, stops following once the user scrolls up, and follows again from the next message the user sends.

**Offline has no special treatment.** Losing connectivity is `ApiError.Network` and renders as the ordinary inline error item with Retry (3h), the same as any other failure.

**The error item does not survive process death.** It is drawn from the turn, which 004 holds in memory, so a relaunch shows the chat without it. Sending again is the way forward.

## Dev key

`:app` enables `buildConfig` and exposes a debug-only `DEV_API_KEY`, read at configuration time from the `ANTHROPIC_API_KEY` environment variable or the `anthropic.api.key` property in `local.properties`. Those are the same two sources 003's gated integration test already reads, so a machine set up to run that test needs no further setup. `DevApiKeyProvider` lives in `:app`'s `debug` source set and is bound to `ApiKeyProvider` there.

Release builds get no `ApiKeyProvider` binding and therefore do not assemble until 006 contributes the real one. That is deliberate: the alternative is a release-only stub that would have to be remembered and removed. The M1 sideload checkpoint is a debug build.

## Testing

TDD throughout, fakes not mocks, per the architecture skill.

- **ViewModels** against `:shared:testing`'s `FakeChatRepository`, asserting on `StateFlow.value`. The item-folding rule carries the most risk and gets the most cases: thinking before the first token, streaming text, the stale-live-item window after a completed turn, the error item surviving alongside its `Failed` assistant row, blank messages filtered, and cancelled partial text rendered.
- **Repository additions** in `:shared`'s `androidHostTest`, where a real database is available: the snippet subquery picking the last `Complete` message, excluding a non-`Complete` one, and `getChatFlow` emitting null after delete.
- **Compose UI tests** under Robolectric with the v2 rule, driving the stateless screens: composer enablement, send and stop, the overflow menu and delete dialog, the model picker, and retry.
- **Screenshots.** Every public composable ships `@PreviewTest` previews in both dark and light, one per frame the two design files carry, minus the frames listed above as not built.

### Journeys

New XML files in `journeys/`, covering the M1 exit gate:

| Journey | Proves |
|---|---|
| `send-first-message` | New chat, send, reply streams in, chat appears in the list with its snippet |

## Deferred to later specs

| Piece | Owner | What it adds |
|---|---|---|
| Search and no-results states (2c, 2d) | M4 | A `LIKE` query over title and snippet, plus a search destination |
| Unread indicator | M4 | Needs a `lastReadAt` column, so a schema bump and migration |
| Suggested prompt chips (3a) | M4 | Copy-only, but the reminder-flavored prompt only makes sense once 010 exists |
| Model picker blurbs (3e) | M4 | Localizable copy, so feature string resources keyed by `ClaudeModel`, alongside the name already on the model |
| Settings entry point | 007 | The list top bar's settings action, once there is a screen behind it |
| Tool-call chips (3c, 3d) | 008, 009, 010 | `ToolUse` and `ToolResult` blocks become items; the chip is their rendering |
| Real `ApiKeyProvider` | 006 | Replaces the debug dev-key stub and makes release builds assemble |
