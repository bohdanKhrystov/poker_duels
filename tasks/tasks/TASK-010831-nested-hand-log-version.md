---
schema: 2
id: TASK-010831
title: A hand log with an unknown version is refused inside a match log too
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [engine, log, serialization]
depends_on: [TASK-010826]
verify:
  - ./gradlew :poker-engine:test --tests '*MatchLogSerializationTest'
  - ./gradlew :poker-engine:test --tests '*HandLogJsonTest'
  - ./gradlew :poker-engine:check
---

## Goal

`decodeMatchLog` checks the match log's own version and nothing else. A `MatchLog` whose outer
`version` is current but whose third `HandLog` carries a version this build does not know decodes
**silently, with no exception anywhere**.

Found during the `TASK-010826` review, verified by instrumentation. It is outside that ticket's
acceptance criteria, which asked only for the outer guard — so it is filed rather than folded in.

## Why it matters

The version guard exists so that a log written by a future build is refused rather than
misinterpreted. Today that promise holds for a hand log read on its own through `decodeHandLog`,
and for the match log's own envelope — but not for the hands *inside* a match log, which is how
hands will actually be stored once a duel is persisted.

The gap exists because there are two codecs and only one of them checks:

- `decodeHandLog` (`HandLogJson.kt`) checks `version == HAND_LOG_VERSION`
- `decodeMatchLog` (`MatchLogJson.kt`) inspects only the top-level `version`, then calls
  `decodeFromJsonElement` for the whole tree — never routing the nested hands through
  `decodeHandLog`
- `HandLog.init` only asserts `version >= 1`, not that the version is one this build knows

So each piece behaves correctly in isolation and the composition leaks.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/MatchLogJson.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/MatchLogSerializationTest.kt` | modify |

Read `HandLogJson.kt`, `HandLog.kt` and `MatchLog.kt`. Modify none of them — in particular, do not
tighten `HandLog.init` to demand the current version, which would make an older in-memory log
unconstructible and is a different decision from what a *decoder* accepts.

## Scope

Make `decodeMatchLog` refuse a match log containing a hand log whose version this build does not
know, naming which hand and both versions.

Decide and state in a comment whether the check walks the parsed `JsonElement` before decoding
(consistent with how both existing guards work) or routes each nested hand through `decodeHandLog`.
Prefer the former if it keeps one parse; say why in the comment either way.

## Tests

| Name | Asserts |
| --- | --- |
| `rejectsAMatchLogWhoseNestedHandHasAnUnknownVersion` | a good outer version with a bad version on the *third* hand throws, and the message names the hand and both versions |
| `rejectsAMatchLogWhoseNestedHandHasNoVersionMember` | a nested hand missing `version` is refused, not defaulted |
| `aGoodMatchLogStillDecodes` | the existing round trip is unaffected |

Target the nested field specifically. A plain `String.replace` is global and will corrupt every
version in the document — the defect this ticket's parent was sent back for. Splice by index, and
comment why.

## Done

All three `verify:` commands exit 0, and a match log carrying a hand from a future build is refused
rather than silently misread.
