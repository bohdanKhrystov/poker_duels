---
id: STORY-0205
title: Sessions and the socket lifecycle
type: story
status: backlog
parent: EPIC-02
module: poker-server
labels: [server, websocket, session]
depends_on: [STORY-0202]
---

## Goal

A browser can open a WebSocket, say who it is, and hold a session for as long as the connection
lasts. The server knows, for every frame it ever receives, which profile sent it — and for every
frame it sends, which connection it goes to.

## Why

Every later message needs an answer to "who is this?". Doing identity at the connection boundary,
once, is what stops each feature re-deriving it from a field in a payload — and a payload field is
a client asserting a fact about itself, which `ADR-0002` forbids.

It is also the story that owns the `/ws` route. `STORY-0207` and `STORY-0208` both extend that
route, which is why they wait on this one rather than running beside it.

## Design notes

- One WebSocket route, `/ws`, using Ktor's `WebSockets` plugin installed in `STORY-0201`. Ktor's
  own ping/pong configuration is the keepalive; no hand-rolled heartbeat message.
- **The handshake is the first frame or the socket closes.** The first frame must be
  `ClientMessage.Hello(deviceId?, protocolVersion)`. Anything else, or a version mismatch, closes
  the connection with a defined close reason. An unauthenticated socket that is allowed to linger
  is a resource nobody owns.
- Identity per [`ADR-0012`](../../docs/adr/ADR-0012-device-bound-anonymous-profiles.md): a device
  presents a device id *or is issued one*. This story issues it — the id is opaque, server-minted
  from a `SecureRandom`-backed source, returned in `ServerMessage.Welcome`, and stored by the
  client. A server-issued id is strictly better than a client-chosen one: it costs nothing and
  removes a class of collision and spoofing that a client-chosen string invites.
- Profile resolution goes through a **port declared here and implemented in `STORY-0210`**:
  something like `PlayerDirectory.resolve(deviceId: DeviceId?): Player`.
  [`ADR-0011`](../../docs/adr/ADR-0011-postgres-in-v01.md) puts persistence behind a repository
  boundary; declaring the port at its consumer is what lets this story run with no database at
  all. The in-memory implementation these tests use lives in the server's **test** sources and
  never ships.
- A `SessionRegistry` maps connection to `SessionId` and `PlayerId`, and drops the entry when the
  socket closes. It holds no game state — this story knows sessions, not duels.
- **One writer per connection.** Outbound frames go through a single writing coroutine fed by a
  channel, so two concurrent producers cannot interleave halves of two frames. This is cheap now
  and very hard to retrofit once three stories are sending.
- Closing is symmetric: every close path — client close, protocol error, server shutdown — removes
  the session exactly once. `STORY-0208` layers the grace period on top of this; here, close means
  gone.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0205`.* | — |

## Acceptance criteria

- [ ] A client connecting with no device id receives a `Welcome` carrying a newly issued one; a
      client reconnecting with that id resolves to the same profile and no new profile is created.
- [ ] A first frame that is not `Hello` closes the socket with a defined close reason, asserted.
- [ ] A `Hello` declaring the wrong protocol version closes the socket with the version-mismatch
      message from `STORY-0202`.
- [ ] A malformed frame mid-session produces an error message to that client and does not close
      the socket or affect any other session.
- [ ] Closing a socket removes exactly one entry from the `SessionRegistry`, asserted for both a
      clean close and an abrupt one.
- [ ] Two coroutines sending on one connection produce whole, non-interleaved frames, asserted by
      a concurrency test.
- [ ] Device ids are unique across 100 000 issues and are drawn from an injected secure source, not
      from the engine's `Rng` — the engine's determinism is a game property, never a security one.

## Out of scope

- Rooms, seats and duels — `STORY-0206`, `STORY-0207`.
- The disconnect grace period and reconnect resync — `STORY-0208`. Here, a closed socket is simply
  a gone session.
- The Postgres implementation of `PlayerDirectory` — `STORY-0210`.
- Authentication of any kind. `ADR-0012` says v0.1 has none, and `EPIC-04` owns the claim flow it
  demands.
