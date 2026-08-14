#!/usr/bin/env python3
"""Run journeys against an emulator and report pass/fail.

Every journeys/*.xml by default, or the ones named as arguments.

Journey <action> blocks are natural language, so each journey is handed to a
headless `claude -p` that drives the device following the rules in
.claude/skills/android-cli/references/journeys.md. Per-journey result JSON and
scratch output land in build/journey-results/.

Exits non-zero if any journey fails or if its evaluation cannot be parsed.

Each journey declares the state and the device it needs on its own root element,
and this script establishes them. What those declarations mean is documented in
the `journeys` skill, which also covers running a journey without this script.

Pass --avd to force one device instead of the ones the journeys ask for.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
from pathlib import Path
from xml.etree import ElementTree

REPO = Path(__file__).resolve().parent.parent
PACKAGE = "com.shayanaryan.chatbot"
APK = "app/build/outputs/apk/debug/app-debug.apk"
JOURNEY_DIR = REPO / "journeys"
RESULTS_DIR = REPO / "build" / "journey-results"
JOURNEY_REF = REPO / ".claude/skills/android-cli/references/journeys.md"
AVD_HOME = Path.home() / ".android" / "avd"
LOCAL_PROPERTIES = REPO / "local.properties"

DATA_DIR = f"/data/user/0/{PACKAGE}"
# Every Room artifact, including the -wal and -shm sidecars: leaving those
# behind restores rows the .db alone no longer has.
CHAT_FILES = f"{DATA_DIR}/databases/chatbot.db*"
KEY_STORE = f"{DATA_DIR}/files/datastore/api_key.preferences_pb"

API_KEY_ENVIRONMENT_VARIABLE = "ANTHROPIC_API_KEY"
API_KEY_PROPERTY = "anthropic.api.key"

# What a journey may ask for on its root element. The journey owns these because
# they are facts about that journey, so a new one declares what it needs without
# this script carrying a list of names that would silently go stale.
STATE_FRESH_INSTALL = "fresh-install"
STATE_ONBOARDED = "onboarded"
STATES = (STATE_ONBOARDED, STATE_FRESH_INSTALL)

DEVICE_PHONE = "phone"
DEVICE_TABLET = "tablet"
DEVICES = (DEVICE_PHONE, DEVICE_TABLET)

# The window width at which the app lays out two panes, so it is what separates
# a journey that wants a chat filling the screen from one that wants both at
# once. `calculatePaneScaffoldDirective` returns two horizontal partitions only
# for the Expanded width class, whose lower bound this is; Medium (600dp) still
# gets one pane.
EXPANDED_WIDTH_DP = 840
DEFAULT_DENSITY_DPI = 160

# Journeys run the shipped APK, so the device must match targetSdk. The API
# ships as android-37.0 / android-37.1, hence the prefix match.
REQUIRED_TARGET = "android-37"

EVALUATOR_MODEL = "sonnet"

# The helper below is allowed by exact name. The evaluator runs from the
# journey's artifacts directory, so this is how it invokes it.
KEY_HELPER_NAME = "type-api-key.sh"
EVALUATOR_TOOLS = [
    "Bash(android *)",
    "Bash(adb *)",
    f"Bash(./{KEY_HELPER_NAME})",
    "Read",
    "Glob",
]

# Types the key into whatever field is focused, without ever printing it.
#
# The evaluator cannot reach the key any other way: `printenv`, `env` and
# `python3` are outside its allowlist and headless runs have nobody to approve
# them, while `adb ... "$KEY"` is refused because a prefix-matched command may
# not contain a shell expansion. Expanding inside a script sidesteps both, and
# keeps the key out of the transcript, where the command line is recorded.
KEY_HELPER = """\
#!/bin/sh
set -eu
exec adb -s "$ANDROID_SERIAL" shell input text "$ANTHROPIC_API_KEY"
"""

KEY_HELPER_PROMPT = """\
An action asking for the value of the {variable} environment variable is
performed by focusing the field first, then running ./{helper} with no
arguments, which types the key in. That value is deliberately unreadable by any
other means, and must never be printed, echoed or written to a file.
"""

PROMPT = """\
Evaluate an Android journey test against the running emulator.

Rules for evaluating a journey:

{rules}

--- BEGIN JOURNEY UNDER TEST ({name}) ---
{journey}
--- END JOURNEY UNDER TEST ---

The app package is {package}. It is installed but NOT running. {state} The
journey's own actions are responsible for opening it.

More than one emulator may be attached: only ever act on device {serial}. Pass
--device={serial} to 'android' commands and -s {serial} to 'adb' commands.

