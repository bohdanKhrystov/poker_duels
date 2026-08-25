---
schema: 2
id: TASK-041639
title: A reset that cannot write the password spends no token
type: task
status: done
parent: STORY-0416
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [server, db, auth, security, invariant]
depends_on: [TASK-041614]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPasswordResetsConsumeTest' -PrequireDocker=true
  - ./gradlew :poker-server:ktlintCheck
  - ./gradlew :poker-server:detekt
---

## Goal

One test in `PostgresPasswordResetsConsumeTest` reddens when the token spend is committed before the
credential write, so the first of `consume`'s two transaction boundaries is held by a gate rather
than by review.

## Why this exists

`TASK-041614` shipped `consume` as one connection, `autoCommit` off, one commit — and its own Notes
record that **splitting the transaction at either boundary leaves all five of its tests green**. The
coder found it, the reviewer reproduced it, and the reviewer then built and verified the fixture that
closes the first half. This ticket is that fixture, and nothing else.

The fixture is a live reset token for a player with **no** `password` credential row, which is a
reachable state: `password_reset.player_id` references `player (id)` and nothing else (`V8`), and
under `ADR-0012` and `ADR-0036` an account — and therefore a credential — is optional. Under the
shipped atomic code the credential `UPDATE` affects zero rows, `consume` answers `false`, and the
**token delete rolls back with it**, so once the credential exists the same token still works. Under
a `commit()` placed after the token spend, that first attempt burned the token and the second call
answers `false`.

