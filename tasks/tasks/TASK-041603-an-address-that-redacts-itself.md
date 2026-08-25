---
schema: 2
id: TASK-041603
title: An address that redacts itself
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, auth, security]
depends_on: [TASK-041602]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.EmailAddressTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`EmailAddress` exists, carries the address the player typed, and prints a fixed redaction — so
leaking one into a log line takes intent rather than a careless string template.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/EmailAddress.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/EmailAddressTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/PresentedSecret.kt` — `SessionToken` is the
exact shape this copies, down to the `REDACTION` companion constant;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §6.3.

## Scope

- A `public @JvmInline value class EmailAddress(public val value: String)` in
  `duels.poker.server.auth`, overriding `toString()` to return `REDACTION`, with
  `public const val REDACTION: String = "EmailAddress(redacted)"` in its companion — the same
  spelling convention `PresentedSecret` and `SessionToken` already use.
- KDoc saying **why**: `ADR-0031` §6.3 puts the address in exactly one flow, and a redacting
  `toString()` is what makes a `"$address"` in a log line print nothing rather than everything.
- **No validation of any kind.** No `init`, no `require`, no regex. The type is a carrier; whether a
  string is an acceptable address is `TASK-041624`, blocked on `DEC-071`, and putting a rule here
  would make the answer to that decision an edit to a value class rather than a function.

## Out of scope

- `emailAddressOrNull` and any syntax rule — `TASK-041624`, blocked on `DEC-071`.
- Case folding. `ADR-0031` §2 folds in a database index and stores what the player typed; a
  `lowercase()` here would silently destroy the deliverable form.
- `VerificationToken` and `ResetToken` — `TASK-041604`.
- Serialisation. This type never crosses the wire (`ADR-0031` §6.3), so it gets no `@Serializable`.

## Tests

`EmailAddressTest`

| Test | Proves |
| --- | --- |
| `theValueIsTheAddressAsTyped` | `EmailAddress("Bob@Example.com").value` is `"Bob@Example.com"` — the case survives, because that is what must be delivered to |
| `printingOneRevealsNothing` | Both `EmailAddress("bob@example.com").toString()` and the interpolation `"${EmailAddress("bob@example.com")}"` equal `EmailAddress.REDACTION`, and neither contains `"bob"`, `"@"` nor `"example.com"` |
| `twoDifferentAddressesPrintTheSameThing` | `EmailAddress("a@b.test").toString() == EmailAddress("zzz@qqq.test").toString()`. The redaction is fixed, not derived — a `toString()` that returned a masked prefix would pass the test above and fail this one |

## Acceptance criteria

- [ ] `EmailAddressTest.theValueIsTheAddressAsTyped` passes
- [ ] `EmailAddressTest.printingOneRevealsNothing` passes
- [ ] `EmailAddressTest.twoDifferentAddressesPrintTheSameThing` passes
- [ ] `printingOneRevealsNothing` asserts the **interpolated** form as well as the direct
      `toString()` call — the interpolation is the leak that actually happens
- [ ] `EmailAddress.kt` contains no `init` block, no `require`, and no `Regex`
- [ ] `EmailAddress.kt` contains no call to `lowercase`, `uppercase` or `trim`
- [ ] Every command in `verify:` exits 0

## Proof

1. Delete the `override fun toString()` line, so the value class prints its default
   `EmailAddress(value=bob@example.com)`. **`printingOneRevealsNothing` and
   `twoDifferentAddressesPrintTheSameThing` both redden**; `theValueIsTheAddressAsTyped` stays
   green. Two reddening is the correct prediction — a mutation that removes the redaction entirely
   must break every assertion about it, and if only one breaks, the other was never about the
   redaction. Revert.
2. Replace the body with `override fun toString(): String = "EmailAddress(${value.take(1)}…)"`.
   **`twoDifferentAddressesPrintTheSameThing` reddens alone** — *expected EmailAddress(a…), got
   EmailAddress(z…)* — while `printingOneRevealsNothing`'s substring assertions still pass, since
   `"EmailAddress(b…)"` contains neither `"bob"` nor `"@"` nor `"example.com"`. This is why the
   third test exists and is the one mutation that justifies it. Revert.
3. Add `.lowercase()` to the constructor's stored value (as an `init`-free
   `public val value: String get() = ...` is impossible on a value class, do it by changing the two
   call sites in the test to pass `"Bob@Example.com".lowercase()` and asserting the original —
   equivalently, make the *test* pass a folded string). **`theValueIsTheAddressAsTyped` reddens
   alone**, *expected Bob@Example.com, got bob@example.com*. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
