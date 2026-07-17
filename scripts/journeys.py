#!/usr/bin/env python3
"""Run every journeys/*.xml against an emulator and report pass/fail.

Journey <action> blocks are natural language, so each journey is handed to a
headless `claude -p` that drives the device following the rules in
.claude/skills/android-cli/references/journeys.md. Per-journey result JSON and
scratch output land in build/journey-results/.

Exits non-zero if any journey fails or if its evaluation cannot be parsed.

The device must match the app's targetSdk. Pass --avd to run against a
specific device instead.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent
PACKAGE = "com.shayanaryan.chatbot"
APK = "app/build/outputs/apk/debug/app-debug.apk"
JOURNEY_DIR = REPO / "journeys"
RESULTS_DIR = REPO / "build" / "journey-results"
JOURNEY_REF = REPO / ".claude/skills/android-cli/references/journeys.md"
AVD_HOME = Path.home() / ".android" / "avd"

# Journeys run the shipped APK, so the device must match targetSdk. The API
# ships as android-37.0 / android-37.1, hence the prefix match.
REQUIRED_TARGET = "android-37"

EVALUATOR_MODEL = "sonnet"
EVALUATOR_TOOLS = ["Bash(android *)", "Bash(adb *)", "Read", "Glob"]

PROMPT = """\
Evaluate an Android journey test against the running emulator.

Rules for evaluating a journey:

{rules}

--- BEGIN JOURNEY UNDER TEST ({name}) ---
{journey}
--- END JOURNEY UNDER TEST ---

The app package is {package}. It is installed but NOT running, and its data has
been cleared. The journey's own actions are responsible for opening it.

More than one emulator may be attached: only ever act on device {serial}. Pass
--device={serial} to 'android' commands and -s {serial} to 'adb' commands.

Inspect the device with 'android layout' and 'android screen capture'.
Screenshots and any other scratch files belong in the current working
directory. Do not rebuild or reinstall the app. Do not modify anything in the
project itself.

End your reply with the result JSON object described in the rules above,
containing one entry per action. Keep any commentary outside that object.
"""


def run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=REPO, check=True, **kwargs)


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


def select_avd(override: str | None) -> str:
    if override:
        target = avd_target(override)
        if not target.startswith(REQUIRED_TARGET):
            print(
                f"WARNING: --avd {override} has target={target}, not {REQUIRED_TARGET}*. "
                f"Journeys will not exercise the shipped targetSdk.",
                file=sys.stderr,
            )
        return override

    names = avd_names()
    for name in names:
        if avd_target(name).startswith(REQUIRED_TARGET):
            return name

    print(f"ERROR: no AVD with target={REQUIRED_TARGET} found.\n", file=sys.stderr)
    print("Available AVDs:", file=sys.stderr)
    for name in names:
        print(f"  {name:<28} target={avd_target(name)}", file=sys.stderr)
    if not names:
        print("  (none)", file=sys.stderr)
    print(
        "\nCreate a matching device in Android Studio (Device Manager -> Add device),\n"
        "or pass --avd <avd-name> to run against a specific device anyway.",
        file=sys.stderr,
    )
    sys.exit(1)


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


def evaluate(journey: Path, serial: str) -> bool:
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

    # Cold start: each journey opens the app itself via its own first action.
    run(["adb", "-s", serial, "shell", "am", "force-stop", PACKAGE])
    run(["adb", "-s", serial, "shell", "pm", "clear", PACKAGE], stdout=subprocess.DEVNULL)

    prompt = PROMPT.format(
        rules=JOURNEY_REF.read_text(),
        name=journey.relative_to(REPO),
        journey=journey.read_text(),
        package=PACKAGE,
        serial=serial,
    )

    # ANDROID_SERIAL pins the evaluator's adb calls to the selected device even
    # if it forgets to pass a serial itself.
    env = {**os.environ, "ANDROID_SERIAL": serial}

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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--avd",
        metavar="NAME",
        help=(
            f"AVD to run against (see 'android emulator list'). "
            f"Defaults to the first AVD with target={REQUIRED_TARGET}*."
        ),
    )
    return parser.parse_args()


def main() -> int:
    # Subprocesses write straight to the fd, so keep our own prints unbuffered
    # or they arrive out of order when stdout is a pipe.
    sys.stdout.reconfigure(line_buffering=True)

    args = parse_args()
    avd = select_avd(args.avd)
    print(f"==> AVD: {avd} (target={avd_target(avd)})")

    journeys = sorted(JOURNEY_DIR.glob("*.xml"))
    if not journeys:
        print(f"ERROR: no journeys found in {JOURNEY_DIR.relative_to(REPO)}/", file=sys.stderr)
        return 1
    print(f"==> {len(journeys)} journey(s) to evaluate")

    print("==> Starting emulator (no-op if already running)")
    serial = start_emulator(avd)
    print(f"==> Device: {serial}")

    print("==> Building debug APK")
    run(["./gradlew", ":app:assembleDebug"])

    # `android run` installs and launches; the per-journey reset returns the app
    # to a cold, not-running state before anything is evaluated.
    print(f"==> Installing {PACKAGE}")
    run(["android", "run", f"--device={serial}", "--apks", APK])

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    results = [evaluate(journey, serial) for journey in journeys]

    passed = sum(results)
    failed = len(results) - passed
    print(f"\n==> {passed} passed, {failed} failed")
    print(f"==> results -> {RESULTS_DIR.relative_to(REPO)}/")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
