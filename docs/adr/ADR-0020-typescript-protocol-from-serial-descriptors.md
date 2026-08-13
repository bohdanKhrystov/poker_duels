# ADR-0020 — TypeScript protocol types are emitted from the serial descriptors

- **Status:** Accepted
- **Date:** 2026-08-13
- **Resolves:** `DEC-007`
- **Unblocks:** `STORY-0203`

## Context

`ADR-0003` accepted two languages across the socket on one condition: the shared types are
generated from the Kotlin definitions, never hand-written. `STORY-0203` is where that happens, and
`ADR-0017` changed the shape of the problem — later stories extend the existing `ClientMessage` and
`ServerMessage` sealed hierarchies routinely, so a new variant is now a normal event the mechanism
must absorb cheaply, not a rare one it may make expensive.

Three forces are in tension:

1. **The wire truth lives in the `SerialDescriptor`s, not in the Kotlin class shapes.** `Card` is a
   `@JvmInline value class` over an `Int`, but `CardSerializer` declares a `STRING` descriptor and
   writes `"As"`. Any generator that reflects over classes rather than descriptors emits a lie for
   every custom serializer, silently. The descriptor is the only artefact that cannot disagree with
   what `protocolJson` puts on the wire, because it is what `protocolJson` reads.
2. **A generator nobody runs is worse than no generator.** The checked-in TypeScript looks
   authoritative while being wrong, and the failure surfaces weeks later as an `undefined` in a
   browser. Whatever generates must be paired with something that fails CI on drift.
3. **The dependency budget.** `poker-engine` depends on nothing beyond its allowlist
   (`ADR-0010`); the server build is JVM-only; the future web build must not need a JVM. Node
   tooling in the generation path taxes every protocol change with a second toolchain.

The reachable descriptor surface today is small and closed: SEALED (`ClientMessage`,
`ServerMessage`, `PlayerAction`, `Rejection`, `GameEvent`), CLASS, OBJECT
(`Rejection.HandComplete`), ENUM (`ActionType`, `Street`), LIST (`List`, `Set`), and primitives —
including the custom `Card` → STRING case. The structural walk already exists in this repository:
`ProtocolDescriptors.kt` and `ProtocolPayloadTest` (`TASK-020211`) walk these same descriptors to
police the payload.

## Decision

**A Kotlin emitter in `poker-server` walks the protocol's `SerialDescriptor`s and prints one
TypeScript file, which is committed; a Gradle verification task regenerates and byte-compares on
every `check`, so drift fails the existing CI.**

Concretely:

- **The emitter is our code, in `poker-server`** — a few hundred lines under
  `duels.poker.server.protocol.typescript`, walking `ClientMessage.serializer().descriptor` and
  `ServerMessage.serializer().descriptor` recursively, the same walk `ProtocolPayloadTest` already
  performs. No third-party generator, no Node in the generation path, nothing added to
  `poker-engine`.
- **The output is `web-client/src/protocol/protocol.gen.ts`**, committed to the repository.
  `docs/architecture.md` already reserves `web-client/`; `EPIC-03` scaffolds the module around the
  file, which is exactly where its consumer imports it. It is a plain `.ts` containing only
  `export type` / `export interface` declarations — types only, no runtime code — plus a header
  naming the generating command. It also declares `export type ProtocolVersion = 1` from
  `PROTOCOL_VERSION`, so a client that hard-codes a stale version fails `tsc` rather than failing
  the handshake at runtime.
- **Two Gradle tasks in `poker-server`:**
  - `./gradlew :poker-server:generateProtocolTypes` writes the file. This is the command a
    developer runs after a protocol change, and the command the file's header names.
  - `./gradlew :poker-server:verifyProtocolTypes` regenerates into `build/` and **byte-compares**
    against the committed file, failing with the regeneration command in the message. It is wired
    as a dependency of `poker-server`'s `check`, and CI already runs `./gradlew check`
    (`.github/workflows/build.yml`), so a Kotlin protocol change without a matching regeneration
    fails the same build that compiles it. Byte comparison inside the task, not `git diff`, so the
    check is indifferent to working-tree state; the two are equivalent on a clean CI checkout.
- **`tsc --noEmit` under `strict` runs as a CI step** with a pinned TypeScript version (Node is
  available in CI, never in any Gradle build). Once `EPIC-03` gives `web-client` its own
  typecheck, that step folds into it.

The mapping is fixed here so every ticket emits the same shapes:

