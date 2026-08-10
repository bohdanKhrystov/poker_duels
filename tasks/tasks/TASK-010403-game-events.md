---
id: TASK-010403
title: GameEvent hierarchy and EngineResult
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: M
labels: [engine, domain, contract]
depends_on: [TASK-010402]
---

## Goal

The durable vocabulary of everything that can happen in a hand, and the result type the engine
returns.

## Context

- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
  — events are permanent API; treat this ticket accordingly.

## Scope

- `GameEvent` sealed hierarchy, at minimum:
  `HandStarted`, `BlindPosted`, `HoleCardsDealt`, `ActionOn`, `PlayerFolded`, `PlayerChecked`,
  `PlayerCalled`, `PlayerBet`, `PlayerRaised`, `PlayerAllIn`, `StreetDealt`, `BettingRoundEnded`,
  `ShowdownReached`, `HandRevealed`, `PotAwarded`, `UncalledBetReturned`, `HandFinished`.
- Every event carries a schema `version` from the first commit, and a sequence number within the
  hand.
- Events are **facts, not instructions**. `PlayerBet(seat, amount)` — never `ShowBetAnimation`.
  A consumer that cannot be written against facts alone is asking for the wrong event.
- Events carrying hole cards exist only for the recipient who is entitled to see them;
  `HoleCardsDealt` is per-seat, so a broadcast layer can filter by seat rather than by field.
- `EngineResult(newState, events, rejection)` as specified in the ADR.

## Out of scope

- Emitting these events — STORY-0105 onward.
- Serialization — `TASK-010801`. The version field is introduced here so that it exists before
  any log does.
- Match-level events such as `MatchFinished` — STORY-0107.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/GameEvent.kt` | create |
| `poker-engine/src/main/kotlin/.../game/EngineResult.kt` | create |
| `poker-engine/src/test/kotlin/.../game/GameEventTest.kt` | create |

## Acceptance criteria

- [ ] `GameEvent` is sealed; an exhaustive `when` compiles without an `else`.
- [ ] Every event carries a schema version and a within-hand sequence number.
- [ ] No event name or field describes presentation rather than fact.
- [ ] `HoleCardsDealt` is addressed to a single seat.
- [ ] `EngineResult` has an empty event list whenever `rejection` is set.
- [ ] Every event type appears in at least one test.

## Tests

- `GameEventTest` — exhaustiveness, versioning, the rejection/empty-events invariant.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
