---
schema: 2
id: TASK-010826
title: Read and write a match log, version guard included
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [engine, serialization, log, duel]
depends_on: [TASK-010818, TASK-010820, TASK-010821, TASK-010824, TASK-010825]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchLogSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A whole duel — format, every hand and the match event that ended it — is written down and read
back unchanged, and a match log whose version this build does not know is rejected by name.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLog.kt` | modify |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLogJson.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/MatchLogSerializationTest.kt` | create |

## Scope

- `@Serializable` on `MatchLog`, plus the import. No field changes; the `init` validation runs on
  decode.
- `MatchLogJson.kt`, mirroring `HandLogJson.kt` exactly:

  ```kotlin
  public fun encodeMatchLog(log: MatchLog): String
  public fun decodeMatchLog(text: String): MatchLog
  ```

  one private `Json { encodeDefaults = true }`, and a version check performed on the parsed
  `JsonElement` **before** the value is decoded — the `version` member must be present and equal to
  `MATCH_LOG_VERSION`, and a mismatch, a missing member or unparseable text each throw
  `IllegalArgumentException` naming the problem.
- The six lines of version guard are duplicated from `HandLogJson.kt` rather than shared. Two log
  types with two independent version numbers is the honest shape; a shared helper would have to
  take the expected version as a parameter anyway, and factoring it would put a third file in this
  ticket's budget.

## Out of scope

- Any I/O: strings in, strings out.
- Changing `HandLogJson.kt` — it is not in this ticket's budget.
- Migrating older versions: `MATCH_LOG_VERSION` is 1, so "older and known" is empty. The rule is
  reject loudly.

## Tests

`MatchLogSerializationTest`, JUnit 5, package `duels.poker.engine.log`. Logs come from
`playLoggedDuel(seed)`; keep the seed range small, each duel is many hands.

| Test | Proves |
| --- | --- |
| `aMatchLogRoundTripsOverFiveDuels` | for seeds `1..5`, `decodeMatchLog(encodeMatchLog(log)) == log` — hands, events and format all intact |
| `theHandLogsSurviveWithTheirEvents` | for one decoded log, every `HandLog` equals the original element for element |
| `writesTheVersionEvenThoughItIsADefault` | `encodeMatchLog(log)` contains `"version":1` |
| `rejectsAVersionThisBuildDoesNotKnow` | text whose top-level `version` is `2` throws `IllegalArgumentException` naming both `2` and `1` |
| `rejectsAMatchLogWithNoVersionMember` | the top-level `version` member removed throws `IllegalArgumentException` |
| `rejectsTruncatedText` | `encodeMatchLog(log).dropLast(20)` throws `IllegalArgumentException` |

> The top-level `version` is the match log's own. Each nested `HandLog` carries its own `version`
> member too; `rejectsAVersionThisBuildDoesNotKnow` must change the outer one, not a nested one.

## Acceptance criteria

- [ ] `MatchLogSerializationTest.aMatchLogRoundTripsOverFiveDuels` passes
- [ ] `MatchLogSerializationTest.theHandLogsSurviveWithTheirEvents` passes
- [ ] `MatchLogSerializationTest.writesTheVersionEvenThoughItIsADefault` passes
- [ ] `MatchLogSerializationTest.rejectsAVersionThisBuildDoesNotKnow` passes
- [ ] `MatchLogSerializationTest.rejectsAMatchLogWithNoVersionMember` passes
- [ ] `MatchLogSerializationTest.rejectsTruncatedText` passes
- [ ] `MatchLogTest` and `LoggedDuelPlayerTest` still pass unchanged
- [ ] No file outside the three in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
