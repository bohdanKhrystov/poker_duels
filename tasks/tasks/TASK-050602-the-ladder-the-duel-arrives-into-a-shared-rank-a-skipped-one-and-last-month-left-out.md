---
schema: 2
id: TASK-050602
title: The ladder the duel arrives into — a shared rank, a skipped one, and last month left out
type: task
status: backlog
parent: STORY-0506
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, fixtures, tests]
depends_on: [TASK-050601]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theLadderTheDuelArrivesIntoSharesARankAndSkipsTheNext' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The story's standing fixture exists — four players, a tie, a skipped rank and one duel from last
month — and is asserted to be the ladder it claims to be **before** any duel is played into it.
Four later tickets seed it and assert what the duel did to it.

## Why the fixture is a ticket of its own

Every remaining test in this class is a difference against this ladder, so a fixture that is
quietly the wrong shape would make four later tests agree with each other and with nothing else.
Two properties are deliberately built in and asserted here rather than assumed:

- **No rank in it is its row's position.** `ADR-0064` §1: two players tied at `0` both read `2` and
  the next distinct standing reads `4`. A fixture reading `1, 2, 3, 4` could not tell a competition
  rank from an offset, and `TASK-050205` and `TASK-050213` both refused to introduce one.
- **One duel sits in last season**, so the window in `ADR-0061` §4 is load-bearing from the first
  test onwards rather than in one late test.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` — how a
  `FinishedDuel` fixture row is built and recorded (`wonDuel`, `drawnDuel`,
  `duelResultStore.record`). Copy that shape; do not copy its fixed clock.
- `poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt` — the constructor and
  `formatLabel`.
- `poker-server/src/main/kotlin/duels/poker/server/season/Season.kt` — `Season.start`,
  `currentSeason(clock)`.

## Scope

Add to `SocketLadderTest`, nothing else:

- `private const val FILLER_ONE_DEVICE = "e2e-f1"` and `FILLER_TWO_DEVICE = "e2e-f2"` at file
  scope.
- `private val ladderSeason: Season = currentSeason(Clock.systemUTC())`, read **once** per test
  instance, and two instants derived from it:
  - `private fun thisSeasonAt(offsetMillis: Long): Instant = ladderSeason.start.plusMillis(offsetMillis)`
  - `private fun lastSeasonAt(): Instant = ladderSeason.start.minusSeconds(1)`

  `Season.start` is the inclusive edge of the window `PostgresStandingsReads` reads
  (`[season.start, asOf)`), and every ladder request in this class mints its `asOf` from
  `Clock.systemUTC()` **after** these rows are written, so a row at `season.start + a few ms` is
  inside the window at both ends however close to a month boundary the suite runs. Do **not**
  derive a fixture instant from `Instant.now()` minus a duration: on the first of the month that
  lands in last season.
- `private suspend fun resolvePlayer(deviceId: String): Player`, delegating to
  `PostgresPlayerDirectory(dataSource).resolve(DeviceId(deviceId))`. The upsert returns the
  existing row for a device the socket already seated, so this is how the test gets `HOST_DEVICE`'s
  and `GUEST_DEVICE`'s `Player`.
- `private suspend fun recordWin(winner: Player, loser: Player, at: Instant)` and
  `private suspend fun recordDraw(first: Player, second: Player, at: Instant)`, both building a
  `FinishedDuel` with a fresh `UUID.randomUUID()`, `format = formatLabel(DuelFormat.DEFAULT)`,
  `startedAt = at`, `finishedAt = at`, and recording it through `PostgresDuelResultStore(dataSource)`.
  The win carries `DuelOutcome(winner = 0, handsPlayed = 1, finalStacks = listOf(20_000, 0))` with
  the winner in seat 0; the draw carries `DuelOutcome(winner = null, handsPlayed = 1,
  finalStacks = listOf(10_000, 10_000))`.
- `private data class LadderFixture(val fillerOne: Player, val fillerTwo: Player)` and
  `private suspend fun seedTheLadderTheDuelArrivesInto(host: Player, guest: Player): LadderFixture`,
  recording exactly the three duels in the table below.
- `private fun StandingsResponse.rowFor(player: Player): StandingRow`, which fails with a message
  naming the player id and the rows actually returned when there is no such row.
- The one test below.
- No production file is created or modified.

## Out of scope

- **A fixed `Clock`.** `StandingsWalkDatabaseTest` installs one because it drives the route
  directly; this class installs the shipped composition through `installDuelServer`, whose
  `wallClock` is `Clock.systemUTC()`. Pinning it would put the socket duel's own `finished_at` at
  exactly the `asOf` the ladder is read at, and the window is **half-open** — `finished_at < asOf`
  — so the duel under test would vanish from the ladder it is supposed to move.
- **Playing the duel.** This test asserts the fixture before `playToFinish` is ever called;
  `TASK-050603` onwards play into it.
- **Asserting which of the two tied rows comes first.** `ADR-0064` §4: the order tied rows are
  emitted in is arbitrary and is not a measure of anything. Assert the rank list and each player's
  own row, never the position of `guest` relative to `fillerTwo`.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

**The fixture — recorded after `openSocketDuel()`, before any duel is played.**

| # | Duel | When | Standing it moves |
| --- | --- | --- | --- |
| 1 | `host` beats `fillerOne` | `thisSeasonAt(1)` | host `+1`, fillerOne `−1` |
| 2 | `guest` draws with `fillerTwo` | `thisSeasonAt(2)` | both `0` (`ADR-0015`) |
| 3 | `fillerOne` beats `fillerTwo` | `lastSeasonAt()` | neither, this season |

This season's ladder is therefore `host +1`, `guest 0`, `fillerTwo 0`, `fillerOne −1` — ranks
`1, 2, 2, 4`, four rows, summing to zero.

| Test | Proves |
| --- | --- |
| `theLadderTheDuelArrivesIntoSharesARankAndSkipsTheNext` | one request with `limit = 10` and `HOST_DEVICE`: `rows` has exactly four entries whose ranks in page order are `1, 2, 2, 4`; `host` reads rank `1` coins `1`; `guest` reads rank `2` coins `0`; `fillerTwo` reads rank `2` coins `0`; `fillerOne` reads rank `4` coins `−1`; and `nextCursor` is `null` |

**Named mutations.** Deriving a rank from a row's position — `1, 2, 3, 4` — reddens the rank list.
Dropping the window's lower bound so last season's duel #3 counts moves `fillerOne` to `0` and
`fillerTwo` to `−1`; note that this leaves the rank **list** `1, 2, 2, 4` intact, so it is
`fillerOne`'s `−1`, not the ranks, that catches it — assert both.

## Acceptance criteria

- [ ] `SocketLadderTest.theLadderTheDuelArrivesIntoSharesARankAndSkipsTheNext` passes
- [ ] It asserts `rows.map { it.rank }` equals `listOf(1, 2, 2, 4)` in page order
- [ ] It asserts all four players' `coins` individually, including `fillerOne` at `−1`
- [ ] `seedTheLadderTheDuelArrivesInto` records exactly three duels, one of them at `lastSeasonAt()`
- [ ] No fixture instant in the file is derived from `Instant.now()`; every one comes from
      `ladderSeason.start`
- [ ] `theLadderKnowsBothDuellistsAndPlacesNeitherBeforeTheyPlay` passes with its assertions
      unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
