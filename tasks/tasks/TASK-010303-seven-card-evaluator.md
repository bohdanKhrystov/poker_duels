---
id: TASK-010303
title: Seven-card best-of-five evaluation
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: S
labels: [engine, rules]
depends_on: [TASK-010302]
---

## Goal

Evaluate the two hole cards plus five board cards into the best five-card hand available.

## Context

- [`tasks/tasks/TASK-010302-reference-evaluator.md`](TASK-010302-reference-evaluator.md).

## Scope

- `evaluate(seven: List<Card>): HandRank` — the maximum over all 21 five-card subsets.
- Also return *which* five cards were used. The client needs to highlight them at showdown, and
  recomputing that later would mean duplicating this logic.
- Neither hole card need be used; the board can play in full.

## Out of scope

- Speed — `TASK-010305`.
- Six-card or four-card evaluation. Hold'em never needs them.
- Deciding who wins a pot — STORY-0106.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../hand/ReferenceHandEvaluator.kt` | modify |
| `poker-engine/src/test/kotlin/.../hand/SevenCardEvaluationTest.kt` | create |

## Acceptance criteria

- [ ] A seven-card hand ranks equal to the best of its 21 subsets, verified by brute force over
      generated input.
- [ ] The board playing in full is handled: neither hole card is required.
- [ ] The returned five cards are a subset of the seven and evaluate to the returned rank.
- [ ] Order of input never affects the result.
- [ ] Fewer or more than seven cards is rejected.

## Tests

- `SevenCardEvaluationTest` — brute-force agreement over 10 000 random seven-card hands, plus
  named cases: board plays, one hole card plays, a counterfeited two pair.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
