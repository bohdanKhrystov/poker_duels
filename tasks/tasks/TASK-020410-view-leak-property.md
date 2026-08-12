---
schema: 2
id: TASK-020410
title: No view shows a card its viewer may not see, over a thousand duels
type: task
status: backlog
parent: STORY-0204
module: poker-ai
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [ai, simulation, security, property]
depends_on: [TASK-020405, TASK-020407, TASK-020409]
verify:
  - ./gradlew :poker-ai:test --tests '*PlayerViewLeakTest'
  - ./gradlew :poker-ai:check
---

## Goal

Over a thousand simulated duels, at every point of every hand, a seat's `PlayerView` carries its
own cards, the board, and no card of the other seat that the engine has not already revealed.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/test/kotlin/duels/poker/ai/PlayerViewLeakTest.kt` | create |

Read, do not modify: `poker-ai/src/test/kotlin/duels/poker/ai/ObservedDuel.kt` (`observeDuel`,
`ObservedStep`, `seedReport`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` (`of`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt` (`revealedSeats`).

## Scope

- One new test class. It changes no production file: a failure here is a leak, and the fix is a
  new ticket against the engine, not an edit from this branch.
- A private helper keeps the file short:

  ```kotlin
  private fun forEachStep(seeds: LongRange, body: (ObservedDuel, ObservedHand, ObservedStep) -> Unit)
  ```

- The view under test at every step is built exactly as a server would:
  `PlayerView.of(step.state, viewer, revealedSeats(step.eventsSoFar))` — reveals come from the
  events emitted **so far**, so the property is asserted at every intermediate point, not only at
  the end of the hand.
- The cards a view exposes are `view.seats.flatMap { it.holeCards } + view.board.cards`. The
  opponent's actual cards are `step.state.seat(other).holeCards`. Where the opponent is not in
  `revealedSeats(step.eventsSoFar)`, the intersection must be empty.
- Every assertion message is built with `seedReport(duel.seed, hand.handSeed)`, so a failure names
  the two seeds that reproduce it, plus the street and the offending cards.
- `@Timeout(300)` on the thousand-duel test, `@Timeout(120)` on the rest, matching
  `SimulationRunnerTest`'s scale.

## Out of scope

- The filtered event stream: `TASK-020411` asserts the same property from the event side.
- Re-testing what `PlayerViewOfTest` and `PlayerViewRevealTest` already pin on hand-built states.
  This ticket exists for the cases nobody thought to write down.
- Editing the harness. If a needed field is missing from `ObservedStep`, stop and raise a ticket
  rather than widening it here.

## Tests

`PlayerViewLeakTest`, JUnit 5, package `duels.poker.ai`.

| Test | Proves |
| --- | --- |
| `noViewEverShowsAnUnrevealedOpponentCard` | over seeds `1L..1000L`, at every step and for both viewers, the view exposes no card of an opponent not in `revealedSeats(eventsSoFar)` |
| `everyViewShowsTheViewersOwnCards` | over seeds `1L..100L`, `view.viewer.holeCards == step.state.seat(viewer).holeCards` at every step |
| `aRevealedHandAppearsInTheOpponentsView` | over seeds `1L..100L`, at the last step of every hand that emitted `HandRevealed`, the revealed seat's cards appear in the other seat's view |
| `aFoldedHandNeverAppearsInTheOpponentsView` | over seeds `1L..100L`, for every hand where a seat folded, no view of the other seat carries the folder's cards at any step |
| `aMuckedHandNeverAppearsInTheOpponentsView` | over seeds `1L..100L`, for every hand reaching `ShowdownReached` where a seat is absent from `revealedSeats(hand.events)`, no view of the other seat carries that seat's cards at any step |
| `theSampleContainsFoldsRevealsAndMucks` | over seeds `1L..100L`, the sample holds at least one folded hand, one `HandRevealed` and one mucked hand, so none of the properties above is vacuous |

## Acceptance criteria

- [ ] `PlayerViewLeakTest.noViewEverShowsAnUnrevealedOpponentCard` passes over seeds `1L..1000L`
- [ ] `PlayerViewLeakTest.everyViewShowsTheViewersOwnCards` passes
- [ ] `PlayerViewLeakTest.aRevealedHandAppearsInTheOpponentsView` passes
- [ ] `PlayerViewLeakTest.aFoldedHandNeverAppearsInTheOpponentsView` passes
- [ ] `PlayerViewLeakTest.aMuckedHandNeverAppearsInTheOpponentsView` passes
- [ ] `PlayerViewLeakTest.theSampleContainsFoldsRevealsAndMucks` passes
- [ ] Every assertion message in the file is built with `seedReport(...)`
- [ ] No file outside the one in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
