---
id: TASK-010802
title: Replay a match from its log
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: S
labels: [engine, replay]
depends_on: [TASK-010801]
---

## Goal

`(seed, actions)` reproduces a match exactly — every card, every state, every event, on any
machine.

## Context

- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
  — this is the payoff the whole contract was chosen for.

## Scope

- `replay(log): List<GameState>` — re-run the engine over the recorded actions and produce every
  intermediate state.
- Assert during replay that regenerated events match the recorded ones, and fail loudly at the
  first divergence, naming the index. A silent divergence is far worse than a crash.
- `MatchReplay` with the ability to step to any point in the match, which is what a replay
  viewer will need.

## Out of scope

- A replay UI — EPIC-03.
- Replaying a partial or corrupt log beyond reporting where it breaks.

## Files

| File | Action |
| --- | --- |
| `.../MatchReplay.kt` | create |
| `.../MatchReplayTest.kt` | create |

## Acceptance criteria

- [ ] Replaying a recorded match reproduces every state and every event exactly.
- [ ] Replay is deterministic across runs and JVMs.
- [ ] A tampered log — one altered action — is detected, and the error names the divergence
      point.
- [ ] Stepping to hand `n`, action `m` gives the same state as replaying to that point.
- [ ] Over generated matches, record-then-replay is always an identity.

## Tests

- `MatchReplayTest` — identity over generated matches, tamper detection, stepping.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
