# Conversation Storage (`:shared`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Room persistence for conversations and messages in `:shared` commonMain, plus the `ConversationRepository` that owns a chat turn end to end — append the user message, stream the reply through 003's `ChatEngine`, persist the result — with the turn running in an application-scoped coroutine so it outlives the screen that started it.

**Architecture:** Room in KMP mode is the single source of truth for chat history; the one exception is the in-flight assistant reply, which lives in memory as `TurnState` until the stream ends. `ChatbotDatabase` holds two tables (`conversations`, `messages`) with a cascade from parent to child; message content is a JSON column so 008 can add tool blocks without a schema change. `DefaultConversationRepository` keeps a `MutableStateFlow<Map<Long, Turn>>` of live turns, each turn a `MutableStateFlow<TurnState>` plus the `Job` streaming into it, launched in an injected `externalScope`. Entities, DAOs, converters and the repository implementation are `internal`; the module's public surface is the repository interface, the domain models, `ChatbotDatabase` as an opaque handle, and three factories. Spec: `specs/004-conversation-storage.md`. Evidence behind the spec's build and test decisions: `docs/superpowers/plans/2026-07-26-m1-004-storage-spike-findings.md`.

**Tech Stack:** Kotlin 2.4.10, KMP (`androidTarget()` only), Room 2.8.4 (KMP mode, `androidx.room` Gradle plugin, KSP 2.3.10), `androidx.sqlite` 2.6.2 (`sqlite-bundled` + `BundledSQLiteDriver` in production, `AndroidSQLiteDriver` in host tests), kotlinx.serialization 1.11.0, kotlinx.coroutines 1.11.0 (+ `-test`), `kotlin.time.Clock`/`Instant`, kotlin-test, JUnit4 + Robolectric 4.16.1. Tests run via `:shared:testAndroidHostTest`.

## Global Constraints

- **Never commit or push** (CLAUDE.md — this overrides the superpowers commit steps). Each task ends with tests green and changes left in the working tree; report what's ready.
- Tick each step's checkbox (`- [ ]` → `- [x]`) in this plan file as you finish and verify it.
- Execute the plan on the current git branch (`m1/004-database`) — no worktree, no new branch.
- Module: `:shared`. Package roots: `com.shayanaryan.chatbot.shared.conversation` (domain + repository), `…shared.conversation.local` (entities + DAOs), `…shared.database` (database, converters, codec).
- **commonMain stays pure** — no `android.*` import ever (architecture skill). The only platform-specific piece is the androidMain database-builder factory.
- **Hilt never crosses into `:shared`** — plain constructor injection only; `:app` constructs and provides.
- **Repositories are the sole entry to the data layer.** Entities and DAOs are `internal` and never leave `:shared`.
- **No user-visible text in `:shared`.** `TurnState.Failed` carries a typed `ChatError`; feature ViewModels map it to string resources.
- **No mocking libraries** (no MockK). Fakes and real objects only — a real in-memory Room database, and a fake `ChatEngine`.
- **`const val` names use `SCREAMING_SNAKE_CASE`**; non-const `val`s are `camelCase` (CLAUDE.md).
- **No trailing (end-of-line) comments** — comments go on their own line above the code (CLAUDE.md).
- **No volatile references in code comments or KDoc.** Shipped-code comments must not name spec numbers, task numbers, roadmap items, or future/deferred work ("009 adds…", "a future iOS target"). Comment on what the code does and why it is shaped that way in timeless terms. Sequencing lives in the spec's deferrals table and this plan's prose, never in source.
- **KDoc on every interface and contract** (`ConversationRepository`, `TurnState`, the public factories, and any non-obvious function) (CLAUDE.md).
- **Test function names use backtick spaced form** (`` fun `does the thing`() ``).
- "Bro" is display-name only — never in identifiers. Nothing here is user-facing, so the product name appears nowhere.
- Timestamps: epoch millis (`Long`) in the database, `kotlin.time.Instant` in domain models, via an injected `kotlin.time.Clock`. No opt-in annotation is needed on Kotlin 2.4.10.
- `MAX_TITLE_LENGTH` is **60**. Messages order by `id`; conversations order by `updatedAt DESC`. The history query filters `status = 'Complete'`.
- Schema version is **1**; `exportSchema` is on and `shared/schemas/…/1.json` is committed. Destructive fallback is never enabled.
- Test command: `./gradlew :shared:testAndroidHostTest`. Gradle's console does not show per-test detail — read `shared/build/test-results/testAndroidHostTest/TEST-*.xml` when you need it.
- Formatting is a gate: run `./gradlew :shared:spotlessApply` (and `:app:spotlessApply` when you touch `:app`) before finishing each task; `spotlessCheck` must pass. Max line length is 100.
- TDD: red → green → refactor for every step.

## Deviations from the spec (apply as written; spec text is corrected in Task 9)

1. **`api(libs.androidx.room.runtime)`, not `implementation`.** The spec's build snippet says `implementation`. `:app` holds `ChatbotDatabase` as a Hilt `@Singleton`, so `androidx.room.RoomDatabase` — its supertype — must be on `:app`'s compile classpath, or the provider method will not compile. Room's `androidApiElements` variant already exposes `androidx.sqlite:sqlite` and `sqlite-framework` transitively, so `api` on room-runtime alone is enough; `sqlite-bundled` stays `implementation` because `BundledSQLiteDriver` only ever appears as a default-argument expression evaluated inside `:shared`.
2. **`cancel` does not write the `Cancelled` row itself.** The spec says "`cancel` writes the `Cancelled` row itself, then sets `Idle` and drops the entry". The observable outcome is kept exactly — after `cancel` returns, the `Cancelled` row exists, the state is `Idle`, and the entry is gone — but the write happens inside the turn coroutine, in a `withContext(NonCancellable)` block reached when `collect` throws `CancellationException`. `cancel` is then just `job.cancelAndJoin()`. One writer means no window in which a turn completes normally between `cancel`'s liveness check and its own insert, which would otherwise persist two assistant rows for one turn. It also means the partial text comes from the coroutine that produced it rather than being re-read off `TurnState`.
3. **The one-turn guard tests the job *and* the state.** The spec says the guard tests the job rather than the entry's presence, which this keeps — a turn whose job died unexpectedly can never block a later send. It additionally requires `state.value is TurnState.Streaming`, because the turn coroutine sets `Idle` and only then removes its map entry; without the state test, a `send` landing in that gap would throw for a turn that is already finished.
4. **`send` checks the conversation exists.** Task 6's repository code as written skips it, while `FakeConversationRepository` enforces it with `require` and `ConversationRepository`'s KDoc documents `IllegalArgumentException` for it. Left as written, the fake is stricter than the real implementation — 005's ViewModel tests would go green against behaviour production does not have, where a screen left open on a deleted conversation gets a raw foreign-key constraint out of the insert instead. `send` now calls `requireExists(conversationId)` before `requireIdle`, under the same lock, covered by two tests in `ConversationRepositoryTest`.
5. **`delete` drops only the turn it cancelled.** Task 6's `delete` removes the map entry unconditionally, while `clearTurn` is identity-checked. A `send` landing between `delete`'s `cancelAndJoin` and that removal installs a fresh turn which `delete` then drops, leaving the job running with nothing tracking it — `getTurnFlow` reports `Idle` and `cancel` cannot reach it. `delete` now captures the turn up front and routes its removal through `clearTurn`, calling it only when there was a turn to drop. Holding the mutex across the whole of `delete` — the obvious alternative — deadlocks: the cancellation path's `clearTurn` takes that same mutex, so the join would never return. The remaining send-during-delete window is inherent without a lock spanning DAO I/O, and the orphaned turn heals itself, since `runTurn` finds no conversation and goes idle.

Two spec *additions* (no behaviour is being changed, the spec is simply silent) also land in Task 9: an unknown stored `ClaudeModel` name decodes to `ClaudeModel.Default` rather than throwing, and the build gains `-Xexpect-actual-classes`.

## File Structure

All paths relative to the repo root.

```
shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/
  chat/ChatRequest.kt                     MODIFIED: @Serializable on ContentBlock
  database/
    StorageJson.kt                        internal storageJson — the on-disk codec
    ChatbotConverters.kt                  internal Room type converters
    ChatbotDatabase.kt                    @Database + expect object constructor
    ChatbotDatabaseFactory.kt             public createChatbotDatabase(builder, driver, queryContext)
  conversation/
    Conversation.kt                       Conversation, Message, MessageStatus (public domain)
    TurnState.kt                          TurnState sealed interface
    ConversationRepository.kt             public interface + MAX_TITLE_LENGTH
    DefaultConversationRepository.kt       internal implementation
    ConversationRepositoryFactory.kt      public createConversationRepository(...)
    local/
      ConversationEntity.kt               internal entity
      MessageEntity.kt                    internal entity
      ConversationDao.kt                  internal DAO — conversation reads + cross-table writes
      MessageDao.kt                       internal DAO — message reads + row delete
      Mappers.kt                          internal entity → domain / entity → ChatMessage

shared/src/androidMain/kotlin/com/shayanaryan/chatbot/shared/database/
  ChatbotDatabaseBuilder.android.kt       chatbotDatabaseBuilder(context) — the only platform piece

shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/
  FakeClock.kt                            fixed-instant kotlin.time.Clock
  chat/FakeManualChatEngine.kt                channel-backed engine a test drives event by event
  chat/FakeManualChatEngineTest.kt
  database/StorageJsonTest.kt
  database/ChatbotConvertersTest.kt
  conversation/FakeConversationRepository.kt
  conversation/FakeConversationRepositoryTest.kt

shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/
  database/TestDatabase.kt                in-memory database + runDatabaseTest helper
  database/ChatbotDatabaseTest.kt         smoke test: the Room/KSP/Robolectric stack works
  conversation/local/ConversationDaoTest.kt
  conversation/local/MessageDaoTest.kt
  conversation/ConversationRepositoryTest.kt        flows, setModel, delete
  conversation/ConversationTurnTest.kt              send + turn lifecycle
  conversation/ConversationCancelRetryTest.kt       cancel, retry, delete during a turn

shared/src/androidHostTest/resources/robolectric.properties   sdk=36

shared/schemas/com.shayanaryan.chatbot.shared.database.ChatbotDatabase/1.json   exported baseline

app/src/main/kotlin/com/shayanaryan/chatbot/di/
  ApplicationScope.kt                     @Qualifier for the app-scoped CoroutineScope
  CoroutinesModule.kt                     provides that scope
  DatabaseModule.kt                       provides ChatbotDatabase + ConversationRepository
```

Also modified: `gradle/libs.versions.toml`, `shared/build.gradle.kts`, `app/build.gradle.kts`, `specs/004-conversation-storage.md`, `docs/roadmap.md`.

---

### Task 1: Build wiring, the persistence codec, and a serializable `ContentBlock`

