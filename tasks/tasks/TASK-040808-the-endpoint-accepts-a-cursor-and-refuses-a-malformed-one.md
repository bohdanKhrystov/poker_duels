---
schema: 2
id: TASK-040808
title: The endpoint accepts a cursor, and a malformed one is a 400 that reads nothing
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, http, history, paging]
depends_on: [TASK-040807]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`GET /api/me/duels?after=<cursor>` reads the page after that cursor, and a cursor the server would
not have issued answers `400` before anything is read.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileRoutes.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/DuelCursor.kt` | read — `duelCursorOrNull` |

## Scope

- The query parameter is `after`. Absent means the newest page and the port is given `null`;
  present means `duelCursorOrNull(raw)`, and `null` from that answers `400 Bad Request` with an
  empty body, **before** `reads.recentDuelsOf` is called.
- `?after=` with an empty value is *present and unparseable*, so it is `400`. Do not write
  `takeIf { it.isNotBlank() }` here: it would turn a broken cursor into a silent first page, which
  is the one behaviour `STORY-0408` names and refuses.
- The order of refusals stays identity first: `401` for an absent, blank or unknown device before
  the query string is looked at, then the limit's `400`, then the cursor's `400`. A stranger must
  not learn from a `400` that their device id was fine.
- **Extract the handler body, or `check` goes red.** `profileRoutes` spans 55 lines today; detekt
  runs on its default config with `maxIssues: 0`, and `LongMethod`'s threshold is 60 counted over
  non-blank, non-comment lines. The margin is a handful of lines and the cursor code is more than
  that, so measure nothing and extract. Move the whole
  `get("/api/me/duels")` body into a private extension in the same file, e.g.

  ```kotlin
  private suspend fun ApplicationCall.respondWithDuels(reads: ProfileReads) { … }
  ```

  called as `call.respondWithDuels(reads)`, with plain `return` in place of `return@get`. The file
  already declares an `ApplicationCall` extension (`deviceIdOrNull`), so nothing new is imported
  from Ktor. Keep the KDoc on `profileRoutes` describing the endpoint; add nothing to the other two
  routes.

## Out of scope

- The `+ 1` probe, `nextCursor`, and trimming the page — `TASK-040809`, which edits the same two
  files immediately after.
- Changing what `limit` means, its default or its cap.
- Any new status code. The endpoint answers `200`, `400` and `401`, exactly as it does today.

## Tests

`ProfileRouteTest`. Build the fixture cursor with `DuelCursor(...).encoded()` and put it in the
query string — never a hand-written base64 literal, which would rot the first time the encoding
changes.

| Test | Proves |
| --- | --- |
| `aCursorFromTheQueryReachesThePort` | `?after=<encoded>` answers `200` and `cursorsRequested` is exactly `listOf(thatCursor)` — compared as a `DuelCursor` value, so a route that forwarded the raw string or a re-parsed near-miss fails |
| `aMalformedCursorIsABadRequestAndReadsNothing` | `?after=not-a-cursor` answers `400`, the body is empty, and `cursorsRequested.isEmpty()` — the port was never called |
| `anEmptyCursorValueIsABadRequest` | `?after=` answers `400` and `cursorsRequested.isEmpty()` — present-but-empty is refused, not treated as absent |
| `anUnknownDeviceIsRefusedBeforeTheCursorIsParsed` | an unknown device id with `?after=not-a-cursor` answers `401`, not `400` — the same ordering the limit already has, asserted with an input that would otherwise produce the other status |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `ProfileRouteTest.aRequestWithNoCursorAsksForNone` still passes, unchanged
- [ ] `anAbsentLimitAsksForTheDefault`, `aLimitAboveTheCapIsClamped` and
      `aNonNumericLimitIsABadRequest` still pass with their assertions unchanged
- [ ] The duels handler's body lives in a private function, and `./gradlew :poker-server:detekt`
      reports no issue
- [ ] No test in the diff hard-codes an encoded cursor string
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
