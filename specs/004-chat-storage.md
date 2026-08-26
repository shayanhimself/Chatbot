# 004: Chat Storage

Room persistence for chats and messages, plus the `ChatRepository` that owns a chat turn end to end: append the user message, stream the reply through `ClaudeEngine` (003), persist the result. Lives in `:shared` commonMain (data layer); the Android file path is the only platform-specific piece.

Room is the single source of truth for chat history, the error that ended a turn included. The one exception is the in-flight assistant message, which stays in memory as `TurnState` until the stream ends, never a write per token.

Scope is chats and messages. Memories (009) and reminders (010) add their own tables to the same database. UI and ViewModel are 005; the deferrals are listed at the bottom.

## Schema

Version 2, two tables. Entities, DAOs, and converters are `internal` to `:shared`, so nothing above the data layer can reach a DAO.

```
@Entity(tableName = "chats")
internal data class ChatEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val model: ClaudeModel,
  val createdAt: Long,
  val updatedAt: Long,
)

@Entity(
  tableName = "messages",
  foreignKeys = [ForeignKey(
    entity = ChatEntity::class,
    parentColumns = ["id"],
    childColumns = ["chatId"],
    onDelete = ForeignKey.CASCADE,
  )],
  indices = [Index("chatId")],
)
internal data class MessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val chatId: Long,
  val role: Role,
  val content: List<ContentBlock>,
  val status: MessageStatus,
  val error: ApiError? = null,
  val createdAt: Long,
)

enum class MessageStatus { Complete, Failed, Cancelled }
```

Messages order by `id`, not `createdAt`: autoincrement is monotonic, so it is insertion order without ties. Timestamps are for display. Chats order by `updatedAt DESC`, bumped on every message insert.

Deleting a chat cascades to its messages. The index on `chatId` serves both that cascade and every per-chat read.

**Content is a JSON column.** A `List<ContentBlock>` converter stores the block list as text, which requires `@Serializable` on `ContentBlock`. 008 adds `ToolUse` and `ToolResult` as new subtypes with no schema change. Message content is never queried by SQL; full-text search, if it ever exists, is a separate FTS table.

The codec is the storage layer's own `Json`, not 003's `claudeJson`. Sharing one instance would put the on-disk format at the mercy of settings that exist for HTTP reasons, and a stored value that no longer decodes would be coerced to a default rather than failing loudly. Nothing on the wire path serializes `ContentBlock` directly, so the two formats are free to diverge.

Four converters: `ClaudeModel`, the `Role`/`MessageStatus` enums, the content list, and the error. Timestamps are epoch millis in the database and `kotlin.time.Instant` in domain models; a `kotlin.time.Clock` is injected so tests assert exact values.

An unknown stored `ClaudeModel` name decodes to `ClaudeModel.Default` rather than throwing, so a model retired from the picker does not make every chat row naming it unreadable. `Role` and `MessageStatus` throw instead: those sets are closed and written only by this app, so an unrecognised value is corruption.

**A failed row stores the error that ended it**, in a nullable JSON column of its own, which requires `@Serializable` on `ApiError` with a `@SerialName` per case so a renamed case leaves stored rows readable. `status` stays the queryable flag and `error` the reason: the history query matches on `status`, and nothing queries a JSON column. An error whose case no longer exists decodes to `ApiError.Unexpected` rather than throwing, for the reason the model converter falls back: the reply is worth more than the reason it stopped. Rows written before the column have no error, so a chat that failed before the upgrade offers no retry.

Two queries carry the feature: the chat list, and the history sent to Claude:

```
SELECT * FROM messages
WHERE chatId = :id AND status = 'Complete'
ORDER BY id
```

That `WHERE` is why `status` exists: a failed or cancelled turn is structurally incapable of reaching the API.

## Domain models

The repository exposes these; entities never leave the data layer.

```
data class Chat(id, title, model, createdAt, updatedAt)
data class Message(id, chatId, role, content, status, error, createdAt)

sealed interface TurnState {
  data object Idle : TurnState
  data class Streaming(val text: String) : TurnState
}
```

`Streaming` carries cumulative text, not the latest delta, so a collector renders it directly and accumulates nothing.

