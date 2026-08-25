---
schema: 2
id: TASK-041636
title: The attach path gets the quarter hour of silence the reset path has
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, db, auth, security]
depends_on: [TASK-041608]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresRecoveryEmailsClaimTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`PostgresRecoveryEmails.claimPending` answers `Suppressed` and writes nothing when the player
already has a pending row issued less than fifteen minutes ago, so the attach path has the same
durable per-account cap on mail that `PasswordResets.issue` already has.

## Why this exists

`ADR-0031` §5's *Budgets* paragraph names both endpoints and then states one rule: *"a mail is sent
only if the player has no live token issued within the last 15 minutes, read from `issued_at` on
the existing `UNIQUE (player_id)` row."* Both `password_reset` and `email_verification` carry an
`issued_at` and a `UNIQUE (player_id)`, so the sentence covers both paths. **The split built it on
one.** `ADR-0079` §Context found that while choosing the two attempt budgets, and its §Consequences
records the residual: *"on the best reading this is a defect against `TASK-041607`, `TASK-041608`
and `TASK-041625` rather than a new question — a ticket for the planner, due before `EPIC-07`
configures a sender."* It deliberately raised no `DEC`: the mechanism, the number and the column
all already exist on the sibling table.

This is the middle of the three. `TASK-041607` declared `ClaimPendingResult`; this ticket makes
`Suppressed` reachable; `TASK-041637` makes the handler stop mailing on it.

**Why this cap matters more than the attempt budget beside it.** `ADR-0079` §2 is explicit that
`recoveryEmailMaxAttempts` is keyed by remote address, so a second source address buys a fresh
five. This rule is keyed by account and holds **across every source address at once**, across
restarts, with no in-memory state — which is exactly the bound an address-keyed budget cannot
express. `ADR-0079`'s numbers were chosen assuming this rule never arrives; when it does,
`recoveryEmailMaxAttempts` becomes a candidate for raising rather than a thing to leave alone.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresRecoveryEmailsClaimTest.kt` | modify |

Three tests are **added**; no existing test in the file changes, no assertion moves and none is
weakened. `TASK-041608` already advances its fixture clock sixteen minutes between the two claims
in `aSecondClaimLeavesExactlyOnePendingRow`, precisely so that this ticket does not have to reach
into it.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` — `ClaimPendingResult` and
the KDoc clause this ticket makes true;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §5, the ***Budgets*** paragraph;
`docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md`
§Consequences, the *residual* bullet only.

**Do not read `PostgresPasswordResets.kt`.** `TASK-041613` implements the identical rule there and
copying from it is the obvious move, but the two differ in the column lifetime and in the return
type, and a copied `RESET_LIFETIME`/`RESEND_WINDOW` pair is the defect Proof step 4 exists to
catch. Write it from `ADR-0031` §5 and from the statement already in this file.

## Scope

- `claimPending` gains, as the **first statement inside the transaction it already opens**,
  `SELECT issued_at FROM email_verification WHERE player_id = ?`.
- If a row exists and its `issued_at` is within `RESEND_WINDOW` of `clock.instant()`: commit and
  return `ClaimPendingResult.Suppressed`, having run neither the `DELETE` nor the `INSERT`.
- Otherwise: the existing `DELETE` then `INSERT`, and `ClaimPendingResult.Claimed` — unchanged.
- `private val RESEND_WINDOW: Duration = Duration.ofMinutes(15)`, a second private constant beside
  `VERIFICATION_LIFETIME`. The two are **different durations for different things** and the file
  must not derive one from the other.
- The read is `issued_at`, never `expires_at`. On this table they differ by twenty-four hours, so
  reading the wrong one turns a fifteen-minute silence into a day of it.
- **The `SELECT` is inside the same transaction and on the same connection as the write.** It is
  not a pre-check on a separate connection and not a call the caller makes first. A read-then-write
  window lets two concurrent attaches both find no live row, both write, and both cause a mail —
  which uncaps the one thing this rule exists to cap, and is the window `ADR-0031` §4 closed on the
  sibling table.
- The suppressed path leaves the outstanding row exactly as it was: same `token_hash`, same
  `address`, same `issued_at`, same `expires_at`. §5: inside the window the request is a complete
  no-op, and *"crucially the outstanding token is not invalidated, so a double-click does not
  destroy the link the player is about to use."*

## Out of scope

- **The send condition — `TASK-041637`.** This ticket changes no HTTP behaviour: no endpoint exists
  over `claimPending` yet, no status code moves, and `ADR-0031` §5's `202` is unaffected under every
  outcome. Nothing here is gated by a route test because there is no route to gate.
- **Sharing a helper with `PostgresPasswordResets`.** Two tables, two statements, two lifetimes and
  two return types; a shared helper would take the table name as a parameter, which is a string
  that decides which account's mail is capped. Deliberately duplicated. **This refusal produces no
  test** — nothing fails if someone later extracts one — so it is a review criterion, recorded here
  rather than left to be inferred from the absence of a helper.
- **Making the window configurable.** `ADR-0031` §5 fixes fifteen minutes and `ADR-0079` §2 prices
  the attempt budget against it as a constant. A config key would be a fifth number for an operator
  to hold and a way to switch the cap off. Not ticketed anywhere, and it should not be without an
  ADR that supersedes §5.
- **Reporting how long is left.** `ClaimPendingResult.Suppressed` carries no payload by
  `TASK-041607`'s criterion; a remaining-window value is a thing a caller could surface, and §5's
  `202` is identical in every case.
