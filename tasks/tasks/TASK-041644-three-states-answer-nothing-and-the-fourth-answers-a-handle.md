---
schema: 2
id: TASK-041644
title: Three states answer nothing, and the fourth answers a handle
type: task
status: done
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

The third is the fold. A fixed-string `grep` for `COLLATE "und-x-icu"` reddens on a KDoc comment and
stays green on a fold that has quietly stopped being symmetric, and the asymmetric case is not
hypothetical: stripping the parameter-side `COLLATE` from `verifiedOwnerOf` earlier in this story
made a lookup for one player's own address answer a **different player's** `PlayerId`, on a read one
step from authentication. That mutation is invisible to every ASCII fixture, so this ticket's
case-folding test carries a non-ASCII one and the *Tests* section below says which and why.

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
| `theHandleIsFoundWhateverCaseTheAddressIsAskedIn` | **Two owners, two addresses, four lookups.** An address stored as `Bob@Example.com` answers its owner's `ResetRecipient` asked as `BOB@example.COM` and as the exact stored spelling; a second owner's address is **stored as `DOTTED + "@x.test"`** and answers *its* owner's `ResetRecipient` asked as `DOTTED + "@x.test"` and as `FOLDED + "@x.test"`. The pinned `und-x-icu` fold on **both halves** of the comparison, observed rather than greped |

Throughout this ticket, `DOTTED` is the single character **U+0130** (LATIN CAPITAL LETTER I WITH DOT
ABOVE) and `FOLDED` is the two-character sequence **`i` followed by U+0307** (COMBINING DOT ABOVE).
Write both in the test as Kotlin unicode escapes, never as literal glyphs, so the fixture survives
any editor. The two code points above are the whole specification — nothing outside this ticket
needs opening for them. The merged
`RecoveryEmailSchemaTest.twoSpellingsOnlyIcuFoldsTogetherAreOneAddress` pins the same pair, but it
pins it against `recovery_email_address_unique` and against no `SELECT` at all, which is exactly why
it leaves this statement uncovered.

**The second fixture is the one that discriminates, and its direction is load-bearing.** U+0130 is
the character the two collations disagree about: `lower()` maps it to `FOLDED` under `und-x-icu` and
to a bare `i` under the container's musl default. `É`, `ẞ` and `Ж` fold identically under both, and
so does every ASCII letter — which is why the `Bob@Example.com` half proves only that *some* fold
happens, never that it is the pinned one.

Measured against `postgres:16-alpine` — the container `PostgresTestSupport` starts — with the
address stored as `DOTTED + "@x.test"` and the four spellings of the statement asked both ways:

| `SELECT_RESET_RECIPIENT_SQL` | asked `DOTTED` | asked `FOLDED` |
| --- | --- | --- |
| As `TASK-041643` merged it | found | found |
| `COLLATE` dropped from the **parameter** | **not found** | found |
| `COLLATE` dropped from the **column** | **not found** | **not found** |
| `COLLATE` dropped from **both** | found | **not found** |

Neither lookup alone covers that set, which is why the test asks twice: the exact-spelling lookup is
the only one that catches a dropped parameter-side fold, and the combining-sequence lookup is the
only one that catches the pin being removed from both halves at once — a fold that stays
self-consistent while no longer being the fold `recovery_email_address_unique` is built on. Under
every one of the four variants the `Bob@Example.com` lookups are found, so a reader can check the
claim that the ASCII half cannot fail here.

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
- [ ] `theHandleIsFoundWhateverCaseTheAddressIsAskedIn` makes **four** lookups over **two** owners:
      `Bob@Example.com` asked as `BOB@example.COM` and as the exact stored spelling, and a second
      owner's `DOTTED + "@x.test"` asked as `DOTTED + "@x.test"` and as `FOLDED + "@x.test"`. Each
      of the four is asserted against **its own** owner's `ResetRecipient`, id and handle both, and
      the two owners' handles are different strings
