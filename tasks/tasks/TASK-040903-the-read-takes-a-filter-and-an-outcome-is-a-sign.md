---
schema: 2
id: TASK-040903
title: The read takes a filter, and an outcome is the sign of the stored delta
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, db, read-path, history, filters]
depends_on: [TASK-040902]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileReads` can be asked for only the duels a player won, only the ones they lost, or
only the ones they drew — in one query, with the value bound and never interpolated.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — one fixture helper, four tests |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — `DuelFilter`, `DuelFilter.NONE` |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/DuelOutcomes.kt` | read — `outcomeOf`, and `ADR-0014`'s reason for reading the sign |

## Scope — the SQL

`DUEL_LINES` gains **one line**, immediately after `WHERE r.player_id = ?` and inside the same raw
string, so both statements built from it get it and the join text stays one source
(`theCursorQueryAndTheFirstPageQueryShareOneJoinText` keeps passing untouched):

```sql
  AND (?::int IS NULL OR sign(r.coin_delta) = ?::int)
```

- **The clause is always present and neutralised by a bound `NULL`** — not appended conditionally.
  That is what keeps `RECENT_DUELS_SQL` and `DUELS_AFTER_SQL` two fixed strings with fixed bind
  positions, keeps `aListOfThreeDuelsPreparesExactlyOneStatement` true, and keeps the cursor
  predicate a plain row-value comparison rather than something assembled at runtime. It also means
  every existing test in this file passes **unchanged**: with `NULL` bound, the clause is `TRUE` for
  every row.
- **The sign, not the amount.** `outcomeOf` reads only the sign of `coin_delta`, on `ADR-0014`'s
  reasoning that *"a function that reads the sign will survive a future change to the award and one
  that compares against a literal will not"*. The filter is the inverse of that function and is
  written the same way. `sign(r.coin_delta) = 1` is right; `r.coin_delta = 1` is the wrong
  implementation this whole clause is shaped to avoid.
- `r` is the **requesting** player's `duel_result` row (`WHERE r.player_id = ?`), which is the same
  row `readDuelSummary` reads `coin_delta` from. The filter and the reported outcome therefore
  cannot disagree.
- **Verified against `postgres:16-alpine` before this ticket was written**: with the fixture
  `(1, 1), (2, -1), (3, 0)`, `PREPARE f(int) … ($1::int IS NULL OR sign(coin_delta) = $1::int)`
  answers all three rows for `NULL`, `{1}` for `1`, `{2}` for `-1` and `{3}` for `0`.

## Scope — the Kotlin

- One **new public overload**, with **no default value**:

  ```kotlin
  public suspend fun recentDuelsOf(
      playerId: PlayerId,
      limit: Int,
      after: DuelCursor?,
      filter: DuelFilter,
  ): List<DuelSummaryResponse>
  ```

  The existing three-argument `override` stays and becomes
  `recentDuelsOf(playerId, limit, after, DuelFilter.NONE)`. That is the same move `TASK-040802` made
  for the cursor and for the same reason: every existing two- and three-argument call site in
  `PostgresProfileReadsTest.kt` still resolves to the member it resolves to today and **none of them
  is edited**. `TASK-040907` is where the parameter reaches the port and the delegate goes away.
- Bind positions move by two, and the ticket states them so nobody counts twice:

  | Position | Value |
  | --- | --- |
  | 1 | `UUID.fromString(playerId.value)` |
  | 2 and 3 | the outcome's sign, or `setNull(_, Types.INTEGER)` |
  | 4 (no cursor) | `limit` |
  | 4, 5, 6 (cursor) | `finishedAt`, `duelId`, `limit` |

  **Two positions for one value** because JDBC has no named parameters: the `?` appears twice in the
  clause, so it is bound twice. Extract that into a small private helper rather than writing the
  branch out twice —

  ```kotlin
  private fun PreparedStatement.bindOutcome(first: Int, outcome: DuelOutcomeLabel?) {
      val sign = outcome?.let(::coinDeltaSignOf)
      for (index in first..first + 1) {
          if (sign == null) setNull(index, Types.INTEGER) else setInt(index, sign)
      }
  }
  ```

