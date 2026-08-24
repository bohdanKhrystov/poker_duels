---
schema: 2
id: TASK-040613
title: Signed out everywhere, and still signed in here
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, http, revocation, security]
depends_on: [TASK-040612]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DeviceRevocationDatabaseTest' -PrequireDocker=true
---

## Goal

Against a real database and the shipped composition, one `DELETE /api/me/device` leaves the
revoking session working and every other session that player holds dead — the criterion
`ADR-0050` §4 adds to this story.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DeviceRevocationDatabaseTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/db/SignInDatabaseTest.kt` — the setup block and the
private `HttpClient` helpers this file copies — and
`docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` §1 and §2.

## Scope

- A new class, `duels.poker.server.db.DeviceRevocationDatabaseTest`, with `SignInDatabaseTest`'s
  setup verbatim: `containerCoordinates()`, a `PGSimpleDataSource`, `Migrations.migrate(...)`, a
  `ServerConfig` built field by field, and `application { duelServer(serverComponents(config, dataSource)) }`
  inside `testApplication`.
- **Private top-level helpers are copied, not shared.** `SignInDatabaseTest`'s are `private`, which
  in Kotlin is per-file, so this file declares its own `handshake`, `profileOf`, `meWithToken`,
  `signUp`, `signIn` and a new `revokeDevice(token: String): HttpStatusCode`. Extracting a shared
  file would edit two merged suites and is not this ticket. Copy only the helpers the tests below
  use.
- The fixture, in a private `setUpOwner()` helper both tests call: `handshake(OWNER_DEVICE)`, then
  `signUp(OWNER_DEVICE, OWNER_HANDLE, PASSWORD)` — sign-up issues **no** session (`ADR-0030` §3), so
  every token in this class was minted by a sign-in proving a password, which is what makes "every
  other session" a meaningful phrase here.

## Out of scope

- The socket's view of a revoked device — `TASK-040614`, same file.
- Signing in again after revoking — `TASK-040615`, same file.
- Coin properties P1 and P2 — `TASK-040616` onward.
- `SignInDatabaseTest.kt` and `SignUpDatabaseTest.kt` — **named prohibitions.**

## Tests

`DeviceRevocationDatabaseTest`

| Test | Proves |
| --- | --- |
| `theRevokingSessionSurvivesAndTheOthersDoNot` | Sign in three times for the same player, getting `t0`, `t1`, `t2` — three separate `POST /api/auth/sign-in` calls, so three rows. `revokeDevice(t0)` answers `204`. Then, **immediately afterwards and in the same test**: `meWithToken(t0)` answers `200` and its body's `playerId` is the owner's, while `meWithToken(t1)` and `meWithToken(t2)` both answer `401`. Three tokens, two different expected answers — the criterion `ADR-0050` §4 words as *"both asserted by using both tokens"* |
| `exactlyOneSessionRowIsLeft` | In the same shape, `SELECT count(*) FROM auth_session WHERE player_id = ?` reads `3` before the call and `1` after. **A count, not a status**: a `401` cannot distinguish a deleted row from an expired one, and `3 → 1` cannot be satisfied by a `DELETE` that removed everything |
| `anotherPlayersSessionsAreUntouched` | A second player, `d-other`, signs up under their own handle and signs in twice. After the owner revokes, both of the other player's tokens still answer `200` from `meWithToken`, and the owner's own surviving token still answers `200`. Without this, a `DELETE` with no `player_id` predicate passes both tests above |

## Acceptance criteria

- [ ] `DeviceRevocationDatabaseTest.theRevokingSessionSurvivesAndTheOthersDoNot` passes
- [ ] `DeviceRevocationDatabaseTest.exactlyOneSessionRowIsLeft` passes
- [ ] `DeviceRevocationDatabaseTest.anotherPlayersSessionsAreUntouched` passes
- [ ] Each of the three tests issues its tokens through `POST /api/auth/sign-in`, never by calling
      `PostgresAuthSessions.issue` directly
- [ ] `exactlyOneSessionRowIsLeft` asserts both the before value `3` and the after value `1`
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Two mutations on `PostgresDeviceBindings`, each reverted after.

1. **Drop ` AND token_hash <> ?` from the `DELETE`.** `theRevokingSessionSurvivesAndTheOthersDoNot`
   reddens on `meWithToken(t0)` answering `401`, and `exactlyOneSessionRowIsLeft` reddens on the
   after count reading `0`. `anotherPlayersSessionsAreUntouched` reddens too, on the owner's
   surviving token. **Three tests.**
2. **Drop the whole `DELETE` statement.** `theRevokingSessionSurvivesAndTheOthersDoNot` reddens on
   `t1` answering `200`, and `exactlyOneSessionRowIsLeft` reddens on the after count reading `3`.
   `anotherPlayersSessionsAreUntouched` stays **green** — it only ever asserts that sessions still
   work — which is why it is the control and not the criterion.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
