---
schema: 2
id: TASK-040614
title: The revoked device says Hello and is seated as a stranger
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, socket, revocation, security]
depends_on: [TASK-040613]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DeviceRevocationDatabaseTest' -PrequireDocker=true
---

## Goal

A `Hello` presenting only a revoked device id is **not** seated as the player that revoked it: the
socket mints a fresh, empty profile, and the account's coins stay with the account.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DeviceRevocationDatabaseTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/db/SignInDatabaseTest.kt` (its `helloWith` helper,
which this ticket copies into the file above),
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §3 and §6.

## Scope

- Three methods added to the existing class, plus the private `helloWith(deviceId, sessionToken)`
  helper copied from `SignInDatabaseTest` if the file does not already hold it.
- The fixture the three tests share, in a **new** private helper of their own — `TASK-040613`'s
  `setUpOwner()` is not edited, because these tests need a duel and a balance behind them and that
  one deliberately has neither: `handshake(OWNER_DEVICE)` mints the
  profile; a `duel_result` pair written with raw SQL gives it a balance of `1`; `signUp(...)` attaches
  the credential; `signIn(...)` yields the token that revokes.
- **Every claim about who was seated is read from `Welcome.playerId`**, never inferred from a device
  id — the whole question is whether the server agrees with the client about which profile a device
  names.

## Out of scope

- Closing live sockets. `ADR-0049` §6 and `ADR-0050` §2 both say revocation closes none, and this
  ticket asserts that rather than changing it.
- `DuelSocket.kt` and every other file under `src/main`. If a test here fails, the ticket stops and
  reports.
- Signing in with the password afterwards — `TASK-040615`.

## Tests

`DeviceRevocationDatabaseTest`, three new methods.

| Test | Proves |
| --- | --- |
| `aRevokedDeviceIsSeatedAsSomebodyElse` | Record the owner's `playerId` from the first `Welcome`. Revoke. Say `Hello` again with the **same** device id and no token: the answer is a `Welcome` whose `playerId` **differs** from the recorded one. A second `Hello` from an untouched control device, recorded in the same test, still answers **its** original `playerId`. **Two devices, two different expected outcomes** — a `Hello` that had stopped resolving anything would pass the first half and fail the second |
| `theRevokedDevicesNewProfileHasNoCoins` | `GET /api/me` for the revoked device id reads `coinBalance` `0`, while `meWithToken(survivingToken)` reads `1` for the same player who owned that device before. Two reads, two different expected values: with the account at `0` too, this test could not tell a fresh profile from the old one |
| `aSocketOpenedBeforeTheRevocationIsNotClosed` | Open a `/ws` session for the owner's device, complete its handshake, then revoke over HTTP, then send a frame on that **same** session and read the answer. The session is still open and still answers; nothing arrives that closes it. `ADR-0050` §2's *"no live socket is closed"*, which is a promise about a running duel not being folded out from under a player |

## Acceptance criteria

- [ ] `DeviceRevocationDatabaseTest.aRevokedDeviceIsSeatedAsSomebodyElse` passes
- [ ] `DeviceRevocationDatabaseTest.theRevokedDevicesNewProfileHasNoCoins` passes
- [ ] `DeviceRevocationDatabaseTest.aSocketOpenedBeforeTheRevocationIsNotClosed` passes
- [ ] Every test already in `DeviceRevocationDatabaseTest` still passes, and no existing assertion is
      edited
- [ ] `aRevokedDeviceIsSeatedAsSomebodyElse` asserts on two device ids, and asserts inequality for
      one and equality for the other
- [ ] `theRevokedDevicesNewProfileHasNoCoins` asserts two different balances, `0` and `1`
- [ ] No file under `poker-server/src/main` is modified
- [ ] Every command in `verify:` exits 0

## Proof

Mutate `PostgresPlayerDirectory.resolve`'s live-binding read: drop ` AND revoked_at IS NULL` from it.
`aRevokedDeviceIsSeatedAsSomebodyElse` reddens — the second `Welcome` names the original player
again — and `theRevokedDevicesNewProfileHasNoCoins` reddens with it, because `GET /api/me` for that
device now reads `1` instead of `0`. **Two tests.**
`aSocketOpenedBeforeTheRevocationIsNotClosed` stays green: identity is fixed at `Hello` and a socket
re-reads nothing (`ADR-0027` §3), so which profile the revoked device resolves to afterwards does
not reach it. Revert the mutation.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`aSocketOpenedBeforeTheRevocationIsNotClosed` cannot fail for the reason its name gives.** The
reviewer deleted the `revokeDevice(...)` call outright — so no revocation happens at all — and the
test still passed. `DuelSocket` answers every second `Hello` with `Failure(MALFORMED_MESSAGE)`
unconditionally and never consults revocation state, so the property holds because **no mechanism to
close on revocation exists**. The ticket's own Proof asks only that this test stay green, so this is
a ticket-level limit on proving an absence, not a shortcut. Its value is prospective: if socket
closing on revocation is ever added, this test reddens.

**The identity assertion is the load-bearing one.** Three behaviours look like success — seating a
new player, refusing the connection, and **reseating the original account**. Only
`assertNotEquals(owner.ownerId, revoked.playerId)` separates the third, and the third is the one that
would mean revocation never severed the route. Both sides read `Welcome.playerId`, never a device id,
and the pre-revocation id is captured before any of the seeding, sign-up or revocation.

**Reconnect idempotence is covered elsewhere, genuinely.** `DuelSocket` resolves through the same
`PostgresPlayerDirectory.resolve()` that `TASK-040605`'s
`aSecondResolveOfTheRevokedDeviceIsIdempotent` already pins — there is no separate minting path for
sockets, so declining to re-test it here left no gap.

