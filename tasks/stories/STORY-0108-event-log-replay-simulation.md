---
id: STORY-0108
title: Event log, replay and simulation
type: story
status: done
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

- Serialization is kotlinx.serialization, inside `poker-engine`, on the domain types themselves:
  [`ADR-0010`](../../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md) answered
  `DEC-006`. `checkNoDependencies` is narrowed to an allowlist of `kotlin-stdlib` and
  `kotlinx-serialization` and still fails on anything else; every other purity clause stands.
- The log is append-only and versioned. Reading a log written by an older schema version must
  either work or fail loudly — never silently misinterpret.
- Replay determinism means the seed is part of the log. Given `(seed, ordered actions)` the
  entire match is reproducible, including every card.
- The simulation harness takes a bot interface and a count. The only bot needed here is one
  that picks uniformly among legal actions — deliberately stupid, and excellent at finding
  states a sensible player would never reach. It lives in `poker-ai`, which
  [`docs/architecture.md`](../../docs/architecture.md) already names as the home of "bots and the
  simulation harness".
- The wire format is the domain types' own: short `@SerialName` discriminators (`"PlayerBet"`,
  `"MatchFinished"`), cards as their notation (`"As"`), and each log carrying its own version
  member, checked before the value is decoded. A rename must not silently change the format.
- Match-level events are their own hierarchy
  ([`ADR-0009`](../../docs/adr/ADR-0009-match-events-are-their-own-hierarchy.md)), so a match log
  holds `HandLog`s plus its own `MatchEvent`s and never a loose `GameEvent`.
- **The story runs as three independent chains** — serialization, the match log and its replay,
  and the simulation harness in `poker-ai` — that only meet at `TASK-010826`. Three tickets are
  startable the moment the story opens.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-010804](../tasks/TASK-010804-hand-log.md) | HandLog, the replayable record of one hand | ready |
| [TASK-010805](../tasks/TASK-010805-replay-a-hand.md) | Replay a hand from its log | backlog |
| [TASK-010806](../tasks/TASK-010806-replay-divergence.md) | Replay rejects a log that does not match the engine | backlog |
| [TASK-010807](../tasks/TASK-010807-replay-identity-property.md) | Record and replay is an identity over two hundred random hands | backlog |
| [TASK-010808](../tasks/TASK-010808-poker-ai-module.md) | The poker-ai module, where bots and the harness live | backlog |
| [TASK-010809](../tasks/TASK-010809-random-bot.md) | Bot, and a RandomBot that picks uniformly among legal actions | backlog |
| [TASK-010813](../tasks/TASK-010813-serialization-dependency.md) | Take the kotlinx.serialization dependency behind a narrowed guard | ready |
| [TASK-010814](../tasks/TASK-010814-card-serializer.md) | A card serialises as its own notation | backlog |
| [TASK-010815](../tasks/TASK-010815-player-action-serializable.md) | PlayerAction is serializable under a short type name | backlog |
| [TASK-010816](../tasks/TASK-010816-concrete-events-serializable.md) | Every betting and dealer event is serializable | backlog |
| [TASK-010817](../tasks/TASK-010817-game-event-hierarchy-serializable.md) | The whole GameEvent hierarchy serialises polymorphically | backlog |
| [TASK-010818](../tasks/TASK-010818-hand-log-serializable.md) | A hand log round-trips through JSON | backlog |
| [TASK-010819](../tasks/TASK-010819-hand-log-codec-and-version.md) | Read and write a hand log, and refuse a version this build does not know | backlog |
| [TASK-010820](../tasks/TASK-010820-match-log.md) | MatchLog, the record of a whole duel | backlog |
| [TASK-010821](../tasks/TASK-010821-logged-duel-player.md) | Play a whole duel and keep its log | backlog |
| [TASK-010822](../tasks/TASK-010822-replay-a-match.md) | Replay a whole duel from its log | backlog |
| [TASK-010823](../tasks/TASK-010823-blind-types-serializable.md) | The blind types are serializable | backlog |
| [TASK-010824](../tasks/TASK-010824-duel-format-serializable.md) | DuelFormat and its end condition are serializable | backlog |
| [TASK-010825](../tasks/TASK-010825-match-event-serializable.md) | DuelOutcome and MatchFinished are serializable | backlog |
| [TASK-010826](../tasks/TASK-010826-match-log-codec.md) | Read and write a match log, version guard included | backlog |
| [TASK-010827](../tasks/TASK-010827-simulation-invariants.md) | The invariants a simulated hand must never break | ready |
| [TASK-010828](../tasks/TASK-010828-duel-simulator.md) | Simulate one duel between two bots, checking after every action | backlog |
| [TASK-010829](../tasks/TASK-010829-simulation-runner.md) | Run a thousand duels and report on them | backlog |
| [TASK-010830](../tasks/TASK-010830-soak-run.md) | A hundred thousand duels, off the default test task | backlog |

**Retired:** `TASK-010810` (blocked on `DEC-006`), `TASK-010811` and `TASK-010812` (both blocked
on `STORY-0107`) were re-split into `TASK-010813`–`TASK-010830` once `ADR-0009`, `ADR-0010` and
the match layer landed. Those three ids are retired, not reused.

## Acceptance criteria

- [ ] Replaying `(seed, actions)` reproduces every state and every event of a **hand** exactly,
      deck and rng included, asserted over two hundred generated hands.
- [ ] A log whose events do not match what the engine regenerates fails, naming the event index.
- [ ] A match log round-trips through serialization unchanged (`TASK-010826`).
- [ ] Replaying `(seed, actions)` reproduces every state and every event of a **match** exactly,
      asserted over generated matches (`TASK-010822`).
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
