#!/usr/bin/env bash
set -euo pipefail

# Resolve JAVA_HOME if not set — needed when script is run via `bash` (no .zshrc)
if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version >/dev/null 2>&1; then
  for candidate in \
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
    "/usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home" \
    "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"; do
    if [[ -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      break
    fi
  done
fi

if ! command -v java >/dev/null 2>&1; then
  echo "[dev] ERROR: Java not found. Install via: brew install openjdk@17"
  exit 1
fi

# Resolve pnpm — may not be in PATH when script is run via `bash` (no .zshrc)
PNPM_CMD=""
if command -v pnpm >/dev/null 2>&1; then
  PNPM_CMD="pnpm"
else
  for candidate in \
    "$HOME/.local/share/pnpm/pnpm" \
    "$HOME/Library/pnpm/pnpm" \
    "/opt/homebrew/bin/pnpm" \
    "/usr/local/bin/pnpm"; do
    if [[ -x "$candidate" ]]; then
      PNPM_CMD="$candidate"
      break
    fi
  done
fi
if [[ -z "$PNPM_CMD" ]]; then
  echo "[dev] WARNING: pnpm not found — user frontend will fall back to npm (install: npm i -g pnpm)"
fi

BACKEND_PORT=8080
PREFERRED_FRONTEND_PORT=3000
PREFERRED_ADMIN_PORT=3001
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

# Read REDISPASSWORD from .env (Redis is started with --requirepass)
REDIS_PASS="fsr_redis_pass_123"
if [[ -f "$ROOT_DIR/.env" ]]; then
  _redis_pass=$(grep -E '^REDISPASSWORD=' "$ROOT_DIR/.env" 2>/dev/null | head -1 | cut -d= -f2- | tr -d '"' | tr -d "'")
  [[ -n "$_redis_pass" ]] && REDIS_PASS="$_redis_pass"
fi

BACKEND_PID=""
FRONTEND_PID=""
ADMIN_PID=""

# Recursively kill a process tree (children first, then parent).
# Next.js/Turbopack spawns dozens of Node workers; killing only the top PID leaves them orphaned.
kill_tree() {
  local _pid=$1 _sig=${2:-TERM}
  local _children
  _children=$(pgrep -P "$_pid" 2>/dev/null || true)
  while IFS= read -r _child; do
    [[ -z "$_child" ]] && continue
    kill_tree "$_child" "$_sig"
  done <<< "$_children"
  kill -"$_sig" "$_pid" 2>/dev/null || true
}

# Kill user processes on a port, but NEVER kill Docker Desktop's own internal processes.
# Docker Desktop (com.docker.*) owns its ports for its own infra — killing it breaks Docker.
free_port() {
  local port=$1
  # Stop any user-started Docker containers that have mapped this host port
  local containers
  containers=$(docker ps --format '{{.Names}} {{.Ports}}' 2>/dev/null \
    | grep ":${port}->" | awk '{print $1}' || true)
  if [[ -n "$containers" ]]; then
    while IFS= read -r name; do
      echo "  Stopping Docker container: $name"
      docker stop "$name" >/dev/null
    done <<< "$containers"
  fi
  # Kill non-Docker-Desktop processes using this port
  local pids
  pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  if [[ -n "$pids" ]]; then
    while IFS= read -r pid; do
      [[ -z "$pid" ]] && continue
      local cmd
      cmd=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
      # Skip Docker Desktop's backend — it owns infrastructure ports and respawns immediately
      if [[ "$cmd" == *"com.docker"* ]]; then
        echo "  Skipping Docker Desktop process $pid ($cmd)"
        continue
      fi
      echo "  Killing process $pid ($cmd)"
      kill "$pid" 2>/dev/null || true
    done <<< "$pids"

    # Poll until the port is actually free (JVM needs more than 1s to release after SIGTERM).
    # Escalate to SIGKILL after 10s if the process is stubborn.
    local waited=0
    while lsof -ti tcp:"$port" >/dev/null 2>&1; do
      if [[ $waited -ge 10 ]]; then
        local remaining
        remaining=$(lsof -ti tcp:"$port" 2>/dev/null || true)
        while IFS= read -r pid; do
          [[ -z "$pid" ]] && continue
          local cmd2
          cmd2=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
          [[ "$cmd2" == *"com.docker"* ]] && continue
          echo "  Force-killing stubborn process $pid ($cmd2) on port $port"
          kill -9 "$pid" 2>/dev/null || true
        done <<< "$remaining"
        sleep 1
        break
      fi
      sleep 1
      waited=$((waited + 1))
    done
  fi
}

# Return the lowest free TCP port >= preferred.
next_free_port() {
  local port=$1
  while lsof -ti tcp:"$port" >/dev/null 2>&1; do
    port=$((port + 1))
  done
  echo "$port"
}

cleanup() {
  echo ""
  echo "[dev] Shutting down..."
  # SIGTERM the full process trees so Node/Turbopack workers don't become orphans.
  for _cpid in "$BACKEND_PID" "$FRONTEND_PID" "$ADMIN_PID"; do
    [[ -z "$_cpid" ]] && continue
    kill_tree "$_cpid" TERM
  done
  sleep 2
  # SIGKILL any survivors (JVM and stubborn Node workers ignore SIGTERM)
  for _cpid in "$BACKEND_PID" "$FRONTEND_PID" "$ADMIN_PID"; do
    [[ -z "$_cpid" ]] && continue
    kill_tree "$_cpid" KILL
  done
  # Spring Boot JVM is a child of Maven and may outlive it — force-clear port for next run
  local _jvm_pids
  _jvm_pids=$(lsof -ti tcp:"$BACKEND_PORT" 2>/dev/null || true)
  if [[ -n "$_jvm_pids" ]]; then
    while IFS= read -r _pid; do
      [[ -z "$_pid" ]] && continue
      kill -9 "$_pid" 2>/dev/null || true
    done <<< "$_jvm_pids"
  fi
}
trap cleanup EXIT INT TERM

# Stop app/frontend Docker containers first — they bind ports 8080/3000 and conflict with local dev
echo "[dev] Stopping app/frontend Docker containers (if running)..."
docker compose -f "$ROOT_DIR/docker-compose.yml" stop app frontend >/dev/null 2>&1 || true

# Ensure infrastructure is up
echo "[dev] Starting infrastructure services..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up -d mysql redis libretranslate >/dev/null 2>&1 || true

# Wait for MySQL to accept connections (port 3307 mapped locally)
echo "[dev] Waiting for MySQL on :3307..."
mysql_elapsed=0
until docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T mysql mysqladmin ping -h localhost --silent >/dev/null 2>&1; do
  if [[ $mysql_elapsed -ge 60 ]]; then
    echo "[dev] ERROR: MySQL did not become ready within 60s."
    exit 1
  fi
  sleep 2
  mysql_elapsed=$((mysql_elapsed + 2))
done
echo "[dev] MySQL ready."

# Wait for Redis to accept connections (port 6380 mapped locally)
echo "[dev] Waiting for Redis on :6380..."
redis_elapsed=0
until docker compose -f "$ROOT_DIR/docker-compose.yml" exec -T redis redis-cli -a "$REDIS_PASS" ping 2>/dev/null | grep -q PONG; do
  if [[ $redis_elapsed -ge 30 ]]; then
    echo "[dev] ERROR: Redis did not become ready within 30s."
    exit 1
  fi
  sleep 1
  redis_elapsed=$((redis_elapsed + 1))
done
echo "[dev] Redis ready."

# Free ports (skipping Docker Desktop's own processes)
for port in $BACKEND_PORT $PREFERRED_FRONTEND_PORT $PREFERRED_ADMIN_PORT; do
  if lsof -ti tcp:"$port" >/dev/null 2>&1; then
    echo "[dev] Port $port in use — freeing..."
    free_port "$port"
  fi
done

# Assign actual free ports (Docker Desktop may still hold the preferred port)
FRONTEND_PORT=$(next_free_port "$PREFERRED_FRONTEND_PORT")
ADMIN_PORT=$(next_free_port "$PREFERRED_ADMIN_PORT")
# Ensure the two frontend ports don't collide
if [[ "$ADMIN_PORT" == "$FRONTEND_PORT" ]]; then
  ADMIN_PORT=$(next_free_port "$((ADMIN_PORT + 1))")
fi

if [[ "$FRONTEND_PORT" != "$PREFERRED_FRONTEND_PORT" ]]; then
  echo "[dev] WARN: Port $PREFERRED_FRONTEND_PORT occupied (likely Docker Desktop). User frontend on :$FRONTEND_PORT"
fi
if [[ "$ADMIN_PORT" != "$PREFERRED_ADMIN_PORT" ]]; then
  echo "[dev] WARN: Port $PREFERRED_ADMIN_PORT occupied. Admin frontend on :$ADMIN_PORT"
fi

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

# Suppress macOS malloc-stack-logging noise from Node.js worker processes
unset MallocStackLogging

# Start frontends only after backend is healthy
echo "[dev] Starting user frontend  → http://localhost:$FRONTEND_PORT"
if [[ -n "$PNPM_CMD" ]]; then
  (cd "$ROOT_DIR/frontend" && "$PNPM_CMD" dev -p "$FRONTEND_PORT") &
else
  (cd "$ROOT_DIR/frontend" && npm run dev -- -p "$FRONTEND_PORT") &
fi
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
