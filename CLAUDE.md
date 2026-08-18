# Chatbot: Agentic AI Chat for Android

Native Android chatbot, powered by Claude through the user's own Anthropic API key (BYOK), with no backend server and no project-owned key. The assistant is agentic: within a chat it acts on the user's behalf via tool calls, and two capabilities are core to the product: setting **reminders** that later notify the user and resume the chat, and keeping **memory** of user-approved facts across chats. All user data (chats, reminders, memories, key) lives on-device. Built KMP-first (shared Kotlin core, native Compose UI) so an iOS target can be added later without restructuring.

Product: `specs/000-product-brief.md`. Canonical stack: `specs/001-tech-stack.md`. Specifics below defer to those.

## Git

**Never commit or push.** The user reviews all changes and commits manually. This overrides any skill/workflow step that says to commit (e.g. superpowers). Leave changes in the working tree and report what's ready.

## Tooling

We use **`android` CLI** for project creation, SDK/emulator management, running and inspecting the app and doc lookup. Prefer it over ad-hoc gradle/adb. It's the default for API/version/migration questions too. Treat it as fresher than model training.

Run the test suite with **`./scripts/test.sh`**.

## Code style

- **"buddy" is display-name only**: never in code identifiers, packages, files, or functions. Use the neutral project name (`Chatbot` / domain terms) in code; "buddy" appears solely in user-facing copy.
- **`chat` is the stored thread; `Claude` is the wire.** A thread the user sees and the app persists is a chat: `Chat`, `ChatEntity`, `ChatDao`, `ChatRepository`, `chatId`, and the `chats` table. The stateless Messages API client is Claude's: `ClaudeEngine`, `ClaudeMessageRequest`, `ClaudeMessage`, `ClaudeStreamEvent`. A payload carries no identity; a record does. "Conversation" is not a word this codebase uses, in code or in prose.
- **Types both sides share live in the `:shared` root package**, owned by neither: `ApiError`, `ContentBlock`, `Role`, `textContent()`. A type the record package, the wire package, and the UI all consume belongs to none of them, and putting it in one forces the others to import that one's vocabulary.
- **Chat UI is named for the pane it is**: `ChatList*` for the list of chats, `ChatDetail*` for the single chat, in `:feature:chat`'s `list` and `detail` packages.
- **`const val` names use `SCREAMING_SNAKE_CASE`**. Non-const `val`s follow normal `camelCase`.
- **No magic numbers.** A literal whose value carries meaning gets a named `const val`, so the name explains it instead of a comment. Exempt: `0` and `1`, counts and sizes a test asserts on (`assertEquals(2, items.size)`), and numbers that are already the domain's own vocabulary (an HTTP status, a `@Preview` height).
- **No trailing (end-of-line) comments.** Put the comment on its own line *above* the code it describes. The one exception is where a language forces inline syntax.
- **KDoc where it earns its place.** When a function or class (or any of its arguments) isn't self-explanatory from its name and signature, add a KDoc block: one line on what it does, plus `@param`/`@return` for the non-obvious parts. **Interfaces and contracts always get KDoc**.
- **KDoc opens with what the thing is or does.** One sentence, before any reasoning. A block that starts on the logic inside the body leaves the reader to infer the purpose from an explanation of it. Why it works that way comes after that sentence, or moves into the body above the branch it explains.
- **A comment sits with what it explains.** A class KDoc says what the class is, and stops there. Reasoning about one property, function, or branch goes above that property, function, or branch, never accumulated into the block on top of the class. Constructor `@param` lines are the exception, since the language gives them nowhere else to live.
- **Never name a document in code.** Comments and KDoc say what the code does, in its own terms. A spec number (`004 writes…`), a plan task, or a design frame id (`2e`, `3k`) is document-side: it goes stale, and means nothing to a reader who does not have that file open. Name the thing that behaves the way you are describing (`the repository writes…`) instead.
- **Test function names use backtick spaced form** (`` fun `does the thing`() ``).
- **Tests never spell out a string the code owns.** Copy the app ships is read from its resource through the module's test helper, and an identifier the domain owns.
- **A test's own fixture strings are named.** Every value a test builds its fixture from gets a file-level `private const val`. The name says what the string stands for, which a literal cannot, and a value the test also asserts on then cannot drift from the fixture it came from.
- **Public composables ship colocated previews.** Every `public @Composable` gets at least one plain `@Preview` in its own file, one per distinct visual state, each wrapped in `ChatbotTheme`.

## Writing: specs, docs, comments

- Don't repeat yourself.
- No history / run events. State what the system *does*, not what happened once or what was decided when.
- Don't over-explain. Say it once, at the shortest length that's still clear.
- One example only where the rule is genuinely ambiguous without it. Usually zero.
- Never use em dashes. Rewrite the sentence, or use a comma, colon, parentheses, or full stop.
