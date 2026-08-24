---
schema: 2
id: TASK-040618
title: The scenario, steps one to four — anonymous, a duel, a name, an account
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, coins, invariant]
depends_on: [TASK-040617]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.IdentityMovesNoCoinTest' -PrequireDocker=true
---

## Goal

The first four steps of `ADR-0030` §5's scenario run against a real database and the shipped
composition, with P1 and P2 asserted after **every** step and the `player` multiset asserted
byte-identical across the identity operations among them.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/SocketCoinsTest.kt` (the socket-plus-HTTP shape
this file copies), `poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt`,
`poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt`,
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §5.

## Scope

- A new class, `duels.poker.server.e2e.IdentityMovesNoCoinTest`, `@Timeout(120)`, using
  `freshMigratedDatabase()`, `installDuelServer(dataSource)` and `createClient { install(WebSockets) }`
  exactly as `SocketCoinsTest` does.
- **One private `runScenario(): ScenarioRecord` helper drives every step, and every test method
  calls it.** The record carries what each step observed — the host's and guest's player ids, the
  balances read after step 2, and the snapshot pairs — so each test asserts one aspect of one run
  rather than five methods each re-deriving the whole scenario. `assertCoinInvariantHolds` is called
  **inside** the helper, which means a coin that moves mid-scenario reddens every method in the
  class; the step string in the failure message is what says where, and that is exactly why the
  helper takes one.
- The four steps, inside `runScenario()`, in this order, with
  `dataSource.assertCoinInvariantHolds("<step>")` called **before the first step and after each
  one** — five calls, five distinct step strings. `ADR-0030` §5: a mint and a burn cancel, so
  asserting only at the end is not the same test.
  1. **Connect anonymously.** `openSocketDuel()` opens both clients; the host's `Welcome` is step 1.
  2. **Play a duel and win.** `playToFinish()`, and the winner is read from
     `checkNotNull(outcome.winner)`, never assumed to be seat `0`. The winner's `GET /api/me` reads
     `coinBalance` `1` and the loser's reads `-1` — **two players, two different expected values.**
  3. **Set a name.** `PUT /api/me/name` for the winner's device, `200`.
  4. **Sign up.** `POST /api/auth/sign-up` for the same device, `201`.
- Private `HttpClient` helpers copied from `SocketCoinsTest` and `SignInDatabaseTest` as needed:
  `profileOf(deviceId)`, `setName(deviceId, name)`, `signUp(deviceId, handle, password)`. Private
  top-level in Kotlin is per-file, so these are copies, not a shared extraction.
- **The `player` multiset**, via `playerTableSnapshot()`:
  - across **step 4**, before and after: byte-identical, with no exception at all. `ADR-0049` §7 and
    `ADR-0030` §2 make this structural — sign-up writes `credential` only.
  - across **step 3**, before and after: identical **except** for one row, and within that row
    exactly one column — the `display_name` of the player being renamed. Assert this by comparing
    row counts, then comparing every row that is not the renamed player's for equality, then
    asserting that the renamed player's row differs in exactly one position.

## Out of scope

- Steps five to eleven — `TASK-040620`, same file.
- Revocation — `TASK-040621`, same file.
- Any file under `poker-server/src/main`, and any file under `e2e/` other than the new one.
- The name-registry rules. `PUT /api/me/name` is a step here, not a subject; `STORY-0410` owns what
  a legal name is.

## Tests

`IdentityMovesNoCoinTest`

| Test | Proves |
| --- | --- |
| `theFirstFourStepsMoveNoCoin` | The five `assertCoinInvariantHolds` calls all pass, in one run, in order. This is the story's first acceptance criterion for the first half of the scenario |
| `theDuelPaidExactlyOneCoinEachWay` | After step 2, the winner reads `1` and the loser reads `-1` over `GET /api/me`. **The two-input rule**: with only the winner asserted, a sink that credited both players would pass |
| `signingUpLeavesThePlayerTableByteIdentical` | The step-4 snapshots are equal. The story's second acceptance criterion, for sign-up |
| `settingANameTouchesOneRowAndOneColumn` | The step-3 snapshots differ in exactly one row and, within it, exactly one column, and every other row is equal. `ADR-0030` §5's *"the single exception of the one row and one column a rename is permitted to touch"* |
| `theInvariantWasAlreadyRunningBeforeTheFirstStep` | The step-0 call runs against an empty database and passes. Named separately so the five-call sequence cannot be quietly reduced to four |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `theFirstFourStepsMoveNoCoin` contains exactly five calls to `assertCoinInvariantHolds`, with
      five different step strings
- [ ] The winning client is `duel.seat(checkNotNull(outcome.winner))`; the file contains neither
      `duel.seat(0)` nor `clients[0]` nor `clients.first()`
- [ ] `theDuelPaidExactlyOneCoinEachWay` asserts `1` and `-1`, on two different players
- [ ] `settingANameTouchesOneRowAndOneColumn` asserts equality for every unrenamed row and a
      single-position difference for the renamed one — it does not merely assert the two snapshots
      differ
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Add `UPDATE player SET coin_balance = coin_balance + 1 WHERE id = <the winner>` inside
`runScenario()`, immediately **after** step 3's `assertCoinInvariantHolds` call and before step 4.

**Every one of the five test methods reddens**, and that is the expected shape rather than a defect
in the Proof: they all drive the same helper, and the helper throws at the step-4 call before any of
them reaches its own assertion. The discriminating output is the message, which names
`"after signing up"` — P1 fires because that player's balance no longer equals their deltas, and P2
because both global sums move to `1`. Revert.

A second mutation, for the multiset half, and this one reddens exactly one method. In
`PostgresProfileWrites`, change `SET_NAME_SQL` from `UPDATE player SET display_name = ?` to
`UPDATE player SET display_name = ?, created_at = now()`. No coin moves, so nothing throws and
`theFirstFourStepsMoveNoCoin` stays green; `theDuelPaidExactlyOneCoinEachWay` and
`signingUpLeavesThePlayerTableByteIdentical` are untouched. **Only
`settingANameTouchesOneRowAndOneColumn` reddens**, because the renamed player's row now differs in
two columns rather than one — which is precisely what a bare "the two snapshots differ" assertion
would have missed. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
