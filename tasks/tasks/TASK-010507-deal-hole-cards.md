---
schema: 2
id: TASK-010507
title: Deal the hole cards and put the action on the button
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, rules, cards]
depends_on: [TASK-010506]
verify:
  - ./gradlew :poker-engine:test --tests '*HandDealTest'
  - ./gradlew :poker-engine:check
---

## Goal

`startHand` finishes the deal: both seats hold two cards drawn from the shuffled deck, and the
button — the small blind — is on turn.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/HandSetup.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/HandDealTest.kt` | create |

Read `HeadsUpOrder.kt`, `Deck.kt`, `GameEvent.kt`. Modify none of them.

## Scope

- `startHand` keeps its signature and appends three events after the two `BlindPosted`:

  | # | Event |
  | --- | --- |
  | 3 | `HoleCardsDealt(seat = bigBlindSeat(buttonSeat), cards = [c0, c2])` |
  | 4 | `HoleCardsDealt(seat = smallBlindSeat(buttonSeat), cards = [c1, c3])` |
  | 5 | `ActionOn(seat = firstToActOn(Street.PREFLOP, buttonSeat))` |

- `c0..c3` are `deck.deal(4).cards` in order: the cards go round the table one at a time
  starting with the seat left of the button, which heads-up is the big blind. Keeping the
  physical deal order is what makes a recorded deck ordering meaningful.
- The returned state carries `deck.deal(4).deck` — 48 cards. `StateProjection` does not advance
  the deck, so `startHand` must set it.
- The `ActionOn` seat is the button: heads-up, it acts first preflop.
- No other behaviour changes: the three events from `TASK-010506` keep sequences 0, 1 and 2.

## Out of scope

- Anything a player then does — `TASK-010508` onwards.
- Hiding a seat's cards from the other player: that is the projection layer, STORY-0109.

## Tests

`HandDealTest`, JUnit 5, calling `startHand(1, 0, listOf(10_000, 10_000), 50, 100,
SplitMix64Rng(1L))` unless the test says otherwise.

| Test | Proves |
| --- | --- |
| `eachSeatHoldsTwoCards` | `seat(0).holeCards.size == 2` and `seat(1).holeCards.size == 2` |
| `noCardIsDealtTwice` | the four hole cards are four distinct cards |
| `theDeckHasFortyEightLeft` | `newState.deck.remaining == 48` |
| `theBigBlindIsDealtFirstAndEveryOtherCard` | against `Deck.full().shuffled(SplitMix64Rng(1L)).deck.deal(4).cards`, seat 1 holds cards 0 and 2 and seat 0 holds cards 1 and 3 |
| `theButtonIsOnTurnPreflop` | `newState.seatToAct == 0`, and with `buttonSeat = 1` it is 1 |
| `theOpeningSequenceRunsToFive` | six events, `sequence` 0..5, ending `HoleCardsDealt`, `HoleCardsDealt`, `ActionOn` |
| `theSameSeedDealsTheSameCards` | two calls with `SplitMix64Rng(7L)` give identical hole cards; a different seed gives different ones |
| `theEventsDescribeTheState` | `StateProjection.fold` over the six events reproduces `newState` except `deck` and `rng` |

## Acceptance criteria

- [ ] `HandDealTest.eachSeatHoldsTwoCards` passes
- [ ] `HandDealTest.noCardIsDealtTwice` passes
- [ ] `HandDealTest.theDeckHasFortyEightLeft` passes
- [ ] `HandDealTest.theBigBlindIsDealtFirstAndEveryOtherCard` passes
- [ ] `HandDealTest.theButtonIsOnTurnPreflop` passes
- [ ] `HandDealTest.theOpeningSequenceRunsToFive` passes
- [ ] `HandDealTest.theSameSeedDealsTheSameCards` passes
- [ ] `HandDealTest.theEventsDescribeTheState` passes
- [ ] `HandSetupTest` still passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
