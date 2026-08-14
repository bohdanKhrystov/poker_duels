---
id: STORY-0304
title: The store — state is the last frame the server sent
type: story
status: backlog
parent: EPIC-03
module: web-client
labels: [client, state, protocol]
depends_on: [STORY-0303]
---

## Goal

A pure, framework-free store that folds `ServerMessage`s into everything the screens render: the
room (`RoomJoined`, `Failure`), the table (`Snapshot`), the pending turn (`YourTurn`), the narration
log (`Events`), the refusals (`Rejected`) and the outcome (`DuelFinished`).

## Why

This is where the epic honours `ADR-0002` or quietly breaks it. Deciding it once, in a tested
reducer with no React in it, before any screen exists, is the only cheap moment — after three
screens the rule lives in three places and one of them is wrong.

## Design notes

- **The last `Snapshot` is the truth.** The server emits one to each seat after every transition;
  `poker-server/src/main/kotlin/duels/poker/server/duel/Addressed.kt` calls it *"the authoritative
  last word on state"* and always sends it. `Events` are narration: they may drive animation and a
  hand log, and they may never mutate pot, stacks, board, street or whose turn it is. A reducer that
  rebuilds the table from events has re-implemented the rules in TypeScript, and it will be right
  until the first hand where it is not.
- **The store computes nothing a message already carries.** No legality, no pot arithmetic, no
  min-raise, no hand rank, no winner. `PlayerView` carries `pot`, `betToMatch`, `minRaiseTo`,
  `seatToAct`; `LegalActions` carries `allowed`, `callTo`, `minBetTo`, `minRaiseTo`, `allInTo`;
  `DuelOutcome` carries `winner`. They exist so the client can be dumb.
- `mySeat` comes from `RoomJoined.seat`, and `PlayerView.viewerSeat` confirms it. It is never
  inferred from which seat has visible hole cards — that inference is a rule, and it is also wrong
  at showdown.
- A turn is identified by (`handNumber`, `actionSequence`) taken verbatim from `YourTurn` and echoed
  verbatim in `Act`. The store never invents, increments or "repairs" either: a stale echo is how
  the server rejects a late click, and that only works if the client does not fix it first.
- The pending turn is cleared by the next `Snapshot`, a `Rejected`, or a `DuelFinished`. A stale
  pending turn is how a client offers a button for a hand that is over.
- An opponent's `SeatView.holeCards` is an empty array until the server reveals it. The store keeps
  it empty and stores nothing it was not sent — there is no "face-down card" placeholder in state,
  because a placeholder is a rendering decision (`STORY-0306`) and state is the wrong place to
  invent cards.
- Plain TypeScript, testable without rendering anything. How React subscribes to it is a toolchain
  matter from `DEC-022`; the reducer itself must not know.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0304`.* | — |

## Acceptance criteria

- [ ] Applying a `Snapshot` replaces the view wholesale; applying an `Events` frame afterwards
      changes no field of it.
- [ ] `YourTurn` then `Snapshot` leaves no pending turn, and `YourTurn` then `Rejected` leaves the
      view untouched while surfacing the rejection as sent.
- [ ] `RoomJoined` sets the seat, and a `Snapshot` whose `viewerSeat` disagrees is a test case with
      a defined, asserted outcome rather than undefined behaviour.
- [ ] An `Events` frame carrying `HandRevealed` does not populate any seat's hole cards; only the
      snapshot that follows does.
- [ ] The store exports no function taking cards, stacks or amounts and returning a legality, a
      rank, a pot or a winner — asserted structurally, in a test that names the property.

## Out of scope

- Rendering anything — `STORY-0305` onward.
- Reconnect and what survives a reload — `STORY-0310`.
- Rematch — `STORY-0309`.
- The HTTP profile data, which is not a frame and does not belong in this store — `STORY-0311`.
