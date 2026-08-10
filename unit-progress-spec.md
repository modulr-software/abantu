# Unit Progress — Specification

Redefine what is served as a unit's `:progress` so it reflects **how many
times the user has meaningfully completed the unit** relative to **how many
difficulty levels the unit contains**, instead of the current raw
answer-accuracy ratio. Exercise **selection** per session is also in scope,
because the progress calculation relies on the assumption that each session
covers exactly one difficulty level (easy things first, and only those).

---

## 1. Goal

Progress should answer: *"of the number of times this unit is meant to be
repeated, how many has the user done?"* — where the number of times the unit
is meant to be repeated is derived from the **number of distinct difficulty
levels among the unit's exercises**.

---

## 2. Definitions (grounded in the schema)

- **Difficulty level of an exercise** — the `:level` column on the
  `exercises` table (`src/abantu/db/master.clj`, default `1`). Per exercise,
  not the unit's own `:level`.
- **Unit difficulty count (N)** — the count of **distinct** `:level` values
  across all exercises belonging to the unit. Examples:
  - all exercises `:level 1` → `N = 1`
  - exercises span levels `1` and `2` → `N = 2`
  - exercises span levels `1`, `2`, `3` → `N = 3`
- **Ordered difficulty levels** — the unit's distinct `:level` values sorted
  ascending: `L₁ < L₂ < … < L_N`.
- **Session** — a row in the student-DB `sessions` table for this `unit-id`.
  (Practice units use the same student-DB `sessions` / `session-answers`
  tables as lessons — confirmed.)
- **Completed session** — a session row with `completed = 1` (set by
  `sessions/end-session!`).
- **Qualifying completed session** — depends on N:
  - **N = 1**: completed **AND** the user answered **≥ 50% correct** over
    that session's answers. (For N = 1 the session serves **all** the unit's
    exercises, so the session's answers span every question the unit has —
    the denominator is the full unit.)
  - **N ≥ 2**: completed. **No correctness gate** — the 50% rule does not
    apply to multi-level units.
- **C** — the count of qualifying completed sessions for the unit,
  **all-time** (the current one-month window is dropped — confirmed). C is
  effectively capped at N: once the user has completed N qualifying
  sessions, progress is 100% and further practice does not increase C.

> The unit's own `:level` column and `:type` (`"lesson"` / `"practice"`) are
> **not** the difficulty count. `:type` does not change the formula — lesson
> and practice units are computed identically.

---

## 3. Progress formula

### General rule

```
progress = min(C, N) / N        ; capped at 100%
```

Each qualifying completed session advances progress by `1 / N`.

### 3.1 Unit with exactly 1 difficulty level (N = 1)

The unit is meant to be completed once, and that one session shows **all**
the unit's exercises.

- `C = 0` → progress **0%**
- `C ≥ 1` (≥ 1 completed session with ≥ 50% correct) → progress **100%**

All-or-nothing, gated by the 50% threshold.

### 3.2 Unit with exactly 2 difficulty levels (N = 2)

The unit is meant to be repeated twice: easy (level 1) first and only those,
then level 2.

- `C = 0` → progress **0%**
- `C = 1` → progress **50%** (first session completed)
- `C ≥ 2` → progress **100%** (second session completed)

No correctness gate; a completed session counts.

### 3.3 Unit with more than 2 difficulty levels (N > 2)

Same pattern, generalized: every qualifying completed session is one step
toward `N`.

- `C = 0` → progress **0%**
- `C = k` (k < N) → progress **k / N**
- `C ≥ N` → progress **100%**

The unit payload must also **expose `N`** so the client can display "number
of difficulty levels per unit." (See §4.)

### 3.4 Practice units (`:type "practice"`)

Identical formula and identical exercise-selection to lesson units. One
`unit-progress` and one selection rule serve both.

---

## 4. Exercise selection per session (in scope)

When a user starts a session on an N-level unit, serve **only the exercises
of one difficulty level** — the level the user is currently due for. This is
the assumption the progress calculation relies on: it makes "C completed
sessions" mean "C distinct difficulty levels completed," so `progress = C/N`
is well-defined.

Rule:

- Let `C` = the user's current qualifying-completed-session count for this
  unit (before this session).
- The session serves exercises of level `L_{min(C+1, N)}`.
  - N = 1 → serves `L₁` (all exercises). Matches §3.1.
  - N = 2, C = 0 → serves `L₁` (easy only). C = 1 → serves `L₂`.
  - N > 2 → serves `L_{C+1}` until C reaches N.
- After 100% (C = N): further sessions serve `L_N` (the highest level);
  progress stays at 100%. The user can still practice the unit — the
  progress bar does not move.

---

## 5. What the unit payload must carry

In addition to the existing `:progress`, each unit served to a student must
include:

- `:difficulty-levels` — the value `N` (distinct exercise `:level` count) for
  that unit, so the client can show "this unit has N difficulty levels."

`course-progress` (average of unit `:progress` values) is unchanged in
mechanism; its inputs now come from the new `unit-progress`.

---

## 6. Where this lands in the code (context only — not implementation)

- `stats/unit-progress` (`src/abantu/services/stats.clj`) — replace the
  accuracy-ratio body with `min(C, N) / N`. It already receives `user-id`
  and `unit-id`; it additionally needs `N` (distinct `:level` count) — either
  from the exercises already attached to the unit in the student route, or by
  querying the master `exercises` table for distinct `:level` per `unit-id`.
  C is a count of qualifying completed sessions from the student DB
  (`sessions` joined/aggregated with `session-answers` for the N=1
  correctness check), all-time.
- `start-session!` handler in `src/abantu/routes/api/student.clj` (and/or
  `sessions/start-session!`) — currently returns **all** of the unit's
  exercises. Change it to return only the exercises of the level selected by
  the §4 rule, based on the user's current C for that unit.
- `with-progress` in `src/abantu/routes/api/student.clj` — also attach
  `:difficulty-levels`.
- `end-session!` response — `:new-progress` already calls
  `stats/unit-progress`; it will reflect the new formula automatically.

---

## 7. Resolved decisions

- **50% correctness gate applies only to N = 1.** For N ≥ 2 a completed
  session counts regardless of score.
- **For N = 1 the session serves all the unit's questions**, so the 50% is
  measured over the full unit. For N ≥ 2 the 50% is not applicable.
- **Practice units use the student-DB `sessions` / `session-answers`
  tables**, same as lessons. One formula for both.
- **Exercise selection ("easy things first, and only those") is in scope** —
  it is the assumption the progress calculation depends on (§4).
- **Drop the one-month window.** Completions count all-time.
- **C caps at N.** After 100%, further practice does not change the progress
  bar; the user may still redo the unit.

---

## 8. Remaining confirm-point

- **After 100% (C = N), which level does a further practice session serve?**
  §4 defaults to `L_N` (keep serving the highest level). Alternative: restart
  at `L₁`. Confirm the default or override.
