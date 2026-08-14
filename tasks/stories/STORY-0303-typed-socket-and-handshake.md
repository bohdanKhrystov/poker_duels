---
id: STORY-0303
title: The typed socket — handshake and device identity
type: story
status: backlog
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

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0303`.* | — |

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

## Out of scope

- Reconnect, backoff and resuming a seat — `STORY-0310`.
- Room state, duel state, anything folded over frames — `STORY-0304`.
- Any rendering.
- The HTTP endpoints — `STORY-0311`. They are not `ServerMessage`s and do not belong to this module.
