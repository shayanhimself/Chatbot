# 005 Conversation Shell Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the conversation list and chat screens over 003's stream and 004's storage, wired by a Nav 3 back stack that adapts to two panes, so the app is sideloadable at the M1 checkpoint.

**Architecture:** `:shared` gains two repository reads (`getConversationFlow`, a projected `snippet`) and a new sibling module `:shared:testing` that publishes its fakes as production code so feature modules can consume them. `:feature:conversation` owns two stateful routes, two stateless screens, and their components; it takes no Navigation 3 dependency. `:app` owns the typed keys, the `NavDisplay`, the list-detail scene, the deep-link seam, and a debug-only dev key.

**Tech Stack:** Kotlin 2.4.10, AGP 9.3, Compose BOM 2026.06.01, Room 2.8.4, Hilt 2.60.1, Navigation 3 1.1.4, Material 3 Adaptive Navigation 3 1.3.0-rc01, Robolectric 4.16.1, Compose Preview Screenshot Testing 0.0.1-alpha15.

## Global Constraints

- **Never commit or push.** `CLAUDE.md` overrides the superpowers default: this plan has **no commit steps**. Each task ends with a **Checkpoint**: run the verification commands, confirm the output, stop for review. The user commits.
- execute tasks in the current branch, no worktree
- when task is done, check all the steps of that task
- **"buddy" is display-name only.** Never in identifiers, packages, files, functions. Code uses `Chatbot` or plain domain terms.
- **`const val` is `SCREAMING_SNAKE_CASE`**; non-const `val` is `camelCase`.
- **No trailing (end-of-line) comments.** Comment on its own line above the code.
- **No em dashes in prose** (specs, docs, comments). The exception is user-facing copy lifted verbatim from a mockup, which stays exactly as the design wrote it.
- **KDoc on every interface and contract**, and on anything not self-explanatory from its name and signature.
- **Test function names use backtick spaced form**: `` fun `does the thing`() ``.
- **Import order is ktlint_official's**: everything else first (including `kotlinx.*`), then `java.*`, `javax.*`, `kotlin.*`. So `javax.inject.Inject` sits *above* `kotlin.time.Clock`. `spotlessApply` fixes it either way; writing it right keeps the diff honest.
- **Every public composable ships a colocated plain `@Preview`** in its own file, one per distinct visual state, wrapped in `ChatbotTheme(darkTheme = true)`: the app is dark-first, and an unqualified `ChatbotTheme` renders light because the preview renderer defaults `uiMode` to `UI_MODE_NIGHT_NO`. Wrap the content in a `Surface` whenever the composable paints no background of its own, since `showBackground = true` paints white underneath it. Screenshot goldens live separately in `src/screenshotTest/`.
- **No mocking library.** Fakes and real objects only.
- **No ViewModel → UI event channels.** Every outcome is folded into `UiState`.
- **No async work in ViewModel `init` or constructors.** State comes from the `stateIn` pipeline.
- **Navigation 2 / `navigation-compose` is prohibited**. This is load-bearing for Task 1's dependency choice.
- **TDD throughout**: red → green → refactor.
- **Spacing from `Spacing`, radius from `MaterialTheme.shapes`/`ComponentShapes`, falling back to `RadiusPrimitives` only for a radius no slot carries, color from `MaterialTheme.colorScheme` / `ChatbotExtendedTheme.colors`, type from `MaterialTheme.typography`, glyphs from `Glyphs`.** Never a raw hex, never a bare ligature string. Explicit component sizes (`width`, `height`, icon size) are plain `.dp`.
- Verification uses **debug** tasks from Task 3 onward: `:app:assembleRelease` stops working the moment a ViewModel injects `ConversationRepository`, because only then must Dagger resolve `ApiKeyProvider`. That is deliberate; 006 restores it. Debug keeps building because Task 3 Step 11a pulls Task 8's debug-only dev key forward to exactly that moment.

## Design source

Pulled with `pull-design` from the *Chatbot designs* project (`f6b3ad66-7433-4a29-92da-213733550154`):

| File | Frames used |
|---|---|
| `Screen-Conversation List.dc.html` | 2a populated, 2b empty, 2e loading (2c/2d search are M4) |
| `Component-ConversationListItem.dc.html` | the row |
| `Screen-Chat.dc.html` | 3a, 3b, 3e, 3f, 3g, 3h, 3i, 3j, 3k (3c/3d tool chips are 008+) |
| `Screen-Light Theme.dc.html` | 7b, 7c, the light renderings of the same two screens |

`MessageBubble` and `ModelPicker` contracts come from the Design System project (`c5a6030b-52d3-4ecc-ab51-4460eebdc7df`, `components/chat/`). Both are built **in `:feature:conversation`**, not `:core:ui`, per 002: domain-shaped props, one consumer.

### Deliberate deviations from the mockups

Report these to the user at the end; do not silently absorb them.

1. **Bold spans dropped.** Frames 3g and 3h bold a fragment of the error sentence. The row renders one text style; no `AnnotatedString`.
2. **Model names come from `ClaudeModel.displayName`**, so the picker reads "Opus 5", not the mockup's stale "Opus 4.8".
3. **Phone bezel, status bar, home indicator are catalog scaffolding**, never built. Real system bars come from edge-to-edge insets.

## Adaptive posture

Checked against the `adaptive` skill's five steps, so every one of them is either done or a recorded decision.

| Skill step | This plan |
|---|---|
| 1. Form-factor screenshot coverage | Done. Both screens ship a `FormFactorPreviews` annotation covering phone, foldable, tablet and desktop, alongside the per-frame goldens 005 asks for. |
| 2. Adaptive navigation area (`NavigationSuiteScaffold`) | Not applicable. The app has no navigation bar and no top-level destination switcher: the list is the home screen and the chat is its detail. 007's settings entry point is the first thing that could change that. |
| 3.1. List-detail via `SceneStrategy` | Done, and it is the reason the whole nav graph sits in `:app`. `rememberListDetailSceneStrategy` on `NavDisplay.sceneStrategies`, `listPane(detailPlaceholder = …)` and `detailPane()` metadata. **`ListDetailPaneScaffold` is not used**, per the skill's explicit instruction. The detail pane carries no back arrow on a wide window. |
| 3.2. Supporting pane | Not applicable. There is no third surface supporting either screen. |
| 4. Adaptive column count | Deliberately skipped for the conversation list, and it must not be applied to the message list. The list is a column of full-width text rows inside a list pane the scene already bounds, so `GridCells.Adaptive` resolves to one column at every width the app runs at; the design draws one column in both 2a and 3k. Chat bubbles are a conversation, not a collection, so a grid would be wrong at any width. Revisit if a genuinely wide single-pane surface appears. |
| 5. Collapsing app bars on scroll | Deliberately skipped. The chat's top bar holds the title and the only route to delete, and the list's holds the only heading; neither competes for space at any supported width. No mockup collapses either. |


---

### Task 1: Version catalog and the `:shared:testing` module

004 left `FakeConversationRepository` in `:shared` commonTest, where no other Gradle project can see it: test source sets are not published to consumers and Kotlin has no KMP equivalent of test fixtures. This task creates the module that fixes it and pins the two libraries the rest of the plan needs.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts:27-35`
- Modify: `shared/build.gradle.kts:47-58`
- Create: `shared/testing/build.gradle.kts`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt` → `shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt` → `shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeScriptedChatEngine.kt` → `shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/FakeScriptedChatEngine.kt`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt` → `shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt` → `shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt`
- Move: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt` → `shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt`

`TestChatEngine.kt` and `SseFixtures.kt` **stay** in `:shared` commonTest: `ClaudeChatEngine` and `installChatDefaults()` are `internal` to `:shared`, so a helper that builds a real engine over a `MockEngine` cannot compile from another module.

**Interfaces:**
- Produces: Gradle project `:shared:testing`, consumed as `testImplementation(projects.shared.testing)`. Publishes `FakeClock`, `FakeConversationRepository`, `FakeScriptedChatEngine`, `FakeManualChatEngine` in their existing packages.
- Produces: catalog aliases `libs.androidx.hilt.lifecycle.viewmodel.compose`, `libs.androidx.adaptive.navigation3` and `libs.kotlin.test.junit`.

- [x] **Step 1: Confirm the baseline is green before touching anything**

```bash
./gradlew :shared:testAndroidHostTest :core:ui:testDebugUnitTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. If it is not, stop and report. No task starts on a red baseline.

- [x] **Step 2: Pin the three missing libraries**

Both are additions to `gradle/libs.versions.toml`. In `[versions]`, after `screenshot = "0.0.1-alpha15"`:

```toml
androidxHilt = "1.4.0"
adaptiveNavigation3 = "1.3.0-rc01"
```

In `[libraries]`, after `androidx-navigation3-ui`:

```toml
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
androidx-adaptive-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "adaptiveNavigation3" }
```

and, after `kotlin-test`:

```toml
kotlin-test-junit = { group = "org.jetbrains.kotlin", name = "kotlin-test-junit", version.ref = "kotlin" }
```

`kotlin-test` alone is enough in `:shared`, where the Kotlin Multiplatform plugin substitutes the
framework-specific variant. `:feature:conversation` is a plain Android library, nothing performs
that substitution there, and every `kotlin.test` symbol the feature's tests import (`Test`,
`BeforeTest`, `AfterTest`, `assertEquals`) is unresolved without this artifact. `:core:ui` never
hit it because it imports JUnit 4 annotations directly.

Two things this deliberately does **not** do, both verified against the published POMs:

- It does **not** add `androidx.hilt:hilt-navigation-compose`, which 005 names. That artifact's 1.4.0 POM declares `androidx.navigation:navigation-compose:2.9.0` as a `compile` dependency, which would put Navigation 2 on the classpath, which 001 prohibits. `hilt-lifecycle-viewmodel-compose` is the artifact that actually carries `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (including the `creationCallback` overload the nav3 recipe uses) and depends on no navigation library at all.
- It does **not** give `adaptive-navigation3` a `version.ref = "navigation3"`. It ships from `androidx.compose.material3.adaptive`, whose releases are independent of `androidx.navigation3`. It has no stable release: the published versions run `1.0.0-alpha01…03` then `1.3.0-alpha01…rc01`, so `1.3.0-rc01` is the newest and the only sensible pin. Its POM pulls `navigation3-ui` `1.0.0` at runtime, which our `1.1.4` satisfies.

`lifecycle-viewmodel-navigation3` is already pinned via `version.ref = "lifecycle"` and 2.11.0 exists, so no change is needed.

- [x] **Step 3: Register the module**

In `settings.gradle.kts`, add `":shared:testing"` to the `include(…)` list, after `":shared"`:

```kotlin
include(
    ":app",
    ":core:ui",
    ":shared",
    ":shared:testing",
    ":feature:conversation",
    ":feature:onboarding",
    ":feature:settings",
    ":feature:reminders",
)
```

- [x] **Step 4: Create the module's build script**

Create `shared/testing/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.shayanaryan.chatbot.shared.testing"
        compileSdk = 37
        minSdk = 31

        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation: every fake's public surface is a :shared type: the
            // repository interface it implements, the models it returns. A consumer that
            // sees the fake must see those too.
            api(projects.shared)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
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
```

The fakes live in `commonMain`, a production source set, which is exactly what makes them visible to other Gradle projects. Every consumer takes the module as `testImplementation`, so nothing reaches an APK. The graph stays acyclic because Gradle resolves classpaths per source set: `:shared`'s main compilation and its test compilation are separate units, and only the test one points at `:shared:testing`.

- [x] **Step 5: Move the four fakes and their two tests**

```bash
mkdir -p shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/{chat,conversation}
mkdir -p shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/{chat,conversation}

git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt \
       shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt
git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt \
       shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt
git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeScriptedChatEngine.kt \
       shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/FakeScriptedChatEngine.kt
git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt \
       shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt
git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt \
       shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt
git mv shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt \
       shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt
```

Package declarations are unchanged: keeping the fakes in the packages they already sit in costs no churn in `:shared`'s own tests and is legal on Android. Two Gradle projects then share a package, and `internal` still does not cross between them, which is why `TestChatEngine.kt` had to stay behind.

- [x] **Step 6: Point `:shared`'s tests at the new module**

In `shared/build.gradle.kts`, add one line to each test source set:

```kotlin
        commonTest.dependencies {
            implementation(projects.shared.testing)
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        // No typed accessor exists for this source set.
        getByName("androidHostTest").dependencies {
            implementation(projects.shared.testing)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
```

- [x] **Step 7: Run both modules' tests**

```bash
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest
```

Expected: PASS for both. `:shared`'s existing tests import the fakes from packages that have not changed, so no import edits are needed anywhere. A failure here is a missing dependency edge, not a test regression.

- [x] **Step 8: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Report the new module and the two pinned versions, including the `hilt-navigation-compose` → `hilt-lifecycle-viewmodel-compose` substitution and its reason. Stop for review.

---

### Task 2: `snippet` and `getConversationFlow` in `:shared`

004's contract defers both to this spec. Neither changes the schema: `snippet` is a correlated subquery on the query that already backs the list, and `getConversationFlow` is a second read of the same projection.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/ContentText.kt`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/Conversation.kt:8-14`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationWithSnippet.kt`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDao.kt:26-27`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/Mappers.kt:9-16`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepository.kt:16-17`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/DefaultConversationRepository.kt:64-65`
- Modify: `shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDaoTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationSnippetTest.kt` (new)
- Test: `shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt`

**Interfaces:**
- Produces: `fun List<ContentBlock>.textContent(): String` in `com.shayanaryan.chatbot.shared.chat`.
- Produces: `Conversation(id: Long, title: String, model: ClaudeModel, snippet: String?, createdAt: Instant, updatedAt: Instant)`.
- Produces: `ConversationRepository.getConversationFlow(conversationId: Long): Flow<Conversation?>`.
- Consumes: nothing new from Task 1 beyond the module the fake now lives in.

- [x] **Step 1: Write the failing DAO tests for the snippet subquery**

Step 4 *replaces* `observeAll()` rather than adding beside it, so two tests already in this file
stop compiling and are rewritten here alongside the new ones. Both read the entity directly where
the projection now wraps it:

```kotlin
                    .observeAllWithSnippet()
                    .first()
                    .map { it.conversation.id }
```

in `orders conversations by most recently updated`, and the same substitution plus
`.first().conversation.id` in `touching a conversation moves it to the head of the list`.

Then append to `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDaoTest.kt`, inside the class:

