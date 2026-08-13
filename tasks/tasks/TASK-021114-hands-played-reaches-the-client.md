---
schema: 2
id: TASK-021114
title: Hands played reaches the client instead of null
type: task
status: done
parent: STORY-0211
module: poker-server
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [server, read-path, protocol]
depends_on: [TASK-021014]
verify:
  - ./gradlew :poker-server:test --tests '*PostgresProfileReadsTest' -PrequireDocker=true
  - ./gradlew :poker-server:test --tests '*ProfileDtosTest'
  - ./gradlew :poker-server:check -PrequireDocker=true
---

## Goal

`TASK-021014` stores `hands_played`. This returns it, and `handsPlayed` stops being `null`
(`ADR-0019`).

Every result line currently carries `handsPlayed = null` with `DEC-014` named in its KDoc, and a
test pinning that. `DEC-014` is answered, so both the null and the test that guards it must go.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/db/PostgresProfileReads.kt` | modify |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/http/ProfileDtos.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/db/PostgresProfileReadsTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/protocol/http/ProfileDtosTest.kt` | modify |
| `poker-server/src/test/kotlin/duels/poker/server/http/ProfileRouteTest.kt` | modify |

> **Files table corrected during implementation.** The last two were missing. Both construct
> `DuelSummaryResponse` and stop compiling once `handsPlayed` is non-null, so `files_touched`
> should have been 5. Each change supplies a hand count where one is now required.

## Scope

- `recentDuelsOf` selects `d.hands_played` and maps it into the response.
- `DuelSummaryResponse.handsPlayed` becomes a non-null `Int`. **Do not give it a default value** —
  no DTO in this file declares one, deliberately: an undefaulted property cannot be omitted by any
  `Json` configuration, which is what keeps a zero from vanishing off the wire.
- **Delete `handsPlayedIsNullWhileTheColumnDoesNotExist`.** It asserts a condition that is no longer
  true. Leaving it passing would mean the column was not actually wired through — it is the test
  that proves this ticket did nothing.
- Remove the `DEC-014` KDoc notes that say the field is null pending a decision, and replace them
  with what the field now means. A stale "pending decision" comment is worse than none.

## Tests

| Name | Asserts |
| --- | --- |
| `aResultLineCarriesTheHandCount` | a duel recorded as lasting N hands reads back with `handsPlayed == N`, through the real read path |
| `theHandCountSurvivesEncoding` | a `DuelSummaryResponse` encodes with `handsPlayed` present in the JSON text — including when it is **zero**, which is the value an undefaulted property protects |

The zero case matters for the same reason `TASK-021101`'s zero-balance test does: a duel that ended
on hand zero is unusual but representable, and a defaulted field would drop it silently.

## Out of scope

`docs/protocol.md` says `handsPlayed` is always null. Updating it is a documentation change with its
own test (`HttpEndpointDocumentationTest`) — **file it as a follow-up rather than doing it here**, or
this ticket touches five files. Say in your report that the document is now stale.

## Done

All three `verify:` commands exit 0, a result line carries its hand count end to end, and no test or
KDoc still claims the field is null.
