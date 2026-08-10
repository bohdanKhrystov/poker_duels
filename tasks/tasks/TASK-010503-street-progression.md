---
id: TASK-010503
title: Betting round completion and street advance
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010502]
---

## Goal

A hand moves from preflop to flop to turn to river to showdown, dealing the right cards at the
right moment and knowing exactly when a round is finished.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — the order of play. Note that no burn cards
  are dealt, deliberately.

## Scope

- Detect the end of a betting round: both seats have acted, and their committed amounts are
  equal — or one is all-in and the other has matched or folded.
- Advance the street, resetting per-street state: current bet, committed-this-street, minimum
  raise, and the seat to act.
- Deal the board: three cards for the flop, one each for the turn and river, from the deck in
  the state.
- Emit `BettingRoundEnded` and `StreetDealt`.
- When both seats are all-in, run the remaining board out with no further action and go straight
  to showdown.
- When a seat folds, the hand ends immediately with no further cards dealt.

## Out of scope

- Awarding the pot at showdown — STORY-0106.
- Anything after the hand ends — STORY-0107.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/StreetProgression.kt` | create |
| `poker-engine/src/main/kotlin/.../game/DefaultPokerEngine.kt` | modify |
| `poker-engine/src/test/kotlin/.../game/StreetProgressionTest.kt` | create |

## Acceptance criteria

- [ ] Preflop ends when the big blind checks its option or the bet is matched.
- [ ] Each street deals exactly the right number of cards, and none is ever a duplicate.
- [ ] Per-street state resets on advance; total-committed-this-hand does not.
- [ ] Both seats all-in runs the board out with no further action.
- [ ] A fold ends the hand immediately and deals nothing further.
- [ ] The seat to act after the flop is the non-button.
- [ ] A full hand — preflop through river — can be played to showdown in a test.

## Tests

- `StreetProgressionTest` — round completion in each of its forms, board dealing, all-in run-out,
  fold on each street.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
