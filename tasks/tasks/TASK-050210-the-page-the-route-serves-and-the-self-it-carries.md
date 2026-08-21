---
schema: 2
id: TASK-050210
title: The page the route serves — the probe row, the last page, the empty ladder, and the self object
type: task
status: ready
parent: STORY-0502
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, http, leaderboard, route, tests]
depends_on: [TASK-050209]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsRouteTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The behaviour `TASK-050209` implemented around the page — one row more than the page is asked for
and never served, `nextCursor` names the row that **was** served, an empty ladder is `200`, and the
self object takes one of its three shapes — is pinned by tests rather than by reading the handler.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsCursor.kt` | read |

## Scope

- Four tests added to the existing class, reusing its fakes and its
  `Clock.fixed(Instant.parse("2026-08-20T09:00:00Z"), ZoneOffset.UTC)`.
- The cursor the response hands out is checked by **decoding** it with `standingsCursorOrNull` under
  the same season, so the assertion is about the row it names rather than about a string shape.
- No production file changes.

## Out of scope

- **The refusals** — `TASK-050211`.
- **Whether the numbers are right.** The fakes decide what the aggregate says; that it is a real
  whole-ladder aggregate is `TASK-050208`'s and `TASK-050216`'s.
- Changing any assertion already in `StandingsRouteTest`.

## Tests

`StandingsRouteTest`. The fake `StandingsReads` returns the rows the test hands it, so a test asking
for `limit = 3` hands it four rows to make the probe observable.

| Test | Proves |
| --- | --- |
| `theProbeRowIsNeverServedAndTheCursorNamesTheRowThatWas` | with `limit = 3` and four rows available, the body carries **three** rows, the fourth id appears nowhere in it, and `nextCursor` decodes to `StandingsCursor(asOf, coins of row 3, id of row 3)` — decoded with `standingsCursorOrNull`, and asserted against **row three**, not row four |
| `theLastPageSaysThereIsNoNextPage` | with `limit = 3` and exactly three rows available, `nextCursor` is `null` and all three rows are served |
| `anEmptyLadderIsTwoHundredWithAnEmptyPage` | no rows at all answers `200` with `rows` empty and `nextCursor` null — not `404`, the same shape `GET /api/me/duels` chose |
| `theSelfObjectTakesOneOfItsThreeShapes` | three requests against the same fake rows: no `X-Device-Id` header at all → `self` is `null` **and** the `ProfileReads` fake was never asked; a known device whose `standingOf` answers a standing → `self` carries that rank and those coins; a known device whose `standingOf` answers `null` → `self` is present with `rank` and `coins` both `null` and is **not** a zero. The `rows` of all three responses are identical, id for id |

**Named mutations.** Minting `nextCursor` from the probe row reddens the first test — and that
mutation is the one that would silently drop a player from every walk, forever. Serving `limit + 1`
rows reddens it too. Returning `SelfStandingResponse(id, 0, 0)` for a player with no row reddens the
fourth. Answering `404` on an empty ladder reddens the third.

## Acceptance criteria

- [ ] `StandingsRouteTest.theProbeRowIsNeverServedAndTheCursorNamesTheRowThatWas` passes, decoding
      the issued cursor and asserting it names row **three**
- [ ] `StandingsRouteTest.theLastPageSaysThereIsNoNextPage` passes
- [ ] `StandingsRouteTest.anEmptyLadderIsTwoHundredWithAnEmptyPage` passes
- [ ] `StandingsRouteTest.theSelfObjectTakesOneOfItsThreeShapes` passes on all three requests and
      asserts the three pages are identical
- [ ] Every test already in `StandingsRouteTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
