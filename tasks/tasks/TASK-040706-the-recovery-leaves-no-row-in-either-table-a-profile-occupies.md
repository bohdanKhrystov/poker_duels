---
schema: 2
id: TASK-040706
title: The recovery leaves no row in either table a profile occupies
type: task
status: ready
parent: STORY-0407
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, e2e, auth, invariant]
depends_on: [TASK-040705]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.RecoveryOnAFreshBrowserTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`STORY-0407`'s central negative becomes a gate: signing in from a browser that has never been seen,
opening a socket with it and reading back through it adds **no `player` row and no `device_binding`
row** — asserted over both whole tables, not eyeballed.

## Why both tables

After `ADR-0049` a profile occupies two of them. `V7__device_binding.sql` dropped
`player.device_id`, so the device→profile edge is a `device_binding` row, and a **rebinding** — an
insert naming an existing player — adds no `player` row at all. A `player`-only check is byte-identical
across it and says nothing. `TASK-040701` moved the snapshot for the second table into
`CoinInvariant.kt` for exactly this ticket.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/RecoveryOnAFreshBrowserTest.kt` | modify |

Read, and do not edit:
`poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt`,
`poker-server/src/main/resources/db/migration/V7__device_binding.sql`,
`docs/adr/ADR-0030-a-claim-adds-a-credential-and-moves-nothing.md` §2.

## Scope

- **Open the bracket** immediately after the sign-up and after `originalProfile`/`originalDuels` are
  read, and **before** the original device's control handshake: `playerBeforeRecovery =
  dataSource.playerTableSnapshot()` and `deviceBindingBeforeRecovery =
  dataSource.deviceBindingTableSnapshot()`.
- **Close it** immediately after `recoveredDuels` is read, with the matching `…AfterRecovery` pair.
  Everything the fresh browser does — the sign-in, the handshake, both reads — is inside, and so is
  the original device's control handshake, which is correct: that handshake presents a device that
  already has a live binding, so `IdentityResolver` answers `Identity.Device` and the directory is
  never asked to create anything.
- Two counts, taken once the bracket has closed:
  `freshDeviceLiveBindings = dataSource.liveBindingCountFor(FRESH_DEVICE)` and
  `originalDeviceLiveBindings = dataSource.liveBindingCountFor(winner.deviceId)`.
- One private helper at the bottom of the file, with a KDoc saying it has one use site and would move
  to `CoinInvariant.kt` on a second:

  ```kotlin
  private fun DataSource.liveBindingCountFor(deviceId: String): Int
  ```

  `SELECT count(*) FROM device_binding WHERE device_id = ? AND revoked_at IS NULL`, run on a
  `PreparedStatement`, returning the count. **A count, not a boolean** — "there is no binding" and
  "there is exactly one" are different claims and both are asserted.
- Six new `RecoveryRecord` fields with `@property` lines: the four snapshots and the two counts.

## Out of scope

- `auth_session`. The sign-in writes exactly one row there and is *supposed* to — that table is
  `TASK-040707`'s and `TASK-040708`'s instrument, and asserting it unchanged here would assert the
  opposite of `ADR-0030` §2.
- Sign-out, and what the fresh browser becomes afterwards — `TASK-040708`, same file. Its steps run
  **after** this bracket closes, so they cannot perturb it.
- `deviceBindingColumnNames`, and naming which column moved. Nothing here is expected to move at all;
  the assertion is whole-table equality.
- Any file under `poker-server/src/main`.

## Tests

`RecoveryOnAFreshBrowserTest`

| Test | Proves |
| --- | --- |
| `theRecoveryCreatesNoPlayerRow` | `playerBeforeRecovery` equals `playerAfterRecovery`, every column of every row. `ADR-0027` path 1 short-circuits before path 3's minting, so recovery litters no orphan profile |
| `theRecoveryCreatesNoDeviceBinding` | `deviceBindingBeforeRecovery` equals `deviceBindingAfterRecovery`. The half a `player`-only check cannot see: a rebinding adds a row here and none there |
| `theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne` | `liveBindingCountFor(FRESH_DEVICE)` is `0` and `liveBindingCountFor(winner.deviceId)` is `1`. **Two inputs, two different expected values** — a helper that answered `0` for everything would satisfy the first assertion for free |

## Acceptance criteria

- [ ] `RecoveryOnAFreshBrowserTest.theRecoveryCreatesNoPlayerRow` passes
- [ ] `RecoveryOnAFreshBrowserTest.theRecoveryCreatesNoDeviceBinding` passes
- [ ] `RecoveryOnAFreshBrowserTest.theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne` passes
- [ ] The two "before" snapshots are taken before the original device's control handshake, and the
      two "after" snapshots after `recoveredDuels` is read
- [ ] The two snapshot assertions compare whole tables with `assertEquals`, and neither compares only
      `size`
- [ ] `theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne` asserts `0` for one device and `1` for
      another, from two separate `liveBindingCountFor` calls
- [ ] `liveBindingCountFor` returns an `Int` and its SQL contains `revoked_at IS NULL`
- [ ] `deviceBindingTableSnapshot` is imported from `duels.poker.server.db` and not redeclared in
      this file
- [ ] `runRecovery()` still contains exactly seven calls to `assertCoinInvariantHolds`
- [ ] Every test method added by `TASK-040702` through `TASK-040705` still passes with its assertions
      unchanged
- [ ] The diff touches exactly one file, and it is the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In `poker-server/src/main/kotlin/duels/poker/server/DuelSocket.kt`, make the `Identity.Session` arm
resolve the device beside the session — the orphan-minting defect this story exists to forbid:

```kotlin
is Identity.Session -> {
    hello.deviceId?.let { deps.directory.resolve(DeviceId(it)) }
    Player(identity.playerId, SESSION_PLACEHOLDER_DEVICE_ID) to
        ServerMessage.Welcome(playerId = identity.playerId.value, deviceId = null)
}
```

`DeviceId` is already imported in that file. **All three methods above redden, and nothing else in
the class does.** Trace it, because the last part is the point:

- `theRecoveryCreatesNoPlayerRow` — one minted row appears inside the bracket.
- `theRecoveryCreatesNoDeviceBinding` — the same statement inserts its binding.
- `theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne` — the first count is now `1`, not `0`.
- `theRecoveryArcMovesNoCoin` stays **green**: the minted player has `coin_balance` `0` and no
  `duel_result` rows, so P1 holds for it and both P2 sums are unmoved. **The coin invariant cannot
  see an orphan profile**, which is why this ticket exists and why `assertCoinInvariantHolds` is not
  the gate for it.
- `theFreshBrowsersWelcomeNamesTheRecoveredAccount` and `theFreshBrowsersWelcomeCarriesNoDeviceId`
  stay green: the `Welcome` is byte-for-byte what it was.
- `theFreshBrowserReadsTheSameProfile` and `theFreshBrowserReadsTheSameDuels` stay green: both reads
  present the token, so they resolve to the account and never touch the minted row.

Revert.

A second mutation, this one on the test's own helper. Change `liveBindingCountFor`'s
`WHERE device_id = ?` to `WHERE device_id <> ?`. **`theFreshDeviceHasNoLiveBindingAndTheOriginalHasOne`
reddens alone, on its first assertion**: `device_binding` holds exactly two rows at that point — the
duel's two devices — so the fresh device's count becomes `2` instead of `0`, while the winner's
becomes `1` and still matches. A helper that ignored its argument would pass one of the two
assertions; it cannot pass both. Revert.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Why a snapshot and not a row count.** `ADR-0030` §5 chose byte-identical multisets over counts for
`player` because a defect that adds one row and removes another is invisible to a count. The same
argument applies here, and more sharply: `device_binding`'s interesting mutation is an `UPDATE` of
`revoked_at`, which changes no count at all.

**This is the direct half of the story's claim. `TASK-040709` is the durable half** — the arc
assertions here cover the operations this scenario performs, and nothing more.