Everything the storage layer needs before a single Room type exists: the catalog entries, the `:shared` build script, the Robolectric host-test plumbing, and the codec that turns a `List<ContentBlock>` into the text stored in the `content` column.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`
- Create: `shared/src/androidHostTest/resources/robolectric.properties`
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/ChatRequest.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/StorageJson.kt`
- Test: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/database/StorageJsonTest.kt`

**Interfaces:**
- Consumes: `com.shayanaryan.chatbot.shared.chat.ContentBlock` (existing sealed interface with one `Text(text: String)` subtype).
- Produces: `internal val storageJson: Json` in `com.shayanaryan.chatbot.shared.database`; `ContentBlock` and `ContentBlock.Text` become `@Serializable`, serializing as `{"type":"text","text":"…"}`.

- [x] **Step 1: Write the failing codec test**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/database/StorageJsonTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageJsonTest {
    @Test
    fun `encodes a content list with a type discriminator`() {
        val encoded = storageJson.encodeToString(listOf<ContentBlock>(ContentBlock.Text("hi")))

        assertEquals("""[{"type":"text","text":"hi"}]""", encoded)
    }

    @Test
    fun `round trips a multi block content list`() {
        val content = listOf<ContentBlock>(ContentBlock.Text("one"), ContentBlock.Text("two"))

        val decoded = storageJson.decodeFromString<List<ContentBlock>>(
            storageJson.encodeToString(content),
        )

        assertEquals(content, decoded)
    }

    @Test
    fun `decodes a stored block that carries an unknown field`() {
        val stored = """[{"type":"text","text":"hi","tokens":3}]"""

        val decoded = storageJson.decodeFromString<List<ContentBlock>>(stored)

        assertEquals(listOf<ContentBlock>(ContentBlock.Text("hi")), decoded)
    }
}
```

- [x] **Step 2: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*StorageJsonTest*'`
Expected: compilation failure — `Unresolved reference: storageJson`.

- [x] **Step 3: Make `ContentBlock` serializable**

Edit `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/chat/ChatRequest.kt`. Add the two imports at the top of the import block:

```kotlin
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
```

and replace the `ContentBlock` declaration with:

```kotlin
/**
 * A single piece of a message. Modelled as a list on [ChatMessage] rather than a bare string so
 * additional block types can be added without reshaping the message type.
 *
 * `@Serializable` because blocks are stored as JSON text; the wire format is a separate DTO, so
 * the two are free to differ.
 */
@Serializable
sealed interface ContentBlock {
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
    ) : ContentBlock
}
```

- [x] **Step 4: Write the codec**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/StorageJson.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import kotlinx.serialization.json.Json

/**
 * Codec for values stored as text in the database. Deliberately separate from the codec used on
 * the wire: an on-disk format has no migration story for a settings change made for HTTP reasons,
 * and a stored value that stops decoding must fail loudly rather than be coerced to a default.
 */
internal val storageJson =
    Json {
        // A row written by a newer version still reads on an older one.
        ignoreUnknownKeys = true
        // Pins the stored shape against a change of library default.
        classDiscriminator = "type"
    }
```

- [x] **Step 5: Run the codec test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*StorageJsonTest*'`
Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [x] **Step 6: Add the catalog entries**

In `gradle/libs.versions.toml`, add under `[versions]` (next to `room`):

```toml
sqlite = "2.6.2"
```

under `[libraries]` (next to the other `androidx-room-*` lines):

```toml
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqlite" }
```

and under `[plugins]`:

```toml
androidx-room = { id = "androidx.room", version.ref = "room" }
```

`2.6.2` is the version `room-runtime:2.8.4` itself declares for `androidx.sqlite:sqlite` and `sqlite-framework`. `sqlite-bundled` 2.7.0 exists but would drag the sqlite core above what Room declares.

- [x] **Step 7: Rewrite `shared/build.gradle.kts`**

Replace the whole file with:

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        // Silences the Beta warning on the database constructor's expect/actual pair.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "com.shayanaryan.chatbot.shared"
        compileSdk = 37
        minSdk = 31
        // enables commonTest on JVM; returnDefaultValues stops android.jar stubs
        // (e.g. android.util.Log, touched by OkHttp's platform detection) from
        // throwing "not mocked" on the host test classpath.
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            // `api`, not `implementation`: consumers hold the database as a singleton, so
            // androidx.room.RoomDatabase — its supertype — must be on their compile classpath.
            api(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
        // No typed accessor exists for this source set.
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

dependencies {
    // KSP configurations are created per target after the kotlin {} block evaluates, so there is
    // no typed accessor for them and the catch-all `ksp(…)` is deprecated in KSP 2 —
    // add("ksp<Target>", …) is the only available spelling. Each further target adds its own line.
    add("kspAndroid", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
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

- [x] **Step 8: Pin the Robolectric SDK**

Create `shared/src/androidHostTest/resources/robolectric.properties`:

```
sdk=36
```

Robolectric 4.16.1 tops out at SDK 36 and would otherwise default to `targetSdk` 37, which it cannot run. Note the path: `androidHostTest/resources`, not the `src/test/resources` the non-KMP modules use.

- [x] **Step 9: Verify the whole module still builds and every existing test still passes**

Run: `./gradlew :shared:testAndroidHostTest`
Expected: `BUILD SUCCESSFUL`. The 003 chat suite plus the three new codec tests are green. The Room plugin and KSP are now applied but have nothing to process yet.

- [x] **Step 10: Format and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Leave changes in the working tree. Report: the build wiring is in, `ContentBlock` is `@Serializable`, the storage codec round-trips, and the existing suite is unaffected.

---

### Task 2: Entities, converters, and the database

The Room stack end to end, proven by one smoke test. This is the task that either shows KSP, the Room Gradle plugin, `@ConstructedBy`, schema export and Robolectric all working together in this module — or shows exactly which one does not.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/Conversation.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationEntity.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageEntity.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDao.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageDao.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotConverters.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabase.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseFactory.kt`
- Create: `shared/src/androidMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseBuilder.android.kt`
- Create: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/database/TestDatabase.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseTest.kt`
- Test: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotConvertersTest.kt`

**Interfaces:**
- Consumes: `storageJson` (Task 1); `ContentBlock`, `Role` (`…shared.chat`); `ClaudeModel` (`…shared.model`).
- Produces: `enum class MessageStatus { Complete, Failed, Cancelled }` in `…shared.conversation`; `internal data class ConversationEntity(id, title, model, createdAt, updatedAt)` and `internal data class MessageEntity(id, conversationId, role, content, status, createdAt)` in `…shared.conversation.local`; `internal abstract class ConversationDao` and `internal interface MessageDao`; `abstract class ChatbotDatabase : RoomDatabase()` with `internal abstract fun conversationDao()` / `messageDao()`; `fun createChatbotDatabase(builder, driver, queryContext): ChatbotDatabase`; `fun chatbotDatabaseBuilder(context: Context): RoomDatabase.Builder<ChatbotDatabase>` (androidMain); test helpers `TestScope.testDatabase()` and `runDatabaseTest { database -> … }`.

- [x] **Step 1: Write the failing converter test**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotConvertersTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChatbotConvertersTest {
    private val converters = ChatbotConverters()

    @Test
    fun `round trips every model`() {
        ClaudeModel.entries.forEach { model ->
            assertEquals(model, converters.toModel(converters.fromModel(model)))
        }
    }

    @Test
    fun `falls back to the default model for a name that no longer exists`() {
        assertEquals(ClaudeModel.Default, converters.toModel("Sonnet4"))
    }

    @Test
    fun `round trips every role`() {
        Role.entries.forEach { role ->
            assertEquals(role, converters.toRole(converters.fromRole(role)))
        }
    }

    @Test
    fun `round trips every status`() {
        MessageStatus.entries.forEach { status ->
            assertEquals(status, converters.toStatus(converters.fromStatus(status)))
        }
    }

    @Test
    fun `rejects a status the app never wrote`() {
        assertFailsWith<IllegalArgumentException> { converters.toStatus("Pending") }
    }

    @Test
    fun `round trips a multi block content list`() {
        val content = listOf<ContentBlock>(ContentBlock.Text("one"), ContentBlock.Text("two"))

        assertEquals(content, converters.toContent(converters.fromContent(content)))
    }

    @Test
    fun `stores the status name so a query can match it as a literal`() {
        assertEquals("Complete", converters.fromStatus(MessageStatus.Complete))
    }
}
```

- [x] **Step 2: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ChatbotConvertersTest*'`
Expected: compilation failure — `Unresolved reference: ChatbotConverters`, `Unresolved reference: MessageStatus`.

- [x] **Step 3: Write the domain-side status enum**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/Conversation.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlin.time.Instant

data class Conversation(
    val id: Long,
    val title: String,
    val model: ClaudeModel,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class Message(
    val id: Long,
    val conversationId: Long,
    val role: Role,
    val content: List<ContentBlock>,
    val status: MessageStatus,
    val createdAt: Instant,
)

/**
 * How a message ended. Only [Complete] messages are sent back to the model — a turn that failed
 * or was cancelled is structurally incapable of reaching the API, which is what the history
 * query's status filter relies on.
 */
enum class MessageStatus { Complete, Failed, Cancelled }
```

- [x] **Step 4: Write the converters**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotConverters.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import androidx.room.TypeConverter
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.model.ClaudeModel

/**
 * Column codecs. Enums are stored as their constant name, which is what lets a query compare
 * `status` against a string literal; message content is stored as JSON text.
 */
internal class ChatbotConverters {
    @TypeConverter
    fun fromModel(model: ClaudeModel): String = model.name

    /**
     * Falls back rather than throwing: a model retired from the picker would otherwise make every
     * conversation row naming it unreadable.
     */
    @TypeConverter
    fun toModel(value: String): ClaudeModel =
        ClaudeModel.entries.firstOrNull { it.name == value } ?: ClaudeModel.Default

    @TypeConverter
    fun fromRole(role: Role): String = role.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromStatus(status: MessageStatus): String = status.name

    /**
     * Throws on an unrecognised value, unlike the model converter. The set of statuses is
     * closed and written only by this app, so anything else is corruption, not an old row.
     */
    @TypeConverter
    fun toStatus(value: String): MessageStatus = MessageStatus.valueOf(value)

    @TypeConverter
    fun fromContent(content: List<ContentBlock>): String = storageJson.encodeToString(content)

    @TypeConverter
    fun toContent(value: String): List<ContentBlock> = storageJson.decodeFromString(value)
}
```

- [x] **Step 5: Run the converter test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ChatbotConvertersTest*'`
Expected: `BUILD SUCCESSFUL`, 7 tests passing.

`MessageStatus.valueOf` throws `IllegalArgumentException` on an unknown name, which is what the rejection test asserts.

- [x] **Step 6: Write the failing database smoke test**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ChatbotDatabaseTest {
    @Test
    fun `stores and reads back a conversation with a message`() =
        runDatabaseTest { database ->
            val conversationId =
                database.conversationDao().insert(
                    ConversationEntity(
                        title = "plan a trip",
                        model = ClaudeModel.Opus,
                        createdAt = 10L,
                        updatedAt = 10L,
                    ),
                )
            database.conversationDao().insertMessage(
                MessageEntity(
                    conversationId = conversationId,
                    role = Role.User,
                    content = listOf(ContentBlock.Text("one"), ContentBlock.Text("two")),
                    status = MessageStatus.Complete,
                    createdAt = 10L,
                ),
            )

            val conversation = database.conversationDao().findById(conversationId)
            val messages = database.messageDao().completeForConversation(conversationId)

            assertEquals(ClaudeModel.Opus, conversation?.model)
            assertEquals("plan a trip", conversation?.title)
            assertEquals(
                listOf<ContentBlock>(ContentBlock.Text("one"), ContentBlock.Text("two")),
                messages.single().content,
            )
            assertEquals(Role.User, messages.single().role)
        }
}
```

- [x] **Step 7: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ChatbotDatabaseTest*'`
Expected: compilation failure — `Unresolved reference: runDatabaseTest`, `ConversationEntity`, `MessageEntity`.

- [x] **Step 8: Write the entities**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationEntity.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.shayanaryan.chatbot.shared.model.ClaudeModel

@Entity(tableName = "conversations")
internal data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val model: ClaudeModel,
    val createdAt: Long,
    val updatedAt: Long,
)
```

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageEntity.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus

/**
 * `content` is a JSON column rather than a text column: a message is a list of blocks, and block
 * kinds are added without a schema change. Message content is never queried by SQL.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId")],
)
internal data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: Role,
    val content: List<ContentBlock>,
    val status: MessageStatus,
    val createdAt: Long,
)
```

- [x] **Step 9: Write the DAOs**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDao.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/**
 * Conversation reads, plus every write that spans both tables. A transaction can only call
 * methods on its own DAO, which is why the message insert lives here as well as on [MessageDao]'s
 * side of the split.
 */
@Dao
internal abstract class ConversationDao {
    @Insert
    abstract suspend fun insert(conversation: ConversationEntity): Long

    @Insert
    abstract suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    abstract fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    abstract suspend fun findById(id: Long): ConversationEntity?

    @Query("UPDATE conversations SET model = :model WHERE id = :id")
    abstract suspend fun setModel(id: Long, model: ClaudeModel)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun touch(id: Long, updatedAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    abstract suspend fun delete(id: Long)

    /**
     * Creates a conversation and its first message together, so an abandoned empty chat can never
     * appear in the list.
     *
     * @return the new conversation's id.
     */
    @Transaction
    open suspend fun createWithFirstMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
    ): Long {
        val conversationId = insert(conversation)
        insertMessage(message.copy(conversationId = conversationId))
        return conversationId
    }

    /**
     * Appends a message and bumps its conversation's `updatedAt`, making the list's ordering rule
     * structural rather than something every caller has to remember.
     *
     * @return the new message's id.
     */
    @Transaction
    open suspend fun appendMessage(message: MessageEntity, updatedAt: Long): Long {
        val id = insertMessage(message)
        touch(message.conversationId, updatedAt)
        return id
    }
}
```

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageDao.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Message reads. Ordering is by `id`, not `createdAt`: autoincrement is monotonic, so it is
 * insertion order without ties. Timestamps are for display.
 */
@Dao
internal interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEntity>>

    /** The history sent to the model: everything a turn actually produced, nothing that failed. */
    @Query(
        "SELECT * FROM messages WHERE conversationId = :conversationId " +
            "AND status = 'Complete' ORDER BY id",
    )
    suspend fun completeForConversation(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY id DESC LIMIT 1")
    suspend fun lastForConversation(conversationId: Long): MessageEntity?

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [x] **Step 10: Write the database and its constructor**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabase.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.shayanaryan.chatbot.shared.conversation.local.ConversationDao
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageDao
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity

/**
 * The app's only database. Callers outside this module hold it as an opaque handle and hand it to
 * a repository factory; the DAOs it exposes are module-internal.
 */
@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ChatbotConverters::class)
@ConstructedBy(ChatbotDatabaseConstructor::class)
abstract class ChatbotDatabase : RoomDatabase() {
    internal abstract fun conversationDao(): ConversationDao

    internal abstract fun messageDao(): MessageDao
}

/**
 * Instantiates the generated database implementation. The `actual` is produced by Room's KSP
 * processor, which is why no matching declaration appears in source.
 */
@Suppress("KotlinNoActualForExpect")
expect object ChatbotDatabaseConstructor : RoomDatabaseConstructor<ChatbotDatabase> {
    override fun initialize(): ChatbotDatabase
}
```

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseFactory.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/**
 * Finishes a platform builder into a usable database.
 *
 * @param builder supplied per platform, since only the file path differs.
 * @param driver a parameter rather than a hardcoded call so tests can swap it; the bundled driver
 *   is compiled from source, so behaviour is identical across OS versions.
 * @param queryContext must contain a dispatcher — Room rejects a context without one.
 */
fun createChatbotDatabase(
    builder: RoomDatabase.Builder<ChatbotDatabase>,
    driver: SQLiteDriver = BundledSQLiteDriver(),
    queryContext: CoroutineContext = Dispatchers.IO,
): ChatbotDatabase =
    builder
        .setDriver(driver)
        .setQueryCoroutineContext(queryContext)
        .build()
```

- [x] **Step 11: Write the Android builder**

Create `shared/src/androidMain/kotlin/com/shayanaryan/chatbot/shared/database/ChatbotDatabaseBuilder.android.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private const val DATABASE_NAME = "chatbot.db"

/**
 * Builds the platform database builder. The file path is the only platform-specific part of the
 * storage layer; the driver and query context are chosen by [createChatbotDatabase].
 */
fun chatbotDatabaseBuilder(context: Context): RoomDatabase.Builder<ChatbotDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<ChatbotDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_NAME).absolutePath,
    )
}
```

- [x] **Step 12: Write the test database helper**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/database/TestDatabase.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.sqlite.driver.AndroidSQLiteDriver
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * An empty in-memory database on the test scheduler.
 *
 * The Android driver, not the bundled one: `sqlite-bundled` ships host natives only in its `jvm`
 * variant, and an Android consumer resolves the `android` one, whose `.so` payload never reaches
 * the unit-test classpath. Robolectric supplies the host SQLite implementation instead.
 */
internal fun TestScope.testDatabase(): ChatbotDatabase =
    createChatbotDatabase(
        builder =
            Room.inMemoryDatabaseBuilder<ChatbotDatabase>(
                ApplicationProvider.getApplicationContext(),
            ),
        driver = AndroidSQLiteDriver(),
        queryContext = StandardTestDispatcher(testScheduler),
    )

/**
 * Runs [body] against a database of its own.
 *
 * The database is deliberately not closed: each test builds a fresh in-memory one that is
 * released with the test JVM, and closing inside the test body would race any collector still
 * running on the scope that outlives it.
 */
internal fun runDatabaseTest(body: suspend TestScope.(ChatbotDatabase) -> Unit): TestResult =
    runTest { body(testDatabase()) }
```

