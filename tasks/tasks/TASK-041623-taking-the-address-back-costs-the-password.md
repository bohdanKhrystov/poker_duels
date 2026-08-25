---
schema: 2
id: TASK-041623
title: Taking the address back costs the password
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, auth, privacy, security]
depends_on: [TASK-041622]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DetachRecoveryEmailRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`DELETE /api/auth/recovery-email` erases the caller's address: `204` whether or not one was
attached, `401` with no session, `403` with a wrong current password.

## Why this exists

`ADR-0031`'s closing `DEC-029` note makes this endpoint the project's answer to *erase my email*:
the address lives in one column of one row, no history is kept, and *"`DELETE /api/auth/recovery-
email` is already that statement."* It also returns the address to the free namespace and reverts
the account to the opted-out risk in full.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/RecoveryDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/DetachRecoveryEmailRouteTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/http/DeviceRoutes.kt` — the session-then-password
guard order and the uniform `204`, which this endpoint copies;
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — `verifyCurrent`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §5.

## Scope

- `RecoveryDtos.kt` gains
  `@Serializable public data class DetachRecoveryEmailRequest(val currentPassword: String)`,
  undefaulted.
- `recoveryRoutes` installs `delete("/api/auth/recovery-email")` — using the `identities` and
  `credentials` parameters `TASK-041618` already declared — in this fixed order:
  1. Resolve identity. Unresolved ⇒ `401`, empty body, **before the body is read** — the same order
     `sign-up` uses, so a stranger never reaches the `403`.
  2. Decode. Any failure ⇒ `400`.
  3. `credentials.verifyCurrent(playerId, PASSWORD, presented)`. `false` ⇒ `403`.
  4. `recoveryEmails.detach(playerId)`, then `204`.
- **`204` whether or not a row existed.** A `404` for "you had none" would tell a caller with a
  stolen session whether the account has recovery configured, which is one of the two facts
  `hasRecoveryEmail` deliberately gates behind the profile read.
- The `403` is reachable only by somebody already holding a valid session, so it discloses nothing
  they could not learn from `GET /api/me`.

## Out of scope

- `POST /api/auth/recovery-email` — `TASK-041625`. This ticket's fixture attaches an address by
  calling `claimPending` + `verifyPending` directly.
- Sending a *your address was removed* mail. `ADR-0031` §6.2's port has two members and this is not
  one of them; adding a third is an ADR, not a ticket.
- Deleting the player's pending claim on a different address — `TASK-041611` fixed that and gave
  the reason.
- Any budget. `ADR-0031` §5 budgets the two `POST`s and not this.
- Installing the route, and `Application.kt` entirely. `TASK-041618` declares `recoveryRoutes` with
  its full parameter list and `TASK-041622` installs it once; this ticket fills a handler and
  changes no signature and no call site — see *Note*.

## Note on the file count

The paragraph above resolves to three files, not four, and the reason is worth stating so the
implementer does not add a fourth. `recoveryRoutes` is given **all** of its parameters by
`TASK-041618`, which declares the function with `recoveryEmails`, `identities`, `credentials` and
`passwordResets` from the start, threading `components` fields that already exist by then. Later
route tickets fill the function body and never touch its signature or its call site. If the
implementer finds themselves editing `Application.kt` here, the signature was written too narrowly
upstream and that is a defect ticket against `TASK-041618`, not a widening of this one.

## Tests

`DetachRecoveryEmailRouteTest`

| Test | Proves |
| --- | --- |
| `theRightPasswordErasesTheAddress` | With an address attached: `204`, `hasRecoveryEmail` is now `false`, and `recovery_email` holds no row for that player |
| `theAddressReturnsToTheFreeNamespace` | After the erase, a **second** player may claim and verify the same address and succeed. The point of the erase, and unobservable from the first test |
| `aWrongPasswordAnswersFourHundredAndThreeAndErasesNothing` | `403`, and `hasRecoveryEmail` is still `true` |
| `noSessionAnswersFourHundredAndOne` | No `Authorization` header: `401`, empty body, and the address survives |
| `detachingNothingStillAnswersTwoHundredAndFour` | A player who never attached: `204`, byte-identical `(status, body, header names)` to the attached player's `204` |
| `oneErasureIsNotAnothers` | Two players with two addresses; the first detaches and the second's row survives with its address intact |

## Acceptance criteria

- [ ] All six `DetachRecoveryEmailRouteTest` tests pass
- [ ] `detachingNothingStillAnswersTwoHundredAndFour` compares its triple to the **attached**
      player's `204` triple, not merely to `204`
- [ ] `oneErasureIsNotAnothers` holds **two** players in one database
- [ ] `noSessionAnswersFourHundredAndOne` sends **no** `Authorization` header and asserts the
      address survives
- [ ] The handler resolves identity **before** reading the body
- [ ] The diff touches exactly the three files in the *Files* table; `Application.kt` is unchanged
- [ ] Every command in `verify:` exits 0

## Proof

1. Move the identity check **after** the password check.
   **`noSessionAnswersFourHundredAndOne` reddens alone**, *expected 401, got 400* — with no session
   the body still decodes, and `verifyCurrent` is reached with no player id, so the route either
   `403`s or throws. Run it and record which; a throw means a `500` and is worse than the ordering
   defect it demonstrates. Revert.
2. Skip the `verifyCurrent` call and always `204`.
   **`aWrongPasswordAnswersFourHundredAndThreeAndErasesNothing` reddens alone.** This is the
   ticket's whole subject — a session alone must not be enough — and it is one deleted `if` away.
   Revert.
3. Answer `404` when `detach` finds no row, which needs `detach` to report a count it deliberately
   does not. Approximate it: answer `404` when `hasRecoveryEmail` was `false` before the delete.
   **`detachingNothingStillAnswersTwoHundredAndFour` reddens alone**, on the triple comparison. The
   oracle this endpoint refuses, and the only test that sees it. Revert.
4. Drop the `WHERE player_id = ?` from `detach`'s statement.
   **`oneErasureIsNotAnothers` reddens alone**, and `theRightPasswordErasesTheAddress` is *more*
   satisfied. Revert.
5. Have `detach` delete from `email_verification` as well — no; that is `TASK-041611`'s mutation.
   Instead have the route call `detach` on a **different** player id, e.g. one it mints.
   **`theRightPasswordErasesTheAddress`, `theAddressReturnsToTheFreeNamespace` and
   `oneErasureIsNotAnothers` all redden.** Three. Revert.
6. Leave the erase in place but have `verifiedOwnerOf` cache its answer, so the freed address is
   still reported as owned. **`theAddressReturnsToTheFreeNamespace` reddens alone**, on the second
   player's `verifyPending` returning `AddressTaken`. This is the mutation that proves the second
   test is not a restatement of the first: `hasRecoveryEmail` is already `false` and
   `recovery_email` is already empty, and only a fresh claim by somebody else notices. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
