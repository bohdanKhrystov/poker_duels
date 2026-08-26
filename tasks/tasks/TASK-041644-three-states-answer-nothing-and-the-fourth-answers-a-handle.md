---
schema: 2
id: TASK-041644
title: Three states answer nothing, and the fourth answers a handle
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, auth, security, test]
depends_on: [TASK-041643]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest.aVerifiedAddressAnswersItsOwnersIdAndHandle' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest.oneOwnersHandleIsNotAnothers' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest.aPendingAddressAndAnUnknownOneBothAnswerNothing' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest.anOwnerWithNoPasswordCredentialAnswersNothing' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest.theHandleIsFoundWhateverCaseTheAddressIsAskedIn' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`resetRecipientOf`'s four outcomes are held by tests rather than by the shape of a SQL string: a
proven address answers its owner's id **and** that owner's own handle, and the three states
`ADR-0082` §1 makes indistinguishable all answer `null`.

## Why this exists

`TASK-041643` lands the statement with textual gates only — fixed-string `grep`s that pin the fold,
the join and the kind literal, because *character for character* is not a property any test can
assert. Nothing there observes a row. This ticket is the other half, and the two are strictly
ordered because they are one statement seen from two sides.

The mutations `TASK-041643`'s Proof runs and cannot catch are this ticket's subject: a `LEFT JOIN`
handing the route a null handle, and a dropped `c.kind = 'password'` that goes ambiguous the day
`DEC-027` admits a second credential kind. Both are gated here by one test.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify |

The second row is a **conditional repair and nothing else**, and its bounds are in *Scope* below —
it is here so that a test reddening on the merged statement is fixed in one dispatch rather than
becoming a block. If no test reddens, this file is not touched and the PR shows one changed file.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` — `resetRecipientOf` and
`ResetRecipient`;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresCredentialsTest.kt` — its raw
`INSERT INTO credential (id, player_id, kind, identifier, secret_hash)` helper, which is the idiom
to copy;
`docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md` §1, whose
three-`null`-states paragraph is the whole of the refusal half of this ticket.

## Scope

- **Five test methods are added** to the existing `PostgresRecoveryEmailsReadsTest` class. **No
  existing test in the file changes, no assertion moves and none is weakened** — the five merged
  tests exercise `verifiedOwnerOf` and `hasRecoveryEmail`, which `TASK-041643` did not touch.
- **One new private helper**, `insertCredential(playerId, kind, identifier)`, doing the raw
  `INSERT INTO credential (id, player_id, kind, identifier, secret_hash)` four other db test files
  already do, with a fresh `UUID` and an arbitrary non-null `secret_hash` string. **Not
  `PostgresCredentials.create`**: that runs Argon2 on every call, and the statement under test
  reads `c.identifier` and never `secret_hash`, so a real hash would buy nothing and cost seconds.
- The class KDoc gains a sentence naming `resetRecipientOf` as the third read this file covers, and
  says the fixed `Clock` still suffices — the new statement reads no timestamp column either.
- **The second file may be edited only if one of the five tests reddens**, and then only
  `SELECT_RESET_RECIPIENT_SQL`'s text or the private row-mapping helper `TASK-041643` wrote. Never
  the member's signature, never `SELECT_VERIFIED_OWNER_SQL`, never a new member, never a new
  constant. If a red needs more than that, **stop and report it** rather than widening — it means
  `ADR-0082` §1 and the merged statement disagree, which is a finding, not a fix.

## Out of scope

- **Any change to `RecoveryEmails.kt`.** The port is merged by `TASK-041643` and this ticket
  observes it. **Gated below**: the Files table has no row for it.
- **A test that a handle is absent from a response body or a log line.** Nothing on this path logs
  (`ADR-0077` §4) and this layer has no response body. The handle's absence from the
  `forgot-password` response is `TASK-041626`'s `theResponseNeverCarriesTheAddress`. **Gated
  below** as a refusal with no assertion of its own.
- **A test that `ResetRecipient.toString()` redacts.** It deliberately does not — `ADR-0082`
  §Consequences chooses that and names the trigger for revisiting it. Asserting redaction here
  would gate the opposite of the decision. **Gated below.**
- **An expired-address case.** `expires_at` lives only in `email_verification`, which this
  statement never reads — the merged class KDoc already argues exactly this for `verifiedOwnerOf`
  and the argument transfers unchanged. A backdated-clock fixture would add no assertion the plain
  pending case does not already make.
- Concurrency, indexes and `EXPLAIN`. `ADR-0082` §Consequences names the unindexed
  `credential (player_id)` scan and the one ticket that would fix all three callers together.

## Tests

`PostgresRecoveryEmailsReadsTest` — five methods added to the merged class.

