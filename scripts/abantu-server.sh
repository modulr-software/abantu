#!/usr/bin/env bash
# Manage the abantu Clojure backend in a detached tmux session.
# Actions: start | stop | restart | status | logs | reload
set -euo pipefail

SESSION="${ABANTU_NREPL_SESSION:-abantu-nrepl}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

for f in dev/dev.clj nrepl.sh .env; do
  if [[ ! -e "$f" ]]; then
    echo "[abantu-server] missing $f in $ROOT" >&2
    exit 1
  fi
done

port_file="$ROOT/.nrepl-port"
client="bb $ROOT/scripts/abantu-server-client.clj"

nrepl-port() { [[ -s "$port_file" ]] && cat "$port_file" || true; }
nrepl-up()   { [[ -s "$port_file" ]] && lsof -iTCP:"$(nrepl-port)" -sTCP:LISTEN >/dev/null 2>&1 || true; }

wait-for-port() {
  local waited=0
  while [[ ! -s "$port_file" ]] && (( waited < 120 )); do
    sleep 1; waited=$((waited+1))
  done
  if [[ ! -s "$port_file" ]]; then
    echo "[abantu-server] timed out waiting for .nrepl-port; attach with: tmux attach -t $SESSION" >&2
    return 1
  fi
  echo "[abantu-server] nrepl port: $(cat "$port_file")"
}

start() {
  if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "[abantu-server] tmux session $SESSION already exists"
  else
    tmux new-session -d -s "$SESSION" -c "$ROOT" "./nrepl.sh"
    echo "[abantu-server] started nrepl in tmux session $SESSION"
    wait-for-port
  fi
  if ! nrepl-up; then
    echo "[abantu-server] .nrepl-port present but no listener; attach to debug: tmux attach -t $SESSION" >&2
    return 1
  fi
  echo "[abantu-server] loading dev/dev.clj (load-file)..."
  $client load-file
  echo "[abantu-server] starting server..."
  $client start-server
}

stop() {
  if tmux has-session -t "$SESSION" 2>/dev/null; then
    tmux kill-session -t "$SESSION"
    echo "[abantu-server] killed tmux session $SESSION"
  else
    echo "[abantu-server] no tmux session $SESSION"
  fi
  rm -f "$port_file"
}

status() {
  if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "[abantu-server] tmux session $SESSION: running"
  else
    echo "[abantu-server] tmux session $SESSION: not running"
  fi
  local p; p="$(nrepl-port)"
  if [[ -n "$p" ]]; then echo "[abantu-server] .nrepl-port: $p"; fi
  if nrepl-up; then
    echo "[abantu-server] (server/running?) =>"
    $client running
  fi
}

logs() {
  local n="${1:-50}"
  if tmux has-session -t "$SESSION" 2>/dev/null; then
    tmux capture-pane -p -t "$SESSION" -S -"$n"
  else
    echo "[abantu-server] no tmux session $SESSION"
  fi
}

reload() {
  if ! nrepl-up; then
    echo "[abantu-server] nrepl not running; start first" >&2
    exit 1
  fi
  bb "$ROOT/scripts/reload.clj"
}

case "${1:-}" in
  start)   start ;;
  stop)    stop ;;
  restart) stop || true; start ;;
  status)  status ;;
  logs)    shift; logs "${1:-50}" ;;
  reload)  reload ;;
  *) echo "usage: $0 {start|stop|restart|status|logs [n]|reload}" >&2; exit 1 ;;
esac
