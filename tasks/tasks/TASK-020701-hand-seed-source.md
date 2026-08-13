---
schema: 2
id: TASK-020701
title: Draw each hand's seed from an injected secure source, never from the engine Rng
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, duel, randomness]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*HandSeedSourceTest'
  - ./gradlew :poker-server:check
---

## Goal

`HandSeedSource` exists as a port, with a `SecureRandom`-backed implementation, so every hand a
duel deals draws its seed from the server's secure source and a test can inject a reproducible one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/HandSeedSource.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/HandSeedSourceTest.kt` | create |

Read, do not modify: `poker-server/src/main/kotlin/duels/poker/server/room/RoomCodeSource.kt` —
this ticket is the same port-plus-secure-implementation shape, down to the `isSecure` property and
the reason the engine's `Rng` is not used.

## Scope

- New package `duels.poker.server.duel`. This is the first file in it.
- Two declarations, KDoc included:

  ```kotlin
  public fun interface HandSeedSource {
      public fun newHandSeed(): Long
  }

  public class SecureHandSeedSource(private val random: Random = SecureRandom()) : HandSeedSource {
      public val isSecure: Boolean get() = random is SecureRandom
      override fun newHandSeed(): Long = random.nextLong()
  }
  ```

- `java.util.Random` and `java.security.SecureRandom`, exactly as `RandomRoomCodeSource` does.
  Never `kotlin.random.Random`, and never `duels.poker.engine.random.Rng`: the engine's generator
  is deterministic by design so a hand replays from its seed, and a seed that replays is precisely
  what the *next* hand's seed must not be.
- The KDoc says, in one sentence, that the seed is recorded in the hand's `HandLog` but never
  leaves the server while the duel is live (`ADR-0002`).

## Out of scope

- Anything that *calls* this port. `startDuel` takes a plain seed (`TASK-020705`); `advance` takes
  this port (`TASK-020707`); the caller that constructs a `SecureHandSeedSource` is `TASK-020714`.
- Persisting or exposing a seed anywhere — `TASK-020712` asserts it never reaches the wire.

## Tests

`HandSeedSourceTest`, JUnit 5, package `duels.poker.server.duel`.

| Test | Proves |
| --- | --- |
| `theDefaultSourceDrawsFromSecureRandom` | `SecureHandSeedSource().isSecure` is true |
| `anInjectedRandomIsNotReportedAsSecure` | `SecureHandSeedSource(Random(1)).isSecure` is false |
| `thesameInjectedSeedGivesTheSameHandSeeds` | two `SecureHandSeedSource(Random(1))` produce the same first three `newHandSeed()` values |
| `consecutiveDrawsFromTheDefaultSourceDiffer` | 100 draws from `SecureHandSeedSource()` contain no duplicate |
| `aLambdaSatisfiesThePort` | `HandSeedSource { 42L }.newHandSeed() == 42L`, proving the fun-interface shape tests can inject |

## Acceptance criteria

- [ ] `HandSeedSourceTest.theDefaultSourceDrawsFromSecureRandom` passes
- [ ] `HandSeedSourceTest.anInjectedRandomIsNotReportedAsSecure` passes
- [ ] `HandSeedSourceTest.thesameInjectedSeedGivesTheSameHandSeeds` passes
- [ ] `HandSeedSourceTest.consecutiveDrawsFromTheDefaultSourceDiffer` passes
- [ ] `HandSeedSourceTest.aLambdaSatisfiesThePort` passes
- [ ] Neither file imports `kotlin.random.Random` or `duels.poker.engine.random.Rng`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
