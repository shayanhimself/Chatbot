# 002 — Design System

The `:core:ui` module: the app's Material 3 design system — theme, design tokens, and a reusable Compose component catalog. Every feature module renders through it; it depends on nothing else in the project.

The single source of truth for the visual design is the **"Bro Design System"** Claude Design project (with screen mockups in the companion **"Bro designs"** project). This spec is a *downstream translation* of that source into Android/Compose terms — it states what `:core:ui` provides and the rules for using it, while raw ramp constants and per-component prop contracts stay in the source. When the design changes upstream, this spec and `:core:ui` are what needs re-syncing; the values here can therefore drift and should be re-verified against the source rather than trusted blindly. Pull the current tokens and designs with the **`pull-design`** skill.

Dark-first: dark is the default scheme; light is a full opt-in scheme. No Material You / dynamic color — the crafted navy+orange identity ships on every device. No gradients; flat tonal fills only.

## Source of truth & where values live

One home per fact:

- **Claude Design ("Bro Design System")** — upstream SSOT for the visual design. Everything below mirrors it and can drift; re-sync with the `pull-design` skill.
- **`:core:ui` Kotlin token files** — the in-project home for exact values (hex, sp, primitive names), once built: one typed definition, consumed by the code.
- **`design-system` skill** — owns *usage* (token accessors, when-to-use, model-glyph map, naming boundary rule, flat-fills / no-gradient constraints) and the manual **sync procedure** (`pull-design` → diff → update code + usage → stamp date). It points to the token files for current values; it does not duplicate them.
- **This spec** — owns the durable *what/why*: decisions and rationale, the two-tier color structure, role names, component families, screenshot strategy, naming rule. It does not own volatile values.

The exact value tables in this spec (primitive hexes, role values, type metrics) are a **temporary in-repo reference for the M1 implementation plan**. When `:core:ui` is built, those values migrate into the Kotlin token files and the `design-system` skill's usage guidance and are **removed from this spec** — structure and rationale stay here, values live in code. Downstream copies carry a `last synced from Bro DS, <date>` provenance line so staleness is visible.

## Module

- `:core:ui`, package `com.shayanaryan.chatbot.core.ui`. Android library (not KMP — pure UI).
- Depends only on Compose (BOM), Material 3, and the icon/screenshot tooling below. No dependency on `:shared` or any feature module.
- Replaces the placeholder AGP-template theme (`Color.kt` / `Type.kt` / `Theme.kt`).

## Token layer

Standard tokens flow through `MaterialTheme` (`ColorScheme`, `Typography`, `Shapes`). Tokens M3 has no slot for — semantic/extended colors, motion, the mono text style, named component shapes — live in `:core:ui`, split by whether their value depends on runtime state.

A token set earns a CompositionLocal only if its value depends on the active color scheme or a user/system preference. `ExtendedColors` is the only one: the `ChatbotTheme` composable installs it, and it is read through the `ChatbotExtendedTheme.colors` accessor. Everything else is constant and read directly — `Spacing.md`, `Elevation.level2`, `ChatbotShapes.bubbleUser`, `Motion.durationMediumMillis`, `MonoTextStyle` — with no theme lookup, and usable outside composition (draw scopes, previews, test fixtures).

### Color

The source organizes color in two tiers, and the Android layer mirrors it: a **primitive palette** (every literal value, named once by hue + tone) and two **semantic `ColorScheme`s** (dark default + light) whose roles each reference a primitive. Components read only roles.

**Primitives**

