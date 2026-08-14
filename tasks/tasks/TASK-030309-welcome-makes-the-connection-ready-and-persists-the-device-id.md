---
schema: 2
id: TASK-030309
title: Welcome makes the connection ready and persists the device id the server issued
type: task
status: backlog
parent: STORY-0303
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, protocol, identity]
depends_on: [TASK-030308]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +51 passed \(51\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'remembers the device id the server issued'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends the remembered device id on the next connection'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'refuses to trust a welcome at another version'
  - cd web-client && npm run check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

A `Welcome` makes the connection `ready` and writes the device id the server issued to storage, so
the next connection presents it — and a `Welcome` at an unexpected version does neither.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/connection.ts` | modify |
| `web-client/src/protocol/connection.test.ts` | modify |
| `web-client/src/protocol/device-id.ts` | read — `DEVICE_ID_STORAGE_KEY`, `writeDeviceId(storage, deviceId)` |

## Scope

- In `onmessage`, before forwarding, interpret exactly one discriminator:

  ```ts
  if (message.type === "Welcome") {
    if (message.protocolVersion === PROTOCOL_VERSION) {
      writeDeviceId(options.storage, message.deviceId);
      status = { kind: "ready", deviceId: message.deviceId };
    } else {
      status = { kind: "outdated" };
    }
  }
  options.onMessage(message);
  ```

- **The `Welcome` is still forwarded**, in both branches. The connection interprets it; it does not
  consume it.
- **A `Welcome` whose `protocolVersion` is not the one this client sent is not trusted**: no write
  to storage, no `ready`, status `outdated`. The server refuses a mismatched `Hello` with
  `Failure(VERSION_MISMATCH)` and should never send such a `Welcome`, so this branch is unreachable
  against a correct server — and it is written anyway because version equality is the *only*
  compatibility mechanism this protocol has (`ADR-0028` §8), and a client that persists an identity
  handed to it over a wire it does not understand is the harm `ADR-0027` §5 names in full.
- Compare against `PROTOCOL_VERSION`, never a literal.
- The client is **told** who it is. It writes what `Welcome.deviceId` carried and never invents,
  derives or defaults one.

## Out of scope

- `Failure`, including `VERSION_MISMATCH` — `TASK-030310`. This ticket produces `outdated` from one
  route only; that ticket adds the other and the rule about what `outdated` forbids.
- Any other discriminator. This is an `if`, not a `switch`, and it stays that way.
- `Welcome.playerId`. `ADR-0027` adds it and makes `deviceId` nullable, in `STORY-0405`. Today
  `Welcome.deviceId` is a non-null `string` in the generated types, and the version bump that
  changes that will fail `TASK-030302`'s constant first. Do not pre-empt it.

## Tests

`web-client/src/protocol/connection.test.ts`, describe block `"the connection"`. Four new `it`
blocks, appended. **The ten before them are not edited.** Their fixtures are `RoomJoined`,
`CreateRoom` and an empty storage, none of which this ticket's branch observes.

| Test | Proves |
| --- | --- |
| `remembers the device id the server issued` | after `socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":2}')`, `status` equals `{ kind: "ready", deviceId: "d-7" }` and `localStorage.getItem("pd.deviceId")` is `"d-7"` |
| `hands the welcome to the listener too` | the same frame is also collected by `onMessage` |
| `sends the remembered device id on the next connection` | after the above, `openConnection` over a **second** `FakeSocket` with the same storage, then `open()`, and the first frame's `deviceId` is `"d-7"` |
| `refuses to trust a welcome at another version` | `socket.receive('{"type":"Welcome","deviceId":"d-7","protocolVersion":3}')` leaves `localStorage.getItem("pd.deviceId")` `null` and sets `status` to `{ kind: "outdated" }` |

Four tests. Forty-seven exist, so the suite reports **51**.

`sends the remembered device id on the next connection` is the story's first acceptance criterion
end to end, in one test: a first visit sends `null`, the server answers, and a second connection
presents what it was given.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 51 passed (51)` | the four tests ran and the ten before them still do |
| the three `--reporter=verbose` greps | persistence, the round trip and the version refusal exist by name |
| `npm run check` | the whole client still typechecks, lints and formats |

**Name the edit that makes each assertion red:**

1. Drop the `writeDeviceId` call and set `ready` only → `remembers the device id the server issued`
   fails on the storage assertion, and `sends the remembered device id on the next connection` fails
   with `expected null to be "d-7"`. Revert.
2. Delete the `message.protocolVersion === PROTOCOL_VERSION` test → `refuses to trust a welcome at
   another version` fails, `expected { kind: 'ready', deviceId: 'd-7' } to equal { kind:
   'outdated' }`. Revert.
3. Return instead of falling through to `options.onMessage(message)` → `hands the welcome to the
   listener too` fails. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the connection > remembers the device id the server issued` passes
- [ ] `the connection > hands the welcome to the listener too` passes
- [ ] `the connection > sends the remembered device id on the next connection` passes
- [ ] `the connection > refuses to trust a welcome at another version` passes
- [ ] `npm run --silent test` reports `Tests  51 passed (51)`
- [ ] The ten `it` blocks from `TASK-030307` and `TASK-030308` are unedited, and their assertions are byte-identical
- [ ] `connection.ts` contains no `switch`, and compares the version against `PROTOCOL_VERSION` rather than a literal
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
