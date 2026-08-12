---
schema: 2
id: TASK-020504
title: A SessionRegistry that maps a connection to a session and drops it exactly once
type: task
status: backlog
parent: STORY-0205
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, session]
depends_on: [TASK-020502]
verify:
  - ./gradlew :poker-server:test --tests '*SessionRegistryTest'
  - ./gradlew :poker-server:check
---

## Goal

The server has a concurrent registry of live sessions, keyed by `SessionId`, whose `remove`
returns the session the first time and `null` every time after — so "closed exactly once" is a
value, not a convention.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/session/SessionRegistry.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/session/SessionRegistryTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/session/PlayerDirectory.kt`
(for `Player` and `PlayerId`).

## Scope

- One main file, package `duels.poker.server.session`, KDoc on everything public:

  ```kotlin
  @JvmInline
  public value class SessionId(public val value: String)

  public data class Session(val id: SessionId, val player: Player)

  public class SessionRegistry {
      public fun register(session: Session)
      public fun remove(id: SessionId): Session?
      public fun get(id: SessionId): Session?
      public fun sessionsOf(playerId: PlayerId): List<Session>
      public val size: Int

      public companion object {
          public fun newSessionId(): SessionId = SessionId(UUID.randomUUID().toString())
      }
  }
  ```

- Backed by a `ConcurrentHashMap<SessionId, Session>`. Every method is non-suspending: removal runs
  from a connection's `finally`, and a `finally` that suspends is a `finally` that may not run.
- `remove` returns the removed `Session`, or `null` if that id was already gone. That return value
  is how `TASK-020508` asserts a close path fires once.
- The registry holds sessions and nothing else — no game state, no room, no channel, no clock.
- `sessionsOf` is a **query, not a policy**. Whether a player may hold two live sessions at once is
  `DEC-011`, and `TASK-020511` implements whatever it answers; this class neither permits nor
  forbids it, it reports.

## Out of scope

- Refusing, evicting or replacing a second session for one player — `DEC-011` / `TASK-020511`.
- Anything about the socket: the registry never sends, closes or knows a `WebSocketSession`.
- Expiry, a grace period, or a timestamp on `Session` — `ADR-0013` and `STORY-0208`. Adding a
  clock here is what the story forbids, and `STORY-0208` will add the field it actually needs.
- Rooms, seats and duels — `STORY-0206`, `STORY-0207`.

## Tests

`SessionRegistryTest`, JUnit 5, package `duels.poker.server.session`. Build sessions with a helper
`private fun session(player: String) = Session(SessionRegistry.newSessionId(), Player(PlayerId(player), DeviceId(player)))`.

| Test | Proves |
| --- | --- |
| `aRegisteredSessionIsFoundById` | after `register(s)`, `get(s.id) == s` and `size == 1` |
| `removeReturnsTheSessionOnceAndNullAfterwards` | first `remove(s.id)` returns `s`, second returns `null`, and `size == 0` — the exactly-once property |
| `removingAnUnknownIdReturnsNull` | `remove(SessionRegistry.newSessionId())` on an empty registry returns `null` and does not throw |
| `sessionsOfListsEverySessionForThatPlayer` | two sessions for one `PlayerId` and one for another: `sessionsOf` returns the two, and not the third |
| `newSessionIdsAreDistinct` | 10 000 `newSessionId()` values collected into a `HashSet` give `size == 10_000` |
| `concurrentRegisterAndRemoveLeavesTheRegistryEmpty` | 1 000 sessions registered and removed across coroutines on `Dispatchers.Default` end with `size == 0`, and exactly 1 000 of the `remove` calls returned non-null |

## Acceptance criteria

- [ ] `SessionRegistryTest.aRegisteredSessionIsFoundById` passes
- [ ] `SessionRegistryTest.removeReturnsTheSessionOnceAndNullAfterwards` passes
- [ ] `SessionRegistryTest.removingAnUnknownIdReturnsNull` passes
- [ ] `SessionRegistryTest.sessionsOfListsEverySessionForThatPlayer` passes
- [ ] `SessionRegistryTest.newSessionIdsAreDistinct` passes
- [ ] `SessionRegistryTest.concurrentRegisterAndRemoveLeavesTheRegistryEmpty` passes
- [ ] No method on `SessionRegistry` is `suspend`
- [ ] `SessionRegistry.kt` imports nothing from `io.ktor`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
