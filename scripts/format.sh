#!/usr/bin/env bash
# Format all Kotlin and Gradle Kotlin DSL sources with ktfmt (via Spotless).
# Pass --check to verify formatting without rewriting files.
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ "${1:-}" == "--check" ]]; then
  shift
  exec ./gradlew spotlessCheck "$@"
fi

exec ./gradlew spotlessApply "$@"
