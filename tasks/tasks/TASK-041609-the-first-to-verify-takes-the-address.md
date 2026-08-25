---
schema: 2
id: TASK-041609
title: The first to verify takes the address
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, auth, security, invariant]
depends_on: [TASK-041636]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsVerifyTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`verifyPending` consumes a pending row and promotes it into `recovery_email` in one transaction —
answering `AddressTaken` when somebody else got there first, and `Refused` for a token that is
unknown, expired or already used, with no way to tell those three apart.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsVerifyTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsClaimTest.kt` — the
fixture and fixed-clock shape this class reuses;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` — the
`SQLException.sqlState == "23505"` handling that turns a unique violation into a result value
rather than an exception;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §2, §3 and §5.

## Scope

- `verifyPending(token)` on one connection with `autoCommit = false`:
  1. `DELETE FROM email_verification WHERE token_hash = ? AND expires_at > now() RETURNING
     player_id, address`. No row ⇒ commit and return `Refused`.
  2. `INSERT INTO recovery_email (player_id, address, verified_at) VALUES (?, ?, ?)`, with
     `verified_at` from the injected `Clock`.
  3. Commit and return `Verified`. A `23505` from step 2 ⇒ **roll back** and return `AddressTaken`.
- The rollback on `AddressTaken` is the substance: without it the pending row is consumed and the
  address is not stored, so a player whose verification lost the race would have their token burned
  and be told to try a link that no longer exists.
- Expiry is enforced **in the statement**, `expires_at > now()`, per §3 — never by reading the row
  and comparing in Kotlin, which is a read-then-write window.
- A `23505` naming `recovery_email_pkey` rather than `recovery_email_address_unique` means the
  player already holds a verified address; that is also `AddressTaken` and needs no separate case,
  because §5 gives the endpoint one `409`.

## Out of scope

- The endpoint and its status codes — `TASK-041618` and `TASK-041619`.
- Deleting the loser's stale pending row. `ADR-0031` §3 says the other player's row *"becomes
  unverifiable"*, and the sweep (`TASK-041611`) collects it at expiry. Deleting it here would need a
  second `WHERE address = ?`, which is a scan of pending addresses and the beginning of the unique
  namespace §3 refuses.
- Replacing an already-verified address with a new one. No endpoint does that; §5's only path from
  a verified address is `DELETE`, then a fresh claim.
- `hasRecoveryEmail` and `verifiedOwnerOf` — `TASK-041610`.

## Tests

`PostgresRecoveryEmailsVerifyTest`, on the same fixture, with a mutable `Clock` the test advances.

| Test | Proves |
| --- | --- |
| `verifyingMovesTheAddressIntoTheProvenTable` | After a claim and a verify: the result is `Verified`, `email_verification` holds no row for that player, and `recovery_email` holds one whose `address` is the string as typed and whose `verified_at` is the injected clock's instant |
| `theSecondUseOfATokenIsRefused` | Verifying the same token twice: the second answers `Refused`, and `recovery_email` still holds exactly one row. The `DELETE … RETURNING` is what makes this true, so a `SELECT`-then-`DELETE` implementation fails here |
| `aTokenPastItsDayIsRefused` | Claim, advance the injected clock past 24 hours, verify: `Refused`, `recovery_email` is empty, and the pending row is **still present** — an expired token is not consumed by a failed attempt. No test sleeps |
| `theSecondPlayerToVerifyOneAddressIsToldItIsTaken` | Two players both claim `bob@x.test` (which `TASK-041602` proved the schema allows). The first verifies: `Verified`. The second verifies: `AddressTaken`, `recovery_email` still holds exactly one row, it belongs to the first player, **and the second player's pending row is still present** — the rollback |
| `aFoldedCollisionIsToldTheSameThing` | As above but the second player claims `BOB@X.TEST`. Still `AddressTaken`. Two inputs, one differing only in case, so a missing `lower()` in the index — or an implementation that compared addresses in Kotlin — is caught here rather than in a schema test that never runs this path |

## Acceptance criteria

- [ ] All five `PostgresRecoveryEmailsVerifyTest` tests pass
- [ ] `theSecondPlayerToVerifyOneAddressIsToldItIsTaken` asserts the loser's `email_verification`
      row **still exists** after the `AddressTaken`
- [ ] `aTokenPastItsDayIsRefused` asserts the pending row still exists after the refusal
- [ ] The expiry test advances an injected `Clock` and the file contains no `Thread.sleep` and no
      `delay`