## Contract

```
interface ChatRepository {
  fun getChatsFlow(): Flow<List<Chat>>
  fun getMessagesFlow(chatId: Long): Flow<List<Message>>
  fun getTurnFlow(chatId: Long): Flow<TurnState>

  suspend fun send(
    chatId: Long?,
    text: String,
    model: ClaudeModel = ClaudeModel.Default,
  ): Long

  suspend fun retry(chatId: Long)
  suspend fun cancel(chatId: Long)
  suspend fun setModel(chatId: Long, model: ClaudeModel)
  suspend fun delete(chatId: Long)
}
```

`send` returns the chat id immediately and does not stream. A null `chatId` means the first message of a new chat: the repository creates the chat and the user message in one transaction and returns the new id. `model` is used only on creation; afterwards the chat's own column decides.

A chat row exists only once its first message is sent, so an abandoned empty chat never appears in the list. `title` is that first message truncated to `MAX_TITLE_LENGTH` (60).

## Turn lifecycle

```
class DefaultChatRepository(
  private val engine: ClaudeEngine,
  private val chatDao: ChatDao,
  private val messageDao: MessageDao,
  private val externalScope: CoroutineScope,
  private val clock: Clock,
)
```

The stream runs in `externalScope`, never the caller's scope. A turn therefore outlives the screen that started it: navigating away mid-reply still persists the assistant message, and returning re-attaches to the turn already in flight. Process death loses the turn; the user message remains, unanswered.

```
send(null, "plan a trip")
  ├─ transaction: insert chat + user message (status = Complete)
  ├─ return id ─────────────────────────────────► caller continues
  └─ externalScope.launch {
       history = SELECT ... status = 'Complete' ORDER BY id  → List<ClaudeMessage>
       engine.stream(ClaudeMessageRequest(history, chat.model, system = null))
         Delta     → TurnState.Streaming(cumulative)
         Completed → insert assistant row (Complete); bump updatedAt; TurnState.Idle
         Failed    → insert assistant row (Failed, partial text, error); TurnState.Idle
     }
```

The insert completes before `TurnState` returns to `Idle`, so the live bubble is never dropped before the persisted row exists to replace it.

`system` is null here; 009 fills it with memories.

**One turn per chat.** `send` on a chat with a live turn throws `IllegalStateException`. 005 disables the composer while streaming, so this is a guard against a programming error, not a user-facing state.

`cancel` cancels the job and writes the partial text as `Cancelled`. `retry` deletes the trailing `Failed` or `Cancelled` assistant row and re-runs the turn; since the history query never saw that row, nothing else needs undoing. Both are no-ops when there is nothing to cancel or retry.

A turn that completes without emitting any text still stores its assistant row, but blank text blocks are dropped on the way into a request and a message left with no blocks is dropped whole. The API rejects an empty text block, and a stored one would otherwise be replayed on every later turn, making the chat permanently un-sendable. Filtering on the read side rather than skipping the write keeps the row for the UI.

Turns live in a `MutableStateFlow<Map<Long, Turn>>`: an immutable map behind a `StateFlow` so `getTurnFlow` reads without a lock and observes turns that start after collection begins:

```
getTurnFlow(id) = turns
  .flatMapLatest { it[id]?.state ?: flowOf(TurnState.Idle) }
  .distinctUntilChanged()
```

Writes take a `Mutex`. `StateFlow.update` makes one compare-and-set atomic, but the guard spans two operations (test for a live turn, then insert one) and without the lock two concurrent sends both see none and both launch.

**The map holds the latest turn per chat.** A turn is live while its job is active, so the one-turn guard tests the job and the state rather than the entry's presence: a turn that dies unexpectedly can never block a later send, and neither can one that has gone `Idle` but not yet been cleared. Every turn ends the same way, whether it completed or failed: the row goes in, the state goes `Idle`, the entry is dropped. Nothing has to outlive the turn, because the row carries the reason. `cancel` cancels the job and joins it; the turn coroutine writes the `Cancelled` row on its way out, in a `NonCancellable` block, then sets `Idle` and drops the entry. One writer per turn means no window in which a turn finishes normally between a liveness check and a second insert, and joining keeps the rule that a row exists before the live bubble disappears.

