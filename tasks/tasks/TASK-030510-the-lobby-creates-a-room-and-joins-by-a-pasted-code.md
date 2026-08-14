---
schema: 2
id: TASK-030510
title: The lobby creates a room, and joins by a pasted code it trims and upper-cases
type: task
status: ready
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby, rooms]
depends_on: [TASK-030509]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +117 passed \(117\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks the server for a room when the host clicks create'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends a pasted code trimmed and upper-cased'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing when the code box holds only whitespace'
  - cd web-client && npm run check
---

## Goal

The first screen a player sees: one button that asks for a room, and one box that joins somebody
else's — both sending from a click handler, never from an effect.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | create |
| `web-client/src/lobby/Lobby.test.tsx` | create |
| `web-client/src/lobby/room-link.ts` | read — `normalizeRoomCode` |
| `web-client/src/store/duel-provider.tsx` | read — `useSend`, `DuelProvider` |

## Scope

- Create `web-client/src/lobby/Lobby.tsx` with exactly this content:

  ```tsx
  import { useState, type ReactElement } from "react";
  import { useSend } from "../store/duel-provider";
  import { normalizeRoomCode } from "./room-link";

  /** The first screen: open a duel room, or join one by the code on the invite. */
  export function Lobby(): ReactElement {
    const send = useSend();
    const [typedCode, setTypedCode] = useState("");
    const code = normalizeRoomCode(typedCode);

    return (
      <section>
        <button type="button" onClick={() => send({ type: "CreateRoom" })}>
          Create a duel room
        </button>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            // An empty code would spend one of the ten failed joins ADR-0022
            // budgets this player every minute, and tell them nothing.
            if (code === "") return;
            send({ type: "JoinRoom", code });
          }}
        >
          <label htmlFor="room-code">Room code</label>
          <input
            id="room-code"
            value={typedCode}
            onChange={(event) => setTypedCode(event.target.value)}
          />
          <button type="submit">Join the duel</button>
        </form>
      </section>
    );
  }
  ```

- **The typed text is state; the normalised code is derived.** What the player sees in the box is
  what they typed — the client does not rewrite the field under their cursor — and what is sent is
  `normalizeRoomCode` of it.
- `event.preventDefault()` is what keeps jsdom (and a browser) from navigating on submit.
- **Both sends happen in event handlers.** No `useEffect` anywhere in this file, in this ticket or
  any later one: `ADR-0032` makes a message-triggered send a boot reaction, and a click-triggered
  send a handler. There is no third kind.
- **No `className` attributes.** This story ships behaviour; the design track applies tokens in its
  own pass, and a bare `<section>` is what it will restyle.
- The vocabulary is *duel*, *rival*, *room* (`docs/vision.md`). Never *table*, *buy-in*, *lobby
  chips*.

## Out of scope

- Anything that depends on `useDuelState` — the waiting panel (`TASK-030511`), the refusal
  (`TASK-030513`), the duel beginning (`TASK-030514`). This component does not call `useDuelState`
  yet, and `noUnusedLocals` will reject calling it before it is read.
- Disabling the join button. The guard is in the handler, which is what the test can actually
  falsify: a click on a disabled button dispatches nothing, so an assertion about it would pass
  even with the guard removed.
- `App.tsx` rendering this — `TASK-030515`.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, one `describe("the lobby")`, with two helpers:

```tsx
function renderLobby(store: DuelStore = createDuelStore()): {
  send: ReturnType<typeof vi.fn>;
} {
  const send = vi.fn();
  render(
    <DuelProvider store={store} send={send}>
      <Lobby />
    </DuelProvider>,
  );
  return { send };
}

function typeCode(value: string): void {
  fireEvent.change(screen.getByLabelText("Room code"), { target: { value } });
}
```

The store parameter is unused by this ticket's three tests and is what every later lobby ticket
drives. Buttons are found by accessible name: `screen.getByRole("button", { name: "Join the
duel" })`.

| Test | Proves |
| --- | --- |
| `asks the server for a room when the host clicks create` | clicking `Create a duel room` calls `send` exactly once, with `{ type: "CreateRoom" }` |
| `sends a pasted code trimmed and upper-cased` | typing `"  abcdefgh  "` then clicking `Join the duel` calls `send` exactly once, with `{ type: "JoinRoom", code: "ABCDEFGH" }` |
| `sends nothing when the code box holds only whitespace` | typing `"   "` then clicking `Join the duel` leaves `send` uncalled |

Three tests. One hundred and fourteen exist, so the suite reports **117**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 117 passed (117)` | the three ran and the hundred-and-fourteen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Send `code: typedCode` instead of `code` → `sends a pasted code trimmed and upper-cased` fails
   with `expected "spy" to be called with arguments: [ { type: 'JoinRoom', …(1) } ]`, the received
   call carrying `'  abcdefgh  '`. Revert.
2. Delete the `if (code === "") return;` guard → `sends nothing when the code box holds only
   whitespace` fails with `expected "spy" to not be called at all, but actually been called 1
   times`. Revert.
3. Make the create button send `{ type: "JoinRoom", code }` → `asks the server for a room when the
   host clicks create` fails with `expected "spy" to be called with arguments: [ { type:
   'CreateRoom' } ]`. Revert.

Quote all three in the PR. The second is the story's rate-limit note made executable.

## Acceptance criteria

- [ ] `the lobby > asks the server for a room when the host clicks create` passes
- [ ] `the lobby > sends a pasted code trimmed and upper-cased` passes
- [ ] `the lobby > sends nothing when the code box holds only whitespace` passes
- [ ] `npm run --silent test` reports `Tests  117 passed (117)`
- [ ] `Lobby.tsx` contains no `useEffect`, no `useRef`, and no `className`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
