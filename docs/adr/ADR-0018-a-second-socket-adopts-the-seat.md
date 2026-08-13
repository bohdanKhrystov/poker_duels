# ADR-0018 — A second socket adopts the seat, and the first is closed

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-011`
- **Unblocks:** `TASK-020511`
- **Relates to:** `ADR-0013` (disconnect grace period), `ADR-0012` (device-bound profiles)

## Context

A device id identifies a player (`ADR-0012`). `SessionRegistry` can already answer which sessions a
device holds — `sessionsOf` was deliberately built as a query that **enforces nothing**, so that
this decision would not be made by accident in the code that happened to need it first.

The situation is a device opening a second socket while one is already live.

## Decision

**The new socket takes the seat; the existing one is closed.**

The server does not wait for the old connection to prove itself dead. A device holds at most one
live session, and the most recent socket is the one that holds it.

## Consequences

**What it buys.** The common cause of a second socket is not a second player — it is the same
player reconnecting after a network drop, while the server still believes the old connection is
alive because nothing has told it otherwise. TCP can take minutes to notice. Refusing the new socket
until then means the app cannot reconnect, and "it just won't come back" is indistinguishable from
broken.

This is also what makes `ADR-0013`'s grace period useful rather than theoretical. That ADR holds the
seat and resends state through the projection layer when a player returns — returning *is* a new
socket, so adopting it is the mechanism by which the grace period pays off.

**What it costs.**

- **A closed socket is not always a dead player.** Two genuine clients sharing a device id — two
  browser tabs — will fight, each closing the other. That is acceptable in v0.1: a device id is one
  player by `ADR-0012`, so two tabs are one player, and the last one wins.
- The close must be **clean**: `ConnectionWriter.close()` in a `finally`, and the session removed
  from `SessionRegistry` exactly once. The existing socket lifecycle already requires this, and
  adopting a seat is a second path into it — an adopted-away session that leaked its writer would
  leak a coroutine per reconnect, which is exactly the shape of bug that only appears under real
  network churn.
- Adoption must not be usable as a denial of service against another player. Since a device id is
  the sole credential (`ADR-0012`), holding someone's device id already means being them — this adds
  no new exposure, but it does mean the farming concern in `DEC-012` now has a second consequence
  and should be weighed before the first public link.

**What it forecloses.** Nothing structural. If v0.2 gains real authentication, "one live session per
identity" survives unchanged; only the definition of identity moves.

## Alternatives considered

**Refuse the new socket while one is live.** Simplest, and impossible to abuse. Rejected because it
makes reconnection depend on the old connection timing out, which is the failure players actually
hit and the one they cannot work around.

**Let both live.** No policy at all. Rejected: two sockets could act for one seat, so the ordering of
actions becomes ambiguous and every broadcast is duplicated. It also gives an attacker holding a
device id a way to observe a duel without disturbing the real player, rather than being immediately
obvious.

**Adopt the new socket but keep the old as a read-only observer.** Tempting for spectating later.
Rejected as scope: it needs a spectator concept the product does not have, and `ADR-0002`'s
per-recipient filtering would have to answer what an observer may see.
