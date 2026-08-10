---
id: EPIC-01
title: Poker engine
type: epic
status: ready
module: poker-engine
labels: [engine, foundation]
---

## Goal

A pure Kotlin library that knows the rules of heads-up No-Limit Texas Hold'em and nothing else.
Given a game state and a player action, it returns the next state and the events that produced
it. It has no dependencies, performs no I/O, reads no clock, and calls no global random source.

When this epic closes, a full duel can be played end to end in a test — dealt, bet, shown down,
and concluded with a winner — and replayed exactly from a seed and a list of actions.

## Why now

Everything else in the project depends on this and nothing here depends on anything else. It is
also the part where correctness is hardest and mistakes are quietest: a poker engine with a
subtly wrong min-raise rule does not crash, it just produces plausible, wrong games forever.
Building it first, in isolation, with heavy property testing, is the whole reason for the
architecture in [`ADR-0001`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md).

It also unblocks the CLI, which means the game becomes playable — against yourself in a
terminal — long before there is a server or a pixel of UI.

## Scope

- Cards, deck, and deterministic shuffling through an injected `Rng`.
- A seven-card hand evaluator producing comparable ranks.
- The domain model: `GameState`, `PlayerAction`, `GameEvent`, `EngineResult`.
- Betting rules for heads-up play: blinds, action order, legality, min-raise, all-in.
- Street progression, showdown, pot resolution, split pots, uncalled bets.
- The duel/match layer: blind schedule, button alternation, end condition.
- Event log serialization, replay, and a headless simulation harness.

## Out of scope

| Not here | Where |
| --- | --- |
| Networking, rooms, WebSocket protocol | EPIC-02 |
| Any UI | EPIC-03 |
| Accounts, ratings, leaderboard | EPIC-04, EPIC-05 |
| Bot strategy beyond a random baseline | EPIC-09 |
| Equity, EV, decision quality | EPIC-08 |
| Timeouts, disconnects, abandonment | EPIC-02 — the engine only sees complete actions |

## Stories

| ID | Title | Status |
| --- | --- | --- |
| [STORY-0101](../stories/STORY-0101-engine-module-scaffold.md) | Engine module and build scaffold | ready |
| [STORY-0102](../stories/STORY-0102-cards-deck-shuffle.md) | Cards, deck and deterministic shuffle | ready |
| [STORY-0103](../stories/STORY-0103-hand-evaluator.md) | Hand evaluator | ready |
| [STORY-0104](../stories/STORY-0104-core-domain-model.md) | Core domain: state, actions, events | ready |
| [STORY-0105](../stories/STORY-0105-betting-rounds.md) | Betting rounds | backlog |
| [STORY-0106](../stories/STORY-0106-showdown-and-pots.md) | Showdown and pot resolution | backlog |
| [STORY-0107](../stories/STORY-0107-duel-format-and-match.md) | Duel format and match progression | backlog |
| [STORY-0108](../stories/STORY-0108-event-log-replay-simulation.md) | Event log, replay and simulation | backlog |

Stories 0105–0108 stay in `backlog` until the domain model in 0104 is merged; their tasks
depend on types that do not exist yet, and specifying them earlier would mean rewriting them.

## Definition of done

- [ ] Every story is `done`.
- [ ] A test plays a complete duel from deal to a declared winner.
- [ ] Replaying a match from `(seed, actions)` reproduces it exactly, asserted over generated
      matches.
- [ ] Chip conservation holds across 100 000 randomly generated hands.
- [ ] `poker-engine` declares zero implementation dependencies, asserted by a test.
- [ ] `poker-cli` can play a duel in a terminal against a random bot.

## Metrics

Filled in when the epic closes.

| | |
| --- | --- |
| Tasks completed | |
| Accepted on first review | |
| Average review iterations | |
| Test lines / production lines | |
| Tasks re-scoped mid-flight | |
| Manual human edits | |
