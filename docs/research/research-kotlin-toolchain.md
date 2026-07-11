# Research — JetBrains "Kotlin Toolchain" Build Tool Evaluation

**Date:** July 3, 2026
**Question:** Can (or when could) the JetBrains Kotlin Toolchain (kotlin-toolchain.org) replace Gradle+AGP for this project?
**Verdict:** **Not usable today; not plannable near-term. Stay on Gradle. Re-evaluate at each beta milestone.**
**Sources:** kotlin-toolchain.org docs, github.com/JetBrains/kotlin-toolchain, JetBrains YouTrack (`AMPER` project — the live tracker; cited issues linked inline), JetBrains blog, kotlinlang.org.

Not to be confused with the **Gradle JVM toolchain** (`kotlin { jvmToolchain(17) }`) — that's a JDK-selection setting inside Gradle, which we use regardless (spec 001).

---

## What it is

- **Amper, rebranded.** JetBrains' YAML-based build tool (dev since ~2023) renamed to "Kotlin Toolchain" at v0.11.0 (June 4, 2026); graduated to **Alpha**. Old `JetBrains/amper` repo archived; issue tracker still `AMPER-xxxx`.
- **Standalone only.** The old "Gradle-based mode" was removed (AMPER-4441 resolved Jun 2025). No hybrid Gradle/Toolchain project is possible.
- **But Android builds secretly run AGP underneath** — build logs show real AGP tasks (`MergeJavaResWorkAction`, `Gradle build failed` errors). Good for correctness; bad because YAML-surface gaps can't be patched with a Gradle escape hatch.
- Current: v0.11.1 (Jun 5, 2026), defaults Kotlin 2.3.21 / KSP 2.3.7 / Ktor 3.4.3. IDE: IntelliJ IDEA 2026.1.2+ with dedicated plugin. **No beta/stable date given.**

## Support matrix vs our stack (spec 001)

| Tech | Status | Evidence |
|---|---|---|
| Android app build (manifest/res/DEX/AAB) | ✅ Works | `android/app` product type; drives AGP internally |
| Signing, R8/shrink | ✅ Works | `settings.android.signing`; R8 auto on release (AMPER-891 resolved) |
| compileSdk/targetSdk 36 | ✅ Works | Full schema; default compileSdk now 37 |
| Compose (Android + Multiplatform) | ✅ Works | `settings.compose: enabled`; own `$compose` catalog — **not** the official Compose BOM artifact |
| KSP | ✅ First-class (KSP2) | `settings.kotlin.ksp.processors` |
| Room | ✅ Android / ❌ iOS-KMP | Doc example uses room-compiler; AMPER-4398 open: Room KSP crashes on iOS targets |
| kotlinx.serialization | ✅ Built-in | Compiler plugin auto-applied |
| Ktor | ✅ Built-in | |
| KMP shared module | ✅ with caveats | Source layout is `src@android/`, `src@ios/` — NOT commonMain/androidMain (porting effort); Xcode framework export one-directional |
| **Hilt / Dagger** | ❌ **Absent** | Zero YouTrack issues even *requesting* it. Hilt's Gradle plugin does an ASM bytecode transform (`@AndroidEntryPoint` → `Hilt_*` base classes) — no analog exists. AMPER-5095 (third-party *compiler* plugins) doesn't cover it. JetBrains' supported DI: **Koin** (AMPER-5110 resolved) |
| Build variants | ⚠️ debug/release only | **No product flavors** — no mechanism at all |
| packagingOptions/excludes | ⚠️ Incomplete | AMPER-5488 open (unresolvable META-INF merge conflicts); AMPER-5393 open — triggered by OkHttp logging-interceptor, adjacent to our Ktor-OkHttp path |
| Navigation 3 | ❓ Unverified | Plain dependency, should resolve; no evidence exercised |
| DataStore / androidx misc | ❓ Presumed fine | No breakage found |
| JUnit4 | ✅ Works | `settings.junit: junit-4` |
| Robolectric | ❓ No evidence | Zero doc/tracker mentions |
| Compose UI tests | ❓ No evidence | Zero hits |
| Screenshot testing | ❌ Likely unsupported | Needs its own Gradle plugin/rendering server; no third-party Gradle plugin mechanism exists |
| Instrumented tests | ❓ Undocumented | Only generic `test/` folder |
| Version catalogs | ⚠️ Partial | Reads `libs.versions.toml` `[versions]`/`[libraries]` only — no `[bundles]`/`[plugins]` |
| **Android Studio** | ❌ **Explicitly not prioritized** | Maintainer statement (AMPER-670): IntelliJ is the supported IDE. No Layout Inspector/APK Analyzer/Device Manager workflows |
| `android` CLI + android/skills | ❌ Incompatible | All assume Gradle+AGP project shape; Toolchain has own `kotlin build/run` (with open device-selection bugs, AMPER-5480) |

## Migration & escape hatch

- Gradle→Toolchain migration guide: **doesn't exist** (AMPER-4721 open since Oct 2025; only Maven guide published)
- Coexistence in one repo: unsupported
- Escape hatch back: none — hand-rewrite `build.gradle.kts`. **One-way door.**

## Community & Google

- GitHub footprint thin (284 stars, 8 issues); real activity in JetBrains YouTrack (actively triaged)
- No meaningful independent Android-community reaction found; no adoption signal
- Google: silent on it; investing in the opposite direction (AGP 9 built-in Kotlin). Efforts uncoordinated.

## Verdict for this project

Blockers, by severity: **(1) no Hilt path** (would force DI rewrite to Koin), **(2) no Android Studio**, (3) no flavors + open packaging-conflict bugs adjacent to our Ktor/OkHttp stack, (4) testing story unverified for 3 of our 5 test layers, (5) one-way door with no migration guide either direction.

Ironically its strengths (Ktor, serialization, KSP, KMP, Compose) map to our `:shared` module — but our `:app` half is exactly where it fails.

**Estimate: 12+ months minimum** from viability for this app, and only if JetBrains prioritizes Android Studio + a Hilt-equivalent — neither currently signaled. Re-check at each beta milestone; any experimentation belongs on a throwaway branch.
