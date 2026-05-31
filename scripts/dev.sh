#!/usr/bin/env bash
set -euo pipefail

PORT=8080

free_port() {
  local port=$1

  # Docker containers using this port
  local containers
  containers=$(docker ps --format '{{.Names}} {{.Ports}}' 2>/dev/null \
    | grep ":${port}->" | awk '{print $1}' || true)

  if [[ -n "$containers" ]]; then
    echo "$containers" | while read -r name; do
      echo "  Stopping Docker container: $name"
      docker stop "$name" >/dev/null
    done
  fi

  # Non-Docker processes using this port
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

# Ensure infrastructure is up
echo "[dev] Starting infrastructure services..."
docker compose up -d mysql redis libretranslate >/dev/null 2>&1 || true

# Free port 8080
echo "[dev] Checking port $PORT..."
if lsof -ti tcp:"$PORT" >/dev/null 2>&1; then
  echo "[dev] Port $PORT in use — freeing..."
  free_port "$PORT"
  echo "[dev] Port $PORT freed."
else
  echo "[dev] Port $PORT is available."
fi

# Run Spring Boot
echo "[dev] Starting Spring Boot (local profile)..."
exec ./mvnw spring-boot:run -Dspring-boot.run.profiles=local "$@"
