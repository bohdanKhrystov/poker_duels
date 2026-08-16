---
schema: 2
id: TASK-031011
title: The reopened socket says Hello, then rejoins, once each
type: task
status: done
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, store, websocket, reconnect]
depends_on: [TASK-031010]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +312 passed \(312\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says Hello then JoinRoom, in that order and once each, on the socket it reopened'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'carries the device id the first Welcome issued into the second Hello'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'rejoins the room the server seated it in, though the tab was opened without a code'
  - cd web-client && npm run check
---

## Goal

The whole recipe, driven end to end through the real boot wiring: a socket drops, another opens,
and it says `Hello` and then `JoinRoom` for the seat this tab already held — with nothing added to
the wire and nobody asked to click.

## Files

`files_touched` counts the create/modify rows only. Everything below it is context.

| File | Action |
| --- | --- |
| `web-client/src/store/reconnect.test.tsx` | create |
| `web-client/src/store/boot.ts` | read — the reaction under test |
| `web-client/src/store/boot.test.ts` | read — `inMemoryStorage`, `WELCOME`, `sentFrames` |
| `web-client/src/protocol/reconnecting.ts` | read — the options |
| `web-client/src/protocol/fake-socket.ts` | read — the double |

## Scope

- One new test file wiring `bootDuelClient` to `openReconnectingConnection` over `FakeSocket`s, on
  virtual time. It is a `.test.tsx` on purpose: `store/one-connection.test.ts` forbids
  `bootDuelClient(` in any **shipped** file, and a shared non-test fixture module would be one, so
  the helper lives inside the test file. `TASK-031012` adds rendering tests to the same file.
- The helper, which the next ticket also uses:

  ```tsx
  function reconnectingClient(joinRoomCode: string | null = null) {
    const sockets: FakeSocket[] = [];
    const storage = inMemoryStorage();
    const client = bootDuelClient({
      connect: (onMessage) =>
        openReconnectingConnection({
          openSocket: () => {
            const socket = new FakeSocket();
            sockets.push(socket);
            return socket.asWebSocket();
          },
          storage,
          onMessage,
          jitter: () => 0,
        }),
      joinRoomCode,
      storage,
    });
    return { sockets, client, storage };
  }
  ```

  One `storage` for the whole tab, exactly as the browser has one: the device id the first `Welcome`
  writes is the device id the second `Hello` reads.
- `vi.useFakeTimers()` in `beforeEach`, `vi.useRealTimers()` in `afterEach`. **No real sleep.**
- Copy `inMemoryStorage()` and `WELCOME` from `boot.test.ts`. That duplication is already four files
  deep and `DEC-032` is the open question about it; unpicking it here would put this ticket over
  its file budget for no behaviour.
- Never write the string `WebSocket` on its own in this file — `boundary.test.ts > finds no raw
  frame handled outside the protocol module` walks every file outside `src/protocol/`, tests
  included. `socket.asWebSocket()` does not match it; a `: WebSocket` annotation does.

## Out of scope

- Rendering anything — `TASK-031012`.
- Any change to `boot.ts`, `reconnecting.ts` or the server. If this ticket finds it needs a frame
  the wire does not carry, that is a `DEC`, not an edit: `EPIC-03`'s rule is that a protocol or
  server change leaves this epic.
- A second tab. `ADR-0018` is explicit that two tabs on one device id take the seat from each
  other, and that is accepted in v0.1.

## Tests

`web-client/src/store/reconnect.test.tsx`, describe block `"a tab whose socket dropped"`.

| Test | Proves |
| --- | --- |
| `says Hello then JoinRoom, in that order and once each, on the socket it reopened` | socket 1: open, `Welcome`, `RoomJoined ABCDEFGH`; close; advance 250 ms; socket 2: open, `Welcome`. Socket 2's `sent`, decoded, is **exactly** `[{ type: "Hello", … }, { type: "JoinRoom", code: "ABCDEFGH" }]` — the order, the count and the code in one assertion on the whole array |
| `carries the device id the first Welcome issued into the second Hello` | the first `Welcome` issues `d-7`; socket 1's `Hello` carried `deviceId: null` and socket 2's carries `"d-7"`. Two different values off one field, so a `Hello` that hardcoded either fails |
| `rejoins the room the server seated it in, though the tab was opened without a code` | booted with `joinRoomCode: null` — the host's case, whose URL never carried a code — and socket 2 still sends `JoinRoom ABCDEFGH`, the code the server named in `RoomJoined` |

```tsx
it("says Hello then JoinRoom, in that order and once each, on the socket it reopened", () => {
  const { sockets } = reconnectingClient("ABCDEFGH");

  sockets[0].open();
  sockets[0].receive(WELCOME);
  sockets[0].receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
  sockets[0].close();

  vi.advanceTimersByTime(250);
  sockets[1].open();
  sockets[1].receive(WELCOME);

  expect(sockets[1].sent.map((frame) => JSON.parse(frame))).toEqual([
    { type: "Hello", deviceId: "d-1", protocolVersion: PROTOCOL_VERSION },
    { type: "JoinRoom", code: "ABCDEFGH" },
  ]);
});
```

Three tests added. Three hundred and nine exist, so the suite reports **312**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 312 passed (312)` | three ran and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks and lints |

**Name the edit that makes each assertion red:**

1. In `boot.ts`, guard the `Welcome` branch so it fires once → `says Hello then JoinRoom, in that
   order and once each, on the socket it reopened` fails with a one-frame array. Revert.
2. In `reconnecting.ts`, give each attempt a fresh `Storage` instead of `options.storage` →
   `carries the device id the first Welcome issued into the second Hello` fails with `null`.
   Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `a tab whose socket dropped > says Hello then JoinRoom, in that order and once each, on the socket it reopened` passes
- [ ] `a tab whose socket dropped > carries the device id the first Welcome issued into the second Hello` passes
- [ ] `a tab whose socket dropped > rejoins the room the server seated it in, though the tab was opened without a code` passes
- [ ] `reconnect.test.tsx` contains `vi.useFakeTimers()`, no `await new Promise`, and no bare
      `WebSocket`
- [ ] No file outside `web-client/src/store/reconnect.test.tsx` differs from what it was
- [ ] `npm run --silent test` reports `Tests  312 passed (312)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
