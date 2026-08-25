---
schema: 2
id: TASK-040701
title: The device_binding snapshot comes from one place
type: task
status: done
parent: STORY-0407
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [server, db, test-support, refactor]
depends_on: [TASK-040623]
verify:
  - grep -c 'internal fun DataSource.deviceBindingTableSnapshot' poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt | grep -qx 1
  - grep -c 'fun DataSource.deviceBindingTableSnapshot' poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt | grep -qx 0
  - ./gradlew :poker-server:test --tests 'duels.poker.server.e2e.IdentityMovesNoCoinTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

`deviceBindingTableSnapshot` becomes `internal` in `CoinInvariant.kt`, beside `playerTableSnapshot`,
so `STORY-0407`'s recovery test can assert over `device_binding` without writing a second copy of it.

## Why this exists

`STORY-0407` claims a recovery sign-in **"leaves no orphan profile behind"**. After `ADR-0049` a
profile occupies **two** tables, not one: `V7__device_binding.sql` dropped `player.device_id`, so the
device→profile edge is a `device_binding` row. A `player`-only check therefore cannot see one real
defect shape — **a rebinding**, which inserts a `device_binding` row naming an existing player and
adds no `player` row at all. `playerTableSnapshot()` would be byte-identical across it.

The snapshot function that would see it already exists, as a `private fun` in
`IdentityMovesNoCoinTest.kt`. Its own KDoc says why it was declared there and not beside
`playerTableSnapshot`: *"declared here rather than beside it: this ticket's Files table touched only
this file."* This is the ticket that finishes that move, on the second use site — the trigger this
repository already uses (`TASK-040617` retired the private `p2LedgerSums` for the same reason).

