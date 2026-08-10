# Poker Duels

> Ranked heads-up poker duels. Win the duel, not the pot.

A two-player Texas Hold'em duel game. No money, no chips to buy, no casino styling — you
challenge someone, you play a heads-up match, the winner takes one **duel coin** and moves on
the leaderboard.

This repository also documents **how** it is being built: a single developer running an
orchestrated set of Claude Code agents against a ticketed backlog. The process is a deliverable
in its own right — see `docs/workflow.md`.

## Status

Pre-alpha. Nothing is playable yet. The backlog starts at
[`tasks/BOARD.md`](tasks/BOARD.md).

## Shape of the system

```
                        React web client
                               │
                          WebSocket
                               │
                        Ktor duel server        ← authoritative
                               │
                    ┌──── poker-engine ────┐    ← pure Kotlin, depends on nothing
                    │          │           │
                   CLI    poker-ai    poker-analysis
```

The engine is a library that knows only the rules of poker. It has no idea whether it is
running inside a server, a terminal, a test, or a million-hand simulation.

## Documentation

| Document | Contents |
| --- | --- |
| [`docs/vision.md`](docs/vision.md) | What this is, who it's for, what it is not |
| [`docs/architecture.md`](docs/architecture.md) | Modules, dependency rules, data flow |
| [`docs/duel-rules.md`](docs/duel-rules.md) | Poker rules and the duel format |
| [`docs/workflow.md`](docs/workflow.md) | The agent development loop |
| [`docs/adr/`](docs/adr/) | Architecture decision records |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Branching, PRs, commits |
| [`tasks/README.md`](tasks/README.md) | The ticket system |
