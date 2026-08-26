---
schema: 2
id: TASK-041626
title: Four different things happen, and the caller reads the same answer
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, http, auth, security, blocked]
depends_on: [TASK-041644]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.allFourCasesAnswerOneIdenticalTwoOhTwo' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.onlyTheVerifiedCaseMintsAToken' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.aSecondRequestInsideAQuarterHourSendsNothingAndKeepsTheLink' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.aSecondRequestAfterAQuarterHourSendsAgain' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.aMalformedBodyAnswersTwoOhTwo' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.theMailCarriesTheOwnersOwnHandle' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ForgotPasswordRouteTest.theResponseNeverCarriesTheAddress' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

> **One `--tests` per command, deliberately.** A whole-class filter exits 0 whether or not a named
> method exists, which is how `TASK-041616` shipped two criteria naming tests nobody had written.
> Multiple `--tests` patterns in **one** invocation have the same defect and it was measured rather
> than assumed: Gradle fails only when the *combined* filter matches nothing, so
> `--tests 'C.real' --tests 'C.neverWritten'` exits 0. Seven tests, seven commands.

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

**`DEC-076` — raised by a coder on this very ticket — is answered and merged too.** The first
attempt at this file blocked, correctly: `RecoveryMailer.sendPasswordReset(address, token, handle)`
needs a login handle and **nothing in this codebase could produce one**.
[`ADR-0082`](../../docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md)
answers it — the handle is read from a **proven address**, never from a player id — and
`TASK-041643` and `TASK-041644` land and gate the read, so this ticket now has a source for its
third argument. Two consequences reach this file and both are in *Scope* below: the handler calls
`resetRecipientOf` where it used to call `verifiedOwnerOf`, and **the verified fixture must
actually hold a `password` credential**, which no `insertPlayer` helper in this repository creates.

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
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` — **`resetRecipientOf`**,
which answers `ResetRecipient(playerId, handle)` for a proven address and `null` for everything
else. Not `verifiedOwnerOf`: that member still exists, still has its own caller on the attach path,
and answers no handle, so it cannot serve this endpoint;
`docs/adr/ADR-0082-a-handle-is-read-from-a-proven-address-never-from-a-player-id.md` §4, the
five-line handler ordering this file implements, and §1's *there is no `PlayerId` overload and
there must never be one*;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §1 (the reset mail is the **one**
exception to *the handle appears in no response body, ever* — the mail, not the response), §4
and §5;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` — the seam, and
what a test may await.

That is five files and it is the whole budget. **`ADR-0079` is deliberately not on it**: the one
sentence of it that binds this file is transcribed here instead — *`forgot-password`'s budget is
admitted **after** the `202`* — so nothing in it needs opening, and the budget itself is
`TASK-041628`'s.

## Scope

- `RecoveryDtos.kt` gains
  `@Serializable public data class ForgotPasswordRequest(val address: String)`, undefaulted.
- `recoveryRoutes` installs `post("/api/auth/forgot-password")`, **unauthenticated** — the caller
  cannot sign in, which is the whole point.
- **The `202` is written first**, before `resetRecipientOf`, before `issue`, before any send. That
  ordering is the timing defence and it is not an optimisation: an implementation that does the
  work and then responds passes every status assertion in this file and leaks the answer through
  latency, exactly as `ADR-0027` §6's dummy hash exists to prevent at sign-in. The extra join
  `ADR-0082` added runs after the response, so it costs the caller nothing observable.
- After responding, `ADR-0082` §4's five lines and no others: `resetRecipientOf(address)` or
  return; `newResetToken()`; `issue(recipient.playerId, token)`; and on `true`,
  `sendPasswordReset(address, token, recipient.handle)`. All over `ADR-0077`'s port — and in this
  file the port is bound to an **undecorated** recording double, never the detached decorator,
  which is what makes *nothing was sent* assertable.
- **A handle-less owner mints no token**, because the route returns before `issue` — that is
  `resetRecipientOf` answering `null` and the ordering above, not a branch to write. A token that
  can never be mailed would spend the player's fifteen-minute window and supersede a link they may
  be holding.
