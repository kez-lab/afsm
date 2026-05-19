#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_ARGS=("$@")

if [[ -z "${ANDROID_HOME:-}" && -f "$ROOT_DIR/local.properties" ]]; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$ROOT_DIR/local.properties" | tail -n 1)"
  if [[ -n "$SDK_DIR" ]]; then
    export ANDROID_HOME="$SDK_DIR"
  fi
fi

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" publishToMavenLocal "${GRADLE_ARGS[@]}"
"$ROOT_DIR/gradlew" -p "$ROOT_DIR/afsm-graph-gradle-plugin" publishToMavenLocal "${GRADLE_ARGS[@]}"
"$ROOT_DIR/gradlew" -p "$ROOT_DIR/consumer-smoke" :app:compileDebugKotlin :app:generateAfsmMmd "${GRADLE_ARGS[@]}"
