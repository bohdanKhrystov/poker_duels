---
schema: 2
id: TASK-041641
title: The profile's recovery flag is that player's, and a pending address is not one
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, db, security]
depends_on: [TASK-041616]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest.theProfileReadsTrueForAPlayerWithAVerifiedAddress' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest.aPendingAddressIsNotARecoveryEmail' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`ProfileResponse.hasRecoveryEmail`, read back out of a real database, describes **that** player and
counts a pending address as no address — so wiring it to a constant, in either direction, reddens a
test.

## Why this exists

`TASK-041616` added the field and the correlated `EXISTS`, and shipped **no test that reads the
field back from Postgres**. Its coder proved the gap by mutation rather than by inspection:

- `PostgresProfileReads`' `hasRecoveryEmail` wired to a constant `false` reddens **nothing**;
- the same wired to a constant `true` reddens **nothing**;
- the uncorrelated `EXISTS (SELECT 1 FROM recovery_email)`, and an `EXISTS` reading
  `email_verification` instead of `recovery_email`, both build fully green.

Four mutations, four green runs. `TASK-041616` had named these two methods but could not carry
them — it declares `atomic:`, so its six-row *Files* table is its whole change, and the file is not
in it. It is not in it because **no gate names it**: `PostgresProfileReadsTest` constructs no
`ProfileResponse`, so it compiles and passes untouched through that ticket's edits, and its probe
reached green without it. That is a dependency, not a coupling, so this is a separate ticket rather
than a fourth `atomic:` item asserting a gate that does not exist.

The two `verify:` filters below name **methods**, not just the class. A whole-class filter exits 0
whether or not the methods exist — which is exactly how the omission got through `TASK-041616`'s
gates. A method filter that matches nothing fails the Gradle task.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` — `PROFILE_OF_SQL` and
the `ProfileResponse(...)` construction in `profileOf`. `## Proof` mutates this file temporarily;
**every step ends with a revert, and the PR diff must contain no change to it**;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` — `claimPending` and
`verifyPending`, which are how a fixture reaches each of the two states;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsReadsTest.kt` — the same
two properties asserted one layer down, in the idiom to copy;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and §6.3.

## Scope

- Add **two** test methods to the existing `PostgresProfileReadsTest` class, and one field to reach
  the fixture with:

  ```kotlin
  private lateinit var recoveryEmails: PostgresRecoveryEmails
  ```

  assigned in the existing `setupDatabase()` as
  `PostgresRecoveryEmails(dataSource, Clock.fixed(Instant.now(), ZoneOffset.UTC))` — the exact line
  `PostgresRecoveryEmailsReadsTest` uses. `PostgresRecoveryEmails` is `internal` and this test is in
  the same module and package, so it is visible; no new helper, no new file, no new source-set
  wiring.
- Both new methods use the `alice` and `bob` the existing `@BeforeEach` already resolves. **Each
  method asserts on both players, in the one database `PostgresTestSupport.freshDatabase()` gives
  it.** One player per method, split across two methods, is the shape this ticket exists to refuse:
  each would pass on its own against an `EXISTS` correlated to nothing.
- A verified address is reached only through `claimPending` then `verifyPending` — never by an
  `INSERT INTO recovery_email` written in the test. A hand-written row would let the two tables
  drift apart from what the production path actually writes.
- Add one paragraph to the class KDoc saying what these two gate: that the flag is per-player, and
  that `recovery_email` alone decides it.
- Every test already in the file stays exactly as it is: no assertion added, removed, weakened or
  renamed, and no test method renamed.

## Out of scope

- **Changing `PostgresProfileReads.kt`, or any production file.** `TASK-041616` shipped the
  behaviour; the gate was what was missing. If either new test fails against `develop` with no
  mutation applied, **stop and report it** — that is a defect in merged code and a different ticket,
  not a licence to edit a production file from inside this one.
- `ProfileDtosTest` and the golden JSON strings — `TASK-041616` owns those and they are merged.
- `PostgresRecoveryEmailsReadsTest`. It already asserts both properties on `RecoveryEmails`
  (`aVerifiedAddressIsFoundByItsOwner`, `aPendingAddressIsFoundByNobody`,
  `aDetachedPlayerReadsFalseAgain`), and it is untouched. **The point here is that the profile's own
  `EXISTS` — a second, separately written statement — agrees with it.** Asserting the two SQL
  strings match each other would be the tautology version of that, so do not.
- `GET /api/me` end to end, and any route test — `TASK-041617` onwards.
- The `verifiedAt` timestamp, the address, and any masked form of it. `ADR-0031` §6.3: the client
  can say recovery is on and can never display the address.