| Descriptor | TypeScript |
| --- | --- |
| SEALED | Discriminated union; discriminator key is `protocolJson`'s `classDiscriminator` (`"type"`); each variant's literal is its element serial name — the `@SerialName`, taken structurally, never from a hand-kept list |
| CLASS | `interface` of its elements |
| OBJECT | `{ type: "Name" }` — discriminator only |
| ENUM | Union of string literals (the entries' serial names) |
| LIST (`List`, `Set`) | `readonly T[]` |
| PRIMITIVE | STRING/CHAR → `string`, BOOLEAN → `boolean`, numeric kinds → `number` (LONG included: all protocol quantities are chip counts and sequence numbers, far under 2^53) |
| Nullable | `T \| null` |

Every field is **required** in TypeScript: `protocolJson` sets `encodeDefaults = true`, so
server-emitted fields are always present, and `ignoreUnknownKeys = false` cuts the other way —
the client should be forced by `tsc` to send what the server checks, `Hello.protocolVersion`
above all. Type names are the last dotted segment of the descriptor's `serialName`; the emitter
fails on a collision, and it **fails loudly on any descriptor kind not in the table** (MAP,
CONTEXTUAL, open polymorphism) rather than guessing — the first map on the wire extends the table
in a reviewed diff, not in an emitter's default branch. Output is deterministic — declaration
order, LF newlines — so the byte comparison is exact.

## Consequences

**What it buys.** Adding a `ServerMessage` variant — the routine event `ADR-0017` made it — costs
one Gradle command: the sealed parent's descriptor enumerates its own subtypes, so the new variant
appears in the union without the emitter changing. Forgetting the command fails CI with the
command in the error. The reviewer sees the TypeScript diff next to the Kotlin diff in the same
PR, which is the drift review `STORY-0203` asks for. Custom serializers are handled for free,
forever, because the walk sees the wire format and nothing else. The web build needs no JVM; the
server build needs no Node.

**What it costs.** We own the emitter, including its future: the first MAP, value class without a
custom serializer, or contextual descriptor is our code to extend, not a library's. The committed
file can conflict when two branches touch the protocol; the resolution rule is *regenerate, never
hand-merge* — the verify task makes a hand-merge that disagrees with the Kotlin unmergeable
anyway. `web-client/` exists as a directory one epic early, and `EPIC-03` must scaffold around it.

**What it forecloses.** Little — this is the cheapest decision to reverse, which weighed in its
favour. The committed file's shape is the contract; if the emitter is ever replaced (by a matured
library, or by JSON Schema when a second client language appears), the replacement must merely
reproduce the same file, and the byte-comparing verify task is the proof it does.

## Alternatives considered

**An off-the-shelf kotlinx→TypeScript generator** (`KxsTsGen` is the real candidate — it walks
`SerialDescriptor`s, which is exactly the right idea). Least code to own, and its author has
already met the corner cases. Rejected: it is pre-1.0 and dormant, its output shape is not ours to
pin, and our descriptor surface is six kinds — owning ~300 lines outweighs owning a fork of an
abandoned dependency, which the story itself flagged as the failure mode. The class-reflection
tools (`ts-generator`, `typescript-generator`) are more mature but read Kotlin classes, not
descriptors, so `Card` would emit as an object and every future custom serializer would be a
silent wire mismatch — disqualified outright by force 1.

**JSON Schema from Kotlin, `quicktype` to TypeScript.** The schema is language-neutral and worth
having the day a second client language appears; both halves are well-trodden. Rejected: no
maintained kotlinx→JSON Schema generator honours custom serializers, so we would write the
descriptor walk anyway — then also carry `quicktype`, Node in the generation path, and a schema
artefact that is itself a second thing that can drift. Two generated artefacts to police instead
of one, for a second consumer that does not exist.

**Hand-written TypeScript with a conformance test.** No generator to maintain, and today's
protocol is small enough to write in an afternoon. Rejected on what `ADR-0017` made routine: every
new variant becomes a manual two-language edit, and a conformance test that truly proves a TS type
matches a Kotlin descriptor must either parse TypeScript or maintain golden fixtures per variant —
per-variant manual work that decays into exactly the silent drift `ADR-0003` took generation as
the condition against.

**Generate on demand instead of committing.** No file to conflict, no verify task. Rejected: the
web build would need a JVM to obtain its own types, and the reviewer would never see the diff a
protocol change causes — the story's design notes already name both as disqualifying, and this ADR
confirms rather than revisits that.
