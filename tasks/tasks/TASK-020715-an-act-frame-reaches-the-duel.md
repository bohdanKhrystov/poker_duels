---
schema: 2
id: TASK-020715
title: An Act arriving on a socket reaches the duel, and the duel's frames reach both sockets
type: task
status: ready
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, duel, websocket, protocol]
depends_on: [TASK-020731]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDuelTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:test --tests '*RunnerLeakTest'
  - ./gradlew :poker-server:check
---

## Goal

An `Act` frame on a socket whose session is seated in a live room reaches `duels.poker.server.duel.act`
through that room's mutex, and every `Addressed` the duel produces — including the `DuelFinished` that
ends it — is written to that seat's `ConnectionWriter` and nobody else's.

This is the last ticket of `STORY-0207`: after it, the story's own goal holds. An action arriving on
a socket reaches `DefaultPokerEngine.handle`, and both players get back exactly the view the engine
says each is entitled to.

## Both blocking decisions are answered

`DEC-015` and `DEC-010` are both resolved by
[`ADR-0017`](../../docs/adr/ADR-0017-the-server-says-when-a-duel-ends.md): the server states the
ending with a `ServerMessage.DuelFinished` projection, and later stories extend the existing sealed
hierarchies rather than introducing a parallel protocol. Everything those answers implied — the
version bump, the two new server messages, the two new client messages, the connection directory,
the seat-delivery function and the room association — is `TASK-020718` through `TASK-020731`, all of
which merge before this one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDuelTest.kt` | create |

`DuelSocketFrameLoopTest` is **not** in the budget and must not be edited. Its
`anActOutsideADuelIsAnsweredWithNotInDuel` case builds a socket that has joined no room, and that
socket still receives `Failure(NOT_IN_DUEL)` after this ticket — the case is already narrow enough,
and this ticket must leave it standing exactly as it is. If it goes red, the `null`-room path has
been broken, not outgrown.

## Scope

- Replace `replyTo`'s `Act` branch. Given the `RoomCode` the connection remembers (`TASK-020731`):
  - no remembered code → `Failure(NOT_IN_DUEL)`;
  - `deps.rooms.get(code)` is `null` → `Failure(NOT_IN_DUEL)`;
  - `room.seatOf(session.player.id)` is `null` → `Failure(NOT_IN_DUEL)`;
  - otherwise `deps.rooms.act(code) { it.act(seat, message, deps.rooms.handSeeds) }`, then
    `deliver(step.outbound, room, deps.connections)`. A `null` step — the room is not `PLAYING`, or
    carries no runner — sends nothing and answers nothing: the room has no move to make and the
    client has already been told everything true about it.
- **The seat comes from the session, never from the frame.** `Act` carries `handNumber`,
  `actionSequence` and a `PlayerAction` whose `seat` field the client filled in; none of them decide
  who is acting. `seatOf(session.player.id)` does, and `TASK-020706`'s guard is handed that seat and
  nothing else. A frame claiming another seat is refused by the guard as `NOT_YOUR_TURN`, which is
  already `TASK-020708`'s behaviour and is not re-decided here.
- Use `deps.rooms.handSeeds` (`TASK-020724`), never a seed source of the socket's own: every hand of
  one duel must draw from the source that opened it.
- Every outbound frame goes through `deliver` (`TASK-020730`). `DuelSocket.kt` writes no frame to a
  socket directly, encodes nothing itself for a duel frame, and constructs no `ServerMessage` that
  carries state.
- No card filtering, no `PlayerView`, no `holeCards` anywhere in `DuelSocket.kt`.

## Out of scope

- Reconnect, resync and the grace period — `ADR-0013`, `STORY-0208`.
- Rematch over the wire, and a rematch's opening frames — see `TASK-020725`.
- The full end-to-end duel against a real database — `STORY-0212`.
- Any change to the engine, the runner, `Room` or `RoomRegistry`.

## Tests

`DuelSocketDuelTest` — a new file. Two real clients through `testApplication`, each handshaking and
entering the room the test pre-created on the registry it handed to `testDeps(rooms = …)`, so the
test chooses both the `DuelFormat` and the `HandSeedSource`. Use `EndCondition.FixedHands(1)` for
the ending case: `DuelFormat.DEFAULT` is a freezeout and would take a whole duel to reach.

| Test | Proves |
| --- | --- |
| `theSeatOnTurnIsTheOnlyOneToldItIsItsTurn` | after the guest sits, exactly one client has received a `YourTurn`, and it is the seat the server put on turn |
| `anActFromTheSeatOnTurnMovesTheDuelAndBothSeatsSeeIt` | a legal `Act` from that client produces a fresh `Snapshot` on **both** sockets |
| `anActFromTheSeatNotOnTurnIsAnsweredToThatSocketAlone` | the other client's `Act` returns a `Rejected` to it and the opponent receives nothing within a timeout |
| `anActFromASocketInNoRoomIsStillRefused` | a third, handshaken client that entered no room gets `Failure(NOT_IN_DUEL)` |
| `theSeatOnTheFrameIsIgnored` | an `Act` whose `PlayerAction.seat` names the *opponent's* seat is judged by the sender's own seat — it does not act for the opponent |
| `bothSocketsAreToldWhenTheDuelEnds` | playing the `FixedHands(1)` duel out, each client receives exactly one `ServerMessage.DuelFinished`, carrying the same `DuelOutcome` |
| `neitherSocketEverSawTheOthersHoleCards` | across every frame either client received, no `Snapshot` showed the opponent's `holeCards` before a `HandRevealed` for that seat, and no `Events` frame carried the opponent's `HoleCardsDealt` |

The last is the epic's non-negotiable asserted where it can be observed honestly — from the frames a
client actually received, not from the runner's state.

## Acceptance criteria

- [ ] All seven `DuelSocketDuelTest` cases named above pass
- [ ] `DuelSocketFrameLoopTest` passes with the file **unchanged**, including
      `anActOutsideADuelIsAnsweredWithNotInDuel`
- [ ] `RunnerLeakTest.noServerSourceFileTouchesHoleCards` passes
- [ ] `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` passes with `RunnerLeakTest.kt`
      unchanged
- [ ] `DuelSocket.kt` contains no `PlayerView`, no `holeCards`, and no construction of
      `ServerMessage.Snapshot`, `ServerMessage.Events` or `ServerMessage.DuelFinished`
- [ ] `DuelSocket.kt` names no `HandSeedSource` other than `deps.rooms.handSeeds`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
