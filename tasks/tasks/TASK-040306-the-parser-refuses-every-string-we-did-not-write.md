---
schema: 2
id: TASK-040306
title: The parser accepts what we wrote and refuses everything else
type: task
status: backlog
parent: STORY-0403
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, auth, crypto, security]
depends_on: [TASK-040305]
verify:
  - ./gradlew :poker-server:test --tests '*Argon2PhcParseTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`parseArgon2PhcOrNull` returns the salt and tag of a string this project's own encoder produced, and
`null` for every other string — including one that differs only in a cost parameter, which is what a
downgrade attack looks like when it is hiding in a helper function.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/Argon2Phc.kt` | modify — add the parser beside the encoder |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2PhcParseTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/Argon2PhcEncodeTest.kt` | read — the literal strings this parser must accept |

## Scope

- `internal fun parseArgon2PhcOrNull(encoded: String): Argon2Phc?` in the same file. It **returns
  `null` and never throws** — including for input that is not base64 at all, which
  `java.util.Base64` signals with `IllegalArgumentException` and this function must catch.
- The accepted shape is exactly one: split on `$` gives six parts, the first empty, then
  `argon2id`, `v=19`, `m=19456,t=2,p=1`, the salt, the tag. **The three fixed parts are compared as
  whole literal strings** against the constants `TASK-040305` introduced, so a reordering, an added
  space or a changed digit is refused without a second rule being written.
- **`=` in the salt or tag section is refused explicitly.** `java.util.Base64`'s decoder accepts
  input with *and* without padding, so a test for "no padding" that relies on the decoder passes
  vacuously; the check has to be ours.
- The decoded salt must be exactly 16 bytes and the decoded tag exactly 32. A short salt is a real
  attack, not a typo.
- No logging, no exception message, and no returned value ever contains any part of `encoded`.

## Out of scope

- Deciding what happens to rows written under **older** parameters, the day the cost is raised.
  `ADR-0027` §1 says raising them is *"a constant change plus a rehash on next successful verify"*,
  which needs a parser that accepts the old string; this story says refuse. The conservative
  direction is taken here — loosening a refusal later is additive, and no row with other parameters
  has ever existed — and the question is registered as `DEC-044` for the architect, due before
  anyone raises the cost. It blocks nothing today.
- Computing or comparing a tag — `TASK-040308`.
- Rehashing anything, ever, in this story.

## Tests

`Argon2PhcParseTest`. Two of the three tests are table-driven; **each case in the table is named**,
and the assertion collects every failure and reports them together rather than stopping at the
first — a universal claim is proven by enumerating what it quantifies over.

| Test | Proves |
| --- | --- |
| `whatTheEncoderWroteParsesBackToTheSameBytes` | both literal strings from `Argon2PhcEncodeTest` parse to non-null, and `assertContentEquals` holds for the salt and for the tag |
| `everyParameterThatIsNotOursIsRefused` | each of these returns `null`: `argon2i`, `argon2d`, `argon2`, `v=16`, `v=13`, `m=4096,t=2,p=1`, `m=19456,t=1,p=1`, `m=19456,t=2,p=2`, `m=19456, t=2, p=1` (spaced), `t=2,m=19456,p=1` (reordered) |
| `everyMalformedStringIsRefused` | each of these returns `null`: the empty string, a string with no leading `$`, five sections, seven sections, a padded salt (`=`), a padded tag (`=`), URL-safe base64 (`-` and `_`), a non-base64 character (`!`), a salt that decodes to 8 bytes, a tag that decodes to 16 bytes |

## Acceptance criteria

- [ ] All three tests above pass
- [ ] `everyParameterThatIsNotOursIsRefused` contains all ten named cases, and its assertion reports
      the **collected** list of cases that were wrongly accepted — not one `assertNull` that stops at
      the first
- [ ] `everyMalformedStringIsRefused` contains all ten named cases, likewise collected
- [ ] `whatTheEncoderWroteParsesBackToTheSameBytes` uses `assertContentEquals` on both fields; it
      must not compare two `Argon2Phc` instances, which have no value `equals` by design
- [ ] `parseArgon2PhcOrNull` throws nothing for any input in either table — the tests would fail with
      an exception rather than a `null`, so this is checked by their passing
- [ ] No message, log or return value in `Argon2Phc.kt` contains any part of the parsed string
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
