---
schema: 2
id: TASK-010608
title: Give the synthetic showdown fixtures hole cards
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [engine, tests]
depends_on: [TASK-010607]
verify:
  - ./gradlew :poker-engine:test --tests '*StreetAdvanceTest'
  - ./gradlew :poker-engine:test --tests '*AllInRunOutTest'
  - ./gradlew :poker-engine:test --tests '*DefaultPokerEngineContractTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every merged fixture that can reach a showdown holds two hole cards per seat, so the showdown
resolution landing in `TASK-010611` and `TASK-010612` has seven cards to evaluate instead of five.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StreetAdvanceTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/AllInRunOutTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DefaultPokerEngineContractTest.kt` | modify |

Read `GameStates.kt` and `card/Cards.kt` (the `cards("As Kd")` helper). Modify neither.

## Scope

This ticket changes **fixtures only**. Not one assertion is added, removed, weakened or moved,
and every test in the three classes passes before and after with the same reasoning.

- `StreetAdvanceTest.riverState()`: seat 0 gets `cards("Qh Jc")`, seat 1 `cards("Td 8c")`. The
  board is `As Kd 7c 2h 9s` and no further card is dealt from this fixture, so these four are
  distinct from it and from each other. They also decide the hand — seat 0 plays `A K Q J 9`
  against seat 1's `A K T 9 8` — which is what `TASK-010611` will assert on.
- `AllInRunOutTest.turnWithSeatOneAllIn()`: seat 0 gets `cards("Qs Jh")`, seat 1 `cards("9d 9c")`.
  This fixture deals its river off `Deck.full()`, whose first card is the two of clubs
  (`Card.all` is rank-major, so `all[0] == 2c`), so the four hole cards must avoid `2c` as well
  as the board — these do.
- `DefaultPokerEngineContractTest.allInPosition`: take the hole cards from the fixture's own deck
  rather than naming them, since its board is already dealt from that deck:

  ```kotlin
  val holeDeal = allInDeal.deck.deal(4)
  ```

  seat 0 gets `holeDeal.cards[0], holeDeal.cards[1]`, seat 1 the other two, and the fixture
  carries `deck = holeDeal.deck` so the run-out cannot deal a card a seat is already holding.
  `everyFixtureHasADeckThatExcludesItsBoard` must still pass untouched.

## Out of scope

- `flopPosition` in the same file, and every fixture reachable only by a fold: a fold reveals
  nothing and evaluates nothing, so it needs no cards.
- Any behaviour change. `StreetProgression.kt` is not in this ticket's file list.

## Tests

No new tests. The three classes above are the test: they must pass unchanged, which is what
proves the fixtures are still consistent (no duplicate card, no deck that still holds a dealt
card).

## Acceptance criteria

- [ ] `StreetAdvanceTest` passes with every assertion byte-identical to before
- [ ] `AllInRunOutTest` passes with every assertion byte-identical to before
- [ ] `DefaultPokerEngineContractTest` passes with every assertion byte-identical to before
- [ ] Each of the three fixtures named above gives both seats exactly two hole cards, and the
      cards named in Scope are the cards used
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
