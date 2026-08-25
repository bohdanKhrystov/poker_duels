---
schema: 2
id: TASK-041640
title: A failure between the password and the sessions undoes both
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, auth, security, invariant]
depends_on: [TASK-041639]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPasswordResetsConsumeTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A manufactured failure at `consume`'s `DELETE FROM auth_session` leaves the password unrewritten and
the token unspent, so the boundary `TASK-041614`'s title actually foregrounds — *a reset that
succeeds cannot leave the attacker's session running* — reddens a test when it is split.

## Why this exists

`TASK-041614` is one connection, `autoCommit` off, one commit, and **its five tests all stay green if
a `commit()` is inserted between the credential write and the session delete**. `TASK-041639` closed
the other boundary with a data fixture. This one cannot be closed that way: the session delete is
unconditional but for `player_id` and has no `CHECK`, no trigger and no unique index to violate, so
nothing reachable through `consume` can make it fail on data alone. The failure has to be
manufactured.

This repository has done exactly that before, and the harness is proven: `PostgresDeviceBindingsTest`
wraps a real `DataSource`, hands out a wrapped `Connection`, and returns a `PreparedStatement` whose
`executeUpdate()` throws **before delegating** — so the statement is prepared against the real
driver, the write before it really ran against the real container, and the `rollback()` inside the
production `catch` is a real statement on a real connection. Everything downstream of the throw is
real; only the throw is invented.

Left unclosed, the defect this gates is the one worth the most: the owner is told `204`, believes
they have recovered, and the attacker's session is still alive.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPasswordResetsConsumeTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDeviceBindingsTest.kt` — the wrapper
classes at the bottom of the file and `aFailureBetweenTheStatementsRollsBackTheUpdate`, the test that
drives them. This is the shape to copy, and the class KDoc there says why each layer is real;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPasswordResets.kt` — the three statements
`consume` prepares, in order: `DELETE … RETURNING` on `password_reset`, `UPDATE credential`,
`DELETE FROM auth_session`. `## Proof` mutates this file temporarily; **every step ends with a
revert, and the PR diff must contain no change to it**;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- Add **one** test method to `PostgresPasswordResetsConsumeTest`, and the wrapper classes it needs at
  the bottom of that same file.
- **The wrapper picks its victim by SQL, not by a call index.** It fails the prepared statement whose
  SQL contains `auth_session`, and records that statement's SQL. An index (*fail the third
  `prepareStatement`*) silently aims somewhere else the day `consume` prepares a fourth statement,
  and the test would keep passing while gating a different boundary; matching on the table name says
  what it means and moves with the code.
- **Name the wrapper classes so they collide with nothing.** A `private` top-level class in Kotlin is
  package-visible on the JVM, so a second `FailingStatementDataSource` or `FailingPreparedStatement`
  in `duels.poker.server.db` is a duplicate-JVM-class-name error, not a shadowed private. Use
  `SessionDeleteFailingDataSource` for the one top-level class and keep the `Connection` and
  `PreparedStatement` wrappers as `private inner` classes of it, which cannot collide at all.
- The wrapper exposes the failed statement's SQL — `val failedSql: String?`, null until it fires —
  and the test asserts it names `auth_session`. See `## Proof` step 2 for why that assertion is not
  decoration.
- Every assertion after the throw reads through the **unwrapped** `dataSource`: the `credentials`,
  `authSessions` and `passwordResets` built in `@BeforeEach`. Nothing trusts the connection under
  test to describe its own state.
- The six tests already in the file do not move: no assertion is added, removed, weakened or renamed
  in any of them.

## Out of scope

- **Extracting the harness into a shared test fixture, and this is deliberate.** Doing it would
  either leave two identical harnesses under two names anyway, or modify
  `PostgresDeviceBindingsTest.kt`, whose `aFailureBetweenTheStatementsRollsBackTheUpdate` is a merged
  gate, for no behavioural gain. Twenty-odd duplicated lines is the cheaper of the two. If a third
  caller ever wants it, that is a ticket of its own; it is **not yet ticketed**, and a reviewer
  should not manufacture it here.
