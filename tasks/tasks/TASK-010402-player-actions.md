---
id: TASK-010402
title: PlayerAction hierarchy and legality descriptor
type: task
status: backlog
parent: STORY-0104
module: poker-engine
estimate: S
labels: [engine, domain, contract]
depends_on: [TASK-010401]
---

## Goal

A closed set of things a player can attempt, and a way to ask the engine what is currently
allowed — so a client can render exactly the right buttons without knowing any rules.

## Context

- [`docs/duel-rules.md`](../../docs/duel-rules.md) — the legal action table.
- [`docs/adr/ADR-0002-server-authoritative.md`](../../docs/adr/ADR-0002-server-authoritative.md)
  — why the client asks rather than decides.

## Scope

- `PlayerAction` sealed hierarchy: `Fold`, `Check`, `Call`, `Bet(amount)`, `Raise(to)`,
  `AllIn`. Every one carries the acting seat.
- Amounts are **totals, not increments**: `Raise(to = 300)` means the seat's committed total for
  the street becomes 300. Increments are the classic source of ambiguity in poker APIs, and
  the KDoc must state the convention explicitly.
- `LegalActions`: which action types are available, plus `minBet`, `minRaiseTo`, `maxRaiseTo`
  and `callAmount`. Enough for a UI to build every control without a rule of its own.
- `Rejection`: a reason type for an action that is not legal — `NotYourTurn`, `ActionNotAllowed`,
  `AmountTooSmall`, `AmountTooLarge`, `HandComplete` — each carrying the relevant numbers.

## Out of scope

- Computing `LegalActions` from a state. This ticket defines the types; STORY-0105 fills them in.
- Applying an action — STORY-0105.
- Timeouts and auto-fold, which are a server concern.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/.../game/PlayerAction.kt` | create |
| `poker-engine/src/main/kotlin/.../game/LegalActions.kt` | create |
| `poker-engine/src/main/kotlin/.../game/Rejection.kt` | create |
| `poker-engine/src/test/kotlin/.../game/PlayerActionTest.kt` | create |

## Acceptance criteria

- [ ] `PlayerAction` is sealed; an exhaustive `when` compiles without an `else`.
- [ ] Amounts are documented as street totals, and the KDoc gives a worked example.
- [ ] A negative or zero bet amount is rejected at construction.
- [ ] `LegalActions` can express every situation in the rules table, including facing an all-in
      that cannot be covered.
- [ ] Every `Rejection` carries the numbers a client needs to explain itself to a player.

## Tests

- `PlayerActionTest` — construction, invalid amounts, exhaustiveness over the sealed hierarchy.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, status `done`,
`BOARD.md` updated, squash-merged into `develop` by a PR linking this ticket.
