---
schema: 2
id: TASK-040304
title: Bouncy Castle on the classpath, pinned to the published Argon2id vector
type: task
status: ready
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, auth, build, crypto]
depends_on: [TASK-040303]
verify:
  - ./gradlew :poker-server:test --tests '*Argon2VectorTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`org.bouncycastle:bcprov-jdk18on` is a declared dependency of `:poker-server`, and the Argon2id it
computes is proven to be Argon2id by reproducing RFC 9106's published test vector byte for byte.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify — one `[versions]` entry, one `[libraries]` entry |
| `poker-server/build.gradle.kts` | modify — one `implementation(...)` line |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2VectorTest.kt` | create |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1 names the artifact and the parameters, and says why Bouncy Castle rather than a native binding |

## Scope

- `[versions]` gains `bouncycastle = "1.79"`; `[libraries]` gains
  `bouncycastle-provider = { group = "org.bouncycastle", name = "bcprov-jdk18on", version.ref = "bouncycastle" }`.
  Keep both entries in the alphabetical neighbourhood of their sections, as the file already is.
- `poker-server/build.gradle.kts` gains `implementation(libs.bouncycastle.provider)` beside the other
  `implementation` lines. **Not `testImplementation`** — the hasher this dependency exists for ships
  in `main`.
- The test lives in `duels.poker.server.db`, the package `ADR-0027` §1 confines the hash to.
- **The provider is never registered with JCE.** This project calls
  `org.bouncycastle.crypto.generators.Argon2BytesGenerator` and
  `org.bouncycastle.crypto.params.Argon2Parameters` directly — the lightweight API, no
  `Security.addProvider`, no `java.security` plumbing.
- Nothing else in this ticket: no hasher, no PHC string, no production code at all.

## Out of scope

- Encoding or parsing a PHC string — `TASK-040305` and `TASK-040306`.
- The hasher itself, its salt, and its comparison — `TASK-040308`.
- `bcpkix`, `bcutil`, or any other Bouncy Castle artifact. One artifact, one reason.

## Tests

`Argon2VectorTest` — a known-answer test against the vector in
[RFC 9106](https://www.rfc-editor.org/rfc/rfc9106.html) §5.3, which the test's comment must cite.

| Test | Proves |
| --- | --- |
| `theRfc9106Argon2idVectorReproduces` | with `version = 19`, `t = 3`, `m = 32` KiB, `p = 4`, password 32 × `0x01`, salt 16 × `0x02`, secret 8 × `0x03`, associated data 12 × `0x04`, a 32-byte tag equals `0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659` |
| `oneDifferentSaltByteChangesTheTag` | the same call with the salt's last byte changed to `0x03` produces a different tag — so the previous test observes a function of its inputs and not a constant the code returns regardless |
| `theProductionParametersProduceAThirtyTwoByteTag` | `m = 19456` KiB, `t = 2`, `p = 1`, a 16-byte salt and a 32-byte output run to completion in this JVM — the parameters `ADR-0027` §1 fixes are allocatable here, not just in the ADR |

## Acceptance criteria

- [ ] `Argon2VectorTest.theRfc9106Argon2idVectorReproduces` passes, and the expected tag is a literal
      constant in the test compared byte for byte — not recomputed by the code under test
- [ ] `Argon2VectorTest.oneDifferentSaltByteChangesTheTag` passes
- [ ] `Argon2VectorTest.theProductionParametersProduceAThirtyTwoByteTag` passes
- [ ] The vector's parameters are `ARGON2_id` — a test that passes with `ARGON2_i` or `ARGON2_d`
      selected is testing the wrong algorithm, and the RFC's tag for each differs
- [ ] `libs.versions.toml` declares the version once, in `[versions]`; no version string appears in
      `poker-server/build.gradle.kts`
- [ ] Every command in `verify:` exits 0

> If the computed tag does not equal the constant above, **do not adjust the constant to match the
> code.** Stop and report: either the parameters are wrong or this ticket is, and a known-answer test
> edited to agree with its subject proves nothing.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