```kotlin
    @Test
    fun `the snippet is the last complete message`() =
        runDatabaseTest { database ->
            val id = database.newConversation("chat", updatedAt = 1L)
            database.conversationDao().insertMessage(
                message("first", createdAt = 1L, conversationId = id),
            )
            database.conversationDao().insertMessage(
                message("second", createdAt = 2L, conversationId = id),
            )

            val row = database.conversationDao().observeAllWithSnippet().first().single()

            assertEquals("second", (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the snippet skips a message the app never completed`() =
        runDatabaseTest { database ->
            val id = database.newConversation("chat", updatedAt = 1L)
            database.conversationDao().insertMessage(
                message("asked", createdAt = 1L, conversationId = id),
            )
            database.conversationDao().insertMessage(
                message("half", createdAt = 2L, conversationId = id)
                    .copy(role = Role.Assistant, status = MessageStatus.Failed),
            )

            val row = database.conversationDao().observeAllWithSnippet().first().single()

            assertEquals("asked", (row.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `a conversation with no complete message has no snippet`() =
        runDatabaseTest { database ->
            database.newConversation("chat", updatedAt = 1L)

            val row = database.conversationDao().observeAllWithSnippet().first().single()

            assertNull(row.snippet)
        }

    @Test
    fun `the single-conversation read carries the same snippet`() =
        runDatabaseTest { database ->
            val id = database.newConversation("chat", updatedAt = 1L)
            database.conversationDao().insertMessage(
                message("only", createdAt = 1L, conversationId = id),
            )

            val row = database.conversationDao().observeByIdWithSnippet(id).first()

            assertEquals("only", (row?.snippet?.single() as ContentBlock.Text).text)
        }

    @Test
    fun `the single-conversation read is null for a row that does not exist`() =
        runDatabaseTest { database ->
            assertNull(database.conversationDao().observeByIdWithSnippet(404L).first())
        }

    /**
     * The subquery filters on a string literal that no compiler checks against the enum. This is
     * what fails if the constant is ever renamed.
     */
    @Test
    fun `the snippet filter names a real status`() {
        assertEquals("Complete", MessageStatus.Complete.name)
    }
```

- [x] **Step 2: Run them to verify they fail**

```bash
./gradlew :shared:testAndroidHostTest --tests "*ConversationDaoTest*"
```

Expected: FAIL to compile. `observeAllWithSnippet` and `observeByIdWithSnippet` are unresolved.

- [x] **Step 3: Add the text fold**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/ContentText.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.chat

/**
 * A message's content as text a person reads: every [ContentBlock.Text] in order, concatenated.
 *
 * This is the single answer to which blocks count as a message's text.
 *
 * @return the joined text, empty when the message carries no text block.
 */
fun List<ContentBlock>.textContent(): String =
    filterIsInstance<ContentBlock.Text>().joinToString(separator = "") { it.text }
```

- [x] **Step 4: Add the DAO projection and the two queries**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationWithSnippet.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Embedded
import com.shayanaryan.chatbot.shared.chat.ContentBlock

/**
 * A conversation row plus the content of its last complete message, projected by the query rather
 * than stored. Nothing writes [snippet], so it cannot drift from the messages it summarizes.
 *
 * @property snippet null for a conversation whose only messages failed or were cancelled, and for
 *   one whose first turn has not finished.
 */
internal data class ConversationWithSnippet(
    @Embedded val conversation: ConversationEntity,
    val snippet: List<ContentBlock>?,
)
```

In `ConversationDao.kt`, replace the `observeAll()` declaration (lines 26-27) with:

```kotlin
    /**
     * Every conversation, most recently updated first, each with the content of its last complete
     * message. Room re-runs this flow whenever a table the query references is invalidated, and
     * the subquery makes `messages` one of them.
     */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.conversationId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM conversations c ORDER BY c.updatedAt DESC",
    )
    abstract fun observeAllWithSnippet(): Flow<List<ConversationWithSnippet>>

    /** The same projection for one conversation. Emits null once the row is gone. */
    @Query(
        "SELECT c.*, (SELECT m.content FROM messages m " +
            "WHERE m.conversationId = c.id AND m.status = 'Complete' " +
            "ORDER BY m.id DESC LIMIT 1) AS snippet " +
            "FROM conversations c WHERE c.id = :id",
    )
    abstract fun observeByIdWithSnippet(id: Long): Flow<ConversationWithSnippet?>
```

The `status = 'Complete'` filter matches the history query for the same reason: a failed turn's partial text is not what the conversation is about, and after a failed first turn the user's own message is the honest summary.

- [x] **Step 5: Run the DAO tests to verify they pass**

```bash
./gradlew :shared:testAndroidHostTest --tests "*ConversationDaoTest*"
```

Expected: PASS, all six new cases.

- [x] **Step 6: Write the failing repository tests**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationSnippetTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.FakeScriptedChatEngine
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConversationSnippetTest {
    private val turnScopes = mutableListOf<CoroutineScope>()

    private fun TestScope.turnScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job()).also { turnScopes += it }

    @AfterTest
    fun cancelTurnScopes() {
        turnScopes.forEach { it.cancel() }
    }

    private fun repository(
        database: ChatbotDatabase,
        scope: CoroutineScope,
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine =
                FakeScriptedChatEngine(
                    events =
                        listOf(
                            ChatStreamEvent.Delta("a reply"),
                            ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)),
                        ),
                ),
            externalScope = scope,
        )

    @Test
    fun `the list snippet is the last complete message`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            repository.send(null, "hello")
            advanceUntilIdle()

            val conversation = repository.getConversationsFlow().first().single()

            assertEquals("a reply", conversation.snippet)
        }

    @Test
    fun `a conversation whose first turn has not finished is summarized by the user's own message`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")

            val conversation = repository.getConversationFlow(id).first()

            assertEquals("hello", conversation?.snippet)
        }

    @Test
    fun `the single-conversation read carries the title`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "plan a weekend")
            advanceUntilIdle()

            assertEquals("plan a weekend", repository.getConversationFlow(id).first()?.title)
        }

    @Test
    fun `the single-conversation read emits null after delete`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())
            val id = repository.send(null, "hello")
            advanceUntilIdle()
            repository.delete(id)
            advanceUntilIdle()

            assertNull(repository.getConversationFlow(id).first())
        }

    @Test
    fun `the single-conversation read emits null for an id that never existed`() =
        runDatabaseTest { database ->
            val repository = repository(database, turnScope())

            assertNull(repository.getConversationFlow(404L).first())
        }
}
```

- [x] **Step 7: Run them to verify they fail**

```bash
./gradlew :shared:testAndroidHostTest --tests "*ConversationSnippetTest*"
```

Expected: FAIL to compile. `Conversation.snippet` and `getConversationFlow` are unresolved.

- [x] **Step 8: Reshape `Conversation` and add the mapper**

In `Conversation.kt`, replace the data class (lines 8-14):

```kotlin
/**
 * @property snippet the last complete message's text, projected by the list query rather than
 *   stored. Null for a conversation with no complete message yet.
 */
data class Conversation(
    val id: Long,
    val title: String,
    val model: ClaudeModel,
    val snippet: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

In `Mappers.kt`, replace `ConversationEntity.toDomain()` (lines 9-16) with a mapper over the projection. The bare-entity mapper goes: nothing can build a `Conversation` without a snippet any more.

```kotlin
internal fun ConversationWithSnippet.toDomain(): Conversation =
    Conversation(
        id = conversation.id,
        title = conversation.title,
        model = conversation.model,
        snippet = snippet?.textContent()?.takeIf { it.isNotBlank() },
        createdAt = Instant.fromEpochMilliseconds(conversation.createdAt),
        updatedAt = Instant.fromEpochMilliseconds(conversation.updatedAt),
    )
```

Add `import com.shayanaryan.chatbot.shared.chat.textContent` and `import com.shayanaryan.chatbot.shared.conversation.local.ConversationWithSnippet` is unnecessary, since the mapper is in the same package. Remove the now-unused `ConversationEntity` import only if nothing else in the file uses it.

- [x] **Step 9: Add the repository read**

In `ConversationRepository.kt`, after `getConversationsFlow()`:

```kotlin
    /**
     * One conversation, re-emitting whenever it or its messages change. Emits null for a
     * conversation that does not exist or has been deleted, which is what a screen seeded from a
     * notification's id has no way to rule out.
     */
    fun getConversationFlow(conversationId: Long): Flow<Conversation?>
```

In `DefaultConversationRepository.kt`, replace `getConversationsFlow` (lines 64-65) and add the new read:

```kotlin
    override fun getConversationsFlow(): Flow<List<Conversation>> =
        conversationDao.observeAllWithSnippet().map { rows -> rows.map { it.toDomain() } }

    override fun getConversationFlow(conversationId: Long): Flow<Conversation?> =
        conversationDao.observeByIdWithSnippet(conversationId).map { it?.toDomain() }
```

- [x] **Step 10: Update the fake to match the reshaped contract**

`shared/testing/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt`:

Add the new override after `getConversationsFlow`:

```kotlin
    override fun getConversationFlow(conversationId: Long): Flow<Conversation?> =
        conversations.map { list -> list.firstOrNull { it.id == conversationId } }.distinctUntilChanged()
```

In `createConversation`, add the snippet, which for a brand-new conversation is its first message:

```kotlin
        val conversation =
            Conversation(
                id = id,
                title = text.take(ConversationRepository.MAX_TITLE_LENGTH),
                model = model,
                snippet = text,
                createdAt = now,
                updatedAt = now,
            )
```

In `appendMessage`, keep the projected snippet honest: only a `Complete` message replaces it:

```kotlin
        conversations.update { list ->
            list
                .map {
                    when {
                        it.id != conversationId -> it
                        status == MessageStatus.Complete && text.isNotBlank() ->
                            it.copy(updatedAt = now, snippet = text)
                        else -> it.copy(updatedAt = now)
                    }
                }.mostRecentFirst()
        }
```

- [x] **Step 11: Add the fake's own coverage**

Append to `shared/testing/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt`, inside the class:

```kotlin
    @Test
    fun `the snippet follows the last complete message`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "a reply")
            repository.completeTurn(id)

            assertEquals("a reply", repository.getConversationsFlow().first().single().snippet)
        }

    @Test
    fun `a failed reply leaves the snippet on the user's own message`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "half")
            repository.failTurn(id, ChatError.Network)

            assertEquals("hello", repository.getConversationsFlow().first().single().snippet)
        }

    @Test
    fun `the single-conversation flow emits null after delete`() =
        runTest {
            val repository = FakeConversationRepository()
            val id = repository.send(null, "hello")
            repository.delete(id)

            assertNull(repository.getConversationFlow(id).first())
        }
```

Add whatever imports these need (`ChatError`, `assertNull`, `first`, `runTest`) if the file lacks them.

- [x] **Step 12: Run everything and fix the fallout**

```bash
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest
```

Expected: PASS. Nothing outside the fake constructs a `Conversation` literal, so reshaping it has no fallout; if a test does turn up, add `snippet = null` or the text it actually expects.

- [x] **Step 13: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Report that no schema version bump was needed and that `schemas/` is unchanged. Stop for review.

---

### Task 3: `:feature:conversation` setup, the injected clock, and the list ViewModel

The module gains Hilt, KSP, the screenshot plugin and the Compose lifecycle artifacts, because the list ViewModel is the first thing that needs any of them. It takes **no Navigation 3 dependency**: the nav graph belongs to `:app`, so this module exports composables and ViewModels and knows nothing about keys.

**Files:**
- Modify: `feature/conversation/build.gradle.kts`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/di/TimeModule.kt`
- Rename: `app/src/main/kotlin/com/shayanaryan/chatbot/di/DatabaseModule.kt` → `DataModule.kt`, and modify it
- Create: `feature/conversation/src/main/res/values/strings.xml`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/RelativeTime.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListUiState.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListViewModel.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/RelativeTimeTest.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListViewModelTest.kt`

**Interfaces:**
- Consumes: `ConversationRepository.getConversationsFlow()` returning `Conversation` with `snippet` (Task 2); `FakeConversationRepository` and `FakeClock` from `:shared:testing` (Task 1).
- Produces: `ConversationListUiState(isLoading: Boolean, conversations: List<ConversationListItemUiState>)`.
- Produces: `ConversationListItemUiState(id: Long, title: String, snippet: String?, relativeTime: RelativeTime)`.
- Produces: `RelativeTime(unitRes: Int, value: Int)` and `fun Instant.relativeTo(now: Instant): RelativeTime`.
- Produces: `ConversationListViewModel.uiState: StateFlow<ConversationListUiState>`.
- Produces: internal `const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L`, in `ConversationListViewModel.kt` beside its first user. Task 5's chat ViewModel reads the same constant.
- Produces: a `kotlin.time.Clock` binding in `SingletonComponent`.

**Spec delta to report:** 005 writes `ConversationListUiState(isLoading, conversations: List<Conversation>)`. Relative timestamps have to be resolved against the injected clock, and the architecture skill forbids `Resources` inside a ViewModel, so the state carries a UI model with a `@StringRes` id instead of the domain `Conversation`. Everything else about the state is as specced.

- [x] **Step 1: Give the module its dependencies**

Replace `feature/conversation/build.gradle.kts` wholesale:

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "com.shayanaryan.chatbot.feature.conversation"
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
    // Gates screenshotTest source-set creation
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
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
    implementation(projects.shared)
    implementation(projects.core.ui)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    // Supplies androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel, including the
    // creationCallback overload the assisted-inject chat ViewModel needs. Deliberately not
    // hilt-navigation-compose, which would drag navigation-compose (Navigation 2) in.
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    ksp(libs.hilt.compiler)

    testImplementation(projects.shared.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    // Carries the JVM actuals for kotlin.test's annotations. This module is a plain Android
    // library, so nothing substitutes the framework variant the way KMP does in :shared, and
    // without this every kotlin.test import in the tests below is unresolved.
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Provides the @PreviewTest annotation; screenshot plugin does not add it to the classpath itself.
    screenshotTestImplementation(libs.screenshot.validation.api)
}
```

- [x] **Step 2: Bind one clock for the whole app**

`createConversationRepository`'s `clock` parameter defaults to `Clock.System` and covers the repository's own timestamps only. The list ViewModel is a second consumer that Hilt constructs, and the graph has no `kotlin.time.Clock` binding, so add one and route the repository through it too. One source of time, and a test that swaps it swaps both.

Create `app/src/main/kotlin/com/shayanaryan/chatbot/di/TimeModule.kt`:

```kotlin
package com.shayanaryan.chatbot.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * The app's only source of time. Both the repository's stored timestamps and the conversation
 * list's relative labels read it, so the two can never disagree about "now".
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
}
```

Rename `DatabaseModule.kt` to `DataModule.kt`, since it now provides more than the database, then take the clock and pass it on:

```kotlin
    @Provides
    @Singleton
    fun provideConversationRepository(
        database: ChatbotDatabase,
        engine: ChatEngine,
        @ApplicationScope externalScope: CoroutineScope,
        clock: Clock,
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine = engine,
            externalScope = externalScope,
            clock = clock,
        )
```

Add `import kotlin.time.Clock`, after `javax.inject.Singleton`.

- [x] **Step 3: Add the feature's strings**

Create `feature/conversation/src/main/res/values/strings.xml`. This file carries every piece of copy both screens need, including the ones later tasks consume, so no task has to come back and edit it:

```xml
<resources>
    <!-- Conversation list -->
    <string name="conversation_list_title">Chats</string>
    <string name="conversation_list_empty_title">no conversations yet</string>
    <string name="conversation_list_empty_body">start a chat and it\'ll show up here. everything stays on this device.</string>
    <string name="conversation_list_new_chat">new chat</string>

    <!-- Relative timestamps. The "now" string takes no argument; the others take a count. -->
    <string name="conversation_time_now">now</string>
    <string name="conversation_time_minutes">%1$dm</string>
    <string name="conversation_time_hours">%1$dh</string>
    <string name="conversation_time_days">%1$dd</string>
    <string name="conversation_time_weeks">%1$dw</string>

    <!-- Chat -->
    <string name="conversation_new_chat_title">new chat</string>
    <string name="conversation_new_chat_greeting">How\'s it going?</string>
    <string name="conversation_composer_placeholder">tell me…</string>
    <string name="conversation_back">Back</string>
    <string name="conversation_more">More</string>
    <string name="conversation_send">Send</string>
    <string name="conversation_stop">Stop</string>
    <string name="conversation_thinking">Thinking</string>
    <string name="conversation_model_picker">Model: %1$s</string>
    <string name="conversation_model_selected">%1$s selected</string>

    <!-- Delete -->
    <string name="conversation_delete">Delete chat</string>
    <string name="conversation_delete_title">Delete this chat?</string>
    <string name="conversation_delete_body">It\'s stored only on this device, so it\'s gone for good.</string>
    <string name="conversation_delete_confirm">Delete</string>
    <string name="conversation_delete_cancel">Cancel</string>

    <!-- Turn failures. One per ChatError case; the repository never carries prose. -->
    <string name="conversation_error_authentication">Your API key was rejected. Check the key this build is using and try again.</string>
    <string name="conversation_error_rate_limited">The API returned 429 (rate limited). Time for bathroom break?</string>
    <string name="conversation_error_rate_limited_after">The API returned 429 (rate limited). Come back in %1$d seconds.</string>
    <string name="conversation_error_overloaded">Claude is overloaded right now. Time for bathroom break?</string>
    <string name="conversation_error_invalid_request">The API rejected the request as invalid. My bad.</string>
    <string name="conversation_error_server">The API hit a server error. Whoopsy.</string>
    <string name="conversation_error_network">Couldn\'t reach the API. Check your connection and try again.</string>
    <string name="conversation_error_timeout">The request timed out before a reply arrived. Try again.</string>
    <string name="conversation_error_unexpected">Something went wrong on the way to Claude. Whoopsy.</string>
</resources>
```

- [x] **Step 4: Write the failing relative-time tests**

Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/RelativeTimeTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RelativeTimeTest {
    private val now = Instant.fromEpochMilliseconds(1_000_000_000L)

    private fun ago(duration: kotlin.time.Duration) = (now - duration).relativeTo(now)

    @Test
    fun `under a minute reads as now`() {
        assertEquals(RelativeTime(R.string.conversation_time_now, 0), ago(30.seconds))
    }

    @Test
    fun `minutes are whole minutes`() {
        assertEquals(RelativeTime(R.string.conversation_time_minutes, 59), ago(59.minutes))
    }

    @Test
    fun `an hour rolls over to hours`() {
        assertEquals(RelativeTime(R.string.conversation_time_hours, 2), ago(2.hours))
    }

    @Test
    fun `a day rolls over to days`() {
        assertEquals(RelativeTime(R.string.conversation_time_days, 3), ago(3.days))
    }

    @Test
    fun `a week rolls over to weeks`() {
        assertEquals(RelativeTime(R.string.conversation_time_weeks, 1), ago(7.days))
    }

    @Test
    fun `a fortnight is two weeks, not fourteen days`() {
        assertEquals(RelativeTime(R.string.conversation_time_weeks, 2), ago(15.days))
    }

    /** A device clock that moved backwards must not print a negative age. */
    @Test
    fun `a future timestamp reads as now`() {
        assertEquals(RelativeTime(R.string.conversation_time_now, 0), ago((-5).hours))
    }
}
```

- [x] **Step 5: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*RelativeTimeTest*"
```

