---
id: STORY-0207
title: The duel runner — the engine behind the socket
type: story
status: backlog
parent: EPIC-02
module: poker-server
labels: [server, engine-integration, duel]
depends_on: [STORY-0205, STORY-0206]
---

## Goal

An action arriving on a socket reaches `DefaultPokerEngine.handle`, and both players get back
exactly the view the engine says each is entitled to. A duel plays from the first deal to
`MatchFinished` without the server containing a single rule of poker.

## Why

This is the join: protocol, rooms, sessions and the engine become a game here. It is also where the
epic's two hardest constraints are either honoured or quietly lost — the server decides whose turn
it is (`ADR-0002`), and the *engine* decides what each player may see (`ADR-0002`, `CLAUDE.md`,
`STORY-0204`).

## Design notes

- A `DuelRunner` per room owns the live duel: the `MatchState`, the current `GameState`, and the
  `MatchLog`/`HandLog` being appended as it plays — both log types already accept an unfinished
  duel. It runs inside the room's single-writer actor from `STORY-0206`, so it needs no locking of
  its own.
- **The runner adds no rules.** Blinds, button, hand numbering and the end condition all come from
  the engine: `startNextHand`, `recordHand`, `outcomeOf`, `matchFinishedEvent`, `DuelFormat.DEFAULT`.
  Arithmetic on a blind level or a button in this module is a review finding.
- Per inbound action, in this order: the sender occupies `state.seatToAct`; the message answers the
  current hand number and action sequence (`ADR-0002` — a replayed or out-of-order frame is dropped,
  not applied); then `handle`. An `EngineResult.rejection` goes to the actor alone, never
  broadcast, and changes nothing.
- **Outbound is per recipient, always through the engine.** For each seat: `PlayerView.of(state,
  seat)` and the per-seat event filter from `STORY-0204`. The transport does no card filtering of
  its own — that is the non-negotiable this story is most able to break.
- The seat on turn also receives `legalActions(state)`, so the client draws buttons without knowing
  a rule.
- Randomness: one seed per hand, drawn server-side from an injected secure source, used to build a
  `SplitMix64Rng`, and recorded in the `HandLog` so the hand is replayable. The seed never leaves
  the server while the duel is live (`ADR-0002`).
- Format: `DuelFormat.DEFAULT` for now. `DEC-001` is still open on what a duel is; the format is
  configuration in the engine already, so answering it later is a config change, not a code change.
- When `outcomeOf` returns non-null, the runner emits the finished duel to a **`DuelResultSink`
  port declared here**, implemented against Postgres in `STORY-0210`. Declaring the port at its
  consumer is what keeps this story free of the database.
- Rematch: `STORY-0206` owns the agreement; this story starts the new duel when the room says so.

## Open decisions

**DEC-015 — how does the end of a duel reach a client?** `ServerMessage.Events` carries
`List<GameEvent>`, and `MatchFinished` is a `MatchEvent` — a separate hierarchy with its own
sequence space by `ADR-0009`. No `ServerMessage` can therefore carry the fact that a duel is over.
`TASK-020707` records `MatchFinished` in the `MatchLog` and broadcasts nothing, which is correct but
incomplete: a client is left to infer the ending from two stacks, and inferring it is a rule of
poker `ADR-0002` says the client does not hold. Registered in
[`docs/adr/README.md`](../../docs/adr/README.md); `TASK-020715` is blocked on it, along with
`DEC-010` (a socket has no way to name its room) and `DEC-013` (where the live runner lives, which
also blocks `TASK-020714`). Everything from `TASK-020701` to `TASK-020713` is unblocked: the runner
is a pure value, and every acceptance criterion above is asserted against it directly.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-020701](../tasks/TASK-020701-hand-seed-source.md) | Draw each hand's seed from an injected secure source, never from the engine Rng | ready |
| [TASK-020702](../tasks/TASK-020702-per-seat-broadcast.md) | Every outbound frame is addressed to one seat and built by the engine's projection layer | backlog |
| [TASK-020703](../tasks/TASK-020703-your-turn-frame.md) | The seat on turn gets YourTurn with the engine's legal actions, and the other seat gets nothing | backlog |
| [TASK-020704](../tasks/TASK-020704-duel-runner-value.md) | The DuelRunner value — a live hand, its match, its logs, and the invariants tying them together | backlog |
| [TASK-020705](../tasks/TASK-020705-open-a-hand-and-a-duel.md) | Open a hand from a seed, and open the duel's first one | backlog |
| [TASK-020706](../tasks/TASK-020706-guard-inbound-actions.md) | A replayed frame is dropped and a frame acting for the opponent is refused, before the engine sees either | backlog |
| [TASK-020707](../tasks/TASK-020707-hand-boundary-and-duel-end.md) | Fold a finished hand back into the duel, deal the next one, or end the duel | backlog |
| [TASK-020708](../tasks/TASK-020708-apply-an-inbound-action.md) | An inbound Act reaches the engine, and its result reaches exactly the seats entitled to it | backlog |
| [TASK-020709](../tasks/TASK-020709-duel-result-sink-port.md) | Declare the DuelResultSink port at its consumer, so this story stays free of the database | backlog |
| [TASK-020710](../tasks/TASK-020710-play-a-duel-through-the-runner.md) | A harness that plays a whole duel through the runner, seeing only what a client would see | backlog |
| [TASK-020711](../tasks/TASK-020711-chips-conserved-from-the-clients-side.md) | Chips are conserved in what the client sees, not just in what the engine knows | backlog |
| [TASK-020712](../tasks/TASK-020712-nothing-secret-leaves-the-runner.md) | No opponent's card and no hand seed ever leaves the runner, and transport filters nothing itself | backlog |
| [TASK-020713](../tasks/TASK-020713-the-log-replays-the-duel-the-server-played.md) | The MatchLog the runner wrote replays into the duel the server actually played | backlog |
| [TASK-020714](../tasks/TASK-020714-host-the-live-runner-in-a-room.md) | Give the live DuelRunner a home in the room, and publish the duel when it ends | blocked |
| [TASK-020715](../tasks/TASK-020715-an-act-frame-reaches-the-duel.md) | An Act arriving on a socket reaches the duel, and the duel's frames reach both sockets | blocked |

## Acceptance criteria

- [ ] A duel plays end to end through the runner in a test — both seats act, hands progress
      through `recordHand`/`startNextHand`, and a `MatchFinished` with a winner comes out.
- [ ] An action from the seat that is not `seatToAct` is rejected to that player only, broadcasts
      nothing, and leaves the `GameState` identical.
- [ ] An action naming a stale hand number or action sequence is dropped: the duel continues as if
      the frame had never arrived, asserted by replaying a captured frame twice.
- [ ] Every outbound payload for a seat is produced by `PlayerView.of` and the engine's event
      filter; a test asserts no card filtering exists in the transport layer.
- [ ] Chips are conserved across a whole duel as observed *only* from the runner's broadcasts —
      the property is checked from the client's side of the boundary, not from the engine's.
- [ ] The seat on turn receives `LegalActions`, and the other seat does not.
- [ ] Each hand's seed is recorded in its `HandLog`, and no `ServerMessage` sent during a live duel
      contains it.
- [ ] Replaying the finished `MatchLog` through `replayMatch` reproduces the duel the server played.

## Out of scope

- Disconnects, timers, pausing — `STORY-0208`.
- Writing the result to a database — `STORY-0210` implements the port this story declares.
- Room creation, join and rematch agreement — `STORY-0206`.
- Any change to `poker-engine`. If the runner needs something the engine does not expose, that is a
  new ticket and probably a new story, not a widened scope here.
