# Design System (`:core:ui`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `:core:ui` — the Bro Material 3 design system: dark-first theme, full token layer, Material Symbols icon system, and the stateless component catalog (core / forms / feedback), all screenshot-tested, plus the companion `design-system` project skill. Chat surfaces are built in `:feature:conversation` under spec 005, not here.

**Architecture:** Two-tier color (internal primitives → two full `ColorScheme`s); standard tokens through `MaterialTheme`; `ExtendedColors` — the one token set that varies by scheme — through a CompositionLocal read via the `ChatbotExtendedTheme` accessor, while the constant `Spacing`, `Elevation`, `ChatbotShapes`, `Motion` and `MonoTextStyle` sets are plain objects read directly. Components are stateless wrappers over `androidx.compose.material3` (aliased `M3*` inside `:core:ui` only). Spec: `specs/002-design-system.md`. Component prop contracts were pulled from the upstream **Bro Design System** Claude Design project (projectId `c5a6030b-52d3-4ecc-ab51-4460eebdc7df`, via `pull-design` skill) on 2026-07-18 and are baked into the signatures below.

**Tech Stack:** Kotlin 2.4.10, Compose BOM 2026.06.01, Material 3 (stable APIs only), Robolectric 4.16.1 (SDK 36, JDK 21 launcher), Compose test rule **v2**, Compose Preview Screenshot Testing plugin `0.0.1-alpha15` (latest on Google Maven as of 2026-07-18; Roborazzi is the documented fallback if it blocks).

## Global Constraints

