# Research — KMP, Compose Multiplatform & Kotlin Toolchain

**Date:** July 3, 2026 · follow-up to `docs/research-report.md`
**Method:** two parallel research agents against live sources (kotlinlang.org, developer.android.com, gradle.org, github.com/google/ksp, JetBrains blog). Sources inline.

---

## Part A — Kotlin Multiplatform & Compose Multiplatform

### KMP state

- Core KMP (expect/actual) stable since Kotlin 1.9.20 (2023). By 2026 Google frames it as **the default answer for sharing business logic Android↔iOS** ([developer.android.com/kotlin/multiplatform](https://developer.android.com/kotlin/multiplatform), [I/O+KotlinConf '25 announcements](https://android-developers.googleblog.com/2025/05/android-kotlin-multiplatform-google-io-kotlinconf-2025.html)). Google Docs iOS ships KMP in production.
- Tooling: dedicated **KMP plugin for IntelliJ IDEA + Android Studio** (K2 mode required). **Fleet is dead** — JetBrains shut it down Dec 2025; Android Studio/IDEA is the only recommended path.
- Roadmap: Swift Export approaching Alpha; Navigation 3 stabilization for CMP.

### Compose Multiplatform state

| Target | Status (CMP 1.11.1 stable, Jun 2, 2026; 1.12.0-beta01 out) |
|---|---|
| Android | = Jetpack Compose |
| **iOS** | **Stable since 1.8.0 (May 2025)**; 1.11 turned on concurrent rendering by default, experimental native UIView text input. JetBrains survey: 96% of teams report no major iOS perf concerns |
| Desktop | Stable/mature |
| Web (Wasm) | **Beta**; Kotlin/Wasm stable targeted late 2026 — not production today |

- `material3-adaptive` + `adaptive-navigation3` ship cross-platform (CMP 1.10+): ListDetailPaneScaffold etc.
- Nav 3 on non-Android landed in CMP 1.10 (Jan 2026); needs polymorphic serialization for keys on non-JVM targets.
- Rough edges: Swift Export still Alpha (teams use third-party SKIE); Compose views opaque to XCTest/Xcode visual debugging.

### Library KMP-readiness (verified)

| Library | KMP? | Notes |
|---|---|---|
| Room 2.8.x | ✅ Stable | DAOs/entities in commonMain |
| DataStore 1.1.x | ✅ Stable | |
| ViewModel/Lifecycle 2.9.x | ✅ Stable | SavedState, Paging recently added |
| Navigation 3 | ◐ runtime KMP (JVM/Native/Web); UI artifact completeness unclear — verify | CMP 1.10 has own Nav3 layer |
| **Hilt** | ❌ **confirmed dead-end** | Java codegen, Android-only |
| Koin 4.x | ✅ | Runtime DI, common Hilt exit |
| kotlin-inject | ✅ | Compile-time |
| **Metro** | ✅ **1.0 stable Apr 2026** | Compile-time DI compiler plugin; Cash App migrated off Hilt (16% faster clean builds, ~60% faster incremental); Vinted et al. ([metro is stable](https://www.zacsweers.dev/metro-is-stable/)) |
| Ktor + kotlinx.serialization | ✅ | De facto KMP networking; Retrofit/Moshi do NOT port |
| SQLDelight | ✅ | Alternative to Room; Room fine for Android-first teams |
| Anthropic SDK | ❌ official Java SDK JVM-only | KMP options: [xemantic/anthropic-sdk-kotlin](https://github.com/xemantic/anthropic-sdk-kotlin) (Ktor, all targets), [tddworks/openai-kotlin](https://github.com/tddworks/openai-kotlin) (multi-provider). Also [JetBrains Koog](https://github.com/JetBrains/koog) 1.0 (May 2026) — KMP AI-agent framework (MCP, RAG) if we ever need more than chat |

### On-device AI asymmetry (the catch for this app)

- **Android:** ML Kit GenAI Prompt API (Gemini Nano) — Android-only, forever.
- **iOS equivalent:** Apple **Foundation Models framework** (iOS 26) — on-device ~3B model; WWDC 2026 added sparse ~20B "AFM 3 Core Advanced" + Core AI framework (local models to 70B). Apple to open-source Foundation Models summer 2026.
- **WWDC 2026:** Anthropic + Google announced **Swift packages extending Foundation Models framework** — Claude/Gemini via the same Swift API surface. Announced, not verified shipped — check before depending on it.
- **No KMP abstraction unifies on-device LLMs.** Pattern if ever needed: thin expect/actual (or DI-injected interface) per platform; shared prompt/conversation logic in commonMain. Community alternative: bundled llama.cpp via [Llamatik](https://github.com/ferranpons/llamatik) — trades OS-integrated models for self-managed ones; not production-vetted.

### Recommendation (expert consensus 2026: Booking.com, Volpis, ProAndroidDev)

**Do NOT go full KMP day one for an Android-first app.** Costs still real: Xcode/Gradle dual build, macOS CI, slower Kotlin/Native compiles, Swift interop friction, weaker iOS test tooling. Full KMP only if iOS is funded and near-term.

**Do keep the extraction path cheap** — and it's now genuinely cheap because the ecosystem converged on KMP-ready libraries:

1. Our persistence/state picks (Room, DataStore, ViewModel) are already KMP-stable — no change needed.
2. Networking: prefer **Ktor + kotlinx.serialization** over Retrofit/Moshi (zero cost today, portable tomorrow). Affects our Claude client choice: official Java SDK is JVM-only; a Ktor-based client keeps the door open.
3. DI is the one real fork: **Hilt blocks KMP**. If iOS becomes real, Metro (stable, proven) or Koin. Migration Hilt→Metro/Koin is well-trodden but not free.
4. On-device engine is platform-locked regardless — `ChatEngine` interface (already planned in spec 002) is exactly the right seam.
5. Chatbot UI would port via CMP (iOS stable) if ever wanted.

---

## Part B — Kotlin toolchain (Kotlin 2.4 / AGP 9 / Gradle 9)

### Kotlin 2.4.0 (stable Jun 2026)

- **Stable:** context parameters (mostly), explicit backing fields, `@all` annotation use-site target.
- **Experimental:** collection literals (`-Xcollection-literals`), must-use return-value checker (`-Xreturn-value-checker`), improved const evaluation.
- **"Rich errors" did NOT ship in 2.4.0** — SEO blogspam claims otherwise; absent from official whatsnew page.
- K1 fully removed — K2 only. Java 26 bytecode target. `kotlin.uuid.Uuid` stable.
- AGP compatibility: full Kotlin 2.4 support needs **AGP 9.1.0+** ([compatibility matrix](https://developer.android.com/build/kotlin-support)).

### ⚠️ KSP blocker — the sharpest edge (as of 2026-07-03)

- Latest KSP release **2.3.9** targets Kotlin **2.3**, not 2.4. **No Kotlin-2.4-compatible KSP exists yet.**
- [google/ksp#2964](https://github.com/google/ksp/issues/2964) — open **P1**: KSP 2.3.9 on Kotlin 2.4.0 breaks Dagger/Hilt codegen (new `{group}:{project_name}` module-name format injects `:` KotlinPoet can't escape). Fix PR in progress. [#2965](https://github.com/google/ksp/issues/2965) tracks the 2.4 upgrade.
- **Consequence: Kotlin 2.4.0 + Room/Hilt (both KSP) doesn't work today.** Options: start on Kotlin 2.3.x + KSP 2.3.9 and bump when KSP ships 2.4 support (recommended), or wait. Check [KSP releases](https://github.com/google/ksp/releases) at scaffold time.

### Toolchain setup

- **JDK to run Gradle:** 17 minimum (AGP 9 + Gradle 9). **Bytecode target:** AGP 9 default is now **Java 17** (up from 11) — use 17.
- Use `kotlin { jvmToolchain(17) }` (propagates to Java compile too). Add foojay resolver in `settings.gradle.kts` for JDK auto-provisioning:
  `id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"`
- AGP 9 built-in Kotlin: `android.kotlinOptions{}` gone; `kotlin.compilerOptions.jvmTarget` **defaults to `compileOptions.targetCompatibility`** — set targetCompatibility once, don't hand-set jvmTarget (kills the classic mismatch bug). Can omit `kotlin-android` plugin entirely.
- **Gradle:** current stable **9.6.1**. AGP floors: 9.0.1→Gradle 9.1, 9.1.1→9.3.1, **9.3.0→9.5.0**. Pin ≥9.5, use 9.6.1.
- **Configuration cache:** enable (`org.gradle.configuration-cache=true`) — Gradle 9's "preferred execution mode," ~2× config-time wins.
- **Isolated Projects:** still experimental — do NOT enable; watch Gradle 9.7.
- **Compose compiler:** ships with Kotlin — `org.jetbrains.kotlin.plugin.compose` version = Kotlin version. Strong skipping default since 2.0.20; in 2.4 setting `StrongSkipping`/`IntrinsicRemember` flags is an **error** — omit `composeCompiler {}` block entirely.

### Reference libs.versions.toml skeleton

```toml
[versions]
agp = "9.3.0"
kotlin = "2.4.0"          # ⚠️ drop to 2.3.x if KSP hasn't shipped 2.4 support at scaffold time
ksp = "?"                  # MUST match Kotlin — check github.com/google/ksp/releases
composeBom = "2026.06.00"
hilt = "2.57"              # spot-check Maven Central

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
# kotlin-android omitted — AGP 9 built-in Kotlin
```

```kotlin
// settings.gradle.kts
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

// app/build.gradle.kts
kotlin { jvmToolchain(17) }
android {
    compileSdk = 36
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17   // jvmTarget inherits this
    }
}
```

```properties
# gradle.properties
org.gradle.configuration-cache=true
```

---

## Impact on spec 001 (proposed)

1. **Kotlin pin:** keep "2.4.x with fallback" but flip expectation — **KSP blocker confirmed, so scaffold will likely start 2.3.x**. Track KSP releases.
2. **Add toolchain rows:** Gradle 9.6.1+, JDK 17 via jvmToolchain + foojay, configuration cache on, isolated projects off, Compose compiler = Kotlin plugin (no flags).
3. **KMP posture (decision needed):** stay native Android. Cheap-extraction insurance options: (a) Ktor-based Claude client instead of Anthropic Java SDK, (b) Metro or Koin instead of Hilt. Both deviate from current accepted spec — decide based on how real iOS is.
