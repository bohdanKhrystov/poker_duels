---
id: STORY-0309
title: Rematch from the result screen
type: story
status: ready
parent: EPIC-03
module: web-client
labels: [client, ui, rooms]
depends_on: [STORY-0308, STORY-0213]
---

## Goal

After a duel ends, either player offers a rematch, sees that they have offered, sees when the
opponent has, and returns to the table when both have — with the button on the other side.

## Why

`docs/vision.md`'s success condition ends on it: *"We hit Rematch."* It is the difference between a
demo and a thing two people spend an evening on. The room layer has supported it since `EPIC-02`:
`Room.offerRematch` records one offer per seat, agrees when both have offered, and `ADR-0022` keeps
a finished room alive for a rematch window before reaping it.

## The wire it renders

`DEC-023` is **answered** by
[`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md): one client
intent in, one room fact out. The server half is
[`STORY-0213`](STORY-0213-the-wire-carries-a-rematch.md) in `EPIC-02` — **this story consumes it and
writes no Kotlin**, which is `EPIC-03`'s standing rule. Nothing here is startable before
`STORY-0213` merges, because `protocol.gen.ts` does not yet name the two messages.

What the client gets:

- **Sends `OfferRematch`** — a message with no fields. The client names no room and no seat; the
  socket already knows both.
- **Receives `RematchOffered(seat)`**, on both sockets, meaning *an offer from that seat stands*.
  Compare it with `mySeat` from `RoomJoined`: this seat, or the opponent's.
- **The rematch's start is the opening `Snapshot`.** There is no started frame. After a
  `DuelFinished`, a `Snapshot` means a new duel has begun in the same room — nothing else can
  produce one, because a resume into a finished room carries `DuelFinished` alone. The reducer
  therefore clears `outcome` and the recorded offers on `Snapshot`.
- **Two refusals, and only two.** `Failure(UNKNOWN_ROOM)` means the room is gone: say so and offer
  the way back to the lobby. `Failure(REMATCH_UNAVAILABLE)` is **transient** — nothing was recorded
  and the same offer may be sent again — so the control stays live and no state is entered.

## Design notes

- Whatever `ADR-0044` says, and nothing beyond it.
- The offer is **idempotent on the wire**: a repeat `OfferRematch` from a seat that has already
  offered is answered with the same `RematchOffered`, not an error. The screen still shows "you have
  offered" from the frame rather than optimistically — nothing is applied before the server speaks
  (the epic's rule) — but a double click cannot produce an error state, so the button needs no
  in-flight lock of its own.
- **The window is finite and the wire does not carry it.** A finished room lingers `finishedMillis`
  (default five minutes, `ADR-0022`) and is then reaped. `ADR-0044` deliberately sends no countdown,
  so the screen shows no timer and does not retire the control on a clock of its own — the client
  never acts on a deadline the server did not state. A control pressed after the room is gone
  answers `UNKNOWN_ROOM`, and *that* is what returns the player to the lobby with the reason shown.
- **An offer survives a reconnect.** A player who reloads or reconnects re-`JoinRoom`s, receives
  `RoomJoined` and `DuelFinished`, and then one `RematchOffered` per standing offer. The result
  screen must therefore be able to take an offer *after* it has been entered, not only while it is
  live.
- The button seat alternates on a rematch; the room owns that, and the client learns it from the
  `Snapshot` that follows. Nothing here computes who is on the button.
- A rematch is the same room, not a new one. Faking it with `CreateRoom` plus a re-shared link
  changes the seating, loses the alternation, and is not a rematch.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0309` once `STORY-0213` has merged.* | — |

## Acceptance criteria

- [ ] Offering a rematch shows that this seat has offered, driven by the `RematchOffered` frame, and
      clicking twice is harmless.
- [ ] When the opponent has offered and this seat has not, the screen says so.
- [ ] When both have offered, the table returns and the first `Snapshot` of the new duel is what is
      rendered — including the button on the other seat, and no trace of the previous result.
- [ ] A rejoin onto a result screen where the opponent had already offered shows that offer.
- [ ] `Failure(UNKNOWN_ROOM)` after an offer returns the player to the lobby with the reason shown,
      rather than leaving a hanging button; `Failure(REMATCH_UNAVAILABLE)` leaves the control live
      and enters no error state.

## Out of scope

- **Any server-side change.** `ADR-0044` puts the server half in `EPIC-02` as `STORY-0213`; this
  story consumes it and does not write it.
- Any countdown, timer or expiry rendering — the wire carries no deadline (`ADR-0044`).
- Withdrawing an offer or declining one explicitly; neither exists on the wire.
- A rematch invitation to anyone who was not seated.
