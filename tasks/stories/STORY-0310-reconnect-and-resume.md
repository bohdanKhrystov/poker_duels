---
id: STORY-0310
title: Reconnect — the client resumes its seat
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, websocket, resilience]
depends_on: [STORY-0306]
---

## Goal

A dropped socket or a reloaded tab returns to the same seat in the same duel without the player
doing anything, and the table repaints from the state the server sends on arrival.

## Why

`EPIC-02` built the whole grace-period machinery (`ADR-0013`, `ADR-0018`, `ADR-0023`) so that a
flaky connection does not end a duel. None of it is reachable if the browser gives up on the first
close. A phone that locks its screen mid-hand is the normal case, not the edge case.

## Design notes

- **The recipe is fixed by the server**, not invented here: reopen the socket, send `Hello` with the
  stored device id, then `JoinRoom` with the stored room code. `RoomRegistry.resume` answers for a
  player already seated in a room that carries a duel and re-delivers that seat's frames, so the
  table repaints from the `Snapshot` that follows. A player who was seated but whose room has since
  been reaped falls through to an ordinary join and is refused — which is a lobby return, not a
  retry.
- The client therefore persists the **room code** as well as the device id, and only those two. A
  persisted `PlayerView` would be a stale game fact held by a client.
- `ADR-0018`: a second socket adopts the seat and the first is closed. A client that finds its
  socket closed just after opening another must not fight for it — that is itself, one tab earlier.
  Reconnect logic must be idempotent per tab and must not loop against its own adoption.
- `ADR-0013`/`ADR-0023`: while a seat is absent the server may check or fold it. The returning
  client is told nothing special about that today, so it renders the state it finds — including a
  hand it lost while away. What a *present* player is shown during the pause is `DEC-018`, and this
  story renders none of it.
- Backoff is bounded and jittered. `VERSION_MISMATCH` ends the loop (the server will refuse
  identically forever), and so does `UNKNOWN_ROOM` on resume — that room is gone.
- Tests drive a fake socket on virtual time. **No test sleeps on a real clock**, the same rule
  `EPIC-02` held itself to.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0310`.* | — |

## Acceptance criteria

- [ ] After a simulated close, the client sends `Hello` then `JoinRoom` with the stored code, in
      that order, exactly once per reconnect.
- [ ] The table renders the `Snapshot` that follows the resume, not the state held before the drop —
      including a hand that ended while the socket was down.
- [ ] Retry delays back off and are asserted on virtual time, with no real sleep in any test.
- [ ] `UNKNOWN_ROOM` on resume, and `VERSION_MISMATCH` at any point, each end the retry loop.
- [ ] A reload with a stored device id and room code lands back at the table with no player action.

## Out of scope

- Anything shown to the player whose opponent is away — `DEC-018`, unanswered.
- Surviving a **server** restart mid-duel: `ADR-0011` says in-flight duel state is not durable, so
  there is nothing to resume into.
- Offline queueing of actions. An action taken while disconnected is not an action.
- Rematch across a reconnect — `STORY-0309`.