## Tests

`PostgresProfileReadsTest` — two rows added to the class as it stands after `TASK-041616`.

| Test | Proves |
| --- | --- |
| `theProfileReadsTrueForAPlayerWithAVerifiedAddress` | `alice` claims and verifies an address; `bob` does nothing. In **one database**, `profileOf(alice.id)` reads `hasRecoveryEmail = true` and `profileOf(bob.id)` reads `false`. One method, two players, both directions — a constant in either direction reddens here, and so does an `EXISTS` correlated to nothing |
| `aPendingAddressIsNotARecoveryEmail` | `alice` claims an address and never verifies it; `bob` never claims. `alice` reads `false`, pinned as a literal; and `alice`'s answer **equals** `bob`'s, asserted as a second assertion against the never-claiming player rather than inferred. `recovery_email` is the only table that decides this, so an `EXISTS` over `email_verification` reddens |

The literal-`false` assertion and the equality assertion are both required in the second test, and
neither substitutes for the other: the equality alone is `x == x` under any constant, and the
literal alone cannot say the two states are *indistinguishable* rather than merely both empty.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.theProfileReadsTrueForAPlayerWithAVerifiedAddress` passes
- [ ] `PostgresProfileReadsTest.aPendingAddressIsNotARecoveryEmail` passes
- [ ] `theProfileReadsTrueForAPlayerWithAVerifiedAddress` calls `profileOf` **twice, for two
      different players, inside the one method**, asserting `true` for one and `false` for the other
- [ ] `aPendingAddressIsNotARecoveryEmail` calls `profileOf` **twice, for two different players,
      inside the one method**, and holds both an `assertEquals(false, …)` on the pending player and
      an assertion that the pending player's value equals the never-claiming player's
- [ ] Neither method contains an `INSERT INTO recovery_email`; the verified state is reached through
      `claimPending` then `verifyPending`
- [ ] `git status` shows no change to `PostgresProfileReads.kt` or to any file outside the *Files*
      table
- [ ] Every test already in `PostgresProfileReadsTest` before this ticket is unchanged: none
      renamed, no assertion added, removed or weakened
- [ ] Every command in `verify:` exits 0

## Proof

Each step mutates `PostgresProfileReads.kt` only, and each ends with a revert.

1. In `profileOf`, replace the **last** constructor argument of the `ProfileResponse(...)` it builds
   — the `rows.getBoolean(...)` `TASK-041616` added — with the literal `false`, ignoring the column.
   This is the constant-`false` wiring that reddens nothing today.
   **`theProfileReadsTrueForAPlayerWithAVerifiedAddress` reddens alone**, on `alice`'s `true`.
   `aPendingAddressIsNotARecoveryEmail` stays green, because `false` is what both of its players
   already read. Revert.
2. Replace that same argument with the literal `true` — the other direction, which also reddens
   nothing today. **Both new methods redden**: the first on `bob`'s `false`, the second on the
   pinned `assertEquals(false, …)`. Predict both. If only the first reddens, the second has no
   literal assertion and has collapsed into the `x == x` this ticket refuses. Revert.
3. Change the `EXISTS` to the uncorrelated `EXISTS (SELECT 1 FROM recovery_email)`, dropping
   `WHERE r.player_id = p.id`. **`theProfileReadsTrueForAPlayerWithAVerifiedAddress` reddens
   alone**, *expected false, got true* for `bob`, once `alice`'s row exists.
   `aPendingAddressIsNotARecoveryEmail` **stays green** — its database holds no `recovery_email`
   row at all, so an uncorrelated `EXISTS` is still `false` there. That asymmetry is the reason the
   first test carries the verified fixture and the second does not, and if the second reddens here
   its fixture has verified something it should not have. Revert.
4. Change the `EXISTS` to read `email_verification` instead of `recovery_email`.
   **`aPendingAddressIsNotARecoveryEmail` reddens**, *expected false, got true* for `alice`, whose
   pending row is still outstanding. `theProfileReadsTrueForAPlayerWithAVerifiedAddress` **reddens
   too**, on `alice`'s `true` becoming `false`: `verifyPending` deletes the pending row as it
   writes the verified one. Two, and if the first stays green its verified fixture is not going
   through `verifyPending` and step 4 of *Scope* has been skipped. Revert.
5. Delete `theProfileReadsTrueForAPlayerWithAVerifiedAddress` and re-run step 1's mutation.
   **Nothing reddens.** That is the finding this ticket is built on, restated as a check on itself:
   before this ticket, the whole `PostgresProfileReadsTest` class was green under a constant flag,
   and it is this one method that changes that. Restore the method and revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
