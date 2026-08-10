---
id: STORY-0103
title: Hand evaluator
type: story
status: ready
parent: EPIC-01
module: poker-engine
labels: [engine, rules]
depends_on: [STORY-0102]
---

## Goal

Given seven cards, produce a `HandRank` that compares correctly against any other `HandRank`,
so that showdowns and ties resolve exactly as the rules require.

## Why

This is the single most correctness-critical component in the project, and the one where a bug
is least likely to announce itself. An evaluator that misranks one hand in ten thousand will
never crash — it will just quietly award pots to the wrong player forever. It therefore gets
heavier testing than anything else in the engine, including exhaustive verification against a
brute-force reference.

## Design notes

- `HandRank` is comparable and carries its `HandCategory` plus the tiebreak ranks in descending
  significance. Comparison is lexicographic: category first, then kickers.
- Two implementations, deliberately:
  - `ReferenceHandEvaluator` — obvious, slow, straightforwardly correct by inspection.
  - the real one — faster, and tested by agreeing with the reference on generated input.
  The reference is not throwaway code; it stays as the test oracle.
- Aces are high, except in the wheel `A-2-3-4-5`, which is the lowest straight. There is no
  wraparound straight `Q-K-A-2-3`. This is the classic off-by-one in every evaluator.
- Suits never break ties. Two hands with identical ranks compare equal, and the caller splits
  the pot.
- No allocation in the hot path beyond the returned rank; the simulation harness will call this
  tens of millions of times.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010301](../tasks/TASK-010301-hand-rank-model.md) | HandCategory and comparable HandRank | ready |
| [TASK-010302](../tasks/TASK-010302-reference-evaluator.md) | Reference five-card evaluator | backlog |
| [TASK-010303](../tasks/TASK-010303-seven-card-evaluator.md) | Seven-card best-of-five evaluation | backlog |
| [TASK-010304](../tasks/TASK-010304-evaluator-test-suite.md) | Exhaustive and property test suite | backlog |
| [TASK-010305](../tasks/TASK-010305-evaluator-performance.md) | Fast evaluator and performance budget | backlog |

## Acceptance criteria

- [ ] Every hand category is detected correctly, including the wheel as the lowest straight.
- [ ] `Q-K-A-2-3` is **not** a straight.
- [ ] Kickers decide within a category; suits never do.
- [ ] Identical hands of different suits compare equal.
- [ ] The fast evaluator agrees with the reference on all 2 598 960 distinct five-card hands.
- [ ] Seven-card evaluation equals the best of all 21 five-card subsets, verified by brute force
      on generated input.
- [ ] Category frequencies over a large random sample match the published probabilities within
      tolerance — a cheap check that catches whole classes of misclassification.

## Out of scope

- Equity, outs, odds, EV — EPIC-08.
- Deciding who wins a *pot*: this story ranks hands, STORY-0106 awards chips.
- Short deck or any other variant.
