---
name: clj-reload-workflow
description: Use when editing Clojure (.clj) files in the abantu repo. Describes the interactive reload workflow wired up via clj-reload, the nREPL bridge, and the opencode plugin that runs (dev/reload) after every edit.
---

# clj-reload workflow in abantu

## Overview

abantu is a Clojure backend with an interactive reload workflow built on
[`clj-reload`](https://github.com/tonsky/clj-reload). The goal: every time a
Clojure source file is edited, the running server is cleanly stopped, the
changed namespaces (and dependents) are unloaded and reloaded in topological
order, and the server is started again — all without restarting the JVM or
losing REPL state.

## Components

1. **`dev/dev.clj`** — the dev namespace. Requires `clj-reload.core` and
   `abantu.server`. Calls `(clj-reload/init {:dirs ["src" "dev" "test"]})`
   at load time. Defines:
   - `dev/reload` → calls `clj-reload.core/reload`
   - `dev/before-ns-unload` → stops the server if running (unload hook)
   - `dev/after-ns-reload` → starts the server (reload hook)

   The hooks fire automatically when `dev` itself is reloaded by
   `clj-reload`. Since `dev` requires `abantu.server`, `dev` unloads first
   (stop server) and loads last (start server after `abantu.server` is fresh).

2. **`deps.edn`** — `:dev` alias pulls in
   `io.github.tonsky/clj-reload {:mvn/version "1.0.0"}`. The user's
   `~/.clojure/deps.edn` `:nrepl` alias already brings clj-reload too.

3. **`./nrepl.sh`** — user runs this from the terminal. It loads `.env` and
   starts `clojure -M:nrepl`, writing `.nrepl-port` to the project root.
   Calva (VS Code) connects to this same nREPL server. **Do not start a
   separate nREPL** — always assume the user's is running.

4. **`scripts/reload.clj`** — babashka nREPL client. Reads `.nrepl-port`,
   opens a TCP socket to `localhost:<port>`, bencode-encodes an `eval` op
   for `(do (require 'dev) (dev/reload))`, drains responses until the
   `done` status, prints stdout/stderr, exits non-zero on `eval-error` /
   `syntax-error` / `unknown-op`. Run it manually with:
   ```
   bb scripts/reload.clj
   ```
   from the project root. If `.nrepl-port` is missing, it prints a clear
   message and exits 1 — do not try to start nREPL yourself; ask the user
   to run `./nrepl.sh`.

5. **`.opencode/plugins/reload.ts`** — auto-discovered opencode plugin that
   registers a `tool.execute.after` hook. After every `edit` or `write` tool
   call, it checks for `.nrepl-port`, spawns `bb scripts/reload.clj` with
   `cwd = worktree`, and appends `[clj-reload] (exit N)` + stdout/stderr
   to the tool result's content. Silently does nothing if the user's
   nREPL isn't running.

## Workflow to follow when editing Clojure in abantu

1. **Assume the user's nREPL is already running** via `./nrepl.sh`. Do not
   start your own. Calva and the plugin share that one server.

2. **Every edit must leave the project in a reloadable state.** Never make
   an edit — partial, incomplete, or otherwise — that would cause a syntax
   or runtime error during `(dev/reload)`. Each `edit` / `write` call is
   followed automatically by a reload, and that reload must succeed (exit
   0) with all unloaded namespaces loading cleanly. Treat this as a hard
   constraint on how you sequence edits: if a change requires multiple
   steps, order and combine them so that every intermediate file state is
   valid Clojure that compiles and reloads without error.

3. **Make edits normally.** After each `edit` / `write` to any file in the
   repo, the extension runs `(dev/reload)` automatically. You'll see the
   reload output appended to the edit tool result, e.g.:
   ```
   [clj-reload] (exit 0)
   Unloading dev
   Unloading abantu.server
   Loading abantu.server
   Loading dev
   Starting server on port 3001...
   ```

   **Skip the reload when it would be a no-op.** If a new file is being
   added that has no link (no `:require` or other reference) to any
   namespace that is already loaded, clj-reload will have nothing to
   unload or reload — do not run the hook in that case. However, when a
   new file is added AND it is linked into an actively-loaded namespace
   (e.g. a `:require` of an existing namespace, or an existing namespace
   adds a `:require` for the new one), always run the hook.

4. **If reload output says `no .nrepl-port`**, the user's nREPL isn't up.
   Ask them to run `./nrepl.sh` — do not attempt to start one yourself.

5. **If reload exits non-zero** (exit 1), that is the top priority — drop
   everything else. Read the appended stderr, which will contain the
   compile error / stacktrace from Clojure. **Revert the edit that caused
   the failure immediately.** Do not attempt to fix forward by piling on
   more edits — revert first, then assess why the error happened, then
   redo the edit correctly. Repeat this revert → assess → redo cycle
   until clj-reload exits 0 and the server starts again. Keep iterating
   until `[clj-reload] (exit 0)` appears. Only once the reload is green
   again should you stop and re-validate whether the original task goal
   is still satisfied. If the fix you applied diverges from the intended
   design, adapt the generated code so it both (a) fulfills the task
   requirements and (b) keeps clj-reload loading cleanly — never
   sacrifice the reload workflow to force a feature through. The local
   dev toolchain's ability to reload and give feedback is the source of
   truth for "is this code correct right now"; do not break it.

7. **You can also trigger a reload manually** with:
   ```
   bb scripts/reload.clj
   ```
   from the project root (`workdir=/home/merv/Developer/abantu`). Use this
   if you need to reload without making an edit (e.g. after the user
   reports a change made in Calva, or to verify server state).

   **If the server is not running after a change, bring it back up.**
   After any bash command that modifies Clojure files (e.g. `mv`, `cp`,
   `git checkout`), the reload extension does not fire — so the server may
   be left stopped. Always follow up with `touch src/abantu/server.clj &&
   bb scripts/reload.clj` to force a reload and restart the server.
   Verify "Starting server on port ..." appears in the output. If it
   doesn't, keep retrying until the server is confirmed up.

8. **Do not hold onto old var references.** clj-reload replaces whole
   namespaces, so any var resolved at plugin/skill load time will be stale
   after a reload. When you need the current version of a var, re-resolve
   it via `(resolve 'full.ns/sym)`. In practice this matters for the bb
   script (which always re-evals `(require 'dev)`) and for any ad-hoc REPL
   evals — always re-eval fresh forms rather than caching.

9. **`dev` namespace is the entry point.** Anything you want to call
   through the reload workflow goes through `dev` — e.g.
   `(do (require 'dev) (dev/reload))`, `(server/running?)` after
   `(require 'abantu.server :as server)`. Don't add a `:require` for
   reloadable namespaces to your own scratch ns and expect it to see new
   versions after reload.

## Verifying reload worked

- `[clj-reload] (exit 0)` in the edit output = reload call succeeded.
- "Starting server on port ..." appears when `after-ns-reload` fired.
- "Stopping server..." appears when `before-ns-unload` fired.
- "Nothing to unload / Nothing to reload" = no files changed since the
  last reload (still a successful call; safe to ignore).
- A handler's new behaviour visible via an HTTP request = the router was
  rebuilt with fresh route fns (the real proof reload took effect).

## Files involved

- `dev/dev.clj` — hooks + `reload` entry point
- `deps.edn` — `:dev` alias with clj-reload dep
- `scripts/reload.clj` — babashka nREPL client
- `.opencode/plugins/reload.ts` — opencode plugin that triggers reload
- `.nrepl-port` — written by `./nrepl.sh`, read by both the extension
  and the bb script
