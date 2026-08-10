---
id: TASK-010301
title: HandCategory and comparable HandRank
type: task
status: backlog
parent: STORY-0103
module: poker-engine
estimate: S
labels: [engine, rules]
depends_on: [TASK-010201]
---

## Goal

A `HandRank` that totally orders every possible poker hand, so a showdown is a comparison rather
than a special case.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — the ranking order and the rule that suits
  never break ties.

## Scope

- `HandCategory` enum, ordered low to high: `HIGH_CARD`, `PAIR`, `TWO_PAIR`, `THREE_OF_A_KIND`,
  `STRAIGHT`, `FLUSH`, `FULL_HOUSE`, `FOUR_OF_A_KIND`, `STRAIGHT_FLUSH`.
- `HandRank`: a `HandCategory` plus tiebreak ranks in descending order of significance,
  implementing `Comparable<HandRank>`.
- Comparison is lexicographic — category first, then each tiebreak in turn.
- A readable `toString`, e.g. `"Two pair, kings and sevens, ace kicker"`. Tests are much easier
  to trust when a failure message is legible.

## Out of scope

- Producing a `HandRank` from cards. This ticket defines the result type and its ordering only;
  evaluation is `TASK-010302`.
- Encoding a rank as a single packed integer. That is a performance concern and belongs with
  `TASK-010305`, where it can be measured rather than guessed at.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../hand/HandCategory.kt` | create |
| `poker-engine/src/main/kotlin/.../hand/HandRank.kt` | create |
| `poker-engine/src/test/kotlin/.../hand/HandRankTest.kt` | create |

## Acceptance criteria

- [ ] Categories order correctly: a straight flush beats four of a kind, and so on down.
- [ ] Within a category, tiebreaks decide: aces full of twos beats kings full of aces.
- [ ] Ranks with identical category and tiebreaks compare equal, and `equals` agrees with
      `compareTo`.
- [ ] Comparison is a total order — antisymmetric and transitive, asserted as a property over
      generated ranks.
- [ ] `toString` names the hand in a form a poker player would recognise.

## Tests

- `HandRankTest` — an ordered table of ranks, asserted pairwise in both directions.
- Property: sorting generated ranks is consistent and transitive.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
