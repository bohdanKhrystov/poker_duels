---
id: TASK-010204
title: Determinism and shuffle distribution tests
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: S
labels: [engine, test, determinism]
depends_on: [TASK-010203]
---

## Goal

Evidence that the shuffle is both reproducible and not obviously biased — the two ways this
component can be wrong.

## Context

- [`tasks/tasks/TASK-010203-rng-and-deck.md`](TASK-010203-rng-and-deck.md).

## Scope

- A locked-in regression test: a fixed seed produces one specific, hardcoded card ordering. If
  the PRNG or the shuffle ever changes, this test breaks loudly, because such a change
  invalidates every replay ever stored.
- Distribution check: over many shuffles, each card lands in each position at close to uniform
  frequency. A chi-squared style tolerance, tuned to be stable — a flaky test here is worse than
  no test.
- Independence check: the first card of the deck is uniformly distributed across seeds.
- All statistical tests use fixed seeds, so a failure is reproducible.

## Out of scope

- Proving the PRNG is cryptographically strong. It is not, and does not need to be — the seed
  never leaves the server during a hand.
- Benchmarks — `TASK-010305` covers performance, and for the evaluator rather than the deck.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/.../card/DeckDeterminismTest.kt` | create |
| `poker-engine/src/test/kotlin/.../card/ShuffleDistributionTest.kt` | create |

## Acceptance criteria

- [ ] Seed `42` produces a hardcoded ordering, and the test comment explains that changing it
      is a breaking change to stored replays.
- [ ] Position frequencies over 100 000 shuffles fall inside the stated tolerance.
- [ ] The distribution tests use fixed seeds and cannot flake.
- [ ] The whole suite runs in under ten seconds.

## Tests

- `DeckDeterminismTest`, `ShuffleDistributionTest` — as above.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
