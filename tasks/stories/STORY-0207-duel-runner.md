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

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0207`.* | — |

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
