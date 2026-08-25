---
schema: 2
id: TASK-041610
title: A pending address answers exactly as an unknown one
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, auth, security]
depends_on: [TASK-041609]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`hasRecoveryEmail` and `verifiedOwnerOf` answer from `recovery_email` alone, so an address that is
pending — or expired, or belonging to somebody else — is indistinguishable from one this server has
never heard of.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsReadsTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsVerifyTest.kt` — the
fixture this class reuses;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3's *"What the unverified state means
for the account in the meantime: nothing"* and §5.

## Scope

- `hasRecoveryEmail(playerId)`: `SELECT EXISTS (SELECT 1 FROM recovery_email WHERE player_id = ?)`.
  One statement, one table.
- `verifiedOwnerOf(address)`:
  `SELECT player_id FROM recovery_email WHERE lower(address COLLATE "und-x-icu") = lower(? COLLATE
  "und-x-icu")`, returning `PlayerId?`. The fold is applied **in SQL under the pinned collation**,
  matching the unique index exactly — a `lowercase()` in Kotlin would use a different rule from the
  index that decided ownership, so a case-varying address could be accepted as unique and then not
  be findable.
- Neither statement reads `email_verification`. That is the whole ticket: the two tables are
  separate so the dangerous state cannot be selected by accident (`ADR-0031`'s rejected
  single-table alternative).

## Out of scope

- `detach` and `deleteExpiredVerifications` — `TASK-041611`.
- Returning the address. `verifiedOwnerOf` answers an id; the address a mail is sent to is read
  inside the sending path, which is `DEC-072`'s.
- Any budget or timing defence. `ADR-0031` §5 closes the timing channel at the **endpoint**, by
  writing the `202` before any mail work; this port is allowed to be as fast or slow as the query
  is.
- `ProfileResponse.hasRecoveryEmail` — `TASK-041616` reads it through a different statement, in
  `PostgresProfileReads`, for the round-trip reason `ADR-0053` gives.

## Tests

`PostgresRecoveryEmailsReadsTest`

| Test | Proves |
| --- | --- |
| `aVerifiedAddressIsFoundByItsOwner` | After claim + verify, `hasRecoveryEmail(player)` is `true` and `verifiedOwnerOf(EmailAddress("bob@x.test"))` is that player |
| `aPendingAddressIsFoundByNobody` | After a claim and **no** verify: `hasRecoveryEmail(player)` is `false` and `verifiedOwnerOf` of that same address is `null` — byte-identical to the answers for an address never mentioned, asserted in the same test against a second, never-claimed address |
| `anAddressIsFoundWhateverCaseItIsAskedIn` | With `Bob@Example.com` verified, `verifiedOwnerOf(EmailAddress("BOB@example.COM"))` returns the owner, and so does `verifiedOwnerOf(EmailAddress("Bob@Example.com"))`. Two spellings, one answer |
| `onePlayersAddressIsNotAnothers` | Two players with two different verified addresses: each lookup returns its own owner, and `hasRecoveryEmail` is `true` for both. The guard against a query that ignores its parameter — a `verifiedOwnerOf` returning the first row in the table passes every test above with one fixture |
| `aDetachedPlayerReadsFalseAgain` | With one player verified and a second never claiming, `hasRecoveryEmail` is `true` and `false` respectively. Two players, two answers, in one database — the correlated-versus-uncorrelated trap `ADR-0053` names |

## Acceptance criteria

- [ ] All five `PostgresRecoveryEmailsReadsTest` tests pass
- [ ] `aPendingAddressIsFoundByNobody` asserts against **two** addresses — the pending one and one
      never mentioned — and asserts the two answers are equal
- [ ] `onePlayersAddressIsNotAnothers` holds **two** verified players in one database
- [ ] `PostgresRecoveryEmails.kt`'s `hasRecoveryEmail` and `verifiedOwnerOf` statements name
      `recovery_email` and do not name `email_verification`
- [ ] The fold in `verifiedOwnerOf` is applied in SQL with `COLLATE "und-x-icu"` on **both** sides,
      and the Kotlin does not call `lowercase()`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change `verifiedOwnerOf` to also union in `email_verification`:
   `... UNION SELECT player_id FROM email_verification WHERE lower(address ...) = lower(? ...)`.
   **`aPendingAddressIsFoundByNobody` reddens alone**, *expected null, got <player>*. This is the
   defect the ticket exists to prevent — it looks like a helpful fix for "the player says the mail
   never arrived" and it turns `forgot-password` into a reset for an unproven mailbox. Revert.
2. Change `hasRecoveryEmail` to `SELECT EXISTS (SELECT 1 FROM recovery_email)` — uncorrelated.
   **`aDetachedPlayerReadsFalseAgain` reddens alone**, *expected false, got true* for the second
   player. `aVerifiedAddressIsFoundByItsOwner` holds one verified player and stays green, which is
   why the fifth test holds two. Revert.
3. Drop the `WHERE` from `verifiedOwnerOf`, returning the first row.
   **`onePlayersAddressIsNotAnothers` reddens alone.** Every other test in the file has at most one
   verified row and would pass. Revert.
4. Replace the SQL fold with a Kotlin `address.value.lowercase()` compared against the stored
   `address` column directly (no `lower()` in SQL).
   **`anAddressIsFoundWhateverCaseItIsAskedIn` reddens alone**, on the `Bob@Example.com` lookup:
   the stored form is mixed case and the folded parameter no longer matches it. Note the trap this
   demonstrates — asking in *lowercase* still fails, so a test that only asked in one case would
   have caught it, but a test that stored a *lowercase* address and asked in mixed case would not.
   The fixture must store the mixed-case form. Revert.
5. Change `hasRecoveryEmail` to read `email_verification` instead.
   **`aPendingAddressIsFoundByNobody` and `aDetachedPlayerReadsFalseAgain` both redden**, and
   `aVerifiedAddressIsFoundByItsOwner` reddens as well, because verification deletes the pending
   row. Three reddening is the prediction. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
