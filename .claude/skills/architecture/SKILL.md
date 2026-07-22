---
name: architecture
description: This project's app architecture and conventions. Use when building or
  modifying any feature — adding a screen, ViewModel, UiState, repository, data source,
  use case, Room entity/DAO, or a new module; when deciding which module or layer code
  belongs in; or when wiring Hilt.
metadata:
  keywords:
  - architecture
  - feature
  - module
  - layer
  - ViewModel
  - UiState
  - repository
  - data source
  - UseCase
  - StateFlow
  - Hilt
  - KMP
  - Room
  - Database
---

Conventions here are binding. They implement Google's architecture guidance.

## Layering

Two mandatory layers plus one optional, dependencies point one way only:

```
UI layer (:feature:*)  →  [domain layer (optional)]  →  data layer (:shared)
```

- **Repositories are the sole entry to the data layer.** ViewModels and use cases
  never touch a DAO, DataStore, or the Ktor client directly.
- **Single source of truth (SSOT):** Room is the SSOT for all persisted data
  (conversations, reminders, memories). The app is local-only — no remote sync,
  no conflict resolution. One exception: the in-flight streaming assistant message
  is in-memory UI state until `message_stop`, then persisted once (never per-token
  DB writes).
- **Unidirectional data flow (UDF):** state flows down (immutable), events flow up
  (method calls). Applies inside every layer and between them.
- **Data exposed by any layer is immutable.** Mutation happens only inside the owner.

## Modules

`:shared` is the only KMP module. It replaces Google's `:core:data` /
`:core:database` / `:core:network` / `:core:common` — organized as packages
inside, not sub-modules.

| Module | Contents |
|---|---|
| `:shared` (KMP) | **commonMain:** `ChatEngine`, Ktor client + SSE parsing, agentic tool-use loop + tool definitions, repositories, Room (entities, DAOs, database), domain models and logic. **androidMain:** Ktor OkHttp engine, Keystore/Tink crypto `actual`s |
| `:core:ui` | Theme, design system, shared composables, generic strings (OK/Cancel/retry) |
| `:feature:onboarding` | First-launch flow: API key entry + validation, then straight into chat |
| `:feature:conversation` | Chat screen, ChatViewModel, streaming UI, in-conversation model picker |
| `:feature:settings` | API key management (view/update/remove), memory management UI, permission flows |
| `:feature:reminders` | Reminders list UI |
| `:app` | MainActivity, Nav 3 graph, Hilt root, tool executor implementations, BroadcastReceivers, WorkManager workers |

Dependency rules (hard):

- `:feature:*` depends on `:shared` + `:core:ui` only. **Never feature → feature**;
  cross-feature navigation goes through the `:app` nav graph.
- `:core:ui` depends on nothing in the project (not even `:shared`).
- `:app` depends on everything; it is the only module that sees all features.
- `:shared` depends on nothing in the project.
- **commonMain stays pure** — no `android.*` import ever. Platform access via
  `expect`/`actual` in `:shared` androidMain (when intrinsic to shared logic:
  crypto, HTTP engine) or via interfaces implemented in `:app` (when app-level:
  alarms, notifications — e.g. tool executors).
- **Hilt never crosses into `:shared`.** Shared classes use plain constructor
  injection; Hilt modules in `:app`/`:feature:*` construct and provide them.
- **No user-visible text in `:shared`.** Expose typed errors
  (e.g. `ChatError.RateLimited`); feature ViewModels map them to string resources.
  Strings live in the module that owns the UI: feature strings in the feature,
  generic strings in `:core:ui`, app name in `:app`.

## Non-negotiables

- No ViewModel → UI events (no SharedFlow/Channel event bus). Fold every event
  outcome into `UiState`; UI acknowledges consumption via a ViewModel method.
- No async work in ViewModel `init` blocks or constructors.
- Navigation 2 (`navigation-compose`) is prohibited — Nav 3 only.
- No MockK or any mocking library. Fakes and real objects only
  (Room in-memory, Ktor `MockEngine`).
- Every type is main-safe; the data layer moves work off the main thread.
- TDD (red → green → refactor) for every step.

## Naming

| Thing | Pattern | Example |
|---|---|---|
| Repository | `XRepository` (interface) | `ReminderRepository` |
| Repository impl | `DefaultXRepository` / descriptive | `DefaultReminderRepository` |
| Test double | `FakeX` | `FakeReminderRepository` |
| Data source | `XLocalDataSource` / `XRemoteDataSource` | `MemoryLocalDataSource` |
| Use case | `VerbNounUseCase`, `operator fun invoke` | `ComposeReminderMessageUseCase` |
| Screen state | `ScreenUiState` | `ConversationUiState` |
| Flow accessor | `getXFlow()` | `getRemindersFlow()` |

**Product name vs. identifiers.** "Bro" is the **display name only** — launcher label,
wordmark, and user-facing copy. Code identifiers — Gradle projects, packages, modules,
classes, files, functions — use the neutral project name (`Chatbot`) or plain domain terms, and never "Bro".

## Building a feature end to end

Order matters: data layer first, UI last. TDD at every step.

1. **Read the feature spec** (`specs/NNN-*.md`) — data model, acceptance criteria,
   journey files.
2. **Data layer** (`:shared` commonMain): Room entities + DAO → repository
   interface + `Default` impl. Write the `FakeXRepository` alongside in
   commonTest. → [references/data-layer.md](references/data-layer.md)
3. **Domain layer** — only if logic is reused across ViewModels or a ViewModel is
   getting complex. Usually skip. → [references/domain-layer.md](references/domain-layer.md)
4. **ViewModel + UiState** (`:feature:*`): define `XUiState`, produce it with the
   `stateIn` recipe, handle events as methods.
   → [references/ui-layer.md](references/ui-layer.md)
5. **Screen composable**: stateless, takes `uiState` + event lambdas. Compose from
   `:core:ui` tokens + catalog components, translate the mockup → `design-system` skill.
   Adaptive from day one → `adaptive` skill; insets → `edge-to-edge` skill.
6. **Navigation**: typed key + `entryProvider` entry in the `:app` graph, deep
   link if the spec requires → `navigation-3` skill.
7. **Hilt wiring**: provide shared classes from a Hilt module in the feature
   (or `:app` if cross-feature).
8. **Tests**: ViewModel unit tests (fakes, assert on `StateFlow.value`), DAO
   tests (in-memory Room), Compose UI tests → `testing-setup` skill. Every public
   screen/component composable also ships `@PreviewTest` previews in dark **and**
   light, recorded/checked with `updateDebugScreenshotTest` /
   `validateDebugScreenshotTest` (infra set up in `:core:ui`).
9. **Journeys**: run the spec's journey XMLs via the `android` CLI.

## References

- [references/ui-layer.md](references/ui-layer.md) — UiState modeling, state
  production recipe, ViewModel rules, state holder choice
- [references/data-layer.md](references/data-layer.md) — repositories, data
  sources, Room/DataStore, KMP rules, threading
- [references/domain-layer.md](references/domain-layer.md) — use cases
- Google source docs these distill — fetch latest via `android docs fetch <uri>`:
  - `kb://android/topic/architecture/index`
  - `kb://android/topic/architecture/recommendations`
  - `kb://android/topic/architecture/ui-layer/index`
  - `kb://android/topic/architecture/data-layer/index`
  - `kb://android/topic/architecture/domain-layer`
  - `kb://android/topic/libraries/architecture/viewmodel/index`
  - `kb://android/develop/ui/compose/architecture`
  - `kb://android/kotlin/flow/stateflow-and-sharedflow`
