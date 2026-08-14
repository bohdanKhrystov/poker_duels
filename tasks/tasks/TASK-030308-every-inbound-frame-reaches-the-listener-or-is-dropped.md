---
schema: 2
id: TASK-030308
title: Every inbound frame reaches the listener, and an unreadable one is logged and dropped
type: task
status: ready
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030307]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +47 passed \(47\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'hands a decoded frame to the listener'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'logs and drops a frame it cannot read'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'handles the next valid frame after a dropped one'
  - grep -qF 'console.warn' web-client/src/protocol/connection.ts
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

The connection listens: every frame the codec can read reaches `onMessage`, and everything else is
logged once and dropped without reaching a render.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | modify |
| `web-client/src/protocol/connection.test.ts` | modify |
| `web-client/src/protocol/frames.ts` | read — `decodeServerMessage(data: unknown): ServerMessage \| null` |

## Scope

- `openConnection` sets `socket.onmessage` before returning:

  ```ts
  socket.onmessage = (event) => {
    const message = decodeServerMessage(event.data);
    if (message === null) {
      // A frame we cannot read is not an error to raise into a render: the next
      // Snapshot re-establishes the truth, which is why the server sends one
      // after every transition.
      console.warn("protocol: dropped an unreadable frame", event.data);
      return;
    }
    options.onMessage(message);
  };
  ```

- **Every** decoded message goes to `onMessage`, with no filtering and no `switch`. `Welcome` and
  `Failure` get extra treatment in `TASK-030309` and `TASK-030310`, but they are forwarded as well
  as interpreted — a store that is told about every frame is a store that needs no second channel.
- Nothing here enumerates variants. When `ADR-0028` adds `OpponentPresence` and
  `ActedForAbsentSeat`, this function is unchanged; only `frames.ts`'s table moves.
- `event.data` is passed to the codec untouched: the codec owns what a frame may be, including a
  binary one.

## Out of scope

- Interpreting `Welcome` — `TASK-030309` — or `Failure` — `TASK-030310`.
- Any exhaustive `switch` over `ServerMessage`, now or later in this story. The protocol is not
  closed; two variants are already decided and unbuilt.
- A logger abstraction, a log level, a ring buffer. `console.warn` is what a browser has.
- Reacting to `onclose` or `onerror` — `STORY-0310`.

## Tests

`web-client/src/protocol/connection.test.ts`, describe block `"the connection"`. Four new `it`
blocks, appended. **The six from `TASK-030307` are not edited**: this ticket adds an inbound path
that none of them observes — they assert on `socket.sent`, `socket.closed` and the initial status,
and this ticket writes to none of those.

| Test | Proves |
| --- | --- |
| `hands a decoded frame to the listener` | after `socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}')`, the collected messages are exactly that one object |
| `logs and drops a frame it cannot read` | with `vi.spyOn(console, "warn")`, `socket.receive("not json")` collects nothing and calls `warn` once |
| `drops a frame whose type it does not know` | `socket.receive('{"type":"Nonsense"}')` collects nothing |
| `handles the next valid frame after a dropped one` | after those two drops, `socket.receive('{"type":"RoomJoined","code":"ABCD","seat":0}')` collects exactly one message |

Four tests. Forty-three exist, so the suite reports **47**.

`RoomJoined` is the fixture on purpose: it is a frame the connection has no opinion about, so these
four tests keep passing unchanged when `TASK-030309` and `TASK-030310` teach it about `Welcome` and
`Failure`.

Restore the spy — `vi.restoreAllMocks()` in `afterEach` — or the count assertion in the next test
file to run becomes a mystery.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 47 passed (47)` | the four tests ran and the six before them still do |
| the three `--reporter=verbose` greps | forwarding, the drop and the recovery exist by name |
| `grep 'console.warn' connection.ts` | the drop is logged, not silent. The test asserts the call; the grep asserts it is in this file rather than somewhere a spy happened to catch |

**Name the edit that makes each assertion red:**

1. Delete the `console.warn` line → `logs and drops a frame it cannot read` fails,
   `expected "warn" to be called once, but it was called 0 times`. Revert.
2. Return early from `onmessage` after the null check instead of calling `options.onMessage` →
   `hands a decoded frame to the listener` fails, `expected [] to have a length of 1`. Revert.
3. Let a decode failure throw instead of returning → `handles the next valid frame after a dropped
   one` fails. Revert.

Quote all three in the PR. The third is the story's fourth acceptance criterion: a malformed frame
must not cost the client the frames that follow it.

## Acceptance criteria

- [ ] `the connection > hands a decoded frame to the listener` passes
- [ ] `the connection > logs and drops a frame it cannot read` passes
- [ ] `the connection > drops a frame whose type it does not know` passes
- [ ] `the connection > handles the next valid frame after a dropped one` passes
- [ ] `npm run --silent test` reports `Tests  47 passed (47)`
- [ ] The six `it` blocks from `TASK-030307` are unedited, and their assertions are byte-identical
- [ ] `connection.ts` contains no `switch`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
