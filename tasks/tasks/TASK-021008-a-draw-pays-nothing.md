---
schema: 2
id: TASK-021008
title: Prove a drawn duel is recorded and pays nobody
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, persistence, coins]
depends_on: [TASK-021007]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A drawn duel is stored like any other — one duel row, one result row per seat — with both coin
deltas `0` and neither balance moved, per `ADR-0014`.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |

Read, do not modify:
`docs/adr/ADR-0014-duel-coin-economy.md` (a draw pays nothing, and why draws are rare),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/DuelOutcome.kt` (`isDraw`, and that only a
`FixedHands` duel can finish level).

## Scope

- Add exactly two tests to the existing `PostgresDuelResultStoreTest`, using its existing helpers.
  No existing test, helper or assertion changes.
- The drawn fixture is `finishedDuel(winner = null)` with its format copied to the fixed-hands
  label — `.copy(format = formatLabel(DuelFormat.DEFAULT.copy(endCondition = EndCondition.FixedHands(20))))`
  — with a `why` comment: `Freezeout` cannot draw, so a drawn duel that claimed to be a freezeout
  would be a fixture asserting something the engine cannot produce. This is the case most likely to
  go untested, which is exactly why it has its own ticket.
- **A draw writes two rows of zero, not zero rows.** The story's acceptance criteria are one duel
  row and one result row per participant for every finished duel, and `STORY-0211` lists a player's
  duels by joining `duel_result` on their player id — a drawn duel with no rows would be a duel
  that happened to nobody. The `-- draw absent (no row)` remark in `V1__initial_schema.sql` is
  loose wording about the *balance*; the migration is merged and is never edited.
- The second test records a decided duel first, then the drawn one, so "neither balance moved" is
  asserted against balances that are demonstrably movable rather than against two zeroes.

## Out of scope

- Any change to `PostgresDuelResultStore.kt` or to `CoinDeltas.kt`. `coinDeltas` already returns
  `CoinDeltas(0, 0)` for a draw, and `TASK-021001` pinned it.
- A new migration, or any edit to `V1__initial_schema.sql`.
- The negative-balance cases — `TASK-021010`.

## Tests

`PostgresDuelResultStoreTest`, the existing class, two tests added.

| Test | Proves |
| --- | --- |
| `aDrawnDuelWritesOneDuelRowAndTwoResultRowsOfZero` | after recording the drawn fixture, `duelRowCount() == 1`, `duelResultRowCount() == 2`, and `resultDeltaOf(id, alice.id)` and `resultDeltaOf(id, bob.id)` are both `0` |
| `aDrawnDuelMovesNeitherBalance` | after recording `finishedDuel(winner = 0)` and then the drawn fixture, `coinBalanceOf(alice.id) == 1` and `coinBalanceOf(bob.id) == -1` — the draw changed neither |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.aDrawnDuelWritesOneDuelRowAndTwoResultRowsOfZero` passes
- [ ] `PostgresDuelResultStoreTest.aDrawnDuelMovesNeitherBalance` passes
- [ ] The five tests already in `PostgresDuelResultStoreTest` still pass, with their assertions
      unchanged — this ticket only adds test methods
- [ ] No file other than `PostgresDuelResultStoreTest.kt` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
