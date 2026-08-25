---
schema: 2
id: TASK-041626
title: Four different things happen, and the caller reads the same answer
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, http, auth, security, blocked]
depends_on: [TASK-041637]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Unblocked

**`DEC-072` — the architect's — is answered and merged**, so this section is history rather than a
gate. The `blocked` label in the front matter is a historical marker and this ticket's `status:` is
not `blocked`.

[`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
settles the seam, the scope, the failure semantics — and the clause three of this ticket's tests
needed, **what a test can await**: the test binds an *undecorated recording double*, so the send is
an ordinary suspend call inside the handler and both `assertEquals(1, mailer.sent.size)` and
`assertEquals(emptyList(), mailer.sent)` are decidable with no join, no channel and no timeout.
**Absence is what forced that shape** — no await proves a negative. No sender configured is
`NoRecoveryMailer`, a `public object` with two empty bodies and never a null, which is what the
fourth case below binds.

**Not the human's and not about money.** `ADR-0031` §7 defers the transport to `EPIC-07`; this
endpoint answers `202` identically under an answer that ships no sender at all.

## Goal

`POST /api/auth/forgot-password` answers `202` in every case, with an identical body, and mints a
reset token only for a verified address that has not been mailed in the last fifteen minutes.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/RecoveryDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ForgotPasswordRouteTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/PasswordResets.kt` — `issue` already returns
the `Boolean` that says whether to send;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` — `verifiedOwnerOf`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4 and §5;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` — the seam, and
what a test may await;
`docs/adr/ADR-0079-five-to-attach-ten-to-forget-and-the-attach-budget-is-the-only-mail-cap.md`, for
the one sentence that binds this file: `forgot-password`'s budget is admitted **after** the `202`.

## Scope

- `RecoveryDtos.kt` gains
  `@Serializable public data class ForgotPasswordRequest(val address: String)`, undefaulted.
- `recoveryRoutes` installs `post("/api/auth/forgot-password")`, **unauthenticated** — the caller
  cannot sign in, which is the whole point.
- **The `202` is written first**, before `verifiedOwnerOf`, before `issue`, before any send. That
  ordering is the timing defence and it is not an optimisation: an implementation that does the
  work and then responds passes every status assertion in this file and leaks the answer through
  latency, exactly as `ADR-0027` §6's dummy hash exists to prevent at sign-in.
- After responding: resolve the owner; if there is one, `issue`; if `issue` returned `true`, send.
  All over `ADR-0077`'s port — and in this file the port is bound to an **undecorated** recording
  double, never the detached decorator, which is what makes *nothing was sent* assertable.
- A malformed body is **also `202`**, not `400`. §5 says `202` always and lists no exception; a
  `400` here would distinguish a well-formed unknown address from a malformed one, which is a
  weaker oracle than the one being refused but is still one, and there is no reason to spend it.
  **This is a reading of §5 rather than a quotation of it** — say so in the PR, and if a reviewer
  reads §5 as permitting `400`, that is a `DEC-NNN`.

## Out of scope

- The budget — `TASK-041628`. Over budget answers `202` like everything else, so adding it later
  moves no assertion in this file; `ADR-0079` puts its `admit` **after** the `202` this ticket
  writes, which is why that ordering is stated in *Scope* as a property and not as an optimisation.
- `reset-password`, which spends the token — `TASK-041620`.
- The mail's wording and the `<baseUrl>/#/reset/<token>` link (`ADR-0081` §1) — `TASK-041633` builds
  the link, and `ADR-0031` defers the copy to `STORY-0412`.
- Telling anybody anything. There is no failure response here and no success response either.

## Tests

`ForgotPasswordRouteTest`

| Test | Proves |
| --- | --- |
| `allFourCasesAnswerOneIdenticalTwoOhTwo` | Four requests — an address nobody has mentioned; one that is pending but unverified; one that is verified; and one from a caller with **no sender configured** — produce four byte-identical `(status, body, header names)` triples, and the status is `202` |
| `onlyTheVerifiedCaseMintsAToken` | Across the same four requests, exactly **one** `password_reset` row exists afterwards, and it belongs to the verified address's owner. The positive control: without it, an endpoint that did nothing at all passes the test above |
| `aSecondRequestInsideAQuarterHourSendsNothingAndKeepsTheLink` | Two requests 14 minutes apart on the injected clock: both `202`, exactly one send, and the stored `token_hash` is still the **first** one's — the outstanding link the player is about to click survives |
| `aSecondRequestAfterAQuarterHourSendsAgain` | 16 minutes apart: both `202`, **two** sends, and the stored hash is the second one's. The boundary from the other side |
| `aMalformedBodyAnswersTwoOhTwo` | An empty body and `{}` both produce the identical triple, and mint nothing |
| `theResponseNeverCarriesTheAddress` | No body and no header value across all of the above contains the address or any substring of it longer than three characters |

## Acceptance criteria

- [ ] All six `ForgotPasswordRouteTest` tests pass
- [ ] `allFourCasesAnswerOneIdenticalTwoOhTwo` asserts the four triples **equal to each other**, and
      the file contains no assertion of the form *each is 202 with an empty body* standing in for it
- [ ] The four cases include a **no-sender-configured** run, built by binding `NoRecoveryMailer`
- [ ] `onlyTheVerifiedCaseMintsAToken` asserts a row count of exactly `1` **and** whose it is
- [ ] The two window tests sit at 14 and 16 minutes
- [ ] The `202` is written **before** `verifiedOwnerOf` is called, checked by reading the handler
- [ ] Every command in `verify:` exits 0

## Proof

1. Answer `404` when `verifiedOwnerOf` is `null`.
   **`allFourCasesAnswerOneIdenticalTwoOhTwo` and `aMalformedBodyAnswersTwoOhTwo` both redden.**
   The single most valuable mutation in this file: it is the helpful error message §5 exists to
   forbid, and it is what almost every other product does. Revert.
2. Send on the **pending** case too, by having the route fall back to `email_verification`.
   **`onlyTheVerifiedCaseMintsAToken` reddens alone**, *expected 1 row, got 2*, while all four
   triples still match. A reset for an unproven mailbox, invisible to every status assertion.
   Revert.
3. Move the `call.respond` to the **end** of the handler, after the send.
   **Nothing reddens.** Record it, and record it prominently: the timing defence §5 specifies is
   **not** gated by this suite, and cannot be without a flaky latency assertion. The criterion above
   is a review criterion for that reason, and the PR must say the reviewer read the handler. This
   repository has precedent for naming an untestable criterion rather than manufacturing a gate.
4. Change `issue`'s suppression window to 30 minutes.
   **`aSecondRequestAfterAQuarterHourSendsAgain` reddens alone**, on the send count. Revert.
5. Invalidate the outstanding token inside the window — have the route call `issue` with a fresh
   token unconditionally. **`aSecondRequestInsideAQuarterHourSendsNothingAndKeepsTheLink` reddens**
   on the stored-hash assertion, and on the send count if the route then sends. The double-click
   that destroys the link the player is about to use. Revert.
6. Answer `400` for a malformed body.
   **`aMalformedBodyAnswersTwoOhTwo` reddens alone.** Run it, and note in the PR that this is the
   one behaviour in this ticket that is a *reading* of §5 rather than a quotation of it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
