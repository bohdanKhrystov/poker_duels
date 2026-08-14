---
id: STORY-0303
title: The typed socket — handshake and device identity
type: story
status: done
parent: EPIC-03
module: web-client
labels: [client, protocol, websocket]
depends_on: [STORY-0301]
---

## Goal

One module opens `/ws`, completes the `Hello`/`Welcome` handshake, persists the device id the server
issues, encodes every outbound `ClientMessage` and decodes every inbound `ServerMessage` — and it is
the only file in the client that ever sees a raw frame.

## Why

Every screen is downstream of it. It is also where the "never hand-write a protocol type" rule is
either kept or lost: one module that could break it is reviewable, and a wire shape re-declared in
each component is not.

## Design notes

- **Types come from `web-client/src/protocol/protocol.gen.ts` only** (`ADR-0020`). No hand-written
  mirror, no schema library re-declaring a protocol shape, no `any` at the boundary. Narrowing is by
  the `type` discriminator, which `protocolJson` writes on every frame.
- The version the client sends is a constant typed against the generated alias —
  `const PROTOCOL_VERSION: ProtocolVersion = 2`. When the server bumps to 3 the alias becomes `3`
  and `tsc` fails here, which is the entire reason `ADR-0020` emits it: a stale version must fail
  the build, not the handshake in someone's browser.
- **Identity.** `Hello.deviceId` is `string | null`: a first visit sends `null` and the server mints
  one, returning it in `Welcome.deviceId`, which is non-null. The client persists that value under
  one storage key owned by this module and sends it on every later connection. `ADR-0012` accepts
  the consequence — clearing site data or switching browser loses the profile — so nothing here
  tries to be cleverer than `localStorage`.
- **One key, two consumers.** `STORY-0311` sends the same id as the `X-Device-Id` header on
  `GET /api/me`. The key is exported from here; two storage keys would mean two identities for one
  player.
- `Failure` does not close the socket — `docs/protocol.md` is explicit that the server never
  silently drops a connection. The client surfaces the `ProtocolError` and keeps the socket.
  `VERSION_MISMATCH` is the exception: it is terminal for the session and must present as *reload*,
  never as a retry loop against a server that will refuse identically forever.
- A frame that is not JSON, or whose `type` is not in the union, is logged and dropped — not thrown
  into a render. The next `Snapshot` re-establishes the truth, which is exactly why the server sends
  one after every transition.
- Tests drive a fake WebSocket. No test in this story opens a real socket, binds a port, or needs a
  server running.

## What the split settled

Two questions the tickets could each have answered differently, decided once here so no coder has
to guess. Neither needed an ADR: both are local to this module and follow from decisions already
made.

- **A `ServerMessage` variant the client does not recognise is logged and dropped.** The decoder
  holds a table of discriminators pinned to the generated union by
  `satisfies Record<ServerMessage["type"], true>`, so adding a variant fails `tsc` here — the
  compiler proves the coverage, and the runtime set is what lets an unrecognised `type` be dropped
  rather than cast. Nothing in the client `switch`es over `ServerMessage` or over `ProtocolError`.
  When `ADR-0028` adds `OpponentPresence` and `ActedForAbsentSeat`, this client costs two lines in
  one table; when `ADR-0027` adds `INVALID_SESSION`, it costs nothing — `TASK-030310` has a test
  that receives that exact value today and keeps it verbatim.
- **An unexpected version is terminal, in both directions.** `Failure(VERSION_MISMATCH)` and a
  `Welcome` whose `protocolVersion` is not the one this client sent both produce
  `{ kind: "outdated" }`: no further frames, no `close()`, no reconnect, and no device id written.
  Not closing is deliberate — `STORY-0310`'s reconnect reacts to a close, so a terminal state that
  closed the socket would be a retry loop against a server that will refuse identically forever.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-030301](../tasks/TASK-030301-nothing-outside-the-protocol-module-declares-a-wire-type.md) | Nothing outside src/protocol declares a wire type or touches a raw frame | ready |
