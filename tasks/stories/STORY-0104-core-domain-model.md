---
id: STORY-0104
title: Core domain — state, actions, events
type: story
status: ready
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

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010401](../tasks/TASK-010401-game-state.md) | GameState and its sub-models | ready |
| [TASK-010402](../tasks/TASK-010402-player-actions.md) | PlayerAction hierarchy and legality descriptor | backlog |
| [TASK-010403](../tasks/TASK-010403-game-events.md) | GameEvent hierarchy and EngineResult | backlog |
| [TASK-010404](../tasks/TASK-010404-engine-contract-tests.md) | PokerEngine interface and contract test suite | backlog |

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