Expected: FAIL to compile. `RelativeTime` and `relativeTo` are unresolved.

- [x] **Step 6: Implement relative time**

Create `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/RelativeTime.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.annotation.StringRes
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * A timestamp reduced to the coarsest unit that still describes it.
 *
 * Split in two so the ViewModel never touches `Resources`: it picks the unit, the composable
 * resolves the string.
 *
 * @property unitRes a feature string taking one integer, except the "now" string, which takes none
 *   and ignores the extra argument.
 * @property value how many of that unit.
 */
data class RelativeTime(
    @param:StringRes val unitRes: Int,
    val value: Int,
)

/**
 * Truncates towards the past:
 * 45s  -> now    90m -> 1h    9d  -> 1w
 * 100s -> 1m     26h -> 1d    15d -> 2w
 *
 * @param now the reading of the injected clock this label is relative to. A timestamp in the
 *   future (a device clock that moved backwards) reads as "now" rather than as a negative age.
 */
fun Instant.relativeTo(now: Instant): RelativeTime {
    val elapsed = now - this
    return when {
        elapsed < 1.minutes -> {
            RelativeTime(R.string.conversation_time_now, 0)
        }

        elapsed < 1.hours -> {
            RelativeTime(
                R.string.conversation_time_minutes,
                elapsed.inWholeMinutes.toInt(),
            )
        }

        elapsed < 1.days -> {
            RelativeTime(
                R.string.conversation_time_hours,
                elapsed.inWholeHours.toInt(),
            )
        }

        elapsed < 7.days -> {
            RelativeTime(
                R.string.conversation_time_days,
                elapsed.inWholeDays.toInt(),
            )
        }

        else -> {
            RelativeTime(R.string.conversation_time_weeks, (elapsed.inWholeDays / 7).toInt())
        }
    }
}
```

- [x] **Step 7: Run the relative-time tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*RelativeTimeTest*"
```

Expected: PASS.

- [x] **Step 8: Write the failing list ViewModel tests**

Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListViewModelTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.conversation.FakeConversationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = FakeClock(instant = Instant.fromEpochMilliseconds(1_000_000_000L))

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    /**
     * `stateIn(WhileSubscribed)` produces nothing until something collects, so every test needs a
     * collector before it reads `value`.
     */
    private fun TestScope.collecting(viewModel: ConversationListViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `starts loading with no rows`() =
        runTest(dispatcher) {
            val viewModel = ConversationListViewModel(FakeConversationRepository(clock), clock)

            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(
                emptyList<ConversationListItemUiState>(),
                viewModel.uiState.value.conversations,
            )
        }

    @Test
    fun `stops loading on the first emission`() =
        runTest(dispatcher) {
            val viewModel = ConversationListViewModel(FakeConversationRepository(clock), clock)
            collecting(viewModel)
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.isLoading)
        }

    @Test
    fun `carries title, snippet and id from the repository`() =
        runTest(dispatcher) {
            val repository = FakeConversationRepository(clock)
            val id = repository.send(null, "plan a weekend")
            repository.emitDelta(id, "Powell's Books first.")
            repository.completeTurn(id)
            val viewModel = ConversationListViewModel(repository, clock)
            collecting(viewModel)
            advanceUntilIdle()

            val row =
                viewModel.uiState.value.conversations
                    .single()

            assertEquals(id, row.id)
            assertEquals("plan a weekend", row.title)
            assertEquals("Powell's Books first.", row.snippet)
        }

    @Test
    fun `formats the timestamp against the injected clock`() =
        runTest(dispatcher) {
            val repository = FakeConversationRepository(clock)
            repository.send(null, "hello")
            clock.advanceBy(2.hours)
            val viewModel = ConversationListViewModel(repository, clock)
            collecting(viewModel)
            advanceUntilIdle()

            assertEquals(
                RelativeTime(R.string.conversation_time_hours, 2),
                viewModel.uiState.value.conversations
                    .single()
                    .relativeTime,
            )
        }
}
```

- [x] **Step 9: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationListViewModelTest*"
```

Expected: FAIL to compile. `ConversationListViewModel` is unresolved.

- [x] **Step 10: Write the state and the ViewModel**

Create `ConversationListUiState.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

/**
 * @property isLoading true until Room's first emission.
 * @property conversations every conversation, most recently updated first.
 */
data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationListItemUiState> = emptyList(),
)

/**
 * One row. A UI model rather than the domain `Conversation` because the timestamp is already
 * resolved here, against the clock the ViewModel was given.
 *
 * @property id the conversation a tap on this row opens.
 * @property title the conversation's first message, truncated by the repository.
 * @property snippet the last complete message's text, the row's second line.
 * @property relativeTime how long ago the conversation was last written to, already reduced to a
 *   unit and a count so the row only has to resolve a string.
 */
data class ConversationListItemUiState(
    val id: Long,
    val title: String,
    val snippet: String?,
    val relativeTime: RelativeTime,
)
```

Create `ConversationListViewModel.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * How long a `stateIn` pipeline keeps collecting after its last subscriber leaves. Long enough to
 * survive a configuration change, short enough that a backgrounded screen stops reading Room.
 */
internal const val SUBSCRIPTION_TIMEOUT_MILLIS: Long = 5_000L

/**
 * The conversation list. Read-only: every mutation belongs to the chat screen, so this holds no
 * events at all.
 */
@HiltViewModel
class ConversationListViewModel
    @Inject
    constructor(
        repository: ConversationRepository,
        clock: Clock,
    ) : ViewModel() {
        val uiState: StateFlow<ConversationListUiState> =
            repository
                .getConversationsFlow()
                .map { conversations ->
                    val now = clock.now()
                    ConversationListUiState(
                        isLoading = false,
                        conversations = conversations.map { it.toUiState(now) },
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                    initialValue = ConversationListUiState(),
                )
    }

private fun Conversation.toUiState(now: Instant) =
    ConversationListItemUiState(
        id = id,
        title = title,
        snippet = snippet,
        // Timestamps are resolved once per emission rather than on a ticker, so a row reading "2h" does
        // not become "3h" while the screen stays open. Room re-emits on every message write, which in
        // practice refreshes them often enough.
        relativeTime = updatedAt.relativeTo(now),
    )
```

- [x] **Step 11: Run the list ViewModel tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationListViewModelTest*"
```

Expected: PASS, all four.

- [x] **Step 11a: Pull the dev key forward from Task 8**

`ConversationListViewModel` is `@HiltViewModel` and injects `ConversationRepository`, and `:app`
depends on `:feature:conversation`, so from this step on Dagger must resolve `ChatEngine` →
`ApiKeyProvider` to build `:app` at all. Debug included, not release only. The dev key therefore
lands here rather than in Task 8, which keeps `:app:assembleDebug` usable as every later
checkpoint's verification.

Apply **Task 8 Step 2 in full** (`app/src/debug/kotlin/com/shayanaryan/chatbot/di/DevApiKeyModule.kt`),
and the dev-key half of **Task 8 Step 1**: the `devApiKey` provider, `buildConfig = true`, and the
`debug { buildConfigField(…) }` block. Task 8's serialization plugin and Navigation 3 artifacts stay
in Task 8, since nothing references them yet.

- [x] **Step 12: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :feature:conversation:testDebugUnitTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`, which is what Step 11a is there to keep true. Stop for review.

---

### Task 4: The conversation list screen

Frames 2a, 2b, 2e and the light rendering 7b. Search and settings actions in the top bar are **not built**: search is M4, settings has no destination until 007. The unread dot is **not built**: it needs a `lastReadAt` column, so M4 owns it.

**Files:**
- Modify: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/designsystem/icon/Glyphs.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ConversationListItem.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ConversationListEmpty.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ConversationListSkeleton.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListScreen.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListRoute.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListScreenTest.kt`
- Create: `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/preview/FormFactorPreviews.kt`
- Test: `feature/conversation/src/screenshotTest/kotlin/com/shayanaryan/chatbot/feature/conversation/preview/ConversationListPreviews.kt`

**Interfaces:**
- Consumes: `ConversationListUiState`, `ConversationListItemUiState`, `RelativeTime`, `ConversationListViewModel` (Task 3).
- Produces: `ConversationListScreen(uiState, selectedConversationId, onConversationClick, onNewChat, modifier)`, stateless.
- Produces: `ConversationListRoute(selectedConversationId, onConversationClick, onNewChat, modifier, viewModel)`, stateful.
- Produces: `ConversationListItem(title, snippet, relativeTime, selected, onClick, modifier)`.
- Produces: `Glyphs.ADD`, `Glyphs.ARROW_BACK`, `Glyphs.ARROW_UPWARD`, `Glyphs.STOP`, `Glyphs.MORE_VERT`, `Glyphs.DELETE`, `Glyphs.EXPAND_MORE`, `Glyphs.EXPAND_LESS`, `Glyphs.CHECK`, `Glyphs.REFRESH`.

- [x] **Step 1: Add every glyph both screens reference**

In `Glyphs.kt`, the object grows once, here, so no later task edits `:core:ui`:

```kotlin
object Glyphs {
    const val BRAND = "forum"
    const val CLOSE = "close"
    const val ERROR = "error"
    const val ARROW_FORWARD = "arrow_forward"
    const val ADD = "add"
    const val ARROW_BACK = "arrow_back"
    const val ARROW_UPWARD = "arrow_upward"
    const val STOP = "stop"
    const val MORE_VERT = "more_vert"
    const val DELETE = "delete"
    const val EXPAND_MORE = "expand_more"
    const val EXPAND_LESS = "expand_less"
    const val CHECK = "check"
    const val REFRESH = "refresh"
}
```

- [x] **Step 2: Write the failing Compose UI tests**

Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationListScreenTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ConversationListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val rows =
        listOf(
            ConversationListItemUiState(
                id = 1L,
                title = "Weekend trip to Portland",
                snippet = "Powell's Books first.",
                relativeTime = RelativeTime(R.string.conversation_time_hours, 2),
            ),
            ConversationListItemUiState(
                id = 2L,
                title = "Miso glaze recipe",
                snippet = null,
                relativeTime = RelativeTime(R.string.conversation_time_days, 1),
            ),
        )

    private fun setScreen(
        uiState: ConversationListUiState,
        onConversationClick: (Long) -> Unit = {},
        onNewChat: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ConversationListScreen(
                    uiState = uiState,
                    selectedConversationId = null,
                    onConversationClick = onConversationClick,
                    onNewChat = onNewChat,
                )
            }
        }
    }

    @Test
    fun `shows a row per conversation with its snippet and age`() {
        setScreen(ConversationListUiState(isLoading = false, conversations = rows))

        composeRule.onNodeWithText("Weekend trip to Portland").assertIsDisplayed()
        composeRule.onNodeWithText("Powell's Books first.").assertIsDisplayed()
        composeRule.onNodeWithText("2h").assertIsDisplayed()
        composeRule.onNodeWithText("1d").assertIsDisplayed()
    }

    @Test
    fun `reports the id of the row that was tapped`() {
        var clicked: Long? = null
        setScreen(
            ConversationListUiState(isLoading = false, conversations = rows),
            onConversationClick = { clicked = it },
        )

        composeRule.onNodeWithText("Miso glaze recipe").performClick()

        assertEquals(2L, clicked)
    }

    @Test
    fun `shows the empty state when there is nothing stored`() {
        setScreen(ConversationListUiState(isLoading = false, conversations = emptyList()))

        composeRule.onNodeWithText("no conversations yet").assertIsDisplayed()
    }

    @Test
    fun `shows neither rows nor the empty state while loading`() {
        setScreen(ConversationListUiState(isLoading = true, conversations = emptyList()))

        composeRule.onNodeWithText("no conversations yet").assertDoesNotExist()
    }

    @Test
    fun `the new chat button reports a tap`() {
        var tapped = false
        setScreen(
            ConversationListUiState(isLoading = false, conversations = rows),
            onNewChat = { tapped = true },
        )

        composeRule.onNodeWithText("new chat").performClick()

        assertEquals(true, tapped)
    }
}
```

Add `import androidx.compose.ui.test.assertDoesNotExist`.

- [x] **Step 3: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationListScreenTest*"
```

Expected: FAIL to compile. `ConversationListScreen` is unresolved.

- [x] **Step 4: Build the row**

Create `component/ConversationListItem.kt`. The design file marks the last row with a `last` boolean so it drops its own divider; in Compose that is the call site drawing dividers *between* items, so the row itself carries none.

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R
import com.shayanaryan.chatbot.feature.conversation.RelativeTime

/**
 * One conversation in the list: title and age on the first line, the last complete reply on the
 * second.
 *
 * @param snippet null for a conversation whose first turn has not finished, which leaves the
 *   second line empty rather than reserving space for nothing.
 * @param selected the open conversation on a wide window. A narrow window never shows the list
 *   beside a chat, so it always passes false.
 */
@Composable
fun ConversationListItem(
    title: String,
    snippet: String?,
    relativeTime: RelativeTime,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(
                    if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                ).clickable(onClick = onClick)
                .padding(Spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s0_5)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(relativeTime.unitRes, relativeTime.value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (snippet != null) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListItemPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ConversationListItem(
                title = "Weekend trip to Portland",
                snippet = "Booked — I'll remind you to check in Friday.",
                relativeTime = RelativeTime(R.string.conversation_time_hours, 2),
                selected = false,
                onClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListItemSelectedPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface {
            ConversationListItem(
                title = "Miso glaze recipe",
                snippet = null,
                relativeTime = RelativeTime(R.string.conversation_time_days, 1),
                selected = true,
                onClick = {},
            )
        }
    }
}
```

- [x] **Step 5: Build the empty and loading states**

Create `component/ConversationListEmpty.kt`, frame 2b's centred tile, headline and body:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R

/** Nothing stored yet, so the screen says so and the new-chat button carries the flow. */
@Composable
fun ConversationListEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.s10),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(88.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraLarge,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            DsIcon(
                glyph = Glyphs.BRAND,
                contentDescription = null,
                size = 46.dp,
                filled = true,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.conversation_list_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.conversation_list_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface { ConversationListEmpty() }
    }
}
```

Create `component/ConversationListSkeleton.kt`, frame 2e's four placeholder rows:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing

/**
 * How wide a placeholder row's two bars are, each a fraction of the row's width. The bars stand in
 * for the two lines [ConversationListItem] draws.
 *
 * @property title the top bar, where the title goes.
 * @property snippet the bottom bar, where the snippet goes.
 */
private data class SkeletonWidths(
    val title: Float,
    val snippet: Float,
)

/**
 * One entry per placeholder row, top to bottom. Widths differ because real titles and replies do;
 * identical rows would read as a table.
 */
private val skeletonRows =
    listOf(
        SkeletonWidths(title = 0.62f, snippet = 0.88f),
        SkeletonWidths(title = 0.48f, snippet = 0.74f),
        SkeletonWidths(title = 0.70f, snippet = 0.56f),
        SkeletonWidths(title = 0.54f, snippet = 0.80f),
    )

/**
 * Opacity of the bottom row, faded so the list reads as continuing past the edge of the screen
 * rather than ending there.
 */
private const val LAST_SKELETON_ROW_ALPHA = 0.6f

/** What the list shows for the frame or two before Room's first emission arrives. */
@Composable
fun ConversationListSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        skeletonRows.forEachIndexed { index, widths ->
            SkeletonRow(
                widths = widths,
                modifier =
                    Modifier.alpha(
                        if (index == skeletonRows.lastIndex) LAST_SKELETON_ROW_ALPHA else 1f,
                    ),
            )
        }
    }
}

