---
id: STORY-0405
title: Sign-in, the session, and what the socket presents
type: story
status: backlog
parent: EPIC-04
module: poker-server
labels: [server, auth, http, protocol, wire]
depends_on: [STORY-0404, STORY-0213, STORY-0214]
---

## Goal

A player signs in with a handle and a password, receives a session token once, and is that player on
every request and on the socket — with the device id travelling alongside and **ignored**. One
resolver decides identity for the whole server, and an invalid session is refused rather than
quietly downgraded to anonymous.

## Why

This is the story `EPIC-04` cannot route around: until a session outranks a device id, an account is
a row nobody can use. It is also the one that moves `PROTOCOL_VERSION`, which is why it queues
behind `STORY-0213` and `STORY-0214` — [`ADR-0045`](../../docs/adr/ADR-0045-presence-belongs-to-the-table.md)
§4 and [`ADR-0047`](../../docs/adr/ADR-0047-a-protocol-version-is-claimed-in-a-ledger.md) allow
**one protocol-bumping branch open at a time**, in that order.

## Design notes

- **`POST /api/auth/sign-in` and `POST /api/auth/sign-out`** ([`ADR-0027`](../../docs/adr/ADR-0027-the-session-outranks-the-device-id.md)
  §3). Sign-in verifies the credential and inserts one `auth_session` row; the token is 256 bits from
  `SecureRandom`, URL-safe base64 unpadded, returned exactly once in that response body, stored as
  SHA-256, and never returned, logged or put in a `ServerMessage` again.
- **Thirty days absolute, no sliding window.** Thirty days is a constant; `issued_at` and
  `expires_at` are `TIMESTAMPTZ` columns compared against SQL `now()`, so both instants come
  from an injected `java.time.Clock` —
  [`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md),
  which amends `ADR-0027` §1 on exactly this clause. **Not `ServerClock`**: it reports elapsed
  milliseconds from an arbitrary epoch, so a row stamped from it expires in 1970 and every
  session is dead on arrival. Expiry is still enforced at read time
  (`WHERE expires_at > now()`), so an expired row is garbage rather than a hole. No test sleeps
  to prove it: the test fixes the clock and issues a second one thirty days on.
- **Sign-out is `DELETE FROM auth_session WHERE token_hash = ?` and answers `204` either way**
  ([`ADR-0030`](../../docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md) §3). It
  writes nothing to `player`, and it **does not close live sockets** — tearing one down mid-duel
  would abandon a seat and cost somebody a coin, which is the exact class of harm this epic forbids.
- **One `IdentityResolver`, called by the socket handshake and by every HTTP route** (`ADR-0027`
  §4). A rule implemented twice is a rule with two behaviours. Precedence: a valid session wins
  outright and any device id presented alongside is not even validated; an invalid, expired or
  unknown session is **refused** — `ProtocolError.INVALID_SESSION` on the socket, `401` with an
  empty body over HTTP — and never falls back to the device id; with no session, today's device
  behaviour is unchanged.
- **`ProfileReads.profileOf` takes a `PlayerId`**, not a `DeviceId` (`ADR-0030` §4). Under a session
  the signed-in player's `device_id` is another device's entirely, so a device-keyed `GET /api/me`
  would answer with the wrong profile. The *unknown device → `401`* rule moves up into the resolver.
- **The wire**: `Hello` gains `sessionToken: String? = null` — in band, because a browser cannot set
  a header on a WebSocket upgrade; `Welcome` gains `playerId: String` and its `deviceId` becomes
  `String?`, present exactly when this connection's identity came from a device id. A connection's
  identity is fixed at `Hello` and never changes for the life of the socket; there is no sign-in
  message.
- **`PROTOCOL_VERSION` moves to 3 once**, in this story's last ticket, with the `docs/protocol-versions.md`
  ledger row `ADR-0047` requires, the number read from `develop` plus one at that moment.
- **Sign-in tells a stranger nothing** (`ADR-0027` §6): an unknown identifier is verified against a
  fixed dummy Argon2 hash so both paths cost the same time; both failures answer identically; failed
  attempts are budgeted **by remote address**, and over budget answers exactly like a wrong password.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not yet split. Run `/plan-story STORY-0405` once `STORY-0404`, `STORY-0213` and `STORY-0214` have merged.* | — |

## Acceptance criteria

- [ ] A correct handle and password answer `200` with a token; the same token then reads that
      player's profile over HTTP and seats that player on the socket.
- [ ] A wrong password and an unknown handle are indistinguishable: same status, same body, same
      headers — asserted field by field, not by eyeballing.
- [ ] A device id presented alongside a valid session is ignored: a signed-in request from a device
      that owns a *different* profile reads back the session's profile, and the device's profile is
      unchanged.
- [ ] An expired session is refused rather than downgraded, proven by moving the injected clock past
      the expiry — no test sleeps.
- [ ] Sign-out deletes the row, answers `204` twice in a row, and leaves `player` untouched.
- [ ] A device that signs in on a browser that has never connected receives **no device id and no
      `player` row**, and `Welcome.deviceId` is `null`.
- [ ] `PROTOCOL_VERSION` is `3`, `docs/protocol-versions.md` has exactly one new row, and
      `./gradlew :poker-server:verifyProtocolTypes` passes with `protocol.gen.ts` regenerated.

## Out of scope

- The claim's coin proof and the device revocation — `STORY-0406`.
- Recovery from a device that has never been seen, as a scenario — `STORY-0407`.
- The password reset — `STORY-0416`.
- Every screen — `STORY-0412`.
