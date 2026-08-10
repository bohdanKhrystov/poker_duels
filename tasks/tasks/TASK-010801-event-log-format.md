---
id: TASK-010801
title: Versioned event log format and serialization
type: task
status: backlog
parent: STORY-0108
module: poker-engine
estimate: M
labels: [engine, replay, persistence]
depends_on: [TASK-010703]
---

## Goal

A match's events can be written down and read back with nothing lost — the substrate for replay,
audit, analysis and bot training.

## Context

- [`docs/architecture.md`](../../docs/architecture.md) — the engine may not take an
  implementation dependency, which constrains how this is done.
- [`docs/adr/ADR-0001-event-sourced-engine-contract.md`](../../docs/adr/ADR-0001-event-sourced-engine-contract.md).

## Scope

- A serialization format for `GameEvent` and the match header (format, seed, participants).
- **Decide where it lives.** Either hand-write it inside `poker-engine` with no dependency, or
  put it in a `poker-serialization` module that depends on the engine. The second is cleaner and
  costs a module; the first keeps the module count down and costs hand-written code. Pick one,
  and write an ADR — this is exactly the kind of choice a future reader will question.
- Schema versioning: a version on the log, and a reader that rejects an unknown version loudly
  rather than guessing.
- The seed is part of the log. Without it a log is not replayable.

## Out of scope

- Storing logs in a database — EPIC-02.
- Compression.
- A stable format for external consumers. This is internal until something outside the project
  reads it.

## Files

| File | Action |
| --- | --- |
| `.../MatchLog.kt` | create |
| `.../MatchLogSerializer.kt` | create |
| `.../MatchLogSerializerTest.kt` | create |
| `docs/adr/ADR-0006-event-log-serialization.md` | create |

## Acceptance criteria

- [ ] Every `GameEvent` subtype round-trips unchanged.
- [ ] The seed and the `DuelFormat` are in the log.
- [ ] A log with an unknown schema version is rejected with a clear error, never misread.
- [ ] A log with a *known older* version is either read correctly or rejected explicitly —
      never silently misinterpreted.
- [ ] The engine module's dependency rule still holds, and the test asserting it still passes.
- [ ] An ADR records where the serializer lives and why.

## Tests

- `MatchLogSerializerTest` — round trip over every event type, version handling.
- Property: for generated matches, `deserialize(serialize(log)) == log`.

## Definition of done

Standard, per [`tasks/README.md`](../README.md): build green, tests green, `/code-review` run
with findings fixed or answered, CI green, status `done`, `BOARD.md` updated, and
**squash-merged into `develop`** by a PR linking this ticket. Not done until the PR is merged.
