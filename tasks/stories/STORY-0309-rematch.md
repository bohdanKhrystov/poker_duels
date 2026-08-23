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
  live. **The screen half is `TASK-030913`; the transport half does not work yet** — `boot.ts`
  forgets the room code on `DuelFinished` (`TASK-031009`), so no reopened socket re-`JoinRoom`s
  after a duel ends, and `DEC-067` is open on what to do about it.
- The button seat alternates on a rematch; the room owns that, and the client learns it from the
  `Snapshot` that follows. Nothing here computes who is on the button.
- A rematch is the same room, not a new one. Faking it with `CreateRoom` plus a re-shared link
  changes the seating, loses the alternation, and is not a rematch.

## Tasks

Split on 2026-08-23, from a **measured** baseline of 533 client tests; cumulative counts 536 → 566.
`TASK-030901` is startable now. The chain is linear because every ticket's `verify` asserts the
suite's whole count, so two of them in flight at once would each be wrong about the other.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030901](../tasks/TASK-030901-the-store-records-which-seats-have-offered.md) | The store records which seats have offered a rematch | ready |
| [TASK-030902](../tasks/TASK-030902-a-finished-duel-begins-the-result-screen-with-no-offer-standing.md) | A finished duel begins the result screen with no offer standing | backlog |
| [TASK-030903](../tasks/TASK-030903-the-snapshot-after-a-finish-is-the-rematch.md) | The snapshot after a finish is the rematch, and clears the duel that ended | backlog |
| [TASK-030904](../tasks/TASK-030904-a-rematch-the-room-cannot-take-yet-is-recorded-nowhere.md) | A rematch the room cannot take yet is recorded nowhere | backlog |
| [TASK-030905](../tasks/TASK-030905-whose-rematch-offer-it-is.md) | Whose rematch offer it is, read from the seat the server gave this client | backlog |
| [TASK-030906](../tasks/TASK-030906-the-result-panel-shows-the-rematch-it-is-handed.md) | The result panel shows the rematch it is handed, and adds none of its own | backlog |
| [TASK-030907](../tasks/TASK-030907-the-rematch-control-offers-one-press.md) | The rematch control offers one press, and a second press is harmless | backlog |
| [TASK-030908](../tasks/TASK-030908-the-control-says-who-has-offered.md) | The control says who has offered, and reads it from either side | backlog |
| [TASK-030909](../tasks/TASK-030909-a-room-that-is-gone-retires-the-control.md) | A room that is gone retires the control and says so | backlog |
| [TASK-030910](../tasks/TASK-030910-the-result-screen-hands-over-the-control-and-the-press-reaches-the-wire.md) | The result screen hands over the control, and the press reaches the wire | backlog |
| [TASK-030911](../tasks/TASK-030911-the-way-back-steps-aside-for-the-rematch.md) | The way back steps aside for the rematch | backlog |
| [TASK-030912](../tasks/TASK-030912-the-rematch-begins-and-the-button-changes-sides.md) | The rematch begins, and the button is on the other side | backlog |
| [TASK-030913](../tasks/TASK-030913-an-offer-restated-after-a-rejoin-reaches-the-result-screen.md) | An offer restated after a rejoin reaches the result screen, and one stated before it does not | backlog |
| [TASK-030914](../tasks/TASK-030914-a-gone-room-ends-it-and-a-transient-refusal-does-not.md) | A gone room ends the rematch, and a transient refusal leaves it live | backlog |

### What the split found, and what it did not decide

- **`ADR-0044` §5's ordering has a client half, and the store did not have it.** The reducer ignored
  `RematchOffered` entirely (`default: return state`), so nothing distinguished an offer arriving
  before `DuelFinished` from one arriving after — the distinction the server took a commitment on.
  `TASK-030902` makes `DuelFinished` clear the recorded offers, and `TASK-030913` asserts the
  ordering at the screen, both ways round.
- **`Snapshot` did not clear `outcome`, and `Lobby.tsx` tests `outcome` before `view`.** Without
  `TASK-030903` a rematch's opening frames would leave the result screen up forever. Measured: that
  reducer change, together with everything else this story adds to `duel-state.ts`, turns exactly
  **one** pre-existing test red — `starts with nothing the server has not sent`, which
  `TASK-030901` owns and fixes.
- **`STORY-0308`'s `offers no rematch it cannot honour` is invalidated by this story.**
  `TASK-030906` replaces it with two narrower tests rather than leaving a test standing whose
  premise (`DEC-023` open, the wire unable to carry one) is gone.
- **Not decided here: `DEC-067`, the architect's.** `boot.ts` forgets the remembered room code on
  `DuelFinished` (`TASK-031009`, so a reload reaches the lobby), which means a tab that reloads or
  whose socket reopens after the duel ends never re-`JoinRoom`s — and the offer `ADR-0044` §5
  restates reaches nobody. Undoing the forget re-opens the trap `TASK-031009` closed, so it needs an
  answer, not a guess. **It blocks no ticket in this story**: every one of the fourteen applies
  frames to the store, which is what every screen test in `Lobby.test.tsx` already does. It blocks
  the *transport* half of the fourth acceptance criterion below.

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

### Which ticket carries each

| Criterion | Ticket |
| --- | --- |
| 1 — this seat has offered, from the frame; two clicks are harmless | `TASK-030907` (no lock), `TASK-030908` (the chip), `TASK-030910` (nothing before the frame) |
| 2 — the opponent has offered and this seat has not | `TASK-030908`, `TASK-030910` |
| 3 — both have offered: the table returns, button on the other seat, no trace of the result | `TASK-030903` (the reducer), `TASK-030912` (the screen, both button seats read) |
| 4 — a rejoin onto a result screen shows an offer already standing | `TASK-030913`. Its *transport* half — the tab rejoining at all after a `DuelFinished` — is `DEC-067`'s |
| 5 — the two refusals | `TASK-030904`, `TASK-030909`, `TASK-030914` |

Criterion 5's *returns the player to the lobby* is delivered as `ADR-0044` §6 words it — **the
client says so and offers the way back** — not as a navigation the client performs: the reason
replaces the rematch control and `TASK-030807`'s `Back to the lobby` link, directly below it, is the
way back. The alternative, a reducer that cleared `outcome`, `view` and `roomCode` on any
`UNKNOWN_ROOM`, would change what that frame means to the whole client — including a reconnect that
lands on a reaped room mid-duel — and is deliberately not taken here.

## Out of scope

- **Any server-side change.** `ADR-0044` puts the server half in `EPIC-02` as `STORY-0213`; this
  story consumes it and does not write it.
- Any countdown, timer or expiry rendering — the wire carries no deadline (`ADR-0044`).
- Withdrawing an offer or declining one explicitly; neither exists on the wire.
- A rematch invitation to anyone who was not seated.
