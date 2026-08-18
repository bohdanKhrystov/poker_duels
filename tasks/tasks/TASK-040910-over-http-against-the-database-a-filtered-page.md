---
schema: 2
id: TASK-040910
title: Over HTTP, against the database — a filtered page is exactly the filtered rows
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, http, db, history, filters, e2e]
depends_on: [TASK-040909]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelHistoryFilterDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A query string reaches the SQL: `?outcome=` and `?opponent=` on the real endpoint, against real
PostgreSQL, return exactly the duels the filter names.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryFilterDatabaseTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | read — the `freshDatabase()` + `Migrations.migrate` + `testApplication { module(); profileRoutes(…) }` pattern, its `FinishedDuel` fixture and its `protocolJson` decoding |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | read — `setPlayerDisplayName` |

## Scope

- **This is the only test in the story that runs a query string all the way into SQL.** The route
  tests in `ProfileRouteTest` use `FakeProfileReads` and prove the parameters are *parsed and
  handed over*; `PostgresProfileReadsTest` proves the SQL *filters*; neither can catch a route that
  hands the filter to the wrong argument, and this one can.
- One new file with a single top-level class named after it, so ktlint's `standard:filename` rule is
  satisfied by construction. Any constant it needs is a file-level `private const val`, exactly as
  `DuelHistoryPagingDatabaseTest.kt` declares `PAGE_LIMIT` and friends.
- Fixture, built once in `@BeforeEach` against `PostgresTestSupport.freshDatabase()`: alice plays
  four duels — she **wins** against bob (named `Halvard`) at `10:01`, **loses** to carol (named
  `Halvardsen`) at `10:02`, **draws** with dave (unnamed) at `10:03`, and **wins** again against
  carol at `10:04`. Two wins, so a `?outcome=WON` page has two rows rather than one and cannot pass
  by returning whichever row happened to be first.
- Responses are decoded with `protocolJson.decodeFromString<RecentDuelsResponse>(...)`, as the
  neighbouring database test does.
- Every request carries `header(DEVICE_ID_HEADER, "alice")`.

## Out of scope

- **Sending `after` together with a filter.** `DEC-050` is unanswered — see `TASK-040909`'s
  out-of-scope note. No request in this file carries `after`, and no assertion here concerns
  `nextCursor` beyond the one row below.
- Re-proving paging totality inside a filter. `TASK-040906` owns that, at the level where the cursor
  actually lives.
- Re-proving the parser rules. A single `400` here is enough to show the refusal survives the round
  trip; `DuelFilterTest` enumerates which strings are refused.
- Any change to an existing file.

## Tests

`DuelHistoryFilterDatabaseTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `filteringByOutcomeOverHttpReturnsOnlyThatOutcome` | `GET /api/me/duels?outcome=WON` answers `200`, and the decoded `duels` are exactly the two won duel ids, newest first, each with `outcome == WON`. Then `?outcome=DREW` answers the single drawn duel. Two different values against one fixture, because one value alone cannot tell a working filter from a route that ignores the parameter and a fixture that happens to match |
| `searchingAnOpponentOverHttpReturnsOnlyThatOpponentsDuels` | `?opponent=halvardsen` — lower-cased, against the stored `Halvardsen` — answers exactly carol's two duel ids, and bob's duel (`Halvard`, a prefix of the term) and dave's (unnamed) are absent. Proves the case fold, the direction of the match and the exclusion of an unnamed opponent in one request over the real stack |
| `bothFiltersComposeOverHttp` | `?outcome=WON&opponent=Halvardsen` answers exactly one duel — alice's `10:04` win against carol — where `?outcome=WON` alone answers two and `?opponent=Halvardsen` alone answers two. The intersection is smaller than either side, which is what makes this non-vacuous: two clauses `OR`ed together, or one clause silently dropped, both answer two |
| `anEmptyResultIsTwoHundredWithAnEmptyPage` | `?opponent=Sigrid` — a name no opponent holds — answers `200`, `duels` empty and `nextCursor` `null`. Not `404`: an empty filter result is a valid answer, and `404` would also tell a caller something about which names exist |
| `anUnusableOutcomeIsFourHundredOverHttp` | `?outcome=won` answers `400` with an empty body, so the refusal survives `ContentNegotiation` and is not turned into an empty `200` page somewhere in the stack |

## Acceptance criteria

- [ ] `DuelHistoryFilterDatabaseTest.filteringByOutcomeOverHttpReturnsOnlyThatOutcome` passes and
      asserts two different outcome values against one fixture
- [ ] `DuelHistoryFilterDatabaseTest.searchingAnOpponentOverHttpReturnsOnlyThatOpponentsDuels`
      passes, with the term in a different case from the stored name
- [ ] `DuelHistoryFilterDatabaseTest.bothFiltersComposeOverHttp` passes and asserts the intersection
      is strictly smaller than either single-axis result
- [ ] `DuelHistoryFilterDatabaseTest.anEmptyResultIsTwoHundredWithAnEmptyPage` passes and asserts
      `nextCursor` is `null`
- [ ] `DuelHistoryFilterDatabaseTest.anUnusableOutcomeIsFourHundredOverHttp` passes
- [ ] Every assertion names exact duel ids, never a size alone
- [ ] No request in the file carries an `after` parameter
- [ ] No existing file is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