- **Never commit or push** (CLAUDE.md — overrides the superpowers commit steps). Each task ends with tests green and changes left in the working tree; report what's ready.
- Tick each step's checkbox (`- [ ]` → `- [x]`) in this plan file as you finish and verify it.
- Execute the plan on the current git branch — no worktree, no new branch.
- Module: `:core:ui`, package `com.shayanaryan.chatbot.core.ui`. The Bro Design System (theme, icons, catalog, previews) nests under `com.shayanaryan.chatbot.core.ui.designsystem.*`; the `…core.ui` top level stays empty for future non-DS core:ui code. Android library, **not** KMP. Depends only on Compose BOM, Material 3, icon/screenshot tooling — never on `:shared` or feature modules.
- Dark is the default scheme; light is a full opt-in scheme. **No dynamic color** (no `dynamicColor` parameter), no gradients, flat tonal fills; shadow reserved for FAB/menus/dialogs/heads-up notification.
- Naming boundary: components keep M3 names (`Button`, `Icon`, `Card`, …). Wrapper files inside `:core:ui` alias the original (`import androidx.compose.material3.Button as M3Button`). Feature modules must import components only from `:core:ui`.
- All components stateless/presentational: state in via parameters, events out via lambdas.
- **No hardcoded user-visible text in component source.** Any string a user reads or TalkBack speaks — labels, content descriptions, state descriptions — comes from `core/ui/src/main/res/values/strings.xml` via `stringResource(...)`, never a Kotlin literal. Per the architecture skill, `:core:ui` owns only *generic* strings; caller-supplied copy (dialog titles, button labels, error messages) stays a parameter. Material Symbols ligature names are glyph identifiers rather than text, so they don't go in `strings.xml` — but they don't belong at call sites either: every one is a constant on `Glyphs` (Task 6), the only file where the literal appears. Preview and test sources use literal sample *copy* freely, but still take glyphs from `Glyphs`.
- Every component ships `@PreviewTest` previews for its variants in **both** dark and light (in the `screenshotTest` source set — the golden coverage).
- **Colocated previews (CLAUDE.md rule).** Separately from the `screenshotTest` goldens, every `public @Composable` also ships at least one plain `@Preview` in its **own source file**, one per distinct visual state, each wrapped in `ChatbotTheme` (see `icon/Icon.kt`). This is the IDE preview and is required by CLAUDE.md; the `@PreviewTest` set does not satisfy it.
- Compose test rule: `import androidx.compose.ui.test.junit4.v2.createComposeRule` (the parent-package name is the deprecated v1 rule).
- No MockK or mocking libraries — fakes and real objects only.
- Never comment in the code: //last synced from DS on {date}
- TDD: red → green → refactor for every step.
- Formatting is a gate: run `./gradlew :core:ui:spotlessApply` before finishing each task; `spotlessCheck` must pass.
- Emoji are never UI icons. No PNG/SVG icon assets — the single bundled variable font only.
- Token values below are copied from `specs/002-design-system.md` (the spec's in-repo M1 reference tables) and the upstream pull on 2026-07-18. Downstream Kotlin files carry a `last synced from Bro DS, 2026-07-18` provenance comment.

## File Structure

All under `core/ui/src/` unless noted. Package root `com.shayanaryan.chatbot.core.ui`; the entire Bro Design System nests under `…core.ui.designsystem` (theme/icon/component/preview). The `…core.ui` top level is intentionally left empty for now — future reusable-but-not-DS core:ui composables (custom modifiers, layout helpers, generic wrappers) land there beside `designsystem`, not inside it. Android resources (font, strings) and the generated `R` class are **not** package-nested — they stay at the module root `core/ui/src/main/res/` and `com.shayanaryan.chatbot.core.ui.R`.

```
main/kotlin/.../core/ui/designsystem/
  theme/Color.kt              primitives (internal) + Dark/LightColorScheme   (replaces AGP-template file)
  theme/Type.kt               15-role Roboto Typography                       (replaces AGP-template file)
  theme/ExtendedColors.kt     ExtendedColors + dark/light instances + Local
  theme/Shape.kt              M3 Shapes + ChatbotShapes (button/chip/card/input/dialog/bubbles), constants
  theme/Spacing.kt            4dp-grid Spacing (constants)
  theme/Elevation.kt          Elevation levels 1–5 (constants)
  theme/Motion.kt             easings, durations, press scales, state-layer opacities (constants)
  theme/Theme.kt              ChatbotTheme composable + ChatbotExtendedTheme accessor (replaces template)
  icon/Icon.kt                Icon composable over the variable font
  icon/Glyphs.kt              brand + UI-chrome glyph constants
  component/Button.kt         Button + ButtonVariant
  component/IconButton.kt     IconButton + IconButtonVariant
  component/Card.kt           Card + CardVariant
  component/Badge.kt          Badge + BadgeTone
  component/BrandMark.kt      "bro" wordmark + forum tile
  component/TextField.kt      TextField + TextFieldVariant
  component/Switch.kt         Switch
  component/Chip.kt           Chip + ChipVariant
  component/Dialog.kt         Dialog
  component/Snackbar.kt       Snackbar
  component/LoadingIndicator.kt
  component/ErrorState.kt
main/res/font/material_symbols_rounded.ttf   (downloaded variable font — module root, not nested)
main/res/values/strings.xml                  generic strings (loading, retry, dismiss, wordmark — module root)
test/kotlin/.../core/ui/designsystem/...     JVM + Robolectric tests (mirrors main packages)
test/resources/robolectric.properties        sdk=36
screenshotTest/kotlin/.../core/ui/designsystem/preview/   @PreviewTest previews, one file per component family
```

Also modified: `core/ui/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `specs/002-design-system.md`, new `.claude/skills/design-system/SKILL.md`.

---

### Task 1: Test harness + color tokens (primitives + both ColorSchemes)

**Files:**
- Modify: `core/ui/build.gradle.kts`
- Create: `core/ui/src/test/resources/robolectric.properties`
- Create (replace): `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Color.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/ColorSchemeTest.kt`

**Interfaces:**
- Consumes: nothing (first task).
- Produces: `internal object ColorPrimitives` (every `val` is a `Color`, named `Orange50`, `Navy10`, `Sand98`, `ScrimDark`, …); `internal val DarkColorScheme: ColorScheme`; `internal val LightColorScheme: ColorScheme`. Later tasks reference primitives via `ColorPrimitives.X` and schemes via these two vals.

- [x] **Step 1: Wire the test harness into `core/ui/build.gradle.kts`**

Mirror the `:feature:conversation` harness exactly (same testOptions, same Java-21 launcher block):

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shayanaryan.chatbot.core.ui"
    compileSdk = 37
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Robolectric needs a Java 21 runtime for SDK 36; compile toolchain stays 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

Create `core/ui/src/test/resources/robolectric.properties` (the `sdk=36` pin is load-bearing — Robolectric 4.16.1 cannot run SDK 37):

```properties
sdk=36
```

- [x] **Step 2: Write the failing test**

`ColorSchemeTest.kt` — plain JUnit (Compose `Color` is JVM-safe, no Robolectric runner needed). Spot-check each ramp plus the full surface stack and alpha scrims; both schemes must fully specify roles:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorSchemeTest {
    @Test
    fun darkSchemePrimaryRolesResolveToOrangePrimitives() {
        assertEquals(ColorPrimitives.Orange50, DarkColorScheme.primary)
        assertEquals(ColorPrimitives.Orange12, DarkColorScheme.onPrimary)
        assertEquals(ColorPrimitives.Orange20, DarkColorScheme.primaryContainer)
        assertEquals(ColorPrimitives.Orange90, DarkColorScheme.onPrimaryContainer)
    }

    @Test
    fun darkSchemeSurfaceStackIsNavy() {
        assertEquals(ColorPrimitives.Navy10, DarkColorScheme.background)
        assertEquals(ColorPrimitives.Navy10, DarkColorScheme.surface)
        assertEquals(ColorPrimitives.Navy06, DarkColorScheme.surfaceContainerLowest)
        assertEquals(ColorPrimitives.Navy14, DarkColorScheme.surfaceContainerLow)
        assertEquals(ColorPrimitives.Navy17, DarkColorScheme.surfaceContainer)
        assertEquals(ColorPrimitives.Navy22, DarkColorScheme.surfaceContainerHigh)
        assertEquals(ColorPrimitives.Navy26, DarkColorScheme.surfaceContainerHighest)
        assertEquals(ColorPrimitives.Navy95, DarkColorScheme.onSurface)
        assertEquals(ColorPrimitives.Navy80, DarkColorScheme.onSurfaceVariant)
        assertEquals(ColorPrimitives.Navy40, DarkColorScheme.outline)
        assertEquals(ColorPrimitives.Navy22, DarkColorScheme.outlineVariant)
    }

    @Test
    fun darkSchemeErrorInverseAndScrim() {
        assertEquals(ColorPrimitives.Red50, DarkColorScheme.error)
        assertEquals(ColorPrimitives.Red12, DarkColorScheme.onError)
        assertEquals(ColorPrimitives.Red20, DarkColorScheme.errorContainer)
        assertEquals(ColorPrimitives.Red70, DarkColorScheme.onErrorContainer)
        assertEquals(ColorPrimitives.ScrimDark, DarkColorScheme.scrim)
        assertEquals(ColorPrimitives.Navy95, DarkColorScheme.inverseSurface)
        assertEquals(ColorPrimitives.Navy14, DarkColorScheme.inverseOnSurface)
        assertEquals(ColorPrimitives.Warm85, DarkColorScheme.secondary)
        assertEquals(ColorPrimitives.Yellow90, DarkColorScheme.tertiary)
    }

    @Test
    fun lightSchemeDarkensHuesForLegibility() {
        assertEquals(ColorPrimitives.Orange40, LightColorScheme.primary)
        assertEquals(ColorPrimitives.White, LightColorScheme.onPrimary)
        assertEquals(ColorPrimitives.Orange90, LightColorScheme.primaryContainer)
        assertEquals(ColorPrimitives.Orange10, LightColorScheme.onPrimaryContainer)
        assertEquals(ColorPrimitives.Yellow35, LightColorScheme.tertiary)
        assertEquals(ColorPrimitives.Red44, LightColorScheme.error)
        assertEquals(ColorPrimitives.Sand98, LightColorScheme.background)
        assertEquals(ColorPrimitives.Sand11, LightColorScheme.onSurface)
        assertEquals(ColorPrimitives.Sand52, LightColorScheme.outline)
        assertEquals(ColorPrimitives.ScrimLight, LightColorScheme.scrim)
        assertEquals(ColorPrimitives.Sand20, LightColorScheme.inverseSurface)
    }
}
```

- [x] **Step 3: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.ColorSchemeTest"`
Expected: FAIL — unresolved references `DarkColorScheme` / `LightColorScheme` (template `Color.kt` only defines `Purple80` etc.). A compilation failure of the test source set is the red state here.

- [x] **Step 4: Replace `theme/Color.kt` with primitives + schemes**

Full file — every primitive from the spec table, named once; roles reference primitives only:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// last synced from Bro DS, 2026-07-18
internal object ColorPrimitives {
    val Orange90 = Color(0xFFFFDCC2)
    val Orange57 = Color(0xFFFFA257)
    val Orange50 = Color(0xFFFF9239)
    val Orange40 = Color(0xFFED6900)
    val Orange30 = Color(0xFFB84E00)
    val Orange20 = Color(0xFF7A3300)
    val Orange12 = Color(0xFF431C00)
    val Orange10 = Color(0xFF401A00)

    val Yellow90 = Color(0xFFFFE494)
    val Yellow35 = Color(0xFF8A6600)
    val Yellow20 = Color(0xFF4A3600)
    val Yellow15 = Color(0xFF2A1E00)

    val Warm95 = Color(0xFFFFDCC2)
    val Warm85 = Color(0xFFE4C0A4)
    val Warm50 = Color(0xFF7A5733)
    val Warm30 = Color(0xFF5A4029)
    val Warm22 = Color(0xFF422A15)
    val Warm18 = Color(0xFF2A1800)

    val Navy06 = Color(0xFF12161F)
    val Navy10 = Color(0xFF1A2130)
    val Navy14 = Color(0xFF212A3B)
    val Navy17 = Color(0xFF263043)
    val Navy22 = Color(0xFF2E394D)
    val Navy26 = Color(0xFF354158)
    val Navy40 = Color(0xFF55617A)
    val Navy60 = Color(0xFF8791A6) // upstream primitive, currently unused by any role
    val Navy80 = Color(0xFFC3CBD9)
    val Navy95 = Color(0xFFE7EFFE)

    val Sand100 = Color(0xFFFFFFFF)
    val Sand98 = Color(0xFFFBF9F7)
    val Sand96 = Color(0xFFF5F2EE)
    val Sand94 = Color(0xFFEFECE7)
    val Sand92 = Color(0xFFE9E5DF)
    val Sand90 = Color(0xFFE3DFD8)
    val Sand70 = Color(0xFFD5C8BA)
    val Sand52 = Color(0xFF837567)
    val Sand32 = Color(0xFF4F4539)
    val Sand20 = Color(0xFF313029)
    val Sand11 = Color(0xFF1C1B19)

    val Green50 = Color(0xFF4ECB7B)
    val Green44 = Color(0xFF1F7A44)
    val Green88 = Color(0xFFB7F0C8)
    val Green20 = Color(0xFF0F4D2A)
    val Green08 = Color(0xFF00210F)

    val Red70 = Color(0xFFFFB3B3)
    val Red50 = Color(0xFFFF6B6B)
    val Red44 = Color(0xFFBA1A1A)
    val Red90 = Color(0xFFFFDAD6)
    val Red20 = Color(0xFF5C1A1A)
    val Red12 = Color(0xFF430E0E)
    val Red05 = Color(0xFF410002)

    val Amber50 = Color(0xFFFFCE54)
    val Amber35 = Color(0xFF8A5A00)

    val White = Color(0xFFFFFFFF)
    val ScrimDark = Color(0xB806090E) // rgba(6,9,14,0.72)
    val ScrimLight = Color(0x661C1B19) // rgba(28,27,25,0.40)
}

internal val DarkColorScheme =
    darkColorScheme(
        primary = ColorPrimitives.Orange50,
        onPrimary = ColorPrimitives.Orange12,
        primaryContainer = ColorPrimitives.Orange20,
        onPrimaryContainer = ColorPrimitives.Orange90,
        secondary = ColorPrimitives.Warm85,
        onSecondary = ColorPrimitives.Warm22,
        secondaryContainer = ColorPrimitives.Warm30,
        onSecondaryContainer = ColorPrimitives.Warm95,
        tertiary = ColorPrimitives.Yellow90,
        onTertiary = ColorPrimitives.Yellow20,
        tertiaryContainer = ColorPrimitives.Yellow20,
        onTertiaryContainer = ColorPrimitives.Yellow90,
        background = ColorPrimitives.Navy10,
        onBackground = ColorPrimitives.Navy95,
        surface = ColorPrimitives.Navy10,
        onSurface = ColorPrimitives.Navy95,
        surfaceContainerLowest = ColorPrimitives.Navy06,
        surfaceContainerLow = ColorPrimitives.Navy14,
        surfaceContainer = ColorPrimitives.Navy17,
        surfaceContainerHigh = ColorPrimitives.Navy22,
        surfaceContainerHighest = ColorPrimitives.Navy26,
        onSurfaceVariant = ColorPrimitives.Navy80,
        outline = ColorPrimitives.Navy40,
        outlineVariant = ColorPrimitives.Navy22,
        error = ColorPrimitives.Red50,
        onError = ColorPrimitives.Red12,
        errorContainer = ColorPrimitives.Red20,
        onErrorContainer = ColorPrimitives.Red70,
        scrim = ColorPrimitives.ScrimDark,
        inverseSurface = ColorPrimitives.Navy95,
        inverseOnSurface = ColorPrimitives.Navy14,
    )

internal val LightColorScheme =
    lightColorScheme(
        primary = ColorPrimitives.Orange40,
        onPrimary = ColorPrimitives.White,
        primaryContainer = ColorPrimitives.Orange90,
        onPrimaryContainer = ColorPrimitives.Orange10,
        secondary = ColorPrimitives.Warm50,
        onSecondary = ColorPrimitives.White,
        secondaryContainer = ColorPrimitives.Warm95,
        onSecondaryContainer = ColorPrimitives.Warm18,
        tertiary = ColorPrimitives.Yellow35,
        onTertiary = ColorPrimitives.White,
        tertiaryContainer = ColorPrimitives.Yellow90,
        onTertiaryContainer = ColorPrimitives.Yellow15,
        background = ColorPrimitives.Sand98,
        onBackground = ColorPrimitives.Sand11,
        surface = ColorPrimitives.Sand98,
        onSurface = ColorPrimitives.Sand11,
        surfaceContainerLowest = ColorPrimitives.Sand100,
        surfaceContainerLow = ColorPrimitives.Sand96,
        surfaceContainer = ColorPrimitives.Sand94,
        surfaceContainerHigh = ColorPrimitives.Sand92,
        surfaceContainerHighest = ColorPrimitives.Sand90,
        onSurfaceVariant = ColorPrimitives.Sand32,
        outline = ColorPrimitives.Sand52,
        outlineVariant = ColorPrimitives.Sand70,
        error = ColorPrimitives.Red44,
        onError = ColorPrimitives.White,
        errorContainer = ColorPrimitives.Red90,
        onErrorContainer = ColorPrimitives.Red05,
        scrim = ColorPrimitives.ScrimLight,
        inverseSurface = ColorPrimitives.Sand20,
        inverseOnSurface = ColorPrimitives.Sand96,
    )
```

Note: this deletes `Purple80`/`Purple40` etc. — `Theme.kt` still references them and won't compile until Task 4 replaces it. To keep the module green during Tasks 1–3, replace the body of `Theme.kt`'s scheme vals now with a minimal shim:

```kotlin
private val DarkScheme = DarkColorScheme
private val LightScheme = LightColorScheme
```

i.e. edit `Theme.kt` to drop the `Purple*`-based `darkColorScheme`/`lightColorScheme` blocks and the `dynamicColor` branches, selecting `if (darkTheme) DarkColorScheme else LightColorScheme` directly. (Task 4 rewrites the file fully anyway; this keeps every intermediate state compiling.)

- [x] **Step 5: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.ColorSchemeTest"`
Expected: PASS (4 tests). Also run `./gradlew :core:ui:assembleDebug :app:assembleDebug` — whole tree must still compile.

- [x] **Step 6: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck`
Expected: BUILD SUCCESSFUL. Leave changes in the working tree.

---

### Task 2: Typography (15-role Roboto scale + mono style)

**Files:**
- Create (replace): `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Type.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/TypographyTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `internal val ChatbotTypography: Typography` (all 15 M3 roles) and `val MonoTextStyle: TextStyle`, both in `Type.kt`. Task 4 installs the M3 scale; components read `MaterialTheme.typography` and `MonoTextStyle`.

- [x] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun displayAndHeadlineAreRegularWeight() {
        assertEquals(57.sp, ChatbotTypography.displayLarge.fontSize)
        assertEquals(64.sp, ChatbotTypography.displayLarge.lineHeight)
        assertEquals(FontWeight.Normal, ChatbotTypography.displayLarge.fontWeight)
        assertEquals(32.sp, ChatbotTypography.headlineLarge.fontSize)
        assertEquals(40.sp, ChatbotTypography.headlineLarge.lineHeight)
        assertEquals(FontWeight.Normal, ChatbotTypography.headlineLarge.fontWeight)
    }

    @Test
    fun titleAndLabelAreMediumWeight() {
        assertEquals(22.sp, ChatbotTypography.titleLarge.fontSize)
        assertEquals(FontWeight.Medium, ChatbotTypography.titleLarge.fontWeight)
        assertEquals(16.sp, ChatbotTypography.titleMedium.fontSize)
        assertEquals(0.15.sp, ChatbotTypography.titleMedium.letterSpacing)
        assertEquals(14.sp, ChatbotTypography.labelLarge.fontSize)
        assertEquals(0.1.sp, ChatbotTypography.labelLarge.letterSpacing)
        assertEquals(FontWeight.Medium, ChatbotTypography.labelSmall.fontWeight)
        assertEquals(11.sp, ChatbotTypography.labelSmall.fontSize)
    }

    @Test
    fun bodyMetricsMatchSpec() {
        assertEquals(16.sp, ChatbotTypography.bodyLarge.fontSize)
        assertEquals(24.sp, ChatbotTypography.bodyLarge.lineHeight)
        assertEquals(0.5.sp, ChatbotTypography.bodyLarge.letterSpacing)
        assertEquals(14.sp, ChatbotTypography.bodyMedium.fontSize)
        assertEquals(0.25.sp, ChatbotTypography.bodyMedium.letterSpacing)
        assertEquals(12.sp, ChatbotTypography.bodySmall.fontSize)
        assertEquals(0.4.sp, ChatbotTypography.bodySmall.letterSpacing)
    }

    @Test
    fun monoStyleIsMonospaceFourteenSp() {
        assertEquals(FontFamily.Monospace, MonoTextStyle.fontFamily)
        assertEquals(14.sp, MonoTextStyle.fontSize)
        assertEquals(20.sp, MonoTextStyle.lineHeight)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.TypographyTest"`
Expected: FAIL — unresolved reference `ChatbotTypography` (template file defines `Typography` val with defaults only).

- [x] **Step 3: Replace `Type.kt` (M3 scale + `MonoTextStyle`)**

`Type.kt` — Roboto is Compose's `FontFamily.Default` on Android, so no font asset. Tracking note (verified against upstream `tokens/typography.css`, 2026-07-18): sizes/line-heights match upstream exactly; for letter-spacing we keep the M3-exact values where the CSS port is lossy — `displayLarge` (-0.25).sp (upstream omits tracking) and `labelMedium`/`labelSmall` 0.5.sp (upstream reuses body-small's `0.03333em`, a web artifact ≈0.4px). Full file:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// last synced from Bro DS, 2026-07-18
internal val ChatbotTypography =
    Typography(
        displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
        displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
        headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
```

If the old file declared `val Typography`, delete that declaration entirely (Task 4 rewires `Theme.kt`; until then keep the `Theme.kt` reference compiling by pointing it at `ChatbotTypography`).

`MonoTextStyle` lives at the bottom of `Type.kt` — one constant style, so no holder class and no Local. Decision recorded: `FontFamily.Monospace` (resolves to the device monospace, Roboto-Mono-class); bundling an actual Roboto Mono asset is deferred until fidelity demands it:

```kotlin
/** The one style outside the M3 scale — API keys, model ids, code. */
val MonoTextStyle =
    TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.TypographyTest"`
Expected: PASS (4 tests).

- [x] **Step 5: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all module tests green. Leave in tree.

---

### Task 3: Non-M3 token holders — Spacing, Shapes, Elevation, Motion, ExtendedColors

**Files:**
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Spacing.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Shape.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Elevation.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Motion.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/ExtendedColors.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/DesignTokensTest.kt`

**Interfaces:**
- Consumes: `ColorPrimitives` (Task 1).
- Produces (Task 4 installs the one Local; components read constants directly and extended colors via `ChatbotExtendedTheme`):
  - `object Spacing` — `none/xxs/xs/sm/md/lg/xl/xxl/x3l/x4l/x5l: Dp` = 0/4/8/12/16/20/24/32/40/48/64, plus `gutter: Dp = md`. Constant across devices and themes, so plain constants with no CompositionLocal — readable outside composition. No `minTouchTarget` token: use `Modifier.minimumInteractiveComponentSize()`, which expands the touch area without changing visual layout.
  - `internal val ChatbotM3Shapes: Shapes` (xs 4 / sm 8 / md 12 / lg 16 / xl 28); `object ChatbotShapes` — `button/chip: Shape` (pill), `card`/`input`/`dialog: Shape` resolving to `ChatbotM3Shapes.medium`/`.extraSmall`/`.extraLarge` rather than restating 12/4/28, `bubbleUser/bubbleAssistant: Shape` (20 with 4dp squared tail: bottom-end for user, bottom-start for assistant). Constant, so no CompositionLocal.
  - `object Elevation` — `level1..level5: Dp` = 1/3/6/8/12. Same ramp in both schemes (depth in dark comes from the tonal `surfaceContainer*` roles, not different shadow values), so plain constants with no CompositionLocal.
  - `object Motion` — easings, durations (150/250/400), `pressScaleButton = 0.97f`, `pressScaleIconButton = 0.90f`, state-layer opacities (0.08/0.10/0.12), `caretBlinkMillis = 1000`. Constant, so no CompositionLocal. (Honouring the system reduce-motion setting is not in scope; if it is ever specced, this becomes a Local again so durations can be zeroed in one place.)
  - `@Immutable class ExtendedColors` — `success/onSuccess/successContainer/warning/primaryHover/primaryPressed: Color`; `internal val DarkExtendedColors/LightExtendedColors`; `LocalExtendedColors`.

- [x] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignTokensTest {
    @Test
    fun spacingFollowsFourDpGrid() {
        assertEquals(0.dp, Spacing.none)
        assertEquals(4.dp, Spacing.xxs)
        assertEquals(8.dp, Spacing.xs)
        assertEquals(12.dp, Spacing.sm)
        assertEquals(16.dp, Spacing.md)
        assertEquals(20.dp, Spacing.lg)
        assertEquals(24.dp, Spacing.xl)
        assertEquals(32.dp, Spacing.xxl)
        assertEquals(40.dp, Spacing.x3l)
        assertEquals(48.dp, Spacing.x4l)
        assertEquals(64.dp, Spacing.x5l)
        assertEquals(16.dp, Spacing.gutter)
    }

    @Test
    fun componentShapesMatchSpec() {
        assertEquals(CircleShape, ChatbotShapes.button)
        assertEquals(CircleShape, ChatbotShapes.chip)
        assertEquals(RoundedCornerShape(12.dp), ChatbotShapes.card)
        assertEquals(RoundedCornerShape(4.dp), ChatbotShapes.input)
        assertEquals(RoundedCornerShape(28.dp), ChatbotShapes.dialog)
        assertEquals(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp), ChatbotShapes.bubbleUser)
        assertEquals(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp), ChatbotShapes.bubbleAssistant)
    }

    @Test
    fun motionDurationsScalesAndStateLayers() {
        assertEquals(150, Motion.durationShortMillis)
        assertEquals(250, Motion.durationMediumMillis)
        assertEquals(400, Motion.durationLongMillis)
        assertEquals(0.97f, Motion.pressScaleButton, 0f)
        assertEquals(0.90f, Motion.pressScaleIconButton, 0f)
        assertEquals(0.08f, Motion.stateLayerHover, 0f)
        assertEquals(0.10f, Motion.stateLayerFocus, 0f)
        assertEquals(0.12f, Motion.stateLayerPressed, 0f)
        assertEquals(1000, Motion.caretBlinkMillis)
    }

    @Test
    fun elevationLevelsAreM3Dps() {
        assertEquals(1.dp, Elevation.level1)
        assertEquals(3.dp, Elevation.level2)
        assertEquals(6.dp, Elevation.level3)
        assertEquals(8.dp, Elevation.level4)
        assertEquals(12.dp, Elevation.level5)
    }

    @Test
    fun extendedColorsResolvePerScheme() {
        assertEquals(ColorPrimitives.Green50, DarkExtendedColors.success)
        assertEquals(ColorPrimitives.Green08, DarkExtendedColors.onSuccess)
        assertEquals(ColorPrimitives.Green20, DarkExtendedColors.successContainer)
        assertEquals(ColorPrimitives.Amber50, DarkExtendedColors.warning)
        assertEquals(ColorPrimitives.Orange57, DarkExtendedColors.primaryHover)
        assertEquals(ColorPrimitives.Orange40, DarkExtendedColors.primaryPressed)
        assertEquals(ColorPrimitives.Green44, LightExtendedColors.success)
        assertEquals(ColorPrimitives.White, LightExtendedColors.onSuccess)
        assertEquals(ColorPrimitives.Green88, LightExtendedColors.successContainer)
        assertEquals(ColorPrimitives.Amber35, LightExtendedColors.warning)
        assertEquals(ColorPrimitives.Orange30, LightExtendedColors.primaryHover)
        assertEquals(ColorPrimitives.Orange20, LightExtendedColors.primaryPressed)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.DesignTokensTest"`
Expected: FAIL — unresolved references (`Spacing`, `ChatbotShapes`, `Motion`, `Elevation`, `DarkExtendedColors`).

- [x] **Step 3: Create the five token files**

`Spacing.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp-grid spacing scale. `gutter` is the default screen gutter.
 *
 * These values are constant across devices and themes, so they are plain constants rather than
 * CompositionLocal-backed tokens — readable from non-composable code (draw scopes, previews,
 * test fixtures) with no theme lookup.
 *
 * Minimum touch targets are not a token here: use `Modifier.minimumInteractiveComponentSize()`,
 * which expands the touch area without altering visual layout.
 */
object Spacing {
    val none: Dp = 0.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val x3l: Dp = 40.dp
    val x4l: Dp = 48.dp
    val x5l: Dp = 64.dp
    val gutter: Dp = md
}
```

`Shape.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// last synced from Bro DS, 2026-07-18
internal val ChatbotM3Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

/**
 * Named component shapes. Bubble tail (4dp squared corner) sits bottom-end for user, bottom-start
 * for assistant.
 *
 * Shapes that exist on the M3 scale resolve to it rather than restating the radius, so a named
 * shape and the M3 role it corresponds to cannot drift apart — M3 components read
 * `MaterialTheme.shapes` internally while our wrappers read these. Only `button`/`chip` and the
 * bubbles are additive.
 *
 * Constant across schemes and devices, so plain constants rather than CompositionLocal-backed
 * tokens — readable outside composition, no theme lookup.
 */
object ChatbotShapes {
    val button: Shape = CircleShape
    val chip: Shape = CircleShape
    val card: Shape = ChatbotM3Shapes.medium
    val input: Shape = ChatbotM3Shapes.extraSmall
    val dialog: Shape = ChatbotM3Shapes.extraLarge
    val bubbleUser: Shape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
    val bubbleAssistant: Shape =
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
}
```

`Elevation.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shadow elevation levels — reserved for FAB, menus, dialogs, heads-up notification. Cards stay flat.
 *
 * The same ramp applies in both schemes: depth in dark is carried by the tonal `surfaceContainer*`
 * roles of the `ColorScheme`, not by different shadow values. Constant, so plain constants rather
 * than CompositionLocal-backed tokens — readable outside composition, no theme lookup.
 */
object Elevation {
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}
```

`Motion.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

// last synced from Bro DS, 2026-07-18
object Motion {
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val easingEmphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val easingDecelerate: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)
    val easingAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val durationShortMillis: Int = 150
    val durationMediumMillis: Int = 250
    val durationLongMillis: Int = 400
    val pressScaleButton: Float = 0.97f
    val pressScaleIconButton: Float = 0.90f
    val stateLayerHover: Float = 0.08f
    val stateLayerFocus: Float = 0.10f
    val stateLayerPressed: Float = 0.12f
    val caretBlinkMillis: Int = 1000
}
```

`ExtendedColors.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colors M3 has no slot for. */
@Immutable
class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
)

// last synced from Bro DS, 2026-07-18
internal val DarkExtendedColors =
    ExtendedColors(
        success = ColorPrimitives.Green50,
        onSuccess = ColorPrimitives.Green08,
        successContainer = ColorPrimitives.Green20,
        warning = ColorPrimitives.Amber50,
        primaryHover = ColorPrimitives.Orange57,
        primaryPressed = ColorPrimitives.Orange40,
    )

internal val LightExtendedColors =
    ExtendedColors(
        success = ColorPrimitives.Green44,
        onSuccess = ColorPrimitives.White,
        successContainer = ColorPrimitives.Green88,
        warning = ColorPrimitives.Amber35,
        primaryHover = ColorPrimitives.Orange30,
        primaryPressed = ColorPrimitives.Orange20,
    )

internal val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.DesignTokensTest"`
Expected: PASS (5 tests).

- [x] **Step 5: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 4: `ChatbotTheme` entry + `ChatbotExtendedTheme` accessor

**Files:**
- Create (replace): `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/Theme.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/ChatbotThemeTest.kt`

**Interfaces:**
- Consumes: `DarkColorScheme`/`LightColorScheme` (Task 1), `ChatbotTypography` (Task 2), the Task 3 token sets — `ExtendedColors` via its Local, `Spacing`/`Elevation`/`ChatbotShapes`/`Motion` as constants.
- Produces (the public theme API every later task and feature module uses):

```kotlin
@Composable fun ChatbotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)

object ChatbotExtendedTheme {
    val colors: ExtendedColors @Composable get
}
```

The accessor object is named apart from the `ChatbotTheme` composable so the two never read ambiguously at a call site. No `dynamicColor` parameter exists. Edge-to-edge is applied at app level, not here. `MainActivity` already calls `ChatbotTheme { … }`, so no `:app` change is needed.

- [x] **Step 1: Write the failing test** (Robolectric + Compose rule v2)

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatbotThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkThemeInstallsDarkSchemeAndExtendedColors() {
        var primary = Color.Unspecified
        var success = Color.Unspecified
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                success = ChatbotExtendedTheme.colors.success
            }
        }
        assertEquals(ColorPrimitives.Orange50, primary)
        assertEquals(ColorPrimitives.Green50, success)
    }

    @Test
    fun lightThemeSwitchesSchemesAndExtendedColors() {
        var primary = Color.Unspecified
        var success = Color.Unspecified
        composeRule.setContent {
            ChatbotTheme(darkTheme = false) {
                primary = MaterialTheme.colorScheme.primary
                success = ChatbotExtendedTheme.colors.success
            }
        }
        assertEquals(ColorPrimitives.Orange40, primary)
        assertEquals(ColorPrimitives.Green44, success)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotThemeTest"`
Expected: FAIL — `ChatbotExtendedTheme.colors` unresolved (accessor object doesn't exist yet).

- [x] **Step 3: Replace `Theme.kt`**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun ChatbotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChatbotTypography,
            shapes = ChatbotM3Shapes,
            content = content,
        )
    }
}

