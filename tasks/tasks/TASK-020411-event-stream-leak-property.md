---
schema: 2
id: TASK-020411
title: No filtered event stream carries a card its recipient may not see, over a thousand duels
type: task
status: done
parent: STORY-0204
module: poker-ai
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [ai, simulation, security, property]
depends_on: [TASK-020406, TASK-020409]
verify:
  - ./gradlew :poker-ai:test --tests '*EventStreamLeakTest'
  - ./gradlew :poker-ai:check
---

## Goal

Over a thousand simulated duels, the events a seat is delivered carry only its own hole cards,
the board, and hands the engine has revealed — and every delivered event is the original object,
never a copy.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/test/kotlin/duels/poker/ai/EventStreamLeakTest.kt` | create |

Read, do not modify: `poker-ai/src/test/kotlin/duels/poker/ai/ObservedDuel.kt` (`observeDuel`,
`ObservedHand.events`, `seedReport`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt` (`visibleTo`),
`poker-engine/src/test/kotlin/duels/poker/engine/game/CardSecrecyTest.kt` (the `cardsIn` shape —
this file needs its own copy of that exhaustive `when`, since engine test sources are not on
`poker-ai`'s classpath).

## Scope

- One new test class. It changes no production file.
- A private `cardsIn(event: GameEvent): List<Card>` — an exhaustive `when` over all seventeen
  concrete event types with no `else`, so a future card-carrying event cannot be added without
  this test failing to compile.
- The stream under test is `visibleTo(hand.events, seat)` for each seat of each hand.
- The three claims:
  1. the other seat's `HoleCardsDealt` is never in the stream, and every other event is;
  2. every delivered event is the same instance as the one in `hand.events` (`assertSame`), so no
     partially redacted copy exists anywhere;
  3. every card in a seat's stream is either that seat's own dealt cards, a board card from
     `StreetDealt`, or a card from a `HandRevealed`.
- Every assertion message is built with `seedReport(duel.seed, hand.handSeed)`.
- `@Timeout(300)` on the thousand-duel test, `@Timeout(120)` on the rest.

## Out of scope

- `PlayerView`: this ticket never builds one — `TASK-020410` covers the state side, and keeping
  the two properties in separate files keeps each failure unambiguous.
- Weakening or touching `CardSecrecyTest`, which asserts the same secrecy from the log's side and
  must keep passing untouched.
- Editing the harness or anything under `poker-ai/src/main/`.

## Tests

`EventStreamLeakTest`, JUnit 5, package `duels.poker.ai`.

| Test | Proves |
| --- | --- |
| `noSeatReceivesACardItIsNotEntitledTo` | over seeds `1L..1000L`, every card in `visibleTo(hand.events, seat)` is the seat's own dealt card, a board card, or a revealed card |
| `noSeatReceivesTheOtherSeatsDeal` | over seeds `1L..1000L`, no stream contains a `HoleCardsDealt` addressed to the other seat |
| `everyDeliveredEventIsTheOriginalObject` | over seeds `1L..100L`, each delivered event is `assertSame` to its counterpart in `hand.events` |
| `theOnlyEventDroppedIsTheOtherSeatsDeal` | over seeds `1L..100L`, each seat's stream is exactly one event shorter than `hand.events`, and the missing one is the other seat's `HoleCardsDealt` |
| `aMuckedHandAppearsInNoStream` | over seeds `1L..100L`, for hands reaching `ShowdownReached` where a seat never appears in a `HandRevealed`, that seat's cards appear in the other seat's stream not at all |
| `theSampleContainsShowdownsFoldsAndMucks` | over seeds `1L..100L`, the sample holds at least one of each, so none of the properties above is vacuous |

## Acceptance criteria

- [ ] `EventStreamLeakTest.noSeatReceivesACardItIsNotEntitledTo` passes over seeds `1L..1000L`
- [ ] `EventStreamLeakTest.noSeatReceivesTheOtherSeatsDeal` passes over seeds `1L..1000L`
- [ ] `EventStreamLeakTest.everyDeliveredEventIsTheOriginalObject` passes
- [ ] `EventStreamLeakTest.theOnlyEventDroppedIsTheOtherSeatsDeal` passes
- [ ] `EventStreamLeakTest.aMuckedHandAppearsInNoStream` passes
- [ ] `EventStreamLeakTest.theSampleContainsShowdownsFoldsAndMucks` passes
- [ ] The file's private `cardsIn` has no `else` branch and names all seventeen event types
- [ ] Every assertion message in the file is built with `seedReport(...)`
- [ ] No file outside the one in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
