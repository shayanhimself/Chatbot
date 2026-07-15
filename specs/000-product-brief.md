# 000 — Product Brief

Status: draft (refine via superpowers brainstorming skill before writing feature specs)

## Product

An Android chatbot app. The user chats with Claude about anything, using their own Anthropic API key (BYOK). Beyond chat, the app is agentic on-device with no backend server:

- **Reminders:** the AI sets reminders mid-conversation (tool call). At fire time the app notifies the user with a freshly composed message; tapping resumes the conversation.
- **Memory:** the AI remembers user-approved facts across conversations; the user can review and delete them.

**Engine:** Claude via the Anthropic API ( Sonnet 5 default; Haiku 4.5 / Opus 4.8 selectable), user-supplied key only.

## Core user stories (to be expanded into numbered feature specs)

1. As a new user, I add my Anthropic API key on first launch (validated, stored encrypted) and then chat immediately, with streamed responses.
2. As a user, my conversations persist locally (Room) and I can browse/resume/delete them.
3. As a user, I can switch the Claude model per-conversation.
4. As a user, I say "remind me …" mid-conversation; the AI sets a reminder via tool call and confirms it. At the scheduled time I get a notification with a message composed for that moment; tapping it reopens/continues that conversation.
5. As a user, I can ask the AI to remember things about me; those facts shape all future conversations. I can view and delete memories in settings.
6. As a user, my reminders fire on time (exact-alarm permission flow), survive device reboot, and still fire with fallback text when the device is offline.

## Constraints

- Claude key: user-owned, encrypted at rest (Tink + Keystore → DataStore), never logged. Network required for inference — the app has no offline engine.
- Exact alarms: `SCHEDULE_EXACT_ALARM` special permission (not pre-granted on Android 14+; request via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, user can revoke). **Never `USE_EXACT_ALARM`** — Play policy reserves it for alarm/calendar-core apps. When denied: WorkManager inexact fallback.
- Notifications: `POST_NOTIFICATIONS` runtime permission (API 33+).
- Reminder notification content is composed at fire time by an expedited WorkManager job calling Claude; on failure/offline it shows the stored reminder text — a reminder is never silently lost.
- targetSdk 36 (Play requirement from 2026-08-31).

## Specs

- 001-tech-stack.md — canonical tech stack