- [ ] The consuming statement is a single `DELETE … RETURNING`; the file contains no
      `SELECT ... FROM email_verification WHERE token_hash`
- [ ] `verifyPending` returns `Refused` — never `AddressTaken` and never an exception — for an
      unknown token, an expired token and an already-consumed token
- [ ] Every command in `verify:` exits 0

## Proof

1. Drop `AND expires_at > now()` from the `DELETE`.
   **`aTokenPastItsDayIsRefused` reddens alone**, *expected Refused, got Verified*. Revert.
2. Replace the `DELETE … RETURNING` with a `SELECT`, keeping the `INSERT` and never deleting.
   **`theSecondUseOfATokenIsRefused` reddens** (*expected Refused, got AddressTaken* — the second
   attempt now reaches the insert and collides on the primary key) **and
   `verifyingMovesTheAddressIntoTheProvenTable` reddens too**, on its assertion that the pending row
   is gone. Two reddening is the prediction; a run where only one does means the first test never
   observed consumption at all. Revert.
3. Remove the `rollback()` on the `23505` path, committing instead.
   **`theSecondPlayerToVerifyOneAddressIsToldItIsTaken` and `aFoldedCollisionIsToldTheSameThing`
   both redden**, on the assertion that the loser's pending row survives. The returned value is
   still `AddressTaken`, so a test that checked only the result would have stayed green — this
   mutation is the reason the criterion above names the surviving row. Revert.
4. Catch `23505` and return `Refused` instead of `AddressTaken`.
   **Both taken tests redden alone**, on the result value; `theSecondUseOfATokenIsRefused` is
   unaffected because it never collides. Revert.
5. Change the schema's unique index to `ON recovery_email (address)` — no `lower()`.
   **`aFoldedCollisionIsToldTheSameThing` reddens alone**, *expected AddressTaken, got Verified*,
   while `theSecondPlayerToVerifyOneAddressIsToldItIsTaken` uses identical spellings and stays
   green. This is what makes the fifth test more than a duplicate of the fourth. Revert.
6. Set `verified_at` from `Instant.now()` rather than the injected clock.
   **`verifyingMovesTheAddressIntoTheProvenTable` reddens alone**, on the `verified_at` comparison.
   Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This ticket's `## Proof` step 3 is inert for a database reason, not a coverage one.** Swapping
`rollback()` for `commit()` in the `23505` catch reddens nothing, because Postgres aborts the whole
transaction the moment the `INSERT` raises, and a `COMMIT` on an aborted transaction is downgraded to
a rollback **server-side** regardless of what the Kotlin calls. Coder and reviewer each ran it and got
`BUILD SUCCESSFUL`, six green. The coder built a substitute that genuinely splits the transaction —
commit the `DELETE` unconditionally before attempting the `INSERT` — and it reddens
`theSecondPlayerToVerifyOneAddressIsToldItIsTaken` **alone**, which is the only test the acceptance
criteria names for the surviving-row assertion. The original Proof also predicted
`aFoldedCollisionIsToldTheSameThing` would redden; it has no pending-row assertion, so it cannot.
Twenty-third `## Proof` examined this run.

**Two members of this class read two different clocks, and that is the design.** `verifyPending`
enforces expiry with SQL `now()` — the ticket's Scope specifies that text literally, and
`RecoveryEmails.kt`'s own KDoc already said expiry is enforced in every read by
`WHERE expires_at > now()`. `claimPending` compares its fifteen-minute window against
`clock.instant()`. Both are correct against their tickets, and the asymmetry is worth knowing before
reading either: **advancing the injected clock after a claim cannot retroactively expire a row**, so
an expired fixture has to be built by backdating the clock *before* the claim. The consequence is
permanent: no test in this file can assert the exact twenty-four-hour boundary, only that a row well
past it is refused.

**The indistinguishability assertion is documentation, not coverage — today.** `Refused` is a
parameterless `object`, so *"each equals canonical `Refused`"* and *"the three equal each other"* are
transitively the same claim, and no mutation at this type shape separates them. Both coder and
reviewer said so unprompted. It is worth keeping because it states the security property the ticket
exists for, and because the day `Refused` gains a payload the two forms stop being equivalent.

**No sequence puts one address into `recovery_email` for two players.** The reviewer walked it: the
only write path is `verifyPending`'s `INSERT`, gated by `recovery_email_address_unique` over
`lower(address COLLATE "und-x-icu")`, which Postgres enforces atomically at row level whatever the
isolation or timing; every failure path rolls back, so a losing race leaves no half-written row; and
`detach` is still `TODO()`, so nothing can delete a row once written.
