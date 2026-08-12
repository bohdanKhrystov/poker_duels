# ADR-0013 — A dropped connection gets a grace period, then folds

- **Status:** Accepted
- **Date:** 2026-08-12
- **Resolves:** what happens when a player's connection drops mid-duel

## Context

A duel is played over a WebSocket, and connections drop — a tunnel, a sleeping laptop, a flaky
router. The server has to decide what a dropped socket means, and the answer is visible in the
game's fairness: too harsh and a network problem costs a ranked duel; too lenient and one player
can freeze a duel forever while the opponent waits.

## Decision

A dropped connection holds the seat for a **grace period**, then folds.

- On disconnect the seat is held and the duel pauses.
- Reconnecting within the window resumes the duel; the server resends the state the returning
  player is entitled to see — filtered through the engine's projection layer, so a reconnect can
  never reveal the opponent's hole cards.
- When the window expires the player's current hand is folded, and the duel continues or is
  forfeited according to whether they can still play.
- The window is configuration, not a literal scattered through the code, so it can be tuned without
  a code change.

## Consequences

**What it buys.** Real networks are tolerated. A subway tunnel costs a hand at worst, not a ranked
duel. The behaviour matches what online poker players already expect, so it needs no explaining.

**What it costs.** The server gains a clock and per-seat timers — the first genuinely time-dependent
behaviour in the system. That has consequences worth stating:

- The engine stays clock-free. Timing lives in the server; the engine still only ever receives an
  action, and a timeout arrives as an ordinary fold. `ADR-0003`'s no-clock rule for the engine is
  unaffected.
- Tests covering the grace period must inject time rather than sleep, or the suite becomes slow and
  flaky. Any ticket that touches the timer owns that.

**What it forecloses.** Nothing significant. A per-action turn clock — a different feature, and one
this game will probably want — fits alongside this rather than replacing it.

## Alternatives considered

**Immediate forfeit.** Trivial to build, impossible to abuse by rage-quitting. Rejected as too
harsh: on mobile networks it would routinely decide ranked duels for reasons that have nothing to do
with poker.

**Hold indefinitely.** Never punishes a bad connection. Rejected because it hands one player a way
to freeze a duel permanently, and leaves abandoned rooms accumulating with nothing to clean them up.
