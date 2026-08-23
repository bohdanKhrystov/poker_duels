---
id: STORY-0314
title: A host can leave the room they opened
type: story
status: blocked
parent: EPIC-03
module: web-client
labels: [client, ui, rooms]
depends_on: [STORY-0309]
---

## Goal

A player who creates a duel room and is not joined can get back to the lobby, in this tab, without
waiting for the room to be reaped or clearing browser storage.

## Why

Today they cannot, and it is a trap with no exit. It was found while `ADR-0072` was being written —
named in that ADR's Consequences and deliberately left there, because *whether that screen offers a
way out is a screen the product owns*. **Verified against the tree, not taken on trust:**

- `Lobby.tsx`'s `WaitingForRival` renders the code, the invite link and a copy button. There is no
  other control on it, and `design/screens/create-duel.html`'s *Created — waiting for your rival*
  frame has none either: the code, the link line with `Copy link`, the open seat, the host's seat.
- `boot.ts` writes `pd.roomCode` on `RoomJoined`, so the tab is remembered in that room. A reload
  re-`JoinRoom`s it.
- `RoomRegistry.resume` answers `null` for a `RoomState.WAITING` room, so the rejoin falls through
  to the ordinary join; `Room.join` refuses a player who already holds a seat with
  `ALREADY_SEATED`, and `DuelSocket.replyToJoinRoom` answers that *exactly as a fresh seating
  would* — `RoomJoined(code, seat)`. The store sets `roomCode`, and `Lobby.tsx`'s
  `state.roomCode !== null` branch puts the same waiting screen back up.

So the routes out are: a rival joins; the room is reaped as idle (`ADR-0022`, ten minutes
unclaimed); or the player clears storage. `ADR-0072` gives the client the missing piece —
`DuelClient.forgetRoom` and `useForgetRoom()` — which is why this is cheap to close now and was not
before: `TASK-030918` already wires exactly this call to exactly this kind of control, one screen
along.

## What is not decided

**`DEC-068`, the product owner's.** Does that screen offer a way out at all, and what does it say?
Nothing in `docs/vision.md`, `ADR-0022` or the design settles it, and the words are not a planner's
to invent — a *Cancel* that leaves a live room standing for ten minutes says something different
from a *Back*, and the design has no such control to copy. **Nothing in this story is splittable
until it is answered**; it blocks no other story and no ticket outside this one.

What the decision does **not** have to settle, because it is already settled:

- **Nothing is sent.** There is no leave on the wire (`ADR-0044` ships no `LeaveRoom`), so the room
  stands until it is reaped whatever the control says, and the seat is not vacated. A control that
  claimed otherwise would be lying to the player.
- **How it forgets.** `forgetRoom()` from an event handler, `ADR-0072` §4 and `ADR-0032` §3 — never
  from an effect, which would fire on the mount a rejoin has just produced.
- **How it navigates.** The way back on the result screen is a plain `<a href="/">` whose `onClick`
  forgets, and the reload is what reaches an empty store (`TASK-030807`). The same shape applies
  here, and the same modifier-click cost applies with it (`ADR-0072`, Consequences).

## Design notes

- The control belongs to `WaitingForRival` in `web-client/src/lobby/Lobby.tsx`, beside the invite
  box. `Lobby.tsx` already holds `useForgetRoom()` after `TASK-030918`, so nothing new reaches into
  boot.
- Whether `design/screens/create-duel.html` gains the affordance is part of `DEC-068`: the design is
  the source for this screen, and today it shows one.
- A player who leaves and then follows their own invite link is a *first* join to the server, which
  refuses it with `ALREADY_SEATED` and answers `RoomJoined` — they land back on the waiting screen,
  correctly, because they still hold the seat. The client is not being asked to hide that.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Splittable the day `DEC-068` is answered — the mechanism is settled, only the words are not.* | — |

## Acceptance criteria

- [ ] From the *waiting for your rival* screen, one action reaches the lobby in this tab, and a
      reload after it stays at the lobby rather than returning to the room.
- [ ] That action sends nothing: the socket sees no frame because of it.
- [ ] The words on it are `DEC-068`'s, quoted from the ADR that answers it, not chosen by the
      implementer.

## Out of scope

- **A `LeaveRoom` frame, or any change to how a room is reaped.** Both are `EPIC-02`'s and neither
  is proposed here; `EPIC-03` writes no Kotlin.
- Telling the rival anything. Nobody is in the room to tell, and no frame could carry it.
- The result screen's way back, which already exists and already forgets (`TASK-030918`).
- URL-addressable routes and browser *Back*, which are `DEC-054`'s for the whole client.
