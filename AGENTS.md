# abantu

Always load the `ponytail` skill before responding to any prompt in this repo.

Always load the `stay-in-context` skill before responding to any prompt in
this repo. It is always on: take the single reading the current sentence
and session support (never plural-interpret a word into every dictionary
possibility), ask if genuinely ambiguous instead of enumerating paths, and
use only the context the user specifies plus this session's conversation —
anything outside that is not relevant. Never assume the user's emotional
state from a correction.

Always load the `corrections` skill before responding to any prompt in this
repo. It is always on and governs two things: never assume the user's
emotional state when corrected (a correction is a fact to act on, not a tone
to read), and validate every conclusion about data/state/existence through
the repo's real machinery (service functions + the honey/DB interface the
services use) rather than improvised queries that bypass the established
path.

When editing any Clojure (`.clj`) file under this project, load the
`clj-reload-workflow` skill before making changes. Follow the workflow it
describes — assume the user's nREPL is already running (`./nrepl.sh`), make
edits normally, and rely on the reload plugin to re-evaluate namespaces after
each edit. Do not start a separate nREPL.

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
