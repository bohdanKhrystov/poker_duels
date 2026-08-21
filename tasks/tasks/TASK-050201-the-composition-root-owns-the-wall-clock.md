---
schema: 2
id: TASK-050201
title: The composition root owns the one wall clock, and no component mints its own
type: task
status: done
parent: STORY-0502
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [server, wiring, clock]
depends_on: []
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultSinkTest' -PrequireDocker=true
  - ./gradlew :poker-server:compileKotlin
  - ./gradlew :poker-server:ktlintCheck
  - "grep -qF 'wallClock: Clock = Clock.systemUTC()' poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt"
  - test 1 -eq "$(grep -rlF '= Clock.systemUTC()' poker-server/src/main/kotlin | wc -l | tr -d ' ')"
---

## Goal

`serverComponents` holds the server's single `java.time.Clock`, and `PostgresDuelResultSink` can no
longer make one of its own — so the route `TASK-050211` installs takes the same wall clock the sink
stamps rows with.

## Why this is here rather than in `STORY-0501`

[`ADR-0062`](../../docs/adr/ADR-0062-two-clocks-and-a-date-comes-from-java-time-clock.md) §7 names
three tickets and writes none of them. This is **(b)**, whose own words are *"due before
`STORY-0502`"*: this story is the first production caller that needs a wall clock it did not
construct itself. Tickets (a) — `ServerClock`'s KDoc — and (c) — the ambient-time guard test — are
**not** this ticket and are not in this story.

The clock is `java.time.Clock`, never `duels.poker.server.time.ServerClock`: that one is
`System.nanoTime()`, it measures elapsed time from an arbitrary epoch, and a date derived from it
lands in 1970.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/ServerComponents.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultSink.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultSinkTest.kt` | modify |

## Scope

- `serverComponents` gains a **last** parameter, spelled exactly like this because a `verify:` line
  greps for it:

  ```kotlin
  wallClock: Clock = Clock.systemUTC(),
  ```

  It goes last so every existing positional call site keeps compiling untouched.
- `ServerComponents` gains `val wallClock: Clock` as its **last** property, and `serverComponents`
  passes its parameter into it. No call site constructs `ServerComponents` directly today, so
  nothing else moves.
- `serverComponents` passes `wallClock` to `PostgresDuelResultSink`, whose `clock` parameter
  **loses its default**. Its KDoc's *"Defaults to [Clock.systemUTC]"* sentence becomes a statement
  that the composition root supplies it, with the reason: a component that can default its own
  clock is a second place a wall clock is minted, and `ADR-0062` §2 leaves exactly one.
- `PostgresDuelResultSinkTest` compiles again by passing a clock at the six call sites that relied
  on the default. Declare **one** `private val systemClock: Clock = Clock.systemUTC()` on the test
  class and pass it; the call site that already passes a `Clock.fixed` is untouched. **No assertion
  in that file moves, is added, or is weakened** — the file is in this ticket's budget only because
  removing a default breaks its call sites, and its behaviour is unchanged.

## Out of scope

- **Installing any route.** `TASK-050211` wires `standingsRoutes` into `duelServer`; this ticket
  adds no route and no endpoint.
- **`ServerClock` and `SystemClock`**, including their KDoc — `ADR-0062` §7 ticket (a), not written
  here. `duels/poker/server/time/` stays byte-identical.
- **The guard test that fails the build on an ambient time read** — `ADR-0062` §7 ticket (c), not
  written here and not this story's.
- **`RoomRegistry`'s `ServerClock`.** It measures durations and is right as it stands.
- **Any behaviour change.** Production stamped rows from `Clock.systemUTC()` before this ticket and
  after it; only where the object is built moves.

## Tests

`PostgresDuelResultSinkTest`, `-PrequireDocker=true`. **No new test method.** The existing suite is
the gate: it must pass with every assertion exactly as it is today.

The two greps in `verify:` are the real assertion this ticket adds:

| Command | Proves |
| --- | --- |
| `grep -qF 'wallClock: Clock = Clock.systemUTC()' ServerComponents.kt` | the root has the parameter, spelled so a reader finds it |
| `test 1 -eq "$(grep -rlF '= Clock.systemUTC()' poker-server/src/main/kotlin \| wc -l ...)"` | exactly **one** file under `src/main` *assigns* a system clock; the leading `= ` is load-bearing — the bare string also matches `Season.kt`'s KDoc sentence about where `Clock.systemUTC()` belongs, which would make this gate unsatisfiable |

## Acceptance criteria

- [ ] `serverComponents` declares `wallClock: Clock = Clock.systemUTC()` as its last parameter and
      `ServerComponents` exposes it as `val wallClock: Clock`
- [ ] `PostgresDuelResultSink`'s `clock` parameter has **no** default value, and its KDoc no longer
      says it defaults to anything
- [ ] Exactly one file under `poker-server/src/main/kotlin` contains `= Clock.systemUTC()` —
      restoring the sink's default makes the last `verify:` line exit 1. Match the assignment, not
      the bare call: `Season.kt`'s KDoc says where `Clock.systemUTC()` belongs, so a bare grep
      counts two files and can never exit 0
- [ ] Every test already in `PostgresDuelResultSinkTest` passes with its assertions unchanged
- [ ] `poker-server/src/main/kotlin/duels/poker/server/time/` is unchanged: `git diff --stat` names
      no file under it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
