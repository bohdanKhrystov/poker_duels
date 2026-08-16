---
schema: 2
id: TASK-040203
title: Three duels, three opponents, one prepared statement
type: task
status: ready
parent: STORY-0402
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, read-path, tests, performance]
depends_on: [TASK-040202]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.everyDuelReturnsOneRowWhicheverOpponentsAreNamed' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.aListOfThreeDuelsPreparesExactlyOneStatement' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

The new join is proven to be free: three duels against three different opponents come back as three
rows, each labelled with its own opponent, from **one** prepared statement.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — `recentDuelsOf` takes one connection and prepares one statement; that is what is being pinned |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresTestSupport.kt` | read — `freshDatabase()` returns a plain `PGSimpleDataSource`, so wrapping it is a delegation and not a pool fight |
| `docs/adr/ADR-0021-a-profile-gains-a-display-name.md` | read — "still one query, no N+1, per `STORY-0211`'s rule" |

## Scope

- Widen the file's private `finishedDuel` helper with one parameter, `opponent: Player = bob`, and
  build `seats = listOf(alice.id, opponent.id)`. The default keeps every existing call site
  byte-identical, so **no existing test changes**.
- Add a private helper that records the shared fixture once and returns what each line should say —
  three duels for `alice`, against three distinct opponents, with distinct `finishedAt` values:
  `bob` named `Halvard`, `carol` named `Sigrid`, `dave` left unnamed. It answers a
  `Map<String, String?>` of duel id to expected `opponentDisplayName`. Both tests below use it.
  Names go into the row from `NULL` exactly once (the `V3` permanence trigger refuses any rewrite of
  a non-null name with SQLSTATE `23001`).
- Add a private `CountingDataSource` to the same file that counts the statements prepared on the
  connections it hands out:

  ```kotlin
  private class CountingDataSource(private val delegate: DataSource) : DataSource by delegate {
      var statementsPrepared: Int = 0
          private set

      override fun getConnection(): Connection {
          val connection = delegate.connection
          return Proxy.newProxyInstance(
              Connection::class.java.classLoader,
              arrayOf(Connection::class.java),
              InvocationHandler { _, method, args ->
                  if (method.name == "prepareStatement") statementsPrepared++
                  try {
                      method.invoke(connection, *(args ?: emptyArray()))
                  } catch (failure: InvocationTargetException) {
                      throw failure.targetException
                  }
              },
          ) as Connection
      }
  }
  ```

  **Unwrapping `InvocationTargetException` is not optional.** Without it a genuine `SQLException`
  from the query arrives as a reflection wrapper and the test reports the wrong failure, which is
  worse than no test.
- The counting test builds its own `PostgresProfileReads(countingDataSource)`; the class's
  `profileReads` field and its `@BeforeEach` stay exactly as they are.
- Same style rules as the file: `fun name() = runBlocking { ... }`, **no explicit `: Unit`**
  (ktlint's `no-unit-return` fails the build), and the block's last expression is an assertion —
  a test body that produces a value is not run.

## Out of scope

- Asserting the SQL text. `RECENT_DUELS_SQL` is private and stays private; counting prepared
  statements is the behavioural version of the same claim and survives a rewrite of the query.
- Timing, `EXPLAIN`, indexes, or any performance number. This is a shape assertion, not a benchmark.
- Paging over the whole record — `STORY-0408`, which is the reason this guard is worth having before
  the query grows.

## Tests

`PostgresProfileReadsTest`

| Test | Proves |
| --- | --- |
| `everyDuelReturnsOneRowWhicheverOpponentsAreNamed` | the three duels come back as exactly three rows, each `duelId` appearing exactly once, and the map of `duelId` to `opponentDisplayName` equals the helper's expectation — two distinct names and one `null`, each on the right line |
| `aListOfThreeDuelsPreparesExactlyOneStatement` | reading the same three duels through `CountingDataSource` returns three rows and leaves `statementsPrepared == 1` — the join added a column, not a query per row |

The first test enumerates rather than sampling: it asserts the whole map, so a query that fanned out,
dropped the unnamed opponent's duel, or put every line's name on every row fails on the comparison
instead of slipping past a single-row spot check.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.everyDuelReturnsOneRowWhicheverOpponentsAreNamed` passes and asserts
      an equality against a map of all three duel ids, not a `single()` or a `first()`
- [ ] `PostgresProfileReadsTest.aListOfThreeDuelsPreparesExactlyOneStatement` passes and asserts
      `statementsPrepared == 1` **and** that three rows came back
- [ ] The three duels are against three distinct opponents, of whom exactly one has no name
- [ ] `finishedDuel`'s new parameter has a default, and no existing call site of it changes
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] The invocation handler rethrows `InvocationTargetException.targetException`
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
