---
id: TASK-010203
title: Seeded Rng and an immutable Deck
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: M
labels: [engine, domain, determinism]
depends_on: [TASK-010202]
---

## Goal

A deck that shuffles reproducibly from a seed and deals without mutating itself — the mechanism
every replay and every simulation in the project depends on.

## Context

- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
  — why randomness is an injected value rather than an ambient fact.
- [`docs/architecture.md`](../../docs/architecture.md) — the determinism rule.

## Scope

- `Rng` interface: `nextInt(bound: Int): Int`, plus the state needed to reproduce it.
- One implementation with an explicit, documented algorithm — a small named PRNG such as
  xoshiro or a linear congruential generator, written out in the module. It must **not** delegate
  to `kotlin.random.Random`, whose sequence is not contractually stable across Kotlin versions
  and would silently invalidate stored replays on an upgrade.
- `Rng` is immutable in the same style as everything else: advancing it returns a new `Rng`
  alongside the value.
- `Deck`: created full and ordered, `shuffled(rng)` returning a new deck and the advanced rng,
  `deal(n)` returning the cards and the remaining deck.
- Fisher–Yates, implemented explicitly. The algorithm is part of the durable contract and the
  KDoc must say so.

## Out of scope

- Cryptographic randomness or seed secrecy. Where the seed comes from and when it may be
  published is a server concern — see
  [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md).
- Dealing to players, boards, or burn cards — STORY-0104 onward.
- Determinism tests beyond the basics — `TASK-010204`.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../random/Rng.kt` | create |
| `poker-engine/src/main/kotlin/.../card/Deck.kt` | create |
| `poker-engine/src/test/kotlin/.../card/DeckTest.kt` | create |

## Acceptance criteria

- [ ] A fresh deck contains all 52 cards exactly once.
- [ ] `shuffled` returns a new deck; the receiver is unchanged.
- [ ] The same seed yields an identical ordering, every run and every JVM.
- [ ] Different seeds yield different orderings.
- [ ] Dealing all 52 cards yields each exactly once and leaves an empty deck.
- [ ] Dealing more cards than remain fails clearly rather than returning short.
- [ ] `kotlin.random.Random` appears nowhere in the module, asserted by a test that scans the
      compiled classes or the source tree.

## Tests

- `DeckTest` — completeness, immutability, exhaustion, seed reproducibility.
- Property: for any seed, a shuffled deck is a permutation of the full deck.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