- [x] **Step 13: Run the smoke test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ChatbotDatabaseTest*'`
Expected: `BUILD SUCCESSFUL`, 1 test passing.

If KSP reports `Cannot figure out how to save this field into database`, a converter is missing from `ChatbotConverters`. If Kotlin complains that `ChatbotDatabase` exposes an internal type through `@Database` or `@TypeConverters`, make `ChatbotConverters` and the two entities public with an internal constructor is *not* the fix — instead record it in your task report and mark just the offending class public, since annotation arguments are not normally subject to the exposed-visibility check.

- [x] **Step 14: Verify the schema baseline was exported**

Run: `./gradlew :shared:clean :shared:compileAndroidMain && ls shared/schemas/*/`
Expected: `1.json` under `shared/schemas/com.shayanaryan.chatbot.shared.database.ChatbotDatabase/`.

`copyRoomSchemas` reports `NO-SOURCE` when `compileAndroidMain` is already up to date, so the clean is what makes this check meaningful. Confirm the file lists both tables, the `CASCADE` foreign key, and `index_messages_conversationId`. It is a committed artefact — do not add it to `.gitignore`.

- [x] **Step 15: Run the full module suite, format, and report**

Run: `./gradlew :shared:testAndroidHostTest && ./gradlew :shared:spotlessApply && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: Room, KSP, `@ConstructedBy`, schema export and Robolectric all working; the schema baseline path.

---

### Task 3: The DAO query surface

Every query the repository will lean on, each with a test. The `status = 'Complete'` literal is coupled to an enum constant name with nothing in the compiler to check it, so that coupling gets its own test.

**Files:**
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDaoTest.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageDaoTest.kt`

**Interfaces:**
- Consumes: `ConversationDao`, `MessageDao`, `ConversationEntity`, `MessageEntity` (Task 2); `runDatabaseTest` (Task 2).
- Produces: no new production symbols — this task proves the ones Task 2 declared.

