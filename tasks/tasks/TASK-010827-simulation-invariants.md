---
schema: 2
id: TASK-010827
title: The invariants a simulated hand must never break
type: task
status: ready
parent: STORY-0108
module: poker-ai
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [simulation, invariants, ai]
depends_on: []
verify:
  - ./gradlew :poker-ai:test --tests '*SimulationInvariantsTest'
  - ./gradlew :poker-ai:check
---

## Goal

One pure function answers "is this game state legal?", so the harness can ask it after every
single action of every simulated duel.

## Files

| File | Action |
| --- | --- |
| `poker-ai/src/main/kotlin/duels/poker/ai/SimulationInvariants.kt` | create |
| `poker-ai/src/test/kotlin/duels/poker/ai/SimulationInvariantsTest.kt` | create |

Read, do not modify: `poker-engine/src/main/kotlin/duels/poker/engine/game/GameState.kt`
(`chipsInPlay`, `isHandOver`, `seatToAct`, `seats`, `board`), `.../game/Seat.kt`.

## Scope

- One public function with KDoc, package `duels.poker.ai`:

  ```kotlin
  /** The first invariant [state] breaks, described, or null if it breaks none. */
  public fun firstViolation(state: GameState, chipsAtOpen: Int): String?
  ```

- Four checks, in this order, each returning a message naming the offending values:
  1. **chip conservation** — `state.chipsInPlay == chipsAtOpen`;
  2. **no negative stack** — every `seat.stack >= 0`, and no seat's `committedThisStreet` exceeds
     its `committedThisHand`;
  3. **no duplicate card in play** — both seats' `holeCards` plus `board.cards` are all distinct;
  4. **the seat to act can act** — while `!state.isHandOver`, `seatToAct` is non-null and that seat
     has not folded, is not all-in and has a positive stack.
- Pure: no engine call, no randomness, no I/O. It reads a state and returns a string or null. The
  message is the whole diagnostic, so it names values, not just the rule.
- `chipsAtOpen` is a parameter rather than something derived, because conservation is a statement
  about a hand over time and a single state cannot know what it opened with.

## Out of scope

- Playing anything — `TASK-010828` calls this.
- Re-checking rules the engine's own `require` blocks already enforce at construction (that a seat
  index is 0 or 1, that the board size matches the street): a checker earns its keep on the
  invariants that span *actions*, not on those a constructor cannot violate.
- Any invariant about the match layer: chips across hands are `DuelInvariantTest`'s.

## Tests

`SimulationInvariantsTest`, JUnit 5, package `duels.poker.ai`. Build a real opening state with
`startHand(1, 0, listOf(1000, 1000), 50, 100, SplitMix64Rng(1)).newState` and break exactly one
thing at a time with `copy(...)` — `GameState` and `Seat` are data classes.

| Test | Proves |
| --- | --- |
| `aFreshlyDealtHandBreaksNoInvariant` | `firstViolation(state, state.chipsInPlay)` is null |
| `reportsChipsThatDoNotAddUp` | `state.copy(pot = state.pot + 1)` reports a violation whose message contains `chips` |
| `reportsADuplicateCardInPlay` | a state where seat 1 holds seat 0's hole cards reports a violation naming the duplicated card |
| `reportsASeatToActThatHasFolded` | a state whose `seatToAct` seat has `hasFolded = true` reports a violation |
| `reportsASeatToActThatIsAllIn` | a state whose `seatToAct` seat has `isAllIn = true` reports a violation |
| `reportsNoSeatToActInAHandThatIsNotOver` | `state.copy(seatToAct = null)` on a live hand reports a violation |
| `acceptsNoSeatToActOnceTheHandIsOver` | a state with `street = Street.COMPLETE` and `seatToAct = null` reports nothing |
| `checksChipsBeforeAnythingElse` | a state breaking both conservation and the seat-to-act rule reports the chip message, so the order is pinned |

## Acceptance criteria

- [ ] `SimulationInvariantsTest.aFreshlyDealtHandBreaksNoInvariant` passes
- [ ] `SimulationInvariantsTest.reportsChipsThatDoNotAddUp` passes
- [ ] `SimulationInvariantsTest.reportsADuplicateCardInPlay` passes
- [ ] `SimulationInvariantsTest.reportsASeatToActThatHasFolded` passes
- [ ] `SimulationInvariantsTest.reportsASeatToActThatIsAllIn` passes
- [ ] `SimulationInvariantsTest.reportsNoSeatToActInAHandThatIsNotOver` passes
- [ ] `SimulationInvariantsTest.acceptsNoSeatToActOnceTheHandIsOver` passes
- [ ] `SimulationInvariantsTest.checksChipsBeforeAnythingElse` passes
- [ ] No file outside the two in the Files table is modified — in particular no file under
      `poker-engine/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
