---
schema: 2
id: TASK-021213
title: The sweep period is configuration, read once in ServerConfig
type: task
status: backlog
parent: STORY-0212
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [server, config]
depends_on: [TASK-021202]
verify:
  - ./gradlew :poker-server:test --tests '*ServerConfigTest'
  - ./gradlew :poker-server:compileTestKotlin
  - ./gradlew :poker-server:check
---

## Goal

`ServerConfig.sweepPeriodMillis` carries how often the server's sweeps run, read the same way every
other tunable is.

## Why this is its own ticket

`ADR-0025` settles `DEC-019`: one ticker coroutine on the application scope drives both sweeps, on a
fixed delay of `sweepPeriodMillis`. That period is an operator's tunable, so it belongs in
`ServerConfig` — which pushes `TASK-021212` to four files, over the linter's cap of three.

`TASK-021212`'s own instruction is to re-split rather than widen, so the configuration half comes
out here. This mirrors `TASK-020801`/`TASK-020802` exactly, where `disconnectGraceMillis` was added
to the value type and then read from configuration in a separate ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/config/ServerConfig.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/config/ServerConfigTest.kt` | modify |

## Scope

- `ServerConfig` gains `sweepPeriodMillis: Long`, defaulted to `1_000`, read from key
  `server.sweepPeriodMillis` with environment override `SWEEP_PERIOD_MILLIS` — **the same constant
  shape, override mechanism and "must be an integer" failure the existing tunables use.** A third
  convention for the same kind of value is the defect this ticket exists to avoid; compare against
  its siblings line for line rather than judging it alone.
- It is validated positive, alongside the limits already validated there.
- The default keeps every existing construction site compiling, as `disconnectGraceMillis`'s did.

## Out of scope

- Anything that *reads* the period. `TASK-021212` installs the ticker.
- `RoomRegistry`, `Application.kt`, and the sweeps themselves.

## Tests

`ServerConfigTest`

| Test | Proves |
| --- | --- |
| the sweep period is read from the configuration | a configured value reaches the field |
| the environment variable overrides the sweep period | the override works like its siblings' |
| a non-numeric sweep period is refused | the same typed failure the others give |
| the default is what `ADR-0025` names | an unset key yields 1_000, not 0 or a wall-clock guess |

Units are the trap here, exactly as in `TASK-020802`: the field is `Millis`, so a test whose
expected value cannot tell seconds from milliseconds is not a test. Use a value like `2_500` where
a dropped or added factor of a thousand fails.

## Acceptance criteria

- [ ] `sweepPeriodMillis` is read, defaulted, overridable and validated like its siblings
- [ ] A configured value distinguishes milliseconds from seconds in at least one assertion
- [ ] No existing `ServerConfigTest` case is weakened; any renamed one is widened, not just renamed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
