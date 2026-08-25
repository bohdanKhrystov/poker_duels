---
schema: 2
id: TASK-041608
title: A second claim replaces the first, in one transaction
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, db, auth, security]
depends_on: [TASK-041607]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsClaimTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`PostgresRecoveryEmails.claimPending` writes a pending row that expires 24 hours from the injected
clock, replacing whatever the player already had, so an abandoned attempt cannot accumulate.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsClaimTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` — the
`DataSource` + `java.time.Clock` constructor, the `Dispatchers.IO` wrapping, the
`OffsetDateTime.ofInstant(..., ZoneOffset.UTC)` binding and the companion-held SQL constants this
class copies;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`;
`poker-server/src/main/kotlin/duels/poker/server/db/RecoveryTokenDigest.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3.

## Scope

- `internal class PostgresRecoveryEmails(private val dataSource: DataSource, private val clock:
  Clock) : RecoveryEmails`, implementing **only** `claimPending` in this ticket; the other five
  members throw `NotImplementedError` via `TODO()` with a comment naming the ticket that fills each.
- `claimPending` opens one connection, sets `autoCommit = false`, runs
  `DELETE FROM email_verification WHERE player_id = ?` then
  `INSERT INTO email_verification (token_hash, player_id, address, issued_at, expires_at) VALUES
  (?, ?, ?, ?, ?)`, and commits — one transaction, so `UNIQUE (player_id)` can never refuse the
  insert (`ADR-0031` §3). Roll back and rethrow on any `SQLException`.
- `issued_at` is `clock.instant()`; `expires_at` is that plus a private
  `VERIFICATION_LIFETIME: Duration = Duration.ofHours(24)`. The **instant comes from the injected
  `java.time.Clock`**, not `ServerClock` — `ADR-0062` §5 amends `ADR-0031` §3's clause for exactly
  this reason, and `ServerClock` reports elapsed nanos from an arbitrary epoch.
- The stored `address` is `address.value` **unchanged** — no fold, no trim, no lowercase.

## Out of scope

- `verifyPending`, `hasRecoveryEmail`, `verifiedOwnerOf`, `detach`, `deleteExpiredVerifications` —
  `TASK-041609` through `TASK-041611`. Their `TODO()` bodies are deliberate and the ticket that
  removes each is named at the call site.
- Requiring the current password. That is the endpoint's job (`TASK-041625`), because the port has
  no `Credentials` and should not grow one.
- Sending anything. `claimPending` mints and stores; the mail is `DEC-072`'s.
- Wiring the class into `ServerComponents` — `TASK-041627`.

## Tests

`PostgresRecoveryEmailsClaimTest`, on `PostgresTestSupport.freshDatabase()` + `Migrations.migrate`,
constructing the class with a `Clock.fixed(...)` so every instant is pinned.

| Test | Proves |
| --- | --- |
| `aClaimStoresTheAddressExactlyAsTyped` | After `claimPending(player, EmailAddress("Bob@Example.com"), token)`, `SELECT address` returns `Bob@Example.com`, and `SELECT token_hash` equals `recoveryTokenDigest(token)` — the plaintext token is nowhere in the row |
| `aClaimExpiresTwentyFourHoursAfterTheInjectedClock` | With `Clock.fixed` at a known instant, `expires_at` minus `issued_at` is exactly 24 hours and `issued_at` equals the fixed instant. Advancing the injected clock by an hour and claiming again moves both stamps by an hour — **two clock values**, so a hard-coded `Instant.now()` cannot pass |
| `aSecondClaimLeavesExactlyOnePendingRow` | The same player claims `a@x.test`, then `b@x.test` with a different token. `SELECT count(*)` for that player is `1`, and the surviving row's `address` is `b@x.test`. No `SQLException` is thrown |
| `oneClaimNeverDisturbsAnotherPlayers` | Two players each claim; the second's claim leaves the first's row present with its original address and token hash. The count assertion above is under `player_id`, which the defect would still satisfy, so this is what actually rules out a `DELETE` with no `WHERE` |

## Acceptance criteria

- [ ] `PostgresRecoveryEmailsClaimTest.aClaimStoresTheAddressExactlyAsTyped` passes
- [ ] `PostgresRecoveryEmailsClaimTest.aClaimExpiresTwentyFourHoursAfterTheInjectedClock` passes
- [ ] `PostgresRecoveryEmailsClaimTest.aSecondClaimLeavesExactlyOnePendingRow` passes
- [ ] `PostgresRecoveryEmailsClaimTest.oneClaimNeverDisturbsAnotherPlayers` passes
- [ ] The expiry test uses **two different fixed clock instants** and asserts both stamps moved
- [ ] `PostgresRecoveryEmails.kt` contains no `Instant.now()`, no `System.currentTimeMillis()` and
      no `ServerClock`
- [ ] `PostgresRecoveryEmails.kt` contains no `lowercase`, `uppercase` or `trim` applied to an
      address
- [ ] The `DELETE` and the `INSERT` run on one connection with `autoCommit = false` and a single
      `commit()`
- [ ] Every command in `verify:` exits 0

## Proof

1. Replace `clock.instant()` with `Instant.now()`.
   **`aClaimExpiresTwentyFourHoursAfterTheInjectedClock` reddens alone**, on the assertion that
   `issued_at` equals the fixed instant. Note what the *duration* half of that test would have done:
   `expires_at - issued_at` is still 24 hours, so a test that checked only the difference would have
   stayed green. That is why the criterion demands two clock values and an absolute assertion.
   Revert.
2. Change `VERIFICATION_LIFETIME` to `Duration.ofHours(1)`.
   **`aClaimExpiresTwentyFourHoursAfterTheInjectedClock` reddens alone**, *expected PT24H, got
   PT1H*. Revert.
3. Delete the `DELETE FROM email_verification WHERE player_id = ?` statement.
   **`aSecondClaimLeavesExactlyOnePendingRow` reddens alone**, with an `SQLException` naming
   `email_verification_one_per_player` — the second insert now collides. Revert.
4. Widen the delete to `DELETE FROM email_verification` with no `WHERE`.
   **`oneClaimNeverDisturbsAnotherPlayers` reddens alone**: the first player's row is gone.
   `aSecondClaimLeavesExactlyOnePendingRow` counts rows *for one player* and still reads `1`, so it
   stays green — this is the mutation that justifies a fourth test, and it is the exact shape of "a
   count under a unique key is a tautology". Revert.
5. Store `address.value.lowercase()`. **`aClaimStoresTheAddressExactlyAsTyped` reddens alone**,
   *expected Bob@Example.com, got bob@example.com*. Revert.
6. Store `token.value` in `token_hash` instead of the digest — this needs the column read as text,
   so instead store `recoveryTokenDigest(VerificationToken(token.value + "x"))`.
   **`aClaimStoresTheAddressExactlyAsTyped` reddens alone** on the hash comparison. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
