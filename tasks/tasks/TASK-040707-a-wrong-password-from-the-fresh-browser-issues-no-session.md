---
schema: 2
id: TASK-040707
title: A wrong password from the fresh browser issues no session
type: task
status: ready
parent: STORY-0407
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, auth]
depends_on: [TASK-040706]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A sign-in from the fresh browser with the right handle and the **wrong** password answers `401` and
writes no `auth_session` row — the story's fourth acceptance criterion.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/AuthRoutes.kt` (the sign-in ordering its KDoc
sets out),
`docs/adr/ADR-0074-sign-in-is-ten-wrong-passwords-a-minute-reserved-before-the-hash.md` §2.

## Scope

- One reading inserted **upstream**, immediately before the sign-in step `TASK-040704` added:
  `sessionsBeforeAnySignIn = dataSource.authSessionRowCount()`. It is a read and perturbs nothing, so
  `TASK-040706`'s bracket, which is already open at that point, is unaffected. It exists because
  without it every count this ticket asserts is `1`, and a helper that returned a constant `1` would
  pass — see the third test below.
- One step appended to `runRecovery()`, **after `TASK-040706`'s bracket has closed** and after its two
  binding counts are taken, so nothing here can perturb them:
  1. `sessionsBeforeWrongPassword = dataSource.authSessionRowCount()`
  2. `wrongPasswordStatus = client.signInStatus(RECOVERY_HANDLE, "not-$RECOVERY_PASSWORD")` — the
     **real handle** with a wrong password, never a handle that does not exist. A nonexistent handle
     takes `PostgresCredentials`' dummy-hash path and is a different branch; the story's claim is
     about a player who is on a new device and mistypes.
  3. `sessionsAfterWrongPassword = dataSource.authSessionRowCount()`
- `dataSource.assertCoinInvariantHolds(...)` after the attempt, with a new distinct step string.
  `runRecovery()` then holds eight calls in total.
- Two private helpers, both at the bottom of the file:

  ```kotlin
  private suspend fun HttpClient.signInStatus(handle: String, password: String): HttpStatusCode
  private fun DataSource.authSessionRowCount(): Int
  ```

  `signInStatus` posts the same body `signIn` does and returns the status **without asserting it** —
  `signIn` asserts `200` and cannot express a refusal, so this is a second helper rather than a
  change to the first, and `signIn` keeps its shape for the successful path.
  `authSessionRowCount` runs `SELECT count(*) FROM auth_session`.
- Four new `RecoveryRecord` fields with `@property` lines: `sessionsBeforeAnySignIn`,
  `wrongPasswordStatus`, `sessionsBeforeWrongPassword`, `sessionsAfterWrongPassword`.

## Out of scope

- The rate limiter. `ADR-0074` allows ten wrong passwords a minute per address and refunds a
  successful one, so this arc's single successful sign-in plus single wrong password sits far under
  the budget; a test that exercised the limit would be `STORY-0405`'s, and it already exists.
- A `429`, and any assertion that distinguishes an over-budget refusal from a wrong password.
  `ADR-0074` §4 makes them deliberately identical on the wire.
- A nonexistent handle, a malformed body, and the timing-oracle argument behind them. `STORY-0405`
  owns those and gates them at the route.
- Any file under `poker-server/src/main`.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `aWrongPasswordFromTheFreshBrowserIsRefused` | `wrongPasswordStatus` is `401 Unauthorized` |
| `theRefusedSignInAddedNoSessionRow` | `sessionsBeforeWrongPassword` is `1` and `sessionsAfterWrongPassword` is `1`. A count, not a boolean: a status-only test cannot tell a refusal apart from one that issued a token and forgot to return it |
| `theCountSeesASessionRowAppear` | `sessionsBeforeAnySignIn` is `0` and `sessionsBeforeWrongPassword` is `1`. **Two inputs, two different expected values, through the same helper.** Without it, every count this ticket asserts is `1` and an `authSessionRowCount` that returned a constant `1` would satisfy the test above for free |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.aWrongPasswordFromTheFreshBrowserIsRefused` passes
- [ ] `RecoveryOnAFreshBrowserTest.theRefusedSignInAddedNoSessionRow` passes
- [ ] `RecoveryOnAFreshBrowserTest.theCountSeesASessionRowAppear` passes
- [ ] `theRefusedSignInAddedNoSessionRow` asserts `1` before the attempt and `1` after it, from two
      separate `authSessionRowCount` calls
- [ ] `theCountSeesASessionRowAppear` asserts `0` and `1`, so the same helper is shown answering two
      different values
- [ ] `sessionsBeforeAnySignIn` is read before the sign-in step, and nothing else is inserted there
- [ ] The refused attempt uses `RECOVERY_HANDLE` — the handle that exists — and a password that is
      not `RECOVERY_PASSWORD`
- [ ] The step is appended after the snapshots and counts `TASK-040706` added, and `TASK-040706`'s
      three methods still pass with their assertions unchanged
- [ ] `signIn` is unchanged and still asserts `200`
- [ ] `runRecovery()` contains exactly eight calls to `assertCoinInvariantHolds`, with eight
      different step strings
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt`, make `answerFor`
accept any secret: replace its last line
`return if (hasher.matches(presented, secretHash)) PlayerId(row.playerId) else null` with
`return PlayerId(row.playerId)`.

**`aWrongPasswordFromTheFreshBrowserIsRefused` and `theRefusedSignInAddedNoSessionRow` redden, and no
other method in this class does.** Trace it: the wrong password now verifies, so sign-in reaches
`sessions.issue` and answers `200` — the first fails on the status, and a second `auth_session` row
lands, so the second fails with *expected 1, got 2*. `theCountSeesASessionRowAppear` reads two counts
taken **before** the wrong-password attempt and stays green. Everything earlier is untouched: the
successful sign-in already succeeded, the snapshots and counts were taken before this step, and
`auth_session` is not one of the two tables `TASK-040706` snapshots. Revert.

A second mutation, reddening exactly one. In `AuthRoutes.kt`'s sign-in, change the failed-verify
branch from `call.respond(HttpStatusCode.Unauthorized)` to `call.respond(HttpStatusCode.Forbidden)` —
the one immediately after `credentials.verify`, not the one after `loginHandleOrNull` and not the one
after `signInBudget.admit`. **`aWrongPasswordFromTheFreshBrowserIsRefused` reddens alone**: no session
is issued either way, so both counts are still `1` and the other two methods stay green. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why the count is the assertion that carries this.** A status-only test cannot tell a `401` that
refused from a `401` that refused *after* issuing a token it forgot to return — a shape that sounds
absurd until an early return is deleted during a refactor. The row count sees it; the status does not.
