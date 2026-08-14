# ADR-0023 — An absent seat checks when nothing is owed, folds when facing a bet

- **Status:** Accepted — amended by
  [ADR-0028](ADR-0028-the-wire-names-an-absent-opponent.md) on one point only: an absence action
  stays indistinguishable **in the log**, but is no longer indistinguishable **on the wire** — a
  `ServerMessage` marks every check or fold the server took for an absent seat. Which action is
  sent, and why, is unchanged
- **Date:** 2026-08-13
- **Resolves:** DEC-020 — what an absent seat does at a decision point where `Fold` is illegal
- **Amends:** [ADR-0013](ADR-0013-disconnect-grace-period.md) — narrows "the player's current
  hand is folded"

## Context

`ADR-0013` says that when the grace window expires "the player's current hand is folded". The
engine says folding is not always legal: `BettingRules.kt` puts `FOLD` in the legal set only when
the seat faces a bet — with nothing owed, the base set is `CHECK` alone. That rule is the game's
betting rules, pinned by `TASK-010508`/`TASK-010509` and their tests, inside the pure library
everything depends on.

The two collide in merged code. `foldAbsent` (`poker-server/.../duel/AbsentSeats.kt`) always sends
`PlayerAction.Fold`; at any free decision point the engine rejects it, the no-progress guard stops
the loop, and the duel stalls **forever** with the absent seat on turn. The spots are ordinary: the
big blind's option preflop, and first to act on any street with no bet.

So `ADR-0013`'s letter is unimplementable as written, and the two ways out pull against each
other: the universal poker-room convention — check when free, fold to a bet — is cheap and local
to `poker-server`, but lets an opponent check an absent player down to showdown, where they can
*win the hand*; making "always fold" work instead means changing the engine's betting rules for
everyone to serve a transport-layer condition.

One force settles most of it: `ADR-0013`'s letter was **already** out of reach for an all-in
absent seat. A player who moves all-in and then drops has no further decision points — the board
runs out and they reach showdown, and can win, under `ADR-0013` exactly as written. "An absent
player never wins a hand" was never achievable without inventing a concede action poker does not
have.

Settled constraints this decision sits inside: the engine is pure and clock-free; an absence
action goes through the ordinary `act` path as an ordinary action, indistinguishable in the log
and on the wire from one the player sent; the server is authoritative.

## Decision

When an absent seat's grace window has expired and the turn is on it, the server gives up the
turn with the cheapest action **read from the engine's legal set**, never inferred from the
spot's shape:

- `foldAbsent` calls the engine's `legalActions(hand.state)` at each decision point.
- If `FOLD` is in `allowed`, it sends `PlayerAction.Fold(seat)`: the seat faces a bet and
  concedes the hand.
- Otherwise it sends `PlayerAction.Check(seat)`: nothing is owed and the seat pays nothing.
- The absent seat never calls, bets, raises, or moves all-in — it never puts another chip in
  the pot.

The two branches are total. Every non-empty legal set contains exactly one of `CHECK` and `FOLD`
— `BettingRules.kt` builds the base set as either `check` or `fold, call`, never both — so there
is no priority question and no third case. An all-in or short stack needs no special handling:
a short stack facing a bet it cannot cover still has `FOLD` in its set, and a seat with no
decision left has an empty set and no decision point, which `foldAbsent`'s existing exits already
cover. Should `allowed` ever come back empty with the seat on turn — a state the engine does not
produce — `foldAbsent` stops rather than sending anything. This ADR changes *which action is
sent*, never *when the loop stops*.

Reading `legalActions` rather than testing `toCall == 0` is deliberate: the engine owns action
legality, and re-deriving it in the server duplicates the betting rules and goes wrong, silently,
the day they change. The exactly-one-of-`CHECK`/`FOLD` guarantee is an engine invariant the
server gets for free by asking.

**`poker-engine` does not change.** The legal-action rule of `TASK-010508`/`TASK-010509` stands
exactly as written, tests included. Nothing in this decision touches the engine; do not reopen it.

**`ADR-0013`'s promise narrows, explicitly.** "The player's current hand is folded" becomes: the
absent seat gives up every decision at zero further cost — folding when facing a bet, checking
when checking is free. `ADR-0013` is marked amended by this ADR; its grace window, pause,
reconnect and forfeiture provisions are untouched.

## Consequences

**What it buys.** The stall is gone: the chosen action is drawn from the engine's own legal set,
so the engine accepts it and a duel with an absent player always progresses — the defect `DEC-020`
exists to fix. The change is local to `poker-server`, two files (`TASK-020816`), shippable now.
It is also the cheapest decision to reverse, which is part of why it wins on thin evidence: a
future "absence always concedes" rule would supersede this ADR with an engine change, whereas
walking back a widened fold rule — once clients render legal-action sets — would be a visible
retraction of a game rule. And it is what every online poker room does for a timed-out player,
so it needs no explaining.

**What it costs.** An absent player can be checked down to showdown and **win the hand**. Plainly,
because someone will ask: it can only happen if the present player never bets — any bet, minimum
included, forces the fold — so the present player controls it entirely. And the same outcome
already existed under `ADR-0013` as written whenever the absent seat was all-in; this extends an
existing edge from "all-in" to "opponent chooses to check it down", it does not create a new
class of outcome. A smaller cost: `ADR-0013`'s title now overstates its rule, and readers must
follow the amendment note — the standard price of amending an immutable ADR.

**What it forecloses.** Nothing structurally. If the human ever wants "absence always concedes
the pot" as a product rule, it stays reachable — but only via an engine-level concede concept
that also covers the all-in case, in a new ADR superseding this one. It is not reachable by
flipping the fold-legality rule alone, and this ADR should not be reopened in the belief that
it is.

## Alternatives considered

**Make an open fold legal in the engine.** Strongest case: live-poker rulesets do treat an open
fold as legal, if irregular; it is a one-line widening of `BettingRules.kt`, after which
`foldAbsent` keeps sending `Fold` unchanged and `ADR-0013`'s letter holds at every decision
point. Rejected: it rewrites the game's betting rules — pinned by `TASK-010508`/`TASK-010509` in
the pure library everything depends on — to serve a transport-layer condition. The widened set is
published to clients, so a *present* player gains an open-fold button the game never meant to
offer, and taking that back later is a wire-visible rule change. And it still fails to deliver
"an absent player never wins": an all-in absent seat has no decision point at which any fold
could be submitted, so the promise leaks anyway. The largest blast radius on offer, and the
promise still not kept.

**A bespoke concede action in the engine.** Strongest case: the only design that fully delivers
"an absent player never wins a hand" — it closes even the all-in case, honouring `ADR-0013` in
letter and spirit. Rejected: it invents an action poker does not have, inside the pure engine,
for a server-side timeout. Either real players may send it — conceding a pot while all-in, a
rule no poker room has — or the server must special-case it, breaking `TASK-020806`'s property
that an absence action is indistinguishable from a player's own. A new event type, a protocol
change, projection and log-schema changes: the maximum cost, to strengthen a promise beyond what
was ever actually deliverable.

**Infer the spot in the server (`toCall == 0` → check, else fold).** Strongest case: no engine
query, one comparison, and today it computes the same answer at every reachable spot. Rejected:
it re-implements the engine's betting rules outside the engine — the exact drift `legalActions`
exists to prevent — and every future betting-rule change invalidates it silently. Correct today
by coincidence, wrong tomorrow by construction.
