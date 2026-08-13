---
schema: 2
id: TASK-020721
title: The projection layer builds the finished-duel frames, and only it may
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, duel, projection]
depends_on: [TASK-020720]
verify:
  - ./gradlew :poker-server:test --tests '*DuelFinishedFramesTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:check
---

## Goal

`duel/Addressed.kt` has a `finishedFrames(outcome)` that returns one `Addressed` per seat carrying
`ServerMessage.DuelFinished`, and `RunnerLeakTest` forbids any other server source file from
constructing that message — the same rule `Snapshot` and `Events` already live under.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/Addressed.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelFinishedFramesTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerLeakTest.kt` | modify |

`RunnerLeakTest` is in the budget, not a bystander: `onlyTheBroadcastFileBuildsAStateCarryingFrame`
walks a fixed list of needles, and a message carrying both players' final stacks belongs on that
list. This ticket adds one needle and changes nothing else in the file — no other needle, no other
test, no assertion weakened.

## Scope

- Add to `duel/Addressed.kt`:

  ```kotlin
  public fun finishedFrames(outcome: DuelOutcome): List<Addressed>
  ```

  returning exactly two frames, seat 0 then seat 1, each `Addressed(seat, ServerMessage.DuelFinished(outcome))`.
- KDoc why both seats get the *same* outcome and no filtering happens: a `DuelOutcome` names the
  winning seat, the hand count and the final stacks — all three are facts both players are entitled
  to, and the final stacks are already in the last `Snapshot` each seat received. There is nothing
  here to redact, and this file is where that judgement is allowed to be made (`ADR-0017`: the
  finished-duel frame is a projection, filtered like everything else through `Addressed.kt`).
- In `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame`, add `"ServerMessage.DuelFinished("`
  to `needles`. The existing non-vacuity assertion then also covers it: `Addressed.kt` contains the
  needle, so `containing.isNotEmpty()` holds.

## Out of scope

- Calling `finishedFrames` — `TASK-020722` calls it from `advance`.
- Any change to `broadcast`, `framesFor` or the `Addressed` type itself.
- Any change to `RunnerLeakTest` beyond the single needle.

## Tests

`DuelFinishedFramesTest` — a new file, testing `finishedFrames` directly against a hand-built
`DuelOutcome`. No duel is played here.

| Test | Proves |
| --- | --- |
| `bothSeatsAreToldTheDuelFinished` | `finishedFrames(outcome)` returns two frames, addressed to seats 0 and 1 exactly once each |
| `bothSeatsAreToldTheSameOutcome` | both frames carry a `ServerMessage.DuelFinished` whose `outcome` equals the one passed in |
| `adrawIsCarriedAsADraw` | for `DuelOutcome(winner = null, …)` both frames carry `winner == null`, so a level `FixedHands` duel is not reported as a win for anybody |
| `thefinishedFrameSurvivesTheCodec` | `ProtocolCodec.encode` then `protocolJson.decodeFromString<ServerMessage>` round-trips a `DuelFinished` frame back to an equal value |

## Acceptance criteria

- [ ] `DuelFinishedFramesTest.bothSeatsAreToldTheDuelFinished` passes
- [ ] `DuelFinishedFramesTest.bothSeatsAreToldTheSameOutcome` passes
- [ ] `DuelFinishedFramesTest.adrawIsCarriedAsADraw` passes
- [ ] `DuelFinishedFramesTest.thefinishedFrameSurvivesTheCodec` passes
- [ ] `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` passes with
      `"ServerMessage.DuelFinished("` among its needles
- [ ] `RunnerLeakTest.noServerSourceFileTouchesHoleCards` passes
- [ ] Every other test method in `RunnerLeakTest` is byte-identical in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
