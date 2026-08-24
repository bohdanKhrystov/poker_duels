---
schema: 2
id: TASK-040608
title: Revoking is one UPDATE and one DELETE, in one transaction
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, revocation, security]
depends_on: [TASK-040607]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresDeviceBindingsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresDeviceBindings.revoke` marks the caller's live binding revoked and deletes every session
that player holds except the presented one, both on one connection inside one transaction.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDeviceBindings.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDeviceBindingsTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresAuthSessions.kt` (the class shape and
`sessionTokenDigest`'s call sites), `docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md`
§2, `docs/adr/ADR-0050-revoking-the-device-signs-out-everywhere-but-here.md` §1.

## Scope

- `public class PostgresDeviceBindings(private val dataSource: DataSource) : DeviceBindings`, in
  `duels.poker.server.db`, `withContext(Dispatchers.IO)` exactly as its neighbours are.
- **One connection, `autoCommit = false`, two statements, one `commit()`**, with `rollback()` in a
  `catch` and `autoCommit = true` in a `finally` — the shape `PostgresProfileWrites.writeName`
  already uses. Two `dataSource.connection.use { }` blocks would be two transactions, and
  `ADR-0050` §1's *"performs both writes in one transaction"* would be false while every test in
  this class still passed.
- The two statements, in this order and verbatim in intent:

  ```sql
  UPDATE device_binding SET revoked_at = now()
   WHERE player_id = ? AND revoked_at IS NULL;

  DELETE FROM auth_session
   WHERE player_id = ? AND token_hash <> ?;
  ```

- **The `DELETE` runs unconditionally**, whether or not the `UPDATE` touched a row (`ADR-0050` §1).
  A branch on `executeUpdate() > 0` would leave a never-bound player pressing the button, being told
  nothing, and keeping the other session running.
- `token_hash` is bound from `sessionTokenDigest(keeping)` — `TASK-040606`'s function, never a second
  `MessageDigest` here.
- `revoked_at` is the database's own `now()`; no `Clock` and no `ServerClock` is a constructor
  parameter (`ADR-0049` §1 — nothing compares these columns to a clock).
- KDoc names the four tables this writes nothing to: `player`, `credential`, `duel`, `duel_result`.

## Out of scope

- The route, its `401` and its `409` — `TASK-040609` and `TASK-040610`.
- Wiring into `ServerComponents` — `TASK-040611`.
- Any read. This class gains no `isLive`; `deviceRouteLive` comes from `PostgresProfileReads`.
- Retrying, or interpreting a `SQLException`. The finality trigger cannot fire here — the `UPDATE`'s
  own `revoked_at IS NULL` predicate excludes every row it would raise on — so no `sqlState` is
  translated and nothing is caught but to roll back and rethrow.

## Tests

`PostgresDeviceBindingsTest` — a real database, `freshDatabase()` + `Migrations.migrate(...)`.
Sessions are issued through a real `PostgresAuthSessions(dataSource, Clock.systemUTC())` so the
digest under test is the digest the store wrote.

| Test | Proves |
| --- | --- |
| `theLiveBindingIsRevokedAndTheOtherSessionsAreGone` | One player, one live binding, **three** sessions `t0`, `t1`, `t2`. `revoke(player, keeping = t0)`. Afterwards: the binding's `revoked_at` is non-null, `playerOf(t0)` answers the player, and `playerOf(t1)` and `playerOf(t2)` both answer `null`. **Three tokens with two different expected answers** — with one surviving and one swept, a `DELETE` missing its `<> ?` and a `DELETE` that ran on nothing are both still distinguishable |
| `theSurvivingRowIsTheOnlyRowLeft` | In the same shape, `SELECT count(*) FROM auth_session WHERE player_id = ?` reads `1` afterwards. A count, not a boolean: `playerOf` answering `null` cannot tell a deleted row from an expired one |
| `anotherPlayersSessionsAreUntouched` | A second player with two sessions of their own, in the same database. After the first player revokes, both of the second player's tokens still resolve and `count(*)` for them still reads `2`. Without this, a `DELETE` missing its `player_id` predicate passes every test above |
| `aPlayerWithNoLiveBindingStillLosesTheOtherSessions` | A player who never bound a device — no `device_binding` row at all — with two sessions. `revoke(player, keeping = t0)` leaves `t0` alive and `t1` gone. This is `ADR-0050` §1's *"the `DELETE` runs unconditionally"*, and it is exactly the assertion an `if (rowsUpdated > 0)` fails |
| `anAlreadyRevokedBindingIsNotRewritten` | Revoke once, read `revoked_at`, revoke again with a different surviving token. The stored `revoked_at` is **unchanged** by the second call, and nothing raises. The `WHERE revoked_at IS NULL` predicate is what keeps the finality trigger out of this path; without it PostgreSQL raises `23001` and this test fails loudly rather than silently |
| `nothingElseInTheDatabaseMoves` | Snapshot `player`, `credential`, `duel` and `duel_result` — every column of every row, ordered by primary key — before and after a revocation that has a duel and a credential behind it, and assert each is byte-identical. `ADR-0050` §1's last bullet, made checkable |

## Acceptance criteria

- [ ] All six test methods above pass
- [ ] `PostgresDeviceBindings.kt` contains exactly one `dataSource.connection.use` block and exactly
      one `commit()` call
- [ ] `grep -c "MessageDigest" poker-server/src/main/kotlin/duels/poker/server/db/PostgresDeviceBindings.kt`
      reads `0`
- [ ] The `DELETE` is not inside any `if`
- [ ] Every command in `verify:` exits 0

## Proof

Two mutations, each applied to `PostgresDeviceBindings` alone and reverted.

1. **Drop ` AND token_hash <> ?` from the `DELETE`** (and its bind). `playerOf(t0)` now answers
   `null`, so `theLiveBindingIsRevokedAndTheOtherSessionsAreGone` reddens on that assertion;
   `theSurvivingRowIsTheOnlyRowLeft` reddens on a count of `0`;
   `aPlayerWithNoLiveBindingStillLosesTheOtherSessions` reddens on `t0`; and
   `anotherPlayersSessionsAreUntouched` reddens on the **first** player's surviving token, which it
   also asserts. **Four tests.** `anAlreadyRevokedBindingIsNotRewritten` and
   `nothingElseInTheDatabaseMoves` stay green — neither reads a session.
2. **Wrap the `DELETE` in `if (updated > 0)`.** Only
   `aPlayerWithNoLiveBindingStillLosesTheOtherSessions` reddens — every other test in the class
   revokes a player who *does* hold a live binding, so the branch is taken and nothing moves. One
   test, and it is the reason that test exists.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
