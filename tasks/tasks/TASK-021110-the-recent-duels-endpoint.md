---
schema: 2
id: TASK-021110
title: Answer GET /api/me/duels with a bounded, ordered list
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, http, duel]
depends_on: [TASK-021109]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`GET /api/me/duels?limit=N` returns the calling device's recent duels as JSON, with the limit
defaulted, capped, or refused as `duelLimitOrNull` decides.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt`,
`poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt`,
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt`.

## Scope

- Add a second route inside the existing `routing { }` block, reusing `deviceIdOrNull()`:

  ```kotlin
  get("/api/me/duels") {
      // Identity first: an unauthenticated request is refused before its query string is
      // parsed, so a bad limit never tells a stranger that their device id was the problem.
      val profile = call.deviceIdOrNull()?.let { reads.profileOf(it) }
      if (profile == null) {
          call.respond(HttpStatusCode.Unauthorized)
          return@get
      }
      val limit = duelLimitOrNull(call.request.queryParameters["limit"])
      if (limit == null) {
          call.respond(HttpStatusCode.BadRequest)
          return@get
      }
      call.respond(RecentDuelsResponse(reads.recentDuelsOf(PlayerId(profile.playerId), limit)))
  }
  ```

- The route **does no arithmetic and no sorting**: order comes from the query
  (`TASK-021106`/`TASK-021107`), the outcome and the delta come from stored rows, and the limit
  rule lives in `duelLimitOrNull`. If a `sortedBy`, a `take`, a `coerce` or a `10` appears in this
  file, the rule now exists in two places.
- A player with no duels gets `200` and `{"duels":[]}`. Not `404`: the profile exists and the
  honest answer to "how did my last few duels go?" is "there were none".
- Extend the `profileRoutes` KDoc with the second endpoint, its `limit` parameter, its default and
  its cap.

## Out of scope

- Any change to the five tests `TASK-021109` merged: this ticket adds a route and adds tests, and
  touches neither the `/api/me` handler nor its assertions.
- Cursors, paging, filters, or a `total` count — `EPIC-04` owns the history screen.
- The real database — `TASK-021111`.

## Tests

`ProfileRouteTest`, the existing class, six tests added. The fake `ProfileReads` gains a stored
`List<DuelSummaryResponse>` per player id and records the `limit` it was last called with, so the
clamping tests assert on the number the route passed down rather than on the size of a canned list.

| Test | Proves |
| --- | --- |
| `aKnownDeviceGetsItsDuelsInTheOrderTheReaderReturnedThem` | `200`, and the body's `duels` array holds the fake's two summaries in the fake's order — the route re-orders nothing |
| `anAbsentLimitAsksForTheDefault` | with no `limit`, the fake was called with `DEFAULT_DUEL_LIMIT` |
| `aLimitAboveTheCapIsClamped` | `?limit=999` answers `200` and the fake was called with `MAX_DUEL_LIMIT` |
| `aNonNumericLimitIsABadRequest` | `?limit=abc` answers `400`, and the fake's `recentDuelsOf` was never called |
| `aPlayerWithNoDuelsGetsAnEmptyListAndTwoHundred` | a known device with no duels answers `200` and a body of `{"duels":[]}` |
| `anUnknownDeviceIsRefusedBeforeTheLimitIsParsed` | `X-Device-Id: ghost` with `?limit=abc` answers `401`, not `400` |

## Acceptance criteria

- [ ] `ProfileRouteTest.aKnownDeviceGetsItsDuelsInTheOrderTheReaderReturnedThem` passes
- [ ] `ProfileRouteTest.anAbsentLimitAsksForTheDefault` passes
- [ ] `ProfileRouteTest.aLimitAboveTheCapIsClamped` passes
- [ ] `ProfileRouteTest.aNonNumericLimitIsABadRequest` passes
- [ ] `ProfileRouteTest.aPlayerWithNoDuelsGetsAnEmptyListAndTwoHundred` passes
- [ ] `ProfileRouteTest.anUnknownDeviceIsRefusedBeforeTheLimitIsParsed` passes
- [ ] The five tests `TASK-021109` merged pass with their assertions unchanged
- [ ] `ProfileRoutes.kt` contains no `sortedBy`, `sortedByDescending`, `take`, `coerceAtMost` or
      numeric limit literal — the limit comes from `duelLimitOrNull` alone
- [ ] `ProfileRoutes.kt` still names no `DataSource`, `Connection` or SQL string
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
