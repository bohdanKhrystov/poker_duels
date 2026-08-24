---
schema: 2
id: TASK-040615
title: Revoke, then the password reaches the same profile, coins and name
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, http, revocation, auth]
depends_on: [TASK-040614]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DeviceRevocationDatabaseTest' -PrequireDocker=true
---

## Goal

A player who revokes their device and then signs in with their password reaches the same profile,
the same coins and the same name — and the profile they read back says the device route is no
longer live.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DeviceRevocationDatabaseTest.kt` | modify |

Read, and do not edit:
`docs/adr/ADR-0037-the-device-is-a-credential-until-revoked.md`,
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §5's `deviceRouteLive`
bullet.

## Scope

- Four methods added to the existing class, plus a private `setName(deviceIdOrToken, name)` helper
  over `PUT /api/me/name` if the file does not already hold one.
- The fixture, in a **new** private helper the four tests share — neither `TASK-040613`'s nor
  `TASK-040614`'s is edited: the owner handshakes, sets the display name
  `"Owner"`, wins a duel (a `duel_result` pair written with raw SQL, balance `1`), signs up, signs
  in, and revokes with that token. The **whole profile** is snapshotted before the revocation —
  `playerId`, `coinBalance`, `displayName` — so the assertions afterwards compare against a recorded
  value rather than a literal.
- The second sign-in is a fresh `POST /api/auth/sign-in` **after** the revocation, so the token it
  answers with is not the one that revoked.

## Out of scope

- The screens — `STORY-0412` and `ADR-0050` §3.
- Any file under `poker-server/src/main`.
- Password reset and recovery email — `STORY-0416`.

## Tests

`DeviceRevocationDatabaseTest`, four new methods.

| Test | Proves |
| --- | --- |
| `theSecondSignInReachesTheSameProfile` | After revoking, `signIn(OWNER_HANDLE, PASSWORD)` succeeds and `meWithToken(newToken)` answers `200` with the **recorded** `playerId`, `coinBalance` `1`, and `displayName` `"Owner"` — three fields, each compared against what was snapshotted before, not against a literal |
| `theNewTokenIsNotTheRevokingToken` | The token from the second sign-in differs from the one used to revoke, and `SELECT count(*) FROM auth_session WHERE player_id = ?` reads `1` before the second sign-in and `2` after. Without this, the test above could be reading its answer through the surviving token |
| `theProfileSaysTheDeviceRouteIsNoLongerLive` | `meWithToken(newToken)`'s `deviceRouteLive` is `false`, and — read **before** the revocation, in the same test, through the first token — it was `true`. **Two reads of one field with two different expected values.** This is the assertion `TASK-040602` could not make: with `EXISTS (...)` replaced by the literal `true` in `PROFILE_OF_SQL`, this test is the one that reddens |
| `theDeviceItselfIsStillARouteToNothing` | `GET /api/me` presenting only the revoked device id answers `200` for a **different**, freshly minted `playerId` with `coinBalance` `0` and `displayName` `null` — the device is not refused, it simply is not this account any more (`ADR-0049` §3). Assert the id differs from the recorded one |

## Acceptance criteria

- [ ] `DeviceRevocationDatabaseTest.theSecondSignInReachesTheSameProfile` passes
- [ ] `DeviceRevocationDatabaseTest.theNewTokenIsNotTheRevokingToken` passes
- [ ] `DeviceRevocationDatabaseTest.theProfileSaysTheDeviceRouteIsNoLongerLive` passes
- [ ] `DeviceRevocationDatabaseTest.theDeviceItselfIsStillARouteToNothing` passes
- [ ] `theProfileSaysTheDeviceRouteIsNoLongerLive` asserts `true` before the revocation and `false`
      after, in one test method
- [ ] `theSecondSignInReachesTheSameProfile` compares all three fields against values recorded before
      the revocation, and contains no string literal for `playerId`
- [ ] Every test already in `DeviceRevocationDatabaseTest` still passes unchanged
- [ ] No file under `poker-server/src/main` is modified
- [ ] Every command in `verify:` exits 0

## Proof

Replace `PROFILE_OF_SQL`'s `EXISTS (SELECT 1 FROM device_binding …)` expression with the literal
`true` in `PostgresProfileReads`, then run this class.
**`theProfileSaysTheDeviceRouteIsNoLongerLive` reddens on its second assertion** — the field reads
`true` after the revocation — and it is the **only** test in the file that does, because it is the
only one that reads the profile of a player whose binding is revoked. Revert afterwards. This is the
red run `TASK-040602` recorded that it could not produce on its own.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