- [x] **Step 1: Write the conversation DAO tests**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/ConversationDaoTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class ConversationDaoTest {
    private suspend fun ChatbotDatabase.newConversation(
        title: String,
        updatedAt: Long,
    ): Long =
        conversationDao().insert(
            ConversationEntity(
                title = title,
                model = ClaudeModel.Default,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            ),
        )

    private fun message(conversationId: Long, text: String, createdAt: Long) =
        MessageEntity(
            conversationId = conversationId,
            role = Role.User,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = createdAt,
        )

    @Test
    fun `orders conversations by most recently updated`() =
        runDatabaseTest { database ->
            val older = database.newConversation("older", updatedAt = 10L)
            val newer = database.newConversation("newer", updatedAt = 20L)

            val ids = database.conversationDao().observeAll().first().map { it.id }

            assertEquals(listOf(newer, older), ids)
        }

    @Test
    fun `touching a conversation moves it to the head of the list`() =
        runDatabaseTest { database ->
            val older = database.newConversation("older", updatedAt = 10L)
            database.newConversation("newer", updatedAt = 20L)

            database.conversationDao().touch(older, updatedAt = 30L)

            assertEquals(older, database.conversationDao().observeAll().first().first().id)
        }

    @Test
    fun `creating with a first message writes both rows`() =
        runDatabaseTest { database ->
            val conversationId =
                database.conversationDao().createWithFirstMessage(
                    conversation =
                        ConversationEntity(
                            title = "plan a trip",
                            model = ClaudeModel.Haiku,
                            createdAt = 5L,
                            updatedAt = 5L,
                        ),
                    message = message(conversationId = 0, text = "plan a trip", createdAt = 5L),
                )

            val messages = database.messageDao().completeForConversation(conversationId)

            assertEquals(ClaudeModel.Haiku, database.conversationDao().findById(conversationId)?.model)
            assertEquals(conversationId, messages.single().conversationId)
        }

    @Test
    fun `appending a message bumps the conversation`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation("chat", updatedAt = 10L)

            database.conversationDao().appendMessage(
                message = message(conversationId, "hello", createdAt = 40L),
                updatedAt = 40L,
            )

            assertEquals(40L, database.conversationDao().findById(conversationId)?.updatedAt)
        }

    @Test
    fun `changing the model rewrites only that conversation`() =
        runDatabaseTest { database ->
            val target = database.newConversation("target", updatedAt = 10L)
            val other = database.newConversation("other", updatedAt = 20L)

            database.conversationDao().setModel(target, ClaudeModel.Opus)

            assertEquals(ClaudeModel.Opus, database.conversationDao().findById(target)?.model)
            assertEquals(ClaudeModel.Default, database.conversationDao().findById(other)?.model)
        }

    @Test
    fun `deleting a conversation cascades to its messages`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation("chat", updatedAt = 10L)
            database.conversationDao().insertMessage(message(conversationId, "hello", 10L))
            database.conversationDao().insertMessage(message(conversationId, "again", 11L))

            database.conversationDao().delete(conversationId)

            assertNull(database.conversationDao().findById(conversationId))
            assertEquals(
                emptyList(),
                database.messageDao().observeForConversation(conversationId).first(),
            )
        }
}
```

- [x] **Step 2: Write the message DAO tests**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/local/MessageDaoTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.MessageStatus
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MessageDaoTest {
    private suspend fun ChatbotDatabase.newConversation(): Long =
        conversationDao().insert(
            ConversationEntity(
                title = "chat",
                model = ClaudeModel.Default,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

    private suspend fun ChatbotDatabase.append(
        conversationId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
        createdAt: Long,
    ): Long =
        conversationDao().insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = createdAt,
            ),
        )

    @Test
    fun `orders messages by insertion, not by timestamp`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "first", MessageStatus.Complete, 100L)
            database.append(conversationId, Role.Assistant, "second", MessageStatus.Complete, 100L)
            database.append(conversationId, Role.User, "third", MessageStatus.Complete, 5L)

            val texts =
                database
                    .messageDao()
                    .observeForConversation(conversationId)
                    .first()
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf("first", "second", "third"), texts)
        }

    @Test
    fun `history excludes a row the app never completed`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)
            database.append(conversationId, Role.User, "again", MessageStatus.Complete, 3L)
            database.append(conversationId, Role.Assistant, "stub", MessageStatus.Cancelled, 4L)

            val texts =
                database
                    .messageDao()
                    .completeForConversation(conversationId)
                    .map { (it.content.single() as ContentBlock.Text).text }

            assertEquals(listOf("ask", "again"), texts)
        }

    @Test
    fun `history is scoped to one conversation`() =
        runDatabaseTest { database ->
            val mine = database.newConversation()
            val theirs = database.newConversation()
            database.append(mine, Role.User, "mine", MessageStatus.Complete, 1L)
            database.append(theirs, Role.User, "theirs", MessageStatus.Complete, 1L)

            assertEquals(1, database.messageDao().completeForConversation(mine).size)
        }

    @Test
    fun `the last message is the most recently inserted one`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            val lastId =
                database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)

            val last = database.messageDao().lastForConversation(conversationId)

            assertEquals(lastId, last?.id)
            assertEquals(MessageStatus.Failed, last?.status)
        }

    @Test
    fun `deleting a message by id leaves the rest`() =
        runDatabaseTest { database ->
            val conversationId = database.newConversation()
            database.append(conversationId, Role.User, "ask", MessageStatus.Complete, 1L)
            val failed =
                database.append(conversationId, Role.Assistant, "half", MessageStatus.Failed, 2L)

            database.messageDao().deleteById(failed)

            assertEquals(
                1,
                database.messageDao().observeForConversation(conversationId).first().size,
            )
        }
}
```

- [x] **Step 3: Run the DAO tests**

Run: `./gradlew :shared:testAndroidHostTest --tests '*DaoTest*'`
Expected: `BUILD SUCCESSFUL`, 11 tests passing across the two classes.

If any query fails to compile, Room reports the SQL error at build time with the offending method name — fix the query, not the test.

- [x] **Step 4: Format and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: the DAO surface is proven, including the cascade and the `status = 'Complete'` literal.

---

### Task 4: The repository contract and its fake

The interface everything above the data layer depends on, plus the in-memory double that 005's ViewModel tests will use — both written before any real implementation exists, so the contract is designed against a consumer rather than around a database.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/TurnState.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepository.kt`
- Create: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt`
- Create: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt`

**Interfaces:**
- Consumes: `Conversation`, `Message`, `MessageStatus` (Task 2); `ChatError`, `ContentBlock`, `Role` (`…shared.chat`); `ClaudeModel`.
- Produces: `sealed interface TurnState { Idle; Streaming(text: String); Failed(error: ChatError) }`; `interface ConversationRepository` with `getConversationsFlow()`, `getMessagesFlow(Long)`, `getTurnFlow(Long)`, `send(Long?, String, ClaudeModel): Long`, `retry(Long)`, `cancel(Long)`, `setModel(Long, ClaudeModel)`, `delete(Long)`; `internal const val MAX_TITLE_LENGTH = 60`; test-only `FakeClock(instant)` and `FakeConversationRepository` with the driver methods `emitDelta(id, text)`, `completeTurn(id)`, `failTurn(id, error)`.

- [x] **Step 1: Write the failing fake-repository test**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepositoryTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FakeConversationRepositoryTest {
    private val repository = FakeConversationRepository()

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    @Test
    fun `sending with no id creates a conversation titled from the message`() =
        runTest {
            val id = repository.send(null, "plan a trip to Lisbon")

            val conversation = repository.getConversationsFlow().first().single()
            assertEquals(id, conversation.id)
            assertEquals("plan a trip to Lisbon", conversation.title)
            assertEquals(ClaudeModel.Default, conversation.model)
        }

    @Test
    fun `a long first message is truncated into the title`() =
        runTest {
            val id = repository.send(null, "x".repeat(200))

            val title = repository.getConversationsFlow().first().single { it.id == id }.title
            assertEquals(MAX_TITLE_LENGTH, title.length)
        }

    @Test
    fun `sending persists the user message and opens a turn`() =
        runTest {
            val id = repository.send(null, "hello")

            assertEquals(listOf("hello"), repository.getMessagesFlow(id).first().map { it.text() })
            assertEquals(TurnState.Streaming(""), repository.getTurnFlow(id).first())
        }

    @Test
    fun `deltas accumulate into the turn state`() =
        runTest {
            val id = repository.send(null, "hello")

            repository.emitDelta(id, "Hi ")
            repository.emitDelta(id, "there")

            assertEquals(TurnState.Streaming("Hi there"), repository.getTurnFlow(id).first())
        }

    @Test
    fun `completing a turn persists the reply and returns to idle`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi there")

            repository.completeTurn(id)

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("Hi there", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `failing a turn persists the partial reply and keeps the error readable`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi th")

            repository.failTurn(id, ChatError.Overloaded)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("Hi th", last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(TurnState.Failed(ChatError.Overloaded), repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second send on a live turn is rejected`() =
        runTest {
            val id = repository.send(null, "hello")

            assertFailsWith<IllegalStateException> { repository.send(id, "again") }
        }

    @Test
    fun `cancelling persists the partial reply and returns to idle`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.emitDelta(id, "Hi th")

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals("Hi th", last.text())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying drops the unfinished reply and reopens the turn`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.failTurn(id, ChatError.Network)

            repository.retry(id)

            assertEquals(listOf(Role.User), repository.getMessagesFlow(id).first().map { it.role })
            assertEquals(TurnState.Streaming(""), repository.getTurnFlow(id).first())
        }

    @Test
    fun `retrying with nothing to retry does nothing`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.completeTurn(id)

            repository.retry(id)

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
        }

    @Test
    fun `changing the model rewrites the conversation`() =
        runTest {
            val id = repository.send(null, "hello")

            repository.setModel(id, ClaudeModel.Opus)

            assertEquals(
                ClaudeModel.Opus,
                repository.getConversationsFlow().first().single { it.id == id }.model,
            )
        }

    @Test
    fun `deleting removes the conversation, its messages and its turn`() =
        runTest {
            val id = repository.send(null, "hello")
            repository.failTurn(id, ChatError.Network)

            repository.delete(id)

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertIs<TurnState.Idle>(repository.getTurnFlow(id).first())
        }

    @Test
    fun `a second conversation sorts ahead of the first`() =
        runTest {
            val first = repository.send(null, "one")
            repository.completeTurn(first)
            val second = repository.send(null, "two")

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )
        }
}
```

- [x] **Step 2: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*FakeConversationRepositoryTest*'`
Expected: compilation failure — `Unresolved reference: FakeConversationRepository`, `TurnState`, `MAX_TITLE_LENGTH`.

- [x] **Step 3: Write `TurnState`**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/TurnState.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatError

/**
 * The in-memory half of a conversation: the reply currently arriving, and how the last one ended.
 * Everything else about a conversation comes from the database.
 */
sealed interface TurnState {
    /** No reply is in flight. Whatever the last turn produced is already a persisted message. */
    data object Idle : TurnState

    /**
     * @property text the reply so far, cumulative rather than the latest delta, so a collector
     *   renders it directly and accumulates nothing.
     */
    data class Streaming(
        val text: String,
    ) : TurnState

    /**
     * The last turn did not finish. Readable until the next turn on this conversation replaces it;
     * the persisted message records only that it failed, never why.
     */
    data class Failed(
        val error: ChatError,
    ) : TurnState
}
```

- [x] **Step 4: Write the repository contract**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepository.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow

/** How much of a first message becomes a conversation's title. */
internal const val MAX_TITLE_LENGTH: Int = 60

/**
 * Owns chat history and the turn that produces it. The database is the source of truth for every
 * message; only the reply currently streaming is in memory, exposed as [TurnState].
 */
interface ConversationRepository {
    /** Every conversation, most recently updated first. */
    fun getConversationsFlow(): Flow<List<Conversation>>

    /** Every message in one conversation in insertion order, whatever its status. */
    fun getMessagesFlow(conversationId: Long): Flow<List<Message>>

    /**
     * The reply in flight for one conversation. Emits [TurnState.Idle] for a conversation with no
     * turn, and starts observing turns that begin after collection does.
     */
    fun getTurnFlow(conversationId: Long): Flow<TurnState>

    /**
     * Appends [text] as a user message and starts a turn. Returns as soon as the message is
     * stored; the reply arrives on [getTurnFlow] and is persisted when the stream ends, whether or
     * not anything is still collecting.
     *
     * @param conversationId null to start a new conversation, which is created together with this
     *   first message so an abandoned empty chat never appears in the list.
     * @param model used only when creating a conversation; afterwards the conversation's own model
     *   decides, and [setModel] changes it.
     * @return the conversation the message landed in.
     * @throws IllegalStateException if that conversation already has a turn in flight.
     */
    suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel = ClaudeModel.Default,
    ): Long

    /**
     * Drops a trailing reply that failed or was cancelled and runs the turn again. Does nothing
     * when the last message is not an unfinished reply.
     */
    suspend fun retry(conversationId: Long)

    /**
     * Stops the turn in flight, keeping whatever text arrived as a cancelled message. Returns once
     * that message is stored. Does nothing when no turn is in flight.
     */
    suspend fun cancel(conversationId: Long)

    /** Changes which model this conversation's later turns use. */
    suspend fun setModel(conversationId: Long, model: ClaudeModel)

    /** Removes the conversation and its messages, stopping any turn in flight first. */
    suspend fun delete(conversationId: Long)
}
```

