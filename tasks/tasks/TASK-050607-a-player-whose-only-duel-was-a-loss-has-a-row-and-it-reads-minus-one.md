---
schema: 2
id: TASK-050607
title: A player whose only duel was a loss has a row, and it reads minus one
type: task
status: backlog
parent: STORY-0506
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, coins, tests]
depends_on: [TASK-050606]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.aPlayerWhoseOnlyDuelWasALossHasARowAtMinusOne' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0014`'s *"the case to check first"*, checked last as well: the player who lost the only duel
they played is on the ladder, at `−1`, with a rank and a name-less row — not clamped to zero, not
filtered out for being negative, and not hidden for having no display name.

## Why this one has no fixture

Every other test in this class seeds a ladder first, so that a right-looking number cannot be a
default. This one is the opposite case on purpose: the whole ladder is the duel that was just
played, and the point is what a **two-row** ladder contains. `−1` here is not a coincidence to be
guarded against — it is the assertion, and the three ways an implementation loses it (a floor, a
`WHERE coins > 0`, a `JOIN` that needs a display name) each change the row count or the number
rather than leaving it plausibly right.

`ADR-0063` §1 is what makes it a two-row ladder at all: nothing gates a place, so a single duel
puts both players on it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `docs/adr/ADR-0014-duel-coin-economy.md` — signed, unclamped, and the case to check first.
- `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` —
  `theLosersBalanceIsNegativeAndNotClamped`, the same claim about the profile strip.

## Scope

- One new test on `SocketLadderTest`: a fresh database, `openSocketDuel()`, `playToFinish()`, and a
  single `limit = 10` read. No seeded fixture, no filler player.
- The loser is `duel.seat(1 - winnerSeat(outcome))`; no seat number is hard-coded.
- No production file is created or modified.

## Out of scope

- **`GET /api/me`'s `coinBalance` for the loser** — `SocketCoinsTest.theLosersBalanceIsNegativeAndNotClamped`
  already owns that number and this ticket does not restate it.
- **What the client prints for a nameless row** — `TASK-050308`, merged, on the web client. This
  test asserts the field is `null` on the wire and nothing about how it renders.
- **A ladder with more than these two players** — every other test in the class has one.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`.

| Test | Proves |
| --- | --- |
| `aPlayerWhoseOnlyDuelWasALossHasARowAtMinusOne` | one `limit = 10` read with the loser's device id after the duel: `rows` holds exactly **two** entries whose ranks in page order are `1, 2`; the loser's row reads `coins == -1`, `rank == 2` and `displayName == null`; the winner's row reads `coins == 1`, `rank == 1`; `self` for the loser reads rank `2` and coins `−1`; and `nextCursor` is `null` |

Assert `-1` as the literal it is. Not `assertTrue(coins < 0)`, not an absolute value, not a
comparison against a constant the production code also uses.

**Named mutations.** `coerceAtLeast(0)` anywhere on the read path turns the loser's `−1` into `0`
and reddens the test. A `WHERE coins > 0`, or any filter that drops a losing player, turns two rows
into one and reddens the row count and the rank list. An inner join to a display name turns two
rows into none. Deriving the rank from the row's position happens to be right on a two-row ladder,
which is why that mutation is `TASK-050602`'s and `TASK-050606`'s to catch and not this one's.

## Acceptance criteria

- [ ] `SocketLadderTest.aPlayerWhoseOnlyDuelWasALossHasARowAtMinusOne` passes
- [ ] It asserts `rows.size == 2` and `rows.map { it.rank } == listOf(1, 2)`
- [ ] It asserts the loser's `coins` equals the literal `-1`, their `rank` equals `2`, and their
      `displayName` is `null`
- [ ] It asserts the loser's `self` reads rank `2` and coins `−1`
- [ ] The loser is derived from `DuelOutcome.winner`; no seat number is hard-coded in this test
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
