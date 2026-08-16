---
schema: 2
id: TASK-031003
title: A closed socket is reopened, on virtual time, when the backoff says so
type: task
status: done
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol, websocket, reconnect]
depends_on: [TASK-031002]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +289 passed \(289\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens one socket and no more while it stays open'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens nothing until the backoff delay has elapsed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'waits longer after each close, at the delays the backoff names'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'starts the backoff over once a Welcome arrives'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forwards the frames of whichever socket is live'
  - cd web-client && npm run check
---

## Goal

A `Connection` that outlives its socket: when the socket closes it opens another, after the delay
`retryDelayMillis` names, and hands the caller whatever the live socket says.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/reconnecting.ts` | create |
| `web-client/src/protocol/reconnecting.test.ts` | create |
| `web-client/src/protocol/connection.ts` | read — `Connection`, `ConnectionOptions`, `openConnection` |
| `web-client/src/protocol/fake-socket.ts` | read — the double the tests drive |
| `web-client/src/protocol/retry-delay.ts` | read — the delay |

## Scope

- It lives in `src/protocol/` because it names `WebSocket`, and
  `boundary.test.ts > finds no raw frame handled outside the protocol module` is what says so. No
  other directory is available to it.
- The module:

  ```ts
  export interface ReconnectingOptions {
    /** Constructs a fresh socket. Called once per attempt, never reused. */
    readonly openSocket: () => WebSocket;
    readonly storage: Storage;
    readonly onMessage: (message: ServerMessage) => void;
    /** A number in [0, 1) per attempt. Injected so a test hands it a value. */
    readonly jitter?: () => number;
  }

  export function openReconnectingConnection(
    options: ReconnectingOptions,
  ): Connection;
  ```

- Each attempt calls `openConnection({ socket, storage, onMessage: forward })`, so the `Hello`,
  the device id and the frame codec stay exactly where `STORY-0303` put them. This module adds
  **only** the socket lifecycle: nothing here decodes, encodes, or reads a wire field other than
  `message.type === "Welcome"`.
- `socket.onclose` is this module's — `openConnection` sets `onopen` and `onmessage` and nothing
  else, so there is no handler to preserve.
- On a close: wait `retryDelayMillis(failures, jitter())` on `setTimeout`, then attach a fresh
  socket, and count the failure. `jitter` defaults to `Math.random`.
- A `Welcome` resets `failures` to 0 — a handshake that completed is a connection that worked, and
  the next drop deserves a fast retry rather than yesterday's ceiling.
- `send`, `status` and `close` delegate to the current inner `Connection`. `close()` sets a
  `stopped` flag **before** delegating, so the close it causes schedules nothing.
- `send` is dropped while there is no socket to write to. An action taken while disconnected is not
  an action. Track that with one `live` flag: `true` when a socket is attached, `false` from its
  `onclose`. The narrower window between constructing a socket and its `open` event is left as it
  is — today's client has the same gap and no ticket in this story widens it.

## Out of scope

- The stale-socket guard (`ADR-0018`) — `TASK-031004`. Until it lands, a close is a close.
- Ending the loop on a version mismatch — `TASK-031005`.
- Sending `JoinRoom` on the new socket. That is a boot reaction (`ADR-0032`) and belongs to
  `TASK-031007`/`TASK-031008`; this module never composes a `ClientMessage`.
- Changing `connection.ts`, `index.ts` or `fake-socket.ts`. `TASK-031006` wires this in.

## Tests

`web-client/src/protocol/reconnecting.test.ts`, describe block `"the reconnecting connection"`.
Virtual time throughout: `vi.useFakeTimers()` in `beforeEach`, `vi.useRealTimers()` in `afterEach`,
`vi.advanceTimersByTime(...)` to move it. **No test sleeps on a real clock** — `TASK-031013` makes
that a check rather than a promise.

Drive it with an `openSocket` that pushes a fresh `FakeSocket` onto an array and returns it, and a
`jitter` of `() => 0`, so every delay is the low edge `retryDelayMillis` names: 250, 500, 1000, …

| Test | Proves |
| --- | --- |
| `opens one socket and no more while it stays open` | after opening and a `Welcome`, advancing an hour of virtual time leaves `sockets.length === 1` |
| `opens nothing until the backoff delay has elapsed` | close the socket; at 249 ms there is still one socket, at 250 ms there are two. The boundary on both sides, so a delay of zero fails it as loudly as a delay of a minute |
| `waits longer after each close, at the delays the backoff names` | close three times in a row with no `Welcome` between them; the sockets appear at 250 ms, then 500 ms, then 1000 ms after their respective closes, and at 1 ms less than each, they have not |
| `starts the backoff over once a Welcome arrives` | close (250 ms), reopen, `Welcome`, close again → the next socket appears at **250** ms, not 500 |
| `forwards the frames of whichever socket is live` | a `RoomJoined` received on socket 2 reaches `onMessage`, and the two frames collected across the two sockets are in the order they were received |

```ts
it("opens nothing until the backoff delay has elapsed", () => {
  const { sockets } = openOverFakeSockets();

  sockets[0].close();

  vi.advanceTimersByTime(249);
  expect(sockets).toHaveLength(1);

  vi.advanceTimersByTime(1);
  expect(sockets).toHaveLength(2);
});
```

Five tests added. Two hundred and eighty-four exist, so the suite reports **289**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 289 passed (289)` | five ran and nothing else moved |
| the five `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks — `Connection` is satisfied, including `status`'s getter |

**Name the edit that makes each assertion red:**

1. Reopen from `onclose` directly instead of through `setTimeout` → `opens nothing until the backoff
   delay has elapsed` fails at its 249 ms assertion. Revert.
2. Pass a constant `0` instead of `failures` to `retryDelayMillis` → `waits longer after each close,
   at the delays the backoff names` fails at its second close. Revert.
3. Delete the `failures = 0` on `Welcome` → `starts the backoff over once a Welcome arrives` fails
   with a socket that arrives at 500 ms. Revert.

Quote all three in the PR, and say in the PR body that no test in the file installs a real timer.

## Acceptance criteria

- [ ] `the reconnecting connection > opens one socket and no more while it stays open` passes
- [ ] `the reconnecting connection > opens nothing until the backoff delay has elapsed` passes
- [ ] `the reconnecting connection > waits longer after each close, at the delays the backoff names` passes
- [ ] `the reconnecting connection > starts the backoff over once a Welcome arrives` passes
- [ ] `the reconnecting connection > forwards the frames of whichever socket is live` passes
- [ ] `reconnecting.ts` calls `openConnection` and contains no `JSON.parse`, no `JSON.stringify` and
      no `encodeClientMessage`
- [ ] `reconnecting.test.ts` contains `vi.useFakeTimers()` and no `await new Promise`
- [ ] `connection.ts`, `index.ts` and `fake-socket.ts` are byte-identical to what they were
- [ ] `npm run --silent test` reports `Tests  289 passed (289)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
