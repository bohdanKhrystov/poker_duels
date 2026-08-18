---
schema: 2
id: TASK-040909
title: The endpoint reads the filter, and refuses what the parsers refuse
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, history, filters]
depends_on: [TASK-040908]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`GET /api/me/duels?outcome=&opponent=` narrows the page, and a parameter the server will not act on
is `400 Bad Request` before anything is read.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify — `respondWithDuels`, plus the KDoc paragraph |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify — five tests |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelFilter.kt` | read — `duelFilterOrNull` |

## Scope

- In `respondWithDuels`, after the cursor block and before the read:

  ```kotlin
  val filter = duelFilterOrNull(
      request.queryParameters["outcome"],
      request.queryParameters["opponent"],
  )
  if (filter == null) {
      respond(HttpStatusCode.BadRequest)
      return
  }
  ```

  and the read becomes `reads.recentDuelsOf(PlayerId(profile.playerId), limit + 1, cursor, filter)`.
  The `limit + 1` probe and `recentDuelsPage(limit)` are untouched: the probe row is the
  `(limit + 1)`th row **of the filtered set**, which is exactly what it has to be for `nextCursor`
  to mean "there is another page of this filter".
- **The order of refusals is identity, limit, cursor, filter**, and the first of those is the only
  one that is a security property: an unauthenticated request is refused before any query parameter
  is looked at, so a bad `outcome` never tells a stranger that their device id was the problem. The
  existing comment above the identity check already says this; extend it to name the filter rather
  than writing a second comment.
- `profileRoutes`'s KDoc gains the two parameters in its `GET /api/me/duels` paragraph: `outcome`
  takes `WON`, `LOST` or `DREW` and anything else including a lower-case spelling is `400`;
  `opponent` is a case-insensitive substring of the opponent's display name, refused when blank or
  over 32 code points; an empty value of either is present-and-unusable, not absent, and is `400`
  before `reads.recentDuelsOf` is ever called.
- **`respondWithDuels` is 29 lines today** (`ProfileRoutes.kt` lines 114–142) and detekt's
  `LongMethod` budget is 60. The block above plus its comment is about eight lines, which leaves the
  margin intact — so nothing needs extracting here, and `recentDuelsPage` and `deviceIdOrNull` stay
  exactly as they are. Do not add a second private helper for four lines.

## Out of scope

- **A cursor issued under one filter and replayed under another.** `STORY-0409` requires that such a
  cursor be *refused rather than silently reinterpreted*, and the mechanism is **`DEC-050`**,
  unanswered: today's cursor is `(finishedAt, duelId)` and carries nothing that could name the
  filter it was drawn under, and `STORY-0408`'s split recorded that changing what a cursor is *"is
  an ADR, not a ticket"*. Until that lands, `after` and a filter sent together are accepted and the
  cursor is read as a position in the same `(finishedAt, duelId)` order — which is well defined and
  never loses a row *within* a consistent filter, and is a weaker contract than the story wants.
  **Write no test that asserts anything about that combination**, in either direction: whichever way
  it was asserted, the ticket answering `DEC-050` would have to undo it. `TASK-040911` documents the
  gap.
- Any change to `duelLimitOrNull`, `duelCursorOrNull`, `recentDuelsPage` or `DEFAULT_DUEL_LIMIT`.
- The database — no test here touches PostgreSQL. `TASK-040910` proves the parameters reach the SQL.
- The screen — `STORY-0413`.

## Tests

`ProfileRouteTest`, using the existing `FakeProfileReads` with its `filtersRequested` list.

| Test | Proves |
| --- | --- |
| `anOutcomeParameterReachesThePort` | `GET /api/me/duels?outcome=WON` answers `200` and leaves `filtersRequested == listOf(DuelFilter(DuelOutcomeLabel.WON, null))` — the parsed label, not the raw string, and the opponent axis untouched |
| `anOpponentParameterReachesThePort` | `?opponent=Halvard` answers `200` and leaves `filtersRequested == listOf(DuelFilter(null, "Halvard"))`. Together with the row above, the pair is what shows the route is not writing one axis into both |
| `anUnusableOutcomeIsABadRequestAndReadsNothing` | `?outcome=won` answers `400` **and** `filtersRequested.isEmpty()` — refused before the port is called, so a client cannot spend a query on a typo |
| `anEmptyOpponentValueIsABadRequest` | `?opponent=` answers `400` and reads nothing. Present and empty is not absent — the same rule `anEmptyCursorValueIsABadRequest` already pins for `after` |
| `anUnknownDeviceIsRefusedBeforeTheFilterIsParsed` | a request with **no** `X-Device-Id` and `?outcome=won` answers `401`, not `400`, and `filtersRequested.isEmpty()`. The status is the assertion: a route that parsed the filter first would answer `400` and tell a stranger their device id was fine |

## Acceptance criteria

- [ ] `ProfileRouteTest.anOutcomeParameterReachesThePort` passes and asserts the exact
      `listOf(DuelFilter(...))`
- [ ] `ProfileRouteTest.anOpponentParameterReachesThePort` passes and asserts the exact
      `listOf(DuelFilter(...))`
- [ ] `ProfileRouteTest.anUnusableOutcomeIsABadRequestAndReadsNothing` passes and asserts
      `filtersRequested.isEmpty()`
- [ ] `ProfileRouteTest.anEmptyOpponentValueIsABadRequest` passes and asserts nothing was read
- [ ] `ProfileRouteTest.anUnknownDeviceIsRefusedBeforeTheFilterIsParsed` passes and asserts `401`
- [ ] No test in the diff sends `after` together with `outcome` or `opponent`
- [ ] Every test already in `ProfileRouteTest` passes with its assertions unchanged, in particular
      `aRequestWithNoCursorAsksForNone`, `aRequestWithNoFilterAsksForNone`,
      `aFullPageReportsTheCursorOfItsLastRow` and `aShortPageReportsNoNextPage`
- [ ] `respondWithDuels` is under 60 lines and no new private function is added to `ProfileRoutes.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
