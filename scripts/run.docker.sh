#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROFILE="${1:-dev}"
SHARED_SERVICE_NETWORK="${SHARED_SERVICE_NETWORK:-${BACKEND_SHARED_NETWORK:-${MSA_SHARED_NETWORK:-service-backbone-shared}}}"

case "${PROFILE}" in
  dev|prod)
    ;;
  *)
    echo "Usage: $0 [dev|prod]" >&2
    exit 1
    ;;
esac

cd "${REPO_ROOT}/docker"

if ! docker network inspect "${SHARED_SERVICE_NETWORK}" >/dev/null 2>&1; then
  echo "Creating external docker network: ${SHARED_SERVICE_NETWORK}"
  docker network create "${SHARED_SERVICE_NETWORK}" >/dev/null
fi

SHARED_SERVICE_NETWORK="${SHARED_SERVICE_NETWORK}" docker compose -f "docker-compose.${PROFILE}.yml" up --build -d
