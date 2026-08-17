---
schema: 2
id: TASK-040804
title: A duel that finishes between two pages repeats nothing and skips nothing
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, history, paging, tests]
depends_on: [TASK-040803]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A duel recorded **between** two page requests returns no already-read duel a second time and hides
none of the ones still to come — the property that `LIMIT`/`OFFSET` cannot have and the reason this
story exists.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read |

## Scope

- One test, reusing `TASK-040803`'s `everyPage(playerId, pageSize, from)` helper and the same
  seven-duel fixture shape.
- The sequence is the point, so write it in this order and nothing else:
  1. record seven duels for alice at `10:01`…`10:07`;
  2. read **one** page of three — the newest three, `10:07, 10:06, 10:05`;
  3. record an **eighth** duel at `10:08`, newer than every one of them;
  4. continue from the cursor of page one's last row and read every remaining page.
- The assertion is a multiset over the **original seven**, not a size.

## Out of scope

- Concurrency in the threading sense. Nothing here runs in parallel: "between two requests" means
  between two sequential calls, which is exactly the race a reader on a live server sees.
- A duel inserted *older* than the cursor. It legitimately appears on a later page and nothing about
  it is surprising; the interesting insert is the one at the head, which is what shifts offsets.
- The route, the wire and the client.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `aDuelRecordedBetweenTwoPagesRepeatsNothingAndSkipsNothing` | page one's three ids plus every later page's ids contain all **seven** original duel ids, each **exactly once**; no id from page one appears again after it; and the eighth duel's id appears in none of the later pages |

**The wrong implementation this must fail against is `LIMIT 3 OFFSET 3`.** Worked through with the
fixture above: after the eighth duel is inserted the newest-first order is
`8, 7, 6, 5, 4, 3, 2, 1`; page one was read before the insert and held `7, 6, 5`; an offset reader's
second page is `OFFSET 3` into the new order, which is `5, 4, 3` — duel **5 twice**. So assert the
multiset, not the size: the totals happen to differ here too, but a size assertion would pass the
moment the fixture changed shape, and the duplicate is the actual defect.

Assert with a grouping (`groupingBy { it.duelId }.eachCount()`) or by comparing a sorted id list
against the seven recorded ids — anything that names *which* id repeated when it fails. A bare
`assertEquals(7, ids.toSet().size)` hides both the duplicate and the gap.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aDuelRecordedBetweenTwoPagesRepeatsNothingAndSkipsNothing` passes
- [ ] The eighth duel is recorded **after** the first page has been read and **before** the rest are
- [ ] The eighth duel finishes later than all seven — it is inserted at the head of the order
- [ ] The test asserts each of the seven original ids appears exactly once across all pages read,
      by id and not by count alone
- [ ] The test asserts the eighth duel's id appears in no page after the first
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