## Module and DI

`:shared` assembles everything; `:app` registers it with Hilt, mirroring 003.

Build additions: `androidx.sqlite:sqlite-bundled`, the `androidx.room` Gradle plugin, and KSP.

```
plugins { alias(libs.plugins.ksp); alias(libs.plugins.androidx.room) }

commonMain.dependencies {
  api(libs.androidx.room.runtime)
  implementation(libs.androidx.sqlite.bundled)
}

dependencies { add("kspAndroid", libs.androidx.room.compiler) }

room { schemaDirectory("$projectDir/schemas") }
```

`room-runtime` is `api`, not `implementation`: `:app` holds `ChatbotDatabase` as a singleton, so `RoomDatabase`, its supertype, has to be on `:app`'s compile classpath. Room's own Android variant re-exports `androidx.sqlite`, so `sqlite-bundled` stays `implementation`.

KSP configurations are per target and are created after the `kotlin {}` block evaluates, so `add("ksp<Target>", …)` is the only available spelling: there is no typed accessor, and KSP 2 deprecates the catch-all `ksp(…)`. iOS targets add their own lines. The build script carries a comment saying so, since the spelling looks like an oversight otherwise.

The module also sets `-Xexpect-actual-classes`. The database constructor is an `expect object` whose `actual` is generated, and expect/actual classes are still Beta, and without the flag every build carries two warnings that nothing in the project can resolve.

```
@Database(entities = [ChatEntity::class, MessageEntity::class], version = 1)
@TypeConverters(ChatbotConverters::class)
@ConstructedBy(ChatbotDatabaseConstructor::class)
abstract class ChatbotDatabase : RoomDatabase() {
  internal abstract fun chatDao(): ChatDao
  internal abstract fun messageDao(): MessageDao
}

@Suppress("KotlinNoActualForExpect")
expect object ChatbotDatabaseConstructor : RoomDatabaseConstructor<ChatbotDatabase> {
  override fun initialize(): ChatbotDatabase
}
```

Room's KSP processor generates the `actual`. This should be mentioned in the comments.
Two factories are the module's assembly seam:

```
// androidMain: the file path is the only platform-specific piece
fun chatbotDatabaseBuilder(context: Context): RoomDatabase.Builder<ChatbotDatabase>

// commonMain
fun createChatbotDatabase(
  builder: RoomDatabase.Builder<ChatbotDatabase>,
  driver: SQLiteDriver = BundledSQLiteDriver(),
  queryContext: CoroutineContext = Dispatchers.IO,
): ChatbotDatabase

fun createChatRepository(
  database: ChatbotDatabase,
  engine: ClaudeEngine,
  externalScope: CoroutineScope,
  clock: Clock = Clock.System,
): ChatRepository
```

The driver and query context are parameters rather than hardcoded calls, so tests supply their own.

The module's public surface is the repository interface, the domain models, `ChatbotDatabase` as an opaque handle, and those factories. `DefaultChatRepository`, the DAOs, and the entities stay `internal`; the factory reaches the DAO accessors because it lives in the same module.

`:app` contributes one Hilt module providing the database, the repository, and an `@ApplicationScope` `CoroutineScope` (`SupervisorJob() + Dispatchers.Default`) as `externalScope`. That scope is provided from `:app` because Hilt never enters `:shared`, and it is the mechanism behind a turn outliving its screen.

## Migrations

`exportSchema` is on and every version's `shared/schemas/….json` is committed, both as the baseline the next migration diffs against and as the fixture its test builds the old database from. Every version bump ships an `@AutoMigration` and a migration test, added by the spec that changes the schema. Adding a nullable column (version 2's `error`) or a table (009's memories, 010's reminders) is auto-migratable in one line. Destructive fallback is never enabled.

Room's own `MigrationTestHelper` reads those files from an APK's assets; a host test has no APK to package them into, and putting them there would ship the schema history in the app. `createDatabaseAtVersion` builds the old database from the committed JSON instead: its `createSql` statements, its `setupQueries` for the identity hash Room validates the migrated schema against, and `PRAGMA user_version` so the open migrates.

