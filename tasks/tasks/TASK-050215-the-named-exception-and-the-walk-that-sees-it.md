---
schema: 2
id: TASK-050215
title: The named exception, asserted — the loser twice, the winner never, and a new walk that sees both
type: task
status: ready
parent: STORY-0502
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, http, db, leaderboard, paging, tests]
depends_on: [TASK-050214]
verify:
  - ./gradlew :poker-server:test --tests '*StandingsWalkDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`ADR-0066` §4's second refusal is pinned as what the endpoint **does**, not assumed away: a duel
committed after a page was drawn but stamped before the cutoff returns its loser a second time and
its winner not at all — and dropping the cursor shows both of them, immediately, with nothing in
between.

## Why this test asserts a defect on purpose

Exactly-once here is *"exactly once over the ladder as it was **committed at** the cutoff"*, and
`PostgresDuelResultSink` stamps `finished_at` when it begins recording, so a duel can commit after a
page was drawn while carrying an earlier stamp. `ADR-0066` §4 accepts both consequences and §9 asks
for this test in as many words: *"The test pins §4's second refusal so it is known rather than
discovered; if a later design removes the anomaly, this test fails and the sentence in
`docs/protocol.md` changes with it."*

A ticket that made this walk come out clean would be contradicting a merged ADR. `STORY-0408`'s
*total and disjoint* is **not** inherited and is not claimed anywhere in this file.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/http/StandingsWalkDatabaseTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresStandingsReads.kt` | read |
| `docs/adr/ADR-0066-the-ladder-is-computed-per-request-and-a-walk-is-pinned.md` | read |

## Scope

- Two tests added to the existing class, reusing **fixture B** and its id-ordered role assignment
  from `TASK-050214` — `w = byId[0]`, `x = byId[1]`, standings `a +2`, `x +1`, `w 0`, `e -1`,
  `d -2`.
- The only difference from `TASK-050214` is the stamp: the mid-walk duel finishes at
  `2026-08-20T08:59:00Z`, one minute **inside** the window, while the walk's cutoff is
  `2026-08-20T09:00:00Z`.
- The sequence, again exactly:
  1. record fixture B;
  2. read page one (`limit = 2`) — it holds `a` and `x`;
  3. record `w` beats `x`, stamped one minute before the cutoff;
  4. walk to the end from page one's `nextCursor`.
- No production file changes.

## Out of scope

- **Fixing the anomaly.** Removing it needs a snapshot held across requests, which `ADR-0066` §8
  refuses by name, and it would be a new decision rather than a ticket.
- **Asserting it cannot happen**, or asserting a walk is total and disjoint. Either would contradict
  the ADR this story implements.
- **Concurrency in the threading sense.** Two sequential requests with a write between them is the
  whole scenario.

## Tests

`StandingsWalkDatabaseTest`, `-PrequireDocker=true`, `limit = 2`.

After step 3 the pinned ladder is `a +2`, `w +1`, `x 0`, `e -1`, `d -2`. Page one's cursor names
`(coins = 1, id = x)`; `w` now sits at `(1, w)` with `w`'s id **above** `x`'s, so `w` is on the far
side of the cursor and the rest of the walk cannot reach it, while `x` has fallen to `0` and is
below the cursor again.

| Test | Proves |
| --- | --- |
| `aDuelCommittedAfterAPageWasDrawnReturnsItsLoserTwiceAndItsWinnerNever` | across the whole walk, `x`'s id appears **exactly twice** — once at `coins = 1` on page one and once at `coins = 0` afterwards — and `w`'s id appears **zero** times; `a`, `e` and `d` appear exactly once each |
| `aNewWalkSeesTheDuelTheOldWalkCouldNot` | immediately afterwards, with **no** cursor and no refresh, sweep, job or wait in between, a fresh walk over the same clock returns all five ids exactly once, with `w` at `coins = 1` and `x` at `coins = 0` |

**Named mutations.** Materialising the ladder, or refreshing it on a schedule, reddens the second
test — it is the criterion such a design fails. Holding one snapshot across the walk reddens the
first, which is the honest signal that the contract changed and that `docs/protocol.md` must change
with it.

## Acceptance criteria

- [ ] `StandingsWalkDatabaseTest.aDuelCommittedAfterAPageWasDrawnReturnsItsLoserTwiceAndItsWinnerNever`
      passes, asserting **two** appearances of the loser with the two different standings and
      **zero** appearances of the winner
- [ ] `StandingsWalkDatabaseTest.aNewWalkSeesTheDuelTheOldWalkCouldNot` passes, with the second walk
      started by dropping the cursor and nothing else
- [ ] The mid-walk duel is recorded after page one is read, stamped strictly before the cutoff
- [ ] No test in this file claims the walk is total and disjoint
- [ ] Every test already in `StandingsWalkDatabaseTest` passes with its assertions unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
