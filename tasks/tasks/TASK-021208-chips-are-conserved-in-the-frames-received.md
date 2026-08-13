---
schema: 2
id: TASK-021208
title: Chips are conserved in the frames the two clients actually received
type: task
status: ready
parent: STORY-0212
module: poker-server
estimate: S
tier: haiku
review: deep
files_touched: 1
labels: [server, testing, end-to-end]
depends_on: [TASK-021207]
verify:
  - ./gradlew :poker-server:test --tests '*SocketChipsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

At every point in a duel played over real sockets, the chips the two players can see in the frames
they received add up to the chips the duel started with.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketChipsTest.kt` | create |

Read, do not modify: `e2e/SocketDuel.kt`, `e2e/E2eServer.kt`,
`test/duel/RunnerChipConservationTest.kt` (the same formula one layer down).

## Scope

- The total a single snapshot accounts for is
  `view.pot + view.seats.sumOf { it.stack } + view.seats.sumOf { it.committedThisStreet }`, exactly
  as `RunnerChipConservationTest.accountedFor` computes it. The expected value is
  `2 * DuelFormat.DEFAULT.startingStack`, because `CreateRoom` opens the default format.
- Every number comes from a `ServerMessage.Snapshot` in a client's `received`. This test reaches no
  engine state and no log.
- Failure messages name the hand seed, the policy seed, the seat, and the index of the offending
  frame within that client's `received`.

## Out of scope

- Coins. Chips are the duel's internal currency; the duel coin is `TASK-021209`.
- Any statement about pot correctness or side pots — `EPIC-01` owns those, and this story adds no
  production behaviour.

## Tests

`SocketChipsTest`, JUnit 5, package `duels.poker.server.e2e`, `@Timeout(120)`. One duel per test,
each in its own `testApplication` on a fresh migrated schema.

| Test | Proves |
| --- | --- |
| `everySnapshotAccountsForEveryChip` | after `playToFinish()`, every `Snapshot` in either client's `received` accounts for `2 * startingStack` |
| `bothClientsReceivedASnapshotPerHand` | each client's `received` holds at least `outcome.handsPlayed` snapshots — the fixture that stops the test above passing on an empty list |
| `noSnapshotShowedANegativeStackOrPot` | no `Snapshot` in either client's `received` has a negative `pot`, `stack` or `committedThisStreet` |

## Acceptance criteria

- [ ] `SocketChipsTest.everySnapshotAccountsForEveryChip` passes
- [ ] `SocketChipsTest.bothClientsReceivedASnapshotPerHand` passes
- [ ] `SocketChipsTest.noSnapshotShowedANegativeStackOrPot` passes
- [ ] The file names none of `RoomRegistry`, `DuelRunner`, `GameState`, `MatchLog`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
