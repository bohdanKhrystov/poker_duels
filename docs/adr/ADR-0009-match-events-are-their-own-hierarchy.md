# ADR-0009 — Match events are their own hierarchy

- **Status:** Accepted
- **Date:** 2026-08-12
- **Resolves:** `DEC-005`

## Context

`STORY-0107` built the match layer — `MatchState`, `startNextHand`, `recordHand`, `outcomeOf` —
entirely as values. Nothing about a duel ending is currently an event, so a server has nothing to
broadcast when one does.

Putting `MatchFinished` into the existing `GameEvent` hierarchy looked cheapest, and it is the
thing this ADR rejects. Two facts make it expensive:

- `GameEvent.sequence` is documented as a position **within a hand**. A match-level event has no
  meaningful position in any hand, so it would either carry a lie or force the meaning of
  `sequence` to change everywhere.
- `StateProjection.dispatch` is an exhaustive `when` over `GameEvent` with no `else`, deliberately.
  A new subtype stops that file compiling and drags the engine contract suite into the diff — for
  an event the hand projection has nothing to do with.

The answer also binds `STORY-0108`'s serialiser and `EPIC-02`'s broadcast path, so it is worth
settling before either is written.

## Decision

Match-level events live in their own sealed `MatchEvent` hierarchy, with their own sequence space.

- `GameEvent` stays hand-scoped, and `sequence` keeps meaning "position within a hand".
- `MatchEvent` numbers its own events from the start of the match.
- The two logs are separate. A match log references its hands; it does not contain their events.

## Consequences

**What it buys.** The hand layer is untouched — `StateProjection`, its exhaustive `when`, and the
contract suite all stay as they are, and a hand remains replayable in isolation with no match
context. `HandLog` and `replayHand` keep working unchanged. Each hierarchy keeps one honest meaning
for `sequence`.

**What it costs.** Two event hierarchies, so transport and persistence each handle two shapes, and
`EPIC-02` needs a second projection. A consumer wanting a single ordered stream of everything that
happened in a duel has to interleave the two itself.

**What it forecloses.** Nothing permanently — a later ADR could unify them. The reverse direction
is the hard one, which is why the separation is the safer default now.

## Alternatives considered

**Into the `GameEvent` log.** One log, one sequence space, one projection. Rejected because it
breaks the documented meaning of `GameEvent.sequence` and forces every exhaustive `when` over
`GameEvent` to grow a branch for an event the hand layer does not care about. The simplicity is
real but it is bought by making the hand log dishonest.

**Nowhere — `DuelOutcome` as a value only.** Simplest today, and consistent with how the match
layer already works. Rejected because `EPIC-02` will need something to broadcast when a duel ends,
and inventing it at the transport layer would put a game fact outside the engine — which
`ADR-0002` (the server is authoritative, and the engine owns game facts) does not allow.
