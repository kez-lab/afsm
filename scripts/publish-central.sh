#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <release-version>" >&2
  exit 64
fi

release_version="$1"

for required_var in AFSM_CENTRAL_USERNAME AFSM_CENTRAL_PASSWORD AFSM_SIGNING_KEY AFSM_SIGNING_PASSWORD; do
  if [[ -z "${!required_var:-}" ]]; then
    echo "Missing required environment variable: $required_var" >&2
    exit 1
  fi
done

if [[ "$release_version" == *-SNAPSHOT ]]; then
  echo "Maven Central releases cannot use a -SNAPSHOT version." >&2
  exit 1
fi

if [[ ! "$release_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.-]+)?$ ]]; then
  echo "Release version must use a SemVer-compatible form." >&2
  exit 1
fi

configured_version="$(sed -n 's/^afsmVersion=//p' gradle.properties)"
if [[ "$configured_version" != "$release_version" ]]; then
  echo "Requested version $release_version does not match gradle.properties ($configured_version)." >&2
  exit 1
fi

staging_dir="build/central-staging-repository"
plugin_staging_dir="afsm-graph-gradle-plugin/build/central-staging-repository"
bundle_path="build/afsm-${release_version}-central-bundle.zip"

rm -rf "$staging_dir" "$plugin_staging_dir"
rm -f "$bundle_path"

export ORG_GRADLE_PROJECT_signingInMemoryKey="$AFSM_SIGNING_KEY"
export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$AFSM_SIGNING_PASSWORD"

./gradlew publishAllPublicationsToCentralStagingRepository --no-daemon --stacktrace
./gradlew -p afsm-graph-gradle-plugin publishAllPublicationsToCentralStagingRepository --no-daemon --stacktrace

if [[ -d "$plugin_staging_dir" ]]; then
  mkdir -p "$staging_dir"
  cp -R "$plugin_staging_dir/"* "$staging_dir/"
fi

if ! find "$staging_dir" -type f -path "*/${release_version}/*.asc" -print -quit | grep -q .; then
  echo "No PGP signatures were generated in $staging_dir." >&2
  exit 1
fi

while IFS= read -r -d '' artifact; do
  test -f "${artifact}.md5"
  test -f "${artifact}.sha1"
done < <(find "$staging_dir" -type f -path "*/${release_version}/*" \
  ! -name '*.asc' ! -name '*.md5' ! -name '*.sha1' ! -name '*.sha256' ! -name '*.sha512' -print0)

(
  cd "$staging_dir"
  find . -type f -path "*/${release_version}/*" -print | \
    zip -q "../$(basename "$bundle_path")" -@
)

authorization="$(printf '%s:%s' "$AFSM_CENTRAL_USERNAME" "$AFSM_CENTRAL_PASSWORD" | base64 | tr -d '\n')"
deployment_id="$(curl --fail-with-body --silent --show-error \
  --request POST \
  --header "Authorization: Bearer $authorization" \
  --form "bundle=@${bundle_path};type=application/octet-stream" \
  "https://central.sonatype.com/api/v1/publisher/upload?name=io.github.afsm%3A${release_version}&publishingType=AUTOMATIC")"

echo "Central deployment: $deployment_id"
echo "deployment_id=$deployment_id" >> "${GITHUB_OUTPUT:-/dev/null}"

for attempt in {1..60}; do
  status="$(curl --fail-with-body --silent --show-error \
    --request POST \
    --header "Authorization: Bearer $authorization" \
    --get \
    --data-urlencode "id=$deployment_id" \
    "https://central.sonatype.com/api/v1/publisher/status")"
  state="$(jq -r '.deploymentState // empty' <<<"$status")"
  echo "Central deployment state: $state"

  case "$state" in
    PUBLISHED)
      exit 0
      ;;
    FAILED)
      jq '.errors // .' <<<"$status" >&2
      exit 1
      ;;
  esac

  sleep 10
done

echo "Timed out waiting for Central deployment $deployment_id." >&2
exit 1
