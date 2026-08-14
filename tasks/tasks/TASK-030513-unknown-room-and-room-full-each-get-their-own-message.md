---
schema: 2
id: TASK-030513
title: UNKNOWN_ROOM and ROOM_FULL each get their own message, and nothing retries
type: task
status: done
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, lobby, rooms]
depends_on: [TASK-030512]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +126 passed \(126\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says an unknown room is unknown'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a full room is full'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing after a refusal until a fresh click'
  - cd web-client && npm run check
---

## Goal

The story's third acceptance criterion: a refused join says which refusal it was, leaves the form
where it is, and sends nothing more until the player clicks again.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — the nine members of `ProtocolError` |

## Scope

- `Lobby` renders the refusal as the first child of the entry `<section>`, above the create
  button:

  ```tsx
  {state.refusal !== null && <p>{refusalMessage(state.refusal)}</p>}
  ```

- One more function in the same file:

  ```tsx
  /**
   * The client shows the refusal and stops. Retrying on the player's behalf would
   * spend the ten failed joins a minute `ADR-0022` budgets them.
   */
  function refusalMessage(error: ProtocolError): string {
    switch (error) {
      case "UNKNOWN_ROOM":
        return "No duel room has that code.";
      case "ROOM_FULL":
        return "That duel room already has a rival in it.";
      default:
        return "The server refused that.";
    }
  }
  ```

  with `import type { ProtocolError } from "../protocol";` — importing a wire type is exactly what
  `src/protocol/boundary.ts` wants; declaring one outside `src/protocol/` is what it fails on.
- **Nothing here retries, and nothing here sends.** No `useEffect`, no timer, no back-off. The
  refusal sits on screen until the player acts, because an automatic retry spends a real player's
  ten-per-minute budget on their behalf.
- The refusal appears on the **entry** screen, so the form is still there to correct the code in.
  It is not reachable from the waiting panel: `TASK-030502` has `RoomJoined` clear `refusal`, and
  the `roomCode` branch returns before this line.
- The other seven `ProtocolError` members share one sentence. This story owes a distinct message
  only for the two joins can produce.

## Out of scope

- A distinct screen for `VERSION_MISMATCH` (the outdated-client notice) — not this story's.
- `TOO_MANY_ATTEMPTS`. `ADR-0022` names it as a `RoomRefusal`, but the generated `ProtocolError`
  does not carry it today; adding it is a protocol change and is not this ticket's.
- Clearing the message when the player starts typing again. It clears when the next `RoomJoined`
  lands, which is the state change that actually happened.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Three `it` blocks appended
after `TASK-030512`'s; nothing already there is edited. Each builds a store, applies
`{ type: "Failure", error: … }` before rendering, and renders through the existing `renderLobby`.

| Test | Proves |
| --- | --- |
| `says an unknown room is unknown` | after `Failure{UNKNOWN_ROOM}`, `screen.getByText("No duel room has that code.")` is findable |
| `says a full room is full` | after `Failure{ROOM_FULL}`, `screen.getByText("That duel room already has a rival in it.")` is findable — a different sentence |
| `sends nothing after a refusal until a fresh click` | after `Failure{UNKNOWN_ROOM}` and a render, `send` is uncalled; then typing `"abcdefgh"` and clicking `Join the duel` calls it exactly once with `{ type: "JoinRoom", code: "ABCDEFGH" }` |

Three tests. One hundred and twenty-three exist, so the suite reports **126**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 126 passed (126)` | the three ran and the hundred-and-twenty-three before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Delete the two named `case`s, leaving only the `default` → both message tests fail with
   `TestingLibraryElementError: Unable to find an element with the text: …`. Revert.
2. Delete the `{state.refusal !== null && …}` line → the same two fail, and `tsc` additionally
   reports `TS6133` for the now-unused `refusalMessage`. Revert.
3. Hide the submit button behind `state.refusal === null` → `sends nothing after a refusal until a
   fresh click` fails with `Unable to find an accessible element with the role "button" and name
   "Join the duel"`. Revert.

Quote all three in the PR. The third is the *"without a fresh click"* half of the criterion: the
way to make a fresh click impossible is to take the button away.

## Acceptance criteria

- [ ] `the lobby > says an unknown room is unknown` passes
- [ ] `the lobby > says a full room is full` passes
- [ ] `the lobby > sends nothing after a refusal until a fresh click` passes
- [ ] `npm run --silent test` reports `Tests  126 passed (126)`
- [ ] `Lobby.tsx` still contains no `useEffect`, no `useRef` and no timer
- [ ] The nine `it` blocks from `TASK-030510` through `TASK-030512` are unedited, and their
      assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
