---
schema: 2
id: TASK-021009
title: Make recording idempotent on the duel id, so a retry pays once
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 2
labels: [server, persistence, coins, idempotency]
depends_on: [TASK-021008]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Recording the same finished duel twice — a retry, a reconnect, a redelivery — writes its rows once
and awards its coins once.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql` (`duel.id` is the primary
key — the idempotency key),
`poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt`.

## Scope

- The duel insert becomes:

  ```sql
  INSERT INTO duel (id, format, started_at, finished_at) VALUES (?, ?, ?, ?)
  ON CONFLICT (id) DO NOTHING
  ```

  and `insertDuel` returns the `executeUpdate()` row count.
- When that count is `0`, the duel is already recorded: `record` commits the empty transaction and
  returns, writing no result row and moving no balance. When it is `1`, the rest of `record` runs
  exactly as it does now. Express it as a guard inside the existing `try`, for example
  `if (insertDuel(connection, duel) == 0) { connection.commit(); return@use }` — an early return
  from inside `use`/`withContext` needs the label, and it must not skip the commit that ends the
  transaction.
- A `why` comment on the branch: the duel's primary key *is* the idempotency key, so a second
  `record` of the same duel blocks on that unique index until the first transaction commits, then
  sees zero rows inserted and stops. That is what makes a retry safe without a second table or an
  application-level lock — and double-awarding a coin is the failure this ledger shape exists to
  make cheap to prevent and expensive to repair.
- `record` stays `suspend`, still returns `Unit`, still runs inside one transaction, and still
  rethrows a real `SQLException`. A conflicting duel id is not an error and must not throw.
- Extend the `record` KDoc: recording is idempotent on `FinishedDuel.id`; callers may retry.

## Out of scope

- A second `record` with the *same* id but different contents. There is no ticket for detecting
  that, and no caller can produce it: `STORY-0207`'s runner emits one finished duel per duel id.
- An `ON CONFLICT` clause on `duel_result` — the duel row already gates the write.
- Any change to how deltas are computed or balances updated.

## Tests

`PostgresDuelResultStoreTest`, the existing class, two tests added, using its existing helpers and
the `finishedDuel(winner = 0, id = …)` builder so both records name one id.

| Test | Proves |
| --- | --- |
| `recordingTheSameDuelTwiceAwardsCoinsOnce` | after `record(duel)` twice, `coinBalanceOf(alice.id) == 1` and `coinBalanceOf(bob.id) == -1` |
| `recordingTheSameDuelTwiceWritesItsRowsOnce` | after the same two calls, `duelRowCount() == 1` and `duelResultRowCount() == 2` |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.recordingTheSameDuelTwiceAwardsCoinsOnce` passes
- [ ] `PostgresDuelResultStoreTest.recordingTheSameDuelTwiceWritesItsRowsOnce` passes
- [ ] The seven tests already in `PostgresDuelResultStoreTest` still pass, with their assertions
      unchanged — a first `record` of a new duel id behaves exactly as before
- [ ] The duel insert in `PostgresDuelResultStore.kt` contains `ON CONFLICT (id) DO NOTHING`, and
      the second `record` throws nothing
- [ ] `PostgresDuelResultStore.kt` contains no `SELECT` on `duel` and no separate
      "already recorded?" query
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