- [x] **Step 5: Write `FakeClock`**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/FakeClock.kt`:

```kotlin
package com.shayanaryan.chatbot.shared

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/** A clock a test moves by hand, so timestamp assertions are exact. */
class FakeClock(
    var instant: Instant = Instant.fromEpochMilliseconds(0),
) : Clock {
    override fun now(): Instant = instant

    fun advanceBy(duration: Duration) {
        instant += duration
    }
}
```

If the compiler asks for an opt-in on `kotlin.time.Clock`, add `@OptIn(ExperimentalTime::class)` to the class and note it in your report — on Kotlin 2.4.10 the call sites compile without one.

- [x] **Step 6: Write the fake repository**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/conversation/FakeConversationRepository.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Clock

/**
 * In-memory [ConversationRepository] for tests above the data layer.
 *
 * There is no engine: a turn is opened by [send] or [retry] and moved by [emitDelta],
 * [completeTurn] and [failTurn], so a test decides exactly when a reply arrives. Turn state is
 * held independently of any collector, mirroring the production guarantee that a turn outlives
 * the screen that started it.
 */
class FakeConversationRepository(
    private val clock: Clock = FakeClock(),
) : ConversationRepository {
    private val conversations = MutableStateFlow<List<Conversation>>(emptyList())
    private val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val turns = MutableStateFlow<Map<Long, TurnState>>(emptyMap())

    private var nextConversationId = 1L
    private var nextMessageId = 1L

    override fun getConversationsFlow(): Flow<List<Conversation>> = conversations

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        messages.map { it[conversationId].orEmpty() }.distinctUntilChanged()

    override fun getTurnFlow(conversationId: Long): Flow<TurnState> =
        turns.map { it[conversationId] ?: TurnState.Idle }.distinctUntilChanged()

    override suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long {
        check(turns.value[conversationId] !is TurnState.Streaming) {
            "conversation $conversationId already has a turn in flight"
        }
        val id = conversationId ?: createConversation(text, model)
        appendMessage(id, Role.User, text, MessageStatus.Complete)
        turns.update { it + (id to TurnState.Streaming("")) }
        return id
    }

    override suspend fun retry(conversationId: Long) {
        val last = messages.value[conversationId]?.lastOrNull() ?: return
        if (last.role != Role.Assistant || last.status == MessageStatus.Complete) return
        messages.update { all ->
            all + (conversationId to all.getValue(conversationId).dropLast(1))
        }
        turns.update { it + (conversationId to TurnState.Streaming("")) }
    }

    override suspend fun cancel(conversationId: Long) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Cancelled)
        turns.update { it - conversationId }
    }

    override suspend fun setModel(conversationId: Long, model: ClaudeModel) {
        conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(model = model) else it }
        }
    }

    override suspend fun delete(conversationId: Long) {
        conversations.update { list -> list.filterNot { it.id == conversationId } }
        messages.update { it - conversationId }
        turns.update { it - conversationId }
    }

    /** Grows the reply in flight, as a stream delta would. */
    fun emitDelta(conversationId: Long, text: String) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        turns.update {
            it + (conversationId to TurnState.Streaming(streaming.text + text))
        }
    }

    /** Ends the turn successfully: the reply becomes a complete message. */
    fun completeTurn(conversationId: Long) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Complete)
        turns.update { it - conversationId }
    }

    /** Ends the turn in failure: the partial reply is stored and [error] stays readable. */
    fun failTurn(conversationId: Long, error: ChatError) {
        val streaming = turns.value[conversationId] as? TurnState.Streaming ?: return
        appendMessage(conversationId, Role.Assistant, streaming.text, MessageStatus.Failed)
        turns.update { it + (conversationId to TurnState.Failed(error)) }
    }

    private fun createConversation(text: String, model: ClaudeModel): Long {
        val now = clock.now()
        val id = nextConversationId++
        val conversation =
            Conversation(
                id = id,
                title = text.take(MAX_TITLE_LENGTH),
                model = model,
                createdAt = now,
                updatedAt = now,
            )
        conversations.update { listOf(conversation) + it }
        messages.update { it + (id to emptyList()) }
        return id
    }

    private fun appendMessage(
        conversationId: Long,
        role: Role,
        text: String,
        status: MessageStatus,
    ) {
        val now = clock.now()
        val message =
            Message(
                id = nextMessageId++,
                conversationId = conversationId,
                role = role,
                content = listOf(ContentBlock.Text(text)),
                status = status,
                createdAt = now,
            )
        messages.update { all ->
            all + (conversationId to (all[conversationId].orEmpty() + message))
        }
        conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(updatedAt = now) else it }
        }
    }
}
```

- [x] **Step 7: Run the fake test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*FakeConversationRepositoryTest*'`
Expected: `BUILD SUCCESSFUL`, 13 tests passing.

- [x] **Step 8: Format, run the full suite, and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: the contract and its fake are in; the fake is the dependency the conversation UI's tests will take.

---

### Task 5: A chat engine a test drives event by event

003's `FakeScriptedChatEngine` emits a fixed list eagerly on collection, which cannot express "assert while the stream is open". Three of the repository tests need exactly that. This adds a second double beside it; the scripted one keeps the behaviour its own spec documents, and the pair is named for how events arrive — scripted up front, or fed by hand.

**Files:**
- Create: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt`
- Test: `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt`
- Already applied: 003's `FakeChatEngine` was renamed to `FakeScriptedChatEngine` (file and class, behaviour untouched) so the two doubles read as a pair. `specs/003-chat-engine.md` carries the renamed one; `specs/004-conversation-storage.md` names both.

**Interfaces:**
- Consumes: `ChatEngine`, `ChatRequest`, `ChatStreamEvent`, `StopReason`, `TokenUsage` (003).
- Produces: `class FakeManualChatEngine : ChatEngine` with `suspend fun awaitStream()`, `suspend fun send(event: ChatStreamEvent)`, `fun close()`, and `val requests: List<ChatRequest>`.

- [x] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngineTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeManualChatEngineTest {
    private val request =
        ChatRequest(messages = listOf(ChatMessage(Role.User, listOf(ContentBlock.Text("hi")))))

    @Test
    fun `holds the stream open until it is closed`() =
        runTest {
            val engine = FakeManualChatEngine()
            val collected = mutableListOf<ChatStreamEvent>()
            val collector = launch { engine.stream(request).toList(collected) }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("one"))
            runCurrent()

            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("one")), collected)
            assertTrue(collector.isActive)

            engine.send(ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1)))
            engine.close()
            collector.join()

            assertEquals(2, collected.size)
        }

    @Test
    fun `records the request it was asked to stream`() =
        runTest {
            val engine = FakeManualChatEngine()
            val collector = launch { engine.stream(request).toList() }

            engine.awaitStream()
            engine.close()
            collector.join()

            assertEquals(listOf(request), engine.requests)
        }

    @Test
    fun `serves a second stream after the first is closed`() =
        runTest {
            val engine = FakeManualChatEngine()
            val first = mutableListOf<ChatStreamEvent>()
            val firstCollector = launch { engine.stream(request).toList(first) }
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("one"))
            engine.close()
            firstCollector.join()

            val second = mutableListOf<ChatStreamEvent>()
            val secondCollector = launch { engine.stream(request).toList(second) }
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("two"))
            engine.close()
            secondCollector.join()

            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("one")), first)
            assertEquals(listOf<ChatStreamEvent>(ChatStreamEvent.Delta("two")), second)
            assertEquals(2, engine.requests.size)
        }
}
```

- [x] **Step 2: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*FakeManualChatEngineTest*'`
Expected: compilation failure — `Unresolved reference: FakeManualChatEngine`.

- [x] **Step 3: Write the engine**

Create `shared/src/commonTest/kotlin/com/shayanaryan/chatbot/shared/chat/FakeManualChatEngine.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.chat

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * A [ChatEngine] whose stream the test opens, feeds, and closes by hand, so an assertion can be
 * taken while a turn is still in flight.
 *
 * Usage is always: start collecting, [awaitStream] to wait for the collector to arrive, then
 * [send] events and [close] when the turn is over. A second collection is served the same way
 * once the first is closed.
 *
 * @property requests every request this engine was asked to stream, in call order.
 */
class FakeManualChatEngine : ChatEngine {
    private val recorded = mutableListOf<ChatRequest>()
    private val opened = Channel<Channel<ChatStreamEvent>>(Channel.UNLIMITED)
    private var current: Channel<ChatStreamEvent>? = null

    val requests: List<ChatRequest> get() = recorded.toList()

    /** Suspends until a collector opens a stream, which then becomes the target of [send]. */
    suspend fun awaitStream() {
        current = opened.receive()
    }

    suspend fun send(event: ChatStreamEvent) {
        requireNotNull(current) { "no stream is open; call awaitStream() first" }.send(event)
    }

    /** Ends the open stream, completing the collector. */
    fun close() {
        requireNotNull(current) { "no stream is open; call awaitStream() first" }.close()
        current = null
    }

    override fun stream(request: ChatRequest): Flow<ChatStreamEvent> =
        flow {
            val events = Channel<ChatStreamEvent>(Channel.UNLIMITED)
            recorded += request
            opened.send(events)
            emitAll(events.consumeAsFlow())
        }
}
```

- [x] **Step 4: Run the test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*FakeManualChatEngineTest*'`
Expected: `BUILD SUCCESSFUL`, 3 tests passing.

- [x] **Step 5: Format and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: `FakeManualChatEngine` is available; `FakeScriptedChatEngine`'s behaviour is unchanged.

---

### Task 6: The repository — reads, model changes, and delete

The real implementation against a real database, with no turn machinery exercised yet. This is the task where the mappers and the assembly seam land.

**Files:**
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/Mappers.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/DefaultConversationRepository.kt`
- Create: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepositoryFactory.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepositoryTest.kt`

**Interfaces:**
- Consumes: `ConversationDao`, `MessageDao`, entities (Task 2); `ConversationRepository`, `TurnState`, `MAX_TITLE_LENGTH` (Task 4); `FakeClock` (Task 4); `FakeScriptedChatEngine` (003); `runDatabaseTest` (Task 2).
- Produces: `internal class DefaultConversationRepository(engine, conversationDao, messageDao, externalScope, clock)`; `fun createConversationRepository(database, engine, externalScope, clock = Clock.System): ConversationRepository`; `internal fun ConversationEntity.toDomain()`, `internal fun MessageEntity.toDomain()`, `internal fun MessageEntity.toChatMessage()`.

