---
schema: 2
id: TASK-020722
title: A duel that ends says so, in the same step that ends it
type: task
status: ready
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, correctness]
depends_on: [TASK-020721]
verify:
  - ./gradlew :poker-server:test --tests '*DuelProgressTest'
  - ./gradlew :poker-server:test --tests '*RunnerDuelTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:test --tests '*RunnerChipConservationTest'
  - ./gradlew :poker-server:test --tests '*RoomDuelTest'
  - ./gradlew :poker-server:check
---

## Goal

The `DuelStep` that ends a duel carries the two `DuelFinished` frames, so a caller that writes every
`Addressed` in a step to its seat has already told both players the duel is over — without knowing a
rule of poker.

Today `advance` sets `runner.outcome` and returns no frame for it, which is why `TASK-020707` could
only record `MatchFinished` in the log and broadcast nothing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelProgress.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelProgressTest.kt` | modify |

`DuelProgressTest` is in the budget because this ticket adds cases to it. Nothing already in that
file changes: `bothSeatsSeeTheNextHandOpen` counts frames for a duel that **continues**, and
`afinishedDuelKeepsNoLiveHand` and `afinishedDuelRecordsMatchFinishedOnce` assert on the runner and
the log, not on `outbound`. If any existing assertion in it needs editing, stop and report — that
means the change reached a path it should not have.

## Scope

- In `advance`, in the branch where `matchFinishedEvent` returns non-null, append
  `finishedFrames(outcome)` to the frames the step returns, where `outcome` is the same
  `outcomeOf(match)` already stored on the returned `DuelRunner`. Use it via `checkNotNull` — a
  match that produced a `MatchFinished` event always has an outcome, and if it ever does not, that
  is a bug worth a loud failure rather than a silent missing frame.
- The `DuelFinished` frames come **last**, after every frame the loop accumulated. A client must see
  the final hand's `Events` and `Snapshot` before it is told the duel ended; the other order would
  have it render an ending it has not yet seen the last card of.
- Add nothing else to `DuelProgress.kt`. No rule of poker is decided here: which seat won, how many
  hands were played and what the final stacks are all come from `outcomeOf`, which is the engine's.

## Out of scope

- Writing the frames anywhere — `TASK-020715`.
- `RoomRegistry`, `Room` or the sink. `RoomRegistry.act` already returns the step it is given, so it
  carries the new frames without a change.
- The rematch path: an agreed rematch's opening frames are `TASK-020725`'s and a *later* story's
  problem, not this one's.

## Tests

`DuelProgressTest` — four new cases; the existing ten are untouched.

| Test | Proves |
| --- | --- |
| `afinishedDuelTellsBothSeats` | the step from a duel that just ended carries exactly one `ServerMessage.DuelFinished` for seat 0 and one for seat 1 |
| `thefinishedFrameCarriesTheEnginesOwnOutcome` | the `outcome` in both frames equals `step.runner.outcome`, which equals `outcomeOf(step.runner.match)` |
| `thefinishedFrameComesAfterTheLastSnapshot` | in each seat's frames, the index of its `DuelFinished` is greater than the index of its last `Snapshot` |
| `aduelThatContinuesTellsNobodyItFinished` | advancing into a *next hand* produces no `DuelFinished` frame at all |

The first three use the existing `oneHand` format fixture (`EndCondition.FixedHands(1)`) with
`foldedFirstHand(oneHand)`, which is how `afinishedDuelKeepsNoLiveHand` already reaches a finished
duel. The fourth uses `foldedFirstHand()` on the `flat` freezeout fixture.

## Acceptance criteria

- [ ] `DuelProgressTest.afinishedDuelTellsBothSeats` passes
- [ ] `DuelProgressTest.thefinishedFrameCarriesTheEnginesOwnOutcome` passes
- [ ] `DuelProgressTest.thefinishedFrameComesAfterTheLastSnapshot` passes
- [ ] `DuelProgressTest.aduelThatContinuesTellsNobodyItFinished` passes
- [ ] Every test method already in `DuelProgressTest` is byte-identical in the diff
- [ ] `RunnerDuelTest`, `RunnerLeakTest`, `RunnerChipConservationTest` and `RoomDuelTest` pass with
      all four files unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
