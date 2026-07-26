# 004 Conversation Storage — Spike Findings

Empirical results from throwaway spikes run against the real project on branch `m1/004-database`, to settle the open questions in `specs/004-conversation-storage.md` before an implementation plan is written. **All spike code was reverted; the working tree is clean.** Nothing here is committed except this document.

Every claim below was produced by running the build, not by reasoning about the libraries. Where a claim is inference rather than an observed result, it says so.

`specs/004-conversation-storage.md` and `specs/001-tech-stack.md` have already been corrected from these findings, so the specs are canonical and this document is the evidence and decision trail behind them. Read it for *why* a spec says what it says, and for the parts (§9) nothing has proven yet.

---

## Summary of what is now settled

| Question the spec left open | Answer | Evidence |
|---|---|---|
| `BundledSQLiteDriver` on the JVM host test classpath? | **No.** Fails at runtime. | §1 |
| DAO/repository tests in `commonTest` or `androidHostTest`? | **`androidHostTest`**, Robolectric + `AndroidSQLiteDriver`. | §1, §2 |
| Room KMP + KSP + Room Gradle plugin wiring on `com.android.kotlin.multiplatform.library`? | Works as specced; `add("kspAndroid", …)` is correct. | §3 |
| `@ConstructedBy` / `expect object` constructor? | Works; KSP generates the `actual`. One Beta warning, suppressible. | §3, §7 |
| Schema export? | Works; lands at `shared/schemas/<db FQCN>/1.json`. | §4 |
| `@Transaction` in an abstract-class DAO, FK cascade, enum query params, `status = 'Complete'` literal? | All four work. | §5 |
| Which `Clock` / `Instant` type? | `kotlin.time.Clock` / `kotlin.time.Instant`, no opt-in needed. | §6 |
| `queryContext` may be `EmptyCoroutineContext`? | **No.** Room rejects it. | §2 |

The spikes also surfaced five loose ends that are **not** open questions — each has a decision recorded at the point it is raised, marked **Decided** or **No action needed**: the persistence codec (§8), the streaming test double (§8), cross-module fake sharing (§8), `:app`'s missing `ApiKeyProvider` binding (§8), and the KT-61573 compiler flag (§7). §9 lists what remains unproven.

---

## 1. `sqlite-bundled` does not work in JVM host tests

The spec framed the test source set as a fork "depending on whether `sqlite-bundled` resolves host-loadable natives on the JVM test classpath". It does not.

**Artifact inspection.** `androidx.sqlite:sqlite-bundled` is a KMP artifact; an Android consumer resolves the `-android` variant. That AAR contains only Android ABI natives:

```
$ unzip -l sqlite-bundled-android-2.7.0.aar
  jni/arm64-v8a/libsqliteJni.so
  jni/armeabi-v7a/libsqliteJni.so
  jni/x86/libsqliteJni.so
  jni/x86_64/libsqliteJni.so
```

The host natives exist only in the `-jvm` variant, which an Android consumer never resolves:

```
$ unzip -l sqlite-bundled-jvm-2.6.2.jar
  natives/linux_arm64/libsqliteJni.so
  natives/linux_x64/libsqliteJni.so
  natives/osx_arm64/libsqliteJni.dylib
  natives/osx_x64/libsqliteJni.dylib
  natives/windows_x64/sqliteJni.dll
```

AGP does not put AAR `jni/` payloads on the unit-test classpath, so `System.loadLibrary("sqliteJni")` has nothing to find.

**Confirmed by running it.** A host test that built a Room database with `BundledSQLiteDriver()` and executed one query produced:

```
java.lang.UnsatisfiedLinkError: no sqliteJni in java.library.path: /Users/shayan/Library/Java/Extensions:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java:.:/Users/shayan/Development/Projects/Chatbot/shared/src/androidHostTest/jniLibs
```

