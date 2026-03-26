#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROFILE="${1:-dev}"

case "${PROFILE}" in
  dev|prod)
    ;;
  *)
    echo "Usage: $0 [dev|prod]" >&2
    exit 1
    ;;
esac

cd "${REPO_ROOT}/docker"
docker compose -f "docker-compose.${PROFILE}.yml" up --build -d
