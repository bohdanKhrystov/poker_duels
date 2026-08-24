---
schema: 2
id: TASK-040605
title: A revoked device resolves to a new, empty profile — never the one it left
type: task
status: backlog
parent: STORY-0406
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, identity, db, security]
depends_on: [TASK-040604]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresPlayerDirectoryRevocationTest' -PrequireDocker=true
---

## Goal

`PostgresPlayerDirectory` reads only *live* bindings: a revoked device id is unknown to
`findOrNull`, and `resolve` mints a fresh, empty profile for it rather than handing back the one
that revoked it.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryRevocationTest.kt` | create |

Read, and do not edit:
`poker-server/src/main/kotlin/duels/poker/server/db/PostgresPlayerDirectory.kt`,
`poker-server/src/test/kotlin/duels/poker/server/db/PostgresPlayerDirectoryTest.kt` (for the shape
these tests copy), `docs/adr/ADR-0049-a-device-binding-is-a-row-and-revoking-is-final.md` §3.

## Scope

- A new class, `duels.poker.server.db.PostgresPlayerDirectoryRevocationTest` — **not** new methods
  on `PostgresPlayerDirectoryTest`, which `TASK-040601` has just rewritten and which this ticket
  must leave untouched so the two diffs stay separately reviewable.
- Setup copies `PostgresPlayerDirectoryTest`'s: `freshDatabase()`, `Migrations.migrate(...)`, and a
  `PostgresPlayerDirectory(dataSource)` field.
- Revocation here is raw SQL —
  `UPDATE device_binding SET revoked_at = now() WHERE player_id = ? AND revoked_at IS NULL` — because
  no Kotlin revoke path exists yet. That is deliberate: this ticket is about what `resolve` reads,
  not about who writes it.
- Balances are written with `UPDATE player SET coin_balance = ? WHERE id = ?` so an abandoned
  profile can be shown to keep its coins.

## Out of scope

- `PostgresPlayerDirectory.kt` itself. If a test here fails, the ticket stops and reports — it does
  not edit the production file, which `TASK-040601` owns.
- `PostgresPlayerDirectoryTest.kt` — **a named prohibition**, not an omission.
- The HTTP endpoint and the `DeviceBindings` port — `TASK-040607` onward.
- The socket's view of a revoked device — `TASK-040614`.

## Tests

`PostgresPlayerDirectoryRevocationTest`

| Test | Proves |
| --- | --- |
| `findingARevokedDeviceIsNullWhileALiveOneIsFound` | Two devices, `"d-revoked"` and `"d-live"`, each resolved to its own profile; `"d-revoked"` is then revoked. `findOrNull(DeviceId("d-revoked"))` is `null` **and** `findOrNull(DeviceId("d-live"))` still answers its original player id. **Two inputs whose expected answers differ**: a `findOrNull` that had stopped finding anything at all would pass the first half and fail the second |
| `resolvingARevokedDeviceMintsADifferentProfile` | Resolve `"d-revoked"` to player `P`, revoke it, resolve `"d-revoked"` again. The returned `PlayerId` is **not** `P`, `SELECT count(*) FROM player` reads `2`, and `SELECT count(*) FROM device_binding WHERE device_id = 'd-revoked'` reads `2` — one revoked, one live |
| `theAbandonedProfileKeepsItsCoins` | Give `P` a balance of `3` before revoking. After the re-resolve, `P`'s `coin_balance` still reads `3` and the freshly minted player's reads `0`. **Two rows, two different expected values** — with one row at `0` the assertion could not tell a preserved balance from a fresh one. `ADR-0049` §2's *"this writes nothing to `player`"* is what this test observes |
| `theRevokedBindingIsUntouchedByTheReResolve` | After the re-resolve, the original `("d-revoked", P)` row still has a non-null `revoked_at` and its `bound_at` is unchanged from the value read before the re-resolve. The mint inserted a new row rather than reviving the old one |
| `aSecondResolveOfTheRevokedDeviceIsIdempotent` | Resolving `"d-revoked"` a third time returns the same player id the second resolve returned, and `SELECT count(*) FROM player` still reads `2`. Without this, `resolve` could be minting a fresh profile on **every** connection of a revoked device, which is a leak rather than a feature |

## Acceptance criteria

- [ ] All five test methods above pass
- [ ] `theAbandonedProfileKeepsItsCoins` asserts two different balances, `3` and `0`, read from two
      different `player` rows
- [ ] No file under `poker-server/src/main` is modified, and the diff against `develop` touches
      exactly one file under `poker-server/`, the one in the *Files* table
- [ ] Every command in `verify:` exits 0

## Proof

Mutate `PostgresPlayerDirectory`'s live-binding statement — drop ` AND revoked_at IS NULL` from it —
run this class, then revert.

**Four tests redden, and here is why each does.** `findingARevokedDeviceIsNullWhileALiveOneIsFound`
fails on its first assertion, because `findOrNull` now finds the revoked row.
`resolvingARevokedDeviceMintsADifferentProfile` fails because the second resolve returns `P` and the
`player` count reads `1`. `aSecondResolveOfTheRevokedDeviceIsIdempotent` fails on the same count.
`theAbandonedProfileKeepsItsCoins` fails too, and this is the one worth spelling out: its "freshly
minted player" **is** `P` under the mutation, so its second assertion compares `P`'s balance against
`0` and reads `3`. Only `theRevokedBindingIsUntouchedByTheReResolve` stays **green** — nothing wrote
to `revoked_at` or `bound_at` either way — and it is recorded here so a reviewer does not read its
survival as the test being dead. It guards the tombstone, not the predicate.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
