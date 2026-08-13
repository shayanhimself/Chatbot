---
name: journeys
description: Use when running this project's journey tests, evaluating a journeys/*.xml against an emulator, or checking the acceptance gate before a milestone.
metadata:
  keywords:
  - journey
  - journeys.py
  - acceptance test
  - emulator
  - onboard-with-key
  - two-pane-layout
---

# Running journey tests

## Overview

A journey is an XML acceptance test in `journeys/`. The format, how to walk the
actions, what counts as a failure, and the result schema all belong to the
`android-cli` skill.

**Required reading before writing a journey, or evaluating one by hand:**
`.claude/skills/android-cli/references/journeys.md`. This page does not repeat
it.

This project adds two things on top: each journey declares the starting state
and the device it needs, and results are written to disk.

## Declarations

The root element carries them. Both have a default, so a journey says only what
is unusual about it.

| Attribute | Values | Default | Means |
|---|---|---|---|
| `state` | `onboarded`, `fresh-install` | `onboarded` | The app the first action opens |
| `device` | `phone`, `tablet` | `phone` | The form factor the journey is about |

`onboarded` is a stored API key with no conversations. `fresh-install` is
nothing stored at all, which only the onboarding journeys want.

**Whoever runs the journey establishes the declared state before the first
action.** No journey clears data itself.

## Running

```bash
./scripts/journeys.py                              # every journey
./scripts/journeys.py onboard-offline              # bare name
./scripts/journeys.py switch-model app-launch      # several
```

Each form factor gets an AVD matching what its journeys declared, so a run
covers both without being told. `--avd NAME` forces one device instead.

Results land in `build/journey-results/`: `<name>.json` is the result schema,
`<name>.envelope.json` is the evaluator's raw output, and `<name>-artifacts/`
holds its screenshots. Exit is non-zero if any journey fails.

## Running one by hand

Worth it to debug a journey the script reports failing, since the reasoning
stays in front of you. Establish the state first.

```bash
# fresh-install
adb shell pm clear com.shayanaryan.chatbot

# onboarded: erase conversations, keep the key
adb shell "run-as com.shayanaryan.chatbot sh -c 'rm -f /data/user/0/com.shayanaryan.chatbot/databases/chatbot.db*'"
```

Quote the second one exactly as written. `adb shell` joins its arguments with
spaces and quotes none of them, so an unquoted command arrives as `sh -c rm -f
<path>` and everything past `rm` becomes a positional argument. The inner
`sh -c` is what expands the glob as the app's own user, which the shell user
cannot do, and the `-wal` and `-shm` sidecars have to go with the `.db` or they
restore the rows it no longer has.

An app that has been cleared can only be returned to `onboarded` by onboarding
it through the UI. `pm clear` destroys the Keystore master key along with the
store, so a saved copy of the ciphertext is undecryptable.

Then drive the device per the `android-cli` rules. `android layout` reports the
text, content description and tap coordinates of everything on screen, and
answers any check about what is present or what it says. `android screen
capture` is for checks about appearance: a dimmed button, a highlighted row,
two panes at once. Each screenshot stays in context for the rest of the run.

## Common mistakes

- **Running a journey on the wrong form factor.** The app shows two panes on an
  Expanded-width window (840dp and up), where the chat pane has no back arrow
  because nothing is behind it. A phone journey run there fails on that, which
  reads as an app defect.
- **Editing a journey to make it pass.** The XML is the source of truth. If the
  app disagrees, the app failed.
- **Grading your own work.** A result you produced for code you wrote is a
  self-report. For a milestone gate, run the script: its evaluator saw only the
  journey.
