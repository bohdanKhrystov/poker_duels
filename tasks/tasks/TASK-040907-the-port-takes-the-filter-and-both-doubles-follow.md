---
schema: 2
id: TASK-040907
title: The port's duel read takes the filter, and both doubles follow
type: task
status: done
parent: STORY-0409
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, ports, history, filters]
depends_on: [TASK-040906]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ProfileReads` — the port the routes hold — can be asked for a filtered page, so `TASK-040909` has
something to call.

## Files

**Four, and the linter caps the field at three.** Adding a parameter to an interface method breaks
every implementer at compile time; there are exactly two, both test doubles, and each takes three
lines. The frontmatter says `3`; the fourth is `AuthRouteDoubles.kt`, named here so the count is
honest rather than hidden. `TASK-040807` paid the same price for the cursor and recorded it the same
way.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify — delete one delegate |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify — `FakeProfileReads`, plus one new test |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | modify — `FixedProfileReads`, three lines |

## Scope

- The port declares one duel read and it takes the filter:

  ```kotlin
  public suspend fun recentDuelsOf(
      playerId: PlayerId,
      limit: Int,
      after: DuelCursor? = null,
      filter: DuelFilter = DuelFilter.NONE,
  ): List<DuelSummaryResponse>
  ```

  Both default values live **on the interface declaration only** — an override may not repeat one,
  and Kotlin gives every override its base declaration's defaults. That is what keeps the two- and
  three-argument call sites in `PostgresProfileReadsTest.kt` and `AuthRouteDoublesTest.kt` compiling
  untouched, through a receiver typed as the concrete class.
- `PostgresProfileReads`: delete the three-argument `override` that `TASK-040903` left delegating,
  and put `override` on the four-argument function it wrote. **No SQL changes in this ticket.**
- `FakeProfileReads` (in `ProfileRouteTest.kt`) gains the parameter and records it as
  `val filtersRequested: MutableList<DuelFilter> = mutableListOf()`. A list, for the same reason
  `cursorsRequested` is one: the tests need to tell *never asked* (`isEmpty()`) from *asked with no
  filter* (`listOf(DuelFilter.NONE)`), and a single field collapses those two into one value.
- `FixedProfileReads` (in `AuthRouteDoubles.kt`) gains the parameter and keeps returning
  `emptyList()`. Its KDoc's "an empty list for any player's duels" stays true.
- Update the port's KDoc: `filter` narrows which of the player's duels are returned, `DuelFilter.NONE`
  narrows nothing, and the caller has already parsed and refused what it would not accept.

## Out of scope

- The route reading the two query parameters — `TASK-040909`. The route body in this ticket keeps
  calling `recentDuelsOf(playerId, limit + 1, cursor)` and gets the default.
- Any change to `ServerComponents.kt` or `Application.kt`. The port's implementer is unchanged, so
  the composition root is untouched — that is why the parameter goes on the existing method rather
  than into a second port.
- Deleting or renaming anything in `PostgresProfileReadsTest.kt` or `AuthRouteDoublesTest.kt`.
- **Making `FixedProfileReads` observe the filter.** Recorded because it is a real limitation, not an
  oversight, and because `TASK-040807` recorded exactly the same one for the cursor:
  `FixedProfileReads` accepts `filter` and returns `emptyList()` regardless, so **any future test
  that passes a filter through this double proves nothing about filtering.** It would first need a
  `filtersRequested` list of its own, or a fixture whose page varies with the filter. No test in
  `STORY-0409` does so; the sign-up tests that use this double never filter.

## Tests

`ProfileRouteTest`

| Test | Proves |
| --- | --- |
| `aRequestWithNoFilterAsksForNone` | `GET /api/me/duels` with no query string answers `200` and leaves `filtersRequested == listOf(DuelFilter.NONE)` — the port was asked exactly once, and the route invented no filter of its own (`ADR-0002`). It fails against a route that passes a fabricated filter and against one that asks twice; `isEmpty()` would pass for a route that never read at all, which is why the assertion is the singleton list |

One new test, and it is deliberately narrow: everything about *reading* the parameters arrives in
`TASK-040909`, and this ticket's real gate is that the whole suite still compiles and passes with
both parameters defaulted.

## Acceptance criteria

- [ ] `ProfileRouteTest.aRequestWithNoFilterAsksForNone` passes and asserts
      `listOf(DuelFilter.NONE)`, not `isEmpty()`
- [ ] `ProfileReads` declares exactly one duel-reading method, with `after: DuelCursor? = null` and
      `filter: DuelFilter = DuelFilter.NONE`
- [ ] No `override` in the diff repeats either default value
- [ ] Not one call site in `PostgresProfileReadsTest.kt` or `AuthRouteDoublesTest.kt` is edited
- [ ] Every test already in `ProfileRouteTest`, `AuthRouteDoublesTest`, `AuthRouteTest`,
      `SignUpSecrecyTest`, `PostgresProfileReadsTest` and `DuelHistoryPagingDatabaseTest` passes with
      its assertions unchanged
- [ ] `ServerComponents.kt` and `Application.kt` are not in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
