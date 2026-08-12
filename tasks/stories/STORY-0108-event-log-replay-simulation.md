---
id: STORY-0108
title: Event log, replay and simulation
type: story
status: backlog
parent: EPIC-01
module: poker-engine
labels: [engine, replay, simulation]
depends_on: [STORY-0106]
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
  engine rather than the other way round. This is `DEC-006` below, not a ticket-level choice, and
  it ends in an ADR either way.
- The log is append-only and versioned. Reading a log written by an older schema version must
  either work or fail loudly — never silently misinterpret.
- Replay determinism means the seed is part of the log. Given `(seed, ordered actions)` the
  entire match is reproducible, including every card.
- The simulation harness takes a bot interface and a count. The only bot needed here is one
  that picks uniformly among legal actions — deliberately stupid, and excellent at finding
  states a sensible player would never reach. It lives in `poker-ai`, which
  [`docs/architecture.md`](../../docs/architecture.md) already names as the home of "bots and the
  simulation harness".
- **The story splits in two along the hand/match line.** A hand log, replay of one hand and the
  replay identity property need only the `GameEvent` types that exist today, and start now. A
  match log, match replay and a harness that plays *duels* need the match types
  [`STORY-0107`](STORY-0107-duel-format-and-match.md) introduces, and wait for it — those tickets
  carry the dependency in prose rather than in `depends_on`, because that story is split
  separately and its task ids are not this story's to name.

> ### ⚠ Open decision — DEC-006
>
> *Where* serialisation lives, and in what format, is not settled. `poker-engine` may take no
> dependency, so either the format is hand-written inside the engine, or a new module that
> depends on the engine owns it and is free to use kotlinx.serialization — a module the
> architecture's module list does not currently contain — or the format waits for `EPIC-02` to
> decide how a match is stored. The answer binds `EPIC-02` and `EPIC-03`, so it is not a
> ticket-level choice. Everything else in this story ships regardless: the log is a value, and
> replaying it does not require having written it to disk. `TASK-010810` alone is blocked until
> this is decided, and is re-split afterwards.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010804](../tasks/TASK-010804-hand-log.md) | HandLog, the replayable record of one hand | ready |
| [TASK-010805](../tasks/TASK-010805-replay-a-hand.md) | Replay a hand from its log | backlog |
| [TASK-010806](../tasks/TASK-010806-replay-divergence.md) | Replay rejects a log that does not match the engine | backlog |
| [TASK-010807](../tasks/TASK-010807-replay-identity-property.md) | Record and replay is an identity over two hundred random hands | backlog |
| [TASK-010808](../tasks/TASK-010808-poker-ai-module.md) | The poker-ai module, where bots and the harness live | backlog |
| [TASK-010809](../tasks/TASK-010809-random-bot.md) | Bot, and a RandomBot that picks uniformly among legal actions | backlog |
| [TASK-010810](../tasks/TASK-010810-hand-log-serialization.md) | Serialise a hand log and read it back | blocked (DEC-006) |
| [TASK-010811](../tasks/TASK-010811-match-log-and-replay.md) | The log of a whole duel, and replaying it | blocked (STORY-0107) |
| [TASK-010812](../tasks/TASK-010812-simulation-harness.md) | Headless simulation harness and invariant fuzzing | blocked (STORY-0107) |

## Acceptance criteria

- [ ] Replaying `(seed, actions)` reproduces every state and every event of a **hand** exactly,
      deck and rng included, asserted over two hundred generated hands.
- [ ] A log whose events do not match what the engine regenerates fails, naming the event index.
- [ ] A match log round-trips through serialization unchanged. *(waits on `DEC-006`)*
- [ ] Replaying `(seed, actions)` reproduces every state and every event of a **match** exactly,
      asserted over generated matches. *(waits on `STORY-0107`)*
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
