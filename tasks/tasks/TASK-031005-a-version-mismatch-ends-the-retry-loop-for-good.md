---
schema: 2
id: TASK-031005
title: A version mismatch ends the retry loop for good
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, protocol, websocket, reconnect]
depends_on: [TASK-031004]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +296 passed \(296\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens no further socket once the Welcome named another protocol version'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'opens no further socket after a VERSION_MISMATCH failure'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps retrying after a failure that is not a version mismatch'
  - cd web-client && npm run check
---

## Goal

A server that refused this protocol version will refuse it identically forever, so the loop stops.
Every other refusal leaves it running.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/reconnecting.ts` | modify — three lines in `forward` |
| `web-client/src/protocol/reconnecting.test.ts` | modify — three tests added, none changed |
| `web-client/src/protocol/connection.ts` | read — where `outdated` is decided |

## Scope

- The decision is already made once, in `openConnection`: a `Welcome` carrying another
  `protocolVersion` and a `Failure` carrying `VERSION_MISMATCH` both leave `status.kind` as
  `"outdated"`. This module reads that rather than re-deciding it, so there is one place that knows
  what a version mismatch is:

  ```ts
  function forward(message: ServerMessage): void {
    if (message.type === "Welcome") failures = 0;
    // openConnection has already refused to send on this socket. Reopening
    // would only reach the same refusal, more slowly. It reads the status
    // rather than re-testing the version, so there is one definition of
    // "outdated" in the client.
    if (current !== null && current.status.kind === "outdated") stopped = true;
    options.onMessage(message);
  }
  ```

  `openConnection` sets its status before it calls `onMessage`, so the status is settled by the
  time this reads it.
- `stopped` is the flag `TASK-031003` already checks before scheduling and before attaching. No new
  branch is needed anywhere else.
- `stopped` is never cleared. There is no path back: `PROTOCOL_VERSION` is a constant in the bundle
  and a reload is the only way to get a different one.

## Out of scope

- Showing the player anything about it. The store already records the `Failure` as `refusal`
  (`TASK-030502`), and what the screen says about an outdated client is not this story's.
- `UNKNOWN_ROOM`. That refusal is about a *room*, not about this socket, and it is
  `TASK-031010`'s — the socket stays and keeps reconnecting; what stops is resuming into a room
  that is gone.
- Closing the socket. `openConnection` deliberately leaves a refused socket open
  (`TASK-030310`); this changes nothing about that.

## Tests

`web-client/src/protocol/reconnecting.test.ts`, same describe block, same fixture, same virtual
time.

| Test | Proves |
| --- | --- |
| `opens no further socket once the Welcome named another protocol version` | a `Welcome` with `protocolVersion: PROTOCOL_VERSION + 1`, then a close, then an hour of virtual time → still one socket |
| `opens no further socket after a VERSION_MISMATCH failure` | `{"type":"Failure","error":"VERSION_MISMATCH"}`, then a close, then an hour → still one socket |
| `keeps retrying after a failure that is not a version mismatch` | `{"type":"Failure","error":"UNKNOWN_ROOM"}`, then a close, then 250 ms → **two** sockets. Named as the third case on purpose: a `stopped` set on any `Failure` passes the two above and fails this one |

```ts
it("keeps retrying after a failure that is not a version mismatch", () => {
  const { sockets } = openOverFakeSockets();

  sockets[0].open();
  sockets[0].receive('{"type":"Failure","error":"UNKNOWN_ROOM"}');
  sockets[0].close();
  vi.advanceTimersByTime(250);

  expect(sockets).toHaveLength(2);
});
```

Three tests added, none of the nine changed. Two hundred and ninety-three exist, so the suite
reports **296**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 296 passed (296)` | three ran, the nine before them still do, and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks and lints |

**Name the edit that makes each assertion red:**

1. Delete the `status.kind === "outdated"` line → both `opens no further socket …` tests fail with
   a second socket. Revert.
2. Replace it with `if (message.type === "Failure") stopped = true;` → `keeps retrying after a
   failure that is not a version mismatch` fails with one socket. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the reconnecting connection > opens no further socket once the Welcome named another protocol version` passes
- [ ] `the reconnecting connection > opens no further socket after a VERSION_MISMATCH failure` passes
- [ ] `the reconnecting connection > keeps retrying after a failure that is not a version mismatch` passes
- [ ] `reconnecting.ts` does not compare `protocolVersion` itself and names `PROTOCOL_VERSION` nowhere
- [ ] The nine tests before this ticket are byte-identical, and no assertion in them is weakened
- [ ] `npm run --silent test` reports `Tests  296 passed (296)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
