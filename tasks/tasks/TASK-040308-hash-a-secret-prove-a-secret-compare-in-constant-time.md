---
schema: 2
id: TASK-040308
title: Hash a secret, prove a secret, and compare the tags in constant time
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, auth, crypto, security]
depends_on: [TASK-040307]
verify:
  - ./gradlew :poker-server:test --tests '*Argon2HasherTest'
  - sh -c 'grep -q "MessageDigest.isEqual" poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt && ! grep -q "contentEquals" poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A `PresentedSecret` hashes to a PHC string this project's own parser accepts, the same secret
verifies against it, and the two tags are compared with `MessageDigest.isEqual` — never with an
early-exiting `contentEquals` that tells an attacker how many leading bytes were right.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2HasherTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Phc.kt` | read — the constants, the encoder and `parseArgon2PhcOrNull` this uses |

## Scope

- `internal interface SecretHasher` with two suspending functions:
  `suspend fun hash(secret: PresentedSecret): String` and
  `suspend fun matches(secret: PresentedSecret, storedPhc: String): Boolean`. It is an interface
  because `TASK-040313` needs to count calls through it; that is the only reason, and it is enough.
- `internal class Argon2Hasher(private val random: SecureRandom = SecureRandom()) : SecretHasher`.
  Both are `internal` and both live in `duels.poker.server.db` — `hash` returns a hash, and
  `ADR-0027` §1 says no hash leaves this package.
- `hash`: 16 random bytes from the injected `SecureRandom`, an Argon2id tag over
  `secret.value.toByteArray(Charsets.UTF_8)` with the `TASK-040305` constants, then `encode()`. The
  UTF-8 conversion is written out rather than left to Bouncy Castle's `char[]` overload, so the
  encoding is this project's and is under test.
- `matches`: `parseArgon2PhcOrNull(storedPhc) ?: return false`, recompute the tag over the parsed
  salt, then `java.security.MessageDigest.isEqual(recomputed, parsed.tag)`. **A stored string the
  parser refuses answers `false`; it never throws.**
- Both functions wrap their work in `withContext(Dispatchers.IO)`, following
  `PostgresProfileWrites`. The bound of four is `TASK-040309`.
- Nothing here logs, and no exception message names the secret, the salt, the tag or the stored
  string.

## Out of scope

- Bounding concurrency — `TASK-040309`, which replaces the dispatcher and tests the bound.
- Any database access, any `credential` row — `TASK-040312`.
- Rehashing on a successful verify under raised parameters — see `DEC-044`; not built.

## Tests

`Argon2HasherTest`, with `runBlocking`. Kotlin test bodies are block bodies with **no** explicit
`: Unit` — ktlint's `no-unit-return` fails the build, and an expression body returning a value is
silently never run as a test.

| Test | Proves |
| --- | --- |
| `aHashedSecretVerifiesAgainstItsOwnString` | `matches(secret, hash(secret))` is true |
| `theSameSecretHashedTwiceGivesTwoStringsAndBothVerify` | the two strings differ (the salt varies) and both verify — a fixed salt passes the first test and fails this one |
| `theHashIsAStringTheProjectsOwnParserAccepts` | `parseArgon2PhcOrNull(hash(secret))` is not null, so the encoder and the hasher agree on the format |
| `aWrongSecretDoesNotVerify` | a different secret against the same string is false |
| `aTagDifferingOnlyInItsLastByteDoesNotVerify` | a valid string whose tag's last byte is flipped is false |
| `aTagDifferingOnlyInItsFirstByteDoesNotVerify` | the same with the first byte — the pair is what shows the comparison examines the whole array rather than stopping early |
| `aStoredStringTheParserRefusesAnswersFalse` | `matches(secret, "not-a-phc-string")` is false and throws nothing |
| `aNonAsciiSecretVerifies` | a secret containing non-ASCII characters hashes and verifies — the UTF-8 conversion is stable in both directions |

## Acceptance criteria

- [ ] All eight tests above pass
- [ ] `Argon2Hasher.kt` contains `MessageDigest.isEqual` and does **not** contain `contentEquals` —
      the second `verify` command checks both, and both matter: the first alone would pass with a
      `contentEquals` sitting beside it
- [ ] No `==`, `equals` or `contentEquals` is applied to a tag or to a stored string anywhere in the
      file
- [ ] `theSameSecretHashedTwiceGivesTwoStringsAndBothVerify` asserts the two strings are **not**
      equal, and that both verify — two assertions, because either alone permits a defect
- [ ] `aTagDifferingOnlyInItsFirstByteDoesNotVerify` and `aTagDifferingOnlyInItsLastByteDoesNotVerify`
      are both present
- [ ] `SecretHasher` and `Argon2Hasher` are `internal`; the file contains no `public`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
