---
id: TASK-010202
title: Parse and format cards in poker notation
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: S
labels: [engine, domain]
depends_on: [TASK-010201]
---

## Goal

`Card.parse("As")` and `card.toString()` are exact inverses over the 52 valid strings, so tests
and logs can be written in notation a poker player reads without effort.

## Context

- [`tasks/tasks/TASK-010201-card-rank-suit.md`](TASK-010201-card-rank-suit.md) — the types being
  extended.

## Scope

- `toString()` on `Card` producing two characters: rank then suit, e.g. `As`, `Th`, `2c`, `Kd`.
- Rank characters: `2 3 4 5 6 7 8 9 T J Q K A`. Suit characters: `c d h s`, lower case.
- `Card.parse(String): Card` — strict. Rank character is case-insensitive, suit is not, length
  must be exactly 2.
- `Card.parseOrNull(String): Card?` for callers that expect failure.
- A `cards("As Kd 7h")` test helper that parses a whitespace-separated list. It lives in test
  sources, not production.

## Out of scope

- Unicode suit symbols. They are for the UI and belong in the client.
- Parsing hands, boards or ranges — later stories.
- Localisation. Poker notation is poker notation.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../card/Card.kt` | modify |
| `poker-engine/src/test/kotlin/.../card/CardNotationTest.kt` | create |
| `poker-engine/src/test/kotlin/.../card/CardsHelper.kt` | create |

## Acceptance criteria

- [ ] `Card.parse(c.toString()) == c` for all 52 cards.
- [ ] `"10s"`, `""`, `"Xs"`, `"AS"`, `"As "`, `"sA"` are all rejected.
- [ ] `"as"` parses as the ace of spades — rank is case-insensitive.
- [ ] `parse` throws with a message naming the offending input; `parseOrNull` returns null.
- [ ] `cards("As Kd")` yields the two expected cards, and rejects a duplicate.

## Tests

- `CardNotationTest` — exhaustive round trip over all 52 cards, plus a table of rejected inputs.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
