# ADR-0003 — Technology stack

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

A solo project built largely by AI agents on a personal subscription. The stack has to be
boring, well-represented in training data, fast to iterate on, and something the author is
already fluent in — because the human's job here is reviewing, and you cannot review what you
cannot read.

The author is a senior Android developer: Kotlin and JVM tooling are home ground.

## Decision

| Layer | Choice |
| --- | --- |
| Engine, analysis, bots, CLI | **Kotlin / JVM**, no framework |
| Server | **Ktor**, WebSocket transport |
| Persistence | **PostgreSQL** (deferred until v0.2) |
| Web client | **React**, **TypeScript**, **Tailwind** |
| Build | **Gradle**, Kotlin DSL, version catalog |
| Tests | **JUnit 5** + **kotest** property testing |
| Delivery | Docker; host chosen later (Fly.io / Railway / VPS) |

Not chosen, deliberately:

- **No game engine.** Unity and Godot bring a renderer, an asset pipeline and a runtime, and
  this game draws a table, ten cards and some buttons. Nothing to gain, plenty to carry.
- **No Kotlin Multiplatform for the MVP.** The engine is written as plain Kotlin with no JVM
  dependencies, so converting it to a multiplatform module later is mechanical. Doing it now
  would add build complexity before there is any consumer that needs it.
- **No ORM in the engine or near it.** The engine touches no database, ever.

## Consequences

**Gained**

- The human reviewer is fast on the largest and most correctness-critical part of the system.
- Kotlin's sealed classes and data classes fit the event/state model almost exactly; the
  compiler enforces exhaustive handling of actions and events.
- JVM performance is more than sufficient for millions of simulated hands.
- React means the client is the part with the most abundant AI training data, which is where
  generated code needs the least supervision.

**Cost**

- Two languages, so shared types are duplicated across the WebSocket boundary. Mitigated by
  generating the TypeScript protocol types from the Kotlin definitions rather than hand-writing
  them (a story in the server epic).
- JVM cold start is unremarkable but irrelevant for a long-lived socket server.

## Alternatives considered

- **TypeScript everywhere** — one language, shared types for free. Rejected: the engine is the
  part that most needs a strong type system and fast property-based testing, and it is the part
  the author would review most slowly in TypeScript.
- **Unity or Godot** — rejected as above. The reasoning would flip entirely for a game with
  real graphics; it does not apply to this one.
- **Rust for the engine** — genuinely good fit for a deterministic simulator, and the fastest
  option for mass simulation. Rejected: it would make the human the bottleneck on review of
  exactly the code that matters most.
