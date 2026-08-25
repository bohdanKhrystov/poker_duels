---
schema: 2
id: TASK-041622
title: A reset signs you out everywhere, including here
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, http, auth, security]
depends_on: [TASK-041621]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ResetPasswordEndsSessionsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.DuelServerRoutesTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

A successful reset leaves the player with no live session anywhere, asserted at the wire — and
`recoveryRoutes` is installed on the real server, so both of its endpoints answer for a
running application rather than only inside `testApplication`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/Application.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ResetPasswordEndsSessionsTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/DuelServerRoutesTest.kt` — the assertion that every
registered route answers, which this ticket's install must not break;
`docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` — the contrast this ticket
must **not** copy;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- `Application.kt`'s `duelServer` gains one line beside the four existing installs:
  `recoveryRoutes(components.recoveryEmails, components.passwordResets)`. `ServerComponents` gains
  `passwordResets` in the same edit if `TASK-041613` did not already add it.
- `ResetPasswordEndsSessionsTest`, asserting the sweep through HTTP: the resetting player's sessions
  stop working, a second player's does not, and the reset issues nothing to replace them.
- **The contrast with `ADR-0050` is the substance and belongs in the test names.**
  `DELETE /api/me/device` deliberately spares the caller's own session — *"everywhere except here"*.
  A reset spares **nothing**, because the reset endpoint requires no session at all, so there is no
  "here" to spare, and the usual reason to reset is that somebody else has the password. A future
  reader who half-remembers `ADR-0050` will try to add the exclusion; the test is what stops them.

## Out of scope

- Changing `ADR-0050`'s `DELETE /api/me/device` behaviour or its tests. The two paths differ on
  purpose and this ticket touches neither.
- The `422` — `TASK-041629`. `ADR-0080` §1 puts the policy check in **front** of `consume`, so
  nothing on this ticket's path moves when it lands and no test here has to know it is coming.
- `POST /api/auth/recovery-email` and `POST /api/auth/forgot-password`, which are not yet installed
  because they are not yet written — `TASK-041625` and `TASK-041626` add them to the same call.
- Closing live WebSockets. `AuthRoutes`' sign-out already declines to, on the grounds that tearing
  a socket down abandons a seat mid-duel and `ADR-0013`'s grace period then folds it — an
  authentication operation that costs a coin. A reset inherits that reasoning unchanged.

## Tests

`ResetPasswordEndsSessionsTest`

| Test | Proves |
| --- | --- |
| `aResetEndsEverySessionThePlayerHeld` | The player signs in twice, holding **two** tokens. After a successful reset, a request authenticated with **each** token is refused. Two tokens, because deleting only the newest row passes a one-token test |
| `aResetEndsNobodyElsesSession` | A second player's token still works after the first player's reset. Guards a `DELETE FROM auth_session` with no `WHERE` |
| `aResetHandsBackNoReplacement` | The `204`'s body is empty, it carries no `Set-Cookie`, and the response text contains no substring of either deleted token — so the endpoint is not quietly re-issuing one |
| `theNewPasswordSignsInAfterwards` | `POST /api/auth/sign-in` with the new password answers `200` and yields a **fresh** token that differs from both deleted ones. The positive control: without it, a reset that deleted every session and also broke the credential passes the three tests above |
| `everyRecoveryRouteAnswersOnTheRealServer` | Against `duelServer`, `POST /api/auth/verify-email` and `POST /api/auth/reset-password` answer `400` for a fabricated token rather than `404`. The install is real, not `testApplication`-only |

## Acceptance criteria

- [ ] All five `ResetPasswordEndsSessionsTest` tests pass
- [ ] `DuelServerRoutesTest` passes **unchanged** — the new install adds routes and moves no
      existing assertion
- [ ] `aResetEndsEverySessionThePlayerHeld` holds **two** tokens for the resetting player and
      asserts both are refused
- [ ] `aResetEndsNobodyElsesSession` holds a second player whose token still works
- [ ] `everyRecoveryRouteAnswersOnTheRealServer` asserts `400`, and explicitly asserts the status is
      not `404`
- [ ] `Application.kt` gains exactly one `recoveryRoutes(...)` call
- [ ] Neither `AuthRoutes.kt` nor `DeviceRoutes.kt` changes
- [ ] Every command in `verify:` exits 0

## Proof

1. In `PostgresPasswordResets.consume`, exclude one session from the delete —
   `AND token_hash <> ?` bound to the newest row, the shape `ADR-0050` uses.
   **`aResetEndsEverySessionThePlayerHeld` reddens alone**, on the surviving token.
   `aResetEndsNobodyElsesSession` is about a different player and passes; `theNewPasswordSignsIn
   Afterwards` passes. This is the exact edit a reader half-remembering `ADR-0050` makes, and this
   test is the only thing in the repository that catches it. Revert.
2. Delete only the most recent session — `... AND token_hash = (SELECT token_hash ... ORDER BY
   issued_at DESC LIMIT 1)`. **`aResetEndsEverySessionThePlayerHeld` reddens alone**, on the older
   token. A one-token fixture would have passed, which is why the criterion demands two. Revert.
3. Drop the `WHERE player_id = ?` from the session delete.
   **`aResetEndsNobodyElsesSession` reddens alone.** The other player's token is gone, while every
   assertion about the resetting player is *more* satisfied. Revert.
4. Remove the `recoveryRoutes(...)` line from `Application.kt`.
   **`everyRecoveryRouteAnswersOnTheRealServer` reddens alone**, *expected 400, got 404*. Every
   other test in this file installs the routes through its own `testApplication` block and stays
   green — which is precisely the blind spot this fifth test exists to close, and why it asserts
   *not 404* rather than merely *400*. Revert.
5. Make the reset respond `204` with header `Set-Cookie: sessionToken=<a fresh token>`.
   **`aResetHandsBackNoReplacement` reddens alone.** Revert.
6. Break the credential write: `UPDATE credential SET secret_hash = 'not-a-phc-string' ...`.
   **`theNewPasswordSignsInAfterwards` reddens alone**; the three session tests pass, because the
   sessions really are gone. The positive control that stops "delete everything" reading as
   success. Revert.

## Notes

- The Goal originally said `recoveryRoutes` answers for "all three" endpoints; it installs two —
  `/api/auth/verify-email` and `/api/auth/reset-password` — matching the Tests table and the
  Out-of-scope note that the other two recovery endpoints are not yet written. Corrected before
  landing so a reader comparing Goal to Tests table does not conclude the Tests table is the one
  that is wrong.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

**The coder blocked, and found two defects in this ticket rather than one.** `recoveryRoutes` needs a
`passwordResets` argument that `ServerComponents` did not have — `TASK-041613` and `TASK-041614` added
`PasswordResets` and its Postgres implementation without ever wiring them into the composition — so the
sole declared `Application.kt` edit could not compile inside `files_touched: 2`. This ticket's own Scope
had anticipated it (*"`ServerComponents` gains `passwordResets` in the same edit if `TASK-041613` did
not already add it"*), so the prose authorised what the Files table forbade. Three files is inside
`MAX_FILES_TOUCHED`, so the fix was a row and a number, not an `atomic:` block. It also caught the Goal
claiming *all three* endpoints when `recoveryRoutes` installs **two** — the Tests table and Out of scope
both already said two.

**`assertAll` over sequential assertions is a real difference, and the reviewer measured it.** With
`recoveryRoutes(...)` removed from the composition, the shipped test reports `Multiple Failures (2
failures)` naming both endpoints. The reviewer rewrote the same block as two sequential `assertEquals`
under the identical mutation and got **one** failure — verify-email only — silently hiding that
reset-password was equally unwired. Knowing one endpoint is missing and knowing both are is the
difference between a five-minute fix and a wrong one.

**Two sessions, and the mutations catch both directions.** Excluding the newest session reddens
`aResetEndsEverySessionThePlayerHeld` on the *second* token; deleting only the newest reddens the same
test on the *first*. One session could not tell *deleted the one you used* from *deleted all of them*.
A second player's token is asserted still live, and dropping `WHERE player_id` reddens that test alone.

**Liveness is proven by a refused request, never a table read** — `GET /api/me` with a Bearer token,
`401` against `200`. That is what the ticket asked for, and it is the difference between the row being
gone and the session being dead.

**The asymmetry worth knowing:** removing the whole `recoveryRoutes(...)` line from `Application.kt`
reddens exactly one test, while removing just the reset-password handler from inside it reddens all
five — every test in the file performs a real reset as a precondition.
