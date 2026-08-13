---
schema: 2
id: TASK-021001
title: Map a DuelOutcome to two signed coin deltas, in one function
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, coins, duel]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*CoinAwardTest'
  - ./gradlew :poker-server:check
---

## Goal

`coinDeltas(outcome)` turns a finished duel's `DuelOutcome` into the two signed coin deltas
`ADR-0014` prescribes — winner `+1`, loser `−1`, draw `0` to both — and it is the only place in the
server that decides what a duel is worth.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/CoinDeltas.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/CoinAwardTest.kt` | create |

Read, do not modify:
`docs/adr/ADR-0014-duel-coin-economy.md`,
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt`.

## Scope

- New package `duels.poker.server.duel`. One file, two public declarations, both with KDoc:

  ```kotlin
  public data class CoinDeltas(val seat0: Int, val seat1: Int) {
      public fun forSeat(seat: Int): Int =
          when (seat) {
              0 -> seat0
              1 -> seat1
              else -> throw IllegalArgumentException("seat must be 0 or 1, got $seat")
          }
  }

  public fun coinDeltas(outcome: DuelOutcome): CoinDeltas =
      when {
          outcome.isDraw -> CoinDeltas(seat0 = 0, seat1 = 0)
          outcome.winner == 0 -> CoinDeltas(seat0 = 1, seat1 = -1)
          else -> CoinDeltas(seat0 = -1, seat1 = 1)
      }
  ```

- The draw case reads `DuelOutcome.isDraw`, which the engine already computes — this function
  re-derives nothing about who won.
- **Signed, never floored.** No `coerceAtLeast`, no `maxOf(0, …)`, no `UInt`, no absolute value
  anywhere in the file. `ADR-0014` says why: a balance is `wins − losses` and a new profile's first
  loss puts it at `−1`, which is the intended day-one case, not an error state.
- KDoc on both declarations citing `ADR-0014`, and saying that this is the single place the award
  is decided — the ADR anticipates being superseded by a floating or opponent-weighted award, and a
  rule expressed once is a rule that can be replaced once.
- No database, no `DataSource`, no SQL, no coroutines in this file. It is a pure function over an
  engine type.

## Out of scope

- Writing anything to PostgreSQL — `TASK-021005` and `TASK-021006`.
- The type describing a finished duel (`FinishedDuel`) — `TASK-021002`.
- Any change to `poker-engine`. `DuelOutcome` and `isDraw` already exist and are used as they are.

## Tests

`CoinAwardTest`, JUnit 5, package `duels.poker.server.duel`. Plain tests, no coroutines and no
database. Build outcomes directly, for example
`DuelOutcome(winner = 0, handsPlayed = 12, finalStacks = listOf(20_000, 0))` and a draw as
`DuelOutcome(winner = null, handsPlayed = 20, finalStacks = listOf(10_000, 10_000))`.

| Test | Proves |
| --- | --- |
| `theWinnerAtSeatZeroGainsOneAndSeatOneLosesOne` | `coinDeltas` of an outcome with `winner = 0` is `CoinDeltas(1, -1)` |
| `theWinnerAtSeatOneGainsOneAndSeatZeroLosesOne` | `coinDeltas` of an outcome with `winner = 1` is `CoinDeltas(-1, 1)` |
| `aDrawPaysNothingToEitherSeat` | `coinDeltas` of an outcome with `winner = null` is `CoinDeltas(0, 0)` |
| `theTwoDeltasAlwaysSumToZero` | for the win, loss and draw outcomes above, `seat0 + seat1 == 0` — coins are conserved, never minted |
| `forSeatReturnsTheDeltaOfThatSeat` | `CoinDeltas(1, -1).forSeat(0) == 1` and `.forSeat(1) == -1` |
| `forSeatRejectsASeatThatIsNotZeroOrOne` | `forSeat(2)` and `forSeat(-1)` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `CoinAwardTest.theWinnerAtSeatZeroGainsOneAndSeatOneLosesOne` passes
- [ ] `CoinAwardTest.theWinnerAtSeatOneGainsOneAndSeatZeroLosesOne` passes
- [ ] `CoinAwardTest.aDrawPaysNothingToEitherSeat` passes
- [ ] `CoinAwardTest.theTwoDeltasAlwaysSumToZero` passes
- [ ] `CoinAwardTest.forSeatReturnsTheDeltaOfThatSeat` passes
- [ ] `CoinAwardTest.forSeatRejectsASeatThatIsNotZeroOrOne` passes
- [ ] `CoinDeltas.kt` contains no `coerceAtLeast`, `coerceIn`, `maxOf`, `abs`, `absoluteValue` or
      `UInt`
- [ ] `CoinDeltas.kt` names no SQL, `DataSource`, `Connection` or coroutine type
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
