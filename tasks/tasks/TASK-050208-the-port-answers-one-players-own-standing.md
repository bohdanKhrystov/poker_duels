---
schema: 2
id: TASK-050208
title: The port answers one player's own whole-season standing, computed against the whole ladder
type: task
status: backlog
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, db, leaderboard, self-standing]
depends_on: [TASK-050207]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresSelfStandingTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresStandingsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - "! grep -qiE 'having|dense_rank|coin_balance' poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt"
---

## Goal

The port can answer *where does **this** player stand this season* as a rank and a standing over the
whole ladder — for a player who is on no page anybody drew — and answers **nothing** for a player
who finished no duel in the season.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/http/StandingsReads.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresSelfStandingTest.kt` | create |

## Scope

- The port gains exactly one method:

  ```kotlin
  public suspend fun standingOf(playerId: PlayerId, season: Season, asOf: Instant): SelfStandingResponse?
  ```

  `null` means **this player has no row on this season's ladder** — they finished no duel in the
  window. It never means zero: `0` is a real standing a draw earns (`ADR-0015`, `ADR-0065` §4), and
  the two answers are the pair an obvious implementation collapses.
- The implementation is a **second statement**, under the **same** `[season.start, asOf)` window,
  on the same connection and with **no explicit transaction** — `ADR-0066` §6: the two answers agree
  because the window is closed, not because they share a snapshot.
- The SQL is `TASK-050204`'s two CTEs with a different tail, so the `standing`/`ranked` body is
  shared as a constant rather than copied:

  ```sql
  SELECT r.rank, r.coins FROM ranked r WHERE r.player_id = ?::uuid
  ```

  No row means `null`. The rank is `rank()` over the **whole** `standing` CTE, so a player's own
  rank counts everybody standing above them and not the rows of any page (`ADR-0066` §5).
- KDoc names the three answers and says which of them this method does **not** produce: the
  *unknown device* answer is a fact about the request and is decided before any SQL runs
  (`ADR-0066` §6), so it belongs to the route.

## Out of scope

- **A `playerId` request parameter, or a standing for anybody but the requester.** The route
  resolves the id from the `X-Device-Id` header; an endpoint answering *what is player X's standing*
  for any X asked is `DEC-057`'s question and this story does not pre-empt it (`ADR-0065` §3).
- **The three-state mapping and the wire.** `TASK-050209` turns `null` into *no place this season*
  and an unknown device into an absent object.
- **Folding the two statements into one.** `ADR-0066` §6 permits it later precisely because it
  changes no observable contract; it is not this ticket's, and one statement answering both is
  harder to read than two.
- **An index on `duel_result (player_id)`** — `ADR-0066` §8 says it would buy this statement nothing,
  because even one player's rank is a whole-ladder aggregate.

## Tests

`PostgresSelfStandingTest`, in `duels.poker.server.db`, `-PrequireDocker=true`. Same setup shape as
`PostgresStandingsReadsTest`. Season `Season(2026, 8)`, `asOf = season.endExclusive`.

**The fixture is one ladder of six**, built so that reading it in pages of two puts different
players on different pages. Reuse `TASK-050205`'s fixture A duel list: standings `a +3`, `b +2`,
`c +1`, `d +1`, `f -3`, `e -4`; ranks `1, 2, 3, 3, 5, 6`.

| Test | Proves |
| --- | --- |
| `theStandingIsTheWholeLaddersRankForAPlayerOnThePageAndForOneFarBelowIt` | with `standingsPage(limit = 2)` drawing only `a` and `b`, `standingOf(a)` is rank `1` / coins `3` **and** `standingOf(e)` is rank `6` / coins `-4` — `e` is on no page this test drew, and the number is right anyway. Two inputs, because a fixture whose player is on the page drawn cannot tell a whole-ladder aggregate from an echo of the rows |
| `aTiedPlayersOwnStandingIsTheSharedRank` | `standingOf(c)` and `standingOf(d)` both read rank `3` — not `3` and `4` (`ADR-0064` §1) |
| `aPlayerWhoFinishedNoDuelThisSeasonHasNoStanding` | a player whose only duel finished in **July**, and a player who finished none at all, both answer `null` — and the July player's `player.coin_balance` is non-zero, so `null` is about the window rather than about having never played |

**Named mutations.** Computing the rank over the page rather than over the CTE reddens the first
test on `e`. `dense_rank()` reddens the second. Returning `SelfStandingResponse(id, 0, 0)` instead
of `null` — the collapse `ADR-0065` §4 names — reddens the third.

## Acceptance criteria

- [ ] `PostgresSelfStandingTest.theStandingIsTheWholeLaddersRankForAPlayerOnThePageAndForOneFarBelowIt`
      passes, asserting both players in one test, with the page drawn holding only the first
- [ ] `PostgresSelfStandingTest.aTiedPlayersOwnStandingIsTheSharedRank` passes
- [ ] `PostgresSelfStandingTest.aPlayerWhoFinishedNoDuelThisSeasonHasNoStanding` passes for both
      kinds of player, and asserts the July player's non-zero `coin_balance`
- [ ] `standingOf` returns `null` and never a zero-valued `SelfStandingResponse`
- [ ] `PostgresStandingsReadsTest` passes with every assertion unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
