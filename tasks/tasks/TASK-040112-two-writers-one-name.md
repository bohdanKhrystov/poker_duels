---
schema: 2
id: TASK-040112
title: "Two writers, one name: the loser is refused and keeps its nothing"
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, concurrency, identity]
depends_on: [TASK-040111]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileWritesConcurrencyTest' -PrequireDocker=true
---

## Goal

When two players send the same name at the same instant, one takes it and the other is told `409` —
and the loser's profile is left exactly as it was, free to try another name immediately.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileWritesConcurrencyTest.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreConcurrencyTest.kt` | read — `runBlocking(Dispatchers.Default)`, the container setup, how concurrent work is launched and joined |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileWrites.kt` | read — the statement under test |
| `docs/adr/ADR-0029-a-display-name-is-unique-and-permanent.md` | read — §5's race paragraph, checked against `postgres:16-alpine` |

## Scope

- One test class, container-backed, launching the two writes on `Dispatchers.Default` so they are
  genuinely concurrent rather than interleaved by hand.
- The claim being tested is `ADR-0029` §5's: the second writer **blocks** on the first
  transaction's uncommitted index entry and takes the `23505` when it commits — so exactly one
  `NameSet` and exactly one `NameTaken` come back, whichever order they were launched in.
- **The loser burns nothing**: its `display_name` is still `NULL` afterwards, and a second call with
  a *different* name succeeds. That second call is what makes "burns nothing" an assertion rather
  than a sentence.
- Run the pair more than once — ten rounds with fresh players and a fresh name each round — so a
  pass is not one lucky scheduling.

## Out of scope

- The HTTP status codes — `TASK-040116`. This is the port's behaviour under contention.
- Any change to `PostgresProfileWrites`. If this test fails, the ticket to fix it is a new one.

## Tests

`PostgresProfileWritesConcurrencyTest`

| Test | Proves |
| --- | --- |
| `twoPlayersSendingOneNameProduceOneWinner` | across ten rounds, exactly one `NameSet` and one `NameTaken` per round — asserted as counts over the results, not as "at least one succeeded" |
| `theLoserIsStillUnnamed` | after the round, the refused player's `display_name` is `NULL` |
| `theLoserCanTakeAnotherNameImmediately` | that same player then sets a different name and gets `NameSet` |
| `theWinnersNameIsTheOneStored` | the row holds the string the winning call passed, and only one `player` row holds it |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] `twoPlayersSendingOneNameProduceOneWinner` runs at least ten rounds and asserts the counts of
      each answer per round
- [ ] The two writes are launched concurrently on `Dispatchers.Default`, not serialised by the test
- [ ] `theLoserCanTakeAnotherNameImmediately` uses a name no other player in the test holds
- [ ] No test in this file sleeps to create the race
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
