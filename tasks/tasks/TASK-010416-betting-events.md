---
schema: 2
id: TASK-010416
title: Betting events
type: task
status: done
parent: STORY-0104
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, domain, contract]
depends_on: [TASK-010415]
verify:
  - ./gradlew :poker-engine:test --tests '*BettingEventsTest'
  - ./gradlew :poker-engine:check
---

## Goal

The six things a player doing something with chips leaves in the log, under one sub-interface so
the projection can dispatch on it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingEvents.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/BettingEventsTest.kt` | create |

Read `GameEvent.kt` (`TASK-010415`) for the base interface and its rules. Do not modify it — a
sealed interface may be implemented from another file in the same package and module.

## Scope

- `BettingEvents.kt`, package `duels.poker.engine.game`:

  ```kotlin
  /** Something a seat did with its chips. The projection dispatches on this sub-interface. */
  public sealed interface BettingEvent : GameEvent {
      public val seat: Int
  }

  public data class PlayerFolded(override val sequence: Int, override val seat: Int) : BettingEvent
  public data class PlayerChecked(override val sequence: Int, override val seat: Int) : BettingEvent
  public data class PlayerCalled(override val sequence: Int, override val seat: Int, val to: Int) : BettingEvent
  public data class PlayerBet(override val sequence: Int, override val seat: Int, val to: Int) : BettingEvent
  public data class PlayerRaised(override val sequence: Int, override val seat: Int, val to: Int) : BettingEvent
  public data class PlayerAllIn(override val sequence: Int, override val seat: Int, val to: Int) : BettingEvent
  ```

- `to` is the seat's **street total after the action**, exactly as in `PlayerAction`. KDoc on the
  file or on each event repeats the convention and its consequence: the chips that moved are
  `to - committedThisStreet`, which only the state knows, so a reader of the log folds rather
  than sums.
- `PlayerCalled.to` equals the bet being matched, except for a call all-in for less, where it is
  the caller's whole stack.
- `require`s: `sequence >= 0`, `seat in 0..1`, `to > 0` where present.

## Out of scope

- `BettingRoundEnded`, street, showdown and pot events — `TASK-010417`. They are the dealer's
  acts, not a player's.
- Emitting these — STORY-0105. Folding them — `TASK-010421`.
- **No `when` over `GameEvent` in these tests**: `TASK-010417` adds more subtypes and would break
  it.

## Tests

`BettingEventsTest`, JUnit 5.

| Test | Proves |
| --- | --- |
| `everyBettingEventIsAGameEventWithASeat` | all six are `GameEvent`, are `BettingEvent`, report `seat == 1` and `version == EVENT_SCHEMA_VERSION` |
| `totalsAreStreetTotals` | `PlayerBet(2, 0, to = 300).to == 300`, `PlayerRaised(3, 1, to = 900).to == 900`, `PlayerCalled(4, 0, to = 900).to == 900`, `PlayerAllIn(5, 1, to = 4_000).to == 4_000` |
| `foldAndCheckCarryNoAmount` | `PlayerFolded(1, 0)` and `PlayerChecked(1, 0)` construct from sequence and seat alone |
| `rejectsANonPositiveTotal` | `PlayerBet(1, 0, to = 0)` and `PlayerRaised(1, 0, to = -1)` each throw `IllegalArgumentException` |
| `rejectsAnInvalidSeatOrSequence` | `seat = 2` and `sequence = -1` each throw |

## Acceptance criteria

- [ ] `BettingEventsTest.everyBettingEventIsAGameEventWithASeat` passes
- [ ] `BettingEventsTest.totalsAreStreetTotals` passes
- [ ] `BettingEventsTest.foldAndCheckCarryNoAmount` passes
- [ ] `BettingEventsTest.rejectsANonPositiveTotal` passes
- [ ] `BettingEventsTest.rejectsAnInvalidSeatOrSequence` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
