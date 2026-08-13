# ADR-0017 — The server says when a duel ends

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-015`, and `DEC-010` by the precedent it sets
- **Unblocks:** `TASK-020715`

## Context

`ServerMessage.Events` carries `List<GameEvent>`. `MatchFinished` is a `MatchEvent`, which
`ADR-0009` gave its own hierarchy and its own sequence space, because a match-level event is not a
position within a hand.

The consequence went unnoticed until the duel runner was built: **no `ServerMessage` can carry the
fact that a duel is over.** `TASK-020707` records `MatchFinished` in the `MatchLog` and broadcasts
nothing, which is correct and leaves the client with no way to learn the duel ended except by
looking at two stacks.

`DEC-010` asked a related question — whether new server-side messages belong to `STORY-0202`'s
protocol or extend the sealed hierarchies as later stories need them.

## Decision

**`ServerMessage` gains a `DuelFinished` variant carrying the duel's outcome.** The server states
the ending; the client is told.

And the general form, which answers `DEC-010`: **later stories extend the existing sealed
hierarchies rather than introducing a parallel protocol.** One `ServerMessage` hierarchy, one
`ClientMessage` hierarchy, extended where a story needs a new fact on the wire.

## Consequences

**What it buys.** `ADR-0002`'s rule holds: the client never asserts a game fact. Whether a duel has
ended is a rule of poker — a `Freezeout` ends when a stack reaches zero, a `FixedHands` duel ends on
a count and can end level — and the client is not the place to re-derive it. A client that inferred
the ending would need the rules of poker to render a scoreboard.

The exhaustive `when` over `ServerMessage` makes the addition safe: every handler must account for
the new variant or fail to compile. That is the same property that has caught omissions repeatedly
in this codebase.

Keeping one hierarchy also keeps `ProtocolCodec`, the frame limits, the documentation test and the
generated TypeScript in `STORY-0203` pointed at a single thing.

**What it costs.** The protocol grows, and `PROTOCOL_VERSION` moves. Every exhaustive `when` over
`ServerMessage` needs a new branch, including in tests — that is the intended cost of the
compiler-enforced completeness rather than an unfortunate one.

`DuelFinished` must be a *projection*, not a `MatchEvent` on the wire. It carries the outcome the
recipient is entitled to see, filtered like everything else through `Addressed.kt`. Serialising
`MatchFinished` directly would leak the match's own sequence numbering into the client and blur the
boundary `ADR-0009` drew.

**What it forecloses.** Nothing. Room and lobby messages, when they arrive, extend the same
hierarchies by the same precedent.

## Alternatives considered

**Widen `Events` to carry `MatchEvent` as well.** No new message type, and the frame already exists.
Rejected: `ADR-0009` gave `MatchEvent` a separate sequence space precisely so the two could not be
confused, and a frame carrying both would force a client to keep two counters to reassemble one
stream.

**Let the client infer the ending from the stacks.** Costs nothing to build. Rejected under
`ADR-0002` — and it is simply wrong for `FixedHands`, which can end level with both stacks
non-zero, so the client would miss the ending entirely in the one case where a draw is possible.

**A separate lobby/room protocol alongside the duel protocol.** Rejected as the answer to `DEC-010`:
two hierarchies mean two codecs, two sets of frame limits and two generated clients, for messages
that travel the same socket.
