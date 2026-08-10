---
id: TASK-010302
title: Reference five-card evaluator
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: M
labels: [engine, rules]
depends_on: [TASK-010301]
---

## Goal

An evaluator that turns five cards into a `HandRank`, written to be obviously correct rather
than fast — the oracle every later optimisation is tested against.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — ranking rules, and the wheel.
- [`tasks/stories/STORY-0103-hand-evaluator.md`](../stories/STORY-0103-hand-evaluator.md) — why
  there are deliberately two implementations.

## Scope

- `ReferenceHandEvaluator.evaluate(cards: List<Card>): HandRank`, exactly five cards.
- Detect every category, and populate tiebreaks correctly for each:
  - straight flush — including the steel wheel `A-2-3-4-5` suited,
  - four of a kind — quad rank, then kicker,
  - full house — trips rank, then pair rank,
  - flush — all five ranks descending,
  - straight — high card, with the wheel counting as five-high,
  - three of a kind — trips rank, then two kickers,
  - two pair — high pair, low pair, kicker,
  - one pair — pair rank, then three kickers,
  - high card — five ranks descending.
- Clarity over speed throughout. Grouping, sorting and readable branches are correct here.

## Out of scope

- Seven-card hands — `TASK-010303`.
- Any optimisation — `TASK-010305`. Making this one fast would destroy its value as an oracle.
- Input validation beyond requiring five distinct cards.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../hand/HandEvaluator.kt` | create |
| `poker-engine/src/main/kotlin/.../hand/ReferenceHandEvaluator.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/ReferenceHandEvaluatorTest.kt` | create |

## Acceptance criteria

- [ ] Every category is produced for a known example hand.
- [ ] `A-2-3-4-5` is a straight, ranked below `2-3-4-5-6`.
- [ ] `Q-K-A-2-3` is **not** a straight.
- [ ] Suited `A-2-3-4-5` is a straight flush, and the lowest one.
- [ ] `A-K-Q-J-T` suited is the highest possible hand.
- [ ] The same five cards in any order evaluate identically, asserted over shuffled input.
- [ ] Two hands differing only in suit compare equal.

## Tests

- `ReferenceHandEvaluatorTest` — a table of hands in poker notation with expected categories and
  an expected ordering, read straight off the rules document.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
