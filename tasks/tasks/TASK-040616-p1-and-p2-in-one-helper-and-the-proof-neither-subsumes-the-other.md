---
schema: 2
id: TASK-040616
title: P1 and P2 in one helper, and the proof that neither subsumes the other
type: task
status: ready
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [server, db, coins, invariant, test-support]
depends_on: [TASK-040615]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.CoinInvariantTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

One shared test helper asserts `ADR-0030` §5's two coin properties against the live schema, and its
own tests prove that dropping either one stops detecting a shape the other never saw.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariantTest.kt` | create |

Read, and do not edit:
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §5 (the two SQL statements,
verbatim), `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` (its private
`p1BrokenBalanceCount` and `p2LedgerSums`, which this file generalises).

## Scope

- A file of **top-level functions only** — no class — so ktlint's filename rule does not apply and
  every database suite can reach it:

  ```kotlin
  internal fun DataSource.assertCoinInvariantHolds(step: String)
  internal fun DataSource.playerTableSnapshot(): List<List<Any?>>
  ```

- `assertCoinInvariantHolds` runs **both** properties and fails naming [step], so a scenario that
  calls it ten times says which step broke:
  - **P1**, the exact statement `ADR-0030` §5 gives, asserted to return **zero rows**, and the
    failure message lists the offending player ids.
  - **P2**, the exact statement `ADR-0030` §5 gives, asserted to be `(0, 0)`, and the failure
    message prints both sums.
- **Both, always, in one function.** `ADR-0030` §5 and this story's design note both say P1 does not
  subsume P2; a helper that ran only P1 would look equivalent and would silently stop detecting the
  shape `theGlobalSumCatchesWhatThePerPlayerCheckCannot` below constructs. The KDoc says this, and
  cites the two tests by name.
- `playerTableSnapshot` reads **every column of every `player` row ordered by `id`**, taking column
  names and count from `java.sql.ResultSetMetaData` rather than a hard-coded list, so it keeps
  comparing correctly across a migration that adds or drops one — `SignUpDatabaseTest.snapshot` is
  the shape to copy and the reason to copy it.
- A private fixture helper inside the **test** file writes one finished duel: a `duel` row, **two**
  `duel_result` rows of `+1` and `−1`, and the matching `coin_balance` update on both players,
  mirroring `PostgresDuelResultStore.record`. A fixture writing only the winner's row breaks P2
  before anything under test runs.

## What is total, and what is not — say it plainly

P1 and P2 are total over `player` and `duel_result`: **any** statement, from any endpoint written by
anyone, that leaves a balance disagreeing with its deltas or moves the global sums off zero trips
them, without that endpoint's author having heard of this file. That is why they are properties and
not per-endpoint assertions.

What they do **not** catch, and the ticket says so rather than implying otherwise:

- **A coin column on a new table.** P1 and P2 name `player.coin_balance` and
  `duel_result.coin_delta` and nothing else. A future `season_award` table with its own balance is
  invisible to both, and adding one obliges its author to widen these two statements.
- **An endpoint the scenario never calls.** These are assertions over a *state*, so somebody has to
  reach that state. `TASK-040620` closes exactly this gap with a test that enumerates every `/api/`
  path literal in the route sources and fails if the scenario does not exercise it.

## Out of scope

- Editing `SignUpDatabaseTest` to use the helper — `TASK-040617`, deliberately its own diff.
- `SignInDatabaseTest` and its `snapshot`. It has no P1/P2 of its own and gains none here.
- The scenario — `TASK-040618` onward.
- Anything under `poker-server/src/main`.

## Tests

`CoinInvariantTest`

| Test | Proves |
| --- | --- |
| `aCorrectLedgerPasses` | The positive control. Two players, one finished duel, balances `+1` and `−1`: `assertCoinInvariantHolds("control")` does not throw. Without this, all four failure tests below pass against a helper that throws unconditionally |
| `anEmptyDatabasePasses` | No players and no duels: the helper does not throw. Both statements `COALESCE` to `0`, and a scenario asserts the invariant *before* its first step |
| `thePerPlayerCheckCatchesACoinMovedBetweenBalances` | From the correct fixture, `UPDATE player SET coin_balance = 2 WHERE id = winner` and `coin_balance = -2 WHERE id = loser`. The two global sums are **still both zero**, so P2 holds; P1 returns two rows and the helper throws. This is the half P2 cannot see |
| `theGlobalSumCatchesWhatThePerPlayerCheckCannot` | From the correct fixture, delete the **loser's** `duel_result` row and set their `coin_balance` back to `0` — both together. That player is now internally consistent, so P1 returns **zero rows** and holds; P2's two sums both read `1` and the helper throws. This is the half P1 cannot see, and it is the exact shape this story's design note says a careless consolidation stops detecting |
| `theFailureMessageNamesTheStep` | `assertCoinInvariantHolds("after sign-up")` on a broken ledger throws with `"after sign-up"` in the message. A scenario calling this ten times is unusable without it |
| `theSnapshotSeesEveryColumnAndDetectsAChangedOne` | `playerTableSnapshot()` before and after `UPDATE player SET coin_balance = coin_balance + 1` are **unequal**, and before and after a statement that writes nothing are **equal**. **Two comparisons with opposite expected outcomes** — an equality that always fails and one that always passes are both useless, and only the pair rules them out |

## Acceptance criteria

- [ ] All six test methods above pass
- [ ] `assertCoinInvariantHolds` issues both statements on every call — neither is behind a
      parameter, a flag or a branch
- [ ] `thePerPlayerCheckCatchesACoinMovedBetweenBalances` asserts, before breaking anything, that its
      mutation leaves both P2 sums at zero, so its claim about which property fires is checked rather
      than asserted in prose
- [ ] `theGlobalSumCatchesWhatThePerPlayerCheckCannot` asserts that P1's statement returns zero rows
      after its mutation, for the same reason
- [ ] `playerTableSnapshot` contains no hard-coded column name
- [ ] Nothing under `poker-server/src/main` is modified, and `SignUpDatabaseTest.kt` is unmodified
- [ ] Every command in `verify:` exits 0

## Proof

Delete the P2 half of `assertCoinInvariantHolds`, leaving P1.
**`theGlobalSumCatchesWhatThePerPlayerCheckCannot` reddens, and it is the only test that does** —
`aCorrectLedgerPasses`, `anEmptyDatabasePasses` and `theSnapshotSeesEveryColumnAndDetectsAChangedOne`
never depended on P2, and `thePerPlayerCheckCatchesACoinMovedBetweenBalances` is caught by P1 alone
by construction. `theFailureMessageNamesTheStep` stays green if it breaks the ledger in P1's way;
write it that way.

Then restore P2 and delete P1 instead: **`thePerPlayerCheckCatchesACoinMovedBetweenBalances` reddens,
and so does `theFailureMessageNamesTheStep`** — it breaks the ledger in P1's way by construction, so
deleting P1 stops it throwing at all. Two tests, and the second one is a consequence of how it is
written rather than of what it proves.

The two mutations reddening two *different* criterion tests — one each — is the whole claim of this
ticket, and it is the claim `ADR-0030` §5 and this story's design note both make in prose.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
