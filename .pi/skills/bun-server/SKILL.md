---
name: bun-server
description: >-
  Start, stop, restart, and inspect bun dev servers running in a detached tmux
  session. Use when the user says "start the bun server", "launch bun", "run
  the backend", "start the dev server in tmux", "background the bun server",
  or otherwise asks to run a bun server in the background via tmux. Also use
  for "stop the bun server", "restart bun", "show bun server logs", "tail the
  bun server", or "is the bun server running".
---

# bun-server

Manages a bun dev server inside a detached tmux session so it keeps running
in the background after the agent exits. All tmux plumbing is wrapped in
`scripts/bun-server.sh` — call that, never raw `tmux`.

## Prerequisites

- `tmux` must be installed (`sudo pacman -S tmux` on Arch). The script
  checks and errors clearly if it's missing.
- `bun` must be on `$PATH` and the current working directory must be the
  project root (the server runs with `cwd` = wherever the script is invoked
  from).

## Usage

```bash
./scripts/bun-server.sh start                 # start 'bun-dev' session running 'bun run dev'
./scripts/bun-server.sh start my-api          # named session 'my-api', default command
./scripts/bun-server.sh start my-api -- bun run start:dev   # custom command
./scripts/bun-server.sh status                # is it running? show last lines
./scripts/bun-server.sh status my-api
./scripts/bun-server.sh logs                  # tail last 50 lines
./scripts/bun-server.sh logs my-api -n 200    # last 200 lines
./scripts/bun-server.sh restart               # stop + start with same defaults
./scripts/bun-server.sh stop                  # kill the session
```

## How to use this skill

1. Determine the session name. Default is `bun-dev`. If the user names a
   service (e.g. "start the api server", "start the websocket server"),
   use a descriptive session name like `bun-api` / `bun-ws` so multiple
   servers can coexist.
2. Determine the command. Default is `bun run dev`. If the project uses a
   different script (check `package.json` `scripts`), pass it after `--`.
3. Run `./scripts/bun-server.sh start <name> -- <cmd>` from the project
   root.
4. Report back: the session name, the command, and how to attach
   (`tmux attach -t <name>`) or view logs
   (`./scripts/bun-server.sh logs <name>`). Do not attach yourself — the
   agent cannot hold an interactive tmux session; the user attaches in
   their own terminal.
5. If the user asks to stop/restart/logs/status, run the matching
   subcommand. For "is it running?", use `status` and report the result.

## Defaults

| Thing | Default |
|-------|---------|
| session name | `bun-dev` |
| command | `bun run dev` |
| working dir | current `$PWD` (run from project root) |

## Notes

- The script is idempotent on `start`: if the session already exists it
  reports that and shows the running command instead of starting a
  duplicate.
- `logs` uses `tmux capture-pane` so it works without attaching.
- Never run `tmux attach` from the agent — it blocks. Tell the user the
  command instead.
