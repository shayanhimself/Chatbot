# 002 — Design System

The `:core:ui` module: the app's Material 3 design system — theme, design tokens, and a reusable Compose component catalog. Every feature module renders through it; it depends on nothing else in the project.

The single source of truth for the visual design is the **"Bro Design System"** Claude Design project (with screen mockups in the companion **"Bro designs"** project). This spec is a *downstream translation* of that source into Android/Compose terms — it states what `:core:ui` provides and the rules for using it, while raw ramp constants and per-component prop contracts stay in the source. When the design changes upstream, this spec and `:core:ui` are what needs re-syncing; the values here can therefore drift and should be re-verified against the source rather than trusted blindly. Pull the current tokens and designs with the **`pull-design`** skill.

Dark-first: dark is the default scheme; light is a full opt-in scheme. No Material You / dynamic color — the crafted navy+orange identity ships on every device. No gradients; flat tonal fills only.

## Source of truth & where values live

One home per fact:

- **Claude Design ("Bro Design System")** — upstream SSOT for the visual design. Everything below mirrors it and can drift; re-sync with the `pull-design` skill.
- **`:core:ui` Kotlin token files** — the in-project home for exact values (hex, sp, primitive names), once built: one typed definition, consumed by the code.
- **`design-system` skill** — owns *usage* (how to find token accessors and catalog components in the code, the design→Compose translation, naming boundary rule) and the manual **sync procedure** (`pull-design` → diff → update code + usage → stamp date). It points to the token files for current values; it does not duplicate them, and the design — not the skill — decides which role/component each element uses.
- **This spec** — owns the durable *what/why*: decisions and rationale, the two-tier color structure, role names, component families, screenshot strategy, naming rule. It does not own volatile values.

Exact values live in the `:core:ui` Kotlin token files, not here — this spec keeps only structure and rationale and points to the code for each family. Downstream copies carry a `last synced from Bro DS, <date>` provenance line so staleness is visible.

## Module

- `:core:ui`, package `com.shayanaryan.chatbot.core.ui`. Android library (not KMP — pure UI).
- Depends only on Compose (BOM), Material 3, and the icon/screenshot tooling below. No dependency on `:shared` or any feature module.
- Replaces the placeholder AGP-template theme (`Color.kt` / `Type.kt` / `Theme.kt`).

## Token layer

Standard tokens flow through `MaterialTheme` (`ColorScheme`, `Typography`, `Shapes`). Tokens M3 has no slot for — semantic/extended colors, motion, the mono text style, named component shapes — live in `:core:ui`, split by whether their value depends on runtime state.

A token set earns a CompositionLocal only if its value depends on the active color scheme or a user/system preference. `ExtendedColors` is the only one: the `ChatbotTheme` composable installs it, and it is read through the `ChatbotExtendedTheme.colors` accessor. Everything else is constant and read directly — the `Spacing`, `Elevation`, `ChatbotShapes` and `Motion` sets and `MonoTextStyle` — with no theme lookup, and usable outside composition (draw scopes, previews, test fixtures).

### Color

The source organizes color in two tiers, and the Android layer mirrors it: a **primitive palette** (every literal value, named once by hue + tone) and two **semantic `ColorScheme`s** (dark default + light) whose roles each reference a primitive. Components read only roles. Both schemes fully specify every role — the light scope darkens hues for legibility on light surfaces rather than reusing the dark tones. Semantic colors M3 has no slot for are an extended set, read via `ChatbotExtendedTheme.colors`.

### Typography

Full 15-role M3 scale on **Roboto**, with per-role weights from the source.

A monospace style (`MonoTextStyle`) — API keys, model ids, code — is the one text style outside the M3 scale. It resolves to the device monospace via `FontFamily.Monospace`; bundling a Roboto Mono asset is deferred until fidelity demands it.

### Shape

Named component shapes (`ChatbotShapes`) layer over the M3 `Shapes` ramp: pill buttons and chips, plus chat bubbles. A named shape that also exists on the M3 scale resolves to it rather than restating a radius, so it cannot drift from the M3 role that M3 components read internally. A bubble is rounded except for one squared tail corner — bottom-end for the user, bottom-start for the assistant.

### Elevation & motion

Dark elevation is conveyed by tonal surface color; shadow (`Elevation`) is reserved for FAB, menus, dialogs, and the fire-time heads-up notification. Cards default to flat filled surfaces. The shadow ramp is identical in both schemes — depth in dark comes from the tonal `surfaceContainer*` roles, not from different shadow values.

Motion (`Motion`): a set of easings and durations. Press scales the target down and, on filled surfaces, shifts to the pressed color; hover applies a state layer. Streaming caret blinks at a fixed interval. No infinite decorative loops. No glass/backdrop blur; `scrim` dims behind dialogs.

Honouring the system reduce-motion setting is out of scope. Compose does not apply the platform animator scale to these durations automatically, so it would be deliberate work; if it is ever specced, `Motion` becomes a CompositionLocal again so durations can be zeroed in one place rather than checked in every animated component.

