# ADR-0005 — Hand analysis sits behind an interface

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

Post-match analysis is one of the most interesting things this product could offer: telling a
player that they lost the duel but made the better decisions is a genuinely different
experience from a results screen.

The strongest version of that feature is GTO-based — showing the equilibrium strategy at a
given node. But writing a solver is a different project entirely. Computing Nash equilibria
over a no-limit hold'em game tree (CFR, CFR+, MCCFR and relatives) is research work that teams
have spent years on, and it dwarfs the game itself in difficulty.

External options were surveyed and none of them is a foundation to build on today:

- **GTO Wizard** has a public API, but it is for benchmarking AI agents against their bot —
  not for querying strategies at a node.
- **PioSolver** is a local desktop application. It can be automated, but it is not a service.
- Various commercial "solver API" offerings exist, are new, and have unclear licensing.

The important insight is that a solver query is a function of a hand history and a node. As
long as the history is captured completely, the analysis can be added at any point in the
future.

## Decision

Analysis is defined by an interface from the start, and **nothing in the product depends on any
particular implementation of it**:

```kotlin
interface HandAnalyzer {
    fun analyze(hand: HandHistory): Analysis
}
```

`HandHistory` is derived from the engine's event log, which is captured from day one and is
complete enough to reconstruct any decision node — every action, every board card, both hole
cards, stack sizes and the blind level.

Implementations arrive in order of cost:

1. `BasicAnalyzer` — pot odds, equity, SPR, outs, all-in EV. Arithmetic, no search. Enough to
   be genuinely useful.
2. A third-party provider adapter, if a suitable service matures.
3. A solver of our own, if it ever justifies itself.

No solver work is in scope for the MVP, and no product surface may assume solver-quality output
exists.

## Consequences

**Gained**

- The analysis feature can start shallow and deepen without any of it becoming a rewrite.
- Nothing is coupled to a young commercial API.
- Capturing complete hand histories now costs almost nothing and is a hard prerequisite for
  every version of this feature.

**Cost**

- Event and hand-history formats must be complete and stable enough to support analysis that
  does not exist yet. That constraint is cheap now and expensive to retrofit, so it is taken on
  deliberately.

## Note on framing

Even with a perfect solver, raw equilibrium frequencies are a poor thing to show a player.
*"Bet 33% of the time, check 67%"* is hard to act on. The useful framing is **decision
quality** — *"this call costs 0.4 big blinds in theory"*, *"you out-played your opponent on the
turn"*. The solver, if it ever exists, is the engine behind that answer, not the answer itself.
