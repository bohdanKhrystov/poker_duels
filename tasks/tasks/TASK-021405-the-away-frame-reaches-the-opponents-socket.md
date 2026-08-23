---
schema: 2
id: TASK-021405
title: The AWAY frame reaches the opponent's socket, from inside the NonCancellable block
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, protocol, presence, sockets]
depends_on: [TASK-021404]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDisconnectTest'
---

## Goal

A closing socket puts exactly one `OpponentPresence(AWAY, …)` on the other player's socket, carrying
the configured window — delivered inside the `withContext(NonCancellable)` block the `disconnect`
call already sits in, because outside it the most common close path would never write the frame.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDisconnectTest.kt` | modify |

Read `SeatDelivery.kt` (`deliver` routes by seat and skips a seat with no live writer),
`room/Disconnection.kt`, and `docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §5's two
plumbing bullets. Nothing else.

## Scope

- In `DuelSocket`'s `finally` block, bind the `Disconnection` the call already returns and
  `deliver(it.outbound, it.room, deps.connections)` — **inside the same
  `withContext(NonCancellable)`**, in the same statement's scope. A `finally` reached by
  cancellation throws at the first plain suspending call, which is the reason the comment already
  there gives for the `disconnect` call itself.
- `deliver` is unchanged: it already routes by seat and already skips a seat with no live writer.
- Nothing else in `DuelSocket` changes. The exhaustive `when` already carries both new variants
  from `TASK-021402`.

## Out of scope

- Building the frame — `TASK-021404` did that, and this ticket adds no `OpponentPresence`
  constructor call anywhere.
- The `Act`-after-zero rule — `TASK-021406`, in this same file, next.
- Expiry, resume and the mark — `TASK-021407`, `TASK-021410`, `TASK-021408`.

## Tests

`DuelSocketDisconnectTest` — an existing file, on `MutableClock` and
`TEST_DISCONNECT_GRACE_MILLIS = 45_000L`, which is deliberately **not**
`RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS`, so a frame carrying that number proves the value
travelled from the registry's configured timeouts rather than from a literal.

**One merged test is invalidated and this ticket owns it.**
`theOpponentCannotActWhileTheDuelIsPaused` reads `setup.host.nextServerMessage() as
ServerMessage.Failure` immediately after `setup.guest.close()`. The host now receives the
`OpponentPresence` **first**, so that cast throws. The fix is to read frames until the `Failure`
arrives rather than assuming it is next: its two assertions — `DUEL_PAUSED`, and
`assertSame(runnerBefore, …)` — stay exactly as written, and **neither is weakened**. Nothing else
in the file changes: `closingASocketStartsThatSeatsWindow`, `theWindowIsTheConfiguredOne`,
`aSecondSocketForTheSameDeviceDoesNotPauseTheDuel` and `aSocketThatEnteredNoRoomPausesNothing` all
read the registry rather than a socket and pass unchanged.

| Test | Proves |
| --- | --- |
| `aClosingSocketTellsTheOpponentItIsAway` | after `setup.guest.close()`, the host's drained frames contain exactly one `OpponentPresence`, with `presence == AWAY` and `graceRemainingMillis == TEST_DISCONNECT_GRACE_MILLIS` |
| `theClosingSocketIsToldNothing` | the guest's own socket receives no `OpponentPresence` — the frame goes to the seat that stayed and to no one else |
| `aThirdSocketInNoRoomIsToldNothing` | a third connected client that entered no room drains no `OpponentPresence` when the guest closes |
| `theFrameIsWrittenEvenWhenTheCloseCancels` | the frame still arrives on the host's socket when the guest's connection is closed abruptly rather than by a clean `close()` — the `NonCancellable` claim, falsifiable: moving the `deliver` outside that block fails this test |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `DuelSocketDisconnectTest.theOpponentCannotActWhileTheDuelIsPaused` passes with both of its
      original assertions intact — `ProtocolError.DUEL_PAUSED`, and `assertSame` on the runner —
      the only change being that it reads past the presence frame to find the `Failure`
- [ ] The other four tests in the file are byte-identical to `develop`
- [ ] No test in the file sleeps on a real clock, and no test asserts a frame delivered to a seat
      with no live writer
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
