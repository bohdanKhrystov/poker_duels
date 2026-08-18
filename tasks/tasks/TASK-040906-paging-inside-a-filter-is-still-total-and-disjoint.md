---
schema: 2
id: TASK-040906
title: Paging inside a filter is still total and disjoint, across an insert that matches it
type: task
status: ready
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, read-path, history, paging, filters, tests]
depends_on: [TASK-040905]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`STORY-0408`'s guarantee — every duel exactly once, no gap, no duplicate, even across an insert — is
asserted again **inside a filter**, which is where a cursor and a predicate could disagree.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify — one helper signature, one fixture, two tests |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the clauses and the cursor predicate under test |
| `tasks/tasks/TASK-040804-a-duel-that-finishes-between-two-pages.md` | read — the insert-between-pages shape these tests copy |

## Scope

- **No production code changes.** The filter clauses and the row-value cursor comparison already
  compose: both are `AND`ed into one `WHERE`, and the `ORDER BY` is unchanged, so the cursor still
  names a row rather than a position. That is the claim; these two tests are what turns it from a
  claim into a gate.
- `everyPage` gains a fourth parameter, `filter: DuelFilter = DuelFilter.NONE`, and passes it to
  `recentDuelsOf`. The default is what keeps **every existing call site of `everyPage` untouched** —
  `everyDuelIsReadExactlyOnceInPagesOfThree`, `aDuelRecordedBetweenTwoPagesRepeatsNothingAndSkipsNothing`
  and `twoDuelsInTheSameInstantPageWithoutADuplicate` are not edited. Its KDoc gains one sentence.
- One new fixture helper:

  ```kotlin
  /**
   * Eleven duels for alice a minute apart from 10:01 to 10:11; alice loses the ones at 10:02,
   * 10:05, 10:07 and 10:10 and wins the other seven. Returns them newest-first, exactly as
   * recentDuelsOf does.
   */
  private suspend fun elevenDuelsFourOfThemLost(): List<DuelSummaryResponse>
  ```

  **Seven wins, paged three at a time** — because seven is not a multiple of three, so the walk has
  a partial last page and a fixture that divided evenly could not show one. **The four losses are
  interleaved, not appended**, so a walk that dropped the filter after the first request would put
  a loss on page one and be caught by the very first assertion rather than by a count at the end.

## Out of scope

- The route, HTTP, and the `after` query parameter — `TASK-040909`, `TASK-040910`.
- **A cursor issued under one filter and replayed under another.** That is `DEC-050`, unanswered:
  `STORY-0409` requires such a cursor be *refused rather than silently reinterpreted*, and no
  mechanism for telling the two apart exists yet. Both tests here keep **one** filter for the whole
  walk, which is the only thing today's cursor can express. Do not add a test that changes the
  filter mid-walk: whatever it asserted would have to be undone by the ticket that answers
  `DEC-050`.
- Any index, any migration.
- Editing any existing test in this file.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `everyMatchingDuelIsReadExactlyOnceInPagesOfThree` | `everyPage(alice.id, pageSize = 3, filter = DuelFilter(WON, null))` over the eleven-duel fixture returns page **sizes** `[3, 3, 1]`; the flattened duel ids equal the seven won ids newest-first; `distinct().size == 7`; every returned row's `outcome` is `WON`; and **no** id of the four lost duels appears. Sizes `[3, 3, 3, 2]` is what a lost filter looks like, and any repeat or gap moves the flattened list — a size assertion alone would catch neither |
| `aMatchingDuelRecordedBetweenTwoFilteredPagesRepeatsNothingAndSkipsNothing` | read page one under the `WON` filter (`10:11, 10:09, 10:08`); **then** record an eighth won duel at `10:12`; then walk every remaining page from page one's own last row's cursor, under the same filter. Asserts: page one's last row never reappears; the `10:12` duel never appears, since it finished after the cursor was cut and therefore sits on the already-passed side of it; and page one plus every later page is exactly the seven original won ids, each once. This is `aDuelRecordedBetweenTwoPagesRepeatsNothingAndSkipsNothing` with a predicate in the way — the case where a filter applied *after* paging, or a cursor compared against the wrong column, silently loses a row |

Both build the cursor the way the file already does — `DuelCursor(Instant.parse(row.finishedAt),
UUID.fromString(row.duelId))` — the same round trip through the wire's string form a real client
makes. `everyPage`'s twenty-request cap is what turns a cursor that stops advancing into a failure
in seconds rather than a hang.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.everyMatchingDuelIsReadExactlyOnceInPagesOfThree` passes and asserts
      page sizes `[3, 3, 1]`, the exact seven ids in order, and that no lost duel id is present
- [ ] `PostgresProfileReadsTest.aMatchingDuelRecordedBetweenTwoFilteredPagesRepeatsNothingAndSkipsNothing`
      passes and asserts all three of: no repeat of page one's last row, no appearance of the duel
      recorded mid-walk, and the seven original ids each exactly once
- [ ] The fixture holds both won and lost duels **interleaved in time**, and seven is not a multiple
      of the page size
- [ ] `everyPage`'s new parameter has a default, and none of its three existing call sites is edited
- [ ] `poker-server/src/main/kotlin/` is not in the diff
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
