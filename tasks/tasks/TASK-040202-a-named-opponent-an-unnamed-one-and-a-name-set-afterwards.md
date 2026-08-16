---
schema: 2
id: TASK-040202
title: A named opponent, an unnamed one, and a name set after the duel
type: task
status: done
parent: STORY-0402
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, db, read-path, tests, identity]
depends_on: [TASK-040201]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.aDuelAgainstANamedOpponentReadsBackThatName' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.anUnnamedOpponentReadsBackNullEvenWhenTheReaderIsNamed' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest.aNameSetAfterTheDuelFinishedAppearsOnItsLine' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

Three tests against the real database prove the joined name is the **opponent's**, that it varies
between a string and `null` within one run, and that it is read at request time rather than frozen
at duel time.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | read — the join `TASK-040201` added, and the `opponent_display_name` label |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the permanence trigger, which decides what a fixture may and may not update |
| `docs/adr/ADR-0021-a-profile-gains-a-display-name.md` | read — "current name at read time", the model the third test pins |

## Scope

- Three tests added to `PostgresProfileReadsTest`. **Nothing existing moves** — no assertion, no test
  name, no helper signature changes, and the class's existing tests must pass untouched.
- The fixture that writes a name is the private `setPlayerDisplayName` helper already in the file.
  It writes straight into the `player` row; no write path is invoked and none is invented here.
- **Never rename an already-named row.** `V3` installs `player_display_name_permanent`, a
  `BEFORE UPDATE` trigger that raises with SQLSTATE `23001` when a non-null name changes. Every
  fixture write in this ticket goes from `NULL` to a name, exactly once per row. If a test in this
  ticket ever asserts a refusal — none should — it asserts on `SQLException.sqlState` and the
  constraint or trigger name, never on message text.
- Names used: `Ingrid` and `Torvald`. Both are absent from every other test in the file, and neither
  is a device id string the class already uses (`alice`, `bob`, `carol`, `dave`), so a passing
  assertion cannot be an accident of a shared literal.
- Match the file's existing test style: `fun name() = runBlocking { ... }`. **Do not write an
  explicit `: Unit`** — ktlint's `no-unit-return` fails the build on it. And make sure the block's
  final expression is an assertion: a test body whose last expression produces a value is not a
  valid JUnit test and is silently never run.

## Out of scope

- The one-statement and one-row-per-duel guarantees — `TASK-040203`, in this same file, immediately
  after.
- Anything about the JSON encoding of the field — `TASK-040204`.
- Setting a name over `PUT /api/me/name`; `STORY-0401` shipped that and it is not what this ticket
  is about.

## Tests

`PostgresProfileReadsTest`

| Test | Proves |
| --- | --- |
| `aDuelAgainstANamedOpponentReadsBackThatName` | `bob`'s row is given `Ingrid`, a duel is recorded, and alice's single line has `opponentDisplayName == "Ingrid"` |
| `anUnnamedOpponentReadsBackNullEvenWhenTheReaderIsNamed` | `alice` is given `Ingrid` and `bob` is left unnamed; **in one test**, alice's line reads `null` and bob's line reads `"Ingrid"` — so the field is shown to vary, and shown to carry the *opponent's* row rather than the reader's |
| `aNameSetAfterTheDuelFinishedAppearsOnItsLine` | the duel is recorded and read once with `opponentDisplayName == null`; `bob` is then given `Torvald`; **the same duel** is read again and now reads `"Torvald"`, with its `duelId` unchanged between the two reads |

The second test is the load-bearing one. A `null` asserted on its own cannot be told apart from a
constant, and joining the reader's own `player` row instead of the opponent's would pass a test that
only ever names one side. Asserting both directions in one run refuses both mistakes.

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.aDuelAgainstANamedOpponentReadsBackThatName` passes
- [ ] `PostgresProfileReadsTest.anUnnamedOpponentReadsBackNullEvenWhenTheReaderIsNamed` passes, and
      asserts **both** the `null` and the `"Ingrid"` side inside that one test method
- [ ] `PostgresProfileReadsTest.aNameSetAfterTheDuelFinishedAppearsOnItsLine` passes, and reads the
      list **twice** — once before the name is set and once after — asserting a different
      `opponentDisplayName` each time for the same `duelId`
- [ ] No fixture in the diff updates a `display_name` that is already non-null
- [ ] Every test already in `PostgresProfileReadsTest` passes with its assertions unchanged
- [ ] No test method in the diff declares `: Unit`, and every one ends in an assertion
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
