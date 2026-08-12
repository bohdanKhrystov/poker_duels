---
schema: 2
id: TASK-010725
title: MatchEvent, its own hierarchy, and MatchFinished
type: task
status: ready
parent: STORY-0107
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, duel, events]
depends_on: []
verify:
  - ./gradlew :poker-engine:test --tests '*MatchEventTest'
  - ./gradlew :poker-engine:check
---

## Goal

The end of a duel is a durable event in a hierarchy of its own, so a server has something to
broadcast when a duel ends and a replay can state its own ending.

## Context

[`ADR-0009`](../../docs/adr/ADR-0009-match-events-are-their-own-hierarchy.md) resolved `DEC-005`
and **is the specification for this ticket**: match-level events live in their own sealed
`MatchEvent` hierarchy with their own sequence space. `GameEvent` stays hand-scoped, its
`sequence` keeps meaning "position within a hand", and `StateProjection`'s exhaustive `when` is
not touched by this work — nothing under `duels.poker.engine.game` changes at all.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchEvent.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/MatchEventTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt`
(it declares both `DuelOutcome` and `outcomeOf`), `.../duel/MatchState.kt`.

## Scope

- One constant, one sealed interface, one data class and one function, all public, all with KDoc,
  in the existing package `duels.poker.engine.duel`:

  ```kotlin
  public const val MATCH_EVENT_SCHEMA_VERSION: Int = 1

  public sealed interface MatchEvent {
      /** Position within the match, starting at 0, dense and gap-free — never a position in a hand. */
      public val sequence: Int

      public val version: Int get() = MATCH_EVENT_SCHEMA_VERSION
  }

  public data class MatchFinished(
      override val sequence: Int,
      val outcome: DuelOutcome,
  ) : MatchEvent {
      init {
          require(sequence >= 0) { "sequence must be non-negative, was $sequence" }
      }
  }

  public fun matchFinishedEvent(match: MatchState, sequence: Int = 0): MatchFinished?
  ```

- `MatchFinished` carries `DuelOutcome` whole rather than re-listing the winner, the hand count
  and the final stacks: `DuelOutcome` already validates those three, and one source of truth for
  "who won" is what stops the event and the value from ever disagreeing.
- `version` is a property with a default rather than a constructor parameter, exactly as
  `GameEvent` does it, so it never takes part in equality.
- `matchFinishedEvent` returns `null` when `outcomeOf(match)` returns `null` — a match that is
  still running has not finished — and otherwise `MatchFinished(sequence, outcome)`. It contains
  no end-condition logic of its own; `outcomeOf` owns that.

## Out of scope

- Any change under `duels.poker.engine.game` — `GameEvent`, `StateProjection` and the contract
  suite are untouched by `ADR-0009`, deliberately.
- Serialising `MatchEvent` — `TASK-010825`.
- A log that holds these events — `TASK-010820`.
- Any further match event (`MatchStarted`, per-hand match events): not ticketed, and `EPIC-02`
  decides them when it decides what the server broadcasts.

## Tests

`MatchEventTest`, JUnit 5, package `duels.poker.engine.duel`. Build finished matches with the
`MatchState(format, handsPlayed, stacks, buttonSeat)` constructor directly — it accepts a zero
stack, which `MatchState.start` cannot produce.

| Test | Proves |
| --- | --- |
| `carriesTheSequenceAndTheOutcome` | `MatchFinished(3, outcome)` returns both unchanged |
| `defaultsToTheCurrentSchemaVersion` | `MatchFinished(0, outcome).version == MATCH_EVENT_SCHEMA_VERSION` and that constant is `1` |
| `twoEventsWithTheSameContentAreEqual` | data-class equality holds, which every later round-trip assertion depends on |
| `rejectsANegativeSequence` | `MatchFinished(-1, outcome)` throws `IllegalArgumentException` |
| `noEventForAMatchStillRunning` | a freezeout with stacks `[500, 500]` gives `matchFinishedEvent(match) == null` |
| `namesTheWinnerOfAFinishedFreezeout` | stacks `[0, 1000]` give a `MatchFinished` whose `outcome.winner == 1` and whose `outcome.finalStacks == listOf(0, 1000)` |
| `reportsADrawAsANullWinner` | a `FixedHands(1)` format with `handsPlayed = 1` and stacks `[500, 500]` gives an event whose `outcome.isDraw` is true |
| `theSequenceIsTheCallersToChoose` | `matchFinishedEvent(match, sequence = 7)!!.sequence == 7`, and the default is `0` |

## Acceptance criteria

- [ ] `MatchEventTest.carriesTheSequenceAndTheOutcome` passes
- [ ] `MatchEventTest.defaultsToTheCurrentSchemaVersion` passes
- [ ] `MatchEventTest.twoEventsWithTheSameContentAreEqual` passes
- [ ] `MatchEventTest.rejectsANegativeSequence` passes
- [ ] `MatchEventTest.noEventForAMatchStillRunning` passes
- [ ] `MatchEventTest.namesTheWinnerOfAFinishedFreezeout` passes
- [ ] `MatchEventTest.reportsADrawAsANullWinner` passes
- [ ] `MatchEventTest.theSequenceIsTheCallersToChoose` passes
- [ ] No file outside the two in the Files table is modified — in particular no file under
      `duels/poker/engine/game/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