/**
 * Accessors for tokens which M3 has no slot for and whose value depends on the active scheme.
 * Standard tokens come from [MaterialTheme]; constant token sets ([Spacing], [Elevation],
 * [ChatbotShapes], [Motion], [MonoTextStyle]) are read directly.
 */
object ChatbotExtendedTheme {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}
```

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotThemeTest"`
Expected: PASS (2 tests). Then `./gradlew :app:assembleDebug` — MainActivity call site still compiles (it never passed `dynamicColor`).

- [x] **Step 5: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.


### Task 5: Screenshot-test infrastructure + first previews

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `gradle.properties`
- Modify: `core/ui/build.gradle.kts`
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/ThemePreviews.kt`

**Interfaces:**
- Consumes: `ChatbotTheme` (Task 4).
- Produces: the `screenshotTest` source set + `updateDebugScreenshotTest` / `validateDebugScreenshotTest` tasks. Every later task adds `@PreviewTest` previews under `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/` and re-runs record + validate. Convention: one preview file per component family; every preview wraps content in `ChatbotTheme(darkTheme = …)` with a `Surface`; names end `…DarkPreview` / `…LightPreview`.

- [x] **Step 1: Wire the plugin**

`gradle/libs.versions.toml` — add to `[versions]`:

```toml
screenshot = "0.0.1-alpha15"
```

and to `[plugins]`:

```toml
screenshot = { id = "com.android.compose.screenshot", version.ref = "screenshot" }
```

and to `[libraries]` (alpha15 does not auto-add the `@PreviewTest` annotation to the `screenshotTest` classpath — it must be an explicit dependency):

```toml
screenshot-validation-api = { group = "com.android.tools.screenshot", name = "screenshot-validation-api", version.ref = "screenshot" }
```

`gradle.properties` — append (required by the preview screenshot tool while it's experimental):

```properties
# Compose Preview Screenshot Testing (spec 002)
android.experimental.enableScreenshotTest=true
```

`core/ui/build.gradle.kts` — add to `plugins {}`:

```kotlin
    alias(libs.plugins.screenshot)
