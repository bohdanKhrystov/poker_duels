---
schema: 2
id: TASK-020715
title: An Act arriving on a socket reaches the duel, and the duel's frames reach both sockets
type: task
status: blocked
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, duel, websocket, protocol, blocked]
depends_on: [TASK-020714]
verify:
  - ./gradlew :poker-server:test --tests '*DuelSocketDuelTest'
  - ./gradlew :poker-server:test --tests '*DuelSocketFrameLoopTest'
  - ./gradlew :poker-server:check
---

## Goal

An `Act` frame on a live socket reaches `act`, each `Addressed` the duel produces is written to that
seat's `ConnectionWriter` and nobody else's, and the end of a duel is something a client is actually
told about.

## Blocked on DEC-015 and DEC-010

**Do not start this ticket.** Two questions must be answered first; both are registered in
[`docs/adr/README.md`](../../docs/adr/README.md).

> **DEC-015** — how does the end of a duel reach a client? `ServerMessage.Events` carries
> `List<GameEvent>`, and `MatchFinished` is a `MatchEvent` — a separate hierarchy with its own
> sequence space by `ADR-0009`. So today no `ServerMessage` can carry the fact that the duel is
> over.

`TASK-020707` therefore records `MatchFinished` in the `MatchLog` and broadcasts nothing. The
candidates are not equivalent:

- **A `ServerMessage.MatchEvents(List<MatchEvent>)`**, mirroring `Events`. Symmetric, and future
  match-level events cost nothing — but it publishes a second sequence space to every client, and
  `ADR-0009` kept the two hierarchies apart for the server's benefit, not the client's.
- **A `ServerMessage.DuelFinished(outcome: DuelOutcome)`.** One message, one meaning, nothing to
  explain; but a second match-level event later needs a second message.
- **Nothing on the wire**: the client infers the ending from the final `Snapshot`. Cheapest, and
  wrong — inferring "the duel is over" from two stacks is a rule of poker, and `ADR-0002` says the
  client holds none.

> **DEC-010** — do the room and lobby messages belong to `STORY-0202`'s protocol, or does
> `STORY-0207` extend the two sealed hierarchies once `RoomRegistry` exists?

`ClientMessage` is `Hello` and `Act` only, so a socket has no way to say which room it is in, and
this ticket cannot route an `Act` without an answer.

## Files

Provisional — rewritten by `/plan-story STORY-0207` once both decisions are recorded.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketDuelTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/DuelSocketFrameLoopTest.kt` | modify |

`DuelSocketFrameLoopTest` is in the budget, not a bystander: its
`anActFrameIsAnsweredWithNotInDuel` case pins today's behaviour, that an `Act` on a socket in no
duel gets `Failure(NOT_IN_DUEL)`. A socket that *is* in a duel must stop answering that way, so this
ticket owns moving that assertion — narrowing it to a socket genuinely in no duel — and changes
nothing else in the file. No assertion in it is deleted or weakened.

## Scope

- Written when `DEC-015` and `DEC-010` are answered. Whatever the answers:
  - the seat an `Act` is attributed to comes from the **session**, never from the frame;
    `TASK-020706`'s guard is given that seat and nothing else;
  - each `Addressed` is encoded once and sent to that seat's `ConnectionWriter` only — one writer per
    connection stays the rule from `TASK-020505`, and no frame is written to a socket directly;
  - no card filtering, no `PlayerView`, no `holeCards` appears in `DuelSocket.kt`;
    `TASK-020712`'s source scan must keep passing.

## Out of scope

- Where the runner is stored and how callers are serialised — `TASK-020714`, `DEC-013`.
- Reconnect, resync and the grace period — `ADR-0013`, `STORY-0208`.
- The full socket-to-socket duel — `STORY-0212`, which builds on this.

## Tests

To be named once both decisions are answered. `DuelSocketDuelTest` will assert, at minimum: two
connected clients receive their own `Snapshot`s and only the seat on turn a `YourTurn`; an `Act`
from the seat not on turn is answered to that socket alone; and whatever `DEC-015` chooses for the
end of the duel arrives on both sockets exactly once.

## Acceptance criteria

- [ ] `DEC-015` and `DEC-010` are answered and recorded before any code is written
- [ ] The tests named in this ticket after those answers all pass
- [ ] `RunnerLeakTest.onlyTheBroadcastFileBuildsAStateCarryingFrame` and
      `RunnerLeakTest.noServerSourceFileTouchesHoleCards` still pass
- [ ] `DuelSocketFrameLoopTest` keeps every one of its other assertions, and the `Act` case is
      narrowed rather than deleted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
