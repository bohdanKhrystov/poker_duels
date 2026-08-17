---
schema: 2
id: TASK-040307
title: Two values that print a redaction, in every form a string can take
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, security]
depends_on: [TASK-040306]
verify:
  - ./gradlew :poker-server:test --tests '*RedactedValuesTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PresentedSecret` and `SessionToken` exist in `duels.poker.server.auth`, and putting either one into
a log line, a string template or an exception message yields a fixed redaction — so leaking one
takes intent rather than a careless `"$it"`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PresentedSecret.kt` | create — both value classes, one file, because they share one rule and one test |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RedactedValuesTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` | read — `DeviceId` and `PlayerId` are the house shape for a `@JvmInline value class` |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §2, the redaction rule and what a session token is |

## Scope

- `public value class PresentedSecret(public val value: String)`, `@JvmInline`, in the new package
  `duels.poker.server.auth`. `toString()` returns the constant `"PresentedSecret(redacted)"`.
- `public value class SessionToken(public val value: String)`, same shape, `toString()` returns
  `"SessionToken(redacted)"`.
- Each redaction is a `public const val` on the class's companion, so the test asserts against the
  same constant the class returns and the string is written once.
- KDoc on both saying what the redaction is for: a bearer secret in a log line is the leak that no
  amount of endpoint care repairs, and `value` is the deliberate, named way to read one.
- **Neither class validates anything.** No `init`, no `require`, no length rule, no character rule.
  What a password may be is answered by
  [`ADR-0048`](../../docs/adr/ADR-0048-a-password-has-one-rule-and-it-is-length.md) — and it
  **confirms this instruction rather than retracting it**: the 8-code-point minimum is a
  sign-up/reset rule that never runs at sign-in, and this value class is constructed at sign-in
  too, so a `require` here would refuse a legitimate sign-in by an account created before the
  rule. The 128 maximum belongs wherever a secret is hashed, not here.

## Out of scope

- Hashing either value — `TASK-040308`.
- Issuing, storing or expiring a session token — `STORY-0405`. This ticket creates the type because
  the redaction rule is the same for both and belongs in one test.
- Anything that reads `value`. The type is created here; its first caller arrives in `TASK-040308`.

## Tests

`RedactedValuesTest`, in `duels.poker.server.auth`. Use a raw value that could not appear by
accident — `"correct-horse-battery-staple"` — so a `contains` assertion means what it says.

| Test | Proves |
| --- | --- |
| `aPresentedSecretPrintsItsRedaction` | `toString()` equals `PresentedSecret.REDACTION` exactly |
| `aSessionTokenPrintsItsRedaction` | `toString()` equals `SessionToken.REDACTION` exactly |
| `twoDifferentSecretsPrintTheSameThing` | two secrets built from different strings have equal `toString()` — the output is a constant, not a function of the value, which is what "redacted" has to mean |
| `theSecretReachesNoStringByAnyOfTheUsualRoutes` | enumerates the routes and asserts none of the results contains the raw value: `"$secret"`, `secret.toString()`, `listOf(secret).toString()`, `mapOf("k" to secret).toString()`, `String.format("%s", secret)`, `StringBuilder().append(secret).toString()`, and `IllegalStateException("secret was $secret").message` |
| `theValueIsStillReadableToCodeThatAsksForIt` | `secret.value` equals the raw string — the redaction is a printing rule, not encryption, and the hasher has to be able to read it |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `theSecretReachesNoStringByAnyOfTheUsualRoutes` covers **all seven** routes listed above, in
      one collected assertion that names which route leaked — a test that checks only `toString()`
      proves nothing about the template form, which is how a leak actually happens
- [ ] `twoDifferentSecretsPrintTheSameThing` is present and uses two genuinely different strings
- [ ] Both tests for `SessionToken` and `PresentedSecret` exist — the story names both types, and one
      redacted type beside one unredacted one is worse than none
- [ ] `PresentedSecret.kt` contains no `init`, no `require` and no length or character rule
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
