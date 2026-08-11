---
id: STORY-0104
title: Core domain — state, actions, events
type: story
status: done
parent: EPIC-01
module: poker-engine
labels: [engine, domain, contract]
depends_on: [STORY-0102]
---

## Goal

The types the whole project is written in terms of: `GameState`, `PlayerAction`, `GameEvent`,
`EngineResult`, and the `PokerEngine` interface itself — plus the contract test suite that
every implementation must pass.

## Why

This is the widest blast radius in the codebase. The server, the client protocol, the event
log, the bots and the analysis layer are all written against these types, and renaming an event
after logs exist is a migration rather than a refactor. It is worth spending disproportionate
care here and specifying the rest of the engine only once these are merged.

## Design notes

- Shapes are fixed by [`ADR-0001`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md).
  This story implements that decision; it does not revisit it.
- `GameState` is a `data class` of `val`s all the way down, including the `Rng` state, so a
  state is a complete and copyable description of a game.
- `PlayerAction` and `GameEvent` are sealed hierarchies. Exhaustive `when` over them is the
  main tool keeping the engine honest as it grows.
- Events are the durable contract. Each carries a schema version from the first commit, so a
  future change is a migration rather than a break.
- Seats are indices `0` and `1`, never "player" and "opponent" — those are relative to a
  viewer and belong in the projection layer, not the state.
- **The contract test is the important deliverable here**, not the types. It asserts that
  folding `events` over the old state reproduces `newState` exactly. Without it, the two halves
  of `EngineResult` drift apart silently, which is the one real risk in this architecture.

## Tasks

The four schema-1 tickets (`TASK-010401`–`TASK-010404`) were re-split into the chain below.
Their IDs are retired, not reused.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010405](../tasks/TASK-010405-street-enum.md) | Street enum with board size and successor | ready |
| [TASK-010406](../tasks/TASK-010406-board-value-type.md) | Board value type that can only hold 0, 3, 4 or 5 cards | backlog |
| [TASK-010407](../tasks/TASK-010407-seat-state.md) | Seat state and its construction invariants | backlog |
| [TASK-010408](../tasks/TASK-010408-seat-chip-transitions.md) | Seat chip transitions — commit, award, collect | backlog |
| [TASK-010409](../tasks/TASK-010409-game-state.md) | GameState fields and construction invariants | backlog |
| [TASK-010410](../tasks/TASK-010410-game-state-derived.md) | GameState derived properties and seat update | backlog |
| [TASK-010411](../tasks/TASK-010411-game-state-test-fixture.md) | handState test fixture for game states | backlog |
| [TASK-010412](../tasks/TASK-010412-player-actions.md) | PlayerAction hierarchy and ActionType | backlog |
| [TASK-010413](../tasks/TASK-010413-rejection-reasons.md) | Rejection reasons for an illegal action | backlog |
| [TASK-010414](../tasks/TASK-010414-legal-actions.md) | LegalActions descriptor | backlog |
| [TASK-010415](../tasks/TASK-010415-game-event-base.md) | GameEvent base and hand lifecycle events | backlog |
| [TASK-010416](../tasks/TASK-010416-betting-events.md) | Betting events | backlog |
| [TASK-010417](../tasks/TASK-010417-dealer-events.md) | Dealer events for street progress and showdown | backlog |
| [TASK-010418](../tasks/TASK-010418-settlement-events.md) | Settlement events — uncalled bet, pot award, hand finished | backlog |
| [TASK-010419](../tasks/TASK-010419-engine-result.md) | EngineResult and the rejection invariant | backlog |
| [TASK-010420](../tasks/TASK-010420-domain-immutability-test.md) | Reflective immutability test over the domain types | backlog |
| [TASK-010421](../tasks/TASK-010421-poker-engine-interface.md) | PokerEngine interface and a no-op implementation | backlog |
| [TASK-010422](../tasks/TASK-010422-betting-projection.md) | Fold betting events into a state | backlog |
| [TASK-010423](../tasks/TASK-010423-dealer-projection.md) | Fold dealer events into a state | backlog |
| [TASK-010424](../tasks/TASK-010424-settlement-projection-tests.md) | Settlement projection tests and chip conservation | backlog |
| [TASK-010425](../tasks/TASK-010425-state-projection.md) | StateProjection — the one entry point that folds events into a state | backlog |
| [TASK-010426](../tasks/TASK-010426-engine-contract-suite.md) | PokerEngineContract — the reusable engine test suite | backlog |
| [TASK-010427](../tasks/TASK-010427-contract-detects-drift.md) | Prove the contract suite catches a drifting engine | backlog |

## Acceptance criteria

- [ ] `GameState` is fully immutable; no `var` and no mutable collection appears in the domain,
      asserted by a test over the public API.
- [ ] `PlayerAction` and `GameEvent` are sealed, and every subtype is covered by a test.
- [ ] `EngineResult` carries `newState`, `events` and an optional `rejection`.
- [ ] A rejected action returns an empty event list and a state identical to the input.
- [ ] The contract suite asserts `fold(oldState, events) == newState` and is reusable by every
      future engine implementation.

## Out of scope

- The rules themselves — STORY-0105 onward. A no-op engine that rejects everything is enough to
  exercise the contract suite here.
- Serialization — STORY-0108.
- Per-player redaction: the `PlayerView` projection is specified here but implemented in
  EPIC-02, where it is first needed.
