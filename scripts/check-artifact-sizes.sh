#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LIMITS_FILE="$ROOT_DIR/gradle/artifact-size-limits.properties"
GROUP="${1:-com.github.ci}"
VERSION="${2:-0.0.0-ci}"
M2_BASE="${M2_REPO:-$HOME/.m2/repository}/$(echo "$GROUP" | tr '.' '/')"

fail=0

check_size() {
  local key="$1"
  local path="$2"
  local limit
  limit="$(grep -E "^${key}=" "$LIMITS_FILE" | cut -d= -f2 | tr -d '[:space:]')"

  if [[ -z "$limit" ]]; then
    echo "Missing limit for $key in $LIMITS_FILE"
    fail=1
    return
  fi

  if [[ ! -f "$path" ]]; then
    echo "FAIL missing $key at $path"
    fail=1
    return
  fi

  local size
  size="$(wc -c < "$path" | tr -d ' ')"
  if (( size > limit )); then
    echo "FAIL $key size=${size}B limit=${limit}B path=$path"
    fail=1
  else
    echo "OK   $key size=${size}B limit=${limit}B"
  fi
}

if [[ ! -f "$LIMITS_FILE" ]]; then
  echo "Limits file not found: $LIMITS_FILE"
  exit 1
fi

for artifact in AdsKit-core AdsKit-admob AdsKit-applovin AdsKit; do
  check_size "aar.$artifact" "$M2_BASE/$artifact/$VERSION/$artifact-$VERSION.aar"
done

APK_PATH="$ROOT_DIR/sample/build/outputs/apk/debug/sample-debug.apk"
check_size "apk.sample-debug" "$APK_PATH"

if (( fail != 0 )); then
  exit 1
fi

echo "All artifact size checks passed."