## Theme entry

```
ChatbotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
```

Selects the dark or light `ColorScheme`, installs `MaterialTheme(colorScheme, typography, shapes)`, and provides the extended-token CompositionLocals. No `dynamicColor` parameter. Edge-to-edge is applied at the app level (per the `edge-to-edge` skill), not here.

## Icon system

`Icon` wraps **Material Symbols Rounded**, shipped as a **variable-font asset in `:core:ui`** (the source loads it from the Google Fonts CDN; the app bundles it in-APK so chrome renders offline). Glyphs are referenced by ligature name; variable axes `FILL` (rest → active), `wght`, `GRAD`, and optical size are settable per use. No PNG/SVG icon assets; emoji are never UI icons. The entire icon set is that one bundled font file — no per-icon drawables; subsetting the font to only used glyphs is a deferred APK-size optimization.

Every ligature the app renders is a constant on `Glyphs` so no call site spells one out. Feature modules extend it as their screens need glyphs. Models carry no glyph: a model is identified by name only, never an icon.

**Brand.** The lowercase **bro** wordmark string (`core_ui_brand_wordmark`) and the `forum` glyph tile are the reserved brand vocabulary; No standalone logo asset exists (yet).

**Naming.** "Bro" is the **display name only** — launcher label, wordmark, and user-facing copy. Code identifiers — Gradle projects, packages, modules, classes, files, functions — use the neutral project name (`Chatbot` / domain terms) and never "Bro". This is a project-wide convention (also recorded in the `architecture` skill), stated here because 002 owns the brand vocabulary.

## Component catalog

All components are stateless and presentational — state in via parameters, events out via lambdas — so feature modules own state and these stay screenshot-testable. Grouped as in the source project:

- **core** — `DsButton`, `DsIconButton`, `DsIcon`, `DsCard`, `DsBadge`. `DsButton` carries a `loading` state distinct from disabled: full colour and label kept, trailing slot becomes a spinner, click swallowed.
- **forms** — `DsTextField`, `DsSwitch`, `DsChip`.
- **feedback** — `DsDialog`, `DsSnackbar`.

**Strings.** No component holds a user-visible literal. Text a user reads or TalkBack speaks resolves from `:core:ui`'s `strings.xml`, and the module declares only strings that are generic by nature — a loading state description, a retry label, a dismiss description, the wordmark. Per-screen copy is a component parameter: the feature owns the words and resolves them from its own resources at the call site, which is what keeps `:core:ui` free of product vocabulary. Material Symbols ligature names are glyph identifiers rather than text, so they stay in code — but only in `Glyphs`, never at a call site: a mistyped ligature renders nothing and no compiler catches it.

**What is deliberately not here.** A component earns a place in `:core:ui` by having more than one consumer *and* a shape that does not vary per screen. Two families fail that test:

- **Chat surfaces** — `MessageBubble`, `ConversationListItem`, `ModelPicker`. Single consumer (`:feature:conversation`), and their props are domain-shaped (message role, tool chips, model identity), which `:core:ui` cannot see. Hosting them here would mean duplicating those concepts as DS-local enums and mapping to them at every call site. They are built in the feature module under spec 005.
- **Empty states** — structurally different per screen, not merely different in copy: the conversation screen wants a hero block, a list screen wants icon + line + optional CTA. A shared component would freeze a guessed layout before any screen exists to validate it. Features build their own; hoist only once two screens demonstrably share a structure.

The design system remains the source of truth for their *appearance* regardless: bubble shapes (`ChatbotShapes.bubbleUser`/`bubbleAssistant`) and the streaming-caret duration are tokens defined here and consumed there. Appearance SSOT is the token vocabulary, not every composition built from it.

**Naming.** Catalog components carry a `Ds` prefix (`DsButton`, `DsIcon`, `DsCard`, …). Feature modules legitimately use both the catalog and `androidx.compose.material3` directly — the latter for M3 components the catalog does not wrap — so an un-prefixed `Button` would leave a reader unsure whether it is ours or M3's. The prefix makes provenance obvious at every call site and lets both coexist in one file with no import aliasing. Inside `:core:ui`, a wrapper still aliases the M3 original it delegates to (e.g. `import androidx.compose.material3.Button as M3Button`). Recorded in the `architecture` / `design-system` skills.

## Screenshot testing

Compose Preview Screenshot Testing (`com.android.compose.screenshot` plugin, `@PreviewTest` + `@Preview`), wired into the version catalog and `:core:ui`. Each component ships previews covering its variants in **both** dark and light. Roborazzi is the documented fallback if the preview-channel tool blocks.

## Companion skill

A project skill at `.claude/skills/design-system/SKILL.md` is what later feature work loads to build a screen: a short screen recipe, pointers to look tokens and catalog components up **in the code** (the code is the SSOT for names and values), the design→Compose translation layer, the M3-import boundary rule, and the manual upstream-sync procedure. Which role/component each element uses is decided by the design, not restated in the skill. It grows as the catalog grows in later milestones.