@Composable
private fun SkeletonRow(
    widths: SkeletonWidths,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(widths.title)
                    .height(14.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.shapes.small,
                    ),
            )
            Box(
                Modifier
                    .fillMaxWidth(widths.snippet)
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.shapes.small,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationListSkeletonPreview() {
    ChatbotTheme(darkTheme = true) {
        Surface { ConversationListSkeleton() }
    }
}
```

- [x] **Step 6: Build the stateless screen**

Create `ConversationListScreen.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.component.ConversationListEmpty
import com.shayanaryan.chatbot.feature.conversation.component.ConversationListItem
import com.shayanaryan.chatbot.feature.conversation.component.ConversationListSkeleton

/**
 * The app's home screen: browse and resume conversations, or start one.
 *
 * Stateless. This overload is what previews, screenshot tests and Compose tests drive.
 *
 * @param selectedConversationId the conversation open in the detail pane, highlighted in the row.
 *   Always null on a narrow window, which never shows the list beside a chat.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    uiState: ConversationListUiState,
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.conversation_list_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        floatingActionButton = {
            DsButton(
                text = stringResource(R.string.conversation_list_new_chat),
                onClick = onNewChat,
                variant = ButtonVariant.Filled,
                leadingGlyph = Glyphs.ADD,
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> ConversationListSkeleton(Modifier.padding(padding))
            uiState.conversations.isEmpty() -> ConversationListEmpty(Modifier.padding(padding))
            else ->
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(horizontal = Spacing.s2, vertical = Spacing.s1),
                ) {
                    itemsIndexed(uiState.conversations, key = { _, row -> row.id }) { index, row ->
                        ConversationListItem(
                            title = row.title,
                            snippet = row.snippet,
                            relativeTime = row.relativeTime,
                            selected = row.id == selectedConversationId,
                            onClick = { onConversationClick(row.id) },
                        )
                        // The design marks the last row so it drops its own divider; drawing
                        // between items says the same thing without a prop.
                        if (index != uiState.conversations.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = Spacing.s3),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
        }
    }
}
```

Add the colocated previews at the bottom of the same file, one per frame:

```kotlin
internal val PREVIEW_CONVERSATIONS =
    listOf(
        ConversationListItemUiState(1L, "Weekend trip to Portland", "Booked — I'll remind you to check in Friday.", RelativeTime(R.string.conversation_time_hours, 2)),
        ConversationListItemUiState(2L, "Miso glaze recipe", "Try broiling the last 2 minutes.", RelativeTime(R.string.conversation_time_days, 1)),
        ConversationListItemUiState(3L, "Standup notes", "Got it — remembered you're on the payments team.", RelativeTime(R.string.conversation_time_days, 3)),
        ConversationListItemUiState(4L, "Coroutine leak in onCleared when the ViewModelScope isn't cancelled", "Cancel the viewModelScope — it's automatic, actually.", RelativeTime(R.string.conversation_time_days, 5)),
        ConversationListItemUiState(5L, "Gift ideas for dad", "A cast-iron skillet + a good chef's apron.", RelativeTime(R.string.conversation_time_weeks, 1)),
    )

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ConversationListPopulatedPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(isLoading = false, conversations = PREVIEW_CONVERSATIONS),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ConversationListEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(isLoading = false, conversations = emptyList()),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ConversationListLoadingPreview() {
    ChatbotTheme(darkTheme = true) {
        ConversationListScreen(
            uiState = ConversationListUiState(isLoading = true),
            selectedConversationId = null,
            onConversationClick = {},
            onNewChat = {},
        )
    }
}
```

- [x] **Step 7: Build the stateful route**

Create `ConversationListRoute.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the conversation list: resolves the ViewModel and hands its state down.
 * This is what `:app` calls; the screen below it knows nothing about navigation keys.
 */
@Composable
fun ConversationListRoute(
    selectedConversationId: Long?,
    onConversationClick: (Long) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ConversationListScreen(
        uiState = uiState,
        selectedConversationId = selectedConversationId,
        onConversationClick = onConversationClick,
        onNewChat = onNewChat,
        modifier = modifier,
    )
}
```

- [x] **Step 8: Run the UI tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationListScreenTest*"
```

Expected: PASS, all five.

- [x] **Step 9: Record the screenshot goldens**

First create `core/ui/src/main/kotlin/com/shayanaryan/chatbot/core/ui/preview/FormFactorPreviews.kt`. 005 asks for a golden per design frame, which proves the states; the `adaptive` skill asks for a golden per form factor, which proves the layout. Both screens ship both, and this annotation is the second half. It lives in `:core:ui`'s **main** source set, not a test one: every feature's screenshot tests need it, and a test source set is not published to consumers. `ui-tooling-preview` is the production-safe half of the tooling split, so the annotation costs nothing at runtime.

```kotlin
package com.shayanaryan.chatbot.core.ui.preview

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview

/**
 * The four widths the app is expected to survive. A list-detail layout changes shape between them,
 * so a golden per form factor is what catches a pane that stops laying out.
 */
@Preview(name = "phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews
```

Then create `.../preview/ConversationListPreviews.kt`, one `@PreviewTest` pair per built frame, dark and light:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.preview.FormFactorPreviews
import com.shayanaryan.chatbot.feature.conversation.ConversationListScreen
import com.shayanaryan.chatbot.feature.conversation.ConversationListUiState
import com.shayanaryan.chatbot.feature.conversation.PREVIEW_CONVERSATIONS

@Composable
private fun ListScreen(uiState: ConversationListUiState) {
    ConversationListScreen(
        uiState = uiState,
        selectedConversationId = null,
        onConversationClick = {},
        onNewChat = {},
    )
}

private val populated =
    ConversationListUiState(isLoading = false, conversations = PREVIEW_CONVERSATIONS)
private val empty = ConversationListUiState(isLoading = false, conversations = emptyList())
private val loading = ConversationListUiState(isLoading = true)

@PreviewTest
@Preview(name = "list-2a-populated-dark", heightDp = 780)
@Composable
private fun ListPopulatedDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(populated) }
}

@PreviewTest
@Preview(name = "list-2a-populated-light", heightDp = 780)
@Composable
private fun ListPopulatedLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(populated) }
}

@PreviewTest
@Preview(name = "list-2b-empty-dark", heightDp = 780)
@Composable
private fun ListEmptyDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(empty) }
}

@PreviewTest
@Preview(name = "list-2b-empty-light", heightDp = 780)
@Composable
private fun ListEmptyLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(empty) }
}

@PreviewTest
@Preview(name = "list-2e-loading-dark", heightDp = 780)
@Composable
private fun ListLoadingDarkPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(loading) }
}

@PreviewTest
@Preview(name = "list-2e-loading-light", heightDp = 780)
@Composable
private fun ListLoadingLightPreview() {
    ChatbotTheme(darkTheme = false) { ListScreen(loading) }
}
```

`PREVIEW_CONVERSATIONS` is `internal` in the main source set, which the screenshot source set can see: same module, same compilation unit family.

Add the form-factor sweep to the same file, on the populated state, since that is the one with layout to break:

```kotlin
@PreviewTest
@FormFactorPreviews
@Composable
private fun ListFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { ListScreen(populated) }
}
```

```bash
./gradlew :feature:conversation:updateDebugScreenshotTest
./gradlew :feature:conversation:validateDebugScreenshotTest
```

Expected: the update task writes goldens under `feature/conversation/src/debug/screenshotTest/reference/`; the validate task then passes. Before moving on, show the user the populated and empty goldens and the four form-factor ones, and get them to confirm. A golden recorded from a wrong render is worse than no golden, and only a person can say whether the tablet width still looks right.

- [x] **Step 10: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :feature:conversation:testDebugUnitTest :feature:conversation:validateDebugScreenshotTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Report the frames covered and any off-grid values snapped. Stop for review.

---

### Task 5: The chat ViewModel and the row-folding rule

Room and the in-memory turn are folded into one list so the `LazyColumn` has a single source and no composable has to reconcile two. This carries the most risk in the spec and gets the most cases.

**Files:**
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationUiState.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ChatRows.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationViewModel.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ChatRowsTest.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationViewModelTest.kt`

**Interfaces:**
- Consumes: `ConversationRepository` (`getConversationFlow`, `getMessagesFlow`, `getTurnFlow`, `send`, `retry`, `cancel`, `setModel`, `delete`), `TurnState`, `Message`, `MessageStatus`, `ChatError`, `ClaudeModel`, `List<ContentBlock>.textContent()`.
- Produces: `sealed interface ChatRow` with `Persisted(message: Message)`, `Thinking`, `Streaming(text: String)`, `Error(error: ChatError)`.
- Produces: `internal fun chatRows(messages: List<Message>, turn: TurnState): List<ChatRow>`.
- Produces: `ConversationUiState(conversationId: Long?, title: String?, model: ClaudeModel, rows: List<ChatRow>, isStreaming: Boolean, deleteDialogVisible: Boolean, deleted: Boolean)`.
- Produces: `ConversationViewModel` with `uiState: StateFlow<ConversationUiState>`, `Factory.create(initialConversationId: Long?)`, and methods `onSend(String)`, `onCancel()`, `onRetry()`, `onModelSelected(ClaudeModel)`, `onDeleteRequested()`, `onDeleteDismissed()`, `onDeleteConfirmed()`.

**Two spec deltas to report**, both forced by the spec's own prose:

- `ConversationUiState` gains `conversationId`. 005 says the list's selected row reads the live id reported "through an `onConversationIdChanged` lambda driven by its `UiState`", and that the overflow button is hidden while the id is null. Both need the id in the state.
- `ConversationUiState` gains `deleted`. 005 says confirming delete "pops to the list, or on a wide window returns the detail pane to the new-chat state", a navigation trigger, which the architecture skill requires be a state field the UI observes, never an event channel. Setting it only after `delete` returns is also what keeps `viewModelScope` alive long enough to finish the delete before the entry is popped.

- [ ] **Step 1: Write the failing row-folding tests**

The rule is pure, so it is tested as a pure function before any flow plumbing exists. Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ChatRowsTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.conversation.TurnState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class ChatRowsTest {
    private var nextId = 1L

    private fun message(
        role: Role,
        text: String,
        status: MessageStatus = MessageStatus.Complete,
    ) = Message(
        id = nextId++,
        conversationId = 1L,
        role = role,
        content = listOf(ContentBlock.Text(text)),
        status = status,
        createdAt = Instant.fromEpochMilliseconds(nextId),
    )

    private fun user(text: String) = message(Role.User, text)

    private fun assistant(
        text: String,
        status: MessageStatus = MessageStatus.Complete,
    ) = message(Role.Assistant, text, status)

    @Test
    fun `an idle conversation is only its persisted messages`() {
        val rows = chatRows(listOf(user("hi"), assistant("hello")), TurnState.Idle)

        assertEquals(2, rows.size)
        assertTrue(rows.all { it is ChatRow.Persisted })
    }

    @Test
    fun `an empty streaming turn after a user message is the thinking row`() {
        val rows = chatRows(listOf(user("hi")), TurnState.Streaming(""))

        assertEquals(ChatRow.Thinking, rows.last())
    }

    @Test
    fun `a streaming turn with text is the streaming row`() {
        val rows = chatRows(listOf(user("hi")), TurnState.Streaming("hel"))

        assertEquals(ChatRow.Streaming("hel"), rows.last())
    }

    /**
     * 004 stores the assistant row *before* the turn returns to Idle, so there is a window where
     * Room has already emitted the finished message while the turn still reads Streaming.
     * Rendering both would double the bubble for a frame.
     */
    @Test
    fun `a stale streaming turn behind a persisted reply adds no row`() {
        val rows = chatRows(listOf(user("hi"), assistant("hello")), TurnState.Streaming("hello"))

        assertEquals(2, rows.size)
        assertTrue(rows.all { it is ChatRow.Persisted })
    }

    /**
     * 004 writes a Failed assistant row and keeps the turn entry until the next send, retry or
     * delete, so the error renders *after* that row rather than instead of it.
     */
    @Test
    fun `a failed turn keeps its persisted row and adds the error`() {
        val rows =
            chatRows(
                listOf(user("hi"), assistant("half", MessageStatus.Failed)),
                TurnState.Failed(ChatError.Network),
            )

        assertEquals(3, rows.size)
        assertIs<ChatRow.Persisted>(rows[1])
        assertEquals(ChatRow.Error(ChatError.Network), rows[2])
    }

    @Test
    fun `a failed turn that produced no text still shows the error`() {
        val rows =
            chatRows(
                listOf(user("hi"), assistant("", MessageStatus.Failed)),
                TurnState.Failed(ChatError.Overloaded),
            )

        assertEquals(2, rows.size)
        assertEquals(ChatRow.Error(ChatError.Overloaded), rows.last())
    }

    @Test
    fun `a blank message is not a row`() {
        val rows = chatRows(listOf(user("hi"), assistant("")), TurnState.Idle)

        assertEquals(1, rows.size)
    }

    @Test
    fun `a cancelled reply keeps its partial text as an ordinary message`() {
        val rows =
            chatRows(
                listOf(user("hi"), assistant("half a th", MessageStatus.Cancelled)),
                TurnState.Idle,
            )

        assertEquals(2, rows.size)
        val persisted = assertIs<ChatRow.Persisted>(rows.last())
        assertEquals(MessageStatus.Cancelled, persisted.message.status)
    }

    @Test
    fun `an empty conversation with an idle turn has no rows at all`() {
        assertEquals(emptyList<ChatRow>(), chatRows(emptyList(), TurnState.Idle))
    }
}
```

- [ ] **Step 2: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ChatRowsTest*"
```

Expected: FAIL to compile. `chatRows` and `ChatRow` are unresolved.

- [ ] **Step 3: Write the row model and the fold**

Create `ConversationUiState.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * One item in the message list. Persisted history and the turn in flight are folded into a single
 * list so the `LazyColumn` reads one source and no composable reconciles two.
 */
sealed interface ChatRow {
    /** A message Room has stored, whatever status it ended with. */
    data class Persisted(
        val message: Message,
    ) : ChatRow

    /** A turn that has started but produced no token yet. */
    data object Thinking : ChatRow

    /** @property text the reply so far, cumulative rather than the latest delta. */
    data class Streaming(
        val text: String,
    ) : ChatRow

    /** The last turn failed. Renders after the failed message rather than instead of it. */
    data class Error(
        val error: ChatError,
    ) : ChatRow
}

/**
 * @property conversationId null until the first send creates a conversation. Also what hides the
 *   overflow menu: a chat with no first message has nothing to delete.
 * @property title null for a chat with no first message yet, which the screen renders as the
 *   new-chat copy.
 * @property model the conversation's own model once it exists, and the model the first send will
 *   create it with before that.
 * @property deleted true once a confirmed delete has finished, which is the navigation trigger
 *   `:app` reads to pop or to reset the detail pane.
 */
data class ConversationUiState(
    val conversationId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel = ClaudeModel.Default,
    val rows: List<ChatRow> = emptyList(),
    val isStreaming: Boolean = false,
    val deleteDialogVisible: Boolean = false,
    val deleted: Boolean = false,
)
```

Create `ChatRows.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.textContent
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.TurnState

/**
 * Folds stored messages and the turn in flight into the one list the message list renders.
 *
 * Blank messages are dropped for the same reason 004 drops them on the way into a request: a turn
 * that produced no text still stores its row, and an empty bubble is noise.
 *
 * The live row is gated on the last *stored* message still being the user's. 004 guarantees the
 * assistant row is inserted before the turn returns to [TurnState.Idle], so there is a window
 * where Room has already emitted the finished message while the turn still reads
 * [TurnState.Streaming]; once an assistant message is the last stored row, any live text is stale
 * by definition. [TurnState.Failed] is exempt, because 004 writes a failed assistant row and keeps
 * the turn entry until the next send, retry or delete.
 */
internal fun chatRows(
    messages: List<Message>,
    turn: TurnState,
): List<ChatRow> {
    val rows = messages.filter { it.content.textContent().isNotBlank() }.map(ChatRow::Persisted)
    val awaitingReply = messages.lastOrNull()?.role == Role.User
    val trailing =
        when {
            turn is TurnState.Failed -> ChatRow.Error(turn.error)
            !awaitingReply -> null
            turn is TurnState.Streaming && turn.text.isEmpty() -> ChatRow.Thinking
            turn is TurnState.Streaming -> ChatRow.Streaming(turn.text)
            else -> null
        }
    return if (trailing == null) rows else rows + trailing
}
```

