---
schema: 2
id: TASK-040805
title: Two duels that finished in the same instant still page
type: task
status: done
parent: STORY-0408
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, history, paging, tests]
depends_on: [TASK-040804]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Two duels with an identical `finished_at` are ordered totally and paged without a duplicate — the
tie-break column proved rather than assumed.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the row-value comparison and the `ORDER BY` it mirrors |

## Scope

- One test. Two duels for alice, recorded with the **same** `finishedAt` (
  `Instant.parse("2026-08-13T10:05:00Z")` for both), against `bob` and a second opponent so the join
  has two distinct rows to find.
- Read them in one page of two — **that read is the reference order**. Then read the same two in
  pages of one, using `TASK-040803`'s `everyPage(alice.id, 1)`, and compare.

## Out of scope

- Asserting *which* of the two comes first. **Never sort the two UUIDs in Kotlin to predict it**:
  `UUID.compareTo` compares two signed 64-bit halves while PostgreSQL's `uuid` type compares
  unsigned bytes, so the two disagree for any pair differing above bit 63. The reference order comes
  from the database's own two-row read, which is the only order that matters.
- Making the ids deterministic, seeding them, or choosing them to force a particular order. The
  property is *total and duplicate-free*, not *this one first*.

## Tests

`PostgresProfileReadsTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `twoDuelsInTheSameInstantPageWithoutADuplicate` | the ids read in two pages of one, concatenated, equal the ids read in one page of two, in the same order; and a third page is empty |

**The wrong implementations this must fail against**, all three of which pass every other test in
this file:

- `WHERE d.finished_at < ?` alone — the second page is empty, so the concatenation has one id where
  the reference has two;
- `<=` on the row value — the second page returns the first row again, so the concatenation has the
  same id twice;
- comparing `id` before `finished_at` — the two pages come back in the other order and the list
  equality fails.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.twoDuelsInTheSameInstantPageWithoutADuplicate` passes
- [ ] Both duels are recorded with the identical `finishedAt` value, written once and used twice
- [ ] The expected order is the database's own one-page-of-two read, and no Kotlin-side sort of
      UUIDs appears anywhere in the diff
- [ ] The test asserts list equality of ids **and** that the third page is empty
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