```

to the `android {}` block. AGP 9.3 has **two separate gates**: the plugin's `apply` step reads `android.experimental.enableScreenshotTest` from `gradle.properties`, while the library's `screenshotTest` source-set creation reads the module-level `experimentalProperties` map (which is *not* populated from the global project property). Both are required — dropping either fails configuration:

```kotlin
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
```

and to `dependencies {}` — only the annotation artifact. `ui-tooling` (`debugImplementation`) and `ui-tooling-preview` (`implementation`) are already on the `screenshotTest` classpath (it extends the debug variant's configs), so re-declaring them as `screenshotTestImplementation` is redundant and the IDE flags it:

```kotlin
    screenshotTestImplementation(libs.screenshot.validation.api)
```

- [x] **Step 2: Write the first preview (red = validate fails with no reference images)**

`ThemePreviews.kt` — a token swatch proving theme + both schemes render:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotExtendedTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.MonoTextStyle
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

@Composable
private fun ThemeSwatch() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            Text("Display", style = MaterialTheme.typography.displaySmall)
            Text("Title", style = MaterialTheme.typography.titleMedium)
            Text("Body", style = MaterialTheme.typography.bodyMedium)
            Text("api-key-0000", style = MonoTextStyle)
            Box(
                Modifier
                    .size(Spacing.x4l)
                    .background(MaterialTheme.colorScheme.primary, ChatbotShapes.card),
            )
            Box(
                Modifier
                    .size(Spacing.x4l)
                    .background(ChatbotExtendedTheme.colors.success, ChatbotShapes.card),
            )
        }
    }
}

@PreviewTest
@Preview(name = "theme-dark")
@Composable
private fun ThemeSwatchDarkPreview() {
    ChatbotTheme(darkTheme = true) { ThemeSwatch() }
}

@PreviewTest
@Preview(name = "theme-light")
@Composable
private fun ThemeSwatchLightPreview() {
    ChatbotTheme(darkTheme = false) { ThemeSwatch() }
}
```

- [x] **Step 3: Record reference images**

Run: `./gradlew :core:ui:updateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL; reference PNGs generated under `core/ui/src/debug/screenshotTest/reference/` (path is tool-managed — accept wherever alpha15 writes them; they are checked-in goldens).

- [x] **Step 4: Validate**

Run: `./gradlew :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL, 2 screenshots verified (HTML report under `core/ui/build/reports/screenshotTest/`).

**Fallback trigger (from spec/tech-stack):** if alpha15 fails against AGP 9.3/Kotlin 2.4 after one honest debugging pass, stop, remove the plugin wiring, and swap to Roborazzi — raise this at the task review checkpoint before proceeding; do not burn time forcing the preview tool.

- [x] **Step 5: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree (including the recorded goldens).

---

### Task 6: Icon system — variable font, `Icon`, glyph constants

**Files:**
- Create: `core/ui/src/main/res/font/material_symbols_rounded.ttf` (downloaded)
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/icon/Glyphs.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/icon/Icon.kt`
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/IconPreviews.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/icon/IconTest.kt`

**Interfaces:**
- Consumes: `ChatbotTheme` (Task 4).
- Produces:

```kotlin
object Glyphs {
    const val BRAND = "forum"
    const val CLOSE = "close"
    const val ERROR = "error"
    const val ARROW_FORWARD = "arrow_forward"
}

@Composable fun Icon(
    glyph: String,
    contentDescription: String?,   // null = decorative
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,       // FILL axis 0 → 1
    weight: Int = 400,             // wght axis
    grade: Int = 0,                // GRAD axis
    tint: Color = LocalContentColor.current,
)
```

All later components render glyphs through this `Icon`.

- [x] **Step 1: Download the font**

The source project loads Material Symbols Rounded from the Google Fonts CDN; we bundle the same variable font in-APK (axes FILL, GRAD, opsz, wght). Res font names must be lowercase:

```bash
mkdir -p core/ui/src/main/res/font
curl -fL -o core/ui/src/main/res/font/material_symbols_rounded.ttf \
  "https://github.com/google/material-design-icons/raw/master/variablefont/MaterialSymbolsRounded%5BFILL%2CGRAD%2Copsz%2Cwght%5D.ttf"
file core/ui/src/main/res/font/material_symbols_rounded.ttf
```

Expected: `TrueType font data`, size roughly 5–12 MB. (Subsetting to used glyphs is an explicitly deferred APK-size optimization — do not subset now.) If the GitHub raw URL 404s, fetch the same file from https://github.com/google/material-design-icons (`variablefont/` directory) — verify the name still matches the four axes.

- [x] **Step 2: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.icon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun iconExposesContentDescriptionNotLigatureText() {
        composeRule.setContent {
            ChatbotTheme { Icon(glyph = Glyphs.CLOSE, contentDescription = "Close") }
        }
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        // Ligature text must not leak into semantics — TalkBack would read the raw glyph name.
        composeRule.onNodeWithText(Glyphs.CLOSE).assertDoesNotExist()
    }

    @Test
    fun glyphConstantsMatchSpec() {
        assertEquals("forum", Glyphs.BRAND)
        assertEquals("close", Glyphs.CLOSE)
        assertEquals("error", Glyphs.ERROR)
        assertEquals("arrow_forward", Glyphs.ARROW_FORWARD)
    }
}
```

- [x] **Step 3: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.icon.IconTest"`
Expected: FAIL — unresolved `Icon` / `Glyphs`.

- [x] **Step 4: Implement**

`Glyphs.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.icon

/**
 * Material Symbols Rounded ligature names used across the app.
 *
 * Every glyph a component renders is named here — a bare `"close"` at a call site is a typo away
 * from a missing symbol that no compiler catches, and the ligature name is the one string in the
 * icon system that a wrong value fails silently on. Feature modules add their own constants here
 * as screens need them.
 */
object Glyphs {
    // Brand.
    const val BRAND = "forum"

    // UI chrome.
    const val CLOSE = "close"
    const val ERROR = "error"
    const val ARROW_FORWARD = "arrow_forward"
}
```

`Icon.kt` — glyphs render as ligature text in the variable font; axes are set per use via `FontVariation`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.icon

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R

