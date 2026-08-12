---
schema: 2
id: TASK-020102
title: Assert the engine and Ktor are on the server module's classpath
type: task
status: backlog
parent: STORY-0201
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [server, build]
depends_on: [TASK-020101]
verify:
  - ./gradlew :poker-server:test --tests '*PokerServerModuleTest'
---

## Goal

`:poker-server` compiles Kotlin and its declared dependencies really resolve — the engine and
Ktor are both reachable from a test in the module.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/PokerServerModuleTest.kt` | create |

Read, do not modify: `poker-ai/src/test/kotlin/duels/poker/ai/PokerAiModuleTest.kt` (the same
test, one module over), `poker-server/build.gradle.kts`.

## Scope

- One JUnit 5 test class in package `duels.poker.server`, no `src/main` source of any kind.
- Imports are exactly: `duels.poker.engine.card.Card`, `io.ktor.http.HttpStatusCode`,
  `org.junit.jupiter.api.Assertions.assertEquals`, `org.junit.jupiter.api.Test`.
- This is the ticket that fails loudly if a coordinate added in `TASK-020101` does not exist:
  compiling the test resolves the whole test compile classpath.

## Out of scope

- Anything under `poker-server/src/main` — `TASK-020103` writes the first one.
- Starting a server, a route, or `testApplication` — `TASK-020105`.
- Changing `poker-server/build.gradle.kts`. If a dependency turns out to be missing, that is a
  finding to report, not a widening of this ticket.

## Tests

`PokerServerModuleTest`, JUnit 5, package `duels.poker.server`.

| Test | Proves |
| --- | --- |
| `theEngineIsOnTheModulesClasspath` | `Card.all.size == 52`, imported from `duels.poker.engine.card` |
| `ktorIsOnTheModulesClasspath` | `HttpStatusCode.OK.value == 200`, imported from `io.ktor.http` |

## Acceptance criteria

- [ ] `PokerServerModuleTest.theEngineIsOnTheModulesClasspath` passes
- [ ] `PokerServerModuleTest.ktorIsOnTheModulesClasspath` passes
- [ ] No file outside the one in the Files table is created or modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
