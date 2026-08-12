# 006 — Onboarding

First launch asks for the user's Anthropic API key, validates it against the API, and stores it encrypted. Until a key is stored the app has nothing to show and nothing it can do, so onboarding is also the gate: it is the only reachable destination while the key is missing, and the rest of the app becomes reachable the moment one is stored.

This is the last spec in M1. It replaces the debug-only dev key 005 stands on, which is what makes a release build assemble.

## Scope

In: the key entry screen and its states, key validation against the API, encrypted storage, the navigation gate and the navigator it lands in, and the removal of the dev-key stub.

Out, with owners: viewing, changing, and removing a stored key (007); everything the key is subsequently used for (already built in 003 through 005).

## Data layer

Two additions to `:shared`, both in commonMain, both following the platform-seam pattern the database already uses: a platform builder in androidMain takes the `Context`, `:app` calls it from a Hilt module, and a commonMain factory finishes assembly.

### Key storage

```
ApiKeyRepository
  fun hasKeyFlow(): Flow<Boolean>
  suspend fun apiKey(): String?
  suspend fun save(key: String)
  suspend fun clear()
```

`hasKeyFlow` reports whether a ciphertext entry exists and never decrypts, so the gate that collects it for the app's whole lifetime costs no crypto. `apiKey` returns null rather than throwing, because a caller racing a `clear` is a legitimate state rather than a programming error.

`clear` has no caller in this spec. It is declared here because removing a key is the inverse of storing one and belongs to whoever owns the storage contract, not to the settings screen that eventually calls it.

Ciphertext lives in DataStore Preferences. `KeyCipher` is an interface in commonMain; its Android implementation is Tink AEAD keyed by a hardware-backed Android Keystore master key. Both the DataStore instance and the cipher are built in androidMain from a `Context` and handed to the commonMain factory, so commonMain stays free of platform imports and an iOS Keychain implementation later replaces one class.

The store is excluded from cloud backup and from device transfer. The master key never leaves this device's Keystore, so a copy restored elsewhere cannot be decrypted.

### Key validation

```
ApiKeyValidator
  suspend fun validate(key: String): KeyValidationResult

KeyValidationResult
  Valid
  Failed(error: ChatError)
```

`GET https://api.anthropic.com/v1/models?limit=1`, with the `x-api-key` and `anthropic-version` headers every request already sends. The endpoint authenticates, consumes no tokens, runs no inference, and streams nothing, so validation costs the user nothing and needs none of the SSE machinery. `ChatEngine` is untouched and stays stream-only; the validator shares the existing Ktor client.

Failures are reported as `ChatError` rather than a second error vocabulary, so the status mapping stays the only one in the app. That mapping, and the version header and key header constants beside it, are file-private to the engine today and move to an internal file both callers share.

**Only 401 and 403 mean the key itself is wrong.** Every other outcome (network failure, timeout, rate limiting, server error) is a retryable failure that leaves the key's validity unknown, and the screen says so. A 429 in particular may be throttling applied before authentication, and telling a user their working key is invalid is the one mistake this screen cannot afford.

## Screen

`:feature:onboarding` gains Hilt, KSP, the lifecycle-compose artifacts, and the screenshot plugin, matching what 005 gave `:feature:conversation`. It ships the `OnboardingRoute` / `OnboardingScreen` stateful-stateless pair for the same reasons.

```
sealed interface OnboardingUiState {
  data object Idle : OnboardingUiState
  data object Validating : OnboardingUiState
  data class Failed(val error: ChatError) : OnboardingUiState
}
```

The design draws six frames; they resolve to these three states plus what the screen already knows. The rejected-key and offline frames are one `Failed` path, with copy and a trailing glyph chosen from the error rather than two code paths.

Nothing describing the key text belongs in this state, because the ViewModel does not see the text until it is submitted and so cannot produce anything derived from it. The screen can: submission is enabled at the length threshold the design names, read from the field it holds. The empty and awaiting-submission frames differ by that derivation, and the masked and revealed frames by the reveal toggle. Neither reaches the ViewModel.

Enabled and loading stay separate rather than collapsing into one disabled flag, because the catalog button treats them differently and the design relies on the difference: the empty frame is dimmed and the validating frame keeps full colour under a spinner.

There is no `sk-ant-` prefix check behind the threshold. A vendor's key format is not ours to hardcode, and the network round trip is the authoritative answer regardless.

The ViewModel takes two events. `onSubmit` carries the key. `onKeyEdited` carries nothing and returns a `Failed` state to `Idle`, so a rejected field stops being red as soon as the user starts correcting it instead of staying red until the next attempt.

The field is a password keyboard with autocorrect and capitalization off, submitting on the IME action. All of these are wrong by default for a credential and none of them are visible in a mockup.

### Handling the key

**The key is a parameter, never a field and never saved state.**

