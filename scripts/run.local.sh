#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

export USER_SERVICE_BASE_URL="${USER_SERVICE_BASE_URL:-http://localhost:8082}"

cd "${REPO_ROOT}"
./gradlew clean :app:bootRun
