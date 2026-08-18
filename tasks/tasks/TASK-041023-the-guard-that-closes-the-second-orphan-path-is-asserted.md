---
schema: 2
id: TASK-041023
title: The guard that closes the second orphan path is asserted, not merely read
type: task
status: ready
parent: STORY-0410
module: poker-server
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [server, db, test, identity, regression]
depends_on: [TASK-041013]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.PostgresProfileWritesTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0051` §2's orphaned-registry-row defect has two paths. `TASK-041003` covers the first — the
`UPDATE` matching zero rows. `TASK-041013` established that the second is **closed**: the permanence
trigger cannot raise after a successful registry insert, because `SET_NAME_SQL` carries
`AND display_name IS NULL` and the trigger's raise condition requires `OLD.display_name IS NOT NULL`.
The two are mutually exclusive, so the trigger never runs for a row that clause excluded.

That closure rests entirely on reading the code. **Delete `AND display_name IS NULL` from
`SET_NAME_SQL` and the whole suite stays green** — `AlreadyNamed` would still be reported, because
the second write is still refused, only now by the trigger and after the registry row is already
written. The orphan reappears and nothing says so.

This ticket makes the guard assert itself.

## Scope

- One test in `PostgresProfileWritesTest`: a player who **already holds** a name attempts to set a
  second, different, previously-unregistered name. Afterwards, `name_registry` must contain **no row**
  for that second string.
- It must exercise `PostgresProfileWrites.setDisplayName` — the production path — not a hand-built
  sequence. The point is that the production statement carries the guard.
- The result assertion (`AlreadyNamed`) stays, but it is not what this test is for. Without the
  registry assertion the test is `aRefusedSecondNameLeavesNoRegistryRow` again.

## Out of scope

- The migration. The guard being asserted lives in Kotlin, in `SET_NAME_SQL`.
- Changing `SET_NAME_SQL`. It is correct; this ticket pins it.
- The first orphan path — `TASK-041003` owns it.

## Tests

`PostgresProfileWritesTest`, `-PrequireDocker=true`. One test added; nothing existing edited.

| Test | Proves |
| --- | --- |
| `aSecondNameIsNeverRegisteredForAnAlreadyNamedPlayer` | After a refused second `setDisplayName`, `SELECT count(*) FROM name_registry WHERE name = <second>` is `0`. **Fails against** a `SET_NAME_SQL` without `AND display_name IS NULL`: the registry insert lands, the permanence trigger then refuses the `player` write, and the second string is `TAKEN` by nobody — forever, since a registered name is never released |

## Acceptance criteria

- [ ] `PostgresProfileWritesTest.aSecondNameIsNeverRegisteredForAnAlreadyNamedPlayer` passes
- [ ] It asserts the registry contains no row for the refused name, by count, not by result type alone
- [ ] It calls `PostgresProfileWrites.setDisplayName`, not a hand-built `INSERT`/`UPDATE` pair
- [ ] Removing `AND display_name IS NULL` from `SET_NAME_SQL` makes it fail — demonstrate and revert
- [ ] No existing test is edited or renamed
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
