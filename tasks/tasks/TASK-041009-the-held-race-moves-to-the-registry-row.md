---
schema: 2
id: TASK-041009
title: The held race moves to the registry row, and the probe that waits for it follows
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, fixtures, concurrency, identity]
depends_on: [TASK-041008]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesConcurrencyTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The held-transaction race holds the **registry** row rather than the `player` index entry, and the
`pg_stat_activity` probe waits for a writer blocked on that row — so the two held tests still prove
blocking rather than timing.

## Why this is its own ticket

`ADR-0051` §8 names this file as the one that *"changes in a way that will otherwise look like a
flake"*. `awaitLockWaitOn` polls for a backend waiting on a `Lock` while running a query matching
`ILIKE 'UPDATE player SET display_name%'`. Once the holder registers the name inside its open
transaction, the second writer blocks on `name_registry_folded` during its **`INSERT`** and never
reaches the `UPDATE` — the probe matches nothing, `withTimeout(30_000)` expires, and the failure
reads as a 30-second hang rather than as a stale string.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesConcurrencyTest.kt` | modify — one helper, one probe, two KDoc blocks |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read — the exact text of the registry `INSERT`, which the probe must match |

## Scope

- `updateDisplayName(connection, playerId, name)` — the holder's write — gains
  `INSERT INTO name_registry (name, reason) VALUES (?, 'TAKEN')` **on the same `connection`**, before
  its `UPDATE`. Same connection is the whole point: the row must be part of the caller's open,
  uncommitted transaction, which is what the second writer then blocks on. It keeps returning the
  `UPDATE`'s row count, so `assertEquals(1, updateDisplayName(...))` at both call sites is unchanged.
- `isBlockedOnNameWrite(excludingPid)` — the `ILIKE` pattern becomes
  `'INSERT INTO name_registry%'`. Keep `wait_event_type = 'Lock' AND pid <> ?`, keep the 5 ms poll,
  keep `withTimeout(30_000)`.

  **Keep a specific pattern.** Dropping the `ILIKE` and matching any lock wait would make the probe
  pass on a lock this test did not create — the probe would stop being evidence.
- The class KDoc and `awaitLockWaitOn`'s KDoc say what is now held: the registry row for the name,
  not the `player` unique index entry. Update both; a comment that describes the old mechanism is
  worse than none.
- Nothing else changes. `race`, `loserOf`, `winnerOf`, `backendPidOf`, `displayNameOf`,
  `countPlayersNamed` and all six test methods keep their bodies and assertions.

## What the two held tests then prove

| Test | Sequence, after this change |
| --- | --- |
| `secondWriterBlocksThenLosesWhenTheHolderCommits` | Holder registers `"heldthencommitted"` and writes it, uncommitted. The waiter's production `INSERT` blocks on `name_registry_folded`. The holder commits; the waiter takes `23505` and answers `NameTaken`; the waiter's transaction rolls back, so it holds nothing and has spent nothing |
| `secondWriterSucceedsWhenTheHolderRollsBack` | The same up to the block; the holder rolls back, releasing both its registry row and its `player` write; the waiter's `INSERT` and `UPDATE` both succeed and it answers `NameSet`. The holder ends nameless |

## Out of scope

- `PostgresProfileWrites` — read only. Its statement text is what the probe matches, so if that
  `INSERT` does not begin with `INSERT INTO name_registry` the fix belongs there and this ticket
  stops and says so rather than loosening the pattern.
- The four unheld race tests. They exercise the production path on both sides and need no fixture.
- The foreign key — `TASK-041010`.

## Tests

`PostgresProfileWritesConcurrencyTest`, `-PrequireDocker=true`. Nothing added; six existing tests
must pass, and `@Timeout(60)` on each is what turns a probe that matches nothing into a failure
rather than a hang.

**How to know the probe is falsifiable rather than decorative:** temporarily change the pattern to
`'INSERT INTO nothing%'` and both held tests must fail on the `withTimeout`, not pass. Do that check
locally; do not commit it.

| Test | Proves |
| --- | --- |
| `secondWriterBlocksThenLosesWhenTheHolderCommits` | The waiter is observed **waiting** before the holder commits, then loses. Fails against a probe that never matches (timeout) and against a holder that registers on a different connection (the waiter never blocks, the probe never matches, timeout again) |
| `secondWriterSucceedsWhenTheHolderRollsBack` | The same block, then the waiter wins and the holder ends with `NULL` |
| `twoPlayersSendingOneNameProduceOneWinner` | Ten unheld rounds still produce exactly one `NameSet` and one `NameTaken` each |
| `theLoserIsStillUnnamed`, `theLoserCanTakeAnotherNameImmediately`, `theWinnersNameIsTheOneStored` | The loser burns nothing and can name itself immediately; the winner's name is the stored one and exactly one player holds it |

## Acceptance criteria

- [ ] Every test in `duels.poker.server.db.PostgresProfileWritesConcurrencyTest` passes with its
      assertions unchanged
- [ ] `updateDisplayName` issues its registry `INSERT` on the `connection` parameter it was handed,
      never on `dataSource.connection`
- [ ] The `pg_stat_activity` predicate matches `'INSERT INTO name_registry%'` and still carries both
      `wait_event_type = 'Lock'` and `pid <> ?`
- [ ] The string `UPDATE player SET display_name%` no longer appears in the file
- [ ] The class KDoc and `awaitLockWaitOn`'s KDoc describe the registry row as what is held
- [ ] No test method is added, removed, renamed or has an assertion changed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
