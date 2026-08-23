---
schema: 2
id: TASK-040504
title: A session token is 256 bits, URL-safe and unpadded
type: task
status: backlog
parent: STORY-0405
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, session]
depends_on: [TASK-040503]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.SessionTokensTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

There is one place that mints a session token, it draws 256 bits from a `SecureRandom` it was
handed, and what comes out is URL-safe base64 with no padding.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/SessionTokens.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/SessionTokensTest.kt` | create |

Read `poker-server/src/main/kotlin/duels/poker/server/auth/PresentedSecret.kt` — `SessionToken`
already lives there and is **not** redeclared — and
`poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt` for the
`SecureRandom`-as-a-constructor-parameter shape this copies. `ADR-0027` §2, first bullet.

## Scope

- `public class SessionTokens(private val random: SecureRandom = SecureRandom())` with

  ```kotlin
  public fun newToken(): SessionToken
  ```

- Thirty-two bytes (`SESSION_TOKEN_BYTES = 32`, a named constant — 256 bits), then
  `Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)`.
- `random` is a **constructor** parameter, not a value read at call time, so a test can hand it a
  source whose output it controls and pin the exact string. That is the seam `Argon2Hasher` already
  uses.
- KDoc says why the token is not hashed here: the stored form is SHA-256 and it belongs to
  `duels.poker.server.db` (`ADR-0027` §2), so nothing in this file ever sees a stored form.

## Out of scope

- Storing, hashing, reading or deleting a token — `TASK-040506`–`TASK-040508`.
- `SessionToken` itself, which already exists.

## Tests

`SessionTokensTest`

| Test | Proves |
| --- | --- |
| `aTokenIsFortyThreeCharacters` | 32 bytes unpadded base64 is exactly 43 characters |
| `aTokenUsesOnlyTheUrlSafeAlphabet` | over 200 tokens from a real `SecureRandom`, every character is in `A-Za-z0-9-_`; none is `+`, `/` or `=` |
| `twoTokensDiffer` | two calls on one instance return different values |
| `theTokenIsExactlyWhatTheRandomGave` | a `SecureRandom` subclass whose `nextBytes` fills the array with `0x00..0x1F` yields the one pinned string, **and** a second stub filling `0xFF` yields a different pinned string — two inputs, because one fixed expectation is also what a hard-coded constant returns |
| `nothingIsDrawnUntilAskedTwice` | the stub counts `nextBytes` calls: one per `newToken`, two after two calls — a cached token would answer one |

## Acceptance criteria

- [ ] `SessionTokensTest.aTokenIsFortyThreeCharacters` passes
- [ ] `SessionTokensTest.aTokenUsesOnlyTheUrlSafeAlphabet` passes
- [ ] `SessionTokensTest.twoTokensDiffer` passes
- [ ] `SessionTokensTest.theTokenIsExactlyWhatTheRandomGave` passes
- [ ] `SessionTokensTest.nothingIsDrawnUntilAskedTwice` passes
- [ ] Every command in `verify:` exits 0

## Proof

Swap `withoutPadding()` for the padding encoder: `aTokenIsFortyThreeCharacters` and
`theTokenIsExactlyWhatTheRandomGave` both go red. Drop `SESSION_TOKEN_BYTES` from 32 to 16 and the
length test goes red while `aTokenUsesOnlyTheUrlSafeAlphabet` stays green — which is why the length
test exists.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
