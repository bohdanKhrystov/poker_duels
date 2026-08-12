---
schema: 2
id: TASK-010623
title: A showdown emits HandRevealed for the hands that are shown
type: task
status: done
parent: STORY-0106
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [engine, rules, security]
depends_on: [TASK-010617, TASK-010622]
verify:
  - ./gradlew :poker-engine:test --tests '*StreetAdvanceTest' --tests '*AllInRunOutTest' --tests '*CardSecrecyTest'
  - ./gradlew :poker-engine:check
---

## Goal

Every showdown puts the shown hands on the log: `ShowdownReached`, then one `HandRevealed` per
seat named by `revealOrder`, in that order, then the settlement events — and nothing for a mucked
hand.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/game/StreetProgression.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/StreetAdvanceTest.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/game/AllInRunOutTest.kt` | modify |

Read `Showdown.kt`, `DealerEvents.kt`, `StateProjection.kt` and
`docs/adr/ADR-0008-loser-mucks-at-showdown.md`. Modify none of them.

## Scope

- Only `reachShowdownAndSettle` changes. It is the single funnel for both ways a hand reaches a
  showdown — `endBettingRound`'s river branch and `runOutBoard` — so both get reveals from one
  edit, and `settleHand` is not touched at all.
- The new body, in order: emit `ShowdownReached`; compute `showdownWinners` on the state after it;
  for each seat from `revealOrder(state, winners)` emit `HandRevealed(current.eventCount, seat,
  current.seat(seat).holeCards)` and fold it in before building the next one, so sequences stay
  dense; then `settleHand(current, winners)` as today. The event list is
  `showdownEvent + reveals + settled.events`.
- Fold each reveal through `StateProjection.apply` rather than incrementing a counter by hand —
  the contract suite compares the engine's state against the fold of its own events, and a
  hand-rolled sequence would drift.
- `HandRevealed` carries the seat's real hole cards, which the state already holds; nothing new is
  dealt and the deck is not touched.

## Tests it invalidates, and exactly how

Both tests below pin an exact event list at a real showdown, so both move with this change. No
assertion is weakened: each keeps its existing checks and gains the reveal.

**`StreetAdvanceTest.theRiverShowdownPaysTheBetterHand`** — seat 0 wins with `Qh Jc`, so there is
one reveal:

- `assertEquals(5, result.events.size)` becomes `6`.
- Indices 0–2 (`PlayerChecked`, `BettingRoundEnded`, `ShowdownReached`) are unchanged.
- `result.events[3]` is now a `HandRevealed` with `seat == 0` and `cards == cards("Qh Jc")`.
- The `PotAwarded` assertions (`seat == 0`, `amount == 600`) move from index 3 to index 4.
- `result.events[4] is HandFinished` becomes `result.events[5] is HandFinished`.
- Add one assertion: seat 1's `Td 8c` appear in no event of `result.events` — the muck, in the one
  fixture whose losing hand is known by name.
- The stack, pot, street and `seatToAct` assertions at the end of the test are unchanged.

**`AllInRunOutTest.theRunOutDealsEachStreetInOrder`** — the seeded hand has a single winner (which
is why `7` is the count today):

- `assertEquals(7, dealerEvents.size)` becomes `8`.
- `dealerEvents[0..4]` (`BettingRoundEnded`, three `StreetDealt`, `ShowdownReached`) are unchanged.
- `dealerEvents[5]` is now a `HandRevealed`, and its `seat` equals the seat of the single
  `PotAwarded` in the list — the winner shows, the loser does not.
- `dealerEvents.subList(5, dealerEvents.size - 1)` becomes `subList(6, dealerEvents.size - 1)`,
  still `all { it is PotAwarded }`.
- `dealerEvents.last() is HandFinished` is unchanged.

Nothing else in either file changes. In particular `aRunOutFromTheTurnDealsOnlyTheRiver`,
`theEventsDescribeTheTransition` and every chip-conservation test read the log by type rather than
by index and must pass untouched.

## Out of scope

- `CardSecrecyTest`: `TASK-010617` already replaced its no-reveal test with the muck invariant, so
  it must pass **unchanged** here — it is in this ticket's `verify` precisely because it is the
  gate this change has to clear.
- `HandWalkthroughTest` and `OpeningRunOutTest` read their logs by type and fold them, so reveals
  leave them green. If either fails, stop and report rather than editing a fourth file.
- Reveal order at a tie, end to end — `TASK-010624`.

## Tests

No new test class. The two modified tests above are the ones `verify` names, together with
`CardSecrecyTest`, which must pass without being touched.

## Acceptance criteria

- [ ] `StreetAdvanceTest.theRiverShowdownPaysTheBetterHand` passes with six events, `HandRevealed`
      for seat 0 at index 3, and no event carrying seat 1's cards
- [ ] `AllInRunOutTest.theRunOutDealsEachStreetInOrder` passes with eight dealer events and
      `HandRevealed` at index 5 naming the same seat as the `PotAwarded`
- [ ] Every other test in `StreetAdvanceTest` and `AllInRunOutTest` passes with no change to its
      body
- [ ] `CardSecrecyTest` passes with no change to the file
- [ ] No file outside the table above is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
