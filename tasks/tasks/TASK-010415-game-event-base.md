---
schema: 2
id: TASK-010415
title: GameEvent base and hand lifecycle events
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010414]
verify:
  - ./gradlew :poker-engine:test --tests '*GameEventTest'
  - ./gradlew :poker-engine:check
---

## Goal

The root of the durable event vocabulary — versioned, sequenced — plus the four events that open
a hand.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/GameEvent.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/GameEventTest.kt` | create |

Read `docs/adr/ADR-0001-event-sourced-engine-contract.md` and
`poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt`.

## Scope

- `GameEvent.kt`, package `duels.poker.engine.game`:

  ```kotlin
  /** Payload version of every event in this module. Bumping it is a log migration. */
  public const val EVENT_SCHEMA_VERSION: Int = 1

  public sealed interface GameEvent {
      /**
       * Position within the hand, starting at 0, dense and gap-free: the next event a hand
       * produces always has `sequence == state.eventCount`.
       */
      public val sequence: Int
      public val version: Int get() = EVENT_SCHEMA_VERSION
  }

  public data class HandStarted(
      override val sequence: Int,
      val handNumber: Int,
      val buttonSeat: Int,
      val smallBlind: Int,
      val bigBlind: Int,
      val stacks: List<Int>,
  ) : GameEvent

  /** [to] is the seat's street total after posting; a short stack posts all-in for less. */
  public data class BlindPosted(
      override val sequence: Int,
      val seat: Int,
      val to: Int,
      val isBigBlind: Boolean,
  ) : GameEvent

  /** Addressed to one seat, so a broadcast filters by recipient rather than by field. */
  public data class HoleCardsDealt(
      override val sequence: Int,
      val seat: Int,
      val cards: List<Card>,
  ) : GameEvent

  public data class ActionOn(override val sequence: Int, val seat: Int) : GameEvent
  ```

- `require`s: `sequence >= 0` on every event, `stacks.size == 2`, `to > 0`, `cards.size == 2` and
  distinct, `seat in 0..1`, `buttonSeat in 0..1`.
- KDoc on `GameEvent` stating the three rules that hold for every event ever added:
  1. events are **facts, not instructions** — `PlayerBet(seat, to)`, never `ShowBetAnimation`;
  2. no event carries an undealt or unrevealed card, ever — that is a project non-negotiable, and
     it is why `HandStarted` carries neither the shuffled deck nor the seed;
  3. `version` is a property with a default, not a constructor parameter, so it never takes part
     in equality but is always available to a serializer (`TASK-010801`).
- `version` is not repeated on the subtypes.

## Out of scope

- Betting events — `TASK-010416`. Street and showdown events — `TASK-010417`.
- Emitting any of these — STORY-0105.
- Folding them into a state — `TASK-010423`.
- **Do not write a `when` over `GameEvent` in this ticket's tests.** Two later tickets add
  subtypes, and an exhaustive `when` written here would stop compiling when they land. The one
  exhaustive `when` lives in the projection.

## Tests

`GameEventTest`, JUnit 5. Build cards with `duels.poker.engine.card.cards("As Kd")`.

| Test | Proves |
| --- | --- |
| `everyLifecycleEventCarriesTheSchemaVersion` | all four report `version == EVENT_SCHEMA_VERSION` |
| `versionDoesNotAffectEquality` | two `ActionOn(3, 1)` values are equal and share a `hashCode` |
| `handStartedDescribesTheOpeningPosition` | `HandStarted(0, 7, 1, 50, 100, listOf(9_000, 11_000))` reads back all six fields |
| `blindPostedNamesTheBlindAndTheTotal` | `BlindPosted(1, seat = 0, to = 50, isBigBlind = false)` and the big blind case read back |
| `holeCardsDealtIsAddressedToOneSeat` | `HoleCardsDealt(3, 1, cards("As Kd")).seat == 1` and its cards are those two |
| `rejectsAnInvalidEvent` | `sequence = -1`, `stacks` of size 1, `HoleCardsDealt` with one card, and `seat = 2` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `GameEventTest.everyLifecycleEventCarriesTheSchemaVersion` passes
- [ ] `GameEventTest.versionDoesNotAffectEquality` passes
- [ ] `GameEventTest.handStartedDescribesTheOpeningPosition` passes
- [ ] `GameEventTest.blindPostedNamesTheBlindAndTheTotal` passes
- [ ] `GameEventTest.holeCardsDealtIsAddressedToOneSeat` passes
- [ ] `GameEventTest.rejectsAnInvalidEvent` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
