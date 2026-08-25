---
schema: 2
id: TASK-041606
title: A port that can send exactly two mails
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, mail, security, invariant]
depends_on: [TASK-041605]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.RecoveryMailerShapeTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`RecoveryMailer` exists with exactly two functions, both named for the only two permitted mails,
and a test fails the build the moment a third member appears — so "recovery only" is enforced by a
shape rather than by a sentence somebody has to remember.

## Why this exists

`ADR-0031` §6.2 is explicit that this is a **mechanism**, not documentation: *"Adding a newsletter
therefore means adding a member to a port called `RecoveryMailer`, in a diff a reviewer reads. A
test asserts the interface declares exactly these two members, structurally over the public API."*
The whole promise the human made when choosing this option — *"Never used for contact or
marketing"* — is a schema property (`TASK-041601`) plus this.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryMailer.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RecoveryMailerShapeTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/Credentials.kt` — the port KDoc house style;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt`;
`poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddress.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §6.2.

## Scope

- `public interface RecoveryMailer` in `duels.poker.server.auth`, with exactly:

  ```kotlin
  public suspend fun sendVerification(address: EmailAddress, token: VerificationToken)
  public suspend fun sendPasswordReset(address: EmailAddress, token: ResetToken, handle: String)
  ```

- `RecoveryMailerShapeTest`, `internal`, asserting the member set **reflectively over
  `RecoveryMailer::class`**, comparing `declaredMemberFunctions.map { it.name }.toSet()` against
  `setOf("sendVerification", "sendPasswordReset")` — a set of names, never a count, so the failure
  message names the offending member.
- The KDoc says outright that there is no `send(to, subject, body)` in this codebase and that a
  third member is a decision requiring an ADR that supersedes `ADR-0031`.

## Out of scope

- Any implementation, real or fake. What the wiring holds when no sender is configured is
  `DEC-072`, and `TASK-041627` builds it. A no-op implementation added here would answer half of
  that decision inside a ticket.
- The subject lines, the bodies, and the `<baseUrl>/reset#token=…` link. The link's `baseUrl` is
  part of `DEC-072`; the wording of both mails is `ADR-0031`'s explicit deferral to `STORY-0412`.
- A `LoginHandle` type. `ADR-0031` §6.2's snippet writes `handle: LoginHandle`, and no such type
  exists: `loginHandleOrNull` returns `String?` and `Credentials` takes `identifier: String`. The
  parameter is therefore `String`, and this divergence from an **illustrative** snippet is recorded
  here rather than resolved silently. The mechanism §6.2 specifies is the two member *names*, which
  are transcribed exactly.

## Tests

`RecoveryMailerShapeTest`

| Test | Proves |
| --- | --- |
| `theMailerDeclaresExactlyTwoMembers` | `RecoveryMailer::class.declaredMemberFunctions.map { it.name }.toSet()` equals `setOf("sendVerification", "sendPasswordReset")`. A third member — `sendNewsletter`, `sendDigest`, or a generic `send` — fails the build |
| `theReflectionSeesTheMembersItClaimsTo` | The same reflection over a **second** interface declared privately in the test file with three known members returns those three names. The positive control: a helper that returned a hard-coded set, or one reading the wrong `KClass`, passes the test above and fails this one |
| `neitherMailFunctionReturnsAnything` | Both members' `returnType.classifier` is `Unit::class`. A mailer that returned a delivery receipt would put a send outcome in a caller's hands, and `ADR-0031` §5 requires `forgot-password` to answer identically whether or not anything was sent |

## Acceptance criteria

- [ ] `RecoveryMailerShapeTest.theMailerDeclaresExactlyTwoMembers` passes
- [ ] `RecoveryMailerShapeTest.theReflectionSeesTheMembersItClaimsTo` passes
- [ ] `RecoveryMailerShapeTest.neitherMailFunctionReturnsAnything` passes
- [ ] The member assertion compares **sets of names**, and the file contains no assertion on
      `declaredMemberFunctions.size` alone
- [ ] The control interface is declared inside `RecoveryMailerShapeTest.kt` and has **three**
      members, so its expected set differs from the real one in both size and content
- [ ] `RecoveryMailer.kt` contains no member whose name is `send`, and no parameter named `subject`
      or `body`
- [ ] Every command in `verify:` exits 0

## Proof

1. Add `public suspend fun sendNewsletter(address: EmailAddress)` to `RecoveryMailer`.
   **`theMailerDeclaresExactlyTwoMembers` reddens alone**, naming `sendNewsletter` in the diff of
   the two sets. The other two tests are unaffected — the control interface is untouched and the
   two real members still return `Unit`. This is the exact diff §6.2 says a reviewer must be made
   to read. Revert.
2. Rename `sendPasswordReset` to `send`. **`theMailerDeclaresExactlyTwoMembers` reddens alone**,
   *expected {sendVerification, sendPasswordReset}, got {sendVerification, send}* — the count is
   unchanged, which is precisely why the assertion is over names and the criterion above forbids a
   size check. Revert.
3. Change `sendVerification` to return `Boolean`. **`neitherMailFunctionReturnsAnything` reddens
   alone.** `theMailerDeclaresExactlyTwoMembers` still sees two names and passes, so this mutation
   is what proves the third test is not a restatement of the first. Revert.
4. The control's own mutation: change the test's reflection helper to ignore its `KClass` argument
   and always read `RecoveryMailer::class`. **`theReflectionSeesTheMembersItClaimsTo` reddens
   alone**, *expected three names, got two*, while `theMailerDeclaresExactlyTwoMembers` passes.
   Run this one — a reflection helper that quietly reads the wrong class is how a structural test
   becomes a tautology, and nothing else in the file would notice.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
