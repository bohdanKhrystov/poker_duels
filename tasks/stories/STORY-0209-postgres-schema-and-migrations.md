---
id: STORY-0209
title: PostgreSQL — schema, migrations and the connection pool
type: story
status: done
parent: EPIC-02
module: poker-server
labels: [server, persistence, postgres]
depends_on: [STORY-0201]
---

## Goal

The server owns a PostgreSQL schema, applies its migrations at startup, holds a connection pool,
and runs its tests against a real database rather than an imitation of one.

## Why

[`ADR-0011`](../../docs/adr/ADR-0011-postgres-in-v01.md) pulled PostgreSQL into v0.1 because a coin
balance that a deploy can destroy is not a reward. It is also the single largest scope addition to
this epic, which is the argument for doing it early and on its own: the schema, the pool and the
test harness are all infrastructure, and infrastructure discovered late is infrastructure retrofitted
into finished code.

It shares no file with the protocol, room or socket stories, so it runs **fully in parallel** with
them.

## Design notes

- Migrations with **Flyway**: versioned SQL files a reviewer can read, applied in order, recorded
  in the database. The alternative — a schema created by code at startup — has no history and no
  way to answer "what changed".
- Pooling with **HikariCP**, configured from `ServerConfig` (`STORY-0201`), so the database URL and
  pool size are environment-overridable for `EPIC-07`.
- **Plain JDBC and SQL, no ORM.** `ADR-0003` refuses an ORM anywhere near the engine, and the
  schema here is a handful of tables; an ORM would add a mapping layer with more concepts than the
  schema has.
- Tests use **Testcontainers PostgreSQL** — the real database, the real types, the real constraint
  behaviour. H2 in PostgreSQL mode disagrees with PostgreSQL in exactly the places that matter
  (upserts, transactional DDL, integer overflow), so a green H2 suite would be worth very little.
  CI runners already provide Docker.
- The tables serve `ADR-0011`'s enumerated durables and nothing more: a player profile keyed by
  device id (`ADR-0012`), a finished duel, and a per-player-per-duel result row carrying the coin
  delta.
- **The coin column is a signed integer**, and so is any stored balance
  ([`ADR-0014`](../../docs/adr/ADR-0014-duel-coin-economy.md)): winner `+1`, loser `−1`, draw `0`,
  balance `= wins − losses`, which may be negative. An unsigned column, a `CHECK (balance >= 0)`,
  or a `UInt` in the Kotlin mapping is a bug waiting for the first losing streak — and the floor
  is refused deliberately, because flooring makes a long losing streak indistinguishable from
  never having played.
- Storing the delta per duel rather than only a running total is what keeps `ADR-0014`'s successor
  cheap: a floating rating, or an award weighted by opponent strength, becomes a new ADR and a new
  computation over rows that already exist, not a migration.
- Uniqueness where it protects truth: one profile per device id, one result row per (duel, player).
  The database is where an invariant survives a buggy caller.
- A local `docker-compose.yml` for a development PostgreSQL, so a fresh clone can run the tests
  without installing anything. Production delivery — images, hosting, secrets — is `EPIC-07`.

### `DEC-008` — noted, blocking nothing

`ADR-0011` enumerates the durables as profiles, duel results and coin balances, and does not
include the event log; `architecture.md`'s runtime diagram describes appending events to "the match
log (persistent)". **Is the full `MatchLog` persisted in v0.1, and where — a column, a table per
hand, or object storage?** It decides whether `EPIC-03`'s replay viewer and `EPIC-08`'s analysis
have anything to read. Until it is answered, this story implements `ADR-0011`'s enumeration only
and leaves room for a log to be attached to a duel row later. Nothing in this epic waits on it.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-020901](../tasks/TASK-020901-database-dependencies.md) | Put the database dependencies in the version catalog and the server build | ready |
| [TASK-020902](../tasks/TASK-020902-database-settings-in-server-config.md) | Read the database URL, credentials and pool size from `ServerConfig` | backlog |
| [TASK-020903](../tasks/TASK-020903-postgres-test-harness.md) | Start one PostgreSQL container for the suite, and decide what a missing Docker means | backlog |
| [TASK-020904](../tasks/TASK-020904-initial-schema-and-flyway.md) | Create the initial schema and apply it with Flyway | backlog |
| [TASK-020905](../tasks/TASK-020905-signed-coin-columns.md) | Prove a negative coin balance and a negative delta round-trip through PostgreSQL | backlog |
| [TASK-020906](../tasks/TASK-020906-schema-constraints.md) | Prove the schema refuses a duplicate device id and a duplicate result row | backlog |
| [TASK-020907](../tasks/TASK-020907-hikari-connection-pool.md) | Open a HikariCP connection pool from `ServerConfig` | backlog |
| [TASK-020908](../tasks/TASK-020908-migrate-at-startup.md) | Open the pool and migrate at startup, and make a second startup a no-op | backlog |
| [TASK-020909](../tasks/TASK-020909-local-development-database.md) | Give a fresh clone a local database with `docker compose` | backlog |

## Acceptance criteria

- [ ] `./gradlew :poker-server:test` starts a PostgreSQL container, applies every migration and
      passes, with no database installed on the machine.
- [ ] Migrations run at application startup; a second startup against the same database is a no-op.
- [ ] Applying every migration in order to an empty database produces the schema the repositories
      expect — a renamed or missing column fails the build rather than a query at runtime.
- [ ] A negative coin balance and a negative per-duel delta are both storable and readable back
      unchanged; a test stores `−1` and reads `−1`.
- [ ] Inserting a second profile for one device id fails on a constraint, as does a second result
      row for one (duel, player).
- [ ] `docker compose up` gives a working local database and the test suite runs against it.
- [ ] `./gradlew :poker-engine:checkNoDependencies` still passes — no database dependency reaches
      the engine, ever (`ADR-0011`).

## Out of scope

- Repositories with domain behaviour, profile creation, result recording — `STORY-0210`. This story
  owns the schema, the migration mechanism and the pool.
- Reading anything back for a player — `STORY-0211`.
- Persisting the `MatchLog` — `DEC-008`.
- Persisting rooms or in-flight duels — never, per `ADR-0011`.
- Backups, retention, hosting the database — `EPIC-07`.