- [x] **Step 1: Write the failing test**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepositoryTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.FakeScriptedChatEngine
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class ConversationRepositoryTest {
    private fun TestScope.turnScope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun TestScope.repository(
        database: ChatbotDatabase,
        scope: CoroutineScope,
        clock: FakeClock = FakeClock(),
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine = FakeScriptedChatEngine(
                events = listOf(ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))),
            ),
            externalScope = scope,
            clock = clock,
        )

    @Test
    fun `a new conversation carries the requested model and a truncated title`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val id = repository.send(null, "y".repeat(120), ClaudeModel.Opus)
            advanceUntilIdle()

            val conversation = repository.getConversationsFlow().first().single()
            assertEquals(id, conversation.id)
            assertEquals(MAX_TITLE_LENGTH, conversation.title.length)
            assertEquals(ClaudeModel.Opus, conversation.model)
            assertEquals(Instant.fromEpochMilliseconds(1_000), conversation.createdAt)
            scope.cancel()
        }

    @Test
    fun `conversations are listed most recently updated first`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val first = repository.send(null, "one")
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, "two")
            advanceUntilIdle()

            assertEquals(
                listOf(second, first),
                repository.getConversationsFlow().first().map { it.id },
            )
            scope.cancel()
        }

    @Test
    fun `the sent message is exposed as a complete user message`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = repository(database, scope, clock)

            val id = repository.send(null, "hello")
            advanceUntilIdle()

            val first = repository.getMessagesFlow(id).first().first()
            assertEquals(Role.User, first.role)
            assertEquals("hello", (first.content.single() as ContentBlock.Text).text)
            assertEquals(MessageStatus.Complete, first.status)
            assertEquals(Instant.fromEpochMilliseconds(1_000), first.createdAt)
            assertEquals(id, first.conversationId)
            scope.cancel()
        }

    @Test
    fun `a conversation with no turn reports idle`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)

            assertEquals(TurnState.Idle, repository.getTurnFlow(404L).first())
            scope.cancel()
        }

    @Test
    fun `changing the model rewrites only that conversation`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val target = repository.send(null, "target")
            advanceUntilIdle()
            val other = repository.send(null, "other")
            advanceUntilIdle()

            repository.setModel(target, ClaudeModel.Haiku)

            val byId = repository.getConversationsFlow().first().associateBy { it.id }
            assertEquals(ClaudeModel.Haiku, byId.getValue(target).model)
            assertEquals(ClaudeModel.Default, byId.getValue(other).model)
            scope.cancel()
        }

    @Test
    fun `deleting removes the conversation and its messages`() =
        runDatabaseTest { database ->
            val scope = turnScope()
            val repository = repository(database, scope)
            val id = repository.send(null, "hello")
            advanceUntilIdle()

            repository.delete(id)

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            scope.cancel()
        }
}
```

- [x] **Step 2: Run it to verify it fails**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationRepositoryTest*'`
Expected: compilation failure — `Unresolved reference: createConversationRepository`.

- [x] **Step 3: Write the mappers**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/local/Mappers.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation.local

import com.shayanaryan.chatbot.shared.chat.ChatMessage
import com.shayanaryan.chatbot.shared.conversation.Conversation
import com.shayanaryan.chatbot.shared.conversation.Message
import kotlin.time.Instant

internal fun ConversationEntity.toDomain(): Conversation =
    Conversation(
        id = id,
        title = title,
        model = model,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

internal fun MessageEntity.toDomain(): Message =
    Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        status = status,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
    )

internal fun MessageEntity.toChatMessage(): ChatMessage =
    ChatMessage(role = role, content = content)
```

- [x] **Step 4: Write the repository**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/DefaultConversationRepository.kt`. This is the full file; Task 7 and Task 8 fill in the turn body, so the parts they own are stubbed here in the smallest form that satisfies this task's tests.

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.conversation.local.ConversationDao
import com.shayanaryan.chatbot.shared.conversation.local.ConversationEntity
import com.shayanaryan.chatbot.shared.conversation.local.MessageDao
import com.shayanaryan.chatbot.shared.conversation.local.MessageEntity
import com.shayanaryan.chatbot.shared.conversation.local.toDomain
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/**
 * One reply in flight: the state a collector reads, and the coroutine filling it.
 */
private class Turn(
    val state: MutableStateFlow<TurnState>,
    val job: Job,
)

internal class DefaultConversationRepository(
    private val engine: ChatEngine,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val externalScope: CoroutineScope,
    private val clock: Clock,
) : ConversationRepository {
    /**
     * The latest turn per conversation, live or terminal. An immutable map behind a state flow, so
     * reads need no lock and a collector sees turns that start after it does.
     */
    private val turns = MutableStateFlow<Map<Long, Turn>>(emptyMap())

    /**
     * Guards the writes. A single compare-and-set is already atomic, but the guard spans two
     * operations — test for a live turn, then start one — and without the lock two concurrent
     * sends both see none and both launch.
     */
    private val mutex = Mutex()

    override fun getConversationsFlow(): Flow<List<Conversation>> =
        conversationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun getMessagesFlow(conversationId: Long): Flow<List<Message>> =
        messageDao
            .observeForConversation(conversationId)
            .map { entities -> entities.map { it.toDomain() } }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTurnFlow(conversationId: Long): Flow<TurnState> =
        turns
            .flatMapLatest { it[conversationId]?.state ?: flowOf(TurnState.Idle) }
            .distinctUntilChanged()

    override suspend fun send(
        conversationId: Long?,
        text: String,
        model: ClaudeModel,
    ): Long =
        mutex.withLock {
            val now = clock.now().toEpochMilliseconds()
            val id =
                if (conversationId == null) {
                    conversationDao.createWithFirstMessage(
                        conversation =
                            ConversationEntity(
                                title = text.take(MAX_TITLE_LENGTH),
                                model = model,
                                createdAt = now,
                                updatedAt = now,
                            ),
                        message = userMessage(conversationId = 0, text = text, createdAt = now),
                    )
                } else {
                    requireIdle(conversationId)
                    conversationDao.appendMessage(
                        message = userMessage(conversationId, text, now),
                        updatedAt = now,
                    )
                    conversationId
                }
            launchTurn(id)
            id
        }

    override suspend fun retry(conversationId: Long) {
        // Filled in with the turn lifecycle.
    }

    override suspend fun cancel(conversationId: Long) {
        // Filled in with the turn lifecycle.
    }

    override suspend fun setModel(conversationId: Long, model: ClaudeModel) {
        conversationDao.setModel(conversationId, model)
    }

    override suspend fun delete(conversationId: Long) {
        turns.value[conversationId]?.job?.cancelAndJoin()
        conversationDao.delete(conversationId)
        mutex.withLock { turns.update { it - conversationId } }
    }

    private fun userMessage(conversationId: Long, text: String, createdAt: Long) =
        MessageEntity(
            conversationId = conversationId,
            role = Role.User,
            content = listOf(ContentBlock.Text(text)),
            status = MessageStatus.Complete,
            createdAt = createdAt,
        )

    /**
     * A turn is live while its job is running *and* its state still says so: testing the job
     * alone would let a turn that died unexpectedly block every later send, and testing the entry
     * alone would reject a send that lands between a finished turn going idle and its entry being
     * dropped.
     */
    private fun requireIdle(conversationId: Long) {
        val turn = turns.value[conversationId]
        check(turn == null || !turn.job.isActive || turn.state.value !is TurnState.Streaming) {
            "conversation $conversationId already has a turn in flight"
        }
    }

    /** Call under [mutex]: the entry must exist before the coroutine can clear it. */
    private fun launchTurn(conversationId: Long) {
        val state = MutableStateFlow<TurnState>(TurnState.Streaming(""))
        val job =
            externalScope.launch(start = CoroutineStart.LAZY) {
                runTurn(conversationId, state)
            }
        turns.update { it + (conversationId to Turn(state, job)) }
        job.start()
    }

    private suspend fun runTurn(conversationId: Long, state: MutableStateFlow<TurnState>) {
        state.value = TurnState.Idle
        clearTurn(conversationId, state)
    }

    /**
     * Drops this turn's entry, and only this one — a later turn may already have replaced it.
     */
    private suspend fun clearTurn(conversationId: Long, state: MutableStateFlow<TurnState>) {
        mutex.withLock {
            turns.update { current ->
                if (current[conversationId]?.state === state) current - conversationId else current
            }
        }
    }
}
```

- [x] **Step 5: Write the assembly seam**

Create `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationRepositoryFactory.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Clock

/**
 * Assembles the repository over a database. The DAOs stay inside this module; callers pass the
 * database as an opaque handle.
 *
 * @param externalScope where a turn runs. It must outlive any screen, since a reply is persisted
 *   whether or not anything is still collecting it.
 * @param clock injected so tests can assert exact timestamps.
 */
fun createConversationRepository(
    database: ChatbotDatabase,
    engine: ChatEngine,
    externalScope: CoroutineScope,
    clock: Clock = Clock.System,
): ConversationRepository =
    DefaultConversationRepository(
        engine = engine,
        conversationDao = database.conversationDao(),
        messageDao = database.messageDao(),
        externalScope = externalScope,
        clock = clock,
    )
```

- [x] **Step 6: Run the test to verify it passes**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationRepositoryTest*'`
Expected: `BUILD SUCCESSFUL`, 6 tests passing.

Every assertion here is deliberately about a write the repository makes itself, never about a reply — the turn body is still a stub, so no assistant row exists yet.

- [x] **Step 7: Format, run the full suite, and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: reads, `setModel` and `delete` work against a real database; the turn body is still a stub.

---

### Task 7: The turn lifecycle

