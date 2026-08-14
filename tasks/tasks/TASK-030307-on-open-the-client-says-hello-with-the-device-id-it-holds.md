---
schema: 2
id: TASK-030307
title: On open the client says Hello with the device id it holds
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol, identity]
depends_on: [TASK-030306]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +43 passed \(43\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says hello with no device id on a first visit'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says hello with the device id it already holds'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing before the socket opens'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`openConnection` exists: it writes `Hello` the moment the socket opens, carrying the device id this
browser holds and the version this client speaks.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | create |
| `web-client/src/protocol/connection.test.ts` | create |
| `web-client/src/protocol/fake-socket.ts` | read — the double this test drives |

You do not need to open the other two imports. Their whole surface is:
`export const PROTOCOL_VERSION: ProtocolVersion` from `./version`,
`readDeviceId(storage: Storage): string | null` from `./device-id`, and
`encodeClientMessage(message: ClientMessage): string` from `./frames`.

## Scope

- `connection.ts` declares the module's public shape. Its three wire types come from the generated
  file and nowhere else:

  ```ts
  import type {
    ClientMessage,
    ProtocolError,
    ServerMessage,
  } from "./protocol.gen";

  export type ConnectionStatus =
    | { readonly kind: "connecting" }
    | { readonly kind: "ready"; readonly deviceId: string }
    | { readonly kind: "refused"; readonly error: ProtocolError }
    | { readonly kind: "outdated" };

  export interface ConnectionOptions {
    readonly socket: WebSocket;
    readonly storage: Storage;
    readonly onMessage: (message: ServerMessage) => void;
  }

  export interface Connection {
    readonly status: ConnectionStatus;
    send(message: ClientMessage): void;
    close(): void;
  }

  export function openConnection(options: ConnectionOptions): Connection;
  ```

  Declare all four `ConnectionStatus` variants now, even though this ticket only ever produces
  `connecting`: `TASK-030309` and `TASK-030310` fill in the rest, and a union that grows in three
  diffs is three chances to reshape it.
- `openConnection` sets `socket.onopen` before returning, and that handler sends exactly one frame:
  `encodeClientMessage({ type: "Hello", deviceId: readDeviceId(options.storage), protocolVersion: PROTOCOL_VERSION })`.
- `send(message)` writes `encodeClientMessage(message)` straight to the socket, with no queue. That
  is safe by construction and worth stating: the first frame this client ever sends is `Hello`, from
  `onopen`; every other outbound frame answers something the server sent, which can only have
  arrived over an open socket.
- `close()` calls `socket.close()`.
- `status` is a getter over a closure variable — `let status: ConnectionStatus = { kind:
  "connecting" }` and `get status() { return status; }` on the returned object. **`let`, not
  `const`**, even though nothing reassigns it in this ticket: `TASK-030309` and `TASK-030310` do,
  and a `const` here would make each of them rewrite a line this ticket wrote. Neither
  `eslint:recommended` nor `typescript-eslint`'s recommended set enables `prefer-const`, so this
  passes `npm run lint` as it stands.
- The **word "session" does not appear** in any name here. `ADR-0027` warns that `Session` already
  means a live socket on the server and that an auth session is a different thing with the same
  obvious name; the client is not going to be where those two get confused.

## Out of scope

- Anything inbound. `socket.onmessage` is not set by this ticket — `TASK-030308` sets it.
- `onclose` and `onerror`. Reconnect, backoff and resuming a seat are `STORY-0310`, and a close
  handler written here would be the first half of a retry loop nobody asked for.
- Persisting the device id. `TASK-030309`.
- Constructing the socket. It is passed in; `TASK-030311` constructs the real one.
- A singleton. `openConnection` is a factory, so `STORY-0304`'s store is free to decide where the
  one instance lives.

## Tests

`web-client/src/protocol/connection.test.ts`, describe block `"the connection"`. Six `it` blocks,
with `beforeEach(() => localStorage.clear())`:

| Test | Proves |
| --- | --- |
| `starts out connecting` | `openConnection(...).status` equals `{ kind: "connecting" }` |
| `sends nothing before the socket opens` | immediately after `openConnection(...)`, `socket.sent` is `[]` |
| `says hello with no device id on a first visit` | with empty storage, after `socket.open()`, `JSON.parse(socket.sent[0])` equals `{ type: "Hello", deviceId: null, protocolVersion: 2 }` |
| `says hello with the device id it already holds` | with `pd.deviceId` seeded as `"d-1"`, after `socket.open()`, the parsed frame's `deviceId` is `"d-1"` |
| `writes a client message to the socket` | `connection.send({ type: "CreateRoom" })` puts `'{"type":"CreateRoom"}'` on `socket.sent` |
| `closes the underlying socket` | `connection.close()` sets `socket.closed` to `true` |

Six tests. Thirty-seven exist, so the suite reports **43**.

`says hello with no device id on a first visit` asserts the **literal `2`**, not `PROTOCOL_VERSION`.
Asserting against the constant would make the test agree with any bump automatically; the literal
means a version move shows up as a red test that a human has to look at, which is what a wire change
should cost.

It also asserts `deviceId: null` is **present** in the parsed object, not merely absent —
`ignoreUnknownKeys = false` and required TypeScript fields (`ADR-0020`) mean the key must be on the
wire.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 43 passed (43)` | the six tests ran and nothing earlier was displaced |
| the three `--reporter=verbose` greps | the two identity cases and the "not before open" rule exist by name |
| `npm run check` | the new module typechecks under `strict` and passes the boundary guard from `TASK-030301` |

**Name the edit that makes each assertion red:**

1. Send `Hello` from `openConnection` instead of from `onopen` → `sends nothing before the socket
   opens` fails, `expected [ '{"type":"Hello"…' ] to equal []`. Revert.
2. Replace `readDeviceId(options.storage)` with `null` → `says hello with the device id it already
   holds` fails, `expected null to be "d-1"`. Revert.

Quote both in the PR. The second is the one that matters: without it the client would mint a new
profile on every visit, which is exactly the harm `ADR-0012` accepts responsibility for and
`ADR-0027` §5 calls the decisive case.

## Acceptance criteria

- [ ] `the connection > starts out connecting` passes
- [ ] `the connection > sends nothing before the socket opens` passes
- [ ] `the connection > says hello with no device id on a first visit` passes
- [ ] `the connection > says hello with the device id it already holds` passes
- [ ] `the connection > writes a client message to the socket` passes
- [ ] `the connection > closes the underlying socket` passes
- [ ] `npm run --silent test` reports `Tests  43 passed (43)`
- [ ] `connection.ts` sets no `onmessage`, no `onclose` and no `onerror`
- [ ] `connection.ts` contains no `JSON.stringify` — every outbound frame goes through `encodeClientMessage`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
