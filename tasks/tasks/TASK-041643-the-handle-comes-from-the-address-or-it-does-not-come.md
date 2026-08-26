---
schema: 2
id: TASK-041643
title: The handle comes from the address, or it does not come
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, db, auth, security]
depends_on: [TASK-041642]
verify:
  - ./gradlew :poker-server:compileKotlin
  - ./gradlew :poker-server:compileTestKotlin
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.VerificationSweepTest' -PrequireDocker=true
  - grep -qF 'lower(r.address COLLATE \"und-x-icu\") = lower(? COLLATE \"und-x-icu\")' poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt
  - grep -qF "JOIN credential c ON c.player_id = r.player_id AND c.kind = 'password'" poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt
  - grep -c 'LEFT JOIN' poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt | grep -qx 0
  - grep -c 'fun resetRecipientOf' poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt | grep -qx 1
  - grep -qF 'the only read that returns an owner and nothing else' poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt
  - grep -qF 'structurally cannot be an address' poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`RecoveryEmails.resetRecipientOf(address)` answers the player id **and** the login handle behind a
proven recovery address in one statement, so `POST /api/auth/forgot-password` has a source for the
third argument `RecoveryMailer.sendPasswordReset` has always required and nothing has ever been able
to supply.

## Why this exists

`DEC-076` was raised by a coder implementing `TASK-041626`, who found that
`sendPasswordReset(address, token, handle)` needs a login handle and that **nothing in this
codebase can obtain one from a `PlayerId`** — and blocked instead of guessing. That block was
correct.
[`ADR-0082`](../../docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md)
answers it: the handle is read from a **proven address**, never from a player id, because obtaining
a handle then requires already holding the exact secret `forgot-password` exists to refuse to
disclose.

## Why this is a ticket in front of `TASK-041626` and not three more files inside it

`MAX_FILES_TOUCHED` is 3. The probe `ADR-0070` prescribes was run against this change, and it
matters that it was run rather than remembered:

- **Run 1** stubbed the member and the `data class` on `RecoveryEmails` and ran
  `./gradlew check -PrequireDocker=true` and the client job, verbatim from
  `.github/workflows/build.yml`. It named **one** path — `PostgresRecoveryEmails.kt`, from
  `:poker-server:compileKotlin`.
- **Run 2**, after the minimal fix there, named `VerificationSweepTest.kt` from
  `:poker-server:compileTestKotlin`. That path was **invisible to run 1**: Gradle stops at its
  first failing task, so a red main-source compile hides every gate behind it. A first red run's
  file list is a *prefix*, never an answer.
- **Run 3** exited 0 across both jobs. Three files, and the probe was then reverted.

So the smallest commit that lands this member is exactly these three, no `atomic:` is needed, and
`TASK-041626` cannot absorb them.

**This ticket ships no new test method, and that is the honest shape rather than a gap.** What it
ships is a statement whose substance is *textual*: that the fold is applied in SQL under the pinned
collation, that the join is inner, and that the kind is spelled the way the sibling statement spells
it. No behavioural test can assert *character for character*; a fixed-string `grep` can, and that is
what the `verify:` block above does. The **behaviour** of the statement is `TASK-041644`, the very
next ticket, which is why the two are strictly ordered. The two class-wide `--tests` filters above
are deliberately class-wide: they mean *these merged suites still pass unchanged*, not *these
methods exist*.

## Files

Every row carries the probe run that named it.

| File | Action | Why it is in this commit |
| --- | --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` | modify | The member and `ResetRecipient` themselves. Everything below exists because these lines do |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify | `:poker-server:compileKotlin`, run 1 — *Class 'PostgresRecoveryEmails' is not abstract and does not implement abstract member 'resetRecipientOf'*. Also carries the statement |
| `poker-server/src/test/kotlin/duels/poker/server/VerificationSweepTest.kt` | modify | `:poker-server:compileTestKotlin`, run 2 — the same error for the `ThrowingRecoveryEmails` object, the only double of this port in the repository |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPasswordResets.kt` —
`REWRITE_CREDENTIAL_SQL` only, for the one way this repository spells the password kind;
`docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md` §1 and §5 —
§1 is the statement, §5 is the list of what does **not** move;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and §6.2.

