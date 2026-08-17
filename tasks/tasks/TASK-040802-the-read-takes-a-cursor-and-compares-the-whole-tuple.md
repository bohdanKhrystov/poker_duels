---
schema: 2
id: TASK-040802
title: The read takes a cursor, and PostgreSQL compares the whole tuple
type: task
status: backlog
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, read-path, history, paging]
depends_on: [TASK-040801]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileReads` can read the duels **strictly after** a cursor — same order, same join, one
query — and the two-argument read it already has is byte-identical to what it was.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read — `DuelCursor(finishedAt, duelId)` |
| `docs/adr/ADR-0039-v01-offers-no-account-deletion.md` | read — why the join stays |

## Scope

- Add **one public overload** to `PostgresProfileReads`:

  ```kotlin
  public suspend fun recentDuelsOf(playerId: PlayerId, limit: Int, after: DuelCursor?): List<DuelSummaryResponse>
  ```

  **No default value on it.** The existing two-argument `override` stays and becomes
  `recentDuelsOf(playerId, limit, null)`, so every one of the ~20 existing call sites in this test
  file resolves unambiguously to the two-argument member and none of them changes. `TASK-040807`
  is where the parameter reaches the port and the two-argument member goes away.
- Split the SQL so the join has exactly one source text:

  ```kotlin
  private const val DUEL_LINES = """SELECT … FROM … WHERE r.player_id = ?"""
  private const val DUEL_ORDER = "ORDER BY d.finished_at DESC, d.id DESC LIMIT ?"
  private const val RECENT_DUELS_SQL = "$DUEL_LINES $DUEL_ORDER"
  private const val DUELS_AFTER_SQL = "$DUEL_LINES AND (d.finished_at, d.id) < (?::timestamptz, ?::uuid) $DUEL_ORDER"
  ```

  Two statements, one body. The existing comment block above the SQL stays with `DUEL_LINES`.
- **The comparison is the whole ticket.** `(d.finished_at, d.id) < (?, ?)` is a row-value
  comparison: PostgreSQL compares `finished_at` first and falls through to `id` only on a tie,
  which is exactly the sequence `ORDER BY d.finished_at DESC, d.id DESC` produces. Three wrong
  forms it must beat, each of which loses or repeats a duel:
  - `d.finished_at < ?` alone — drops every duel sharing the cursor's instant, including the other
    side of a tie;
  - `d.finished_at <= ? AND d.id < ?` — drops every older duel whose id happens to be larger;
  - `<=` instead of `<` — returns the cursor's own row again on every page boundary.
  The explicit `::timestamptz` / `::uuid` casts are there because parameter type inference inside a
  row constructor is not something to rely on.
- Bind in order: `setObject(1, UUID.fromString(playerId.value))`, then for the cursor
  `setObject(2, OffsetDateTime.ofInstant(after.finishedAt, ZoneOffset.UTC))` and
  `setObject(3, after.duelId)`, then `setInt(4, limit)`. Reading a row stays one private function
  used by both paths.
- **`ADR-0039`: `JOIN player p ON p.id = o.player_id` stays.** v0.1 offers no account deletion *and
  the schema may not foreclose one*, which forbids denormalising a display name into `duel_result`.
  Do not drop the join, cache the name, or copy it into a column to make paging cheaper. An
  opponent who holds no name still reads back `null`, exactly as today.
- Nothing in this SQL reads `name_registry` or `retired_from`. `ADR-0053` puts the retired-name
  `EXISTS` on the profile read and **only** there: the same expression pasted into a duel line
  publishes a takedown to a stranger.

## Out of scope

- An index or a migration. There is no index on `duel.finished_at` or on `duel_result.player_id`
  today, and adding one is a new `V<n>` that would race `STORY-0410`'s migration number
  (`ADR-0029` §8). Not yet ticketed; recorded in `STORY-0408`'s split notes.
- The port, the route, the response field, the walking of pages — `TASK-040803`…`TASK-040809`.
- Changing any existing test in this file.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`. Both new tests use the file's own
`finishedDuel(…)` helper, whose `finishedAt` must be at or after its default `startedAt` of
`2026-08-13T10:00:00Z`.

| Test | Proves |
| --- | --- |
| `aCursorReadsOnlyTheDuelsOlderThanIt` | five duels for alice at `10:01`…`10:05`; read all five with the two-argument call, build `DuelCursor` from the **second** row (`Instant.parse(row.finishedAt)`, `UUID.fromString(row.duelId)`), then `recentDuelsOf(alice.id, 10, thatCursor)` returns exactly the third, fourth and fifth rows' ids, in that order. Fails against `<=` (four rows, the cursor's own repeated), against a reversed comparison (the two newer rows), and against no filter at all (five rows) |
| `aCursorAtTheOldestDuelReadsNothing` | the same fixture, cursor built from the **fifth** row: the result is empty. Asking past the end is an empty page, not an error and not a wrap-around to the newest |

Both assert on the whole list of ids, not on `size` alone — a size assertion cannot tell the third,
fourth and fifth rows from any other three.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aCursorReadsOnlyTheDuelsOlderThanIt` passes and asserts the exact
      list of three duel ids in order
- [ ] `PostgresProfileReadsTest.aCursorAtTheOldestDuelReadsNothing` passes
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged, and no
      existing call site of `recentDuelsOf` in that file is edited
- [ ] `aListOfThreeDuelsPreparesExactlyOneStatement` still passes — both paths prepare one statement
      per call
- [ ] The `SELECT`, `FROM`, `JOIN` and `WHERE r.player_id = ?` text appears **once** in
      `PostgresProfileReads.kt`, shared by both statements
- [ ] `JOIN player p ON p.id = o.player_id` is still in that shared text
- [ ] The cursor predicate is a row-value comparison with `<`, not two column comparisons
- [ ] Neither `name_registry` nor `retired_from` appears anywhere in `PostgresProfileReads.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
