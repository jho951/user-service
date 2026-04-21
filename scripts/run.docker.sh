#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ACTION="${1:-up}"
ENV_NAME="${2:-dev}"
shift $(( $# > 0 ? 1 : 0 )) || true
shift $(( $# > 0 ? 1 : 0 )) || true

usage() {
  echo "Usage: ./scripts/run.docker.sh [up|down|build|logs|ps|restart] [dev|prod] [docker compose options]" >&2
}

case "$ACTION" in
  up|down|build|logs|ps|restart) ;;
  *) usage; exit 1 ;;
esac

case "$ENV_NAME" in
  dev|prod)
    COMPOSE_FILE="$PROJECT_ROOT/docker/compose.yml"
    ENV_COMPOSE_FILE="$PROJECT_ROOT/docker/$ENV_NAME/compose.yml"
    ;;
  *) usage; exit 1 ;;
esac

gradle_property() {
  local key="$1"
  local gradle_properties="${HOME}/.gradle/gradle.properties"
  [[ -f "$gradle_properties" ]] || return 0
  awk -F= -v key="$key" '$1 == key { print $2; exit }' "$gradle_properties"
}

ENV_FILE="$PROJECT_ROOT/.env.$ENV_NAME"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

if [[ -z "${GH_TOKEN:-}" ]]; then
  GH_TOKEN="$(gradle_property githubPackagesToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property githubToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property ghToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property gh_token)"
  export GH_TOKEN
fi

if [[ -z "${GITHUB_ACTOR:-}" ]]; then
  GITHUB_ACTOR="$(gradle_property githubPackagesUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="$(gradle_property githubUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="jho951"
  export GITHUB_ACTOR
fi

if [[ -z "${GITHUB_TOKEN:-}" && -n "${GH_TOKEN:-}" ]]; then
  export GITHUB_TOKEN="$GH_TOKEN"
fi

COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-user-service-${ENV_NAME}}"
SHARED_NETWORK="${SERVICE_SHARED_NETWORK:-${BACKEND_SHARED_NETWORK:-${MSA_SHARED_NETWORK:-service-backbone-shared}}}"
if ! docker network inspect "$SHARED_NETWORK" >/dev/null 2>&1; then
  echo "Creating external docker network: $SHARED_NETWORK"
  docker network create "$SHARED_NETWORK" >/dev/null
fi

compose() {
  SERVICE_SHARED_NETWORK="$SHARED_NETWORK" BACKEND_SHARED_NETWORK="$SHARED_NETWORK" MSA_SHARED_NETWORK="$SHARED_NETWORK" \
    docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" -f "$ENV_COMPOSE_FILE" "$@"
}

case "$ACTION" in
  up) compose up --build -d "$@" ;;
  down) compose down --remove-orphans "$@" ;;
  build) compose build "$@" ;;
  logs) compose logs -f "$@" ;;
  ps) compose ps "$@" ;;
  restart) compose down --remove-orphans && compose up --build -d "$@" ;;
esac
