---
schema: 2
id: TASK-020602
title: A RoomCode value type that only accepts a human-typable code
type: task
status: backlog
parent: STORY-0206
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [server, rooms]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*RoomCodeTest'
  - ./gradlew :poker-server:check
---

## Goal

A room code is a type with a shape, not a `String`: eight characters from one unambiguous
alphabet, parsed leniently from what a human types and stored strictly.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomCode.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomCodeTest.kt` | create |

## Scope

- New package `duels.poker.server.room`. One file, KDoc on everything public:

  ```kotlin
  @JvmInline
  public value class RoomCode(public val value: String) {
      init { require(...) { "..." } }

      public companion object {
          public const val LENGTH: Int = 8
          public const val ALPHABET: String = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
          public fun parse(raw: String): RoomCode?
      }
  }
  ```

- The alphabet is Crockford base32: the digits plus the uppercase letters **excluding `I`, `L`,
  `O` and `U`**, so a code read aloud or typed from a link cannot be misheard as another code.
  Length 8 over 32 symbols is 40 bits — `TASK-020603` mints it, and the KDoc must state that
  number, because it is the whole answer to "can a stranger guess their way into a duel".
- `init` rejects anything that is not exactly `LENGTH` characters all drawn from `ALPHABET`,
  with `IllegalArgumentException`. Lowercase is **not** accepted by the constructor.
- `parse` is the lenient front door: it trims, uppercases, strips `-` and spaces, and returns
  `null` — never throws — when the result is still not a valid code.
- No `Random`, no clock, no engine import in this file.

## Out of scope

- Minting codes — `TASK-020603`.
- Any link or URL format built around a code — `EPIC-03`.
- Rate-limiting join attempts, or any other answer to code guessing beyond entropy — `DEC-012`.

## Tests

`RoomCodeTest`, JUnit 5, package `duels.poker.server.room`.

| Test | Proves |
| --- | --- |
| `acceptsAWellFormedCode` | `RoomCode("2B7KMNPQ").value == "2B7KMNPQ"` |
| `rejectsACodeOfTheWrongLength` | `RoomCode("2B7KMNP")` and `RoomCode("2B7KMNPQR")` both throw `IllegalArgumentException` |
| `rejectsALetterOutsideTheAlphabet` | each of `"IBCDEFGH"`, `"LBCDEFGH"`, `"OBCDEFGH"`, `"UBCDEFGH"` throws |
| `rejectsALowercaseCode` | `RoomCode("2b7kmnpq")` throws |
| `parseUppercasesAndStripsFormatting` | `RoomCode.parse(" 2b7k-mnpq ")` equals `RoomCode("2B7KMNPQ")` |
| `parseReturnsNullForRubbish` | `RoomCode.parse("")`, `RoomCode.parse("hello")` and `RoomCode.parse("IIIIIIII")` are all `null` |
| `theAlphabetHasThirtyTwoUnambiguousSymbols` | `ALPHABET.length == 32`, its characters are distinct, and none of `I`, `L`, `O`, `U` appears |

## Acceptance criteria

- [ ] `RoomCodeTest.acceptsAWellFormedCode` passes
- [ ] `RoomCodeTest.rejectsACodeOfTheWrongLength` passes
- [ ] `RoomCodeTest.rejectsALetterOutsideTheAlphabet` passes
- [ ] `RoomCodeTest.rejectsALowercaseCode` passes
- [ ] `RoomCodeTest.parseUppercasesAndStripsFormatting` passes
- [ ] `RoomCodeTest.parseReturnsNullForRubbish` passes
- [ ] `RoomCodeTest.theAlphabetHasThirtyTwoUnambiguousSymbols` passes
- [ ] `RoomCode.parse` has no `throw` on any path
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
