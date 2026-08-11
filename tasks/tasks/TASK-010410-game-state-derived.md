---
schema: 2
id: TASK-010410
title: GameState derived properties and seat update
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, domain, chips]
depends_on: [TASK-010409]
verify:
  - ./gradlew :poker-engine:test --tests '*GameStateDerivedTest'
  - ./gradlew :poker-engine:check
---

## Goal

Everything computable about a state is computed from it, so no caller can hold a stale total, and
changing one seat is one call rather than a list rebuild.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameStateDerivedTest.kt` | create |

Read `Seat.kt` for `commit`/`award`. Do not change the constructor or the `require` blocks from
`TASK-010409`.

## Scope

Members added to `GameState`:

```kotlin
/** The pot plus everything still in front of the seats on this street. */
public val potTotal: Int get() = pot + seats.sumOf { it.committedThisStreet }

/** Every chip in the hand. Constant from the first blind to the last award. */
public val chipsInPlay: Int get() = potTotal + seats.sumOf { it.stack }

/** True once the hand can accept no further action. */
public val isHandOver: Boolean get() = street == Street.COMPLETE

public fun seat(index: Int): Seat

/**
 * Chips [index] must put in to match [betToMatch], capped at its stack: a seat can always call
 * all-in for less than the full amount.
 */
public fun toCall(index: Int): Int

/** This state with [index] replaced by `transform(seat(index))`. */
public fun withSeat(index: Int, transform: (Seat) -> Seat): GameState
```

- `seat(index)` requires `index in 0..1`; since `seats` is ordered by index it is `seats[index]`.
- `toCall(index)` is `(betToMatch - seat.committedThisStreet).coerceIn(0, seat.stack)` — never
  negative, never more than the seat has.
- `withSeat` rebuilds the list preserving order, so the constructor's index invariant still holds.
- KDoc on `chipsInPlay` naming it the conservation invariant the rules document requires.

## Out of scope

- Whose turn it is next, whether the round is over, what is legal — STORY-0105.
- Awarding the pot — STORY-0106.

## Tests

`GameStateDerivedTest`, JUnit 5. Build states inline; the shared fixture arrives in
`TASK-010411`.

| Test | Proves |
| --- | --- |
| `potTotalAddsStreetCommitmentsToThePot` | pot 400 with commitments 100 and 300 → 800 |
| `chipsInPlayCountsStacksAndPot` | two 1 000 stacks that have each committed 100, pot 200 → 2 400 |
| `chipsInPlayIsUnchangedByACommit` | `state.withSeat(0) { it.commit(250) }.chipsInPlay == state.chipsInPlay` |
| `isHandOverOnlyOnComplete` | true for `COMPLETE`, false for `PREFLOP` and `SHOWDOWN` |
| `seatReturnsTheSeatWithThatIndex` | `state.seat(1).index == 1` |
| `seatRejectsAnOutOfRangeIndex` | `state.seat(2)` throws `IllegalArgumentException` |
| `toCallIsTheDifferenceToTheCurrentBet` | `betToMatch = 300`, seat committed 100 → 200 |
| `toCallIsZeroWhenAlreadyMatched` | seat committed 300 against `betToMatch = 300` → 0 |
| `toCallIsCappedAtTheStack` | `betToMatch = 900`, seat committed 0 with a stack of 400 → 400 |
| `withSeatReplacesOnlyThatSeat` | after `withSeat(1) { it.award(500) }`, seat 1's stack grew and seat 0 is untouched, and `seats[0].index == 0` |

## Acceptance criteria

- [ ] `GameStateDerivedTest.potTotalAddsStreetCommitmentsToThePot` passes
- [ ] `GameStateDerivedTest.chipsInPlayCountsStacksAndPot` passes
- [ ] `GameStateDerivedTest.chipsInPlayIsUnchangedByACommit` passes
- [ ] `GameStateDerivedTest.isHandOverOnlyOnComplete` passes
- [ ] `GameStateDerivedTest.seatReturnsTheSeatWithThatIndex` passes
- [ ] `GameStateDerivedTest.seatRejectsAnOutOfRangeIndex` passes
- [ ] `GameStateDerivedTest.toCallIsTheDifferenceToTheCurrentBet` passes
- [ ] `GameStateDerivedTest.toCallIsZeroWhenAlreadyMatched` passes
- [ ] `GameStateDerivedTest.toCallIsCappedAtTheStack` passes
- [ ] `GameStateDerivedTest.withSeatReplacesOnlyThatSeat` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
