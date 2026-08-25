---
schema: 2
id: TASK-041605
title: One digest for both recovery tokens
type: task
status: backlog
parent: STORY-0416
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, db, security]
depends_on: [TASK-041604]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RecoveryTokenDigestTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

Both recovery tokens have a SHA-256 stored form, computed by one function in
`duels.poker.server.db`, so no plaintext token is ever written to a `token_hash` column.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/RecoveryTokenDigest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/RecoveryTokenDigestTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/SessionTokenDigest.kt` — the function this
mirrors, including the fresh-`MessageDigest`-per-call comment and the `UTF_8` charset;
`poker-server/src/main/kotlin/duels/poker/server/auth/RecoveryTokens.kt`;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- Two `internal` top-level functions in `duels.poker.server.db`:
  `recoveryTokenDigest(token: VerificationToken): ByteArray` and
  `recoveryTokenDigest(token: ResetToken): ByteArray`, both delegating to one private
  `digestOf(value: String): ByteArray`.
- `MessageDigest.getInstance("SHA-256")` **per call** — instances are not thread-safe and these are
  called from `Dispatchers.IO`, exactly as `sessionTokenDigest`'s comment records.
- `internal`, not `public`: `ADR-0031` §6.3 keeps the address and its tokens inside
  `duels.poker.server.db`, and a public digest is an invitation to hash a token somewhere else.

## Out of scope

- Any database statement. This ticket computes bytes; `TASK-041608` onward store them.
- A generic `digest(String)` on the public API. The two overloads exist so that a caller must
  already hold a token type, which is what stops an arbitrary string being hashed into a
  `token_hash` column.
- Reusing `sessionTokenDigest` by widening it to take a `String`. That function's signature is what
  keeps `auth_session` writes honest, and loosening it would remove a guard from merged code for
  the convenience of new code.

## Tests

`RecoveryTokenDigestTest`

| Test | Proves |
| --- | --- |
| `aVerificationTokenHashesToItsPublishedSha256` | `recoveryTokenDigest(VerificationToken("abc"))` equals the published SHA-256 of `"abc"`, written as a **literal** 32-byte hex string (`ba7816bf…f20015ad`), never recomputed with `MessageDigest` in the test |
| `aResetTokenHashesToTheSameBytes` | `recoveryTokenDigest(ResetToken("abc"))` equals the identical literal. The two overloads must agree, or a token verified through one path and stored through the other never matches |
| `aDifferentTokenHashesDifferently` | `recoveryTokenDigest(VerificationToken("abc"))` and `recoveryTokenDigest(VerificationToken("abd"))` are unequal, and the digest is 32 bytes long in both cases. Two inputs, because one fixture cannot tell a hash from a constant |
| `aNonAsciiTokenIsHashedAsUtf8` | `recoveryTokenDigest(VerificationToken("é"))` equals the published SHA-256 of the two UTF-8 bytes `C3 A9`, so the platform default charset can never decide the stored form |

## Acceptance criteria

- [ ] `RecoveryTokenDigestTest.aVerificationTokenHashesToItsPublishedSha256` passes
- [ ] `RecoveryTokenDigestTest.aResetTokenHashesToTheSameBytes` passes
- [ ] `RecoveryTokenDigestTest.aDifferentTokenHashesDifferently` passes
- [ ] `RecoveryTokenDigestTest.aNonAsciiTokenIsHashedAsUtf8` passes
- [ ] The expected digests are **literals in the test source**; the file contains no call to
      `MessageDigest`
- [ ] Both `recoveryTokenDigest` overloads are `internal`, and neither is `public`
- [ ] Every command in `verify:` exits 0

## Proof

1. Change `digestOf` to `MessageDigest.getInstance("SHA-1")`. **All four tests redden** — three on
   the byte comparison and `aDifferentTokenHashesDifferently` on its length assertion, *expected 32,
   got 20*. Four is the correct prediction; if any stayed green, that test was not comparing bytes.
   Revert.
2. Change the `ResetToken` overload to prefix its input, e.g. `digestOf("reset:" + token.value)`.
   **`aResetTokenHashesToTheSameBytes` reddens alone.** This is the realistic defect — domain
   separation looks like good practice and would silently break every reset, because
   `TASK-041613` hashes at issue and `TASK-041614` hashes at consumption. Revert.
3. Change `digestOf` to use `value.toByteArray()` with no explicit charset.
   **`aNonAsciiTokenIsHashedAsUtf8` reddens on a JVM whose default charset is not UTF-8, and stays
   green on one whose default is.** Run it, and if it stays green, say so in the PR: the assertion
   is a *documentation* of the intended encoding rather than a gate on this machine. Do not delete
   it for that reason — a token minted by `RecoveryTokens` is base64 and ASCII-only, so this test is
   the only thing in the repository that pins the charset of this function at all. Revert.
4. Replace the body of `digestOf` with a constant `ByteArray(32)`.
   **`aDifferentTokenHashesDifferently` reddens on the inequality**, and the two literal tests
   redden on their comparisons. The length assertion alone would not have caught it, which is why
   the inequality is there. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
