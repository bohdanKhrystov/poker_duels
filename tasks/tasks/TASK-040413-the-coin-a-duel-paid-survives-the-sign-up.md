---
schema: 2
id: TASK-040413
title: The coin a duel paid is still there after the sign-up
type: task
status: backlog
parent: STORY-0404
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, auth, db, coins]
depends_on: [TASK-040412]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SignUpDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

A player who won a duel before signing up reads that coin back afterwards, and the ledger sums that
`ADR-0030` §5 states as properties hold on both sides of the sign-up.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` | modify — add tests and one fixture helper |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresDuelResultStore.kt` | read — the columns a finished duel writes |
| `docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` | read — §5, which gives P1 and P2 as SQL |

## Scope

- A fixture helper that writes one finished duel with raw SQL: one `duel` row, **two**
  `duel_result` rows of `+1` and `−1`, and the matching `coin_balance` update on both players.
  **A fixture that writes only the winner's row makes P2 fail before the sign-up even happens**, for
  a reason that is not sign-up's fault, and the failure would look like a defect in the endpoint.
- P1 and P2 as `ADR-0030` §5 writes them, run as raw SQL immediately before and immediately after
  the sign-up request:
  - **P1** — `SELECT p.id FROM player p LEFT JOIN duel_result r ON r.player_id = p.id GROUP BY p.id, p.coin_balance HAVING p.coin_balance <> COALESCE(SUM(r.coin_delta), 0)` returns zero rows.
  - **P2** — `SUM(player.coin_balance)` and `SUM(duel_result.coin_delta)` are both `0`.
- Asserting after only is not enough and asserting the delta is not enough: a mint and a burn
  cancel. Both properties are checked at both moments.

## Out of scope

- Making P1/P2 a shared fixture used by every identity operation, and the whole-flow scenario test —
  `STORY-0406`, per `ADR-0030` §5 and `EPIC-04`'s story table.
- The rest of the identity flow: sign-in, sign-out, revocation. None exists yet.
- Reading the balance through `PostgresProfileReads` in place of `GET /api/me`. The criterion is
  what the player reads, so it goes over HTTP.

## Tests

`SignUpDatabaseTest`, added to the class `TASK-040412` created.

| Test | Proves |
| --- | --- |
| `theWinnersCoinIsStillThereAfterSigningUp` | the winner's `GET /api/me` reports `coinBalance` `1` before the sign-up and `1` after it, and the loser's reports `-1` both times. **Two players, two signs**, because a balance read as `0` on both sides would pass a test that only checked *unchanged* |
| `theLedgerSumsAreUnchangedByASignUp` | P2 holds before and after: both sums are `0`, asserted at both moments |
| `everyPlayersBalanceStillEqualsTheirDeltas` | P1 returns zero rows before and after. **The wrong implementation this must fail against is any `UPDATE player SET coin_balance`, and the `duel_result` repointing `ADR-0030` names**, both of which leave a row whose balance no longer matches its deltas |
| `theWinnerReadsTheSameDuelLineAfterSigningUp` | `GET /api/me/duels` returns the same single duel, with the same `duelId`, the same `opponentPlayerId` and the same `coinDelta`, before and after — the opponent-side double-count `ADR-0030` describes shows up here as two lines where there was one |

## Acceptance criteria

- [ ] All four tests above pass
- [ ] The fixture writes **two** `duel_result` rows and updates **both** players' `coin_balance`,
      so P1 and P2 hold before the sign-up
- [ ] Each of `theLedgerSumsAreUnchangedByASignUp` and `everyPlayersBalanceStillEqualsTheirDeltas`
      asserts **before and after**, never after alone
- [ ] `theWinnersCoinIsStillThereAfterSigningUp` asserts `1` for the winner and `-1` for the loser,
      so a balance of `0` cannot satisfy it
- [ ] `theWinnerReadsTheSameDuelLineAfterSigningUp` asserts the duel list has size `1` after the
      sign-up, not merely that it is non-empty
- [ ] The five tests `TASK-040412` wrote pass unchanged
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
