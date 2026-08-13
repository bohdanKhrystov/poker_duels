---
schema: 2
id: TASK-021207
title: Neither client ever received the other's hole cards before the reveal
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, testing, security, end-to-end]
depends_on: [TASK-021206]
verify:
  - ./gradlew :poker-server:test --tests '*SocketSecrecyTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Across a whole duel played over real sockets, no frame either client actually received carried the
opponent's hole cards before the engine revealed them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketSecrecyTest.kt` | create |

Read, do not modify: `e2e/SocketDuel.kt`, `e2e/E2eServer.kt`,
`test/duel/RunnerLeakTest.kt` (the same three claims one layer down).

## Scope

- One private checker, so the same code proves both the claim and its own falsifiability:

  ```kotlin
  private fun leaks(seat: Int, messages: List<ServerMessage>): List<String>
  ```

  It walks `messages` in order, tracking which seats a `HandRevealed` has named **in the current
  hand only** — reset on every `HandStarted` — exactly as `RunnerLeakTest.revealedAtEachSnapshot`
  does. It returns one description per violation, and violations are of two kinds:
  - a `Snapshot` whose `view.seats[1 - seat].holeCards` is non-empty while `1 - seat` has not been
    revealed in the current hand;
  - an `Events` frame carrying a `HoleCardsDealt` for `1 - seat`.
- Everything comes from `client.received`. This test reaches no `RoomRegistry`, no `DuelRunner`, no
  `GameState`, no `MatchLog`: a statement about what a client can learn is only honest if it is
  built from what the client was actually sent.
- The frames are the ones the harness collected, not re-derived: no frame is re-encoded, re-decoded
  or re-projected on the way into the assertion.

## Out of scope

- Chips, coins, history, reconnection — one file each, `TASK-021208` onwards.
- Any assertion about frames the server *did not* send. Absence of a frame is not this ticket's
  claim; the content of the frames that arrived is.

## Tests

`SocketSecrecyTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. One duel per test,
each in its own `testApplication` on a fresh migrated schema.

| Test | Proves |
| --- | --- |
| `neitherClientEverSawTheOthersCards` | after `playToFinish()`, `leaks(seat, client.received)` is empty for both clients; the assertion message names the hand and policy seeds |
| `eachClientSawItsOwnTwoCards` | every `Snapshot` a client received shows exactly two hole cards for its own seat, and each client received at least `outcome.handsPlayed` snapshots — the fixture that stops the test above passing because nothing was ever sent |
| `theCheckerCatchesAPlantedCard` | taking the first `Snapshot` the host received and `copy`ing the viewer's `holeCards` onto the opponent's `SeatView`, `leaks` on that doctored list is **not** empty |

## Acceptance criteria

- [ ] `SocketSecrecyTest.neitherClientEverSawTheOthersCards` passes
- [ ] `SocketSecrecyTest.eachClientSawItsOwnTwoCards` passes
- [ ] `SocketSecrecyTest.theCheckerCatchesAPlantedCard` passes
- [ ] The file names none of `RoomRegistry`, `DuelRunner`, `GameState`, `MatchLog`, and imports
      nothing from `duels.poker.engine.log`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
