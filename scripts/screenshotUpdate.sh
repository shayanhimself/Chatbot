#!/usr/bin/env bash
# Record/refresh the golden PNGs from the current Compose previews.
# Run after an intended visual change, then review and commit the updated goldens.
set -euo pipefail

cd "$(dirname "$0")/.."

exec ./gradlew updateDebugScreenshotTest "$@"
