---
schema: 2
id: TASK-010819
title: Read and write a hand log, and refuse a version this build does not know
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, serialization, log]
depends_on: [TASK-010818]
verify:
  - ./gradlew :poker-engine:test --tests '*HandLogJsonTest'
  - ./gradlew :poker-engine:check
---

## Goal

One pair of functions writes a hand log and reads it back, and a log whose version this build does
not know is rejected by name rather than read on a best-effort basis.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/HandLogJson.kt` | create |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandLogJsonTest.kt` | create |

## Scope

- Two public functions with KDoc, in the existing package `duels.poker.engine.log`:

  ```kotlin
  public fun encodeHandLog(log: HandLog): String
  public fun decodeHandLog(text: String): HandLog
  ```

- One private `Json` instance shared by both, configured `encodeDefaults = true`. Without it
  `version` — a constructor property with a default — is never written, and an unversioned log is
  exactly the thing that gets silently misread three schema versions later.
- `decodeHandLog` checks the version **before** decoding the value: parse the text to a
  `JsonElement`, read the `version` member of the top-level object, and require that it is present
  and equal to `HAND_LOG_VERSION`. Then decode from that same element. Reading the version out of
  an already-decoded `HandLog` would be too late — the default would have filled it in.
- Three failure modes, each an `IllegalArgumentException` whose message names the problem:
  - a version other than `HAND_LOG_VERSION`: the message contains both the version found and the
    version expected,
  - no `version` member at all: the message says so, and does not fall back to the default,
  - text that is not a JSON object, or is truncated: the message includes the underlying parser
    message, which carries the offset.
- Migration of older, known versions is not implemented and not needed: `HAND_LOG_VERSION` is 1,
  so "older and known" is currently the empty set. The rule the ticket establishes is *reject
  loudly*, which is the half `STORY-0108` requires.

## Out of scope

- Writing to a file or any other I/O — the engine takes a `String` and returns a `String`;
  `ADR-0010` narrowed the dependency rule, it did not repeal the no-I/O clause.
- Match logs — `TASK-010826` mirrors this shape for them.
- Changing `HandLog` or `HAND_LOG_VERSION`.

## Tests

`HandLogJsonTest`, JUnit 5, package `duels.poker.engine.log`. Build logs from `playRandomHand`
with a private local helper, as `TASK-010818` did.

| Test | Proves |
| --- | --- |
| `roundTripsTenRandomHands` | for seeds `1..10`, `decodeHandLog(encodeHandLog(log)) == log` |
| `writesTheVersionEvenThoughItIsADefault` | `encodeHandLog(log)` contains `"version":1` |
| `rejectsAVersionThisBuildDoesNotKnow` | text with `"version":2` throws `IllegalArgumentException` whose message contains both `2` and `1` |
| `rejectsALogWithNoVersionMember` | the `version` member removed from a valid log's text throws `IllegalArgumentException` |
| `rejectsTruncatedText` | `encodeHandLog(log).dropLast(20)` throws `IllegalArgumentException` |
| `rejectsTextThatIsNotAnObject` | `"[]"` throws `IllegalArgumentException` |

## Acceptance criteria

- [ ] `HandLogJsonTest.roundTripsTenRandomHands` passes
- [ ] `HandLogJsonTest.writesTheVersionEvenThoughItIsADefault` passes
- [ ] `HandLogJsonTest.rejectsAVersionThisBuildDoesNotKnow` passes
- [ ] `HandLogJsonTest.rejectsALogWithNoVersionMember` passes
- [ ] `HandLogJsonTest.rejectsTruncatedText` passes
- [ ] `HandLogJsonTest.rejectsTextThatIsNotAnObject` passes
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
