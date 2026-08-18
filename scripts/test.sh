#!/usr/bin/env bash
# Run every test level: JVM unit tests in all modules including the KMP ones, Compose UI tests on
# Robolectric, screenshot goldens, and the ktlint gate.
#
# `./gradlew test` is not equivalent. KMP modules attach their tests to `allTests`, so `test`
# silently skips :shared and :shared:testing, which is most of the suite. `check` covers both.
#
# Instrumented tests need a device and are not part of this gate; run scripts/instrumented.sh.
set -euo pipefail

cd "$(dirname "$0")/.."

exec ./gradlew check "$@"
