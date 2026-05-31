#!/usr/bin/env bash
set -euo pipefail

BACKEND_PORT=8080
FRONTEND_PORT=3000
ADMIN_PORT=3001
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

BACKEND_PID=""
FRONTEND_PID=""
ADMIN_PID=""

free_port() {
  local port=$1
  local containers
  containers=$(docker ps --format '{{.Names}} {{.Ports}}' 2>/dev/null \
    | grep ":${port}->" | awk '{print $1}' || true)
  if [[ -n "$containers" ]]; then
    echo "$containers" | while read -r name; do
      echo "  Stopping Docker container: $name"
      docker stop "$name" >/dev/null
    done
  fi
  local pids
  pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  if [[ -n "$pids" ]]; then
    echo "$pids" | while read -r pid; do
      local cmd
      cmd=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
      echo "  Killing process $pid ($cmd)"
      kill "$pid" 2>/dev/null || true
    done
    sleep 1
  fi
}

cleanup() {
  echo ""
  echo "[dev] Shutting down..."
  [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "$FRONTEND_PID" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
  [[ -n "$ADMIN_PID" ]] && kill "$ADMIN_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# Ensure infrastructure is up
echo "[dev] Starting infrastructure services..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d mysql redis libretranslate >/dev/null 2>&1 || true

# Free all ports
for port in $BACKEND_PORT $FRONTEND_PORT $ADMIN_PORT; do
  if lsof -ti tcp:"$port" >/dev/null 2>&1; then
    echo "[dev] Port $port in use — freeing..."
    free_port "$port"
  fi
done

# Start Spring Boot in background FIRST (clean to avoid stale class files)
echo "[dev] Starting Spring Boot (local profile) — compiling..."
cd "$ROOT_DIR"
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local "$@" &
BACKEND_PID=$!

# Wait for Spring Boot to be healthy before starting frontends.
# This prevents the Next.js proxy from returning 500 while the backend is still starting.
echo "[dev] Waiting for Spring Boot on :$BACKEND_PORT (this may take ~60s on first run)..."
elapsed=0
until curl -sf "http://localhost:$BACKEND_PORT/api/words/count" >/dev/null 2>&1; do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo ""
    echo "[dev] ERROR: Spring Boot exited unexpectedly. Check logs above."
    exit 1
  fi
  if [[ $elapsed -ge 300 ]]; then
    echo ""
    echo "[dev] ERROR: Spring Boot did not become ready within 5 minutes."
    exit 1
  fi
  printf "."
  sleep 3
  elapsed=$((elapsed + 3))
done
echo ""
echo "[dev] Spring Boot is ready!"

# Start frontends only after backend is healthy
echo "[dev] Starting user frontend  → http://localhost:$FRONTEND_PORT"
(cd "$ROOT_DIR/frontend" && pnpm dev -p "$FRONTEND_PORT") &
FRONTEND_PID=$!

echo "[dev] Starting admin frontend → http://localhost:$ADMIN_PORT"
(cd "$ROOT_DIR/admin-frontend" && npm run dev -- -p "$ADMIN_PORT") &
ADMIN_PID=$!

echo "[dev] ─────────────────────────────────────────"
echo "[dev]   User UI  → http://localhost:$FRONTEND_PORT"
echo "[dev]   Admin UI → http://localhost:$ADMIN_PORT"
echo "[dev]   API      → http://localhost:$BACKEND_PORT"
echo "[dev] ─────────────────────────────────────────"

# Keep script alive — exit only when Spring Boot exits
wait "$BACKEND_PID" || true
