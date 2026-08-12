---
schema: 2
id: TASK-021007
title: Prove a failure part-way through recording leaves no row and no coin behind
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: XS
tier: haiku
review: deep
files_touched: 1
labels: [server, persistence, coins, transactions]
depends_on: [TASK-021006]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A `record` that fails part-way leaves the database exactly as it was: no duel row, no result row,
no moved balance — proved with a real failure rather than a mock.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt`,
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql` (`duel_result.player_id`
references `player (id)` — that foreign key is the induced failure).

## Scope

- Add exactly one test to the existing `PostgresDuelResultStoreTest`, using its existing helpers.
  No existing test, helper or assertion changes.
- The failure is induced with the schema, not with a mock or a fake `DataSource`: seat 1 is a
  `PlayerId(UUID.randomUUID().toString())` that was never resolved into a profile, so its
  `duel_result` insert violates the foreign key. Build it as
  `finishedDuel(winner = 0).copy(seats = listOf(alice.id, PlayerId(UUID.randomUUID().toString())))`.
- It is a *part-way* failure by construction: the `duel` row is inserted before any result row, so
  by the time the foreign key fires there is already work in the transaction to undo.
- The test asserts, in this order: `record` fails with a `SQLException`
  (`assertFailsWith<SQLException> { runBlocking { store.record(duel) } }`), then
  `duelRowCount() == 0`, `duelResultRowCount() == 0`, and `coinBalanceOf(alice.id) == 0`.
- A `why` comment: the third assertion is the point of the ticket — a duel that failed to record
  must not have paid anybody, or the ladder stops matching its own history.

## Out of scope

- Any change to `PostgresDuelResultStore.kt`. If the test fails, report it; do not edit the store
  from this ticket.
- Simulating a process crash between statements, a killed connection or a network partition. The
  transaction boundary is what is under test, and the foreign key exercises it deterministically.
- Retrying a failed record — `TASK-021009` makes a retry safe.

## Tests

`PostgresDuelResultStoreTest`, the existing class, one test added.

| Test | Proves |
| --- | --- |
| `aDuelNamingAnUnknownPlayerLeavesNoRowAndNoCoinBehind` | `record` throws `SQLException`, and afterwards `duelRowCount() == 0`, `duelResultRowCount() == 0` and `coinBalanceOf(alice.id) == 0` |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.aDuelNamingAnUnknownPlayerLeavesNoRowAndNoCoinBehind` passes
- [ ] The four tests already in `PostgresDuelResultStoreTest` still pass, with their assertions
      unchanged — this ticket only adds a test method
- [ ] No file other than `PostgresDuelResultStoreTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
