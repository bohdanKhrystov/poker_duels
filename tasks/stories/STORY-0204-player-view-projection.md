---
id: STORY-0204
title: PlayerView — per-recipient projection in the engine
type: story
status: ready
parent: EPIC-02
module: poker-engine
labels: [engine, projection, security]
depends_on: []
---

## Goal

The engine can answer two questions it currently cannot: *what may seat N see of this state?* and
*may seat N see this event?* One type, `PlayerView`, and one per-seat event filter — so that when
the server starts broadcasting, it never decides what a player may see.

## Why

[`architecture.md`](../../docs/architecture.md) lists `PlayerView` in its projection table and
calls it "the security-critical one". [`ADR-0002`](../../docs/adr/ADR-0002-server-authoritative.md)
says clients receive a `PlayerView` and that redaction happens in the engine's projection layer,
"not by omitting a field at the transport boundary". `CLAUDE.md` states it as a non-negotiable.

It does not exist. `EPIC-01` built `StateProjection` — the fold from events to state — and never
built the redaction half, because nothing consumed it. This epic is that consumer, and **nothing
may be sent to a client until this story is done**, which is why it is one of the two stories
startable when the epic opens.

It is the only story in `EPIC-02` that touches `poker-engine`, and it touches nothing else there.

## Design notes

- `PlayerView` and `PlayerView.of(state: GameState, seat: Int): PlayerView` live in
  `duels.poker.engine.game`, beside `StateProjection`. Not in `poker-server`: a redaction rule in
  the transport module is exactly the arrangement `ADR-0002` forbids.
- A `PlayerView` carries what a player is entitled to know: hand number, street, `Board`, `pot`,
  both seats' stacks and street commitments, `seatToAct`, the blinds, and **its own** hole cards.
  The opponent's hole cards appear only once the engine has already revealed them — a
  `HandRevealed` event has been emitted, per
  [`ADR-0008`](../../docs/adr/ADR-0008-loser-mucks-at-showdown.md), where the loser mucks.
- It never carries `GameState.deck`, `GameState.rng` or a seed. Those are the two fields
  `StateProjection`'s KDoc already singles out as never leaving the server, and `ADR-0002` says
  the seed may only be published after a match ends.
- Event redaction is a separate function, per event: a `GameEvent` filtered for a seat is either
  the event unchanged or **nothing at all**. `HoleCardsDealt` addressed to the other seat is
  dropped whole — its own KDoc already says "addressed to one seat, so a broadcast filters by
  recipient rather than by field". Returning `null` rather than a blanked copy means no
  half-redacted card object exists anywhere to be leaked by a later refactor.
- Folded and mucked hands appear in no event at all — already guaranteed by `TASK-010614` and
  `TASK-010617`. This story must not weaken those tests; it asserts the property from the
  recipient's side as well.
- Serialization: `PlayerView` is `@Serializable`, like the rest of the domain, under
  [`ADR-0010`](../../docs/adr/ADR-0010-engine-takes-a-serialization-dependency.md)'s allowlist. No
  new dependency, and `checkNoDependencies` still passes unchanged.
- The property test belongs in `poker-ai`'s test sources, where the duel simulator from
  `STORY-0108` already plays thousands of duels between `RandomBot`s. Reusing it is far cheaper
  than hand-building duels in the engine's own tests, and it exercises real showdowns.
- These are the `review: deep` tickets of this epic. A defect here is silent, permanent and
  invisible in every screenshot.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets are produced by `/plan-story STORY-0204`.* | — |

## Acceptance criteria

- [ ] `PlayerView.of(state, seat)` contains the viewer's own hole cards and, before the engine
      emits `HandRevealed`, no card belonging to the other seat.
- [ ] After `HandRevealed`, the revealed hand appears in both seats' views — a showdown a player
      cannot see is a different bug.
- [ ] A `GameEvent` filtered for a seat is the event unchanged or absent; no event type is ever
      delivered with a modified card list.
- [ ] `PlayerView` exposes no deck, no rng and no seed, asserted structurally rather than by
      reading the class.
- [ ] Over at least 1 000 simulated duels, no card the other seat holds appears in either seat's
      views or filtered event stream before the engine reveals it; a failure reports the seed that
      reproduces it.
- [ ] A folded hand and a mucked hand appear in no view and no event, at any point.
- [ ] `./gradlew :poker-engine:checkNoDependencies` still passes.

## Out of scope

- Anything that sends a view anywhere — `STORY-0202` defines the message that carries it,
  `STORY-0207` sends it.
- `StatsProjection` and `ReplayProjection` from `architecture.md`'s table — `EPIC-08`.
- Publishing the seed after a match ends. `ADR-0002` permits it; nobody has asked for it, and it
  is an addition rather than a change when they do.
