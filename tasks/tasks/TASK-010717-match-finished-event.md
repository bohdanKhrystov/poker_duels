---
schema: 2
id: TASK-010717
title: The end of a match as a durable event
type: task
status: blocked
parent: STORY-0107
module: poker-engine
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [engine, duel, events, blocked]
depends_on: [TASK-010712]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchFinishedTest'
  - ./gradlew :poker-engine:check
---

## Goal

The fact that a duel ended, and who won it, is on the log — not only in a value the caller has to
remember to keep.

## Blocked on DEC-005

**Do not implement this ticket.** It is registered in
[`docs/adr/README.md`](../../docs/adr/README.md) as `DEC-005` and waits on a human decision:

> Does a match-level event live in the `GameEvent` log, or in a hierarchy of its own?
>
> `GameEvent.sequence` is documented as a position *within a hand*, dense and gap-free, and
> `StateProjection.dispatch` is exhaustive over `GameEvent` with no `else`. A match has no hand to
> sit inside, so every answer costs something:
>
> 1. **`MatchFinished` is a `GameEvent`**, appended after the final hand's `HandFinished` with
>    `sequence == finalState.eventCount`, and `StateProjection` folds it as a no-op. One log, one
>    serializer, one broadcast path — but `GameEvent`'s "position within the hand" contract
>    becomes a half-truth, and the projection grows a branch that deliberately does nothing.
> 2. **A separate `MatchEvent` hierarchy** with its own numbering, kept beside the per-hand logs.
>    `GameEvent` stays honestly hand-scoped and `StateProjection` is untouched — at the cost of a
>    second event type, a second serializer path in `TASK-010801`, and a second broadcast path in
>    `EPIC-02`.
> 3. **No match event at all**: the match layer returns `DuelOutcome` and the server decides what
>    to durably record. Cheapest now, but the winner of a duel is then re-derived by every
>    consumer, and a replay cannot state its own ending.
>
> The answer binds `STORY-0108` (the event log format and replay) and `EPIC-02` (what the server
> broadcasts), which is why it is not a ticket-level choice.

Once answered, this ticket is **re-split** by the splitter: the files below are a sketch of the
shape under answer 1, not a sized unit, and answers 2 and 3 differ by several tickets.

## Files (provisional)

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchFinished.kt` | create |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StateProjection.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/duel/MatchFinishedTest.kt` | create |

## Scope (provisional)

- An event carrying the winner, the hand count and the final stacks — the same three facts
  `DuelOutcome` already holds (`TASK-010711`), which is why the type is not the hard part.
- Under answer 1 only: a branch in `StateProjection.dispatch`, whose exhaustive `when` stops
  compiling the moment a new `GameEvent` subtype exists. `StateProjectionTest`,
  `DefaultPokerEngineContractTest` and `ContractDetectsDriftTest` are then in the blast radius and
  must be budgeted across tickets, three files at a time.
- Under answer 2: nothing under `duels.poker.engine.game` is touched at all.

## Acceptance criteria

- [ ] Not startable. `DEC-005` is answered and this ticket is re-split before any code is written.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