**Not attempted, and deliberately so:** forcing `sqlite-bundled-jvm` onto the host test classpath alongside the Android variant. Both variants ship the same FQCN `androidx.sqlite.driver.bundled.BundledSQLiteDriver` with different native-loading strategies, so which one wins depends on classpath order. Rejected as unreliable rather than tested.

`BundledSQLiteDriver` remains correct for production on device — the APK does get the `jni/` payload. Only host tests need a different driver.

## 2. `AndroidSQLiteDriver` under Robolectric works

Same database, same converters, driver swapped:

```kotlin
@RunWith(RobolectricTestRunner::class)
class SpikeDaoTest {
    private fun TestScope.db(): ChatbotDatabase =
        createChatbotDatabase(
            builder = Room.inMemoryDatabaseBuilder<ChatbotDatabase>(ApplicationProvider.getApplicationContext()),
            driver = AndroidSQLiteDriver(),
            queryContext = StandardTestDispatcher(testScheduler),
        )
}
```

Result: 3 of 3 tests passed (`shared/build/test-results/testAndroidHostTest/TEST-…SpikeDaoTest.xml`, `tests="3" failures="0" errors="0"`). Exercised in that run: `@Transaction` DAO methods, foreign-key cascade, `Flow` DAO queries, enum converters, and the `List<ContentBlock>` JSON converter.

This is why `createChatbotDatabase` taking the driver as a parameter is load-bearing, exactly as the spec says — it is the seam the tests use.

**`queryContext` must contain a dispatcher.** Passing `EmptyCoroutineContext` fails:

```
java.lang.IllegalArgumentException: It is required that the coroutine context contain a dispatcher.
	at androidx.room.RoomDatabase$Builder.setQueryCoroutineContext(RoomDatabase.android.kt:1645)
```

Tests must pass a real dispatcher; `StandardTestDispatcher(testScheduler)` works and keeps the database on `runTest`'s virtual clock.

**`commonTest` cannot build a database at all.** Because `:shared` declares only `androidTarget()`, common source sets compile into the Android compilation, so Room's Android overloads are what resolve in common code. A `commonTest` call to `Room.inMemoryDatabaseBuilder<SpikeDatabase>()` failed to compile:

```
e: SpikeCommonTest.kt:10:81 No value passed for parameter 'context'.
e: SpikeCommonTest.kt:10:81 No value passed for parameter 'klass'.
```

So the choice is not stylistic. Any test that needs a real database needs a `Context`, which means `androidHostTest`. Tests that need no database (`FakeConversationRepository` and its test) can stay in `commonTest`.

Note for whoever writes the plan: for this module, `commonTest` and `androidHostTest` sources compile into the *same* compilation and run under the same `:shared:testAndroidHostTest` task — the earlier compile error was reported by `compileAndroidHostTest`. Placement is therefore about future iOS portability and Robolectric availability, not about which task runs the test. `androidHostTest` test code can see `commonTest` declarations (`ManualChatEngine`, `FakeChatEngine`, fakes) directly.

## 3. Build wiring that was verified working

Version catalog additions:

```toml
[versions]
sqlite = "2.6.2"

[libraries]
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "sqlite" }

[plugins]
androidx-room = { id = "androidx.room", version.ref = "room" }
```

`room = "2.8.4"` is already pinned. `sqlite = "2.6.2"` is the version `room-runtime:2.8.4` itself requires — read out of its Gradle module metadata, which declares `androidx.sqlite:sqlite` and `androidx.sqlite:sqlite-framework` at `2.6.2`. `sqlite-bundled` 2.7.0 exists but would drag the sqlite core above what Room 2.8.4 declares; 2.6.2 keeps the group aligned.

`shared/build.gradle.kts` additions (all exercised):

```kotlin
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    android {
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
```

- `add("kspAndroid", …)` is correct for this module. The task that runs is `:shared:kspAndroidMain`. There is no typed accessor, as the spec says.
- The `androidHostTest` source set has no typed accessor either; `getByName("androidHostTest")` is the spelling that works.
- `isIncludeAndroidResources = true` was set alongside Robolectric and the run was green. It was **not** independently tested for necessity — nobody checked whether Robolectric passes without it.

