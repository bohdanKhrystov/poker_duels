---
schema: 2
id: TASK-010517
title: Close the round and deal the next street
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, chips]
depends_on: [TASK-010516]
verify:
  - ./gradlew :poker-engine:test --tests '*StreetAdvanceTest'
  - ./gradlew :poker-engine:check
---

## Goal

A finished betting round sweeps the chips, deals the next street and puts the non-button on
turn — or, on the river, reaches the showdown.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StreetAdvanceTest.kt` | create |

Read `DealerEvents.kt`, `DealerProjection.kt`, `Deck.kt`, `HeadsUpOrder.kt`. Modify none of them.

## Scope

- `continueHand`'s last branch — the round is complete and nobody folded — emits, in order:

  1. `BettingRoundEnded(state.street)`,
  2. on `RIVER`: `ShowdownReached`, and nothing more;
  3. otherwise `StreetDealt(next, cards)` followed by
     `ActionOn(firstToActOn(next, state.buttonSeat))`.

- `next` is `state.street.next`; `cards` is `deck.deal(next.boardCards - state.board.size)` —
  three for the flop, one for the turn and the river. No burn card, deliberately
  (`docs/duel-rules.md`).
- `StateProjection` never advances the deck, so `continueHand` must `copy(deck = deal.deck)`
  onto the running state itself. Getting this wrong deals the same card twice.
- Every event is built from the running state's `eventCount` and applied before the next one is
  built, so sequences stay dense.
- The per-street reset is `DealerProjection`'s job, not this ticket's: `BettingRoundEnded`
  already sweeps `committedThisStreet` into `pot` and resets `betToMatch` and `minRaiseTo`.
  Do not repeat it here.

## Out of scope

- The case where fewer than two seats can still act — `TASK-010518` inserts the run-out between
  steps 1 and 3. Every test here has both seats holding chips.
- Anything after `ShowdownReached` — STORY-0106.

## Tests

`StreetAdvanceTest`, JUnit 5. Open with
`startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L))` and drive
`DefaultPokerEngine.handle`; build the river position with `handState().copy(...)` and
`cards("…")` from `card/Cards.kt`.

| Test | Proves |
| --- | --- |
| `aFinishedPreflopDealsThreeFlopCards` | after `Call(0)`, `Check(1)`: `board.size == 3`, `street == FLOP` |
| `theEventOrderIsEndedThenDealtThenActionOn` | those three dealer events, in that order, after the `PlayerChecked` |
| `theNonButtonActsFirstOnTheFlop` | `seatToAct == 1` with the button on seat 0 |
| `everyCommitmentIsSweptAndTheBarResets` | `pot == 200`, `betToMatch == 0`, `minRaiseTo == 100`, both `committedThisStreet == 0` |
| `committedThisHandSurvivesTheAdvance` | both seats still show `committedThisHand == 100` |
| `theDeckShrinksByExactlyTheCardsDealt` | `deck.remaining` goes 48 → 45 on the flop and 45 → 44 on the turn |
| `noCardAppearsTwice` | after playing to the turn, the four hole cards and four board cards are eight distinct cards |
| `theRiverEndsInShowdownReached` | from a `RIVER` position, `Check(1)` then `Check(0)`: `BettingRoundEnded` then `ShowdownReached`, `street == SHOWDOWN`, `seatToAct == null`, no `StreetDealt` |
| `chipsAreConservedAcrossTheAdvance` | `chipsInPlay == 20_000` after every action above |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition` holds for the action that ends each round |

## Acceptance criteria

- [ ] `StreetAdvanceTest.aFinishedPreflopDealsThreeFlopCards` passes
- [ ] `StreetAdvanceTest.theEventOrderIsEndedThenDealtThenActionOn` passes
- [ ] `StreetAdvanceTest.theNonButtonActsFirstOnTheFlop` passes
- [ ] `StreetAdvanceTest.everyCommitmentIsSweptAndTheBarResets` passes
- [ ] `StreetAdvanceTest.committedThisHandSurvivesTheAdvance` passes
- [ ] `StreetAdvanceTest.theDeckShrinksByExactlyTheCardsDealt` passes
- [ ] `StreetAdvanceTest.noCardAppearsTwice` passes
- [ ] `StreetAdvanceTest.theRiverEndsInShowdownReached` passes
- [ ] `StreetAdvanceTest.chipsAreConservedAcrossTheAdvance` passes
- [ ] `StreetAdvanceTest.theEventsDescribeTheTransition` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
