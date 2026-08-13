---
schema: 2
id: TASK-021205
title: Two real sockets create and join one room by code
type: task
status: ready
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, testing, end-to-end]
depends_on: [TASK-021204]
verify:
  - ./gradlew :poker-server:test --tests '*SocketDuelTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Two WebSocket clients, each with its own handshake and its own profile row, end up seated in one
room — one by `CreateRoom`, the other by `JoinRoom` with the code the first was given.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuelTest.kt` | create |

Read, do not modify: `e2e/E2eServer.kt`, `DuelSocketRoomTest.kt` (the same flow, written by hand),
`protocol/ClientMessage.kt`.

## Scope

- One file, `internal` throughout:

  ```kotlin
  internal const val HOST_DEVICE: String = "e2e-host"
  internal const val GUEST_DEVICE: String = "e2e-guest"

  internal class SocketClient(val deviceId: String, val seat: Int, var session: DefaultClientWebSocketSession) {
      val received: MutableList<ServerMessage> = mutableListOf()
  }

  internal class SocketDuel(val code: String, val handSeed: Long, val clients: List<SocketClient>) {
      fun seat(index: Int): SocketClient = clients.single { it.seat == index }
  }

  internal suspend fun HttpClient.openSocketDuel(handSeed: Long = HAND_SEED): SocketDuel
  ```

- `openSocketDuel` opens `/ws` for `HOST_DEVICE`, completes its handshake, sends `CreateRoom`, and
  reads the `RoomJoined` that answers it; then opens a second `/ws` for `GUEST_DEVICE`, completes
  its handshake, sends `JoinRoom(code)`, and reads its `RoomJoined`. The seat of each client is the
  one **the server named in that frame** — never assumed from the order they connected.
- `received` records every `ServerMessage` a client got after its `Welcome`, starting with its own
  `RoomJoined`, in arrival order. This list is the only evidence the later assertions in this story
  are allowed to use, so nothing may be filtered out of it on the way in.
- Nothing else is read off either socket here. The opening hand's frames are already queued on the
  guest's join and stay queued for `TASK-021206`'s loop to collect.
- `session` is a `var` so that `TASK-021211` can replace one client's socket without rebuilding the
  duel. Say that in its KDoc; nothing in this ticket assigns to it.
- `handSeed` is recorded, not used: the server was already given it by `installDuelServer`. Its
  KDoc must say that a caller passing a different value to each is lying to its own failure
  messages.

## Out of scope

- Playing the duel — `TASK-021206`.
- Any assertion about cards, chips, coins or reconnection — `TASK-021207` onwards, one file each.
- Choosing the duel format. `CreateRoom` always opens `DuelFormat.DEFAULT`, and a freezeout is what
  this story wants: it always ends with a winner, so the coin assertions have no draw to handle.

## Tests

`SocketDuelTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. `@BeforeEach` builds
a fresh migrated database; each test is `testApplication { installDuelServer(dataSource); … }` with
a client that has `install(WebSockets)`.

| Test | Proves |
| --- | --- |
| `bothClientsAreSeatedInOneRoom` | the two clients' `RoomJoined` frames name the same code, and their seats are exactly `setOf(0, 1)` |
| `eachDeviceGotItsOwnProfile` | `PostgresProfileReads(dataSource).profileOf` is non-null for both `HOST_DEVICE` and `GUEST_DEVICE`, and the two `playerId`s differ — two handshakes, two profiles, as a browser would create them |

## Acceptance criteria

- [ ] `SocketDuelTest.bothClientsAreSeatedInOneRoom` passes
- [ ] `SocketDuelTest.eachDeviceGotItsOwnProfile` passes
- [ ] `SocketDuel.kt` reaches no `RoomRegistry`, `Room`, `DuelRunner` or `GameState`: everything it
      knows about the room comes from a decoded `ServerMessage`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
