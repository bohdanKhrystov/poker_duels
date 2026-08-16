---
schema: 2
id: TASK-031006
title: The tab's one connection is the one that comes back
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, protocol, websocket, reconnect]
depends_on: [TASK-031005]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +298 passed \(298\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens the socket again, at the same url, after it closes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says Hello on the socket it reopened, with the device id it holds'
  - cd web-client && npm run check
---

## Goal

`connectToDuelServer` — the one call `main.tsx` makes — returns a connection that reopens its
socket. Nothing outside `src/protocol/` learns that it does.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/index.ts` | modify — the body of `connectToDuelServer`, and one export |
| `web-client/src/protocol/index.test.ts` | modify — two tests added, none changed |
| `web-client/src/protocol/reconnecting.ts` | read — the options it takes |

## Scope

- The body becomes:

  ```ts
  export function connectToDuelServer(
    onMessage: (message: ServerMessage) => void,
  ): Connection {
    return openReconnectingConnection({
      // Re-read on every attempt rather than captured once: a socket opened an
      // hour later should still go where this page came from.
      openSocket: () => new WebSocket(socketUrl(window.location)),
      storage: localStorage,
      onMessage,
    });
  }
  ```

- `index.ts` also re-exports `openReconnectingConnection` and its options type beside
  `openConnection`, in the order `npm run format` leaves them.
- `openConnection` stays exported. It is what `boot.test.ts` and `boot-strict-mode.test.tsx` drive,
  and nothing in this story replaces it.
- The return type stays `Connection`. `bootDuelClient`, `main.tsx` and every screen are untouched
  and unaware.

## Out of scope

- `main.tsx` — it already calls this and needs no edit here. `TASK-031007` is the one ticket in this
  story that touches it.
- The rejoin. This ticket restores the *socket*; `TASK-031008` restores the *seat*.
- The three tests already in `index.test.ts`. All three still hold: one socket is constructed at the
  `/ws` url, the device id comes from the stubbed global storage, and the status before any frame
  is `{ kind: "connecting" }` — the reconnecting connection delegates `status` to the live inner
  connection, so it reads the same.

## Tests

`web-client/src/protocol/index.test.ts`, describe block `"the duel server connection"`. It stubs the
`WebSocket` global already; the two new tests give that stub a **factory** returning a fresh
`FakeSocket` each call, and add `vi.useFakeTimers()` / `vi.useRealTimers()` around themselves.

| Test | Proves |
| --- | --- |
| `opens the socket again, at the same url, after it closes` | close the first socket, advance 500 ms of virtual time → the `WebSocket` constructor has been called **twice**, and both calls carry `ws://${window.location.host}/ws` |
| `says Hello on the socket it reopened, with the device id it holds` | with `pd.deviceId` set to `d-9` before booting, the *second* socket's first frame is a `Hello` carrying `d-9` — the recipe restarts from the top, and it restarts with the identity this browser already has |

```ts
it("says Hello on the socket it reopened, with the device id it holds", () => {
  localStorage.setItem("pd.deviceId", "d-9");
  const sockets: FakeSocket[] = [];
  vi.stubGlobal(
    "WebSocket",
    vi.fn(() => {
      const socket = new FakeSocket();
      sockets.push(socket);
      return socket.asWebSocket();
    }),
  );

  connectToDuelServer(() => {});
  sockets[0].close();
  vi.advanceTimersByTime(500);
  sockets[1].open();

  expect(JSON.parse(sockets[1].sent[0])).toMatchObject({
    type: "Hello",
    deviceId: "d-9",
  });
});
```

500 ms rather than 250: production jitter is `Math.random`, so the delay is somewhere in
`[250, 500]` and only the upper bound is safe to advance past. This is the one place in the story
where the jitter is not injected, because it is the one place the production wiring is what is under
test.

Two tests added, the three before them unchanged. Two hundred and ninety-six exist, so the suite
reports **298**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 298 passed (298)` | two ran, the three before them still do, and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks — the return type is still `Connection` |

**Name the edit that makes each assertion red:**

1. Put `openConnection({ socket: new WebSocket(...), ... })` back → `opens the socket again, at the
   same url, after it closes` fails with one construction. Revert.
2. Hoist `const socket = new WebSocket(socketUrl(window.location))` out of `openSocket` and return
   the same one every time → the second socket is the first, so `says Hello on the socket it
   reopened` finds a second `Hello` on socket 1 and `sockets[1]` is `undefined`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the duel server connection > opens the socket again, at the same url, after it closes` passes
- [ ] `the duel server connection > says Hello on the socket it reopened, with the device id it holds` passes
- [ ] The three tests already in `index.test.ts` are byte-identical, and no assertion in them is weakened
- [ ] `store/one-connection.test.ts` still passes unchanged — `connectToDuelServer` is still named
      by `main.tsx` and by no other shipped file outside `src/protocol/`
- [ ] `npm run --silent test` reports `Tests  298 passed (298)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
