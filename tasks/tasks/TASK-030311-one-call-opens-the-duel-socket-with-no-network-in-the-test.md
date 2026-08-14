---
schema: 2
id: TASK-030311
title: One call opens the duel socket, and the test that proves it touches no network
type: task
status: done
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030310]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +59 passed \(59\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the socket at /ws on this page'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands the connection the storage the profile endpoint reads'
  - grep -qF 'export type * from "./protocol.gen"' web-client/src/protocol/index.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`connectToDuelServer(onMessage)` is the whole public entrance to the wire: it opens `/ws` on this
page's origin, hands the connection this browser's storage, and is proven by a test that constructs
no real socket.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/index.ts` | create |
| `web-client/src/protocol/index.test.ts` | create |
| `web-client/src/protocol/fake-socket.ts` | read — `FakeSocket`, `asWebSocket()` |

## Scope

- `index.ts` is the module's only public surface. It both imports what it uses and re-exports what
  the rest of the client needs:

  ```ts
  import { openConnection, type Connection } from "./connection";
  import { socketUrl } from "./socket-url";
  import type { ServerMessage } from "./protocol.gen";

  export type * from "./protocol.gen";
  export { PROTOCOL_VERSION } from "./version";
  export { DEVICE_ID_STORAGE_KEY, readDeviceId } from "./device-id";
  export { socketUrl } from "./socket-url";
  export { openConnection } from "./connection";
  export type { Connection, ConnectionOptions, ConnectionStatus } from "./connection";

  /** Opens this client's one socket to the duel server. */
  export function connectToDuelServer(
    onMessage: (message: ServerMessage) => void,
  ): Connection {
    return openConnection({
      socket: new WebSocket(socketUrl(window.location)),
      storage: localStorage,
      onMessage,
    });
  }
  ```

  `export type *` rather than `export *`: the generated file has no runtime exports, and
  `verbatimModuleSyntax` wants the intent stated.
- This is the only `new WebSocket` and the only `localStorage` in the client. Everything downstream
  — the store in `STORY-0304`, every screen after it — imports from `src/protocol` and never from a
  file inside it.
- `boundary.ts` and `fake-socket.ts` are deliberately **not** re-exported: one is a check, the other
  is a double, and neither is part of the wire.
- Run `npm run format` before committing.

## Out of scope

- A singleton, a React context, a provider. `connectToDuelServer` is a function; where the one
  instance lives is `STORY-0304`'s to decide.
- Calling it from `App.tsx` or `main.tsx`. Nothing renders from a frame yet, and mounting a live
  socket into the app root would put a connection attempt into every existing component test.
- Reconnect on close — `STORY-0310`.

## Tests

`web-client/src/protocol/index.test.ts`, describe block `"the duel server connection"`. Three `it`
blocks, with `beforeEach(() => localStorage.clear())` and `afterEach(() => vi.unstubAllGlobals())`:

Each test stubs the constructor rather than the instance:

```ts
const socket = new FakeSocket();
const constructor = vi.fn(() => socket.asWebSocket());
vi.stubGlobal("WebSocket", constructor);
```

| Test | Proves |
| --- | --- |
| `opens the socket at /ws on this page` | `constructor` was called once, with `` `ws://${window.location.host}/ws` `` |
| `hands the connection the storage the profile endpoint reads` | with `pd.deviceId` seeded as `"d-9"`, after `socket.open()`, the first frame's `deviceId` is `"d-9"` |
| `hands decoded frames to the listener it was given` | `socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}')` reaches the callback passed to `connectToDuelServer` |

Three tests. Fifty-six exist, so the suite reports **59**.

The URL is asserted against `window.location.host` rather than a hard-coded `localhost:3000`, so the
assertion says *"this page's own host"* — which is the actual requirement — instead of pinning
whatever URL the jsdom environment happens to default to.

The test file imports `Connection` and `ServerMessage` **from `./index`**, not from `./connection`
or `./protocol.gen`, and uses both in its own declarations. That is what proves the re-export: the
types are erased at runtime, so `tsc` is the only thing that can observe them, and deleting the
`export type *` line breaks `npm run typecheck`.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 59 passed (59)` | the three tests ran and nothing earlier was displaced |
| the two `--reporter=verbose` greps | the URL and the storage wiring exist by name |
| `grep 'export type * from "./protocol.gen"'` | the wire types are re-exported by path, so downstream code has one import specifier and no reason to reach inside |
| `npm run check` | typecheck, lint, format and the whole suite together |

`npm run build` is deliberately **not** in the `verify` block. Nothing reachable from `index.html`
imports this module yet, so Vite would not bundle a byte of it and a green build would prove
nothing about this ticket. `STORY-0304` is where the module first enters the graph and where a
bundle assertion first has a subject.

**Name the edit that makes each assertion red:**

1. Pass `sessionStorage` instead of `localStorage` → `hands the connection the storage the profile
   endpoint reads` fails, `expected null to be "d-9"`. Revert.
2. Construct `new WebSocket("/ws")` → `opens the socket at /ws on this page` fails, naming the
   relative path against the expected `ws://…` URL. Revert.
3. Delete `export type * from "./protocol.gen"` → `npm run typecheck` fails on the test file's
   `ServerMessage` import. Revert.

Quote all three in the PR. Note for the reviewer: no test in this file — or anywhere in `EPIC-03` —
constructs a real `WebSocket`; the global is stubbed before the call and unstubbed after it.

## Acceptance criteria

- [ ] `the duel server connection > opens the socket at /ws on this page` passes
- [ ] `the duel server connection > hands the connection the storage the profile endpoint reads` passes
- [ ] `the duel server connection > hands decoded frames to the listener it was given` passes
- [ ] `npm run --silent test` reports `Tests  59 passed (59)`
- [ ] `index.ts` re-exports no member of `boundary.ts` and none of `fake-socket.ts`
- [ ] `App.tsx` and `main.tsx` are unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
