---
schema: 2
id: TASK-040810
title: Over HTTP, against the database — every duel exactly once, and one player's cursor
type: task
status: backlog
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, db, history, paging, tests]
depends_on: [TASK-040809]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.DuelHistoryPagingDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Driven over real HTTP against a real PostgreSQL, the paged endpoint returns every one of a player's
duels exactly once, accepts the cursor it issued, and hands a second player nothing of the first
player's record.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/DuelHistoryPagingDatabaseTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileEndpointsDatabaseTest.kt` | read — the `freshDatabase()` + `Migrations.migrate` + `testApplication { module(); profileRoutes(…) }` pattern, and its inline `FinishedDuel` fixture |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | read — `freshDatabase()` |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read — `duelCursorOrNull` for decoding what came back |

## Scope

- One test class, set up exactly as `ProfileEndpointsDatabaseTest` does: `PostgresTestSupport
  .freshDatabase()`, `Migrations.migrate(dataSource)`, a `PostgresPlayerDirectory` to resolve
  devices, a `PostgresDuelResultStore` to record duels, and
  `testApplication { application { module(); profileRoutes(PostgresProfileReads(dataSource), PostgresProfileWrites(dataSource)) } }`.
- Its own private `finishedDuel(...)` helper — the one in `PostgresProfileReadsTest` is private to
  that class, and `SignUpDatabaseTest` set the precedent of a self-contained database test file.
- Decode every response with `protocolJson.decodeFromString<RecentDuelsResponse>(body)`, as the
  neighbouring database tests do, rather than by matching substrings.
- A private page walker that requests `/api/me/duels?limit=3`, then
  `/api/me/duels?limit=3&after=<nextCursor>` until `nextCursor` is `null`. **Cap it at 10 requests
  and `fail(...)`** — a cursor that does not advance must be a named failure in seconds, not a hung
  job.
- Alice's seven duels are all against bob, at `10:01`…`10:07`. Carol's one duel is against dave, at
  `10:00:30Z` — deliberately **older** than every cursor alice can issue, so the cross-player test
  reads something rather than trivially reading nothing.

## Out of scope

- Booting the whole `duelServer` with its socket and sweep loop. These three routes are what
  `duelServer` installs for HTTP, and the neighbouring database tests install them directly for the
  same reason: fewer moving parts between the assertion and the SQL.
- The `400` on a malformed cursor — `TASK-040808` asserts it at route level and nothing about the
  database changes it.
- Sign-in, sessions or `auth_session`. Identity here is the `X-Device-Id` header, which is the only
  credential the server has today.

## Tests

`DuelHistoryPagingDatabaseTest`, `-PrequireDocker=true`, one migrated database per test.

| Test | Proves |
| --- | --- |
| `everyDuelComesBackExactlyOnceOverThePagedEndpoint` | walking `?limit=3` returns pages of sizes `[3, 3, 1]`; the flattened duel ids equal the ids from a single `?limit=50` request, in the same order; each of the seven appears exactly once; and the third response carries `"nextCursor":null` |
| `theCursorTheServerIssuedIsTheCursorItAccepts` | page one's `nextCursor`, handed back **verbatim** in the query string, answers `200` — the encoding survives URL transport and `TASK-040801`'s canonical check, which is the property that would break the day a `+`, `/` or `=` reached the wire |
| `aCursorIssuedToOnePlayerReadsNoneOfThatPlayersDuels` | carol's device replays alice's page-one `nextCursor` and receives exactly her own one duel — none of alice's seven ids appear in the body, asserted against the full set of seven |

`STORY-0408` phrases the last one as *"a cursor issued to one player returns nothing for another
player"*. Taken literally that is only true when the second player has no older duels of their own,
because a keyset cursor is a position and not a permission — the read is keyed off the device the
server resolved, so the second player sees *their* rows before that instant and nothing else. The
testable, general property is the one above: **no cross-player row**, asserted with a second player
who does have a duel there, so the test cannot pass by returning an empty page.

## Acceptance criteria

- [ ] `everyDuelComesBackExactlyOnceOverThePagedEndpoint` passes and asserts page sizes `[3, 3, 1]`,
      the ordered id list against the single-request read, and no repeated id
- [ ] `theCursorTheServerIssuedIsTheCursorItAccepts` passes and sends the cursor string exactly as
      received, with no re-encoding of its own
- [ ] `aCursorIssuedToOnePlayerReadsNoneOfThatPlayersDuels` passes, carol's page is non-empty, and
      the assertion is against all seven of alice's ids
- [ ] The page walker fails with a named message after 10 requests rather than looping
- [ ] Every response is decoded into `RecentDuelsResponse`, not matched as a substring
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