The heart of the spec: streaming a reply in a scope that outlives its collector, persisting before going idle, and refusing a second concurrent turn.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/DefaultConversationRepository.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationTurnTest.kt`

**Interfaces:**
- Consumes: everything from Task 6; `FakeManualChatEngine` (Task 5).
- Produces: the real `runTurn` — `ChatStreamEvent.Delta` grows `TurnState.Streaming`, `Completed` persists a `Complete` assistant row then emits `Idle`, `Failed` persists a `Failed` assistant row then emits `TurnState.Failed(error)`.

A turn catches `CancellationException` and nothing else. A throw the `ChatEngine` contract does not describe — storage failing under the turn is the only realistic source — ends the process, since nothing is waiting on the coroutine and the scope carries no handler. Left that way deliberately: the guard costs more structure than the fault is worth, and a crash is at least loud.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationTurnTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.FakeManualChatEngine
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import com.shayanaryan.chatbot.shared.model.ClaudeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class ConversationTurnTest {
    private fun TestScope.scope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    private fun completed() = ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))

    @Test
    fun `deltas accumulate on the turn and the reply lands as a complete message`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi "))
            runCurrent()
            assertEquals(TurnState.Streaming("Hi "), repository.getTurnFlow(id).first())

            engine.send(ChatStreamEvent.Delta("there"))
            runCurrent()
            assertEquals(TurnState.Streaming("Hi there"), repository.getTurnFlow(id).first())

            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("Hi there", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `the reply row exists before the turn reports idle`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            val rowsWhenIdle = mutableListOf<Int>()
            val collector =
                launch {
                    repository.getTurnFlow(id).collect { state ->
                        if (state == TurnState.Idle) {
                            rowsWhenIdle += repository.getMessagesFlow(id).first().size
                        }
                    }
                }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("done"))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(listOf(2), rowsWhenIdle)
            collector.cancel()
            turnScope.cancel()
        }

    @Test
    fun `bumping updatedAt on the reply reorders the conversation list`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val clock = FakeClock(Instant.fromEpochMilliseconds(1_000))
            val repository = createConversationRepository(database, engine, turnScope, clock)
            val first = repository.send(null, "one")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()
            clock.advanceBy(60.seconds)
            val second = repository.send(null, "two")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            clock.advanceBy(60.seconds)
            repository.send(first, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(first, second),
                repository.getConversationsFlow().first().map { it.id },
            )
            turnScope.cancel()
        }

    @Test
    fun `a failed stream persists the partial reply and keeps the error readable`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi th"))
            engine.send(ChatStreamEvent.Failed(ChatError.Overloaded))
            engine.close()
            advanceUntilIdle()

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("Hi th", last.text())
            assertEquals(MessageStatus.Failed, last.status)
            assertEquals(
                TurnState.Failed(ChatError.Overloaded),
                repository.getTurnFlow(id).first(),
            )
            turnScope.cancel()
        }

    @Test
    fun `a failed reply is left out of the next turn's history`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("Hi th"))
            engine.send(ChatStreamEvent.Failed(ChatError.Network))
            engine.close()
            advanceUntilIdle()

            repository.send(id, "are you there")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val history = engine.requests.last().messages
            assertEquals(listOf(Role.User, Role.User), history.map { it.role })
            assertEquals(
                listOf("hello", "are you there"),
                history.map { (it.content.single() as ContentBlock.Text).text },
            )
            turnScope.cancel()
        }

    @Test
    fun `the turn uses the conversation's own model, not the send default`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello", ClaudeModel.Opus)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.setModel(id, ClaudeModel.Haiku)
            repository.send(id, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf(ClaudeModel.Opus, ClaudeModel.Haiku),
                engine.requests.map { it.model },
            )
            turnScope.cancel()
        }

    @Test
    fun `a second send while a turn is in flight is rejected`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()

            assertFailsWith<IllegalStateException> { repository.send(id, "again") }

            assertEquals(1, repository.getMessagesFlow(id).first().size)
            engine.send(completed())
            engine.close()
            advanceUntilIdle()
            turnScope.cancel()
        }

    @Test
    fun `a collector attaching mid stream sees the text already accumulated`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("already "))
            engine.send(ChatStreamEvent.Delta("here"))
            runCurrent()

            assertEquals(TurnState.Streaming("already here"), repository.getTurnFlow(id).first())

            engine.send(completed())
            engine.close()
            advanceUntilIdle()
            turnScope.cancel()
        }

    @Test
    fun `a turn outlives the scope that was collecting it`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            val screenScope = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
            val seen = mutableListOf<TurnState>()
            screenScope.launch { repository.getTurnFlow(id).toList(seen) }

            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("par"))
            runCurrent()
            screenScope.cancel()
            runCurrent()

            engine.send(ChatStreamEvent.Delta("tial"))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals("partial", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertTrue(seen.none { it == TurnState.Idle })
            turnScope.cancel()
        }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationTurnTest*'`
Expected: FAIL. `engine.awaitStream()` hangs or the assertions report `[User]` where `[User, Assistant]` is expected — the stub `runTurn` never opens a stream.

If a test hangs rather than failing, `runTest` times out after 60 s with `After waiting for 1m, the test coroutine is not completing`. That is the expected red for this step.

- [ ] **Step 3: Implement the turn body**

In `DefaultConversationRepository.kt`, add these imports:

```kotlin
import com.shayanaryan.chatbot.shared.chat.ChatRequest
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.conversation.local.toChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
```

and replace the stub `runTurn` with:

```kotlin
    private suspend fun runTurn(conversationId: Long, state: MutableStateFlow<TurnState>) {
        val conversation = conversationDao.findById(conversationId)
        if (conversation == null) {
            state.value = TurnState.Idle
            clearTurn(conversationId, state)
            return
        }
        val history = messageDao.completeForConversation(conversationId).map { it.toChatMessage() }
        val reply = StringBuilder()
        var failure: ChatError? = null
        try {
            engine
                .stream(
                    ChatRequest(
                        messages = history,
                        model = conversation.model,
                        system = null,
                    ),
                ).collect { event ->
                    when (event) {
                        is ChatStreamEvent.Delta -> {
                            reply.append(event.text)
                            state.value = TurnState.Streaming(reply.toString())
                        }

                        is ChatStreamEvent.Failed -> failure = event.error
                        is ChatStreamEvent.Completed -> Unit
                    }
                }
        } catch (cancellation: CancellationException) {
            // The turn owns every write, cancellation included, so exactly one assistant row is
            // ever produced per turn — and it is stored before the caller resumes.
            withContext(NonCancellable) {
                persistReply(conversationId, reply.toString(), MessageStatus.Cancelled)
                state.value = TurnState.Idle
                clearTurn(conversationId, state)
            }
            throw cancellation
        }
        val error = failure
        if (error == null) {
            persistReply(conversationId, reply.toString(), MessageStatus.Complete)
            state.value = TurnState.Idle
            clearTurn(conversationId, state)
        } else {
            // The entry stays so the error is still readable when a collector arrives late; the
            // next turn on this conversation replaces it.
            persistReply(conversationId, reply.toString(), MessageStatus.Failed)
            state.value = TurnState.Failed(error)
        }
    }

    private suspend fun persistReply(
        conversationId: Long,
        text: String,
        status: MessageStatus,
    ) {
        val now = clock.now().toEpochMilliseconds()
        conversationDao.appendMessage(
            message =
                MessageEntity(
                    conversationId = conversationId,
                    role = Role.Assistant,
                    content = listOf(ContentBlock.Text(text)),
                    status = status,
                    createdAt = now,
                ),
            updatedAt = now,
        )
    }
```

- [ ] **Step 4: Run the turn tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationTurnTest*'`
Expected: `BUILD SUCCESSFUL`, 9 tests passing.

- [ ] **Step 5: Re-run Task 6's suite, which now exercises a real turn**

`ConversationRepositoryTest` builds its repository over `FakeScriptedChatEngine`, which emits a single `Completed` eagerly. Task 6 ran those tests against a stubbed turn; they now run a real one, so this confirms the turn body did not change any of the read paths.

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationRepositoryTest*'`
Expected: `BUILD SUCCESSFUL`, 6 tests passing.

- [ ] **Step 6: Format, run the full suite, and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`.

Report: the turn lifecycle is complete; call out that the external-scope lifetime test passes, since it is the assertion the whole design exists to satisfy.

---

### Task 8: Cancel, retry, and deleting mid-turn

The two operations that reach into a live turn, plus the interaction between `delete` and a turn still running.

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/shayanaryan/chatbot/shared/conversation/DefaultConversationRepository.kt`
- Test: `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationCancelRetryTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 6 and 7.
- Produces: the real `cancel` and `retry` bodies. No signature changes.

- [ ] **Step 1: Write the failing tests**

Create `shared/src/androidHostTest/kotlin/com/shayanaryan/chatbot/shared/conversation/ConversationCancelRetryTest.kt`:

```kotlin
package com.shayanaryan.chatbot.shared.conversation

import com.shayanaryan.chatbot.shared.FakeClock
import com.shayanaryan.chatbot.shared.chat.ChatError
import com.shayanaryan.chatbot.shared.chat.ChatStreamEvent
import com.shayanaryan.chatbot.shared.chat.ContentBlock
import com.shayanaryan.chatbot.shared.chat.FakeManualChatEngine
import com.shayanaryan.chatbot.shared.chat.Role
import com.shayanaryan.chatbot.shared.chat.StopReason
import com.shayanaryan.chatbot.shared.chat.TokenUsage
import com.shayanaryan.chatbot.shared.database.runDatabaseTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ConversationCancelRetryTest {
    private fun TestScope.scope(): CoroutineScope =
        CoroutineScope(StandardTestDispatcher(testScheduler) + Job())

    private fun Message.text(): String = (content.single() as ContentBlock.Text).text

    private fun completed() = ChatStreamEvent.Completed(StopReason.EndTurn, TokenUsage(1, 1))

    @Test
    fun `cancelling stores the partial reply and returns the turn to idle`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half way"))
            runCurrent()

            repository.cancel(id)

            val last = repository.getMessagesFlow(id).first().last()
            assertEquals("half way", last.text())
            assertEquals(MessageStatus.Cancelled, last.status)
            assertEquals(Role.Assistant, last.role)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `cancelling with no turn in flight does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.cancel(id)

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `a cancelled reply is left out of the next turn's history`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            runCurrent()
            repository.cancel(id)

            repository.send(id, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(
                listOf("hello", "again"),
                engine.requests.last().messages.map {
                    (it.content.single() as ContentBlock.Text).text
                },
            )
            turnScope.cancel()
        }

    @Test
    fun `sending is allowed again once a turn is cancelled`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            repository.cancel(id)

            repository.send(id, "again")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(4, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `retrying drops the failed reply and runs the turn again`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            engine.send(ChatStreamEvent.Failed(ChatError.Overloaded))
            engine.close()
            advanceUntilIdle()

            repository.retry(id)
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("second try"))
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            val messages = repository.getMessagesFlow(id).first()
            assertEquals(listOf(Role.User, Role.Assistant), messages.map { it.role })
            assertEquals("second try", messages.last().text())
            assertEquals(MessageStatus.Complete, messages.last().status)
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }

    @Test
    fun `retrying drops a cancelled reply too`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            runCurrent()
            repository.cancel(id)

            repository.retry(id)
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            turnScope.cancel()
        }

    @Test
    fun `retrying after a completed turn does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(completed())
            engine.close()
            advanceUntilIdle()

            repository.retry(id)
            advanceUntilIdle()

            assertEquals(2, repository.getMessagesFlow(id).first().size)
            assertEquals(1, engine.requests.size)
            turnScope.cancel()
        }

    @Test
    fun `retrying an unknown conversation does nothing`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())

            repository.retry(404L)
            advanceUntilIdle()

            assertTrue(engine.requests.isEmpty())
            turnScope.cancel()
        }

    @Test
    fun `deleting during a turn leaves nothing behind`() =
        runDatabaseTest { database ->
            val engine = FakeManualChatEngine()
            val turnScope = scope()
            val repository =
                createConversationRepository(database, engine, turnScope, FakeClock())
            val id = repository.send(null, "hello")
            engine.awaitStream()
            engine.send(ChatStreamEvent.Delta("half"))
            runCurrent()

            repository.delete(id)
            advanceUntilIdle()

            assertTrue(repository.getConversationsFlow().first().isEmpty())
            assertTrue(repository.getMessagesFlow(id).first().isEmpty())
            assertEquals(TurnState.Idle, repository.getTurnFlow(id).first())
            turnScope.cancel()
        }
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationCancelRetryTest*'`
Expected: FAIL — `cancel` and `retry` are no-op stubs, so the cancelled/retried assertions report the wrong message counts, and `sending is allowed again once a turn is cancelled` throws `IllegalStateException`.