Robolectric also needs, mirroring `:core:ui`:

```kotlin
// Robolectric needs a Java 21 runtime for SDK 36; compile toolchain stays 17.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        },
    )
}
```

and the SDK pin at `shared/src/androidHostTest/resources/robolectric.properties`:

```
sdk=36
```

That path is the one that worked for this module — note it is `androidHostTest/resources`, not the `src/test/resources` used by the non-KMP modules.

## 4. Schema export works

After a build, `room { schemaDirectory("$projectDir/schemas") }` produced `shared/schemas/<database FQCN>/1.json` via a `:shared:copyRoomSchemas` task. Parsed contents for the spike's two tables:

```
conversations  id INTEGER notNull, title TEXT, model TEXT, createdAt INTEGER, updatedAt INTEGER
messages       id INTEGER notNull, conversationId INTEGER, role TEXT, content TEXT, status TEXT, createdAt INTEGER
  foreignKeys  [{table: conversations, onDelete: CASCADE, columns: [conversationId], referencedColumns: [id]}]
  indices      [index_messages_conversationId]
```

So the spec's schema, converters, and index all survive the round trip into the exported baseline.

Caveat: `copyRoomSchemas` reports `NO-SOURCE` when `compileAndroidMain` is up to date. A clean/`--rerun-tasks` build regenerates it. Worth knowing so nobody concludes export is broken.

## 5. DAO shapes that were verified

The following compiled and passed. This is the riskiest part of the spec's design and it holds up.

**Abstract-class DAO with `@Transaction` methods.** Room accepts `open suspend` methods with bodies on an abstract-class DAO, and a single DAO may declare `@Insert` for more than one entity — which is what makes the spec's "insert conversation + user message in one transaction" expressible without reaching for `useWriterConnection`:

```kotlin
@Dao
internal abstract class ConversationDao {
    @Insert abstract suspend fun insert(conversation: ConversationEntity): Long
    @Insert abstract suspend fun insertMessage(message: MessageEntity): Long

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    abstract suspend fun touch(id: Long, updatedAt: Long)

    @Transaction
    open suspend fun createWithFirstMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
    ): Long {
        val conversationId = insert(conversation)
        insertMessage(message.copy(conversationId = conversationId))
        return conversationId
    }

    @Transaction
    open suspend fun appendMessage(message: MessageEntity, updatedAt: Long): Long {
        val id = insertMessage(message)
        touch(message.conversationId, updatedAt)
        return id
    }
}
```

`appendMessage` is a convenient place to make "bump `updatedAt` on every message insert" structural rather than a rule callers must remember.

**Foreign-key cascade is on.** After `dao.delete(conversationId)`, `SELECT COUNT(*) FROM messages` returned 0. Room enables `PRAGMA foreign_keys` itself; nothing extra was configured.

**Enums work as query parameters** through the type converter:

```kotlin
@Query("UPDATE conversations SET model = :model WHERE id = :id")
abstract suspend fun setModel(id: Long, model: ClaudeModel)
```

Round-tripped `ClaudeModel.Opus` correctly.

**The `status = 'Complete'` literal matches converter output**, given a converter that stores `MessageStatus.name`:

```kotlin
@Query("SELECT * FROM messages WHERE conversationId = :conversationId AND status = 'Complete' ORDER BY id")
suspend fun completeForConversation(conversationId: Long): List<MessageEntity>
```

Returned only the `Complete` row when the conversation also held a `Failed` one. This coupling between a SQL string literal and an enum constant name is invisible to the compiler — worth a test that inserts a non-`Complete` row and asserts it is excluded, which is what the spec already asks for.

**`Flow` DAO queries** emitted correctly under Robolectric with `StandardTestDispatcher`, and `ORDER BY updatedAt DESC` reordered as expected after `touch`.

