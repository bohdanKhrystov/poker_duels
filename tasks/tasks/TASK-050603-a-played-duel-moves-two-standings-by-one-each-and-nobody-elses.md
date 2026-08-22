---
schema: 2
id: TASK-050603
title: A played duel moves two standings by one each — measured as a difference, on a ladder that was never zero
type: task
status: ready
parent: STORY-0506
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, leaderboard, coins, tests]
depends_on: [TASK-050602]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theLadderMovesTheWinnerUpOneAndTheLoserDownOne' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`STORY-0506`'s central claim, observed from outside: a duel played through two WebSocket
connections moves the winner's season standing by exactly `+1` and the loser's by exactly `−1`,
read off `GET /api/standings` before and after, and nobody else's standing moves at all.

## The trap this ticket exists to avoid

The story says it plainly: **a test that asserts the winner has one coin passes on an empty
database and proves nothing.** An implementation that printed `+1` beside whoever won the last duel
and `−1` beside whoever lost it — never summing anything — would satisfy that test forever.

So this ticket's fixture starts both duellists at `±3`, and every assertion about them is an
`after − before` **difference**. A ladder that answers `+1` for a winner and `−1` for a loser
regardless of history is then wrong for **both** duellists whichever seat wins: the right answer here
is a *difference* of exactly `+1` and `−1`, and a history-ignoring ladder produces some other
difference for both. Which wrong pair it produces depends on which seat won — that is precisely why
the criterion asserts the difference rather than any absolute pair. The fixture is a different one from `TASK-050602`'s for exactly
this reason: that ladder's post-duel numbers happen to include a `+1`, and a number that could have
been right by accident is not evidence.

Nothing in this test names a winning seat. The winner is read off `DuelOutcome.winner` and the
assertions are symmetric, so it holds whichever seat the seeds send to the top.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` — `winnerSeat(outcome)`,
  and the *"established before `playToFinish()`"* comment that says why the reads bracket the duel.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt` — `playToFinish`,
  `SocketDuel.seat`, `POLICY_SEED`.
- `docs/adr/ADR-0014-duel-coin-economy.md` — signed, unclamped, `wins − losses`.

## Scope

- One new test on `SocketLadderTest`, with its own private fixture builder
  `private suspend fun seedADeepLadder(host: Player, guest: Player): LadderFixture` that uses the
  `recordWin` helper `TASK-050602` added: three wins for `host` over `fillerOne` at
  `thisSeasonAt(1)`, `thisSeasonAt(2)`, `thisSeasonAt(3)`, and three wins for `fillerTwo` over
  `guest` at `thisSeasonAt(4)`, `thisSeasonAt(5)`, `thisSeasonAt(6)`.
- A private `winnerSeat(outcome)` on the class, copied from `SocketCoinsTest`: a `checkNotNull`
  that names both seeds when the outcome has no winner, so a fixture that somehow drew reports
  that rather than silently treating seat 0 as the winner.
- No production file is created or modified.

## Out of scope

- **`GET /api/me`'s `coinBalance`.** Every duel in this fixture is inside the current season, so
  the two numbers agree here and the agreement proves nothing about either. `TASK-050608` is where
  they are compared, on a fixture where they also **disagree** for somebody.
- **Rank movement.** This test is about the number, not the place; `TASK-050604` owns the order.
- **Naming a winning seat.** `TASK-050604` pins one, for a reason that does not apply here.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

**Fixture.** `openSocketDuel()`, then `seedADeepLadder(host, guest)`. Standings before the duel:
`fillerTwo +3`, `host +3` (both rank `1`), `guest −3`, `fillerOne −3` (both rank `3`).

| Test | Proves |
| --- | --- |
| `theLadderMovesTheWinnerUpOneAndTheLoserDownOne` | reads all four players' `coins` off one `limit = 10` request **before** `playToFinish()`, asserting `host = 3`, `guest = −3`, `fillerOne = −3`, `fillerTwo = 3`; plays the duel; reads the same four again; asserts `after(winner) − before(winner) == +1` and `after(loser) − before(loser) == −1`, where the winner and loser are `duel.seat(winnerSeat(outcome))` and `duel.seat(1 − winnerSeat(outcome))`; and asserts both fillers' `coins` are **identical** before and after |

Every assertion message must name `duel.handSeed`, `POLICY_SEED`, the seat and both numbers, as the
rest of this package's messages do — a failure here is the story's headline failure and must be
diagnosable from the report alone.

**Named mutations.** Awarding the coin to the losing seat reddens both differences. Clamping a
standing at zero (`ADR-0014`'s *"the case to check first"*) reddens the loser's, whose right answer
is negative on both sides of the duel. Answering a fixed `+1`/`−1` per outcome instead of summing
the window reddens both, because the measured difference is then not `+1`/`−1` for either duellist —
the exact wrong values depend on which seat won, which is why the assertion is on the difference. This
is the mutation `TASK-050602`'s fixture could not have caught. Widening the write to touch a third player's rows
reddens the fillers-unchanged assertion.

## Acceptance criteria

- [ ] `SocketLadderTest.theLadderMovesTheWinnerUpOneAndTheLoserDownOne` passes
- [ ] The pre-duel read happens before `playToFinish()` is called, and both duellists' pre-duel
      `coins` are asserted to be `3` and `−3`
- [ ] The post-duel assertions are written as `after − before`, equal to `+1` and `−1`
- [ ] The winner is taken from `DuelOutcome.winner`; no seat number is hard-coded in this test
- [ ] Both fillers' `coins` are asserted equal before and after
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
