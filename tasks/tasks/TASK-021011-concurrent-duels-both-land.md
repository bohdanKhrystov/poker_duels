---
schema: 2
id: TASK-021011
title: Prove two duels finishing at once for one player both land
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, coins, concurrency]
depends_on: [TASK-021010]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreConcurrencyTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Duels that finish at the same moment for the same player all land: every coin delta is applied,
none is lost to a concurrent write, and nothing deadlocks.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreConcurrencyTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` (the SQL increment
and the player-id lock order it relies on),
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` (the setup and
helpers to mirror).

## Scope

- A new class, not an addition to `PostgresDuelResultStoreTest`: this one needs several profiles
  and a coroutine harness, and mixing it into the sequential class would obscure both.
- `@BeforeEach`: `PostgresTestSupport.freshDatabase()`, `Migrations.migrate(dataSource)`, a
  `PostgresPlayerDirectory` and a `PostgresDuelResultStore` over it. Private helpers
  `coinBalanceOf(playerId)`, `duelRowCount()`, `duelResultRowCount()`, mirroring the sequential
  test class.
- The many-duels test **alternates which seat the shared player occupies** — seat 0 in even
  iterations, seat 1 in odd ones, winning every duel either way. A `why` comment: this is what
  exercises the store's fixed player-id lock order. An implementation that locked the two `player`
  rows in seat order would have two transactions taking the same two rows in opposite orders, and
  PostgreSQL would abort one of them as a deadlock.
- Every `record` runs in its own `async` on `Dispatchers.IO`, awaited with `awaitAll` inside
  `runBlocking`, so a failure in any of them fails the test rather than disappearing.
- Each duel gets a fresh `UUID.randomUUID()` id, so idempotency never masks a lost update.

## Out of scope

- Any change to `PostgresDuelResultStore.kt`. If a test here fails, report it — the fix is a new
  ticket, and probably a change `TASK-021006` should have been reviewed for.
- Concurrent profile creation — `TASK-021004` covers that.
- Recording the *same* duel concurrently: `TASK-021009` pins the idempotency, and its guarantee
  under concurrency is the same primary key.
- Retry, backoff or serialisation-failure handling in the store. Nothing has asked for it; if this
  test proves it is needed, that is a new ticket.

## Tests

`PostgresDuelResultStoreConcurrencyTest`, JUnit 5, package `duels.poker.server.db`.

| Test | Proves |
| --- | --- |
| `twoDuelsFinishingAtOnceForOnePlayerBothLand` | with three profiles, two duels won by the shared player against two different opponents recorded concurrently, the shared player's balance is `2`, each opponent's is `-1`, and there are `2` duel rows and `4` result rows |
| `twentyConcurrentDuelsApplyEveryDelta` | twenty duels between two profiles, all won by the shared player and recorded concurrently with alternating seats, leave the winner at `20`, the loser at `-20`, `20` duel rows and `40` result rows, with no exception thrown |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreConcurrencyTest.twoDuelsFinishingAtOnceForOnePlayerBothLand` passes
- [ ] `PostgresDuelResultStoreConcurrencyTest.twentyConcurrentDuelsApplyEveryDelta` passes
- [ ] No file other than the new test class is added or changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
