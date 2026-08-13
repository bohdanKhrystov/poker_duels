---
schema: 2
id: TASK-021102
title: Read won, lost or drew off a stored coin delta
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, http, coins]
depends_on: [TASK-021101]
verify:
  - ./gradlew :poker-server:test --tests '*DuelOutcomesTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`outcomeOf(coinDelta)` turns the `duel_result` row a player already has into `WON`, `LOST` or
`DREW`, so the server states the outcome and the client never derives it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/DuelOutcomes.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/DuelOutcomesTest.kt` | create |

Read, do not modify:
`poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` (`DuelOutcomeLabel`),
`docs/adr/ADR-0015-a-draw-writes-two-result-rows.md`,
`docs/adr/ADR-0014-duel-coin-economy.md`.

## Scope

- One file in `duels.poker.server.protocol.http`, one public function with KDoc:

  ```kotlin
  public fun outcomeOf(coinDelta: Int): DuelOutcomeLabel =
      when {
          coinDelta > 0 -> DuelOutcomeLabel.WON
          coinDelta < 0 -> DuelOutcomeLabel.LOST
          else -> DuelOutcomeLabel.DREW
      }
  ```

- **The sign, not the magnitude.** `ADR-0014` anticipates being superseded by a floating or
  opponent-weighted award; a winner still gains and a loser still loses under any such rule, so a
  function that reads the sign survives the change and one that compares against `1` does not.
- **A zero delta is a draw, never a missing row.** `ADR-0015`: every participant of every completed
  duel has exactly one `duel_result` row, and a drawn duel writes two rows of `0`. Put that in the
  KDoc, citing the ADR — the `-- draw absent (no row)` comment in `V1__initial_schema.sql` says the
  opposite and is wrong.
- No database, no Ktor, no coroutines, no engine types. It is a pure function over an `Int`.

## Out of scope

- Reading the delta out of PostgreSQL — `TASK-021106`.
- Any change to `ProfileDtos.kt` or to `DuelOutcomeLabel` itself.
- Deciding the outcome from the engine's `DuelOutcome`: the read path answers from stored rows
  only, and the engine's value is long gone by the time anyone asks.

## Tests

`DuelOutcomesTest`, JUnit 5, package `duels.poker.server.protocol.http`. Plain tests, no
coroutines and no database.

| Test | Proves |
| --- | --- |
| `aPositiveDeltaIsAWin` | `outcomeOf(1) == DuelOutcomeLabel.WON` |
| `aNegativeDeltaIsALoss` | `outcomeOf(-1) == DuelOutcomeLabel.LOST` |
| `aZeroDeltaIsADraw` | `outcomeOf(0) == DuelOutcomeLabel.DREW` |
| `theLabelFollowsTheSignNotTheSizeOfTheAward` | `outcomeOf(7) == WON` and `outcomeOf(-7) == LOST`, so a future award larger than one coin still reads correctly |

## Acceptance criteria

- [ ] `DuelOutcomesTest.aPositiveDeltaIsAWin` passes
- [ ] `DuelOutcomesTest.aNegativeDeltaIsALoss` passes
- [ ] `DuelOutcomesTest.aZeroDeltaIsADraw` passes
- [ ] `DuelOutcomesTest.theLabelFollowsTheSignNotTheSizeOfTheAward` passes
- [ ] `DuelOutcomes.kt` compares only against `0` — it contains no `== 1`, `== -1` or other coin
      literal
- [ ] `DuelOutcomes.kt` names no `DataSource`, `Connection`, SQL string, Ktor type or coroutine
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
