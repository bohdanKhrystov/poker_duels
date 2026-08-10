---
id: STORY-0108
title: Event log, replay and simulation
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, replay, simulation]
depends_on: [STORY-0107]
---

## Goal

An event log that survives being written to disk and read back, replay that reconstructs a match
exactly, and a headless harness that plays thousands of duels to hunt for rule violations.

## Why

This closes the loop that [`ADR-0001`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
was chosen for. It is also the cheapest bug-finding tool the project will ever have: a fuzzing
run over a hundred thousand generated duels, checking invariants after every action, finds
rule errors that no hand-written test would think to look for.

Replay is a hard prerequisite for the analysis work in EPIC-08 and for training bots in EPIC-09.

## Design notes

- Serialization must not pull a dependency into `poker-engine`. Either the format is
  hand-written in the engine, or the serializer lives in a sibling module that depends on the
  engine rather than the other way round. Decide in the task, record it as an ADR if the second
  route is taken.
- The log is append-only and versioned. Reading a log written by an older schema version must
  either work or fail loudly — never silently misinterpret.
- Replay determinism means the seed is part of the log. Given `(seed, ordered actions)` the
  entire match is reproducible, including every card.
- The simulation harness takes a bot interface and a count. The only bot needed here is one
  that picks uniformly among legal actions — deliberately stupid, and excellent at finding
  states a sensible player would never reach.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010801](../tasks/TASK-010801-event-log-format.md) | Versioned event log format and serialization | backlog |
| [TASK-010802](../tasks/TASK-010802-replay.md) | Replay a match from its log | backlog |
| [TASK-010803](../tasks/TASK-010803-simulation-harness.md) | Headless simulation harness and invariant fuzzing | backlog |

## Acceptance criteria

- [ ] A match log round-trips through serialization unchanged.
- [ ] Replaying `(seed, actions)` reproduces every state and every event exactly, asserted over
      generated matches.
- [ ] A log written under an older schema version is either read correctly or rejected with a
      clear error — never misread.
- [ ] The harness plays 100 000 duels between random bots with no rule violation and no crash.
- [ ] Invariants checked after every action across the whole run: chip conservation, no
      duplicate cards in play, action always on a player who can act, no negative stack.
- [ ] A failing run reports a `(seed, actions)` pair that reproduces the failure exactly.

## Out of scope

- Storing matches in a database — EPIC-02.
- A replay viewer — EPIC-03.
- Bots that play well — EPIC-09.
