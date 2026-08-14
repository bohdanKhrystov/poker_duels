---
id: STORY-0309
title: Rematch from the result screen
type: story
status: blocked
parent: EPIC-03
module: web-client
labels: [client, ui, rooms]
depends_on: [STORY-0308]
---

## Goal

After a duel ends, either player offers a rematch, sees that they have offered, sees when the
opponent has, and returns to the table when both have — with the button on the other side.

## Why

`docs/vision.md`'s success condition ends on it: *"We hit Rematch."* It is the difference between a
demo and a thing two people spend an evening on. The room layer has supported it since `EPIC-02`:
`Room.offerRematch` records one offer per seat, agrees when both have offered, and `ADR-0022` keeps
a finished room alive for a rematch window before reaping it.

## Blocked on

`DEC-023`. **The wire cannot carry a rematch.** `ClientMessage` is `Hello | CreateRoom | JoinRoom |
Act`, and no `ServerMessage` tells a seat that the opponent has offered or that a new duel has
begun. Whether the protocol gains `OfferRematch` and an offered/started frame — and whether
`PROTOCOL_VERSION` moves — or whether the client re-`JoinRoom`s a finished room, is a protocol
decision with a server half, and `EPIC-03` does not edit Kotlin. Nothing here goes `ready` until an
ADR answers it.

## Design notes

- Whatever `DEC-023`'s ADR says, and nothing beyond it.
- The offer is **per seat and idempotent**: `Room.offerRematch` refuses a repeat as
  `ALREADY_OFFERED` and leaves the recorded offers untouched, so the screen shows "you have offered"
  rather than sending again.
- The window is finite. A finished room lingers `finishedMillis` (default five minutes, `ADR-0022`)
  and is then reaped, after which its code may in principle be minted again — so the screen must
  handle the offer expiring, and an expired room must not be presented as a still-open invitation.
- The button seat alternates on a rematch; the room owns that, and the client learns it from the
  `Snapshot` that follows. Nothing here computes who is on the button.
- A rematch is the same room, not a new one. Faking it with `CreateRoom` plus a re-shared link
  changes the seating, loses the alternation, and is not a rematch.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0309` once `DEC-023` is answered.* | — |

## Acceptance criteria

- [ ] Offering a rematch shows that this seat has offered, and clicking twice sends one offer.
- [ ] When the opponent has offered and this seat has not, the screen says so.
- [ ] When both have offered, the table returns and the first `Snapshot` of the new duel is what is
      rendered — including the button on the other seat.
- [ ] An offer window that expires, or a room that has been reaped, returns the player to the lobby
      with the reason shown rather than leaving a hanging button.

## Out of scope

- Any server-side change: if `DEC-023` puts the server half in another epic, this story consumes it
  and does not write it.
- Rematch across a reconnect (offer, drop, return) beyond what the answered ADR specifies.
- A rematch invitation to anyone who was not seated.
