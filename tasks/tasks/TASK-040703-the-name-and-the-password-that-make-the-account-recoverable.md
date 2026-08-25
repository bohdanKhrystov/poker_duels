---
schema: 2
id: TASK-040703
title: The name and the password that make the account recoverable
type: task
status: backlog
parent: STORY-0407
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, e2e, auth]
depends_on: [TASK-040702]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

The duel winner of `TASK-040702` sets a display name and signs up, so the arc holds an account with a
coin, a name and a history — the three things `STORY-0407`'s fresh browser must find again.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` (its `setName` and
`signUp` helpers are the ones to copy).

## Scope

- Two steps appended to `runRecovery()`, after the duel and before the record is built:
  1. `PUT /api/me/name` for **the winner's device**, body `{"name":"$RECOVERED_NAME"}`, asserted
     `200 OK` inside the helper.
  2. `POST /api/auth/sign-up` for **the winner's device**, `RECOVERY_HANDLE` and
     `RECOVERY_PASSWORD`, asserted `201 Created` inside the helper.
- `dataSource.assertCoinInvariantHolds(...)` after each of the two, with two new distinct step
  strings. `runRecovery()` then holds four calls in total.
- The loser's device does **not** set a name and does **not** sign up. That is deliberate and is what
  makes this ticket's test a two-input one: after this step the winner's `displayName` is
  `RECOVERED_NAME` and the loser's is `null`.
- The three file-private constants this ticket reads, declared here because it is the first ticket to
  read them: `RECOVERED_NAME = "Champion"`, `RECOVERY_HANDLE = "Recovered_1"`,
  `RECOVERY_PASSWORD = "password1"`. Nothing declares a constant it does not use — detekt runs with
  `maxIssues: 0` and reports an unused private top-level property.
- Two private `HttpClient` helpers copied from `IdentityMovesNoCoinTest` — `setName(deviceId, name)`
  returning the status, `signUp(deviceId, handle, password)` returning the status. Copies, not an
  extraction: a file-private top-level declaration in Kotlin is scoped to the file that declares it.
- Two new `RecoveryRecord` fields, each with its own `@property` line: `originalProfile` — the
  winner's `GET /api/me` read **after** the sign-up, the value the whole story compares against — and
  `loserProfile`, the loser's, read at the same moment.

  `loserProfile` is **not** a duplicate of `TASK-040702`'s `loserProfileAfterDuel`, and the difference
  is the whole assertion: that one was read before the rename, so it cannot say the loser is *still*
  nameless afterwards. Read both, keep both.

## Out of scope

- The fresh browser, the sign-in and the token — `TASK-040704`, same file.
- Asserting anything about what a legal handle, password or display name is. `STORY-0403`,
  `STORY-0404` and `STORY-0410` own those rules; here the three values are fixture, and the only
  thing asserted about them is the status each endpoint answered.
- A second sign-up on the same device, and the `409` it answers. That is `STORY-0406`'s claim guard,
  already gated by `SignUpRouteTest`, and it is not what recovery is about.
- Any file under `poker-server/src/main`.
- Adding another `…MovesNoCoin` method. `theRecoveryArcMovesNoCoin` already covers every call this
  ticket adds, because the calls live inside the helper it drives.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `theWinnerIsNamedAndTheLoserIsNot` | `originalProfile.displayName` is `RECOVERED_NAME` and `loserProfile.displayName` is `null`. **Two inputs, two different expected values** — asserting only the winner's name would pass against a read path that returned the same name for every player |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.theWinnerIsNamedAndTheLoserIsNot` passes
- [ ] `theRecoveryArcMovesNoCoin` still passes, now over four `assertCoinInvariantHolds` calls
- [ ] `theDuelPaidExactlyOneCoinEachWay` still passes, with its assertions unchanged
- [ ] `runRecovery()` contains exactly four calls to `assertCoinInvariantHolds`, with four different
      step strings
- [ ] `runRecovery()` asserts `200 OK` from `PUT /api/me/name` and `201 Created` from
      `POST /api/auth/sign-up`
- [ ] `theWinnerIsNamedAndTheLoserIsNot` asserts a non-null name on one player and `null` on another
- [ ] The loser's device is never passed to `setName` or `signUp`
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `runRecovery()`, pass `loser.deviceId` to `setName` instead of `winner.deviceId`, changing nothing
else.

**`theWinnerIsNamedAndTheLoserIsNot` reddens, and it is the only method that does.** Trace it: the
rename now lands on the loser's row, so `originalProfile.displayName` is `null` — the first assertion
fails with *expected Champion, got null*. Nothing else moves: `PUT /api/me/name` still answers `200`
for a device that has a profile, so `runRecovery()` completes; a rename writes no `duel_result` row
and no balance, so all four invariant calls still pass and `theRecoveryArcMovesNoCoin` stays green;
and `theDuelPaidExactlyOneCoinEachWay` reads `coinBalance` only, which a rename does not touch.
Revert.

**Run it.** This is the mutation that shows the assertion pair cannot be satisfied by a read path
returning one constant name for everybody — the failure `TASK-040620`'s reviewer had to construct by
hand, and the reason the loser is deliberately left nameless.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**`originalProfile` is read after the sign-up, not before.** Every later ticket compares what the
fresh browser reads against *this* value, so it has to be the profile as it stands the instant before
recovery begins — name set, credential attached, coin won.