**Converters used.** `ClaudeModel`, `Role`, and `MessageStatus` stored as `name`; content stored as JSON. The spike's model converter decoded defensively:

```kotlin
@TypeConverter
fun toModel(value: String): ClaudeModel =
    ClaudeModel.entries.firstOrNull { it.name == value } ?: ClaudeModel.Default
```

This is a spike decision, not a spec requirement — the plan author should decide whether an unknown stored model name falls back or throws. The argument for falling back: a model dropped from the picker in a later version would otherwise make every conversation row that names it unreadable.

## 6. `Clock` and `Instant` are `kotlin.time`, not `kotlinx.datetime`

The spec says "a `Clock` is injected" and "`Instant` in domain models" without naming the package. Under Kotlin 2.4.10 this compiled with **no opt-in and no warning**:

```kotlin
import kotlin.time.Clock
import kotlin.time.Instant

internal fun spikeNow(clock: Clock = Clock.System): Instant = clock.now()
internal fun spikeMillis(clock: Clock = Clock.System): Long = clock.now().toEpochMilliseconds()
internal fun spikeFromMillis(millis: Long): Instant = Instant.fromEpochMilliseconds(millis)
```

So domain models take `kotlin.time.Instant` and the repository takes `kotlin.time.Clock` with `Clock.System` as the default. `kotlinx-datetime` stays a dependency for reminder-domain work but is not what this feature's timestamps hang off.

Not verified: implementing `kotlin.time.Clock` in a test fake (`class FakeClock(var instant: Instant) : Clock { override fun now() = instant }`). Only call sites were compiled, not an implementation. Expected to work — it is an ordinary interface — but it is inference, not a result.

## 7. Known warnings

Two warnings appeared and neither is fatal:

```
w: …/ksp/android/androidMain/…/ChatbotDatabaseConstructor.kt:5:8 'expect'/'actual' classes … are in Beta. Consider using the '-Xexpect-actual-classes' flag …
w: …/Spike.kt:157:1 'expect'/'actual' classes … are in Beta. …
```

The `@Suppress("KotlinNoActualForExpect")` the spec puts on the `expect object` is still needed — it suppresses a *different* diagnostic (no `actual` visible in source, because KSP generates it). These two are the separate KT-61573 Beta warning, on both the hand-written `expect` and the generated `actual`.

Silencing them would take a module-level compiler arg:

```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
```

**This was not tested.** It is the standard remedy for KT-61573 but nobody ran it here.

**Decided — add the flag.** Without it every build of `:shared` carries two warnings that will never go away on their own, which trains people to ignore warnings. If the DSL spelling above turns out to be wrong the build fails loudly and is cheap to fix.

## 8. Facts confirmed about existing code that 004 builds on

- **`ContentBlock` needs `@Serializable` added**, as the spec says. Applied during the spike as:

  ```kotlin
  @Serializable
  sealed interface ContentBlock {
      @Serializable
      @SerialName("text")
      data class Text(val text: String) : ContentBlock
  }
  ```

  Serialized shape is `{"type":"text","text":"…"}` with kotlinx's default `type` discriminator.

- **Adding `@Serializable` does not disturb the wire format.** `shared/chat/dto/MessageRequestDto.kt` maps `ContentBlock` onto a separate `ContentBlockDto` for the request body; nothing on the HTTP path serializes `ContentBlock` directly. Persistence format and wire format are therefore independent.

  **Decided — the storage layer gets its own `Json`, deviating from the spec.** The spec says the converter should use 003's `chatJson`. That instance is tuned for the wire (`encodeDefaults`, `explicitNulls = false`, `coerceInputValues`, plus SSE fallback deserializers), so reusing it makes the on-disk format hostage to codec changes made for HTTP reasons, with no migration to catch them — and `coerceInputValues` on disk turns a corrupt stored value into a default instead of failing loudly. The persistence codec is therefore separate, owned by the storage layer, with durability settings (`ignoreUnknownKeys = true`, explicit `classDiscriminator`). The spike used a plain `Json {}` and round-tripped fine. The plan carries this in a Deviations section and a spec-correction task, mirroring how 003's plan handled its deviations.

