---
schema: 2
id: TASK-010522
title: Give the contract fixtures a deck consistent with their board
type: task
status: ready
parent: STORY-0105
module: poker-engine
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [engine, test, bug]
depends_on: [TASK-010516]
verify:
  - ./gradlew :poker-engine:test --tests '*DefaultPokerEngineContractTest'
  - ./gradlew :poker-engine:check
---

## Goal

Two fixtures in `DefaultPokerEngineContractTest` describe a state that cannot exist: a board of
three cards dealt from a deck that still contains all fifty-two. The moment anything deals from
those states, the same card comes out twice.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/DefaultPokerEngineContractTest.kt` | modify |

Read `Deck.kt` and `Board.kt`. Modify neither.

## Scope

`flopPosition` and `allInPosition` set `board = Board(cards("2c 3c 4c"))` while leaving
`deck = Deck.full()`. `Deck.full()` starts `2c 3c 4c 5c …`, so dealing the turn from either
fixture deals `2c` a second time and `StreetDealt` rejects the board as holding a duplicate.

Take the board cards *out of the deck the same way a real hand would*: deal them, and keep what
the deal leaves behind. Whatever shape that takes, the invariant to satisfy is the one a real
state always has — **no card appears both on the board and in the deck**.

Do not change what the fixtures are testing: same seats, same stacks, same `betToMatch`, same
street. Only the deck/board relationship changes.

## Out of scope

- The contract suite itself (`PokerEngineContract.kt`) — untouched.
- `DefaultPokerEngine`, or any production file. This is a test-fixture bug.
- The four inherited contract tests — they keep passing, unmodified.

## Tests

The four inherited contract tests are the test. Add one assertion of your own:

| Test | Proves |
| --- | --- |
| `everyFixtureHasADeckThatExcludesItsBoard` | for every state in `cases()`, no card on `board` also appears in `deck`; the failure message names the state and the duplicated card |

## Acceptance criteria

- [ ] `everyFixtureHasADeckThatExcludesItsBoard` passes
- [ ] the four inherited contract tests still pass, unmodified
- [ ] no file under `src/main/` is touched
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
