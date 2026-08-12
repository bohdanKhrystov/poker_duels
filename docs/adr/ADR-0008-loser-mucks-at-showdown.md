# ADR-0008 — The loser mucks at showdown

- **Status:** Accepted
- **Date:** 2026-08-12
- **Resolves:** `DEC-004`

## Context

A showdown has to decide what the rest of the system is allowed to know. The engine already
holds the line that folded cards appear in no event, anywhere — `CardSecrecyTest` asserts it over
a thousand random hands. The showdown is where that line either holds or quietly breaks, because
it is the one moment the engine has a reason to publish hole cards.

Three answers were live, and the choice is not obvious, because Poker Duels has no money in it.
Without a pot to protect, the usual argument for secrecy weakens, and the analysis board planned
in `EPIC-08` would genuinely be better if every hand were fully known.

The forces:

- **Secrecy is a rule, not a feature.** An opponent's range is the game. A player who learns what
  the loser folded or lost with has learned something the game is built to withhold, and no
  amount of "it is only a duel coin" changes that.
- **The reveal order is information too.** Who shows first is determined by who bet last. That is
  not decoration — it is why a player can win a pot without ever showing.
- **Replay and analysis want everything.** A log that omits the losing hand can never reconstruct
  an equity graph after the fact.

## Decision

At a showdown, the last aggressor shows first, and the losing hand is never revealed.

- The engine emits `HandRevealed` only for hands that are actually shown.
- A mucked hand appears in **no event**, exactly as a folded hand does. Same rule, same test.
- Reveal order follows the betting: the last player to bet or raise on the final street shows
  first. If the final street was checked through, the player first to act shows first.
- A player who wins because everyone else folded shows nothing.

## Consequences

**What it buys.** The secrecy non-negotiable becomes uniform: there is exactly one rule — a hand
that is not shown appears nowhere — rather than one rule for folds and an exception for
showdowns. `CardSecrecyTest` extends to mucked hands without special-casing, and the projection
layer keeps its single filtering point.

**What it costs.** The engine must track the last aggressor through the final betting round to
determine reveal order, which is state it would not otherwise carry past the round. Showdown
becomes order-dependent, so its tests need fixtures per betting shape rather than per hand.

**What it forecloses.** `EPIC-08`'s analysis board cannot show what the loser held, because the
log will not contain it. This is deliberate. If that turns out to be the wrong trade, the fix is a
*post-hand* disclosure written by the server after the hand is settled — never a loosening of what
the engine publishes mid-hand.

## Alternatives considered

**Both hands always shown.** Simplest engine, simplest replay, and the best possible input for an
analysis board. Rejected because it makes the engine the one component that leaks what the rest of
the system spends its effort protecting, and because a habit of showing everything is very hard to
walk back once clients depend on it. The simplicity is real but it is bought with the game's
central rule.

**Showing is a `PlayerAction`.** The most faithful to live play, and the most expressive — a
player choosing to show a bluff is part of poker. Rejected for now on sequencing, not on merit: it
adds a decision point after the betting is over, which means a new action type, new legal-action
states, and a client that must prompt for it. That last part lands in `EPIC-03`, which does not
exist yet. Nothing in this ADR prevents it later — a voluntary show is a strict addition to
"loser mucks", and would supersede this ADR rather than contradict it.
