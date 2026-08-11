---
schema: 2
id: TASK-010203
title: Format and parse cards in standard poker notation
type: task
status: done
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, domain]
depends_on: [TASK-010202]
verify:
  - ./gradlew :poker-engine:test --tests '*CardNotationTest'
  - ./gradlew :poker-engine:check
---

## Goal

`Card.parse("As")` and `card.toString()` are exact inverses over the 52 valid strings, so every
test and log line downstream can be written the way a poker player reads it.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Card.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/CardNotationTest.kt` | create |

`Rank.kt` and `Suit.kt` already carry the `symbol` characters; read them, do not modify them.

## Scope

- `override fun toString(): String` on `Card`, producing rank symbol then suit symbol —
  `As`, `Th`, `2c`, `Kd`.
- `public fun parse(text: String): Card` on the companion. Strict:
  - length exactly 2,
  - the rank character matches a `Rank.symbol` case-insensitively (`a` and `A` both mean ace),
  - the suit character matches a `Suit.symbol` **exactly**, so it must be lower case,
  - otherwise `require`/`IllegalArgumentException` whose message contains the offending input.
- `public fun parseOrNull(text: String): Card?` returning `null` where `parse` throws.
  Implement `parse` in terms of `parseOrNull` so the two can never disagree.
- Resolve symbols by searching `Rank.entries` / `Suit.entries`; do not add lookup tables to
  `Rank.kt` or `Suit.kt`.
- KDoc on `parse` naming the accepted grammar.

## Out of scope

- The `cards("As Kd")` multi-card test helper — `TASK-010204`.
- Unicode suit glyphs (`♠`), ten written as `10`, or any localisation.
- Parsing boards, hands or ranges — later stories.
- Touching `Deck` or anything in `duels.poker.engine.random`.

## Tests

`CardNotationTest`

| Test | Proves |
| --- | --- |
| `formatsEveryCardAsRankThenSuit` | for all 52, `toString()` is the card's rank symbol followed by its suit symbol, plus the four spot checks `As`, `Th`, `2c`, `Kd` |
| `parsesEveryCardItFormats` | `Card.parse(c.toString()) == c` for all 52 cards |
| `parsesTheRankCharacterCaseInsensitively` | `Card.parse("as") == Card.parse("As")` and both are the ace of spades |
| `rejectsMalformedNotation` | each of `""`, `"A"`, `"As "`, `" As"`, `"10s"`, `"Xs"`, `"AS"`, `"sA"`, `"1c"`, `"AsKd"` throws `IllegalArgumentException` whose message contains the input |
| `parseOrNullReturnsNullForMalformedNotation` | the same inputs all return `null` |

## Acceptance criteria

- [ ] `CardNotationTest.formatsEveryCardAsRankThenSuit` passes
- [ ] `CardNotationTest.parsesEveryCardItFormats` passes
- [ ] `CardNotationTest.parsesTheRankCharacterCaseInsensitively` passes
- [ ] `CardNotationTest.rejectsMalformedNotation` passes
- [ ] `CardNotationTest.parseOrNullReturnsNullForMalformedNotation` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
