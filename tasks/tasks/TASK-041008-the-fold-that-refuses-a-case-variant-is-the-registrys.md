---
schema: 2
id: TASK-041008
title: The fold that refuses a case variant is the registry's, and the schema test says so
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, fixtures, identity, schema]
depends_on: [TASK-041007]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.SchemaConstraintsTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`SchemaConstraintsTest` keeps its five `player`-side `CHECK` assertions raw and moves its one
uniqueness assertion onto `name_registry_folded`, which is the index that now refuses a case variant.

## The assertion that has to move, and why it is not a weakening

`twoDisplayNamesDifferingOnlyByCaseAreNotAllowed` asserts today that the exception message contains
`player_display_name_unique`. After `TASK-041010` that index is **unreachable by any well-formed
path** — `ADR-0051` §2: *"`player_display_name_unique` survives as a second line of defence that can
only fire if the registry and the column have somehow disagreed, which the foreign key makes
unreachable."* A test that keeps naming it is asserting that a defect exists.

The name the test is *about* — two names differing only in case are not both allowed — is unchanged.
Only the structure that refuses them moves, from `player`'s index to the registry's, which is
`ADR-0029` §1's fold either way. The index itself stays in the schema and is not dropped.

**This ticket lands before the foreign key on purpose**: registering both names makes the refusal
come from `name_registry_folded` today, which is also what it will be after `TASK-041010`. The test
is green on both sides of that migration, so nothing has to be timed.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/SchemaConstraintsTest.kt` | modify — one helper, one new helper, one test renamed and re-asserted |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read — `name_registry_folded` |
| `docs/adr/ADR-0051-a-name-is-registered-before-it-is-held.md` | read — §2's fourth bullet |

## Scope

- `setDisplayName(playerId, displayName)` stays **raw** and keeps its five callers: the four
  `CHECK` tests (`aDisplayNameLongerThan32CharactersIsRejected`, `anEmptyDisplayNameIsRejected`,
  `aDisplayNameWithLeadingWhitespaceIsRejected`, `aDisplayNameWithTrailingWhitespaceIsRejected`,
  `aNonNFCNormalizedDisplayNameIsRejected`). Each asserts a `player_display_name_*` name and each
  must keep getting it — see `TASK-041007` for why registering first would break them.
- A new registering helper — `registerAndSetDisplayName(playerId, displayName)` — used by exactly
  two call sites:
  - `anUpdateToAnAlreadySetDisplayNameIsRejected`'s **first** write (`"Bob"`, which must land). Its
    second write (`"Alice"`, which the trigger must refuse) stays on the raw helper, so the test
    keeps asserting `23001` and the message `display_name is permanent once set`.
  - both writes in the renamed uniqueness test below.
- `twoDisplayNamesDifferingOnlyByCaseAreNotAllowed` is renamed
  **`twoDisplayNamesDifferingOnlyByCaseAreRefusedByTheRegistry`** and re-asserted:
  - Player 1: `registerAndSetDisplayName(playerId1, "Bob")` succeeds.
  - Player 2: `registerAndSetDisplayName(playerId2, "bob")` raises.
  - `assertEquals("23505", exception.sqlState)` — unchanged.
  - The message assertion changes from `player_display_name_unique` to **`name_registry_folded`**.
  - One assertion is **added**, not replaced: player 1 still holds `"Bob"` afterwards. A refusal that
    also damaged the holder would otherwise pass.
- Nothing else in the file changes: `insertPlayer`, `insertDuel`, `insertDuelResult`, and every
  non-display-name test are untouched.

## Out of scope

- Dropping `player_display_name_unique`. It stays; `ADR-0051` §2 keeps it as a second line of
  defence.
- Asserting `player_display_name_registered` — that constraint does not exist yet and is
  `TASK-041010`'s.
- The five `CHECK` tests' assertions.

## Tests

`SchemaConstraintsTest`, `-PrequireDocker=true`. One test renamed and re-asserted; the rest unchanged.

| Test | Proves |
| --- | --- |
| `twoDisplayNamesDifferingOnlyByCaseAreRefusedByTheRegistry` | `"Bob"` lands; `"bob"` is refused with `23505` and a message naming `name_registry_folded`; `"Bob"` is still stored. **The wrong implementations it must fail against**: (a) a helper that swallows the registry conflict with `ON CONFLICT DO NOTHING` — the refusal then names `player_display_name_unique` and the message assertion fails; (b) a registry index built on `lower(name)` without `COLLATE "und-x-icu"` — passes for `"Bob"`/`"bob"` and is caught instead by `DisplayNameUniquenessTest.theFoldReachesBeyondAscii`, which is why that test is not duplicated here |
| `anUpdateToAnAlreadySetDisplayNameIsRejected` | Still `23001` and still the trigger's message — the first write now registers, the second does not |
| The five `CHECK` tests | Still `23514` and still a `player_display_name_*` constraint name |

## Acceptance criteria

- [ ] `SchemaConstraintsTest.twoDisplayNamesDifferingOnlyByCaseAreRefusedByTheRegistry` passes and
      asserts `23505`, a message containing `name_registry_folded`, and that player 1 still holds
      `"Bob"`
- [ ] `SchemaConstraintsTest.anUpdateToAnAlreadySetDisplayNameIsRejected` passes with its `23001` and
      message assertions unchanged
- [ ] The five `player_display_name_*` `CHECK` tests pass with their assertions unchanged
- [ ] The string `player_display_name_unique` no longer appears in `SchemaConstraintsTest.kt`
- [ ] `player_display_name_unique` still exists in the schema — `MigrationsTest` is not edited and
      `V3` is not edited
- [ ] No test other than the renamed one has an assertion changed, and no assertion anywhere in the
      file is removed without a replacement that is at least as strong
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