@Composable
fun Icon(
    glyph: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    filled: Boolean = false,
    weight: Int = 400,
    grade: Int = 0,
    tint: Color = LocalContentColor.current,
) {
    val fontFamily =
        remember(filled, weight, grade, size) {
            FontFamily(
                Font(
                    R.font.material_symbols_rounded,
                    variationSettings =
                        FontVariation.Settings(
                            FontVariation.Setting("FILL", if (filled) 1f else 0f),
                            FontVariation.Setting("wght", weight.toFloat()),
                            FontVariation.Setting("GRAD", grade.toFloat()),
                            FontVariation.Setting("opsz", size.value),
                        ),
                ),
            )
        }
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Text(
        text = glyph,
        modifier =
            modifier.clearAndSetSemantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            },
        color = tint,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
    )
}
```

- [x] **Step 5: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.icon.IconTest"`
Expected: PASS (2 tests).

- [x] **Step 6: Previews + screenshots**

`IconPreviews.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
private fun IconRow() {
    Surface {
        Row(Modifier.padding(Spacing.md)) {
            Icon(Glyphs.CLOSE, contentDescription = null)
            Icon(Glyphs.ERROR, contentDescription = null, filled = true)
            Icon(Glyphs.ARROW_FORWARD, contentDescription = null, weight = 600)
            Icon(Glyphs.BRAND, contentDescription = null, size = 40.dp, filled = true)
        }
    }
}

@PreviewTest
@Preview(name = "icons-dark")
@Composable
private fun IconRowDarkPreview() {
    ChatbotTheme(darkTheme = true) { IconRow() }
}

@PreviewTest
@Preview(name = "icons-light")
@Composable
private fun IconRowLightPreview() {
    ChatbotTheme(darkTheme = false) { IconRow() }
}
```

Run: `./gradlew :core:ui:updateDebugScreenshotTest :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL. **Inspect the new goldens by eye**: glyphs must render as symbols, not ligature text — if you see the literal word "close", the variable font didn't load and the task is not done.

- [x] **Step 7: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 7: Core components I — `Button`, `IconButton`

**Files:**
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Button.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/IconButton.kt`
- Create: `core/ui/src/main/res/values/strings.xml` (the module's complete generic-string set — Tasks 8/9/10 consume it, none add to it)
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/ButtonPreviews.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/ButtonTest.kt`

**Interfaces:**
- Consumes: `Icon`, `Glyphs` (Task 6); `ChatbotShapes.button`, `Motion` (Tasks 3–4).
- Produces:

```kotlin
enum class ButtonVariant { Filled, Tonal, Outlined, Text, Elevated }
@Composable fun Button(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled, enabled: Boolean = true,
    loading: Boolean = false,
    leadingGlyph: String? = null, trailingGlyph: String? = null,
)

enum class IconButtonVariant { Standard, Filled, Tonal, Outlined }
@Composable fun IconButton(
    glyph: String, contentDescription: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, variant: IconButtonVariant = IconButtonVariant.Standard,
    enabled: Boolean = true, selected: Boolean = false,  // selected renders the glyph filled
)
```

Upstream contract deltas, decided here: the web `size` prop (small/medium/large) and `fullWidth` are dropped — spec 002's catalog lists variants only; width comes from `Modifier`. `ariaLabel` becomes required `contentDescription` on `IconButton`.

`loading` is a state distinct from `enabled = false`, and the distinction is load-bearing: **disabled dims the button; loading keeps the full filled appearance and the label**, swaps the trailing slot for a spinner, and drops the click. So it is *not* implemented as `enabled = enabled && !loading` — that would dim it. The button stays enabled and `onClick` becomes a no-op. Upstream sets `aria-busy`; the Compose equivalent is a `stateDescription` on the button node. `IconButton` has no loading state upstream and gets none here.

The spinner is `androidx.compose.material3.CircularProgressIndicator` (aliased, per the wrapper rule), passing `trackColor` at 0.25 alpha to match the faint ring the upstream SVG draws under its arc. It is deliberately *not* a hand-drawn port of that SVG: M3's indeterminate motion differs (an arc that grows and shrinks over roughly 1.3s, against upstream's fixed 90° arc at a constant 0.7s), and that difference is not worth ~35 lines of `Canvas` geometry in `:core:ui`. The DS is the source of truth for the token vocabulary — colour, size, the ring — not for reimplementing platform primitives stroke-for-stroke; same reasoning as `card`/`input`/`dialog` resolving to M3 shapes instead of restating radii. Going custom later is a contained change behind this call site if the difference ever bothers anyone.

It is also not the `LoadingIndicator` from Task 10 — Task 7 must not depend forward on Task 10, and that component is a differently-sized wrapper anyway.

- [x] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun buttonRendersEveryVariantAndFiresClick() {
        var clicked = false
        composeRule.setContent {
            ChatbotTheme {
                Column {
                    ButtonVariant.entries.forEach { variant ->
                        Button(text = variant.name, onClick = { clicked = variant == ButtonVariant.Filled }, variant = variant)
                    }
                }
            }
        }
        ButtonVariant.entries.forEach { composeRule.onNodeWithText(it.name).assertIsDisplayed() }
        composeRule.onNodeWithText("Filled").performClick()
        assertTrue(clicked)
    }

    @Test
    fun disabledButtonIsNotEnabled() {
        composeRule.setContent {
            ChatbotTheme { Button(text = "Off", onClick = {}, enabled = false) }
        }
        composeRule.onNodeWithText("Off").assertIsNotEnabled()
    }

    @Test
    fun loadingButtonKeepsLabelAndBlocksClicks() {
        var clicked = false
        composeRule.setContent {
            ChatbotTheme { Button(text = "Validating…", onClick = { clicked = true }, loading = true) }
        }
        // Loading is not disabled: the node stays enabled and full-colour, the click just does nothing.
        composeRule.onNodeWithText("Validating…").assertIsDisplayed().assertIsEnabled().performClick()
        assertFalse(clicked)
    }

    @Test
    fun iconButtonExposesContentDescriptionAndClicks() {
        var clicked = false
        composeRule.setContent {
            ChatbotTheme {
                IconButton(glyph = Glyphs.CLOSE, contentDescription = "Close", onClick = { clicked = true })
            }
        }
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonTest"`
Expected: FAIL — unresolved `Button` in package `component` / `ButtonVariant`.

- [x] **Step 3: Implement**

`Button.kt` (the M3 aliasing pattern — only wrapper files inside `:core:ui` see both names):

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.Button as M3Button
import androidx.compose.material3.CircularProgressIndicator as M3CircularProgressIndicator
import androidx.compose.material3.ElevatedButton as M3ElevatedButton
import androidx.compose.material3.FilledTonalButton as M3FilledTonalButton
import androidx.compose.material3.OutlinedButton as M3OutlinedButton
import androidx.compose.material3.TextButton as M3TextButton

enum class ButtonVariant { Filled, Tonal, Outlined, Text, Elevated }

@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.pressScaleButton else 1f,
        animationSpec = tween(Motion.durationShortMillis, easing = Motion.easingStandard),
        label = "button-press-scale",
    )
    // Loading is deliberately not folded into `enabled`: disabled dims the button, loading keeps
    // the full filled appearance and the label, and only swallows the click.
    val loadingDescription = stringResource(R.string.core_ui_loading)
    val pressModifier =
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.semantics { if (loading) stateDescription = loadingDescription }
    val action = if (loading) ({}) else onClick
    val shape = ChatbotShapes.button
    val content: @Composable RowScope.() -> Unit = {
        if (leadingGlyph != null) {
            Icon(leadingGlyph, contentDescription = null, size = 18.dp)
            Spacer(Modifier.width(Spacing.xs))
        }
        Text(text)
        // The spinner occupies the trailing slot, replacing any trailing glyph while it spins.
        if (loading) {
            Spacer(Modifier.width(Spacing.xs))
            M3CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                trackColor = LocalContentColor.current.copy(alpha = 0.25f),
            )
        } else if (trailingGlyph != null) {
            Spacer(Modifier.width(Spacing.xs))
            Icon(trailingGlyph, contentDescription = null, size = 18.dp)
        }
    }
    when (variant) {
        ButtonVariant.Filled ->
            M3Button(onClick = action, modifier = pressModifier, enabled = enabled, shape = shape, interactionSource = interactionSource, content = content)
        ButtonVariant.Tonal ->
            M3FilledTonalButton(onClick = action, modifier = pressModifier, enabled = enabled, shape = shape, interactionSource = interactionSource, content = content)
        ButtonVariant.Outlined ->
            M3OutlinedButton(onClick = action, modifier = pressModifier, enabled = enabled, shape = shape, interactionSource = interactionSource, content = content)
        ButtonVariant.Text ->
            M3TextButton(onClick = action, modifier = pressModifier, enabled = enabled, shape = shape, interactionSource = interactionSource, content = content)
        ButtonVariant.Elevated ->
            M3ElevatedButton(onClick = action, modifier = pressModifier, enabled = enabled, shape = shape, interactionSource = interactionSource, content = content)
    }
}

```

`strings.xml` (module root — the whole generic-string set for `:core:ui`, written once here; Tasks 8/9/10 only reference it):

```xml
<resources>
    <string name="core_ui_loading">Loading</string>
    <string name="core_ui_retry">Retry</string>
    <!-- %1$s is the chip's own label, e.g. "Dismiss Filter". -->
    <string name="core_ui_dismiss">Dismiss %1$s</string>
    <!-- Brand wordmark: a name, never translated or capitalized. -->
    <string name="core_ui_brand_wordmark" translatable="false">bro</string>
</resources>
```

`core_ui_retry`/`core_ui_dismiss`/`core_ui_brand_wordmark` have no consumer until Tasks 8–10; declaring them together keeps one edit of this file instead of three, and lint's unused-resource check does not fire on library `values/` strings.

`IconButton.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.FilledIconButton as M3FilledIconButton
import androidx.compose.material3.FilledTonalIconButton as M3FilledTonalIconButton
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedIconButton as M3OutlinedIconButton

enum class IconButtonVariant { Standard, Filled, Tonal, Outlined }

@Composable
fun IconButton(
    glyph: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IconButtonVariant = IconButtonVariant.Standard,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) Motion.pressScaleIconButton else 1f,
        animationSpec = tween(Motion.durationShortMillis, easing = Motion.easingStandard),
        label = "icon-button-press-scale",
    )
    val pressModifier =
        modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    when (variant) {
        IconButtonVariant.Standard ->
            M3IconButton(onClick = onClick, modifier = pressModifier, enabled = enabled, interactionSource = interactionSource) {
                // selected: glyph fills and tints primary (upstream contract for the standard variant).
                val tint = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                Icon(glyph, contentDescription = contentDescription, filled = selected, tint = tint)
            }
        IconButtonVariant.Filled ->
            M3FilledIconButton(onClick = onClick, modifier = pressModifier, enabled = enabled, interactionSource = interactionSource) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
        IconButtonVariant.Tonal ->
            M3FilledTonalIconButton(onClick = onClick, modifier = pressModifier, enabled = enabled, interactionSource = interactionSource) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
        IconButtonVariant.Outlined ->
            M3OutlinedIconButton(onClick = onClick, modifier = pressModifier, enabled = enabled, interactionSource = interactionSource) {
                Icon(glyph, contentDescription = contentDescription, filled = selected)
            }
    }
}
```

(If an M3 overload's parameter order fights you, keep everything named as above — the M3 signatures put other params between these.)

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonTest"`
Expected: PASS (4 tests).

