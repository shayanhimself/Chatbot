#!/usr/bin/env bash
# Validate Compose preview screenshots against the checked-in golden PNGs.
# Fails on any pixel diff; report at <module>/build/reports/screenshotTest/.
set -euo pipefail

cd "$(dirname "$0")/.."

exec ./gradlew validateDebugScreenshotTest "$@"