- [ ] The two non-ASCII spellings in that test are written as Kotlin unicode escapes — U+0130 for
      `DOTTED`, `i` plus U+0307 for `FOLDED` — with no literal glyph anywhere in the file, and the
      stored address is the `DOTTED` one. **Stored and asked are not interchangeable here**: storing
      `FOLDED` and asking `DOTTED` would still catch a dropped parameter-side `COLLATE` but would go
      blind to a dropped column-side one, and storing `DOTTED` and asking only `FOLDED` goes blind
      to the parameter side, which is the mutation this test exists for
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
4. Delete `COLLATE "und-x-icu"` from the **parameter half only** of `SELECT_RESET_RECIPIENT_SQL`,
   leaving `lower(r.address COLLATE "und-x-icu") = lower(?)`.
   **`theHandleIsFoundWhateverCaseTheAddressIsAskedIn` reddens alone, and inside it only the
   `DOTTED` lookup**: the parameter now folds under the database default, which maps U+0130 to a
   bare `i`, while the column still folds to `FOLDED` — so a verified address answers `null` when
   asked in **the exact spelling it was stored in**. Its other three lookups stay green, and so do
   the other four tests, whose fixtures are ASCII and fold identically either way. PostgreSQL raises
   no collation conflict and logs nothing, so an assertion is the only thing in the system that can
   see this. It is the mutation that, applied to `verifiedOwnerOf` earlier in this story, made a
   lookup for one player's own address answer a **different player's** `PlayerId`. Revert.

   Then delete the `COLLATE` from **both** halves. **The same test reddens, and now on the `FOLDED`
   lookup instead** — the fold is self-consistent again but is no longer the one
   `recovery_email_address_unique` is built on, so two spellings the index treats as one address
   become two for the read. Each lookup catches what the other cannot; that is why this test asks
   twice and why the *Tests* table above is a measurement rather than a recollection. Revert.
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

## Notes

**This ticket's fixture was rewritten before a line of it was implemented, because the original
could not fail.** `theHandleIsFoundWhateverCaseTheAddressIsAskedIn` originally stored
`Bob@Example.com` and asked `BOB@example.COM` — plain ASCII, which folds identically under every
collation, so it passed with the `COLLATE` clause deleted. `TASK-041643` was **held** on that: it
ships `resetRecipientOf` with no behavioural test of its own and defers every property here, and a
deferral to a test that cannot fail is not a deferral.

**The replacement was measured against real Postgres, and two of its properties are counter-intuitive
enough that reasoning would have got them wrong.** The address is stored as `U+0130` and asked in
both spellings:

- **Direction is load-bearing.** Storing the already-folded `i` + U+0307 and asking `U+0130` — which
  reads more naturally as a case test — is **blind** to a column-side strip, because
  `lower(i+U+0307)` is a fixed point under both collations.
- **Neither lookup subsumes the other.** Dropping `COLLATE` from the **parameter** half alone reddens
  only the exact-spelling ask; dropping it from **both** halves reddens only the combining-sequence
  ask, because a fold that stays self-consistent still stops being the fold the unique index is built
  on. Coder and reviewer each confirmed both directions against the shipped statement.

**A tooling defect nearly made the fixture decorative.** Typing the token for a `\u` escape into an
edit parameter was **silently decoded into the glyph** — the tell was Edit reporting old and new
strings as identical — and doubling the backslash produced literally two backslashes, which is wrong
Kotlin. A file carrying the decoded glyph compiles and passes while testing something other than what
it claims. The coder routed around it by building the backslash at runtime, and the reviewer verified
the bytes independently rather than accepting the report: exactly two escape sequences, **zero** raw
`U+0130` or combining-dot bytes, zero double backslashes. The planner hit the same bug hours earlier
writing this ticket, which is why the code points appear in it as prose.

**This ticket was over its own estimate and there was no honest label for it.** `estimate: S` caps at
**120 changed lines**; the five tests, the `insertCredential` helper and the KDoc sentence measured
roughly **130–145** before a later amendment added about ten more. Schema 2 admits only `XS` and `S`
— `M` was deleted on purpose — so the ceiling was not a label that could be corrected, and the only
truthful fix at planning time would have been **two tickets**: the three refusal tests
(`aPendingAddressAndAnUnknownOneBothAnswerNothing`, `anOwnerWithNoPasswordCredentialAnswersNothing`)
in one, the two positive reads and the collation fixture in the other. That split is no longer
available: this is merged, and re-cutting a merged ticket rewrites the trail rather than repairing
it, which is the one thing the trail is for.

**So this paragraph is the whole of the action, deliberately.** It is recorded because *which* fact
is wrong matters to the next planner: the estimate, not the scope. Nothing in the Files table, the
Tests table or the acceptance criteria is false — the ticket did what it says and the two files it
names are the two it touched. What failed is that **a ticket's size was judged from its file count**,
and this one is two files holding five database tests with a measured, four-row collation table
behind one of them. Line count and file count came apart here, and only the file count was checked.
`ADR-0070`'s probe sizes an `atomic:` ticket's *file* list; nothing sizes a ticket's *diff*, and
`files_touched` passing the linter is not evidence that `estimate:` did.

**One assertion here is vacuous and stays by ticket mandate.** The cross-`assertEquals` in
`aPendingAddressAndAnUnknownOneBothAnswerNothing` cannot fail independently of the two `assertNull`s
above it — null equals null, and it is unreachable if either `assertNull` has already reddened. The
ticket argues for it anyway as a statement of the security property (*the two agree*, not merely that
each is empty), which is a settled design choice rather than a defect. Recorded so the next reader
does not mistake it for a working gate.
