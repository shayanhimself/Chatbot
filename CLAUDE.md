# Chatbot — Agentic AI Chat for Android

Native Android chatbot, powered by Claude through the user's own Anthropic API key (BYOK) — no backend server and no project-owned key. The assistant is agentic: within a conversation it acts on the user's behalf via tool calls, and two capabilities are core to the product — setting **reminders** that later notify the user and resume the conversation, and keeping **memory** of user-approved facts across conversations. All user data (conversations, reminders, memories, key) lives on-device. Built KMP-first (shared Kotlin core, native Compose UI) so an iOS target can be added later without restructuring.

Product: `specs/000-product-brief.md`. Canonical stack: `specs/001-tech-stack.md`. Specifics below defer to those.

## Tooling

We use **`android` CLI** for project creation, SDK/emulator management, running and inspecting the app, doc lookup, and journey tests. Prefer it over ad-hoc gradle/adb. It's the default for API/version/migration questions too — treat it as fresher than model training.

## Spec files

Spec files describe the *current* system. When writing or updating them:
- Don't repeat yourself.
- No history / run events. State what the system *does*, not what happened once or what was decided when.
- Be concise. Don't over-explain.
