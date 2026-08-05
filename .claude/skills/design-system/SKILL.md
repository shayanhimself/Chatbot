---
name: design-system
description: Use when building or styling any screen or component in this app — choosing colors, text styles, spacing, shapes, icons, or picking a catalog component.
metadata:
  keywords:
  - design system
  - theme
  - colorScheme
  - typography
  - text style
  - Spacing
  - Jetpack Compose
  - Screen
  - Compose screen
---

# Chatbot Design System (`:core:ui`)

Material 3.

- **Design System SSOT is**: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/`.
  It mirrors upstream and is canonical for exact token values, accessor names, and
  component APIs/variants. **Read it** to find the right design→code mapping.

## Building a screen

1. Content is already wrapped in `ChatbotTheme { }` at the app root (`MainActivity`),
   so `MaterialTheme` colors/type/shapes are in scope.
2. Screen gutter: `Modifier.padding(horizontal = Spacing.gutter)`. Space on the
   `Spacing` scale — never a raw `.dp`.
3. Edge-to-edge is applied at app level (see the `edge-to-edge` skill) — consume
   system insets via `Scaffold`/`WindowInsets`, don't hardcode status/nav padding.
4. Compose from the catalog: the `Ds*` components in `…core.ui.designsystem.component`
   (`DsButton`, `DsTextField`, …) and `DsIcon` from `…designsystem.icon`. Anything the
   catalog does *not* wrap comes straight from `androidx.compose.material3` / foundation —
   the `Ds` prefix keeps the two unambiguous, so both live in one file with no import
   aliasing. If the catalog lacks a component you need more than once, wrap it in the
   feature module — not inline, not forced into `:core:ui`.

## Tokens & components — look them up in the code

Inventory, so you know what exists to grep for (exact members,
variants and values live in the file):

- theme (`…designsystem/theme/`): `MaterialTheme.colorScheme` roles + `ChatbotExtendedTheme.colors`;
  `MaterialTheme.typography` + `MonoTextStyle`; `ComponentShapes`; `Spacing`;
  `Elevation`; `Motion`.
- component (`…designsystem/component/`): `DsButton`, `DsIconButton`, `DsCard`, `DsBadge`, `DsTextField`,
  `DsSwitch`, `DsChip`, `DsDialog`, `DsSnackbar`.
- icon (`…designsystem/icon/`): `DsIcon`, `Glyphs`.

How they're accessed:

- Standard M3 through the theme: `MaterialTheme.colorScheme` / `.typography` / `.shapes`.
- Scheme-dependent tokens M3 has no slot for: `ChatbotExtendedTheme.colors` — the
  only CompositionLocal-backed set.
- Everything else is a plain constant, read directly — no theme lookup, usable
  outside composition (`Spacing`, `Elevation`, `ComponentShapes`, `Motion`, `MonoTextStyle`).
- Touch targets: `--touch-target-min` as a width, height or min-height is
  `Spacing.touchTargetMin`, applied through whichever `Modifier` sizes the element
  (`size`, `defaultMinSize`, `heightIn`). Never a spacing step, never a raw `.dp`.

## Which token / component to use = the design decides

The mockup dictates the color role, type, and component for each element (see
"Reading a design → Compose"). This skill carries no when-to-use guidance per role
or component — that call lives in the design. Your job: translate what the design
specifies into the looked-up code token/component.

## Reading a design → Compose

Screen mockups come from the *Chatbot designs* project (`pull-design`). The markup
already names the DS component, color role and type for each element — translate,
don't reinvent.

A mockup may wrap the screen in a **phone frame** (device bezel, status-bar clock,
battery/signal glyphs, a home indicator) purely to show how it looks on a phone.
That chrome is presentation, not app UI — don't build it. The real system bars come
from the platform (edge-to-edge insets); Build only the screen content inside the
frame.

- **Color — exact.** Mockup writes a role name: `var(--on-surface-variant)` →
  `MaterialTheme.colorScheme.onSurfaceVariant` (kebab → camel). Roles M3 lacks:
  `var(--success)` → `ChatbotExtendedTheme.colors.success`. Never a hex; Never
  primitives (`ColorPrimitives`), unless the role doesn't exist.
  The theme already resolves dark/light.
- **Type — exact, 1:1.** Mockup puts a role class on the element:
  `class="type-title-large"` → `MaterialTheme.typography.titleLarge` (kebab → camel),
  same shape as the color rule. `class="type-mono"` → `MonoTextStyle`. The class
  carries family, weight, size, line-height and tracking as one unit, so never author
  a `FontWeight`, a `.sp` size or a `letterSpacing` at a call site. An element with no
  `type-*` class inherits one from an ancestor — walk up before deciding.
  `font-size` on a `.msy` span is an icon size, not type (see Icons/glyphs).
  A frame that still writes raw px + weight (`font:500 22px Roboto`) is stale,
  flag it and report it to the user, to update the design.
- **Spacing & radius — exact, 1:1.** `Spacing` is for padding / margin / gaps,
  *not* component size. `var(--space-4)` → `Spacing.s4`.
  Radius is component-scoped where the design names a component (`--radius-full`
  → `CircleShape`, `--radius-card` → `ComponentShapes.card`). On a plain surface,
  `--radius-N` takes the `MaterialTheme.shapes.*` slot holding that radius, and falls
  back to `RoundedCornerShape(RadiusPrimitives.radiusN)` only for a radius no slot carries.
  - Raw px in app UI is stale, report it so the design gets
    tokenized. Never silently absorb an off-grid or untokenized value.
  - Exception: Phone-frame chrome keeps raw px because it is never built.
- **Explicit size — px is dp, use `.dp` directly.** A fixed width/height/icon size
  (`width:40px`, `size:24px`) is *not* spacing. 1 px in design = 1 dp in Android, so write it as a plain `.dp`.
  `Spacing` is padding/margin/gaps only.
- **Sizing props — dropped.** Mockup may pass `Button size="small"`,
  `fullWidth`, `IconButton size="{{48}}"`. Our components have no `size`/
  `fullWidth` — ignore them, size via `Modifier` (`fillMaxWidth()`, `heightIn()`).
- **Component vs composition — read the tag.**
  `<x-import …BroDesignSystem_c5a603.IconButton>` = a catalog component → call it, map
  props. A plain token-styled `<div>` (the chat composer, a settings row) = **not**
  in the catalog. Do not invent a DS component for it. If DS genuinely lacks the
  UI, build a component **in the feature module** that needs it, never
  forced into `:core:ui`. Hoisting to DS is a later, higher-bar call (two+ consumers
  + a shape that doesn't vary per screen; In that case ask user to update the upstream DS).
- **Icons/glyphs — ligature → `Glyphs` constant.** Mockup shows a Material Symbols
  ligature (`<span class="msy">forum</span>`, or an `icon="arrow_back"` prop) →
  `DsIcon(Glyphs.X)`. Never pass a bare ligature string — a typo renders nothing and
  compiles fine, so every glyph goes through a `Glyphs` constant. Glyph not in
  `Glyphs` yet → add it there.

## Sync with upstream (manual — only when the user invokes this skill for it)

`:core:ui` mirrors the upstream Chatbot Design System and can drift from it. Re-syncing
is a deliberate step the user triggers by explicitly invoking this skill — never run
it as a side effect of building a screen.

1. Using `pull-design` skill → fetch the current tokens and component contracts from the Chatbot
   Design System project.
2. Diff each resolved value against the Kotlin code (`…designsystem/`); change only what actually differs, re-verify the rest.
3. Update this skill only if accessor names, components, or the design→code mapping
   changed — not for value-only changes (values live in the code, not here).
4. Re-record the screenshot goldens and review the image diffs.
