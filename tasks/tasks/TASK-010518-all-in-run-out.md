---
schema: 2
id: TASK-010518
title: Run the board out when nobody can bet again
type: task
status: backlog
parent: STORY-0105
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, rules, cards]
depends_on: [TASK-010517]
verify:
  - ./gradlew :poker-engine:test --tests '*AllInRunOutTest'
  - ./gradlew :poker-engine:check
---

## Goal

Once a seat is all-in and the betting cannot continue, the rest of the board appears in one step
and the hand arrives at the showdown without asking anybody anything.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/AllInRunOutTest.kt` | create |

Read `DealerEvents.kt`, `Street.kt`, `Deck.kt`. Modify none of them.

## Scope

- A seat can act when `!hasFolded && !isAllIn && stack > 0`. After `BettingRoundEnded`, if fewer
  than two seats can act, `continueHand` runs the board out instead of dealing one street:
  a `StreetDealt` for every street from `state.street.next` through `RIVER` that is not yet on
  the board, then `ShowdownReached`, and **no `ActionOn` anywhere**.
- Each `StreetDealt` deals `street.boardCards - board.size` cards from the running deck, in
  street order, carrying `deck` forward exactly as `TASK-010517` does. Three, then one, then
  one — never five at once, so the log reads like the deal it was.
- One seat all-in and the other still holding chips is the same case: with only one seat able to
  act there is nobody to bet into. Say so in the KDoc — it is not only the both-all-in case.
- Already on the river when this happens: no card is dealt, just `ShowdownReached`.

## Out of scope

- Awarding the pot, returning the uncalled part of the larger stack's bet, revealing hands —
  STORY-0106. This ticket stops at `ShowdownReached`.

## Tests

`AllInRunOutTest`, JUnit 5, opening with
`startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(1L))` and driving
`DefaultPokerEngine.handle` unless stated.

| Test | Proves |
| --- | --- |
| `bothAllInPreflopRunsOutFiveCards` | `AllIn(0)`, `Call(1)`: `board.size == 5`, `street == SHOWDOWN` |
| `theRunOutDealsEachStreetInOrder` | the dealer events are `BettingRoundEnded`, `StreetDealt(FLOP, 3)`, `StreetDealt(TURN, 1)`, `StreetDealt(RIVER, 1)`, `ShowdownReached` |
| `theRunOutEmitsNoActionOn` | no `ActionOn` in the result, `seatToAct == null` |
| `aShorterStackAllInStillRunsOut` | stacks `listOf(4_000, 10_000)`: `AllIn(0)`, `Call(1)` runs out even though seat 1 still holds chips |
| `aRunOutFromTheTurnDealsOnlyTheRiver` | from a `TURN` position with seat 1 all-in and matched, one `StreetDealt` then `ShowdownReached` |
| `noCardIsDealtTwiceInARunOut` | four hole cards and five board cards are nine distinct cards, `deck.remaining == 43` |
| `chipsAreConservedByTheRunOut` | `chipsInPlay == 20_000` before and after, and `pot == potTotal` |
| `theEventsDescribeTheTransition` | `assertEventsDescribeTheTransition` holds for the action that triggers the run-out |

## Acceptance criteria

- [ ] `AllInRunOutTest.bothAllInPreflopRunsOutFiveCards` passes
- [ ] `AllInRunOutTest.theRunOutDealsEachStreetInOrder` passes
- [ ] `AllInRunOutTest.theRunOutEmitsNoActionOn` passes
- [ ] `AllInRunOutTest.aShorterStackAllInStillRunsOut` passes
- [ ] `AllInRunOutTest.aRunOutFromTheTurnDealsOnlyTheRiver` passes
- [ ] `AllInRunOutTest.noCardIsDealtTwiceInARunOut` passes
- [ ] `AllInRunOutTest.chipsAreConservedByTheRunOut` passes
- [ ] `AllInRunOutTest.theEventsDescribeTheTransition` passes
- [ ] `StreetAdvanceTest` still passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
