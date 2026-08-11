---
schema: 2
id: TASK-010210
title: Fail the build if engine sources reach for ambient randomness
type: task
status: backlog
parent: STORY-0102
module: poker-engine
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [engine, test, determinism]
depends_on: [TASK-010205]
verify:
  - ./gradlew :poker-engine:test --tests '*NoAmbientRandomTest'
  - ./gradlew :poker-engine:check
---

## Goal

The determinism rule stops being a convention: a test scans the engine's production sources and
fails the build the first time one of them names a global random source.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/random/NoAmbientRandomTest.kt` | create |

## Scope

- Walk `File("src/main/kotlin")` recursively — Gradle runs tests with the module directory as
  the working directory — and read every `.kt` file's text.
- Fail if any of these fragments appears: `kotlin.random`, `java.util.Random`, `Math.random`,
  `Random(`. The failure message lists each offending file and fragment.
- A second test guards the guard: the scanned directory must exist and contain at least one
  `.kt` file, so a wrong working directory cannot make the scan pass by finding nothing.
- A comment saying why a text scan rather than a bytecode scan: it also catches a KDoc example
  or a commented-out line, which is exactly how this rule gets eroded.

## Out of scope

- Clocks, `System.currentTimeMillis`, `UUID.randomUUID` and the rest of the ambient-input family.
  They deserve the same treatment, but this ticket is randomness only; widen it in a new ticket
  when a clock first tempts someone.
- Scanning test sources. Tests may do whatever they like, and this file itself names the
  forbidden fragments.
- Scanning other modules — they do not exist yet.
- Any change to production code or to the build files.

## Tests

`NoAmbientRandomTest`

| Test | Proves |
| --- | --- |
| `engineProductionSourcesNameNoGlobalRandomSource` | no `.kt` file under `src/main/kotlin` contains `kotlin.random`, `java.util.Random`, `Math.random` or `Random(` |
| `theScannedSourceTreeIsFound` | `src/main/kotlin` exists and holds at least one `.kt` file, so the scan is never vacuous |

## Acceptance criteria

- [ ] `NoAmbientRandomTest.engineProductionSourcesNameNoGlobalRandomSource` passes
- [ ] `NoAmbientRandomTest.theScannedSourceTreeIsFound` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
