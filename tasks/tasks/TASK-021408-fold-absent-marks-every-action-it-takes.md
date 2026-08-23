---
schema: 2
id: TASK-021408
title: foldAbsent marks every action it takes for an absent seat, to both seats
type: task
status: backlog
parent: STORY-0214
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, presence, provenance]
depends_on: [TASK-021407]
verify:
  - ./gradlew :poker-server:test --tests '*AbsentSeatsTest'
  - ./gradlew :poker-server:test --tests '*RoomAbsentSeatTest'
---

## Goal

Every action the server submits for an absent seat is labelled as the server's:
`ActedForAbsentSeat(seat, handNumber, actionSequence, action)`, to **both** seats, immediately
before the frames that action produced — and only when the action actually moved the duel.

This is the deliberate reversal `ADR-0028` records: `ADR-0023`'s wire-indistinguishability property
was correct when it was written and becomes false here. **Say so in the commit message.**

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/duel/AbsentSeats.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/duel/AbsentSeatsTest.kt` | modify |

Read `duel/Addressed.kt`, `duel/DuelTurn.kt` (`decisionPointOf`), and
`docs/adr/ADR-0028-the-wire-names-an-absent-opponent.md` §4. Nothing else.

## Scope

- In `foldAbsent`'s loop, after `act` returns and **only when the runner moved**, prepend two
  frames — one `Addressed(0, mark)` and one `Addressed(1, mark)` carrying the same
  `ActedForAbsentSeat` — before that call's own `outbound`. The mark's fields are the ones the loop
  already holds: the absent `seat`, `hand.state.handNumber`, the `actionSequence` it put on the
  `Act`, and `ActionType.FOLD` or `ActionType.CHECK` to match the `PlayerAction` it chose.
- **The check is marked exactly as the fold is.** `ADR-0023` means a timeout is often a check;
  marking only folds reproduces the dishonesty one case further in.
- **No mark on a submission that made no progress.** The existing `if (next.runner == before)`
  guard already detects it; restructure so the mark is appended on the moved branch only. The three
  early returns — no hand, no seat on turn, seat not absent — likewise emit nothing.
- **`foldAbsent`'s KDoc must be corrected in the same change.** Two sentences become false: *"a turn
  given up for absence is, in the log and on the wire, indistinguishable from one a player acted on
  themselves"* — the **log** half stands, the **wire** half does not — and *"Nothing here constructs
  a game state, an event, or a frame of its own"*, which now constructs a frame and only a frame.
  Cite `ADR-0028` in the replacement.
- The engine's event log is untouched: `PlayerFolded` and `PlayerChecked` are exactly what they
  were, and `EVENT_SCHEMA_VERSION` does not move.

## Out of scope

- **`Room.foldAbsentSeats`' KDoc.** Its indistinguishability sentence compares the expiry route to
  the `act` route — *"indistinguishable from one folded because `act` happened to be called right
  after it"* — and both routes run through `foldAbsent`, so both are marked and the sentence stays
  true. `Room.kt` is not in this ticket's *Files* table and must not be edited.
- The checked-down case's own test — `TASK-021409`.
- Provenance in the event log. Absence is a fact about the server; `poker-engine` learns nothing.
- Emitting a mark anywhere but `foldAbsent`.

## Tests

`AbsentSeatsTest` — an existing file, on `seeds = HandSeedSource { 7L }`, `oneHand`/`threeHands`,
and a seat taken from `step.runner.hand!!.state.seatToAct!!` rather than written as a literal.

**Three merged tests already assert `result.outbound == step.outbound` on paths where nothing
folds** — `aSeatSomebodyIsSittingInIsLeftAlone`, `noAbsentSeatAtAllIsLeftAlone` and
`aFinishedDuelIsLeftAlone`. They are the story's *"a submission that makes no progress produces no
mark"* criterion and they must pass **unchanged**: an implementation that marks unconditionally
fails all three. Do not edit them. `theOpponentIsToldWhatHappened` reads
`result.outbound.drop(step.outbound.size)` and asserts *at least one* frame for the opponent, so it
also passes unchanged.

| Test | Proves |
| --- | --- |
| `anAbsentFoldIsMarkedAsTheServersOwn` | on `oneHand` with `absent = setOf(seatToAct)`, the new frames contain an `ActedForAbsentSeat` whose `seat` is `seatToAct`, whose `action` is `FOLD`, and whose `handNumber` is `1` |
| `theMarkGoesToBothSeats` | that same mark appears once addressed to seat `0` and once to seat `1`, and the two payloads are equal |
| `theMarkPrecedesTheFramesTheActionProduced` | the index of the first mark is lower than the index of the first `Events` or `Snapshot` frame the same action produced — by index, not by presence |
| `theMarkNamesTheDecisionPointTheActionWasSentFor` | the mark's `actionSequence` equals the sequence `foldAbsent` put on the `Act`, so a client can attach the label at coordinates it already holds |
| `aStepThatFoldsNothingCarriesNoMark` | on the three left-alone fixtures, `result.outbound` contains no `ActedForAbsentSeat` at all — stated as its own assertion rather than left implicit in the equality |
| `everyActionInARunAwayDuelIsMarked` | on `threeHands` with `absent = setOf(0, 1)`, the number of marks addressed to seat `0` equals the number of actions in `runner.log.hands.flatMap { it.actions }` — every action taken, not just the last |

## Acceptance criteria

- [ ] Every test method in the table above passes
- [ ] `aSeatSomebodyIsSittingInIsLeftAlone`, `noAbsentSeatAtAllIsLeftAlone`,
      `aFinishedDuelIsLeftAlone` and `theOpponentIsToldWhatHappened` pass with no edit
- [ ] `RoomAbsentSeatTest` passes with no edit — its `oneStepCarriesBothTheActionAndTheFold`
      asserts an inequality on frame counts, which the mark only widens
- [ ] `foldAbsent`'s KDoc no longer claims wire-indistinguishability or that it constructs no frame,
      and cites `ADR-0028`
- [ ] `Room.kt` is not in the diff
- [ ] The commit message names the reversal explicitly
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
