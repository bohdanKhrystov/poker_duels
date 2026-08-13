---
schema: 2
id: TASK-021210
title: The duel appears in both players' recent duels with opposite deltas
type: task
status: ready
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, testing, end-to-end]
depends_on: [TASK-021209]
verify:
  - ./gradlew :poker-server:test --tests '*SocketHistoryTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The duel the two sockets just played is the one duel in each player's `GET /api/me/duels`, seen
from opposite sides.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketHistoryTest.kt` | create |

Read, do not modify: `e2e/SocketDuel.kt`, `protocol/http/ProfileDtos.kt`
(`RecentDuelsResponse`, `DuelSummaryResponse`), `protocol/http/DuelOutcomes.kt`
(`DuelOutcomeLabel`), `http/ProfileRoutes.kt`.

## Scope

- Both lists are read with `client.get("/api/me/duels") { header(DEVICE_ID_HEADER, deviceId) }` in
  the same `testApplication` that played the duel, decoded with `protocolJson` into a
  `RecentDuelsResponse`.
- The winner's device is `duel.seat(outcome.winner).deviceId`, taken from the `DuelFinished` frame.
- Each player's `playerId` for the opponent cross-check comes from that player's own `GET /api/me`.
- Failure messages name the hand seed and the policy seed.

## Out of scope

- Ordering, the `limit` parameter and the cap — `TASK-021107` owns those and proves them directly.
- Balances — `TASK-021209`.

## Tests

`SocketHistoryTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. Each test runs in
its own `testApplication` on a fresh migrated schema, playing one duel with `playToFinish()`.

| Test | Proves |
| --- | --- |
| `oneDuelIsInBothPlayersLists` | each list holds exactly one `DuelSummaryResponse`, and both name the same `duelId` |
| `theDeltasAreOppositeAndTheOutcomesAgree` | the two `coinDelta`s are `1` and `-1` and sum to `0`; the winner's `outcome` is `DuelOutcomeLabel.WON` and the loser's is `LOST`; each entry's `opponentPlayerId` is the other player's `playerId` from `GET /api/me` |
| `bothListsAgreeWithTheFrameOnHandsPlayed` | each entry's `handsPlayed` equals `outcome.handsPlayed` from the `DuelFinished` frame the clients received — the socket and the read path describe one duel |

## Acceptance criteria

- [ ] `SocketHistoryTest.oneDuelIsInBothPlayersLists` passes
- [ ] `SocketHistoryTest.theDeltasAreOppositeAndTheOutcomesAgree` passes
- [ ] `SocketHistoryTest.bothListsAgreeWithTheFrameOnHandsPlayed` passes
- [ ] The file names no `PostgresDuelResultStore` and runs no SQL: everything it asserts on came
      over HTTP or over the socket
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
