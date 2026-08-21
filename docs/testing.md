# Testing

## The test suite principle

A test is worth having only if it can fail for the right reason. Every check goes to the cheapest
layer that can still fail for that reason, and to exactly one layer.

Cheapness is not only wall-clock time. A JVM test that fails names one class; a journey that fails
names the whole app and needs a human to read the screenshot. Pushing a check down makes the failure
more legible as well as faster.

## Doubles

Real object first, hand-written fake second, mocking library never. There is no mocking dependency in
the build and no case for adding one: a mock returns the value the test wrote and asserts the call
rather than the result, so it stays green after the real collaborator stops behaving that way.
A fake asserts what the call produced, so a refactor that preserves the behaviour preserves the
test.

Fakes live in `:shared:testing` and `:core:testing`, in `commonMain`, which is what makes them
visible to other modules' tests. `MockEngine` and `MockWebServer` are not exceptions: they stand in
for the server, not for a class we own.

## The layers

### 1. Unit

**Where:** `shared/src/commonTest`, `shared/src/androidHostTest`, `shared/testing/src/commonTest`,
and the ViewModel and mapper tests in `app/src/test` and `feature/*/src/test`.

**Tools:** JUnit4, `kotlin.test`, `kotlinx-coroutines-test`, Ktor `MockEngine`, Room in-memory under
Robolectric.

**What goes here:** one class against its collaborators' fakes. Every failure branch: authentication
rejections, rate limits and their `retry-after` hint, overload, truncated streams, byte-gap stalls,
lost connectivity, malformed frames. Repository rules such as title truncation, ordering, and
rejecting a send to a deleted chat.

**Why here:** a decline or a timeout is a value the fake returns, not a condition anyone has to
arrange. The fakes in `:shared:testing` carry their own tests, because a fake that lies is worse than
no fake.

### 2. Screenshot

**Where:** `core/ui/src/screenshotTest`, `feature/*/src/screenshotTest`, with goldens checked in
under `src/screenshotTestDebug/reference`.

**Tools:** Compose Preview Screenshot Testing, `@PreviewTest` on a `@Preview`. Run by
`scripts/screenshotTest.sh`, re-recorded by `scripts/screenshotUpdate.sh`, and gated by `check`.

**What goes here:** rendering. Every component in light and dark, every distinct visual state of a
screen, the four form factors, and the large-font settings.

**Why here:** assertions are worst at exactly this. A test can confirm a label exists and the layout
can still be broken behind it.

### 3. Screen

**Where:** `feature/*/src/test`, `core/ui/src/test`, `app/src/test`.

**Tools:** Compose testing on Robolectric. No emulator.

**What goes here:** behaviour inside a single screen or component. Conditional visibility, validation,
enable and disable rules, which callback fires, and the accessibility contracts: minimum touch
target, state and role semantics, live regions.

**Why here:** it is logic that happens to live in the UI layer, so it is tested in the UI layer, at
JVM speed.

### 4. Flow

**Where:** `app/src/androidTest/kotlin/.../flow`. On a device.

**Tools:** Hilt instrumented testing, Compose testing and `MockWebServer` answering as
`api.anthropic.com` through the device's global proxy, trusted by a debug-only certificate anchor.
Run by `scripts/instrumented.sh`.

**What goes here:** multi-screen flows, integrating every real layer of the app and only the server
is replaced.
Navigation, arguments arriving intact, state surviving a trip out and back, an intent landing on the
right destination, and a message making the full round trip from the composer into Room and back
onto the screen.

**Why here:** the question is whether the layers compose. The server is mocked and the repository is
not, because a fake repository would remove Room, the Keystore and the engine, which are the seams
under test.

**Reading a failure:** a flow test failing should mean two layers are wired together wrong. If the
cause turns out to sit inside one layer, that is also a gap in the cheaper test that should have
caught it, and both get fixed.

### 5. Device smoke

**Where:** `app/src/androidTest`, and `shared/src/androidDeviceTest`.

**Tools:** Hilt instrumented testing, plain JUnit4.

**What goes here:** the two things a device buys that no JVM test can reach. The real Hilt graph
resolving, and the key store running over real Tink and the real Android Keystore.

**Why here:** Robolectric has no Keystore, so the JVM tests substitute the cipher and would stay green
through a real crypto failure.

### 6. E2E

**Where:** `journeys/*.xml`.

**Tools:** journey XML evaluated by an agent driving an emulator through the `android` CLI, against
the real API with a real key. Run by `scripts/journeys.py`.

**What goes here:** only what needs both halves of the real thing at once, the build a user installs
and the server they reach.

Every layer below substitutes one or the other, so a check a mocked server
could fail belongs at layer 4, and a check a fake could fail belongs lower still. The wire contract
alone does not qualify: the engine against the live API is a JVM test.

**Why here:** this layer replaces nothing. Everywhere else the app draws its UI from data we wrote,
and a flow test swaps its `Application` for a test one.
Running the real build against the real server is what makes this the most expensive and least
repeatable layer, so it stays small and a failure is read as signal rather than re-run.

## Where a test goes

| Layer | Runs on | In `check` | Command |
|---|---|---|---|
| Unit | JVM | yes | `scripts/test.sh` |
| Screenshot | JVM | yes | `scripts/test.sh`, re-record with `scripts/screenshotUpdate.sh` |
| Screen | JVM | yes | `scripts/test.sh` |
| Flow | device | no | `scripts/instrumented.sh` |
| Device smoke | device | no | `scripts/instrumented.sh` |
| E2E | device, real API | no | `scripts/journeys.py` |

Whether a test runs on the JVM or on a device is decided by its source set, never by an annotation.
`@RunWith(AndroidJUnit4::class)` appears on both a Robolectric test and a device test, and only the
source set separates them. In KMP modules that is `androidHostTest` for the JVM and
`androidDeviceTest` for a device; in Android modules it is `test` and `androidTest`.

Inside `:shared`, `commonTest` and `androidHostTest` compile together and run in one task, so the
split is only about what a test may touch. Anything reaching for Robolectric, a real database, or a
JVM or Android API goes in `androidHostTest`. Everything else stays in `commonTest`, which is
Kotlin-pure and is the half that would survive a second target being added.

## What is deliberately not tested

**The same assertion at two layers.** A scenario covered by a flow test is not also a journey.
Duplication does not add confidence, it adds a second place to diagnose the same failure and a second
thing to update.

**Framework and library behaviour.** We test our adapter, not the library under it. The engine's
handling of a truncated stream is ours and is tested; whether OkHttp reports a mid-stream close the
same way Ktor's `MockEngine` does is theirs and is not.

**Failure branches above layer 1.** Cheap as a fake's return value, awful to provoke on a device or
against a live API.

**Every state crossed with every theme and form factor as a golden.** Combinatorial. Representative
screen states, plus component goldens for the rest.

**Anything non-deterministic in a screenshot.** The streaming caret animates and relative timestamps
move, so a golden capturing either would fail on its own schedule. Freeze it or capture a state
without it.

## Golden maintenance

Baselines are re-recorded only in the change that intentionally alters the design, and the image diff
is part of that review. A design system change is one deliberate re-record, not a hundred goldens
quietly updated alongside unrelated work. A screenshot suite that goes noisy gets ignored, which is
worse than not having it.

## When a layer gets too expensive

Cut in this order:

1. E2E, to a smoke set run before a milestone rather than per change. `docs/roadmap.md`
   already treats journeys as a milestone gate.
2. Flow, to merge into `main` rather than every push.
3. Nothing else. Unit, screenshot and screen tests stay on every change; they are the cheap layers,
   and cutting them is how the fast signal disappears.
