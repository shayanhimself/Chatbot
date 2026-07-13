# Android Reference (vendored)

Snapshots of official developer.android.com architecture docs, pulled via `android docs fetch`
(the CLI's curated Knowledge Base — see `docs/android-cli-and-skills.md` §Docs search).

**Why vendored:** offline, reviewable, stable citation targets for the stack decisions in
`specs/001-tech-stack.md` §Architecture & DI. These are *reference*, not project specs — when
they conflict with `specs/`, the specs win (specs adapt Google's guidance to this app: KMP
`:shared`/`:app` split, Hilt-in-`:app`-only, BYOK, on-device agent).

**Snapshots, not live.** Each file's top HTML comment records source URL + fetch date. The KB
refreshes on `android update`; re-fetch to update (same `android docs fetch <url> | sed '1,/^----/d'`).

## Index

| File | Source `kb://` | Maps to |
|---|---|---|
| `00-architecture-guide.md` | `android/topic/architecture/index` | Overview — start here |
| `architecture-recommendations.md` | `android/topic/architecture/recommendations` | The rules (SSOT, UDF, DI, ViewModel) — spec 001 cites |
| `ui-layer.md` | `android/topic/architecture/ui-layer/index` | UI layer — `:app` Compose/VM |
| `ui-layer-state-holders.md` | `android/topic/architecture/ui-layer/stateholders` | State holders / ViewModel vs plain |
| `ui-layer-state-production.md` | `android/topic/architecture/ui-layer/state-production` | Building `UiState` |
| `data-layer.md` | `android/topic/architecture/data-layer/index` | Repos + SSOT — `:shared` (Room, reminders, memory) |
| `domain-layer.md` | `android/topic/architecture/domain-layer` | Optional Domain layer |
| `compose-udf.md` | `android/develop/ui/compose/architecture` | UDF in Compose |
| `viewmodel.md` | `android/topic/libraries/architecture/viewmodel/index` | ViewModel API |
| `stateflow-sharedflow.md` | `android/kotlin/flow/stateflow-and-sharedflow` | StateFlow (spec 001 async choice) |