- [x] **Step 5: Previews + screenshots**

`ButtonPreviews.kt` — all five `Button` variants (one disabled) + all four `IconButton` variants + one `selected = true`, dark and light:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.Button
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.IconButton
import com.shayanaryan.chatbot.core.ui.designsystem.component.IconButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
private fun ButtonGallery() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            ButtonVariant.entries.forEach { variant ->
                Button(text = variant.name, onClick = {}, variant = variant, leadingGlyph = Glyphs.CLOSE)
            }
            Button(text = "Continue", onClick = {}, trailingGlyph = Glyphs.ARROW_FORWARD)
            Button(text = "Disabled", onClick = {}, enabled = false)
            // Loading sits beside disabled on purpose — the golden is what proves they look different.
            Button(text = "Validating…", onClick = {}, loading = true)
            Row {
                IconButtonVariant.entries.forEach { variant ->
                    IconButton(glyph = Glyphs.CLOSE, contentDescription = variant.name, onClick = {}, variant = variant)
                }
                IconButton(glyph = Glyphs.CLOSE, contentDescription = "Selected", onClick = {}, selected = true)
            }
        }
    }
}

@PreviewTest
@Preview(name = "buttons-dark")
@Composable
private fun ButtonGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { ButtonGallery() }
}

@PreviewTest
@Preview(name = "buttons-light")
@Composable
private fun ButtonGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { ButtonGallery() }
}
```

Run: `./gradlew :core:ui:updateDebugScreenshotTest :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL; eyeball goldens — pill shapes, orange filled button, glyphs render as symbols, and the loading button at full colour next to the dimmed disabled one, its spinner over a faint track ring. The rotation is infinite (functional, not one of the decorative loops the spec bans) and the preview samples one frame of it — if that turns out to be a *different* frame run to run, the golden will flake; drop the loading button from the preview and keep the semantics test as the only coverage. The animation itself is not screenshot-testable either way; eyeball it once in the running app.

- [x] **Step 6: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 8: Core components II — `Card`, `Badge`, `BrandMark`

**Files:**
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Card.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Badge.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/BrandMark.kt`
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/CardBadgePreviews.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/CardBadgeTest.kt`

**Interfaces:**
- Consumes: `Icon`, `Glyphs` (Task 6); `ChatbotShapes.card`, `ChatbotExtendedTheme.colors` (Tasks 3–4).
- Produces:

```kotlin
enum class CardVariant { Filled, Outlined, Elevated }
@Composable fun Card(
    modifier: Modifier = Modifier, variant: CardVariant = CardVariant.Filled,
    onClick: (() -> Unit)? = null,   // non-null = interactive card (upstream `interactive` prop)
    content: @Composable ColumnScope.() -> Unit,
)

enum class BadgeTone { Primary, Error, Success, Neutral }
@Composable fun Badge(modifier: Modifier = Modifier, tone: BadgeTone = BadgeTone.Error, text: String? = null)  // null text = dot

@Composable fun BrandMark(modifier: Modifier = Modifier)
```

- [ ] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cardRendersContentAndClicks() {
        var clicked = false
        composeRule.setContent {
            ChatbotTheme {
                Card(onClick = { clicked = true }) { Text("Card body") }
            }
        }
        composeRule.onNodeWithText("Card body").assertIsDisplayed().performClick()
        assertTrue(clicked)
    }

    @Test
    fun badgeShowsCountText() {
        composeRule.setContent {
            ChatbotTheme { Badge(text = "3", tone = BadgeTone.Error) }
        }
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun brandMarkShowsLowercaseWordmark() {
        composeRule.setContent {
            ChatbotTheme { BrandMark() }
        }
        composeRule.onNodeWithText("bro").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.CardBadgeTest"`
Expected: FAIL — unresolved `Card` / `Badge` / `BrandMark` in `component` package.

- [ ] **Step 3: Implement**

`Card.kt` — flat filled surfaces by default; `Elevated` is the only shadowed variant:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.Card as M3Card
import androidx.compose.material3.ElevatedCard as M3ElevatedCard
import androidx.compose.material3.OutlinedCard as M3OutlinedCard

enum class CardVariant { Filled, Outlined, Elevated }

@Composable
fun Card(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.Filled,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = ChatbotShapes.card
    when (variant) {
        CardVariant.Filled ->
            if (onClick != null) {
                M3Card(onClick = onClick, modifier = modifier, shape = shape, content = content)
            } else {
                M3Card(modifier = modifier, shape = shape, content = content)
            }
        CardVariant.Outlined ->
            if (onClick != null) {
                M3OutlinedCard(onClick = onClick, modifier = modifier, shape = shape, content = content)
            } else {
                M3OutlinedCard(modifier = modifier, shape = shape, content = content)
            }
        CardVariant.Elevated ->
            if (onClick != null) {
                M3ElevatedCard(onClick = onClick, modifier = modifier, shape = shape, content = content)
            } else {
                M3ElevatedCard(modifier = modifier, shape = shape, content = content)
            }
    }
}
```

`Badge.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotExtendedTheme

enum class BadgeTone { Primary, Error, Success, Neutral }

@Composable
fun Badge(
    modifier: Modifier = Modifier,
    // Upstream default is `error` — an unqualified badge is an attention marker, not a brand accent.
    tone: BadgeTone = BadgeTone.Error,
    text: String? = null,
) {
    val extended = ChatbotExtendedTheme.colors
    val scheme = MaterialTheme.colorScheme
    val (container, onContainer) =
        when (tone) {
            BadgeTone.Primary -> scheme.primary to scheme.onPrimary
            BadgeTone.Error -> scheme.error to scheme.onError
            BadgeTone.Success -> extended.success to extended.onSuccess
            BadgeTone.Neutral -> scheme.surfaceContainerHighest to scheme.onSurfaceVariant
        }
    if (text == null) {
        Box(modifier.size(8.dp).background(container, CircleShape))
    } else {
        Box(
            modifier.background(container, CircleShape).padding(horizontal = 6.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = onContainer, style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

`BrandMark.kt` — wordmark only; never invent a standalone Bro logo:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

/** The "bro" wordmark: forum-glyph tile in the accent color beside lowercase Roboto Medium text. */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Glyphs.BRAND, contentDescription = null, size = 20.dp, filled = true, tint = MaterialTheme.colorScheme.onPrimary)
        }
        Spacer(Modifier.width(Spacing.xs))
        // `translatable="false"` — the wordmark is a name, and lowercase is part of the mark.
        Text(
            stringResource(R.string.core_ui_brand_wordmark),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.CardBadgeTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Previews + screenshots**

`CardBadgePreviews.kt` — `BrandMark`, three card variants, four badge tones + dot; same gallery pattern (private gallery composable + `@PreviewTest @Preview` dark/light pair in `ChatbotTheme`):

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.Badge
import com.shayanaryan.chatbot.core.ui.designsystem.component.BadgeTone
import com.shayanaryan.chatbot.core.ui.designsystem.component.BrandMark
import com.shayanaryan.chatbot.core.ui.designsystem.component.Card
import com.shayanaryan.chatbot.core.ui.designsystem.component.CardVariant
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
private fun CardBadgeGallery() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            BrandMark()
            CardVariant.entries.forEach { variant ->
                Card(variant = variant, modifier = Modifier.padding(top = Spacing.xs)) {
                    Text(variant.name, Modifier.padding(Spacing.md))
                }
            }
            Row(Modifier.padding(top = Spacing.xs)) {
                BadgeTone.entries.forEach { tone ->
                    Badge(tone = tone, text = "3", modifier = Modifier.padding(end = Spacing.xxs))
                }
                Badge()
            }
        }
    }
}

@PreviewTest
@Preview(name = "cards-badges-dark")
@Composable
private fun CardBadgeGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { CardBadgeGallery() }
}

@PreviewTest
@Preview(name = "cards-badges-light")
@Composable
private fun CardBadgeGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { CardBadgeGallery() }
}
```

Run: `./gradlew :core:ui:updateDebugScreenshotTest :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 9: Forms — `TextField`, `Switch`, `Chip`

**Files:**
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/TextField.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Switch.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Chip.kt`
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/FormsPreviews.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/FormsTest.kt`

**Interfaces:**
- Consumes: `Icon` (Task 6), `ChatbotShapes.input` / `.chip`, `MonoTextStyle`.
- Produces:

```kotlin
enum class TextFieldVariant { Outlined, Filled }
@Composable fun TextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: String? = null, placeholder: String? = null,
    variant: TextFieldVariant = TextFieldVariant.Outlined,
    leadingGlyph: String? = null, trailingGlyph: String? = null, onTrailingClick: (() -> Unit)? = null,
    supportingText: String? = null, isError: Boolean = false, enabled: Boolean = true,
    singleLine: Boolean = true, minLines: Int = 1,
    mono: Boolean = false,   // API keys / model ids / code
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
)

@Composable fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true)

enum class ChipVariant { Assist, Filter, Input, Suggestion }
@Composable fun Chip(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Assist, selected: Boolean = false,
    leadingGlyph: String? = null, onDismiss: (() -> Unit)? = null, enabled: Boolean = true,
)
```

Contract deltas: web `multiline/rows` become `singleLine/minLines`; `helperText` → `supportingText` (M3 name); `type` → `visualTransformation` + `keyboardOptions` (Android idiom, needed for API-key masking in 006). Floating label and the 2dp accent focus border come from M3 `OutlinedTextField`/`TextField` defaults — do not re-implement.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun textFieldEmitsValueChanges() {
        var value = ""
        composeRule.setContent {
            ChatbotTheme {
                TextField(value = value, onValueChange = { value = it }, label = "API key")
            }
        }
        composeRule.onNodeWithText("API key").performTextInput("sk-ant")
        assertEquals("sk-ant", value)
    }

    @Test
    fun textFieldShowsSupportingTextInErrorState() {
        composeRule.setContent {
            ChatbotTheme {
                TextField(value = "x", onValueChange = {}, label = "Key", isError = true, supportingText = "Invalid key")
            }
        }
        composeRule.onNodeWithText("Invalid key").assertIsDisplayed()
    }

    @Test
    fun switchTogglesState() {
        var checked = false
        composeRule.setContent {
            ChatbotTheme { Switch(checked = checked, onCheckedChange = { checked = it }) }
        }
        composeRule.onNode(androidx.compose.ui.test.isToggleable()).assertIsOff().performClick()
        assertTrue(checked)
    }

    @Test
    fun chipRendersAllVariantsAndDismisses() {
        var dismissed = false
        composeRule.setContent {
            ChatbotTheme {
                androidx.compose.foundation.layout.Column {
                    ChipVariant.entries.forEach { variant ->
                        Chip(label = variant.name, onClick = {}, variant = variant, onDismiss = { dismissed = variant == ChipVariant.Input })
                    }
                }
            }
        }
        ChipVariant.entries.forEach { composeRule.onNodeWithText(it.name).assertIsDisplayed() }
        // Resolved from resources, not spelled out — this asserts the wiring, not the copy.
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.core_ui_dismiss, ChipVariant.Input.name))
            .performClick()
        assertTrue(dismissed)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.FormsTest"`
