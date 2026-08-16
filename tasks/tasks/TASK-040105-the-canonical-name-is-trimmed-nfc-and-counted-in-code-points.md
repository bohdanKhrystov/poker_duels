---
schema: 2
id: TASK-040105
title: The canonical name is trimmed, NFC, and counted in code points
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, identity, unicode]
depends_on: [TASK-040104]
verify:
  - ./gradlew :poker-server:test --tests '*DisplayNameTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

One pure function turns what a player typed into the exact string the database will store, or into
`null` when there is no such string: trimmed, NFC-normalised, and between 1 and 32 **code points**.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/DisplayName.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/DisplayNameTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt` | read — the precedent for a request-value rule living in `http` as a pure function |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §2, and why the count is in code points |

## Scope

- `public fun canonicalDisplayNameOrNull(raw: String): String?` in `duels.poker.server.http`, with
  KDoc naming `ADR-0029` §2 and saying what `null` means.
- The order is fixed and matters: **trim, then normalise to NFC, then measure**. Normalising can
  change the code-point count, so measuring first would accept a name the database then refuses.
- Trimming is `String.trim()`; normalisation is `java.text.Normalizer.normalize(value,
  Normalizer.Form.NFC)`.
- The bound is `1..32` **code points** — `codePointCount(0, length)`, never `length`. A mismatch
  here is a `500` where a `400` belonged, because `char_length` in the `CHECK` counts code points.
- No I/O, no database, no logging. This function is what the route calls before it touches a port.

## Out of scope

- Refusing control characters, exotic whitespace and doubled spaces — `TASK-040106` adds those to
  this same function, deliberately as a second diff so the two rules are reviewed apart.
- The blocklist — `STORY-0410`.
- Anything that calls this. The route arrives in `TASK-040115`.

## Tests

`DisplayNameTest`

| Test | Proves |
| --- | --- |
| `aPlainNameIsReturnedUnchanged` | `"bob"` canonicalises to `"bob"` |
| `surroundingSpaceIsTrimmed` | `"  bob  "` canonicalises to `"bob"` |
| `aDecomposedNameIsComposed` | `"élodie"` returns the string equal to `"élodie"`, asserted by string equality against the composed literal **and** by a code-point count of 6 |
| `anAlreadyComposedNameIsUnchanged` | `"élodie"` returns itself — the pair is what shows normalisation is applied, not merely survived |
| `aBlankInputIsRefused` | `""` and `"   "` both return `null` |
| `thirtyTwoCodePointsAreAccepted` | a 32-character name returns itself |
| `thirtyThreeCodePointsAreRefused` | a 33-character name returns `null` |
| `astralCharactersCountAsOneEach` | 17 × `U+1D504` (34 UTF-16 units) is **accepted**, and 33 × `U+1D504` is refused — `length` would reject the first |
| `theBoundIsMeasuredAfterNormalising` | an input of 33 code points that composes to 32 is **accepted**, and its result has 32 code points |

## Acceptance criteria

- [ ] All nine tests above pass
- [ ] `astralCharactersCountAsOneEach` fails if the implementation uses `String.length`, and the PR
      says the author ran it that way once to check
- [ ] `theBoundIsMeasuredAfterNormalising` fails if trim/normalise/measure are reordered
- [ ] The function is pure: `DisplayName.kt` imports nothing from `db`, `session`, or any logging or
      I/O package
- [ ] `ktlintCheck` passes for `:poker-server`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
