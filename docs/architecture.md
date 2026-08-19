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

## Time in the server

The engine has no clock at all — a timestamp it needs arrives inside an action. `poker-server` has
**two**, they measure different quantities, and neither answers the other's question:

| The question | The instrument | Production | In a test |
| --- | --- | --- | --- |
| *How long since…*, *has this deadline passed?* | `duels.poker.server.time.ServerClock` | `SystemClock` — `System.nanoTime()` | `MutableClock` |
| *What is the date?*, *which month is it?* | `java.time.Clock` | `Clock.systemUTC()` | `Clock.fixed(instant, ZoneOffset.UTC)` |

`ServerClock` is monotonic on purpose: a grace window measured on the wall clock would stretch or
collapse when the host corrects its time. It reports elapsed milliseconds from an **arbitrary**
epoch, so no date is derivable from it — `Instant.ofEpochMilli(clock.nowMillis())` is a defect
wherever it appears, and it yields a date in 1970.

A deadline is sorted by one question: **does it outlive the process?** A grace window, a room
timeout, an in-memory rate-limit window and a sweep period do not, and are counted on `ServerClock`.
An `expires_at` column compared against SQL `now()` does, and is a wall-clock instant — the duration
stays a constant, added to `clock.instant()`.

Anything that is a function of the **calendar** — a season, a row's timestamp, a date compared to
today — takes a `java.time.Clock` as a parameter. No static reads: `Instant.now()`,
`System.currentTimeMillis()`, `LocalDate.now()` and `ZoneId.systemDefault()` in their no-argument
forms appear nowhere in `poker-server/src/main`. See
[`adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md`](adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md),
which exists because a merged ADR once named the monotonic clock as the source of the current
calendar month.

## Deployment (later)

`poker-server` in Docker, PostgreSQL for accounts, matches and event logs, hosted on Fly.io,
Railway or a plain VPS. Not decided yet; not needed before v0.2.
