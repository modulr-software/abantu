---
description: Commit all task-relevant changes with a summary message
agent: build
---
!`git status`
!`git diff`
!`git diff --staged`
!`git log --oneline -10`

Scan the diff above for temporary or development-only code that should not be committed:
- `println` / `prn` / `console.log` / `print` statements that look like debug noise
- `TODO` / `FIXME` / `XXX` / `HACK` markers introduced this session
- Commented-out code blocks
- Hardcoded secrets, local file paths, or machine-specific values

For every candidate you find, quote the file:line and the offending lines back to the user and ask "Remove this before committing? (yes/no)". Wait for an explicit answer per item. Only remove what the user approves. If the user says "no" or "skip", leave it in and move on. If there is nothing to ask about, say so in one line and proceed.

After any removals the user approved, stage all changes relevant to the current task and commit them in one commit. Write the message from the diff above — not from memory of the session:
- Imperative mood, present tense ("Fix arg order in set-password flow", not "Fixed ...")
- First line ≤ 72 chars, a concise headline
- If the change spans more than one logical concern, add a blank line then a short bulleted body (one bullet per concern, ≤ 72 chars each)
- No `Co-Authored-By`, no `Generated with`, no agent-attribution lines
- Match the tone of recent `git log` entries

Commit with `git commit -m "<message>"`. If the message has a body, use multiple `-m` flags. Show the full output of the commit command, including any pre-commit hook output.
