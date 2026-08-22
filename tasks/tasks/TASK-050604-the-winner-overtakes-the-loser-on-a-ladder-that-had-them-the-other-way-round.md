---
schema: 2
id: TASK-050604
title: The winner overtakes the loser on a ladder that had them the other way round
type: task
status: backlog
parent: STORY-0506
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, ranking, tests]
depends_on: [TASK-050603]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.theDuelPutsTheWinnerAboveTheLoserOnALadderThatHadThemTheOtherWayRound' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

The duel does not just change two numbers — it changes who is above whom. The ladder that the duel
is played into has the **loser** ranked above the winner; the ladder read afterwards has them the
other way round.

## Why the head start has to belong to the loser, and why that means naming a seat

An ordering claim is only evidence if the fixture could have produced the wrong order. A ladder
sorted by insertion order, by player id, or by *most recent winner first* would look correct on any
fixture where the winner already happened to be on top. The only fixture that rules all of those
out is one where the winner starts **below** the loser and has to overtake them — and a ±1 swing
overtakes only across a gap of one, so the head start cannot be handed to whichever seat turns out
to win. It has to be chosen before the duel is played.

It can be, because the duel is deterministic. `HAND_SEED` and `POLICY_SEED` are fixed constants and
`SocketDuelTest.theSameSeedsPlayTheSameDuel` is the merged proof that the same seeds play the same
duel — on a fresh schema, twice, frame count included. Under those two seeds the duel is won by
**seat 1**, the `GUEST_DEVICE` client, in 9 hands with final stacks `[0, 20000]`. That is a
measured property of the seeds, recorded here so the coder does not have to discover it, and it is
pinned by an assertion in the test rather than assumed silently.

If that assertion ever fails, the seeds have changed what they play. The fix is one line — move the
head start in `seedTheLadderTheDuelArrivesInto` to the other seat — and the failure message must
say so, because a green ordering assertion on a fixture that no longer runs the other way round is
worth nothing and would not otherwise announce itself.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `docs/adr/ADR-0064-tied-players-share-one-rank-and-row-order-is-not-a-ranking.md` — §1 and §2.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuelTest.kt` —
  `theSameSeedsPlayTheSameDuel`, the determinism this ticket leans on.

## Scope

- One new test on `SocketLadderTest`, using `seedTheLadderTheDuelArrivesInto` from `TASK-050602`
  unchanged — `host +1`, `guest 0`, `fillerTwo 0`, `fillerOne −1`.
- A file-scope `private const val EXPECTED_WINNING_SEAT: Int = 1`, with a comment naming
  `HAND_SEED`, `POLICY_SEED`, `SocketDuelTest.theSameSeedsPlayTheSameDuel`, and what to do when it
  stops holding.
- No production file is created or modified.

## Out of scope

- **Asserting which of two tied rows comes first.** `ADR-0064` §4 — arbitrary, invisible, not a
  measure of play. `guest` is tied with `fillerTwo` before the duel and `host` is tied with
  `fillerTwo` after it, and neither tie's internal order is asserted anywhere.
- **Treating a row's position as a rank.** `ADR-0064` §2 — the rank is the field on the row. The
  row indices are asserted here as *the order the ladder was served in*, alongside the ranks and
  never instead of them.
- **The coin arithmetic.** `TASK-050603` owns the `+1`/`−1`; this test asserts standings only as
  far as it needs to state the order.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `theDuelPutsTheWinnerAboveTheLoserOnALadderThatHadThemTheOtherWayRound` | **before**: `host` reads rank `1` coins `1`, `guest` reads rank `2` coins `0`, and `host`'s row index in `rows` is lower than `guest`'s — the loser-to-be is ahead. Then `playToFinish()`, and `outcome.winner` is asserted to equal `EXPECTED_WINNING_SEAT` with a message naming both seeds and the remedy above. **After**: `guest` reads rank `1` coins `1`, `host` reads rank `2` coins `0`, and `guest`'s row index is now lower than `host`'s |

Both reads use `limit = 10`, so one page holds the whole four-row ladder and no cursor is involved.

**Named mutations.** Ordering rows by insertion order, by player id, or by *whoever won most
recently* reddens the after-order assertion while leaving the before-order assertion green — the
asymmetry that makes the head start worth building. Ordering by `coins` ascending reddens both.
Computing rank from the row's position reddens the after-ranks, where the second row's rank is `2`
and the fourth's is `4`.

## Acceptance criteria

- [ ] `SocketLadderTest.theDuelPutsTheWinnerAboveTheLoserOnALadderThatHadThemTheOtherWayRound`
      passes
- [ ] The test asserts, before the duel, that `host` outranks `guest` — both the ranks `1` and `2`
      and the two row indices
- [ ] The test asserts `outcome.winner == EXPECTED_WINNING_SEAT`, and its failure message names
      `HAND_SEED`, `POLICY_SEED` and the instruction to move the head start to the other seat
- [ ] The test asserts, after the duel, that `guest` outranks `host` — both the ranks and the two
      row indices
- [ ] No assertion in the test constrains the relative order of two rows with equal `coins`
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
