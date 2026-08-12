---
schema: 2
id: TASK-010604
title: Compute the uncalled part of a bet
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010521]
verify:
  - ./gradlew :poker-engine:test --tests '*UncalledBetTest'
  - ./gradlew :poker-engine:check
---

## Goal

A pure function names the seat that put in more than its opponent ever covered, and how much of
that is coming back.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Settlement.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/UncalledBetTest.kt` | create |

Read `Seat.kt`, `GameState.kt`, `DealerEvents.kt` (for `UncalledBetReturned`'s contract) and
`GameStates.kt` (the `handState()`/`seats()` fixtures). Modify none of them.

## Scope

- New file `Settlement.kt`, package `duels.poker.engine.game`, containing exactly two public
  declarations with KDoc:

  ```kotlin
  public data class UncalledPortion(val seat: Int, val amount: Int)

  public fun uncalledPortion(state: GameState): UncalledPortion?
  ```

- `UncalledPortion`'s `init` requires `seat in 0..1` and `amount > 0` — an uncalled portion of
  nothing is not a value, it is `null`.
- `uncalledPortion` reads **`Seat.committedThisHand`**, never `committedThisStreet`: the hand
  total is gross and survives the per-street sweep, which is the only reason the uncalled part
  of a bet is still recoverable once `BettingRoundEnded` has emptied the seats. Returns the seat
  with the strictly larger `committedThisHand` and the difference, or `null` when both match.
- KDoc must say *why* the seat that is owed can never be the seat that folded: folding is only
  legal facing a bet, so a folder's hand total is always strictly the smaller of the two.

## Out of scope

- Emitting `UncalledBetReturned` or moving any chips — `TASK-010605`.
- Deciding who wins the pot — `TASK-010610`.
- Touching `StreetProgression.kt`. Nothing calls this function yet, and that is deliberate: this
  ticket changes no engine output at all, so no existing test can be affected by it.

## Tests

`UncalledBetTest`, JUnit 5, package `duels.poker.engine.game`. Build states with `handState(...)`
and `seats(...)` from `GameStates.kt`, varying `committedThisHand` with `Seat(...)` directly.

| Test | Proves |
| --- | --- |
| `equalCommitmentsLeaveNothingUncalled` | both seats at `committedThisHand = 300` → `null` |
| `theOvercommittedSeatGetsTheDifference` | seat 1 at 500 against seat 0 at 300 → `UncalledPortion(1, 200)` |
| `eitherSeatCanBeTheOneOwed` | seat 0 at 900 against seat 1 at 100 → `UncalledPortion(0, 800)` |
| `itReadsTheHandTotalNotTheStreetTotal` | with both `committedThisStreet == 0` after a sweep and hand totals 300/500, the answer is still `UncalledPortion(1, 200)` |
| `anEmptyPortionIsNotAValue` | `UncalledPortion(0, 0)` throws `IllegalArgumentException` |
| `aPortionBelongsToARealSeat` | `UncalledPortion(2, 100)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `UncalledBetTest.equalCommitmentsLeaveNothingUncalled` passes
- [ ] `UncalledBetTest.theOvercommittedSeatGetsTheDifference` passes
- [ ] `UncalledBetTest.eitherSeatCanBeTheOneOwed` passes
- [ ] `UncalledBetTest.itReadsTheHandTotalNotTheStreetTotal` passes
- [ ] `UncalledBetTest.anEmptyPortionIsNotAValue` passes
- [ ] `UncalledBetTest.aPortionBelongsToARealSeat` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
