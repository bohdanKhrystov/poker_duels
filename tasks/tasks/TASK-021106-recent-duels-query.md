---
schema: 2
id: TASK-021106
title: Read a player's recent duels with their opponent in one query
type: task
status: backlog
parent: STORY-0211
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, persistence, duel]
depends_on: [TASK-021105]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`PostgresProfileReads.recentDuelsOf(playerId, limit)` returns that player's finished duels — the
opponent, the outcome, the signed delta and the finish time — from a single statement, newest
first.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/DuelOutcomes.kt` (`outcomeOf`),
`poker-server/src/main/resources/db/migration/V1__initial_schema.sql`,
`docs/adr/ADR-0015-a-draw-writes-two-result-rows.md`.

## Scope

- Add one method to the `ProfileReads` port, with KDoc saying the list is newest first, is capped
  by `limit`, and contains only duels this player sat in:

  ```kotlin
  public suspend fun recentDuelsOf(playerId: PlayerId, limit: Int): List<DuelSummaryResponse>
  ```

- Implement it in `PostgresProfileReads` with **one statement**, the opponent found by joining
  `duel_result` to itself:

  ```sql
  SELECT d.id AS duel_id,
         o.player_id AS opponent_id,
         r.coin_delta AS coin_delta,
         d.finished_at AS finished_at
  FROM duel_result r
  JOIN duel d ON d.id = r.duel_id
  JOIN duel_result o ON o.duel_id = r.duel_id AND o.player_id <> r.player_id
  WHERE r.player_id = ?
  ORDER BY d.finished_at DESC, d.id DESC
  LIMIT ?
  ```

  Bind the player with `UUID.fromString(playerId.value)` and the limit with `setInt`. The `d.id`
  tie-break is deliberate: two duels can share a `finished_at`, and a list whose order depends on
  which row PostgreSQL happens to return is a list whose test is flaky.
- **The self-join is what `ADR-0015` bought.** Every participant of every completed duel has one
  `duel_result` row, so the opponent is always there and a drawn duel is not a special case. Put
  that in a `why` comment naming the ADR — a reader who trusts `V1__initial_schema.sql`'s comment
  instead would delete this join's premise.
- Map each row to a `DuelSummaryResponse`: `duelId` and `opponentPlayerId` as the text form of the
  UUIDs, `coinDelta` from the column, `outcome = outcomeOf(coinDelta)` — this file makes no other
  decision about who won — and
  `finishedAt = rows.getObject("finished_at", OffsetDateTime::class.java).toInstant().toString()`,
  which is ISO-8601 in UTC.
- `handsPlayed = null`, with a `why` comment: the `duel` table has no `hands_played` column and
  `DEC-014` decides whether it gains one. No number is invented here, and no other column stands in
  for it.
- Still `SELECT` only, still inside `withContext(Dispatchers.IO)`, still no `INSERT`/`UPDATE`.

## Out of scope

- Ordering, capping and isolation proofs — `TASK-021107` adds those tests against this query.
- The drawn-duel proof — `TASK-021108`.
- Filling `handsPlayed` — blocked on `DEC-014`, and not ticketed in this story.
- Any route, and any `N+1`: a second query per duel to fetch the opponent is exactly what this
  ticket exists to avoid.

## Tests

`PostgresProfileReadsTest`, the existing class, four tests added, using the `finishedDuel(...)`
builder and the `PostgresDuelResultStore` already there. No existing assertion changes.

| Test | Proves |
| --- | --- |
| `aRecordedDuelComesBackWithItsOpponentOutcomeAndDelta` | after `record(finishedDuel(winner = 0, id = duelId))`, `recentDuelsOf(alice.id, 10)` has one entry with `duelId` as text, `opponentPlayerId == bob.id.value`, `outcome == WON` and `coinDelta == 1` |
| `theLoserSeesTheSameDuelAsALoss` | `recentDuelsOf(bob.id, 10)` has one entry for the same `duelId`, with `opponentPlayerId == alice.id.value`, `outcome == LOST` and `coinDelta == -1` |
| `theFinishTimeComesBackAsTheStoredInstant` | the entry's `finishedAt` equals the fixture's `finishedAt.toString()` |
| `aPlayerWithNoDuelsGetsAnEmptyList` | with a third profile `carol` resolved and no duel of hers recorded, `recentDuelsOf(carol.id, 10)` is empty — empty, not an error |
| `handsPlayedIsNullWhileTheColumnDoesNotExist` | the entry's `handsPlayed` is `null`, so no coder fills it with a guess before `DEC-014` is answered |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aRecordedDuelComesBackWithItsOpponentOutcomeAndDelta` passes
- [ ] `PostgresProfileReadsTest.theLoserSeesTheSameDuelAsALoss` passes
- [ ] `PostgresProfileReadsTest.theFinishTimeComesBackAsTheStoredInstant` passes
- [ ] `PostgresProfileReadsTest.aPlayerWithNoDuelsGetsAnEmptyList` passes
- [ ] `PostgresProfileReadsTest.handsPlayedIsNullWhileTheColumnDoesNotExist` passes
- [ ] The six tests already in `PostgresProfileReadsTest` pass with their assertions unchanged
- [ ] `PostgresProfileReads.kt` contains exactly two SQL strings, and the second contains
      `JOIN duel_result o`, `ORDER BY d.finished_at DESC` and `LIMIT ?`
- [ ] `PostgresProfileReads.kt` still contains no `INSERT`, `UPDATE`, `DELETE` or `ON CONFLICT`
- [ ] `PostgresProfileReads.kt` decides the outcome only by calling `outcomeOf`: it contains no
      `> 0`, `< 0` or coin literal of its own
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
