---
name: build-logic
description: This project's Gradle convention plugins and what each module build file may declare. Use when creating or editing any build.gradle.kts, adding a module, applying a Gradle plugin, or changing SDK and JVM levels.
metadata:
  keywords:
  - gradle
  - build.gradle.kts
  - build-logic
  - convention plugin
  - version catalog
  - libs.versions.toml
  - compileSdk
  - minSdk
  - jvmToolchain
  - new module
---

Shared Android and Kotlin configuration lives in convention plugins under
`build-logic/`, an included build. A module script declares only what is unique to
that module.

## The plugins

Applied by catalog alias, for example `alias(libs.plugins.chatbot.android.library)`.

| Plugin | What it configures | Apply to |
|---|---|---|
| `chatbot.android.application` | Application plugin, SDK and JVM levels, Compose | the app module |
| `chatbot.android.library` | Library plugin, SDK and JVM levels | a library with no Compose UI |
| `chatbot.android.library.compose` | the above, plus the Compose plugin and `compose = true` | a library with Compose UI |
| `chatbot.android.library.screenshot` | the above, plus screenshot tests and the Robolectric runtime | a Compose library whose previews are screenshot-tested |
| `chatbot.kmp.library` | Multiplatform and Android KMP library plugins, JVM level, Robolectric runtime | a module with shared Kotlin |
| `chatbot.robolectric` | the JVM that `Test` tasks run on | already inside the two rows above; apply alone for a module that runs Robolectric without them |

## A new module

1. Add it to `include(...)` in `settings.gradle.kts`.
2. Apply exactly one plugin from the table.
3. Set `namespace`, then declare dependencies. Nothing else belongs there.

## Rules

- **A module never sets `compileSdk`, `minSdk`, `targetSdk`, `compileOptions`,
  `jvmToolchain`, or `compose = true`.** The convention plugin owns them. A module
  keeps `namespace`, its own `buildFeatures`, `dependencies`, and per-module plugins
  such as KSP, Hilt, serialization or Room.
- **SDK and JVM levels are `Int` constants in
  `build-logic/convention/src/main/kotlin/AndroidVersions.kt`.** Edit there and every
  module follows. They are deliberately not catalog entries, whose values are always
  strings.
- **Dependency and third-party plugin versions stay in `gradle/libs.versions.toml`.**
  The convention plugins are catalog entries carrying an id and no version, since the
  build compiles them from source.
- **Inside a convention plugin, plugin ids are literal strings.** Gradle extracts that
  `plugins` block and compiles it before any project exists, so `libs` and `alias()`
  cannot resolve. Only module scripts get the catalog.
- **A convention plugin that applies a new third-party plugin also needs that plugin's
  artifact in `build-logic/convention/build.gradle.kts`.** `compileOnly` covers a
  plugin named in a `plugins` block; one applied by id from the script body has to be
  `implementation` to reach the consuming build at execution time.

## Verify

Configuration and convention-plugin compilation errors surface without a full build:

```
./gradlew help
```
