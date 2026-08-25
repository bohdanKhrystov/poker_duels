---
schema: 2
id: TASK-041625
title: Attaching an address costs the current password
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, http, auth, security, blocked]
depends_on: [TASK-041627]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AttachRecoveryEmailRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Unblocked

**Both decisions are answered and merged**, so this section is history rather than a gate.

- **`DEC-071` — the product owner's** — by
  [`ADR-0078`](../../docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md) §1, whose
  four-clause predicate `TASK-041624` builds as `emailAddressOrNull`, and whose §6 carries the
  fixture table this ticket's `400` test draws from.
- **`DEC-072` — the architect's** — by
  [`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md),
  which settles the seam, the scope, the failure semantics and — the clause two of this ticket's
  criteria needed — **what a test can await**: the test binds an *undecorated recording double*, so
  the send is an ordinary suspend call in the handler and `assertEquals(emptyList(), mailer.sent)`
  is decidable with no join, no channel and no timeout.

**Neither was the human's, and neither was about money.** `ADR-0031` §7 defers the transport — SMTP
relay or provider API, and therefore any bill — to `EPIC-07`, and this ticket sends nothing that
reaches a real mailbox under any answer.

## Goal

`POST /api/auth/recovery-email` records a pending claim for the calling player, answers `202`
whatever happens, and refuses a wrong current password with `403`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/RecoveryDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AttachRecoveryEmailRouteTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/http/DetachRecoveryEmailRouteTest.kt` — the
identity-then-password guard order and the fixture this class mirrors;
`poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddressSyntax.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and §5;
`docs/adr/ADR-0078-the-mail-is-the-only-real-check-on-an-address.md` §6's two fixture tables.

## Scope

- `RecoveryDtos.kt` gains
  `@Serializable public data class AttachRecoveryEmailRequest(val address: String, val
  currentPassword: String)`, neither field defaulted.
- `recoveryRoutes` installs `post("/api/auth/recovery-email")` in this fixed order:
  1. Resolve identity. Unresolved ⇒ `401`, **before the body is read**.
  2. Decode. Any failure ⇒ `400`.
  3. `emailAddressOrNull` ⇒ `400` on `null`.
  4. `credentials.verifyCurrent` ⇒ `403` on `false`.
  5. `recoveryEmails.claimPending(playerId, address, newVerificationToken())`, holding its
     `ClaimPendingResult`, then `202`.
  6. The send — `mailer.sendVerification(address, token)` over `ADR-0077`'s port — **skipped when
     `verifiedOwnerOf(address)` is non-null**, because §5 answers `202` even when the address
     already belongs to another player *and sends nothing in that case*. That skip is this
     ticket's: `anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` and Proof step 5 both already
     depend on it, and the step list previously left the call that performs it unnamed.
- **The send is not yet conditioned on the `ClaimPendingResult` — that is `TASK-041637`**, the next
  ticket. Hold the value, do not branch on it here, and do not discard it into `_`: the branch and
  the test that gates it arrive together one ticket later, and adding a seventh test to this file
  is what pushes this ticket past `S`.
- **`202` even when the address already belongs to another player**, sending nothing in that case.
  `ADR-0031` §5: the alternatives either tell a stranger an address is registered, or send
  unsolicited mail to a mailbox whose owner did nothing, and the second is forbidden outright by
  *recovery only*.
- **Attaching requires the current password even inside a valid session** (§3): a session token is
  a bearer credential in web storage, and without this a minute at an unattended browser converts
  into permanent ownership of the account.

## Out of scope

- **`ADR-0031` §5's fifteen-minute resend suppression on this path — `TASK-041637`.** Until it
  merges, this endpoint sends a verification mail on **every** successful attach, for ever, which
  is the defect `ADR-0079` §Consequences names against this ticket. The storage half is already in
  place by then (`TASK-041636`), so the only missing piece is the branch. `ADR-0079` fixes the
  deadline — before `EPIC-07` configures a sender — and `TASK-041637` is the next ticket in the
  chain. **Say so in the PR.**
- The budget — `TASK-041628`, unblocked by
  [`ADR-0079`](../../docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md)
  §2 and §3: five per rolling sixty seconds, keyed by `origin.remoteAddress`, admitted **after**
  step 3 and **before** step 4, over budget answering `202`. That is one line in a place the ADR
  names, and it is `TASK-041628`'s line, not this ticket's. Until it lands this endpoint is
  unbudgeted, and `ADR-0055`'s condition applies: **no deployment may expose it without one.** Say
  so in the PR.
- `DELETE /api/auth/recovery-email` — `TASK-041623`, already merged by the time this runs.
- `verify-email`, which consumes the token this endpoint mints — `TASK-041618`.
- Returning the address, a masked form of it, or anything about it. §6.3: it is in no response
  body, no `ServerMessage` and no log line.
- Telling the player an address is taken. There is no such response.

## Tests

`AttachRecoveryEmailRouteTest`

| Test | Proves |
| --- | --- |
| `theRightPasswordRecordsAPendingClaim` | `202`, an empty body, and `email_verification` holds one row for that player with the address **as typed**. `hasRecoveryEmail` is still `false` — nothing is attached until it is proven |
| `anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` | A second player attaches an address the first has already verified: `202`, byte-identical `(status, body, header names)` to the test above, and **no mail is sent** — asserted through `ADR-0077`'s undecorated recording double |
| `aWrongPasswordAnswersFourHundredAndThreeAndRecordsNothing` | `403`, and `email_verification` holds no row for that player |
| `noSessionAnswersFourHundredAndOne` | No `Authorization` header: `401`, and no row is recorded |
| `aStringThatIsNotAnAddressAnswersFourHundred` | Using a refused form from `ADR-0078` §6's refused table: `400`, and no row is recorded |
| `theResponseNeverCarriesTheAddress` | Across the `202`, `400`, `401` and `403` responses, no body and no header value contains the address or any substring of it longer than three characters |

## Acceptance criteria

- [ ] `ADR-0078` and `ADR-0077` are merged — both are, as of 2026-08-25; the `blocked` label in the
      front matter is a historical marker and this ticket's `status:` is not `blocked`
- [ ] All six `AttachRecoveryEmailRouteTest` tests pass
- [ ] The handler skips the send when `verifiedOwnerOf(address)` is non-null, and
      `anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` asserts through `ADR-0077`'s recording
      double that `mailer.sent` is empty for that request
- [ ] The handler binds `claimPending`'s `ClaimPendingResult` to a named value and does not discard
      it — `TASK-041637` branches on it
- [ ] `anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` compares its triple to the success
      triple, not merely to `202`
- [ ] `theRightPasswordRecordsAPendingClaim` asserts `hasRecoveryEmail` is still `false`
- [ ] `theResponseNeverCarriesTheAddress` covers **all four** status codes this endpoint produces
- [ ] The handler resolves identity **before** reading the body
- [ ] `RecoveryRoutes.kt` contains no log statement naming an address, and no
      `EmailAddress(...).value` inside a string template
- [ ] Every command in `verify:` exits 0

## Proof

1. Skip the `verifyCurrent` call and always proceed.
   **`aWrongPasswordAnswersFourHundredAndThreeAndRecordsNothing` reddens alone.** This is §3's
   entire subject and one deleted `if` away. Revert.
2. Answer `409` when `verifiedOwnerOf(address)` is non-null.
   **`anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` reddens alone**, on the triple. The
   oracle §5 forbids, and it is the edit a reader who has just built `verify-email`'s `409` makes by
   analogy. Revert.
3. Move the identity check after the address check.
   **`noSessionAnswersFourHundredAndOne` reddens alone**, *expected 401, got 400* — a stranger now
   learns the address rule without an account. Revert.
4. Include the address in the `400` body: `call.respond(BadRequest, request.address)`.
   **`theResponseNeverCarriesTheAddress` reddens alone.** Revert.
5. Send the verification mail on the already-taken path as well.
   **`anAddressAlreadyProvenElsewhereStillAnswersTwoOhTwo` reddens alone**, on its no-mail
   assertion, while the triple still matches — the wire is identical and only the mailbox differs.
   Run this one: it is the mutation that makes this server a relay pointed at a stranger's inbox,
   and it is invisible to every assertion about status codes.
6. Store `emailAddressOrNull(raw)` folded with `.lowercase()`.
   **`theRightPasswordRecordsAPendingClaim` reddens alone**, on the as-typed assertion. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
