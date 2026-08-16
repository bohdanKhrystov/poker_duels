---
schema: 2
id: TASK-031014
title: The reconnecting connection's own send, status and close are proven
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, protocol, reconnect, tests]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +319 passed \(319\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes nothing to the socket that has closed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'closes the socket it is holding and opens no other'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reports the status of the socket it opened last'
  - cd web-client && npm run check
---

## Goal

The three members `openReconnectingConnection` returns — `send`, `status` and `close` — are held by
tests that fail when they are wrong, so a no-op `close()`, an ungated `send` and a `status` that
answers a stale socket all go red.

## Why this exists

`TASK-031003` shipped all three and its test list named none of them. Two later tests came close
and neither closes the hole:

- `drops a send made while no socket is open` asserts only what reached **socket 1**. Delete
  `if (!live) return` from `send` and the frame lands on the closed **socket 0**, which the test
  never reads — it stays green. The guard it is named for is not exercised.
- Nothing anywhere calls `connection.close()` or reads `connection.status` on a reconnecting
  connection. `close()` could return without doing anything, and `status` could answer a socket the
  tab replaced an hour ago, and the suite would be silent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/reconnecting.test.ts` | modify — three tests added, none changed |
| `web-client/src/protocol/reconnecting.ts` | read — `live`, `stopped`, `generation`, and the order inside `close` |
| `web-client/src/protocol/fake-socket.ts` | read — `closed`, `sent`, `open()`, `close()` |
| `web-client/src/protocol/connection.ts` | read — what `status` is for one socket |

## Scope

- Three tests, added to the existing `describe("the reconnecting connection")` block, using the
  file's own `openOverFakeSockets()` and `welcomeFrame()` helpers. No new helper, no new file, no
  production change.
- The virtual clock is already installed by this file's `beforeEach`; the new tests advance it and
  never sleep on a real one.
- **No existing test is edited.** `drops a send made while no socket is open` keeps its name and its
  assertions; the new send test is the one that reads the socket the frame would actually reach.

## Out of scope

- Changing `reconnecting.ts`. Every behaviour asserted here is behaviour it already has; if one of
  these tests is red on first run, the ticket has found a defect and it is reported, not fixed here.
- `openConnection`'s own `status` and `close` — `connection.test.ts` already covers those, and this
  ticket is about the wrapper that delegates to them.
- Anything about `Connection` being disposed on unmount. No caller closes this connection today.

## Tests

`web-client/src/protocol/reconnecting.test.ts`, in the existing describe block
`"the reconnecting connection"`.

| Test | Proves |
| --- | --- |
| `writes nothing to the socket that has closed` | after the live socket closes, `send` writes to **no** socket at all — asserted on `sockets[0].sent`, the one an ungated send reaches |
| `closes the socket it is holding and opens no other` | `close()` really closes (`sockets[0].closed`) **and** starts no retry after an hour of virtual time — the two halves that a no-op and a mis-ordered `close` fail separately |
| `reports the status of the socket it opened last` | `status` after two reconnections, with **two distinct device ids**, so a getter answering a constant or a stale socket cannot pass |

```ts
it("writes nothing to the socket that has closed", () => {
  const { sockets, connection } = openOverFakeSockets();

  sockets[0].open(); // the Hello goes out
  sockets[0].close(); // and the connection is no longer live

  connection.send({ type: "CreateRoom" });

  // The socket that just closed is the one an ungated send reaches, so it is
  // the one that has to be read. Nothing has been retried yet, so it is still
  // the only socket there is.
  expect(
    sockets[0].sent.map((frame) => (JSON.parse(frame) as { type: string }).type),
  ).toEqual(["Hello"]);
  expect(sockets).toHaveLength(1);
});

it("closes the socket it is holding and opens no other", () => {
  const { sockets, connection } = openOverFakeSockets();

  sockets[0].open();
  connection.close();

  expect(sockets[0].closed).toBe(true);

  vi.advanceTimersByTime(60 * 60 * 1000);
  expect(sockets).toHaveLength(1);
});
```

The third asserts `connection.status` is `{ kind: "ready", deviceId: "device-1" }` after socket 0
welcomes `device-1`, then — after that socket closes and 250 ms of virtual time opens socket 1 —
`{ kind: "ready", deviceId: "device-2" }` when socket 1 welcomes `device-2`. Two ids, because a
value asserted only at one fixture value cannot be told from a hardcoded constant.

Three tests added. Three hundred and sixteen exist, so the suite reports **319**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 319 passed (319)` | three ran and nothing else moved |
| the three `--reporter=verbose` greps | all three names exist |
| `npm run check` | typechecks, lints and is formatted |

**Name the edit that makes each assertion red** — run each, quote the failure in the PR, revert:

1. Delete `if (!live) return;` from `send` → `writes nothing to the socket that has closed` fails
   with `["Hello", "CreateRoom"]`. The older `drops a send made while no socket is open` **stays
   green**, which is the reason this ticket exists — say so in the PR.
2. Make `close()` a no-op (`{}`) → `closes the socket it is holding and opens no other` fails on
   `sockets[0].closed`.
3. Swap the two lines inside `close()` so `current.close()` runs before `stopped = true` → the same
   test fails on `toHaveLength(1)`, because the close it caused schedules a retry that then fires.
4. Make `status` return `{ kind: "connecting" }` → `reports the status of the socket it opened last`
   fails at its first assertion.

## Acceptance criteria

- [ ] `the reconnecting connection > writes nothing to the socket that has closed` passes
- [ ] `the reconnecting connection > closes the socket it is holding and opens no other` passes
- [ ] `the reconnecting connection > reports the status of the socket it opened last` passes
- [ ] The status test asserts two **different** device ids, from two different sockets
- [ ] No existing test in the file is renamed, removed, or has an assertion changed
- [ ] `web-client/src/protocol/reconnecting.ts` is byte-identical to what it was
- [ ] `npm run --silent test` reports `Tests  319 passed (319)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