| Ramp | Tones (name `#hex`) |
|---|---|
| Orange | `orange-90` #ffdcc2 · `orange-57` #ffa257 · `orange-50` #ff9239 · `orange-40` #ed6900 · `orange-30` #b84e00 · `orange-20` #7a3300 · `orange-12` #431c00 · `orange-10` #401a00 |
| Yellow | `yellow-90` #ffe494 · `yellow-35` #8a6600 · `yellow-20` #4a3600 · `yellow-15` #2a1e00 |
| Warm neutral | `warm-95` #ffdcc2 · `warm-85` #e4c0a4 · `warm-50` #7a5733 · `warm-30` #5a4029 · `warm-22` #422a15 · `warm-18` #2a1800 |
| Navy | `navy-06` #12161f · `navy-10` #1a2130 · `navy-14` #212a3b · `navy-17` #263043 · `navy-22` #2e394d · `navy-26` #354158 · `navy-40` #55617a · `navy-60` #8791a6 · `navy-80` #c3cbd9 · `navy-95` #e7effe |
| Sand (light) | `sand-100` #ffffff · `sand-98` #fbf9f7 · `sand-96` #f5f2ee · `sand-94` #efece7 · `sand-92` #e9e5df · `sand-90` #e3dfd8 · `sand-70` #d5c8ba · `sand-52` #837567 · `sand-32` #4f4539 · `sand-20` #313029 · `sand-11` #1c1b19 |
| Green | `green-50` #4ecb7b · `green-44` #1f7a44 · `green-88` #b7f0c8 · `green-20` #0f4d2a · `green-08` #00210f |
| Red | `red-70` #ffb3b3 · `red-50` #ff6b6b · `red-44` #ba1a1a · `red-90` #ffdad6 · `red-20` #5c1a1a · `red-12` #430e0e · `red-05` #410002 |
| Amber | `amber-50` #ffce54 · `amber-35` #8a5a00 |
| Other | `white` #ffffff · `scrim-dark` rgba(6,9,14,0.72) · `scrim-light` rgba(28,27,25,0.40) |

**Roles** — each cell names the primitive that scheme resolves to:

| M3 role | Dark (default) | Light |
|---|---|---|
| primary | orange-50 | orange-40 |
| onPrimary | orange-12 | white |
| primaryContainer | orange-20 | orange-90 |
| onPrimaryContainer | orange-90 | orange-10 |
| secondary | warm-85 | warm-50 |
| onSecondary | warm-22 | white |
| secondaryContainer | warm-30 | warm-95 |
| onSecondaryContainer | warm-95 | warm-18 |
| tertiary | yellow-90 | yellow-35 |
| onTertiary | yellow-20 | white |
| tertiaryContainer | yellow-20 | yellow-90 |
| onTertiaryContainer | yellow-90 | yellow-15 |
| background / surface | navy-10 | sand-98 |
| surfaceContainerLowest | navy-06 | sand-100 |
| surfaceContainerLow | navy-14 | sand-96 |
| surfaceContainer | navy-17 | sand-94 |
| surfaceContainerHigh | navy-22 | sand-92 |
| surfaceContainerHighest | navy-26 | sand-90 |
| onSurface | navy-95 | sand-11 |
| onSurfaceVariant | navy-80 | sand-32 |
| outline | navy-40 | sand-52 |
| outlineVariant | navy-22 | sand-70 |
| error | red-50 | red-44 |
| onError | red-12 | white |
| errorContainer | red-20 | red-90 |
| onErrorContainer | red-70 | red-05 |
| scrim | scrim-dark | scrim-light |
| inverseSurface | navy-95 | sand-20 |
| inverseOnSurface | navy-14 | sand-96 |

Both schemes fully specify every role — the light scope darkens `tertiary`, `error`, and the semantic hues for legibility on light surfaces rather than reusing the dark tones.

**Extended colors** (`ChatbotExtendedTheme.colors`, no M3 slot):

| Token | Dark | Light |
|---|---|---|
| success / onSuccess / successContainer | green-50 / green-08 / green-20 | green-44 / white / green-88 |
| warning | amber-50 | amber-35 |
| primaryHover / primaryPressed | orange-57 / orange-40 | orange-30 / orange-20 |

**State-layer opacities:** hover `0.08`, focus `0.10`, pressed `0.12`.

### Typography

Full 15-role M3 scale on **Roboto**, exact metrics from the source (size / line-height / letter-spacing). Weights: Display & Headline 400; Title & Label 500; Body 400; `bold` 700 available.