## Scope

- **`RecoveryEmails` gains exactly one member**, beside `verifiedOwnerOf`:

  ```kotlin
  public suspend fun resetRecipientOf(address: EmailAddress): ResetRecipient?
  ```

  and one `public data class ResetRecipient(val playerId: PlayerId, val handle: String)` beside
  `VerifyEmailResult` and `ClaimPendingResult`. An ordinary `data class` — **not** given
  `EmailAddress`'s redacting `toString()`, per `ADR-0082` §Consequences, which names the trigger
  for revisiting that as *the first log line anywhere on the reset path*.
- **There is no `PlayerId` overload and there must never be one.** The fence is the argument type,
  not the member name. Gated: `verify:` asserts the port declares `fun resetRecipientOf` exactly
  once.
- **`PostgresRecoveryEmails` gains one `private const val SELECT_RESET_RECIPIENT_SQL` and one
  private mapping helper**, in `selectVerifiedOwner`'s shape. The statement is `ADR-0082` §1's:

  ```sql
  SELECT r.player_id, c.identifier FROM recovery_email r
  JOIN credential c ON c.player_id = r.player_id AND c.kind = 'password'
  WHERE lower(r.address COLLATE "und-x-icu") = lower(? COLLATE "und-x-icu")
  ```

  Three things about it are gated by fixed-string `grep`, so **each must sit on one source line**
  of the Kotlin constant — the concatenation may break between the clauses, never inside one:
  1. `lower(r.address COLLATE \"und-x-icu\") = lower(? COLLATE \"und-x-icu\")`. The fold is
     applied **in SQL** under the collation `recovery_email_address_unique` is built on, never as a
     Kotlin `lowercase()`. `SELECT_VERIFIED_OWNER_SQL`'s comment already gives the reason and it is
     unchanged here: a fold outside SQL uses a different rule from the one that decided uniqueness,
     so a case-varying address could be unique at write time and unfindable here. The only
     difference from the sibling constant is the `r.` qualifier, which two tables make necessary.
  2. `JOIN credential c ON c.player_id = r.player_id AND c.kind = 'password'`. `'password'` is a
     SQL literal matching `REWRITE_CREDENTIAL_SQL` verbatim, so the reset path spells the kind one
     way in both statements that make it up.
  3. **A `JOIN`, never a `LEFT JOIN`.** Three states answer `null` and are indistinguishable to the
     caller: an unknown address, a pending-only one (`ADR-0031` §3 — one state as far as the
     account is concerned), and a verified address whose owner holds no `password` credential. A
     `LEFT JOIN` would hand the route a null handle to make a decision about.
- **`ThrowingRecoveryEmails` in `VerificationSweepTest` gains the member**, with the file's own
  `error("ThrowingRecoveryEmails: not used by this test")` body verbatim. No test in that file
  calls it, and none should.
- **Two KDoc sentences on `RecoveryEmails` are amended, not deleted**, because `ADR-0082`
  §Consequences records that each becomes wrong on its own if it is not. Both phrasings are gated
  by `grep`, so write **these exact phrases** somewhere in the amended sentences:
  1. `verifiedOwnerOf`'s *"the only read that returns an address's owner"* becomes
     **`the only read that returns an owner and nothing else`** — it keeps its second caller, the
     attach path's already-proven-elsewhere check, which must not receive a handle.
  2. The class KDoc's *"no member returns a `String` that could be one"* gains the reason it
     survives: `ResetRecipient.handle` is a login handle and `loginHandleOrNull` permits only
     `[a-z0-9._-]`, so it **`structurally cannot be an address`**.

## Out of scope

- **Any behavioural test of the new statement.** `TASK-041644` is the next ticket and owns all five
  of them, including the `JOIN`-not-`LEFT JOIN` one. **Gated below**: the Files table has no
  `PostgresRecoveryEmailsReadsTest.kt` row, and the probe reaching green without that file is the
  proof no merged gate names it.
