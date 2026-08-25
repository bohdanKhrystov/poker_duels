---
schema: 2
id: TASK-040708
title: Signing out returns the fresh browser to nothing, and the original device to itself
type: task
status: backlog
parent: STORY-0407
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, e2e, auth]
depends_on: [TASK-040707]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

Signing out on the fresh browser leaves it a first-time visitor over HTTP — `401`, no profile — while
the original device still reads the account it always had. `ADR-0030` §3: sign-out restores whatever
the device had, and a never-seen device had nothing.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §3,
`poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` (its `signOut`
helper is the one to copy).

## Scope

- Four steps appended to `runRecovery()`, after everything `TASK-040707` added:
  1. `POST /api/auth/sign-out` with the session token, asserted `204 No Content` in place.
  2. `sessionsAfterSigningOut = dataSource.authSessionRowCount()`.
  3. `freshBrowserStatusAfterSignOut` — `GET /api/me` carrying `X-Device-Id: FRESH_DEVICE` and **no
     `Authorization` header at all**. This is the story's fifth criterion and the branch it needs is
     `Identity.UnknownDevice`, which is only reached when both halves are true: a device the
     directory does not know **and** no token beside it.
  4. `tokenStatusAfterSignOut` — `GET /api/me` carrying the now-deleted token and no device id.
  5. `originalProfileAfterSignOut = client.profileOf(winner.deviceId)`.
- `dataSource.assertCoinInvariantHolds(...)` once after the four, with a new distinct step string.
  `runRecovery()` then holds nine calls in total.
- Two private helpers: `HttpClient.signOut(token): HttpStatusCode`, copied from
  `IdentityMovesNoCoinTest`, and

  ```kotlin
  private suspend fun HttpClient.profileStatus(deviceId: String? = null, token: String? = null): HttpStatusCode
  ```

  which sets each header only when its argument is non-null and returns the status without asserting
  it. `profileOf` asserts `200` and decodes, so it cannot express a refusal.
- Four new `RecoveryRecord` fields with `@property` lines: `sessionsAfterSigningOut`,
  `freshBrowserStatusAfterSignOut`, `tokenStatusAfterSignOut`, `originalProfileAfterSignOut`.

## Out of scope

- **Opening a socket from the fresh browser after sign-out.** A `Hello` carrying an unknown device id
  and no token reaches `Identity.UnknownDevice`, and the socket *mints* there (`ADR-0027` §4) — that
  is correct behaviour, not a defect, and a test asserting "no profile" against it would be asserting
  something false. The claim after sign-out is an HTTP one.
- Signing back in, or a second token. One sign-out is the story's criterion.
- The live socket the fresh browser still holds. `ADR-0030` §3 is explicit that sign-out closes no
  sockets, and why; `STORY-0405` gates that and this ticket must not restate it.
- Any file under `poker-server/src/main`.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `afterSigningOutTheFreshBrowserIsAStranger` | `freshBrowserStatusAfterSignOut` is `401 Unauthorized` — the device that recovered an account holds no profile of its own |
| `theTokenStopsWorkingAfterSigningOut` | `tokenStatusAfterSignOut` is `401 Unauthorized` |
| `signingOutDeletesTheOneSessionRow` | `sessionsAfterWrongPassword` is `1` and `sessionsAfterSigningOut` is `0`. **Two inputs, two different expected values**, and a count rather than a boolean: without the `1`, a `0` after sign-out is equally consistent with a session that was never issued |
| `theOriginalDeviceIsUnaffectedBySigningOut` | `originalProfileAfterSignOut` equals `originalProfile`, whole value — the same player, coin, name and device route it had before any of this began |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.afterSigningOutTheFreshBrowserIsAStranger` passes
- [ ] `RecoveryOnAFreshBrowserTest.theTokenStopsWorkingAfterSigningOut` passes
- [ ] `RecoveryOnAFreshBrowserTest.signingOutDeletesTheOneSessionRow` passes
- [ ] `RecoveryOnAFreshBrowserTest.theOriginalDeviceIsUnaffectedBySigningOut` passes
- [ ] The fresh browser's post-sign-out request sets `X-Device-Id` and **no** `Authorization` header
- [ ] `signingOutDeletesTheOneSessionRow` asserts `1` and then `0`, from two recorded counts
- [ ] `theOriginalDeviceIsUnaffectedBySigningOut` compares two whole `ProfileResponse` values in one
      `assertEquals`
- [ ] No socket is opened after the sign-out step
- [ ] `runRecovery()` contains exactly nine calls to `assertCoinInvariantHolds`, with nine different
      step strings
- [ ] Every test method added by `TASK-040702` through `TASK-040707` still passes with its assertions
      unchanged
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt`, delete the
`sessions.delete(token)` call from the sign-out handler, leaving the `204` it answers.

**`theTokenStopsWorkingAfterSigningOut` and `signingOutDeletesTheOneSessionRow` redden; the other two
stay green.** Trace it: the row survives, so the count after sign-out is `1` rather than `0`, and the
token still resolves, so `GET /api/me` under it answers `200`. `afterSigningOutTheFreshBrowserIsAStranger`
sends no token at all and is unaffected; `theOriginalDeviceIsUnaffectedBySigningOut` reads by device
id and is unaffected. Revert.

A second mutation, reddening exactly one. In
`poker-server/src/main/kotlin/duels/poker/server/auth/IdentityResolver.kt`, change
`val player = players.findOrNull(deviceId)` to `val player = players.resolve(deviceId)` — the
never-refuse variant, which mints rather than answering `null`. Kotlin will warn that the following
`player != null` is always true; it compiles.

**`afterSigningOutTheFreshBrowserIsAStranger` reddens alone**, answering `200` where `401` is
expected. Nothing else moves, and each reason is worth checking rather than assuming:
`resolveOverConnection` reads the live binding **first** and returns it, so every device-keyed read
earlier in the arc still resolves to the player it always did; every fresh-browser read presents a
token, and `IdentityResolver` returns on the token branch before a device is consulted, so
`TASK-040706`'s bracket sees nothing; and the profile minted by this one call has `coin_balance` `0`
and no `duel_result` rows, so P1 and P2 both hold and `theRecoveryArcMovesNoCoin` stays green.
Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This is the case a reader assumes is broken**, which is the story's own reason for asserting it: it
looks as though signing out ought to leave the browser holding *something*. It does not, and
`ADR-0030` §3 says the mechanism is subtraction — remove the higher-precedence edge, and whatever
lower edge existed becomes visible again. Here there was none.
