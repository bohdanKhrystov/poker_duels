---
schema: 2
id: TASK-010409
title: GameState fields and construction invariants
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010408]
verify:
  - ./gradlew :poker-engine:test --tests '*GameStateTest'
  - ./gradlew :poker-engine:check
---

## Goal

A single immutable value that describes one hand of heads-up hold'em completely — copy it, store
it, hand it back to the engine, and the hand continues exactly where it was.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameStateTest.kt` | create |

Read `Seat.kt`, `Board.kt`, `Street.kt` from `duels/poker/engine/game/`, and
`duels/poker/engine/card/Deck.kt` and `duels/poker/engine/random/Rng.kt` for their APIs. Do not
modify them.

## Scope

- `GameState.kt`, package `duels.poker.engine.game`:

  ```kotlin
  public data class GameState(
      val handNumber: Int,
      val buttonSeat: Int,
      val street: Street,
      val seats: List<Seat>,
      val board: Board,
      val pot: Int,
      val betToMatch: Int,
      val minRaiseTo: Int,
      val seatToAct: Int?,
      val smallBlind: Int,
      val bigBlind: Int,
      val eventCount: Int,
      val deck: Deck,
      val rng: Rng,
  )
  ```

  `eventCount` is how many events this hand has already produced, so the next event's `sequence`
  is exactly `eventCount`. It lives in the state because `handle` is pure: an engine that kept a
  counter of its own would be hidden state, which ADR-0001 forecloses.

- `init` requires, each with a message naming the offending value:
  - `seats.size == 2` and `seats[0].index == 0 && seats[1].index == 1` — the list is ordered by
    seat index, so `seats[i]` is always seat `i`.
  - `buttonSeat in 0..1`; `seatToAct == null || seatToAct in 0..1`.
  - `handNumber >= 1`; `pot >= 0`; `betToMatch >= 0`; `minRaiseTo >= 0`; `eventCount >= 0`.
  - `0 < smallBlind && smallBlind < bigBlind`.
  - the board matches the street: `street == Street.COMPLETE || board.size == street.boardCards`.
    `COMPLETE` is exempt because a hand that ends on a fold stops dealing — a preflop fold leaves
    an empty board on a completed hand.
- KDoc on the class:
  - the state is the whole hand; nothing needed to resume it lives anywhere else,
  - `betToMatch` and `minRaiseTo` are **street totals**, not increments (see `TASK-010412`),
  - `pot` holds only chips already swept from finished betting rounds; chips still in front of a
    seat are that seat's `committedThisStreet` until the round ends,
  - `deck` and `rng` are carried in the state because the engine may never reach for an ambient
    random source, and no `GameEvent` ever reveals either of them.
- Blind *levels* are a match concern (STORY-0107); this state simply carries the two amounts in
  force for this hand.

## Out of scope

- Derived properties and `withSeat` — `TASK-010410`.
- A test fixture for building states — `TASK-010411`.
- Starting a hand, dealing, or any rule — STORY-0105.
- A `Pot` type: two players cannot make a side pot (see `TASK-010601`), so the pot is an `Int`.

## Tests

`GameStateTest`, JUnit 5. Build a state inline in each test — the shared fixture arrives in
`TASK-010411`. Use `Deck.full()` and `SplitMix64Rng(1L)`.

| Test | Proves |
| --- | --- |
| `buildsAPreflopState` | a preflop state with two seats, `Board.EMPTY` and `seatToAct = 0` constructs and reads back its fields |
| `copyProducesAnEqualIndependentValue` | `state.copy() == state`, and `state.copy(pot = 500) != state` while `state.pot` is unchanged |
| `equalityReachesIntoSeatsAndBoard` | two states differing only in `seats[1].stack` are not equal; two built from identical values are |
| `rejectsAnythingOtherThanTwoSeats` | one seat and three seats each throw `IllegalArgumentException` |
| `rejectsSeatsOutOfIndexOrder` | `listOf(seat1, seat0)` throws |
| `rejectsAnOutOfRangeButtonOrActor` | `buttonSeat = 2` throws, `seatToAct = 2` throws |
| `rejectsBlindsThatAreNotAscending` | `smallBlind = 100, bigBlind = 100` throws, `smallBlind = 0` throws |
| `rejectsANegativeEventCount` | `eventCount = -1` throws |
| `rejectsABoardThatContradictsTheStreet` | `street = FLOP` with `Board.EMPTY` throws; `street = PREFLOP` with a three-card board throws |
| `allowsAnyBoardOnACompletedHand` | `street = COMPLETE` with `Board.EMPTY` constructs |

## Acceptance criteria

- [ ] `GameStateTest.buildsAPreflopState` passes
- [ ] `GameStateTest.copyProducesAnEqualIndependentValue` passes
- [ ] `GameStateTest.equalityReachesIntoSeatsAndBoard` passes
- [ ] `GameStateTest.rejectsAnythingOtherThanTwoSeats` passes
- [ ] `GameStateTest.rejectsSeatsOutOfIndexOrder` passes
- [ ] `GameStateTest.rejectsAnOutOfRangeButtonOrActor` passes
- [ ] `GameStateTest.rejectsBlindsThatAreNotAscending` passes
- [ ] `GameStateTest.rejectsANegativeEventCount` passes
- [ ] `GameStateTest.rejectsABoardThatContradictsTheStreet` passes
- [ ] `GameStateTest.allowsAnyBoardOnACompletedHand` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
