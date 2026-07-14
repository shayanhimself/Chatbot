# Domain layer (optional)

Use cases live in `:shared` commonMain when used at all.

## Default: skip it

ViewModels call repositories directly. Add a use case only when one of these is
true:

- The same business logic is needed by **two or more ViewModels** (or by a
  ViewModel and a Worker/tool executor).
- A ViewModel's state production has grown genuinely complex and extracting a
  named operation makes it readable.

Never add pass-through use cases that just forward one repository call — that is
ceremony, not architecture. There is no blanket "UI must go through domain"
rule in this codebase.

## Shape

```kotlin
class ComposeReminderMessageUseCase(
    private val chatEngine: ChatEngine,
    private val memoryRepository: MemoryRepository,
) {
    suspend operator fun invoke(reminder: Reminder): String { /* ... */ }
}
```

- Name: *VerbNoun*UseCase (`FormatDateUseCase`, `ComposeReminderMessageUseCase`).
- Single responsibility; callable via `operator fun invoke`.
- **Stateless.** No mutable fields, no lifecycle. New instance per injection site
  is fine (unscoped Hilt provider).
- Depends on repositories (and other use cases); never on DAOs or the UI.
- Main-safe. If it does CPU-heavy aggregation, `withContext(defaultDispatcher)`
  inside — but first ask whether that work belongs in the data layer (cacheable,
  reusable → repository).
- If Room can answer it with a relationship query, write the query in a
  repository instead of combining repositories in a use case.

## Testing

Plain unit tests in commonTest with fake repositories. No Android, no mocks.