- [ ] **Step 3: Implement `cancel` and `retry`**

In `DefaultConversationRepository.kt`, replace the two stubs with:

```kotlin
    override suspend fun retry(conversationId: Long) {
        mutex.withLock {
            val last = messageDao.lastForConversation(conversationId) ?: return@withLock
            if (last.role != Role.Assistant || last.status == MessageStatus.Complete) {
                return@withLock
            }
            messageDao.deleteById(last.id)
            launchTurn(conversationId)
        }
    }

    override suspend fun cancel(conversationId: Long) {
        // Not under the lock: the turn's own cancellation path takes it to clear its entry, and
        // joining while holding it would deadlock. Joining is what guarantees the cancelled
        // message is stored before this returns.
        turns.value[conversationId]?.job?.cancelAndJoin()
    }
```

`retry` needs no separate liveness guard: while a turn is in flight the last row is the user message, so the role test already makes it a no-op. Deleting the trailing row needs no other undo — the history query never saw it.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :shared:testAndroidHostTest --tests '*ConversationCancelRetryTest*'`
Expected: `BUILD SUCCESSFUL`, 9 tests passing.

If `deleting during a turn leaves nothing behind` fails with a foreign-key constraint error, the join in `delete` is not happening before `conversationDao.delete` — check the order in `delete`.

- [ ] **Step 5: Format, run the full suite, and report**

Run: `./gradlew :shared:spotlessApply && ./gradlew :shared:testAndroidHostTest && ./gradlew spotlessCheck`
Expected: `BUILD SUCCESSFUL`, the whole `:shared` suite green.

Report: `cancel`, `retry` and mid-turn `delete` are done; the repository is feature-complete.

---

### Task 9: `:app` wiring, spec reconciliation, and the full gate

The DI registrations `:app` owns, and the spec brought back in line with what shipped.

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/di/ApplicationScope.kt`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/di/CoroutinesModule.kt`
- Create: `app/src/main/kotlin/com/shayanaryan/chatbot/di/DatabaseModule.kt`
- Modify: `specs/004-conversation-storage.md`
- Modify: `docs/roadmap.md`

**Interfaces:**
- Consumes: `ChatbotDatabase`, `createChatbotDatabase`, `chatbotDatabaseBuilder` (Task 2); `ConversationRepository`, `createConversationRepository` (Task 6); `ChatEngine` (003, already provided by `ChatModule`).
- Produces: Hilt bindings for `ChatbotDatabase`, `ConversationRepository`, and an `@ApplicationScope CoroutineScope`, available to feature ViewModels from the next spec onward.

- [ ] **Step 1: Add the coroutines dependency to `:app`**

`:shared` declares `kotlinx-coroutines-core` as `implementation`, so it is not on `:app`'s compile classpath, and the scope provider names `CoroutineScope`, `SupervisorJob` and `Dispatchers`. In `app/build.gradle.kts`, add to the `dependencies` block under `implementation(libs.androidx.lifecycle.runtime.compose)`:

```kotlin
    implementation(libs.kotlinx.coroutines.core)
```

- [ ] **Step 2: Write the qualifier**

Create `app/src/main/kotlin/com/shayanaryan/chatbot/di/ApplicationScope.kt`:

```kotlin
package com.shayanaryan.chatbot.di

import javax.inject.Qualifier

/**
 * Marks the coroutine scope that lives as long as the process. Work launched there survives the
 * screen that started it — a reply still being streamed is persisted whether or not anything is
 * left collecting.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
```

- [ ] **Step 3: Write the scope module**

Create `app/src/main/kotlin/com/shayanaryan/chatbot/di/CoroutinesModule.kt`:

```kotlin
package com.shayanaryan.chatbot.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {
    /**
     * A [SupervisorJob] so one failed piece of background work never cancels the rest, and never
     * cancelled itself — the process ending is what ends it.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
```

No `CoroutineExceptionHandler`. A supervisor job keeps one failed piece of work from taking the others down; it does not swallow throws, and a fault nobody expected should stay visible rather than becoming a silent log line.

- [ ] **Step 4: Write the database module**

Create `app/src/main/kotlin/com/shayanaryan/chatbot/di/DatabaseModule.kt`:

```kotlin
package com.shayanaryan.chatbot.di

import android.content.Context
import com.shayanaryan.chatbot.shared.chat.ChatEngine
import com.shayanaryan.chatbot.shared.conversation.ConversationRepository
import com.shayanaryan.chatbot.shared.conversation.createConversationRepository
import com.shayanaryan.chatbot.shared.database.ChatbotDatabase
import com.shayanaryan.chatbot.shared.database.chatbotDatabaseBuilder
import com.shayanaryan.chatbot.shared.database.createChatbotDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Registers `:shared`'s storage layer. Hilt cannot enter `:shared`, so the database and the
 * repository are assembled here through the module's public factories; the DAOs stay inside it.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideChatbotDatabase(
        @ApplicationContext context: Context,
    ): ChatbotDatabase = createChatbotDatabase(chatbotDatabaseBuilder(context))

    @Provides
    @Singleton
    fun provideConversationRepository(
        database: ChatbotDatabase,
        engine: ChatEngine,
        @ApplicationScope externalScope: CoroutineScope,
    ): ConversationRepository =
        createConversationRepository(
            database = database,
            engine = engine,
            externalScope = externalScope,
        )
}
```

- [ ] **Step 5: Verify the app still assembles**

Run: `./gradlew :app:assembleDebug`
Expected: `BUILD SUCCESSFUL`.

Two things this step is really checking:

1. **`api(libs.androidx.room.runtime)` is doing its job.** If Kotlin reports `Cannot access class 'androidx.room.RoomDatabase'. Check your module classpath`, the `api` declaration did not propagate through the AGP KMP library plugin. Fall back to adding `api(libs.androidx.room.runtime)` to `androidMain.dependencies` as well, and record it in your task report.
2. **The missing `ApiKeyProvider` binding is still harmless.** Dagger prunes provider methods nothing reaches, and nothing injects `ConversationRepository` yet. If Dagger instead reports `ApiKeyProvider cannot be provided without an @Provides-annotated method`, delete `DatabaseModule.kt`, note in your report that the storage bindings move to the spec that introduces the dev-key stub, and adjust the spec edit in Step 6 to match.

- [ ] **Step 6: Reconcile the spec with what shipped**

Edit `specs/004-conversation-storage.md`:

1. In **Module and DI**, change the `commonMain.dependencies` block to:

   ```
   commonMain.dependencies {
     api(libs.androidx.room.runtime)
     implementation(libs.androidx.sqlite.bundled)
   }
   ```

   and add one line after the code block:

   > `room-runtime` is `api`, not `implementation`: `:app` holds `ChatbotDatabase` as a singleton, so `RoomDatabase` — its supertype — has to be on `:app`'s compile classpath. Room's own Android variant re-exports `androidx.sqlite`, so `sqlite-bundled` stays `implementation`.

2. In the same section, after the sentence about the `ksp<Target>` spelling, add:

   > The module also sets `-Xexpect-actual-classes`. The database constructor is an `expect object` whose `actual` is generated, and expect/actual classes are still Beta — without the flag every build carries two warnings that nothing in the project can resolve.

3. In **Schema**, after the paragraph on converters, add:

   > An unknown stored `ClaudeModel` name decodes to `ClaudeModel.Default` rather than throwing, so a model retired from the picker does not make every conversation row naming it unreadable. `Role` and `MessageStatus` throw instead: those sets are closed and written only by this app, so an unrecognised value is corruption.

4. In **Turn lifecycle**, replace "`cancel` writes the `Cancelled` row itself, then sets `Idle` and drops the entry, which keeps the rule that a row exists before the live bubble disappears." with:

   > `cancel` cancels the job and joins it; the turn coroutine writes the `Cancelled` row on its way out, in a `NonCancellable` block, then sets `Idle` and drops the entry. One writer per turn means no window in which a turn finishes normally between a liveness check and a second insert, and joining keeps the rule that a row exists before the live bubble disappears.

5. In **Turn lifecycle**, in the paragraph beginning "The map holds the latest turn per conversation", replace "so the one-turn guard tests the job rather than the entry's presence" with "so the one-turn guard tests the job and the state rather than the entry's presence — a turn that dies unexpectedly can never block a later send, and neither can one that has gone `Idle` but not yet been cleared".

- [ ] **Step 7: Review the spec edit**

Run: `git diff specs/004-conversation-storage.md`
Expected: only the five changes above. The spec must still read as a description of the current system — no history, no "changed from".

- [ ] **Step 8: Mark the roadmap**

In `docs/roadmap.md`, leave the **Status** table alone (M1 is not finished) but confirm the `004-conversation-storage.md` row in the M1 table still describes what shipped: "Room schema: conversations + messages (reminders/memories tables deferred to their specs)". It also now carries the `ConversationRepository` and the turn lifecycle, so change that cell to:

> Room schema and `ConversationRepository`: conversations + messages, and the turn that streams a reply and persists it (reminders/memories tables deferred to their specs)

- [ ] **Step 9: Run the full gate**

Run: `./gradlew :shared:testAndroidHostTest :app:assembleDebug && ./gradlew spotlessApply && ./gradlew spotlessCheck build`
Expected: `BUILD SUCCESSFUL`, the whole `:shared` suite green and every other module unaffected.

- [ ] **Step 10: Report**

Leave all changes in the working tree — do not commit. Report: total tests passing, whether `api(libs.androidx.room.runtime)` was enough or androidMain needed it too, whether `DatabaseModule` stayed in `:app`, and the path of the committed schema baseline.

---

## Deferred (do not build here)

| Piece | Owner |
|---|---|
| Chat UI, `ChatViewModel`, streaming rendering, model picker, journey XMLs for persist/resume/delete | 005 |
| Dev-key `ApiKeyProvider` stub, which is what makes the `ConversationRepository` binding injectable | 005 |
| A mechanism for sharing `FakeConversationRepository` with feature modules (`commonTest` classes are invisible across Gradle projects and Kotlin has no KMP test fixtures) | 005 |
| `ContentBlock.ToolUse` / `ToolResult` persistence — new `@Serializable` subtypes in the existing JSON column, no schema change | 008 |
| Memory injection into `ChatRequest.system`, which is null here | 009 |
| Memories and reminders tables, added to `ChatbotDatabase` with an `@AutoMigration` and their own repository factories | 009, 010 |
| `@AutoMigration` and `MigrationTestHelper` tests | whichever spec first bumps the version |
| Conversation rename and search — the schema supports both, no UI is specced | future |
| AI-generated titles replacing the truncated first message | future |
| Stored error detail on failed rows | future |
| Full-text search as a separate FTS table | future |
| `iosMain` database builder via `NSFileManager`, plus per-target `ksp` lines | future |