| Role | Size / line | Role | Size / line |
|---|---|---|---|
| displayLarge | 57 / 64 | titleMedium | 16 / 24 (+0.15) |
| displayMedium | 45 / 52 | titleSmall | 14 / 20 (+0.1) |
| displaySmall | 36 / 44 | bodyLarge | 16 / 24 (+0.5) |
| headlineLarge | 32 / 40 | bodyMedium | 14 / 20 (+0.25) |
| headlineMedium | 28 / 36 | bodySmall | 12 / 16 (+0.4) |
| headlineSmall | 24 / 32 | labelLarge | 14 / 20 (+0.1) |
| titleLarge | 22 / 28 | labelMedium | 12 / 16 (+0.5) |
| | | labelSmall | 11 / 16 (+0.5) |

A monospace style (`MonoTextStyle`, 14sp, alongside the M3 scale in `Type.kt`) is used for API keys, model ids, and code — the one text style outside the M3 scale. It resolves to the device monospace via `FontFamily.Monospace`; bundling a Roboto Mono asset is deferred until fidelity demands it.

### Shape

M3 `Shapes`: xs 4 · sm 8 · md 12 · lg 16 · xl 28. Named component shapes on `ChatbotShapes`: `button`/`chip` = pill, `card`/`input`/`dialog` resolve to the M3 `medium`/`extraSmall`/`extraLarge` rather than restating the radius (so a named shape cannot drift from the M3 role that M3 components read internally), `bubble` = 20 with one squared tail corner (bottom-end for the user, bottom-start for the assistant).

### Spacing

4dp grid on `Spacing`: 0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64. Default screen gutter 16dp.

### Elevation & motion

Dark elevation is conveyed by tonal surface color; shadow (`Elevation`, levels 1–5) is reserved for FAB, menus, dialogs, and the fire-time heads-up notification. Cards default to flat filled surfaces. The shadow ramp is identical in both schemes — depth in dark comes from the tonal `surfaceContainer*` roles, not from different shadow values.

Motion (`Motion`): standard easing `cubic-bezier(0.2,0,0,1)`, emphasized easing `cubic-bezier(0.05,0.7,0.1,1)` (plus decelerate/accelerate variants); durations short 150 / medium 250 / long 400 ms. Press scales the target down (button 0.97, icon button 0.90) and, on filled surfaces, shifts to the pressed color; hover applies an 8% state layer. Streaming caret blinks at 1s. No infinite decorative loops. No glass/backdrop blur; `scrim` dims behind dialogs.

Honouring the system reduce-motion setting is out of scope. Compose does not apply the platform animator scale to these durations automatically, so it would be deliberate work; if it is ever specced, `Motion` becomes a CompositionLocal again so durations can be zeroed in one place rather than checked in every animated component.

## Theme entry