This gap is heavier than the identical-shaped one `TASK-041608` recorded as a note. There, a split
left an *identical* end state — a `DELETE` plus an `INSERT` has nothing to roll back. Here a split
leaves a state **strictly worse than no reset at all**: the owner believes they have recovered, the
attacker's session is still running, and the `204` says nothing. And no later ticket in this chain
would incidentally re-exercise it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPasswordResetsConsumeTest.kt` | modify |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPasswordResets.kt` — the four steps of
`consume` and the `rewriteCredential(...) != 1` guard this test drives into. `## Proof` mutates this
file temporarily; **every step ends with a revert, and the PR diff must contain no change to it**;
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresCredentials.kt` — `create` and `verify`,
whose signatures the fixture already uses in this test file;
`docs/adr/ADR-0031-an-optional-verified-recovery-email.md` §4.

## Scope

- Add **one** test method to the existing `PostgresPasswordResetsConsumeTest` class, using the
  `insertPlayer()`, `tokens`, `passwordResets`, `credentials` and `authSessions` fixtures already in
  the file. No new helper, no new file, no new fixture field.
- The fixture player is created by `insertPlayer()` and **is given no `credential` row** before the
  first `consume`. `credentials.create` runs only after that call has returned.
- Add one paragraph to the class KDoc naming what the new test gates and what it does not — that it
  reaches the token-spend/credential-write boundary and that the credential-write/session-delete
  boundary stays ungated until `TASK-041640`.
- The five tests already in the file do not move: no assertion is added, removed, weakened or
  renamed in any of them.

## Out of scope

- **The second boundary — the credential write to the session delete — and it stays ungated when
  this merges.** The session delete is unconditional but for `player_id`, so **no data fixture can
  make it fail**; reaching it needs a wrapped `DataSource`/`Connection` that throws between the two
  writes. That is `TASK-041640`, and until it merges, a `commit()` inserted between step 2 and step 3
  of `consume` leaves every test in this file green. Say so; do not attempt it here.
- **Changing `PostgresPasswordResets.kt`.** The shipped code already satisfies this test — the gate
  was missing, not the behaviour. If the new test fails against `develop` with no mutation applied,
  **stop and report it**: that is a defect in shipped code and a different ticket, not a licence to
  edit the production file inside this one.
- Threading, latches, and a second `Rng` or clock. This fixture needs none;
  `twoConcurrentUsesOfOneTokenYieldExactlyOneSuccess` already owns the concurrent property.
- Asserting the `password_reset` row directly over SQL. The behavioural assertion — *the same token
  consumed again answers `true`* — is what a caller can observe, and a row count would pass for an
  implementation that leaves a spent row behind.
- The endpoint's `204`/`400`/`422` — `TASK-041620` through `TASK-041622` and `TASK-041629`.

## Tests

`PostgresPasswordResetsConsumeTest` — one row added to the five already there.

| Test | Proves |
| --- | --- |
| `aRefusedCredentialWriteLeavesTheTokenSpendable` | A player with **no** `password` credential holds a live reset token and one session. `consume` answers `false` and the session survives. `credentials.create` then adds the credential, and **the same `ResetToken`** consumed again answers `true`, with the new secret verifying. The refused attempt must not have spent the token — which is only true because the failed credential `UPDATE` rolled the token delete back inside the same transaction |

The order inside the test is load-bearing and is part of what a reviewer checks: `insertPlayer` →
`issue` → `authSessions.issue` → **first `consume`** → assertions → `credentials.create` → **second
`consume`** → assertions.

## Acceptance criteria

- [ ] `PostgresPasswordResetsConsumeTest.aRefusedCredentialWriteLeavesTheTokenSpendable` passes
- [ ] No `credentials.create` call appears above the first `consume` in that test
- [ ] The test calls `consume` twice with **the same `ResetToken` value**, asserting `false` then
      `true`
- [ ] It asserts `credentials.verify(CredentialKind.PASSWORD, IDENTIFIER, newSecret)` returns the
      player id after the second call
- [ ] It asserts `authSessions.playerOf(session)` still returns the player id after the **first**
      call
- [ ] The five pre-existing tests are unchanged: no assertion added, removed, weakened or renamed,
      and no test method renamed
- [ ] `git status` shows no change to `PostgresPasswordResets.kt` or any other file outside the
      Files table
- [ ] Every command in `verify:` exits 0

## Proof

1. In `PostgresPasswordResets.consume`, add `connection.commit()` immediately after `deleteLiveToken`
   returns a non-null `playerId` and before `hasher.hash(secret)` — the token spend committed on its
   own. **`aRefusedCredentialWriteLeavesTheTokenSpendable` reddens alone**, on the *second* `consume`
   answering `false`: the token was committed as spent by an attempt that wrote no password. **All
   five pre-existing tests stay green**, and that is the entire reason this ticket exists — predict
   both halves and check both. Revert.
2. Do **not** reach for `rollback()` → `commit()` as a mutation. Postgres downgrades a `COMMIT` on an
   aborted transaction to a rollback server-side, so that edit changes no observable behaviour and
   reddens nothing; a green run after it would read as evidence that this test is inert when it is
   not. Step 1 is the mutation that works.
3. Change the guard `if (rewriteCredential(connection, playerId, secretHash) != 1)` to `< 0`, so a
   zero-row update counts as success. **`aRefusedCredentialWriteLeavesTheTokenSpendable` reddens
   alone**, at its first assertion — `consume` answers `true` for a player it wrote nothing for. The
   five pre-existing tests stay green, because their players all have a credential and the `UPDATE`
   returns 1 under both spellings. Revert.
4. Move `credentials.create` **above** the first `consume`, so the fixture player has a credential
   all along, then re-apply step 1's mutation. **Nothing reddens** — the test passes as a duplicate
   of `aGoodTokenRewritesThePasswordAndReturnsTrue`. That is the finding this ticket records: the
   missing-credential fixture is the only thing in this file that reaches the boundary at all, and
   the assertion order alone would not save it. Restore the fixture and revert step 1.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**Correction to this ticket's `## Proof`, step 4.** It predicts *"Nothing reddens — the test passes
as a duplicate of `aGoodTokenRewritesThePasswordAndReturnsTrue`."* It does not. Giving the fixture
player a credential before the first `consume` makes that call succeed, and the test's first
assertion expects `false`, so it reddens there — for a reason that has nothing to do with step 1's
mutation. Coder and reviewer independently traced it to the same place.

The **conclusion** the step draws survives, by a different mechanism than the one written down: the
missing-credential fixture is still the only thing in this file that reaches the boundary, because
the first assertion *is* the fixture condition rather than a consequence of it. Fifty-second `##
Proof` examined this run, and the thirteenth found wrong or imprecise. Recorded rather than quietly
amended, because a Proof step whose prediction never held is evidence about how these are written.

**The reordering the test does not catch is `TASK-041640`'s, and it is named.** An implementation
that delayed the token delete until after the session delete would still pass here, because by then
the credential exists and the `UPDATE` succeeds. `## Out of scope` defers exactly that — the second
boundary needs a wrapped `DataSource` that throws *between* the two writes — so this is a stated
limit, not a gap. The coder volunteered it as the diff's weakest assertion before being asked about
that section, which is the reason it is written here rather than discovered later.
