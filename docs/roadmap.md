# Implementation Roadmap

Build order and milestone gates for the app described in `specs/000-product-brief.md` on the stack in `specs/001-tech-stack.md`. Spec numbers follow build order. This document owns ordering and milestone status; specs stay descriptions of the current system.

**Strategy: walking skeleton.** Milestone 0 scaffolds every module thin and proves the toolchain end to end; each later milestone cuts a vertical slice (spec → data → UI → journeys). Integration risk dies first; the app is sideloadable from the end of M1.

## Milestones

### M0 — Scaffold

No feature spec; this section is the definition.

- Pin open library versions (kotlinx.serialization / coroutines / datetime, androidx.work, Nav 3 patch — tech-stack Open items 1 & 4). Check KSP-on-Kotlin-2.4 status (Open item 5); scaffold on Kotlin 2.3.x.
- Create project via `android` CLI: Gradle 9.6.1+, AGP 9.3.x, version catalog, configuration cache ON.
- Create all modules thin: `:app`, `:shared` (KMP, `androidTarget()` only), `:core:ui`, feature module stubs. Dependency rules per `architecture` skill.
- Wire Hilt. Set up test harness (JUnit4, Robolectric 4.16+, Compose testing v2) via `testing-setup` skill.
- One trivial journey XML passing on emulator (app launches).

**Exit gate:** empty app builds and launches; unit + journey baseline green.

### M1 — Chat MVP (specs 002–006)

First sideload. Streaming chat with the user's own key; no agentic tools yet.

| Spec | Contents |
|---|---|
| `002-design-system.md` | `:core:ui` M3 theme, core components, previews + screenshot tests, companion `design-system` project skill. Grows in later milestones as screens need components |
| `003-chat-engine.md` | `ChatEngine` interface, Ktor Claude implementation, SSE streaming. Highest technical risk — built first among features. Tool-use handling deferred to 008 |
| `004-conversation-storage.md` | Room schema: conversations + messages (reminders/memories tables deferred to their specs) |
| `005-conversation-shell.md` | Nav 3 back stack, conversation list + chat screen (streaming UI, composer, in-conversation model picker), adaptive two-pane, deep-link readiness. Runs on a dev-key stub from debug build config (developer's own key, debug builds only — the product stays BYOK-only) |
| `006-onboarding.md` | First-launch key entry, validation, encrypted storage (Tink + Keystore → DataStore), Nav 3 conditional gate (no key → onboarding). Replaces the dev-key stub |

**Exit gate:** onboard with real key → stream chat → conversations persist/resume/delete; all journeys green. **Sideload checkpoint.**

### M2 — Tool loop + memory (specs 007–009)

| Spec | Contents |
|---|---|
| `007-settings.md` | Settings shell + API key management (view/change/remove) |
| `008-agent-tools.md` | Tool definitions and execution protocol: engine handles `tool_use` / `tool_result` blocks, `input_json_delta`, `stop_reason: "tool_use"`; agentic loop in `:shared` commonMain, executors injected |
| `009-memory.md` | `remember` tool, memory Room table, injection policy + token budgeting, memory management UI added to settings |

Memory before reminders: smallest surface that proves the whole tool loop.

**Exit gate:** ask AI to remember → fact persists → shapes new conversations → viewable/deletable in settings; journeys green. **Sideload checkpoint.**

### M3 — Reminders (spec 010)

| Spec | Contents |
|---|---|
| `010-reminders.md` | `set_reminder` tool, reminders Room table, AlarmManager exact scheduling + permission flow added to settings, receiver → expedited WorkManager → fire-time composition, notification deep link into conversation, reboot re-registration, offline fallback text |

Hardest subsystem, lands on a proven tool loop.

**Exit gate:** reminder journeys green, including permission-denied (inexact fallback), reboot, and offline-fallback cases. **Sideload checkpoint.**

### M4 — Polish

- Adaptive two-pane completion across screens (005 follow-through)
- Screenshot-test coverage sweep, edge cases
- Portfolio README + demo recordings

## Per-milestone workflow

1. Brainstorm the feature spec (superpowers brainstorming skill), save to `specs/`
2. Isolated worktree, then implementation plan → `docs/superpowers/plans/`
3. TDD implementation (red → green → refactor)
4. Exit gate: all tests + the milestone's journey XMLs green on the emulator — no milestone starts on a red baseline

Spec = what/why (`specs/`); plan = how (`docs/superpowers/plans/`). The settings screen grows across milestones; each addition is specced by the feature that owns it (007 shell + key, 009 memory UI, 010 permission flows).

## Standing risks

- **Kotlin 2.4 / KSP blocker** (tech-stack Open item 5): re-check google/ksp#2964 / #2965 at every milestone boundary; bump from 2.3.x when KSP ships 2.4 support.
- **compileSdk 37 bump** expected with Compose 1.12 (Open item 2): revisit at milestone boundaries.
- **M3 device-matrix risk:** exact alarms, Doze, and reboot behavior vary by OEM — test journeys on a physical device, not just the emulator.

## Status

| Milestone | State |
|---|---|
| M0 scaffold | not started |
| M1 chat MVP (002–006) | not started |
| M2 tool loop + memory (007–009) | not started |
| M3 reminders (010) | not started |
| M4 polish | not started |
