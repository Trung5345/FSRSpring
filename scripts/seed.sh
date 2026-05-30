#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────
# seed.sh — (Re)seed vocabulary data and trigger enrichment
#
# Usage:
#   ./scripts/seed.sh              # seed + enrich (default)
#   ./scripts/seed.sh --no-enrich  # seed only
#   ./scripts/seed.sh --enrich-only # skip SQL, trigger enrich
# ──────────────────────────────────────────────────────────────
set -euo pipefail

# ── Config (override via env vars) ────────────────────────────
DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_HOST_PORT:-3307}"
DB_NAME="${MYSQL_DATABASE:-fsrspring}"
DB_USER="${MYSQL_USER:-fsr_user}"
DB_PASS="${MYSQL_PASSWORD:-fsr_pass}"

APP_HOST="${APP_HOST:-localhost}"
APP_PORT="${APP_HOST_PORT:-8080}"
APP_BASE="http://${APP_HOST}:${APP_PORT}"

SQL_FILE="$(dirname "$0")/../src/main/resources/data.sql"

SKIP_SQL=false
SKIP_ENRICH=false

for arg in "$@"; do
  case "$arg" in
    --no-enrich)   SKIP_ENRICH=true ;;
    --enrich-only) SKIP_SQL=true ;;
  esac
done

# ── Helpers ───────────────────────────────────────────────────
info()  { echo "[seed] $*"; }
warn()  { echo "[seed] WARN: $*" >&2; }
die()   { echo "[seed] ERROR: $*" >&2; exit 1; }

require_cmd() { command -v "$1" &>/dev/null || die "'$1' not found. Please install it."; }

# ── Step 1: Run SQL seed ───────────────────────────────────────
if [[ "$SKIP_SQL" == "false" ]]; then
  require_cmd mysql

  [[ -f "$SQL_FILE" ]] || die "SQL file not found: $SQL_FILE"

  info "Connecting to MySQL at ${DB_HOST}:${DB_PORT} (db: ${DB_NAME}) ..."
  mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$SQL_FILE" \
    && info "SQL seed applied successfully." \
    || die "MySQL seed failed. Check connection and credentials."

  # Quick word count
  WORD_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" \
    -sN -e "SELECT COUNT(*) FROM words;" 2>/dev/null || echo "?")
  info "Total words in DB: ${WORD_COUNT}"
fi

# ── Step 2: Trigger enrichment pipeline ───────────────────────
if [[ "$SKIP_ENRICH" == "false" ]]; then
  require_cmd curl

  info "Triggering enrichment pipeline at ${APP_BASE}/api/dictionary/enrich-all ..."

  HTTP_STATUS=$(curl -s -o /tmp/enrich_response.json -w "%{http_code}" \
    -X POST "${APP_BASE}/api/dictionary/enrich-all" \
    -H "Content-Type: application/json" \
    --connect-timeout 10 --max-time 30 || echo "000")

  if [[ "$HTTP_STATUS" == "200" ]]; then
    QUEUED=$(cat /tmp/enrich_response.json 2>/dev/null | grep -oP '"queued"\s*:\s*\K[0-9]+' || echo "?")
    info "Enrichment triggered. Words queued: ${QUEUED}"
    info "The enrichment poller will process jobs in the background (check app logs)."
  elif [[ "$HTTP_STATUS" == "000" ]]; then
    warn "Could not reach app at ${APP_BASE}. Is the server running?"
    warn "Start it with: ./mvnw spring-boot:run  OR  docker compose up"
    warn "Then run: curl -X POST ${APP_BASE}/api/dictionary/enrich-all"
  else
    warn "Enrichment endpoint returned HTTP ${HTTP_STATUS}."
    warn "Response: $(cat /tmp/enrich_response.json 2>/dev/null)"
  fi
fi

info "Done."
