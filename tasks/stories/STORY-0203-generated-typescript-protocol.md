---
id: STORY-0203
title: Generated TypeScript protocol types
type: story
status: blocked
parent: EPIC-02
module: poker-server
labels: [protocol, typescript, tooling, blocked]
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

## ⚠ Blocked — `DEC-007`

> **How are the TypeScript protocol types generated, and what stops the checked-in output
> drifting?**
>
> Three plausible answers, each dragging a different dependency into a different build:
>
> 1. **A Gradle task in `poker-server`** that walks the kotlinx.serialization `SerialDescriptor`s
>    and emits a `.d.ts`. No third-party dependency, no new toolchain; a few hundred lines of
>    emitter we own and maintain, including its handling of sealed hierarchies and value classes.
> 2. **A third-party generator** (e.g. a kotlinx-serialization-to-TypeScript plugin). Less code to
>    own; a dependency on a small project whose abandonment would be our problem, and whose output
>    shape we do not control.
> 3. **Emit JSON Schema from Kotlin, run `quicktype` in the web build.** Both halves are
>    well-trodden, and the schema is useful on its own; it puts Node tooling in the path between
>    the two languages and makes the client build depend on a generation step.
>
> It constrains `EPIC-03`'s build, so it is not a decision a ticket here should make in passing.
>
> **Nothing else in `EPIC-02` depends on this story.** It is a leaf; `EPIC-03` is the consumer.

## Design notes

Whatever `DEC-007` settles, these hold:

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
| — | *Blocked on `DEC-007`. Tickets come from `/plan-story STORY-0203` once it is answered.* | — |

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
