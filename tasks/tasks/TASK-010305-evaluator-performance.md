---
id: TASK-010305
title: Fast evaluator and performance budget
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: M
labels: [engine, performance]
depends_on: [TASK-010304]
---

## Goal

An evaluator fast enough that a million-hand simulation is an overnight job rather than a
week-long one — proven equal to the reference, not merely believed to be.

## Context

- [`tasks/tasks/TASK-010304-evaluator-test-suite.md`](TASK-010304-evaluator-test-suite.md) — the
  contract suite this implementation must pass unchanged.

## Scope

- A faster `HandEvaluator`: rank and suit bitmasks, packed integer ranks, table lookups —
  whatever the measurements justify.
- Run the existing `HandEvaluatorContract` against it, unmodified.
- **Equivalence test**: agreement with the reference on all 2 598 960 five-card hands. Not a
  sample — the whole space. It is affordable and it is conclusive.
- A benchmark reporting evaluations per second, and a documented budget with the number the
  implementation actually achieves.

## Out of scope

- Multithreading. Parallelism belongs to the simulation harness, not the evaluator.
- Deleting the reference evaluator. It stays forever as the oracle.
- Micro-optimising anything that has not been measured.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../hand/FastHandEvaluator.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/FastEvaluatorEquivalenceTest.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/EvaluatorBenchmark.kt` | create |

## Acceptance criteria

- [ ] The fast evaluator passes `HandEvaluatorContract` with no modification to the contract.
- [ ] It agrees with the reference on all 2 598 960 five-card hands.
- [ ] It agrees with the reference on 1 000 000 random seven-card hands.
- [ ] It is measurably faster than the reference, and the factor is recorded in the KDoc.
- [ ] No allocation per evaluation beyond the returned rank.
- [ ] The benchmark runs on demand, not on every build.

## Tests

- `FastEvaluatorEquivalenceTest` — exhaustive agreement.
- `EvaluatorBenchmark` — throughput, reported not asserted, so a slow CI machine cannot make the
  build red.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
