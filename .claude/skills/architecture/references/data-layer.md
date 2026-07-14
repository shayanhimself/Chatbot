# Data layer

Lives in `:shared` commonMain (repositories, data sources, Room, Ktor client,
domain models). Platform `actual`s in `:shared` androidMain.

## Repositories

One repository per data type, interface + implementation:

```kotlin
// commonMain — interface is what everything else depends on
interface ReminderRepository {
    fun getRemindersFlow(): Flow<List<Reminder>>
    suspend fun schedule(reminder: Reminder)
    suspend fun cancel(id: Long)
}

class DefaultReminderRepository(
    private val reminderDao: ReminderDao,
    private val alarmScheduler: AlarmScheduler,   // interface; impl injected from :app
) : ReminderRepository { /* ... */ }
```

- Repositories are the **only** entry to the data layer. Nothing above them
  touches a DAO, DataStore, or the Ktor client.
- Expose `Flow<T>` for data that changes over time, `suspend fun` for one-shot
  operations. Never expose mutable types.
- Plain constructor injection — no Hilt annotations in `:shared`. A Hilt module
  in `:app` (or the owning feature) binds interface → implementation.
- Repositories contain business logic and resolve any multi-source questions.
  A repository may depend on other repositories when aggregation demands it.
- When a repository would only proxy a single DAO with zero logic, inject the DAO
  into the repository anyway (skip a separate data source class) — split a data
  source out when a second source or real logic appears.

## Single source of truth

Room is the SSOT for all persisted data. The app is local-only: no remote sync,
no cache-vs-server conflicts. Flows come from Room (`dao.observeX()`), writes go
through the repository into Room, and the UI updates reactively off the DB.

The Claude API is not a data source of record — responses become app data only
when the repository persists them. The in-flight streaming message is transient
in-memory state (a `StateFlow` in the engine/repository); persist once on
`message_stop` or tool-use pause, never per token.

## Storage choice

| Data | Store |
|---|---|
| Conversations, messages, reminders, memories | Room (queryable, relational) |
| Settings, selected model | DataStore Preferences |
| Claude API key | Keystore master key → Tink AEAD → ciphertext in DataStore. **EncryptedSharedPreferences is deprecated — never use.** Plaintext key exists only in memory; never logged, never in Room |

## KMP rules

- commonMain: no `android.*`, no Hilt, no user-visible strings. Errors are typed
  (`sealed class ChatError`), mapped to string resources in feature modules.
- Time: `kotlinx-datetime` only.
- Platform capability needed by shared logic itself (crypto, HTTP engine):
  `expect`/`actual`, actual in `:shared` androidMain.
- Platform capability owned by the app (alarms, notifications, tool executors):
  interface in commonMain, implementation in `:app`, injected via constructor.

## Threading and scopes

- Every repository and data source is **main-safe**. Room/Ktor suspend APIs
  already are; wrap any CPU-heavy or blocking work in
  `withContext(defaultDispatcher)` inside the data layer, not in callers.
- Inject dispatchers (`ioDispatcher`, `defaultDispatcher`) — never hardcode
  `Dispatchers.IO` inline; tests replace them.
- Work that must outlive the calling screen (finish persisting a completed
  response after the user navigates away): inject an application-scoped
  `CoroutineScope` (`SupervisorJob() + Dispatchers.Default`, provided by Hilt
  from `:app`) and launch there:

```kotlin
class DefaultConversationRepository(
    private val engine: ChatEngine,
    private val messageDao: MessageDao,
    private val externalScope: CoroutineScope,
) {
    suspend fun send(conversationId: Long, text: String) =
        externalScope.launch { /* stream, then persist */ }.join()
}
```

- Work that must survive process death (reminder fire-time composition):
  WorkManager, not a coroutine scope. Worker classes live in `:app`; the
  logic they call lives in a repository.

## SharedFlow below the UI

Allowed in the data layer for genuine broadcast: `shareIn` to share one upstream
(one SSE connection, many collectors), replay-1 caches. Prefer `StateFlow`
whenever a "current value" exists.

## Errors

- Suspend functions throw typed exceptions (`ChatException(ChatError.RateLimited)`);
  never swallow `CancellationException` — always rethrow it.
- Flows: handle with `.catch` where the repository can degrade gracefully;
  otherwise let the ViewModel catch and fold into UiState.

## Testing

- Repository unit tests in `:shared` commonTest: fake DAOs/data sources,
  Ktor `MockEngine` for the Claude client (canned SSE bodies test the parser).
- DAO tests against in-memory Room.
- Write `FakeXRepository` (in-memory, plain Kotlin) next to each repository
  interface — it is the standard dependency for ViewModel and use-case tests.
  No mocking libraries.
