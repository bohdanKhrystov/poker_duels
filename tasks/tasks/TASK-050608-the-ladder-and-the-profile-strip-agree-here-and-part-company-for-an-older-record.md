---
schema: 2
id: TASK-050608
title: The ladder and the profile strip agree for both duellists — and part company for an older record
type: task
status: ready
parent: STORY-0506
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, seasons, tests]
depends_on: [TASK-050607]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theLadderAgreesWithTheProfileStripForBothDuellistsAndDisagreesForAnOlderRecord' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The number on the ladder row and the number in the profile strip are equal for both duellists — and
the test says, in the same breath and on the same fixture, that this is a **property of the
fixture** and not a promise the product makes.

## The sentence this ticket exists to keep honest

`STORY-0506` asks for the agreement and then warns about it: the two numbers are `ADR-0061` §4's
season window and §6's all-time counter, and they part company for anybody who played in more than
one month. A test asserting only the agreement would be read, later and by somebody in a hurry, as
a guarantee it never made — and it would go on passing under an implementation that read
`player.coin_balance` for the ladder, which is the single most likely wrong answer on this whole
read path.

So the same test carries both: two players for whom the numbers agree, because every duel they have
played is inside this season, and two for whom they differ, because of the one duel
`seedTheLadderTheDuelArrivesInto` put in last season. One fixture cannot tell a window from a
column; this one can.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md` — §4, §5, §6.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` — `profileOf`, the
  `GET /api/me` read to copy.
- `poker-server/src/test/kotlin/duels/poker/server/db/PostgresStandingsReadsTest.kt` —
  `theCoinsAreTheSeasonsWindowAndNotTheAllTimeColumn`, the same two inputs one layer down.

## Scope

- A private `HttpClient.profileOf(deviceId: String): ProfileResponse` on `SocketLadderTest`, copied
  from `SocketCoinsTest`: asserts `200`, decodes with `protocolJson`.
- One new test on the fixture `seedTheLadderTheDuelArrivesInto(host, guest)` followed by
  `playToFinish()`.
- No production file is created or modified.

## An assertion shape this story has already been caught by

`TASK-050603` shipped two *unchanged across the duel* assertions of the form
`assertEquals(beforeFiller, afterFiller)`. Deep review confirmed by isolating them that they **pass
vacuously** under a ladder that is consistently wrong on both reads — they compare a value to itself,
and carry weight only because a *separate, earlier* assertion pinned the before-value to a literal.
That safety net is positional, not structural, and it disappears the moment the idiom is copied.

**So: every "unchanged" assertion in this story must have its value pinned to a literal on at least
one side, in the same test method — never merely equal to its own earlier reading.**

## Out of scope

- **Changing `ProfileResponse` or `GET /api/me`.** `ADR-0065` §2 and `ADR-0061` §6 — the strip is
  untouched. It is read here and nothing else.
- **A season number on the profile strip.** That is `DEC-059`'s question and this test does not
  pre-empt it.
- **A fixture spanning two seasons for the duellists.** `STORY-0506` puts that in `STORY-0502`'s
  scope on purpose: the duel under test here is played *now*, so both duellists' records are
  single-season by construction, and the test says so where it asserts the agreement.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`. After the duel the four players stand as follows.

| Player | Ladder `coins` (this season) | `GET /api/me` `coinBalance` (all time) | |
| --- | --- | --- | --- |
| `host` | `0` | `0` | agree |
| `guest` | `1` | `1` | agree |
| `fillerOne` | `−1` | `0` | differ |
| `fillerTwo` | `0` | `−1` | differ |

| Test | Proves |
| --- | --- |
| `theLadderAgreesWithTheProfileStripForBothDuellistsAndDisagreesForAnOlderRecord` | one `limit = 10` ladder read and four `GET /api/me` reads: `host` and `guest` each have equal numbers, asserted as the exact values above and accompanied by a comment naming *every duel in their record finished this season* as the reason; `fillerOne` reads `−1` on the ladder against `0` in the strip, and `fillerTwo` reads `0` against `−1`, each asserted as two named values **and** as an inequality |

**Named mutations.** Reading `player.coin_balance` as the ladder's number reddens both filler
assertions and leaves both duellists' green — the exact asymmetry that makes the fillers worth
having. Zeroing `coin_balance` at a season boundary, which `ADR-0061` §5 forbids, reddens the
filler assertions from the other side.

## Acceptance criteria

- [ ] `SocketLadderTest.theLadderAgreesWithTheProfileStripForBothDuellistsAndDisagreesForAnOlderRecord`
      passes
- [ ] It asserts equality of the two numbers for `host` and for `guest`, at the values `0` and `1`
- [ ] It asserts both numbers for `fillerOne` (`−1` and `0`) and for `fillerTwo` (`0` and `−1`),
      and asserts explicitly that each pair is unequal
- [ ] A comment at the agreeing assertions names the fixture property that makes them agree, and
      states that the two numbers are not the same number in general (`ADR-0061` §6)
- [ ] Every `coinBalance` in the test is read through `GET /api/me`, never from the database
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
