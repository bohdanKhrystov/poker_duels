---
schema: 2
id: TASK-031405
title: The press leaves nothing on the wire, and the next socket rejoins nothing
type: task
status: ready
parent: STORY-0314
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, rooms, transport]
depends_on: [TASK-031404]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing on the live socket when the host leaves the waiting screen'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the next socket at the lobby once the host has left the waiting room'
  - cd web-client && npm run check
---

## Goal

Over a real booted client and a real `Lobby`, pressing `Back to the lobby` on the waiting screen
puts no frame on the open socket, and the socket that replaces it asks for no room.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/reconnect.test.tsx` | modify — two tests added, none changed |
| `web-client/src/lobby/Lobby.tsx` | read — `WaitingForRival`, for the control's accessible name |

## Scope

- Two tests only. No source file changes anywhere.
- Use the file's existing helpers unchanged: `reconnectingClient`, `renderDuelScreen`, `sentFrames`,
  `WELCOME`, `readRoomCode`, `PROTOCOL_VERSION`. All are already imported; add no import unless
  `tsc` demands one.
- Both tests live in the existing `describe("a tab whose socket dropped")`, whose `beforeEach`
  installs fake timers — that is what makes `vi.advanceTimersByTime(250)` reach the retry.
- This is the story's *"a reload after it stays at the lobby"* criterion. jsdom does not navigate,
  so the reload is modelled the way this file already models one: the socket closes, the retry loop
  opens the next one, and that socket says `Hello`.

## Out of scope

- Any component assertion. `Lobby.test.tsx` owns the strings, the control and the press;
  `TASK-031401` to `TASK-031404` cover them.
- Anything about the *server*. There is no `LeaveRoom` on the wire (`ADR-0044`), the seat is not
  vacated, and the room still resolves for a rival who follows the link — `ADR-0073` §3. This ticket
  asserts the client sends nothing, never that the room ended.
- **The `mySeat`/`roomCode` gap** in `ADR-0073`'s Consequences — a host who is still connected when
  a rival joins gets the opening `Snapshot` on a socket whose store never saw `RoomJoined`. Real,
  reachable from this control, and not ticketed anywhere yet. Do not fix it here.

## Tests

`reconnect.test.tsx`, inside `describe("a tab whose socket dropped")`. Both open the same way —
`const { sockets, client, storage } = reconnectingClient(null)`, `renderDuelScreen(client)`, then
inside one `act`: `sockets[0].open()`, `sockets[0].receive(WELCOME)`, and
`sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}')`. With no `Snapshot`, that
is the waiting screen.

| Test | Proves |
| --- | --- |
| `sends nothing on the live socket when the host leaves the waiting screen` | the frames on the open socket are the same before and after the press |
| `opens the next socket at the lobby once the host has left the waiting room` | the memory is gone, and the socket the retry loop opens next sends `Hello` and nothing else |

**`sends nothing on the live socket when the host leaves the waiting screen`**

- `expect(screen.getByText("Waiting for your rival")).toBeDefined()`
- `const before = sentFrames(sockets[0])`, then `expect(before).toHaveLength(1)` and
  `expect(before[0]).toMatchObject({ type: "Hello" })` — the **positive control**: this socket has
  spoken, so an unchanged list afterwards is a fact about the press and not about a dead fixture
- press, inside `act`:
  `fireEvent.click(screen.getByRole("link", { name: "Back to the lobby" }))`
- `expect(sentFrames(sockets[0])).toEqual(before)`

**`opens the next socket at the lobby once the host has left the waiting room`**

- `expect(readRoomCode(storage)).toBe("ABCDEFGH")` — the **positive control**: there was a memory to
  lose, so the `null` below cannot be a memory that was never written
- press, as above
- `expect(readRoomCode(storage)).toBeNull()`
- then, inside one `act`: `sockets[0].close()`, `vi.advanceTimersByTime(250)`, `sockets[1].open()`,
  `sockets[1].receive(WELCOME)`
- ```ts
  expect(sentFrames(sockets[1])).toEqual([
    {
      type: "Hello",
      deviceId: "d-1",
      protocolVersion: PROTOCOL_VERSION,
      sessionToken: null,
    },
  ]);
  ```

  `toEqual` on the whole list, not `not.toContain` on `JoinRoom`: the `Hello` in it is what proves
  the socket really opened and really sent, and the absence of a second entry is then the claim.
  The device id is `d-1` because the first `Welcome` issued it — the merged test *rejoins the room
  the server seated it in, though the tab was opened without a code* asserts the same two frames
  for a tab that has **not** left, and is the contrast this pair turns on.

Note for the implementer: `forgetRoom` clears storage, not the store, so `state.roomCode` is
unchanged and the waiting panel is still mounted after the press. Assert nothing about a screen
change.

## Acceptance criteria

- [ ] `a tab whose socket dropped > sends nothing on the live socket when the host leaves the
      waiting screen` passes
- [ ] `a tab whose socket dropped > opens the next socket at the lobby once the host has left the
      waiting room` passes
- [ ] Both tests assert their positive control before their negative one, as written above
- [ ] No existing test in `reconnect.test.tsx` is edited or deleted, and no source file changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
