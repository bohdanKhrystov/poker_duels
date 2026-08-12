---
schema: 2
id: TASK-010615
title: Showdown reveals, reveal order and the muck
type: task
status: blocked
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules, security, blocked]
depends_on: [TASK-010614]
verify:
  - ./gradlew :poker-engine:test --tests '*ShowdownRevealTest'
  - ./gradlew :poker-engine:check
---

## Goal

A showdown emits `HandRevealed` for the hands that are actually shown, in the order the rules
give, and for no hand that is mucked.

## Blocked on DEC-004

**Do not implement this ticket.** It is registered in
[`docs/adr/README.md`](../../docs/adr/README.md) as `DEC-004` and waits on a human decision:

> At a heads-up showdown, who shows? `docs/duel-rules.md` says the loser *may* muck — a
> permission belonging to a player, which the engine has no way to ask for. Three answers, each
> with consequences outside this story:
>
> 1. **The loser mucks by default.** Only the winning hand is ever revealed. Maximum secrecy,
>    nothing new in the public API, and reveal order stops mattering — but a replay can never
>    show what the loser held, and `EPIC-08`'s "who was ahead" analysis loses half its input.
> 2. **Both hands are shown.** Every settled showdown reveals both, in the rules' order. Best for
>    replay and analysis, no new API — but it contradicts the rules document's "may", and a
>    player can never hide a bluff.
> 3. **Showing or mucking is a `PlayerAction`.** Faithful to the rules — and it adds a member to
>    the engine's public action set, a decision point after `ShowdownReached`, entries in
>    `LegalActions`, `rejectionFor` and every transport that carries an action.

Once answered, this ticket is **re-split** by the splitter: the work below is a sketch of the
shape, not a sized unit, and answers 2 and 3 differ by several tickets.

## Files (provisional)

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/Showdown.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/ShowdownRevealTest.kt` | create |

## Scope (provisional)

- `HandRevealed` events sit between `ShowdownReached` and the settlement events, so every test
  that pins a settled showdown's event list — `StreetAdvanceTest`, `AllInRunOutTest`,
  `OpeningRunOutTest`, `HandWalkthroughTest`, `CardSecrecyTest` — is in the blast radius and must
  be split across tickets accordingly, three files at a time.
- Reveal order under answers 2 and 3 needs the river's last aggressor, which `GameState` does not
  carry today. The plausible shape is a field set by any betting event that raises `betToMatch`
  and cleared by `StreetDealt`, so that at showdown it holds the river's aggressor or `null` when
  the river was checked through — in which case the seat out of position shows first. That is a
  ticket of its own, with `BettingProjection`, `DealerProjection` and their tests in scope.
- `CardSecrecyTest` gains the muck case: a hand that is not shown appears in no event, exactly as
  a folded hand does not.

## Acceptance criteria

- [ ] Not startable. `DEC-004` is answered and this ticket is re-split before any code is written.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
