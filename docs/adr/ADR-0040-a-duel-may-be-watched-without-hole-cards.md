# ADR-0040 — A duel may be watched live, minus every hole card

- **Status:** Accepted
- **Date:** 2026-08-15
- **Resolves:** `DEC-009` — **the human's product call**, made as *"live, hole cards hidden"*.
  This ADR records the call and works out what it costs the projection layer.
- **Constrains:** `poker-engine`'s projection layer (`PlayerView.of`, `visibleTo`) and the leak
  properties that guard it; the wire, when a spectator path is built; and any epic that ships
  spectating.

## Context

`ADR-0002` makes the server authoritative and this project's hardest non-negotiable is that hole
cards are filtered **in the engine's projection layer**, never ad hoc in transport. Two functions
carry that today, and both are indexed by seat:

- `PlayerView.of(state, seat, revealed)` builds what one seat may see;
- `visibleTo(event, seat)` redacts an event for one seat.

A spectator has no seat. That single fact is the whole technical content of this decision: there
is no seat number that means "watching", and inventing one — a sentinel `-1`, or seating a
phantom third player — would put a value into the one code path that exists to be exhaustively
reasoned about, where every existing test assumes a seat is a player.

`EPIC-03` records spectating as out of scope precisely because *"no `PlayerView` exists for a
third party"*. That remains true; this ADR says what the third one must be, not that it is built.

## Decision

**A duel may be watched live by a third party who sees no hole card that has not been revealed
at showdown.** A spectator sees the board, the pot, the stacks, the blinds, the actions and the
street — everything the game makes public — and learns nothing about either player's hand until
the engine reveals it under `ADR-0008`.

**The spectator projection lives in the engine, beside the two that exist.** It is a third entry
point in the projection layer, not a seat argument and not a filter applied in transport. What it
is called and whether it returns a `PlayerView` or its own type is left to the story that builds
it; that it lives here is not.

**A mucked hand stays mucked for spectators too.** `ADR-0008` has the loser muck, and folded and
mucked cards appear in no event for anyone. A spectator is not an exception, and specifically:
spectating must not become the reason a "show the folded hand" feature appears, since that would
retract a guarantee players currently have.

**The spectator projection carries its own leak property.** `TASK-020410` and `TASK-020411`
assert that no `PlayerView` and no event stream carries a secret it should not. A third
projection without a third property is the shape of defect this project has repeatedly caught —
an assertion that cannot fail because nothing exercises the new path. The property must
enumerate what a spectator may see, not merely check that it does not equal a player's view.

**Live is live: nothing is delayed.** A broadcast delay is a mitigation for the collusion risk
that the hidden-cards decision has already removed, and adding one would introduce a clock into a
path that does not have one.

## Consequences

- **Spectating is safe for ranked play.** A spectator learns nothing a player could relay,
  because a spectator knows nothing the opponent does not already know. The collusion channel
  that would break a ladder is closed by construction rather than by policy.
- **`poker-engine` stays pure.** A third projection is more of exactly what the module already
  does — no networking, no clock, no framework types.
- **Nothing is built by this ADR.** No epic currently owns spectating; `EPIC-03` explicitly
  excludes it. This settles the shape so that whichever epic picks it up starts from a decision
  rather than a design meeting, and so that nobody builds the tempting wrong version in the
  meantime.
- **Open, and deliberately not answered here:** who may watch (anyone with a link, or only a
  signed-in player), whether the players are told they are being watched, and whether a spectator
  count is shown. Those are product questions that arrive with the feature, and answering them
  now would be guessing at a feature nobody has scheduled.
- `DEC-008` — whether the full `MatchLog` is persisted, and where — is untouched. Live spectating
  needs no persistence at all; a replay feature would, and that is the decision `DEC-008` holds.

## Alternatives considered

**No spectating at all.** Cheapest, and it keeps the projection layer at exactly two recipients,
which is genuinely easier to reason about. Rejected because the feature is wanted and the
question was registered to be answered rather than avoided — and because a decision recorded now
costs nothing while the wrong ad-hoc implementation later would cost the non-negotiable.

**Replay after the duel ends, from the `MatchLog`.** Attractive: zero live leak surface, and the
log already replays byte-identically. Rejected as an answer to *this* question — it is a
different feature, it depends on `DEC-008` being answered first, and it does not let anyone watch
a duel that is happening. Not foreclosed; a replay feature can still be built and this ADR does
not compete with it.

**Live with both hands visible — the "God view" that makes poker watchable on stream.** Rejected
outright and worth recording why: a spectator who sees both hands can relay the opponent's cards
to a player over any side channel, undetectably. On a ranked ladder that breaks the thing the
ladder measures. It could only ever be safe for duels that are not ranked, and this product has
no such duel.
