---
schema: 2
id: TASK-030503
title: Boot joins the tab's one connection to its one store
type: task
status: done
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030502]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +94 passed \(94\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF "opens the tab's one connection"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'folds every frame the server sends into the store'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends through the connection it opened'
  - cd web-client && npm run check
---

## Goal

`bootDuelClient` exists: framework-free wiring that opens the tab's one connection, folds every
frame it delivers into the tab's one store, and hands back the one way out (`ADR-0032` §2).

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | create |
| `web-client/src/store/boot.test.ts` | create |
| `web-client/src/protocol/connection.ts` | read — `Connection`, `ConnectionOptions`, `openConnection` |
| `web-client/src/protocol/fake-socket.ts` | read — `FakeSocket`, `open`, `receive`, `sent`, `asWebSocket` |
| `docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md` | read — §2 and §4 |

## Scope

- Create `web-client/src/store/boot.ts` with exactly this content:

  ```ts
  import type { ClientMessage, Connection, ServerMessage } from "../protocol";
  import { createDuelStore, type DuelStore } from "./duel-store";

  /** The tab's store, and the one way out to the server. */
  export interface DuelClient {
    readonly store: DuelStore;
    readonly send: (message: ClientMessage) => void;
  }

  export interface BootOptions {
    readonly connect: (onMessage: (message: ServerMessage) => void) => Connection;
  }

  /**
   * Wires this tab's one connection to its one store, outside React's tree:
   * `main.tsx` calls this once, before rendering (`ADR-0032`). Nothing closes the
   * connection — closing the tab is the close.
   */
  export function bootDuelClient(options: BootOptions): DuelClient {
    const store = createDuelStore();
    const connection = options.connect((message) => {
      store.apply(message);
    });

    return {
      store,
      send: (message) => {
        connection.send(message);
      },
    };
  }
  ```

- **`options.connect` is called exactly once**, and the returned `Connection` never leaves this
  function. `DuelClient` exposes `store` and `send` and nothing else: no `close`, no `status`, no
  socket. A screen that could call `close()` is a screen that can end a duel by re-rendering.
- **This file imports nothing from `react`** and nothing from `src/protocol/connection.ts`
  directly — the barrel `../protocol` is the surface, and `connect` arrives as a parameter so this
  module never names the real socket.
- **Do not write the token `WebSocket` or `MessageEvent` anywhere in either file.**
  `src/protocol/boundary.ts` fails the suite on `\b(?:WebSocket|MessageEvent)\b` in any file
  outside `src/protocol/`. `fake.asWebSocket()` is safe — the regex needs a word boundary before
  `W`, and `asWebSocket` has none — but a variable annotated `: WebSocket` is not.

## Out of scope

- The `JoinRoom` reaction and the `joinRoomCode` option — `TASK-030504`. `BootOptions` has exactly
  one field in this ticket.
- Reconnection, retry, or reopening after a close — `STORY-0310`. Nothing here calls `close`.
- Anything React — `TASK-030505`.

## Tests

`web-client/src/store/boot.test.ts`, one `describe("booting the duel client")`.

Three helpers above it, in this order:

- `inMemoryStorage(): Storage` — copy the body from `web-client/src/protocol/connection.test.ts`
  verbatim, keeping a one-paragraph version of its comment. **Never touch the real
  `localStorage`**: Node 24+ defines an inert one that shadows jsdom's under Vitest, so
  `typeof localStorage === "undefined"` while `sessionStorage` works.
- `bootOverFakeSocket()` — takes no arguments in this ticket, has **no explicit return type** (the
  inferred one carries the `vi.fn` generics; `ReturnType<typeof vi.fn>` does not typecheck here):

  ```ts
  function bootOverFakeSocket() {
    const socket = new FakeSocket();
    const connect = vi.fn((onMessage: (message: ServerMessage) => void) =>
      openConnection({
        socket: socket.asWebSocket(),
        storage: inMemoryStorage(),
        onMessage,
      }),
    );
    const client = bootDuelClient({ connect });
    return { socket, client, connect };
  }
  ```

  The spy's parameter **must** be annotated `(message: ServerMessage) => void`; leaving it to be
  inferred as `never` fails `tsc` with `TS2322`.
- `sentFrames(socket: FakeSocket): { type: string; code?: string }[]` — `socket.sent` mapped
  through `JSON.parse` with that cast.

`FakeSocket` is **not** exported from `src/protocol/index.ts`; import it as
`import { FakeSocket } from "../protocol/fake-socket";`. `openConnection` and the type
`ServerMessage` do come from the barrel: `import { openConnection, type ServerMessage } from
"../protocol";`.

| Test | Proves |
| --- | --- |
| `opens the tab's one connection` | booting calls the `connect` spy exactly once — `toHaveBeenCalledOnce()` |
| `folds every frame the server sends into the store` | after `socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}')`, `client.store.getState()` has `roomCode` `"ABCDEFGH"` and `mySeat` `1` |
| `sends through the connection it opened` | after `socket.open()` then `client.send({ type: "CreateRoom" })`, `socket.sent` has length 2 and `JSON.parse(socket.sent[1])` equals `{ type: "CreateRoom" }` — the `Hello` the connection wrote on open is the first |

Three tests. Ninety-one exist, so the suite reports **94**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 94 passed (94)` | the three tests ran and the ninety-one before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats — and re-runs `boundary.test.ts` over the two new files |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Add a second `options.connect(() => {});` before the `return` → `opens the tab's one
   connection` fails with `expected "spy" to be called once, but got 2 times`. Revert.
2. Replace the callback with `options.connect(() => {})` → `folds every frame the server sends
   into the store` fails with `expected null to be 'ABCDEFGH' // Object.is equality`. Revert.
3. Replace the returned `send` with `send: () => {}` → `sends through the connection it opened`
   fails with `expected [ Array(1) ] to have a length of 2 but got 1`, and `tsc` additionally
   reports `error TS6133: 'connection' is declared but its value is never read`. Revert both.

Quote all three in the PR.

## Acceptance criteria

- [ ] `booting the duel client > opens the tab's one connection` passes
- [ ] `booting the duel client > folds every frame the server sends into the store` passes
- [ ] `booting the duel client > sends through the connection it opened` passes
- [ ] `npm run --silent test` reports `Tests  94 passed (94)`
- [ ] `boot.ts` contains neither the string `react` nor the string `WebSocket`
- [ ] No test in this ticket reads or writes the global `localStorage`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