Inspect the device with 'android layout', which reports the text, content
description and tap coordinates of everything on screen, and answers any check
about what is present or what it says.

Reach for 'android screen capture' when the check is about appearance rather
than content: a dimmed button, a highlighted row, two panes at once. A
screenshot stays in context for the rest of the run, so every turn that follows
pays for it again.

An action that performs an interaction is followed by the next action. The rules
above put the whole burden of what that interaction produced on the next check,
so a passing interaction needs no inspection of its own.

Screenshots and any other scratch files belong in the current working
directory. Do not rebuild or reinstall the app. Do not modify anything in the
project itself.

{key_helper}
End your reply with the result JSON object described in the rules above,
containing one entry per action. Keep any commentary outside that object.
"""

# What the journey's declared state means for the app the evaluator will find.
PROMPT_STATE = {
    STATE_FRESH_INSTALL: "Its data has been cleared, so no API key is stored.",
    STATE_ONBOARDED: (
        "Its chat history has been erased, but the API key it was onboarded "
        "with is still stored, so it opens on the chat list."
    ),
}

SEED_PROMPT = """\
Onboard an Android app so that later journeys, which assume a key is already
stored, open on the chat list rather than the onboarding screen.

The app package is {package}. It is installed but NOT running, and its data has
been cleared, so it opens on the onboarding screen.

More than one emulator may be attached: only ever act on device {serial}. Pass
--device={serial} to 'android' commands and -s {serial} to 'adb' commands.

1. Launch the app.
2. Tap the field labelled "Anthropic API key", then run ./{helper} with no
   arguments to type the key into it.
3. Tap the button labelled "Validate & continue", and wait until the onboarding
   screen is replaced by a chat.

The key is deliberately unreadable by any means other than that helper, and must
never be printed, echoed or written to a file.

Inspect the device with 'android layout' to locate each element. Screenshots and
any other scratch files belong in the current working directory. Do not rebuild
or reinstall the app. Do not modify anything in the project itself.