| Test | Proves |
| --- | --- |
| `aVerifiedAddressAnswersItsOwnersIdAndHandle` | One player with a `password` credential whose identifier is a known handle, one verified address. `resetRecipientOf` answers a non-null `ResetRecipient` and **both** fields are asserted — the id equals the player's, the handle equals the identifier inserted |
| `oneOwnersHandleIsNotAnothers` | **Two** players, **two** verified addresses, **two different** handles. Each address answers its own owner's id and its own owner's handle. A statement that ignores its parameter, joins on the wrong column, or returns the first row in the table passes every other test in this file and fails this one — and a constant handle cannot pass it at all |
| `aPendingAddressAndAnUnknownOneBothAnswerNothing` | A claimed-but-unverified address and an address nobody has ever mentioned both answer `null`, asserted **individually and equal to each other** — the security property is that the two agree, not that each happens to be the empty value. The file's merged `aPendingAddressIsFoundByNobody` makes the same argument for `verifiedOwnerOf` and this follows it |
| `anOwnerWithNoPasswordCredentialAnswersNothing` | Two verified addresses whose owners hold, respectively, **no credential at all** and **only a non-`password` credential** (`CredentialKind("oauth:google")`). Both answer `null`. This is the `JOIN`-not-`LEFT JOIN` test *and* the `c.kind = 'password'` test: a `LEFT JOIN` answers a recipient with no handle for the first owner, and a join with the kind predicate dropped answers the OAuth identifier as though it were a login handle for the second |
| `theHandleIsFoundWhateverCaseTheAddressIsAskedIn` | An address stored as `Bob@Example.com` answers the same `ResetRecipient` when asked as `BOB@example.COM` and as the exact stored spelling. The pinned `und-x-icu` fold, observed rather than greped |

## Acceptance criteria

- [ ] All five named methods pass, and the whole `PostgresRecoveryEmailsReadsTest` class passes
- [ ] `aVerifiedAddressAnswersItsOwnersIdAndHandle` asserts `playerId` **and** `handle`, not just
      that the result is non-null
- [ ] `oneOwnersHandleIsNotAnothers` uses two handles that are **different strings**, and asserts
      each address against its own owner's handle — no assertion in it would pass if the two
      handles were swapped
- [ ] `aPendingAddressAndAnUnknownOneBothAnswerNothing` contains an `assertEquals` of the two
      results **against each other**, in addition to pinning each to `null`
- [ ] `anOwnerWithNoPasswordCredentialAnswersNothing` covers **both** owners — one with no
      credential row and one with a `CredentialKind("oauth:google")` row — in one test and one
      database
- [ ] The five merged tests in the file are **byte-unchanged**; `git diff` shows additions only,
      plus the KDoc sentence and the new helper
- [ ] The file contains no call to `PostgresCredentials.create` and no `Argon2`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change the `JOIN` to a `LEFT JOIN` in `SELECT_RESET_RECIPIENT_SQL`.
   **`anOwnerWithNoPasswordCredentialAnswersNothing` reddens alone**, on the no-credential owner:
   the row comes back with a `NULL` identifier, so the mapper either throws or produces a
   `ResetRecipient` with an empty handle, and the test expected `null`. Every other test in the
   file **stays green** — each of their owners has a `password` credential, so an outer join and an
   inner one agree for all of them. Run it: this is the mutation `TASK-041643` could not catch and
   the reason this ticket exists.
2. Drop `AND c.kind = 'password'` from the join condition.
   **`anOwnerWithNoPasswordCredentialAnswersNothing` reddens alone again**, this time on the
   OAuth owner — the statement answers `ResetRecipient(playerId, "…google identifier…")` where the
   test expected `null`. The other four stay green, because none of their fixtures holds a second
   credential kind. Run it, and note in the PR that one test kills both mutations and that the
   fixture holding **two** differently-broken owners is what makes that possible.
3. Return a constant handle from the mapper — ignore `c.identifier` and answer `"handle"`.
   **`oneOwnersHandleIsNotAnothers` reddens**, and `aVerifiedAddressAnswersItsOwnersIdAndHandle`
   reddens **only if its fixture handle is not the literal `"handle"`**. Choose the fixture handles
   so it does: this is the *one value across a whole story* trap, and a single-fixture test cannot
   tell a copy from a constant. Revert.
4. Replace the SQL fold with `address.value.lowercase()` bound as a plain parameter.
   **`theHandleIsFoundWhateverCaseTheAddressIsAskedIn` reddens on the shouted-case lookup**, and —
   predicted honestly — **also on the exact-stored-spelling lookup**, because `Bob@Example.com`
   lowercased no longer matches the stored mixed-case row under a comparison that is no longer
   folding the column. Two assertions in one test, not one. That second red is the tell that the
   fold moved out of SQL rather than merely weakening. Revert.
5. Delete `aPendingAddressAndAnUnknownOneBothAnswerNothing`'s `assertEquals` of the two results
   against each other, keeping both `assertNull`s.
   **Nothing reddens.** Written down as inert on purpose: the cross-assertion is not defending
   against today's statement, it is defending against a future one that answers `null` for a
   pending address for a *different* reason than it does for an unknown one. Restore it — and note
   that deleting an assertion proving nothing is not evidence the assertion is worthless, which is
   why this step also breaks what it guards: additionally make the statement read
   `email_verification` in a `UNION`, and **then** the cross-assertion and the first `assertNull`
   both redden.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
