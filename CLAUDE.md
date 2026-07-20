# Chatbot — Agentic AI Chat for Android

Native Android chatbot, powered by Claude through the user's own Anthropic API key (BYOK) — no backend server and no project-owned key. The assistant is agentic: within a conversation it acts on the user's behalf via tool calls, and two capabilities are core to the product — setting **reminders** that later notify the user and resume the conversation, and keeping **memory** of user-approved facts across conversations. All user data (conversations, reminders, memories, key) lives on-device. Built KMP-first (shared Kotlin core, native Compose UI) so an iOS target can be added later without restructuring.

Product: `specs/000-product-brief.md`. Canonical stack: `specs/001-tech-stack.md`. Specifics below defer to those.

## Git

**Never commit or push.** The user reviews all changes and commits manually. This overrides any skill/workflow step that says to commit (e.g. superpowers). Leave changes in the working tree and report what's ready.

## Tooling

We use **`android` CLI** for project creation, SDK/emulator management, running and inspecting the app, doc lookup, and journey tests. Prefer it over ad-hoc gradle/adb. It's the default for API/version/migration questions too — treat it as fresher than model training.

## Code style

- **"Bro" is display-name only** — never in code identifiers, packages, files, or functions. Use the neutral project name (`Chatbot` / domain terms) in code; "Bro" appears solely in user-facing copy.
- **`const val` names use `SCREAMING_SNAKE_CASE`**. Non-const `val`s follow normal `camelCase`.
- **No trailing (end-of-line) comments.** Put the comment on its own line *above* the code it describes. The one exception is where a language forces inline syntax.
- **KDoc where it earns its place.** When a function or class — or any of its arguments — isn't self-explanatory from its name and signature, add a KDoc block: one line on what it does, plus `@param`/`@return` for the non-obvious parts. **Interfaces and contracts always get KDoc**.
- **Public composables ship colocated previews.** Every `public @Composable` gets at least one plain `@Preview` in its own file, one per distinct visual state, each wrapped in `ChatbotTheme`.

## Spec files

Spec files describe the *current* system. When writing or updating them:
- Don't repeat yourself.
- No history / run events. State what the system *does*, not what happened once or what was decided when.
- Be concise. Don't over-explain.
