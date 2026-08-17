---
schema: 2
id: TASK-040305
title: The PHC string this project writes, and the one function that writes it
type: task
status: done
parent: STORY-0403
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, auth, crypto]
depends_on: [TASK-040304]
verify:
  - ./gradlew :poker-server:test --tests '*Argon2PhcEncodeTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A salt and a tag encode to exactly the string `ADR-0027` §1 names —
`$argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>` — and the parameters that string carries exist once,
as named constants, in the package the hash is confined to.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Phc.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2PhcEncodeTest.kt` | create |
| `docs/adr/ADR-0027-the-session-outranks-the-device-id.md` | read — §1, the string and the parameters it must carry |

## Scope

- Everything in this file is `internal`, and it lives in `duels.poker.server.db`. `ADR-0027` §1:
  *"the hash never leaves `duels.poker.server.db`"* — a public function that returns a PHC string is
  a public function that returns a hash, so there is not one.
- Six `internal const val`s and nothing computed from them at runtime: version `19`, memory
  `19456` KiB, iterations `2`, parallelism `1`, salt `16` bytes, tag `32` bytes.
- `internal class Argon2Phc(val salt: ByteArray, val tag: ByteArray)` with
  `fun encode(): String`.
  - **A plain class, not a `data class`.** A `data class` over `ByteArray` gets an `equals` that
    compares references while reading like value equality — the exact trap a round-trip test would
    fall into. This class deliberately has no `equals`, and tests compare fields with
    `assertContentEquals`.
  - `init` requires `salt.size == ARGON2_SALT_BYTES` and `tag.size == ARGON2_TAG_BYTES`. **The
    failure message names the expected and actual *lengths* only** — never the bytes, which are the
    secret material this whole story exists to keep out of messages.
- The encoding is `java.util.Base64.getEncoder().withoutPadding()` — the **standard** alphabet
  (`+` and `/`), no `=`. Not `getUrlEncoder()`: the PHC string format specifies the standard
  alphabet, and a URL-safe encoder produces a string that this project's own parser must then
  refuse.
- KDoc on the class saying why the parameters travel with the row: raising them is a constant change
  plus a rehash on next successful verify, never a migration.
- `$` opens a template in a Kotlin string literal, so the format is written `"\$argon2id\$v=19…"` or
  with `${'$'}`. Getting this wrong produces a compile error, not a wrong hash — but it is the first
  thing to hit.

## Out of scope

- Parsing — `TASK-040306`, which adds `parseArgon2PhcOrNull` to this same file.
- Computing a tag, choosing a salt, or comparing anything — `TASK-040308`.
- Making any of this public, or exposing it through a port. It stays `internal` forever.

## Tests

`Argon2PhcEncodeTest`.

| Test | Proves |
| --- | --- |
| `theEncodedStringIsExactlyTheShapeAdr0027Names` | salt = 16 × `0x02`, tag = 32 × `0x03` encodes to `$argon2id$v=19$m=19456,t=2,p=1$AgICAgICAgICAgICAgICAg$AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM`, compared as one literal string |
| `theEncodingIsStandardBase64NotUrlSafe` | salt = `0xFB, 0xEF` × 8 and tag = `0xFB, 0xEF` × 16 encode to `$argon2id$v=19$m=19456,t=2,p=1$++/77/vv++/77/vv++/77w$++/77/vv++/77/vv++/77/vv++/77/vv++/77/vv++8` — a URL-safe encoder writes `-` and `_` here and fails |
| `theEncodedStringCarriesNoPadding` | no `=` in either encoding above; a 16-byte salt pads with two `=` under a padding encoder, so this is a case that would actually catch it |
| `aDifferentTagEncodesToADifferentString` | two tags differing in one byte encode differently — the encoder reads its arguments |
| `aSaltOrTagOfTheWrongLengthIsRefused` | a 15-byte salt and a 31-byte tag each raise `IllegalArgumentException`, and neither message contains any byte of the input |

## Acceptance criteria

- [ ] All five tests above pass
- [ ] `theEncodedStringIsExactlyTheShapeAdr0027Names` asserts one whole literal string — not a
      `startsWith`, a `contains` or a regex, either of which passes with the parameters wrong
- [ ] `theEncodingIsStandardBase64NotUrlSafe` is present and its expected string contains both `+`
      and `/`
- [ ] `aSaltOrTagOfTheWrongLengthIsRefused` covers **both** the salt and the tag
- [ ] Every declaration in `Argon2Phc.kt` is `internal`; the file contains no `public`
- [ ] `Argon2Phc` is not a `data class`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
