---
schema: 2
id: TASK-040702
title: A duel the recovered account will remember
type: task
status: done
parent: STORY-0407
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, coins, invariant]
depends_on: [TASK-040701]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`RecoveryOnAFreshBrowserTest` exists, boots the shipped composition against a real database, plays
one anonymous duel, and records what the two devices read afterwards — the coin and the history that
`STORY-0407`'s recovery has to find again from a browser that has never been seen.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | create |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` (the shape this file
follows — one helper, a record, one aspect per method),
`poker-server/src/test/kotlin/duels/poker/server/e2e/E2eServer.kt`,
`poker-server/src/test/kotlin/duels/poker/server/e2e/SocketDuel.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt`.

## Scope

- A new class `duels.poker.server.e2e.RecoveryOnAFreshBrowserTest`, `internal`, `@Timeout(120)`, with
  a `@BeforeEach` that calls `PostgresTestSupport.requireDocker()` and assigns
  `freshMigratedDatabase()` — the same four lines `IdentityMovesNoCoinTest.setup` uses.
- **One private `runRecovery(): RecoveryRecord` drives the whole arc, and every test method calls
  it** and asserts one aspect of the record it returns. No method re-derives the arc, and no method
  builds its own server. Inside `testApplication`, with `installDuelServer(dataSource)` and
  `createClient { install(WebSockets) }`.
- The steps this ticket adds to `runRecovery()`, in order, with
  `dataSource.assertCoinInvariantHolds("<step>")` **before the duel and after it** — two calls, two
  distinct step strings:
  1. `openSocketDuel()`, then `playToFinish()`.
  2. The winning seat is `checkNotNull(outcome.winner)` and the losing seat is `1 - winnerSeat`;
     `winner` and `loser` are `duel.seat(...)` of those. **Never seat `0`, never `clients.first()`.**
  3. `GET /api/me` for each device, recorded whole.
- The record is `private data class RecoveryRecord`, KDoc'd with one `@property` line per field. This
  ticket's fields: `winnerDeviceId`, `loserDeviceId`, `winnerProfileAfterDuel: ProfileResponse`,
  `loserProfileAfterDuel: ProfileResponse`. **Whole `ProfileResponse` values, not extracted fields** —
  later tickets in this story compare them for equality, and a record holding only `playerId` and
  `coinBalance` would silently narrow what those comparisons cover.
- One private `HttpClient` helper, copied from `IdentityMovesNoCoinTest` rather than shared, because
  a file-private top-level declaration in Kotlin is scoped to its own file:

  ```kotlin
  private suspend fun HttpClient.profileOf(deviceId: String, token: String? = null): ProfileResponse
  ```

  It sets `X-Device-Id` always, sets `Authorization: Bearer $token` only when `token` is non-null,
  asserts `200`, and decodes with `protocolJson`. **Declare the `token` parameter now, defaulted to
  `null`** — `TASK-040705` passes it, and adding a second near-identical helper later is the outcome
  this signature exists to prevent.
- **Declare no constant this ticket does not read.** detekt runs with `maxIssues: 0` and its default
  rule set, which reports an unused private top-level property, so the later tickets' fixture values
  are declared by the tickets that use them, not banked here.

## Out of scope

- The name, the sign-up and everything after — `TASK-040703` onwards, same file.
- Editing `IdentityMovesNoCoinTest.kt`, `SocketDuel.kt`, `E2eServer.kt` or `CoinInvariant.kt`. This
  file uses what they already expose and adds nothing to them.
- Any file under `poker-server/src/main`. Every behaviour this story asserts already ships; the story
  is a scenario, not a feature.
- A `theWholeArcMovesNoCoin` method per later ticket. There is exactly one such method in this class,
  added here; later tickets add `assertCoinInvariantHolds` **calls** to `runRecovery()` and state the
  new total in their own acceptance criteria, rather than a near-identical method each.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `theRecoveryArcMovesNoCoin` | Every `assertCoinInvariantHolds` call inside `runRecovery()` passes, in one run, in order. `ADR-0030` §5: a mint and a burn cancel, so this is asserted at every step and never only at the end |
