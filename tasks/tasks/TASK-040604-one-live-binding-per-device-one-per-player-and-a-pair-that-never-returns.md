---
schema: 2
id: TASK-040604
title: One live binding per device, one per player, and a pair that never comes back
type: task
status: done
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, schema, index, security]
depends_on: [TASK-040603]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DeviceBindingUniquenessTest' -PrequireDocker=true
---

## Goal

The two partial unique indexes and the natural primary key `V7` created are pinned by tests: at most
one live binding per device, at most one live binding per player, and a device that has revoked a
profile can never be bound to that same profile again.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DeviceBindingUniquenessTest.kt` | create |

Read, and do not edit:
`docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §1 and §3,
`poker-server/src/main/resources/db/migration/V7__device_binding.sql`. Nothing else.

## Scope

- One new test class, `duels.poker.server.db.DeviceBindingUniquenessTest`, built the same way
  `TASK-040603`'s class is: `PostgresTestSupport.freshDatabase()` then `Migrations.migrate(...)` in
  `@BeforeEach`, raw JDBC, `assertFailsWith<SQLException>`.
- Two private helpers: `newPlayer(): UUID` writing one `player` row
  (`INSERT INTO player (id, coin_balance) VALUES (?, 0)`), and
  `bind(deviceId: String, playerId: UUID)` writing one `device_binding` row with `revoked_at` left
  null. Revocation in this class is the raw
  `UPDATE device_binding SET revoked_at = now() WHERE device_id = ? AND player_id = ?`.
- **Every refusal is matched on `sqlState == "23505"` and on the index or constraint name in the
  message**, because the whole point of the three refusals is that they are three *different*
  objects: `device_binding_live_device`, `device_binding_live_player`, `device_binding_pkey`. A test
  that only checked `23505` would pass with all three collapsed into one.

## Out of scope

- The finality trigger — `TASK-040603` owns it, and nothing here updates `revoked_at` twice.
- `PostgresPlayerDirectory` — `TASK-040605`.
- Editing `V7__device_binding.sql`, which is merged and immutable.

## Tests

`DeviceBindingUniquenessTest`

| Test | Proves |
| --- | --- |
| `aSecondLiveBindingForOneDeviceIsRefused` | Two different players, both bound live to `"d-shared"`. The second insert raises; `sqlState` is `23505` and the message contains `device_binding_live_device`. This is `ADR-0012`'s one-profile-per-device rule in its new home |
| `aSecondLiveBindingForOnePlayerIsRefused` | One player, bound live to `"d-first"` and then to `"d-second"`. The second insert raises; `sqlState` is `23505` and the message contains `device_binding_live_player` |
| `aRevokedBindingBlocksNeitherIndex` | The positive control for both, and it needs the pair above to *not* be the reason it passes. Bind player A to `"d-shared"`, revoke it; then binding player **B** to `"d-shared"` succeeds, **and** binding player **A** to `"d-second"` succeeds. Two inserts, two different indexes, both live afterwards: `SELECT count(*) FROM device_binding WHERE revoked_at IS NULL` reads `2`. A non-partial unique index reddens this test on the first of the two inserts |
| `theSamePairNeverBindsAgain` | Bind player A to `"d-a"`, revoke it, then insert `("d-a", A)` again. It raises; `sqlState` is `23505` and the message contains `device_binding_pkey`. `ADR-0049` §3's *"never back to the profile it left"* — and note this is the primary key refusing, **not** a partial index, which is why the row is kept rather than deleted |
| `aRevokedRowSurvivesTheRefusal` | Immediately after `theSamePairNeverBindsAgain`'s refusal — repeated inside this test, not shared — `SELECT count(*) FROM device_binding WHERE device_id = 'd-a'` reads `1` and that row's `revoked_at` is still non-null. The refusal wrote nothing and destroyed nothing |

**Two inputs everywhere a value reaches a comparison.** `aRevokedBindingBlocksNeitherIndex` uses two
distinct device ids and two distinct players precisely so that neither insert can be explained by
the other's key; a version of it written with one device id and one player proves only that
`INSERT` works.

## Acceptance criteria

- [ ] `DeviceBindingUniquenessTest.aSecondLiveBindingForOneDeviceIsRefused` passes
- [ ] `DeviceBindingUniquenessTest.aSecondLiveBindingForOnePlayerIsRefused` passes
- [ ] `DeviceBindingUniquenessTest.aRevokedBindingBlocksNeitherIndex` passes
- [ ] `DeviceBindingUniquenessTest.theSamePairNeverBindsAgain` passes
- [ ] `DeviceBindingUniquenessTest.aRevokedRowSurvivesTheRefusal` passes
- [ ] The three refusal tests name three different objects in their message assertions:
      `device_binding_live_device`, `device_binding_live_player` and `device_binding_pkey`, one each
- [ ] The diff against `develop` touches exactly one file under `poker-server/`, and it is the
      one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Three mutations, each run locally against `V7` and reverted afterwards. `git status` must be clean
of the migration when the ticket is pushed.

1. **Make `device_binding_live_device` non-unique** — `CREATE INDEX` instead of
   `CREATE UNIQUE INDEX`, keeping the `WHERE`. **Exactly one test reddens**,
   `aSecondLiveBindingForOneDeviceIsRefused`: its second insert now succeeds. The other four are
   untouched — `aRevokedBindingBlocksNeitherIndex` expects both of its inserts to succeed and they
   still do, and the pair tests still meet the primary key.
2. **Drop `WHERE revoked_at IS NULL` from `device_binding_live_device`**, keeping it unique.
   `aRevokedBindingBlocksNeitherIndex` reddens on its *first* insert, because the revoked row now
   occupies the value. `theSamePairNeverBindsAgain` **may** redden as well: that insert now violates
   two constraints at once, and which name PostgreSQL puts in the message is not specified. That
   ambiguity is stated rather than guessed, and it is the reason the criterion above is about which
   *object* each test names rather than about how many tests a mutation reddens.
3. **Replace the primary key with `PRIMARY KEY (device_id, player_id, bound_at)`.** The pair is no
   longer unique and neither partial index blocks the re-insert — the old row is revoked, the new
   one is live — so `theSamePairNeverBindsAgain`'s second insert succeeds and that test reddens.
   `aRevokedRowSurvivesTheRefusal` reddens with it: it repeats the same insert, whose
   `assertFailsWith` no longer fires, and its count for `"d-a"` reads `2`. **Two tests, and both are
   named.**

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.

## Notes

**The two indexes are independently gated, and the fixtures are why.** The device test uses one
device with *two different players*; the player test uses one player with *two different devices*.
Share both columns and either index catches the violation, so neither is separately proven. Dropping
the `UNIQUE` from one index reddens exactly its own test and never the other — checked in both
directions, beyond what the Proof asked for.

**Why the Proof hedged, and what the answer is.** It said dropping `WHERE revoked_at IS NULL` from
`device_binding_live_device` *may* also redden `theSamePairNeverBindsAgain`. It did not. The reason
is that the mutation violates **both** that index and the primary key, and PostgreSQL's order of
constraint checking is unspecified — so which one raises is arbitrary. The ticket's criterion is
which *object* each test names, not how many tests a mutation reddens, so the observation is
consistent rather than a gap.