- [ ] **Step 4: Run the fold tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ChatRowsTest*"
```

Expected: PASS, all nine.

- [ ] **Step 5: Write the failing ViewModel tests**

Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationViewModelTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.lifecycle.SavedStateHandle
import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.conversation.FakeConversationRepository
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    // A non-zero step makes successive writes distinguishable, which is what ordering needs.
    private val clock = FakeClock(autoAdvanceBy = 1.milliseconds)
    private val repository = FakeConversationRepository(clock)

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        initialConversationId: Long? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = ConversationViewModel(initialConversationId, savedStateHandle, repository)

    private fun TestScope.collecting(viewModel: ConversationViewModel) {
        backgroundScope.launch { viewModel.uiState.collect {} }
    }

    @Test
    fun `a new chat has no id, no title and no rows`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.conversationId)
            assertNull(viewModel.uiState.value.title)
            assertEquals(emptyList<ChatRow>(), viewModel.uiState.value.rows)
        }

    @Test
    fun `the first send creates a conversation and adopts its id`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("plan a weekend")
            advanceUntilIdle()

            assertEquals(1L, viewModel.uiState.value.conversationId)
            assertEquals("plan a weekend", viewModel.uiState.value.title)
        }

    @Test
    fun `the adopted id survives process death through the saved state handle`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()
            val first = viewModel(savedStateHandle = handle)
            collecting(first)
            first.onSend("hello")
            advanceUntilIdle()

            // The restored back stack still says ChatKey(null); only the handle remembers.
            val restored = viewModel(initialConversationId = null, savedStateHandle = handle)
            collecting(restored)
            advanceUntilIdle()

            assertEquals(1L, restored.uiState.value.conversationId)
        }

    @Test
    fun `thinking, then streaming, then the stored reply`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()
            assertEquals(ChatRow.Thinking, viewModel.uiState.value.rows.last())

            repository.emitDelta(1L, "hel")
            advanceUntilIdle()
            assertEquals(ChatRow.Streaming("hel"), viewModel.uiState.value.rows.last())

            repository.completeTurn(1L)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.rows.last() is ChatRow.Persisted)
            assertEquals(false, viewModel.uiState.value.isStreaming)
        }

    @Test
    fun `a failed turn shows the error row`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()
            repository.failTurn(1L, ChatError.RateLimited(retryAfterSeconds = 30))
            advanceUntilIdle()

            assertEquals(
                ChatRow.Error(ChatError.RateLimited(30)),
                viewModel.uiState.value.rows.last(),
            )
        }

    @Test
    fun `retry clears the error and runs the turn again`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()
            repository.failTurn(1L, ChatError.Network)
            advanceUntilIdle()

            viewModel.onRetry()
            advanceUntilIdle()

            assertEquals(ChatRow.Thinking, viewModel.uiState.value.rows.last())
        }

    @Test
    fun `cancel keeps the partial text as an ordinary message`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()
            repository.emitDelta(1L, "half a th")

            viewModel.onCancel()
            advanceUntilIdle()

            val last = viewModel.uiState.value.rows.last()
            assertTrue(last is ChatRow.Persisted && last.message.content.isNotEmpty())
            assertEquals(false, viewModel.uiState.value.isStreaming)
        }

    @Test
    fun `picking a model before the first send creates the conversation with it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onModelSelected(ClaudeModel.Haiku)
            advanceUntilIdle()
            assertEquals(ClaudeModel.Haiku, viewModel.uiState.value.model)

            viewModel.onSend("hello")
            advanceUntilIdle()

            assertEquals(ClaudeModel.Haiku, viewModel.uiState.value.model)
        }

    @Test
    fun `picking a model on an existing conversation persists it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()

            viewModel.onModelSelected(ClaudeModel.Opus)
            advanceUntilIdle()

            assertEquals(
                ClaudeModel.Opus,
                repository.getConversationsFlow().first().single().model,
            )
        }

    @Test
    fun `the delete dialog opens and closes`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()

            // Every read of uiState needs an advance first: localState reaches it through
            // combine(…).stateIn(viewModelScope), and viewModelScope runs on the
            // StandardTestDispatcher, which executes nothing until it is advanced.
            viewModel.onDeleteRequested()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.deleteDialogVisible)

            viewModel.onDeleteDismissed()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.deleteDialogVisible)
        }

    @Test
    fun `confirming delete removes the conversation and reports it`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            collecting(viewModel)
            viewModel.onSend("hello")
            advanceUntilIdle()

            viewModel.onDeleteRequested()
            viewModel.onDeleteConfirmed()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.deleted)
            assertEquals(emptyList<Conversation>(), repository.getConversationsFlow().first())
        }

    /**
     * 010's notification for a conversation deleted between scheduling and firing is this case in
     * production: the id arrives from outside the app with no guarantee the row still exists.
     */
    @Test
    fun `an id that names no conversation falls back to a new chat`() =
        runTest(dispatcher) {
            val viewModel = viewModel(initialConversationId = 404L)
            collecting(viewModel)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.conversationId)
            assertNull(viewModel.uiState.value.title)
        }
}
```

Add `import kotlinx.coroutines.flow.first`, `import kotlin.time.Duration.Companion.milliseconds` and `import com.shayanaryan.chatbot.shared.conversation.Conversation`.

- [ ] **Step 6: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationViewModelTest*"
```

Expected: FAIL to compile. `ConversationViewModel` is unresolved.

- [ ] **Step 7: Write the ViewModel**

Create `ConversationViewModel.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import com.shayanaryan.chatbot.shared.conversation.TurnState
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the live conversation id survives process death; the nav key never learns it. */
private const val KEY_CONVERSATION_ID = "conversationId"

/** The slice of the state that comes from the repository, for one conversation or for none. */
private data class ChatSlice(
    val conversationId: Long? = null,
    val title: String? = null,
    val model: ClaudeModel? = null,
    val rows: List<ChatRow> = emptyList(),
    val isStreaming: Boolean = false,
)

