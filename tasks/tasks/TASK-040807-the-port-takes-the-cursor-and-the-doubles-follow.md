---
schema: 2
id: TASK-040807
title: The port's duel read takes the cursor, and both doubles follow
type: task
status: ready
parent: STORY-0408
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 3
labels: [server, http, ports, history, paging]
depends_on: [TASK-040806]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileRouteTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ProfileReads` — the port the routes hold — can be asked for the page after a cursor, so
`TASK-040808` has something to call.

## Files

**Four, and the linter caps the field at three.** Adding a parameter to an interface method breaks
every implementer at compile time; there are exactly two, both test doubles, and each takes three
lines. The frontmatter says `3`; the fourth is `AuthRouteDoubles.kt`, named here so the count is
honest rather than hidden.

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/ProfileReads.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify — `FakeProfileReads`, plus one new test |
| `poker-server/src/test/kotlin/duels/poker/server/http/AuthRouteDoubles.kt` | modify — `FixedProfileReads`, three lines |

## Scope

- The port declares one duel read and it takes a cursor:

  ```kotlin
  public suspend fun recentDuelsOf(playerId: PlayerId, limit: Int, after: DuelCursor? = null): List<DuelSummaryResponse>
  ```

  The default value lives **on the interface declaration only** — an override may not repeat one,
  and Kotlin gives every override its base declaration's defaults. That is what keeps the ~20
  two-argument call sites in `PostgresProfileReadsTest.kt` and the one in `AuthRouteDoublesTest.kt`
  compiling untouched, through a receiver typed as the concrete class.
- `PostgresProfileReads`: delete the two-argument `override` that `TASK-040802` left delegating, and
  put `override` on the three-argument function it wrote. No SQL changes in this ticket.
- `FakeProfileReads` (in `ProfileRouteTest.kt`) gains the parameter and records it as
  `val cursorsRequested: MutableList<DuelCursor?> = mutableListOf()`. **A list, not a
  `var lastCursor`** — the existing tests need to tell *never asked* (`isEmpty()`) from *asked with
  no cursor* (`listOf(null)`), and a nullable field collapses those two into one value.
- `FixedProfileReads` (in `AuthRouteDoubles.kt`) gains the parameter and keeps returning
  `emptyList()`. Its KDoc's "an empty list for any player's duels" stays true.
- Update the port's KDoc: `after` is a position in the list, `null` means the newest page, and the
  caller has already clamped `limit` (`duelLimitOrNull`), exactly as the existing text says.

## Out of scope

- The route reading the query parameter or computing a next cursor — `TASK-040808`,
  `TASK-040809`. The route body in this ticket keeps calling `recentDuelsOf(playerId, limit)` and
  gets the default.
- Any change to `ServerComponents.kt` or `Application.kt`. The port's implementer is unchanged, so
  the composition root is untouched — that is why the parameter goes on the existing method rather
  than into a second port.
- Deleting or renaming anything in `PostgresProfileReadsTest.kt` or `AuthRouteDoublesTest.kt`.

## Tests

`ProfileRouteTest`

| Test | Proves |
| --- | --- |
| `aRequestWithNoCursorAsksForNone` | `GET /api/me/duels` with no query string answers `200` and leaves `cursorsRequested == listOf(null)` — the port was asked exactly once, and the route invented no position of its own (`ADR-0002`) |

One new test, and it is deliberately narrow: everything about *reading* a cursor arrives in
`TASK-040808`, and this ticket's real gate is that the whole suite still compiles and passes with
the parameter defaulted. The assertion is not vacuous — it fails against a route that passes a
fabricated cursor, and it is the assertion `TASK-040808` extends rather than replaces.

## Acceptance criteria

- [ ] `ProfileRouteTest.aRequestWithNoCursorAsksForNone` passes and asserts `listOf(null)`, not
      `isEmpty()`
- [ ] `ProfileReads` declares exactly one duel-reading method, with `after: DuelCursor? = null`
- [ ] No `override` in the diff repeats the default value
- [ ] Not one call site in `PostgresProfileReadsTest.kt` or `AuthRouteDoublesTest.kt` is edited
- [ ] Every test already in `ProfileRouteTest`, `AuthRouteDoublesTest`, `AuthRouteTest`,
      `SignUpSecrecyTest` and `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] `ServerComponents.kt` and `Application.kt` are not in the diff
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
