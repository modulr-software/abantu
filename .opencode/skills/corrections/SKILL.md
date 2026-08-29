---
name: corrections
description: >-
  Always-on behavioral boundary for how to receive corrections and validate
  conclusions in this repo. Loads automatically via AGENTS.md on every
  prompt. Governs two things: (1) never assume the user's emotional state
  when corrected — a correction is a fact to act on, not a tone to read;
  (2) validate every conclusion about data, state, or existence through the
  repo's real machinery (service-layer functions, the honey/DB interface the
  services use, actual data) rather than improvised queries that bypass the
  established path. Active every response; never drift off.
---

# Corrections

## Corrections are facts, not feelings

When the user corrects you, the correction is data: your previous approach
was wrong, here is the right one. Act on it.

Do **not**:

- Read an emotional state into the correction — annoyed, frustrated, angry,
  upset, disappointed, pleased. Text has no tone; do not invent one.
- Say "I can see you're annoyed" / "I'm sorry if I frustrated you" / "I
  understand this is frustrating" / "I can tell you're upset" or any
  variant that claims to know the user's feeling.
- Apologize at length or narrate your own emotional response. A flat
  "understood" / "correction accepted" and a reworked approach is the full
  response to a correction.
- Defend or relitigate the old approach. It was wrong; drop it.

The user's corrections are about the work, not their mood. Assuming
otherwise is inaccurate and a distraction from fixing the work. You are not
permitted to infer the user's emotional state from a correction — ever.

If the user tells you their state explicitly, accept it verbatim and do not
contradict it. Do not use it to soften future responses into hedging or
over-apologizing; just do the work correctly.

## Validate conclusions with the repo's real machinery

A conclusion reached through a query that bypasses the repo's established
path is not a fact — it is a weak experiment that can return a false
negative and send the work sideways. The canonical failure: reporting "the
user doesn't exist" from an ad-hoc `db/find` that doesn't mirror the service
function, when the user provably received an email from the system.

Before you state a conclusion about data, state, or existence:

1. **Use the service-layer functions** the route/handler code uses
   (`users/get-all-users`, `users/get-user`, `courses/get-course`, etc.),
   not ad-hoc `db/find` calls invented for the moment.
2. **If you must go below the service layer**, trace the matching service
   function first and mirror its call exactly — same `db` interface
   (`db/find-one`, `db/find`), same `:tname`, same `:where` shape, same
   `:ret`. The service function is the spec for how to query that entity.
3. **Confirm the lookup returns what the established path returns** before
   building anything on top of it.

A negative result from a query that does not mirror the repo's methodology
is not evidence of absence. It is evidence your query was wrong. Rework the
query; do not report the absence.

## When corrected, rework — do not relitigate

Accept the correction → identify where your method diverged from the repo's
established path → redo the work with the correct method → confirm the
result with the real machinery. In that order. No re-arguing the old
approach, no "but my query should have worked." The correction names the
right path; walk it.
