---
schema: 2
id: TASK-010610
title: Decide who wins a showdown
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules]
depends_on: [TASK-010609]
verify:
  - ./gradlew :poker-engine:test --tests '*ShowdownWinnersTest'
  - ./gradlew :poker-engine:check
---

## Goal

Given a hand that reached showdown, one pure function names the seat with the better five cards,
or both seats when the two hands are equal.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Showdown.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ShowdownWinnersTest.kt` | create |

Read `hand/HandEvaluator.kt` (for `bestOfSeven`), `hand/FastHandEvaluator.kt`, `hand/HandRank.kt`
and `GameStates.kt`. Modify none of them.

## Scope

- New file `Showdown.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public fun showdownWinners(state: GameState): List<Int>
  ```

- Requires a real showdown: `require(state.board.size == 5)`, both seats holding exactly two hole
  cards, and neither seat folded — a folded hand never reaches here, because a fold ends the hand
  where it stands. Each `require` names what was wrong.
- Ranks each seat with `FastHandEvaluator.bestOfSeven(seat.holeCards + state.board.cards).rank`
  and compares with `HandRank`'s own ordering. Suits never break a tie, so equal ranks mean both
  seats: return `listOf(0)`, `listOf(1)` or `listOf(0, 1)`, always in ascending seat order, ready
  for `settleHand`.
- KDoc says why this returns seats rather than paying anything: awarding is `Settlement.kt`'s
  job, and keeping the decision separate from the chips is what lets both be tested alone.

## Out of scope

- Re-testing hand evaluation. `bestOfSeven` and `HandRank` are pinned by `STORY-0103`; this
  ticket tests only the seat-picking on top of them.
- Emitting any event, revealing any card, or touching `StreetProgression.kt` — `TASK-010611`
  wires this in, and reveals wait on `DEC-004`.

## Tests

`ShowdownWinnersTest`, JUnit 5. Build positions as
`handState(seats).copy(street = Street.SHOWDOWN, board = Board(cards("...")), seatToAct = null)`
with hole cards on the seats.

| Test | Proves |
| --- | --- |
| `theBetterHandTakesIt` | board `As Kd 7c 2h 9s`, seat 0 `Qh Jc`, seat 1 `Td 8c` → `listOf(0)` |
| `eitherSeatCanWin` | the same board with the hole cards swapped → `listOf(1)` |
| `aKickerDecidesIt` | board `Ah Kc 7d 4s 2c`, seat 0 `Ad Qs`, seat 1 `Ac Js` → `listOf(0)` |
| `theWheelBeatsAPair` | board `2c 3d 4h 5s Kc`, seat 0 `Ad 9h`, seat 1 `Kd Qs` → `listOf(0)` |
| `aBoardThatPlaysSplits` | board `As Kd Qc Jh Ts`, seat 0 `2c 3d`, seat 1 `4h 5s` → `listOf(0, 1)` |
| `anIncompleteBoardIsRefused` | a `Street.FLOP` state with three board cards throws `IllegalArgumentException` |
| `aSeatWithoutCardsIsRefused` | a five-card board with one seat holding no hole cards throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `ShowdownWinnersTest.theBetterHandTakesIt` passes
- [ ] `ShowdownWinnersTest.eitherSeatCanWin` passes
- [ ] `ShowdownWinnersTest.aKickerDecidesIt` passes
- [ ] `ShowdownWinnersTest.theWheelBeatsAPair` passes
- [ ] `ShowdownWinnersTest.aBoardThatPlaysSplits` passes
- [ ] `ShowdownWinnersTest.anIncompleteBoardIsRefused` passes
- [ ] `ShowdownWinnersTest.aSeatWithoutCardsIsRefused` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
