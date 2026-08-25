---
schema: 2
id: TASK-041615
title: A session holder proves the password they already have
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 6
atomic:
  - the Kotlin compiler — Credentials gains the abstract member verifyCurrent, so PostgresCredentials must implement it in the same commit
  - the Kotlin compiler again — TestDoubleCredentials, RecordingCredentials and SignInCredentials implement Credentials and fail with "is not abstract and does not implement abstract member 'verifyCurrent'" without it
  - CredentialsPortTest.noFunctionOnThePortReturnsAString — a golden set of Credentials' member names, asserted at run time, reddens once verifyCurrent exists
labels: [server, db, auth, security]
depends_on: [TASK-041614]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresCredentialsCurrentPasswordTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresCredentialsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`Credentials` can answer *is this the current password of this player* — the check both
recovery-email endpoints need, and which no existing method can perform, because `verify` is keyed
by identifier and a session holder presents no identifier.

## Why this exists

`ADR-0031` §3 requires the current password to attach an address *"even inside a valid session"*,
and §5 gives both `POST` and `DELETE /api/auth/recovery-email` a `403` for a wrong one. The reason
is stated plainly: a session token is a bearer credential in web storage, and *"without this, a
minute at an unattended browser converts into permanent ownership of the account."*

`Credentials.verify(kind, identifier, presented)` cannot do it. The caller is identified by session,
so the server holds a `PlayerId` and not a handle — and looking the handle up to feed `verify` would
put a *reverse* lookup from player to identifier into the codebase, which is a shape nothing else
needs and which makes `credential.identifier` readable.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsCurrentPasswordTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/CredentialsPortTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteTest.kt` | modify |

The last three rows are `ADR-0070` §4 propagation, forced by the gates `atomic:` names above: each
implements the new member (`RecordingCredentials` and `SignInCredentials` throw, matching their
own existing out-of-scope idiom; `TestDoubleCredentials` returns `true`, matching its existing
unconditional-success `verify`), and `CredentialsPortTest`'s golden set of member names gains
`verifyCurrent`.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/PasswordPolicy.kt` — `passwordIsWithinTheWork
Bound`, which this path must apply before hashing;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsTest.kt` — the fixture shape;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and §5.

## Scope

- `Credentials` gains one member:
  `suspend fun verifyCurrent(playerId: PlayerId, kind: CredentialKind, presented: PresentedSecret):
  Boolean`.
- `PostgresCredentials.verifyCurrent` reads `secret_hash` by `(player_id, kind)` and calls the same
  `matches` the existing `verify` calls. **It applies `passwordIsWithinTheWorkBound` before
  hashing**, exactly as sign-in does — an unbounded secret here is the same denial of service
  through a different door.
- **No dummy-hash path, and this is deliberate.** `verify`'s constant-time dummy hash exists because
  its caller is a stranger who must not learn whether a handle exists. This caller has already
  presented a valid session for the player in question, so there is nothing left to enumerate: a
  player without a `password` credential is a fact they already know about themselves. Returning
  `false` immediately is correct, and the KDoc must say so — otherwise somebody adds the dummy hash
  back "for consistency" and pays Argon2 on every wrong-password `403`.
- `verifyCurrent` never returns a `PlayerId`; the caller already has one. A `PlayerId?` return would
  invite using it as a login path.

## Out of scope

- Changing `verify`, `create` or `holdsCredential`. Their signatures and their dummy-hash behaviour
  are `ADR-0027` §6's and are untouched — asserted by `PostgresCredentialsTest` passing unchanged,
  which is why it is in `verify:`.
- The endpoints' `401`/`403` — `TASK-041623` and `TASK-041625`.
- Rehashing on a raised Argon2 cost. `ADR-0054` puts that in `verify` alone, on the sign-in path;
  adding it here would be a second compare-and-set nothing asked for.
- Rate-limiting this check. It is behind a session, so the enumeration and pool arguments that
  produced `ADR-0074` do not apply; if that turns out to be wrong it is a new decision, not a
  silent addition.

## Tests

`PostgresCredentialsCurrentPasswordTest`

| Test | Proves |
| --- | --- |
| `theRightPasswordIsAccepted` | A player with a `password` credential: `verifyCurrent` with the secret used at creation returns `true` |
| `theWrongPasswordIsRefused` | The same player, a different secret: `false`. Two inputs against one fixture, so a method returning a constant fails one of them |
| `anotherPlayersPasswordIsRefused` | Two players with **different** secrets. Player A's secret presented for player B returns `false`, and each player's own returns `true`. Guards a read that ignores `player_id` |
| `aPlayerWithNoCredentialIsRefused` | A player who never signed up: `false`, and no exception |
| `anOverlongSecretIsRefusedWithoutHashing` | A 129-code-point secret returns `false`. Asserted by the return value; the *without hashing* half is not observable from outside and is a review criterion, named here rather than left to be discovered |

## Acceptance criteria

- [ ] All five `PostgresCredentialsCurrentPasswordTest` tests pass
- [ ] `PostgresCredentialsTest` passes **unchanged** — no assertion in it moves
- [ ] `anotherPlayersPasswordIsRefused` holds **two** players with **two different** secrets and
      asserts all four combinations that matter (each own = `true`, each cross = `false`)
- [ ] `verifyCurrent` returns `Boolean`, not `PlayerId?`
- [ ] `PostgresCredentials.verifyCurrent` contains no reference to `DUMMY_PHC` or any dummy hash,
      and its KDoc says why
- [ ] `verifyCurrent` calls `passwordIsWithinTheWorkBound` before any hashing call
- [ ] Every command in `verify:` exits 0

## Proof

1. Change the lookup to `WHERE kind = ?` only, dropping `player_id`.
   **`anotherPlayersPasswordIsRefused` reddens alone**, on a cross combination. `theRightPassword
   IsAccepted` and `theWrongPasswordIsRefused` hold one player and pass — the two-player fixture is
   the only thing that sees it. Revert.
2. Return `true` unconditionally when a row is found.
   **`theWrongPasswordIsRefused` and `anotherPlayersPasswordIsRefused` both redden**;
   `aPlayerWithNoCredentialIsRefused` still passes because no row is found. Revert.
3. Return `false` unconditionally.
   **`theRightPasswordIsAccepted` reddens alone**, and it is the only positive control in the file.
   Run it — a `verifyCurrent` that never says yes would otherwise pass three of five tests. Revert.
4. Remove the `passwordIsWithinTheWorkBound` guard.
   **Nothing reddens**, because a 129-code-point secret still fails to match. Record that result:
   the fifth test's *without hashing* claim is **not** gated, and the criterion above is a review
   criterion for that reason. Do not add a timing assertion to manufacture one — this repository has
   precedent for saying an untestable criterion is untestable, at `DELETE /api/me/device` where a
   malformed credential is indistinguishable from an absent one. Revert.
5. Change `matches` to compare the presented secret against the stored PHC string directly.
   **`theRightPasswordIsAccepted` reddens alone.** Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
