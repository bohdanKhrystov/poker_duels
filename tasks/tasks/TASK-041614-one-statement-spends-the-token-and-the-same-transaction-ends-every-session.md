---
schema: 2
id: TASK-041614
title: One statement spends the token, and the same transaction ends every session
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, auth, security, invariant]
depends_on: [TASK-041613]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPasswordResetsConsumeTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`consume` spends a reset token, writes the new password hash and deletes every one of that player's
`auth_session` rows — all in one transaction, so a reset that succeeds cannot leave the attacker's
session running.

## Why this exists

`ADR-0031` §4 makes two claims that are only true if they are one transaction. *Single use, by
construction, not by a flag*: the token is consumed by `DELETE … RETURNING`, so **no read-then-write
window exists** and two concurrent submissions cannot both succeed. And *"a reset that leaves the
attacker's session running for another 30 days is worse than no reset, because it looks like it
worked."*

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresPasswordResets.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPasswordResetsConsumeTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` — the Argon2id hashing
path this reuses; the reset must write through the **same** hasher and parameters as sign-up
(`ADR-0031` §4, `ADR-0054`);
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt`;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreConcurrencyTest.kt` — the
two-threads-one-latch shape the concurrency test copies;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- `consume(token, secret): Boolean` on one connection with `autoCommit = false`:
  1. `DELETE FROM password_reset WHERE token_hash = ? AND expires_at > now() RETURNING player_id`.
     No row ⇒ commit and return `false`.
  2. `UPDATE credential SET secret_hash = ? WHERE player_id = ? AND kind = 'password'`, with the
     hash from the same Argon2id path `PostgresCredentials.create` uses.
  3. `DELETE FROM auth_session WHERE player_id = ?` — **unconditionally**, served by the existing
     `auth_session_player_id_idx`.
  4. Commit, return `true`.
- Anything but exactly one row from step 2 rolls back and returns `false`: a token whose player has
  somehow lost their credential must not consume the token and delete their sessions for nothing.
- No `used_at` column, no `SELECT` on `password_reset` anywhere in this method.
- `consume` **issues no session and returns no token** — its return type is `Boolean` and there is
  nothing else for it to hand back.

## Out of scope

- The password policy. `passwordIsLongEnough` and `passwordIsWithinTheWorkBound` run at the
  **endpoint** (`TASK-041629`, not `TASK-041620`), and under `ADR-0080` §1 they run **in front of
  this port**: the order is decode ⇒ policy ⇒ `consume`, so a `422` never reaches `consume` at all
  and the endpoint answers it whether or not the token is good. A distinction this port cannot
  express and must not try to — which is why `ADR-0080` §7 keeps `consume(token, secret): Boolean`
  unchanged and gives it no third outcome.
- The endpoint's `204`/`400` — `TASK-041620` through `TASK-041622` — and its `422`, which is
  `TASK-041629`'s alone.
- A second Argon2 path or second parameter set. `ADR-0031` §4: *"This ADR adds no second password
  rule and no second hashing path."*
- Deleting the caller's own session selectively. This is not `ADR-0050`'s revoke: **every** session
  goes, including the one used to request the reset, because the reset endpoint requires no session
  at all.
- Sweeping expired `password_reset` rows — expiry is in the statement.

## Tests

`PostgresPasswordResetsConsumeTest`

| Test | Proves |
| --- | --- |
| `aGoodTokenRewritesThePasswordAndReturnsTrue` | After `consume`, `Credentials.verify` succeeds with the new secret and **fails with the old one**. Both halves, because a no-op `UPDATE` passes the first |
| `theSecondUseOfATokenIsRefused` | `consume` twice sequentially: the second returns `false`, and `verify` against a third secret still fails — nothing was rewritten |
| `twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` | Two threads on one latch call `consume` with the same token and **two different new secrets**. Exactly one returns `true`, and the stored hash matches exactly one of the two secrets. The `DELETE … RETURNING` is what makes this hold; a `SELECT`-then-`DELETE` fails here and nowhere else |
| `aTokenPastItsHourIsRefused` | Issue, advance the injected clock 61 minutes, consume: `false`, the password is unchanged, and the player's sessions **still exist** — a refused reset ends nothing. No test sleeps |
| `aSuccessfulResetEndsEverySessionThePlayerHeld` | The player holds two sessions before the reset; after it, `AuthSessions.playerOf` answers `null` for **both**, and a *second* player's session is untouched. Two players, because a `DELETE FROM auth_session` with no `WHERE` passes the first half |

## Acceptance criteria

- [ ] All five `PostgresPasswordResetsConsumeTest` tests pass
- [ ] `aGoodTokenRewritesThePasswordAndReturnsTrue` asserts the **old** secret no longer verifies
- [ ] `twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` uses two **different** new secrets and
      asserts the stored hash matches exactly one of them
- [ ] `aTokenPastItsHourIsRefused` asserts the player's sessions survive the refusal
- [ ] `aSuccessfulResetEndsEverySessionThePlayerHeld` holds **two** sessions for the resetting
      player and **one** for a second player
- [ ] `PostgresPasswordResets.kt` contains no `SELECT` against `password_reset` inside `consume`,
      and no `used_at`
- [ ] `consume` returns `Boolean` and no session token
- [ ] Every command in `verify:` exits 0

## Proof

1. Replace the `DELETE … RETURNING` with `SELECT player_id FROM password_reset WHERE token_hash = ?
   AND expires_at > now()` followed by a `DELETE` **after** the credential update.
   **`twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` reddens alone**, with two successes and a
   stored hash matching whichever write landed second. `theSecondUseOfATokenIsRefused` runs
   sequentially and still passes, because the row is gone by the time the second call reads — which
   is precisely why the concurrent test is required and why "sequential single-use" is not the same
   property. Run this one; it is the mutation most likely to be flaky, and if it passes on the
   first attempt, raise the thread count rather than accepting the green.
2. Drop `AND expires_at > now()`. **`aTokenPastItsHourIsRefused` reddens alone.** Revert.
3. Delete step 3, leaving sessions alive.
   **`aSuccessfulResetEndsEverySessionThePlayerHeld` reddens alone.** Revert.
4. Widen step 3 to `DELETE FROM auth_session` with no `WHERE`.
   **`aSuccessfulResetEndsEverySessionThePlayerHeld` reddens alone**, on the second player's
   surviving session. Its first half — both of the resetting player's sessions gone — is *more*
   satisfied by the mutation, so the two-player fixture is the only thing that catches it. Revert.
5. Move step 3 **before** step 1, so sessions die whatever the token turns out to be.
   **`aTokenPastItsHourIsRefused` reddens alone**, on its surviving-sessions assertion. This is the
   defect where an attacker with any expired or invented token signs a victim out at will, and the
   only assertion in this file that sees it. Revert.
6. Make step 2 a no-op `UPDATE credential SET secret_hash = secret_hash WHERE player_id = ?`.
   **`aGoodTokenRewritesThePasswordAndReturnsTrue` reddens** on the new-secret assertion **and
   `twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` reddens** on its stored-hash assertion. Two,
   and `theSecondUseOfATokenIsRefused` stays green because it never checks that a write happened.
   Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
