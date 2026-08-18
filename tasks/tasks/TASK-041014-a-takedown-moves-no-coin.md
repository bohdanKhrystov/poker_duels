---
schema: 2
id: TASK-041014
title: A takedown moves no coin
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, moderation, invariant]
depends_on: [TASK-041013]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RetireDisplayNameTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0030` §5's P1 and P2 hold across a takedown, and the `player` row the takedown wrote differs
from its previous self in `display_name` and nothing else.

## Why a whole ticket for this

`ADR-0051` §7 adds a **fourth** writer of `player` where `ADR-0049` §7 had held the count at three,
on the argument that *"an identity operation issuing any `UPDATE player` is one careless `SET` away
from the coin balance."* The fourth writer is a function nobody in this repository can review by
reading Kotlin. This is the assertion that stands in for that review, and it is the story's own
acceptance criterion.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/RetireDisplayNameTest.kt` | modify — one test and three private helpers |
| `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` | read — `p1BrokenBalanceCount`, `p2LedgerSums`, `assertFixtureTook`, which are `private` there and must be re-derived here rather than made shared |
| `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` | read — §5 |

## Scope

- One test in the class `TASK-041012` created, plus copies of P1 and P2 written with **the exact SQL
  `ADR-0030` §5 gives**. Do not paraphrase the two statements and do not "simplify" the `LEFT JOIN`
  in P1 — the ADR gives them as text so that every copy in the repository is the same query.
- Making those helpers shared across test files is **not** in scope: they are `private` in
  `SignUpDatabaseTest.kt` today, moving them is a second diff in a file this story does not own, and
  a second literal copy of a five-line query the ADR publishes is the cheaper mistake.
- The fixture must be non-trivial: **both properties hold on an empty database**, which is the trap
  `SignUpDatabaseTest.assertFixtureTook` exists for. So the test plays a duel first and asserts the
  balances took (`+1` / `-1`, two `duel_result` rows) before touching the name.

## Out of scope

- Every other behaviour of `retire_display_name` — `TASK-041012`.
- Widening `SignUpDatabaseTest`'s scenario test to include a takedown. That file drives the identity
  flow and a takedown is not part of it.

## Tests

`RetireDisplayNameTest`, `-PrequireDocker=true`. One test added; the six from `TASK-041012` are not
edited.

| Test | Proves |
| --- | --- |
| `aTakedownMovesNoCoin` | Alice beats Bob, so alice is `+1` and bob is `-1` and P1/P2 both hold. Alice sets `"Ann"`. `retire_display_name(alice, 'Ann')`. Afterwards: **P1 returns zero rows**, **P2's two sums are both `0`**, `alice.coin_balance` is still `1`, `bob.coin_balance` is still `-1`, and `SELECT count(*) FROM duel_result` is still `2`. **The wrong implementations this must fail against**: a function whose `UPDATE player` carried a second `SET`, and a fixture that never played a duel — the latter is why the `+1`/`-1` assertion runs *before* the takedown as well as after |

The test asserts the two balances **by value** as defence in depth, not because P1 and P2 miss
something specific — measured against a takedown that also decrements `coin_balance`, P1, P2 and the
by-value check all catch it, P1 first. What actually makes this test non-vacuous is the **fixture**:
P1 and P2 both hold on a database with no rows at all, so the duel and the `+1`/`-1` assertion
*before* the takedown are what stop it passing against nothing.

## Acceptance criteria

- [ ] `RetireDisplayNameTest.aTakedownMovesNoCoin` passes
- [ ] It asserts `+1` / `-1` and two `duel_result` rows **before** the takedown
- [ ] It asserts, after the takedown: P1 returns zero rows, P2's two sums are both `0`,
      `alice.coin_balance == 1`, `bob.coin_balance == -1`, and `duel_result` still has two rows
- [ ] The P1 and P2 statements in this file are character-identical to `ADR-0030` §5's, modulo
      indentation
- [ ] The six tests `TASK-041012` added still pass, unedited
- [ ] `SignUpDatabaseTest.kt` is not modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
