---
schema: 2
id: TASK-041612
title: The existing ticker forgets unproven addresses too
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, privacy, wiring]
depends_on: [TASK-041611]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.SweepScheduleTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.VerificationSweepTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

Expired `email_verification` rows are deleted by the one ticker `ADR-0025` already runs — a third
step in `sweepPass`, never a second coroutine — and a database failure in that step stops neither
of the other two.

## Why this exists

`ADR-0031` §3 is unusually firm: the delete *"is not optional"*, because unlike an expired
`auth_session` row — inert garbage — a row here holds an **unproven address**, personal data given
for one purpose the system has not yet been able to use it for. It is also explicit about the
mechanism: *"one statement on the existing sweep, never a second ticker."*

This is the first time `sweepPass` touches a database. That is a real change in its character and
the reason this is its own ticket: a database outage now produces a log line every sweep period,
and the existing per-step `try`/`catch`/log/continue shape is what keeps that from also stopping
room reaping.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/VerificationSweepTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/SweepScheduleTest.kt` — `shrunkServerConfig()`, the
`testApplication` shape, and `aSweepThatThrowsDoesNotStopTheNextOne`, which is the precedent for the
failure assertion here;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt`.

## Scope

- `ServerComponents` gains `val recoveryEmails: RecoveryEmails`, constructed in
  `serverComponents(...)` as `PostgresRecoveryEmails(dataSource, wallClock)` beside
  `PostgresDeviceBindings(dataSource)`. **The wall clock, not the `ServerClock`** — the same
  instrument `PostgresAuthSessions` takes, per `ADR-0062` §2.
- `scheduleSweeps` and `sweepPass` each take one more parameter, the `RecoveryEmails` port. Both
  stay `private`; the call site in `duelServer` passes `components.recoveryEmails`.
- A **third** step in `sweepPass`, after grace expiry and room reaping, in its own `try`/`catch`
  logging `"sweep: deleting expired email verifications failed"` and continuing — the identical
  shape the two existing steps use. It runs last, so a database outage cannot delay a grace-period
  expiry that decides a duel.
- The success log line, if any, carries the **count only**. No address, no player id (`ADR-0031`
  §6.4).

## Out of scope

- A second ticker, a scheduled job, or a `pg_cron` entry. `ADR-0025` decided one loop and
  `ADR-0031` §3 names it.
- Sweeping `password_reset`. `TASK-041611`'s *Out of scope* gives the reason: those rows hold no
  personal data and expiry is enforced at read time.
- Changing the sweep period or making it configurable per step. One `sweepPeriodMillis` drives all
  three.
- A metric or a health check for the sweep. `ADR-0031`'s Consequences hand the startup log line and
  the sender health check to `EPIC-07`.

## Tests

`VerificationSweepTest`, `testApplication` against `PostgresTestSupport.freshDatabase()` with the
shrunk sweep period `SweepScheduleTest` uses.

| Test | Proves |
| --- | --- |
| `anExpiredClaimIsSweptWithoutBeingAsked` | A pending row whose `expires_at` is in the past is gone within a bounded wait, with nothing calling the port directly — the ticker did it |
| `aLiveClaimSurvivesTheSweep` | A pending row an hour old is **still present** after the same wait. The negative half: a sweep with no `WHERE` passes the test above and fails this one |
| `aFailingVerificationSweepDoesNotStopRoomReaping` | With a `RecoveryEmails` whose `deleteExpiredVerifications` throws, an idle room is still reaped on the next pass. The port is stubbed, not the database, so the failure is deterministic |

## Acceptance criteria

- [ ] `VerificationSweepTest.anExpiredClaimIsSweptWithoutBeingAsked` passes
- [ ] `VerificationSweepTest.aLiveClaimSurvivesTheSweep` passes
- [ ] `VerificationSweepTest.aFailingVerificationSweepDoesNotStopRoomReaping` passes
- [ ] `SweepScheduleTest` passes **unchanged** — no assertion in it moves, because this ticket adds
      a step and changes neither existing one
- [ ] `Application.kt` gains exactly one `launch`-free step inside `sweepPass`; the file contains
      exactly one `launch` after this change, as it did before
- [ ] The new step is wrapped in its own `try`/`catch` and is the **last** of the three
- [ ] No log line added by this ticket contains an address or a player id
- [ ] Every command in `verify:` exits 0

## Proof

1. Move the new step **first** in `sweepPass` and make it throw.
   **`aFailingVerificationSweepDoesNotStopRoomReaping` reddens alone** if the `try`/`catch` is also
   removed; with the `catch` in place it stays green even when first, which is the finding to
   record: ordering is defended by the argument (a grace expiry decides a duel) and not by a test,
   and this ticket says so rather than implying a gate it does not have. Revert.
2. Remove the new step's `try`/`catch`, leaving the throw to escape into the loop body.
   **`aFailingVerificationSweepDoesNotStopRoomReaping` reddens alone**, because the idle room is
   never reaped. Both other tests use a working port and are unaffected. Revert.
3. Change `deleteExpiredVerifications`' predicate to `expires_at > now()`.
   **`anExpiredClaimIsSweptWithoutBeingAsked` reddens** (the expired row survives) **and
   `aLiveClaimSurvivesTheSweep` reddens** (the live row is deleted). Both, and that pairing is the
   point of writing the second test. Revert.
4. Delete the new step from `sweepPass` entirely but leave `ServerComponents` and the parameters in
   place. **`anExpiredClaimIsSweptWithoutBeingAsked` reddens alone.** `aLiveClaimSurvivesTheSweep`
   passes, because nothing being swept satisfies it — this is the mutation that shows the second
   test cannot stand alone, and the first cannot either. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`ServerComponents` was probed, not guessed.** Adding a required field to it, running
`./gradlew check -PrequireDocker=true` and the `web-client` job in full, propagates to **nothing**:
every construction site in the repository goes through the `serverComponents(...)` factory, so the
data class and the factory are one file between them. That is why this ticket is three files and
carries no `atomic:`.
