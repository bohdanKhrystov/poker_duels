---
schema: 2
id: TASK-041613
title: One live reset token, and a quarter hour of silence
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, db, auth, security]
depends_on: [TASK-041612]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPasswordResetsIssueTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`PasswordResets.issue` mints a one-hour token, supersedes any the player already had, and answers
`false` when one was issued less than fifteen minutes ago — writing nothing and leaving that
outstanding token alive.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PasswordResets.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresPasswordResets.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPasswordResetsIssueTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresRecoveryEmails.kt` — the transaction,
clock and digest shape this class copies;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4 and §5's *Budgets* paragraph.

## Scope

- `public interface PasswordResets` in `duels.poker.server.auth`, with
  `suspend fun issue(playerId: PlayerId, token: ResetToken): Boolean` and
  `suspend fun consume(token: ResetToken, secret: PresentedSecret): Boolean` — the second declared
  now and implemented by `TASK-041614`, so the port is written once.
- `internal class PostgresPasswordResets(dataSource, clock, tokens)` implementing `issue`, with
  `consume` left as `TODO()` naming `TASK-041614`.
- `issue` runs on one connection with `autoCommit = false`:
  1. `SELECT issued_at FROM password_reset WHERE player_id = ?`. If a row exists and its `issued_at`
     is **within 15 minutes** of `clock.instant()`, commit and return `false` — writing nothing.
  2. Otherwise `DELETE FROM password_reset WHERE player_id = ?` then `INSERT`, and return `true`.
- `false` means *no mail should be sent, and the outstanding token still works*. That is the whole
  suppression rule: `ADR-0031` §5 says inside the window the request is a complete no-op, and
  *"crucially the outstanding token is not invalidated, so a double-click does not destroy the link
  the player is about to use."*
- The suppression is read from `issued_at` on the existing `UNIQUE (player_id)` row — no new state,
  survives a restart, caps one account at four recovery mails an hour.
- `RESET_LIFETIME = Duration.ofHours(1)` and `RESEND_WINDOW = Duration.ofMinutes(15)` as private
  constants. Instants from the injected `java.time.Clock` (`ADR-0062` §5 amends §4's `ServerClock`).

## Out of scope

- `consume` — `TASK-041614`.
- The endpoint, and answering `202` whatever `issue` returns — `TASK-041626`.
- Sending anything, and the `<baseUrl>/#/reset/<token>` link (`ADR-0081` §1) — `TASK-041633` builds
  it and `ADR-0077` settles the seam that delivers it.
- The remote-address budget. That is a *second*, independent limiter over the same endpoint, and
  `ADR-0079` set it at ten a minute for this reason: this one, the durable per-account defence §5
  fixes at fifteen minutes, is what actually caps mail to one victim.
- Suppressing on `expires_at` rather than `issued_at`. §5 names `issued_at`; the two differ by
  exactly the lifetime and reading the wrong one turns a 15-minute window into a 45-minute one.

## Tests

`PostgresPasswordResetsIssueTest`, with a mutable injected `Clock` and a `RecoveryTokens` over a
stubbed `SecureRandom` so each minted token is pinned.

| Test | Proves |
| --- | --- |
| `issuingStoresTheHashAndAnHoursExpiry` | `issue` returns `true`; the row's `token_hash` is `recoveryTokenDigest(token)`, `issued_at` is the injected instant, and `expires_at` is exactly one hour later |
| `aSecondRequestInsideAQuarterHourWritesNothing` | Issue, advance the clock 14 minutes, issue again with a **different** token: returns `false`, and the stored `token_hash` is still the **first** token's. The outstanding link survives — asserted on the hash, not on a row count, because a count of `1` is satisfied by a replacement |
| `aSecondRequestAfterAQuarterHourSupersedesTheFirst` | Issue, advance 16 minutes, issue again: returns `true`, exactly one row for that player, and its `token_hash` is the **second** token's. The boundary from the other side |
| `oneAccountsSilenceIsNotAnothers` | Player A issues; within the window player B issues and gets `true`, with their own row. Two players in one database — the suppression read must be correlated to `player_id`, and a `SELECT issued_at FROM password_reset` with no `WHERE` passes every test above |