- **Changing `PostgresPasswordResets.kt`.** The shipped code already satisfies this test. If it fails
  against `develop` with no mutation applied, **stop and report it** — that is a defect in merged
  code and a separate ticket.
- Making the `DELETE … RETURNING` or the `UPDATE credential` fail. Statement one is `TASK-041639`'s
  boundary and it is reachable with data; statement two failing rolls back to the same end state as
  statement three failing, which is exactly the ambiguity `failedSql` exists to remove.
- A test that the transaction is *one connection*. `consume` takes `dataSource.connection` once and
  the wrapper counts nothing, so nothing here asserts it; that stays a review property, as it was in
  `TASK-041614`.
- Retry, compensation or a second attempt inside `consume`. The `SQLException` propagates, as it does
  today; `ADR-0031` §4 asks for atomicity, not recovery.

## Tests

`PostgresPasswordResetsConsumeTest` — one row added to the six there after `TASK-041639`.

| Test | Proves |
| --- | --- |
| `aFailureAtTheSessionDeleteUndoesThePasswordAndTheToken` | A player holds a `password` credential on the old secret, a live reset token and one session. `consume` is called on a `PostgresPasswordResets` built over `SessionDeleteFailingDataSource`, so the `DELETE FROM auth_session` throws instead of reaching the driver. The call throws `SQLException`; the wrapper's `failedSql` names `auth_session`; the **old** secret still verifies and the new one does not; the session still resolves; and **the same token**, consumed again through the unwrapped fixture, answers `true` and writes the new secret. Under a `commit()` between the credential write and the session delete, the password and the token spend are both already durable and three of those assertions redden |

The order inside the test is load-bearing: arrange with the unwrapped fixtures, act through the
wrapped one, assert the four survival properties, and only then consume again — the second consume
deletes the session, so the session assertion must come before it.

## Acceptance criteria

- [ ] `PostgresPasswordResetsConsumeTest.aFailureAtTheSessionDeleteUndoesThePasswordAndTheToken`
      passes
- [ ] The failing call is asserted with `assertFailsWith<SQLException>`
- [ ] The wrapper selects its victim by testing the prepared SQL for `auth_session`, and no call
      index appears anywhere in it
- [ ] The test asserts `failedSql` is non-null and contains `auth_session`
- [ ] It asserts the **old** secret verifies and that the new secret does **not**
- [ ] It asserts `authSessions.playerOf(session)` still returns the player id, **before** the second
      `consume`
- [ ] It asserts a second `consume` with the same token, through the unwrapped `passwordResets`,
      returns `true`
- [ ] The only top-level class the ticket adds is `SessionDeleteFailingDataSource`, a name that
      appears nowhere else under `poker-server/src/test/kotlin/duels/poker/server/db/`
- [ ] The six pre-existing tests are unchanged: no assertion added, removed, weakened or renamed, and
      no test method renamed
- [ ] `git status` shows no change to `PostgresPasswordResets.kt` or any other file outside the Files
      table
- [ ] Every command in `verify:` exits 0

## Proof

