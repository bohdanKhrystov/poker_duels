---
schema: 2
id: TASK-020802
title: The grace window is configuration, read once in ServerConfig
type: task
status: backlog
parent: STORY-0208
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, config]
depends_on: [TASK-020801]
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:compileTestKotlin
  - grep -rl 'disconnectGraceMillis' poker-server/src/test/kotlin/duels/poker/server/db | wc -l | tr -d ' ' | grep -qx 0
---

## Goal

`ADR-0013`'s window is a value in `ServerConfig` with a file key, an environment variable and a
default — "not a literal scattered through the code" — and `ServerConfig.roomTimeouts()` hands it
down with the other two room limits.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

## Scope

- One field on `ServerConfig`, following the existing pattern exactly — a `DEFAULT_*` constant, a
  `*_KEY`, a `*_ENV`, resolution through the private `resolve`, and a
  `requireNotNull(...toLongOrNull())` whose message names the bad value:

  ```kotlin
  val disconnectGraceMillis: Long = RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS,
  ```

  Key `duel.disconnectGraceMillis`, environment variable `DISCONNECT_GRACE_MILLIS`, default
  `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` (`TASK-020801`) — the same delegation
  `DEFAULT_ROOM_WAITING_TIMEOUT_MILLIS` already makes.
- `roomTimeouts()` becomes
  `RoomTimeouts(roomWaitingTimeoutMillis, roomFinishedTimeoutMillis, disconnectGraceMillis)`.
- **This field has a default and the eight before it do not.** Say why in a comment: three test
  files — `DatabasePoolTest`, `DatabaseStartupTest`, `PersistenceSurvivesRestartTest` — construct
  `ServerConfig(...)` field by field to point Testcontainers at a database, have no opinion on a
  duel's grace window, and are not in this ticket's budget. A field with no default would make this
  ticket a four-file change, which is two tickets, for a value those tests would only ever set to
  the default anyway. The `verify` grep and `compileTestKotlin` together prove they were left alone
  and still compile.
- The class KDoc says "Future fields from `ADR-0013`'s grace period and `ADR-0011`'s database URL
  will land here". Both have now landed; update that sentence rather than leaving it promising a
  future that has arrived.

## Out of scope

- Anything that reads the value. `RoomRegistry` picks it up off `RoomTimeouts` in `TASK-020809`;
  the end-to-end proof that changing it changes behaviour is `TASK-020815`.
- Wiring `ServerConfig` into `Application.module()` — untouched here, as in every story so far.

## Tests

`ServerConfigTest` — modified by appending three tests and strengthening one existing test. Nothing
else in the file changes and no assertion is weakened.

| Test | Proves |
| --- | --- |
| `roomTimeoutsBundlesBothValues` | **existing test, extended and renamed to `roomTimeoutsBundlesAllThreeValues`**: it builds `RoomTimeouts(waiting, finished)` positionally and compares it to `roomTimeouts()`. Once `roomTimeouts()` passes a third value, that comparison silently starts asserting "the third value equals its default" — true, and vacuous. Pass all three values explicitly so the assertion still means what its name says. Nothing else about it changes. |
| `readsTheGraceWindowFromTheConfig` | `MapApplicationConfig("duel.disconnectGraceMillis" to "45000")` yields `disconnectGraceMillis == 45_000L` |
| `theEnvironmentVariableOverridesTheGraceWindow` | with the same file value, `env(DISCONNECT_GRACE_MILLIS_ENV) = "90000"` wins |
| `rejectsAGraceWindowThatIsNotANumber` | `"a minute"` throws `IllegalArgumentException` from `ServerConfig.from` |

## Acceptance criteria

- [ ] `ServerConfigTest.roomTimeoutsBundlesAllThreeValues` passes and names all three values
- [ ] `ServerConfigTest.readsTheGraceWindowFromTheConfig` passes
- [ ] `ServerConfigTest.theEnvironmentVariableOverridesTheGraceWindow` passes
- [ ] `ServerConfigTest.rejectsAGraceWindowThatIsNotANumber` passes
- [ ] No file under `poker-server/src/test/kotlin/duels/poker/server/db` mentions
      `disconnectGraceMillis`, and `:poker-server:compileTestKotlin` succeeds
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
