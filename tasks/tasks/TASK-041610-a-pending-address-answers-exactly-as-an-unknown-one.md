---
schema: 2
id: TASK-041610
title: A pending address answers exactly as an unknown one
type: task
status: done
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

## Notes

**The `COLLATE` on the bind parameter is load-bearing, and dropping it misattributes an account.** The
lookup folds both sides — `lower(address COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu")` — and
applying a collation to a parameter looks like the kind of clause a later reader tidies away. The
reviewer stripped it from the parameter alone and a lookup for player one's own `İ@x.test` returned
**player two's** `PlayerId`: `lower('İ' COLLATE "und-x-icu")` is `i̇` (three bytes) while the default
fold gives `i` (one byte), so the two addresses stop being distinguishable in the direction that hands
one player another's row. Not a missed row — a cross-account misattribution. `É`, `ẞ` and `Ж` fold
identically under both collations and would have proved nothing; `İ` is the input that separates them.

**The expired case is absent because these two statements cannot observe expiry.** Both name
`recovery_email` only, never `email_verification` or `expires_at`, so a pending row past its day and a
fresh one are equally absent and answer identically whatever the clock says. The coder wrote that into
the test class KDoc rather than building a fixture that would look like coverage, and the Tests table
asks for five tests with none named for expiry. The backdated-clock construction earns its keep at
`verifyPending`'s boundary (`TASK-041609`), not here.

**Cross-equality was tested for value rather than argued for.** The coder stripped the cross-equality
assertions from `aPendingAddressIsFoundByNobody` and re-ran the mutations: the literal `false`/`null`
pins caught both alone, because each is a single uniquely-identified value under `==`, so *"Y equals
X"* collapses into *"Y equals the literal"*. Where the wider state space does earn force is
`onePlayersAddressIsNotAnothers`, whose expected `PlayerId` is a run-generated UUID with no literal to
hardcode — there the dynamic comparison is the only way to write the assertion, and it is what catches
a read that returns the first row. Both halves reproduced by the reviewer.

**The Proof undercounts by one.** Mutation 5 is predicted to redden three tests and reddens four:
`onePlayersAddressIsNotAnothers` checks `hasRecoveryEmail` on two *verified* players, and
`verifyPending` deletes their `email_verification` rows on success, so both read `false` under the
mutation. Sound in each case. Twenty-fourth `## Proof` examined this run.
