---
name: abantu-server
description: >-
  Start, stop, restart, reload, and inspect the abantu Clojure backend server
  running in a detached tmux session, without opening an editor. Boots an nREPL
  via ./nrepl.sh in tmux, connects over a TCP socket, and sends bencode ops to
  load-file dev/dev.clj (equivalent of Calva "Load File") then evaluate
  (server/start-server) from the rich comment block. Use when the user says
  "start the abantu server", "start the backend", "boot the clojure server",
  "run abantu without the editor", "stop the abantu server", "restart abantu",
  "reload abantu", "is the abantu server running", or otherwise asks to manage
  the abantu Clojure server lifecycle. Only use in the abantu repo.
---

# abantu-server

Manages the abantu Clojure backend as a background process: an nREPL server
runs in a detached tmux session, and this skill talks to it over a TCP socket
with bencode nREPL ops — exactly what Calva does when you "Load File" on
`dev/dev.clj` and then evaluate `(server/start-server)` in its rich comment
block. No editor required.

All plumbing is wrapped in `scripts/abantu-server.sh` (tmux + orchestration)
and `scripts/abantu-server-client.clj` (the bencode nREPL client, modeled on
the repo's `scripts/reload.clj`). Call the shell script; never drive tmux or
the nREPL socket by hand.

## Prerequisites

- `tmux`, `bb` (babashka), and `clojure` on `$PATH`.
- Run from the **abantu repo root** (must contain `dev/dev.clj`, `nrepl.sh`,
  and `.env`). The script checks and errors if not.
- `.env` must exist (nrepl.sh sources it for DB/config vars).

## Usage

```bash
./scripts/abantu-server.sh start       # boot nrepl in tmux, load dev.clj, start server
./scripts/abantu-server.sh stop        # kill the tmux session (nrepl + server down)
./scripts/abantu-server.sh restart     # stop + fresh start
./scripts/abantu-server.sh status      # tmux session + (server/running?) via nrepl
./scripts/abantu-server.sh logs        # tail last 50 lines of the nrepl pane
./scripts/abantu-server.sh logs -n 200 # tail last 200 lines
./scripts/abantu-server.sh reload      # (do (require 'dev) (dev/reload)) — hot reload
```

The tmux session name defaults to `abantu-nrepl`; override with
`ABANTU_NREPL_SESSION=<name>`.

## How to use this skill

1. Determine the action from the user's request (start / stop / restart /
   status / logs / reload).
2. Run `./scripts/abantu-server.sh <action>` from the repo root
   (`/home/merv/Developer/abantu`). Show the full output.
3. For `start`: the script starts nrepl in tmux, waits for `.nrepl-port`,
   sends an nREPL `load-file` op with the contents of `dev/dev.clj`
   (this is the Calva "Load File" equivalent — it evaluates the `ns` form,
   `(clj-reload/init ...)`, and the `defn`s; the rich comment block is
   skipped), then sends an `eval` op for `(server/start-server)` in the
   `dev` namespace (the form that lives in the rich comment block).
   `server/start-server` already guards against double-start, so `start`
   is idempotent.
4. If nrepl is already up (`.nrepl-port` present, e.g. the user ran
   `./nrepl.sh` themselves or Calva is connected), `start` reuses it and
   only does the load-file + start-server step. It does NOT start a
   duplicate nrepl. This matches the `clj-reload-workflow` skill's rule:
   assume the user's nrepl may already be running.
5. For `reload`: this runs `(do (require 'dev) (dev/reload))`, which is the
   same as `bb scripts/reload.clj`. clj-reload unloads changed namespaces,
   `before-ns-unload` stops the server if running, `after-ns-reload` starts
   it again. Use this after editing Clojure files to hot-reload without
   restarting the JVM. The `clj-reload-workflow` skill governs the
   edit-reload loop in detail — defer to it for the reload-after-edit
   rules.
6. For `stop`: kills the tmux session, which tears down the nrepl JVM and
   the http-kit server together. There is no graceful-only-server stop
   here; if you need to stop just the http server but keep nrepl, run
   `bb scripts/abantu-server-client.clj stop-server` instead.
7. Report back the session name, the port (from `.nrepl-port`), and how to
   attach (`tmux attach -t abantu-nrepl`) or view logs. Do not attach
   yourself — the agent cannot hold an interactive tmux session; the user
   attaches in their own terminal.

## Defaults

| Thing | Default |
|-------|---------|
| tmux session | `abantu-nrepl` (`ABANTU_NREPL_SESSION` to override) |
| nrepl launcher | `./nrepl.sh` (sources `.env`, runs `clojure -M:nrepl`) |
| file loaded | `dev/dev.clj` |
| server start form | `(server/start-server)` evaluated in namespace `dev` |

## Notes

- `start` waits up to 120s for `.nrepl-port` to appear (cold JVM startup).
  If it times out, point the user at `tmux attach -t abantu-nrepl` to see
  the Clojure stacktrace.
- `logs` uses `tmux capture-pane` so the agent can read output without
  attaching.
- Never run `tmux attach` from the agent — it blocks. Tell the user the
  command instead.
- This skill is for the abantu repo only; it verifies `dev/dev.clj` and
  `nrepl.sh` are present in the cwd.
