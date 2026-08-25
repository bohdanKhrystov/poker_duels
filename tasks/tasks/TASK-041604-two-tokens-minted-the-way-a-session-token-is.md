---
schema: 2
id: TASK-041604
title: Two tokens, minted the way a session token is
type: task
status: ready
parent: STORY-0416
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, security]
depends_on: [TASK-041603]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.RecoveryTokensTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`VerificationToken` and `ResetToken` exist as redacting value classes, and `RecoveryTokens` mints
each from 256 bits of `SecureRandom` as URL-safe unpadded base64 — the same generator and the same
shape as `ADR-0027`'s session token, because `ADR-0031` §4 says so in as many words.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/RecoveryTokensTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/auth/SessionTokens.kt` — the constructor-injected
`SecureRandom`, the byte count and the encoder this class copies exactly;
`poker-server/src/main/kotlin/duels/poker/server/auth/PresentedSecret.kt` — the redaction shape;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §3 and §4.

## Scope

- One file holding three declarations, so the two token types cannot drift apart:
  `VerificationToken` and `ResetToken`, both `public @JvmInline value class …(public val value:
  String)` with a redacting `toString()` and a `REDACTION` companion constant; and
  `public class RecoveryTokens(private val random: SecureRandom = SecureRandom())` with
  `newVerificationToken(): VerificationToken` and `newResetToken(): ResetToken`.
- `random` is a **constructor parameter with a default**, not a call-time read — that is what lets
  `RecoveryTokensTest` inject a source whose output it controls and pin an exact string, and it is
  the reason `SessionTokens` is shaped that way.
- 32 bytes, `Base64.getUrlEncoder().withoutPadding()`, giving exactly 43 characters. Name the byte
  count as a private top-level `const`, as `SESSION_TOKEN_BYTES` is.
- KDoc naming why this is not Argon2 (`ADR-0031` §4: there is nothing to guess offline in 256 bits
  the server chose) and that the token is hashed elsewhere — this class owns generation only.

## Out of scope

- Hashing for storage — `TASK-041605`'s `recoveryTokenDigest`, which lives in
  `duels.poker.server.db` for the same reason `sessionTokenDigest` does.
- Expiry, lifetimes and any clock. `ADR-0031`'s 24 hours and one hour are the storage layer's, from
  the injected `java.time.Clock` that `ADR-0062` §5 substituted for `ServerClock` in both clauses.
- Two separate files, one per token type. They are one file because a divergence in entropy between
  them would be invisible in review, and because `TASK-041605` hashes both through one function.

## Tests

`RecoveryTokensTest`

| Test | Proves |
| --- | --- |
| `aVerificationTokenIsTwoHundredAndFiftySixBitsOfUrlSafeBase64` | Minted from a `SecureRandom` stub filling the buffer with a fixed byte pattern, the token equals a literal 43-character string computed by hand; it contains no `+`, `/` or `=` |
| `aResetTokenIsMintedTheSameWay` | The identical stub, the identical expected string — the two minters differ only in return type, so a `newResetToken` that used 16 bytes or the standard alphabet reddens here alone |
| `twoCallsOnARealSecureRandomDiffer` | Two `newVerificationToken()` calls on a default-constructed `RecoveryTokens` are unequal and both 43 characters. The vacuity guard: the two tests above pin a *stub*, and would pass a minter that ignored `random` entirely |
| `printingEitherTokenRevealsNothing` | `"${VerificationToken("abc")}"` equals `VerificationToken.REDACTION` and `"${ResetToken("abc")}"` equals `ResetToken.REDACTION`; neither contains `"abc"`, and the two redactions are **different strings**, so a log line still says which kind leaked |

## Acceptance criteria

- [ ] `RecoveryTokensTest.aVerificationTokenIsTwoHundredAndFiftySixBitsOfUrlSafeBase64` passes
- [ ] `RecoveryTokensTest.aResetTokenIsMintedTheSameWay` passes
- [ ] `RecoveryTokensTest.twoCallsOnARealSecureRandomDiffer` passes
- [ ] `RecoveryTokensTest.printingEitherTokenRevealsNothing` passes
- [ ] The two stub-driven tests assert a **literal** expected string, not one recomputed by calling
      the same `Base64` encoder the production code calls
- [ ] `twoCallsOnARealSecureRandomDiffer` constructs `RecoveryTokens()` with no argument
- [ ] `RecoveryTokens` reads no clock and touches no database
- [ ] Every command in `verify:` exits 0

## Proof

1. In `newResetToken`, change the byte count from the shared constant to `16`.
   **`aResetTokenIsMintedTheSameWay` reddens alone**, *expected a 43-character string, got a
   22-character one*. `aVerificationTokenIsTwoHundredAndFiftySixBitsOfUrlSafeBase64` uses the other
   function and stays green — which is the whole reason the second test is not "argued to be
   unaffected" but written and run. Revert.
2. Swap `getUrlEncoder()` for `getEncoder()` in the shared helper. **Both stub-driven tests
   redden**, because the fixed byte pattern is chosen to produce at least one `+` or `/` under the
   standard alphabet. *Choose the stub's byte pattern so that it does* — verify this before writing
   the literal, because a pattern encoding to the same string under both alphabets makes both tests
   blind to the encoder. Revert.
3. Replace `random.nextBytes(bytes)` with a fixed `bytes.fill(7)`. **`twoCallsOnARealSecureRandom
   Differ` reddens alone**, on the inequality assertion; both stub-driven tests still pass, because
   their stub was producing a fixed pattern anyway. This is the mutation the vacuity guard exists
   for. Revert.
4. Give `ResetToken.REDACTION` the same value as `VerificationToken.REDACTION`. **`printingEither
   TokenRevealsNothing` reddens alone**, on the "different strings" assertion. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
