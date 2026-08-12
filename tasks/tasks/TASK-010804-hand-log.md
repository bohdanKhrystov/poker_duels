---
schema: 2
id: TASK-010804
title: HandLog, the replayable record of one hand
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, replay, log]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*HandLogTest'
  - ./gradlew :poker-engine:check
---

## Goal

One hand is a value: the seed it was dealt from, the parameters it opened with, the actions it
was given and the events it produced — enough, by itself, to replay the hand card for card.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/HandLog.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandLogTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/GameEvent.kt`,
`.../game/PlayerAction.kt`, `.../game/DealerEvents.kt`.

## Scope

- New package `duels.poker.engine.log`. One top-level constant and one data class, both public,
  both with KDoc:

  ```kotlin
  public const val HAND_LOG_VERSION: Int = 1

  public data class HandLog(
      val seed: Long,
      val handNumber: Int,
      val buttonSeat: Int,
      val stacks: List<Int>,
      val smallBlind: Int,
      val bigBlind: Int,
      val actions: List<PlayerAction>,
      val events: List<GameEvent>,
      val version: Int = HAND_LOG_VERSION,
  )
  ```

- The header fields are exactly the arguments `startHand` takes, plus the seed, because replay
  reconstructs the hand by calling `startHand` again — that is why the deck is not stored and
  must never be added here.
- `version` is a constructor property with a default, so it takes part in equality: a log written
  under a different version is a different value, never a silently compatible one.
- `init` validation, each with a message naming the offending value:

  | Rule |
  | --- |
  | `version >= 1` |
  | `handNumber >= 1` |
  | `buttonSeat` is 0 or 1 |
  | `stacks` has exactly 2 entries, each at least 1 |
  | `0 < smallBlind && smallBlind < bigBlind` |
  | `events` is non-empty and `events.first()` is a `HandStarted` |
  | `events[i].sequence == i` for every index, so the log is dense and gap-free |

- A log of an unfinished hand is legal: nothing requires `HandFinished` to be last, because a
  server appends to the log as the hand plays.

## Out of scope

- Serialising the log to text or bytes — `TASK-010810`, blocked on `DEC-006`.
- Replaying it — `TASK-010805`.
- Anything match-level: several hands, blind levels, a match result — `TASK-010811`.

## Tests

`HandLogTest`, JUnit 5, package `duels.poker.engine.log`. Build event lists directly from the
event types; no cards are needed beyond what a test chooses to construct.

| Test | Proves |
| --- | --- |
| `carriesTheSeedAndTheOpeningParameters` | a constructed log returns the seed, hand number, button, stacks and blinds it was given |
| `defaultsToTheCurrentLogVersion` | `HandLog(...).version == HAND_LOG_VERSION` and `HAND_LOG_VERSION == 1` |
| `twoLogsWithTheSameContentAreEqual` | data-class equality holds, which every later round-trip assertion depends on |
| `rejectsAnEventSequenceWithAGap` | events with sequences 0 and 2 throw `IllegalArgumentException` |
| `rejectsALogThatDoesNotStartWithHandStarted` | a log whose first event is `ActionOn(0, 0)` throws `IllegalArgumentException` |
| `rejectsBlindsThatAreNotAscending` | `smallBlind >= bigBlind` throws `IllegalArgumentException` |
| `rejectsStacksThatAreNotTwoSeats` | a one-entry `stacks` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `HandLogTest.carriesTheSeedAndTheOpeningParameters` passes
- [ ] `HandLogTest.defaultsToTheCurrentLogVersion` passes
- [ ] `HandLogTest.twoLogsWithTheSameContentAreEqual` passes
- [ ] `HandLogTest.rejectsAnEventSequenceWithAGap` passes
- [ ] `HandLogTest.rejectsALogThatDoesNotStartWithHandStarted` passes
- [ ] `HandLogTest.rejectsBlindsThatAreNotAscending` passes
- [ ] `HandLogTest.rejectsStacksThatAreNotTwoSeats` passes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
