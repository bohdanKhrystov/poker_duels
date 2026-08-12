---
schema: 2
id: TASK-021010
title: Prove ten losses read back as minus ten, and that nothing floors a balance
type: task
status: backlog
parent: STORY-0210
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, persistence, coins]
depends_on: [TASK-021009]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresDuelResultStoreTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*NoBalanceIsFlooredTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A losing streak reads back as the negative number it is — ten losses are `−10` — and no source
file in the write path contains a clamp that could have hidden it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresDuelResultStoreTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/NoBalanceIsFlooredTest.kt` | create |

Read, do not modify:
`docs/adr/ADR-0014-duel-coin-economy.md` (flooring makes a losing streak indistinguishable from
never having played),
`poker-server/src/test/kotlin/duels/poker/server/db/DevDatabaseComposeTest.kt` (the house idiom for
a test that reads a file: the test working directory is the `poker-server` module directory).

## Scope

- Add exactly one test to `PostgresDuelResultStoreTest`, using its existing helpers: record ten
  duels in a loop, each `finishedDuel(winner = 0)` with the builder's fresh default id, then assert
  `coinBalanceOf(bob.id) == -10`, `coinBalanceOf(alice.id) == 10`, `duelRowCount() == 10` and
  `duelResultRowCount() == 20`. No existing test or helper changes.
- New `NoBalanceIsFlooredTest`: plain text assertions over two main source files, no database and
  no Docker, in the style of `DevDatabaseComposeTest`. The files are
  `File("src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt")` and
  `File("src/main/kotlin/duels/poker/server/duel/CoinAward.kt")`; assert each exists and is
  non-empty first, so a rename fails loudly instead of passing vacuously.
- Forbidden substrings in both files: `coerceAtLeast`, `coerceIn`, `maxOf`, `Math.max`,
  `absoluteValue`, `UInt`. Each assertion message names the file and the token, and cites
  `ADR-0014`: a balance is `wins − losses` and may be negative.
- One positive assertion too: `PostgresDuelResultStore.kt` contains
  `coin_balance = coin_balance +`. It pins the atomic SQL increment structurally, so a later
  read-modify-write rewrite fails a test rather than a code review.

## Out of scope

- Any change to main sources. Both files already satisfy these assertions when `TASK-021006` and
  `TASK-021001` merged; this ticket stops them regressing.
- The read path's rendering of a negative balance — `STORY-0211`.
- A test that scans every file in the module. Two named files is the claim being made; a
  repository-wide scan is a different, broader ticket nobody has asked for.

## Tests

`PostgresDuelResultStoreTest`, the existing class, one test added.

| Test | Proves |
| --- | --- |
| `tenConsecutiveLossesLeaveTheLoserAtMinusTen` | after ten recorded duels won by seat 0, `coinBalanceOf(bob.id) == -10`, `coinBalanceOf(alice.id) == 10`, and the rows are `10` duels and `20` results |

`NoBalanceIsFlooredTest`, JUnit 5, package `duels.poker.server.db`. No Docker.

| Test | Proves |
| --- | --- |
| `theCoinRuleClampsNothing` | `CoinAward.kt` exists, is non-empty, and contains none of the forbidden tokens |
| `theStoreClampsNothing` | `PostgresDuelResultStore.kt` exists, is non-empty, and contains none of the forbidden tokens |
| `theStoreMovesBalancesWithAnSqlIncrement` | `PostgresDuelResultStore.kt` contains `coin_balance = coin_balance +` |

## Acceptance criteria

- [ ] `PostgresDuelResultStoreTest.tenConsecutiveLossesLeaveTheLoserAtMinusTen` passes
- [ ] `NoBalanceIsFlooredTest.theCoinRuleClampsNothing` passes
- [ ] `NoBalanceIsFlooredTest.theStoreClampsNothing` passes
- [ ] `NoBalanceIsFlooredTest.theStoreMovesBalancesWithAnSqlIncrement` passes
- [ ] The nine tests already in `PostgresDuelResultStoreTest` still pass, with their assertions
      unchanged — this ticket only adds a test method
- [ ] Nothing under `poker-server/src/main/` changes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
