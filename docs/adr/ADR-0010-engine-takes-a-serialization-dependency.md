# ADR-0010 — The engine may depend on kotlinx.serialization

- **Status:** Accepted
- **Date:** 2026-08-12
- **Resolves:** `DEC-006`
- **Amends:** the "depends on nothing" non-negotiable in `CLAUDE.md` and `docs/architecture.md`

## Context

`poker-engine` was built under a stated non-negotiable: *it depends on nothing*. That is not
convention — `checkNoDependencies` in `poker-engine/build.gradle.kts` allowlists `kotlin-stdlib`
alone and throws on anything else, and `check` depends on it, so a stray dependency fails the
build. `TASK-010103` exists to enforce exactly this.

`STORY-0108` needs a hand log that can be written and read back. The Kotlin standard library has no
serialisation, so a real format means either an external library, a hand-rolled parser, or a
separate module holding DTOs that mirror the engine's types.

The owner's call, recorded here as made: take the dependency directly in the engine.

## Decision

`poker-engine` may depend on `kotlinx.serialization`, and its domain types may carry
`@Serializable`.

- The engine's build applies the `kotlin("plugin.serialization")` compiler plugin.
- `checkNoDependencies` is narrowed rather than deleted: it now allowlists `kotlin-stdlib` and
  `kotlinx-serialization` and still fails on anything else.
- Every other clause of the purity rule stands unchanged: **no networking, no I/O, no clock, no
  framework types, no `kotlin.random.Random`.** Determinism through the injected `Rng` is
  untouched.

## Consequences

**What it buys.** A real log format with no mapping layer and no fourth module. Serialisation
annotations sit on the types they describe, so a field cannot be added to a domain type and
silently forgotten in a parallel DTO — which is the failure mode the mapping-layer alternative
carries forever.

**What it costs.** The engine is no longer a zero-dependency library. Anything embedding it now
inherits `kotlinx.serialization` and a compiler plugin. The domain types carry annotations that are
about transport, not about poker, which is a small but permanent smear on the model.

**What it forecloses.** This is the hard-to-reverse direction, and it should be recorded as such:
once `GameState` and `GameEvent` are annotated and downstream code depends on their wire format,
removing the dependency means changing the wire format too. The narrowed `checkNoDependencies` is
what stops the erosion continuing — the next dependency has to argue for itself in a new ADR rather
than slipping in behind this one.

**On the guard.** It is narrowed, not removed. A guard deleted the first time it is inconvenient
protects nothing afterwards; a guard with an explicit, short allowlist keeps doing its job.

## Alternatives considered

**A separate `:poker-engine-log` module.** Depends on the engine and on `kotlinx.serialization`,
holding DTOs that map to and from the engine's types. Keeps the engine's zero-dependency property
intact and gives real JSON immediately. Rejected by the owner as not worth the mapping layer and
the fourth module; the mapping layer is also a genuine ongoing cost, since it must be kept in step
with the domain types by hand.

**A hand-written line format inside the engine.** Zero dependencies, engine-local. Rejected because
hand-rolled parsers are where subtle bugs live, and it would be the hardest option to walk back —
a bespoke format acquires consumers quickly.

**Defer to `EPIC-02`.** Nothing consumes a serialised log today; replay and the fuzzing harness both
work in memory. Rejected because the log format binds the server's persistence and the analysis
tooling, and deciding it late means deciding it under delivery pressure.