Expected: FAIL — unresolved `TextField` / `Switch` / `Chip` in `component` package.

- [ ] **Step 3: Implement**

`TextField.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.IconButton as M3IconButton
import androidx.compose.material3.OutlinedTextField as M3OutlinedTextField
import androidx.compose.material3.TextField as M3TextField

enum class TextFieldVariant { Outlined, Filled }

@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    variant: TextFieldVariant = TextFieldVariant.Outlined,
    leadingGlyph: String? = null,
    trailingGlyph: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    mono: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val textStyle = if (mono) MonoTextStyle else LocalTextStyle.current
    val labelComposable: (@Composable () -> Unit)? = label?.let { { Text(it) } }
    val placeholderComposable: (@Composable () -> Unit)? = placeholder?.let { { Text(it) } }
    val supportingComposable: (@Composable () -> Unit)? = supportingText?.let { { Text(it) } }
    val leadingComposable: (@Composable () -> Unit)? = leadingGlyph?.let { { Icon(it, contentDescription = null, size = 20.dp) } }
    val trailingComposable: (@Composable () -> Unit)? =
        trailingGlyph?.let { glyph ->
            {
                if (onTrailingClick != null) {
                    M3IconButton(onClick = onTrailingClick) { Icon(glyph, contentDescription = null, size = 20.dp) }
                } else {
                    Icon(glyph, contentDescription = null, size = 20.dp)
                }
            }
        }
    when (variant) {
        TextFieldVariant.Outlined ->
            M3OutlinedTextField(
                value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
                textStyle = textStyle, label = labelComposable, placeholder = placeholderComposable,
                leadingIcon = leadingComposable, trailingIcon = trailingComposable,
                supportingText = supportingComposable, isError = isError,
                visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
                singleLine = singleLine, minLines = minLines, shape = ChatbotShapes.input,
            )
        TextFieldVariant.Filled ->
            M3TextField(
                value = value, onValueChange = onValueChange, modifier = modifier, enabled = enabled,
                textStyle = textStyle, label = labelComposable, placeholder = placeholderComposable,
                leadingIcon = leadingComposable, trailingIcon = trailingComposable,
                supportingText = supportingComposable, isError = isError,
                visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
                singleLine = singleLine, minLines = minLines,
            )
    }
}
```

`Switch.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Switch as M3Switch

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    M3Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
}
```

`Chip.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.AssistChip as M3AssistChip
import androidx.compose.material3.FilterChip as M3FilterChip
import androidx.compose.material3.InputChip as M3InputChip
import androidx.compose.material3.SuggestionChip as M3SuggestionChip

enum class ChipVariant { Assist, Filter, Input, Suggestion }

@Composable
fun Chip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ChipVariant = ChipVariant.Assist,
    selected: Boolean = false,
    leadingGlyph: String? = null,
    onDismiss: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val shape = ChatbotShapes.chip
    val labelComposable: @Composable () -> Unit = { Text(label) }
    val leadingComposable: (@Composable () -> Unit)? = leadingGlyph?.let { { Icon(it, contentDescription = null, size = 18.dp) } }
    when (variant) {
        ChipVariant.Assist ->
            M3AssistChip(onClick = onClick, label = labelComposable, modifier = modifier, enabled = enabled, leadingIcon = leadingComposable, shape = shape)
        ChipVariant.Filter ->
            M3FilterChip(selected = selected, onClick = onClick, label = labelComposable, modifier = modifier, enabled = enabled, leadingIcon = leadingComposable, shape = shape)
        ChipVariant.Input ->
            M3InputChip(
                selected = selected, onClick = onClick, label = labelComposable, modifier = modifier, enabled = enabled,
                leadingIcon = leadingComposable, shape = shape,
                trailingIcon = onDismiss?.let { dismiss ->
                    {
                        M3IconForDismiss(label = label, onDismiss = dismiss)
                    }
                },
            )
        ChipVariant.Suggestion ->
            M3SuggestionChip(onClick = onClick, label = labelComposable, modifier = modifier, enabled = enabled, icon = leadingComposable, shape = shape)
    }
}

@Composable
private fun M3IconForDismiss(
    label: String,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.IconButton(onClick = onDismiss, modifier = Modifier) {
        Icon(Glyphs.CLOSE, contentDescription = stringResource(R.string.core_ui_dismiss, label), size = 18.dp)
    }
}
```

Note: the test dismisses via the resolved `core_ui_dismiss` description — the `Icon` inside the dismiss button provides it. If the M3 `InputChip` trailing slot clips the 48dp `IconButton` ripple target, replace the inner `IconButton` with `Icon(Glyphs.CLOSE, contentDescription = stringResource(R.string.core_ui_dismiss, label), size = 18.dp, modifier = Modifier.clickable(onClick = onDismiss))` — behavior over pixel-perfection here; keep the content description.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.FormsTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Previews + screenshots**

`FormsPreviews.kt` — outlined + filled text fields (one error, one mono with a fake key), switch on/off, all four chip variants (filter selected); dark + light pair, same gallery pattern:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.Chip
import com.shayanaryan.chatbot.core.ui.designsystem.component.ChipVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.Switch
import com.shayanaryan.chatbot.core.ui.designsystem.component.TextField
import com.shayanaryan.chatbot.core.ui.designsystem.component.TextFieldVariant
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
private fun FormsGallery() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            TextField(value = "", onValueChange = {}, label = "Label")
            TextField(value = "sk-ant-api03-xxxx", onValueChange = {}, label = "API key", mono = true, variant = TextFieldVariant.Filled)
            TextField(value = "bad", onValueChange = {}, label = "Key", isError = true, supportingText = "Invalid key")
            Row {
                Switch(checked = true, onCheckedChange = {})
                Switch(checked = false, onCheckedChange = {})
            }
            Row {
                ChipVariant.entries.forEach { variant ->
                    Chip(
                        label = variant.name,
                        onClick = {},
                        variant = variant,
                        selected = variant == ChipVariant.Filter,
                        onDismiss = if (variant == ChipVariant.Input) fun() {} else null,
                        modifier = Modifier.padding(end = Spacing.xxs),
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "forms-dark")
@Composable
private fun FormsGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { FormsGallery() }
}

@PreviewTest
@Preview(name = "forms-light")
@Composable
private fun FormsGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { FormsGallery() }
}
```

Run: `./gradlew :core:ui:updateDebugScreenshotTest :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL; mono field renders monospace, chips are pills.

- [ ] **Step 6: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 10: Feedback — `Dialog`, `Snackbar`, `LoadingIndicator`, `ErrorState`

**Files:**
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Dialog.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/Snackbar.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/LoadingIndicator.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/ErrorState.kt`
- Create: `core/ui/src/screenshotTest/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/preview/FeedbackPreviews.kt`
- Test: `core/ui/src/test/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/component/FeedbackTest.kt`

**Interfaces:**
- Consumes: `Button`, `ButtonVariant` (Task 7), `Icon` (Task 6), `ChatbotShapes.dialog`.
- Produces:

```kotlin
@Composable fun Dialog(
    onDismissRequest: () -> Unit, title: String, confirmText: String, onConfirm: () -> Unit,
    modifier: Modifier = Modifier, text: String? = null, glyph: String? = null,
    dismissText: String? = null, onDismiss: (() -> Unit)? = null,
)

@Composable fun Snackbar(snackbarData: SnackbarData, modifier: Modifier = Modifier)  // drop into M3 SnackbarHost

@Composable fun LoadingIndicator(modifier: Modifier = Modifier, size: Dp = 40.dp)

@Composable fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null)
```

Generic strings live in `:core:ui` per the architecture skill — `ErrorState`'s retry label is `R.string.core_ui_retry`, already declared in Task 7's `strings.xml`. `Dialog`'s title/text/confirm/dismiss strings stay caller-supplied parameters: they are per-screen copy, so features own them and resolve them from their own `strings.xml` at the call site.

**No `EmptyState` here.** Empty states differ structurally per screen, not just in copy (the conversation screen's is a hero block; a reminders list wants icon + line + CTA; a memories list wants icon + line only). No feature screen exists yet to validate a shared shape against, so a DS component would freeze a guessed layout that callers then fight with nullable slots. Features build their own empty states from `Icon` + typography + `Button`; hoist into `:core:ui` only once two real screens demonstrably share a structure.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dialogShowsTitleAndFiresConfirm() {
        var confirmed = false
        composeRule.setContent {
            ChatbotTheme {
                Dialog(
                    onDismissRequest = {},
                    title = "Delete conversation?",
                    text = "This cannot be undone.",
                    confirmText = "Delete",
                    onConfirm = { confirmed = true },
                    dismissText = "Cancel",
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithText("Delete conversation?").assertIsDisplayed()
        composeRule.onNodeWithText("This cannot be undone.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun errorStateShowsMessageAndRetries() {
        var retried = false
        composeRule.setContent {
            ChatbotTheme { ErrorState(message = "Something went wrong", onRetry = { retried = true }) }
        }
        composeRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        // `message` is caller-supplied so it stays a literal; the retry label is ours, so resolve it.
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.onNodeWithText(context.getString(R.string.core_ui_retry)).performClick()
        assertTrue(retried)
    }
}
```

(`LoadingIndicator` and `Snackbar` are thin M3 wrappers with no logic — screenshot previews cover them.)

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.FeedbackTest"`
Expected: FAIL — unresolved `Dialog` / `ErrorState` in `component` package.

- [ ] **Step 3: Implement**

`Dialog.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import androidx.compose.material3.AlertDialog as M3AlertDialog

@Composable
fun Dialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    glyph: String? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
) {
    M3AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = ChatbotShapes.dialog,
        icon = glyph?.let { { Icon(it, contentDescription = null, size = 24.dp, tint = MaterialTheme.colorScheme.secondary) } },
        title = { Text(title) },
        text = text?.let { { Text(it) } },
        confirmButton = { Button(text = confirmText, onClick = onConfirm, variant = ButtonVariant.Text) },
        dismissButton =
            if (dismissText != null) {
                { Button(text = dismissText, onClick = onDismiss ?: onDismissRequest, variant = ButtonVariant.Text) }
            } else {
                null
            },
    )
}
```

`Snackbar.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.material3.SnackbarData
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Snackbar as M3Snackbar

/** Drop into an M3 `SnackbarHost`: `SnackbarHost(hostState) { data -> Snackbar(data) }`. */
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
) {
    M3Snackbar(snackbarData = snackbarData, modifier = modifier)
}
```

`LoadingIndicator.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    CircularProgressIndicator(modifier = modifier.size(size))
}
```

`ErrorState.kt`:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Icon
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Glyphs.ERROR, contentDescription = null, size = 48.dp, filled = true, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(Spacing.md))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(Spacing.md))
            Button(text = stringResource(R.string.core_ui_retry), onClick = onRetry, variant = ButtonVariant.Tonal)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:ui:testDebugUnitTest --tests "com.shayanaryan.chatbot.core.ui.designsystem.component.FeedbackTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Previews + screenshots**