The screen holds the text in a plain `remember { mutableStateOf("") }` and passes the finished string to the ViewModel, which validates it, hands it to the repository, and retains nothing. This deliberately diverges from 005, whose composer text is `rememberSaveable`.

The reason is that saved state is not ours. `rememberSaveable` and `SavedStateHandle` serialize their value and hand it to the system, which keeps a copy in memory outside this process and survives this process dying. Nothing there can be scoped or cleared by the app. A plain `remember` keeps the key in our own heap for the life of the composition and it dies with us.

`rememberSaveable` and `rememberTextFieldState` are both prohibited here, which is worth naming because either is the obvious call to reach for and both would undo the rule without any visible sign.

The field state and the reveal flag are parameters of the screen defaulting to that plain `remember`, rather than being private to it. Previews and screenshot tests supply their own, which is the only way one stateless screen can render both an empty field and a filled one, and a masked key and a revealed one.

Keeping it out of `UiState` is a smaller matter of the same kind: `StateFlow.value` holds a strong reference for the ViewModel's whole life, and a parameter is reachable only for the length of a call. It costs nothing, since the ViewModel has no reason to retain the key, but it is hygiene rather than a boundary.

Neither rule defends against another app. The sandbox already does that: apps run under separate UIDs and cannot read each other's memory. These rules narrow the window during which a key is exposed to someone with a debuggable build, root, or physical access to an unlocked device. A `String` cannot be zeroed, so the window is narrowed, never closed. The boundary that actually holds is the encryption at rest above.

The visible consequence is that the field is empty after process death. That is correct and intended.

### Copy and layout

Layout, spacing, color, and the composition of each state come from the mockup, not from this spec.

| Design file | Frames |
|---|---|
| `Screen-Onboarding.dc.html` | 1a–1f |

Every element maps to an existing `:core:ui` component, including the button's loading state. `:core:ui` gains the new `Glyphs` constants the screen names, and two parameters on `DsTextField`: an IME action handler, so the keyboard can submit, and a content description for the trailing slot, because the reveal toggle is interactive and an unlabelled tappable icon is unreachable by TalkBack.

**Copy is read from that file with the `pull-design` skill when the implementation plan is written.** This spec names states, never their wording.

Two divergences from what the file draws:

- **The console footer is tappable**, launching `ACTION_VIEW` at the Anthropic console. It is drawn as inert text, which leaves a user without a key stranded on the only screen they can reach.
- **The content column has a maximum width and centers.** The file draws a phone only, and a text field stretched across a tablet is not what it implies.

## Navigation

```
@Serializable data object OnboardingKey : NavKey
```

The gate is the third rule that rewrites the back stack, joining the deep-link seed and the replace-or-stack rule for opening a chat. At three rules the operations stop being incidental, so `:app` gains a `ChatbotNavigator` that owns them.

```
ChatbotNavigator(backStack, hasApiKeyAtStart)
  fun openConversation(id: Long)
  fun openNewChat()
  fun back()
  fun openConversationFromDeepLink(id: Long)
  fun resetForApiKeyState(hasApiKey: Boolean)
```

`rememberNavBackStack` still creates the stack, since that is what save and restore hang from, and the navigator is remembered around it. It is the only thing that mutates it; `ChatbotNavDisplay` reads it to render.

This absorbs navigation logic that is currently inline. Opening a conversation replaces the top entry when a chat is already open and pushes otherwise, so a different conversation gets a different ViewModel; that rule becomes one named function instead of a conditional with a comment. Going back has two call sites today, written twice as the same expression, and becomes one method.

The two ways to reach a conversation differ in what they may assume. `openConversation` runs from the list, so a list-rooted stack at most one chat deep is a given and adjusting the top entry is enough. `openConversationFromDeepLink` serves an intent from outside the app, arriving against a stack it knows nothing about, so it discards what is there and builds the two entries a notification should land on. Both produce the same stack from any arrangement that exists today; they part company as soon as a third kind of destination does.

None of this changes the feature contract. Features still receive lambdas and know nothing about navigation, and the navigator is internal to `:app`.

### Gate

`:app` gains a `MainViewModel` producing `Undecided` until `hasKeyFlow` first emits, then `Decided(hasApiKey)`. `MainActivity` owns it, because a splash screen's keep-on-screen condition has to be installed before content is set, and holding the splash over `Undecided` is what stops a blank frame on a cold start. No mockup draws a splash; this is an addition.

`MainActivity` composes `ChatbotApp` only once the state is decided, so the back stack is seeded correctly on first composition rather than seeded wrong and rewritten. `ChatbotApp` takes the resolved flag as a parameter and stays stateless, matching the split 005 established.

The flag decides both where a launch lands and what a change rewrites the stack to, and the two are not the same stack. A launch without a key starts on `[OnboardingKey]`; a launch with one starts on the conversation list, which is where every existing journey expects to open.

