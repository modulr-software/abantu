# abantu

Always load the `ponytail` skill before responding to any prompt in this repo.

When editing any Clojure (`.clj`) file under this project, load the
`clj-reload-workflow` skill before making changes. Follow the workflow it
describes — assume the user's nREPL is already running (`./nrepl.sh`), make
edits normally, and rely on the reload plugin to re-evaluate namespaces after
each edit. Do not start a separate nREPL.
