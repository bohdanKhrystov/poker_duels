---
schema: 2
id: TASK-031402
title: One line says the room stays open and the link still works
type: task
status: backlog
parent: STORY-0314
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, ui, rooms]
depends_on: [TASK-031401]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says the room stays open and the link still works'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps that line off the screen where there is no room'
  - cd web-client && grep -qF 'The room stays open. That link still works for your rival, and it brings you back.' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiE 'cancel|close the room|delete the room|end the room|leave the room|leave the duel|give up|abandon|withdraw|forfeit|cash out|exit table|stand up|sit out' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qE 'setTimeout|setInterval|Date[.]now|new Date' src/lobby/Lobby.tsx
  - cd web-client && npm run check
---

## Goal

The waiting screen carries `ADR-0073` §3's line, verbatim, directly under the way back — and carries
it nowhere else.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one paragraph in `WaitingForRival` |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |

## Scope

- Immediately after the `<a href="/">Back to the lobby</a>` that `TASK-031401` added, inside the
  same `<section>`, render exactly one paragraph:

  ```tsx
  <p className="text-small text-text-muted">
    The room stays open. That link still works for your rival, and it brings you back.
  </p>
  ```

- **The words are `ADR-0073` §3's and are not the implementer's.** Byte-identical, including both
  full stops and the comma. Do not reword, split, shorten or add to it.
- After the control, not before it: the line explains a control the player has already found, and it
  is what makes `ADR-0073` §4's *no confirmation* defensible. `TASK-031403` asserts this order.
- It is the **only** addition. No heading, no icon, no second sentence, no aria-label — a third
  string on this screen needs an ADR, not a ticket (`ADR-0073` §3).
- Run `npm run format` before `npm run check`.

## Out of scope

- **Any duration, countdown or expiry.** The line names no deadline: the client owns no clock
  against a server window (`ADR-0072` §6), and when the room is finally reaped the already shipped
  *No duel room has that code.* is the correction. `TASK-031404` asserts this.
- **The "no third string" enumeration** — `TASK-031403`.
- **`design/screens/create-duel.html`**, which gains the same words as `EPIC-06`'s work
  (`ADR-0073` §6). This ticket does not touch it and does not wait on it.

## Tests

`Lobby.test.tsx`, inside `describe("the lobby")`. Put the line in a `const` at the top of the two
tests so it is written once, and write it as a **literal** — do not import a constant from
`Lobby.tsx`, or the test would assert the encoder against itself.

| Test | Proves |
| --- | --- |
| `says the room stays open and the link still works` | after `store.apply(ROOM_JOINED)` and `renderLobby(store)`, `screen.getByText("The room stays open. That link still works for your rival, and it brings you back.")` is defined, and `screen.getByRole("link", { name: "Back to the lobby" })` is defined on the same screen |
| `keeps that line off the screen where there is no room` | with a fresh `createDuelStore()` and no frames, `screen.queryByText(<the same literal>)` is `null` **and** `screen.getByRole("button", { name: "Create a duel room" })` is defined |

The second test is a negative assertion with its positive control beside it: the lobby's own
control is on screen, so the missing line is a fact about the branch and not about a render that
failed. It is also the whole test for *"the line belongs to the waiting screen"* — a paragraph
added outside the `state.roomCode !== null` branch passes the first test and fails this one.

## Acceptance criteria

- [ ] `the lobby > says the room stays open and the link still works` passes
- [ ] `the lobby > keeps that line off the screen where there is no room` passes
- [ ] `Lobby.tsx` contains the line byte-identically (the `grep -qF` command in `verify:` exits 0)
- [ ] `Lobby.tsx` still names no refused word and no clock (both `! grep` commands exit 0)
- [ ] No existing test in `Lobby.test.tsx` is edited or deleted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
