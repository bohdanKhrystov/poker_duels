---
schema: 2
id: TASK-021212
title: Something drives the periodic sweeps in the server that ships
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, wiring, blocked]
depends_on: [TASK-021202, TASK-021213, TASK-020812, TASK-021215]
verify:
  - ./gradlew :poker-server:test --tests '*SweepScheduleTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

An idle room is removed, and an expired grace window ends, in a running server that nobody is
poking — not only in a test that calls the sweep itself.

## Blocked on `DEC-019`

**Do not start this ticket.** `DEC-019` is open: nothing has decided what drives
`RoomRegistry.reap()` and `expireGracePeriods()` in production, with what period, what scope, and
what happens when one pass throws. A ticker coroutine in the composition root, a Ktor plugin and an
external trigger are all plausible, and the choice decides where the period is configured.

`RoomRegistry.reap()` has had no production caller since `TASK-020612` wrote it, and
`expireGracePeriods()` will have none either. That gap is deliberate and recorded, not forgotten:
`TASK-021201` and `TASK-021202` install no sweeper on purpose, so that this decision is made once,
in an ADR, rather than guessed inside a wiring ticket.

When the ADR lands: re-read this ticket, confirm the file list below still matches where the ADR
puts the sweep, and re-split it if it does not. Widening it is not the remedy.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/SweepScheduleTest.kt` | create |

Read, do not modify: the ADR resolving `DEC-019`, `room/RoomRegistry.kt` (`reap`),
`ServerComponents.kt`.

## Scope

- Install exactly what the ADR specifies, in the place it specifies, and nothing more.
- The period, the failure behaviour of a pass that throws, and the lifetime of whatever runs the
  sweep are the ADR's to state; this ticket implements them and cites it.
- Whatever the mechanism, the observable is the same and is what the test asserts: a room that has
  been idle past its configured limit is gone from the registry **without the test calling `reap()`
  itself**.

## Out of scope

- Changing `RoomRegistry.reap()` or `expireGracePeriods()`. Both exist and are tested; this ticket
  gives them a caller.
- Metrics, logging or an admin endpoint for the sweep.
- Anything about what an absent seat *does* — `DEC-020` and `STORY-0208` own that.

## Tests

`SweepScheduleTest`, JUnit 5, package `duels.poker.server`. The configured timeouts are set small
enough that the test runs in seconds, and the test never calls `reap()` or `expireGracePeriods()`.

| Test | Proves |
| --- | --- |
| `anIdleRoomIsSweptWithoutBeingAsked` | a room created through the socket and left idle past its configured `waitingMillis` is gone from the registry, with no sweep called by the test |
| `aSweepThatThrowsDoesNotStopTheNextOne` | the behaviour the ADR specifies for a failing pass, asserted as the ADR states it |

## Acceptance criteria

- [ ] `SweepScheduleTest.anIdleRoomIsSweptWithoutBeingAsked` passes
- [ ] `SweepScheduleTest.aSweepThatThrowsDoesNotStopTheNextOne` passes
- [ ] `SweepScheduleTest.kt` names neither `reap(` nor `expireGracePeriods(`
- [ ] The KDoc of whatever installs the sweep cites the ADR that resolved `DEC-019`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.

---

## Re-scoped after `ADR-0025`, by the driver

`DEC-019` is answered, and its answer changed this ticket's shape twice over — both flagged by the
architect that wrote it, not discovered later:

**The period is configuration.** `ADR-0025` runs the ticker on a fixed delay of
`sweepPeriodMillis`, an operator's tunable, which belongs in `ServerConfig`. That would have made
this a four-file ticket, over the linter's cap. It is split out as `TASK-021213`, mirroring
`TASK-020801`/`TASK-020802`. This ticket keeps its two files and now depends on it.

**`expireGracePeriods()` does not exist yet.** `ADR-0025` has the loop drive *both* sweeps, but the
grace half is specced by `TASK-020812`, still in `STORY-0208`'s backlog. Added to `depends_on`, so
this ticket cannot start against a method that isn't there — which would otherwise have surfaced as
a compile error blamed on the ADR.

The failure this ticket prevents is worth restating: every unit test can pass while a shipped server
never reaps an idle room and never expires a grace window, so a disconnected player's duel stalls
forever in production. Nothing green catches that; only this ticket does.
