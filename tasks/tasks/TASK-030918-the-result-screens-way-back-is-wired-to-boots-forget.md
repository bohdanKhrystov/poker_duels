---
schema: 2
id: TASK-030918
title: The result screen's way back is wired to boot's forget
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [client, lobby, result, ui]
depends_on: [TASK-030917]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +574 passed \(574\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room when the player takes the way back'
  - cd web-client && grep -qF 'onLeave={forgetRoom}' src/lobby/Lobby.tsx
  - cd web-client && grep -qF 'forgetRoom={client.forgetRoom}' src/main.tsx
  - cd web-client && npm run check
---

## Goal

Taking the way back off the result screen forgets the room this tab is seated in — in the app the
player runs, not only in a seam.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one hook call, one prop |
| `web-client/src/lobby/Lobby.test.tsx` | modify — the render helper gains a spy, one test added |
| `web-client/src/main.tsx` | modify — one attribute on `DuelProvider` |

## Scope

- `Lobby.tsx`'s import of the provider becomes one line, the way `npm run format` leaves it:

  ```ts
  import { useDuelState, useForgetRoom, useSend } from "../store/duel-provider";
  ```

- One hook call, beside the ones already there, in the order they are written:

  ```ts
  const send = useSend();
  const forgetRoom = useForgetRoom();
  ```

  A **named local**, not `useForgetRoom()` written inline in the JSX — a `verify` command greps for
  `onLeave={forgetRoom}`.
- The `state.outcome !== null` branch's `<DuelResult …>` gains `onLeave={forgetRoom}` beside the
  `outcome`, `mySeat` and `rematch` props `TASK-030910` gave it. Its comment, its position above the
  `state.view` branch, and everything else in the file are unchanged.
- `main.tsx`'s provider gains one attribute: `forgetRoom={client.forgetRoom}`. Nothing else in that
  file moves.
- **Event handler only.** The hook is *read* during render — it returns a function — and the
  function is called from `onClick` and nowhere else. `ADR-0032` §3 extends to it verbatim: a
  `forgetRoom` in an effect would fire on the mount a rejoin has just produced and delete the memory
  of a room this tab is sitting in.

## Out of scope

- The waiting screen. `WaitingForRival` gains no control here — that gap is `STORY-0314`, blocked on
  `DEC-068`, and this ticket must not anticipate it.
- `boot.ts`'s `DuelFinished` branch, which is still in place and still forgets on that frame. It
  goes in `TASK-030919`, deliberately *after* this ticket: until the way back forgets, deleting it
  would leave the lobby unreachable from a finished duel.
- Any assertion about storage. This ticket proves the call reaches boot's function; `TASK-030920`
  proves the whole path down to the stored code.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. One added.

`renderLobby` gains a `const forgetRoom = vi.fn();`, passes it to `DuelProvider` and returns it
beside `send`. Its signature does not change, so every existing call site and every existing test is
untouched.

| Test | Proves |
| --- | --- |
| `forgets the room when the player takes the way back` | after `RoomJoined(seat 1)` and a `DuelFinished`, one click on the `Back to the lobby` link leaves `forgetRoom` with `toHaveBeenCalledOnce()` **and** `send` with `not.toHaveBeenCalled()` — leaving tells the server nothing, because nothing on the wire says leave |

**What could be a constant and pass here is the function, not a value.** The assertion is that the
screen calls *the provider's* `forgetRoom`: a `Lobby.tsx` that passed `onLeave={() => {}}`, or that
wired the handler onto the waiting screen instead, renders identically and fails this test. The seat
is **1** to keep the story's convention that no screen test can pass on a hard-coded `0`; this test
does not otherwise depend on it, and the ticket says so rather than inventing a reason.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 574 passed (574)` | one added to 573 |
| the `--reporter=verbose` grep | the name exists |
| `grep -qF 'onLeave={forgetRoom}' src/lobby/Lobby.tsx` | the prop is wired from the named local |
| `grep -qF 'forgetRoom={client.forgetRoom}' src/main.tsx` | the app the player runs passes boot's own function. `main.tsx` is outside the test net by `ADR-0032`'s own admission and this grep is the only gate on it — `ADR-0072` says so in Consequences |

**Name the edit that makes the assertion red:** pass `onLeave={() => {}}` instead →
`forgets the room when the player takes the way back` fails on the spy. Revert, and quote it in the
PR. Say in the PR body that the `outcome` branch still precedes the `view` branch.

## Acceptance criteria

- [ ] `the lobby > forgets the room when the player takes the way back` passes
- [ ] `Lobby.tsx` contains `onLeave={forgetRoom}` and `const forgetRoom = useForgetRoom();`
- [ ] `main.tsx` contains `forgetRoom={client.forgetRoom}`
- [ ] `Lobby.tsx` still contains no `useEffect` and no `useRef`, and `if (state.outcome !== null)` still appears before `if (state.view !== null)`, which still appears before `if (state.roomCode !== null)`
- [ ] `WaitingForRival` in `Lobby.tsx` is unchanged from `develop`
- [ ] Every pre-existing `it` block in `Lobby.test.tsx` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  574 passed (574)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
