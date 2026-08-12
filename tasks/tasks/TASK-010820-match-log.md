---
schema: 2
id: TASK-010820
title: MatchLog, the record of a whole duel
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, replay, log, duel]
depends_on: [TASK-010725]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchLogTest'
  - ./gradlew :poker-engine:check
---

## Goal

A whole duel is one value: its format, its hands in order, and its own match-level events.

## Context

[`ADR-0009`](../../docs/adr/ADR-0009-match-events-are-their-own-hierarchy.md): *"The two logs are
separate. A match log references its hands; it does not contain their events."* So `MatchLog`
holds `HandLog`s — each of which owns its own `GameEvent`s — and a separate list of `MatchEvent`s
numbered in the match's own sequence space. No `GameEvent` appears at the match level, ever.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLog.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/MatchLogTest.kt` | create |

Read, do not modify: `.../log/HandLog.kt`, `.../duel/MatchEvent.kt`, `.../duel/DuelFormat.kt`.

## Scope

- One constant and one data class, both public, both with KDoc, in `duels.poker.engine.log`:

  ```kotlin
  public const val MATCH_LOG_VERSION: Int = 1

  public data class MatchLog(
      val format: DuelFormat,
      val buttonSeat: Int,
      val hands: List<HandLog>,
      val events: List<MatchEvent>,
      val version: Int = MATCH_LOG_VERSION,
  )
  ```

  `buttonSeat` is the button for hand 1. The format is stored rather than referenced because a log
  that cannot state its own blind schedule cannot be replayed.

- `init` validation, each with a message naming the offending value:

  | Rule |
  | --- |
  | `version >= 1` |
  | `buttonSeat` is 0 or 1 |
  | `hands[i].handNumber == i + 1`, so the hands are dense, 1-based and in order |
  | `hands[i].buttonSeat == (buttonSeat + i) % 2`, so the button alternates exactly as `recordHand` makes it |
  | `events[i].sequence == i`, so the match's own sequence space is dense and gap-free |
  | at most one `MatchFinished` — a duel ends once |

- A log of an unfinished duel is legal, exactly as `HandLog` allows an unfinished hand: `hands` may
  be empty and no `MatchFinished` is required. A server appends as the duel plays.

## Out of scope

- Producing one from a played duel — `TASK-010821`.
- Replaying one — `TASK-010822`.
- Serialising one — `TASK-010826`.
- Any per-hand or per-street match event: `MatchFinished` is the only `MatchEvent` there is.

## Tests

`MatchLogTest`, JUnit 5, package `duels.poker.engine.log`. Build hand logs with a private helper
in this file — `handLog(handNumber, buttonSeat)` returning a `HandLog` whose single event is a
matching `HandStarted(0, handNumber, buttonSeat, 50, 100, listOf(1000, 1000))` — so the tests stay
about `MatchLog`'s own rules.

| Test | Proves |
| --- | --- |
| `carriesTheFormatTheButtonAndItsHands` | a constructed log returns what it was given |
| `defaultsToTheCurrentLogVersion` | `MatchLog(...).version == MATCH_LOG_VERSION` and that constant is `1` |
| `acceptsTheLogOfADuelStillRunning` | empty `hands`, empty `events`, no exception |
| `rejectsHandNumbersWithAGap` | hands numbered 1 and 3 throw `IllegalArgumentException` |
| `rejectsAButtonThatDoesNotAlternate` | two hands both on button 0 throw `IllegalArgumentException` |
| `rejectsAMatchEventSequenceWithAGap` | events with sequences 0 and 2 throw `IllegalArgumentException` |
| `rejectsTwoMatchFinishedEvents` | two `MatchFinished` events, at sequences 0 and 1, throw `IllegalArgumentException` |

> There is no "a `MatchFinished` must be last" rule: `MatchFinished` is the only `MatchEvent`
> subtype there is, so nothing could follow it and no test could show the rule working. Rules that
> cannot fail do not go in `init`.

## Acceptance criteria

- [ ] `MatchLogTest.carriesTheFormatTheButtonAndItsHands` passes
- [ ] `MatchLogTest.defaultsToTheCurrentLogVersion` passes
- [ ] `MatchLogTest.acceptsTheLogOfADuelStillRunning` passes
- [ ] `MatchLogTest.rejectsHandNumbersWithAGap` passes
- [ ] `MatchLogTest.rejectsAButtonThatDoesNotAlternate` passes
- [ ] `MatchLogTest.rejectsAMatchEventSequenceWithAGap` passes
- [ ] `MatchLogTest.rejectsTwoMatchFinishedEvents` passes
- [ ] `MatchLog` declares no `GameEvent` field, per `ADR-0009` — its hand events live inside its
      `HandLog`s
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
