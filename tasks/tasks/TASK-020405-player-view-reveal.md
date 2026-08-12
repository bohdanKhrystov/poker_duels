---
schema: 2
id: TASK-020405
title: Show a hand the engine has already revealed
type: task
status: done
parent: STORY-0204
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, projection, security, showdown]
depends_on: [TASK-020404]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerViewRevealTest'
  - ./gradlew :poker-engine:test --tests '*PlayerViewOfTest'
  - ./gradlew :poker-engine:check
---

## Goal

`PlayerView.of` takes the seats whose hands the engine has already revealed, and shows those
hands to both players — a showdown a player cannot see is a different bug.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerViewRevealTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt`
(`HandRevealed`'s KDoc: it is emitted only for a hand actually shown),
`poker-engine/src/test/kotlin/duels/poker/engine/game/GameStates.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt`.

## Scope

- Widen the existing factory to:

  ```kotlin
  public fun of(state: GameState, seat: Int, revealed: Set<Int> = emptySet()): PlayerView
  ```

  The default is `emptySet()` — nothing revealed — so the safe behaviour is what a caller gets by
  forgetting the argument. Under-revealing is a visible bug; over-revealing would be a silent
  leak, and only an explicit argument can cause it.
- `require(revealed.all { it in 0..1 })`, message naming the offending set.
- The entitlement rule becomes exactly one expression, in the existing `map`:
  `showCards = it.index == seat || it.index in revealed`. Nothing else about the projection
  changes.
- KDoc on `revealed`: the seats a `HandRevealed` event has already been emitted for **in this
  hand**, as `revealedSeats(events)` (`TASK-020407`) computes it. `GameState` keeps both seats'
  hole cards from the deal to the last event, so the state alone cannot say what has been shown —
  the event log is the only record of that, which is why this is a parameter.

## Out of scope

- Computing the set from a hand's events: `TASK-020407` — this ticket only consumes it.
- Any inference of "revealed" from `street == SHOWDOWN`, from `showdownWinners`, or from a seat
  not having folded. The loser mucks (`ADR-0008`), so a rule derived from the state would show a
  mucked hand — the exact leak `CardSecrecyTest` exists to prevent.
- Changing `PlayerViewOfTest`: every call in it is `of(state, seat)`, whose behaviour is
  unchanged by a parameter that defaults to `emptySet()`. It is in `verify:` to prove that.

## Tests

`PlayerViewRevealTest`, JUnit 5, package `duels.poker.engine.game`. Start from `handState()`,
deal `cards("As Kh")` to seat 0 and `cards("Qd Jc")` to seat 1 with `withSeat`.

| Test | Proves |
| --- | --- |
| `revealsNothingByDefault` | `of(state, 0).opponent.holeCards` is empty when `revealed` is omitted |
| `aRevealedOpponentHandAppearsInTheView` | `of(state, 0, setOf(1)).opponent.holeCards == cards("Qd Jc")` |
| `aRevealedHandAppearsInBothSeatsViews` | with `revealed = setOf(1)`, seat 0's `opponent` and seat 1's `viewer` both hold `cards("Qd Jc")` |
| `revealingOnlyTheViewerLeavesTheOpponentHidden` | `of(state, 0, setOf(0)).opponent.holeCards` is empty |
| `revealingBothShowsBothToEitherSeat` | `of(state, 0, setOf(0, 1))` and `of(state, 1, setOf(0, 1))` each carry both hands |
| `theViewerStillSeesItsOwnHandWhenNothingIsRevealed` | `of(state, 1).viewer.holeCards == cards("Qd Jc")` |
| `rejectsARevealedSeatOutsideZeroOrOne` | `of(state, 0, setOf(2))` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `PlayerViewRevealTest.revealsNothingByDefault` passes
- [ ] `PlayerViewRevealTest.aRevealedOpponentHandAppearsInTheView` passes
- [ ] `PlayerViewRevealTest.aRevealedHandAppearsInBothSeatsViews` passes
- [ ] `PlayerViewRevealTest.revealingOnlyTheViewerLeavesTheOpponentHidden` passes
- [ ] `PlayerViewRevealTest.revealingBothShowsBothToEitherSeat` passes
- [ ] `PlayerViewRevealTest.theViewerStillSeesItsOwnHandWhenNothingIsRevealed` passes
- [ ] `PlayerViewRevealTest.rejectsARevealedSeatOutsideZeroOrOne` passes
- [ ] `PlayerViewOfTest` passes with no edit to that file
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
