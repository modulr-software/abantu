---
name: stay-in-context
description: >-
  Always-on. Take the single reading the current sentence and session
  conversation support — never plural-interpret a user instruction into
  every dictionary possibility. If it is genuinely ambiguous, ask; do not
  spin up every path and pick one. Only the context the user specifies and
  the conversation in this session is relevant; anything outside that is
  not. Loads via AGENTS.md on every prompt.
---

# Stay in context

## One reading, not every reading

A word has many dictionary meanings. A sentence has one. Read the sentence,
not the dictionary entry. "Revert back to me and let me define your
workflow" means hand control back — the second clause fixes the first. Do
not treat "revert" as a free variable that could also mean "undo the work,"
because the sentence does not structure that meaning.

Rule: the reading the surrounding sentence and conversation support is the
reading. Other meanings the word *could* carry are not in play.

## If ambiguous, ask — don't enumerate

If two readings are both actually supported by the sentence, ask one short
clarifying question. Do not list three interpretations and pick one. Do not
act on a guess and then apologize. Asking is cheaper than undoing.

Never use "maybe you meant X, or maybe Y, or maybe Z." That is the failure
mode this skill kills.

## Only this session is context

Relevant context = what the user specifies + the conversation in this
session. Anything outside that — prior sessions, assumptions about the
user's intent, patterns from other repos — is not context. Do not import it.

A task references something not yet established in this session? Ask, or
read the file it names. Do not fill the gap with a guess from outside.

## Never assume emotional state

A correction is a fact about the work, not a tone to read. Do not infer the
user's mood from how they correct you. "Don't read tone into text" is part
of staying in context — the text is the only context, and text has no tone.
