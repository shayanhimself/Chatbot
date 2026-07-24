#!/usr/bin/env bash
# Captures a real SSE stream from the Anthropic Messages API so the test fixtures in
# SseFixtures.kt are recorded rather than invented. Usage: scripts/record-sse-fixture.sh [outfile]
set -euo pipefail

KEY="${ANTHROPIC_API_KEY:-}"
if [ -z "$KEY" ] && [ -f local.properties ]; then
  KEY="$(sed -n 's/^anthropic\.api\.key=//p' local.properties | head -1 | tr -d '[:space:]')"
fi
if [ -z "$KEY" ]; then
  echo "Set ANTHROPIC_API_KEY, or add anthropic.api.key=<key> to local.properties." >&2
  exit 1
fi

OUT="${1:-build/sse-capture.txt}"
mkdir -p "$(dirname "$OUT")"

curl -sS --fail -N https://api.anthropic.com/v1/messages \
  -H "x-api-key: $KEY" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -H "accept: text/event-stream" \
  -d '{"model":"claude-sonnet-5","max_tokens":64,"stream":true,"messages":[{"role":"user","content":[{"type":"text","text":"Reply with exactly: Hello"}]}]}' \
  | tee "$OUT"

echo
echo "Wrote $OUT — paste its contents into SseFixtures.HAPPY_PATH."