- **`Credentials.handleOf(playerId)` or anything in that direction.** `ADR-0082` §2 forecloses it
  and `TASK-041642` — this ticket's dependency, merged before it starts — reddens the build on it.
  This is the ticket that gate was ordered ahead of, and it is the temptation the gate is aimed at.
- **`PasswordResets.issue`.** It keeps its `Boolean`; `ADR-0082` §3 argues the `Issued(handle)` /
  `Suppressed` alternative in full and rejects it. `PostgresPasswordResetsIssueTest`'s seven
  assertions and the four `PasswordResets` doubles are untouched. **Gated below** by the Files
  table naming neither.
- **A migration or a `credential (player_id)` index.** `ADR-0082` §5: the join's access path is the
  one `verifyCurrent` and `holdsCredential` already take, and §Consequences names the trigger for
  adding one — all three at once, as one ticket. Not ticketed.
- **`recoveryRoutes`, `Application.kt`, `ServerComponents.kt` and `RecoveryMailer.kt`**, all four of
  which `ADR-0082` §5 lists as byte-unchanged. The route that calls this member is `TASK-041626`.
- A `LoginHandle` value class — `ADR-0082` §Alternatives, rejected as a rider on this decision.

## Tests

**None are added by this ticket**; see *Why this is a ticket in front of `TASK-041626`* above for
why that is the shape and not an omission. Two merged suites must pass **unchanged**, and both are
in `verify:` as class-wide filters for exactly that reason:

| Suite | Proves |
| --- | --- |
| `PostgresRecoveryEmailsReadsTest` | All five merged tests still pass, with no assertion moved and none weakened — `verifiedOwnerOf`, `hasRecoveryEmail` and the pinned-collation case test are untouched by a second read arriving beside them |
| `VerificationSweepTest` | All three merged tests still pass, so the `ThrowingRecoveryEmails` addition compiles and changes no sweep behaviour |

## Acceptance criteria

- [ ] `RecoveryEmails.kt` declares `fun resetRecipientOf` **exactly once**, taking an
      `EmailAddress` and returning `ResetRecipient?`
- [ ] `ResetRecipient` is a `public data class` with `playerId: PlayerId` and `handle: String`, and
      declares no `toString()` override
- [ ] `PostgresRecoveryEmails.kt` contains the two greped SQL fragments, each on one source line
- [ ] `PostgresRecoveryEmails.kt` contains no `LEFT JOIN`
- [ ] `PostgresRecoveryEmails.kt` contains no `lowercase(`, no `uppercase(` and no `Normalizer` —
      the fold is in SQL
- [ ] Both greped KDoc phrases appear in `RecoveryEmails.kt`
- [ ] `PostgresRecoveryEmailsReadsTest` and `VerificationSweepTest` pass with **no line of either
      file changed** except `VerificationSweepTest`'s one new override and its import
- [ ] Every command in `verify:` exits 0

## Proof

Each step names a mutation and the gate that catches it. Two of the five are honest **inert**
predictions, written down rather than dressed up.

1. Replace the SQL fold with a Kotlin one: bind `address.value.lowercase()` and drop
   `lower(? COLLATE …)` from the statement.
   **The `grep -qF` of the fold fragment reddens**, exiting 1, and it is the only gate that does —
   `compileKotlin` is happy and both merged suites stay green, because neither exercises this
   statement at all. Run it. This is the single most valuable mutation here: a Kotlin fold is a
   silent, case-dependent failure to find an address that SQL considers the same one, and until
   `TASK-041644` lands this `grep` is the *only* thing standing between it and `develop`.
2. Change the `JOIN` to a `LEFT JOIN`.
   **`grep -c 'LEFT JOIN' … | grep -qx 0` reddens, and it is the only gate that does.** Predicted
   explicitly, because the obvious guess is wrong: the fixed-string join `grep` **stays green**,
   since `LEFT JOIN credential c ON …` contains `JOIN credential c ON …` as a substring. One gate,
   not two. Say so in the PR, and say what the second gate would have been: `TASK-041644`'s
   `anOwnerWithNoPasswordCredentialAnswersNull`, which does not exist yet. Revert.
