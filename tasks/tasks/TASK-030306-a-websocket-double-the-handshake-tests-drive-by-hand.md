---
schema: 2
id: TASK-030306
title: A WebSocket double the handshake tests drive by hand
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, protocol, testing]
depends_on: [TASK-030305]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +37 passed \(37\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records every frame the client sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a frame to the message handler'
  - grep -cF 'new WebSocket' web-client/src/protocol/fake-socket.ts | grep -qx 0
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

Every handshake test in this story drives one hand-controlled double, so no test in `EPIC-03` opens
a socket, binds a port, or needs a server running.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/fake-socket.ts` | create |
| `web-client/src/protocol/fake-socket.test.ts` | create |

## Scope

- `fake-socket.ts` exports one class:

  ```ts
  /**
   * A `WebSocket` double: no network, no port, no timer. A test calls `open`,
   * `receive` and `close` to move it, and reads `sent` to see what the client
   * wrote. `STORY-0301` set the precedent that no client test reaches the
   * network and `EPIC-03` keeps it.
   */
  export class FakeSocket {
    readonly sent: string[] = [];
    closed = false;

    onopen: (() => void) | null = null;
    onmessage: ((event: { data: unknown }) => void) | null = null;
    onclose: (() => void) | null = null;
    onerror: (() => void) | null = null;

    send(data: string): void;
    close(): void;

    /** Drive it: the socket finished connecting. */
    open(): void;

    /** Drive it: the server sent this frame. */
    receive(data: unknown): void;

    /** The one cast in this module, at the seam where the double stands in. */
    asWebSocket(): WebSocket;
  }
  ```

- `send` pushes onto `sent`. `close` sets `closed` to `true` and then calls `onclose`.
- `open` calls `onopen` if one is set; `receive(data)` calls `onmessage` with `{ data }`. Both are
  no-ops when no handler is set — a test that receives before the client is listening should not
  throw, it should observe nothing.
- `asWebSocket()` is `return this as unknown as WebSocket;`. It is the only cast, it is inside
  `src/protocol/`, and it is what lets the production type stay `WebSocket` while the tests stay
  offline.
- The file lives in `src/`, not in a test directory, because `TASK-030311`'s test needs it too and
  nothing imports it from application code, so it is tree-shaken out of the bundle.

## Out of scope

- Modelling `readyState`, buffering, back-pressure, binary frames or close codes. Nothing in this
  story reads them. `STORY-0310` may extend the double when reconnect needs a close code.
- `addEventListener`. The connection uses the `on*` properties, which is what the double models.
- Any real `WebSocket`. The `verify` block greps that this file never constructs one.

## Tests

`web-client/src/protocol/fake-socket.test.ts`, describe block `"the fake socket"`. Four `it` blocks:

| Test | Proves |
| --- | --- |
| `records every frame the client sent` | after `send("a")` and `send("b")`, `sent` is `["a", "b"]` |
| `hands a frame to the message handler` | with `onmessage` set, `receive("{}")` calls it once with `{ data: "{}" }` |
| `runs the open handler when it is opened` | with `onopen` set, `open()` calls it once |
| `marks itself closed and tells the close handler` | `close()` sets `closed` to `true` and calls `onclose` once |

Four tests. Thirty-three exist, so the suite reports **37**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 37 passed (37)` | the four tests ran and nothing earlier was displaced |
| the two `--reporter=verbose` greps | the two behaviours every later test stands on exist by name |
| `grep -c 'new WebSocket' … \| grep -qx 0` | the double constructs no real socket. The count must be exactly `0`; a plain `grep -v` would pass on any file with a second line, which is why it is written this way (`TASK-030208`'s idiom) |
| `npm run check` | the double typechecks, lints and formats like everything else |

**Name the edit that makes each assertion red:**

1. Make `send` a no-op → `records every frame the client sent` fails, `expected [] to equal
   ["a","b"]`. Revert.
2. Make `receive` call `onmessage` with `data` rather than `{ data }` →
   `hands a frame to the message handler` fails on the argument. Revert.

Quote both in the PR. A double with no test of its own is a place where a silent no-op makes every
downstream assertion vacuous, which is the failure this story is most exposed to.

## Acceptance criteria

- [ ] `the fake socket > records every frame the client sent` passes
- [ ] `the fake socket > hands a frame to the message handler` passes
- [ ] `the fake socket > runs the open handler when it is opened` passes
- [ ] `the fake socket > marks itself closed and tells the close handler` passes
- [ ] `npm run --silent test` reports `Tests  37 passed (37)`
- [ ] `fake-socket.ts` contains no `new WebSocket`, no `fetch`, no port number
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
