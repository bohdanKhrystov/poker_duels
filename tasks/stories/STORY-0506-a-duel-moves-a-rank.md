---
id: STORY-0506
title: A duel moves a rank, end to end
type: story
status: blocked
parent: EPIC-05
module: poker-server
labels: [server, leaderboard, end-to-end]
depends_on: [STORY-0503, STORY-0505]
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

- **The fixture satisfies whatever `DEC-056` requires for a place.** If a threshold gates the
  ladder, two players who have duelled once are not on it, and the test plays enough duels to clear
  the threshold rather than quietly asserting against an empty page. That is a fixture detail, not a
  change to any criterion below — every one of them is a *difference*, and a difference survives a
  threshold.

**Not blocked on any decision.** It is blocked on `STORY-0503` and `STORY-0505` existing, which is
a different thing — and it is the reason the epic can be scheduled the moment `DEC-055` is answered
rather than re-planned.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Not split. Blocked on `STORY-0503` and `STORY-0505` merging — run `/plan-story STORY-0506` then.* | — |

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
- [ ] The ladder read after the duel agrees with `GET /api/me`'s `coinBalance` for both players.
- [ ] The test runs under `-PrequireDocker=true` and fails, rather than skips, when Docker is
      absent.
- [ ] `./gradlew :poker-engine:check` passes with no change to `poker-engine`.

## Out of scope

- **A browser end-to-end test.** `DEC-024` — the architect's, open since `EPIC-03` — asks whether
  the project ships one at all. This story is socket-level, like `EPIC-02`'s suite, and inherits
  whatever `DEC-024` decides rather than pre-empting it.
- **New production code.** If this story needs any, that is a defect in `STORY-0501`–`STORY-0505`
  and becomes a ticket against the story that owes it, not scope here.
- **Performance, load or a ladder of ten thousand players.** No scale problem exists yet; inventing
  a benchmark here would be inventing a requirement.
