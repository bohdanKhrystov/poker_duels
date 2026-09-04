---
schema: 2
id: TASK-131113
title: The late waiting screen is pinned, and the recovery lands where the frames say
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, routing, lobby, recovery]
depends_on: [TASK-131112]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 98) }'
  - awk '/^export type Screen =/ { on = 1 } on && /^  \| "/ { n++ } on && /;$/ { on = 0 } END { exit (n != 7) }' web-client/src/routing/screen.ts
  - grep -qF 'export const PROTOCOL_VERSION: ProtocolVersion = 6;' web-client/src/protocol/version.ts
  - sh -c '! grep -rqiF "Restoring your duel" web-client/src'
  - sh -c '! grep -rqiF "Please wait" web-client/src'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The half of `DEC-127` that was answered **acceptable** is pinned by a test, so that no later ticket
answers it the other way by quietly withholding the waiting screen — and the two recovery sequences
`STORY-1310` actually drove land where the frames say they should.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0118-a-recovering-browser-shows-nothing-it-was-not-told.md` §§3 and 6. Nothing
else — **no production file is opened or changed by this ticket.**

## Scope

- Three `it` blocks, all built on the `roomAwaited: true` fixture `TASK-131112` added to
  `renderLobby`, because a recovering browser is the only tree in which any of this is interesting.
- The first test is the unusual one and its comment must say why it exists, in `ADR-0118` §3's own
  terms: **the stale waiting screen over a resumed room is accepted product behaviour, not an
  unrepaired defect, and no ticket repairs it.** `RoomJoined` is the client's whole knowledge at that
  instant and the waiting screen is the only room screen it supports; the client cannot do better,
  because the server answers a host rejoining a still-`WAITING` room with `RoomJoined` and nothing
  after it, so any rule that waits for a second frame hangs that host for the whole of their wait.

## Out of scope

- **Any production change.** This ticket adds gates to a decision that is already implemented. A red
  test here is an earlier ticket's defect and is a new ticket.
- **Repairing the frame gap.** `ADR-0118` §3 and `ADR-0114` §6 both say closing it exactly requires
  the server to name the room's state in the frame that answers the join — a wire change, the
  architect's, and forbidden on `ADR-0112`'s account (§7). Do not add a second-frame wait, a delay,
  or a guard.
- **The destructive control on that screen.** *Back to the lobby* calls `forgetRoom` and over a
  running duel that hands the seat to `ADR-0113`'s sweep. `ADR-0118` §3 names that hazard and
  **accepts** it for the width of one frame gap. Not repaired here, and not ticketed anywhere.

## Tests

| Test | Proves |
| --- | --- |
| `still shows the whole waiting screen to a recovering browser told only that it holds a room` | `roomAwaited` true, then `act(() => store.apply(ROOM_JOINED))` alone → *"Waiting for your rival"*, the room code `ABCDEFGH`, the *Invite link* label, *You*, *Back to the lobby* **and** the promise sentence *"The room stays open. That link still works for your rival, and it brings you back."* are all on screen. `ADR-0118` §3, pinned rather than merely permitted |
| `lands on the table when the Snapshot follows the RoomJoined` | the same tree, `RoomJoined` then `Snapshot` in two separate `act()` calls → `Pot 30` is on screen and *"Waiting for your rival"* is gone. `STORY-1310`'s `P4` shape |
| `lands on the result screen when a DuelFinished follows the RoomJoined` | the same tree, `RoomJoined` then `DuelFinished` in two separate `act()` calls → the result region is on screen and *"Waiting for your rival"* is gone. `STORY-1310`'s `P2` shape |

**The first test is not a duplicate of the merged `states the six strings the host-alone table
renders with no clipboard, and no seventh`, and the comment should say so.** That test renders a
browser holding no room and awaiting none; this one renders a **recovering** browser, which is the
only tree in which a future "repair" would withhold the screen. A change that withheld the waiting
screen until a second frame would leave the merged test green and this one red, which is the entire
point of writing it.

The two frames in the second and third tests go in **separate** `act()` calls, not one: the whole
subject is a sequence delivered as two socket messages, and applying them together tests a delivery
that does not happen.

## What the gates can and cannot check

The `verify:` block checks that the `Screen` union is still seven members, that `PROTOCOL_VERSION`
is still `6`, and that *Restoring your duel* and *Please wait* appear nowhere under
`web-client/src`. The bare word *Reconnecting* is **not** gated repo-wide, and deliberately so:
`openReconnectingConnection` is a merged export named in three protocol files, so a repo-wide match
on that word is red for a reason having nothing to do with copy — measured while this ticket was
written. `TASK-131112` gates the word inside `Lobby.tsx`, where it could only be copy. **The gates
cannot check that no new player-facing string was minted anywhere** — that is the reviewer's, and
`ADR-0118`'s constraint line is the standard: no wire change, no `PROTOCOL_VERSION` move, no server
file, no schema, no stored key, no new player-facing string, no new control, no new `Screen` member.

## Acceptance criteria

- [ ] All three tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at
      least 98 tests
- [ ] The first test asserts all six of the waiting screen's strings, including the promise sentence
- [ ] The second and third tests apply their two frames in two separate `act()` calls
- [ ] `Screen` still has seven members, `PROTOCOL_VERSION` is still 6, and neither *Restoring your
      duel* nor *Please wait* appears under `web-client/src`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
