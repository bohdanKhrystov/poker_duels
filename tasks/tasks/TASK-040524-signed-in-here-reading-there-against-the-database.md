---
schema: 2
id: TASK-040524
title: Signed in here, reading there — the whole flow against the database
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, session, e2e, db]
depends_on: [TASK-040522]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SignInDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The story's own acceptance criteria are proven once, end to end, against a real schema: a real
sign-up, a real sign-in, a token that reads the right profile over HTTP and seats the right player
on the socket, a sign-out that answers `204` twice, and a `player` table nobody moved.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SignInDatabaseTest.kt` | create |

Read `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` — the fixture, the
`HttpClient` helpers and the `player`-snapshot assertion are all there and are **reused, not
rewritten** — and `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §§2 and 5.
Nothing else.

## Scope

- One Testcontainers test class booting the shipping server through `duelServer(serverComponents(…))`,
  so what is exercised is the wiring, not a hand-assembled route set.
- **Two devices and two profiles throughout.** `d-owner` signs up as `owner` and wins a duel, so it
  holds a coin balance of `1`; `d-other` is a second, untouched anonymous profile with a balance of
  `-1`. Every assertion below names a balance as well as a player id, because a fixture where both
  profiles read `0` cannot tell the session's row from the device's.
- Snapshot every `player` row — id, `device_id`, `coin_balance`, `display_name` — before and after
  the sign-in, and assert the multiset is byte-identical. `ADR-0030` §2's whole claim is that
  sign-in writes nothing to `player`, and this is what makes it checkable rather than promised.
- The socket half uses the existing socket-test helpers: connect with
  `Hello(deviceId = "d-other", sessionToken = <the token>)` and read the `Welcome`.

## Out of scope

- The coin properties P1 and P2 as a reusable fixture helper — `ADR-0030` §5, `STORY-0406`.
- Device revocation, the claim's proof, and recovery as a scenario — `STORY-0406`, `STORY-0407`.
- Anything a browser does — `STORY-0412`, `STORY-0414`.

## Tests

`SignInDatabaseTest`

| Test | Proves |
| --- | --- |
| `aCorrectCredentialAnswersATokenThatReadsThatPlayersProfile` | `POST /api/auth/sign-in` answers `200`; the token on `GET /api/me` answers `owner`'s id and a balance of `1` |
| `theTokenOutranksTheDeviceItTravelsWith` | the same token sent **with** `X-Device-Id: d-other` still answers `owner`'s id and `1`, not `d-other`'s id and `-1` |
| `theDevicesOwnProfileIsUnchangedByAnyOfIt` | `d-other` alone still answers its own id and `-1`, after the signed-in reads |
| `signingInWritesNothingToPlayer` | the `player` snapshot is byte-identical across the sign-in |
| `theSocketSeatsTheSessionsPlayer` | `Hello(deviceId = "d-other", sessionToken = token)` answers a `Welcome` whose `playerId` is `owner`'s and whose `deviceId` is `null` |
| `anExpiredSessionIsRefusedOverHttpAndOnTheSocket` | a row written through `PostgresAuthSessions` on a clock 31 days back answers `401` over HTTP and `Failure(INVALID_SESSION)` on the socket. **No test sleeps** |
| `signingOutAnswersTwoHundredAndFourTwiceAndThenTheDeviceIsItselfAgain` | two `204`s, then `GET /api/me` with `X-Device-Id: d-other` and no token answers `d-other`'s own profile and `-1` — `ADR-0030` §3's *subtraction* restore, with nothing to restore |
| `aBrowserThatSignsInHavingNeverConnectedGetsNoDeviceAndNoRow` | a socket carrying only the token, from a device id that appears nowhere: `Welcome.deviceId` is `null`, and `SELECT count(*) FROM player` is unchanged |

## Acceptance criteria

- [ ] All eight test methods above pass
- [ ] No test in this file calls `Thread.sleep` or `delay`
- [ ] The fixture's two profiles differ in both `playerId` and `coinBalance`
- [ ] `git diff --name-only` names exactly one file
- [ ] Every command in `verify:` exits 0

## Proof

Make `IdentityResolver` prefer the device over the session and
`theTokenOutranksTheDeviceItTravelsWith` and `theSocketSeatsTheSessionsPlayer` both go red. Add an
`UPDATE player SET device_id = ?` to the sign-in handler and only `signingInWritesNothingToPlayer`
catches it — which is why the snapshot is a multiset comparison and not a spot check on one row.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
