---
id: TASK-010404
title: PokerEngine interface and contract test suite
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: M
labels: [engine, contract, test]
depends_on: [TASK-010403]
---

## Goal

The interface itself, plus the reusable test suite that any implementation must pass — including
the one assertion that keeps this architecture from rotting.

## Context

- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md)
  — in particular the "given up" section, which names the risk this ticket exists to close.

## Scope

- `PokerEngine` with the single `handle(state, action): EngineResult` method.
- A `StateProjection` that folds events over a state, used only by tests and by replay.
- `PokerEngineContract` — an abstract test class that any implementation extends. It asserts:
  - **`fold(oldState, result.events) == result.newState`, on every single call.** This is the
    load-bearing assertion. `newState` and `events` are two descriptions of one transition, and
    without this check they drift apart invisibly until a replay disagrees with a live game.
  - a rejected action returns the input state unchanged and no events;
  - the same `(state, action)` always yields an identical result — purity;
  - events are sequentially numbered with no gaps.
- `NoOpEngine`, which rejects everything, purely so the contract suite has something to run
  against before any rules exist.

## Out of scope

- Implementing any rule — STORY-0105 onward. An engine that rejects every action satisfies this
  ticket completely.
- Performance.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/PokerEngine.kt` | create |
| `poker-engine/src/main/kotlin/.../game/StateProjection.kt` | create |
| `poker-engine/src/test/kotlin/.../game/PokerEngineContract.kt` | create |
| `poker-engine/src/test/kotlin/.../game/NoOpEngineTest.kt` | create |

## Acceptance criteria

- [ ] The fold assertion runs on every `handle` call made anywhere in the contract suite, not
      only in a dedicated test.
- [ ] A deliberately broken engine — one that returns a `newState` inconsistent with its events —
      fails the suite. Demonstrate this, then remove the deliberate break.
- [ ] Purity holds: repeated identical calls give identical results.
- [ ] Rejections leave the state untouched and emit nothing.
- [ ] `PokerEngineContract` is extensible by later implementations with no modification.

## Tests

- `PokerEngineContract` — the reusable suite.
- `NoOpEngineTest` — extends it, proving the suite runs.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
