# abantu

Always load the `ponytail` skill before responding to any prompt in this repo.

When editing any Clojure (`.clj`) file under this project, load the
`clj-reload-workflow` skill before making changes. Follow the workflow it
describes — assume the user's nREPL is already running (`./nrepl.sh`), make
edits normally, and rely on the reload plugin to re-evaluate namespaces after
each edit. Do not start a separate nREPL.

When the user says "commit and push" (or a close variant like "commit &
push", "push my changes", "ship it"), load the `commit-and-push` skill
and follow its instructions exactly. Do not start the workflow until the
phrase is used — this skill is trigger-only, never proactive.

When the user says "scope", "lets scope", or "scope x" (where x is what to
scope), load the `scoping` skill and follow its workflow. Trigger-only — do
not start until one of those phrases is used.

When the user says "edit workflow", "edit flow", "lets start an edit
workflow", or "edit flow on `<file>`", load the `edit-workflow` skill and
follow its step-by-step loop. This runs only after scoping — trigger-only,
never proactive.

Load the `suggest-only` skill for any task where the user is exploring or
asking what to do (e.g. "how should we", "what's the best way", "can we",
"what would it take"). Only suggest — never write, edit, or create files —
unless the user explicitly asks for an edit ("do it", "make the change",
"go ahead"). This is a default-on guardrail for planning conversations.
