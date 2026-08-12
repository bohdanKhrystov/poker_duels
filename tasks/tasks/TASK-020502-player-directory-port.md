---
schema: 2
id: TASK-020502
title: Declare the PlayerDirectory port and an in-memory implementation for tests
type: task
status: done
parent: STORY-0205
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, session, identity]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*InMemoryPlayerDirectoryTest'
  - ./gradlew :poker-server:check
---

## Goal

The server has a `PlayerDirectory` port that turns a device id into a profile, and a test-source
implementation of it, so the rest of this story runs with no database.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectory.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/InMemoryPlayerDirectoryTest.kt` | create |

Read, do not modify: `docs/adr/ADR-0012-device-bound-anonymous-profiles.md` (a profile is bound to
a device id and outlives the connection).

## Scope

- New package `duels.poker.server.session`. Main source, one file, four public declarations with
  KDoc:

  ```kotlin
  @JvmInline
  public value class DeviceId(public val value: String) {
      init { require(value.isNotBlank()) { "a device id must not be blank" } }
  }

  @JvmInline
  public value class PlayerId(public val value: String)

  public data class Player(val id: PlayerId, val deviceId: DeviceId)

  public fun interface PlayerDirectory {
      public suspend fun resolve(deviceId: DeviceId): Player
  }
  ```

- `resolve` is **`suspend`** and takes a non-null `DeviceId`. Both choices are load-bearing:
  `STORY-0210` implements this against PostgreSQL and needs to suspend rather than block a
  connection's coroutine, and minting an id when the client presents none is the *connection's*
  job (`TASK-020503`), not the directory's — a directory that mints hides identity creation behind
  a lookup.
- `resolve` is defined to be idempotent per device id: the same `DeviceId` returns a `Player` with
  the same `PlayerId` forever. State that in the KDoc; it is the contract `STORY-0210` must honour.
- The in-memory implementation is `internal class InMemoryPlayerDirectory : PlayerDirectory` in
  **test sources**, backed by a `ConcurrentHashMap<DeviceId, Player>` with `computeIfAbsent`, ids
  minted as `PlayerId("player-" + counter.incrementAndGet())` from an `AtomicInteger`, plus
  `internal val profileCount: Int get() = players.size` so tests can assert that no second profile
  was created. It never ships: `ADR-0011` puts the real one behind this same port.

## Out of scope

- Minting a device id — `TASK-020503` owns `DeviceIdSource`.
- Sessions, connections and the registry — `TASK-020504`.
- Any Postgres, Exposed or SQL — `STORY-0210`. Nothing in this ticket names a database.
- A display name, coin balance or rating on `Player`. `EPIC-04` and `EPIC-05` add fields when they
  have a use for them; a field with no reader is a guess.

## Tests

`InMemoryPlayerDirectoryTest`, JUnit 5, package `duels.poker.server.session`. `resolve` suspends,
so each test body runs inside `kotlinx.coroutines.runBlocking`.

| Test | Proves |
| --- | --- |
| `resolvingANewDeviceCreatesOneProfile` | after one `resolve(DeviceId("d1"))`, `profileCount == 1` and the returned `Player.deviceId` is `DeviceId("d1")` |
| `resolvingTheSameDeviceTwiceReturnsTheSameProfile` | two `resolve(DeviceId("d1"))` calls return equal `Player`s and `profileCount` is still `1` |
| `differentDevicesGetDifferentProfiles` | `d1` and `d2` resolve to different `PlayerId`s and `profileCount == 2` |
| `aBlankDeviceIdIsRejected` | `DeviceId("")` and `DeviceId("   ")` each throw `IllegalArgumentException` |
| `concurrentResolvesOfOneDeviceCreateOneProfile` | 100 coroutines launched over `Dispatchers.Default` resolving `DeviceId("d1")` leave `profileCount == 1` and every returned `PlayerId` equal |

## Acceptance criteria

- [ ] `InMemoryPlayerDirectoryTest.resolvingANewDeviceCreatesOneProfile` passes
- [ ] `InMemoryPlayerDirectoryTest.resolvingTheSameDeviceTwiceReturnsTheSameProfile` passes
- [ ] `InMemoryPlayerDirectoryTest.differentDevicesGetDifferentProfiles` passes
- [ ] `InMemoryPlayerDirectoryTest.aBlankDeviceIdIsRejected` passes
- [ ] `InMemoryPlayerDirectoryTest.concurrentResolvesOfOneDeviceCreateOneProfile` passes
- [ ] `PlayerDirectory.resolve` is declared `suspend` and takes a non-null `DeviceId`
- [ ] Nothing under `poker-server/src/main/` names `InMemoryPlayerDirectory`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
