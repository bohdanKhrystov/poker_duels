---
schema: 2
id: TASK-010506
title: Open a hand by posting both blinds
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010505]
verify:
  - ./gradlew :poker-engine:test --tests '*HandSetupTest'
  - ./gradlew :poker-engine:check
---

## Goal

One call turns two stacks and a seed into a hand that has begun: the button's small blind and the
other seat's big blind are posted, as events and as state.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/HandSetupTest.kt` | create |

Read `HeadsUpOrder.kt`, `GameEvent.kt`, `StateProjection.kt`, `Deck.kt`. Modify none of them.

## Scope

- `HandSetup.kt`, package `duels.poker.engine.game`, one public function:

  ```kotlin
  public fun startHand(
      handNumber: Int,
      buttonSeat: Int,
      stacks: List<Int>,
      smallBlind: Int,
      bigBlind: Int,
      rng: Rng,
  ): EngineResult
  ```

- It builds the opening `GameState` itself: `street = PREFLOP`, `seats` from `stacks`,
  `board = Board.EMPTY`, `pot = 0`, `betToMatch = 0`, `minRaiseTo = bigBlind`,
  `seatToAct = null`, `eventCount = 0`, and `deck`/`rng` from `Deck.full().shuffled(rng)` —
  the deck is shuffled here and nowhere else.
- Events, in this order and with `sequence` 0, 1, 2:

  | # | Event |
  | --- | --- |
  | 0 | `HandStarted(handNumber, buttonSeat, smallBlind, bigBlind, stacks)` |
  | 1 | `BlindPosted(seat = smallBlindSeat(buttonSeat), to = min(smallBlind, stack), isBigBlind = false)` |
  | 2 | `BlindPosted(seat = bigBlindSeat(buttonSeat), to = min(bigBlind, stack), isBigBlind = true)` |

- A seat too short for its blind posts all-in for its whole stack — that is what the `min` is
  for; `Seat.commit` marks it `isAllIn`.
- Fold the events with `StateProjection.fold` to get the state, then `copy` the shuffled deck and
  the advanced `Rng` back onto it: the projection never touches those two fields, by design.
  Return `EngineResult.accepted(state, events)`.
- `require(stacks.size == 2)` and `require(stacks.all { it >= 1 })` — a seat with no chips does
  not start a hand.

## Out of scope

- Hole cards and the first `ActionOn` — `TASK-010507`, which appends events 3, 4 and 5.
- Blind *levels* and who calls `startHand` — STORY-0107. This function takes the amounts.

## Tests

`HandSetupTest`, JUnit 5, calling `startHand(1, buttonSeat, listOf(10_000, 10_000), 50, 100,
SplitMix64Rng(1L))` unless the test says otherwise.

| Test | Proves |
| --- | --- |
| `theButtonPostsTheSmallBlind` | with `buttonSeat = 0`: seat 0 `committedThisStreet == 50`, stack 9 950 |
| `theNonButtonPostsTheBigBlind` | same hand: seat 1 `committedThisStreet == 100`, stack 9 900 |
| `theBlindsFollowTheButtonToTheOtherSeat` | with `buttonSeat = 1`: seat 1 posts 50, seat 0 posts 100 |
| `theOpeningEventsAreExact` | the three events, their types and `sequence` 0, 1, 2, in order |
| `theBigBlindIsTheBarAndSetsTheMinimumRaise` | `betToMatch == 100`, `minRaiseTo == 200` |
| `aShortStackPostsItsBlindAllIn` | stacks `listOf(10_000, 60)`, `buttonSeat = 0`: seat 1 commits 60, stack 0, `isAllIn`, `betToMatch == 100` |
| `chipsAreConserved` | `chipsInPlay == 20_000`, and `potTotal == 150` |
| `theEventsDescribeTheState` | `StateProjection.fold(opening, events)` equals `newState` except `deck` and `rng` |
| `theDeckIsShuffledAndFull` | `newState.deck.remaining == 52` and two different seeds give different first-dealt cards |

## Acceptance criteria

- [ ] `HandSetupTest.theButtonPostsTheSmallBlind` passes
- [ ] `HandSetupTest.theNonButtonPostsTheBigBlind` passes
- [ ] `HandSetupTest.theBlindsFollowTheButtonToTheOtherSeat` passes
- [ ] `HandSetupTest.theOpeningEventsAreExact` passes
- [ ] `HandSetupTest.theBigBlindIsTheBarAndSetsTheMinimumRaise` passes
- [ ] `HandSetupTest.aShortStackPostsItsBlindAllIn` passes
- [ ] `HandSetupTest.chipsAreConserved` passes
- [ ] `HandSetupTest.theEventsDescribeTheState` passes
- [ ] `HandSetupTest.theDeckIsShuffledAndFull` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