## Testing

TDD, real objects over fakes, no mocking library.

**Anything needing a real database lives in `androidHostTest`, under Robolectric with `AndroidSQLiteDriver`.** `sqlite-bundled` ships host natives only in its `jvm` variant, and an Android consumer resolves the `android` one, whose `.so` files AGP never puts on the unit-test classpath, where `BundledSQLiteDriver` dies with `UnsatisfiedLinkError`. It stays the production driver, where the APK does carry those natives. Robolectric supplies the host `android.database.sqlite` implementation and is an Android-only dependency, so this is also the only source set it can live in.

`commonTest` is not an option while `:shared` targets Android alone: common code then resolves Room's Android overloads, and building a database there would need a `Context`. Tests that touch no database, the fakes and their own tests, still belong in `commonTest`.

Swapping the driver is why `createChatbotDatabase` takes it as a parameter.

- **DAO**: cascade delete; `id` and `updatedAt DESC` ordering; the history query returning only `Complete` rows; every converter round-tripping a multi-block content list. The history query's `status = 'Complete'` literal is coupled to an enum constant name with nothing in the compiler to check it, so one test inserts a non-`Complete` row and asserts it is excluded.
- **Repository**: real in-memory database plus a fake engine, so only the network is faked. Creation from a null id with a truncated title; append to an existing chat; `Completed` persisting a `Complete` row and bumping `updatedAt`, with the row queryable before `Idle` is emitted; `Failed` persisting partial text and the error that ended it; the next send excluding that row from history; `cancel` and `retry`; a second concurrent `send` rejected; `getTurnFlow` attaching mid-stream.
- **Engine doubles**: 003's `FakeScriptedClaudeEngine` emits a scripted list eagerly, which cannot express an assertion taken mid-stream. `FakeManualClaudeEngine` alongside it opens a stream the test feeds event by event and closes when done; the three tests that attach mid-flight, assert mid-stream, or cancel a collector need it.
- **Turn lifetime**: collect `getTurnFlow`, cancel the collecting scope mid-stream, assert the assistant row still lands. This requires a `TestScope` for collection separate from `externalScope`, and it is the assertion the whole turn design exists to satisfy.
- **`FakeChatRepository`** in commonTest, in-memory, reproducing the same lifetime semantics, the standard dependency for 005's ViewModel tests.

A fake `Clock` returning fixed instants keeps timestamp assertions exact.

No journey XMLs: 004 has no UI. Persist, resume, and delete are proven end to end by 005's journeys at the M1 exit gate.

## Deferred to later specs

| Piece | Owner | What it adds |
|---|---|---|
| Chat UI and ViewModel | 005 | Collects `getMessagesFlow` and `getTurnFlow`, folds them into `UiState`, renders streaming text, calls `send` / `retry` / `cancel` / `setModel` / `delete`. Owns the journey XMLs for persist, resume, and delete |
| `getChatFlow(id)` and `Chat.snippet` | 005 | The chat screen needs a single-chat read for its title, and the list needs a second line. `snippet` is a correlated subquery for the last `Complete` message on the query that already backs the list, so neither addition changes the schema |
| Chat rename, search | future | The schema supports both; no UI is specced |
| AI-generated titles | future | A cheap model call summarizing the first exchange, replacing the truncated first message |
| `ContentBlock.ToolUse` / `ToolResult` persistence | 008 | New `@Serializable` subtypes in the existing JSON column; no schema change. The agentic loop drives multiple engine turns per user message |
| Memory injection into `system` | 009 | The request-assembly seam already exists; `system` is null here |
| Memories and reminders tables | 009, 010 | Added to `ChatbotDatabase` with an `@AutoMigration` and their own repository factories |
| Sharing test fakes across modules | 005 | `commonTest` classes are invisible to other Gradle projects and Kotlin has no KMP test fixtures, so a feature module cannot yet consume `FakeChatRepository`. 005 owns the mechanism, since it is the first module that needs one |
| iOS database builder | future | An `iosMain` path via `NSFileManager`, plus per-target `ksp` lines. The schema, DAOs, and repository are unchanged |
