#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LOCK_FILE="${LOCK_FILE:-$REPO_ROOT/contract.lock.yml}"
CONTRACT_DIR="${CONTRACT_DIR:-$REPO_ROOT/.contract}"

case "$CONTRACT_DIR" in
  ""|"/")
    echo "Refusing to use unsafe CONTRACT_DIR: $CONTRACT_DIR" >&2
    exit 1
    ;;
esac

[[ -f "$LOCK_FILE" ]] || {
  echo "contract.lock.yml not found: $LOCK_FILE" >&2
  exit 1
}

CONTRACT_REPO_URL="${CONTRACT_REPO_URL:-$(awk '/^[[:space:]]*repo:/ { print $2; exit }' "$LOCK_FILE")}"
CONTRACT_REF="${CONTRACT_REF:-$(awk '/^[[:space:]]*ref:/ { print $2; exit }' "$LOCK_FILE")}"
CONTRACT_COMMIT="${CONTRACT_COMMIT:-$(awk '/^[[:space:]]*commit:/ { print $2; exit }' "$LOCK_FILE")}"

[[ -n "$CONTRACT_REPO_URL" ]] || { echo "Missing contract.repo in $LOCK_FILE" >&2; exit 1; }
[[ -n "$CONTRACT_REF" ]] || { echo "Missing contract.ref in $LOCK_FILE" >&2; exit 1; }
[[ -n "$CONTRACT_COMMIT" ]] || { echo "Missing contract.commit in $LOCK_FILE" >&2; exit 1; }

rm -rf "$CONTRACT_DIR"
mkdir -p "$CONTRACT_DIR"

git -C "$CONTRACT_DIR" init -q
git -C "$CONTRACT_DIR" remote add origin "$CONTRACT_REPO_URL"

if git -C "$CONTRACT_DIR" fetch --depth 1 origin "$CONTRACT_COMMIT" >/dev/null 2>&1; then
  :
else
  git -C "$CONTRACT_DIR" fetch --depth 1 origin "$CONTRACT_REF" >/dev/null 2>&1
fi

git -C "$CONTRACT_DIR" checkout --detach FETCH_HEAD >/dev/null 2>&1

ACTUAL_COMMIT="$(git -C "$CONTRACT_DIR" rev-parse HEAD)"
if [[ "$ACTUAL_COMMIT" != "$CONTRACT_COMMIT" ]]; then
  echo "Locked contract commit mismatch: expected $CONTRACT_COMMIT, got $ACTUAL_COMMIT from $CONTRACT_REF" >&2
  exit 1
fi

echo "Fetched locked contract $ACTUAL_COMMIT from $CONTRACT_REPO_URL"