/** The slice this ViewModel owns outright, with no flow behind it. */
private data class LocalState(
    val pendingModel: ClaudeModel = ClaudeModel.Default,
    val deleteDialogVisible: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * One chat screen.
 *
 * The conversation id is owned here rather than by the navigation key. A new chat keeps
 * `ChatKey(null)` for its whole life: replacing the top of the back stack would recreate the entry
 * and therefore this ViewModel, mid-stream, resetting scroll. The first send writes the created id
 * to [SavedStateHandle], which is what survives process death: the restored back stack still says
 * `ChatKey(null)`, and without that write the user would return to an empty new chat instead of
 * the conversation they were in.
 *
 * @param initialConversationId the id the navigation key carried, null for a new chat. The saved
 *   value wins over it when present.
 */
@HiltViewModel(assistedFactory = ConversationViewModel.Factory::class)
class ConversationViewModel
    @AssistedInject
    constructor(
        @Assisted private val initialConversationId: Long?,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ConversationRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(initialConversationId: Long?): ConversationViewModel
        }

        private val conversationId =
            MutableStateFlow(
                savedStateHandle.get<Long>(KEY_CONVERSATION_ID) ?: initialConversationId,
            )

        private val localState = MutableStateFlow(LocalState())

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<ConversationUiState> =
            combine(
                conversationId.flatMapLatest(::chatSlice),
                localState,
            ) { slice, local ->
                ConversationUiState(
                    conversationId = slice.conversationId,
                    title = slice.title,
                    model = slice.model ?: local.pendingModel,
                    rows = slice.rows,
                    isStreaming = slice.isStreaming,
                    deleteDialogVisible = local.deleteDialogVisible,
                    deleted = local.deleted,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
                initialValue = ConversationUiState(),
            )

        /**
         * A null id subscribes to nothing, so no flow is ever opened on a conversation that does
         * not exist. A conversation that disappears under the screen clears the id and falls back
         * to the same new-chat state.
         */
        private fun chatSlice(id: Long?): Flow<ChatSlice> =
            if (id == null) {
                flowOf(ChatSlice())
            } else {
                combine(
                    repository.getConversationFlow(id).onEach { if (it == null) forgetConversation() },
                    repository.getMessagesFlow(id),
                    repository.getTurnFlow(id),
                ) { conversation, messages, turn ->
                    if (conversation == null) {
                        ChatSlice()
                    } else {
                        ChatSlice(
                            conversationId = id,
                            title = conversation.title,
                            model = conversation.model,
                            rows = chatRows(messages, turn),
                            isStreaming = turn is TurnState.Streaming,
                        )
                    }
                }
            }

        fun onSend(text: String) {
            if (text.isBlank() || uiState.value.isStreaming) return
            val id = conversationId.value
            viewModelScope.launch {
                val created = repository.send(id, text, localState.value.pendingModel)
                if (id == null) rememberConversation(created)
            }
        }

        fun onCancel() {
            val id = conversationId.value ?: return
            viewModelScope.launch { repository.cancel(id) }
        }

        fun onRetry() {
            val id = conversationId.value ?: return
            viewModelScope.launch { repository.retry(id) }
        }

        fun onModelSelected(model: ClaudeModel) {
            localState.update { it.copy(pendingModel = model) }
            val id = conversationId.value ?: return
            viewModelScope.launch { repository.setModel(id, model) }
        }

        fun onDeleteRequested() {
            localState.update { it.copy(deleteDialogVisible = true) }
        }

        fun onDeleteDismissed() {
            localState.update { it.copy(deleteDialogVisible = false) }
        }

        /**
         * `deleted` is set only once the repository has finished, which is what keeps
         * [viewModelScope] alive through the delete: the screen is popped in response to it, and a
         * pop cancels the scope.
         */
        fun onDeleteConfirmed() {
            val id = conversationId.value ?: return
            localState.update { it.copy(deleteDialogVisible = false) }
            viewModelScope.launch {
                repository.delete(id)
                localState.update { it.copy(deleted = true) }
            }
        }

        private fun rememberConversation(id: Long) {
            savedStateHandle[KEY_CONVERSATION_ID] = id
            conversationId.value = id
        }

        private fun forgetConversation() {
            savedStateHandle[KEY_CONVERSATION_ID] = null
            conversationId.value = null
        }
    }
```

- [ ] **Step 8: Run the ViewModel tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationViewModelTest*"
```

Expected: PASS, all twelve.

- [ ] **Step 9: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :feature:conversation:testDebugUnitTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Report the two `ConversationUiState` fields added beyond the spec's listing and why. Stop for review.

---

### Task 6: The chat message rows

Everything that renders inside the message list: the bubble (3b), the thinking indicator (3f), the inline error with retry (3g, 3h), and the new-chat empty state (3a). Suggested prompt chips in 3a and tool chips in 3c/3d are **not built**.

**Files:**
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ChatErrorText.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/MessageBubble.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ThinkingIndicator.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ErrorRow.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/NewChatEmptyState.kt`
- Test: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ErrorRowTest.kt`

**Interfaces:**
- Consumes: `ChatError`, `Role`, `Glyphs`, `ComponentShapes.bubbleUser` / `.bubbleAssistant`, `Motion.caretBlinkMillis`, the error strings from Task 3.
- Produces: `@Composable fun ChatError.text(): String`.
- Produces: `MessageBubble(text: String, role: Role, modifier, streaming: Boolean = false)`.
- Produces: `ThinkingIndicator(modifier)`.
- Produces: `ErrorRow(error: ChatError, onRetry: () -> Unit, modifier)`.
- Produces: `NewChatEmptyState(modifier)`.

- [ ] **Step 1: Write the failing error-copy test**

Every `ChatError` case must resolve to distinct copy, and the rate-limited case must change when the server sent a hint. Create `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ErrorRowTest.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.text
import com.shayanaryan.chatbot.shared.chat.ChatError
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ErrorRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val allErrors =
        listOf(
            ChatError.Authentication,
            ChatError.RateLimited(retryAfterSeconds = null),
            ChatError.RateLimited(retryAfterSeconds = 30),
            ChatError.Overloaded,
            ChatError.InvalidRequest,
            ChatError.Server,
            ChatError.Network,
            ChatError.Timeout,
            ChatError.Unexpected,
        )

    @Test
    fun `every failure resolves to its own copy`() {
        val resolved = mutableListOf<String>()
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                allErrors.forEach { resolved += it.text() }
            }
        }

        assertEquals(allErrors.size, resolved.distinct().size)
        assertTrue(resolved.none { it.isBlank() })
    }

    @Test
    fun `a rate limit with a retry hint names the wait`() {
        var text = ""
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) { text = ChatError.RateLimited(30).text() }
        }

        assertTrue(text.contains("30"))
    }

    @Test
    fun `retry reports a tap`() {
        var retried = false
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ErrorRow(error = ChatError.Network, onRetry = { retried = true })
            }
        }

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(true, retried)
    }

    @Test
    fun `the network failure renders its sentence`() {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ErrorRow(error = ChatError.Network, onRetry = {})
            }
        }

        composeRule
            .onNodeWithText("Couldn't reach the API. Check your connection and try again.")
            .assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ErrorRowTest*"
```

Expected: FAIL to compile. `ErrorRow` and `ChatError.text` are unresolved.

- [ ] **Step 3: Map errors to copy**

Create `ChatErrorText.kt`. This is the whole reason `ChatError` carries no prose: typed errors cross the module boundary, copy stops here.

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shayanaryan.chatbot.shared.chat.ChatError

/**
 * The one place a [ChatError] becomes something a person reads. `:shared` deliberately carries no
 * text a user sees, so every case is resolved here against this module's own resources.
 */
@Composable
fun ChatError.text(): String =
    when (this) {
        ChatError.Authentication -> stringResource(R.string.conversation_error_authentication)
        is ChatError.RateLimited ->
            if (retryAfterSeconds == null) {
                stringResource(R.string.conversation_error_rate_limited)
            } else {
                stringResource(R.string.conversation_error_rate_limited_after, retryAfterSeconds)
            }
        ChatError.Overloaded -> stringResource(R.string.conversation_error_overloaded)
        ChatError.InvalidRequest -> stringResource(R.string.conversation_error_invalid_request)
        ChatError.Server -> stringResource(R.string.conversation_error_server)
        ChatError.Network -> stringResource(R.string.conversation_error_network)
        ChatError.Timeout -> stringResource(R.string.conversation_error_timeout)
        ChatError.Unexpected -> stringResource(R.string.conversation_error_unexpected)
    }
```

- [ ] **Step 4: Build the bubble**

Create `component/MessageBubble.kt`. The Design System's `MessageBubble` contract gives the two corner shapes, the two colour pairs and the blinking caret; `:core:ui` already carries the shapes as `ComponentShapes.bubbleUser` / `.bubbleAssistant` and the blink period as `Motion.caretBlinkMillis`.

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Motion
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.shared.chat.Role

private const val BUBBLE_MAX_WIDTH_FRACTION = 0.82f

/**
 * One chat turn.
 *
 * @param role user turns sit right in the primary container, assistant turns left on the surface.
 * @param streaming appends a blinking caret, so a reply arriving one token at a time reads as
 *   still in progress rather than as a short answer.
 */
@Composable
fun MessageBubble(
    text: String,
    role: Role,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
) {
    val isUser = role == Role.User
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(BUBBLE_MAX_WIDTH_FRACTION)
                    .wrapContentWidth(if (isUser) Alignment.End else Alignment.Start)
                    .background(
                        color =
                            if (isUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        shape = if (isUser) ComponentShapes.bubbleUser else ComponentShapes.bubbleAssistant,
                    ).padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                modifier = Modifier.weight(1f, fill = false),
            )
            if (streaming) {
                StreamingCaret(Modifier.padding(start = Spacing.s1))
            }
        }
    }
}

@Composable
private fun StreamingCaret(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "streaming-caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(Motion.caretBlinkMillis / 2, easing = Motion.easingStandard),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "streaming-caret-alpha",
    )
    Box(
        modifier
            .alpha(alpha)
            .width(8.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
    )
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleUserPreview() {
    ChatbotTheme(darkTheme = true) { MessageBubble(text = "help me plan a weekend in portland", role = Role.User) }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleAssistantPreview() {
    ChatbotTheme(darkTheme = true) {
        MessageBubble(
            text = "Love it. Two nights? I'd do Powell's Books + a food-cart lunch Saturday.",
            role = Role.Assistant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubbleStreamingPreview() {
    ChatbotTheme(darkTheme = true) {
        MessageBubble(
            text = "Love it. Two nights? I'd do Powell's Books + a food-cart lunch Saturday, then Forest Park in the",
            role = Role.Assistant,
            streaming = true,
        )
    }
}
```

Fix the imports the compiler asks for: `Arrangement`, `height`, `wrapContentWidth`. Drop `size` and `layout` if unused.

- [ ] **Step 5: Build the thinking indicator**

Create `component/ThinkingIndicator.kt`, frame 3f's three staggered dots in an assistant-shaped bubble:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R

private const val DOT_COUNT = 3
private const val DOT_CYCLE_MILLIS = 1200
private const val DOT_STAGGER_MILLIS = 200
private const val DOT_MIN_ALPHA = 0.25f

/** The turn has started but no token has arrived yet. */
@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.conversation_thinking)
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier =
            modifier
                .semantics { contentDescription = description }
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = ComponentShapes.bubbleAssistant,
                ).padding(Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = DOT_MIN_ALPHA,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(DOT_CYCLE_MILLIS / 2),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = StartOffset(index * DOT_STAGGER_MILLIS),
                    ),
                label = "thinking-dot-$index",
            )
            Box(
                Modifier
                    .alpha(alpha)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThinkingIndicatorPreview() {
    ChatbotTheme(darkTheme = true) { ThinkingIndicator() }
}
```

- [ ] **Step 6: Build the error row**

Create `component/ErrorRow.kt`, frames 3g and 3h, which differ only in copy:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.R as CoreUiR
import com.shayanaryan.chatbot.core.ui.designsystem.component.ButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ComponentShapes
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.text
import com.shayanaryan.chatbot.shared.chat.ChatError

private const val ERROR_MAX_WIDTH_FRACTION = 0.86f

/**
 * A turn that failed, rendered inline where the reply would have been. Losing connectivity gets no
 * special treatment: it is `ChatError.Network` and lands here like any other failure.
 */
@Composable
fun ErrorRow(
    error: ChatError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(ERROR_MAX_WIDTH_FRACTION),
        verticalArrangement = Arrangement.spacedBy(Spacing.s2),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier =
                Modifier
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = ComponentShapes.bubbleAssistant,
                    ).padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s3),
        ) {
            DsIcon(
                glyph = Glyphs.ERROR,
                contentDescription = null,
                size = 20.dp,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = error.text(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        DsButton(
            text = stringResource(CoreUiR.string.core_ui_retry),
            onClick = onRetry,
            variant = ButtonVariant.Tonal,
            leadingGlyph = Glyphs.REFRESH,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorRowRateLimitedPreview() {
    ChatbotTheme(darkTheme = true) { ErrorRow(error = ChatError.RateLimited(retryAfterSeconds = null), onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ErrorRowNetworkPreview() {
    ChatbotTheme(darkTheme = true) { ErrorRow(error = ChatError.Network, onRetry = {}) }
}
```

- [ ] **Step 7: Build the new-chat empty state**

Create `component/NewChatEmptyState.kt`, frame 3a's body, minus the suggested prompt chips (M4). `:app` also uses this as the detail pane's placeholder on a wide window, which is why it is public.

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.RadiusPrimitives
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R

/** A chat with no first message yet. */
@Composable
fun NewChatEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.s6),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(RadiusPrimitives.radius5),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            DsIcon(
                glyph = Glyphs.BRAND,
                contentDescription = null,
                size = 30.dp,
                filled = true,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = stringResource(R.string.conversation_new_chat_greeting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, heightDp = 400)
@Composable
private fun NewChatEmptyStatePreview() {
    ChatbotTheme(darkTheme = true) { NewChatEmptyState() }
}
```

- [ ] **Step 8: Run the error-row tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ErrorRowTest*"
```

Expected: PASS, all four.

- [ ] **Step 9: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :feature:conversation:testDebugUnitTest :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Screenshot goldens for these come with the whole screen in Task 7: the frames the design draws are screens, not isolated components. Stop for review.

---

### Task 7: The chat screen

The chrome around the rows (top bar, composer, model picker, overflow menu, delete dialog), and the stateless screen and route that assemble it. Frames 3a, 3b, 3e, 3f, 3g, 3h, 3i, 3j.

**Files:**
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/Composer.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/ModelPickerChip.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/component/DeleteChatDialog.kt`
- Replace: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationScreen.kt`
- Create: `feature/conversation/src/main/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationRoute.kt`
- Replace: `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationScreenTest.kt`
- Create: `feature/conversation/src/screenshotTest/kotlin/com/shayanaryan/chatbot/feature/conversation/preview/ChatPreviews.kt`

**Interfaces:**
- Consumes: `ConversationUiState`, `ChatRow`, `MessageBubble`, `ThinkingIndicator`, `ErrorRow`, `NewChatEmptyState`, `textContent()`.
- Produces: `ConversationScreen(uiState, onBack, onSend, onCancel, onRetry, onModelSelected, onDeleteRequested, onDeleteDismissed, onDeleteConfirmed, modifier, composerState)`, stateless; `composerState` defaults to `rememberTextFieldState()`.
- Produces: `internal object ChatPreviewData`, the fixture the colocated previews and the screenshot goldens share.
- Produces: `ConversationRoute(conversationId, onBack, onDeleted, onConversationIdChanged, modifier, viewModel)`, stateful, what `:app` calls.
- Produces: `Composer(state: TextFieldState, isStreaming: Boolean, onSend: (String) -> Unit, onCancel: () -> Unit, modifier)`.
- Produces: `ModelPickerChip(model: ClaudeModel, enabled: Boolean, onModelSelected: (ClaudeModel) -> Unit, modifier)`.
- Produces: `DeleteChatDialog(onConfirm: () -> Unit, onDismiss: () -> Unit)`.

**Test note that will otherwise cost an hour:** `ThinkingIndicator` and the streaming caret use `rememberInfiniteTransition`, which never lets the Compose test clock go idle. Any test that puts a thinking or streaming row on screen must set `composeRule.mainClock.autoAdvance = false` **before** `setContent`, or `waitForIdle` hangs.

- [ ] **Step 1: Write the failing screen tests**

Replace `feature/conversation/src/test/kotlin/com/shayanaryan/chatbot/feature/conversation/ConversationScreenTest.kt`. The M0 placeholder test goes with the placeholder screen:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.Message
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun persisted(
        id: Long,
        role: Role,
        text: String,
    ) = ChatRow.Persisted(
        Message(
            id = id,
            conversationId = 1L,
            role = role,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = Instant.fromEpochMilliseconds(id),
        ),
    )

    private val openChat =
        ConversationUiState(
            conversationId = 1L,
            title = "Weekend trip to Portland",
            model = ClaudeModel.Sonnet,
            rows =
                listOf(
                    persisted(1L, Role.User, "help me plan a weekend in portland"),
                    persisted(2L, Role.Assistant, "Powell's Books first."),
                ),
        )

    private fun setScreen(
        uiState: ConversationUiState,
        onBack: (() -> Unit)? = {},
        onSend: (String) -> Unit = {},
        onCancel: () -> Unit = {},
        onRetry: () -> Unit = {},
        onModelSelected: (ClaudeModel) -> Unit = {},
        onDeleteRequested: () -> Unit = {},
        onDeleteDismissed: () -> Unit = {},
        onDeleteConfirmed: () -> Unit = {},
    ) {
        composeRule.setContent {
            ChatbotTheme(darkTheme = true) {
                ConversationScreen(
                    uiState = uiState,
                    onBack = onBack,
                    onSend = onSend,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    onModelSelected = onModelSelected,
                    onDeleteRequested = onDeleteRequested,
                    onDeleteDismissed = onDeleteDismissed,
                    onDeleteConfirmed = onDeleteConfirmed,
                )
            }
        }
    }

    @Test
    fun `renders the title and both bubbles`() {
        setScreen(openChat)

        composeRule.onNodeWithText("Weekend trip to Portland").assertIsDisplayed()
        composeRule.onNodeWithText("help me plan a weekend in portland").assertIsDisplayed()
        composeRule.onNodeWithText("Powell's Books first.").assertIsDisplayed()
    }

    @Test
    fun `a chat with no first message shows the new-chat copy`() {
        setScreen(ConversationUiState())

        composeRule.onNodeWithText("new chat").assertIsDisplayed()
        composeRule.onNodeWithText("How's it going?").assertIsDisplayed()
    }

    @Test
    fun `send is disabled until there is non-blank text`() {
        setScreen(openChat)

        composeRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
        composeRule.onNodeWithText("tell me…").performTextInput("hello")
        composeRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    @Test
    fun `send reports the composed text and clears the field`() {
        var sent: String? = null
        setScreen(openChat, onSend = { sent = it })

        composeRule.onNodeWithText("tell me…").performTextInput("a packing list please")
        composeRule.onNodeWithContentDescription("Send").performClick()

        assertEquals("a packing list please", sent)
        composeRule.onNodeWithText("tell me…").assertIsDisplayed()
    }

    @Test
    fun `while streaming the trailing button stops the turn`() {
        composeRule.mainClock.autoAdvance = false
        var cancelled = false
        setScreen(
            openChat.copy(rows = openChat.rows + ChatRow.Streaming("Powell"), isStreaming = true),
            onCancel = { cancelled = true },
        )

        composeRule.onNodeWithContentDescription("Stop").performClick()

        assertTrue(cancelled)
    }

    @Test
    fun `the model picker checkmarks the current model and reports a change`() {
        var picked: ClaudeModel? = null
        setScreen(openChat, onModelSelected = { picked = it })

        composeRule.onNodeWithText("Sonnet 5").performClick()
        composeRule.onNodeWithText("Haiku 4.5").performClick()

        assertEquals(ClaudeModel.Haiku, picked)
    }

    @Test
    fun `the model picker is disabled during a turn`() {
        composeRule.mainClock.autoAdvance = false
        setScreen(openChat.copy(isStreaming = true))

        composeRule.onNodeWithText("Sonnet 5").assertIsNotEnabled()
    }

    @Test
    fun `the overflow menu offers delete and reports it`() {
        var requested = false
        setScreen(openChat, onDeleteRequested = { requested = true })

        composeRule.onNodeWithContentDescription("More").performClick()
        composeRule.onNodeWithText("Delete chat").performClick()

        assertTrue(requested)
    }

    @Test
    fun `the overflow button is hidden on a chat with nothing to delete`() {
        setScreen(ConversationUiState())

        composeRule.onNodeWithContentDescription("More").assertDoesNotExist()
    }

    @Test
    fun `confirming the delete dialog reports it`() {
        var confirmed = false
        setScreen(openChat.copy(deleteDialogVisible = true), onDeleteConfirmed = { confirmed = true })

        composeRule.onNodeWithText("Delete this chat?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun `retry on the error row reports it`() {
        var retried = false
        setScreen(openChat.copy(rows = openChat.rows + ChatRow.Error(ChatError.Network)), onRetry = { retried = true })

        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun `the back arrow is absent when the caller gives no back action`() {
        setScreen(openChat, onBack = null)

        composeRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }
}
```

Add `import androidx.compose.ui.test.assertDoesNotExist`.

- [ ] **Step 2: Run them to verify they fail**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationScreenTest*"
```

Expected: FAIL. The placeholder `ConversationScreen()` takes no arguments.

- [ ] **Step 3: Build the composer**

Create `component/Composer.kt`. The composer is a token-styled `<div>` in the mockup, not a catalog component, so it is built here rather than reaching for `DsTextField`, and its text is held in a saveable `TextFieldState`, so it survives rotation without ever entering `UiState`.

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsIconButton
import com.shayanaryan.chatbot.core.ui.designsystem.component.IconButtonVariant
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.R

/**
 * The message input and its trailing action.
 *
 * @param state the composed text. Held by the screen in a saveable [TextFieldState] rather than in
 *   `UiState`: it survives rotation on its own, and the ViewModel only ever sees the finished
 *   string.
 * @param isStreaming turns the trailing button from send into stop, which is the only control a
 *   user has over a turn in flight.
 */
@Composable
fun Composer(
    state: TextFieldState,
    isStreaming: Boolean,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canSend by remember(state) { derivedStateOf { state.text.isNotBlank() } }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.extraLarge,
                    ).padding(horizontal = Spacing.s4, vertical = Spacing.s3),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                state = state,
                textStyle =
                    LocalTextStyle.current.merge(
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                decorator = { inner ->
                    if (state.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.conversation_composer_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
        }
        if (isStreaming) {
            DsIconButton(
                glyph = Glyphs.STOP,
                contentDescription = stringResource(R.string.conversation_stop),
                onClick = onCancel,
                variant = IconButtonVariant.Filled,
            )
        } else {
            DsIconButton(
                glyph = Glyphs.ARROW_UPWARD,
                contentDescription = stringResource(R.string.conversation_send),
                onClick = {
                    onSend(state.text.toString())
                    state.clearText()
                },
                variant = IconButtonVariant.Filled,
                enabled = canSend,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposerEmptyPreview() {
    ChatbotTheme(darkTheme = true) {
        Composer(state = rememberTextFieldState(), isStreaming = false, onSend = {}, onCancel = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ComposerStreamingPreview() {
    ChatbotTheme(darkTheme = true) {
        Composer(state = rememberTextFieldState(), isStreaming = true, onSend = {}, onCancel = {})
    }
}
```

Drop the `RowScope` and `TextStyle` imports if the compiler reports them unused, and hoist `SolidColor` to a proper import.

- [ ] **Step 4: Build the model picker chip**

Create `component/ModelPickerChip.kt`, frame 3e's chip and menu. The per-model blurbs the mockup draws are M4; only the names are built, and they come from `ClaudeModel.displayName` so every screen reads one source.

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * The per-conversation model switch.
 *
 * @param enabled false during a turn, for the same reason the composer's send button is: the model
 *   a request already went out with cannot be changed.
 */
@Composable
fun ModelPickerChip(
    model: ClaudeModel,
    enabled: Boolean,
    onModelSelected: (ClaudeModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier =
                Modifier
                    .height(36.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .background(
                        color =
                            if (expanded) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                androidx.compose.ui.graphics.Color.Transparent
                            },
                        shape = CircleShape,
                    ).clickable(enabled = enabled) { expanded = true }
                    .padding(start = Spacing.s3, end = Spacing.s2),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            DsIcon(
                glyph = if (expanded) Glyphs.EXPAND_LESS else Glyphs.EXPAND_MORE,
                contentDescription = null,
                size = 18.dp,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ClaudeModel.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        expanded = false
                        onModelSelected(option)
                    },
                    trailingIcon = {
                        if (option == model) {
                            DsIcon(
                                glyph = Glyphs.CHECK,
                                contentDescription = null,
                                size = 20.dp,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ModelPickerChipPreview() {
    ChatbotTheme(darkTheme = true) { ModelPickerChip(model = ClaudeModel.Sonnet, enabled = true, onModelSelected = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ModelPickerChipDisabledPreview() {
    ChatbotTheme(darkTheme = true) { ModelPickerChip(model = ClaudeModel.Haiku, enabled = false, onModelSelected = {}) }
}
```

The test asserts `assertIsNotEnabled()` on the chip's text node, so the `Row` needs its disabled state in semantics. `Modifier.clickable(enabled = …)` already reports it; if the assertion targets the wrong node, add `.semantics(mergeDescendants = true) {}` to the `Row` and keep the assertion on the model name.

- [ ] **Step 5: Build the delete dialog**

Create `component/DeleteChatDialog.kt`, frame 3j, a straight `DsDialog` call:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsDialog
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.feature.conversation.R

/** Destructive and local-only, so it asks first and says why it matters. */
@Composable
fun DeleteChatDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DsDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.conversation_delete_title),
        text = stringResource(R.string.conversation_delete_body),
        glyph = Glyphs.DELETE,
        confirmText = stringResource(R.string.conversation_delete_confirm),
        onConfirm = onConfirm,
        dismissText = stringResource(R.string.conversation_delete_cancel),
        onDismiss = onDismiss,
    )
}

@Preview
@Composable
private fun DeleteChatDialogPreview() {
    ChatbotTheme(darkTheme = true) { DeleteChatDialog(onConfirm = {}, onDismiss = {}) }
}
```

- [ ] **Step 6: Build the stateless screen**

Replace `ConversationScreen.kt` entirely:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.shayanaryan.chatbot.core.ui.designsystem.component.DsIconButton
import com.shayanaryan.chatbot.core.ui.designsystem.icon.DsIcon
import com.shayanaryan.chatbot.core.ui.designsystem.icon.Glyphs
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.designsystem.theme.Spacing
import com.shayanaryan.chatbot.feature.conversation.component.Composer
import com.shayanaryan.chatbot.feature.conversation.component.DeleteChatDialog
import com.shayanaryan.chatbot.feature.conversation.component.ErrorRow
import com.shayanaryan.chatbot.feature.conversation.component.MessageBubble
import com.shayanaryan.chatbot.feature.conversation.component.ModelPickerChip
import com.shayanaryan.chatbot.feature.conversation.component.NewChatEmptyState
import com.shayanaryan.chatbot.feature.conversation.component.ThinkingIndicator
import com.shayanaryan.chatbot.shared.chat.textContent
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * One conversation.
 *
 * Stateless. This overload is what previews, screenshot tests and Compose tests drive.
 *
 * @param onBack null on a wide window, where the chat sits beside the list and there is nothing to
 *   go back to. Passing the affordance in rather than deciding it here is what keeps the screen
 *   stateless and both states screenshot-testable.
 * @param composerState hoisted so a test can drive it; the default is saveable, so composed text
 *   survives rotation without ever entering `UiState`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    uiState: ConversationUiState,
    onBack: (() -> Unit)?,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onModelSelected: (ClaudeModel) -> Unit,
    onDeleteRequested: () -> Unit,
    onDeleteDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    composerState: TextFieldState = rememberTextFieldState(),
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBack != null) {
                        DsIconButton(
                            glyph = Glyphs.ARROW_BACK,
                            contentDescription = stringResource(R.string.conversation_back),
                            onClick = onBack,
                        )
                    }
                },
                title = {
                    Text(
                        text = uiState.title ?: stringResource(R.string.conversation_new_chat_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    // A chat with no first message has nothing to delete, so it gets no menu.
                    if (uiState.conversationId != null) {
                        DsIconButton(
                            glyph = Glyphs.MORE_VERT,
                            contentDescription = stringResource(R.string.conversation_more),
                            onClick = { overflowExpanded = true },
                        )
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.conversation_delete)) },
                                leadingIcon = {
                                    DsIcon(glyph = Glyphs.DELETE, contentDescription = null)
                                },
                                onClick = {
                                    overflowExpanded = false
                                    onDeleteRequested()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = Spacing.s3, vertical = Spacing.s2),
                verticalArrangement = Arrangement.spacedBy(Spacing.s2),
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ModelPickerChip(
                    model = uiState.model,
                    enabled = !uiState.isStreaming,
                    onModelSelected = onModelSelected,
                )
                Composer(
                    state = composerState,
                    isStreaming = uiState.isStreaming,
                    onSend = onSend,
                    onCancel = onCancel,
                )
            }
        },
    ) { padding ->
        if (uiState.rows.isEmpty()) {
            NewChatEmptyState(Modifier.padding(padding))
        } else {
            MessageList(
                rows = uiState.rows,
                onRetry = onRetry,
                modifier = Modifier.padding(padding),
            )
        }
    }
    if (uiState.deleteDialogVisible) {
        DeleteChatDialog(onConfirm = onDeleteConfirmed, onDismiss = onDeleteDismissed)
    }
}

@Composable
private fun MessageList(
    rows: List<ChatRow>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Follow the tail while tokens arrive, and stop following once the user scrolls up: the check
    // is taken before the new content is laid out, so it answers "was the user at the bottom".
    LaunchedEffect(rows) {
        if (!listState.canScrollForward && rows.isNotEmpty()) {
            listState.scrollToItem(rows.lastIndex)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = Spacing.s4, vertical = Spacing.s2),
        verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    ) {
        items(rows) { row ->
            when (row) {
                is ChatRow.Persisted ->
                    MessageBubble(
                        text = row.message.content.textContent(),
                        role = row.message.role,
                    )

                ChatRow.Thinking -> ThinkingIndicator()
                is ChatRow.Streaming ->
                    MessageBubble(
                        text = row.text,
                        role = com.shayanaryan.chatbot.shared.chat.Role.Assistant,
                        streaming = true,
                    )

                is ChatRow.Error -> ErrorRow(error = row.error, onRetry = onRetry)
            }
        }
    }
}
```

Add the imports the compiler asks for (`androidx.compose.foundation.background`, `androidx.compose.foundation.lazy.items`, `Role`) and replace the fully-qualified `Role.Assistant` with the import.

- [ ] **Step 7: Add the shared preview fixture and the colocated previews**

One fixture, used by both the colocated previews below and the screenshot goldens in Step 10. Append to `ConversationScreen.kt`:

```kotlin
/**
 * The states the design draws, as one fixture the colocated previews and the screenshot goldens
 * both read, so a golden and a preview can never disagree about what a state contains.
 */
internal object ChatPreviewData {
    private fun message(
        id: Long,
        role: Role,
        text: String,
    ) = ChatRow.Persisted(
        Message(
            id = id,
            conversationId = 1L,
            role = role,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = Instant.fromEpochMilliseconds(id),
        ),
    )

    val newChat = ConversationUiState()

    val openChat =
        ConversationUiState(
            conversationId = 1L,
            title = "Weekend trip to Portland",
            model = ClaudeModel.Sonnet,
            rows =
                listOf(
                    message(1L, Role.User, "help me plan a weekend in portland"),
                    message(
                        2L,
                        Role.Assistant,
                        "Powell's Books + a food-cart lunch Saturday, then Forest Park in the morning.",
                    ),
                ),
        )

    val thinking =
        openChat.copy(
            rows = listOf(openChat.rows.first()) + ChatRow.Thinking,
            isStreaming = true,
        )

    val streaming =
        openChat.copy(
            rows =
                listOf(openChat.rows.first()) +
                    ChatRow.Streaming(
                        "Love it. Two nights? I'd do Powell's Books + a food-cart lunch Saturday, then Forest Park in the",
                    ),
            isStreaming = true,
        )

    val rateLimited =
        openChat.copy(rows = openChat.rows + ChatRow.Error(ChatError.RateLimited(retryAfterSeconds = null)))

    val network = openChat.copy(rows = openChat.rows + ChatRow.Error(ChatError.Network))

    val deleting = openChat.copy(deleteDialogVisible = true)
}

@Composable
private fun PreviewChat(
    uiState: ConversationUiState,
    onBack: (() -> Unit)? = {},
) {
    ConversationScreen(
        uiState = uiState,
        onBack = onBack,
        onSend = {},
        onCancel = {},
        onRetry = {},
        onModelSelected = {},
        onDeleteRequested = {},
        onDeleteDismissed = {},
        onDeleteConfirmed = {},
    )
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatNewPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.newChat) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatIdlePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatThinkingPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.thinking) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatStreamingPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.streaming) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatRateLimitedPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.rateLimited) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatNetworkFailurePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.network) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatDeleteDialogPreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.deleting) }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ChatTwoPanePreview() {
    ChatbotTheme(darkTheme = true) { PreviewChat(ChatPreviewData.openChat, onBack = null) }
}
```

Add `ChatError`, `ContentBlock`, `Message`, `MessageStatus`, `Role` and `kotlin.time.Instant` to the file's imports.

- [ ] **Step 8: Build the stateful route**

Create `ConversationRoute.kt`:

```kotlin
package com.shayanaryan.chatbot.feature.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful half of the chat screen. Seeds its ViewModel with the id the navigation key carried and
 * reports the live id back up, because on a wide window the list beside it highlights the open
 * conversation and the key deliberately never learns the id.
 *
 * @param onBack null on a wide window, which hides the back arrow.
 * @param onDeleted called once a confirmed delete has finished: the caller pops on a narrow
 *   window, or returns the detail pane to a new chat on a wide one.
 */
@Composable
fun ConversationRoute(
    conversationId: Long?,
    onBack: (() -> Unit)?,
    onDeleted: () -> Unit,
    onConversationIdChanged: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationViewModel =
        hiltViewModel<ConversationViewModel, ConversationViewModel.Factory>(
            creationCallback = { factory -> factory.create(conversationId) },
        ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.conversationId) { onConversationIdChanged(uiState.conversationId) }
    LaunchedEffect(uiState.deleted) { if (uiState.deleted) onDeleted() }

    ConversationScreen(
        uiState = uiState,
        onBack = onBack,
        onSend = viewModel::onSend,
        onCancel = viewModel::onCancel,
        onRetry = viewModel::onRetry,
        onModelSelected = viewModel::onModelSelected,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteDismissed = viewModel::onDeleteDismissed,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        modifier = modifier,
    )
}
```

- [ ] **Step 9: Run the screen tests to verify they pass**

```bash
./gradlew :feature:conversation:testDebugUnitTest --tests "*ConversationScreenTest*"
```

Expected: PASS, all twelve. If `waitForIdle` hangs, the test that hangs is missing `composeRule.mainClock.autoAdvance = false` before `setContent`.

- [ ] **Step 10: Record the chat screenshot goldens**

Create `feature/conversation/src/screenshotTest/kotlin/com/shayanaryan/chatbot/feature/conversation/preview/ChatPreviews.kt`, one `@PreviewTest` pair per built frame, dark and light, driving the stateless screen with the fixture from Step 7:

```kotlin
package com.shayanaryan.chatbot.feature.conversation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.core.ui.preview.FormFactorPreviews
import com.shayanaryan.chatbot.feature.conversation.ChatPreviewData
import com.shayanaryan.chatbot.feature.conversation.ConversationScreen
import com.shayanaryan.chatbot.feature.conversation.ConversationUiState

@Composable
private fun Chat(
    uiState: ConversationUiState,
    onBack: (() -> Unit)? = {},
) {
    ConversationScreen(
        uiState = uiState,
        onBack = onBack,
        onSend = {},
        onCancel = {},
        onRetry = {},
        onModelSelected = {},
        onDeleteRequested = {},
        onDeleteDismissed = {},
        onDeleteConfirmed = {},
    )
}

@PreviewTest
@Preview(name = "chat-3a-new-dark", heightDp = 780)
@Composable
private fun ChatNewDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.newChat) }
}

@PreviewTest
@Preview(name = "chat-3a-new-light", heightDp = 780)
@Composable
private fun ChatNewLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.newChat) }
}

@PreviewTest
@Preview(name = "chat-3b-streaming-dark", heightDp = 780)
@Composable
private fun ChatStreamingDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.streaming) }
}

@PreviewTest
@Preview(name = "chat-3b-streaming-light", heightDp = 780)
@Composable
private fun ChatStreamingLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.streaming) }
}

// 3e's menu is driven by the chip's own state, which a preview cannot open, so the golden
// captures the chip in its collapsed state on an idle chat. The open menu is covered by the
// Compose test instead.
@PreviewTest
@Preview(name = "chat-3e-picker-dark", heightDp = 780)
@Composable
private fun ChatPickerDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.openChat) }
}

@PreviewTest
@Preview(name = "chat-3e-picker-light", heightDp = 780)
@Composable
private fun ChatPickerLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.openChat) }
}

@PreviewTest
@Preview(name = "chat-3f-thinking-dark", heightDp = 780)
@Composable
private fun ChatThinkingDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.thinking) }
}

@PreviewTest
@Preview(name = "chat-3f-thinking-light", heightDp = 780)
@Composable
private fun ChatThinkingLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.thinking) }
}

@PreviewTest
@Preview(name = "chat-3g-rate-limited-dark", heightDp = 780)
@Composable
private fun ChatRateLimitedDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.rateLimited) }
}

@PreviewTest
@Preview(name = "chat-3g-rate-limited-light", heightDp = 780)
@Composable
private fun ChatRateLimitedLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.rateLimited) }
}

@PreviewTest
@Preview(name = "chat-3h-network-dark", heightDp = 780)
@Composable
private fun ChatNetworkDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.network) }
}

@PreviewTest
@Preview(name = "chat-3h-network-light", heightDp = 780)
@Composable
private fun ChatNetworkLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.network) }
}

@PreviewTest
@Preview(name = "chat-3j-delete-dark", heightDp = 780)
@Composable
private fun ChatDeleteDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.deleting) }
}

@PreviewTest
@Preview(name = "chat-3j-delete-light", heightDp = 780)
@Composable
private fun ChatDeleteLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.deleting) }
}

// 3k: the same screen rendered as the detail pane, which is the whole visual difference:
// no back arrow. The two-pane composition itself lives in :app and is covered by the journey.
@PreviewTest
@Preview(name = "chat-3k-detail-pane-dark", heightDp = 700, widthDp = 664)
@Composable
private fun ChatDetailPaneDarkPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.openChat, onBack = null) }
}

@PreviewTest
@Preview(name = "chat-3k-detail-pane-light", heightDp = 700, widthDp = 664)
@Composable
private fun ChatDetailPaneLightPreview() {
    ChatbotTheme(darkTheme = false) { Chat(ChatPreviewData.openChat, onBack = null) }
}

@PreviewTest
@FormFactorPreviews
@Composable
private fun ChatFormFactorPreview() {
    ChatbotTheme(darkTheme = true) { Chat(ChatPreviewData.openChat) }
}
```

3i, the open overflow menu, gets no golden for the same reason as 3e: the menu is the top bar's own state and a preview cannot open it. Its Compose test in Step 1 is the coverage.

Then:

```bash
./gradlew :feature:conversation:updateDebugScreenshotTest
./gradlew :feature:conversation:validateDebugScreenshotTest
```

Expected: goldens written, then validated. Show the user the streaming and thinking goldens and the four form-factor ones, and get them to confirm the caret, the dots and each width. An animation captured at the wrong frame is a golden that will flake later. If either flakes across two consecutive `validate` runs, pin the animation by taking the golden from a static variant of the component instead, and say so in the report.

- [ ] **Step 11: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :feature:conversation:testDebugUnitTest :feature:conversation:validateDebugScreenshotTest spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. `:app` does not compile yet: `MainActivity` still calls the old no-argument `ConversationScreen()`. Task 8 replaces it; do not patch it here. Stop for review.

---

### Task 8: Navigation, the deep-link seam, and the dev key

`:app` gains the typed keys, the `NavDisplay`, the list-detail scene, the intent extra 010's notification will one day send, and a debug-only key so the chat actually talks to Claude before 006 exists.

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/NavKeys.kt`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/ChatbotApp.kt`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/ChatbotNavDisplay.kt`
- Replace: `app/src/main/kotlin/com/shayanaryan/chatbot/MainActivity.kt`
- Create: `app/src/debug/kotlin/com/shayanaryan/chatbot/di/DevApiKeyModule.kt`

**Interfaces:**
- Consumes: `ConversationListRoute` (Task 4) and `ConversationRoute` (Task 7), `NewChatEmptyState` (Task 6).
- Produces: `ConversationListKey`, `ChatKey(conversationId: Long? = null)`.
- Produces: `ChatbotApp(deepLinkConversationId: Long?, onDeepLinkHandled: () -> Unit, modifier)`, which owns the back stack.
- Produces: `ChatbotNavDisplay(backStack: NavBackStack<NavKey>, modifier)`, which owns the graph.
- Produces: `MainActivity.EXTRA_CONVERSATION_ID`.
- Produces: `BuildConfig.DEV_API_KEY` on debug builds, and an `ApiKeyProvider` binding in the debug source set only.

- [ ] **Step 1: Give `:app` its dependencies and the dev key field**

**Partly done in Task 3 Step 11a.** The `devApiKey` provider, `buildConfig = true` and the
`debug { buildConfigField(…) }` block are already in place; only the serialization plugin and the
four navigation artifacts remain. The full script is kept below for reference.

In `app/build.gradle.kts`, add the serialization plugin, turn on `buildConfig`, read the dev key through configuration-cache-safe providers, and add the nav and adaptive artifacts:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * The developer's own key, from local.properties. Will be removed when onboarding is implemented.
 */
val devApiKey: Provider<String> =
    providers
        .environmentVariable("ANTHROPIC_API_KEY")
        .orElse(
            providers
                .fileContents(layout.projectDirectory.file("../local.properties"))
                .asText
                .map { contents ->
                    contents
                        .lineSequence()
                        .map(String::trim)
                        .firstOrNull { it.startsWith("anthropic.api.key=") }
                        ?.substringAfter('=')
                        ?.trim()
                        .orEmpty()
                },
        ).orElse("")

android {
    namespace = "com.shayanaryan.chatbot"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.shayanaryan.chatbot"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "com.shayanaryan.chatbot.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEV_API_KEY", "\"${devApiKey.get()}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

and in `dependencies`, after `libs.androidx.lifecycle.runtime.compose`:

```kotlin
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.adaptive.navigation3)
```

`local.properties` is already git-ignored, and the key never reaches a release build: `DEV_API_KEY` is declared on the debug build type only.

- [ ] **Step 2: Bind the dev key, debug only**

**Already done in Task 3 Step 11a.** Verify the file below exists and matches, then move on.

Create `app/src/debug/kotlin/com/shayanaryan/chatbot/di/DevApiKeyModule.kt`:

```kotlin
package com.shayanaryan.chatbot.di

import com.shayanaryan.chatbot.BuildConfig
import com.shayanaryan.chatbot.shared.chat.ApiKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The developer's own key, for debug builds only. 006 replaces this with the real provider over
 * the encrypted store, and until then a release build has no [ApiKeyProvider] binding and does not
 * assemble. That is deliberate, since the alternative is a release-only stub that has to be remembered
 * and removed. The M1 sideload checkpoint is a debug build.
 */
@Module
@InstallIn(SingletonComponent::class)
object DevApiKeyModule {
    @Provides
    @Singleton
    fun provideApiKeyProvider(): ApiKeyProvider = DevApiKeyProvider(BuildConfig.DEV_API_KEY)
}

internal class DevApiKeyProvider(
    private val key: String,
) : ApiKeyProvider {
    override suspend fun apiKey(): String {
        check(key.isNotBlank()) {
            "No developer key. Set ANTHROPIC_API_KEY or anthropic.api.key in local.properties, " +
                "then rebuild."
        }
        return key
    }
}
```

- [ ] **Step 3: Declare the navigation keys**

Create `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/NavKeys.kt`:

```kotlin
package com.shayanaryan.chatbot.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ConversationListKey : NavKey

/**
 * @property conversationId null for a chat with no first message. A new chat keeps this key for
 *   its whole life: the first send creates a conversation, but rewriting the key would recreate
 *   the entry and therefore the ViewModel, mid-stream, resetting scroll. The ViewModel owns the
 *   live id instead. Switching to a *different* conversation is the opposite case and does rewrite
 *   the key, which is correct: a different conversation should get a different ViewModel.
 */
@Serializable
data class ChatKey(
    val conversationId: Long? = null,
) : NavKey
```

- [ ] **Step 4: Build the nav host, in two files**

Two responsibilities, so two files. `ChatbotApp` owns the back stack and the one thing that seeds it from outside the app; `ChatbotNavDisplay` owns what each key renders and how the panes arrange. The seam is one parameter wide, and `rememberNavBackStack` pins its return type to `NavBackStack<NavKey>` (the class delegates to `MutableList<NavKey>`), so the graph can push, replace and pop through it.

Create `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/ChatbotApp.kt`:

```kotlin
package com.shayanaryan.chatbot.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * The app's root composable. It owns the back stack, starts it on the conversation list, and hands
 * rendering to [ChatbotNavDisplay].
 *
 * @param deepLinkConversationId the conversation an intent asked to open, null on a normal launch.
 * @param onDeepLinkHandled called once that id has been applied, so the same intent cannot pull the
 *   user back to the conversation after they navigate away.
 */
@Composable
fun ChatbotApp(
    deepLinkConversationId: Long?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keys are serializable, which is what carries this across process death.
    val backStack = rememberNavBackStack(ConversationListKey)

    LaunchedEffect(deepLinkConversationId) {
        val id = deepLinkConversationId ?: return@LaunchedEffect
        backStack.clear()
        backStack.add(ConversationListKey)
        backStack.add(ChatKey(id))
        onDeepLinkHandled()
    }

    ChatbotNavDisplay(backStack = backStack, modifier = modifier)
}
```

Create `app/src/main/kotlin/com/shayanaryan/chatbot/navigation/ChatbotNavDisplay.kt`:

```kotlin
package com.shayanaryan.chatbot.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.shayanaryan.chatbot.feature.conversation.ConversationListRoute
import com.shayanaryan.chatbot.feature.conversation.ConversationRoute
import com.shayanaryan.chatbot.feature.conversation.component.NewChatEmptyState

/**
 * Maps every key to its route and lets the adaptive scene arrange them.
 *
 * @param backStack owned by the caller, since a launch intent has to be able to replace it
 *   wholesale.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChatbotNavDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
) {
    // The open conversation, reported up by the chat route because the key deliberately never
    // learns it. Plain remember is enough: the ViewModel's SavedStateHandle is the durable store,
    // and the lambda fires again on the first composition after a restore.
    var selectedConversationId by remember { mutableStateOf<Long?>(null) }

    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive =
        remember(windowAdaptiveInfo) {
            // Override the default so there is no horizontal gap between the panes.
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    val twoPane = directive.maxHorizontalPartitions > 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val openChat: (ChatKey) -> Unit = { key ->
        // Replace rather than stack: the list is always the entry below a chat, and a different
        // conversation should get a different ViewModel.
        if (backStack.lastOrNull() is ChatKey) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    // Typed, so the removed element is the lambda's coerced result rather than a discarded
    // expression the compiler warns about.
    val popChat: () -> Unit = { backStack.removeLastOrNull() }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        // The ViewModel store decorator is what scopes a ViewModel to its entry, so a different
        // ChatKey gets a different ConversationViewModel. Adding it means restating the default
        // saveable-state decorator alongside.
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<ConversationListKey>(
                    metadata =
                        ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                Surface(Modifier.fillMaxSize()) { NewChatEmptyState() }
                            },
                        ),
                ) {
                    ConversationListRoute(
                        // A narrow window never shows the list beside a chat, so nothing is selected.
                        selectedConversationId = if (twoPane) selectedConversationId else null,
                        onConversationClick = { id -> openChat(ChatKey(id)) },
                        onNewChat = { openChat(ChatKey()) },
                    )
                }
                entry<ChatKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ConversationRoute(
                        conversationId = key.conversationId,
                        onBack = if (twoPane) null else popChat,
                        // One path for both windows: popping the chat leaves the list, which on a
                        // wide window means the detail pane falls back to its placeholder, and the
                        // placeholder is the new-chat state.
                        onDeleted = popChat,
                        onConversationIdChanged = { selectedConversationId = it },
                    )
                }
            },
    )
}
```

- [ ] **Step 5: Replace `MainActivity`**

```kotlin
package com.shayanaryan.chatbot

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.shayanaryan.chatbot.core.ui.designsystem.theme.ChatbotTheme
import com.shayanaryan.chatbot.navigation.ChatbotApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * The conversation a launch intent asked for, cleared once the back stack has been seeded.
     * Snapshot state rather than a plain field because `onNewIntent` arrives from outside the
     * composition and has to recompose it.
     */
    private var deepLinkConversationId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch seeds. A recreated activity restores its own back stack, and its
        // intent still carries the extra it launched with, so reading that extra again would drag
        // the user back into the notification's conversation on every rotation.
        if (savedInstanceState == null) {
            deepLinkConversationId = intent.conversationIdExtra()
        }
        setContent {
            ChatbotTheme {
                ChatbotApp(
                    deepLinkConversationId = deepLinkConversationId,
                    onDeepLinkHandled = { deepLinkConversationId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Without this, getIntent() keeps returning the intent the activity launched with.
        setIntent(intent)
        deepLinkConversationId = intent.conversationIdExtra()
    }

    companion object {
        /** Set by 010's reminder notification to reopen the conversation that scheduled it. */
        const val EXTRA_CONVERSATION_ID: String = "com.shayanaryan.chatbot.extra.CONVERSATION_ID"
    }
}

/**
 * The id is only ever a database key, and a conversation that does not exist resolves to a new chat
 * rather than an error, so an intent from outside the app cannot do anything worse than open an
 * empty screen.
 */
private fun Intent.conversationIdExtra(): Long? =
    getLongExtra(MainActivity.EXTRA_CONVERSATION_ID, NO_CONVERSATION_ID).takeIf { it > 0 }

private const val NO_CONVERSATION_ID: Long = -1L
```

`ChatbotTheme` wraps here rather than inside `ChatbotApp`. It resolves the same light/dark bit that `enableEdgeToEdge`'s system bar style needs, and that call belongs to the Activity, not to the composition: one file holds both, so 007's theme setting cannot be read in two places that disagree. Leaving `ChatbotApp` unthemed also keeps it renderable in either theme by whatever hosts it, so a preview or a test of the shell needs no `darkTheme` parameter threaded through the navigation layer.

- [ ] **Step 6: Build and install**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

```bash
./gradlew :app:assembleRelease
```

Expected: **FAIL**, with Dagger reporting no binding for `ApiKeyProvider`. That is the intended state: the chat ViewModel now injects `ConversationRepository`, which pulls `ChatEngine`, which needs a key, and only 006 supplies one for release. Record the exact error text in the report so the reviewer can see it is the expected failure and not something else. Every later verification uses debug tasks only.

- [ ] **Step 7: Run the app on a phone emulator and click through it**

```bash
android emulator start Pixel_10
android run
```

Confirm by hand, and capture a screenshot of each: the list's empty state on first launch; a new chat that streams a real reply; the conversation appearing in the list with its snippet; reopening it with history intact; the model picker; the overflow menu and delete dialog.

If the reply fails with the `DevApiKeyProvider` check message, the key is not on the build machine. Set `ANTHROPIC_API_KEY` or add `anthropic.api.key` to `local.properties`, then rebuild.

- [ ] **Step 8: Checkpoint**

```bash
./gradlew spotlessApply
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest \
          :core:ui:testDebugUnitTest :feature:conversation:testDebugUnitTest \
          :feature:conversation:validateDebugScreenshotTest :core:ui:validateDebugScreenshotTest \
          :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`. Report the screenshots from Step 7 and the expected release failure from Step 6. Stop for review.

---

### Task 9: Journeys, the tablet AVD, and the spec edits this work owes

The M1 exit gate, plus the three documents this spec's implementation makes stale.

**Files:**
- Create: `journeys/m1-first-chat.xml`
- Create: `journeys/m1-resume.xml`
- Create: `journeys/m1-delete.xml`
- Create: `journeys/m1-retry.xml`
- Create: `journeys/m1-model-switch.xml`
- Create: `journeys/m1-two-pane.xml`
- Modify: `specs/005-conversation-shell.md:15`, `:75`
- Modify: `specs/001-tech-stack.md` (UI table)
- Modify: `docs/roadmap.md` (Status table)

- [ ] **Step 1: Create the tablet AVD**

`android emulator list` reports only `Pixel_8_API_35` and `Pixel_10`, neither of which is a tablet, so the two-pane journey has nothing to run on.

```bash
android emulator create medium_tablet
android emulator list
```

Expected: a third entry appears. Note its exact name: the journey and the launch command below both need it.

- [ ] **Step 2: Write the journey files**

Each is a `<journey>` with a `<description>` and `<actions>`, matching `journeys/m0-scaffold.xml`. Create all six:

`journeys/m1-first-chat.xml`:

```xml
<journey name="M1 first chat: send and stream a reply">
   <description>
      A new user starts a conversation, sends a message, and watches the reply
      stream in. The conversation then appears in the list with its snippet.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Verify the conversation list shows the empty state "no conversations yet"</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field</action>
     <action>Tap the send button</action>
     <action>Verify the typed message appears as a bubble on the right</action>
     <action>Wait until a reply bubble appears on the left containing "Hello"</action>
     <action>Tap the back arrow</action>
     <action>Verify the list now shows one conversation whose title is the message that was sent</action>
     <action>Verify that row's second line shows the reply text</action>
   </actions>
</journey>
```

`journeys/m1-resume.xml`:

```xml
<journey name="M1 resume: history survives a relaunch">
   <description>
      Chat history is stored on device, so killing the app and reopening a
      conversation shows every message that was there before.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field and tap send</action>
     <action>Wait until a reply bubble appears containing "Hello"</action>
     <action>Force stop the app</action>
     <action>Open the "buddy" app again</action>
     <action>Verify the conversation list shows the conversation</action>
     <action>Tap that conversation</action>
     <action>Verify both the sent message and the reply are still displayed</action>
   </actions>
</journey>
```

`journeys/m1-delete.xml`:

```xml
<journey name="M1 delete: a conversation can be removed">
   <description>
      Deleting is destructive and local-only, so it asks first and then the
      conversation is gone from the list.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field and tap send</action>
     <action>Wait until a reply bubble appears containing "Hello"</action>
     <action>Tap the "More" button in the top bar</action>
     <action>Tap "Delete chat"</action>
     <action>Verify a dialog titled "Delete this chat?" is displayed</action>
     <action>Tap "Delete"</action>
     <action>Verify the conversation list no longer contains that conversation</action>
   </actions>
</journey>
```

`journeys/m1-retry.xml`:

```xml
<journey name="M1 retry: a failed turn can be run again">
   <description>
      A turn that cannot reach the API shows an inline error with Retry rather
      than losing the message. Reconnecting and retrying produces the reply.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Enable airplane mode on the device</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field and tap send</action>
     <action>Verify an error message about no network connection is displayed</action>
     <action>Verify a "Retry" button is displayed below it</action>
     <action>Disable airplane mode on the device</action>
     <action>Tap "Retry"</action>
     <action>Wait until a reply bubble appears containing "Hello"</action>
     <action>Verify the error message is no longer displayed</action>
   </actions>
</journey>
```

`journeys/m1-model-switch.xml`:

```xml
<journey name="M1 model switch: the picker persists per conversation">
   <description>
      The model is a property of the conversation, so switching it survives
      leaving the screen and coming back.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field and tap send</action>
     <action>Wait until a reply bubble appears containing "Hello"</action>
     <action>Tap the model chip labelled "Sonnet 5" above the message field</action>
     <action>Verify a menu listing "Sonnet 5", "Haiku 4.5" and "Opus 5" is displayed</action>
     <action>Tap "Haiku 4.5"</action>
     <action>Verify the chip now reads "Haiku 4.5"</action>
     <action>Tap the back arrow</action>
     <action>Tap that conversation in the list</action>
     <action>Verify the chip still reads "Haiku 4.5"</action>
   </actions>
</journey>
```

`journeys/m1-two-pane.xml`:

```xml
<journey name="M1 two-pane: list and chat side by side on a tablet">
   <description>
      On an expanded window the conversation list and the chat share the screen,
      the open conversation is highlighted in the list, and the chat pane has no
      back arrow because there is nothing to go back to.
   </description>
   <actions>
     <action>Open the "buddy" app</action>
     <action>Tap the "new chat" button</action>
     <action>Type "Reply with exactly: Hello" into the message field and tap send</action>
     <action>Wait until a reply bubble appears containing "Hello"</action>
     <action>Verify the conversation list and the chat are both visible at the same time</action>
     <action>Verify the open conversation's row in the list is highlighted</action>
     <action>Verify no back arrow is displayed in the chat pane's top bar</action>
   </actions>
</journey>
```

- [ ] **Step 3: Run the five phone journeys**

```bash
android emulator start Pixel_10
./gradlew :app:installDebug
```

Then evaluate each of `journeys/m1-first-chat.xml`, `m1-resume.xml`, `m1-delete.xml`, `m1-retry.xml`, `m1-model-switch.xml` against the running emulator, plus `journeys/m0-scaffold.xml`, which now fails, because `MainActivity` no longer shows the text "Conversation". Rewrite that file's last action to verify the conversation list is displayed instead; it is the M0 gate and must stay green.

Expected: all six pass. Fix the app, not the journey, for anything else that fails.

- [ ] **Step 4: Run the two-pane journey on the tablet**

```bash
android emulator start <the tablet AVD name from Step 1>
./gradlew :app:installDebug
```

Evaluate `journeys/m1-two-pane.xml`. Expected: pass.

- [ ] **Step 5: Correct the two lines 005 owes**

`specs/005-conversation-shell.md:15` currently reads "Two changes to 004's contract, which that spec is amended to carry." 004 defers them to 005 instead, matching how the rest of its deferral table works. Replace with:

```markdown
Two additions to 004's contract, which that spec defers to this one.
```

`specs/005-conversation-shell.md:75` names `hilt-navigation-compose`. Replace that dependency name with `hilt-lifecycle-viewmodel-compose`, and add the reason after the sentence listing the feature's dependencies:

```markdown
`hilt-lifecycle-viewmodel-compose` rather than `hilt-navigation-compose`: the latter declares `navigation-compose` as a compile dependency, which would put Navigation 2 on the classpath. The `hiltViewModel` overload taking a `creationCallback` lives in the former either way.
```

- [ ] **Step 6: Record the two new libraries in 001**

`specs/001-tech-stack.md` is the canonical record, and 001 itself says a substitution requires updating it first. In the **UI** table, extend the Navigation row's Choice cell to name the adaptive artifact and its version, and add its pre-release status to the Rationale:

```markdown
Scenes give native adaptive two-pane (conversation list + chat on tablets/foldables) via `androidx.compose.material3.adaptive:adaptive-navigation3` **1.3.0-rc01**. That artifact has no stable release (published versions run 1.0.0-alpha01…03 then 1.3.0-alpha01…rc01), so the adaptive line is the one pre-release dependency in the stack; revisit at 1.3.0 stable.
```

In the **Architecture & DI** table, extend the DI row's Rationale:

```markdown
Compose integration comes from `androidx.hilt:hilt-lifecycle-viewmodel-compose`, **not** `hilt-navigation-compose`, which declares `androidx.navigation:navigation-compose` as a compile dependency and would put the prohibited Navigation 2 on the classpath. The `hiltViewModel` overload taking a `creationCallback` (how a Nav 3 key reaches an assisted-injected ViewModel) lives in the former.
```

- [ ] **Step 7: Bring the roadmap's status up to date**

`docs/roadmap.md`, Status table: 002, 003, 004 and 005 are in, and 006 is not:

```markdown
| Milestone | State |
|---|---|
| M0 scaffold | done |
| M1 chat MVP (002–006) | in progress: 002, 003, 004, 005 done; 006 next |
| M2 tool loop + memory (007–009) | not started |
| M3 reminders (010) | not started |
| M4 polish | not started |
```

- [ ] **Step 8: Review the prose**

Run the `prose-review` skill over the four edited Markdown files. The house style forbids em dashes and history ("what happened once"), and these edits are easy to write in both.

- [ ] **Step 9: Final checkpoint, the M1 exit gate**

```bash
./gradlew spotlessApply
./gradlew :shared:testing:testAndroidHostTest :shared:testAndroidHostTest \
          :core:ui:testDebugUnitTest :core:ui:validateDebugScreenshotTest \
          :feature:conversation:testDebugUnitTest :feature:conversation:validateDebugScreenshotTest \
          :app:assembleDebug spotlessCheck
```

Expected: `BUILD SUCCESSFUL`, and all seven journeys green across the two emulators.

Report to the user:

1. The seven journey results and which emulator each ran on.
2. The design deviations listed at the top of this plan, plus any off-grid values snapped during implementation that are not already listed there.
3. That `:app:assembleRelease` fails with a missing `ApiKeyProvider` binding, with the exact error, and that 006 fixes it.
4. The four documents edited and what changed in each.
5. The `adaptive` skill's five steps and this plan's answer to each, from the Adaptive posture table, so the two skipped ones are a decision on the record rather than an omission.
6. That nothing is committed: the working tree is ready for the user to review and commit.

---

## What this plan does not build

Carried forward exactly as 005's deferral table has it, so nothing here is a silent omission:

| Piece | Owner |
|---|---|
| Search, its no-results state (2c, 2d) and the list top bar's search action | M4 |
| Unread indicator on list rows | M4, needs a `lastReadAt` column, so a schema bump and migration |
| Suggested prompt chips on the new-chat empty state (3a) | M4 |
| Model picker blurbs (3e) | M4 |
| The list top bar's settings action | 007 |
| Tool-call chips (3c, 3d) | 008, 009, 010 |
| The real `ApiKeyProvider`, and a release build that assembles | 006 |
