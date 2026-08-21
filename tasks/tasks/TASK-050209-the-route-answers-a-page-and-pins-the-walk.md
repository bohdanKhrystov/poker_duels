---
schema: 2
id: TASK-050209
title: The route answers a page, names the season it computed, and pins the walk to one cutoff
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, leaderboard, route]
depends_on: [TASK-050208]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsRouteTest'
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
  - test 2 -eq "$(grep -c queryParameters poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt)"
  - "! grep -qF 'ServerClock' poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt"
---

## Goal

`GET /api/standings` exists, answers one page of the current season's ladder with the season it
computed for, and gives every request in one walk the same cutoff — minted from the clock when there
is no cursor, and read back out of the cursor when there is one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | read |

## Scope

- One installer, taking both ports and the wall clock:

  ```kotlin
  public fun Application.standingsRoutes(reads: ProfileReads, standings: StandingsReads, clock: Clock)
  ```

  `clock` is `java.time.Clock` (`ADR-0062`) and has **no default**: the one `Clock.systemUTC()` in
  this server lives at the composition root (`TASK-050201`). `ServerClock` is `System.nanoTime()`
  and would put every season in 1970. A `verify:` line greps this file for the name, **including its
  KDoc** — say *the wall clock* and link `ADR-0062` rather than naming the wrong instrument to warn
  against it.
- The handler, in this order, extracted into a private suspend function so `standingsRoutes` stays
  inside detekt's `LongMethod` budget — `ProfileRoutes.respondWithDuels` is the worked example:

  1. `val season = currentSeason(clock)`;
  2. `limit` from `duelLimitOrNull(request.queryParameters["limit"])` — the **same** parser
     `GET /api/me/duels` uses, so the ladder inherits its default of `10`, its cap of `50` and its
     one refusal vocabulary. There is no second limit parser in this product;
  3. `after`: absent means the first page of a new walk; present means
     `standingsCursorOrNull(raw, season)`;
  4. `val asOf = cursor?.asOf ?: clock.instant()` — **`ADR-0066` §2**: a cursorless request mints
     the cutoff, and every later request in that walk carries the same instant back inside the
     cursor. The clock is read once per request and never again;
  5. `standings.standingsPage(season, asOf, limit + 1, cursor)` — one row more than the page, the
     probe idiom `recentDuelsPage` documents;
  6. the self object (below);
  7. `StandingsResponse(season.toString(), rows, nextCursor, self)`.
- **`nextCursor` is minted from the row actually served last** — `StandingsCursor(asOf, lastRow.coins,
  UUID.fromString(lastRow.playerId)).encoded()`, with the **same** `asOf`, and is `null` when the
  probe row is absent. A cursor minted from the probe names a row one past the page served and the
  row between the two would never be returned; `recentDuelsPage`'s KDoc says *"silently, and
  forever"* and it is the same trap here.
- **The self object**: `deviceIdOrNull()` → `reads.profileOf(it)` → `standings.standingOf(...)`,
  under the **same** `season` and `asOf`. Its three shapes (`ADR-0065` §4): both numbers when the
  player has a row; `SelfStandingResponse(playerId, null, null)` when `standingOf` answers `null`;
  and the whole object `null` when there is no device header or the device is unknown — decided
  before any SQL runs (`ADR-0066` §6).
- **This route does not authenticate.** No `401` anywhere in it: the ladder is readable by a client
  with no profile, and the page is identical for every reader (`ADR-0065` §4). This is the one
  behaviour that differs from every other route in this package, so the KDoc says so out loud.
- KDoc contracts every parameter and every refusal, in the register `profileRoutes`' KDoc uses.

## Out of scope

- **Installing the route on the shipped server** — `TASK-050212`, which is the only ticket that
  touches `Application.kt`.
- **A `playerId`, a *jump to me*, a season parameter or a filter** — `ADR-0065` §3 and §5,
  `ADR-0061` §7. A `verify:` line counts the query parameters this file reads and requires exactly
  **two**, `limit` and `after`, so a third of any name fails the gate; `DEC-057` and `DEC-060` are
  open and this route pre-empts neither.
- **A `PROTOCOL_VERSION` step or a row in `docs/protocol-versions.md`.** These are plain HTTP
  endpoints and are not `ServerMessage`s.
- **The tests for the probe, the last page, the empty ladder and the refusals** — `TASK-050210` and
  `TASK-050211`. This ticket **implements** all of that behaviour; those two pin it.
- Touching `ProfileRoutes.kt`. `deviceIdOrNull`, `DEVICE_ID_HEADER` and `duelLimitOrNull` are
  reused from the same package unchanged.

## Tests

`StandingsRouteTest`, in `duels.poker.server.http`, using `testApplication` and **fakes** — no
database. The fake `StandingsReads` records every `(season, asOf, limit, after)` it is called with
and returns a list the test supplies; the fake `ProfileReads` answers a `ProfileResponse` for one
known device and `null` for anything else. `AuthRouteDoubles.kt` is the model for how a double lives
beside its test.

The clock is `Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`.

| Test | Proves |
| --- | --- |
| `theResponseNamesTheSeasonTheServersClockIsIn` | the body's `season` is `"2026-08"`, and the fake was called with `Season(2026, 8)` — a browser clock decides nothing here |
| `aCursorlessRequestPinsTheWalkToTheInstantTheClockReports` | the fake's recorded `asOf` is exactly `2026-08-20T09:00:00Z`, its recorded `after` is `null`, and its recorded `limit` is the requested limit **plus one** |
| `aCursoredRequestReusesTheCursorsCutoffAndNotTheClock` | a request carrying a cursor whose `asOf` is `2026-08-15T12:00:00Z` — earlier than the clock, inside the same season — makes the fake see `asOf = 2026-08-15T12:00:00Z`; the clock's instant appears nowhere in the call |

**Named mutations.** Passing `season.endExclusive` as the upper bound reddens the second test.
Recomputing `clock.instant()` for a cursored request reddens the third — and that mutation is
exactly *a live walk*, which is what this pair of tests exists to forbid. Passing `limit` instead of
`limit + 1` reddens the second.

## Acceptance criteria

- [ ] `StandingsRouteTest.theResponseNamesTheSeasonTheServersClockIsIn` passes
- [ ] `StandingsRouteTest.aCursorlessRequestPinsTheWalkToTheInstantTheClockReports` passes,
      asserting the recorded `asOf`, `after` and `limit + 1`
- [ ] `StandingsRouteTest.aCursoredRequestReusesTheCursorsCutoffAndNotTheClock` passes
- [ ] `standingsRoutes` takes `java.time.Clock` with no default, `StandingsRoutes.kt` does not name
      `ServerClock`, and it reads exactly two query parameters — `limit` and `after`
- [ ] The route answers no `401` on any path, and `ProfileRoutes.kt` is unchanged
- [ ] `./gradlew :poker-server:detekt` passes — no `LongMethod` on the handler
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