3. Drop `AND c.kind = 'password'` from the join condition.
   **The fixed-string join grep reddens alone.** No compile error, no suite change. Record it: with
   `DEC-027` open and `CredentialKind` built to admit `oauth:<provider>`, this statement returning
   two rows is the failure this literal prevents, and again nothing behavioural gates it until the
   next ticket.
4. Add a second overload `public suspend fun resetRecipientOf(playerId: PlayerId): ResetRecipient?`
   to the port, implemented over `credential` alone.
   **`grep -c 'fun resetRecipientOf' … | grep -qx 1` reddens**, counting 2. `TASK-041642`'s gate
   **stays green** — and that is the finding, not a defect: it reads `Credentials`, and this
   overload is on `RecoveryEmails`, which is precisely the *"a handle read added to some other type
   passes this gate"* limit that ticket's KDoc states. Two gates, two surfaces, and neither covers
   the other. Revert.
5. Change `ThrowingRecoveryEmails`' new body from `error(...)` to `null`.
   **Nothing reddens.** Stated as inert rather than dressed up: no test in `VerificationSweepTest`
   calls this member, so the two bodies are indistinguishable to every assertion in the repository
   today. It is written as `error(...)` because every other member of that object is, and because a
   double that silently answers *no recipient* is the kind of fixture that makes a future route
   test pass for the wrong reason. Restore `error(...)`; this is a review point, not a gated one.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**This ticket ships a security-relevant statement with no behavioural test, by design — and the
deferral was checked rather than accepted.** Every behavioural property goes to `TASK-041644`. The
coder verified the consequence empirically: strip the parameter-side `COLLATE` from
`SELECT_RESET_RECIPIENT_SQL` and the compile plus both merged suites stay green; only the
fixed-string grep reddens.

That clause is the one whose removal already produced a live cross-account leak in this story — a
lookup for player one's own `İ@x.test` returning **player two's** `PlayerId`. So the review's job was
not to confirm the deferral was documented but to confirm the ticket it names **closes** it. It did
not. `TASK-041644`'s case-fold test fixtured `Bob@Example.com` / `BOB@example.COM`, plain ASCII,
which folds identically under every collation and passes with the clause deleted. **The PR was held**
until `TASK-041644` (#1045) carried a fixture that can fail; that merged first, and this landed
after.

The replacement was measured against real Postgres rather than reasoned. Storing `İ` (U+0130) and
asking both spellings gives: parameter-side strip → the exact-spelling lookup answers nothing;
column-side strip → both Unicode lookups answer nothing; **both** sides stripped → the
combining-sequence lookup answers nothing while the exact one still succeeds. The ASCII pair is blind
to all three. Two things that measurement settled and reasoning would have got backwards: **the
direction is load-bearing** — storing the already-folded spelling and asking `İ`, which reads more
naturally as a case test, is blind to the column-side strip because `lower(i+U+0307)` is a fixed
point under both collations — and **one lookup cannot cover the set**, since only the exact spelling
catches the parameter-side strip and only the combining sequence catches the pin dropped from both
halves.

**The `LEFT JOIN` grep is over- and under-sensitive, and something real stands behind it.** It
reddened on the coder's own KDoc, which used the words *"never a `LEFT JOIN`"* twice — a literal
substring count with no notion of code versus prose, whose failure looks nothing like its cause. It
also misses `left join`, `LEFT  JOIN`, a line break between the words, and `LEFT OUTER JOIN`. What
makes the deferral legitimate anyway is `TASK-041644`'s `anOwnerWithNoPasswordCredentialAnswersNothing`,
which runs against real Postgres and catches any outer-join spelling regardless of source text.

**Proof step 5 predicted an inert mutation and was right.** Changing `ThrowingRecoveryEmails`'s new
body from `error(...)` to `null` reddens nothing, because no test in `VerificationSweepTest` calls it.
Writing that down is the correct form — a step that predicts green is evidence, where a step quietly
dropped is not.
