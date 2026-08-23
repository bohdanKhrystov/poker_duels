---
id: STORY-0405
title: Sign-in, the session, and what the socket presents
type: story
status: ready
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
- **`PROTOCOL_VERSION` moves once**, with the `docs/protocol-versions.md` ledger row `ADR-0047`
  requires and the number read from `develop` plus one at that moment. **Two corrections, made when
  this story was split.** The number is not `3`: `STORY-0213` and `STORY-0214` have since claimed
  `3` and `4`, so it is `5` unless `develop` has moved again. And it is not the *last* ticket but
  the second, `TASK-040502` — `ProtocolVersionLedgerTest` compares the live descriptors against the
  last ledger row, so a wire field that lands before its version number fails `check` on every
  commit in between, and every behavioural ticket after it names `Welcome.playerId` and
  `ProtocolError.INVALID_SESSION`.
- **Sign-in tells a stranger nothing** (`ADR-0027` §6): an unknown identifier is verified against a
  fixed dummy Argon2 hash so both paths cost the same time; both failures answer identically; failed
  attempts are budgeted **by remote address**, and over budget answers exactly like a wrong password.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-040501](../tasks/TASK-040501-the-version-is-answered-before-an-identity-is-minted.md) | The version question is answered before any identity is minted | ready |
| [TASK-040502](../tasks/TASK-040502-the-wire-carries-a-token-names-the-player-and-the-version-takes-its-step.md) | The wire carries a session token, names the player, and `PROTOCOL_VERSION` takes its step | backlog |
| [TASK-040503](../tasks/TASK-040503-a-mismatched-version-mints-nothing.md) | A version mismatch mints no device id and creates no profile | backlog |
| [TASK-040504](../tasks/TASK-040504-a-session-token-is-256-bits-url-safe-and-unpadded.md) | A session token is 256 bits, URL-safe and unpadded | backlog |
| [TASK-040505](../tasks/TASK-040505-the-session-store-is-a-port-and-a-double-that-knows-nothing.md) | The session store is a port, and a double that has issued nothing | backlog |
| [TASK-040506](../tasks/TASK-040506-issue-writes-one-row-a-digest-and-thirty-days.md) | `issue` writes one row, a digest, and thirty days from the injected clock | backlog |
| [TASK-040507](../tasks/TASK-040507-playerof-reads-through-the-expiry.md) | `playerOf` reads through the expiry, and a clock thirty days on refuses | backlog |
| [TASK-040508](../tasks/TASK-040508-delete-removes-the-row-and-says-the-same-thing-twice.md) | `delete` removes the row, and says the same thing twice | backlog |
| [TASK-040509](../tasks/TASK-040509-the-directory-finds-a-profile-without-creating-one.md) | The directory can find a profile without creating one | backlog |
| [TASK-040510](../tasks/TASK-040510-one-resolver-and-an-invalid-session-never-falls-back.md) | One resolver, and an invalid session never falls back to the device | backlog |
| [TASK-040511](../tasks/TASK-040511-the-read-path-follows-the-resolved-player.md) | The profile read follows the resolved player, and every route resolves the same way | backlog |
| [TASK-040512](../tasks/TASK-040512-a-signed-in-request-reads-the-sessions-profile.md) | A signed-in request reads the session's profile, and the device beside it is ignored | backlog |
| [TASK-040513](../tasks/TASK-040513-the-sign-in-body-is-two-fields-and-the-answer-carries-the-token-once.md) | The sign-in body is two fields, and its answer carries the token exactly once | backlog |
| [TASK-040514](../tasks/TASK-040514-sign-in-the-credential-decides-and-a-stranger-learns-nothing.md) | `POST /api/auth/sign-in`: the credential decides, and a stranger learns nothing | backlog |
| [TASK-040515](../tasks/TASK-040515-sign-out-is-one-delete-and-two-hundred-and-four-either-way.md) | `POST /api/auth/sign-out`: one delete, `204` either way, and no socket closes | backlog |
| [TASK-040516](../tasks/TASK-040516-the-document-names-sign-in-and-sign-out.md) | The document names sign-in and sign-out, and the test that reads it keeps its bearings | backlog |
| [TASK-040517](../tasks/TASK-040517-the-socket-is-handed-the-resolver.md) | The socket's dependencies carry the resolver | backlog |
| [TASK-040518](../tasks/TASK-040518-the-socket-presents-the-session-and-an-invalid-one-is-refused.md) | The socket presents the session, and an invalid one is refused rather than downgraded | backlog |
| [TASK-040519](../tasks/TASK-040519-a-budget-is-a-rolling-window-and-over-budget-still-counts.md) | A budget is a rolling window, an over-budget attempt still counts, and a slot can be refunded | backlog |
| [TASK-040520](../tasks/TASK-040520-the-sign-up-budget-is-two-config-values.md) | The two auth budgets are four configuration values with defaults | backlog |
| [TASK-040521](../tasks/TASK-040521-sign-up-over-budget-answers-429.md) | Sign-up over budget answers `429`, and the budget meters the hash | backlog |
| [TASK-040522](../tasks/TASK-040522-the-document-names-the-seventh-answer.md) | The document names sign-up's seventh answer | backlog |
| [TASK-040523](../tasks/TASK-040523-sign-in-carries-a-budget-of-its-own.md) | Sign-in carries a budget of its own | backlog |
| [TASK-040524](../tasks/TASK-040524-signed-in-here-reading-there-against-the-database.md) | Signed in here, reading there — the whole flow against the database | backlog |

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
- [ ] `PROTOCOL_VERSION` is `develop`'s plus one, `docs/protocol-versions.md` has exactly one new row, and
      `./gradlew :poker-server:verifyProtocolTypes` passes with `protocol.gen.ts` regenerated.

## Out of scope

- The claim's coin proof and the device revocation — `STORY-0406`.
- Recovery from a device that has never been seen, as a scenario — `STORY-0407`.
- The password reset — `STORY-0416`.
- Every screen — `STORY-0412`.

## Decisions raised by the split

- **`DEC-069` — the architect's — answered on 2026-08-24 by
  [`ADR-0074`](../../docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md).**
  `ADR-0027` §6 required a failed-sign-in budget and fixed its mechanism, its key and its answer,
  but no ADR fixed its two numbers. They are now **ten failed sign-ins per remote address per
  rolling sixty seconds** (`AUTH_SIGN_IN_MAX_ATTEMPTS` `10`, `AUTH_SIGN_IN_WINDOW_MILLIS` `60000`),
  on a **second** `AttemptBudget` instance — `ADR-0022` §2's pair rather than sign-up's five per
  fifteen minutes, because a fifteen-minute window combined with *over budget still counts* turns a
  shared address's burst into an indefinite lockout, and because the guessing defence is nearly
  insensitive across that range while the collateral is not. The budget is **reserved before the
  hash and refunded when the password was right**, so `AttemptBudget` is born with a second method,
  `refund`. Three tickets move: `TASK-040519` builds the type with both methods, `TASK-040520`
  carries both config pairs, and `TASK-040523` is no longer blocked — five files, `atomic:`,
  waiting only on `TASK-040522`. Sign-in
  still ships unbudgeted until `TASK-040523` merges, which is safe only while `EPIC-07` hosts
  nothing — `ADR-0055`'s *"the deployment wins"* clause is the binding constraint, not a story
  boundary.