`resetForApiKeyState` picks between the two stacks, and rewrites only when the flag differs from the last value it applied, which it is seeded with at construction. That comparison is not an edge case: the effect driving it runs on entering composition, against a navigator built in the same composition from the same value, so equality is the ordinary case on every launch and a genuine change is the rare one. Seeding is already done by the stack's initial value, and the guard is what stops the effect doing it a second time. Because the navigator is remembered rather than saved, activity recreation and process death rebuild it against the flag as it stands at that moment, so a restore reports no transition and leaves the restored stack alone. Without it, a restore would discard the user's position and drop them into a new chat.

**Nothing tells the gate that onboarding finished.** The ViewModel saves the key, `hasKeyFlow` emits, the gate rewrites. `OnboardingRoute` takes no completion callback and knows nothing about navigation, which is what the rule that features never navigate asks for.

The onboarding entry carries no list-detail metadata, so it renders full-screen on every window size.

### Divergence from the conditional-navigation recipe

The vendored `navigation-3` skill carries a conditional-navigation recipe whose navigator intercepts every navigation call, against destinations that each declare whether they require a login. `ChatbotNavigator` is a vocabulary of operations, not that policy layer, and a review comparing this spec against that skill should expect the difference.

The recipe solves a partial gate: most of the app is open, one destination is restricted, and login is a detour the user is redirected into and then returned from. This app is all-or-nothing, since every screen either lists conversations or talks to Claude. Every destination would declare the same value, the field carrying the interrupted destination would never hold anything but the app's normal start, and the check would run on every navigation call in the app to evaluate a condition that is false only at first launch and after a key removal. It would also oblige every future feature that adds a destination to route through it.

Deriving the stack from one flag needs none of that.

The recipe's one genuine advantage is not adopted: a deep link arriving while the key is missing is dropped rather than resumed after onboarding. That requires a reminder to fire against a key the user has since removed, and is accepted.

## Dev key removal

`DevApiKeyModule` in `:app`'s debug source set, the `DEV_API_KEY` build config field, and the `devApiKey` provider in `:app`'s build script are deleted. `ApiKeyProvider` is bound for all build types over `ApiKeyRepository`, and release builds assemble from here on.

003's gated integration test reads the environment directly and is unaffected.

## Testing

TDD throughout, fakes not mocks, per the architecture skill.

- **Repository**, in `androidHostTest` against a fake `KeyCipher`: a save and read round trip, `hasKeyFlow` across store and clear, and a read with nothing stored returning null. The repository's own behaviour is storage and flow logic, and none of it needs real encryption to be exercised.
- **Cipher**, as an instrumented test on a device. The Android Keystore is a platform service with no JVM implementation, so the one test that proves ciphertext round-trips through a hardware-backed key cannot be a host test. This is what the `KeyCipher` interface buys: the seam that keeps the platform out of every other test is also the seam that isolates the one test that needs it. `:shared` has no device test source set yet and gains one, configured the way its host tests already are.
- **Validator**, in commonTest against Ktor's `MockEngine`. The rule that only 401 and 403 condemn the key carries the risk here and gets a case per status: 200 valid, 401 and 403 invalid, and 429, 500, 529 and a connection failure all retryable without being invalid.
- **ViewModel** against a `FakeApiKeyRepository` and a fake validator, both added to `:shared:testing`: the validating state, each failure class, an edit clearing a failure, and a success reaching the repository. The submit threshold is not here, because it is not the ViewModel's to know.
- **Navigator**, as plain JUnit against a real `NavBackStack`, no Compose involved: the stack each gate value produces, a rewrite on transition, a repeated value leaving the stack untouched, a navigator constructed against the current value treating it as no transition, the replace-or-push rule in both directions, and `openConversationFromDeepLink` flattening a stack that `openConversation` would have appended to. That replace-or-push rule ships untested today and is covered here because the code moves.
- **Compose UI tests** under Robolectric with the v2 rule, driving `OnboardingScreen` by typing rather than by constructing state: the submit threshold at its boundary in both directions, the reveal toggle, the field disabled while validating, error rendering, and retry.
- **Screenshots.** `@PreviewTest` previews in dark and light, one per frame the design file carries. The sample key is an obviously fake value in a named constant, per the fixture rule.

### Journeys

| Journey | Proves | Requires |
|---|---|---|
| `onboard-invalid-key` | A rejected key shows the error and stays on the screen | |
| `onboard-offline` | Validation with no network shows the offline failure and remains retryable | |
| `onboard-with-key` | A valid key is stored, the gate opens, and a chat is usable | A live key from `ANTHROPIC_API_KEY`, the source 003's integration test already reads |

## Deferred to later specs

| Piece | Owner | What it adds |
|---|---|---|
| Viewing, changing and removing the stored key | 007 | The settings screen calls `clear`, and the gate closes again on its own |
