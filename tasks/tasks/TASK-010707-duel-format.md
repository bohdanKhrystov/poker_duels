---
schema: 2
id: TASK-010707
title: DuelFormat and the default freezeout from the rules document
type: task
status: backlog
parent: STORY-0107
module: poker-engine
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [engine, duel, config]
depends_on: [TASK-010705, TASK-010706]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelFormatTest'
  - ./gradlew :poker-engine:check
---

## Goal

What a duel *is* — starting stack, blind ladder, ending — is one value, and the default is the
one the rules document describes.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelFormat.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelFormatTest.kt` | create |

Read `poker-engine/src/main/kotlin/duels/poker/engine/duel/BlindSchedule.kt`,
`.../duel/EndCondition.kt` and `docs/duel-rules.md` Part 2 (the defaults table and the blind level
table). Modify none of them.

## Scope

- `public data class DuelFormat(val startingStack: Int, val blinds: BlindSchedule, val endCondition: EndCondition)`.
- `init` requires `startingStack >= blinds.blindsFor(1).bigBlind`, with a message naming both: a
  duel that cannot cover its own first big blind is not a duel.
- `public companion object` holding `public val DEFAULT: DuelFormat` — starting stack `10_000`,
  a `BlindSchedule` of `50/100`, `75/150`, `100/200`, `150/300`, `200/400` with
  `handsPerLevel = 10`, and `EndCondition.Freezeout`. These numbers appear nowhere else in the
  engine; this is the single place they are written down.
- KDoc records that `DEC-001` is open, that `DEFAULT` is what the first playable build ships
  with, and that changing the answer means changing this value and nothing else.

## Out of scope

- Running a match under a format — `TASK-010708` onwards.
- Deciding `DEC-001`.

## Tests

`DuelFormatTest`

| Test | Proves |
| --- | --- |
| `theDefaultMatchesTheRulesDocument` | `DEFAULT.startingStack == 10_000`, `DEFAULT.endCondition == EndCondition.Freezeout`, and `DEFAULT.blinds.blindsFor` gives 50/100, 75/150, 100/200, 150/300 and 200/400 for hands 1, 11, 21, 31 and 41 |
| `theDefaultKeepsDoublingPastTheLastLevel` | `DEFAULT.blinds.blindsFor(51) == BlindLevel(400, 800)` |
| `theFixedLengthAlternativeIsExpressible` | `DuelFormat(DEFAULT.startingStack, DEFAULT.blinds, EndCondition.FixedHands(25))` constructs and holds `FixedHands(25)`, so the alternative under consideration needs no new type |
| `rejectsAStartingStackBelowTheFirstBigBlind` | `DuelFormat(99, DEFAULT.blinds, EndCondition.Freezeout)` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelFormatTest.theDefaultMatchesTheRulesDocument` passes
- [ ] `DuelFormatTest.theDefaultKeepsDoublingPastTheLastLevel` passes
- [ ] `DuelFormatTest.theFixedLengthAlternativeIsExpressible` passes
- [ ] `DuelFormatTest.rejectsAStartingStackBelowTheFirstBigBlind` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
