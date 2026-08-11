---
schema: 2
id: TASK-010201
title: Rank and Suit enums with poker values and notation symbols
type: task
status: done
parent: STORY-0102
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [engine, domain]
depends_on: [TASK-010102]
verify:
  - ./gradlew :poker-engine:test --tests '*RankSuitTest'
  - ./gradlew :poker-engine:check
---

## Goal

The two enums every other card type is built from exist, each carrying its poker value and its
one-character notation symbol.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Rank.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/card/Suit.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/card/RankSuitTest.kt` | create |

This is the first production code in the module, so `src/main/kotlin/` does not exist yet.
Package is `duels.poker.engine.card`.

## Scope

- `public enum class Rank(public val value: Int, public val symbol: Char)`, declared in
  ascending order: `TWO(2, '2')`, `THREE(3, '3')`, `FOUR`, `FIVE`, `SIX`, `SEVEN`, `EIGHT`,
  `NINE(9, '9')`, `TEN(10, 'T')`, `JACK(11, 'J')`, `QUEEN(12, 'Q')`, `KING(13, 'K')`,
  `ACE(14, 'A')`.
- `public enum class Suit(public val symbol: Char)`, declared in this order:
  `CLUBS('c')`, `DIAMONDS('d')`, `HEARTS('h')`, `SPADES('s')`.
- KDoc on both, stating that **declaration order is contractual**: `Card`'s integer encoding and
  every recorded shuffle are derived from these ordinals, so reordering them invalidates stored
  replays.
- Explicit `public` visibility, per the project style.

## Out of scope

- `Card` — `TASK-010202`. Do not create it here.
- `toString`, parsing, or any lookup from a symbol back to a `Rank`/`Suit` — `TASK-010203`,
  which must not need to reopen these two files.
- Making `Rank` `Comparable` or encoding the wheel's low ace. Straights belong to the evaluator,
  `STORY-0103`.
- Unicode suit glyphs. Those are a client concern.

## Tests

`RankSuitTest`

| Test | Proves |
| --- | --- |
| `ranksAscendFromTwoToAce` | `Rank.entries` has 13 entries and their `value`s are exactly `2..14` in declaration order |
| `rankSymbolsAreStandardPokerNotation` | `Rank.entries.map { it.symbol }.joinToString("") == "23456789TJQKA"` |
| `suitsAreClubsDiamondsHeartsSpades` | `Suit.entries` has 4 entries and `Suit.entries.map { it.symbol }.joinToString("") == "cdhs"` |

## Acceptance criteria

- [ ] `RankSuitTest.ranksAscendFromTwoToAce` passes
- [ ] `RankSuitTest.rankSymbolsAreStandardPokerNotation` passes
- [ ] `RankSuitTest.suitsAreClubsDiamondsHeartsSpades` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
