---
schema: 2
id: TASK-041627
title: A sender that sends nothing
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, mail, wiring]
depends_on: [TASK-041624]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.mail.NoRecoveryMailerTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`NoRecoveryMailer` exists, so the state every developer machine and every CI run is in — no mail
transport at all — is an ordinary implementation of `RecoveryMailer` rather than a `null` three
route handlers each have to remember to check.

## Why this comes before the endpoints

`TASK-041626`'s `allFourCasesAnswerOneIdenticalTwoOhTwo` needs a **no-sender-configured** run as one
of its four cases, and `TASK-041625` asserts through the same seam.
[`ADR-0077`](../../docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md)
§1 makes that seam this object. It was previously the tail of `TASK-041627`, which sat *after* both
of them; a seam has to exist before its consumers, so this ticket moved ahead of `TASK-041625` and
`TASK-041625` now depends on it. Nothing else about either ticket changed.

An inline `object : RecoveryMailer {}` written inside a route test would be a *copy* of the seam, not
the seam, and the property those tickets assert — that the shipped no-sender state answers
identically — would then be about a fixture rather than about the server.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/mail/NoRecoveryMailer.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/mail/NoRecoveryMailerTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryMailer.kt` — the two signatures this
implements, `sendVerification(address, token)` and `sendPasswordReset(address, token, handle)`;
`docs/adr/ADR-0077-no-sender-is-an-implementation-and-detachment-is-a-decorator.md` §1 and §8;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §7.

## Scope

- `public object NoRecoveryMailer : RecoveryMailer` in `duels.poker.server.mail`, with **two empty
  bodies**. A new package and a new file named for its single public type, per `ADR-0077` §8 and
  ktlint's filename rule.
- KDoc saying what it is for: this is what the wiring binds when no transport is configured, it is
  not a placeholder awaiting an exception, and `EPIC-07` replaces the *binding* rather than this
  file.

## Out of scope

- **Throwing, logging or warning.** An empty body is the decision, not an oversight —
  `ADR-0031` §7 makes a build with no sender a *valid* state, and `ADR-0077` §1's whole argument is
  that no caller may behave differently because a sender is absent. The startup log line and health
  check that would announce a configured sender are `ADR-0031`'s Consequences' deferral to
  `EPIC-07`. **Gated below**, because a refusal to throw produces no assertion by itself.
- `DetachedRecoveryMailer` — `TASK-041630`.
- Binding it anywhere. `ServerComponents` and `Application.kt` are `TASK-041634`'s; nothing
  constructs this object in `main` until then, and that is expected.
- Any transport, dependency or credential — `EPIC-07`, and the human's.

## Tests

`NoRecoveryMailerTest`

| Test | Proves |
| --- | --- |
| `bothMembersCompleteAndThrowNothing` | `sendVerification` and `sendPasswordReset` are each called once on `NoRecoveryMailer` and each returns normally. **Both members, not one** — a `TODO()` left in the second is invisible to a test that only calls the first |
| `itIsAnObjectAndNotAClass` | `NoRecoveryMailer::class.objectInstance` is not `null` and is the same reference as `NoRecoveryMailer`, and the value is assignable to a `RecoveryMailer`-typed reference. `objectInstance` is non-null **only** for an object declaration, so this is a real assertion rather than the tautology `NoRecoveryMailer === NoRecoveryMailer` would be. One shared stateless binding is the point: nothing constructs a second sender |

## Acceptance criteria

- [ ] `NoRecoveryMailerTest.bothMembersCompleteAndThrowNothing` passes
- [ ] `NoRecoveryMailerTest.itIsAnObjectAndNotAClass` passes, via `objectInstance`
- [ ] `bothMembersCompleteAndThrowNothing` calls **both** members
- [ ] `NoRecoveryMailer.kt` contains no `TODO`, no `throw`, no `error(`, no `require`, no `check`
      and no logging call
- [ ] The file declares exactly one public type and is named for it
- [ ] Every command in `verify:` exits 0

## Proof

1. Replace `sendPasswordReset`'s body with `error("no sender configured")`.
   **`bothMembersCompleteAndThrowNothing` reddens**, on the second call. This is the entire reason
   this ticket has a test at all: a loud failure so nobody deploys without a sender is the single
   most reasonable-looking edit anyone will make to this file, and under `ADR-0031` §5 it converts
   `forgot-password`'s unconditional `202` into a `500` on the exact path that must not vary.
   Revert.
2. Replace `sendVerification`'s body with `error(...)` and leave `sendPasswordReset` empty.
   **`bothMembersCompleteAndThrowNothing` reddens on the first call.** Run this one too: if only
   step 1 reddens, the test is calling one member and the other is ungated.
3. Change `object` to `class` and add `()` at both use sites so the file still compiles.
   **`itIsAnObjectAndNotAClass` reddens alone**, at the `objectInstance` assertion —
   *expected not null* — while `bothMembersCompleteAndThrowNothing` **passes**, because a class with
   two empty bodies behaves identically. Run it: without the `()` the module simply stops compiling,
   which is a build failure rather than a test failure; record which of the two the coder produced.
   Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
