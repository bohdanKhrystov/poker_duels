---
id: STORY-0310
title: Reconnect — the client resumes its seat
type: story
status: done
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
  hand it lost while away. What a *present* player is shown during the pause was `DEC-018`, and
  `ADR-0028` has since answered it: `OpponentPresence` and `ActedForAbsentSeat`. **Neither exists on
  today's wire** — no Kotlin type, no row in `protocol.gen.ts` — and `EPIC-03` may not add one, so
  this story renders none of it and never will. `DEC-038` is answered by
  [`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md): the frames are `EPIC-02`'s
  `STORY-0214` and the rendering is `STORY-0313`, a story of its own, because four of the five
  presence frames reach the player who *stayed*. **This story is unchanged by that answer** — its
  thirteen tickets stand and none is added.
- Backoff is bounded and jittered. `VERSION_MISMATCH` ends the loop entirely — the server will
  refuse this client identically forever. `UNKNOWN_ROOM` answering a rejoin ends the *resume*: that
  room is gone, so the tab forgets it, but the socket keeps coming back, because a player at the
  lobby still needs one. The two clauses have different reasons and therefore different reach.
- Tests drive a fake socket on virtual time. **No test sleeps on a real clock**, the same rule
  `EPIC-02` held itself to.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-031001](../tasks/TASK-031001-the-room-code-lives-under-one-key-this-module-owns.md) | The room code lives under one storage key this module owns | ready |
| [TASK-031002](../tasks/TASK-031002-the-retry-delay-doubles-to-a-ceiling-and-spends-the-jitter.md) | The retry delay doubles to a ceiling and spends the jitter it is handed | backlog |
| [TASK-031003](../tasks/TASK-031003-a-closed-socket-is-reopened-on-virtual-time.md) | A closed socket is reopened, on virtual time, when the backoff says so | backlog |
| [TASK-031004](../tasks/TASK-031004-a-socket-the-tab-replaced-starts-no-retry-of-its-own.md) | A socket the tab has replaced starts no retry of its own | backlog |
| [TASK-031005](../tasks/TASK-031005-a-version-mismatch-ends-the-retry-loop-for-good.md) | A version mismatch ends the retry loop for good | backlog |
| [TASK-031006](../tasks/TASK-031006-the-tabs-one-connection-is-the-one-that-comes-back.md) | The tab's one connection is the one that comes back | backlog |
| [TASK-031007](../tasks/TASK-031007-boot-remembers-each-room-the-server-seats-it-in.md) | Boot remembers each room the server seats it in | backlog |
| [TASK-031008](../tasks/TASK-031008-with-no-code-in-hand-boot-rejoins-the-room-it-remembers.md) | With no code in hand, boot rejoins the room it remembers | backlog |
| [TASK-031009](../tasks/TASK-031009-a-finished-duel-is-forgotten-so-the-lobby-stays-reachable.md) | A finished duel is forgotten, so the way back to the lobby stays open | backlog |
| [TASK-031010](../tasks/TASK-031010-a-room-that-is-gone-is-forgotten-and-no-socket-resumes-into-it.md) | A room that is gone is forgotten, and no socket resumes into it | backlog |
| [TASK-031011](../tasks/TASK-031011-the-reopened-socket-says-hello-then-rejoins-once-each.md) | The reopened socket says Hello, then rejoins, once each | backlog |
| [TASK-031012](../tasks/TASK-031012-the-table-repaints-from-the-snapshot-that-followed-the-resume.md) | The table repaints from the snapshot that followed the resume | backlog |
| [TASK-031013](../tasks/TASK-031013-no-client-test-sleeps-on-a-real-clock.md) | No client test sleeps on a real clock | backlog |

The chain is deliberately linear: `031003`–`031005` share `reconnecting.ts`, `031007`–`031010`
share `boot.ts`, and `031011`–`031012` share one test file, so no two are startable at once.

## Acceptance criteria

- [ ] After a simulated close, the client sends `Hello` then `JoinRoom` with the stored code, in
      that order, exactly once per reconnect.
- [ ] The table renders the `Snapshot` that follows the resume, not the state held before the drop —
      including a hand that ended while the socket was down.
- [ ] Retry delays back off and are asserted on virtual time, with no real sleep in any test.
- [ ] `UNKNOWN_ROOM` on resume, and `VERSION_MISMATCH` at any point, each end the retry loop.
- [ ] A reload with a stored device id and room code lands back at the table with no player action.

## Out of scope

- Anything shown to the player whose opponent is away. `ADR-0028` answered `DEC-018`, but nothing
  on today's wire carries `OpponentPresence` or `ActedForAbsentSeat` and `EPIC-03` may not add them.
  `ADR-0045` answers `DEC-038` and puts the rendering in `STORY-0313`, on `EPIC-02`'s `STORY-0214`.
- Surviving a **server** restart mid-duel: `ADR-0011` says in-flight duel state is not durable, so
  there is nothing to resume into.
- Offline queueing of actions. An action taken while disconnected is not an action.
- Rematch across a reconnect — `STORY-0309`.
