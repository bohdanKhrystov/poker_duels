---
schema: 2
id: TASK-020702
title: Every outbound frame is addressed to one seat and built by the engine's projection layer
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [server, duel, projection, secrecy]
depends_on: [TASK-020701]
verify:
  - ./gradlew :poker-server:test --tests '*DuelBroadcastTest'
  - ./gradlew :poker-server:check
---

## Goal

`broadcast(state, newEvents, handEvents)` turns one engine transition into the `Snapshot` and
`Events` frames each seat is entitled to — every card decision made by `PlayerView.of`,
`visibleTo` and `revealedSeats`, and none by this file.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/Addressed.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelBroadcastTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` (`PlayerView.of(state, seat,
revealed)`), `poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt`
(`visibleTo(events, seat)`, `revealedSeats(events)`),
`poker-server/src/main/kotlin/duels/poker/server/protocol/ServerMessage.kt`.

## Scope

- Package `duels.poker.server.duel`. Two declarations, KDoc included:

  ```kotlin
  public data class Addressed(val seat: Int, val message: ServerMessage)

  public fun broadcast(
      state: GameState,
      newEvents: List<GameEvent>,
      handEvents: List<GameEvent>,
  ): List<Addressed>
  ```

- `newEvents` is what the transition just produced; `handEvents` is every event of the live hand so
  far, `newEvents` included. Both are needed: the `Events` frame carries only what is new, while
  `revealed` must be computed over the whole hand — `revealedSeats` is documented as taking **one
  hand's** events.
- The body is exactly this shape, in this order, for `seat` in `0..1`:
  1. `visibleTo(newEvents, seat)`; if it is non-empty, emit `Addressed(seat,
     ServerMessage.Events(it))`. An empty list emits **no** frame — a seat is never sent an empty
     `Events`.
  2. emit `Addressed(seat, ServerMessage.Snapshot(PlayerView.of(state, seat,
     revealedSeats(handEvents))))`.
  So the result is seat 0's frames then seat 1's, and within a seat, events before the snapshot:
  the snapshot is the authoritative last word on state.
- `Addressed.init` requires `seat in 0..1`.
- **This file contains no card logic at all.** No reference to `holeCards`, no `filter` over an
  event's cards, no `copy` of an event. Every card decision is a call into the three engine
  functions named above. A reviewer finding any other card handling here should reject the PR.

## Out of scope

- `YourTurn` and legal actions — `TASK-020703`.
- Anything holding a `GameState` between calls: this is a pure function, and the state it is given
  comes from `TASK-020705` onwards.
- Writing a frame to a socket. `Addressed` names a seat, not a connection; mapping a seat to a
  `ConnectionWriter` is `TASK-020714`.

## Tests

`DuelBroadcastTest`, JUnit 5, package `duels.poker.server.duel`. Build the fixture with
`startHand(handNumber = 1, buttonSeat = 0, stacks = listOf(1_000, 1_000), smallBlind = 50,
bigBlind = 100, rng = SplitMix64Rng(7))`; its `newState` and `events` are the state and the hand's
events. For the reveal test, append a hand-made
`HandRevealed(sequence = events.size, seat = 1, cards = state.seat(1).holeCards)` to `handEvents`.

| Test | Proves |
| --- | --- |
| `eachSeatGetsASnapshotOfItsOwnView` | exactly two `Snapshot` frames, and each one's `view.viewerSeat` equals the seat it is addressed to |
| `aSnapshotShowsOnlyTheRecipientsHoleCards` | in seat 0's snapshot `view.seats[0].holeCards.size == 2` and `view.seats[1].holeCards` is empty; the mirror holds for seat 1 |
| `holeCardsDealtReachesOnlyTheSeatItNames` | seat 0's `Events` frame contains the `HoleCardsDealt` naming seat 0 and no `HoleCardsDealt` naming seat 1 |
| `aSeatWithNothingVisibleGetsNoEventsFrame` | with `newEvents` = only the `HoleCardsDealt` naming seat 1, seat 0's frames are a `Snapshot` and nothing else |
| `aRevealedSeatIsShownToBothSeats` | with the `HandRevealed` in `handEvents`, seat 0's snapshot shows seat 1's two hole cards |
| `framesAreOrderedEventsThenSnapshotWithinASeat` | the frame list is `[Events(0), Snapshot(0), Events(1), Snapshot(1)]` by type and addressee |

## Acceptance criteria

- [ ] `DuelBroadcastTest.eachSeatGetsASnapshotOfItsOwnView` passes
- [ ] `DuelBroadcastTest.aSnapshotShowsOnlyTheRecipientsHoleCards` passes
- [ ] `DuelBroadcastTest.holeCardsDealtReachesOnlyTheSeatItNames` passes
- [ ] `DuelBroadcastTest.aSeatWithNothingVisibleGetsNoEventsFrame` passes
- [ ] `DuelBroadcastTest.aRevealedSeatIsShownToBothSeats` passes
- [ ] `DuelBroadcastTest.framesAreOrderedEventsThenSnapshotWithinASeat` passes
- [ ] `Addressed.kt` does not contain the string `holeCards`
- [ ] `Addressed.kt` builds every payload by calling `PlayerView.of`, `visibleTo` or
      `revealedSeats`, and constructs no `PlayerView`, `SeatView` or `GameEvent` of its own
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
