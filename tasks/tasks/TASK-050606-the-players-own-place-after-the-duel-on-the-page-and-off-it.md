---
schema: 2
id: TASK-050606
title: The player's own place after the duel — served on the page drawn and off it
type: task
status: backlog
parent: STORY-0506
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, self-standing, tests]
depends_on: [TASK-050605]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theSelfStandingIsTheWholeLaddersPlaceForAPlayerOnNoPageDrawn' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theSelfStandingRepeatsThePlayersOwnRowWhenTheyAreOnThePage' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

After the duel, a player asking for the ladder is told where **they** stand — and is told it
whether or not their own row came back on the page they asked for.

## Two inputs, and why the off-page one is the bottom of the ladder

`ADR-0065` §8: *"two inputs, one player on the page and one off it, because a single fixture cannot
tell a real aggregate from an echo of the page."* An implementation that finds the requester among
the rows it just drew is right whenever they are on screen and silently wrong the rest of the time,
which is the entire case the self line exists for.

The off-page input here is the player at the **bottom** of the four-row ladder, rank `4`, asked for
with `limit = 1`. That is deliberate: a self rank computed as *rows on this page plus one* would
answer `2`, which is the right answer for the second-placed player and the wrong one here. Picking
the rank-2 player as the off-page input would have let that mutation through.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `docs/adr/ADR-0065-the-ladder-hands-a-player-their-own-row.md` — §4, §5, §6, §8.
- `poker-server/src/test/kotlin/duels/poker/server/http/StandingsSelfDatabaseTest.kt` — the same
  two inputs, one layer down.

## Scope

- Two new tests on `SocketLadderTest`, both on the fixture
  `seedTheLadderTheDuelArrivesInto(host, guest)` followed by `playToFinish()`, and both reading
  with `limit = 1`.
- No new helper is needed: `ladder(deviceId, limit)` from `TASK-050601` already takes both.
- No production file is created or modified.

## Out of scope

- **A `playerId` parameter, or any *jump to me*.** `ADR-0065` §3 and §5, `DEC-057` still open. The
  requester is named by `X-Device-Id` and by nothing else.
- **A marker on the requester's row.** `ADR-0065` §5 and §6: a player appearing in both the self
  line and the page is correct, and no test here treats it as a duplicate.
- **The unknown device and the header-less request** — `TASK-050601` already owns those two of
  `ADR-0065` §4's three answers.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`. The ladder after the duel is `guest +1` (rank `1`),
`host 0` and `fillerTwo 0` (both rank `2`), `fillerOne −1` (rank `4`).

| Test | Proves |
| --- | --- |
| `theSelfStandingIsTheWholeLaddersPlaceForAPlayerOnNoPageDrawn` | `limit = 1` with `FILLER_ONE_DEVICE`: `rows` holds exactly one row, `fillerOne`'s player id is not on it, `nextCursor` is non-null, and `self` reads that player's own id with rank `4` and coins `−1` — a rank naming three players ahead on a page showing one |
| `theSelfStandingRepeatsThePlayersOwnRowWhenTheyAreOnThePage` | `limit = 1` with `GUEST_DEVICE`, the duel's winner and now rank `1`: `rows` holds exactly one row, it **is** `guest`'s row with rank `1` and coins `1`, and `self` reads the same id, rank and coins. The row is present, not filtered out for being the requester's |

**Named mutations.** Computing the self standing from the rows of the page reddens the first test
and leaves the second green — the asymmetry that makes one input insufficient. Computing the rank
as the number of rows on the page above the requester, plus one, answers `2` for `fillerOne` and
reddens the first. Removing the requester's row from the page reddens the second.

## Acceptance criteria

- [ ] `SocketLadderTest.theSelfStandingIsTheWholeLaddersPlaceForAPlayerOnNoPageDrawn` passes,
      asserting rank `4` and coins `−1` for a player on none of the rows returned
- [ ] `SocketLadderTest.theSelfStandingRepeatsThePlayersOwnRowWhenTheyAreOnThePage` passes,
      asserting the self rank and coins against that player's own row in the same response
- [ ] Both tests assert `rows.size == 1`
- [ ] Neither request carries a `playerId` query parameter
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
