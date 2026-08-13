---
schema: 2
id: TASK-021209
title: The winner's coin is one higher and the loser's one lower, read back over HTTP
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, testing, end-to-end]
depends_on: [TASK-021208]
verify:
  - ./gradlew :poker-server:test --tests '*SocketCoinsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A duel played over sockets moves exactly one coin, and both players can read the new balance back
over `GET /api/me` in the same running server.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` | create |

Read, do not modify: `e2e/SocketDuel.kt`, `http/ProfileRoutes.kt` (the `X-Device-Id` header),
`protocol/http/ProfileDtos.kt` (`ProfileResponse`), `duel/CoinDeltas.kt` (`ADR-0014`).

## Scope

- Balances are read with `client.get("/api/me") { header(DEVICE_ID_HEADER, deviceId) }` and decoded
  with `protocolJson` into a `ProfileResponse`, inside the same `testApplication` that ran the duel.
- The winner's device is `duel.seat(outcome.winner).deviceId`; the loser's is the other client's.
  The seat comes from the `DuelFinished` frame, never from the order the clients connected.
- **No sleep, no poll, no retry.** `RoomRegistry.act` awaits `DuelResultSink.record` before the step
  it returns is delivered, so a client holding a `DuelFinished` frame is holding proof that the row
  and both coin updates are already committed. If this test needs a wait to pass, that is a defect
  in the write path and becomes its own ticket against `STORY-0210` — not a sleep here.
- Assertions are on the raw integer read back. Nothing in the test clamps, `coerceAtLeast`es or
  takes an absolute value: a negative balance is the point (`TASK-021010`).

## Out of scope

- The recent-duels list — `TASK-021210`.
- A drawn duel. `CreateRoom` opens a freezeout, which always names a winner; the draw path is
  already covered by `TASK-021008` and `TASK-021108`.

## Tests

`SocketCoinsTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. Each test runs in its
own `testApplication` on a fresh migrated schema.

| Test | Proves |
| --- | --- |
| `bothBalancesStartAtZero` | after both handshakes and before any duel, `GET /api/me` for each device is `200` with `coinBalance == 0` — the baseline the next test measures against |
| `theWinnerGainsACoinAndTheLoserLosesOne` | after `playToFinish()`, the winner's `coinBalance` is its pre-duel value plus one and the loser's is its pre-duel value minus one, both read in the same application |
| `theLosersBalanceIsNegativeAndNotClamped` | the loser's `coinBalance` read back is exactly `-1` |

## Acceptance criteria

- [ ] `SocketCoinsTest.bothBalancesStartAtZero` passes
- [ ] `SocketCoinsTest.theWinnerGainsACoinAndTheLoserLosesOne` passes
- [ ] `SocketCoinsTest.theLosersBalanceIsNegativeAndNotClamped` passes
- [ ] The file contains no `delay(`, no `Thread.sleep`, no retry loop around an HTTP call
- [ ] The file names no `PostgresDuelResultStore` and runs no SQL: every balance it asserts on came
      over HTTP
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