- `coinDeltaSignOf` is a private exhaustive `when` over `DuelOutcomeLabel` — `WON → 1`, `LOST → -1`,
  `DREW → 0` — with no `else` branch, so a fourth label would fail to compile rather than silently
  filter nothing.
- `recentDuelsOf`'s body is 22 lines today and detekt's `LongMethod` budget is 60; the helper above
  is what keeps the margin rather than spends it.
- Update the function's KDoc: what `filter` narrows, and that `DuelFilter.NONE` reads exactly what
  the three-argument call always has.

## Out of scope

- The opponent search clause — `TASK-040904`. Only the outcome line is added here.
- The port, the doubles, the route — `TASK-040907`, `TASK-040909`.
- An index on `coin_delta`. `STORY-0408` recorded that `duel.finished_at` and `duel_result.player_id`
  are unindexed and that a new `V<n>` would race `STORY-0410`'s migration number (`ADR-0029` §8);
  nothing here changes that, and the clause is deliberately not sargable in any case.
- Editing or deleting any existing test, helper or call site in `PostgresProfileReadsTest.kt`.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`. One new fixture helper beside
`threeDuelsAgainstThreeOpponents`:

```kotlin
/** Alice wins at 10:01, loses at 10:02 and draws at 10:03; returns outcome -> duelId. */
private suspend fun threeDuelsOneOfEachOutcome(): Map<DuelOutcomeLabel, String>
```

built from `finishedDuel(winner = 0 | 1 | null, id = …, finishedAt = …)` — `winner = 0` is alice,
`winner = 1` is the opponent, `null` is the draw — with `finishedAt` values at or after
`finishedDuel`'s default `startedAt` of `2026-08-13T10:00:00Z`.

| Test | Proves |
| --- | --- |
| `filteringByWonReturnsOnlyTheWonDuel` | `recentDuelsOf(alice.id, 10, null, DuelFilter(WON, null))` returns a single row whose `duelId` is the won duel's **and** whose `outcome` is `WON`. Fails against `r.coin_delta = 1` only if the award ever changes — but fails **today** against `>=`, against a reversed sign, and against a clause that filters nothing (three rows) |
| `filteringByLostReturnsOnlyTheLostDuel` | the same, for `LOST` and `-1`. This is the case a `sign(...) > 0`-style slip returns empty for |
| `filteringByDrewReturnsOnlyTheDrawnDuel` | the same, for `DREW` and `0`. This is the case a `coin_delta <> 0` guard would drop entirely — the exact mistake `ADR-0015` warns about and the reason the fixture holds all three |
| `noFilterStillReadsEveryOutcome` | the same fixture read with `DuelFilter.NONE` returns all three duel ids, newest first. Without it, a clause that accidentally matched nothing would leave the three tests above green only because each of them expects one row — and a filter that matched everything would leave them red, so the four together bracket the clause from both sides |

Each of the three filtered tests asserts the **exact single duel id and the row's `outcome`**, never
`size == 1` alone: a size assertion cannot tell the won duel from the drawn one.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.filteringByWonReturnsOnlyTheWonDuel` passes
- [ ] `PostgresProfileReadsTest.filteringByLostReturnsOnlyTheLostDuel` passes
- [ ] `PostgresProfileReadsTest.filteringByDrewReturnsOnlyTheDrawnDuel` passes
- [ ] `PostgresProfileReadsTest.noFilterStillReadsEveryOutcome` passes and asserts all three ids
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged, and no
      existing call site of `recentDuelsOf` in that file is edited
- [ ] `theCursorQueryAndTheFirstPageQueryShareOneJoinText` still passes — `DUEL_LINES`,
      `RECENT_DUELS_SQL` and `DUELS_AFTER_SQL` all still exist as `private const val`
- [ ] `aListOfThreeDuelsPreparesExactlyOneStatement` still passes
- [ ] No filter value appears in any SQL string: the only new text in `PostgresProfileReads.kt` is
      the clause above, with two `?` in it
- [ ] `PostgresProfileReads.kt` contains no string concatenation or `StringBuilder` that assembles
      SQL at runtime
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
