#!/usr/bin/env bash
# mem-watchdog.sh — Auto-kill a process when its total RSS exceeds a memory limit.
#
# Usage:
#   bash scripts/mem-watchdog.sh <ROOT_PID> <LIMIT_MB> <NAME>
#
# Environment overrides:
#   CHECK_INTERVAL  seconds between RSS polls   (default: 10)
#   GRACE_SECONDS   SIGTERM → SIGKILL wait time (default: 5)
#   WATCHDOG_LOG    log file path               (default: stderr only)
#
# Behaviour mirrors Docker --memory:
#   - Sums RSS of the process + its entire descendant tree (e.g. Maven→JVM,
#     npm→Node workers, Turbopack sub-processes).
#   - SIGTERM first; SIGKILL after GRACE_SECONDS if still alive.
#   - Exits cleanly when the watched process exits on its own.
#
# NOTE: We kill the *entire* process subtree (children-first, then parent) so
# that Turbopack/Node workers do not survive as orphans after the root PID dies.

set -euo pipefail

ROOT_PID="${1:?Usage: $0 ROOT_PID LIMIT_MB NAME}"
LIMIT_MB="${2:?Usage: $0 ROOT_PID LIMIT_MB NAME}"
NAME="${3:-pid-$ROOT_PID}"
CHECK_INTERVAL="${CHECK_INTERVAL:-10}"
GRACE_SECONDS="${GRACE_SECONDS:-5}"
WATCHDOG_LOG="${WATCHDOG_LOG:-}"

PREFIX="[watchdog/${NAME}]"

log() {
  local msg
  msg="$(date '+%Y-%m-%dT%H:%M:%S') $PREFIX $*"
  echo "$msg" >&2
  [[ -n "$WATCHDOG_LOG" ]] && echo "$msg" >> "$WATCHDOG_LOG"
}

# Walk the full process subtree rooted at ROOT_PID using a single ps snapshot.
# Returns all descendant PIDs (including ROOT_PID) ordered leaves-first so that
# signals reach children before parents — avoids zombie bursts on SIGTERM.
list_subtree_pids() {
  local root=$1
  ps -A -o pid=,ppid= 2>/dev/null | awk -v root="$root" '
  {
    pid  = $1 + 0
    ppid = $2 + 0
    children[ppid] = children[ppid] " " pid
  }
  END {
    # BFS from root to collect all descendants
    queue[0] = root + 0; qlen = 1; seen[root + 0] = 1; order_len = 0
    while (qlen > 0) {
      cur = queue[--qlen]
      order[order_len++] = cur
      n = split(children[cur], kids, " ")
      for (i = 1; i <= n; i++) {
        k = kids[i] + 0
        if (k > 0 && !(k in seen)) { seen[k] = 1; queue[qlen++] = k }
      }
    }
    # Print in reverse BFS order (leaves first, root last)
    for (i = order_len - 1; i >= 0; i--) print order[i]
  }'
}

# Send signal to every process in the subtree (children first, then root).
# Mirrors dev.sh kill_tree but uses a single ps snapshot for efficiency.
kill_subtree() {
  local root=$1
  local sig=${2:-TERM}
  local pids
  pids=$(list_subtree_pids "$root")
  local count=0
  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    if kill -"$sig" "$pid" 2>/dev/null; then
      count=$((count + 1))
    fi
  done <<< "$pids"
  log "kill_subtree SIG${sig} — sent to ${count} process(es) in tree rooted at PID ${root}"
}

# Collect total RSS (KB) for the subtree rooted at ROOT_PID.
# Uses a single `ps` snapshot parsed by awk — no recursive shell calls,
# so it handles deep/wide trees (e.g. Turbopack workers) efficiently.
subtree_rss_kb() {
  local root=$1
  ps -A -o pid=,ppid=,rss= 2>/dev/null | awk -v root="$root" '
  {
    pid  = $1 + 0
    ppid = $2 + 0
    rss  = $3 + 0
    children[ppid] = children[ppid] " " pid
    rss_of[pid]    = rss
  }
  END {
    queue[0] = root + 0
    qlen     = 1
    total    = 0
    seen[root + 0] = 1
    while (qlen > 0) {
      cur = queue[--qlen]
      if (cur in rss_of) total += rss_of[cur]
      n = split(children[cur], kids, " ")
      for (i = 1; i <= n; i++) {
        k = kids[i] + 0
        if (k > 0 && !(k in seen)) {
          seen[k] = 1
          queue[qlen++] = k
        }
      }
    }
    print total
  }'
}

log "started — limit: ${LIMIT_MB} MB, interval: ${CHECK_INTERVAL}s, grace: ${GRACE_SECONDS}s"

while kill -0 "$ROOT_PID" 2>/dev/null; do
  total_kb=$(subtree_rss_kb "$ROOT_PID")
  total_mb=$(( total_kb / 1024 ))

  if [[ $total_mb -gt $LIMIT_MB ]]; then
    log "OVER LIMIT — ${total_mb} MB > ${LIMIT_MB} MB — killing full subtree of PID ${ROOT_PID}"
    kill_subtree "$ROOT_PID" TERM

    elapsed=0
    while kill -0 "$ROOT_PID" 2>/dev/null && [[ $elapsed -lt $GRACE_SECONDS ]]; do
      sleep 1
      elapsed=$(( elapsed + 1 ))
    done

    if kill -0 "$ROOT_PID" 2>/dev/null; then
      log "SIGKILL — PID ${ROOT_PID} still alive after ${GRACE_SECONDS}s — force-killing subtree"
      kill_subtree "$ROOT_PID" KILL
    else
      log "PID ${ROOT_PID} subtree exited after SIGTERM"
    fi
    exit 0
  fi

  sleep "$CHECK_INTERVAL"
done

log "PID ${ROOT_PID} exited on its own — watchdog done"
