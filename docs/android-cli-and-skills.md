# Android CLI & Official Android Skills — Usage Guide

Two related pieces from Google (announced April 2026, ["Build Android apps 3x faster using any agent"](https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html)):

- **`android` CLI** — command-line tool for project creation, emulators, running apps, device inspection, docs search, journey tests, and skills management. Docs: [developer.android.com/tools/agents/android-cli](https://developer.android.com/tools/agents/android-cli)
- **Android skills** — [github.com/android/skills](https://github.com/android/skills): AI-optimized SKILL.md instruction sets (open [Agent Skills standard](https://agentskills.io)) covering areas where LLMs underperform. Works with Claude Code, Gemini, Antigravity, Codex. Docs: [developer.android.com/tools/agents/android-skills](https://developer.android.com/tools/agents/android-skills)

Both are pinned stack choices — see `specs/001-tech-stack.md`.

---

## 1. Android CLI

### Install / update

```bash
# macOS Apple Silicon:
curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh | bash
# (linux_x86_64 / darwin_x86_64 / windows_x86_64 variants exist)

android update          # update the CLI itself
android info            # environment info (SDK location etc.)
android init            # initialize environment (e.g. skills) for the CLI
```

### SDK management

```bash
android sdk list --all                                   # installed + available packages
android sdk install platforms/android-36                 # install (multiple, @version optional)
android sdk update                                       # update all (or one) package
android sdk remove <pkg>
```

### Project creation

```bash
android create --list                                    # list templates
android create empty-activity --name="Chatbot" --output=./app-dir --minSdk=31
```

### Emulators

```bash
android emulator list
android emulator create ...
android emulator start ...     # returns when emulator fully booted
android emulator stop ...
android emulator remove ...
```

### Run the app

```bash
android run --apks=<path.apk> [--activity=...] [--device=<serial>] [--debug]
android describe               # project metadata: build targets + APK output paths
```

### Inspect a running app (agent superpower — cheaper than screenshots)

```bash
android layout --pretty                  # JSON tree of on-screen UI elements
android layout --diff                    # only elements changed since last call — keeps context small
android screen capture -o screen.png     # screenshot (secondary tool; WebViews, images)
android screen capture --annotate -o screen.png   # numbered boxes on UI elements
android screen resolve --screen screen.png --string "#3"   # label → coordinates
```

Each `layout` element carries `text`, `resourceId`, `contentDesc`, `interactions` (clickable/scrollable/…), `state` (focused/checked/…), `bounds`, `center`. Interaction is plain adb using those coordinates:

```bash
adb shell input tap 152 23                     # tap element center
adb shell input swipe 250 400 250 600 500      # slow scroll (5th arg = duration ms)
adb shell input $(android screen resolve --screen screen.png --string "tap #34")
```

Rules of thumb (from the skill): text fields must show `focused` before typing; scroll slowly; after an action wait, then `layout --diff`; if `layout` fails (WebView/animation), fall back to annotated screenshot.

### Docs search (Android Knowledge Base)

```bash
android docs search <keywords>
android docs fetch <...>
```

Authoritative developer.android.com content, AI-curated. Use for migration guides, API examples, best practices — fresher than model training data.

### Android Studio bridge

```bash
android studio find-declaration | find-usages | open-file | analyze-file
android studio render-compose-preview        # render a Compose preview headlessly
android studio version-lookup                # latest versions of maven artifacts, Android versions
```

`version-lookup` is the tool for pinning versions in `gradle/libs.versions.toml` (see spec 001 open items).

### Journeys (agent-evaluated E2E tests)

Journey = XML file of natural-language steps, evaluated by an AI agent driving a real device/emulator. **Source of truth = the XML**: if the app disagrees, the app failed. Crash/freeze = instant fail.

```xml
<journey name="Send first chat message">
  <description>New user sends a message and receives a streamed reply</description>
  <actions>
    <action>Tap the "New chat" button</action>
    <action>Type "hello" into the message field and tap send</action>
    <action>Verify a response message appears below the user message</action>
  </actions>
</journey>
```

Semantics:

- Steps execute literally, in order, independently — no intent-guessing
- `Verify`/`Check` steps inspect the current screen only (no scrolling, no interaction)
- A single verify step with multiple expectations fails if ANY is false
- Output: JSON per action — `PASSED` / `FAILED` / `SKIPPED` + adb commands used + comments

Project convention (spec 000): every feature spec ships journey files covering its acceptance criteria. Journeys complement coded tests (JUnit/Compose testing APIs), never replace them — they're slow, token-costed, nondeterministic, but survive UI refactors because they test intent.

---

## 2. Official Android skills

### What we vendor

Copied into `.claude/skills/` (committed, so they're reproducible, reviewable, offline):

| Skill | Use when |
|---|---|
| `navigation-3` | Any navigation work — setup, migration, deep links, multiple back stacks, scenes (dialog/bottom-sheet/list-detail/two-pane), conditional nav, result flows, Hilt/ViewModel integration |
| `adaptive` | UI across phones/tablets/foldables/desktop — window size classes, multi-pane via Nav3 Scenes, adaptive components |
| `testing-setup` | Installing test libraries, building unit/UI/screenshot/e2e harnesses |
| `edge-to-edge` | Edge-to-edge migration; UI obscured by system bars; IME insets |
| `android-cli` | Teaches the agent the `android` CLI itself (section 1 is distilled from it) |

They auto-activate — Claude Code matches the task against each skill's `description`. Manual trigger: just name the topic, or invoke via `/` skill menu.

Full catalog (browse before adding more): `android skills list`, `android skills find <keyword>`, or [developer.android.com/tools/agents/android-skills/browse](https://developer.android.com/tools/agents/android-skills/browse). Available but not vendored: `agp-9-upgrade`, `migrate-xml-views-to-jetpack-compose`, `styles`, `r8-analyzer`, `perfetto-*`, `android-intent-security`, camera/wear/xr/play skills.

### Adding a skill

```bash
android skills add --skill=<name> --project=.
```

Installs into detected agent directories (Claude Code → `.claude/skills/`). Then commit. (Equivalent: copy the folder from a clone of github.com/android/skills — that's how the current five were installed.)

### Updating skills — the important part

No `android skills update` command exists. Procedure:

```bash
android update                                    # refresh CLI (and its skill catalog)
android skills add --skill=navigation-3 --project=.   # re-add = overwrite with latest
# repeat per vendored skill
git diff .claude/skills/                          # REVIEW before committing
git add .claude/skills && git commit
```

**Never commit upstream skill changes unreviewed.** Skills are instructions the agent obeys — a prompt-injection surface. The git diff review is the security boundary. Cadence: monthly, plus before major feature work (repo releases roughly monthly).

### Removing

```bash
android skills remove ...        # or just delete the folder from .claude/skills/ and commit
```

---

## References

- `specs/001-tech-stack.md` — stack decisions incl. these tools
- `docs/superpowers-guide.md` — the workflow these tools plug into
- `.claude/skills/android-cli/references/` — `interact.md` (device interaction), `journeys.md` (journey format spec)
- [android-developers.googleblog.com — launch post](https://android-developers.googleblog.com/2026/04/build-android-apps-3x-faster-using-any-agent.html)
