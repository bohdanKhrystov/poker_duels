---
schema: 2
id: TASK-020711
title: Chips are conserved in what the client sees, not just in what the engine knows
type: task
status: backlog
parent: STORY-0207
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, duel, property, chips]
depends_on: [TASK-020710]
verify:
  - ./gradlew :poker-server:test --tests '*RunnerChipConservationTest'
  - ./gradlew :poker-server:check
---

## Goal

Every `Snapshot` the runner ever sends accounts for all 20 000 chips, checked from the frames alone
— so a projection that dropped or duplicated a chip on its way to a player fails here even though
the engine's own invariants hold.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/RunnerChipConservationTest.kt` | create |

Read, do not modify:
`poker-server/src/test/kotlin/duels/poker/server/duel/RunnerDuel.kt` (`playDuel`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/PlayerView.kt` (`pot` holds only swept chips;
chips still in front of a seat are its `committedThisStreet`),
`poker-engine/src/main/kotlin/duels/poker/engine/game/SeatView.kt`.

## Scope

- One test class, no production code. Package `duels.poker.server.duel`.
- The total a snapshot accounts for is
  `view.pot + view.seats.sumOf { it.stack } + view.seats.sumOf { it.committedThisStreet }`.
  It must equal `2 * DuelFormat.DEFAULT.startingStack` in every snapshot of every hand, because a
  duel's chips never leave the two stacks between hands.
- **The test reads `ServerMessage` values only.** It must not import `GameState`, `MatchState`,
  `HandLog`, `MatchLog` or anything from `duels.poker.engine.log`, and must not read
  `PlayedDuel.runner` except for `runner.outcome` in the final-stacks test. Checking the property
  against the engine's own state would restate `SimulationInvariants`; checking it against the
  frames is the only version of it this story does not already have.
- A failing assertion names the duel seed and the index of the offending frame, so a failure is
  reproducible from the message alone.
- Seeds `1L..20L`, `@Timeout(120)`.

## Out of scope

- Card secrecy and the seed — `TASK-020712`.
- Replay — `TASK-020713`.
- Any new invariant inside the engine: if this test fails, the bug is in the projection or the
  runner, and the fix is a new ticket, not a widened one.

## Tests

`RunnerChipConservationTest`, JUnit 5, package `duels.poker.server.duel`.

| Test | Proves |
| --- | --- |
| `everySnapshotAccountsForEveryChip` | over seeds `1L..20L`, every `Snapshot` in `outbound` totals 20 000 |
| `noSnapshotShowsANegativeStackOrPot` | over the same range, every `SeatView.stack`, `committedThisStreet` and `view.pot` is at least 0 |
| `theLastSnapshotEachSeatSawMatchesTheOutcome` | for each seat, the final `Snapshot` addressed to it shows `seats.map { it.stack }` equal to `runner.outcome!!.finalStacks` |
| `bothSeatsSawTheSameChipTotalThroughout` | pairing the snapshots each seat received by index, the totals agree frame for frame |

## Acceptance criteria

- [ ] `RunnerChipConservationTest.everySnapshotAccountsForEveryChip` passes
- [ ] `RunnerChipConservationTest.noSnapshotShowsANegativeStackOrPot` passes
- [ ] `RunnerChipConservationTest.theLastSnapshotEachSeatSawMatchesTheOutcome` passes
- [ ] `RunnerChipConservationTest.bothSeatsSawTheSameChipTotalThroughout` passes
- [ ] The file imports nothing from `duels.poker.engine.log`, and does not name `GameState` or
      `MatchState`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
