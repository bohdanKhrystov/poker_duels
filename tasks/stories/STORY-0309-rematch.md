---
id: STORY-0309
title: Rematch from the result screen
type: story
status: done
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
  live. **The screen half is `TASK-030913`; the transport half is `DEC-067`'s**, answered by
  [`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md):
  `boot.ts`'s `DuelFinished` branch is deleted so a reopened socket rejoins, the memory is cleared
  by the way back instead (`DuelClient.forgetRoom`, `useForgetRoom`, one `onClick` on `DuelResult`'s
  existing `<a href="/">`), and the first fourteen tickets below are unchanged by it. That work is
  the last six, `TASK-030915`–`TASK-030920`, and this story is not `done` until they land.
- **The way back stops being a plain link, and that costs something.** It keeps its `href="/"` and
  gains one `onClick`, so a ctrl-, cmd- or shift-click fires the handler and opens the lobby in a
  *new* tab while this one stays on the result screen having already forgotten its room — its
  rematch control then answers `UNKNOWN_ROOM` on the next socket. `ADR-0072` prices that as *small,
  real, and not fixable while keeping both the link and the forget*, and `TASK-030917` leaves it
  exactly there. A guard on `event.metaKey` would narrow it in four lines; it is a behaviour the ADR
  did not decide, so it is a new `DEC` rather than a coder's or a reviewer's call.
- The button seat alternates on a rematch; the room owns that, and the client learns it from the
  `Snapshot` that follows. Nothing here computes who is on the button.
- A rematch is the same room, not a new one. Faking it with `CreateRoom` plus a re-shared link
  changes the seating, loses the alternation, and is not a rematch.

## Tasks

Split on 2026-08-23, from a **measured** baseline of 533 client tests; cumulative counts 536 → 566.
`TASK-030901` is startable now. The chain is linear because every ticket's `verify` asserts the
suite's whole count, so two of them in flight at once would each be wrong about the other.

**Six more on the same day**, `TASK-030915`–`TASK-030920`, the transport half `ADR-0072` decided
after the first split; the chain continues from 566 to **576**. They are last because five of them
touch files the fourteen above are still editing, and because the order inside them matters: the
way back learns to forget *before* `boot.ts` stops forgetting, so the lobby is never unreachable
from a finished duel in between.

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
| [TASK-030915](../tasks/TASK-030915-boot-can-forget-the-room-this-tab-remembers.md) | Boot can forget the room this tab remembers | backlog |
| [TASK-030916](../tasks/TASK-030916-the-provider-carries-the-forget-down-to-the-screen.md) | The provider carries the forget down to the screen | backlog |
| [TASK-030917](../tasks/TASK-030917-the-way-back-calls-the-forget-it-is-handed.md) | The way back calls the forget it is handed, and still navigates | backlog |
| [TASK-030918](../tasks/TASK-030918-the-result-screens-way-back-is-wired-to-boots-forget.md) | The result screen's way back is wired to boot's forget | backlog |
| [TASK-030919](../tasks/TASK-030919-a-finished-duel-forgets-nothing-and-the-next-socket-rejoins.md) | A finished duel forgets nothing, and the next socket rejoins that room | backlog |
| [TASK-030920](../tasks/TASK-030920-from-a-resumed-sockets-frames-the-way-back-forgets-the-room.md) | From a resumed socket's frames, the way back forgets the room | backlog |

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
- **Not decided at the split: `DEC-067`, the architect's — now answered.** `boot.ts` forgot the
  remembered room code on `DuelFinished` (`TASK-031009`, so a reload reaches the lobby), which meant
  a tab that reloads or whose socket reopens after the duel ends never re-`JoinRoom`s — and the
  offer `ADR-0044` §5 restates reached nobody.
  [`ADR-0072`](../../docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md)
  deletes that branch and moves the forget onto the way back, so the memory names the room this tab
  is **seated in** and only the player leaving or a refused rejoin clears it. **It blocked no ticket
  in this story** — every one of the fourteen applies frames to the store, which is what every
  screen test in `Lobby.test.tsx` already does — but the *transport* half of the fourth acceptance
  criterion below is its work, and it is `TASK-030915`–`TASK-030920`.

### What the second pass found

- **The blast radius was measured, not remembered.** `ADR-0072`'s whole change was applied at once
  in a throwaway tree and the client's gate set run in full — `tsc`, ESLint, `prettier --check`,
  Vitest and `vite build`, which is what `.github/workflows/build.yml` runs on a pull request.
  Exactly **two** merged tests turn red, both in `boot.test.ts`, and both are the two `ADR-0072` §9
  names; nothing in `reconnect.test.tsx`, `duel-provider.test.tsx`, `Lobby.test.tsx`,
  `DuelResult.test.tsx`, `App.test.tsx`, `boot-strict-mode.test.tsx` or `src/e2e/` moves, and
  nothing fails to typecheck. `TASK-030919` owns those two, deletes them in the diff that
  invalidates them, and replaces them with three.
- **§9's four replacement assertions are split across two tickets on purpose, and the story says
  which owns each** — `TASK-030919`'s table is the map. The split is not cosmetic: at
  `TASK-030915`, `boot.ts` still forgets on `DuelFinished`, so a test that finished a duel *before*
  calling `forgetRoom()` would pass with `forgetRoom` implemented as `() => {}`. It would be
  asserting the branch, not the new member. `TASK-030915` therefore proves the forget without a
  finish in the frames, and `TASK-030919` proves it with one, once the branch is gone.
- **The reversal comes last, after the wiring.** `TASK-031009`'s reason for the branch is still
  true until the way back forgets: delete it first and the way on from the result reloads straight
  back into the result screen, with the lobby unreachable — for as long as it takes the next four
  tickets to merge. So `TASK-030918` wires the screen, and only then does `TASK-030919` reverse
  `boot.ts`.
- **One assertion in `TASK-030919` cannot detect the reversal, and the ticket says so.** Restoring
  the deleted branch turns two of its three tests red and leaves the third green — the branch has
  already forgotten the code by the time `forgetRoom()` is called. It was run both ways to find
  that out. Its job is that the forget still reaches a room whose duel has ended, and the `! grep`
  on `boot.ts`, not its green, is what proves the branch is gone.

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
- [ ] A tab whose socket reopens onto a finished duel **rejoins the room**, and the way back to the
      lobby is what forgets it: after the rejoin the code is still stored, and one press of `Back to
      the lobby` removes it.

### Which ticket carries each

| Criterion | Ticket |
| --- | --- |
| 1 — this seat has offered, from the frame; two clicks are harmless | `TASK-030907` (no lock), `TASK-030908` (the chip), `TASK-030910` (nothing before the frame) |
| 2 — the opponent has offered and this seat has not | `TASK-030908`, `TASK-030910` |
| 3 — both have offered: the table returns, button on the other seat, no trace of the result | `TASK-030903` (the reducer), `TASK-030912` (the screen, both button seats read) |
| 4 — a rejoin onto a result screen shows an offer already standing | `TASK-030913` at the screen; its *transport* half — the tab rejoining at all after a `DuelFinished` — is `TASK-030919` |
| 5 — the two refusals | `TASK-030904`, `TASK-030909`, `TASK-030914` |
| 6 — the tab rejoins, and the way back forgets | `TASK-030915` (boot can forget), `TASK-030916` (the provider carries it), `TASK-030917` (the link calls it), `TASK-030918` (the screen and `main.tsx` wire it), `TASK-030919` (`DuelFinished` forgets nothing), `TASK-030920` (all of it in one assertion) |

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
- **A way out of the *waiting for your rival* screen.** `forgetRoom` is exactly what such a control
  would call, which is why `ADR-0072` names the gap while closing none of it — but a host who has
  created a room has never been in a duel, and this story is about the screen after one.
  [`STORY-0314`](STORY-0314-a-host-can-leave-the-room-they-opened.md) owns it, and `ADR-0073` has
  settled its words.
- **Narrowing the modifier click** on the way back — see the design note above. `ADR-0072` left it,
  and a change there is a new decision, not a follow-up ticket.
