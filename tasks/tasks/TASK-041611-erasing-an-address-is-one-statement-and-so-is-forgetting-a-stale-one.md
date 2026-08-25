---
schema: 2
id: TASK-041611
title: Erasing an address is one statement, and so is forgetting a stale one
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, db, auth, privacy]
depends_on: [TASK-041610]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsDeletesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`detach` erases a player's address in one statement, and `deleteExpiredVerifications` removes
unproven addresses whose day has passed — the two deletes that keep `ADR-0031`'s answer to *erase
my email* a single `DELETE`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsDeletesTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsReadsTest.kt` — the
fixture this class reuses;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3's *"Expired rows are deleted, and that
deletion is not optional"* and its closing `DEC-029` note.

## Scope

- `detach(playerId)`: `DELETE FROM recovery_email WHERE player_id = ?`, returning `Unit`. It does
  **not** report whether a row existed — §5's endpoint answers `204` either way, and a port that
  reported it would put an oracle one refactor away.
- `deleteExpiredVerifications(): Int`: `DELETE FROM email_verification WHERE expires_at <= now()`,
  returning `executeUpdate()`'s count.
- The count is returned so the sweep can log a number and a test can assert one. It carries no
  address and no player id — §6.4 forbids a delivery log with an address, and the same reasoning
  applies to a retention log.
- Both statements delete from one table only. `detach` does not touch `email_verification`: a
  detached player may hold a live pending claim on a *different* address, and destroying it would
  cancel an attach they are midway through.

## Out of scope

- The endpoint, its `401`/`403` and the current-password check — `TASK-041623`.
- Calling `deleteExpiredVerifications` from the ticker — `TASK-041612`.
- Deleting expired `password_reset` rows. `ADR-0031` §3 requires the sweep for
  `email_verification` because a row there holds **an unproven address**; a `password_reset` row
  holds no personal data and is inert garbage, exactly like an expired `auth_session`. Adding it
  would be a second statement nothing asked for.
- Any `ON DELETE` behaviour. `DEC-029` stays unanswered and these are explicit deletes.

## Tests

`PostgresRecoveryEmailsDeletesTest`

| Test | Proves |
| --- | --- |
| `detachingLeavesNoAddressBehind` | After claim + verify + `detach`, `hasRecoveryEmail` is `false` and `SELECT count(*) FROM recovery_email` is `0`. The address is gone from the only column it lived in |
| `detachingOnePlayerLeavesTheOtherAlone` | Two verified players; detaching the first leaves the second's row intact with its original address. Guards a `DELETE` with no `WHERE` |
| `detachingNothingIsNotAnError` | `detach` on a player who never attached completes normally and throws nothing |
| `detachingLeavesALivePendingClaimAlone` | A player with a verified address **and** a pending claim on a second address: after `detach`, `email_verification` still holds their pending row |
| `theSweepTakesOnlyRowsPastTheirDay` | Two pending rows, claimed 25 hours apart on an advancing injected clock. `deleteExpiredVerifications()` returns `1`, the older row is gone and the newer one remains. **Two rows with two fates**, so a statement with no `WHERE` and one that matches nothing are both caught |
| `theSweepReportsZeroWhenNothingIsStale` | With one fresh pending row, `deleteExpiredVerifications()` returns `0` and the row survives |

## Acceptance criteria

- [ ] All six `PostgresRecoveryEmailsDeletesTest` tests pass
- [ ] `theSweepTakesOnlyRowsPastTheirDay` asserts the **returned count** and the **surviving row**,
      not just one of them
- [ ] The sweep test advances an injected `Clock`; the file contains no `Thread.sleep`
- [ ] `detach`'s statement names `recovery_email` and does not name `email_verification`
- [ ] `detach` returns `Unit` and its signature reports no row count
- [ ] `PostgresRecoveryEmails.kt` no longer contains any `TODO()`
- [ ] Every command in `verify:` exits 0

## Proof

1. Drop the `WHERE player_id = ?` from `detach`.
   **`detachingOnePlayerLeavesTheOtherAlone` reddens alone.** `detachingLeavesNoAddressBehind` holds
   one player and its `count(*)` of `0` is *more* satisfied by the mutation, so it stays green —
   which is exactly why the second test exists. Revert.
2. Change `detach` to also `DELETE FROM email_verification WHERE player_id = ?`.
   **`detachingLeavesALivePendingClaimAlone` reddens alone.** This is a plausible "tidy up
   everything about this player" edit, and it silently cancels an attach in progress. Revert.
3. Change the sweep's predicate to `expires_at > now()` — the sign flip.
   **`theSweepTakesOnlyRowsPastTheirDay` reddens** (*expected 1, got 1* on the count, but the wrong
   row survives — so it reddens on the surviving-row assertion, **not** the count) **and
   `theSweepReportsZeroWhenNothingIsStale` reddens** on its count, *expected 0, got 1*. Run this
   one and confirm which assertion fires: it is the mutation that proves the count alone is not
   enough, and the prediction that the first test fails on its row assertion rather than its count
   is the part most likely to be wrong.
4. Drop the sweep's `WHERE` entirely. **`theSweepTakesOnlyRowsPastTheirDay` reddens** on the count
   (*expected 1, got 2*) **and `theSweepReportsZeroWhenNothingIsStale` reddens** (*expected 0, got
   1*). Revert.
5. Return a hard-coded `0` from `deleteExpiredVerifications` while still executing the delete.
   **`theSweepTakesOnlyRowsPastTheirDay` reddens alone**, on the count; `theSweepReportsZeroWhen
   NothingIsStale` expects `0` and passes. The positive control for the count assertion. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
