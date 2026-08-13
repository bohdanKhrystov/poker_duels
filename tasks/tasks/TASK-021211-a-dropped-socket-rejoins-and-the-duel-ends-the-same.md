---
schema: 2
id: TASK-021211
title: A dropped socket rejoins inside the window and the duel ends the same way
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, testing, resilience, end-to-end]
depends_on: [TASK-021210, TASK-020814]
verify:
  - ./gradlew :poker-server:test --tests '*SocketReconnectTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*SocketDuelTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Dropping one client's socket mid-duel and rejoining with the same device inside the grace window
resumes the same duel on the same seat, and the duel ends with the outcome it would have had
without the interruption.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketReconnectTest.kt` | create |

Read, do not modify: `e2e/E2eServer.kt`, `DuelSocket.kt` (`replyToJoinRoom`'s resume branch),
`tasks/tasks/TASK-020810-the-frames-a-returning-player-is-entitled-to.md` (what a resume delivers).

## Scope

- Added to `SocketDuel.kt`:

  ```kotlin
  internal suspend fun SocketDuel.reconnect(http: HttpClient, seat: Int)
  ```

  It closes that client's session, opens a fresh `/ws`, completes the handshake with the **same
  device id**, sends `JoinRoom(code)`, reads the `RoomJoined` that answers, checks it names the same
  seat, appends it to that client's `received`, and assigns the new session to `client.session`.
- `playToFinish` gains one defaulted parameter and two lines in its loop:

  ```kotlin
  beforeAct: suspend (SocketClient, ServerMessage.YourTurn) -> Boolean = { _, _ -> false }
  ```

  On a `YourTurn`, the loop calls `beforeAct` first; a `true` result means the hook disturbed this
  decision point, so the loop drops the prompt it holds and goes back to receiving. **This is what
  keeps the duel reproducible**: the dropped prompt consumes no policy draw, and the `YourTurn` the
  resume re-delivers consumes exactly one — which is only true because `TASK-021206` draws when the
  `Act` is sent, not when the prompt arrives.
- The test drops the client that is **on turn**, before it answers. A resume delivers that seat a
  `Snapshot` plus the `YourTurn` it is holding everyone up for (`TASK-020810`), so the duel
  continues; dropping the seat that is *not* on turn would instead pause the room and refuse the
  other player's action, which is `TASK-020807`'s subject and not this ticket's.
- **No clock is injected and no time is advanced.** The reconnect is immediate and in-process,
  which is inside the 60-second default window by any measure. A test that had to move the clock
  would be testing expiry, and expiry has no production driver yet — see `DEC-019`.

## Out of scope

- A reconnect **after** the window closes, and anything about an absent seat's forced action —
  `DEC-020` is being answered now and owns that; `TASK-020806` and `TASK-020812` own the behaviour.
- Reconnecting from a different device — `TASK-020814`'s own tests prove that is refused.
- Any change to `SocketDuel.kt` beyond the one function and the one defaulted parameter above.

## Tests

`SocketReconnectTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. Each test runs in
its own `testApplication` on a fresh migrated schema, with the same seeds as an undisturbed duel.

| Test | Proves |
| --- | --- |
| `theOutcomeIsTheSameAsWithoutTheDisconnect` | a duel played straight through and a duel of the same two seeds interrupted before its tenth `Act` produce equal `DuelOutcome`s |
| `theReturningSocketKeepsItsSeat` | the `RoomJoined` the resumed socket receives names the same seat that client held before the drop |
| `theReturningSocketIsPromptedAgain` | after that `RoomJoined`, the resumed client's `received` gains a `Snapshot` and a `YourTurn` — the duel resumed rather than stalled |

## Acceptance criteria

- [ ] `SocketReconnectTest.theOutcomeIsTheSameAsWithoutTheDisconnect` passes
- [ ] `SocketReconnectTest.theReturningSocketKeepsItsSeat` passes
- [ ] `SocketReconnectTest.theReturningSocketIsPromptedAgain` passes
- [ ] All five `SocketDuelTest` tests pass with their bodies unchanged: `beforeAct` defaults to a
      hook that returns `false`, so a caller that omits it sends the same actions in the same order
      as before
- [ ] `SocketReconnectTest.kt` contains no `delay(`, no `Thread.sleep` and no `ServerClock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
