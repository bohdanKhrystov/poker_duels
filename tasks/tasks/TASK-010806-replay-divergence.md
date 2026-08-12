---
schema: 2
id: TASK-010806
title: Replay rejects a log that does not match the engine
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [engine, replay, log]
depends_on: [TASK-010805]
verify:
  - ./gradlew :poker-engine:test --tests '*HandReplayTest'
  - ./gradlew :poker-engine:check
---

## Goal

A log whose events do not match what the engine regenerates fails at the first event that
differs, naming its index — a silent divergence is far worse than a crash.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/HandReplay.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandReplayTest.kt` | modify |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/log/HandLog.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt`.

## Scope

- `replayHand` now compares every event it regenerates against `log.events` at the same index,
  incrementally: after `startHand`, and after each action's `handle`. The public signature does
  not change and no new type is introduced.
- Two failures, both `IllegalStateException`, both with the index in the message:
  - a differing event at index `i` →
    `"log diverges at event index $i: recorded <recorded>, replayed <replayed>"`;
  - a count mismatch, in either direction, once replay is finished or once the replay runs past
    the log's last index →
    `"log has ${log.events.size} events, the replay produced $n"`.
- The check runs before the action loop's rejection check for the events already produced, so a
  tampered opening is reported as a divergence rather than as a rejection.

## Tests

`HandReplayTest` — **five existing tests stay exactly as they are and none of their assertions
is weakened**: every log they build already carries the engine's own events, so a faithful
replay still matches, index for index. Three tests are added, each tampering with a log built
from `playRandomHand(7L)`:

| Test | Proves |
| --- | --- |
| `divergingEventIsReportedWithItsIndex` | replacing `events[1]`, a `BlindPosted`, with `copy(to = to + 1)` — density preserved — throws `IllegalStateException` whose message contains `event index 1` |
| `aTruncatedLogIsRejected` | `log.copy(events = log.events.dropLast(1))` throws `IllegalStateException` whose message contains both the log's event count and the replayed count |
| `anExtraEventAtTheEndIsRejected` | appending `HandFinished(sequence = log.events.size)` throws `IllegalStateException` whose message contains both counts |

## Out of scope

- Detecting a tampered *action* list: replay already refuses a rejected action (`TASK-010805`)
  and a surviving action change shows up as an event divergence here.
- Repairing or partially replaying a corrupt log. Report where it breaks and stop.
- Signing or hashing a log — not yet ticketed.

## Acceptance criteria

- [ ] `HandReplayTest.divergingEventIsReportedWithItsIndex` passes
- [ ] `HandReplayTest.aTruncatedLogIsRejected` passes
- [ ] `HandReplayTest.anExtraEventAtTheEndIsRejected` passes
- [ ] The five tests `TASK-010805` added to `HandReplayTest` still pass, unedited
- [ ] `replayHand` keeps the signature `(HandLog) -> HandReplay`
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
