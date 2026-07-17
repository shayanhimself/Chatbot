#!/usr/bin/env bash
# Run every journeys/*.xml against a target API emulator and report pass/fail.
#
# Journey <action> blocks are natural language, so each journey is evaluated by
# `claude -p` following .claude/skills/android-cli/references/journeys.md.
# Result JSON per journey lands in build/journey-results/.
#
# Exits non-zero if any journey fails or if its evaluation cannot be parsed.
# Override the device with CHATBOT_AVD=<avd-name>; otherwise the first AVD
# whose target matches targetSdk is used.
set -euo pipefail

cd "$(dirname "$0")/.."

PACKAGE="com.shayanaryan.chatbot"
APK="app/build/outputs/apk/debug/app-debug.apk"
JOURNEY_DIR="journeys"
RESULTS_DIR="build/journey-results"
JOURNEY_REF=".claude/skills/android-cli/references/journeys.md"
# Journeys run the shipped APK, so the device must match targetSdk.
REQUIRED_TARGET="android-37"

# The API level an AVD was created against. Authoritative; the AVD's name is
# free-form and may not reflect it.
avd_target() {
  local ini="$HOME/.android/avd/$1.ini"
  [[ -f "$ini" ]] || return 0
  sed -n 's/^target=//p' "$ini" | head -1
}

select_avd() {
  if [[ -n "${CHATBOT_AVD:-}" ]]; then
    echo "$CHATBOT_AVD"
    return 0
  fi
  local avd
  while read -r avd; do
    [[ -n "$avd" ]] || continue
    if [[ "$(avd_target "$avd")" == "$REQUIRED_TARGET"* ]]; then
      echo "$avd"
      return 0
    fi
  done < <(android emulator list)
  return 1
}

no_avd_error() {
  local avd
  {
    echo "ERROR: no AVD with target=$REQUIRED_TARGET found."
    echo
    echo "Available AVDs:"
    while read -r avd; do
      [[ -n "$avd" ]] || continue
      printf '  %-28s target=%s\n' "$avd" "$(avd_target "$avd")"
    done < <(android emulator list)
    echo
    echo "Create a target API emulator in Android Studio (Device Manager -> Add device),"
    echo "or set CHATBOT_AVD=<avd-name> to run against a specific device anyway."
  } >&2
  exit 1
}

# Pull the journey result object out of the evaluator's reply. The reply is
# free text that usually but not always contains only the JSON, so scan for the
# first well-formed object carrying a "results" key and ignore anything around
# it (prose, markdown fences, trailing commentary).
extract_result_json() {
  python3 -c '
import json, sys
text = sys.stdin.read()
decoder = json.JSONDecoder()
for index, char in enumerate(text):
    if char != "{":
        continue
    try:
        obj, _ = decoder.raw_decode(text[index:])
    except ValueError:
        continue
    if isinstance(obj, dict) and "results" in obj:
        json.dump(obj, sys.stdout, indent=2, ensure_ascii=False)
        sys.exit(0)
sys.exit(1)
'
}

AVD="$(select_avd)" || no_avd_error
echo "==> AVD: $AVD (target=$(avd_target "$AVD"))"

shopt -s nullglob
journeys=("$JOURNEY_DIR"/*.xml)
shopt -u nullglob
if [[ ${#journeys[@]} -eq 0 ]]; then
  echo "ERROR: no journeys found in $JOURNEY_DIR/" >&2
  exit 1
fi
echo "==> ${#journeys[@]} journey(s) to evaluate"

echo "==> Starting emulator (no-op if already running)"
android emulator start "$AVD"

echo "==> Building debug APK"
./gradlew :app:assembleDebug

# `android run` installs and launches; the per-journey reset below returns the
# app to a cold, not-running state before anything is evaluated.
echo "==> Installing $PACKAGE"
android run --apks "$APK"

mkdir -p "$RESULTS_DIR"
passed=0
failed=0

for journey in "${journeys[@]}"; do
  name="$(basename "$journey" .xml)"
  envelope="$RESULTS_DIR/$name.envelope.json"
  result="$RESULTS_DIR/$name.json"
  # The evaluator is run from here so screenshots and other scratch output land
  # under build/ instead of the repo root.
  artifacts="$RESULTS_DIR/$name-artifacts"

  echo
  echo "--> $journey"
  rm -rf "$artifacts"
  mkdir -p "$artifacts"

  # Cold start: each journey opens the app itself via its own first action.
  adb shell am force-stop "$PACKAGE"
  adb shell pm clear "$PACKAGE" >/dev/null

  prompt="$(
    cat <<EOF
Evaluate an Android journey test against the running emulator.

Rules for evaluating a journey:

$(cat "$JOURNEY_REF")

--- BEGIN JOURNEY UNDER TEST ($journey) ---
$(cat "$journey")
--- END JOURNEY UNDER TEST ---

The app package is $PACKAGE. It is installed but NOT running, and its data has
been cleared. The journey actions themselves are responsible for opening it.

Inspect the device with 'android layout' and 'android screen capture'. Screenshots
and any other scratch files belong in the current working directory. Do not
rebuild or reinstall the app. Do not modify anything in the project itself.

End your reply with the result JSON object described in the rules above,
containing one entry per action. Keep any commentary outside that object.
EOF
  )"

  if ! (
    cd "$artifacts" &&
      claude -p "$prompt" \
        --output-format json \
        --allowedTools "Bash(android *)" "Bash(adb *)" Read Glob \
        --model sonnet
  ) >"$envelope"; then
    echo "    FAILED — evaluator exited non-zero (see $envelope)"
    failed=$((failed + 1))
    continue
  fi

  if ! jq -r '.result // empty' "$envelope" | extract_result_json >"$result" ||
    ! jq -e '.results | arrays and length > 0' "$result" >/dev/null 2>&1; then
    echo "    FAILED — evaluator output was not valid journey JSON (see $envelope)"
    failed=$((failed + 1))
    continue
  fi

  jq -r '.results[] | "    \(.status)\t\(.action)"' "$result"

  if jq -e '[.results[].status] | any(. != "PASSED")' "$result" >/dev/null; then
    echo "    FAILED"
    failed=$((failed + 1))
  else
    echo "    PASSED"
    passed=$((passed + 1))
  fi
done

echo
echo "==> $passed passed, $failed failed"
echo "==> results -> $RESULTS_DIR/"
[[ $failed -eq 0 ]]
