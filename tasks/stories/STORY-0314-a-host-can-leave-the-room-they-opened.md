---
id: STORY-0314
title: A host can leave the room they opened
type: story
status: done
parent: EPIC-03
module: web-client
labels: [client, ui, rooms]
depends_on: [STORY-0309]
---

## Goal

A player who creates a duel room and is not joined presses **`Back to the lobby`** and gets there,
in this tab, without waiting for the room to be reaped or clearing browser storage — and the screen
tells them the room stays open and the link still works ([`ADR-0073`](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)).

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

## What was decided, and what it fixes

**`DEC-068` is answered by [`ADR-0073`](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md).**
Every string below is quoted from it and none of them is the implementer's to choose:

- **The control reads `Back to the lobby`** — capital *B*, lower-case *lobby*, no full stop,
  byte-identical to the string `DuelResult.tsx` already renders for the same action on the same
  memory (`ADR-0073` §2).
- **It does nothing to the room**, and **exactly one other line** says so, placed with the control
  (`ADR-0073` §3):

  > `The room stays open. That link still works for your rival, and it brings you back.`

- **No confirmation** — no dialog, no second press, no undo (`ADR-0073` §4). The action destroys
  nothing.
- **No duration, countdown or expiry time** appears anywhere on this screen: the client owns no
  clock against a server window (`ADR-0072` §6). When the room is finally reaped, the already
  shipped *No duel room has that code.* is the correction.
- **Those two strings are the whole addition.** A third string on this screen needs a new ADR, not a
  ticket (`ADR-0073` §3).
- *Cancel*, *Close the room*, *Delete the room*, *Leave*, *Back* alone, *forfeit*, *give up*,
  *stand up* and *sit out* are refused by name, each with its reason (`ADR-0073` §5).
- **`design/screens/create-duel.html`'s waiting frame gains both strings verbatim**, as `EPIC-06`'s
  work. Placement and weight are the design's. **This story does not wait on that frame**
  (`ADR-0073` §6).

This story is splittable now, and it blocks no other story and no ticket outside itself.

What the decision did **not** have to settle, because it was already settled:

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
- `design/screens/create-duel.html` gains the control and the line (`ADR-0073` §6), as `EPIC-06`'s
  work. The words are fixed, so the two cannot drift while they are briefly out of step.
- A player who leaves and then follows their own invite link is a *first* join to the server, which
  refuses it with `ALREADY_SEATED` and answers `RoomJoined` — they land back on the waiting screen,
  correctly, because they still hold the seat. The client is not being asked to hide that.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-031401](../tasks/TASK-031401-the-waiting-screen-offers-the-way-back-to-the-lobby.md) | The waiting screen offers the way back to the lobby, and the press forgets the room | ready |
| [TASK-031402](../tasks/TASK-031402-one-line-says-the-room-stays-open.md) | One line says the room stays open and the link still works | backlog |
| [TASK-031403](../tasks/TASK-031403-two-strings-are-the-whole-addition-and-nothing-stands-between-the-press-and-the-lobby.md) | Two strings are the whole addition, and nothing stands between the press and the lobby | backlog |
| [TASK-031404](../tasks/TASK-031404-the-waiting-screen-offers-none-of-the-words-adr-0073-refuses.md) | The waiting screen offers none of the words `ADR-0073` refuses, and names no deadline | backlog |
| [TASK-031405](../tasks/TASK-031405-the-press-leaves-nothing-on-the-wire-and-the-next-socket-rejoins-nothing.md) | The press leaves nothing on the wire, and the next socket rejoins nothing | backlog |

One chain, one startable ticket: every ticket after the first waits on the one above it, because
four of the five touch `Lobby.test.tsx` and the run is sequential.

Each acceptance criterion below is somebody's: the first is `TASK-031401`'s control and
`TASK-031405`'s next socket, the second is `TASK-031401`'s `send` and `TASK-031405`'s frame count,
the third is `TASK-031402`'s line and `TASK-031403`'s enumeration, the fourth is `TASK-031403`'s
press and `TASK-031404`'s vocabulary.

## Acceptance criteria

- [ ] From the *waiting for your rival* screen, one action reaches the lobby in this tab, and a
      reload after it stays at the lobby rather than returning to the room.
- [ ] That action sends nothing: the socket sees no frame because of it.
- [ ] The control reads `Back to the lobby` and the screen carries exactly one other new line,
      `The room stays open. That link still works for your rival, and it brings you back.` — both
      quoted from `ADR-0073`, neither chosen by the implementer, and no third string added.
- [ ] No confirmation step stands between the press and the lobby, and no duration, countdown or
      expiry time appears on this screen.

## Out of scope

- **A `LeaveRoom` frame, or any change to how a room is reaped.** Both are `EPIC-02`'s and neither
  is proposed here; `EPIC-03` writes no Kotlin.
- Telling the rival anything. Nobody is in the room to tell, and no frame could carry it.
- The result screen's way back, which already exists and already forgets (`TASK-030918`).
- URL-addressable routes and browser *Back*, which are `DEC-054`'s for the whole client.
- **The `mySeat`/`roomCode` gap this control makes reachable.** A host who presses this with their
  tab still open, and whose rival then follows the link, is pulled into the duel on a socket whose
  store never saw `RoomJoined`: `deliver` addresses frames by player id, so the opening `Snapshot`
  arrives, `Lobby.tsx`'s `state.view !== null` branch renders the table, and `duel-state.ts` sets
  `mySeat` and `roomCode` only on `RoomJoined` — so the duel plays with `mySeat` null and a reload
  does not rejoin. [`ADR-0073`](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md)
  records it in Consequences as *"a defect and a ticket, not a decision"*: closing the tab reaches it
  too, so this story does not create it, and none of the five tickets touches it. **It is not
  ticketed anywhere yet** and needs a home of its own.
