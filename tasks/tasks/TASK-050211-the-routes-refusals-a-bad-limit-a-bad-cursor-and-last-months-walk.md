---
schema: 2
id: TASK-050211
title: The route's refusals — a bad limit, a cursor that does not decode, and a walk from last month
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, http, leaderboard, route, tests]
depends_on: [TASK-050210]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsRouteTest'
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

Every way this endpoint says no is one `400 Bad Request` with an empty body and nothing read — and
the refusal a client will actually meet, a cursor held across a month boundary, is asserted with a
cursor that decodes **perfectly**, so it is about the season rather than about the encoding.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/RecentDuelsLimit.kt` | read |

## Scope

- Four tests added to the existing class, reusing its fakes.
- *Nothing read* is asserted, not assumed: the fake `StandingsReads` counts its calls and the count
  is `0` on every refusal.
- The month-boundary test installs the route **twice**, under two `Clock.fixed` instants either side
  of `2026-09-01T00:00:00Z`, and hands the *same* cursor string to both.
- No production file changes.

## Out of scope

- **A `401`.** This route has none: the ladder is readable with no profile and the page is identical
  for every reader (`ADR-0065` §4). A test asserting `401` anywhere here is asserting the opposite
  of the decision.
- **A distinguishable refusal.** `ADR-0066` §7 and `ADR-0057` §5: an out-of-season cursor and a
  cursor that does not decode answer the same status with the same empty body, and the remedy for
  both is to drop the cursor and ask for the first page. A test that tells them apart by the
  response is pinning something the product refuses to say.
- **Serving last month's ladder to a stale cursor.** `ADR-0061` §7 forbids any season but the
  current one; `DEC-060` owns whether a finished season is ever reachable and is not pre-empted.
- Changing any assertion already in `StandingsRouteTest`.

## Tests

`StandingsRouteTest`.

| Test | Proves |
| --- | --- |
| `aLimitTheServerWillNotParseIsFourHundredAndReadsNothing` | `limit=0`, `limit=-1` and `limit=abc` each answer `400` with an empty body, and the standings fake was called zero times |
| `aLimitAboveTheCapIsClampedRatherThanRefused` | `limit=999` answers `200` and the fake's recorded limit is `51` — the cap of `50` plus the probe row |
| `aCursorThatDoesNotDecodeIsFourHundredAndReadsNothing` | `after=not-a-cursor` and `after=` (present and empty, which is not absent) each answer `400` with an empty body and zero calls |
| `aCursorFromAnotherSeasonIsTheSameFourHundred` | a cursor carrying `asOf = 2026-08-20T09:00:00Z`, which **decodes perfectly**, answers `200` under a clock fixed at `2026-08-20T09:00:00Z` and `400` with an empty body and zero calls under a clock fixed at `2026-09-01T00:00:01Z`. The reverse pairing is asserted in the same test: a cursor carrying a September instant is `400` under the August clock |

**Named mutations.** Deleting the season check in `standingsCursorOrNull` reddens the fourth test's
September half while leaving the third green — which is why the fourth uses a cursor that decodes.
Treating a refused cursor as an absent one, and serving the first page instead, reddens the third
and the fourth on the status and on the call count. Rejecting an over-cap limit instead of clamping
it reddens the second.

## Acceptance criteria

- [ ] `StandingsRouteTest.aLimitTheServerWillNotParseIsFourHundredAndReadsNothing` passes on all
      three inputs, asserting the empty body and zero calls
- [ ] `StandingsRouteTest.aLimitAboveTheCapIsClampedRatherThanRefused` passes, asserting `51`
- [ ] `StandingsRouteTest.aCursorThatDoesNotDecodeIsFourHundredAndReadsNothing` passes on both
      inputs
- [ ] `StandingsRouteTest.aCursorFromAnotherSeasonIsTheSameFourHundred` passes, asserting `200`
      under the in-season clock and `400` under the clock on the other side of the boundary, both
      directions
- [ ] No test in this file asserts `401`
- [ ] Every test already in `StandingsRouteTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
