---
name: commit-and-push
description: Use when the user says "commit and push" (or close variants like "commit & push", "push my changes", "ship it"). Runs the full stage → commit → push workflow against the current branch. Never invoke proactively — only when the user explicitly asks to commit and push.
---

# commit-and-push

Triggered verbatim by the phrase "commit and push" (and close variants). Run
the following steps **in order**, in the project root. Do not skip steps, do
not summarize output, do not run anything else alongside.

## Step 0 — scan for temporary / dev-only code

Before touching git, scan the working tree for anything that looks like
temporary or development-only scaffolding that should not reach prod:

- `println` / `prn` / `console.log` / `print` statements that look like
  debug noise (not real, intentional logging).
- `TODO` / `FIXME` / `XXX` / `HACK` markers introduced this session.
- Commented-out code blocks.
- `ponytail:` self-check markers the user did not ask to keep.
- Hardcoded secrets, local file paths, or machine-specific values.

**Do not remove anything on your own judgement.** For every candidate you
find, quote the file:line and the offending lines back to the user and ask
"Remove this before committing? (yes/no)". Wait for an explicit answer per
item. Only remove what the user approves. If the user says "no" or
"skip", leave it in and move on. If there is nothing to ask about, say so
in one line and proceed to Step 1.

After any removals the user approved, run the repo's reload step
(`bb scripts/reload.clj` from the project root) if Clojure files were
touched, so the running server stays in sync. Skip if no Clojure files
changed.

## Step 1 — inspect what will be committed

Run, in parallel, and show full output:

- `git status`
- `git diff` (unstaged changes)
- `git diff --staged` (anything already staged)
- `git log --oneline -10` (recent commits, for message-style reference)

Do not skip or truncate any of the above. The user needs to see it all.

## Step 2 — stage everything

```
git add .
```

Show the full output of the command.

## Step 3 — write the commit message

Build the message from the actual diff (Step 1), not from memory of the
session. The message must be a **summary of the changes since the last
commit** — what files changed and what the substantive change in each is.

Rules:
- Imperative mood, present tense ("Fix arg order in set-password flow",
  not "Fixed ...").
- First line ≤ 72 chars, a concise headline.
- If the change spans more than one logical concern, add a blank line
  then a short bulleted body (one bullet per concern, ≤ 72 chars each).
- No `Co-Authored-By`, no `Generated with`, no agent-attribution lines
  unless the user explicitly asks for them.
- Match the tone of recent `git log` entries — if the repo uses short
  one-liners, keep it a one-liner; if it uses detailed bodies, do that.

Commit with:

```
git commit -m "<message>"
```

If the message has a body, use multiple `-m` flags
(`git commit -m "<headline>" -m "<body>"`). Show the full output of the
commit command, including any pre-commit hook output.

## Step 4 — push

```
git push
```

Show the full output. If the push is rejected (non-fast-forward, hook
failure, auth error), stop and report the raw error to the user — do not
attempt `--force`, do not attempt to rebase, do not retry on your own.
Hand the failure back to the user and wait.

## Step 5 — report back

After the push returns, print a short summary block:

```
Done.
Branch:    <current branch>
Commit:    <short hash> <headline>
Pushed to: <remote/branch>
Files:     <N> changed
```

Then stop. Do not open a follow-up task, do not suggest next steps unless
the user asks. Hand control back to the user.

## Hard constraints

- **Never use `--force`, `--no-verify`, `-i`/interactive, or amend a
  prior commit.** If something needs amending, ask the user first.
- **Never commit secrets.** If `git diff` shows anything that looks like
  an API key, token, password, or `.env` contents staged for commit,
  stop and surface it to the user before running `git commit`.
- **Never push without committing first**, and never commit without
  staging first. Order is fixed: stage → commit → push.
- **Show every command's full terminal output to the user.** No
  truncation, no "output omitted", no summarizing stdout/stderr. If a
  command produces a lot of output, that's fine — let it all through.
- **One push per invocation.** No retry loops. If push fails, hand back
  to the user with the raw error.
