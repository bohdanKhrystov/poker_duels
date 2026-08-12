---
schema: 2
id: TASK-010818
title: A hand log round-trips through JSON
type: task
status: done
parent: STORY-0108
module: poker-engine
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [engine, serialization, log, replay]
depends_on: [TASK-010815, TASK-010817]
verify:
  - ./gradlew :poker-engine:test --tests '*HandLogSerializationTest'
  - ./gradlew :poker-engine:check
---

## Goal

A `HandLog` written to JSON and read back is the same value — seed, header, every action and every
event — over fifty hands nobody hand-picked.

## Files

| File | Action |
| --- | --- |
| `poker-engine/src/main/kotlin/duels/poker/engine/log/HandLog.kt` | modify |
| `poker-engine/src/test/kotlin/duels/poker/engine/log/HandLogSerializationTest.kt` | create |

## Scope

- One annotation: `@Serializable` on `HandLog`, plus its import. No field is added, removed or
  reordered, and the `init` validation stays exactly as it is — it runs on decode, which is what
  makes a corrupt log fail loudly instead of decoding into an impossible value.
- `version` is a constructor property with a default. kotlinx.serialization **omits defaults
  unless told otherwise**, so a plain `Json` writes no `version` member at all. That is the whole
  reason `TASK-010819` exists; this ticket only has to pin the fact with a test.

## Out of scope

- The versioned reader and writer, `encodeHandLog` / `decodeHandLog` — `TASK-010819`. This ticket
  adds no function.
- Match-level logs — `TASK-010820`, `TASK-010826`.

## Tests

`HandLogSerializationTest`, JUnit 5, package `duels.poker.engine.log`. Build logs from
`playRandomHand(seed)` with a private local helper in this file, the same shape
`HandLogReplayPropertyTest` uses (`seed`, then the opening state's hand number, button, stacks and
blinds, then the played actions and events) — that helper is private to its own file, so copy it,
do not modify it.

| Test | Proves |
| --- | --- |
| `aHandLogRoundTripsOverFiftyRandomHands` | for seeds `1..50`, `decode(encode(log)) == log` through `HandLog.serializer()` |
| `theSameLogAlwaysEncodesToTheSameText` | encoding one log twice gives identical text, which every stored-log comparison depends on |
| `theVersionIsOmittedUnlessDefaultsAreEncoded` | with a plain `Json`, the text contains no `"version"`; with `Json { encodeDefaults = true }` it contains `"version":1` |
| `decodingALogWithAGapInEventSequencesFails` | the literal below, with its second event at sequence 2, throws `IllegalArgumentException` |
| `decodingALogWhoseFirstEventIsNotHandStartedFails` | the literal below, whose only event is an `ActionOn`, throws `IllegalArgumentException` |

The two literals, so neither test has to guess the wire shape:

```json
{"seed":1,"handNumber":1,"buttonSeat":0,"stacks":[1000,1000],"smallBlind":50,"bigBlind":100,"actions":[],"events":[{"type":"HandStarted","sequence":0,"handNumber":1,"buttonSeat":0,"smallBlind":50,"bigBlind":100,"stacks":[1000,1000]},{"type":"ActionOn","sequence":2,"seat":0}],"version":1}
{"seed":1,"handNumber":1,"buttonSeat":0,"stacks":[1000,1000],"smallBlind":50,"bigBlind":100,"actions":[],"events":[{"type":"ActionOn","sequence":0,"seat":0}],"version":1}
```

## Acceptance criteria

- [ ] `HandLogSerializationTest.aHandLogRoundTripsOverFiftyRandomHands` passes
- [ ] `HandLogSerializationTest.theSameLogAlwaysEncodesToTheSameText` passes
- [ ] `HandLogSerializationTest.theVersionIsOmittedUnlessDefaultsAreEncoded` passes
- [ ] `HandLogSerializationTest.decodingALogWithAGapInEventSequencesFails` passes
- [ ] `HandLogSerializationTest.decodingALogWhoseFirstEventIsNotHandStartedFails` passes
- [ ] `HandLogTest` and `HandLogReplayPropertyTest` still pass unchanged
- [ ] No file outside the two in the Files table is modified
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