- The remote-address attempt budget — `TASK-041628`, a second and independent limiter over the same
  endpoint, whose numbers are `ADR-0079`'s.

## Tests

`PostgresRecoveryEmailsClaimTest`, three new methods on the fixture the class already builds.

| Test | Proves |
| --- | --- |
| `aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` | Claim `a@x.test`; advance the injected clock **14 minutes**; claim `b@x.test` with a different token. Returns `Suppressed`, and the stored row still has the **first** token's `token_hash` and the address `a@x.test`. Asserted on the hash and the address, never on a row count — a count of `1` is satisfied by a replacement |
| `aSecondClaimAfterAQuarterHourReplacesTheFirst` | Claim; advance **16 minutes**; claim again. Returns `Claimed`, exactly one row for that player, and its `token_hash` is the **second** token's. The boundary from the other side |
| `oneAccountsSilenceIsNotAnothers` | Player A claims. Within the window, player B claims and gets `Claimed` with their own row, and A's row keeps its original `token_hash`. Two players in one database — a suppression `SELECT` with no `WHERE player_id = ?` passes both tests above |

## Acceptance criteria

- [ ] `PostgresRecoveryEmailsClaimTest.aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` passes
- [ ] `PostgresRecoveryEmailsClaimTest.aSecondClaimAfterAQuarterHourReplacesTheFirst` passes
- [ ] `PostgresRecoveryEmailsClaimTest.oneAccountsSilenceIsNotAnothers` passes
- [ ] The four pre-existing `PostgresRecoveryEmailsClaimTest` tests pass **unchanged** — this ticket
      adds methods and edits none, moves no assertion and weakens none
- [ ] The two window tests sit on **opposite sides** of fifteen minutes, at 14 and 16 minutes
- [ ] `aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` asserts the stored `token_hash` equals
      the **first** token's digest, and does not rely on a row count
- [ ] `oneAccountsSilenceIsNotAnothers` holds two players in one database
- [ ] `PostgresRecoveryEmails.kt` reads `issued_at`, not `expires_at`, for the suppression check
- [ ] The suppression `SELECT` runs on the **same connection** as the `DELETE` and the `INSERT`,
      inside the same `autoCommit = false` transaction, with a single `commit()` on both paths
- [ ] Returning `Suppressed` performs no `INSERT` and no `DELETE`
- [ ] `RESEND_WINDOW` is its own constant and is not derived from `VERIFICATION_LIFETIME`
- [ ] `PostgresRecoveryEmails.kt` contains no `Instant.now()` and no `ServerClock`
- [ ] Every command in `verify:` exits 0

## Proof

Each mutation is applied, the suite is run, and the mutation is reverted.

1. Change `RESEND_WINDOW` to `Duration.ofMinutes(10)`.
   **`aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` reddens alone**, *expected Suppressed,
   got Claimed*, and its hash assertion fails too — at 14 minutes the window has already closed.
   `aSecondClaimAfterAQuarterHourReplacesTheFirst` expects `Claimed` at 16 minutes and still gets
   it. Revert.
2. Change `RESEND_WINDOW` to `Duration.ofMinutes(30)`.
   **`aSecondClaimAfterAQuarterHourReplacesTheFirst` reddens alone**, *expected Claimed, got
   Suppressed*. Steps 1 and 2 are a pair: either alone leaves one side of the boundary unmeasured,
   which is why both window tests exist. Revert.
3. On the suppressed path, run the `DELETE` and the `INSERT` anyway while still returning
   `Suppressed`.
   **`aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` reddens alone**, on the `token_hash`
   and `address` assertions. The returned value is unchanged, so a test asserting only `Suppressed`
   would have stayed green. **Run this one** — it is the mutation that destroys the verification
   link a player is about to click while still reporting that nothing happened, and it is the
   reason the assertion is on the stored row rather than on a count. Revert.
4. Read `expires_at` instead of `issued_at` in the suppression `SELECT`.
   **`aSecondClaimAfterAQuarterHourReplacesTheFirst` reddens alone**, *expected Claimed, got
   Suppressed*: `expires_at` is twenty-four hours ahead, so at 16 minutes the row still looks fresh
   and every claim is suppressed for the rest of the day.
   `aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive` expects `Suppressed` and passes.
   Note the difference from the sibling: on `password_reset` the lifetime is one hour, so the same
   mutation there yields a 45-minute silence, and here it yields a 24-hour one — same defect, two
   orders of magnitude apart, which is why the constants must not be shared. Revert.
5. Drop `WHERE player_id = ?` from the suppression `SELECT`.
   **`oneAccountsSilenceIsNotAnothers` reddens alone**, *expected Claimed, got Suppressed* for
   player B. Both single-player tests hold one player and pass unchanged. Revert.
6. **The whole-feature control — run it and record it.** Delete the suppression `SELECT` and the
   branch entirely, returning to `TASK-041608`'s implementation.
   **Exactly one test reddens: `aSecondClaimInsideAQuarterHourLeavesTheFirstTokenLive`.**
   `aSecondClaimAfterAQuarterHourReplacesTheFirst` expects `Claimed` and gets it;
   `oneAccountsSilenceIsNotAnothers` expects `Claimed` for player B and gets it; all four
   pre-existing tests stay green. That is the honest measure of this ticket's gate — **one test
   stands between the tree and the defect `ADR-0079` named** — and it is why step 3 matters more
   than the count of tests here.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
