---
schema: 2
id: TASK-020404
title: Project a state into one seat's view
type: task
status: backlog
parent: STORY-0204
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, projection, security]
depends_on: [TASK-020403]
verify:
  - ./gradlew :poker-engine:test --tests '*PlayerViewOfTest'
  - ./gradlew :poker-engine:check
---

## Goal

`PlayerView.of(state, seat)` answers *what may seat N see of this state?* — the viewer's own hole
cards and nothing of the other seat's.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/PlayerViewOfTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/Seat.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/GameStates.kt` (`handState()`, `seats()`,
`SMALL_BLIND`, `BIG_BLIND`, `START_STACK`),
`poker-engine/src/test/kotlin/duels/poker/engine/card/Cards.kt` (`cards("As Kh")`).

## Scope

- Add to `PlayerView` a companion object holding one public function with KDoc:

  ```kotlin
  public companion object {
      public fun of(state: GameState, seat: Int): PlayerView
  }
  ```

- `require(seat in 0..1)` first, with a message naming the value.
- Copy the scalar fields across one by one — `handNumber`, `buttonSeat`, `street`, `board`,
  `pot`, `betToMatch`, `minRaiseTo`, `seatToAct`, `smallBlind`, `bigBlind` — and set
  `viewerSeat = seat`.
- Build the two `SeatView`s through **one private helper that takes the cards explicitly**:

  ```kotlin
  private fun seatView(seat: Seat, showCards: Boolean): SeatView
  ```

  with `holeCards = if (showCards) seat.holeCards else emptyList()`, called as
  `state.seats.map { seatView(it, showCards = it.index == seat) }`. No generic field copy, no
  reflection, no `Seat` leaking into the view: a field added to `Seat` must be added here by hand
  before it can reach a client, and that is the property under review.
- `state.deck` and `state.rng` are never read.

## Out of scope

- Revealing an opponent's hand after a showdown — `TASK-020405` adds the `revealed` parameter.
  Until it merges, `of` hides the other seat's cards unconditionally, which is the safe default.
- Filtering events: `TASK-020406`.
- Serializing a view or asserting its structure: `TASK-020408`.

## Tests

`PlayerViewOfTest`, JUnit 5, package `duels.poker.engine.game`. Start from `handState()` and
`copy` in what each test needs; deal hole cards with
`state.withSeat(0) { it.copy(holeCards = cards("As Kh")) }`.

| Test | Proves |
| --- | --- |
| `theViewerSeesItsOwnHoleCards` | `of(state, 0).viewer.holeCards == cards("As Kh")` |
| `theOpponentsHoleCardsAreAbsent` | `of(state, 0).opponent.holeCards` is empty though seat 1 holds `cards("Qd Jc")` |
| `eachSeatSeesOnlyItsOwn` | `of(state, 1).viewer.holeCards == cards("Qd Jc")` and `of(state, 1).opponent.holeCards` is empty |
| `carriesTheBoardPotAndBlinds` | a state on the flop with a pot projects `board`, `pot`, `smallBlind` and `bigBlind` unchanged |
| `carriesStacksCommitmentsAndStatus` | both `SeatView`s carry the seats' `stack`, `committedThisStreet`, `committedThisHand`, `hasFolded` and `isAllIn` |
| `carriesTheButtonStreetHandNumberAndSeatToAct` | those four fields match the state, `viewerSeat` is the argument |
| `showsNoHoleCardsBeforeTheDeal` | on a state where neither seat has cards, both `SeatView`s have empty `holeCards` |
| `rejectsASeatOutsideZeroOrOne` | `of(state, 2)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `PlayerViewOfTest.theViewerSeesItsOwnHoleCards` passes
- [ ] `PlayerViewOfTest.theOpponentsHoleCardsAreAbsent` passes
- [ ] `PlayerViewOfTest.eachSeatSeesOnlyItsOwn` passes
- [ ] `PlayerViewOfTest.carriesTheBoardPotAndBlinds` passes
- [ ] `PlayerViewOfTest.carriesStacksCommitmentsAndStatus` passes
- [ ] `PlayerViewOfTest.carriesTheButtonStreetHandNumberAndSeatToAct` passes
- [ ] `PlayerViewOfTest.showsNoHoleCardsBeforeTheDeal` passes
- [ ] `PlayerViewOfTest.rejectsASeatOutsideZeroOrOne` passes
- [ ] `of` reads neither `state.deck` nor `state.rng`, and `PlayerViewTest` is not modified
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
