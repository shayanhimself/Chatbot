# Chatbot

A native Android chatbot powered by Claude, using your own Anthropic API key. Everything stays on your device.

You pick the Claude model per conversation (Sonnet 5 default; Haiku 4.5 and Opus 4.8 selectable).

Beyond chat, the assistant acts on your behalf within a conversation:

- **Reminders** — ask it to remind you of something; at the scheduled time you get a notification with a freshly composed message, and tapping it resumes the conversation.
- **Memory** — it remembers facts you approve across conversations, shaping future chats. You can review and delete them anytime.


Built KMP-first (shared Kotlin core, native Compose UI) so an iOS target can be added later.
