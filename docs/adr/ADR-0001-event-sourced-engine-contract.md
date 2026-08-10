# ADR-0001 — Event-sourced engine contract

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

The engine is consumed by a server, a CLI, bots, a simulation harness, a replay viewer and a
test suite. Its shape is the most consequential decision in the project, because everything
else is built on top of it and it is the hardest thing to change later.

Three shapes were on the table.

**Mutable engine.** `engine.apply(action)`, state held inside. Familiar and simple, but the
engine becomes a thing you must clone to simulate, history is lost unless separately recorded,
and tests need setup and teardown rather than plain values.

**Pure reducer.** `newState = reduce(state, action)`. Clean and easy to test, but it throws
away *what happened* — the difference between two states has to be reverse-engineered to
produce a replay, a broadcast message, or a statistic.

**Event sourcing.** The engine emits events; state is folded from them. Everything downstream
becomes a projection. The cost is that reconstructing state on every action is wasteful, and a
strict event-sourced design puts that cost on the hot path.

## Decision

The engine is a **pure function that returns both the next state and the events that produced
it**:

```kotlin
interface PokerEngine {
    fun handle(state: GameState, action: PlayerAction): EngineResult
}

data class EngineResult(
    val newState: GameState,
    val events: List<GameEvent>,
    val rejection: Rejection? = null,
)
```

- `GameState` is immutable. `data class`, `val` throughout, no `var` anywhere in the domain.
- Randomness enters only through an injected `Rng` carried in the state. The engine never calls
  a global random source or reads a clock.
- An illegal action neither throws nor mutates: it returns `rejection` set and `events` empty.
- The server keeps the current `GameState` in memory and appends `events` to a durable log.
  It does **not** replay the log to compute the next state.

Consumers other than the live game read the event log and fold it into whatever shape they
need: a redacted per-player view, a statistics summary, a replay timeline, training data.

## Consequences

**Gained**

- The engine is trivially testable: two values in, one value out, no fixtures.
- Replay, undo, spectating, audit and post-match analysis all come from one mechanism.
- Thousands of games can be simulated in parallel with no cloning and no shared state.
- A bug is reproducible from `(seed, actions)` — a bug report is two lines of data.
- Per-player redaction happens in one place, as a projection, rather than scattered through
  the transport layer.

**Given up**

- Two things must be kept consistent: the state transition and the events describing it. A
  contract test asserts that folding `events` over the old state reproduces `newState` exactly,
  on every engine test. Without that test this design rots quietly.
- Events are permanent API. Renaming one breaks stored logs, so they need versioning
  discipline from the first day.
- Slightly more code than a plain reducer.

**Foreclosed**

- The engine can never acquire hidden internal state. Anything it needs to remember must live
  in `GameState`, which is the point.

## Alternatives considered

- **Mutable engine** — rejected: hostile to simulation, replay and parallel tests.
- **Pure reducer without events** — rejected: replay, broadcast and analytics would each have
  to reconstruct what happened by diffing states.
- **Strict event sourcing with no `newState`** — rejected: it forces a fold on every single
  action for no benefit, since the server has the state in hand already. The event log remains
  the source of truth for everything *except* the live game.
