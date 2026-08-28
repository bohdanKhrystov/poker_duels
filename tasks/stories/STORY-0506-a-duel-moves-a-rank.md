---
id: STORY-0506
title: A duel moves a rank, end to end
type: story
status: in-progress
parent: EPIC-05
module: poker-server
labels: [server, leaderboard, end-to-end]
depends_on: [STORY-0503]
---

## Goal

One test plays a real duel between two profiles against a real database, then reads the ladder and
finds both players where `ADR-0014`'s arithmetic says they should be — the winner one coin up, the
loser one coin down, and every coin on the ladder accounted for.

## Why

It is the epic's closing proof, and it is the only story here whose criteria were writable on the
day the epic was written, because they depend on no open decision. Every other story in `EPIC-05`
answers *what the ladder is*; this one answers *does the ladder tell the truth about a duel that
actually happened*, and those are different questions with different failure modes. `EPIC-04` ends
the same way, with `STORY-0414`.

## Design notes

**Settled, and none of it moves when `DEC-055`–`DEC-059` land:**

- **A real duel, not a fixture.** The proof is worth having only if the coins came from the same
  path production uses: the socket, the room, the engine, the result sink. `PostgresTestSupport`
  and the `DuelSocket*` suite are the existing machinery, and this story composes them rather than
  inventing a shortcut. Writing `duel_result` rows directly would prove the query and nothing else.
- **Against PostgreSQL, gated the way the rest of the suite is.** `-PrequireDocker=true` turns a
  missing daemon from a skipped test into a failure; this test must be inside that gate, because a
  silently skipped end-to-end test is worse than none.
- **The arithmetic is `ADR-0014`'s and is asserted as a delta, not as a value.** Read both players'
  standings before the duel and after it, and assert the *change* — `+1` and `−1`. A test that
  asserts the winner has one coin passes on an empty database and proves nothing.
- **Conservation is asserted over the whole set**, not per player: the coins the ladder reports sum
  to the `coin_delta`s stored for that scope. This is chip conservation at ladder scale, and it is
  the assertion that catches a ladder which is internally consistent and globally wrong.
- **A draw moves nobody.** `ADR-0015` writes a draw as two rows of zero rather than as no rows, so
  the ladder must show two players unmoved and two rows recorded — the two are not the same thing
  and only one of them is checkable without looking at the table.
- **The engine learns nothing**, and this story adds no production code at all if the five before it
  did their jobs. If it needs production code, that is a finding about one of them.

- **The fixture needs no threshold to clear.**
  [`ADR-0063`](../../docs/adr/ADR-0063-nothing-gates-a-place-and-the-farm-is-accepted-until-the-ladder-is-public.md)
  answers `DEC-056`: nothing gates a place, so two players who have duelled **once** are both on the
  ladder and this test plays exactly the duels it means to. The prediction written here — that every
  criterion below is a *difference*, and a difference survives a threshold — was right and cost
  nothing. The same ADR's §4 sharpens the conservation note above from *sums agree* to a constant:
  a season's standings sum to exactly `0`, because every duel writes two rows summing to zero and
  both players are listed.

