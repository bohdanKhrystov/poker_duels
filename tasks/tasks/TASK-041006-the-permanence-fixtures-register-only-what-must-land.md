---
schema: 2
id: TASK-041006
title: The permanence fixtures register only the names that must land
type: task
status: backlog
parent: STORY-0410
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, test, fixtures, identity]
depends_on: [TASK-041005]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.DisplayNamePermanenceTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`DisplayNamePermanenceTest` registers a name where the write is meant to succeed and writes raw where
the permanence trigger is meant to refuse, so all seven of its tests keep asserting `23001` for the
reason they were written to assert it.

## Why this file needs two helpers and the last one did not

Three of its uses are writes the trigger must refuse, and two of those would be intercepted by the
registry first:

| Test | The write | Registering first would |
| --- | --- | --- |
| `aRenameThatOnlyChangesCaseIsRefused` | holds `"Bob"`, writes `"bob"` | raise `23505` from `name_registry_folded` — the trigger never runs and the test asserts `23001` |
| `writingTheIdenticalNameChangesNothing` | holds `"bob"`, writes `"bob"` | raise `23505` on the already-registered string — the test expects the write to succeed silently |
| `aNamedProfileCannotBeRenamed` | holds `"bob"`, writes `"robert"` | succeed, then the trigger raises `23001` anyway — but it leaves a `TAKEN` row for a name nobody holds, which is the exact state `ADR-0051` §2 exists to prevent |

`BEFORE ROW` triggers run before the row is written and foreign keys are checked as `AFTER` triggers,
so a raw write of an unregistered name still reports `23001` from the permanence trigger after
`TASK-041010`. Raw is correct here and stays correct.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/db/DisplayNamePermanenceTest.kt` | modify — one helper body, one new helper, three call sites |
| `poker-server/src/main/resources/db/migration/V5__name_registry.sql` | read |
| `poker-server/src/main/resources/db/migration/V3__player_display_name.sql` | read — the trigger's firing condition |

## Scope

- `insertPlayerWithName(displayName)` gains the registry insert. All five of its uses are names that
  must land.
- `updatePlayerName(playerId, displayName)` **gains the registry insert** and keeps its name. Its two
  remaining callers are `anUnnamedProfileCanBeNamed` and `theCoinWriteDoesNotFireTheTrigger`, both of
  which expect the write to succeed.
- A new private helper, raw, named for what it is:

  ```kotlin
  /** Writes display_name without registering it: for the three writes the trigger must refuse. */
  private fun forceWriteName(playerId: UUID, displayName: String)
  ```

  `aNamedProfileCannotBeRenamed`, `aRenameThatOnlyChangesCaseIsRefused` and
  `writingTheIdenticalNameChangesNothing` call it instead of `updatePlayerName`. Those three call
  sites are the only lines inside test methods this ticket touches.
- `clearPlayerName` and `readDisplayName` are unchanged — `clearPlayerName` must stay raw, and
  `aNamedProfileCannotBeUnnamed` must keep asserting `23001` (there is no `RETIRED` row, so
  `ADR-0051` §3's exception does not apply and permanence still holds).
- No assertion in the file changes.

## Out of scope

- The permanence trigger's **new** behaviour — `TASK-041013` adds those tests, in this same file.
  This ticket only keeps what is there green.
- `theCoinWriteDoesNotFireTheTrigger`'s duel and coin assertions.

## Tests

`DisplayNamePermanenceTest`, `-PrequireDocker=true`. Nothing added; seven existing tests must pass.

| Test | Proves, after this change |
| --- | --- |
| `anUnnamedProfileCanBeNamed` | The registering path still lands a name |
| `aNamedProfileCannotBeRenamed` | `23001` still comes from the trigger, not `23505` from the registry |
| `aNamedProfileCannotBeUnnamed` | `23001` for `name → NULL` with no `RETIRED` row — the case `TASK-041013` will later show the single exception to |
| `aRenameThatOnlyChangesCaseIsRefused` | `23001`. **The wrong implementation this must fail against**: routing this call through the registering helper, which answers `23505` |
| `writingTheIdenticalNameChangesNothing` | The write succeeds. **Fails against** routing this call through the registering helper, which raises on the already-spent string |
| `anInsertCarryingANameIsNotAViolation` | `insertPlayerWithName` still lands a name |
| `theCoinWriteDoesNotFireTheTrigger` | `"alice"` registers and lands, and the coin write leaves it alone |

## Acceptance criteria

- [ ] Every test in `duels.poker.server.db.DisplayNamePermanenceTest` passes with its assertions
      unchanged
- [ ] `insertPlayerWithName` and `updatePlayerName` each register before their `player` write
- [ ] A raw helper exists that does not register, and exactly three test methods call it:
      `aNamedProfileCannotBeRenamed`, `aRenameThatOnlyChangesCaseIsRefused` and
      `writingTheIdenticalNameChangesNothing`
- [ ] `clearPlayerName` still issues `UPDATE player SET display_name = NULL WHERE id = ?` and nothing
      else
- [ ] No assertion in the file is changed, weakened or removed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
