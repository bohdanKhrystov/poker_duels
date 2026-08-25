---
schema: 2
id: TASK-041637
title: The second attach in a quarter hour is answered the same, and mails nothing
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, http, auth, mail, security]
depends_on: [TASK-041625]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.AttachRecoveryEmailRouteTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`POST /api/auth/recovery-email` sends a verification mail only when `claimPending` reports it wrote
a row, so `ADR-0031` §5's fifteen-minute rule caps verification mail per account the way it already
caps reset mail — and the response is byte-identical either way.

## Why this exists

This is the last of the three tickets `ADR-0079` §Consequences named: *"It needs `claimPending` to
answer whether it wrote, and the handler to send only then; the endpoint still answers `202` either
way, so no response, no DTO and no test in `TASK-041625` moves."* `TASK-041607` declared the
outcome, `TASK-041636` made `Suppressed` reachable, and this ticket is the four lines that make the
outcome matter.

**Nothing else caps this.** `ADR-0079` §2 is explicit that `recoveryEmailMaxAttempts` is keyed by
remote address, so a second source address buys a fresh five, and that without a per-account rule
the attach budget is *"the only cap on the mail it causes"* — 300 verification mails an hour from
one source, to mailboxes the caller chose. The rule this ticket wires up is keyed by account and
holds across every source address at once. `ADR-0079` fixes the deadline: **before `EPIC-07`
configures a sender**, which is also the day `ADR-0077`'s `NoRecoveryMailer` stops being what the
wiring holds.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecoveryRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/AttachRecoveryEmailRouteTest.kt` | modify |

Two tests are **added**; no existing test in the file changes, no assertion moves and none is
weakened. All six of `TASK-041625`'s tests observe a *first* attach by a player, which is `Claimed`
under this ticket exactly as it was before it, so none of them can see this change.

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryEmails.kt` — `ClaimPendingResult`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §5, the ***Budgets*** paragraph and the
`202`-in-every-case paragraph above it;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` — specifically
the clause fixing that **the test binds an undecorated recording double**, which is what makes *no
mail was sent* assertable with no join, no channel and no timeout.

## Scope

- `RecoveryRoutes`' `post("/api/auth/recovery-email")` handler keeps every step
  `TASK-041625` fixed, and changes exactly one thing: the send becomes conditional on the
  `ClaimPendingResult` that handler already holds.

  ```kotlin
  val claim = recoveryEmails.claimPending(playerId, address, token)
  call.respond(HttpStatusCode.Accepted)
  when (claim) {
      ClaimPendingResult.Claimed -> if (recoveryEmails.verifiedOwnerOf(address) == null) {
          mailer.sendVerification(address, token)
      }
      ClaimPendingResult.Suppressed -> Unit
  }
  ```

- **The `202` is written before the `when`, unchanged.** §5 requires the answer before any mail
  work, and the answer never depends on the outcome — an attach that mails and one that does not
  are identical in status, body and header names. `TASK-041626` calls that ordering the timing
  defence rather than an optimisation, and the same reading applies here.
- **The `when` is exhaustive over `ClaimPendingResult` and has no `else`.** An `else` costs nothing
  today and silently absorbs a third outcome the day one is added, on the one branch in this
  codebase that decides whether outbound mail leaves the building.
- The `verifiedOwnerOf` skip `TASK-041625` shipped stays exactly where it is, nested inside the
  `Claimed` arm. The two suppressors compose; neither replaces the other.
- The token passed to `sendVerification` is the same value passed to `claimPending`. On the
  `Claimed` path that is the token now stored; on the `Suppressed` path nothing is sent, which is
  the only correct thing to do with a token that was discarded.

## Out of scope

- **Telling the player anything about the suppression.** No `202` variant, no header, no body, no
  `Retry-After`. §5 answers `202` in every case and `ADR-0079` §6 refuses `429` on this endpoint by
  name; a response a caller could distinguish is the oracle the endpoint pair is built to avoid.
  **This refusal is gated** — `theResponseNeverCarriesTheAddress` covers the body and headers of
  every status, and the new suppression test asserts the response triple against the success
  triple rather than merely against `202`.
- **Logging that a send was suppressed.** `ADR-0077` fixes the log surface at one line on failure
  carrying a member name and an exception class, with **no success line** and no `player_id`; a
  suppression line would be a third kind, and §6.4 admits none. **This refusal produces no test** —
  nothing reddens if someone adds one — so it is a review criterion, named here rather than left to
  be inferred.
- **The remote-address budget — `TASK-041628`.** A second, independent limiter over the same
  endpoint, whose one line goes between steps 3 and 4 per `ADR-0079` §3. This ticket adds nothing
  at that position and must not.
- **The forgot-password path — `TASK-041626`.** `PasswordResets.issue` has answered `Boolean` for
  this rule since `TASK-041613`, and that handler's *send only if `issue` returned `true`* is its
  own scope. This ticket touches the same file, which is why the two are sequential and not
  concurrent.
- **Refactoring the two handlers' send conditions into one helper.** Two ports, two result types,
  two suppressor sets. Not ticketed anywhere, and it should not be.

## Tests

`AttachRecoveryEmailRouteTest`, two new methods on the fixture the class already builds. The
fixture's injected `java.time.Clock` must be movable between requests; `TASK-041608` already builds
`PostgresRecoveryEmails` over a mutable holder for the same reason.

| Test | Proves |
| --- | --- |
| `aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing` | One player attaches `a@x.test`, then — with the clock unmoved — attaches `b@x.test`. Both answer `202`, and the second's `(status, body, header names)` triple is compared against the **first's**, not against a bare `202`. `mailer.sent` then maps to exactly **`[a@x.test]`**: one send, and it is the *first* attach's. Asserting the list and not its size is what distinguishes this from an inverted branch, which also sends exactly once |
| `anAttachAfterAQuarterHourSendsAgain` | Attach; advance the injected clock past fifteen minutes; attach again. `202`, and `mailer.sent` maps to `[a@x.test, b@x.test]` in that order. Without this a handler that simply never called the mailer would pass the test above |

## Acceptance criteria

- [ ] `AttachRecoveryEmailRouteTest.aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing`
      passes
- [ ] `AttachRecoveryEmailRouteTest.anAttachAfterAQuarterHourSendsAgain` passes
- [ ] All six pre-existing `AttachRecoveryEmailRouteTest` tests pass **unchanged** — this ticket
      adds methods and edits none, moves no assertion and weakens none
- [ ] `aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing` asserts on the **contents** of
      `mailer.sent`, naming the first attach's address, and not on `mailer.sent.size`
- [ ] `aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing` compares its response triple to
      the first attach's triple, not merely to `202`
- [ ] `anAttachAfterAQuarterHourSendsAgain` advances the injected clock by more than fifteen minutes
- [ ] `RecoveryRoutes.kt`'s `when` over `ClaimPendingResult` contains no `else` branch
- [ ] `call.respond` for the `202` appears **before** the `when` in source order
- [ ] `RecoveryRoutes.kt` contains no log statement on the suppressed path, and none naming an
      address
- [ ] Every command in `verify:` exits 0

## Proof

1. **The mutation this ticket exists for.** Replace the `when` with an unconditional
   `mailer.sendVerification(address, token)` — that is, `TASK-041625`'s shipped handler.
   **`aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing` reddens alone**, *expected
   [a@x.test], got [a@x.test, b@x.test]*. `anAttachAfterAQuarterHourSendsAgain` still sees two sends
   and passes, and **every status, body and header assertion in the file stays green** — the wire is
   byte-identical and only the mailbox differs. **Run this one.** It is the defect `ADR-0079`
   §Consequences named, and it is invisible to all six of `TASK-041625`'s tests. Revert.
2. **Invert the branch**: send on `Suppressed`, do nothing on `Claimed`.
   **`aSecondAttachInsideAQuarterHourAnswersTheSameAndSendsNothing` reddens**, *expected [a@x.test],
   got [b@x.test]* — the first attach is `Claimed` and now sends nothing, the second is `Suppressed`
   and now sends. **The mailer still holds exactly one entry**, so a test asserting
   `assertEquals(1, mailer.sent.size)` would have passed. That is why the criterion above demands
   the contents. `anAttachAfterAQuarterHourSendsAgain` also reddens, *expected two, got zero*.
   Revert.
3. Remove the clock advance from `anAttachAfterAQuarterHourSendsAgain`.
   **That test reddens under the unmutated handler**, *expected two sends, got one*: the second
   attach falls inside the window and is suppressed. The control that proves the advance is
   load-bearing rather than decoration, and that this test is not a duplicate of the one above.
   Revert.
4. Add `else -> Unit` to the `when` and delete the `ClaimPendingResult.Suppressed` arm.
   **Nothing reddens.** The behaviour is identical today. **Record it in the PR**: the `else` is
   free until `ClaimPendingResult` grows a third value, at which point it silently routes that value
   to *do not send* — or, with the arms written the other way round, to *send*. Nothing in this
   repository would fail. This is why exhaustiveness is a review criterion here and not a test, and
   why it is written down rather than assumed.
5. Move `call.respond(HttpStatusCode.Accepted)` to after the `when`.
   **Nothing reddens.** The send is an ordinary suspend call on `ADR-0077`'s undecorated double, so
   the `202` still arrives and every assertion holds. **Record it too.** `ADR-0077` already
   established that §5's ordering *"is not gated and cannot be"* and that `TASK-041626`'s Proof
   predicted the same; this is the second endpoint where that is true, and a reader who finds the
   ordering criterion above should know no test is behind it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
