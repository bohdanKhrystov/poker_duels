---
schema: 2
id: TASK-021105
title: Prove the balance read back is the one the duels wrote, minus one included
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, coins]
depends_on: [TASK-021104, TASK-021006]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A balance read back through `profileOf` equals the sum of that player's stored coin deltas,
including the `−1` a player whose only duel was a loss carries.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` (`record`),
`poker-server/src/main/kotlin/duels/poker/server/duel/FinishedDuel.kt` (and `formatLabel`),
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` (the
`finishedDuel(...)` fixture idiom to copy),
`docs/adr/ADR-0014-duel-coin-economy.md`.

## Scope

- Add to the existing `PostgresProfileReadsTest`: a `PostgresDuelResultStore` built over the same
  `dataSource` in `@BeforeEach`, a private `finishedDuel(winner: Int?, id: UUID = UUID.randomUUID(),
  finishedAt: Instant = …)` builder filling `format` with `formatLabel(DuelFormat.DEFAULT)`, fixed
  `Instant`s and `seats = listOf(alice.id, bob.id)`, and the two tests below.
- **No assertion already in the file changes**, and no helper it already has is rewritten. This
  ticket adds a collaborator, a builder and two test methods.
- Balances are asserted through `profileOf`, not by reading `player.coin_balance` with JDBC: the
  claim under test is that the *read path* reports the stored number, and a test that queried the
  column itself would prove the column, not the path.
- The loss case is asserted as exactly `-1`. Not "less than zero", not "not null": `ADR-0014` says
  a new profile's first loss puts it at `−1`, and that is the number a player must see.

## Out of scope

- Any change to `PostgresProfileReads.kt`, `ProfileReads.kt` or the DTOs — `profileOf` already
  returns whatever `coin_balance` holds, and this ticket only pins what that is after real writes.
- The recent-duels list — `TASK-021106`.
- Ten losses in a row and the no-floor property: `TASK-021010` already pins that on the write path.

## Tests

`PostgresProfileReadsTest`, the existing class, two tests added.

| Test | Proves |
| --- | --- |
| `theWinnersBalanceReadsBackAsOne` | after `record(finishedDuel(winner = 0))`, `profileOf(DeviceId("alice"))!!.coinBalance == 1` |
| `aPlayerWhoseOnlyDuelWasALossReadsBackMinusOne` | after that same record, `profileOf(DeviceId("bob"))!!.coinBalance == -1` — negative, unclamped, and not an absent profile |

## Acceptance criteria

- [ ] `PostgresProfileReadsTest.theWinnersBalanceReadsBackAsOne` passes
- [ ] `PostgresProfileReadsTest.aPlayerWhoseOnlyDuelWasALossReadsBackMinusOne` passes
- [ ] The four tests already in `PostgresProfileReadsTest` pass with their assertions unchanged —
      this ticket only adds a fixture and two test methods
- [ ] Neither new test reads `player.coin_balance` with JDBC; both assert through `profileOf`
- [ ] No file other than `PostgresProfileReadsTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
