# ADR-0012 — Anonymous profiles, bound to a device

- **Status:** Accepted
- **Date:** 2026-08-12
- **Resolves:** how a v0.1 player is identified
- **Constrains:** `EPIC-04` (identity and profiles)

## Context

`EPIC-04` schedules real identity for v0.2, but v0.1 has to name the two people at a table, and —
since duel coins and a leaderboard place are the whole reward of this game — it has to remember
what they won. A purely ephemeral session cannot do that.

Building accounts now would answer it, but pulls authentication into v0.1, and auth is where
security defects concentrate.

## Decision

A player is an **anonymous profile bound to a device id**, stored in PostgreSQL.

- On first contact a device presents (or is issued) a device id; the server creates a `player` row
  keyed to it.
- Duel results and the coin balance are stored against that profile, durably.
- No password, no email, no login flow in v0.1.
- The profile row exists from day one, so `EPIC-04` attaches credentials to an identity that
  already has history, rather than retrofitting identity into duels that never had it.

## Consequences

**What it buys.** Coins and results persist with no auth to build. Two people can play, and the
ladder means something. `EPIC-04` becomes additive.

**What it costs — and these are real, not theoretical:**

- **A lost device is a lost profile.** Clearing site data, switching browser or getting a new phone
  strands the profile and its coins. This is inherent to device-bound identity, not an
  implementation flaw. `EPIC-04` therefore **must** include a *claim* flow — attaching credentials
  to an existing device profile — or the first player to change phones loses their ladder position
  with no recourse.
- **Device ids are trivially minted.** Anyone can create unlimited profiles. Against a ranked
  ladder with coins that is a farming and smurfing vector. It is acceptable now, when there are no
  users and no public leaderboard. **It must not still be true when the leaderboard goes public** —
  that is a gate on `EPIC-05`, recorded here so it is not rediscovered late.

**What it forecloses.** Nothing structurally; it is a strictly weaker identity than `EPIC-04` will
provide, and the migration path is addition rather than replacement — provided the claim flow above
is built.

## Alternatives considered

**Ephemeral session, no persistence.** Simplest, and it was the original v0.1 plan. Rejected
because the reward in this game *is* the record; a duel coin that vanishes on refresh is not a
reward.

**Simple accounts now.** Makes ranked results meaningful immediately and avoids the lost-device
problem entirely. Rejected for v0.1 on scope and risk: it pulls `EPIC-04` forward into work that is
running unattended, and authentication is the wrong thing to build without close review.

**Seat tokens only, no identity.** A duel is a URL with two tokens. Rejected: nothing to attach a
coin balance to, which is the requirement that started this.
