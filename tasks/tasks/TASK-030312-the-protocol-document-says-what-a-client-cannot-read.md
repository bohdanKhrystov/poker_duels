---
schema: 2
id: TASK-030312
title: docs/protocol.md says what a client does with a frame it cannot read
type: task
status: done
parent: STORY-0303
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [docs, protocol, client]
depends_on: [TASK-030311]
verify:
  - test -f docs/protocol.md
  - grep -qF '## What a client does with a frame it cannot read' docs/protocol.md
  - grep -qF 'is logged with `console.warn` and dropped' docs/protocol.md
  - grep -qF 'sends nothing further, does not close the socket, and does not reconnect' docs/protocol.md
  - grep -qF 'web-client/src/protocol/' docs/protocol.md
  - test -f web-client/src/protocol/connection.ts
  - test -f docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md
  - test -f docs/adr/ADR-0027-the-session-outranks-the-device-id.md
  - test -f docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

The wire contract states, in one place, what a client does with a frame or a version it does not
recognise — so the two changes already decided against this wire do not have to rediscover it.

## Why it is worth a ticket

`ADR-0027` moves `PROTOCOL_VERSION` and adds `INVALID_SESSION`; `ADR-0028` moves it again and adds
two `ServerMessage` variants. Whoever lands either will ask what the deployed client does with a
`type` it has never heard of and a version it did not expect. The answer is implemented in
`STORY-0303` and is otherwise findable only by reading four TypeScript files.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |

## Scope

- Add one `## What a client does with a frame it cannot read` section, after *Protocol Errors* and
  before *Notes*. Prose and bullets only — **no table**. `ProtocolDocumentationTest` scrapes every
  line matching ``^\| `Name` \|`` and fails if the backticked identifier is not a real message, so a
  row for `VERSION_MISMATCH` would break the build.
- The section says exactly these five things:
  - The web client sees a raw frame in exactly one place, `web-client/src/protocol/`; no other file
    in it declares a wire type or touches a `WebSocket`.
  - A frame that is not JSON, is not an object, carries no `type`, or carries a `type` the client's
    generated union does not name **is logged with `console.warn` and dropped**. Nothing is thrown
    into a render; the next `Snapshot` re-establishes the truth, which is why the server sends one
    after every transition.
  - A `Failure` never closes the socket. The client surfaces the `ProtocolError` **verbatim,
    including a value its generated union does not name**, and keeps the connection.
  - `VERSION_MISMATCH`, and a `Welcome` whose `protocolVersion` is not the one the client sent, are
    terminal: the client **sends nothing further, does not close the socket, and does not
    reconnect**, and a mismatched `Welcome` is not persisted. Reloading is the only remedy.
  - The client's version is one constant typed against the generated `ProtocolVersion` alias
    (`ADR-0020`), so moving `PROTOCOL_VERSION` — `ADR-0027`, then `ADR-0028`, or the other way
    round — fails the client's typecheck until it moves with it. Adding a `ServerMessage` variant
    costs the client one entry in the table `frames.ts` proves against the union.
- Link `ADR-0020`, `ADR-0027` and `ADR-0028` by relative path from `docs/`, e.g.
  `[ADR-0028](adr/ADR-0028-the-wire-names-an-absent-opponent.md)`. Every link in the new section is
  `test -f`-ed by the `verify` block; do not add one that is not.
- The version line at the top of the document is **not** touched. It says 2 and the wire is at 2.

## Out of scope

- Anything about how a *server* handles an unknown frame. That is already documented, and it is a
  `Failure`, not a drop.
- Reconnect policy — `STORY-0310` will document its own, and this section deliberately says only
  what *forbids* a reconnect.
- Editing `CONTRIBUTING.md`. The client's commands are already there from `TASK-030111`.
- Any change to the message tables or the error list.

## Tests

None. The assertions are the `verify` block: four content greps that pin the exact sentences, four
`test -f` checks over everything the section names, and the existing Kotlin documentation test.

`test -f` on every linked path is not ceremony: a ticket in this repository has already shipped a
dead ADR link with every other check green.

## Proof

| Command | Proves |
| --- | --- |
| the heading grep | the section exists under the heading the acceptance criterion names |
| the *logged and dropped* grep | the drop rule is stated, in the words the implementation uses |
| the *sends nothing further* grep | the terminal rule is stated in full — all three clauses, since any one alone is a different and wrong policy |
| the four `test -f` commands | every path and ADR the section names exists |
| `ProtocolDocumentationTest` | the addition did not break the row scrape, the version assertion or the error list |

**Name the edit that makes each assertion red:** delete any one of the three sentences, or rename a
linked ADR file, and exactly one command in the block fails. Delete the section's heading and the
first grep fails while the rest still pass, which is why the sentences are greped and not just the
heading.

## Acceptance criteria

- [ ] `docs/protocol.md` contains a `## What a client does with a frame it cannot read` section
- [ ] The section contains no markdown table
- [ ] `./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'` exits 0
- [ ] Every path and ADR the section links exists on disk
- [ ] The document's `Protocol version: **2**` line is unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