| `theDuelPaidExactlyOneCoinEachWay` | After the duel the winner's `GET /api/me` reads `coinBalance` `1` and the loser's reads `-1`. **Two players, two different expected values** — with only the winner asserted, a sink that credited both would pass, and so would one that paid two coins each |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.theRecoveryArcMovesNoCoin` passes
- [ ] `RecoveryOnAFreshBrowserTest.theDuelPaidExactlyOneCoinEachWay` passes
- [ ] `runRecovery()` contains exactly two calls to `assertCoinInvariantHolds`, with two different
      step strings
- [ ] `theDuelPaidExactlyOneCoinEachWay` asserts `1` and `-1`, on two different players, from two
      separate `GET /api/me` responses
- [ ] The winning client is `duel.seat(checkNotNull(outcome.winner))`; the file contains none of
      `duel.seat(0)`, `clients[0]` or `clients.first()`
- [ ] `RecoveryRecord` holds `winnerProfileAfterDuel` and `loserProfileAfterDuel` as whole
      `ProfileResponse` values
- [ ] `profileOf` is declared with a `token: String? = null` parameter
- [ ] Both test methods call `runRecovery()` and neither builds its own server or duel
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/duel/CoinDeltas.kt`, double both non-draw
branches: `outcome.winner == 0 -> CoinDeltas(seat0 = 2, seat1 = -2)` and
`else -> CoinDeltas(seat0 = -2, seat1 = 2)`.

**`theDuelPaidExactlyOneCoinEachWay` reddens and `theRecoveryArcMovesNoCoin` stays green** — which is
the whole reason this ticket has two tests rather than one. Trace it: each player's `coin_balance` is
still the sum of their own `duel_result` deltas, so P1 holds; the two deltas still sum to zero and so
do the two balances, so P2 holds. The invariant is blind to a duel that pays the wrong *amount*, as
long as it pays symmetrically — only the two-input balance assertion sees it, and it fails with
*expected 1, got 2*. Revert.

A second mutation, for the invariant half. In `CoinDeltas.kt`, make the loser's delta `0`:
`outcome.winner == 0 -> CoinDeltas(seat0 = 1, seat1 = 0)`. **Both methods redden**, and that is the
expected shape rather than a defect in this Proof: `assertCoinInvariantHolds` is called inside
`runRecovery()`, so it throws at the *"after the duel"* call before either method reaches its own
assertion. The discriminating output is the message, which names that step and reports P2 violated
with `SUM(player.coin_balance) = 1`. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why a second scenario file rather than more of `IdentityMovesNoCoinTest`.** That file drives
`ADR-0030` §5's twelve-step scenario and holds
`everyApiPathInTheRouteSourcesIsExercisedByTheScenario`, whose `SCENARIO_ENDPOINTS` is a fact about
*that* arc. Adding a recovery arc to it would put every ticket in this story over `S` and would
perturb a gate that has nothing to do with recovery. `STORY-0407` adds no route, so that test is
unaffected by this file's existence and must stay unaffected.

**One value across a whole story is how a hard-coded seat survives.** `STORY-0213` shipped a
hard-coded seat `0` that passed eight of nine tests. Here the winner is whichever seat the engine
adjudicated, and the loser is the other one; the story's later tickets all hang off `winner`, so a
seat assumed once would be assumed everywhere.

**The Proof's second mutation is wrong, and the substitute was right.** This ticket says to mutate
the `outcome.winner == 0` branch of `CoinDeltas`. For this fixture's seeds that branch never fires:
`SocketLadderTest`'s KDoc already records, as an established fact about `HAND_SEED`/`POLICY_SEED`,
that seat 1 — the `GUEST_DEVICE` client — wins in nine hands. Applied literally the mutation leaves
both tests green and proves nothing. The coder applied the same intent to the arm that does fire
(`else -> CoinDeltas(seat0 = 0, seat1 = 1)`) and both methods redden inside `runRecovery()`'s "after
the duel" checkpoint with `[after the duel] P2 violated (ADR-0030 §5, global): SUM(player.coin_balance)
= 1, SUM(duel_result.coin_delta) = 1`. The reviewer reproduced all three runs independently rather
than accepting the report. Eleventh wrong `## Proof` in this run.

**A fixed seed turns the other arm into dead code.** The Proof was written as though either seat
might win. It cannot: the seeds are fixed and a merged test already documented which seat they
favour. A mutation aimed at the arm that never fires is untestable by construction and reads exactly
like one that works — which is why the instruction to *run* the Proof rather than trust it is the
part that caught this.

**`loserDeviceId` is populated and asserted by nothing.** `RecoveryRecord`'s two device-id fields
appear in this ticket only inside failure-message interpolation. `TASK-040704` pins `winnerDeviceId`
via `theOriginalDevicesWelcomeStillCarriesItsDeviceId`; no ticket in the story reads `loserDeviceId`.
An edit inside `runRecovery()` that swapped which device id belongs to which player, while leaving
the `ProfileResponse` objects correctly assigned, would pass every test in this file. Recorded rather
than filed because `TASK-040705` and `TASK-040707` may yet read it; if the story closes with it still
unread, it becomes a ticket then.
