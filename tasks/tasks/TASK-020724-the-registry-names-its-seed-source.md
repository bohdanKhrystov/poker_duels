---
schema: 2
id: TASK-020724
title: A room registry says which seed source its duels draw from
type: task
status: ready
parent: STORY-0207
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, rooms, determinism]
depends_on: [TASK-020723]
verify:
  - ./gradlew :poker-server:test --tests '*RoomRegistryTest'
  - ./gradlew :poker-server:check
---

## Goal

`RoomRegistry` exposes the `HandSeedSource` it was built with, so that a caller of
`Room.act(seat, message, seeds)` cannot hand the duel a *different* seed source from the one that
opened it.

## Why this and not a second field somewhere else

`Room.act` takes the seed source as a parameter, so the socket that routes an inbound `Act`
(`TASK-020715`) must supply one. The obvious move — give `SocketDependencies` its own
`HandSeedSource` — creates two sources of hand seeds for one duel: the registry's, used to open
hand 1, and the socket's, used for every hand after it. In production they would usually be the same
instance and in a test they would silently not be, which is the worst version of that bug: a duel
that is reproducible up to hand 1 and not after. One accessor removes the possibility.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/room/RoomRegistry.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/room/RoomRegistryTest.kt` | modify |

## Scope

- Add to `RoomRegistry`:

  ```kotlin
  public val handSeeds: HandSeedSource get() = seeds
  ```

- KDoc it with the reason above in one sentence: every hand of a duel this registry hosts draws from
  this one source, so a caller moving a duel forward must use it rather than a source of its own.
- Change nothing else. No new constructor parameter, no new `withLock`, no behaviour.

## Out of scope

- Using it — `TASK-020715` passes it to `Room.act`.
- Exposing `codes`, `clock`, `timeouts` or `sink`. Nothing needs them and each one widened is a
  reason for a later caller to reach past the registry's own methods.

## Tests

`RoomRegistryTest` — one new case; every existing case is untouched.

| Test | Proves |
| --- | --- |
| `theRegistryNamesTheSeedSourceItsDuelsDrawFrom` | a registry built with a given `HandSeedSource` returns that same instance from `handSeeds` (`assertSame`) |

## Acceptance criteria

- [ ] `RoomRegistryTest.theRegistryNamesTheSeedSourceItsDuelsDrawFrom` passes
- [ ] Every test method already in `RoomRegistryTest` is byte-identical in the diff
- [ ] `RoomRegistry.kt` still contains exactly one `withLock`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
