---
schema: 2
id: TASK-050206
title: The ladder is results, not players — a draw earns a row, a nameless player keeps one, one duel is enough
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, leaderboard, tests]
depends_on: [TASK-050205]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresStandingsReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The set the query returns is exactly `ADR-0061` §4's — whoever finished at least one duel in the
season — narrowed by nothing at all (`ADR-0063` §1), with the two cases an obvious implementation
gets wrong pinned by tests: the drawn duel that earns a row at `0`, and the nameless player who
keeps their row and carries `null`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | read |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read |

## Scope

- Three tests added to the existing class, each recording its own duels.
- A display name is set through `PostgresProfileWrites.setDisplayName`, never by raw SQL — the
  permanence trigger and the NFC constraints are the write path's, and a test that goes around them
  is testing a row this product cannot produce.
- Nothing in production changes. As in `TASK-050205`, a red test here means `TASK-050204`'s SQL is
  wrong and the fix lands in this diff.

## Out of scope

- **The self standing's *no place this season* answer** — `TASK-050208` at the port and
  `TASK-050216` over HTTP. This ticket is about **rows on the page**, which is a different question
  from what the requester is told about themselves.
- **`No name`.** The wire carries `null` (`ADR-0029` §6); the string is the client's
  (`ADR-0058`, `STORY-0503`) and appears nowhere in this module.
- **An eligibility predicate of any kind**, including a *temporary* one for test convenience —
  `ADR-0063` §1.
- Changing any assertion already in the class.

## Tests

`PostgresStandingsReadsTest`, `-PrequireDocker=true`. Season `Season(2026, 8)`,
`asOf = season.endExclusive`.

| Test | Proves |
| --- | --- |
| `aDrawEarnsARowAtZeroAndAPlayerWhoDidNotPlayHasNone` | alice draws bob in August (`ADR-0015` writes two rows of `0`); carol holds a profile and has finished **no** duel at all; dave and erin finished one in **July**. The page holds exactly alice and bob, both at `coins = 0`, and holds none of carol, dave, erin |
| `aNamelessPlayerHasARowCarryingNull` | alice has a display name set through `PostgresProfileWrites`, bob has never set one; both are on the same page, alice's `displayName` is her name and bob's is `null` |
| `onePlayerWithOneDuelIsOnThePageBesideOneWithSeveral` | frank finished exactly one duel this season, losing it, and is on the page at `coins = -1`; the player who beat him three times is on the same page at `+3`; both rows are present in one read with no parameter, profile field or credential distinguishing them |

**Two inputs everywhere.** The first test's *absent* players are two different kinds of absence — a
profile that played nothing, and a profile whose only duel is in the neighbouring month — because a
`LEFT JOIN player` implementation passes a fixture that has only the second kind, and a missing
lower bound passes one that has only the first.

**Named mutations.** `LEFT JOIN player` (or any read that starts from `player`) reddens the first
test by listing carol at `0`. A `WHERE dr.coin_delta <> 0` reddens it by dropping the drawn pair.
`COALESCE(p.display_name, 'No name')` or `WHERE p.display_name IS NOT NULL` reddens the second.
`HAVING count(*) > 1` — the shape a minimum-duels gate takes — reddens the third.

## Acceptance criteria

- [ ] `PostgresStandingsReadsTest.aDrawEarnsARowAtZeroAndAPlayerWhoDidNotPlayHasNone` passes, with
      both kinds of absent player in the fixture
- [ ] `PostgresStandingsReadsTest.aNamelessPlayerHasARowCarryingNull` passes, asserting `null` and
      asserting the named player's name in the same page
- [ ] `PostgresStandingsReadsTest.onePlayerWithOneDuelIsOnThePageBesideOneWithSeveral` passes
- [ ] The display name in the second test is set through `PostgresProfileWrites`, not by raw SQL
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