**The move and the deletion must land together.** `CoinInvariant.kt`'s KDoc records the exact hazard:
Kotlin does not scope a file-private top-level declaration away from an `internal` one in the same
module, so an `internal fun DataSource.deviceBindingTableSnapshot` added while the `private` one
still stands collides as an unresolvable overload ambiguity at every call site in
`IdentityMovesNoCoinTest.kt`. That is why this ticket names two files rather than one.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/e2e/IdentityMovesNoCoinTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/resources/db/migration/V7__device_binding.sql`.

## Scope

- Move `deviceBindingTableSnapshot` from `IdentityMovesNoCoinTest.kt` into `CoinInvariant.kt`,
  changing `private` to `internal` and nothing else about its body: the same
  `SELECT * FROM device_binding ORDER BY player_id`, the same `ResultSetMetaData` column count, the
  same `List<List<Any?>>` return. **`SELECT *` is load-bearing** — it is what makes the snapshot keep
  comparing correctly across a migration that adds or drops a column, exactly as
  `playerTableSnapshot`'s own KDoc says of its own.
- Rewrite its KDoc for the new home: drop the *"declared here rather than beside it"* sentence, which
  is now false, and say instead that it is the `device_binding` counterpart of `playerTableSnapshot`
  and that a profile after `ADR-0049` occupies both tables.
- Delete the `private fun DataSource.deviceBindingTableSnapshot` from `IdentityMovesNoCoinTest.kt`
  and add `import duels.poker.server.db.deviceBindingTableSnapshot`. ktlint orders imports
  lexicographically, so it belongs between `duels.poker.server.db.assertCoinInvariantHolds` and
  `duels.poker.server.db.playerTableSnapshot`, which are already there.
- Nothing else in `IdentityMovesNoCoinTest.kt` changes. The two call sites keep the identical
  expression `dataSource.deviceBindingTableSnapshot()`, and every test method keeps its assertions.

## Out of scope

- `private fun DataSource.deviceBindingColumnNames`. It stays in `IdentityMovesNoCoinTest.kt`, private.
  It has one use site — `revokingChangesExactlyOneBindingColumn` — and `STORY-0407` does not need it:
  the recovery assertions compare snapshots for equality, and never need to name which column moved.
  A second use site is what promotes a helper here, and there is not one.
- `playerTableSnapshot`, `assertCoinInvariantHolds`, `p1BrokenBalancePlayerIds` and
  `coinInvariantP2Sums`. Untouched.
- Renaming `CoinInvariant.kt`. It already holds `playerTableSnapshot`, which is not a coin invariant
  either; the file is the db-test-support file by precedent, and renaming it would touch every
  importer for no gate.
- Any file under `poker-server/src/main`.

## Tests

No new test methods. This is behaviour-preserving, and the gate is that the two methods which
actually read a `device_binding` snapshot still pass through the moved function.

`IdentityMovesNoCoinTest`

| Test | Proves |
| --- | --- |
| `revokingChangesExactlyOneBindingColumn` | Still passes, now reading its two snapshots through the moved `internal` function |
| `revokingLeavesThePlayerTableByteIdentical` | Still passes, untouched — named here so a reviewer can see the `player` half was deliberately left alone |

## Acceptance criteria

- [ ] `CoinInvariant.kt` declares exactly one `internal fun DataSource.deviceBindingTableSnapshot`
- [ ] `IdentityMovesNoCoinTest.kt` declares no `deviceBindingTableSnapshot` at all, and imports it
      from `duels.poker.server.db`
- [ ] `IdentityMovesNoCoinTest.kt` still declares `private fun DataSource.deviceBindingColumnNames`
- [ ] `IdentityMovesNoCoinTest.revokingChangesExactlyOneBindingColumn` passes
- [ ] `IdentityMovesNoCoinTest.revokingLeavesThePlayerTableByteIdentical` passes
- [ ] No test method in `IdentityMovesNoCoinTest` gains, loses or weakens an assertion
- [ ] The diff touches exactly the two files in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

In the moved `CoinInvariant.kt` function, change `SELECT * FROM device_binding ORDER BY player_id`
to `SELECT device_id, player_id FROM device_binding ORDER BY player_id`.

`revokingChangesExactlyOneBindingColumn` reddens, and **it is the only method that does**. Trace it:
revocation writes `revoked_at` and nothing else, so with that column no longer selected the before
and after snapshots are equal row for row. `assertEquals(before.size, after.size)` still passes;
`changedRows` is then empty, and `assertEquals(1, changedRows.size)` fails with *expected 1, got 0* —
before `changedColumns` or `deviceBindingColumnNames` is reached at all.

Every other method in the class stays green: they read `playerTableSnapshot`, balances or `Welcome`
frames, and the mutated query still returns without error, so `runScenario()` completes normally.
Revert.

**This is also the proof that the moved function is the one in use**, which a pure move otherwise has
no gate for: mutating the copy in `CoinInvariant.kt` is what reddens a test in `e2e`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The `SELECT *` is load-bearing, and a mutation proved it.** Narrowing it to
`SELECT device_id, player_id` makes `revokingChangesExactlyOneBindingColumn` fail with
"expected 1, got 0" — revocation changes only `revoked_at`, so a narrower column list makes the
snapshot blind to the one thing it exists to see. The body is otherwise byte-identical to the
original; only visibility changed.

**The promotion was atomic, which is why no compile error appeared.** An `internal` top-level
declaration and a same-named `private` one in the same module collide as an unresolvable overload at
**every** call site — `TASK-040616` had an untouched file fail to compile for exactly this. Moving
and deleting in one commit skips the transient state entirely.

**One thing was lost in the KDoc rewrite this ticket asked for.** The old text cited `ADR-0050` §1 to
explain why ordering by `player_id` is deterministic: a single terminal revocation means no two rows
share a `player_id` across snapshots. The new text says "deterministic" without that reason. In its
old home that was safe; shared, a caller with two bindings for one player now gets unstable ordering
and nothing warns them. Not a defect in this diff — the rewrite was specified — but worth a line if a
later ticket touches this helper.

**The two file greps in `verify:` fail before the work and pass after**, which the Gradle command
alone would not: `IdentityMovesNoCoinTest` is green on `develop` today, so a verify block of tests
only would be satisfied by doing nothing.
