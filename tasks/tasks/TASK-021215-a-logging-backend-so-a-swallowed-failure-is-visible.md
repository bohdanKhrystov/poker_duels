---
schema: 2
id: TASK-021215
title: A logging backend, so a swallowed sweep failure is visible
type: task
status: done
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, infrastructure, observability]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.LoggingBackendTest'
  - ./gradlew :poker-server:check
---

## Goal

A `logger.error(...)` from `poker-server` reaches somewhere a person can read. Today it does not.

## What was found

Found in `TASK-020812`. That ticket added the first `logger.error` in the module — the per-room
`catch` that keeps one room's failure from orphaning the rest of a sweep. It runs, and the message
goes nowhere:

```
SLF4J(W): No SLF4J providers were found.
```

The SLF4J **API** arrives transitively through Ktor, so the call compiles. No **backend** is on the
classpath — nothing in `poker-server/build.gradle.kts`, nothing in `gradle/libs.versions.toml` — so
SLF4J falls back to its no-op logger and every message is discarded silently.

**This makes `ADR-0025` decorative.** Its failure design is:

> any `Throwable` except `CancellationException` is logged and the pass retried next tick, so
> nothing but shutdown cancellation ends the loop

A sweep that dies is exactly the failure the ADR exists to prevent, and "logged" is how an operator
finds out. Without a backend, "log and continue" is **swallow and continue**: a server whose sweeps
fail every tick looks identical to one where nothing is wrong, and rooms nobody can join accumulate
in silence.

`TASK-021212` builds the production ticker on that error handling, which is why this lands first.

## Files

| File | Action |
| --- | --- |
| `gradle/libs.versions.toml` | modify |
| `poker-server/build.gradle.kts` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/LoggingBackendTest.kt` | create |

A configuration file, if the chosen backend needs one, replaces the test in this budget — say so and
stop rather than taking a fourth file.

## Scope

- Add a logging backend to the catalog and the server's runtime classpath. `logback-classic` is the
  ordinary choice for Ktor and needs no configuration file to log at `INFO` to the console; prefer
  it unless something in the build makes it awkward.
- Keep it a **runtime** dependency. Production code must keep depending on the SLF4J API only, so no
  source file names the backend.
- The test proves a real backend is bound — see below.

## Out of scope

- Log levels, formats, appenders, structured logging, or a configuration file beyond what is needed
  to bind a backend at all. Getting from "discarded" to "visible" is the whole ticket.
- Adding logging calls anywhere. `TASK-020812`'s is the only one, and it stays as it is.
- `poker-engine`, which takes no dependencies and logs nothing.

## Tests

`LoggingBackendTest`

| Test | Proves |
| --- | --- |
| a logger factory binds a real implementation | `LoggerFactory.getILoggerFactory()` is not SLF4J's `NOPLoggerFactory` — the exact condition that made `TASK-020812`'s error vanish |

**The trap in this ticket is a test that passes either way.** `LoggerFactory.getLogger(...)` returns
a non-null logger with no backend present, and `logger.error(...)` throws nothing when discarded. So
neither proves anything. Assert on the *factory implementation*, which is the thing that actually
differs.

Confirm it can fail: with the backend dependency removed the test must go red. Say in the PR that
you checked, and how.

## Acceptance criteria

- [ ] A logging backend is on `poker-server`'s runtime classpath, declared through the catalog
- [ ] No production source file names the backend — only the SLF4J API
- [ ] `LoggingBackendTest` asserts the bound factory is not the NOP one, and fails without the
      dependency
- [ ] Running the server's tests no longer prints `No SLF4J providers were found`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