| [TASK-030302](../tasks/TASK-030302-the-protocol-version-is-typed-against-the-generated-alias.md) | The protocol version the client sends is typed against the generated alias | backlog |
| [TASK-030303](../tasks/TASK-030303-the-frame-codec-decodes-only-what-the-union-names.md) | The frame codec decodes only what the generated union names | backlog |
| [TASK-030304](../tasks/TASK-030304-the-device-id-lives-under-one-key-this-module-owns.md) | The device id lives under one storage key this module owns | backlog |
| [TASK-030305](../tasks/TASK-030305-the-socket-url-comes-from-the-pages-own-origin.md) | The socket URL is derived from the page's own origin | backlog |
| [TASK-030306](../tasks/TASK-030306-a-websocket-double-the-handshake-tests-drive-by-hand.md) | A WebSocket double the handshake tests drive by hand | backlog |
| [TASK-030307](../tasks/TASK-030307-on-open-the-client-says-hello-with-the-device-id-it-holds.md) | On open the client says Hello with the device id it holds | backlog |
| [TASK-030308](../tasks/TASK-030308-every-inbound-frame-reaches-the-listener-or-is-dropped.md) | Every inbound frame reaches the listener, and an unreadable one is logged and dropped | backlog |
| [TASK-030309](../tasks/TASK-030309-welcome-makes-the-connection-ready-and-persists-the-device-id.md) | Welcome makes the connection ready and persists the device id the server issued | backlog |
| [TASK-030310](../tasks/TASK-030310-a-refusal-keeps-the-socket-a-version-mismatch-ends-the-session.md) | A refusal keeps the socket, and a version mismatch ends the connection for good | backlog |
| [TASK-030311](../tasks/TASK-030311-one-call-opens-the-duel-socket-with-no-network-in-the-test.md) | One call opens the duel socket, and the test that proves it touches no network | backlog |
| [TASK-030312](../tasks/TASK-030312-the-protocol-document-says-what-a-client-cannot-read.md) | docs/protocol.md says what a client does with a frame it cannot read | backlog |

## Acceptance criteria

- [ ] A first connection sends `Hello` with `deviceId: null` and `protocolVersion: 2`; after
      `Welcome`, the id is in storage, and a second connection sends it.
- [ ] The decoder handles every variant of the generated `ServerMessage` union, and adding a variant
      to the union without handling it fails the typecheck — exhaustiveness is proven by the
      compiler, not by a list someone maintains.
- [ ] `VERSION_MISMATCH` produces a terminal state and no reconnect attempt.
- [ ] A malformed frame is dropped without crashing the client, and a following valid frame is still
      handled.
- [ ] No file outside `src/protocol/` declares a socket message type — asserted by a check the
      client's own test command runs.

### Where each criterion lands

| Criterion | Ticket | Test |
| --- | --- | --- |
| The `Hello`/`Welcome` round trip | `TASK-030307`, `TASK-030309` | `says hello with no device id on a first visit`, `sends the remembered device id on the next connection` |
| Every variant decoded, a new one caught by the compiler | `TASK-030303` | `knows exactly the variants the generated union declares`, plus `satisfies Record<ServerMessage["type"], true>` in `frames.ts` |
| `VERSION_MISMATCH` is terminal, with no reconnect | `TASK-030310` | `sends nothing more once the version is wrong`, `never closes the socket itself` |
| A malformed frame is dropped and the next one still lands | `TASK-030308` | `handles the next valid frame after a dropped one` |
| Nothing outside the module declares a wire type | `TASK-030301` | `finds no wire type declared outside the protocol module` |

The second criterion says *"not by a list someone maintains"*, and the implementation does hold a
list of eight discriminators. The distinction is real and worth keeping straight: the list is
**maintained by the compiler's insistence**, not by anyone's diligence — a missing key is TS2739
and an extra key is TS2353. A list is unavoidable, because TypeScript types are erased and a
*runtime* set is the only thing that can tell an unknown `type` from a known one; what is avoidable
is a list that can silently fall behind, and `satisfies` plus
`knows exactly the variants the generated union declares` is what makes this one unable to.

## Out of scope

- Reconnect, backoff and resuming a seat — `STORY-0310`.
- Room state, duel state, anything folded over frames — `STORY-0304`.
- Any rendering.
- The HTTP endpoints — `STORY-0311`. They are not `ServerMessage`s and do not belong to this module.
