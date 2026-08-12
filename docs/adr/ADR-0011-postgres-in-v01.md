# ADR-0011 — PostgreSQL lands in v0.1

- **Status:** Accepted
- **Date:** 2026-08-12
- **Amends:** `ADR-0003` — which deferred PostgreSQL to v0.2

## Context

`ADR-0003` chose PostgreSQL but deferred it to v0.2, on the reasoning that v0.1 needed nothing
durable. That reasoning no longer holds, because v0.1 now has something worth keeping: a player
profile carrying duel results and duel coins (`ADR-0012`).

The alternatives were an in-memory server, or appending `MatchLog` JSON to disk. Both are cheaper
to build and both were rejected: the moment a profile accrues a coin balance, "the process
restarted" cannot be an acceptable way to lose it, and a file-based store would mean inventing
locking, cleanup and indexing that a database already has.

## Decision

PostgreSQL is part of v0.1. The duel server owns the schema and the migrations.

- The engine still touches no database, ever — `ADR-0003`'s rule stands, and
  `checkNoDependencies` still guards it.
- Persistence lives in the server module, behind a repository boundary, so the game logic does
  not acquire a database shape.
- Durable from v0.1: player profiles, duel results, coin balances. In-flight duel state is *not*
  required to survive a restart — that is a separate decision, deliberately left for later.

## Consequences

**What it buys.** Coins and results mean something from the first duel. No migration story later,
which is the expensive kind of rework: moving from in-memory to a database after data exists means
writing a one-off importer nobody wants to own.

**What it costs.** v0.1 gains a database: schema, migrations, a connection pool, local setup and a
container in delivery. It is the single largest scope addition to `EPIC-02`, and it lands before
there are any users to need it.

**What it forecloses.** Little. The engine boundary is unchanged, so the pure-Kotlin core is
unaffected either way.

## Alternatives considered

**In-memory, duels and profiles die on restart.** Fastest to ship, and consistent with
`ADR-0003`'s deferral. Rejected once profiles gained a coin balance — a deploy silently destroying
the ladder is not a defect anyone would notice until it mattered.

**Append the `MatchLog` JSON to disk.** Reuses the codec from `TASK-010826` and avoids a database.
Rejected: it solves storage and leaves querying, concurrent access and retention unsolved, and each
of those is a thing PostgreSQL already does properly.