- **Everything this test plays happens inside one season**, because it plays it now. That is why the
  ladder's number and `GET /api/me`'s `coinBalance` agree here — a **property of the fixture**, not
  an invariant of the product
  ([`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md)
  §4 and §6: the ladder prints the season standing, the strip prints the all-time counter, and they
  part company from the second season onwards). The criterion below says so, so that nobody later
  reads a passing test as a guarantee it never made.

**Not blocked on any decision.** It is blocked on `STORY-0503` existing, which is a different thing —
and it is the reason the epic could be scheduled the moment `DEC-055` was answered rather than
re-planned. It was blocked on `STORY-0505` too until
[`ADR-0061`](../../docs/adr/ADR-0061-a-season-is-a-calendar-month-and-the-coin-never-resets.md)
dropped that story: a boundary runs no code, so there is nothing for this test to wait for.

## Tasks

Split on 2026-08-22 into **nine** tickets, strictly linear: every one of them adds tests to the
single class `poker-server/src/test/kotlin/duels/poker/server/e2e/SocketLadderTest.kt`, so no two
are startable at once. `TASK-050601` is `ready`; the rest are `backlog`.

| ID | Title | Status |
| --- | --- | --- |
| [TASK-050601](../tasks/TASK-050601-the-ladder-two-seated-players-and-no-place-yet.md) | The ladder, read from the same application the duel is played in — two seated players and no place yet | **ready** |
| [TASK-050602](../tasks/TASK-050602-the-ladder-the-duel-arrives-into-a-shared-rank-a-skipped-one-and-last-month-left-out.md) | The ladder the duel arrives into — a shared rank, a skipped one, and last month left out | backlog |
| [TASK-050603](../tasks/TASK-050603-a-played-duel-moves-two-standings-by-one-each-and-nobody-elses.md) | A played duel moves two standings by one each — measured as a difference, on a ladder that was never zero | backlog |
| [TASK-050604](../tasks/TASK-050604-the-winner-overtakes-the-loser-on-a-ladder-that-had-them-the-other-way-round.md) | The winner overtakes the loser on a ladder that had them the other way round | backlog |
| [TASK-050605](../tasks/TASK-050605-every-standing-is-that-players-own-season-results.md) | Every standing on the ladder is that player's own season results, and the ladder totals zero | backlog |
| [TASK-050606](../tasks/TASK-050606-the-players-own-place-after-the-duel-on-the-page-and-off-it.md) | The player's own place after the duel — served on the page drawn and off it | backlog |
| [TASK-050607](../tasks/TASK-050607-a-player-whose-only-duel-was-a-loss-has-a-row-and-it-reads-minus-one.md) | A player whose only duel was a loss has a row, and it reads minus one | backlog |
| [TASK-050608](../tasks/TASK-050608-the-ladder-and-the-profile-strip-agree-here-and-part-company-for-an-older-record.md) | The ladder and the profile strip agree for both duellists — and part company for an older record | backlog |
| [TASK-050609](../tasks/TASK-050609-a-draw-moves-nobody-and-is-still-two-rows.md) | A draw moves nobody, and is still two rows and two places | backlog |

**Three things the split settled that the story had left open.**

- **A drawn duel cannot be played over the socket.** `CreateRoom` opens the shipped default format,
  whose end condition is `EndCondition.Freezeout`, and `outcomeOf` names a `null` winner only under
  `EndCondition.FixedHands` with level stacks. `TASK-050609` therefore records the drawn duel
  through `PostgresDuelResultStore` — the class `PostgresDuelResultSink` delegates every write to,
  and where `coinDeltas` and `ADR-0015`'s two-row write live — and says so in the test. Making a
  draw reachable over the wire would be new production code, which this story refuses.
- **The clock stays real.** `PostgresStandingsReads` reads the half-open window
  `[season.start, asOf)`, so a fixed `wallClock` would stamp the duel's `finished_at` at exactly
  the `asOf` its own ladder read mints and the duel under test would vanish from the ladder it is
  supposed to move. Fixture rows are placed at `currentSeason(Clock.systemUTC()).start` plus
  milliseconds instead, which is inside the window at both ends whenever the suite runs.
- **The ordering criterion names a winning seat, once.** A ±1 swing overtakes only across a gap of
  one, so the head start cannot be handed to whichever seat turns out to win — it has to be chosen
  before the duel. It can be: under `HAND_SEED` and `POLICY_SEED` the duel is won by seat 1, and
  `SocketDuelTest.theSameSeedsPlayTheSameDuel` is the merged proof that the same seeds play the
  same duel. `TASK-050604` pins that seat in an assertion whose failure message says what to change.

## Acceptance criteria

- [ ] A duel played through the socket to a winner moves the winner's standing by exactly `+1` and
      the loser's by exactly `−1`, asserted as a **before-and-after difference** on both players.
- [ ] The two players' relative order on the ladder afterwards is the order the duel produced,
      asserted against a starting fixture where it was the other way round — so the assertion cannot
      pass on the order they were inserted in.
- [ ] The coins the ladder reports sum to the `coin_delta`s stored for the same scope, asserted
      after the duel.
- [ ] A drawn duel leaves both players' standings unchanged and both still on the ladder, asserted
      as a difference of zero rather than as an absence.
- [ ] A player whose only duel was a loss appears on the ladder at `−1` and is not clamped, hidden
      or filtered — `ADR-0014`'s *"the case to check first"*, checked here last as well as first.
- [ ] The ladder read after the duel agrees with `GET /api/me`'s `coinBalance` for both players
      **because every duel in the fixture is inside the current season**, and the test says so where
      it asserts it. The two numbers are not the same number in general (`ADR-0061` §6); a fixture
      spanning two seasons is `STORY-0502`'s and does not belong in this end-to-end proof.
- [ ] The test runs under `-PrequireDocker=true` and fails, rather than skips, when Docker is
      absent.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **A browser end-to-end test.** `DEC-024` asked whether the project ships one at all, and
  [`ADR-0088`](../../docs/adr/ADR-0088-the-two-browser-proof-is-a-written-hand-check.md) answered on
  2026-08-28: **it does not.** No browser runner, and the automated ceiling stays socket-level on the
  JVM and jsdom-level in the client. This story is socket-level, like `EPIC-02`'s suite, and is now
  at that ceiling rather than waiting under an open decision.
- **New production code.** If this story needs any, that is a defect in `STORY-0501`–`STORY-0504`
  and becomes a ticket against the story that owes it, not scope here.
- **Performance, load or a ladder of ten thousand players.** No scale problem exists yet; inventing
  a benchmark here would be inventing a requirement.
