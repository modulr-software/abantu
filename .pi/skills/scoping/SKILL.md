---
name: scoping
description: Activates when the user says "scope", "lets scope", or "scope x". Takes a feature spec, produces a minimal list of changes needed, and lets the user refine before committing to a scope file.
---

# Scoping Workflow

## Activation

Trigger when the user says any variant of:
- "scope"
- "lets scope"
- "lets scope x" (where x is what to scope)
- "scope x"

## Workflow

### 1. Gather Context & Produce List

When the user specs out a feature or change:

1. Use the **ponytail** mindset at all times — brief, minimal, no fluff.
2. Gather just enough context to understand what needs to change.
3. Produce a **very brief, very precise** list of only the relevant changes.
4. If something in the codebase already exists for what they're describing, mention it, state whether it's complete, then move on.
5. Do **not** suggest anything out of scope.
6. Print the list on screen and wait for instructions.
7. Do **not** make any edits, run anything, or install anything.

### 2. Review & Corrections

The user will review the list and may:
- Make corrections
- Suggest changes
- Instruct you to edit something in advance before finalizing

Follow their exact instructions:
- Do **exactly** what they say, as briefly as possible.
- Do **not** execute anything unless asked.
- Do **not** install or configure anything unless asked.
- Do **not** suggest extra things.

### 3. Add Snippets

When the user says **"add some snippets"** :

1. Re-print your summary.
2. For each point in the list, add a relevant code snippet showing the update.
3. Keep it as brief as possible without leaving out crucial details — the user needs to read snippets in context and evaluate them.
4. Do **nothing else** — no running code, no installing, no suggesting, no questions.
5. Print the updated list with snippets and wait.

### 4. Commit Scope

When the user says **"commit this scope"** :

1. Ask the user for the filename (or they may already have said it).
2. Write the scope to an `.md` file with that name, containing the final list and code snippets in a clean, concise format.
3. Do **nothing else** — no edits, no runs, no installs.
4. Wait for further instructions.
