---
schema: 2
id: TASK-010614
title: A folded hand appears in no event, over a thousand hands
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [engine, tests, security]
depends_on: [TASK-010613]
verify:
  - ./gradlew :poker-engine:test --tests '*CardSecrecyTest'
  - ./gradlew :poker-engine:check
---

## Goal

The story's security boundary is asserted rather than assumed: across a thousand generated hands,
no event anywhere in a log carries a card belonging to a seat that folded.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/CardSecrecyTest.kt` | create |

Read `RandomHandPlayer.kt`, `GameEvent.kt`, `DealerEvents.kt`, `BettingInvariantTest.kt`. Modify
none of them.

## Scope

- A local `fun cardsIn(event: GameEvent): List<Card>` with an exhaustive `when` — no `else`
  branch, so a future event carrying cards cannot be added without this test failing to compile.
  `HoleCardsDealt`, `StreetDealt` and `HandRevealed` carry cards; everything else carries none.
- The rule under test: a seat's own `HoleCardsDealt` is addressed to that seat and is the one
  legitimate carrier of its cards — it is what the projection layer filters per recipient. Every
  **other** event in the log, including the opponent's `HoleCardsDealt`, must be free of a folded
  seat's two cards.
- Hands are played with `playRandomHand(seed)` over `1L..1000L`, `@Timeout(30)`, each failure
  message naming its seed and the offending event.

## Out of scope

- Mucked hands at showdown. Reveals are not emitted at all yet — see `DEC-004` and
  `TASK-010615`, which extends this file with the muck case once the decision lands.
- Filtering for a recipient. That is the projection layer's job (`EPIC-02`); this ticket asserts
  only what the engine puts in the log.

## Tests

`CardSecrecyTest`

| Test | Proves |
| --- | --- |
| `noEventCarriesAFoldersCards` | for every seed whose hand ended with a fold, no event other than that seat's own `HoleCardsDealt` contains either of its cards |
| `theBoardNeverShowsAFoldersCards` | the same hands' `StreetDealt` cards are disjoint from the folder's hole cards — the specific case a dealing bug would produce |
| `noHandIsRevealedAnywhereYet` | no `HandRevealed` event appears in any of the thousand logs, which is what makes the fold case above complete today |
| `theSampleContainsFolds` | more than 100 of the thousand hands ended with a fold, so the scan is not vacuous |

## Acceptance criteria

- [ ] `CardSecrecyTest.noEventCarriesAFoldersCards` passes
- [ ] `CardSecrecyTest.theBoardNeverShowsAFoldersCards` passes
- [ ] `CardSecrecyTest.noHandIsRevealedAnywhereYet` passes
- [ ] `CardSecrecyTest.theSampleContainsFolds` passes
- [ ] `cardsIn` is an exhaustive `when` over `GameEvent` with no `else` branch
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
