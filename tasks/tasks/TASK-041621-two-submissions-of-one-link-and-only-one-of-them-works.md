---
schema: 2
id: TASK-041621
title: Two submissions of one link, and only one of them works
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, auth, security, test]
depends_on: [TASK-041620]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordIsSingleUseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

A reset token works exactly once **at the wire**, proven with two concurrent submissions as well as
two sequential ones — the story's criterion, asserted through HTTP rather than through the port.

## Why this exists

`TASK-041614` proves single use at the port, over `PostgresPasswordResets.consume`. That is the
mechanism; this is the contract. The two differ in a way that matters: a route is free to retry, to
wrap the call in its own transaction, or to catch an exception and call again, and none of those
would fail a port-level test. `STORY-0416`'s acceptance criterion is deliberately worded *"asserted
with two concurrent uses as well as two sequential ones"*, and concurrency at the wire is the half
that catches a route-level retry.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordIsSingleUseTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordRouteTest.kt` — the
`testApplication` fixture and the `recoveryRoutes(...)` install this class reuses;
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreConcurrencyTest.kt` — the
latch-and-two-threads shape;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- One new test class. **No production change.**
- The concurrent test issues one token, then fires **two** `POST`s from two threads released by one
  `CountDownLatch`, carrying **two different new passwords**. Exactly one answers `204` and the
  other `400`, and the password that ends up stored is the winner's.
- Two different passwords is the whole design: with one password, both submissions succeeding would
  leave a database indistinguishable from one submission succeeding, and the test would pass while
  the property was false.
- The sequential test asserts the second `400`'s `(status, body, header names)` triple equals the
  triple a **never-issued** token produces — a spent token and an invented one are one answer, per
  §5's three-indistinguishable-cases rule applied to this endpoint.

## Out of scope

- The port-level proof — `TASK-041614`, which stays as it is and is not weakened.
- Any change to `RecoveryRoutes.kt` or `PostgresPasswordResets.kt`. If the concurrent test fails,
  that is a defect ticket against one of them, not a relaxed assertion here.
- Expiry — `TASK-041622` asserts it alongside the session sweep, on the same fixture.
- Load or performance. Two threads is enough to observe the property; this is not a stress test and
  must not grow a loop count that makes CI slow.

## Tests

`ResetPasswordIsSingleUseTest`

| Test | Proves |
| --- | --- |
| `theSecondSubmissionOfOneLinkIsRefused` | Two sequential `POST`s with one token: `204` then `400`. The second `400`'s triple equals a never-issued token's triple |
| `twoSimultaneousSubmissionsYieldExactlyOneSuccess` | Two threads, one latch, one token, **two different new passwords**. Exactly one `204` and one `400`; the stored password matches exactly one of the two, and it is the one whose request answered `204` |
| `theLoserOfTheRaceChangedNothing` | In the same run, the losing request's password does **not** verify. The count assertion above says one succeeded; this says the other did not partially succeed — a route that wrote the credential and then failed to delete the row would satisfy the first and fail this |

## Acceptance criteria

- [ ] All three `ResetPasswordIsSingleUseTest` tests pass
- [ ] `twoSimultaneousSubmissionsYieldExactlyOneSuccess` uses **two different** new passwords and
      correlates the stored one to the request that answered `204`
- [ ] `theSecondSubmissionOfOneLinkIsRefused` compares the spent token's triple to a **never-issued**
      token's triple, rather than asserting each is `400` separately
- [ ] The concurrent test releases both threads from one latch and joins both before asserting
- [ ] No file under `src/main` changes
- [ ] The test class contains no loop whose count exceeds 2
- [ ] Every command in `verify:` exits 0

## Proof

1. In `PostgresPasswordResets.consume`, replace the `DELETE … RETURNING` with a `SELECT` followed by
   a `DELETE` after the credential update.
   **`twoSimultaneousSubmissionsYieldExactlyOneSuccess` reddens**, with two `204`s, **and
   `theLoserOfTheRaceChangedNothing` reddens**, since the loser's password now verifies.
   `theSecondSubmissionOfOneLinkIsRefused` runs sequentially and **stays green** — the row is gone
   before the second call reads. That green is the finding: sequential single-use is a weaker
   property, and a suite holding only it would have shipped this defect. Run this mutation; if the
   concurrent test passes anyway, the latch is not releasing both threads together and the test is
   worthless until it does.
2. Wrap the route's `consume` call in a `repeat(2)` retry that calls again on `false`.
   **`theSecondSubmissionOfOneLinkIsRefused` reddens**, because the first request now spends the
   token and reports success, and the second still gets `400` — so actually it **stays green**.
   Correct prediction: **nothing reddens.** Record it. A same-request retry is invisible to
   single-use assertions because the second attempt fails harmlessly; what a retry would break is
   `TASK-041622`'s session sweep count, and this ticket does not gate it. Revert.
3. Have the route respond `400` on a `consume` exception rather than propagating.
   **Nothing reddens** — no path throws. Record it, and note that this is why the *Out of scope*
   above forbids softening: the route's exception behaviour is ungated by this file.
4. Give both concurrent requests the same new password.
   **`theLoserOfTheRaceChangedNothing` reddens** — the loser's password is the winner's and
   verifies. This is the mutation on the *test*, and running it is what proves the two-password
   design is load-bearing rather than decorative. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