- **The fixture's verified owner must actually hold a `password` credential.** Every `insertPlayer`
  helper in this repository creates a player with none, and `resetRecipientOf` joins `credential`,
  so a fixture built the usual way answers `null` for a verified address and
  `onlyTheVerifiedCaseMintsAToken` reddens on a row count of **zero**. The call is
  `credentials.create(playerId, CredentialKind.PASSWORD, handle, PresentedSecret(...))`, written
  out here in full so no sixth file has to be opened for it; `recoveryRoutes` already takes a
  `Credentials`, so this file already has one in scope. This is a fixture requirement, not a
  behaviour — the handler holds no `Credentials` call of its own.
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
- **Sourcing the handle.** `resetRecipientOf` and its statement are merged by `TASK-041643` and
  gated by `TASK-041644` before this ticket starts; this handler calls the member and never reads
  `credential` itself. **Gated below**: the Files table names neither `RecoveryEmails.kt` nor
  `PostgresRecoveryEmails.kt`, so any need to touch them means the two tickets in front of this one
  left something undone — stop and report it rather than widening.
- **`Credentials.handleOf(playerId)` or any other player→handle read**, which is the cheap wrong
  answer `ADR-0082` §2 forecloses and `TASK-041642`'s gate reddens the build on. This handler holds
  a `PlayerId` and a `Credentials` at the same moment, which is exactly why the refusal is worth
  restating here. **Gated** by that merged test, not by anything in this file.
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
| `theMailCarriesTheOwnersOwnHandle` | **Two** verified addresses owned by **two** players holding **two different** handles. A request for each sends exactly one mail, and each mail's `handle` argument is that address's owner's handle. A handler that passes a constant, the wrong player's handle, or the address in the handle slot fails this and passes every other test in the file |
| `theResponseNeverCarriesTheAddress` | No body and no header value across all of the above contains the address, any substring of it longer than three characters, **or either player's handle**. `ADR-0031` §1 bars the handle from every response body and every `ServerMessage`; the reset mail is the one exception and the handler now holds a handle, so the response is worth asserting about |

## Acceptance criteria

- [ ] All seven `ForgotPasswordRouteTest` tests pass
- [ ] `allFourCasesAnswerOneIdenticalTwoOhTwo` asserts the four triples **equal to each other**, and
      the file contains no assertion of the form *each is 202 with an empty body* standing in for it
- [ ] The four cases include a **no-sender-configured** run, built by binding `NoRecoveryMailer`
- [ ] Every fixture player whose address is meant to be found holds a `password` credential, created
      before the request — a verified address alone is not enough now that the read joins `credential`
- [ ] `onlyTheVerifiedCaseMintsAToken` asserts a row count of exactly `1` **and** whose it is
- [ ] The two window tests sit at 14 and 16 minutes
- [ ] `theMailCarriesTheOwnersOwnHandle` uses **two** players whose handles are **different
      strings**, and asserts each mail against its own owner's handle — no assertion in it would
      pass if the two handles were swapped
- [ ] `theResponseNeverCarriesTheAddress` searches for **both** players' handles as well as the
      addresses
- [ ] The `202` is written **before** `resetRecipientOf` is called, checked by reading the handler
- [ ] The handler calls `resetRecipientOf` exactly once and `verifiedOwnerOf` not at all
- [ ] Every command in `verify:` exits 0

## Proof

1. Answer `404` when `resetRecipientOf` is `null`.
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
7. Pass a constant to the mailer's third argument — `sendPasswordReset(address, token, "handle")`,
   the literal `NoRecoveryMailerTest` uses.
   **`theMailCarriesTheOwnersOwnHandle` reddens alone**, on the second player, and only because the
   test uses **two** owners: with one verified fixture a constant is indistinguishable from a copy.
   Choose both fixture handles so neither is the literal `"handle"`, or this mutation survives the
   first assertion too. Revert.
8. Swap the mailer's second and third arguments — `sendPasswordReset(address, handle, token)`.
   **`theMailCarriesTheOwnersOwnHandle` reddens alone**, and the *alone* is the prediction to check:
   the two window tests assert a send **count** and the stored `token_hash`, neither of which a
   swap disturbs, so this test is the only one in the file that reads the mailer's arguments at
   all. Worth running because the compiler cannot catch it — `token` and `handle` are both `String`
   on `RecoveryMailer`, which `ADR-0082` §5 keeps byte-unchanged, and `RecoveryMailer.kt`'s KDoc
   records why no `LoginHandle` type exists. Revert.
9. Leak the handle into the response body: make the single `call.respond` read
   `call.respond(HttpStatusCode.Accepted, recoveryEmails.resetRecipientOf(address)?.handle ?: "")`.
   One edit, one `respond`, and the handler still answers before `issue`.
   **`theResponseNeverCarriesTheAddress` reddens on the handle clause, and
   `allFourCasesAnswerOneIdenticalTwoOhTwo` reddens too** — the verified case's body now differs
   from the other three, so the four triples stop matching. This is the mutation the handle clause
   was added for: `ADR-0031` §1 permits the handle in the mail and nowhere else, and before
   `ADR-0082` this handler had no handle to leak. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
