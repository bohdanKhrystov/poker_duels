---
id: STORY-0107
title: Duel format and match progression
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, rules, duel]
depends_on: [STORY-0106]
---

## Goal

Hands become a duel: equal starting stacks, a rising blind schedule, an alternating button, and
an end condition that produces one winner and one duel coin.

## Why

This is the story that turns a poker implementation into *this* product. It is also the layer
that guarantees a duel terminates — without a rising blind schedule, a heads-up freezeout can
run indefinitely.

## Design notes

- The format is **configuration**, not code. `DuelFormat` carries the starting stack, the blind
  schedule and the end condition. [`DEC-001`](../../docs/duel-rules.md) is still open, and
  changing the answer must never require an engine change. Any task here that hardcodes 50/100
  or "ten hands per level" is wrong.
- The default format is a freezeout: play until one player holds every chip.
- The alternative under consideration — a fixed-length match decided on chip count — must be
  expressible in the same `DuelFormat` type. If it is not, the type is wrong. This is the
  concrete test of whether the abstraction actually holds.
- Match state is separate from hand state. `GameState` describes one hand; `MatchState` holds
  the format, the hand number, the blind level, stacks between hands and the button.
- Blind levels advance on hand boundaries, never mid-hand.

> ### ⚠ Open decision — DEC-005
>
> *Where* a match-level event lives is not settled. `GameEvent.sequence` is a position within a
> hand and `StateProjection` is exhaustive over `GameEvent` with no `else`, so a `MatchFinished`
> either rides on the final hand's log as a no-op for the projection, or starts a `MatchEvent`
> hierarchy of its own, or does not exist and the match layer returns `DuelOutcome` alone. The
> answer binds `STORY-0108` and `EPIC-02`, so it is not a ticket-level choice. Every other ticket
> in this story ships regardless — the duel plays, terminates and declares a winner as a value —
> and `TASK-010717` alone is blocked until this is decided, and is re-split afterwards.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010704](../tasks/TASK-010704-blind-level.md) | A blind level that carries a small and big blind and can double | ready |
| [TASK-010705](../tasks/TASK-010705-blind-schedule.md) | A blind schedule that answers which blinds a hand number plays | backlog |
| [TASK-010706](../tasks/TASK-010706-end-condition.md) | The two duel end conditions as a sealed type | ready |
| [TASK-010707](../tasks/TASK-010707-duel-format.md) | DuelFormat and the default freezeout from the rules document | backlog |
| [TASK-010708](../tasks/TASK-010708-match-state.md) | MatchState, what survives between two hands | backlog |
| [TASK-010709](../tasks/TASK-010709-start-next-hand.md) | Deal the match's next hand at its scheduled blinds | backlog |
| [TASK-010710](../tasks/TASK-010710-record-hand.md) | Fold a finished hand back into the match and pass the button | backlog |
| [TASK-010711](../tasks/TASK-010711-duel-outcome.md) | DuelOutcome, the result of a finished duel | ready |
| [TASK-010712](../tasks/TASK-010712-evaluate-the-end-condition.md) | Decide whether a match is over, and who won it | backlog |
| [TASK-010713](../tasks/TASK-010713-random-duel-harness.md) | Play a whole duel from one seed, and prove it produces a winner | backlog |
| [TASK-010714](../tasks/TASK-010714-duel-invariants.md) | Button, blinds and chips across a whole duel | backlog |
| [TASK-010715](../tasks/TASK-010715-duel-termination-property.md) | Every default duel terminates, well inside an asserted ceiling | backlog |
| [TASK-010716](../tasks/TASK-010716-fixed-length-duel.md) | A fixed-length duel plays and is decided on chips | backlog |
| [TASK-010717](../tasks/TASK-010717-match-finished-event.md) | The end of a match as a durable event | blocked (DEC-005) |

## Acceptance criteria

- [ ] A complete duel plays end to end in a test and declares exactly one winner.
- [ ] The button alternates every hand, and blind levels rise only between hands.
- [ ] Both the freezeout and the fixed-length format are expressible in `DuelFormat` and both
      are covered by tests.
- [ ] Every duel under the default format terminates — asserted over a large number of
      simulated matches with a hand-count ceiling that must never be reached.
- [ ] The winner, the hand count and the final stacks are one value — `DuelOutcome`. Whether that
      value is also an event waits on `DEC-005` (`TASK-010717`).
- [ ] Total chips in the match are constant from the first deal to the last.

## Out of scope

- Rating, ladder position, or what a duel coin is worth — EPIC-05.
- Rematch and challenge flow — EPIC-02.
- Persisting a match — EPIC-02.
