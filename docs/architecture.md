# Architecture

## Modules

```
poker-engine/      pure Kotlin. The rules of poker. Depends on nothing.
poker-analysis/    equity, EV, decision quality. Depends on engine.
poker-ai/          bots and the simulation harness. Depends on engine.
poker-cli/         terminal client. Depends on engine.
poker-server/      Ktor. WebSocket transport, rooms, persistence. Depends on engine.
web-client/        React + Tailwind. Talks to the server. Knows no poker rules.
```

### The dependency rule

```
                     web-client
                          │  (WebSocket, JSON)
                          ▼
                    poker-server
                          │
        ┌────────────┬────┴───────┬──────────────┐
        ▼            ▼            ▼              ▼
  poker-engine   poker-cli    poker-ai    poker-analysis
        ▲            │            │              │
        └────────────┴────────────┴──────────────┘
```

**Everything depends on `poker-engine`. `poker-engine` depends on nothing.** Not on Ktor, not
on a serialization library, not on a logging framework, not on coroutines. If a change makes
the engine import something, that change is wrong until an ADR says otherwise.

This is enforced, not merely requested: the engine module declares no implementation
dependencies, and a test asserts it.

## What lives in the engine

Only rules and the vocabulary they need:

```
Card  Rank  Suit  Deck  Rng
GameState  Seat  Pot  Street  Board
PlayerAction  GameEvent  EngineResult
HandRank  HandEvaluator
DuelFormat  MatchState
```

What never lives in the engine:

```
Socket  Http  Json  Logger  Clock  Thread  Coroutine
Sprite  Component  Button  Canvas
Random()  System.currentTimeMillis()  UUID.randomUUID()
```

Time and randomness are inputs, not ambient facts. If the engine needs a shuffled deck it asks
the injected `Rng`; if it needs a timestamp, the caller supplies one in the action.

## The engine contract

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

A pure function. Same `state` + same `action` ⇒ same `EngineResult`, always, on any machine.

It returns **both** the next state and the events that produced it:

- `newState` is what the server needs immediately in order to keep playing.
- `events` are what everything else needs — replay, persistence, analytics, bots, and the
  messages broadcast to clients.

An illegal action does not throw and does not mutate anything. It comes back as a `rejection`
with an empty event list.

Full reasoning: [`adr/ADR-0001-event-sourced-engine-contract.md`](adr/ADR-0001-event-sourced-engine-contract.md).

### Example

```
PlayerAction.Raise(seat = 1, to = 50)
        │
        ▼
   PokerEngine
        │
        ▼
  events:  BetPlaced(seat = 1, amount = 50)
           StackChanged(seat = 1, delta = -50)
           ActionOn(seat = 0)
  newState: GameState(...)
```

## Projections

The event log is the source of truth for everything that is not the live game. Different
consumers fold the same events into different shapes:

| Projection | Produces |
| --- | --- |
| `StateProjection` | the authoritative `GameState` (used for recovery and replay) |
| `PlayerView` | one player's redacted view — opponent hole cards removed |
| `StatsProjection` | VPIP, PFR, aggression, showdown rates |
| `ReplayProjection` | a seekable timeline of a finished match |

`PlayerView` is the security-critical one. Redaction happens here, inside the engine's own
projection code — never by trimming a payload at the transport layer, where it is one careless
field away from leaking.

## Runtime flow

```
browser
   │  PlayerAction over WebSocket
   ▼
poker-server   ── authenticates, resolves the room, checks it is this player's turn
   │
   ▼
poker-engine   ── handle(state, action)
   │
   ├────────────────────► append events to the match log (persistent)
   │
   ▼
new GameState (held in memory per room)
   │
   ▼
PlayerView per recipient  ──►  broadcast over WebSocket
```

The server keeps the current `GameState` in memory rather than replaying the log on every
action — the log exists for durability, audit, replay and analysis, not as the hot path.

## Trust

The client is untrusted, permanently and regardless of stakes. The server decides:

- what cards exist and who holds them,
- whose turn it is,
- whether an action is legal,
- what the pot is,
- who won.

A client message can only ever be *"I intend to take this action"*. Everything else is derived
server-side. See [`adr/ADR-0002-server-authoritative.md`](adr/ADR-0002-server-authoritative.md).

## Determinism

A match is reproducible from `(seed, ordered actions)`. This is not a nice-to-have; it is what
makes the following cheap:

- property-based tests over tens of thousands of random hands,
- replaying a bug report exactly,
- running a million duels overnight to tune bots,
- recovering a room after a server restart.

Every source of nondeterminism is an injected dependency. The rule has no exceptions.

## Deployment (later)

`poker-server` in Docker, PostgreSQL for accounts, matches and event logs, hosted on Fly.io,
Railway or a plain VPS. Not decided yet; not needed before v0.2.
