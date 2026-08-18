#!/usr/bin/env bash
# Run the tests that need a real device: the smoke tests and the flow tests in :app, and the
# Keystore cipher in :shared. Kept out of scripts/test.sh because a device is not always attached.
#
# The app's base URL is a compile-time constant pointing at api.anthropic.com, so nothing in the
# app is redirected. The device's global proxy is what sends those requests to the MockWebServer
# the test process runs on PROXY_PORT instead.
set -euo pipefail

cd "$(dirname "$0")/.."

# Must match PROXY_PORT in app/src/androidTest/.../wire/LocalAnthropic.kt.
PROXY_PORT=8099

clear_proxy() {
  # A stale proxy pointing at a port nothing is listening on fails every later run with a network
  # error that reads like a product bug, so this runs on any exit, a failed run included.
  adb shell settings put global http_proxy :0 >/dev/null 2>&1 || true
}
trap clear_proxy EXIT

adb wait-for-device
adb shell settings put global http_proxy "127.0.0.1:${PROXY_PORT}"

./gradlew :app:connectedDebugAndroidTest :shared:connectedAndroidDeviceTest "$@"