```
ChatbotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

Selects the dark or light `ColorScheme`, installs `MaterialTheme(colorScheme, typography, shapes)`, and provides the extended-token CompositionLocals. No `dynamicColor` parameter. Edge-to-edge is applied at the app level (per the `edge-to-edge` skill), not here.

## Icon system

`Icon` wraps **Material Symbols Rounded**, shipped as a **variable-font asset in `:core:ui`** (the source loads it from the Google Fonts CDN; the app bundles it in-APK so chrome renders offline). Glyphs are referenced by ligature name; variable axes `FILL` (0 at rest → 1 when selected/active), `wght` (default 400), `GRAD`, and optical size are settable per use. No PNG/SVG icon assets; emoji are never UI icons. The entire icon set is that one bundled font file — no per-icon drawables; subsetting the font to only used glyphs is a deferred APK-size optimization.

Every ligature the app renders is a constant on `Glyphs` — model identity (Sonnet → `balance`, Haiku → `bolt`, Opus → `auto_awesome`), the brand mark, and UI chrome — so no call site spells one out. Feature modules extend it as their screens need glyphs.

**Brand mark.** The wordmark renders lowercase **bro** in Roboto Medium beside a `forum`-glyph tile in the accent color. No standalone logo asset exists and none is to be invented.

**Naming.** "Bro" is the **display name only** — launcher label, wordmark, and user-facing copy. Code identifiers — Gradle projects, packages, modules, classes, files, functions — use the neutral project name (`Chatbot` / domain terms) and never "Bro". This is a project-wide convention (also recorded in the `architecture` skill), stated here because 002 owns the brand mark.

## Component catalog

All components are stateless and presentational — state in via parameters, events out via lambdas — so feature modules own state and these stay screenshot-testable. Grouped as in the source project:

- **core** — `Button` (filled / tonal / outlined / text / elevated; a `loading` state distinct from disabled — full colour and label kept, trailing slot becomes a spinner, click swallowed), `IconButton` (standard / filled / tonal / outlined), `Icon`, `Card` (filled / outlined / elevated), `Badge`.
- **forms** — `TextField` (outlined / filled, floating label, focus border thickens to 2px accent), `Switch`, `Chip` (assist / filter / input / suggestion).
- **feedback** — `Dialog`, `Snackbar`, `LoadingIndicator`, `ErrorState`.

**Strings.** No component holds a user-visible literal. Text a user reads or TalkBack speaks resolves from `:core:ui`'s `strings.xml`, and the module declares only strings that are generic by nature — a loading state description, a retry label, a dismiss description, the wordmark. Per-screen copy is a component parameter: the feature owns the words and resolves them from its own resources at the call site, which is what keeps `:core:ui` free of product vocabulary. Material Symbols ligature names are glyph identifiers rather than text, so they stay in code — but only in `Glyphs`, never at a call site: a mistyped ligature renders nothing and no compiler catches it.

**What is deliberately not here.** A component earns a place in `:core:ui` by having more than one consumer *and* a shape that does not vary per screen. Two families fail that test:

- **Chat surfaces** — `MessageBubble`, `ConversationListItem`, `ModelPicker`. Single consumer (`:feature:conversation`), and their props are domain-shaped (message role, tool chips, model identity), which `:core:ui` cannot see. Hosting them here would mean duplicating those concepts as DS-local enums and mapping to them at every call site. They are built in the feature module under spec 005.
- **Empty states** — structurally different per screen, not merely different in copy: the conversation screen wants a hero block, a list screen wants icon + line + optional CTA. A shared component would freeze a guessed layout before any screen exists to validate it. Features build their own; hoist only once two screens demonstrably share a structure.

The design system remains the source of truth for their *appearance* regardless: bubble shapes (`ChatbotShapes.bubbleUser`/`bubbleAssistant`), the streaming-caret duration, and the model glyphs are tokens defined here and consumed there. Appearance SSOT is the token vocabulary, not every composition built from it.

**Naming.** Components keep the familiar Material 3 names (`Button`, `Icon`, `Card`, …) even though they collide with the `androidx.compose.material3` composables. This is safe under one boundary rule: **feature modules import components only from `:core:ui`, never `androidx.compose.material3` directly** — so each feature file has exactly one `Button` (ours) in scope. Only the wrapper files inside `:core:ui` see both names, and they alias the M3 original (e.g. `import androidx.compose.material3.Button as M3Button`). The rule is enforced by convention (and, where practical, lint) and recorded in the `architecture` / `design-system` skills.

## Screenshot testing

Compose Preview Screenshot Testing (`com.android.compose.screenshot` plugin, `@PreviewTest` + `@Preview`), wired into the version catalog and `:core:ui`. Each component ships previews covering its variants in **both** dark and light. Roborazzi is the documented fallback if the preview-channel tool blocks.

## Companion skill

A project skill at `.claude/skills/design-system/SKILL.md` documents, for later feature work: token accessors and names, the component list with when-to-use guidance, the mono-for-keys/ids/code rule, the model-glyph map, brand-mark rules ("do not invent a Bro logo"), and the flat-fills / no-gradients / sparing-shadow constraints. It grows as the catalog grows in later milestones.
