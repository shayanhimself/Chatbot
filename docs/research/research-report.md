# Research Report — Spec-Driven Native Android Development System

**Project:** Chatbot (on-device Gemini Nano + optional Claude BYOK)
**Date:** July 2, 2026
**Method:** Three parallel research agents (web search + primary-source fetches), synthesized into this report. Sources linked inline.

---

## Table of contents

1. [Spec-driven development: obra/superpowers](#1-spec-driven-development-obrasuperpowers)
2. [Official Android skills for Claude Code](#2-official-android-skills-for-claude-code)
3. [Claude Code skills/plugins mechanics](#3-claude-code-skillsplugins-mechanics)
4. [Jetpack Navigation 3](#4-jetpack-navigation-3)
5. [Recommended Android tech stack (mid-2026)](#5-recommended-android-tech-stack-mid-2026)
6. [Testing stack](#6-testing-stack)
7. [Gemini Nano on-device APIs](#7-gemini-nano-on-device-apis)
8. [Cloud fallback options](#8-cloud-fallback-options)
9. [Claude API from Android (BYOK)](#9-claude-api-from-android-byok)
10. [Synthesized architecture recommendation](#10-synthesized-architecture-recommendation)
11. [What was set up in this project](#11-what-was-set-up-in-this-project)
12. [Open uncertainties](#12-open-uncertainties)

---

## 1. Spec-driven development: obra/superpowers

**What it is:** An "agentic skills framework & software development methodology" by Jesse Vincent — a composable library of skills plus workflow scaffolding. Cross-agent: works with Claude Code, Antigravity, Cursor, Kimi Code, Pi, GitHub Copilot CLI, and Codex.

**Repo structure** (v2.0+ split into two repos):

- [github.com/obra/superpowers](https://github.com/obra/superpowers) — core skills library/methodology
- [github.com/obra/superpowers-marketplace](https://github.com/obra/superpowers-marketplace) — the Claude Code plugin marketplace used to install it

**Install (Claude Code):**

```
/plugin marketplace add obra/superpowers-marketplace
/plugin install superpowers@superpowers-marketplace
```

Optional companion plugins from the same marketplace: `elements-of-style`, `superpowers-developing-for-claude-code`, `private-journal-mcp`.

**Skills library** (marketplace.json v6.1.0 at fetch time):

| Category | Skills |
|---|---|
| Testing/quality | `test-driven-development` (RED-GREEN-REFACTOR; deletes code written before tests), `verification-before-completion` |
| Debugging | `systematic-debugging` (4-phase root-cause analysis), `receiving-code-review` |
| Collaboration/workflow | `brainstorming` (Socratic design refinement), `writing-plans`, `executing-plans`, `dispatching-parallel-agents`, `requesting-code-review`, `using-git-worktrees`, `finishing-a-development-branch`, `subagent-driven-development` |
| Meta | `writing-skills`, `using-superpowers` |

**Recommended spec-driven workflow (7 stages):**

1. **Brainstorming** — refine the rough idea via questions, explore design alternatives, produce a design doc
2. **Git worktrees** — isolated workspace/branch with clean test baseline
3. **Writing plans** — break work into 2–5 minute tasks with exact specifications (the "spec" artifact)
4. **Subagent-driven development** — fresh subagent per task, two-stage review (compliance + quality)
5. **Test-driven development** — RED (failing test) → GREEN (make it pass) → REFACTOR
6. **Code review** — review completed work against the plan, issues blocked by severity
7. **Finishing the branch** — verify tests, then merge / open PR / keep working / discard

Sources: [superpowers repo](https://github.com/obra/superpowers), [superpowers-marketplace](https://github.com/obra/superpowers-marketplace), [blog.fsck.com writeup](https://blog.fsck.com/2025/10/09/superpowers/)

---

## 2. Official Android skills for Claude Code

### Anthropic

`github.com/anthropics/skills` exists (demonstration repo of Agent Skills: creative, dev/technical, enterprise, document skills, plus `skill-creator` meta-skill) — but **contains no Android-specific skills**.

### Google — the real answer: `android/skills`

- Repo: [github.com/android/skills](https://github.com/android/skills) — announced on the Android Developers Blog **April 16, 2026**: ["Android CLI and skills: Build Android apps 3x faster using any agent"](https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html). v1.0.3 released June 29, 2026; ~4,500+ stars.
- Built on the open [Agent Skills standard](https://agentskills.io) (same SKILL.md format Anthropic uses). Explicitly supports **Gemini in Android Studio, Gemini CLI, Antigravity, Claude Code, and Codex**.
- Claimed benchmarks: 70%+ reduction in LLM token usage, 3× faster task completion vs. standard toolsets.
- Google's stated focus: use cases where evaluations show LLMs underperform (not well-established basics).
- Docs: [developer.android.com/tools/agents/android-skills](https://developer.android.com/tools/agents/android-skills)

**Skill catalog** (v1.x):

| Skill | Domain |
|---|---|
| `navigation-3` | Jetpack Navigation 3 install/migration/patterns (deep links, scenes, Hilt/ViewModel integration) |
| `adaptive` | Adaptive UI across phones/tablets/foldables/desktop/TV/Auto/XR; Nav3 Scenes multi-pane |
| `testing-setup` | Testing strategy, libraries, unit/UI/screenshot/e2e harnesses |
| `edge-to-edge` | Edge-to-edge migration, inset bugs |
| `android-cli` | Installing/using the `android` CLI |
| `agp-9-upgrade` | AGP 9 migration |
| `migrate-xml-views-to-jetpack-compose` | XML → Compose migration |
| `styles` | Compose Styles API |
| `r8-analyzer` | R8 keep-rule analysis |
| `perfetto-trace-analysis`, `perfetto-sql` | Performance tracing |
| `camera1-to-camerax`, `android-intent-security`, `verified-email`, `appfunctions`, `engage-sdk-integration`, `play-billing-library-version-upgrade`, `wear-compose-m3`, `display-glasses-with-jetpack-compose-glimmer` | Other domains |

**Android CLI** (`android` command) ships alongside: `android sdk install`, `android create`, `android emulator`, `android run`, `android update`, `android docs`, `android skills list`, `android skills add --skill <name>`. Docs: [developer.android.com/tools/agents/android-cli](https://developer.android.com/tools/agents/android-cli)

### Android Studio's separate rules system

Distinct from `android/skills`, for the IDE's built-in Gemini agent: `AGENTS.md` files at project/module roots ([docs](https://developer.android.com/studio/gemini/agent-files)) and IDE-specific Rules in `.idea/project.prompts.xml` ([docs](https://developer.android.com/studio/gemini/rules)).

### Community (non-official) Android skill packs

`github.com/rcosteira79/android-skills` (Android & KMP), `github.com/dpconde/claude-android-skill`, `github.com/new-silvermoon/awesome-android-agent-skills`.

---

## 3. Claude Code skills/plugins mechanics

**Skill locations & precedence** (enterprise > personal > project; all override same-named bundled skills):

| Location | Path | Scope |
|---|---|---|
| Personal | `~/.claude/skills/<name>/SKILL.md` | all projects |
| Project | `.claude/skills/<name>/SKILL.md` | this project (committable) |
| Plugin | `<plugin>/skills/<name>/SKILL.md` | wherever plugin enabled |

**SKILL.md format:** YAML frontmatter + Markdown body. Only `description` is strictly needed (drives auto-invocation). Other frontmatter: `name`, `when_to_use`, `argument-hint`, `disable-model-invocation`, `user-invocable`, `allowed-tools`/`disallowed-tools`, `model`, `effort`, `context: fork`, `agent`, `hooks`, `paths`, `shell`. Keep SKILL.md under ~500 lines; push detail into `references/`, `scripts/`, `assets/` (progressive disclosure).

**Skills vs plugins vs marketplaces:**

- **Skill** = one capability unit (folder + SKILL.md), standalone in `.claude/skills/` or bundled in a plugin
- **Plugin** = shareable package (skills + agents + hooks + MCP servers + manifest `.claude-plugin/plugin.json`); plugin skills namespaced `/plugin-name:skill-name`
- **Marketplace** = git repo catalog of plugins; `/plugin marketplace add <owner/repo>` then `/plugin install <plugin>@<marketplace>`. Anthropic runs `claude-plugins-official` (auto-registered) and `claude-community`
- Local testing: `claude --plugin-dir ./my-plugin`; live reload via `/reload-plugins`

Sources: [code.claude.com/docs/en/skills](https://code.claude.com/docs/en/skills), [code.claude.com/docs/en/plugins](https://code.claude.com/docs/en/plugins)

---

## 4. Jetpack Navigation 3

**Status: STABLE.** 1.0.0 on **Nov 19, 2025** ([announcement](https://developer.android.com/blog/posts/jetpack-navigation-3-is-stable)); first announced May 14, 2025.

**Versions** (per [official release notes](https://developer.android.com/jetpack/androidx/releases/navigation3), live fetch July 2026):

| Track | Version | Notes |
|---|---|---|
| Stable | **1.1.4** (Jul 1, 2026) | current |
| 1.1.0 (Apr 8, 2026) | milestone | shared elements, `SceneDecoratorStrategy`, type-safe `NavMetadata` DSL, `OverlayScene` animations |
| Alpha | 1.2.0-alpha05 | deep-link request extras, `rememberResultEventBus` |

**Maven coordinates:**

```
androidx.navigation3:navigation3-runtime:1.1.4
androidx.navigation3:navigation3-ui:1.1.4
androidx.compose.material3.adaptive:adaptive-navigation3:<version>   // list-detail Scene/Strategy
```

KMP note: runtime artifact ships JVM, Native (iOS/macOS/Linux/etc.), and Web (JS/Wasm) targets.

**Core concepts** ([docs](https://developer.android.com/guide/navigation/navigation-3)):

- **Back stack** — a plain `mutableStateListOf<Any>` of serializable keys you own directly; no `NavGraph`
- **NavEntry** — key + composable content + optional metadata map
- **entryProvider DSL** — `entryProvider { entry<Key> { ... } }` maps key → NavEntry
- **NavDisplay** — observes back stack, renders current Scene; takes `backStack`, `entryProvider`, `onBack`, `sceneStrategies`
- **Scene / SceneStrategy** — a Scene can render multiple NavEntries at once (list+detail); `SceneStrategy.calculateScene()` groups entries by window size/metadata; falls back to `SinglePaneSceneStrategy`. This is the adaptive-layout mechanism (`rememberListDetailSceneStrategy()`)

**Nav3 vs Nav2:**

| | Navigation 2 | Navigation 3 |
|---|---|---|
| Model | Declarative NavGraph/NavHost, one visible destination | You own a plain list as the back stack |
| Multi-pane | Workarounds | Native via Scenes |
| Type safety | Bolted on later | Typed keys from day one |
| Extensibility | Fixed navigator model | Pluggable SceneStrategy/SceneDecoratorStrategy |

**Resources:** official recipes [github.com/android/nav3-recipes](https://github.com/android/nav3-recipes) (saveable back stack, deep links, dialog/bottom-sheet/list-detail scenes, conditional nav, Hilt/Koin/Metro modular nav, ViewModel integration, migration guide); CMP fork [terrakok/nav3-recipes](https://github.com/terrakok/nav3-recipes). Official `navigation-3` skill in `android/skills`.

---

## 5. Recommended Android tech stack (mid-2026)

| Component | Version / status |
|---|---|
| Android Studio | Quail 1 (2026.1.1 Patch 2, Apr 28, 2026) |
| Kotlin | 2.3.20 stable (Mar 2026); 2.4.20 expected ~Sep 2026 |
| AGP | 9.x stable (9.3.0 Jun 2026); AGP 10 removes opt-out for new DSL defaults |
| Compose BOM | 2026.06.00; core modules ~1.11.0. Upcoming Compose 1.12.0 will require compileSdk 37 + AGP 9 |
| KSP | KSP2 (default/only actively developed) |
| DI | **Hilt** — still Google's official recommendation ([architecture guide](https://developer.android.com/topic/architecture)). Emerging alternative: **Metro** ([ZacSweers/metro](https://github.com/ZacSweers/metro)) — compile-time, KMP-friendly, used in official nav3-recipes samples. Koin remains the runtime-based simple option. No consensus shift yet; Hilt is the safe default |
| Persistence | Room 2.8.x (structured data), DataStore 1.1.x (key-value/proto); both officially KMP now |
| Serialization | kotlinx.serialization (verify exact version against GitHub releases — search results conflicted) |
| Material | M3 stable; **M3 Expressive still stabilizing** (material3 1.5.0-alpha22 as of Jun 2026) |
| Architecture | Unchanged official guidance: UI → optional Domain → Data, UDF, SSOT, ViewModel state holders |
| Build config | `libs.versions.toml` version catalogs standard; JDK 17 jvmTarget (JDK 21 needed to run Robolectric against SDK 36) |
| SDK levels | **targetSdk 36 (Android 16) mandatory for Play submissions by Aug 31, 2026**; compileSdk 36 (37 coming); minSdk 26+ recommended floor. Android 16: 16KB page-size alignment for native libs, `USE_FULL_SCREEN_INTENT` permission change |

---

## 6. Testing stack

- **JUnit4** remains the Android default; JUnit5 is opt-in via third-party `de.mannodermaus.android-junit5` plugin (not first-party)
- **Compose UI tests:** v2 testing APIs became default in the April 2026 release (v1 deprecated); default test dispatcher changed `UnconfinedTestDispatcher` → `StandardTestDispatcher` (behavioral change to watch)
- **Robolectric 4.16.1:** supports SDK 36; needs JDK 21 for SDK-36 tests; native resources mode
- **Screenshot testing:** [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing) is Google's recommended tool (`@PreviewTest`, `screenshotTest` source set, HTML diff reports) — still preview-channel. Mature third-party alternatives: Paparazzi, Roborazzi, Shot, Dropshots, unified by [ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner)

---

## 7. Gemini Nano on-device APIs

### Current recommendation: ML Kit GenAI APIs (not raw AICore SDK)

| | ML Kit GenAI (current) | Direct AICore SDK (legacy) |
|---|---|---|
| Maven | `com.google.mlkit:genai-prompt:1.0.0-beta2` (+ `genai-summarization`, `genai-proofreading`, `genai-rewriting`, `genai-image-description`) | `com.google.ai.edge.aicore:aicore:0.0.1-exp01` |
| Status | Beta, recommended surface; new investment lands here (Prompt API, Gemma 4 support) | Stalled at experimental — soft-deprecated |
| Min SDK | 26 | 31 |

### Prompt API (the chatbot API)

```kotlin
val generativeModel = Generation.getClient()

// Availability + model download (model is Play-services-managed, not bundled):
generativeModel.checkStatus() // AVAILABLE | DOWNLOADABLE | UNAVAILABLE | DOWNLOADING
generativeModel.download(downloadCallback) // DownloadStarted/Progress/Completed/Failed

// Generate:
generativeModel.generateContent("prompt")                     // one-shot
generativeModel.generateContentStream(...)                    // Flow-based streaming
generativeModel.generateContent(generateContentRequest(ImagePart(bitmap), TextPart(text))) // multimodal
```

- **Streaming:** yes (Flow in Kotlin, callbacks in Java)
- **Multi-turn:** **not native** — no session/chat object; app manages history and re-injects trimmed context each call
- **System instructions:** no first-class parameter documented — fold into prompt text
- **Generation params:** `temperature`, `seed`, `topK`, `candidateCount`, `maxOutputTokens`

### Limits

- Input: keep under **~4000 tokens** (~3000 English words)
- Output: Google advises against use cases needing **>256 output tokens**
- **Foreground-only** inference (no background)

### Device support

- **nano-v2:** Pixel 9 series, Galaxy S25 series, select Honor/iQOO/Motorola/OnePlus/OPPO/POCO/realme/vivo/Xiaomi
- **nano-v3:** Pixel 10 series + newer 2026 flagships
- AICore gating is an internal allowlist (Android 14+, NPU) — **no published spec threshold; always detect via `checkStatus()` at runtime, never hardcode device lists**
- Caveat: the system-level "Gemini Intelligence" feature (12GB+ RAM, nano-v3 only) is a different thing — don't conflate with the ML Kit developer API's support table

Sources: [ML Kit GenAI overview](https://developers.google.com/ml-kit/genai), [Prompt API get-started](https://developers.google.com/ml-kit/genai/prompt/android/get-started), [Gemini Nano on Android](https://developer.android.com/ai/gemini-nano), [Prompt API launch blog](https://developer.android.com/blog/posts/ml-kit-s-prompt-api-unlock-custom-on-device-gemini-nano-experiences)

---

## 8. Cloud fallback options

Google's recommended pattern: **Firebase AI Logic hybrid inference** — wraps ML Kit Prompt API on-device, transparently falls back to cloud Gemini Developer API.

```gradle
implementation("com.google.firebase:firebase-ai:17.13.0")
implementation("com.google.firebase:firebase-ai-ondevice:16.0.0-beta03")
```

Modes: `PREFER_ON_DEVICE`, `PREFER_IN_CLOUD`, `ONLY_ON_DEVICE`, `ONLY_IN_CLOUD`. On-device constraints mirror ML Kit (4000-token cap; only English + Korean validated).

For this project the cloud tier is **Claude**, so we replicate the pattern ourselves: `checkStatus()` gate → Claude path when Nano unavailable. Sources: [hybrid get-started](https://firebase.google.com/docs/ai-logic/hybrid/android/get-started), [hybrid inference blog](https://developer.android.com/blog/posts/experimental-hybrid-inference-and-new-gemini-models-for-android)

---

## 9. Claude API from Android (BYOK)

### BYOK vs backend proxy

- **Service-owned key embedded in app: never** — extractable via APK decompilation/MITM/memory inspection; backend proxy is the safe default for that model
- **BYOK (user supplies their own key): legitimate direct-from-device pattern** — blast radius is the user's own key/spend
- `anthropic-dangerous-direct-browser-access` header is **browser-CORS-only** — irrelevant to native Android; no mobile equivalent needed

**Required headers:**

```
Content-Type: application/json
x-api-key: <user-supplied key>
anthropic-version: 2023-06-01
```

### SDK

No Kotlin-specific SDK, but the **official Java SDK works from Kotlin on Android** (OkHttp default backend):

```gradle
implementation("com.anthropic:anthropic-java:2.34.0")
```

```kotlin
val client = AnthropicOkHttpClient.builder().apiKey(userSuppliedKey).build()

val params = MessageCreateParams.builder()
    .model(Model.CLAUDE_HAIKU_4_5)
    .maxTokens(4096L)
    .addUserMessage(userText)
    .build()

// Streaming (SSE):
client.messages().createStreaming(params).use { stream ->
    stream.stream()
        .flatMap { it.contentBlockDelta().stream() }
        .flatMap { it.delta().text().stream() }
        .forEach { print(it.text()) }
}
```

Leaner alternative: raw OkHttp/Retrofit against `POST https://api.anthropic.com/v1/messages` with `stream: true`, parse SSE (`message_start` → `content_block_delta`/`text_delta` → `message_stop`).

### Model picks

| Model | ID | Role |
|---|---|---|
| Claude Haiku 4.5 | `claude-haiku-4-5` | Default — fast, cheap, forgiving of user's own key |
| Claude Sonnet 5 | `claude-sonnet-5` | Quality/speed balance ($2/$10 per MTok intro pricing through 2026-08-31) |
| Claude Opus 4.8 | `claude-opus-4-8` | Premium tier |

### Key storage on Android

- **EncryptedSharedPreferences is deprecated** (`androidx.security:security-crypto` stalled at 1.1.0-alpha07) — do not use
- Pattern: **Android Keystore** holds the encryption key (hardware-backed) → **Tink** AEAD encrypts the API-key string → ciphertext stored in **DataStore** Preferences → decrypt into memory only when building the client; never log/persist plaintext

Sources: [anthropic-sdk-java](https://github.com/anthropics/anthropic-sdk-java), [Java SDK docs](https://platform.claude.com/docs/en/api/sdks/java)

---

## 10. Synthesized architecture recommendation

1. **Engine abstraction:** common `ChatEngine` interface with two implementations — `NanoChatEngine` (ML Kit Prompt API) and `ClaudeChatEngine` (Anthropic Java SDK / SSE)
2. **On-device tier (default):** `checkStatus()` → `download()` with progress UX → `generateContentStream()`; app-managed history windowing/summarization (4000-in / 256-out limits); foreground-only
3. **Fallback triggers:** `UNAVAILABLE` status, or task needs long-form output / more context → route to Claude if key configured, else device-unsupported state
4. **Cloud tier:** BYOK Claude, default `claude-haiku-4-5`, model picker for Sonnet 5 / Opus 4.8; key via Tink + Keystore + DataStore
5. **Navigation:** Nav 3 — typed keys, `NavDisplay`, list-detail Scene for tablet/foldable conversation list + chat
6. **Process:** superpowers spec-driven pipeline (brainstorm → spec → plan → TDD subagent implementation → review → finish branch), specs in `specs/` as source of truth

Product-decision flag: Nano's ~256-token output ceiling means short on-device answers — decide in spec 001 whether long-answer requests auto-suggest the Claude engine.

---

## 11. What was set up in this project

```
CLAUDE.md                     project constitution: stack pins, spec-driven pipeline, engine rules
specs/000-product-brief.md    product brief + planned feature specs 001–005
docs/research-report.md       this report
.claude/skills/
  navigation-3/               official Google skill (from android/skills)
  adaptive/
  testing-setup/
  edge-to-edge/
  android-cli/
```

Remaining manual steps: `git init`; install superpowers (`/plugin marketplace add obra/superpowers-marketplace` → `/plugin install superpowers@superpowers-marketplace`); optionally install the `android` CLI.

---

## 12. Open uncertainties

1. **Nav3 exact latest version** — live release-notes fetch showed 1.1.4 stable; some stale search caches said 1.0.0-rc01. Verify against the [release-notes page](https://developer.android.com/jetpack/androidx/releases/navigation3) before pinning
2. **kotlinx.serialization / kotlinx.coroutines versions** — conflicting reports; check GitHub releases directly before locking
3. **compileSdk 36 vs 37** — 36 today, but Compose 1.12.0 will require 37; expect a bump within the year
4. **DI landscape** — Hilt officially recommended; Metro gaining official-sample adoption; genuinely unsettled
5. ~~**Superpowers in `claude-plugins-official`** — unverified~~ **Resolved (July 3, 2026):** confirmed from the repo README — superpowers is in Anthropic's official marketplace; `/plugin install superpowers@claude-plugins-official` works directly, no marketplace-add needed
6. **android/skills dates/versions** — April 16, 2026 announcement and v1.0.3 taken from fetched pages, not byte-verified against GitHub's release API
