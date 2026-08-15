# ADR-0035 — A duel is a freezeout

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-001` — **the human's product call**, made as *"freezeout, confirm the
  documented default"*. [`docs/duel-rules.md`](../duel-rules.md) Part 2 asked for exactly this
  and named the ADR as the place to record it.
- **Constrains:** nothing that is not already built. `DuelFormat` keeps the format as
  configuration, and this ADR fixes only which configuration v0.1 ships.

## Context

`docs/duel-rules.md` Part 2 has carried a `DEC-001` banner since the rules were first written:
the duel format *"shapes how the game feels more than any UI choice, and it should be decided
by playing, not by argument."* The engine was therefore built to take the format as
configuration, and both candidate shapes are implemented — `TASK-010707` gave the engine
`DuelFormat`, `TASK-010706` the end condition, and `TASK-010716` a fixed-length duel with its
own tests.

So this decision costs nothing to make and nothing to reverse. What it settles is which format
the first playable build presents as *the* duel, and therefore what the client, the design
system and the leaderboard are built around.

The tension is real. **Freezeout** ends in a knockout, which is what the word "duel" means and
what `ADR-0014`'s single duel coin rewards. **A fixed-length match** — 25 or 50 hands, most
chips wins — has lower variance, so it is the better measurement for a ranked ladder, and it
has a predictable duration, which matters for a product people play in short sessions.

The counter-argument that decided it: a ranked ladder gets its signal from *many* duels, not
from one. Lowering the variance of a single duel to sharpen a ladder that already averages over
a season buys accuracy the ladder does not need, and pays for it with an ending that is an
arithmetic comparison — the moment of winning becomes reading a number rather than taking the
last chip.

## Decision

**A duel is a freezeout**, as `docs/duel-rules.md` Part 2 already describes it:

| Setting | Value |
| --- | --- |
| Starting stack | 100 big blinds (10 000 chips at 50/100) |
| Blind schedule | levels rise every 10 hands, doubling every 10 from level 5 |
| End condition | one player holds every chip |
| Expected length | 20–45 hands, roughly 5–15 minutes |
| Reward | winner gets 1 duel coin |

The `DEC-001` banner is removed from `docs/duel-rules.md` and Part 2 now states the format as
settled, pointing here.

**The numbers stay configuration.** Deciding the *shape* is not deciding the constants. Starting
stack, blind levels and the escalation rate remain `DuelFormat` values, tunable without an engine
change, exactly as `docs/duel-rules.md` requires. A future ADR may move any of them; it will not
have to re-litigate the shape.

**The escalating blind schedule stays load-bearing.** It is what guarantees termination — without
it a heads-up freezeout can run indefinitely, and `TASK-010715`'s termination property depends on
it. Any tuning of the schedule must preserve that property, which the test already enforces.

## Consequences

- The client, the design system and every screen that ends a duel are built around a knockout.
  `STORY-0308`'s result screen shows a winner, not a chip count comparison.
- `ADR-0014`'s one-coin award needs no qualification: a freezeout always has exactly one winner,
  so the draw case `ADR-0015` writes two zero rows for arises only from abandonment, never from
  the format.
- Duel length is variable, 5–15 minutes typically. A product decision that follows from this one
  — what happens to a duel that runs far past its expected length — is *not* raised here, because
  the escalating schedule bounds it in practice and no evidence yet says otherwise.
- Fixed-length duels remain implemented and tested. They are available as configuration for
  anything that wants them later — an exhibition mode, a tournament format — without a new build.

## Alternatives considered

**Fixed-length match (25 or 50 hands).** Lower variance and predictable duration, both genuine
advantages for a ladder. Lost on the ending: winning by holding more chips when the hand counter
runs out is a comparison, not a victory, and it makes the duel coin a reward for arithmetic. The
variance argument is also weaker than it looks, since a season of duels averages out what one
duel cannot.

**Defer until the client is playable**, as `docs/duel-rules.md` itself suggests. Legitimate and
not overdue — `DEC-001` is due before v0.2 and `EPIC-03` is roughly half built. Rejected because
deferring costs something now: `EPIC-03`'s remaining screens and `EPIC-06`'s duel-end design have
to be built around *some* ending, and building them around a shape nobody has committed to means
either hedging both or redoing one. The format was always the likely answer; recording it lets
the screens commit. If play proves it wrong, superseding this ADR costs a configuration change
and a new ADR, which is exactly what the engine's design was for.

**Single hand** and **race to the first pot won** were rejected in `docs/duel-rules.md` before
this decision was registered, on variance grounds, and nothing here revisits them.
