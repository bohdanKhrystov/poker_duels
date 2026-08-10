---
id: TASK-010803
title: Headless simulation harness and invariant fuzzing
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: M
labels: [engine, test, simulation]
depends_on: [TASK-010802]
---

## Goal

Play a hundred thousand duels with no UI, checking every rule after every action — the cheapest
bug-finding tool this project will ever have.

## Context

- [`tasks/stories/STORY-0108-event-log-replay-simulation.md`](../stories/STORY-0108-event-log-replay-simulation.md).
- [`tasks/tasks/TASK-010504-betting-property-tests.md`](TASK-010504-betting-property-tests.md) —
  the same idea at hand scope; reuse its invariant checks rather than rewriting them.

## Scope

- A `Bot` interface: given a `PlayerView` and `LegalActions`, choose an action.
- `RandomBot` — uniform over legal actions. Deliberately bad, and excellent at reaching states a
  reasonable player never would.
- `SimulationRunner(bots, format, count, seed)` returning aggregate results.
- Invariants checked after every action across the entire run: chip conservation, no duplicate
  cards in play, action always on a seat that can act, no negative stack, bounded hand length.
- On any violation: stop, and report the seed and action sequence that reproduces it.
- A summary: hands played, matches completed, average hands per match, category frequencies at
  showdown as a sanity check on the evaluator.

## Out of scope

- Bots that play well — EPIC-09.
- Multithreading. Correct and single-threaded first; the harness is already fast enough for the
  numbers here.
- A CLI front end — that is the `poker-cli` module.

## Files

| File | Action |
| --- | --- |
| `.../sim/Bot.kt` | create |
| `.../sim/RandomBot.kt` | create |
| `.../sim/SimulationRunner.kt` | create |
| `.../sim/SimulationTest.kt` | create |

## Acceptance criteria

- [ ] 100 000 duels between random bots complete with no invariant violation and no crash.
- [ ] Every invariant listed is checked after every action.
- [ ] A violation reports a reproducing `(seed, actions)` pair.
- [ ] The run is deterministic: the same seed gives the same aggregate results.
- [ ] Showdown category frequencies are close to the published probabilities.
- [ ] A short run — 1 000 duels — is part of the normal test suite; the full run is tagged for
      on-demand execution.

## Tests

- `SimulationTest` — the short run, plus determinism of aggregate results.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
