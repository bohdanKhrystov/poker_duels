---
schema: 2
id: TASK-041017
title: The port test builds its profile through the shared builder
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [server, test, dto, prep]
depends_on: [TASK-041016]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.http.ProfileWritesPortTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The one test-side call of `ProfileResponse(...)` that does not go through `profileResponse(...)` goes
through it, so `TASK-041018`'s new field costs one file fewer.

## Why

`ProfileDtoFixtures.kt` exists for exactly this and says so in its own KDoc: *"These DTOs take no
default values (see `ADR-0021`), so adding a new field to either breaks every constructor call at
once. By routing all test-side construction through these builders first, the next field either DTO
gains becomes a one-line change."* `ProfileWritesPortTest` is the one call site that was missed.
`ADR-0053` §1 forbids a default value on the field `TASK-041018` adds, so this really does break at
compile time and this really is the cheapest way to shrink that ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileWritesPortTest.kt` | modify — one construction |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | read — `profileResponse`'s parameters |

## Scope

- In `settingANameReturnsTheProfileItProduced`, replace

  ```kotlin
  val testProfile = ProfileResponse(playerId = "1", coinBalance = 42, displayName = "TestName")
  ```

  with `profileResponse(playerId = "1", coinBalance = 42, displayName = "TestName")`.
- `profileResponse` is `internal` and lives in `duels.poker.server.protocol.http`; add the import and
  drop the `ProfileResponse` import if nothing else in the file uses the type.
- ktlint groups `java.*`, `javax.*` and `kotlin.*` **after** everything else; leave the rest of the
  import block alone.
- Nothing else in the file changes — the test double, the assertions and every other test stay as
  they are.

## Out of scope

- `ProfileResponse` itself, and the new field — `TASK-041018`.
- Any other construction of `ProfileResponse`. The only two that remain are in production code
  (`PostgresProfileReads`, `PostgresProfileWrites`) and both are `TASK-041018`'s.

## Tests

`ProfileWritesPortTest`. No test added or edited; the guarantee is that the file still compiles and
passes with one construction routed differently.

| Test | Proves |
| --- | --- |
| `settingANameReturnsTheProfileItProduced` | `SetNameResult.NameSet` still carries the exact profile the double was given — `assertEquals(testProfile, result.profile)` is unchanged, so a builder that dropped or reordered an argument fails it |

## Acceptance criteria

- [ ] `ProfileWritesPortTest.settingANameReturnsTheProfileItProduced` passes
- [ ] Every other test in `ProfileWritesPortTest` passes unchanged
- [ ] `grep -R 'ProfileResponse(' poker-server/src/test` returns only the line inside
      `ProfileDtoFixtures.kt`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
