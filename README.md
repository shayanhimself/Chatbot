# Chatbot

> **Work in progress.** Under active development, not yet ready for use.

This is a native Android chatbot powered by Claude, using your own Anthropic API key.
As an offline-first app, everything stays on your device, synced to your Google Drive.

Beyond chat, the assistant acts on your behalf within a conversation:

- **Reminders**: Ask it to remind you of something, one-off or recurring; at the scheduled time you get a notification that starts a conversation. (As of today, no chatbot has this ability)
- **Memory**: it remembers facts you approve across conversations, shaping future chats. You can manage them anytime.

---

The purpose of this project is to try out the latest Android tech stack: Kotlin Multiplatform, Jetpack Compose, Material 3, Navigation 3, Ktor, Room, DataStore, Hilt.
iOS target can be added later.
