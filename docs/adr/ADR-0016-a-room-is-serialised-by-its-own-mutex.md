# ADR-0016 — A room is serialised by its own mutex, not by an actor

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-013`
- **Unblocks:** `TASK-020714`, and `TASK-020715` behind it

## Context

`RoomRegistry` holds each room in a `Holder` with its own `Mutex`, and every mutating operation
goes through a single private `mutate` helper — deliberately **one** `withLock` call site, because
a second, subtly different critical section is how this class would grow a lost update.

That design was settled when a room held only seating. `TASK-020714` puts a live `DuelRunner`
inside it, and the question was whether a mutex still suffices once a duel is running, or whether
the room should become a coroutine consuming a command channel.

## Decision

**The per-room `Mutex` stays.** A room with a live duel is serialised exactly as a room without one.

Nothing about the runner changes the shape of the problem. `act` is a pure function returning a new
`DuelRunner`, so moving a duel is the same read-modify-write the registry already performs, and the
same lock already makes it atomic. `RoomRegistry` keeps its single `withLock` call site, plus the
narrowly-scoped second one `reap` needs for removal.

## Consequences

**What it buys.** No rewrite of code whose atomicity is proven rather than argued: breaking the lock
inside `mutate` turns `oneHundredConcurrentJoinersProduceExactlyOneGuest` red, and that has been
demonstrated three times as the class changed. An actor would have replaced a proven mechanism with
an unproven one to solve a problem that had not appeared.

It also keeps the room synchronous to its caller. A frame arrives, the duel moves, frames go out —
no mailbox in between, so a failure surfaces to the caller that caused it rather than to whoever
happens to be draining the channel.

**What it costs.** The discipline is a convention rather than a structure: correctness depends on
every mutating path continuing to go through `mutate`. A future contributor can add a second lock
site, and only review would catch it. `TASK-020612` already had to justify its second `withLock`
for reaping, which is the shape that argument will keep taking.

Long operations under the lock would also block other callers on that room. Today nothing under the
lock does I/O — the engine is pure and the result sink is called outside it — and that must stay
true. Recording a duel result to PostgreSQL while holding a room's mutex would couple a database
round trip to every action in that room.

**What it forecloses.** Little. If a room later needs to own timers — `ADR-0013`'s grace period is
the obvious candidate — an actor becomes worth revisiting, and this ADR would be superseded rather
than worked around. The registry's boundary is unchanged either way.

## Alternatives considered

**A channel-fed actor per room.** Serialisation by construction rather than by discipline, and a
natural home for the disconnect timers `STORY-0208` will need. Rejected for now: it rewrites
mutation-proven code, and introduces backpressure and actor-lifecycle failure modes for which no
tests exist. Worth reopening when timers actually arrive.

**A single global lock.** Trivially correct and trivially wrong at any scale — every room would
queue behind every other, so one slow duel would stall the server.
