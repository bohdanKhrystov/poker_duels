---
schema: 2
id: TASK-040108
title: ProfileResponse carries the name the row holds
type: task
status: backlog
parent: STORY-0401
module: poker-server
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [server, http, read-path, identity]
depends_on: [TASK-040107]
verify:
  - grep -q 'val displayName: String?' poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ProfileEndpointsDatabaseTest' -PrequireDocker=true
---

## Goal

`ProfileResponse` gains `displayName: String?`, and `profileOf` fills it from the column rather than
from a placeholder.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | modify |
| `docs/adr/ADR-0021-a-profile-gains-a-display-name.md` | read — the wire shape, and why the field takes no default |

## Scope

- `ProfileResponse` gains `val displayName: String?` — **no default value**. `Application.module()`
  installs a `Json` with `encodeDefaults = false`, so a defaulted property would be absent from the
  response body instead of present as `null`. Its KDoc says `null` means *never set* and that the
  server fabricates no placeholder (`ADR-0029` §6).
- `profileOf` selects `display_name` alongside `id` and `coin_balance` and passes it through.
  `ResultSet.getString` returns `null` for a SQL `NULL`, which is exactly the value wanted — no
  `?: ""`, no `orEmpty()`.
- The `profileResponse` builder gains `displayName: String? = null`. This is the one parameter with
  a default, and the reason is that most tests are not about the name; **any test that asserts the
  name passes it explicitly**, which `TASK-040109` does.

## Out of scope

- `DuelSummaryResponse.opponentDisplayName` and the join — `STORY-0402`.
- New assertions about the name — `TASK-040109`, in the two test files that own them.
- `docs/protocol.md` — `TASK-040118`, which owns the document and its test together.

## Tests

No new tests here; this ticket is the widening, and its gate is that four existing suites — two of
them against the database — still pass with the field threaded through. `TASK-040109` is the proof
and lands immediately after.

## Acceptance criteria

- [ ] `ProfileResponse.displayName` is `String?` with **no** default value
- [ ] `PostgresProfileReads.profileOf` reads the column, and nothing in the file substitutes a
      value for `null`
- [ ] Every test in `ProfileDtosTest`, `ProfileRouteTest`, `PostgresProfileReadsTest` and
      `ProfileEndpointsDatabaseTest` passes, with no assertion changed or weakened
- [ ] The only default value introduced anywhere in this diff is the builder's, in the test tree
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
