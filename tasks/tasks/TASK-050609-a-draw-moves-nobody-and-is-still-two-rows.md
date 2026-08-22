---
schema: 2
id: TASK-050609
title: A draw moves nobody, and is still two rows and two places
type: task
status: backlog
parent: STORY-0506
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, e2e, leaderboard, coins, tests]
depends_on: [TASK-050608]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.aDrawnDuelMovesNeitherPlayersStanding' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest.aDrawnDuelIsTwoResultRowsAndNotNone' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.SocketLadderTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-engine:check
---

## Goal

The last of `STORY-0506`'s criteria: a drawn duel leaves both players exactly where they were —
asserted as a difference of **zero**, not as an absence — while still writing the two result rows
`ADR-0015` requires, and still putting a player whose only duel was a draw on the ladder at `0`.

## Why this duel is recorded and not played, and how far down the stack it goes

No duel reachable through the socket can draw. `CreateRoom` opens the shipped default format, whose
end condition is `EndCondition.Freezeout`, and `outcomeOf` returns a `null` winner only under
`EndCondition.FixedHands` with level stacks. `SocketCoinsTest` and `SocketHistoryTest` both say so
in their `winnerSeat` helpers, and `TASK-021008` and `TASK-021108` parked the socket-level draw
before this epic existed. There is nothing to play.

So the drawn duel is recorded through `PostgresDuelResultStore`, which is as far down as this can
honestly go: it is the class `PostgresDuelResultSink` delegates **every** write to, and it is where
`coinDeltas` (`ADR-0014`) and `ADR-0015`'s two-row write actually live. The sink above it only
stamps two timestamps and a format label, neither of which this test is about. Say all of this in
the class KDoc or the test's, so a later reader does not mistake a deliberate choice for a
shortcut — and so nobody "fixes" it by inventing a way to open a fixed-hands duel over the wire,
which would be new production code and is `STORY-0506`'s out-of-scope list, not this ticket's.

**The two halves are not the same claim.** *Nobody moved* is visible on the ladder. *Two rows
exist* is not: a draw that wrote no rows at all would leave every standing exactly as unmoved. That
half is checked through `GET /api/me/duels`, which is the product's own view of those rows, and it
is the assertion `ADR-0015` was written for.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt` | modify |

Read, do not edit:

- `docs/adr/ADR-0015-a-draw-writes-two-result-rows.md` — the whole decision.
- `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` —
  `DuelOutcomeLabel.DREW`, `DuelSummaryResponse`.
- `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultSink.kt` — what the sink
  adds over the store, and therefore what this test is not exercising.

## Scope

- A private fixture builder on `SocketLadderTest` returning the four players it creates, using the
  `resolvePlayer`, `recordWin` and `recordDraw` helpers from `TASK-050602`:
  - `settled` beats `unsettled` at `thisSeasonAt(1)` — `settled +1`, `unsettled −1`;
  - `newcomerOne` draws `newcomerTwo` at `thisSeasonAt(2)` — both `0`, and neither has any other
    duel;
  - device ids `"e2e-draw-1"` … `"e2e-draw-4"` at file scope.
- Two new tests. Neither opens a socket or plays a duel; both still call `installDuelServer` and
  read over HTTP.
- No production file is created or modified.

## Out of scope

- **Making a drawn duel reachable over the socket.** New production code, and `STORY-0506` says a
  story that needs any has found a defect in an earlier story rather than a task of its own. If
  this is ever wanted it is a ticket against the protocol, raised then.
- **Asserting the draw's `startedAt`/`finishedAt` shape**, which is `PostgresDuelResultSink`'s
  business and already has its own tests, not the ladder's.
- **The order of the two tied rows** — `ADR-0064` §4.

## Tests

`SocketLadderTest`, `-PrequireDocker=true`. Before the drawn duel under test, the ladder is
`settled +1` (rank `1`), `newcomerOne 0` and `newcomerTwo 0` (both rank `2`), `unsettled −1`
(rank `4`).

| Test | Proves |
| --- | --- |
| `aDrawnDuelMovesNeitherPlayersStanding` | reads the ladder with `limit = 10`, records a **draw** between `settled` and `unsettled` at `thisSeasonAt(3)`, reads it again, and asserts: `settled` is `+1` before and after and `unsettled` is `−1` before and after, each written as an `after − before == 0` difference against a value that is not zero; the row count is `4` in both reads; and `newcomerOne` and `newcomerTwo` are on the ladder at `0`, which is a draw showing as a place and not as an absence (`ADR-0061` §4) |
| `aDrawnDuelIsTwoResultRowsAndNotNone` | after that same draw, `GET /api/me/duels` for `settled` and for `unsettled` each return **two** duels; the drawn one carries the **same** `duelId` in both lists, `coinDelta == 0` on both, and `outcome == DuelOutcomeLabel.DREW` on both |

**Named mutations.** Awarding a coin on a draw reddens the first test's difference on both players.
Writing no rows for a draw — the alternative `ADR-0015` rejected — leaves the first test **green**
and reddens the second, which is exactly why both are here. Writing one row instead of two reddens
the second on whichever player lost their row. Omitting players with a zero standing from the
ladder reddens the newcomers' assertion and the row count.

## Acceptance criteria

- [ ] `SocketLadderTest.aDrawnDuelMovesNeitherPlayersStanding` passes, asserting both standings as
      `after − before == 0` against pre-draw values of `+1` and `−1`
- [ ] It asserts `rows.size == 4` on both reads and both newcomers present at `0`
- [ ] `SocketLadderTest.aDrawnDuelIsTwoResultRowsAndNotNone` passes, asserting two duels in each
      list, one shared `duelId`, `coinDelta == 0` and `DuelOutcomeLabel.DREW` on both sides
- [ ] The test or class KDoc states that no socket-reachable duel can draw, naming
      `EndCondition.Freezeout`, and why the drawn duel is recorded through
      `PostgresDuelResultStore`
- [ ] Every test already in the class passes with its assertions unchanged
- [ ] Every file this ticket creates or modifies is under `poker-server/src/test/`; `poker-engine`
      is untouched by this ticket and by every ticket of `STORY-0506`
- [ ] Every command in `verify:` exits 0, including `./gradlew :poker-engine:check`

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