## Acceptance criteria

- [ ] All four `PostgresPasswordResetsIssueTest` tests pass
- [ ] `aSecondRequestInsideAQuarterHourWritesNothing` asserts the stored `token_hash` equals the
      **first** token's digest, and does not rely on a row count
- [ ] The two window tests sit on **opposite sides** of fifteen minutes, at 14 and 16 minutes
- [ ] `oneAccountsSilenceIsNotAnothers` holds two players in one database
- [ ] `PostgresPasswordResets.kt` reads `issued_at`, not `expires_at`, for the suppression check
- [ ] `PostgresPasswordResets.kt` contains no `Instant.now()` and no `ServerClock`
- [ ] `issue` returning `false` performs no `INSERT` and no `DELETE`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change the suppression read to `expires_at` instead of `issued_at`.
   **`aSecondRequestAfterAQuarterHourSupersedesTheFirst` reddens alone**, *expected true, got
   false*: at 16 minutes the stored `expires_at` is still 44 minutes ahead, so the window never
   opens. `aSecondRequestInsideAQuarterHourWritesNothing` expects `false` and passes — which is
   exactly why the 16-minute test is not optional. Revert.
2. Change `RESEND_WINDOW` to `Duration.ofMinutes(30)`.
   **`aSecondRequestAfterAQuarterHourSupersedesTheFirst` reddens alone.** Revert.
3. Change `RESEND_WINDOW` to `Duration.ofMinutes(10)`.
   **`aSecondRequestInsideAQuarterHourWritesNothing` reddens alone**, *expected false, got true*,
   and its hash assertion fails too. The pair of mutations is what pins the boundary; either alone
   leaves one side unmeasured. Revert.
4. On the suppressed path, run the `DELETE` and `INSERT` anyway while still returning `false`.
   **`aSecondRequestInsideAQuarterHourWritesNothing` reddens alone**, on the hash assertion. The
   return value is unchanged, so a test asserting only `false` would have passed — this is the
   mutation that destroys the link the player is about to click, and the one this ticket exists to
   prevent. Revert.
5. Drop the `WHERE player_id = ?` from the suppression `SELECT`.
   **`oneAccountsSilenceIsNotAnothers` reddens alone**, *expected true, got false* for player B.
   Revert.
6. Change `RESET_LIFETIME` to `Duration.ofHours(24)`.
   **`issuingStoresTheHashAndAnHoursExpiry` reddens alone**. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**All six `## Proof` mutations reddened exactly as written**, each hitting one test, with the failure
messages checked rather than the pass counts. That makes this the sixth fully-correct Proof of
twenty-seven examined in this run — the base rate is twelve wrong or incomplete.

**The hash assertion is what makes the suppression real.** Returning `false` while still running the
`DELETE`/`INSERT` reddens **only** *"a suppressed request must leave the first token's hash live, not
the second's"*; the return-value assertion stays green throughout. So a suppression that renamed the
outcome while destroying the outstanding token would have shipped on a return-value test alone. This
is the same shape `TASK-041636` established on the attach side, and it was worth copying rather than
re-deriving.

**The clock mutation reddens two assertions, and one of them is not obvious.** Swapping
`clock.instant()` for `SELECT now()` fails the absolute-instant check in
`issuingStoresTheHashAndAnHoursExpiry` — expected, since `TASK-041608` proved a duration-only
assertion cannot see it — and *also* the return value in
`aSecondRequestAfterAQuarterHourSupersedesTheFirst`, because a test clock's sixteen-minute advance no
longer moves the real database's `now()`. The second failure is the one that would confuse a reader
debugging it later.

**`tokens` is a forward-declared constructor parameter and the ticket asked for it.** `issue` never
reads it; `TASK-041614`'s `consume` will. The Scope names the three-parameter constructor explicitly,
so `@Suppress("unused")` follows the ticket and is the minimum that holds detekt at zero. Recorded
because a suppressed warning is a permanent small cost and the next reader should know it was
deliberate rather than inherited.
