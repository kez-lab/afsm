#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_ARGS=("$@")

"$ROOT_DIR/gradlew" -p "$ROOT_DIR/afsm-graph-gradle-plugin" \
  test \
  --stacktrace \
  "${GRADLE_ARGS[@]}"

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" \
  :afsm-core:test \
  :afsm-graph-ksp:test \
  :afsm-runtime:test \
  :afsm-test:test \
  :afsm-viewmodel:testDebugUnitTest \
  :sample-shop:compileDebugKotlin \
  :sample-shop:testDebugUnitTest \
  :sample-shop:generateAfsmMmd \
  apiCheck \
  --stacktrace \
  "${GRADLE_ARGS[@]}"

"$ROOT_DIR/scripts/verify-consumer-smoke.sh" "${GRADLE_ARGS[@]}"
