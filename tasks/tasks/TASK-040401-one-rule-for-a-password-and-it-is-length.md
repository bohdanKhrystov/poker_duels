---
schema: 2
id: TASK-040401
title: One rule for a password, and it is length
type: task
status: ready
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, auth, security]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.auth.PasswordPolicyTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0048`'s whole password policy exists as three pure functions: one that produces the NFC form of
a presented secret, and two predicates over its code-point count — the minimum and the maximum,
separately, because they are enforced in different places.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PasswordPolicy.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/auth/PasswordPolicyTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/auth/LoginHandle.kt` | read — the code-point counting idiom this file copies (`codePointCount`, never `String.length`) |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PresentedSecret.kt` | read — the parameter type, which deliberately has no `init` |

## Scope

Three functions in `duels.poker.server.auth`, and nothing else:

- `internal fun nfcNormalisedSecret(secret: PresentedSecret): String` — `java.text.Normalizer.normalize(secret.value, Normalizer.Form.NFC)`. **NFC, never NFKC** (`ADR-0048` §5: NFKC folds `ﬁ` to `fi` and `５` to `5`, which are strings a player can tell apart, so it would silently store a different secret from the one that was set). This is the only call to `Normalizer` this project makes for a secret; `TASK-040402` makes `Argon2Hasher` call it rather than adding a second one.
- `public fun passwordIsLongEnough(secret: PresentedSecret): Boolean` — the NFC form is **at least 8** code points. `ADR-0048` §2: a sign-up and reset rule, never applied at sign-in.
- `public fun passwordIsWithinTheWorkBound(secret: PresentedSecret): Boolean` — the NFC form is **at most 128** code points. `ADR-0048` §2: applied wherever a secret is hashed, including sign-in, which is why it is a separate function rather than one `isAcceptable`.

Two constraints that will not be obvious from the ADR:

- **Neither public name may contain `hash`, `digest`, `phc` or `tag`, in any case.**
  `PublicApiHasNoHashTest` sweeps every public member of `duels.poker.server.auth` for those four
  words and fails the build on a match — which is why the maximum is *the work bound* and not
  *the hashing bound*. `nfcNormalisedSecret` is `internal`, and the sweep skips non-public members.
- **Count code points, never `String.length`.** `codePointCount(0, length)`, exactly as
  `loginHandleOrNull` does. `String.length` counts UTF-16 units, so four emoji measure 8 there.

## Out of scope

- Any rule other than length. No composition rule, no character rule, no breach corpus, no strength
  meter, no similarity check against the handle (`ADR-0048` §3, which states each refusal as a
  decision so nobody later reads it as an omission and repairs it).
- **Trimming.** A leading or trailing space is part of the password (`ADR-0048` §4). There is no
  `raw.trim()` in this file, and a test below pins that.
- Putting any of this in `PresentedSecret`'s `init` — `ADR-0048` §6 refuses it explicitly and
  `TASK-040307`'s *"no `init`, no `require`"* still stands.
- Calling any of these from anywhere. `TASK-040402` wires the normalisation into the hasher and
  `TASK-040407` wires the predicates into the endpoint.

## Tests

`PasswordPolicyTest`. Test bodies are block bodies with **no** explicit `: Unit`.

| Test | Proves |
| --- | --- |
| `sevenCodePointsIsTooShort` | 7 ASCII characters fail `passwordIsLongEnough` |
| `eightCodePointsIsLongEnough` | 8 ASCII characters pass it — the boundary is inclusive |
| `oneHundredAndTwentyEightCodePointsIsWithinTheBound` | 128 ASCII characters pass `passwordIsWithinTheWorkBound` — inclusive at the top too |
| `oneHundredAndTwentyNineCodePointsIsOverTheBound` | 129 fails it |
| `fourAstralCharactersAreTooShortThoughTheyAreEightUtf16Units` | four emoji (each a surrogate pair) fail the minimum. **The wrong implementation this must fail against is `secret.value.length >= 8`, which accepts them** |
| `oneHundredAndTwentyEightAstralCharactersAreWithinTheBound` | 128 emoji pass the maximum. **The wrong implementation this must fail against is `secret.value.length <= 128`, which refuses them.** Both directions are needed: either test alone passes for a `String.length` implementation |
| `sevenComposableCharactersAreTooShortThoughTheyAreFourteenBeforeNfc` | seven `e` + `U+0301` pairs — 14 code points as typed, 7 after NFC — fail the minimum. **The wrong implementation this must fail against is one that counts before normalising**, which sees 14 and accepts |
| `nothingIsTrimmed` | `"   a    "` (8 code points, 1 of them not a space) passes the minimum, and `"        "` (8 spaces) passes it too. **The wrong implementation this must fail against is one that calls `trim()` first**, which sees 1 and 0 |
| `noCodePointIsRefused` | a fixed list of at least five awkward secrets — one containing `U+0000`, one containing `U+202E` (right-to-left override), one of emoji, one of CJK, one with a space in the middle — each padded to between 8 and 128 code points, all pass **both** predicates. The list is a `val` the test iterates, and the test asserts `secrets.isNotEmpty()` first, so an empty list cannot make this pass vacuously |

## Acceptance criteria

- [ ] All nine tests above pass
- [ ] `PasswordPolicy.kt` declares exactly three functions, `nfcNormalisedSecret` (`internal`),
      `passwordIsLongEnough` (`public`) and `passwordIsWithinTheWorkBound` (`public`)
- [ ] No public name in the file contains `hash`, `digest`, `phc` or `tag`, and
      `PublicApiHasNoHashTest` passes unchanged
- [ ] `PasswordPolicy.kt` contains no `trim`, and no character, composition or dictionary check
- [ ] Both predicates read `codePointCount`; neither reads `String.length` or `secret.value.length`
- [ ] `Normalizer` is named exactly once in the file, inside `nfcNormalisedSecret`, with
      `Normalizer.Form.NFC` and never `NFKC`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
