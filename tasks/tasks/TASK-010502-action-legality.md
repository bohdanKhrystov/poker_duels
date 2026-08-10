---
id: TASK-010502
title: Action legality and min-raise arithmetic
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010501]
---

## Goal

Every action is either applied correctly or rejected with a reason — and the min-raise
arithmetic, which is where poker engines usually go wrong, is right.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — the betting section, in full.
- [`tasks/stories/STORY-0105-betting-rounds.md`](../stories/STORY-0105-betting-rounds.md) — the
  list of traps.

## Scope

- `legalActions(state): LegalActions` — the complete set for the seat to act.
- Apply `Fold`, `Check`, `Call`, `Bet`, `Raise`, `AllIn`, each emitting its event and moving
  chips.
- Minimum bet is one big blind.
- **Minimum raise is the largest raise increment already made on this street**, not the current
  bet. Bet 10, raise to 40 — the increment is 30, so the next raise is to at least 70.
- A `Call` facing more than the caller's stack is a call for the whole stack, all-in.
- **A short all-in does not reopen betting** for a seat that has already faced a full raise:
  that seat may call, not re-raise. `legalActions` must reflect this.
- The big blind may raise preflop when the action has merely been called.

## Out of scope

- Detecting that a round is over — `TASK-010503`.
- Awarding the pot — STORY-0106.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/BettingRules.kt` | create |
| `poker-engine/src/main/kotlin/.../game/DefaultPokerEngine.kt` | modify |
| `poker-engine/src/test/kotlin/.../game/ActionLegalityTest.kt` | create |
| `poker-engine/src/test/kotlin/.../game/MinRaiseTest.kt` | create |

## Acceptance criteria

- [ ] Checking while facing a bet is rejected as `ActionNotAllowed`.
- [ ] Acting out of turn is rejected as `NotYourTurn`.
- [ ] A raise below the minimum is rejected as `AmountTooSmall`, and the rejection names the
      minimum.
- [ ] A raise above the acting seat's stack is rejected as `AmountTooLarge`.
- [ ] Min-raise after bet 10 → raise 40 is 70, asserted directly.
- [ ] A short all-in leaves a previously-full-raise-facing seat with call but not raise.
- [ ] The big blind may raise preflop after a call.
- [ ] Every rejection leaves the state byte-identical to the input.
- [ ] Chip conservation holds after every applied action.

## Tests

- `ActionLegalityTest` — a table of state, action and expected outcome.
- `MinRaiseTest` — worked raise sequences with expected minima.
- Property: no sequence of legal actions ever produces a negative stack.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
