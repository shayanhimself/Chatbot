# UI layer

ViewModels and screen composables live in `:feature:*`. Reusable design-system
composables live in `:core:ui`.

## UiState

One immutable data class per screen, named `ScreenUiState`, exposed as a single
`val uiState: StateFlow<ScreenUiState>`.

```kotlin
data class ConversationUiState(
    val messages: List<MessageUiModel> = emptyList(),
    val streamingText: String? = null,        // in-flight assistant message
    val selectedModel: ClaudeModel = ClaudeModel.Default,
    val isSending: Boolean = false,
    val userMessages: List<UserMessage> = emptyList(),  // pending snackbars
)

data class UserMessage(val id: Long, @StringRes val textRes: Int)
```

- All fields immutable (`val`, immutable collections).
- Loading = boolean field. Errors = data in state (see events section), not exceptions.
- Derive, don't duplicate: `val ConversationUiState.canSend get() = !isSending && ...`
- Mutually exclusive states may be a sealed interface
  (`Loading` / `Ready` / `Error`) instead of a flag bag — choose per screen.
- Multiple `StateFlow` properties are acceptable only for genuinely unrelated data
  updating at very different rates. Default is one.
- `PagingData`, if ever used, is exposed as its own `Flow` — never inside `UiState`.

Map `:shared` domain models to UI models in the ViewModel when the screen needs a
different shape (formatted dates, resolved strings). Skip the mapping when the
domain model already fits.

## State production recipe

Flows from the data layer → `combine` → `stateIn`:

```kotlin
@HiltViewModel(assistedFactory = ConversationViewModel.Factory::class)
class ConversationViewModel @AssistedInject constructor(
    @Assisted private val navKey: ConversationKey,   // typed Nav 3 key = nav args
    private val conversationRepository: ConversationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(navKey: ConversationKey): ConversationViewModel
    }

    private val localInput = MutableStateFlow(LocalInputState())  // one-shot results

    val uiState: StateFlow<ConversationUiState> = combine(
        conversationRepository.getMessagesFlow(navKey.conversationId),
        settingsRepository.getSelectedModelFlow(),
        localInput,
    ) { messages, model, local ->
        ConversationUiState(
            messages = messages.map { it.toUiModel() },
            selectedModel = model,
            isSending = local.isSending,
            userMessages = local.userMessages,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ConversationUiState(),
    )
}
```

Rules:

- **Flow inputs:** `combine(...)` + `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`.
  `WhileSubscribed(5_000)` stops upstream collection 5s after the UI disappears and
  survives configuration changes.
- **One-shot inputs only** (screen has no upstream `Flow`s): the `MutableStateFlow`
  holds the whole UiState — private `MutableStateFlow(XUiState())` +
  `_uiState.update { it.copy(...) }`, exposed via `asStateFlow()`. No `combine`.
- **Mixed:** UiState comes out of `combine`, so one-shot results go into a private
  `MutableStateFlow` holding only the VM-local slice (`LocalInputState` in the
  snippet above), fed into `combine` as one more input.
- Never launch async work in `init` — the `stateIn` pipeline starts lazily on
  first collection.
- ViewModel work must be main-safe; long-running work belongs in the data layer.
  If forced to update from a background dispatcher, use `_uiState.update { }`
  (atomic).

## Events: no ViewModel → UI channel

`SharedFlow`/`Channel` events from ViewModel to UI are prohibited: an emission
while the UI isn't collecting (config change, background) is dropped silently.
State can't be lost — the UI resubscribes and reads the current value.

Pattern — event outcome becomes state, UI acknowledges consumption:

```kotlin
// ViewModel
fun send(text: String) = viewModelScope.launch {
    try { conversationRepository.send(navKey.conversationId, text) }
    catch (e: ChatException) {
        localInput.update {
            it.copy(userMessages = it.userMessages + UserMessage(newId(), e.toUserMessageRes()))
        }
    }
}

fun messageShown(id: Long) = localInput.update { s ->
    s.copy(userMessages = s.userMessages.filterNot { it.id == id })
}

// UI
uiState.userMessages.firstOrNull()?.let { msg ->
    LaunchedEffect(msg.id) {
        snackbarHostState.showSnackbar(context.getString(msg.textRes))
        viewModel.messageShown(msg.id)
    }
}
```

Navigation triggers follow the same rule: a state field (`isSignedIn`,
`savedAndDone`) that the UI observes and translates into a nav call.

`SharedFlow` remains legitimate *below* the UI layer (see data-layer reference).

## ViewModel rules

- Screen-level only — one ViewModel per navigation destination. Never for
  reusable components (chip rows, input bars): those take hoisted state + lambdas.
- Constructor injection via Hilt (`@HiltViewModel`). Dependencies are repositories
  or use cases — never DAOs, DataStore, or the engine directly.
- No `Context`, `Resources`, `Activity`, or any lifecycle type inside. No
  `AndroidViewModel`. String resolution happens in the composable (or pass
  `@StringRes` ids through UiState).
- Nav arguments arrive as the typed Nav 3 key via Hilt assisted injection (recipe
  above). Entry-side wiring: `navigation-3` skill, "Passing Arguments to
  ViewModels". `SavedStateHandle` only for minimal process-death restoration.
- Never pass a ViewModel instance down to child composables. Pass state values
  and event lambdas:

```kotlin
@Composable
fun ConversationScreen(viewModel: ConversationViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationScreen(          // stateless overload — previewable, testable
        uiState = uiState,
        onSend = viewModel::send,
        onModelSelected = viewModel::selectModel,
    )
}
```

- Always collect with `collectAsStateWithLifecycle()` — never `collectAsState()`
  or raw `collect` in a `LaunchedEffect`.

## State holder choice

| | ViewModel | Plain `@Stable` class |
|---|---|---|
| Scope | Screen / nav destination | Reusable UI component or app shell |
| Logic | Business logic (calls repos) | UI logic only (scroll, visibility, nav-suite choice) |
| Survives recreation | Yes | No — `remember` + delegate persistence to `rememberSaveable` |
| Android deps | Forbidden | Allowed (same lifecycle as UI) |

Plain state holder convention:

```kotlin
@Stable
class ChatInputState(private val focusManager: FocusManager) { /* UI logic */ }

@Composable
fun rememberChatInputState(
    focusManager: FocusManager = LocalFocusManager.current,
): ChatInputState = remember(focusManager) { ChatInputState(focusManager) }
```

Compounding rules:

- Business logic always applies before UI logic in the state pipeline.
- A plain holder never receives a ViewModel instance — pass only the fields and
  lambdas it needs.
- A screen-level holder never depends on another screen-level holder.
- Simple UI logic stays inline in the composable; extract a holder only when it
  hurts readability.

## StateFlow vs SharedFlow

`StateFlow` for all state — always has a value, replays latest, conflates.
`SharedFlow` in the UI layer: never (events rule above). Elsewhere: only for
genuine broadcast without a "current value" semantic — rare here.

## Testing

- Unit test ViewModels with fakes; assert on `uiState.value`.
- `stateIn(WhileSubscribed)` flows need a collector in tests before `value`
  updates — collect in a `backgroundScope` (see `testing-setup` skill).
- The stateless screen overload is the Compose-test and preview target.
