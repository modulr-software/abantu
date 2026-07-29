#!/usr/bin/env bash
# bun-server.sh — manage a bun dev server inside a detached tmux session.
#
# Subcommands: start | stop | restart | status | logs
#
#   start [session] [-- cmd...]
#   stop  [session]
#   restart [session] [-- cmd...]
#   status [session]
#   logs [session] [-n lines]
#
# Defaults: session=bun-dev, command="bun run dev", cwd=$PWD.
set -euo pipefail

err()  { printf 'bun-server: %s\n' "$*" >&2; }
have() { command -v "$1" >/dev/null 2>&1; }

if ! have tmux; then
  err "tmux is not installed. On Arch: sudo pacman -S tmux"
  exit 1
fi
if ! have bun; then
  err "bun is not on PATH. Install from https://bun.sh"
  exit 1
fi

default_session="bun-dev"
default_cmd=(bun run dev)

# Parse the subcommand.
sub="${1:-}"; shift || true
case "$sub" in
  start|stop|restart|status|logs) ;;
  ""|-h|--help)
    sed -n '2,11p' "$0"
    exit 0 ;;
  *) err "unknown subcommand: $sub"; err "expected start|stop|restart|status|logs"; exit 2 ;;
esac

# Parse optional [session] and [-- cmd...] for commands that take them.
session="$default_session"
cmd=()
if [[ "$sub" == start || "$sub" == restart ]]; then
  if [[ $# -gt 0 && "$1" != -- ]]; then
    session="$1"; shift
  fi
  if [[ $# -gt 0 && "$1" == -- ]]; then
    shift
    cmd=("$@")
  else
    cmd=("${default_cmd[@]}")
  fi
elif [[ "$sub" == stop || "$sub" == status ]]; then
  [[ $# -gt 0 ]] && { session="$1"; shift; }
elif [[ "$sub" == logs ]]; then
  [[ $# -gt 0 && "$1" != -n && "$1" != --lines ]] && { session="$1"; shift; }
fi

# Re-parse a trailing -n <n> for logs.
lines=50
if [[ "$sub" == logs ]]; then
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -n|--lines) lines="$2"; shift 2 ;;
      *) shift ;;
    esac
  done
fi

session_exists() { tmux has-session -t "$1" 2>/dev/null; }

case "$sub" in
  start)
    if session_exists "$session"; then
      printf 'bun-server: session %q already running.\n' "$session"
      tmux list-panes -t "$session" -F '#{pane_current_command} #{pane_start_command}' 2>/dev/null \
        | head -1 || true
      printf 'View logs:  %s logs %s\n' "$0" "$session"
      printf 'Attach:     tmux attach -t %s\n' "$session"
      exit 0
    fi
    printf -v cmdstr '%q ' "${cmd[@]}"
    tmux new-session -d -s "$session" -c "$PWD" "${cmd[@]}"
    printf 'bun-server: started session %q in %s\n' "$session" "$PWD"
    printf '  command: %s\n' "${cmdstr% }"
    printf '  attach:  tmux attach -t %s\n' "$session"
    printf '  logs:    %s logs %s\n' "$0" "$session"
    ;;

  stop)
    if session_exists "$session"; then
      tmux kill-session -t "$session"
      printf 'bun-server: stopped session %q\n' "$session"
    else
      printf 'bun-server: no session %q to stop\n' "$session"
      exit 0
    fi
    ;;

  restart)
    if session_exists "$session"; then
      tmux kill-session -t "$session"
      printf 'bun-server: stopped session %q (restarting)\n' "$session"
    fi
    tmux new-session -d -s "$session" -c "$PWD" "${cmd[@]}"
    printf -v cmdstr '%q ' "${cmd[@]}"
    printf 'bun-server: restarted session %q in %s\n' "$session" "$PWD"
    printf '  command: %s\n' "${cmdstr% }"
    printf '  attach:  tmux attach -t %s\n' "$session"
    ;;

  status)
    if session_exists "$session"; then
      printf 'bun-server: session %q is RUNNING\n' "$session"
      tmux capture-pane -p -t "$session" -S -10 2>/dev/null | sed '/^$/d' | tail -10 || true
    else
      printf 'bun-server: session %q is NOT running\n' "$session"
      exit 0
    fi
    ;;

  logs)
    if ! session_exists "$session"; then
      err "no session %q" "$session"
      exit 1
    fi
    tmux capture-pane -p -t "$session" -S "-$lines" 2>/dev/null | sed '/^$/d' || true
    ;;
esac
