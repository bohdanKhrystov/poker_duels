---
schema: 2
id: TASK-050217
title: Three answers about the reader, one page for everybody, and a read that creates nothing
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, http, db, leaderboard, self-standing, tests]
depends_on: [TASK-050216]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsSelfDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0065` §4's three answers are three answers over a real database — a rank and a standing, **no
place this season**, and nothing at all — the page is the same for all three readers, and reading
the ladder still mints no row.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsSelfDatabaseTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsRoutes.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | read |

## Scope

- Three tests added to the class `TASK-050216` created, with a second fixture builder for the first
  of them; `TASK-050216`'s fixture and its three tests keep every assertion.
- The `player` row count is read with one direct `SELECT count(*) FROM player` in the test — the
  same way `ServerComponentsTest` counts rows — because the assertion is about the table, not about
  a port.
- No production file changes.

## Out of scope

- **A `401` on any of these requests.** An unknown device reads the ladder like anybody else
  (`ADR-0065` §4); the `401` vocabulary belongs to `GET /api/me`.
- **Creating a profile from this endpoint.** `ProfileReads`' rule holds: an unknown device is `null`
  and nothing is minted (`ADR-0012` — profiles are created on the socket handshake only).
- **`GET /api/me` gaining anything.** `ADR-0065` §2: `ProfileResponse` is untouched by this story
  and `PostgresProfileReadsTest` does not move.

## Tests

`StandingsSelfDatabaseTest`, `-PrequireDocker=true`, `limit = 2`.

**Fixture B — the two answers an implementation collapses.** `drew` and `rival` draw a duel in
**August** (both stand at `0`, `ADR-0015`); `absent` holds a profile whose only duel finished in
**July**, against `july`. The August ladder holds `drew` and `rival` and nobody else.

| Test | Proves |
| --- | --- |
| `aProfileWithNoDuelThisSeasonIsToldItHasNoPlaceAndIsNotGivenAZero` | asked with `absent`'s device, `self` is **present** with `rank` and `coins` both `null`; asked with `drew`'s device, `self` reads a rank and `coins = 0`. The two are asserted in one test because collapsing them is the mistake |
| `anUnknownDeviceGetsThePageAndNoSelfLine` | asked with `X-Device-Id: never-seen-before` and asked with **no header at all**, both answer `200` with `self = null`, and the `rows` of both are identical — id, rank and coins, position by position — to the page a known device is served from the same fixture |
| `readingTheLadderCreatesNothing` | the `player` row count is the same before and after a request from an unknown device, asserted as a number rather than as *at most* |

**Named mutations.** Returning `SelfStandingResponse(id, 0, 0)` for a player with no row reddens the
first test — and it is the shape that would tell somebody who played nothing that they stand at zero
alongside a player who drew. Narrowing the page by who is asking, in any way, reddens the second.
Resolving the device through a *create-if-absent* directory instead of `ProfileReads` reddens the
third.

## Acceptance criteria

- [ ] `StandingsSelfDatabaseTest.aProfileWithNoDuelThisSeasonIsToldItHasNoPlaceAndIsNotGivenAZero`
      passes, asserting both readers in one test
- [ ] `StandingsSelfDatabaseTest.anUnknownDeviceGetsThePageAndNoSelfLine` passes for both the unknown
      device and the absent header, and compares the rows position by position against a known
      device's page
- [ ] `StandingsSelfDatabaseTest.readingTheLadderCreatesNothing` passes, asserting an exact count
- [ ] No request in this file answers `401`
- [ ] `TASK-050216`'s three tests pass with their assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
