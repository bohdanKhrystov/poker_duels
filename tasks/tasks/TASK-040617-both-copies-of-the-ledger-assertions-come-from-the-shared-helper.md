---
schema: 2
id: TASK-040617
title: Both copies of the ledger assertions come from the shared helper
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [server, db, coins, invariant, refactor]
depends_on: [TASK-040616]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SignUpDatabaseTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.RetireDisplayNameTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`SignUpDatabaseTest` and `RetireDisplayNameTest` stop carrying their own copies of P1 and P2 and
call `assertCoinInvariantHolds` instead, so there is one implementation of the two properties in the
repository rather than three.

> **Three, not two.** `RetireDisplayNameTest.aTakedownMovesNoCoin` holds a second copy — as
> `private` class members rather than top-level functions, with its own nested `LedgerSums` — which
> is easy to miss and is exactly the drift a shared helper exists to stop. It is the reason this
> ticket names two files.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SignUpDatabaseTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/RetireDisplayNameTest.kt` | modify |

Read, and do not edit: `poker-server/src/test/kotlin/duels/poker/server/db/CoinInvariant.kt`.

## Scope

- Delete the private `p1BrokenBalanceCount()`, `p2LedgerSums()` and the `LedgerSums` data class from
  `SignUpDatabaseTest`.
- `theLedgerSumsAreUnchangedByASignUp` and `everyPlayersBalanceStillEqualsTheirDeltas` each call
  `dataSource.assertCoinInvariantHolds(step)` — **before and after the sign-up**, exactly where they
  read the sums today, with step strings `"before sign-up"` and `"after sign-up"`.
- **Both methods keep their names and both keep asserting before *and* after.** `ADR-0030` §5's
  reason is that a mint and a burn cancel, so an after-only assertion is not the same test; this
  ticket preserves that shape rather than collapsing the two calls into one.
- `RetireDisplayNameTest` loses its `private fun p1BrokenBalanceCount`, `private fun p2LedgerSums`
  and `private data class LedgerSums` the same way, and `aTakedownMovesNoCoin` calls the shared
  helper twice, with step strings `"before takedown"` and `"after takedown"` — it already asserts
  before and after, and that shape is preserved.
- `assertFixtureTook`, `balanceOf`, `writeFinishedDuel`, `snapshot` and `rowCount` stay where they
  are, in both files. This ticket moves the two *properties*, not the fixture helpers.

## Out of scope

- `SignInDatabaseTest` — it has no P1/P2 to consolidate.
- Adding new assertions to `SignUpDatabaseTest`, or changing what any of its tests observes. This is
  behaviour-preserving; if a test fails after the swap, that is a finding about the helper, not a
  licence to edit the test.
- `TableSnapshot`/`snapshot` in `SignUpDatabaseTest`. `playerTableSnapshot` exists in the shared
  file for the scenario's use, and consolidating this second pair is not this ticket.

## Tests

No new test methods. The gate is that every method already in `SignUpDatabaseTest` passes unchanged,
and that the two properties now come from one place.

| Test | Proves |
| --- | --- |
| `theLedgerSumsAreUnchangedByASignUp` | Still passes, now through the shared helper, still asserting before and after |
| `everyPlayersBalanceStillEqualsTheirDeltas` | Still passes, same shape |
| `thePlayerTableIsTheSameMultisetAfterTheSignUp` | Still passes, untouched — named here so a reviewer can see the snapshot half was deliberately left alone |
| `RetireDisplayNameTest.aTakedownMovesNoCoin` | Still passes, now through the shared helper, still asserting before and after |

## Acceptance criteria

- [ ] Every test method in `SignUpDatabaseTest` and `RetireDisplayNameTest` passes, and no method in
      either is renamed, added or removed
- [ ] `grep -rln "COALESCE(SUM" poker-server/src/test` names exactly one file, and it is
      `CoinInvariant.kt`
- [ ] `theLedgerSumsAreUnchangedByASignUp`, `everyPlayersBalanceStillEqualsTheirDeltas` and
      `RetireDisplayNameTest.aTakedownMovesNoCoin` each contain **two** calls to
      `assertCoinInvariantHolds`, with different step strings
- [ ] The diff against `develop` touches exactly two files under `poker-server/`, and they are the
      two in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Replace one of the two `assertCoinInvariantHolds` calls in `theLedgerSumsAreUnchangedByASignUp` with
the other's step string, so both read `"after sign-up"`. Nothing reddens — and that is the point of
the criterion above rather than of a red run: the two calls differ only in *when* they run, so no
assertion can tell them apart, and what keeps the before-call from being deleted is the criterion
that counts two calls with different step strings. Revert.

A red run is available for the swap itself: delete the `"before sign-up"` call entirely and break
the ledger *before* the sign-up with `UPDATE player SET coin_balance = coin_balance + 1`. The test
then fails at `"after sign-up"` and blames the sign-up for a coin that moved before it — the
misdiagnosis the before-call exists to prevent.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**A consolidation's failure mode is asserting less than the copies it replaced**, with every test
still green because the assertion that used to catch something is simply gone. The coder did not run
a mutation, so the review did the comparison assertion for assertion:
`assertEquals(0, p1BrokenBalanceCount())` ↔ `check(brokenPlayers.isEmpty())` — equivalent, since a
count of zero and an empty list are the same claim; two separate `assertEquals(0, sum)` ↔ one
conjoined `check(playerBalanceSum == 0 && duelResultDeltaSum == 0)` — also equivalent.

**The before/after pairs survived.** `ADR-0030` §5 needs the invariant on both sides of the operation
under test, and all three tests keep exactly two calls with distinct step strings — "before sign-up"
/ "after sign-up", "before takedown" / "after takedown". Collapsing a pair into one post-operation
check is the silent loss this shape invites.

**The consolidation strengthened the tests.** Each call site now checks **both** properties, where
the private copies checked whichever their file happened to need.

**The collision resolved itself by deletion.** While the file-private `p2LedgerSums` and the
`internal` helper both existed, an untouched file failed to compile with overload-resolution
ambiguity — `TASK-040616` renamed its helper to sidestep it. Removing the copies here removes the
ambiguity entirely; neither name survives anywhere in the module.

