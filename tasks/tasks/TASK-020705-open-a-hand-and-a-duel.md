---
schema: 2
id: TASK-020705
title: Open a hand from a seed, and open the duel's first one
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, duel, engine-integration]
depends_on: [TASK-020704]
verify:
  - ./gradlew :poker-server:test --tests '*DuelStartTest'
  - ./gradlew :poker-server:check
---

## Goal

`startDuel(format, buttonSeat, seed)` deals the first hand of a duel through `startNextHand`,
records the seed in that hand's `HandLog`, and returns both seats' opening frames — with every
hand parameter read off `MatchState`, never computed here.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/DuelStart.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/duel/DuelStartTest.kt` | create |

Read, do not modify:
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchProgression.kt` (`startNextHand`
returns an `EngineResult`, so its `events` are the hand's opening events),
`poker-engine/src/main/kotlin/duels/poker/engine/duel/MatchState.kt` (`nextHandNumber`, `blinds`,
`stacks`, `buttonSeat`), `poker-server/src/main/kotlin/duels/poker/server/duel/DuelRunner.kt`,
`poker-server/src/main/kotlin/duels/poker/server/duel/DuelTurn.kt` (`framesFor`).

## Scope

- Package `duels.poker.server.duel`. Two declarations, KDoc included:

  ```kotlin
  internal fun openHand(match: MatchState, seed: Long): LiveHand

  public fun startDuel(format: DuelFormat, buttonSeat: Int, seed: Long): DuelStep
  ```

- `openHand` is exactly:
  - `val result = startNextHand(match, SplitMix64Rng(seed))`
  - `LiveHand(result.newState, HandLog(seed = seed, handNumber = match.nextHandNumber, buttonSeat =
    match.buttonSeat, stacks = match.stacks, smallBlind = match.blinds.smallBlind, bigBlind =
    match.blinds.bigBlind, actions = emptyList(), events = result.events))`

  Every one of those six parameters is read off `match`. Computing a hand number, alternating a
  button or picking a blind level in this file is a review finding: `MatchState` already derives
  all of them, and a second derivation is a second source of truth.
- `startDuel` is `MatchState.start(format, buttonSeat)`, then `openHand(match, seed)`, then a
  `DuelRunner(match, hand, MatchLog(format, buttonSeat, emptyList(), emptyList()), outcome = null)`,
  with `outbound = framesFor(hand.state, hand.log.events, hand.log.events)` — for an opening hand,
  the new events and the hand's events are the same list.
- `startDuel` ends with `check(hand.state.seatToAct != null)`. A duel's *first* hand always has a
  seat to act — `DuelFormat` requires `startingStack >= bigBlind`, so the button, posting the small
  blind, always has chips left — and the check states that rather than assuming it. A *later* hand
  can deal itself out when a seat is short; `TASK-020707` owns that case.
- The seed is a parameter, never drawn here: `HandSeedSource` is the caller's business
  (`TASK-020714`), which is what keeps this function pure and its tests reproducible.
- `SplitMix64Rng` is the only random source named in this file. No `kotlin.random.Random`, no
  `SecureRandom`, no clock.

## Out of scope

- Opening the *next* hand once one finishes — `TASK-020707` calls `openHand` for that; this ticket
  makes it `internal` precisely so that ticket can.
- Rematch: starting a second duel in the same room is `TASK-020714`; it calls `startDuel` again.
- Applying any action — `TASK-020708`.

## Tests

`DuelStartTest`, JUnit 5, package `duels.poker.server.duel`. Use `DuelFormat.DEFAULT` and
`startDuel(DuelFormat.DEFAULT, buttonSeat = 0, seed = 7L)` unless a test says otherwise.

| Test | Proves |
| --- | --- |
| `theFirstHandIsHandOneWithTheOpeningButton` | `runner.hand!!.log.handNumber == 1`, `log.buttonSeat == 0` and `runner.match.handsPlayed == 0` |
| `theHandLogRecordsTheSeedItWasDealtFrom` | `runner.hand!!.log.seed == 7L` |
| `theHandLogTakesItsStacksAndBlindsFromTheMatch` | `log.stacks == listOf(10_000, 10_000)`, `log.smallBlind == 50` and `log.bigBlind == 100`, matching `MatchState.start(DuelFormat.DEFAULT, 0)` |
| `theSameSeedOpensTheSameHand` | `startDuel(DuelFormat.DEFAULT, 0, 7L)` twice gives equal `hand.log.events` and equal `outbound` |
| `adifferentSeedOpensAdifferentHand` | seeds `7L` and `8L` give different `hand.log.events` |
| `bothSeatsAreToldTheHandHasStarted` | `outbound` holds one `Snapshot` addressed to each seat, and each `Events` frame contains a `HandStarted` |
| `exactlyOneSeatIsPromptedToAct` | `outbound` holds exactly one `YourTurn`, addressed to `runner.hand!!.state.seatToAct` |
| `theOpeningHandAlwaysHasASeatToAct` | for the tightest legal format — `DuelFormat(startingStack = 100, blinds = BlindSchedule(listOf(BlindLevel(50, 100)), 10), EndCondition.Freezeout)` — `runner.hand!!.state.seatToAct` is non-null |
| `theMatchLogStartsEmptyAndUnfinished` | `runner.log.hands` is empty, `runner.log.events` is empty and `runner.outcome` is null |

## Acceptance criteria

- [ ] `DuelStartTest.theFirstHandIsHandOneWithTheOpeningButton` passes
- [ ] `DuelStartTest.theHandLogRecordsTheSeedItWasDealtFrom` passes
- [ ] `DuelStartTest.theHandLogTakesItsStacksAndBlindsFromTheMatch` passes
- [ ] `DuelStartTest.theSameSeedOpensTheSameHand` passes
- [ ] `DuelStartTest.adifferentSeedOpensAdifferentHand` passes
- [ ] `DuelStartTest.bothSeatsAreToldTheHandHasStarted` passes
- [ ] `DuelStartTest.exactlyOneSeatIsPromptedToAct` passes
- [ ] `DuelStartTest.theOpeningHandAlwaysHasASeatToAct` passes
- [ ] `DuelStartTest.theMatchLogStartsEmptyAndUnfinished` passes
- [ ] `DuelStart.kt` contains no `+`, `-` or `%` applied to a hand number, a button seat or a blind
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
