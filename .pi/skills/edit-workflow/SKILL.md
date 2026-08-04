---
name: edit-workflow
description: Activates when the user says "edit workflow", "edit flow", "lets start an edit workflow", or anything to that effect. Use only after scoping has been done — enters a step-by-step implementation loop where you only edit when told.
---

# Edit Workflow

## Activation

Trigger when the user says any variant of:
- "edit workflow"
- "edit flow"
- "lets start an edit workflow"
- "lets start the edit workflow"
- "start edit workflow"
- "start edit flow"
- "edit flow on `<file>`" (e.g. "edit flow on scope.md")

### Scope from a file

If the user says something like **"edit flow on `<file>`"** or **"edit flow on `<path>`"**:

1. Read the specified `.md` file.
2. Parse its content as the **countable, ordered list** of steps (treat markdown headings or list items as individual steps).
3. Note the file path — this is the **scope file** to update on completion.
4. Enter the edit flow loop with that list.

Otherwise, if no file is specified, take the previous suggestions we agreed on during scoping and treat them as the list (there is no scope file to update).

## The Loop

### Setup

Take the list of steps — either from the scoping phase or from the `.md` file — and treat them as a **countable, ordered list** (1, 2, 3, ...). Each item is one step of work.

### "next"

When the user says **"next"**:

1. Read the next unprocessed item from the list.
2. Paste it **verbatim** on screen so the user can see it.
3. Do **not** make any edits.
4. Wait for the user's comments.

The user may:
- Suggest improvements to the step
- Give a side quest involving an explicit edit instruction
- Say anything else relevant

Do not implement anything unless told to.

### "make these edits" / "make this edit" / "go ahead"

When the user says **"make these edits"**, **"make this edit"**, or **"go ahead"**:

1. Make the edit for the current step exactly as instructed, corrected, or pointed out — always without exception.
2. Do **not** proceed past this step.
3. Do **not** exceed the user's instructions.
4. Once done editing:
   - If a **scope file** was specified, update it to mark the current step as completed. Use `[x]` as the completion marker (or replace `[ ]` with `[x]` if the list uses checkboxes). Also update the step's text in the file to reflect any corrections or changes made during the session, so it matches what was actually implemented.
   - If multiple items in the file were touched (e.g. a side quest added new items), save the latest state of the entire list back to the file.
5. Stop and wait for further instruction.

### Adding to the list mid-process

During the process, items may be added to the list between edits. When the user gives an instruction that changes the current scope:
- If it expands on the current step, treat it as part of that step.
- If it's a new item, append it to the end of the list.
- If a scope file exists, update the file to reflect these additions immediately.

Do **not** create new separate lists. Everything goes into one running list.

### Next steps

Only proceed to the next item when the user says **"next"**. Continue until the list is implemented and the user is satisfied.

## Rules

- Do not skip ahead.
- Do not implement items until told to.
- Do not create new lists.
- Do not proceed past the current step.
