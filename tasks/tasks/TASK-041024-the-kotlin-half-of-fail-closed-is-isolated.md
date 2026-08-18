---
schema: 2
id: TASK-041024
title: The Kotlin half of fail-closed is isolated from the foreign key that covers for it
type: task
status: done
parent: STORY-0410
module: poker-server
estimate: XS
tier: sonnet
review: deep
files_touched: 1
labels: [server, db, test, identity, moderation, regression]
depends_on: [TASK-041016]
verify:
  - ./gradlew :poker-server:test --tests 'duels.poker.server.db.NameBlocklistTest' -PrequireDocker=true
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`ADR-0038` calls fail-closed *the single thing most likely to be got backwards*, and `TASK-041016`
planted a real failure to prove the system closes: rename `name_registry` away, and
`setDisplayName` throws instead of writing.

It closes. But **two mechanisms close it independently, and that test cannot tell them apart.**
Measured against the fail-open mutant `ADR-0038` names — a `catch (SQLException)` that swallows the
registry failure and falls through to a bare `UPDATE player` —
`aRegistryThatCannotBeReachedRefusesTheName` still passes, because
`player_display_name_registered` refuses that `UPDATE` with `23503`: the name is in no registry, so
the foreign key has nothing to point at.

The observed result under the mutant:

```
outcome=THREW sqlState=23503
  ERROR: insert or update on table "player" violates foreign key constraint
  "player_display_name_registered"
  Detail: Key (display_name)=(Fresh) is not present in table "name_registry_unreachable".
storedDisplayName=null
```

That is defence in depth working, and it is good news. It is also why the Kotlin half is currently
untested: delete the rethrow and the schema still saves us — until someone drops the foreign key,
or a future write path builds a name the registry already contains.

## Scope

- One test asserting the **rethrow itself**, not its downstream consequence: after the registry is
  made unreachable, `setDisplayName` must propagate the `42P01` rather than translate it to any
  `SetNameResult`.
- The way to separate the two mechanisms is to make the fallback `UPDATE` **satisfiable**: register
  the name first, *then* rename the table away. A fail-open path would then find the foreign key
  content — the row exists under the renamed table's constraint — and the Kotlin rethrow becomes the
  only thing left refusing. Confirm that arrangement actually distinguishes them before relying on
  it; if renaming the table also takes its constraint out of scope for `player`, say so and use a
  different plant.
- Assert the SQLSTATE reaching the caller is `42P01`, and that `display_name` is still `NULL`.

## Out of scope

- Changing `PostgresProfileWrites`. It is correct; this ticket pins it.
- The five other tests in `NameBlocklistTest`.
- Any mocked or in-memory double. The point is the real driver's real SQLSTATE.

## Tests

`NameBlocklistTest`, `-PrequireDocker=true`. One test added; nothing existing edited.

| Test | Proves |
| --- | --- |
| `theRegistryFailureIsRethrownRatherThanTranslated` | With the registry unreachable, the exception reaching the caller carries `42P01` — `PostgresProfileWrites.kt:43`'s `if (failure.sqlState != UNIQUE_VIOLATION_SQLSTATE) throw failure`. **Fails against** a `catch` that swallows it, in an arrangement where the foreign key cannot cover for the fall-through |

## Acceptance criteria

- [ ] `NameBlocklistTest.theRegistryFailureIsRethrownRatherThanTranslated` passes
- [ ] It asserts the propagated SQLSTATE is `42P01`, not merely that something was thrown
- [ ] It fails against a fail-open mutant, demonstrated and quoted — and the demonstration shows the
      foreign key is **not** what refuses in that run
- [ ] No production file is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
