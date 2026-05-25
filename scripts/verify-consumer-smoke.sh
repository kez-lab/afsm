#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLE_ARGS=("$@")
AFSM_VERSION="$(
  sed -n 's/^afsmVersion=//p' "$ROOT_DIR/gradle.properties" | tail -n 1
)"

if [[ -z "$AFSM_VERSION" ]]; then
  echo "Missing afsmVersion in $ROOT_DIR/gradle.properties" >&2
  exit 1
fi

if [[ -z "${ANDROID_HOME:-}" && -f "$ROOT_DIR/local.properties" ]]; then
  SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$ROOT_DIR/local.properties" | tail -n 1)"
  if [[ -n "$SDK_DIR" ]]; then
    export ANDROID_HOME="$SDK_DIR"
  fi
fi

"$ROOT_DIR/gradlew" -p "$ROOT_DIR" publishToMavenLocal "${GRADLE_ARGS[@]}"
"$ROOT_DIR/gradlew" -p "$ROOT_DIR/afsm-graph-gradle-plugin" publishToMavenLocal "${GRADLE_ARGS[@]}"
"$ROOT_DIR/gradlew" -p "$ROOT_DIR/consumer-smoke" \
  -PafsmVersion="$AFSM_VERSION" \
  --refresh-dependencies \
  clean \
  :app:compileDebugKotlin \
  :app:testDebugUnitTest \
  :app:generateAfsmMmd \
  "${GRADLE_ARGS[@]}"

MMD_FILE="$ROOT_DIR/consumer-smoke/app/build/generated/afsm/mmd/ConsumerSmoke.mmd"
if [[ ! -f "$MMD_FILE" ]]; then
  echo "Missing consumer smoke Afsm graph: $MMD_FILE" >&2
  exit 1
fi

if ! head -n 1 "$MMD_FILE" | grep -q '^stateDiagram-v2$'; then
  echo "Invalid consumer smoke Afsm graph header: $MMD_FILE" >&2
  exit 1
fi

if ! grep -q '^  Editing --> Saving: SaveClicked$' "$MMD_FILE"; then
  echo "Missing consumer smoke Afsm graph transition: Editing --> Saving: SaveClicked" >&2
  exit 1
fi

if ! grep -q '^  Saving --> Saved: Saved$' "$MMD_FILE"; then
  echo "Missing consumer smoke Afsm graph transition: Saving --> Saved: Saved" >&2
  exit 1
fi
