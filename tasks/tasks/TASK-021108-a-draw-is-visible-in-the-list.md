---
schema: 2
id: TASK-021108
title: Prove a drawn duel is visible in both players' lists
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, duel, coins]
depends_on: [TASK-021107, TASK-021008]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A drawn duel appears in both players' recent duels, labelled `DREW` with a zero delta and still
naming the opponent — the property `ADR-0015` exists to guarantee.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, do not modify:
`docs/adr/ADR-0015-a-draw-writes-two-result-rows.md`,
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` (the drawn
fixture: `winner = null` with the fixed-hands format label),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt`.

## Scope

- Add exactly two tests to the existing `PostgresProfileReadsTest`, using its `finishedDuel(...)`
  builder with `winner = null` and the format copied to the fixed-hands label —
  `.copy(format = formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))))`
  — with a `why` comment: a freezeout cannot end level, so a drawn freezeout fixture would assert
  something the engine cannot produce.
- **This is the test that fails if a draw ever writes no rows.** `ADR-0015` says a draw writes two
  `duel_result` rows of `0`; the self-join in `recentDuelsOf` finds the opponent through the other
  player's row, so under the discarded design a drawn duel would be invisible to both players. Say
  that in the test class KDoc or in a comment on the first test, citing the ADR — the comment in
  `V1__initial_schema.sql` claims the opposite and is wrong.
- No existing test, helper or assertion changes.

## Out of scope

- Any change to `PostgresProfileReads.kt`, `DuelOutcomes.kt` or the DTOs. `outcomeOf(0)` already
  returns `DREW` and `TASK-021102` pinned it.
- Whether a draw pays anything: `TASK-021008` owns that on the write path.

## Tests

`PostgresProfileReadsTest`, the existing class, two tests added.

| Test | Proves |
| --- | --- |
| `aDrawnDuelAppearsInBothPlayersLists` | after recording the drawn fixture, `recentDuelsOf(alice.id, 10)` and `recentDuelsOf(bob.id, 10)` each contain exactly one entry, and both name the drawn duel's id |
| `aDrawnDuelReadsBackAsDrewWithAZeroDeltaAndAnOpponent` | in both those entries, `outcome == DREW`, `coinDelta == 0`, and `opponentPlayerId` is the *other* player's id |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aDrawnDuelAppearsInBothPlayersLists` passes
- [ ] `PostgresProfileReadsTest.aDrawnDuelReadsBackAsDrewWithAZeroDeltaAndAnOpponent` passes
- [ ] The fourteen tests already in `PostgresProfileReadsTest` pass with their assertions unchanged
- [ ] No file other than `PostgresProfileReadsTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
