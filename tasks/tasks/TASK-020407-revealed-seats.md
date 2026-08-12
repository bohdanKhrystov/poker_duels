---
schema: 2
id: TASK-020407
title: Name the seats a hand has already revealed
type: task
status: done
parent: STORY-0204
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [engine, projection, security, showdown]
depends_on: [TASK-020406]
verify:
  - ./gradlew :poker-engine:test --tests '*RevealedSeatsTest'
  - ./gradlew :poker-engine:check
---

## Goal

`revealedSeats(events)` turns a hand's events into the set of seats whose hands have been shown,
so a caller never has to recognise a `HandRevealed` for itself.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/RevealedSeatsTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt`
(`HandRevealed`).

## Scope

- One public function with KDoc, appended to `EventRedaction.kt`:

  ```kotlin
  public fun revealedSeats(events: List<GameEvent>): Set<Int>
  ```

  returning `events.filterIsInstance<HandRevealed>().map { it.seat }.toSet()`.
- KDoc says what it is for: the argument to `PlayerView.of`'s `revealed` parameter, and that
  `events` is **one hand's** events — `GameEvent` is hand-scoped, and feeding it a longer stream
  would carry a previous hand's reveals into this one.
- KDoc says why the state cannot answer this: `GameState` holds both seats' hole cards from the
  deal onwards, so only the log records what was actually shown.
- Nothing already in `EventRedaction.kt` changes.

## Out of scope

- Deciding *who* reveals at a showdown — `revealOrder` and `TASK-010623` already own that; this
  function only reads what was emitted.
- Any `MatchEvent`. Reveals are hand-scoped (`ADR-0009`).

## Tests

`RevealedSeatsTest`, JUnit 5, package `duels.poker.engine.game`.

| Test | Proves |
| --- | --- |
| `namesNoSeatForAnEmptyLog` | `revealedSeats(emptyList())` is empty |
| `namesNoSeatWhenNothingWasShown` | a log of `HandStarted`, two `HoleCardsDealt`, `PlayerFolded`, `HandFinished` yields an empty set |
| `namesTheSeatThatShowed` | a log with one `HandRevealed(seat = 1)` yields `setOf(1)` |
| `namesBothSeatsAtATiedShowdown` | a log with `HandRevealed` for seat 0 and seat 1 yields `setOf(0, 1)` |
| `aDealIsNotAReveal` | a log whose only card-carrying events are `HoleCardsDealt` and `StreetDealt` yields an empty set |

## Acceptance criteria

- [ ] `RevealedSeatsTest.namesNoSeatForAnEmptyLog` passes
- [ ] `RevealedSeatsTest.namesNoSeatWhenNothingWasShown` passes
- [ ] `RevealedSeatsTest.namesTheSeatThatShowed` passes
- [ ] `RevealedSeatsTest.namesBothSeatsAtATiedShowdown` passes
- [ ] `RevealedSeatsTest.aDealIsNotAReveal` passes
- [ ] `EventRedactionTest` passes with no edit to that file
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
