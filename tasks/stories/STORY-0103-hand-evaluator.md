---
id: STORY-0103
title: Hand evaluator
type: story
status: done
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

Schema 2, and a strict chain: each ticket depends on the one above it, so exactly one is
startable at any moment.

| ID | Title | Est | Status |
| --- | --- | --- | --- |
| [TASK-010301](../tasks/TASK-010301-hand-category.md) | HandCategory, ordered low to high | XS | ready |
| [TASK-010302](../tasks/TASK-010302-hand-rank.md) | HandRank, comparable lexicographically | S | backlog |
| [TASK-010303](../tasks/TASK-010303-straight-detection.md) | Detect a straight, including the wheel | S | backlog |
| [TASK-010304](../tasks/TASK-010304-rank-groups.md) | Group ranks by count into tiebreak order | S | backlog |
| [TASK-010305](../tasks/TASK-010305-reference-evaluator.md) | Reference five-card evaluator | S | backlog |
| [TASK-010306](../tasks/TASK-010306-evaluator-rule-tests.md) | Wheel, non-wraparound, suit irrelevance | S | backlog |
| [TASK-010307](../tasks/TASK-010307-exhaustive-five-card-counts.md) | Exhaustive five-card category counts | S | backlog |
| [TASK-010308](../tasks/TASK-010308-seven-card-best-of-five.md) | Seven-card best-of-five evaluation | S | backlog |
| [TASK-010309](../tasks/TASK-010309-seven-card-brute-force-test.md) | Brute-force check of seven-card evaluation | S | backlog |
| [TASK-010310](../tasks/TASK-010310-fast-evaluator.md) | Bitmask five-card evaluator | S | backlog |
| [TASK-010311](../tasks/TASK-010311-fast-evaluator-seven-card-equivalence.md) | Fast and reference agree on seven cards | XS | backlog |

The evaluator is built from the bottom: the two subtle predicates — the straight (with the wheel)
and the rank grouping — are written and tested alone before anything assembles them into a
category, so a failure names the rule it broke rather than "the evaluator".

> ### ⚠ Open decision — DEC-002
>
> This story proves the fast evaluator **correct**, not **fast**: it asserts equivalence with the
> reference over the whole five-card space and 100 000 dealt seven-card hands, and asserts
> nothing about throughput. What performance budget the evaluator carries, how it is measured
> (a benchmark harness such as JMH would be the module's first tooling dependency), and whether
> `HandRank` gains a packed-integer representation to make evaluation allocation-free are one
> question, and it is not answered by any ADR. It stays open rather than being guessed at inside
> a ticket. Due before the simulation harness in STORY-0108 needs it.

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