1. In `PostgresPasswordResets.consume`, insert `connection.commit()` between the credential write and
   `deleteSessions` — immediately after the `rewriteCredential(...) != 1` guard is passed.
   **`aFailureAtTheSessionDeleteUndoesThePasswordAndTheToken` reddens alone**, at its old-secret
   assertion: the credential write committed before the statement that threw, so the old secret no
   longer verifies. Predict the rest too, and check it:
   `aRefusedCredentialWriteLeavesTheTokenSpendable` (`TASK-041639`'s) **stays green**, because the
   mutation sits inside a branch a missing credential never reaches, and the five original tests stay
   green because nothing throws in them. Two tests, two boundaries, neither covering the other's —
   that is the whole shape of this pair. Revert.
2. Change the wrapper's match from `auth_session` to `credential`, so the throw lands on the `UPDATE`
   instead. **The test reddens on the `failedSql` assertion, and on nothing else** — every other
   assertion in it still passes, because a rollback at *either* boundary leaves the database in the
   same state and no assertion made after the fact can tell the two apart. That is the finding this
   ticket records: the test's claim to be about the second boundary rests entirely on where the
   wrapper aims, so the aim has to be asserted rather than assumed. Note that the failure mode here
   is degradation rather than vacuity — a wrapper aimed at statement two re-gates `TASK-041639`'s
   boundary — which is why `failedSql`, and not a stronger end-state assertion, is the right
   instrument. Restore.
3. Change the wrapper's match to `auth_sessions`, which matches no statement, so it never fires.
   **The test reddens at `assertFailsWith<SQLException>`** — `consume` succeeds and throws nothing. A
   harness that quietly does nothing therefore cannot pass, which is the failure a silent fault
   injector produces in every other repository that has one. Restore.
4. Do **not** try `rollback()` → `commit()` in the `catch` block. Postgres downgrades a `COMMIT` on an
   aborted transaction to a rollback server-side, so the edit changes nothing observable and reddens
   nothing; a green run after it would be read as evidence that this test is inert. Step 1 is the
   mutation that works, and it is the one to run.
5. With step 1's mutation applied, delete the old-secret and new-secret assertions and keep only the
   token one. **The test still reddens**, on the second `consume` answering `false`. Recorded so a
   reviewer knows those three assertions are one boundary asserted three ways rather than three
   independent gates, and does not read their redundancy as thoroughness. Restore both, and revert
   step 1.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The session-survives assertion is vacuous, and it is kept anyway.** `assertEquals(playerId,
authSessions.playerOf(session), "the session must still resolve — its delete never committed
either")` cannot be reddened by any mutation of `consume`. The wrapper's `executeUpdate()` sets
`failedSql` and throws *before* delegating, so the `auth_session` row is never reached at all — a
`consume` that caught the `SQLException` and returned `false` without ever calling `rollback()`
would pass it identically. The coder volunteered this as its diff's weakest assertion before being
asked, and the reviewer confirmed it independently rather than accepting `PostgresDeviceBindingsTest`
as precedent.

It stays because it is an acceptance criterion and because removing it would prove nothing: nothing
imaginable would notice its absence, by the same argument that shows it cannot redden. What is worth
recording is that **its message is wrong in a way that matters** — *"its delete never committed"*
implies a write that was attempted and rolled back, when the statement never ran. The sibling in
`PostgresDeviceBindingsTest` says *"the DELETE never really ran"*, which is the honest form. The
boundary this ticket is named for is genuinely gated, by the two credential assertions above it;
Proof step 1 reddens at the old-secret line, not here.

**A correction to the coder's own report, from the reviewer.** The report claimed a token spend split
into a separately-committed transaction would leave every assertion passing, indistinguishable from a
correct implementation. It would not: an independently-committed token delete leaves the row gone by
the time the second `consume` runs, so *the same token must still work* reddens. The real limitation
is narrower and already refused in `## Out of scope` — this test cannot verify `consume` uses **one
connection**, and a correctly-coordinated multi-connection implementation would be behaviourally
indistinguishable. That stays a review property.

**Two escape routes from the wrapper, neither reachable by the shipped code.** It matches
`sql.contains("auth_session")`, so schema qualification (`public.auth_session`) is still caught, but
**different casing** is not — Kotlin's `contains` is case-sensitive while Postgres folds unquoted
identifiers — and neither is a **different statement API**: only the single-`String`
`prepareStatement` overload is overridden, and `createStatement()` or any other overload passes
through the delegate unguarded. Both are properties of the harness rather than defects here, and both
are the kind of thing that goes unnoticed until someone edits `consume`.
