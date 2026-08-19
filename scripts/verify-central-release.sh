#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <release-version>" >&2
  exit 64
fi

release_version="$1"

for attempt in {1..30}; do
  if ./gradlew -p consumer-smoke -PafsmVersion="$release_version" -PuseMavenLocal=false \
    --refresh-dependencies clean :app:compileDebugKotlin :app:testDebugUnitTest :app:verifyAfsmMmd \
    --no-daemon --stacktrace; then
    exit 0
  fi

  if [[ "$attempt" -eq 30 ]]; then
    exit 1
  fi

  echo "Artifacts are not yet reachable from Maven Central; retrying in 20 seconds."
  sleep 20
done
