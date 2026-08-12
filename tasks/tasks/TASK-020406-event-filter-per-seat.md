---
schema: 2
id: TASK-020406
title: Filter an event for one recipient
type: task
status: done
parent: STORY-0204
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, projection, security]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*EventRedactionTest'
  - ./gradlew :poker-engine:check
---

## Goal

`visibleTo(event, seat)` answers *may seat N see this event?* — the event unchanged, or `null`,
never a copy with a card removed.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/EventRedaction.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/EventRedactionTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/GameEvent.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/DealerEvents.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/game/BettingEvents.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/CardSecrecyTest.kt` (its private `cardsIn`
is the shape of `when` this ticket wants).

## Scope

- Two public functions with KDoc, package `duels.poker.engine.game`:

  ```kotlin
  public fun visibleTo(event: GameEvent, seat: Int): GameEvent?
  public fun visibleTo(events: List<GameEvent>, seat: Int): List<GameEvent>
  ```

  The list form is `events.mapNotNull { visibleTo(it, seat) }` and nothing more.
- `require(seat in 0..1)` first, naming the value.
- The body is **one `when` over `GameEvent` naming all seventeen concrete types**, with no `else`
  and no branch on `BettingEvent` or `DealerEvent`. A new event type must then fail to compile
  until someone decides who may see it — a sub-interface branch would silently pass a future
  card-carrying event through, which is the failure this ticket exists to make impossible.
- Exactly one branch filters: `is HoleCardsDealt -> if (event.seat == seat) event else null`.
  Every other branch returns `event` — the same instance, never a `copy`. `HandRevealed` reaches
  both seats: the engine emits it only for a hand actually shown (`ADR-0008`).
- No blanked, masked or partially-cleared event is ever constructed. Dropping the whole event is
  what guarantees no half-redacted card object exists to be leaked by a later refactor; say so in
  the KDoc.

## Out of scope

- Which seats have been revealed so far: `TASK-020407` adds `revealedSeats` to this same file.
- Anything that broadcasts a filtered stream — `STORY-0207`.
- Touching `CardSecrecyTest`, `GameEvent` or any event type. This ticket adds a reader; it changes
  no event and no state.

## Tests

`EventRedactionTest`, JUnit 5, package `duels.poker.engine.game`. Reuse the seventeen-event
fixture list in `GameEventSerializationTest.everySubtypeRoundTripsThroughTheParentSerializer` as
the sample — build it as a private `val` in this file with `Card.parse("As")` and friends, seat 0
holding `As Kh` and seat 1 holding `Qd Jc`.

| Test | Proves |
| --- | --- |
| `aSeatReceivesItsOwnDeal` | `visibleTo(HoleCardsDealt(2, 0, cards("As Kh")), 0)` returns the same instance |
| `aSeatNeverReceivesTheOtherSeatsDeal` | `visibleTo(HoleCardsDealt(2, 1, cards("Qd Jc")), 0)` returns `null` |
| `everyOtherEventTypeIsDeliveredUnchanged` | for each of the sixteen non-`HoleCardsDealt` fixtures and each seat, `visibleTo` returns the identical instance (`assertSame`) |
| `theListFormDropsOnlyTheOtherSeatsDeal` | over the full fixture list, `visibleTo(list, 0)` has exactly one fewer entry, and the missing one is seat 1's `HoleCardsDealt` |
| `aRevealedHandReachesBothSeats` | `HandRevealed(13, 1, cards("Qd Jc"))` is delivered to seat 0 and to seat 1 |
| `theFilteredStreamKeepsItsOrder` | `visibleTo(list, 1)` is the fixture list with one entry removed, order otherwise unchanged |
| `rejectsASeatOutsideZeroOrOne` | `visibleTo(HandFinished(16), 2)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `EventRedactionTest.aSeatReceivesItsOwnDeal` passes
- [ ] `EventRedactionTest.aSeatNeverReceivesTheOtherSeatsDeal` passes
- [ ] `EventRedactionTest.everyOtherEventTypeIsDeliveredUnchanged` passes
- [ ] `EventRedactionTest.theListFormDropsOnlyTheOtherSeatsDeal` passes
- [ ] `EventRedactionTest.aRevealedHandReachesBothSeats` passes
- [ ] `EventRedactionTest.theFilteredStreamKeepsItsOrder` passes
- [ ] `EventRedactionTest.rejectsASeatOutsideZeroOrOne` passes
- [ ] `EventRedaction.kt` contains no `else ->` branch, no `is BettingEvent ->` branch and no
      `is DealerEvent ->` branch, and names all seventeen concrete event types
- [ ] `EventRedaction.kt` contains no call to `copy(`
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
