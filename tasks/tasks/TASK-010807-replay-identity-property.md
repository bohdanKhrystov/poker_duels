---
schema: 2
id: TASK-010807
title: Record and replay is an identity over two hundred random hands
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, replay, test, determinism]
depends_on: [TASK-010806]
verify:
  - ./gradlew :poker-engine:test --tests '*HandLogReplayPropertyTest'
  - ./gradlew :poker-engine:check
---

## Goal

`(seed, actions)` reproduces a hand exactly — not in the one hand somebody wrote down, but in
two hundred nobody designed, card for card and event for event.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandLogReplayPropertyTest.kt` | create |

Read, do not modify: `poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/log/HandLog.kt`,
`poker-engine/src/main/kotlin/duels/poker/engine/log/HandReplay.kt`.

## Scope

- One new test file. **This ticket adds no production code**; anything it finds becomes a new
  ticket.
- A private helper in the file turns a `PlayedHand` into a `HandLog`:

  ```kotlin
  private fun logOf(seed: Long, played: PlayedHand) = HandLog(
      seed = seed,
      handNumber = played.opening.handNumber,
      buttonSeat = played.opening.buttonSeat,
      stacks = played.opening.seats.map { it.stack },
      smallBlind = played.opening.smallBlind,
      bigBlind = played.opening.bigBlind,
      actions = played.actions,
      events = played.events,
  )
  ```

  `HandReplayTest` has a helper of its own — do not modify that file, and do not extract a shared
  one here.
- Equality is full `GameState` equality, `deck` and `rng` included: replay re-runs `startHand`
  from the same seed, so nothing is excused from matching. An assertion that compares only some
  fields fails this ticket.
- `@Timeout(60)` on the two-hundred-hand test, so a stall fails rather than hangs CI.

## Out of scope

- Match-level replay — `TASK-010811`.
- Serialised round-tripping — `TASK-010810`, blocked on `DEC-006`.
- Any change to `RandomHandPlayer.kt` or to the invariants it already checks (`TASK-010521`).

## Tests

`HandLogReplayPropertyTest`, JUnit 5, package `duels.poker.engine.log`.

| Test | Proves |
| --- | --- |
| `recordAndReplayIsAnIdentityOverTwoHundredRandomHands` | for seeds `1..200`, `replayHand(logOf(seed, playRandomHand(seed)))` has `events == played.events` and `finalState == played.finalState`, with the failing seed in the assertion message |
| `everyReplayedHandEndsWhereTheRecordedHandEnded` | for seeds `1..200`, `replayHand(...).finalState.isHandOver` is true |
| `theSameLogReplaysToTheSameValueTwice` | for seeds `1..20`, `replayHand(log) == replayHand(log)` |
| `aLogMissingItsLastActionDoesNotReplayAsAnIdentity` | for the first seed in `1..200` whose hand has at least two actions, `log.copy(actions = log.actions.dropLast(1))` throws `IllegalStateException` — the negative control proving these assertions can fail |

## Acceptance criteria

- [ ] `HandLogReplayPropertyTest.recordAndReplayIsAnIdentityOverTwoHundredRandomHands` passes
- [ ] `HandLogReplayPropertyTest.everyReplayedHandEndsWhereTheRecordedHandEnded` passes
- [ ] `HandLogReplayPropertyTest.theSameLogReplaysToTheSameValueTwice` passes
- [ ] `HandLogReplayPropertyTest.aLogMissingItsLastActionDoesNotReplayAsAnIdentity` passes
- [ ] The file references neither `kotlin.random.Random` nor any clock
- [ ] No file under `src/main` is modified, and no existing test file is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
