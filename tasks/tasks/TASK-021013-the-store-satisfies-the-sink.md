---
schema: 2
id: TASK-021013
title: The Postgres store satisfies the DuelResultSink port
type: task
status: done
parent: STORY-0210
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [server, persistence, integration, correctness]
depends_on: [TASK-020709, TASK-021006]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultSinkTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Nothing connects the two halves of the write path, and neither half looks broken on its own.

- `STORY-0207` declares the port: `DuelResultSink.record(result: DuelResult)`, where
  `DuelResult` is `(outcome, seats, log)` — `TASK-020709`.
- `STORY-0210` delivers the store: `PostgresDuelResultStore.record(duel: FinishedDuel)`, where
  `FinishedDuel` is `(id, format, startedAt, finishedAt, seats, outcome)` — `TASK-021005`.

**`PostgresDuelResultStore` does not implement `DuelResultSink`.** The parameter types differ, so
the store cannot be handed to the runner. `TASK-020714` calls `DuelResultSink.record` and lists
"any implementation of `DuelResultSink`" as out of scope, pointing at `STORY-0210`; `STORY-0210`
says it "delivers the store it will be pointed at". Each ticket correctly assumed the other owned
the join, and nobody did.

Left as it is, every duel would finish, the runner would call a sink that nothing implements, and
**no result would ever be written** — while `PostgresDuelResultStoreTest` stayed green, because it
tests the store directly. That is the project owner's stated v0.1 requirement (see the results of a
few games stored, and the coin balance) failing in the one place the tests cannot see.

Found when `TASK-020709` landed and its coder reported the mismatch rather than bending either
type to fit.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultSink.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultSinkTest.kt` | create |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelResultSink.kt` | modify |

Read `PostgresDuelResultStore.kt`, `FinishedDuel.kt` and `MatchLog.kt`. **Modify none of them** —
the store is deep-reviewed and its transaction is proven; this ticket adapts, it does not rewrite.
`DuelResultSink.kt` may gain KDoc naming its shipping implementation, nothing more.

## Scope

An adapter class implementing `DuelResultSink` and delegating to `PostgresDuelResultStore`.

The adaptation is not a field rename — `DuelResult` genuinely lacks three things `FinishedDuel`
needs, and each has to come from somewhere honest:

- **A duel id.** `DuelResult` carries none. Generate it in the adapter from an **injected** source,
  the way `RoomCodeSource` and `HandSeedSource` are injected, so a test can make it deterministic.
  Do not call `UUID.randomUUID()` inline.
- **Timestamps.** `startedAt` and `finishedAt` must come from an injected **wall clock** —
  `java.time.Clock`, defaulting to `Clock.systemUTC()` — read as `Instant.now(clock)`. Never
  `Instant.now()` or `System.currentTimeMillis()` inline: a test that cannot control time cannot
  assert what was stored, and `Clock.fixed` makes it pinnable.

  **Not `ServerClock`.** That one is `System.nanoTime() / 1_000_000` — elapsed time from an
  arbitrary epoch — and its own KDoc says *"Never use this clock to stamp a database row with a
  date."* It is right for timeouts and grace periods, where a wall clock stepping backwards would
  stretch or collapse a duration, and wrong for a date. This ticket originally required
  `ServerClock` here; the coder implemented it as written, flagged the contradiction, and it was
  corrected before landing. Using it would have stamped every duel a few days after 1970 — and
  the recent-duels list the owner asked for is exactly where that would show.
- **The format label.** Take it from the `MatchLog` if it carries one; if it does not, the adapter
  takes it as a constructor argument rather than inventing a string. Say in a comment which, and
  why.

Everything else maps straight across: `seats` and `outcome` are the same values.

The adapter must be a **thin** delegation. No SQL, no transaction handling, no coin arithmetic —
`TASK-021006` owns the transaction and `CoinDeltas` owns the economy, and a second copy of either
would be the drift this ticket exists to close.

## Tests

Against a real database, via `PostgresTestSupport`.

| Name | Asserts |
| --- | --- |
| `recordingThroughThePortWritesTheSameRowsAsTheStore` | a `DuelResult` recorded through the sink produces the one `duel` row and two `duel_result` rows the store would have written directly — the two halves agree |
| `theWinnerGainsAndTheLoserLosesThroughThePort` | balances move by `+1`/`-1`, so the whole path from port to column is exercised, not just the adapter's shape |
| `aDrawRecordedThroughThePortWritesTwoZeroRows` | `ADR-0015` survives the adapter — a draw is still two rows of `0`, not zero rows |
| `theStoreIsUsableWhereTheSinkIsExpected` | a compile-level check that the adapter satisfies `DuelResultSink`: assign it to a `DuelResultSink` variable and call `record` through that type |
| `theDuelIdAndTimestampsComeFromTheInjectedSources` | with a fixed id source and a `MutableClock`, the stored row carries exactly those values — proving neither is taken from the wall clock |

The last one is what stops the clock creeping back in: a test that cannot pin the timestamp would
pass against `Instant.now()`.

## Done

Both `verify:` commands exit 0, `PostgresDuelResultStore` and its tests are unmodified, and a
`DuelResultSink` variable can hold the Postgres implementation.
