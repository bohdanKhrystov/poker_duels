---
schema: 2
id: TASK-040402
title: The hasher hashes the NFC form, in the one place a secret becomes bytes
type: task
status: done
parent: STORY-0404
module: poker-server
estimate: XS
tier: sonnet
review: deep
files_touched: 2
labels: [server, auth, db, security]
depends_on: [TASK-040401]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.Argon2HasherTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`Argon2Hasher` hashes the NFC form of a presented secret, so a password typed on macOS and re-typed
on Windows is one password — decided once, in the one line where a secret becomes bytes.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Hasher.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2HasherTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/auth/PasswordPolicy.kt` | read — `nfcNormalisedSecret`, which this ticket calls and does not reimplement |

## Scope

- One line changes. In `Argon2Hasher.tagFor`,
  `generator.generateBytes(secret.value.toByteArray(Charsets.UTF_8), tag)` becomes
  `generator.generateBytes(nfcNormalisedSecret(secret).toByteArray(Charsets.UTF_8), tag)`.
- A comment saying **why**, not what: this is the one place a presented secret becomes bytes, so
  putting the fold here is what makes sign-up and sign-in incapable of disagreeing (`ADR-0048` §5).
  Note in it that this is **permanent from the first stored hash** — changing the fold afterwards
  invalidates every stored hash, and `ADR-0031` leaves an opted-out account with no reset.
- `hash` and `matches` both route through `tagFor`, so both are normalised by this one change.
  **Do not normalise in `hash` and `matches` separately** — two call sites is exactly what
  `ADR-0048` §5 forbids.

## Out of scope

- Any length check in the hasher. The maximum is enforced at the endpoint, before Argon2 runs and
  before the identifier is looked up (`ADR-0048` §2) — `TASK-040407` and `TASK-040408`.
- `PostgresCredentials`, `Argon2Phc`, the cost parameters, and `DUMMY_PHC`. None moves.
- NFKC. `ADR-0048` §5 rejects it by name and a test below pins the rejection.

## Tests

`Argon2HasherTest`, adding three tests to the existing class and changing no assertion in it.

| Test | Proves |
| --- | --- |
| `aSecretHashedComposedVerifiesWhenPresentedDecomposed` | `hash(PresentedSecret("café"))`, then `matches(PresentedSecret("café"), thatPhc)` is `true`. **The wrong implementation this must fail against is the one on `develop` today**, which answers `false` |
| `aSecretHashedDecomposedVerifiesWhenPresentedComposed` | the same pair the other way round, so the fold is not accidentally one-directional |
| `aCompatibilityEquivalentSecretDoesNotVerify` | `hash(PresentedSecret("ﬁle1234"))` — `U+FB01` LATIN SMALL LIGATURE FI — then `matches(PresentedSecret("file1234"), thatPhc)` is `false`. **The wrong implementation this must fail against is `Normalizer.Form.NFKC`**, which answers `true` and would silently accept a password that is not the one that was set |

Write the two normalisation forms as `\u` escapes, not as literal characters: a source file whose
own encoding decides which form is stored makes these three tests unreadable and reviewable only by
hex dump.

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `hashingWithAFixedRandomSourceProducesAnExactPhcString` passes **unchanged** — its secret is
      ASCII and ASCII is NFC-invariant, which is `ADR-0048` §5's stated reason `STORY-0403`'s
      pinned strings do not move
- [ ] `Argon2VectorTest` and `Argon2PhcEncodeTest` pass unchanged, for the same reason
- [ ] `Argon2Hasher.kt` names `nfcNormalisedSecret` exactly once and `Normalizer` not at all
- [ ] `Argon2Hasher.kt` contains no length check and no `require`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
