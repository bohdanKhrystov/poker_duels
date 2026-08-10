---
id: TASK-010703
title: Match conclusion and duel result
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
labels: [engine, duel]
depends_on: [TASK-010702]
---

## Goal

A duel ends, once, with a declared winner — and it always ends.

## Context

- [`docs/vision.md`](../../docs/vision.md) — the duel coin is the reward, and one duel produces
  exactly one.

## Scope

- Evaluate the end condition after every hand.
- `Freezeout` ends when one seat has every chip; `FixedHands` ends after the last hand, with the
  larger stack winning.
- `DuelResult`: winner seat, hand count, final stacks, and the reason the match ended.
- `MatchFinished` event carrying the result.
- A `FixedHands` match that ends level is a draw, and `DuelResult` must be able to say so
  rather than picking arbitrarily.
- No further action is accepted once a match is finished; attempts are rejected.

## Out of scope

- Awarding a duel coin to an account — EPIC-05. The engine reports a winner; it does not know
  what a player is.
- Rating changes — EPIC-05.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../duel/DuelResult.kt` | create |
| `poker-engine/src/main/kotlin/.../duel/MatchProgression.kt` | modify |
| `poker-engine/src/test/kotlin/.../duel/MatchConclusionTest.kt` | create |

## Acceptance criteria

- [ ] A freezeout ends exactly when one seat holds every chip.
- [ ] A fixed-hands match ends after the configured number of hands, larger stack winning.
- [ ] A level fixed-hands match reports a draw rather than a winner.
- [ ] `MatchFinished` is emitted exactly once.
- [ ] Actions after `MatchFinished` are rejected and change nothing.
- [ ] Over 10 000 simulated freezeouts with the default format, every match terminates well
      inside a generous hand ceiling — and the ceiling is asserted, not assumed.

## Tests

- `MatchConclusionTest` — both end conditions, the draw case, post-match actions.
- Property: termination over generated matches under the default format.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
