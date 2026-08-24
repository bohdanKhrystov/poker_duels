---
schema: 2
id: TASK-031401
title: The waiting screen offers the way back to the lobby, and the press forgets the room
type: task
status: ready
parent: STORY-0314
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, ui, rooms]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers the way back to the lobby while the room is still waiting'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room and sends nothing when the host leaves the waiting screen'
  - cd web-client && grep -qF 'Back to the lobby' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiE 'cancel|close the room|delete the room|end the room|leave the room|leave the duel|give up|abandon|withdraw|forfeit|cash out|exit table|stand up|sit out' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qE 'setTimeout|setInterval|Date[.]now|new Date' src/lobby/Lobby.tsx
  - cd web-client && npm run check
---

## Goal

`WaitingForRival` renders one control reading `Back to the lobby`; pressing it calls `forgetRoom`
and lets the browser navigate, and it sends nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one prop, one anchor |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `web-client/src/result/DuelResult.tsx` | read — the anchor this copies: the string, the `href`, the `onClick`, the classes |

## Scope

- `WaitingForRival`'s props become `{ code: string; onLeave: () => void }` — **required**, not
  optional: this component has exactly one call site, and an optional handler would be a hole a
  `main.tsx`-shaped mistake could fall through.
- The `state.roomCode !== null` branch passes `onLeave={forgetRoom}`. `Lobby` already holds
  `const forgetRoom = useForgetRoom()` at the top — do not add a hook, and do not reach into boot.
- Inside `WaitingForRival`'s `<section>`, **immediately after `<CopyLink link={link} />`**, render:

  ```tsx
  <a
    className="rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
    href="/"
    onClick={props.onLeave}
  >
    Back to the lobby
  </a>
  ```

  The string is byte-identical to the one `DuelResult.tsx` already renders (`ADR-0073` §2) and the
  classes are that anchor's, so one action reads and looks the same in both places. It stays an
  `<a href="/">`: no `preventDefault`, no `window.location` — `removeItem` is synchronous, so the
  memory is gone before the browser leaves, and the reload is what reaches an empty store
  (`ADR-0072` §5).
- Run `npm run format` before `npm run check`; `format:check` is part of the gate.

## Out of scope

- **The line that says the room stays open** — `TASK-031402`. Add no second string here.
- **The "no third string", "no refused word" and "no clock" assertions** — `TASK-031403` and
  `TASK-031404`.
- **The boot-level proof that a reopened socket no longer rejoins** — `TASK-031405`.
- **Anything sent to the server.** There is no `LeaveRoom` on the wire (`ADR-0044`), the seat is not
  vacated and the room is not told. `EPIC-03` writes no Kotlin.
- **`design/screens/create-duel.html`** — `EPIC-06`'s, per `ADR-0073` §6. This ticket does not wait
  on it and does not touch it.
- **The `mySeat`/`roomCode` gap** in `ADR-0073`'s Consequences (a host pulled into a duel on a socket
  whose store never saw `RoomJoined`). Real, reachable from this control, and not ticketed here.

## Tests

`Lobby.test.tsx`, inside `describe("the lobby")`. Both use the existing `renderLobby` helper and
`store.apply(ROOM_JOINED)` — `ROOM_JOINED` is already defined at the top of the file. Do **not**
call `withClipboard`: the waiting screen without a clipboard is the fixture the later tickets
enumerate.

| Test | Proves |
| --- | --- |
| `offers the way back to the lobby while the room is still waiting` | after `RoomJoined` and no `Snapshot`, `screen.getByRole("link", { name: "Back to the lobby" })` exists, its `href` is `/`, its `className.split(" ")` contains `border-hairline`, and `screen.getByText("Waiting for your rival")` is on the same screen — so the link is on the waiting panel and not somewhere else |
| `forgets the room and sends nothing when the host leaves the waiting screen` | `const clickReturn = fireEvent.click(link)` gives `forgetRoom` called **once**, `clickReturn === true` (nothing prevented the navigation) and `send` **not** called at all |

The second test is a negative assertion (`send` was never called) with its positive control in the
same act: `forgetRoom` **was** called once, so the press reached a live handler and the empty
`send` mock is a fact about the press rather than about an inert fixture.

There is no seat pair here on purpose: `WaitingForRival` is handed a code and a handler, no seat
reaches a comparison anywhere in this change, and a second seat would assert nothing.

Note for the implementer: jsdom does not navigate, so the waiting screen is still on screen after
the click and `state.roomCode` is unchanged — `forgetRoom` clears storage, not the store. Assert the
call, never a screen change.

## Acceptance criteria

- [ ] `the lobby > offers the way back to the lobby while the room is still waiting` passes
- [ ] `the lobby > forgets the room and sends nothing when the host leaves the waiting screen` passes
- [ ] `Lobby.tsx` contains the exact string `Back to the lobby` and none of the words `ADR-0073` §5
      refuses (the `! grep -qiE` command in `verify:` exits 0)
- [ ] `Lobby.tsx` names no timer and no clock (the `! grep -qE` command in `verify:` exits 0)
- [ ] No existing test in `Lobby.test.tsx` is edited or deleted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
