#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" \
  :afsm-core:test \
  :afsm-compose:compileDebugKotlin \
  :afsm-runtime:test \
  :afsm-viewmodel:testDebugUnitTest \
  :sample-shop:compileDebugKotlin \
  :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  apiCheck \
  --stacktrace

"$ROOT_DIR/scripts/verify-consumer-smoke.sh"
