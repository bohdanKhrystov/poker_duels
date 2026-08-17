---
schema: 2
id: TASK-040803
title: Seven duels in pages of three, each exactly once
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, history, paging, tests]
depends_on: [TASK-040802]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Walking a player's whole record a page at a time returns every duel exactly once, in one order,
with no gap and no duplicate — proved at a page size that does not divide the record.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the three-argument `recentDuelsOf` |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read |

## Scope

- One private helper in the test class, used by this ticket and the two after it:

  ```kotlin
  private suspend fun everyPage(
      playerId: PlayerId,
      pageSize: Int,
      from: DuelCursor? = null,
  ): List<List<DuelSummaryResponse>>
  ```

  It reads a page, appends it **as a page**, and continues from a `DuelCursor` built from that
  page's **last** row (`Instant.parse(row.finishedAt)`, `UUID.fromString(row.duelId)`), stopping
  when a page comes back empty. The empty page ends the walk and is **not** appended, so seven
  duels in pages of three answer three pages from four requests. It answers the pages, not a flat
  list, so the page *sizes* are assertable; the tests flatten it themselves.
- It stops on **empty**, never on *shorter than `pageSize`*. The short-page shortcut is the same
  guess the route's probe row exists to avoid, and a test that made it could not observe the bug it
  is here to catch.
- **The loop is capped and the cap fails loudly**: at most 20 requests, then `fail("…")`. A cursor
  that does not advance is a bug this suite must report in seconds, not an infinite loop that hangs
  CI until the job times out.
- Fixture: seven duels for alice against bob at `10:01`…`10:07` (distinct instants, all at or after
  `finishedDuel`'s default `startedAt` of `10:00:00Z`).

## Out of scope

- Anything about a **concurrent** insert — `TASK-040804`, which is the test that separates keyset
  paging from `LIMIT`/`OFFSET`.
- Ties on `finished_at` — `TASK-040805`.
- The route and the wire — `TASK-040808`, `TASK-040809`, `TASK-040810`.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `everyDuelIsReadExactlyOnceInPagesOfThree` | with seven duels recorded and `pageSize = 3`: the pages are `3, 3, 1` in size (**three** pages), the flattened list of ids equals the ids of `recentDuelsOf(alice.id, 10)` in the same order, and the flattened list has size seven with no id twice |
| `theCursorOfTheLastRowReadsAnEmptyPage` | continuing from a cursor built on the seventh row returns an empty list, and doing it a second time returns an empty list again — past the end is empty, never an error and never the newest page |

**Seven and three are chosen so the boundary is observable.** A record that divides evenly by the
page size cannot tell a correct final page from one that silently drops the remainder, and a single
page cannot distinguish paging from no paging at all. Assert the page sizes as a list — `[3, 3, 1]`
— not just the total.

**What this test does *not* catch, and why the next one exists**: a `LIMIT`/`OFFSET` reader passes
this test perfectly, because nothing changes between the pages. What it does catch is `<=` in the
predicate (a duplicate at every boundary, total nine), a cursor taken from the first row of a page
instead of the last (a gap, total three), and an order that differs between pages (the list
equality fails).

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.everyDuelIsReadExactlyOnceInPagesOfThree` passes and asserts the page
      sizes `[3, 3, 1]`, the full id list in order, and that no id appears twice
- [ ] `PostgresProfileReadsTest.theCursorOfTheLastRowReadsAnEmptyPage` passes and asks twice
- [ ] `everyPage` fails with a named message after 20 requests rather than looping
- [ ] The fixture records seven duels and the test asserts seven were read — the sweep is non-empty
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