- **`chatJson` is `internal` to `:shared`**, in package `com.shayanaryan.chatbot.shared.chat` — reachable from a converter elsewhere in the module.

- **`:app` has no `ApiKeyProvider` binding yet** (`ChatModule.provideChatEngine` depends on one; 005 ships a debug stub, 006 the real one). Dagger does not error on an unsatisfiable `@Provides` that nothing injects, which is why `:app` compiles today. A `ConversationRepository` provider will transitively depend on `ChatEngine` → `ApiKeyProvider`, so it stays fine **only while nothing injects the repository**. The first `@Inject` of it — which is 005's ViewModel — is what will force the key-provider binding to exist.

  **No action needed.** The roadmap already assigns the debug dev-key stub to 005 and the real provider to 006. 004's verification step is simply "`:app` still assembles".

- **Existing `FakeChatEngine`** (`shared/src/commonTest/…/chat/FakeChatEngine.kt`) emits a fixed `events` list eagerly on collection and records requests. It has no way to hold a stream open or release events one at a time, so it cannot express "assert state mid-stream", "attach to a turn already in flight", or "cancel the collector while the turn is live" — three of the spec's required repository tests. Not built during the spike.

  **Decided — add a second double, leave `FakeChatEngine` untouched.** `ManualChatEngine` in `:shared` commonTest beside it, channel-backed (`send(event)` / `close()`), so a test opens the stream, feeds it event by event, and closes it. 003's spec documents `FakeChatEngine`'s contract; 004 has no business rewriting it, and one fake per streaming style reads better than one dual-mode fake.

- **Cross-module fake sharing is unsolved, and stays that way.** `commonTest` classes are not visible to other Gradle modules, so `FakeConversationRepository` in `:shared` commonTest is not consumable by `:feature:conversation` tests, which is what the spec promises for 005. The same latent gap already exists for `FakeChatEngine`. Kotlin has no first-class KMP test fixtures (KT-70233 is open), so the fix is a real module — a `:core:testing` holding fakes in its *main* source set, depending on `:shared`, added to feature modules as `testImplementation` (Google's nowinandroid pattern).

  **Decided — 004 does not solve this.** The fake goes in `:shared` commonTest exactly as spec 004 says. 005 builds the sharing mechanism when it has an actual consumer; the cost of deferring is moving one file. Creating the module now would mean shipping infrastructure nothing uses and adding a row to the `architecture` skill's module table for a need 004 does not have.

## 9. What was not spiked

Listed so the plan author knows the boundary of what is proven:

- `DefaultConversationRepository` in any form — the turn map, the `Mutex`, `getTurnFlow`'s `flatMapLatest`, cancellation, retry, the external-scope lifetime guarantee. None of it was built. The spike stopped at the DAO layer.
- `@AutoMigration` and `MigrationTestHelper` — not applicable at version 1, and not exercised.
- Any `:app` Hilt wiring, and any `@ApplicationScope` `CoroutineScope` provider.
- Instrumented (device) tests. Only host tests were run.
- Whether `isIncludeAndroidResources = true` is actually required (see §3).
- The `-Xexpect-actual-classes` compiler arg (see §7).
- A `FakeClock` implementing `kotlin.time.Clock` (see §6).

## 10. How to reproduce

Commands used, from the repo root:

```bash
./gradlew :shared:testAndroidHostTest --console=plain --tests '*SpikeDaoTest*'
./gradlew :shared:compileAndroidMain --console=plain
```

Test results are XML at `shared/build/test-results/testAndroidHostTest/`; Gradle's console output does not show the per-test detail, so read the XML (it also captures `println` output in `<system-out>`, which is how the `UnsatisfiedLinkError` above was recovered from a deliberately non-failing probe test).