`FeedbackPreviews.kt` — `LoadingIndicator`, `ErrorState` (with retry); dark + light pair, same gallery pattern. `Dialog` and `Snackbar` render in overlay windows the preview tool can't capture from a plain composable — preview their content by rendering the M3 pieces inline is out of scope; cover Dialog via the semantics test above and a manual check in later feature work:

```kotlin
package com.shayanaryan.chatbot.core.ui.designsystem.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.component.ErrorState
import com.shayanaryan.chatbot.core.ui.designsystem.component.LoadingIndicator
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme

@Composable
private fun FeedbackGallery() {
    Surface {
        Column(Modifier.padding(Spacing.md)) {
            LoadingIndicator()
            ErrorState(message = "Couldn't reach Claude", onRetry = {})
        }
    }
}

@PreviewTest
@Preview(name = "feedback-dark")
@Composable
private fun FeedbackGalleryDarkPreview() {
    ChatbotTheme(darkTheme = true) { FeedbackGallery() }
}

@PreviewTest
@Preview(name = "feedback-light")
@Composable
private fun FeedbackGalleryLightPreview() {
    ChatbotTheme(darkTheme = false) { FeedbackGallery() }
}
```

Run: `./gradlew :core:ui:updateDebugScreenshotTest :core:ui:validateDebugScreenshotTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format and wrap up (no commit)**

Run: `./gradlew :core:ui:spotlessApply :core:ui:spotlessCheck :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. Leave in tree.

---

### Task 11: `design-system` skill + spec value migration

**Files:**
- Create: `.claude/skills/design-system/SKILL.md`
- Modify: `specs/002-design-system.md` (remove migrated value tables)

**Interfaces:**
- Consumes: everything built in Tasks 1–10 (the skill documents it; verify names against the actual code, not this plan).
- Produces: the usage skill later feature milestones load; spec 002 slimmed to structure/rationale.

This task is documentation — no unit test. Its "test" is the prose-review skill plus a final full-module verification.

- [ ] **Step 1: Write `.claude/skills/design-system/SKILL.md`**

Content requirements (from spec 002 §Companion skill; keep it concise, it grows in later milestones):

```markdown
---
name: design-system
description: Use when building or styling any screen or component in this app —
  choosing colors, text styles, spacing, shapes, icons, or picking a catalog
  component; also owns the manual sync procedure against the upstream Bro
  Design System.
---

# Chatbot Design System (`:core:ui`)

Dark-first Material 3. Upstream SSOT: "Bro Design System" Claude Design project —
sync via `pull-design`. Exact token values live in the Kotlin token files, not here:
`core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/theme/` (last synced from
Bro DS, 2026-07-18).

## Token accessors

- Standard M3: `MaterialTheme.colorScheme` / `.typography` / `.shapes`.
- Constants, read directly — no theme lookup, usable outside composition:
  `Spacing` (none/xxs/xs/sm/md/lg/xl/xxl/x3l/x4l/x5l + gutter), `Elevation`
  (level1–5), `ChatbotShapes` (button, chip, card, input, dialog, bubbleUser,
  bubbleAssistant), `Motion` (easings, durations, press scales, state-layer
  opacities, caretBlinkMillis), `MonoTextStyle` (14sp monospace, in `Type.kt`).
  For touch targets use `Modifier.minimumInteractiveComponentSize()`, not a
  spacing value.
- Scheme-dependent, so via `ChatbotExtendedTheme.colors` (success/onSuccess/
  successContainer, warning, primaryHover/primaryPressed). It is the only one.
- Never hardcode a hex, dp, or sp that a token covers. Components read roles,
  never primitives (`ColorPrimitives` is internal to `:core:ui`).

## Component catalog (import from `com.shayanaryan.chatbot.core.ui.designsystem.component`)

- core: `Button` (Filled/Tonal/Outlined/Text/Elevated), `IconButton`
  (Standard/Filled/Tonal/Outlined, `selected` fills the glyph), `Card`
  (Filled/Outlined/Elevated), `Badge` (Primary/Error/Success/Neutral, null
  text = dot), `BrandMark`.
- forms: `TextField` (Outlined/Filled; `mono = true` for keys/ids), `Switch`,
  `Chip` (Assist/Filter/Input/Suggestion).
- feedback: `Dialog`, `Snackbar` (drop into M3 `SnackbarHost`),
  `LoadingIndicator`, `ErrorState`.

There is no `EmptyState` and there are no chat components here. Empty states and
chat surfaces (`MessageBubble`, `ConversationListItem`, `ModelPicker`) are built
in the feature module that owns them, from these tokens and components. Bubble
shapes (`ChatbotShapes.bubbleUser`/`bubbleAssistant`) and the caret duration
(`Motion.caretBlinkMillis`) stay here — they are vocabulary, not compositions.
Models carry no glyph: a model is identified by name only, never an icon.

## Rules

- **Import boundary:** feature modules import components only from `:core:ui`,
  never `androidx.compose.material3` directly. Only `:core:ui` wrapper files
  see both names (aliased `M3*`).
- **Icons:** `Icon(glyph = …)` always takes a `Glyphs` constant, never a bare
  ligature string — a typo'd ligature renders nothing and compiles fine. Add new
  glyphs to `Glyphs` as screens need them. Models carry no glyph — a model is
  identified by name only, never an icon. Emoji are never UI icons; no PNG/SVG
  icon assets.
- **Mono rule:** API keys, model ids, and code render in
  `MonoTextStyle` (`TextField(mono = true)` for inputs).
- **Strings:** no user-visible literal in component source — labels, content
  descriptions and state descriptions come from `strings.xml` via
  `stringResource(...)`. `:core:ui` owns only generic strings
  (`core_ui_loading`, `core_ui_retry`, `core_ui_dismiss`,
  `core_ui_brand_wordmark`); per-screen copy is a component *parameter* the
  feature resolves from its own `strings.xml`. Material Symbols ligature names
  are glyph ids, not text, and stay literals.
- **Brand:** the wordmark is `BrandMark` (forum tile + lowercase "bro").
  Do not invent a Bro logo. "Bro" never appears in code identifiers.
- **Surfaces:** flat tonal fills; no gradients; no glass/backdrop blur; shadow
  only for FAB, menus, dialogs, heads-up notification. Dark is default; every
  screen must also render in light.
- **Colocated previews:** every `public @Composable` ships at least one plain
  `@Preview` (no `@PreviewTest`) in its own file, one per distinct visual state
  (enabled / disabled / loading / selected …), each wrapped in `ChatbotTheme` —
  these feed the IDE panel and stay in the component file.
- **Screenshot tests:** every new component/variant adds `@PreviewTest`
  previews (dark + light) under `core/ui/src/screenshotTest/.../designsystem/preview/`,
  then `updateDebugScreenshotTest` + `validateDebugScreenshotTest`. These are
  separate from the colocated `@Preview`s above and never live in the component file.

## Sync procedure (manual, on upstream change)

1. `pull-design` → fetch current tokens/components from the Bro Design System
   project.
2. Diff against the Kotlin token files and component wrappers; change only what
   differs, re-verify the rest.
3. Update this skill's usage guidance if accessors/components changed.
4. Re-record screenshot goldens; review the image diffs.
5. Stamp the new `last synced from Bro DS, <date>` in the touched files.
```

- [ ] **Step 2: Migrate spec 002's value tables out**

Edit `specs/002-design-system.md` per its own §"Source of truth & where values live": now that `:core:ui` exists, the temporary value tables move to code. Remove:

- the **Primitives** table and the **Roles** table (§Color) — replace both with one line: primitives and role mappings live in `theme/Color.kt`; extended colors in `theme/ExtendedColors.kt`.
- the typography metrics table (keep the two-sentence description of the 15-role Roboto scale + weights + the mono paragraph, pointing to `theme/Type.kt`).
- the exact shape/spacing/motion/state-layer numbers (keep the *structure* prose — what families exist, bubble-tail rule, 4dp grid, flat-fill constraints — pointing to `theme/Shape.kt`, `theme/Spacing.kt`, `theme/Motion.kt`, `theme/Elevation.kt`).

Keep untouched: decisions/rationale, two-tier structure, role-name concept, module rules, icon system, naming rules, component families, screenshot strategy. Also update the paragraph in §"Source of truth & where values live" that declares the tables "temporary in-repo reference" — the migration it describes has now happened, so state where values live instead. Run the `prose-review` skill over the edited spec.

- [ ] **Step 3: Full verification sweep**

Run:

```bash
./gradlew spotlessCheck :core:ui:testDebugUnitTest :core:ui:validateDebugScreenshotTest :app:assembleDebug
```

Expected: BUILD SUCCESSFUL — all unit tests, all screenshot validations, whole-tree compile. Then run the full test suite once: `./gradlew testDebugUnitTest`. Expected: green (no regression in `:feature:conversation`'s placeholder test or elsewhere).

- [ ] **Step 4: Wrap up (no commit)**

Report the working tree ready for user review: new `:core:ui` sources + goldens, catalog/properties changes, slimmed spec 002, new skill. The user commits manually.

---

## Self-Review (done at plan-writing time)

- **Spec coverage:** module/deps → T1; two-tier color + both schemes + scrims → T1; extended colors + state layers → T3; typography 15 roles + mono → T2; shapes (incl. bubble tails) → T3; spacing/gutter/touch target → T3; elevation + motion (easings, durations, press scales, caret 1s) → T3, exercised in T7; theme entry, no dynamic color → T4; icon font bundled in-APK + axes + ligatures → T6; brand mark + no-logo rule (models carry no glyph) → T6/T8; naming boundary + M3 aliasing → global + T7 code; component catalog: core → T7/T8, forms → T9, feedback → T10; stateless rule → signatures throughout; screenshot testing dark+light per component → T5 + each task; Roborazzi fallback → T5; companion skill → T11; value migration out of spec → T11.
- **Scope boundary:** chat components (`MessageBubble`, `ConversationListItem`, `ModelPicker`) and `EmptyState` are deliberately *not* in `:core:ui` — see spec 002 §Component catalog. The chat trio has a single consumer (`:feature:conversation`) and domain-shaped props, so it is built there under spec 005. Empty states differ structurally per screen and have no validated shared shape yet. The tokens they consume (bubble shapes, caret duration) are still built here, in T3/T6; models carry no glyph, so there is none to build.
- **Known judgment calls (flag at review, don't silently change):** mono uses `FontFamily.Monospace` instead of a bundled Roboto Mono (T2); label/display letter-spacing kept M3-exact where the upstream CSS port is lossy (T2, see tracking note); web `size`/`fullWidth` props dropped (T7); Dialog uses confirm/dismiss params instead of an actions list (T10).
- **Type consistency:** verified — `Glyphs.BRAND/CLOSE/ERROR/ARROW_FORWARD`, `Spacing.md/xs/…`, `ChatbotShapes.bubbleUser/bubbleAssistant`, `Motion.durationShortMillis/pressScaleButton/caretBlinkMillis`, `ButtonVariant`/`IconButtonVariant`/`CardVariant`/`BadgeTone`/`TextFieldVariant`/`ChipVariant` are used with these exact names in every later task.
