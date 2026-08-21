---
schema: 2
id: TASK-050205
title: Tied players share a rank, a rank is not a row's offset, and a tie may span a page boundary
type: task
status: ready
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, leaderboard, rank, tests]
depends_on: [TASK-050204]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresStandingsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0064` §1–§2 is pinned by tests rather than by the shape of the SQL: tied players read one
number, the next distinct standing skips, and the number is never the row's position in the page —
including across a page boundary, where a repeated rank is correct and is **not** a duplicate row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsCursor.kt` | read |

## Scope

- Three tests added to the existing class. **No production file changes** — if a test here fails,
  the defect is in `TASK-050204`'s SQL and the fix belongs in this ticket's diff, but the expected
  outcome is that `rank()` already satisfies all three.
- Every test records its own duels, per that class's `@BeforeEach` rule.
- The third test reads a page with `limit` smaller than the ladder and continues with
  `after = StandingsCursor(asOf, lastRow.coins, UUID.fromString(lastRow.playerId))`. This is the
  first exercise of the `after` variant of the SQL, so a broken row-value comparison shows up here.

## Out of scope

- **Ranks over HTTP.** `TASK-050212` walks the endpoint and asserts page two's ranks there. This
  ticket stays at the port.
- **A tie marker, a count of who shares a rank, or any field naming the order among equals** —
  `ADR-0064` §3–§5. Nothing is asserted about *which* of two tied players comes first, because
  nothing promises it.
- **`dense_rank`.** It is the convention `ADR-0064` rejected; `TASK-050204`'s `verify:` already
  greps for it and this ticket adds no second guard.
- Changing any assertion already in `PostgresStandingsReadsTest`.

## Tests

`PostgresStandingsReadsTest`, `-PrequireDocker=true`. Season `Season(2026, 8)`, every duel finished
`2026-08-1x`, `asOf = season.endExclusive`.

**Fixture A — `3, 3, 5`.** Six players, and these seven duels, whose standings sum to zero because
every duel does:

| Duels | Standing |
| --- | --- |
| a beats f, three times | a `+3`, f `-3` |
| b beats e, twice | b `+2` |
| c beats e | c `+1` |
| d beats e | d `+1`, e `-4` |

Ladder order: `a(+3) b(+2) {c,d}(+1) f(-3) e(-4)`; ranks `1, 2, 3, 3, 5, 6`. Which of `c` and `d`
comes first is decided by `player_id DESC` and is **never asserted**.

**Fixture B — `1, 1, 3`.** Three players: `t1` beats `x`, then `t2` beats `x`. Standings `t1 +1`,
`t2 +1`, `x -2`; ranks `1, 1, 3`.

| Test | Proves |
| --- | --- |
| `tiedPlayersReadTheSameRankAndTheNextDistinctStandingSkips` | over fixture A read whole (`limit = 6`), the rank sequence is exactly `[1, 2, 3, 3, 5, 6]` and the coins sequence is `[3, 2, 1, 1, -3, -4]` |
| `theRankIsNotTheRowsOffset` | over fixture B read whole (`limit = 3`), the rank sequence is exactly `[1, 1, 3]` — an implementation numbering rows from the page offset returns `[1, 2, 3]` and is red on page **one** |
| `aTieSpanningAPageBoundaryRepeatsTheRankAndEachPlayerOnce` | over fixture A in pages of three: page one's last rank and page two's first rank are both `3`, they are two **different** player ids, and the union of the two pages holds each of the six ids exactly once |

**Named mutations.** Replacing `rank()` with `dense_rank()` reddens the first test (`[1,2,3,3,4,5]`)
and the second (`[1,1,2]`). Replacing the rank column with a row counter over the page reddens the
second. Dropping the `after` predicate reddens the third by repeating page one's rows.

**The third test is not a duplicate-hunt on rank numbers.** Totality and disjointness are properties
of **players** (`ADR-0064` §2), so the assertion counts **player ids**, and the repeated `3` is
asserted as correct rather than filtered.

## Acceptance criteria

- [ ] `PostgresStandingsReadsTest.tiedPlayersReadTheSameRankAndTheNextDistinctStandingSkips` passes,
      asserting the literal rank sequence `[1, 2, 3, 3, 5, 6]`
- [ ] `PostgresStandingsReadsTest.theRankIsNotTheRowsOffset` passes, asserting the literal rank
      sequence `[1, 1, 3]`
- [ ] `PostgresStandingsReadsTest.aTieSpanningAPageBoundaryRepeatsTheRankAndEachPlayerOnce` passes,
      asserting the repeated rank across the boundary **and** each of the six player ids exactly once
- [ ] No test names which of two tied players sorts first
- [ ] `playersComeBackInCoinOrder` and `theWindowExcludesTheNeighbouringSeasonInBothDirections` pass
      with their assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
