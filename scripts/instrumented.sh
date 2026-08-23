#!/usr/bin/env bash
# Run the tests that need a real device: the smoke tests and the flow tests in :app, and the
# Keystore cipher in :shared. Kept out of scripts/test.sh because a device is not always attached.
#
#   scripts/instrumented.sh                     every device test
#   scripts/instrumented.sh 'long reply'        the :app tests whose name matches
#   scripts/instrumented.sh ChatFlowTest        likewise, by class
#
# A filter is a regex matched against `package.Class#method`, and narrows the run to :app: a
# filter matching nothing fails the task it matched nothing in, and no filter worth typing is
# meant for both modules at once.
#
# The app's base URL is a compile-time constant pointing at api.anthropic.com, so nothing in the
# app is redirected. The device's global proxy is what sends those requests to the MockWebServer
# the test process runs on PROXY_PORT instead.
set -euo pipefail

cd "$(dirname "$0")/.."

# Must match PROXY_PORT in app/src/androidTest/.../wire/LocalAnthropic.kt.
PROXY_PORT=8099

# A first argument that is not a flag is the filter; anything after it goes to Gradle untouched.
filter=""
if [[ ${1-} != "" && ${1-} != -* ]]; then
  filter="$1"
  shift
fi

# Tasks and the filter share one array, which is never empty: an empty one expanded under `set -u`
# is an unbound variable on the bash macOS ships.
gradle_args=(:app:connectedDebugAndroidTest :shared:connectedAndroidDeviceTest)
if [[ -n $filter ]]; then
  gradle_args=(
    :app:connectedDebugAndroidTest
    "-Pandroid.testInstrumentationRunnerArguments.tests_regex=$filter"
  )
  echo "Filtering on '${filter}': running :app only."
fi

run_log=$(mktemp)

cleanup() {
  # The status the trap was entered with, restored on the way out: without it the last command
  # here would become the script's own exit code and a failed run would report success.
  status=$?
  # A stale proxy pointing at a port nothing is listening on fails every later run with a network
  # error that reads like a product bug, so this runs on any exit, a failed run included.
  adb shell settings put global http_proxy :0 >/dev/null 2>&1 || true
  rm -f "$run_log"
  exit "$status"
}
trap cleanup EXIT

adb wait-for-device
adb shell settings put global http_proxy "127.0.0.1:${PROXY_PORT}"

./gradlew "${gradle_args[@]}" "$@" | tee "$run_log"

# A filter that matches nothing runs no tests and the build still succeeds, so a mistyped one
# would report a green run that tested nothing.
if [[ -n $filter ]] && grep -q "Starting 0 tests" "$run_log"; then
  echo "No test matched '${filter}'." >&2
  exit 1
fi
