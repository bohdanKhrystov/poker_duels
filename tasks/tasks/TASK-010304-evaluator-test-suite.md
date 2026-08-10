---
id: TASK-010304
title: Exhaustive and property test suite for the evaluator
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: M
labels: [engine, test, rules]
depends_on: [TASK-010303]
---

## Goal

Enough evidence to trust the evaluator completely, since a subtle error here corrupts every game
silently and forever.

## Context

- [`tasks/stories/STORY-0103-hand-evaluator.md`](../stories/STORY-0103-hand-evaluator.md) — why
  this component is tested harder than anything else.

## Scope

- **Exhaustive**: evaluate all 2 598 960 distinct five-card hands. Assert that the resulting
  ranks partition into exactly the known number of equivalence classes, and that the count of
  hands in each category matches the published figures exactly:

  | Category | Hands |
  | --- | --- |
  | straight flush | 40 |
  | four of a kind | 624 |
  | full house | 3 744 |
  | flush | 5 108 |
  | straight | 10 200 |
  | three of a kind | 54 912 |
  | two pair | 123 552 |
  | one pair | 1 098 240 |
  | high card | 1 302 540 |

  These numbers are a near-complete correctness proof: almost any misclassification moves hands
  between categories and breaks at least two of the counts.
- **Properties**: order independence; suit symmetry (permuting suits consistently across a hand
  never changes its rank); adding a card to seven never lowers the rank.
- Tag the exhaustive test so it can be excluded from the fast local loop but always runs in CI.

## Out of scope

- Performance measurement — `TASK-010305`.
- Testing the fast evaluator, which does not exist yet. This suite is written against the
  interface so that it applies to both.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/.../hand/ExhaustiveFiveCardTest.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/EvaluatorPropertyTest.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/HandEvaluatorContract.kt` | create |

## Acceptance criteria

- [ ] All nine category counts match exactly across all 2 598 960 hands.
- [ ] Suit-permutation symmetry holds over generated hands.
- [ ] Order independence holds over generated hands.
- [ ] The suite is written against the `HandEvaluator` interface and can be run against any
      implementation.
- [ ] The exhaustive test completes in under two minutes and is tagged for CI.

## Tests

As above; `HandEvaluatorContract` is the reusable body that `TASK-010305` will run against the
fast implementation.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
