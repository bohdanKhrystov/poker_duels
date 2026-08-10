---
id: TASK-010603
title: Hand completion events and hand history record
type: task
status: backlog
parent: STORY-0106
module: poker-engine
estimate: S
labels: [engine, domain]
depends_on: [TASK-010602]
---

## Goal

A finished hand produces a single self-contained record — everything later analysis will need,
captured while it is free to capture.

## Context

- [`docs/adr/ADR-0005-analysis-behind-an-interface.md`](../../docs/adr/ADR-0005-analysis-behind-an-interface.md)
  — the reason completeness matters more here than it appears to today.

## Scope

- `HandFinished` carrying the winner, the amount won, and whether it ended by fold or showdown.
- `HandHistory`: hand number, blind level, starting stacks, button seat, both hole cards, the
  full board, every action in order, and the outcome.
- Built by folding the hand's events, so it cannot disagree with them.
- `HandHistory` is what `HandAnalyzer` will consume; it must be sufficient to reconstruct every
  decision node without any other input.

## Out of scope

- Analysing anything — EPIC-08.
- Persisting it — `TASK-010801` and EPIC-02.
- Redacting it for a viewer. A `HandHistory` is the complete truth; redaction is a projection.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/HandHistory.kt` | create |
| `poker-engine/src/test/kotlin/.../game/HandHistoryTest.kt` | create |

## Acceptance criteria

- [ ] A `HandHistory` is derived purely by folding events, with no other input.
- [ ] It contains every action in order, both hole cards where they were revealed, and the full
      board as dealt.
- [ ] For a hand ending in a fold, it records the fold and does not invent a showdown.
- [ ] Every decision node in the hand is reconstructible from it.
- [ ] Building a history from generated hands never fails.

## Tests

- `HandHistoryTest` — showdown hand, folded hand, all-in run-out.
- Property: over generated hands, a history is always well-formed and its action list replays to
  the same outcome.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
