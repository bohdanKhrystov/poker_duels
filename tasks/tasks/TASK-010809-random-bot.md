---
schema: 2
id: TASK-010809
title: Bot, and a RandomBot that picks uniformly among legal actions
type: task
status: backlog
parent: STORY-0108
module: poker-ai
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [simulation, bot]
depends_on: [TASK-010808]
verify:
  - ./gradlew :poker-ai:test --tests '*RandomBotTest'
  - ./gradlew :poker-ai:check
---

## Goal

A bot is a pure function from a decision point to an action, and the first one is deliberately
stupid: uniform over whatever is legal, which is exactly what reaches states a sensible player
never would.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/main/kotlin/duels/poker/ai/Bot.kt` | create |
| `poker-ai/src/main/kotlin/duels/poker/ai/RandomBot.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/RandomBotTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/LegalActions.kt`,
`poker-engine/src/test/kotlin/duels/poker/engine/game/RandomHandPlayer.kt` — its private
`pickAction` is the behaviour to mirror.

## Scope

- `Bot.kt`, one public interface with one nested type, so the file holds a single top-level
  declaration and ktlint's filename rule is satisfied:

  ```kotlin
  public interface Bot {
      public fun choose(state: GameState, legal: LegalActions, rng: Rng): Choice

      public data class Choice(val action: PlayerAction, val rng: Rng)
  }
  ```

  Returning the next `Rng` alongside the action is what keeps a simulated duel reproducible: the
  bot never holds mutable state and never reaches for a platform random source.
- `RandomBot.kt`, `public object RandomBot : Bot`:
  - draw uniformly over `legal.allowed.sorted()` — sorted, because iteration order of a `Set` is
    not a contract and determinism is;
  - `BET` takes `legal.minBetTo` or `legal.allInTo`, `RAISE` takes `legal.minRaiseTo` or
    `legal.allInTo`, each chosen by a second uniform draw;
  - `FOLD`, `CHECK`, `CALL`, `ALL_IN` carry no amount;
  - the returned `Choice.rng` is the generator left after every draw the decision consumed.
- `state` is unused by `RandomBot` and stays in the interface: a bot that plays is the point of
  EPIC-09 and it needs the board and the stacks.
- KDoc on both public types.

## Out of scope

- Playing a whole hand or duel with a bot, and the simulation runner — `TASK-010812`, blocked on
  `STORY-0107`.
- Computing `LegalActions`: the caller passes them in, already computed by the engine.
- Any bot that plays well, and any notion of a strategy or of equity — EPIC-09.

## Tests

`RandomBotTest`, JUnit 5, package `duels.poker.ai`. Real decision points come from the engine:
`val state = startHand(1, 0, listOf(10_000, 10_000), 50, 100, SplitMix64Rng(7L)).newState` with
`val legal = legalActions(state)`; the `BET` case is a hand-built
`LegalActions(seat = 0, allowed = setOf(ActionType.FOLD, ActionType.CHECK, ActionType.BET),
minBetTo = 100, allInTo = 10_000)`.

| Test | Proves |
| --- | --- |
| `choosesOnlyActionsTheLegalSetAllows` | over 200 successive draws from one seed, every `Choice.action.type` is in `legal.allowed` |
| `theSameStateAndRngAlwaysChooseTheSameAction` | two calls with the same arguments return equal `Choice` values |
| `advancesTheRngItWasGiven` | `choose(...).rng != rng` for the seed above |
| `betsEitherTheMinimumOrAllIn` | over 200 draws on the hand-built set, every `Bet.to` is `100` or `10_000`, and both occur |
| `raisesEitherTheMinimumOrAllIn` | over 200 draws on the preflop `legal`, every `Raise.to` is `legal.minRaiseTo` or `legal.allInTo`, and both occur |
| `everyAllowedActionTypeIsReachable` | over 200 draws on the preflop `legal`, the chosen types cover `legal.allowed` exactly |

## Acceptance criteria

- [ ] `RandomBotTest.choosesOnlyActionsTheLegalSetAllows` passes
- [ ] `RandomBotTest.theSameStateAndRngAlwaysChooseTheSameAction` passes
- [ ] `RandomBotTest.advancesTheRngItWasGiven` passes
- [ ] `RandomBotTest.betsEitherTheMinimumOrAllIn` passes
- [ ] `RandomBotTest.raisesEitherTheMinimumOrAllIn` passes
- [ ] `RandomBotTest.everyAllowedActionTypeIsReachable` passes
- [ ] Neither production file references `kotlin.random.Random`
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
