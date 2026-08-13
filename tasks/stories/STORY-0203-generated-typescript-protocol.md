---
id: STORY-0203
title: Generated TypeScript protocol types
type: story
status: ready
parent: EPIC-02
module: poker-server
labels: [protocol, typescript, tooling]
depends_on: [STORY-0202]
---

## Goal

A TypeScript definition of the wire protocol, produced from the Kotlin one, committed to the
repository, and a CI check that fails the build the moment the two disagree.

## Why

[`ADR-0003`](../../docs/adr/ADR-0003-technology-stack.md) accepted two languages across the socket
on one condition — that the shared types are generated rather than hand-written — and named this
story as where that happens. Hand-written client types drift silently: the failure is a runtime
`undefined` in the browser weeks after the Kotlin change, and nothing in either build notices.

## Answered — `DEC-007` → [`ADR-0020`](../../docs/adr/ADR-0020-typescript-protocol-from-serial-descriptors.md)

The question was how the TypeScript types are generated and what stops the checked-in output
drifting. Three answers were on the table: a Gradle task in `poker-server` walking the
`SerialDescriptor`s, a third-party kotlinx-serialization-to-TypeScript generator, or JSON Schema
plus `quicktype` in the web build.

`ADR-0020` takes the first: **an emitter we own, over the `SerialDescriptor`s**, with a
byte-comparing verify task wired into `check` so CI fails on any drift.

The deciding argument is that the wire truth lives in the descriptors, not in the Kotlin class
shapes — `Card` is a `@JvmInline value class` over an `Int`, but `CardSerializer` declares a
`STRING` descriptor and writes `"As"`. Any generator reflecting over classes rather than
descriptors emits a lie for that type, and would keep emitting it silently.

**Nothing else in `EPIC-02` depends on this story.** It is a leaf; `EPIC-03` is the consumer.

## Design notes

Alongside what `ADR-0020` settles, these hold:

- **The generated file is committed.** The web build must not require a JVM to produce its own
  types, and a reviewer must be able to see the diff a protocol change causes.
- **Drift is a build failure**, not a convention: CI regenerates and fails on any diff. A
  generated file that can drift silently is worse than a hand-written one, because nobody reads it.
- Discriminator strings in TypeScript must equal the Kotlin `@SerialName`s exactly. That is the
  one property a wrong generator would break invisibly.
- The generated types are types only — no runtime code, no validation logic, nothing that could
  become a second implementation of the rules in the client.
- `STORY-0211`'s HTTP DTOs sit in the same package family, so they come along once the mechanism
  exists.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *Tickets come from `/plan-story STORY-0203`, now that `ADR-0020` has answered `DEC-007`.* | — |

## Acceptance criteria

- [ ] Regenerating produces no diff against the committed output: `git diff --exit-code` passes in
      CI after generation.
- [ ] Every `ClientMessage` and `ServerMessage` subtype has a TypeScript type whose discriminator
      literal equals its `@SerialName`.
- [ ] `tsc --noEmit` accepts the generated file under `strict`.
- [ ] Adding a field to a Kotlin message and not regenerating fails CI, demonstrated by a test or
      by the check itself.
- [ ] The generated file carries a header saying it is generated and by which command.

## Out of scope

- Any client code that uses the types — `EPIC-03`.
- A TypeScript build, bundler, or `web-client` module — `EPIC-03`.
- Generating anything beyond the protocol: engine domain types stay in Kotlin.
