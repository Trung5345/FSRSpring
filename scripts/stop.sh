#!/usr/bin/env bash
set -euo pipefail

BACKEND_PORT=8080
FRONTEND_PORT=3000
ADMIN_PORT=3001
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "[stop] Stopping all FSRSpring services..."

for port in $BACKEND_PORT $FRONTEND_PORT $ADMIN_PORT; do
  pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
  if [[ -n "$pids" ]]; then
    echo "$pids" | while read -r pid; do
      cmd=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
      echo "  Killing PID $pid ($cmd) on port $port"
      kill "$pid" 2>/dev/null || true
    done
  fi
done

echo "[stop] Stopping Docker infrastructure..."
docker compose -f "$ROOT_DIR/docker-compose.yml" stop mysql redis libretranslate >/dev/null 2>&1 || true

echo "[stop] Done."
