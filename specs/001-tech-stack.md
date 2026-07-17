# 001 — Tech Stack

This spec is the canonical record of the project's current technology choices. Any substitution or major version jump requires updating this spec first, then updating `gradle/libs.versions.toml`.

## Platform & build

| Decision | Choice                                                                                                                                                                                                                                                                                                                                                                                                             | Rationale                                                                                                                                                                                                                                                                                                                                                                                          |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Language | **Kotlin 2.4.10**                                                                                                                                                                                                                                                                                                                                                                                                  | Current stable line. Needs AGP 9.1+ and KSP ≥ 2.3.10                                                                                                                                                                                                                                                                                                                                               |
| Build | AGP 9.3.x, **Gradle 9.6.1+** (AGP 9.3 floor is 9.5), `gradle/libs.versions.toml` version catalog                                                                                                                                                                                                                                                                                                                   | AGP 9 stable line; catalogs standard. AGP 9 built-in Kotlin: omit `kotlin-android` plugin; set `compileOptions.targetCompatibility` once — `jvmTarget` inherits it (never set separately)                                                                                                                                                                                                          |
| Build performance | Configuration cache ON (`org.gradle.configuration-cache=true`); Isolated Projects OFF                                                                                                                                                                                                                                                                                                                              | Config cache is Gradle 9's preferred mode; Isolated Projects still experimental                                                                                                                                                                                                                                                                                                                    |
| Annotation processing | KSP2 **2.3.10** |  earlier KSP breaks Hilt/Room codegen on 2.4 default module names (google/ksp#2964, fixed in KSP 2.3.10). Only actively developed processor; required by Room/Hilt with Kotlin 2+. KSP2 releases are plain-versioned (the old `<kotlin>-<ksp>` lockstep scheme is KSP1-only), so the KSP version doesn't track the Kotlin version — check the release notes for the supported Kotlin range instead |
| Compose compiler | `org.jetbrains.kotlin.plugin.compose`, version = Kotlin version                                                                                                                                                                                                                                                                                                                                                    | Ships with Kotlin since 2.0. Strong skipping is default; do NOT set `StrongSkipping`/`IntrinsicRemember` flags (error in 2.4) — omit `composeCompiler {}` block                                                                                                                                                                                                                                    |
| JDK | 17 toolchain via `kotlin { jvmToolchain(17) }` + foojay resolver plugin in settings. Gradle daemon JVM pinned by criteria in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=17`)                                                                                                                                                                                                                         | AGP 9 default bytecode target is 17; JDK 17 min to run Gradle; foojay auto-provisions JDK. Daemon criteria select the daemon JVM independently of the launcher JVM, so `./gradlew` needs no per-machine `JAVA_HOME` setup and no `org.gradle.java.home` pinned to a local path. JDK 21 needed only to *run* Robolectric against SDK 36                                                             |
| compileSdk | 37                                                                                                                                                                                                                                                                                                                                                                                                                 | Forced by `androidx.core:core-ktx` 1.19.0 and the `androidx.lifecycle` 2.11.0 artifacts: both hard-require compileSdk ≥ 37 via AGP's AAR-metadata check, which fails the build and is not suppressible                                                                                                                                                                                             |
| targetSdk | 37                                                                                                                                                                                                                                                                                                                                                                                                                 | Matches compileSdk. Clears the Play Store mandate (min 36) for all submissions from 2026-08-31                                                                                                                                                                                                                                                                                                     |
| minSdk | 31                                                                                                                                                                                                                                                                                                                                                                                                                 | `SCHEDULE_EXACT_ALARM` exists since API 31 → single exact-alarm permission model, no legacy path; sensible modern floor. Lower only if reach data demands it                                                                                                                                                                                                                                       |

## UI

| Decision | Choice | Rationale |
|---|---|---|
| UI toolkit | Jetpack Compose, BOM 2026.06.00+ | Standard for new apps |
| Design system | Material 3 (stable APIs only) | M3 Expressive components still alpha (material3 1.5.0-alpha22) — adopt selectively once stable |
| Navigation | **Jetpack Navigation 3** — `androidx.navigation3:navigation3-runtime` + `navigation3-ui` 1.1.x | Stable since Nov 2025. Owned back stack (typed keys in `SnapshotStateList`), `entryProvider` DSL, `NavDisplay`. Scenes give native adaptive two-pane (conversation list + chat on tablets/foldables) via `androidx.compose.material3.adaptive:adaptive-navigation3`. **Navigation 2 / navigation-compose is prohibited.** Reference: `navigation-3` project skill, github.com/android/nav3-recipes |
| Insets | Edge-to-edge from day one | Android 16 requirement; `edge-to-edge` project skill |

## Project structure (KMP shared core, feature-modularized Android app)

| Decision | Choice | Rationale |
|---|---|---|
| Multiplatform | KMP from day one; `:shared` is the **only** KMP module (`androidTarget()` now, iOS targets later), holding all data + domain code. Uses `org.jetbrains.kotlin.multiplatform` + AGP's KMP library plugin (`com.android.kotlin.multiplatform.library`) | iOS addable without restructuring; one KMP module subsumes Google's `:core:*` data-side modules as packages |
| Platform split inside `:shared` | commonMain pure Kotlin; androidMain: Ktor OkHttp engine, Keystore/Tink crypto; iosMain (future): Foundation Models engine, Ktor Darwin engine | Ktor needs a platform engine; crypto is platform API |
| Android modularization | Feature-modularized — module map, dependency rules, and layer conventions owned by the **`architecture` project skill** | Google modularization guidance: enforced boundaries, parallel builds |

## Architecture & DI

| Decision | Choice | Rationale |
|---|---|---|
| Architecture | Official guidance: UI → optional Domain → Data; UDF; SSOT; ViewModel state holders. Data + domain live in `:shared` commonMain where possible | developer.android.com/topic/architecture |
| DI | **Hilt** | Still Google's official recommendation. Metro (ZacSweers) is the emerging compile-time/KMP alternative and appears in official nav3-recipes samples — revisit if project goes KMP; not now |
| Async | kotlinx.coroutines + Flow | Standard |
| Serialization | kotlinx.serialization | Nav 3 key serialization + API payloads. Pin exact version from GitHub releases at scaffold time (research found conflicting version reports) |

## Data

| Decision | Choice | Rationale |
|---|---|---|
| Chat history | Room 2.8.x | Structured/relational local data |
| Settings | DataStore Preferences 1.1.x | Small key-value data |
| Claude API key at rest | Android Keystore (hardware-backed master key) → Tink AEAD → ciphertext in DataStore | **EncryptedSharedPreferences is deprecated (do not use).** Plaintext key only in memory, never logged |
| Reminders & memories | Room tables | Local-only, no server; reminders re-registered from Room after reboot |

## Background work, scheduling & notifications

| Decision | Choice | Rationale |
|---|---|---|
| Reminder firing | AlarmManager `setExactAndAllowWhileIdle` gated by `SCHEDULE_EXACT_ALARM` special permission — request via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` settings intent, re-check on `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | Reminders are user-facing exact-time work; WorkManager alone drifts (Doze batching, 15-min periodic floor). User-scheduled reminders are Google's canonical sanctioned use case for this permission. **`USE_EXACT_ALARM` prohibited** — Play policy reserves it for alarm/calendar-core apps |
| Permission-denied fallback | WorkManager inexact scheduling | Reminder still fires, within an inexact window |
| Fire-time pipeline | Thin `BroadcastReceiver` (enqueue + return, milliseconds) → **expedited WorkManager job** (`NETWORK_CONNECTED` constraint, backoff) → Claude composes notification message → notification → Nav 3 deep link into conversation | `onReceive()` has a ~10s main-thread budget and the process is killable after return — no place for a network call. Offline/API failure → notification shows stored reminder text; never silently lost |
| Reboot persistence | `BOOT_COMPLETED` receiver re-registers alarms from Room | Alarms don't survive reboot |
| Notifications | `POST_NOTIFICATIONS` runtime permission (API 33+); androidx core-ktx `NotificationCompat` | |
| Background jobs | `androidx.work` (WorkManager) — pin version at scaffold | |
| Time in shared code | `kotlinx-datetime` in commonMain | Reminder domain models stay KMP-pure |

## AI engine

| Decision | Choice | Rationale |
|---|---|---|
| Engine (sole, BYOK) | **Ktor client + kotlinx.serialization** against `POST https://api.anthropic.com/v1/messages` with hand-rolled SSE streaming (`message_start` → `content_block_delta`/`text_delta` → `message_stop`) | KMP-insurance: official Anthropic Java SDK is JVM-only; Ktor layer ports to iOS as-is. Small surface — one endpoint, SSE parse. Headers: `x-api-key`, `anthropic-version: 2023-06-01`, `content-type: application/json`. Default model `claude-sonnet-5`; picker for `claude-haiku-4-5` / `claude-opus-4-8`. User's own key only — never a project-owned key in the app |
| Tool use | Native Anthropic tool use: `tools` param, `tool_use` / `tool_result` content blocks. SSE parser must additionally handle `input_json_delta` deltas and `stop_reason: "tool_use"`. Agentic loop (model emits tool call → app executes locally → resume turn with result) lives in `:shared` commonMain; executors injected via interfaces | Reminders and memory are created by the model mid-conversation |
| Engine abstraction | Common `ChatEngine` interface | Retained despite single engine: test fakes, and a future iOS on-device engine (Foundation Models) slots in without touching callers |

## Testing

| Decision | Choice | Rationale |
|---|---|---|
| Unit tests | JUnit4 | Android default; JUnit5 still third-party-plugin territory |
| Android framework in JVM tests | **Robolectric 4.16.1**; emulated SDK pinned to 36 via `robolectric.properties` | Needs JDK 21. Not 37: 4.16.1 tops out at SDK 36 (37 only in 4.17-beta-1). Emulated SDK is independent of compileSdk; the `sdk=36` pin is load-bearing — Robolectric otherwise defaults to targetSdk 37, which 4.16.1 can't run. Revisit at 4.17 stable |
| UI tests | **Compose testing APIs** (v2), `createComposeRule` + semantics matchers — both local (Robolectric) and instrumented | The tool for Compose UIs. Import the rule from `androidx.compose.ui.test.junit4.v2` — the same name in the parent `junit4` package is the deprecated v1 rule. v2 runs a `StandardTestDispatcher`, so composition-launched coroutines queue instead of running eagerly. |
| Screenshot tests | Compose Preview Screenshot Testing (`@PreviewTest`) | Official tool; still preview-channel — fall back to Roborazzi if it blocks us |
| Acceptance / E2E | Journey XML files evaluated by agent via `android` CLI against emulator | Spec-level acceptance: each feature spec ships journeys for its acceptance criteria. Complements, never replaces, coded tests |
| Method | TDD (red → green → refactor) mandatory | Superpowers `test-driven-development` skill enforces |

## Development tooling (part of the stack)

| Decision | Choice | Rationale |
|---|---|---|
| Dev CLI | **`android` CLI** (official Google Android CLI) | Project creation (`android create`), emulator management, running apps, device interaction/screenshots, docs lookup (`android docs`), journey evaluation, skills management. Install: `curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh \| bash` |
| Agent skills | **Official Android skills** (github.com/android/skills) vendored in `.claude/skills/`: `navigation-3`, `adaptive`, `testing-setup`, `edge-to-edge`, `android-cli` | AI-optimized instructions from Google (open Agent Skills standard). Update: `android update` + re-run `android skills add --project=.` per skill, then review `git diff .claude/skills/` before commit — never merge upstream skill changes unreviewed |
| Workflow | Superpowers plugin (project scope) | Spec→plan→TDD pipeline — `docs/superpowers-guide.md` |
| Code formatting | **Spotless** 8.8.0, **ktlint** 1.8.0 | Formatting is enforced at the same gate as tests. Style lives in root `.editorconfig` so the IDE and the build agree. |

## Open items

1. M3 Expressive adoption — re-evaluate when components hit stable (material3 1.5.0 line; 1.4.0 is the current stable)


