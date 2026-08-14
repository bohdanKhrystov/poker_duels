---
schema: 2
id: TASK-030310
title: A refusal keeps the socket, and a version mismatch ends the connection for good
type: task
status: done
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol]
depends_on: [TASK-030309]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +56 passed \(56\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'surfaces a refusal and keeps the socket'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing more once the version is wrong'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'never closes the socket itself'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps a refusal it has never heard of verbatim'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

A `Failure` surfaces its `ProtocolError` and leaves the socket alone; `VERSION_MISMATCH` ends the
connection instead — no further frames, no close, and therefore nothing for a reconnect to loop on.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | modify |
| `web-client/src/protocol/connection.test.ts` | modify |

`ProtocolError`'s values are in `docs/protocol.md` under *Protocol Errors*; you do not need to open
the generated file for them.

## Scope

- In `onmessage`, beside the `Welcome` branch:

  ```ts
  if (message.type === "Failure") {
    status =
      message.error === "VERSION_MISMATCH"
        ? { kind: "outdated" }
        : { kind: "refused", error: message.error };
  }
  ```

- `send` becomes a no-op when `status.kind === "outdated"`, and only then:

  ```ts
  send(message) {
    // A server that refused this version will refuse it identically forever.
    // Talking to it again is the retry loop STORY-0303 exists to not build.
    if (status.kind === "outdated") return;
    socket.send(encodeClientMessage(message));
  }
  ```

- **The connection never closes the socket on a `Failure`, including `VERSION_MISMATCH`.**
  `docs/protocol.md` is explicit that the server never silently drops a connection; the client
  matches it. It also means `STORY-0310`'s reconnect has no close event to react to, so a terminal
  state cannot become a reconnect storm by accident.
- A refusal that is not `VERSION_MISMATCH` changes the status and nothing else: the socket stays,
  `send` still works, and the next frame is handled normally. `UNKNOWN_ROOM` and `ROOM_FULL` are
  ordinary answers to an ordinary request, not faults.
- **The error is kept verbatim, never mapped, never enumerated.** `ADR-0027` adds
  `INVALID_SESSION` to `ProtocolError`; a client that `switch`ed over the values would break on that
  day, and this one widens for free.
- `Failure` is forwarded to `onMessage` like everything else.

## Out of scope

- Rendering anything. What a player reads — *"this tab is out of date, reload"* — is `EPIC-03`'s
  copy and the human's, per `ADR-0028`'s closing note. This ticket produces the state, not a word of
  it.
- Reconnect, backoff, resuming a seat — `STORY-0310`. The one thing this ticket owes that story is
  the guarantee that `outdated` is reachable, observable and does not close the socket.
- Retrying a refused `JoinRoom`. `STORY-0305` decides what a lobby does with `UNKNOWN_ROOM`.
- Clearing the device id on a refusal. Nothing here touches storage.

## Tests

`web-client/src/protocol/connection.test.ts`, describe block `"the connection"`. Five new `it`
blocks, appended. **The fourteen before them are not edited.** The only behaviour this ticket
changes for an existing test is `send`, and it changes it only while `status.kind === "outdated"` —
a state no earlier test reaches, since `writes a client message to the socket` sends while the
connection is still `connecting`.

| Test | Proves |
| --- | --- |
| `surfaces a refusal and keeps the socket` | `socket.receive('{"type":"Failure","error":"UNKNOWN_ROOM"}')` sets `status` to `{ kind: "refused", error: "UNKNOWN_ROOM" }`, `socket.closed` is `false`, and a following `send` reaches `socket.sent` |
| `keeps a refusal it has never heard of verbatim` | `socket.receive('{"type":"Failure","error":"INVALID_SESSION"}')` sets `status` to `{ kind: "refused", error: "INVALID_SESSION" }` |
| `treats a version mismatch as the end of the connection` | `socket.receive('{"type":"Failure","error":"VERSION_MISMATCH"}')` sets `status` to `{ kind: "outdated" }` |
| `sends nothing more once the version is wrong` | after that frame, `connection.send({ type: "CreateRoom" })` leaves `socket.sent` the length it already was |
| `never closes the socket itself` | after that frame, `socket.closed` is `false` |

Five tests. Fifty-one exist, so the suite reports **56**.

`keeps a refusal it has never heard of verbatim` uses a raw JSON string, so no cast is needed to
build a value the current union does not name — which is also the point: the codec checks the `type`
discriminator and nothing else, so a new `ProtocolError` arrives intact today.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 56 passed (56)` | the five tests ran and the fourteen before them still do |
| the four `--reporter=verbose` greps | the refusal, the terminal state, the silence after it and the forward compatibility all exist by name |
| `npm run check` | the whole client still typechecks, lints and formats |

**Name the edit that makes each assertion red:**

1. Delete the `status.kind === "outdated"` guard from `send` → `sends nothing more once the version
   is wrong` fails, `expected 1 to be 0`. Revert.
2. Add `socket.close()` to the `VERSION_MISMATCH` branch → `never closes the socket itself` fails,
   `expected true to be false`. Revert.
3. Set `{ kind: "outdated" }` for every `Failure` → `surfaces a refusal and keeps the socket` fails
   on the status. Revert.
4. Map the error through a `switch` with a `default: "UNKNOWN_MESSAGE"` → `keeps a refusal it has
   never heard of verbatim` fails, naming `INVALID_SESSION`. Revert.

Quote all four in the PR. The fourth is this story's forward-compatibility promise made executable:
`ADR-0027` will add that exact value, and the client is meant to widen without a diff.

## Acceptance criteria

- [ ] `the connection > surfaces a refusal and keeps the socket` passes
- [ ] `the connection > keeps a refusal it has never heard of verbatim` passes
- [ ] `the connection > treats a version mismatch as the end of the connection` passes
- [ ] `the connection > sends nothing more once the version is wrong` passes
- [ ] `the connection > never closes the socket itself` passes
- [ ] `npm run --silent test` reports `Tests  56 passed (56)`
- [ ] The fourteen `it` blocks from `TASK-030307` to `TASK-030309` are unedited, and their assertions are byte-identical
- [ ] `connection.ts` contains no `switch` and calls `socket.close()` from `close()` only
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
