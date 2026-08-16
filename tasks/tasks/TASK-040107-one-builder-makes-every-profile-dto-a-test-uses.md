---
schema: 2
id: TASK-040107
title: One builder makes every profile DTO a test uses
type: task
status: done
parent: STORY-0401
module: poker-server
estimate: S
tier: haiku
review: light
files_touched: 3
labels: [server, tests, refactor]
depends_on: [TASK-040106]
verify:
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:test --tests '*ProfileRouteTest'
  - "! grep -n 'ProfileResponse(' poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt"
  - "! grep -n 'DuelSummaryResponse(' poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt"
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

Every `ProfileResponse` and `DuelSummaryResponse` a test constructs comes from one builder, so the
next field either DTO gains is a change to one call site instead of eleven.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtoFixtures.kt` | create |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | read — the two data classes and why no property has a default |

## Scope

- Two `internal fun`s in `ProfileDtoFixtures.kt`, package `duels.poker.server.protocol.http`:
  `profileResponse(...)` and `duelSummaryResponse(...)`, each returning the real DTO.
- **Every parameter that exists today keeps no default**, so no test silently stops saying what it
  is about. Defaults are what the *next* field gets, which is the whole point of the file.
- Rewrite the eleven construction sites — seven `ProfileResponse` and two `DuelSummaryResponse` in
  `ProfileRouteTest`, three and five in `ProfileDtosTest` — to call the builders with the same
  values they pass today.
- **No assertion changes.** Not one expected value, not one test name, not one JSON string. This
  ticket is a call-site move and nothing else; the two `verify` test commands are what proves it.
- A comment at the top of the fixtures file saying why it exists: these DTOs take no default values
  (`ADR-0021`), so a new field breaks every constructor call at once, and `STORY-0402` widens
  `DuelSummaryResponse` next.

## Out of scope

- Adding `displayName` — `TASK-040108`. This ticket must leave both DTOs byte-identical.
- `PostgresProfileReads`, which constructs a `ProfileResponse` in production code and stays as it is.
- Any new test.

## Tests

No new tests. The existing ones are the check: `ProfileDtosTest` (its full set) and
`ProfileRouteTest` (its full set) must pass with their bodies' *assertions* unchanged.

## Acceptance criteria

- [ ] Every test in `ProfileDtosTest` passes, with no expected value, no test name and no JSON
      literal changed
- [ ] Every test in `ProfileRouteTest` passes, under the same rule
- [ ] `grep 'ProfileResponse('` and `grep 'DuelSummaryResponse('` find nothing in either test file —
      the third and fourth `verify` commands
- [ ] `ProfileDtos.kt` is unchanged, and `git diff` on it is empty
- [ ] Neither builder declares a default value for any parameter
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
