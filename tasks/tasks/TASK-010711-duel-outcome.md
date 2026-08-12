---
schema: 2
id: TASK-010711
title: DuelOutcome, the result of a finished duel
type: task
status: ready
parent: STORY-0107
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [engine, duel]
depends_on: [TASK-010616]
verify:
  - ./gradlew :poker-engine:test --tests '*DuelOutcomeTest'
  - ./gradlew :poker-engine:check
---

## Goal

The result of a duel is one value: who won, over how many hands, with which final stacks — and it
can say "nobody" without lying.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/DuelOutcomeTest.kt` | create |

Read `docs/vision.md` (what a duel coin is for) and `docs/duel-rules.md` Part 2. Modify neither.

## Scope

- `public data class DuelOutcome(val winner: Int?, val handsPlayed: Int, val finalStacks: List<Int>)`.
- `init` requires, each with a message naming the offending value: `winner == null || winner in 0..1`,
  exactly two `finalStacks`, no negative stack, `handsPlayed >= 0`.
- `public val isDraw: Boolean get() = winner == null`.
- KDoc explains why `winner` is nullable: a fixed-length duel can finish level, and the engine
  reports that rather than picking a seat arbitrarily. It also says the engine names a seat, never
  a player or an account — that lives in `EPIC-05`.

## Out of scope

- Deciding when a match has an outcome — `TASK-010712` adds `outcomeOf` to this same file.
- Any event carrying this value — blocked on `DEC-005`, `TASK-010717`.
- Duel coins, rating, ladder position — `EPIC-05`.

## Tests

`DuelOutcomeTest`

| Test | Proves |
| --- | --- |
| `carriesTheWinnerHandCountAndFinalStacks` | `DuelOutcome(1, 34, listOf(0, 20_000))` exposes all three, and `isDraw` is false |
| `aDrawHasNoWinner` | `DuelOutcome(null, 25, listOf(10_000, 10_000)).isDraw` is true |
| `rejectsAMalformedOutcome` | `winner = 2`, a single-element `finalStacks`, a negative stack and `handsPlayed = -1` each throw `IllegalArgumentException` |

## Acceptance criteria

- [ ] `DuelOutcomeTest.carriesTheWinnerHandCountAndFinalStacks` passes
- [ ] `DuelOutcomeTest.aDrawHasNoWinner` passes
- [ ] `DuelOutcomeTest.rejectsAMalformedOutcome` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
