---
schema: 2
id: TASK-010704
title: A blind level that carries a small and big blind and can double
type: task
status: done
parent: STORY-0107
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, duel]
depends_on: [TASK-010616]
verify:
  - ./gradlew :poker-engine:test --tests '*BlindLevelTest'
  - ./gradlew :poker-engine:check
---

## Goal

The engine has a validated small/big blind pair, so nothing downstream has to carry two loose
`Int`s and hope they stay in the right order.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindLevel.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/BlindLevelTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` for the blind invariant
this type must match (`0 < smallBlind < bigBlind`), and `docs/duel-rules.md` Part 2 for the
schedule this level is a rung of. Modify neither.

## Scope

- Create the package `duels.poker.engine.duel` — the duel/match layer named in
  `docs/architecture.md`. The dependency runs one way: `duel` may read `game`, and nothing in
  `game` ever imports `duel`. This ticket imports nothing from `game`.
- `public data class BlindLevel(val smallBlind: Int, val bigBlind: Int)`, with `init` requiring
  `smallBlind > 0` and `bigBlind > smallBlind`, each with a message naming the offending values.
  That is the same bar `GameState` sets, so any `BlindLevel` can open a hand.
- `public fun doubled(): BlindLevel` — both blinds multiplied by two. Doubling preserves the
  ordering, so the result always satisfies the same `init`.
- KDoc on the class and on `doubled`, per the engine's public-API rule.

## Out of scope

- Which level applies to which hand — `TASK-010705`.
- The 50/100 numbers themselves. They appear exactly once in the engine, in
  `DuelFormat.DEFAULT` — `TASK-010707`.
- Any overflow guard on repeated doubling: the schedule owns that, `TASK-010705`.

## Tests

`BlindLevelTest`

| Test | Proves |
| --- | --- |
| `holdsItsSmallAndBigBlind` | `BlindLevel(50, 100)` exposes `smallBlind == 50` and `bigBlind == 100` |
| `doubledDoublesBothBlinds` | `BlindLevel(200, 400).doubled() == BlindLevel(400, 800)` |
| `rejectsANonPositiveSmallBlind` | `BlindLevel(0, 100)` and `BlindLevel(-50, 100)` each throw `IllegalArgumentException` |
| `rejectsABigBlindThatIsNotLarger` | `BlindLevel(100, 100)` and `BlindLevel(100, 50)` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `BlindLevelTest.holdsItsSmallAndBigBlind` passes
- [ ] `BlindLevelTest.doubledDoublesBothBlinds` passes
- [ ] `BlindLevelTest.rejectsANonPositiveSmallBlind` passes
- [ ] `BlindLevelTest.rejectsABigBlindThatIsNotLarger` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
