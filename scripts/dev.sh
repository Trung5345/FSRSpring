#!/usr/bin/env bash
set -euo pipefail

BACKEND_PORT=8080
FRONTEND_PORT=3000
ADMIN_PORT=3001
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

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
  echo "[dev] Shutting down frontends..."
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

# Start user frontend on port 3000
echo "[dev] Starting user frontend  → http://localhost:$FRONTEND_PORT"
(cd "$ROOT_DIR/frontend" && pnpm dev -p "$FRONTEND_PORT") &
FRONTEND_PID=$!

# Start admin frontend on port 3001
echo "[dev] Starting admin frontend → http://localhost:$ADMIN_PORT"
(cd "$ROOT_DIR/admin-frontend" && npm run dev -- -p "$ADMIN_PORT") &
ADMIN_PID=$!

echo "[dev] ─────────────────────────────────────────"
echo "[dev]   User UI  → http://localhost:$FRONTEND_PORT"
echo "[dev]   Admin UI → http://localhost:$ADMIN_PORT"
echo "[dev]   API      → http://localhost:$BACKEND_PORT"
echo "[dev] ─────────────────────────────────────────"

# Start Spring Boot in foreground (clean first to avoid stale class files)
echo "[dev] Starting Spring Boot (local profile)..."
cd "$ROOT_DIR"
./mvnw clean spring-boot:run -Dspring-boot.run.profiles=local "$@"
