---
schema: 2
id: TASK-010417
title: Dealer events for street progress and showdown
type: task
status: ready
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010416]
verify:
  - ./gradlew :poker-engine:test --tests '*DealerEventsTest'
  - ./gradlew :poker-engine:check
---

## Goal

What the dealer does between betting — closing a round, putting cards out, calling the showdown,
showing a hand — as its own sub-hierarchy of the event log.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DealerEventsTest.kt` | create |

Read `GameEvent.kt`, `Street.kt` and `duels/poker/engine/card/Card.kt`. Do not modify them.

## Scope

- `DealerEvents.kt`, package `duels.poker.engine.game`:

  ```kotlin
  /** Something the house did: no seat chose it. The projection dispatches on this sub-interface. */
  public sealed interface DealerEvent : GameEvent

  /** The betting on [street] is closed; every commitment goes to the pot. */
  public data class BettingRoundEnded(override val sequence: Int, val street: Street) : DealerEvent

  /** [cards] join the board and the hand moves to [street]: three for the flop, one otherwise. */
  public data class StreetDealt(
      override val sequence: Int,
      val street: Street,
      val cards: List<Card>,
  ) : DealerEvent

  public data class ShowdownReached(override val sequence: Int) : DealerEvent

  /** A seat that showed its hand. Never emitted for a fold or a muck. */
  public data class HandRevealed(
      override val sequence: Int,
      val seat: Int,
      val cards: List<Card>,
  ) : DealerEvent
  ```

- `require`s: `sequence >= 0`; `seat in 0..1`; `HandRevealed.cards` is two distinct cards;
  `BettingRoundEnded.street.isBetting`; `StreetDealt.street` is `FLOP`, `TURN` or `RIVER`, and
  `cards.size` is 3 for the flop and 1 for the turn and river, with no duplicates.
- KDoc on `HandRevealed` repeating the project rule it exists to respect: a folded or mucked hand
  appears in no event, anywhere, so the engine emits this only for a hand actually shown.

## Out of scope

- `PotAwarded`, `UncalledBetReturned`, `HandFinished` — `TASK-010418`, same file.
- Deciding when a round ends or who shows first — STORY-0105 and STORY-0106.
- **No `when` over `GameEvent` or `DealerEvent` in these tests**: `TASK-010418` adds subtypes.

## Tests

`DealerEventsTest`, JUnit 5. Build cards with `duels.poker.engine.card.cards(...)`.

| Test | Proves |
| --- | --- |
| `everyDealerEventIsAVersionedGameEvent` | all four are `GameEvent` and `DealerEvent` and report `version == EVENT_SCHEMA_VERSION` |
| `bettingRoundEndedNamesItsStreet` | `BettingRoundEnded(4, Street.FLOP).street == Street.FLOP` |
| `bettingRoundEndedRejectsANonBettingStreet` | `Street.SHOWDOWN` and `Street.COMPLETE` each throw `IllegalArgumentException` |
| `streetDealtCarriesThreeCardsForTheFlop` | `StreetDealt(5, Street.FLOP, cards("As Kd 7c"))` constructs |
| `streetDealtCarriesOneCardForTheTurnAndRiver` | one card for `TURN` and for `RIVER` constructs |
| `streetDealtRejectsAWrongCardCount` | one card with `FLOP`, and three cards with `TURN`, each throw |
| `streetDealtRejectsAStreetThatDealsNothing` | `Street.PREFLOP` and `Street.SHOWDOWN` each throw |
| `handRevealedShowsTwoCardsForOneSeat` | `HandRevealed(9, 1, cards("As Kd"))` reads back; one card and duplicate cards each throw |

## Acceptance criteria

- [ ] `DealerEventsTest.everyDealerEventIsAVersionedGameEvent` passes
- [ ] `DealerEventsTest.bettingRoundEndedNamesItsStreet` passes
- [ ] `DealerEventsTest.bettingRoundEndedRejectsANonBettingStreet` passes
- [ ] `DealerEventsTest.streetDealtCarriesThreeCardsForTheFlop` passes
- [ ] `DealerEventsTest.streetDealtCarriesOneCardForTheTurnAndRiver` passes
- [ ] `DealerEventsTest.streetDealtRejectsAWrongCardCount` passes
- [ ] `DealerEventsTest.streetDealtRejectsAStreetThatDealsNothing` passes
- [ ] `DealerEventsTest.handRevealedShowsTwoCardsForOneSeat` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
