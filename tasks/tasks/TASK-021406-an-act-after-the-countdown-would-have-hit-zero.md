---
schema: 2
id: TASK-021406
title: An Act sent after the client's countdown would have reached zero is still refused
type: task
status: done
parent: STORY-0214
module: poker-server
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [server, presence, authority]
depends_on: [TASK-021405]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'
---

## Goal

The rule that keeps the countdown from becoming client-side authority becomes executable: a
`graceRemainingMillis` that has elapsed re-enables nothing. An `Act` sent after the window's
duration has passed, but before the sweep has expired it, is still `DUEL_PAUSED` and still moves
nothing.

`ADR-0028` §10 calls this the single most important test in the set. It is a **test-only** ticket:
no production file is opened.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify |

Read `docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §3 and
`room/Room.kt`'s `act` KDoc — the `isPaused` refusal it already implements. Nothing else.

## Scope

- One test added to `DuelSocketDisconnectTest`, alongside `theOpponentCannotActWhileTheDuelIsPaused`,
  which proves the same refusal *inside* the window. This one proves it **after**.
- The shape: start a duel; close the guest; read the host's `OpponentPresence(AWAY, r)`; advance the
  `MutableClock` past `r`; **do not call `expireGracePeriods`**; send the host's `Act`; assert
  `Failure(DUEL_PAUSED)` and that the runner is the same instance as before.
- `assertSame` on the runner, not `assertEquals` — "moves nothing" means the room was never
  rewritten, and an equal-but-rebuilt runner would satisfy the weaker one.
- Virtual time only. `MutableClock.advance` is the whole mechanism; no `Thread.sleep`, no
  `delay` used as a clock, and no dependence on the real sweep having or not having run.

## Out of scope

- **Any production change.** If this test fails, that is a defect and a new ticket, not an edit
  from this branch: `Room.act` already refuses while `isPaused`, and this ticket only proves it.
- Expiry actually running — `TASK-021407` owns what happens when the sweep does land.
- The client's rendering of a countdown reaching zero — `STORY-0313`, `ADR-0045` §6.

## Tests

`DuelSocketDisconnectTest` — an existing file. One test is added and **no existing assertion is
touched**.

| Test | Proves |
| --- | --- |
| `anActAfterTheCountdownWouldHaveEndedIsStillRefused` | with the clock advanced past the `graceRemainingMillis` the host was sent, and the sweep never run, the host's `Act` answers `Failure(DUEL_PAUSED)` and `assertSame(runnerBefore, rooms.get(code)!!.runner)` holds |
| `theRoomIsStillPausedAfterTheWindowHasElapsed` | in the same state, `rooms.get(code)!!.isPaused` is still `true` and `absentSeats` is still empty — the window's *duration* passing is not the window *expiring*, and only the sweep moves a seat |

## Acceptance criteria

- [ ] `DuelSocketDisconnectTest.anActAfterTheCountdownWouldHaveEndedIsStillRefused` passes
- [ ] `DuelSocketDisconnectTest.theRoomIsStillPausedAfterTheWindowHasElapsed` passes
- [ ] Neither test calls `expireGracePeriods`, directly or through a ticker
- [ ] No production file is in the diff
- [ ] Every other test in `DuelSocketDisconnectTest` is byte-identical to its state after
      `TASK-021405`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
