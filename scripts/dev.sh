#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5433}"
DB_NAME="${DB_NAME:-analizator}"

export DB_URL="${DB_URL:-jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"
export DB_USER="${DB_USER:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export DB_DRIVER="${DB_DRIVER:-org.postgresql.Driver}"

BACKEND_PID=""

cleanup() {
  if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID"
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

echo "Checking PostgreSQL at ${DB_HOST}:${DB_PORT}/${DB_NAME}"
if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -d "$DB_NAME" -U "$DB_USER" >/dev/null 2>&1; then
  cat <<EOF
PostgreSQL is not available at ${DB_HOST}:${DB_PORT}/${DB_NAME}.

Start the Docker database first:
  sudo docker rm -f analizator-postgres 2>/dev/null || true
  sudo docker run --name analizator-postgres \\
    -e POSTGRES_DB=analizator \\
    -e POSTGRES_USER=postgres \\
    -e POSTGRES_PASSWORD=postgres \\
    -p 5433:5432 \\
    -d postgres:16

Then run:
  ./scripts/dev.sh
EOF
  exit 1
fi

echo "Starting backend on http://localhost:8080"
./gradlew :backend:run &
BACKEND_PID="$!"

echo "Waiting for backend health check"
for attempt in {1..30}; do
  if curl -fsS "http://localhost:8080/health" >/dev/null 2>&1; then
    break
  fi

  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    wait "$BACKEND_PID" 2>/dev/null || true
    echo "Backend stopped during startup. Check the backend error above."
    exit 1
  fi

  if [[ "$attempt" == "30" ]]; then
    echo "Backend did not become healthy at http://localhost:8080/health."
    exit 1
  fi

  sleep 1
done

echo "Starting frontend on http://localhost:8081"
./gradlew :frontend:wasmJsBrowserDevelopmentRun