Reply with SEEDED once a chat is on screen, or FAILED and the reason.
"""


def run(cmd: list[str], check: bool = True, **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=REPO, check=check, **kwargs)


def run_as(serial: str, command: str, check: bool = True) -> subprocess.CompletedProcess:
    """Run a shell command as the app's own user.

    `run-as` is what makes the app's files reachable without root, and it works
    because journeys run the debug APK.

    [command] is quoted before it is sent: `adb shell` joins its arguments with
    spaces and no quoting of its own, so an unquoted command reaches the device
    as `sh -c rm -f <path>`, where everything past the first word becomes a
    positional argument instead of part of the command.
    """
    return run(
        ["adb", "-s", serial, "shell", "run-as", PACKAGE, "sh", "-c", shlex.quote(command)],
        check=check,
        capture_output=True,
        text=True,
    )


def start_emulator(avd: str) -> str:
    """Boot the AVD if needed and return its adb serial.

    Every later device call is pinned to this serial: with more than one
    emulator attached, an unpinned adb either errors out or silently drives the
    wrong device, which would defeat the AVD selection above.
    """
    # `android emulator start` reports the serial on stderr, so merge streams.
    out = run(
        ["android", "emulator", "start", avd],
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    ).stdout
    print(out.strip())
    serials = re.findall(r"emulator-\d+", out)
    if not serials:
        print(
            f"ERROR: could not determine the adb serial for {avd} from:\n{out}",
            file=sys.stderr,
        )
        sys.exit(1)
    return serials[-1]


def avd_names() -> list[str]:
    out = run(["android", "emulator", "list"], capture_output=True, text=True).stdout
    return [line.strip() for line in out.splitlines() if line.strip()]


def avd_target(name: str) -> str:
    """The API level an AVD was created against.

    Read from the AVD's .ini rather than its name, which is free-form and need
    not reflect the actual API level.
    """
    ini = AVD_HOME / f"{name}.ini"
    try:
        for line in ini.read_text().splitlines():
            if line.startswith("target="):
                return line.split("=", 1)[1].strip()
    except OSError:
        pass
    return "unknown"


def avd_form_factor(name: str) -> str | None:
    """Which of DEVICES an AVD is, by whether the app would show two panes on it.

    Read from the AVD's screen rather than its name, which is free-form. The
    width is the one the config records for the device's natural orientation,
    which is the orientation an emulator boots in, so it is the window width the
    app will actually measure.

    @return the form factor, or None when the config cannot be read.
    """
    config = AVD_HOME / f"{name}.avd" / "config.ini"
    values: dict[str, str] = {}
    try:
        for line in config.read_text().splitlines():
            key, separator, value = line.partition("=")
            if separator:
                values[key.strip()] = value.strip()
    except OSError:
        return None

    try:
        width = int(values["hw.lcd.width"])
        density = int(values["hw.lcd.density"])
    except (KeyError, ValueError):
        return None

    width_dp = width * DEFAULT_DENSITY_DPI / density
    return DEVICE_TABLET if width_dp >= EXPANDED_WIDTH_DP else DEVICE_PHONE


def select_avd(override: str | None, device: str) -> str:
    """The AVD to run a group of journeys on.

    An override is honoured whatever it is, since asking for a named device is a
    deliberate act, but anything it disagrees with is reported: a phone journey
    run on a tablet fails on layout rather than on behaviour, which reads as an
    app defect.
    """
    if override:
        target = avd_target(override)
        if not target.startswith(REQUIRED_TARGET):
            print(
                f"WARNING: --avd {override} has target={target}, not {REQUIRED_TARGET}*. "
                f"Journeys will not exercise the shipped targetSdk.",
                file=sys.stderr,
            )
        form_factor = avd_form_factor(override)
        if form_factor is not None and form_factor != device:
            print(
                f"WARNING: --avd {override} is a {form_factor}, and journeys in this run "
                f"ask for a {device}.",
                file=sys.stderr,
            )
        return override

    names = avd_names()
    for name in names:
        if avd_target(name).startswith(REQUIRED_TARGET) and avd_form_factor(name) == device:
            return name

    print(f"ERROR: no {device} AVD with target={REQUIRED_TARGET} found.\n", file=sys.stderr)
    print("Available AVDs:", file=sys.stderr)
    for name in names:
        print(
            f"  {name:<28} target={avd_target(name):<14} {avd_form_factor(name) or 'unknown'}",
            file=sys.stderr,
        )
    if not names:
        print("  (none)", file=sys.stderr)
    print(
        "\nCreate a matching device in Android Studio (Device Manager -> Add device),\n"
        "or pass --avd <avd-name> to run against a specific device anyway.",
        file=sys.stderr,
    )
    sys.exit(1)


def resolve_api_key() -> str | None:
    """The developer's own Anthropic key, from the two sources 003's gated
    integration test already reads.

    Returned so it can be exported to the evaluator: onboarding is what opens
    the app, and a machine set up to run that test needs no further setup.
    """
    from_environment = os.environ.get(API_KEY_ENVIRONMENT_VARIABLE, "").strip()
    if from_environment:
        return from_environment

    try:
        for line in LOCAL_PROPERTIES.read_text().splitlines():
            name, separator, value = line.partition("=")
            if separator and name.strip() == API_KEY_PROPERTY and value.strip():
                return value.strip()
    except OSError:
        pass
    return None


def declaration(journey: Path) -> tuple[str, str]:
    """The state and device a journey asks for, from its root element.

    Both have a default, so a journey only says what is unusual about it: an
    onboarded app on a phone is what most of them want.

    @return the state, one of STATES, and the device, one of DEVICES.
    """
    try:
        root = ElementTree.parse(journey).getroot()
    except ElementTree.ParseError as error:
        print(f"ERROR: {journey.name} is not valid XML: {error}", file=sys.stderr)
        sys.exit(1)

    state = root.get("state", STATE_ONBOARDED)
    device = root.get("device", DEVICE_PHONE)
    for value, allowed, attribute in ((state, STATES, "state"), (device, DEVICES, "device")):
        if value not in allowed:
            print(
                f"ERROR: {journey.name} declares {attribute}=\"{value}\"; "
                f"expected one of {', '.join(allowed)}.",
                file=sys.stderr,
            )
            sys.exit(1)
    return state, device


def journey_state(journey: Path) -> str:
    return declaration(journey)[0]


def journey_device(journey: Path) -> str:
    return declaration(journey)[1]


def write_key_helper(artifacts: Path) -> None:
    """Put the key-typing helper where the evaluator can run it."""
    helper = artifacts / KEY_HELPER_NAME
    helper.write_text(KEY_HELPER)
    helper.chmod(0o755)


def has_key(serial: str) -> bool:
    return run_as(serial, f"test -f {KEY_STORE}", check=False).returncode == 0


def clear_chats(serial: str) -> None:
    """Erase chat history, leaving the stored key in place."""
    run_as(serial, f"rm -f {CHAT_FILES}")


def seed_key(serial: str, artifacts: Path, env: dict[str, str]) -> bool:
    """Onboard the app so a journey that assumes a stored key can run.

    The key is encrypted against a Keystore master key that a data wipe destroys
    along with it, so a saved copy of the store cannot be restored onto a cleared
    app. Driving onboarding is the only way back to a keyed app.
    """
    print("    seeding: onboarding to store an API key")
    run(["adb", "-s", serial, "shell", "am", "force-stop", PACKAGE])
    run(["adb", "-s", serial, "shell", "pm", "clear", PACKAGE], stdout=subprocess.DEVNULL)
    write_key_helper(artifacts)

    proc = subprocess.run(
        [
            "claude",
            "-p",
            SEED_PROMPT.format(package=PACKAGE, serial=serial, helper=KEY_HELPER_NAME),
            "--model",
            EVALUATOR_MODEL,
            "--allowedTools",
            *EVALUATOR_TOOLS,
        ],
        cwd=artifacts,
        env=env,
        capture_output=True,
        text=True,
    )
    # The reply is not trusted: the stored ciphertext is the evidence that
    # onboarding actually completed.
    run(["adb", "-s", serial, "shell", "am", "force-stop", PACKAGE])
    if proc.returncode == 0 and has_key(serial):
        return True

    print("    FAILED — could not onboard the app before the journey", file=sys.stderr)
    return False


def extract_result(text: str) -> dict | None:
    """Pull the journey result object out of the evaluator's reply.

    The reply is free text that usually but not always contains only the JSON,
    so scan for the first well-formed object carrying a "results" key and
    ignore anything around it (prose, markdown fences, trailing commentary).
    """
    decoder = json.JSONDecoder()
    for index, char in enumerate(text):
        if char != "{":
            continue
        try:
            obj, _ = decoder.raw_decode(text[index:])
        except ValueError:
            continue
        if isinstance(obj, dict) and "results" in obj:
            return obj
    return None


def evaluate(journey: Path, serial: str, env: dict[str, str]) -> bool:
    """Evaluate one journey. True if every action passed."""
    name = journey.stem
    envelope_path = RESULTS_DIR / f"{name}.envelope.json"
    result_path = RESULTS_DIR / f"{name}.json"
    # The evaluator runs from here so screenshots and other scratch output land
    # under build/ instead of the repo root.
    artifacts = RESULTS_DIR / f"{name}-artifacts"

    print(f"\n--> {journey.relative_to(REPO)}")
    shutil.rmtree(artifacts, ignore_errors=True)
    artifacts.mkdir(parents=True)
    has_api_key = API_KEY_ENVIRONMENT_VARIABLE in env
    if has_api_key:
        write_key_helper(artifacts)

    # Cold start: each journey opens the app itself via its own first action.
    run(["adb", "-s", serial, "shell", "am", "force-stop", PACKAGE])

    state = journey_state(journey)
    if state == STATE_FRESH_INSTALL:
        run(["adb", "-s", serial, "shell", "pm", "clear", PACKAGE], stdout=subprocess.DEVNULL)
    else:
        # A preceding fresh-install journey ends with nothing stored, so the key
        # is restored here rather than once for the whole run.
        if not has_key(serial) and not seed_key(serial, artifacts, env):
            return False
        clear_chats(serial)

    prompt = PROMPT.format(
        rules=JOURNEY_REF.read_text(),
        name=journey.relative_to(REPO),
        journey=journey.read_text(),
        package=PACKAGE,
        serial=serial,
        state=PROMPT_STATE[state],
        key_helper=(
            KEY_HELPER_PROMPT.format(
                variable=API_KEY_ENVIRONMENT_VARIABLE,
                helper=KEY_HELPER_NAME,
            )
            if has_api_key
            else ""
        ),
    )

    proc = subprocess.run(
        [
            "claude",
            "-p",
            prompt,
            "--output-format",
            "json",
            "--model",
            EVALUATOR_MODEL,
            "--allowedTools",
            *EVALUATOR_TOOLS,
        ],
        cwd=artifacts,
        env=env,
        capture_output=True,
        text=True,
    )
    envelope_path.write_text(proc.stdout)

    if proc.returncode != 0:
        print(f"    FAILED — evaluator exited {proc.returncode} (see {envelope_path})")
        return False

    try:
        reply = json.loads(proc.stdout).get("result", "")
    except json.JSONDecodeError:
        reply = ""

    result = extract_result(reply)
    if not result or not result.get("results"):
        print(f"    FAILED — evaluator output was not valid journey JSON (see {envelope_path})")
        return False

    result_path.write_text(json.dumps(result, indent=2, ensure_ascii=False))

    for entry in result["results"]:
        print(f"    {entry.get('status', '?')}\t{entry.get('action', '?')}")

    ok = all(entry.get("status") == "PASSED" for entry in result["results"])
    print("    PASSED" if ok else "    FAILED")
    return ok


def resolve_journey(name: str) -> Path | None:
    """Find the journey [name] refers to.

    A bare name is resolved against the journey directory, so a journey can be
    named the way it is spoken about rather than by its path.
    """
    candidates = [Path(name), JOURNEY_DIR / name, JOURNEY_DIR / f"{name}.xml"]
    return next((path for path in candidates if path.is_file()), None)


def select_journeys(names: list[str]) -> list[Path]:
    """The journeys to evaluate, in the order they should run.

    A fresh-install journey ends with an app that has to be onboarded again, so
    running those last spends one onboarding on the whole run rather than one
    per journey that follows.
    """
    if names:
        resolved = [(name, resolve_journey(name)) for name in names]
        missing = [name for name, path in resolved if path is None]
        if missing:
            print(f"ERROR: no such journey: {', '.join(missing)}", file=sys.stderr)
            print(f"Available in {JOURNEY_DIR.relative_to(REPO)}/:", file=sys.stderr)
            for path in sorted(JOURNEY_DIR.glob("*.xml")):
                print(f"  {path.stem}", file=sys.stderr)
            sys.exit(1)
        journeys = [path for _, path in resolved if path is not None]
    else:
        journeys = sorted(JOURNEY_DIR.glob("*.xml"))
        if not journeys:
            print(f"ERROR: no journeys found in {JOURNEY_DIR.relative_to(REPO)}/", file=sys.stderr)
            sys.exit(1)

    journeys.sort(key=lambda journey: journey_state(journey) == STATE_FRESH_INSTALL)
    return journeys


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "journeys",
        nargs="*",
        metavar="JOURNEY",
        help=(
            "Journeys to run, as a path or a bare name (onboard-offline, "
            "journeys/onboard-offline.xml). Defaults to every journey."
        ),
    )
    parser.add_argument(
        "--avd",
        metavar="NAME",
        help=(
            "Force every journey onto this AVD (see 'android emulator list'). "
            "By default each journey runs on the device it declares."
        ),
    )
    return parser.parse_args()


def main() -> int:
    # Subprocesses write straight to the fd, so keep our own prints unbuffered
    # or they arrive out of order when stdout is a pipe.
    sys.stdout.reconfigure(line_buffering=True)

    args = parse_args()
    # Journeys and the key are settled before the AVD, so a typo or an unset key
    # is reported without waiting on device selection.
    journeys = select_journeys(args.journeys)

    api_key = resolve_api_key()
    if api_key is None and any(journey_state(journey) == STATE_ONBOARDED for journey in journeys):
        print(
            f"ERROR: no developer key. Set {API_KEY_ENVIRONMENT_VARIABLE}, or add "
            f"{API_KEY_PROPERTY}=<key> to local.properties.\n"
            "Journeys that open on the chat list cannot run without one.",
            file=sys.stderr,
        )
        return 1

    # An override collapses the run onto one device; otherwise each form factor
    # the journeys ask for gets its own. Emulators are addressed by serial
    # throughout, so both can be running at once and neither has to be stopped.
    if args.avd:
        groups = [(args.avd, journeys)]
    else:
        groups = [
            (select_avd(None, device), [j for j in journeys if journey_device(j) == device])
            for device in DEVICES
            if any(journey_device(journey) == device for journey in journeys)
        ]

    print(f"==> {len(journeys)} journey(s) to evaluate")
    for avd, group in groups:
        print(f"    {avd} ({avd_target(avd)}): {', '.join(p.stem for p in group)}")

    print("==> Building debug APK")
    run(["./gradlew", ":app:assembleDebug"])

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    results: list[bool] = []
    for avd, group in groups:
        print(f"\n==> Starting {avd} (no-op if already running)")
        serial = start_emulator(avd)
        print(f"==> Device: {serial}")

        # `android run` installs and launches; the per-journey reset returns the
        # app to a cold, not-running state before anything is evaluated.
        print(f"==> Installing {PACKAGE}")
        run(["android", "run", f"--device={serial}", "--apks", APK])

        # ANDROID_SERIAL pins the evaluator's adb calls to this device even if it
        # forgets to pass a serial itself. The key is exported rather than
        # written into a prompt, so it stays out of every transcript on disk.
        env = {**os.environ, "ANDROID_SERIAL": serial}
        if api_key is not None:
            env[API_KEY_ENVIRONMENT_VARIABLE] = api_key

        results += [evaluate(journey, serial, env) for journey in group]

    passed = sum(results)
    failed = len(results) - passed
    print(f"\n==> {passed} passed, {failed} failed")
    print(f"==> results -> {RESULTS_DIR.relative_to(REPO)}/")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
