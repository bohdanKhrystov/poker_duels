---
schema: 2
id: TASK-020403
title: The `PlayerView` type
type: task
status: backlog
parent: STORY-0204
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, projection, security]
depends_on: [TASK-020401, TASK-020402]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerViewTest'
  - ./gradlew :poker-engine:check
---

## Goal

`PlayerView` exists as a type: exactly the fields one recipient is entitled to, and no deck, no
rng and no seed anywhere in it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerViewTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` (field names and its
`require` blocks — mirror them, do not import the type's contents),
`poker-engine/src/main/kotlin/duels/poker/engine/game/Board.kt`.

## Scope

- One public type with KDoc, package `duels.poker.engine.game`:

  ```kotlin
  @Serializable
  public data class PlayerView(
      val viewerSeat: Int,
      val handNumber: Int,
      val buttonSeat: Int,
      val street: Street,
      val board: Board,
      val pot: Int,
      val betToMatch: Int,
      val minRaiseTo: Int,
      val seatToAct: Int?,
      val smallBlind: Int,
      val bigBlind: Int,
      val seats: List<SeatView>,
  )
  ```

- Two derived, non-constructor conveniences, so they never appear in the wire form:

  ```kotlin
  public val viewer: SeatView get() = seats[viewerSeat]
  public val opponent: SeatView get() = seats[1 - viewerSeat]
  ```

- `require` blocks: `viewerSeat in 0..1`; `buttonSeat in 0..1`; `seats.size == 2` and
  `seats[i].index == i`; `handNumber >= 1`; `pot >= 0`, `betToMatch >= 0`, `minRaiseTo >= 0`;
  `seatToAct` is null or in `0..1`; `0 < smallBlind && smallBlind < bigBlind`.
- The KDoc states the two fields that are deliberately absent — `GameState.deck` and
  `GameState.rng` — and why: `ADR-0002` says neither, nor the seed, leaves the server while a
  hand is live. A field added here is a decision to publish it.

## Out of scope

- `PlayerView.of(...)`: `TASK-020404`. This ticket declares the shape only, so the redaction rule
  gets its own diff and its own review.
- `eventCount` or any sequence number for the transport to de-duplicate on — `STORY-0202` owns
  the envelope, and this view is its payload, not its header.
- A spectator view: none exists in v0.1, and `DEC-009` records the open question.

## Tests

`PlayerViewTest`, JUnit 5, package `duels.poker.engine.game`. Build views by hand with named
arguments; a private helper returning a valid view that each test `copy`s one field of keeps the
file short.

| Test | Proves |
| --- | --- |
| `carriesTheFieldsItWasBuiltWith` | every constructor field reads back unchanged, including a five-card `Board` |
| `viewerAndOpponentNameTheRightSeats` | with `viewerSeat = 1`, `viewer.index == 1` and `opponent.index == 0` |
| `rejectsAViewerSeatOutsideZeroOrOne` | `viewerSeat = 2` throws `IllegalArgumentException` |
| `rejectsOtherThanTwoSeats` | a one-entry `seats` list throws `IllegalArgumentException` |
| `rejectsSeatsOutOfIndexOrder` | `seats` holding index 1 then index 0 throws `IllegalArgumentException` |
| `rejectsANegativePot` | `pot = -1` throws `IllegalArgumentException` |
| `rejectsASeatToActOutsideZeroOrOne` | `seatToAct = 2` throws `IllegalArgumentException`, while `null` is accepted |
| `rejectsBlindsThatDoNotAscend` | `smallBlind = 100, bigBlind = 100` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `PlayerViewTest.carriesTheFieldsItWasBuiltWith` passes
- [ ] `PlayerViewTest.viewerAndOpponentNameTheRightSeats` passes
- [ ] `PlayerViewTest.rejectsAViewerSeatOutsideZeroOrOne` passes
- [ ] `PlayerViewTest.rejectsOtherThanTwoSeats` passes
- [ ] `PlayerViewTest.rejectsSeatsOutOfIndexOrder` passes
- [ ] `PlayerViewTest.rejectsANegativePot` passes
- [ ] `PlayerViewTest.rejectsASeatToActOutsideZeroOrOne` passes
- [ ] `PlayerViewTest.rejectsBlindsThatDoNotAscend` passes
- [ ] `PlayerView` declares no property of type `Deck`, `Rng` or a seed, and no property named
      `deck`, `rng` or `seed` — `TASK-020408` asserts this structurally
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
