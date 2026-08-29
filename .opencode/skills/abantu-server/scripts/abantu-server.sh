#!/usr/bin/env bash
# abantu-server.sh — manage the abantu Clojure backend as a background process.
#
# Boots an nREPL (via ./nrepl.sh) inside a detached tmux session, then talks to
# it over a TCP socket with bencode nREPL ops: load-file dev/dev.clj, then eval
# (server/start-server) in the dev namespace. This is the headless equivalent of
# Calva "Load File" on dev.clj + evaluating the (server/start-server) form in
# its rich comment block.
#
# Subcommands:
#   start             boot nrepl (if not already up), load dev.clj, start server
#   stop              kill the tmux session (nrepl + server down)
#   restart           stop + start
#   status            tmux session + (server/running?) via nrepl
#   logs [-n N]       tail last N (default 50) lines of the nrepl pane
#   reload            (do (require 'dev) (dev/reload)) — hot reload, JVM stays up
#
# Run from the abantu repo root. Env: ABANTU_NREPL_SESSION (default abantu-nrepl).
set -euo pipefail

err()  { printf 'abantu-server: %s\n' "$*" >&2; }
have() { command -v "$1" >/dev/null 2>&1; }

for dep in tmux bb clojure; do
  if ! have "$dep"; then err "$dep is not installed"; exit 1; fi
done

# Must be run from the abantu repo root.
if [[ ! -f dev/dev.clj || ! -x nrepl.sh ]]; then
  err "must run from the abantu repo root (need dev/dev.clj and nrepl.sh in cwd)"
  exit 1
fi
if [[ ! -f .env ]]; then
  err ".env not found in repo root (nrepl.sh sources it for config/DB vars)"
  exit 1
fi

SELF="$(readlink -f "$0")"
SCRIPT_DIR="$(dirname "$SELF")"
CLIENT="$SCRIPT_DIR/abantu-server-client.clj"
SESSION="${ABANTU_NREPL_SESSION:-abantu-nrepl}"
PORT_FILE=".nrepl-port"

nrepl_up()      { [[ -f "$PORT_FILE" && -s "$PORT_FILE" ]]; }
session_exists() { tmux has-session -t "$SESSION" 2>/dev/null; }

wait_for_port() {
  local i
  for ((i = 0; i < 120; i++)); do
    if nrepl_up; then return 0; fi
    sleep 1
  done
  return 1
}

start_nrepl_in_tmux() {
  if session_exists; then return 0; fi
  tmux new-session -d -s "$SESSION" -c "$PWD" ./nrepl.sh
}

sub="${1:-}"; shift || true

case "$sub" in
  start)
    if nrepl_up; then
      echo "abantu-server: nrepl already up (reusing existing $PORT_FILE)"
    else
      echo "abantu-server: starting nrepl in tmux session '$SESSION'..."
      start_nrepl_in_tmux
      if ! wait_for_port; then
        err "nrepl did not write $PORT_FILE within 120s"
        err "inspect: tmux attach -t $SESSION"
        exit 1
      fi
      echo "abantu-server: nrepl up on port $(cat "$PORT_FILE")"
    fi
    echo "abantu-server: load-file dev/dev.clj + (server/start-server)..."
    if bb "$CLIENT" load-and-start; then
      echo "abantu-server: server started"
      echo "  nrepl session: tmux attach -t $SESSION"
      echo "  reload edits:  $SELF reload"
      echo "  logs:          $SELF logs"
    else
      rc=$?
      err "load-and-start failed (exit $rc)"
      err "inspect: $SELF logs  |  tmux attach -t $SESSION"
      exit "$rc"
    fi
    ;;

  stop)
    if session_exists; then
      tmux kill-session -t "$SESSION"
      echo "abantu-server: killed tmux session '$SESSION' (nrepl + server down)"
    else
      echo "abantu-server: no tmux session '$SESSION' to stop"
    fi
    ;;

  restart)
    "$SELF" stop || true
    "$SELF" start
    ;;

  status)
    if session_exists; then
      echo "abantu-server: tmux session '$SESSION' RUNNING"
    else
      echo "abantu-server: tmux session '$SESSION' NOT running"
    fi
    if nrepl_up; then
      echo "abantu-server: nrepl port = $(cat "$PORT_FILE")"
      bb "$CLIENT" status || true
    else
      echo "abantu-server: no $PORT_FILE (nrepl not up)"
    fi
    ;;

  logs)
    if ! session_exists; then err "no tmux session '$SESSION'"; exit 1; fi
    lines=50
    while [[ $# -gt 0 ]]; do
      case "$1" in
        -n|--lines) lines="$2"; shift 2 ;;
        *) shift ;;
      esac
    done
    tmux capture-pane -p -t "$SESSION" -S "-$lines" 2>/dev/null | sed '/^$/d' || true
    ;;

  reload)
    if ! nrepl_up; then err "no $PORT_FILE; run '$SELF start' first"; exit 1; fi
    bb "$CLIENT" reload
    ;;

  ""|-h|--help)
    sed -n '2,28p' "$SELF"
    exit 0
    ;;

  *)
    err "unknown subcommand: $sub"
    err "expected start|stop|restart|status|logs|reload"
    exit 2
    ;;
esac
