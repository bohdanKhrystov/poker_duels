---
schema: 2
id: TASK-040114
title: The server it ships with can write a name
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, wiring]
depends_on: [TASK-040113]
verify:
  - ./gradlew :poker-server:test --tests '*ServerComponentsTest.theWritesAndTheReadsShareOneDatabase' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ServerComponentsTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The real components a shipping server is built from include a `ProfileWrites` backed by the same
data source everything else uses.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/ServerComponentsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read — the constructor this wiring calls |

## Scope

- `ServerComponents` gains `val writes: ProfileWrites`, and `serverComponents(...)` builds a
  `PostgresProfileWrites(dataSource)` beside the existing `PostgresProfileReads(dataSource)`.
- Both share the one `DataSource` the function is given. A second pool would be a second
  configuration nobody set.
- One test added, in the shape of `theReadsSeeWhatTheDirectoryWrote`: the wired writes set a name on
  a player the directory resolved, and the wired reads see it. That is the wiring proven end to end
  rather than a `assertNotNull(components.writes)`.

## Out of scope

- Installing the route — `TASK-040115` passes `components.writes` to `profileRoutes`.
- Any behaviour of the port itself, which `TASK-040111` owns.

## Tests

`ServerComponentsTest`, added to the existing class.

| Test | Proves |
| --- | --- |
| `theWritesAndTheReadsShareOneDatabase` | a name set through `components.writes` is read back through `components.reads` for the same player — so the two are not wired to different sources |

## Acceptance criteria

- [ ] `ServerComponentsTest.theWritesAndTheReadsShareOneDatabase` passes
- [ ] The new test reads the name back through `components.reads`, not through a fresh
      `PostgresProfileReads` it built itself
- [ ] `serverComponents` constructs exactly one `PostgresProfileWrites` and passes it the same
      `dataSource` argument the reads receive
- [ ] Every test already in `ServerComponentsTest` passes unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
