---
schema: 2
id: TASK-020723
title: A directory of live connection writers, keyed by the player behind them
type: task
status: done
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, sessions, concurrency]
depends_on: [TASK-020722]
verify:
  - ./gradlew :poker-server:test --tests '*ConnectionDirectoryTest'
  - ./gradlew :poker-server:check
---

## Goal

There is a way to find the `ConnectionWriter` of the player sitting in the *other* seat. Today a
writer is a local value inside the `/ws` block and nothing outside that coroutine can reach it, so a
frame addressed to seat 1 has nowhere to go.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/ConnectionDirectory.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/ConnectionDirectoryTest.kt` | create |

## Scope

- A `public class ConnectionDirectory` over a `ConcurrentHashMap<PlayerId, ConnectionWriter>` with
  exactly four members:

  ```kotlin
  public fun register(player: PlayerId, writer: ConnectionWriter)
  public fun forget(player: PlayerId, writer: ConnectionWriter): Boolean
  public fun writerFor(player: PlayerId): ConnectionWriter?
  public val size: Int
  ```

- Every member is **non-suspending**, for the same reason `SessionRegistry`'s are: `forget` is
  called from a connection's `finally` block, and a cleanup that can suspend is a cleanup that might
  not run under cancellation.
- `forget` takes the writer as well as the player and removes only if the stored writer *is* that
  writer — `ConcurrentHashMap.remove(key, value)`. This is the whole reason the method has two
  parameters and must be KDoc'd as such: under `ADR-0018` a second socket for one device adopts the
  seat and the older one then closes. If `forget` removed by key alone, the older socket's `finally`
  would delete the newer socket's writer and the surviving connection would silently stop receiving
  frames. `forget` returns whether it removed anything, so "the adopted socket removed nothing" is
  observable.
- `register` overwrites: the most recent socket for a player is the one that holds the seat, which is
  `ADR-0018` restated for writers.
- This class knows about players and writers and nothing else — no room, no seat, no `ServerMessage`,
  no Ktor. Mapping a seat to a player is the *room's* job and happens elsewhere.

## Out of scope

- Registering anything — `TASK-020729` wires it into the socket.
- Deciding which seat a frame belongs to — `TASK-020730`.
- Putting it in `SocketDependencies` — `TASK-020726`.

## Tests

`ConnectionDirectoryTest` — no socket, no Ktor: `ConnectionWriter` is constructible on its own.

| Test | Proves |
| --- | --- |
| `aregisteredWriterIsFoundByItsPlayer` | `writerFor` returns the exact instance `register` was given |
| `anunknownPlayerHasNoWriter` | `writerFor` is `null` for a player never registered |
| `asecondRegistrationReplacesTheFirst` | registering a second writer for one player leaves `writerFor` returning the second and `size` at 1 |
| `forgetRemovesOnlyTheWriterItNames` | after a replacement, `forget(player, firstWriter)` returns `false` and leaves the second writer in place — the `ADR-0018` case |
| `forgetRemovesTheWriterItDoesName` | `forget(player, currentWriter)` returns `true` and `writerFor` is then `null` |
| `forgettingTwiceRemovesNothingTheSecondTime` | `forget` is safe to call from a `finally` however many times it runs |

## Acceptance criteria

- [ ] `ConnectionDirectoryTest.aregisteredWriterIsFoundByItsPlayer` passes
- [ ] `ConnectionDirectoryTest.anunknownPlayerHasNoWriter` passes
- [ ] `ConnectionDirectoryTest.asecondRegistrationReplacesTheFirst` passes
- [ ] `ConnectionDirectoryTest.forgetRemovesOnlyTheWriterItNames` passes
- [ ] `ConnectionDirectoryTest.forgetRemovesTheWriterItDoesName` passes
- [ ] `ConnectionDirectoryTest.forgettingTwiceRemovesNothingTheSecondTime` passes
- [ ] `ConnectionDirectory.kt` contains no `suspend` modifier
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
