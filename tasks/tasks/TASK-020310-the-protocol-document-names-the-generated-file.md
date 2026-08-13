---
schema: 2
id: TASK-020310
title: The protocol document names the generated file and its command
type: task
status: ready
parent: STORY-0203
module: poker-server
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [documentation, protocol, typescript]
depends_on: [TASK-020309]
verify:
  - ./gradlew :poker-server:test --tests '*ProtocolDocumentationTest'
  - grep -q 'web-client/src/protocol/protocol.gen.ts' docs/protocol.md
  - grep -q ':poker-server:generateProtocolTypes' docs/protocol.md
  - grep -cE '^\| `[A-Za-z]+` \|' docs/protocol.md | grep -qx 12
---

## Goal

`docs/protocol.md` says where the TypeScript lives, which command regenerates it, and that a
conflict in it is resolved by regenerating — the three things someone who changes a message needs
to know and currently cannot find.

Line 3 of the document already promises "the TypeScript client is generated from this schema".
Since `TASK-020307` that is true, and the document should say how.

## Files

| File | Action |
| --- | --- |
| `docs/protocol.md` | modify |

## Scope

One short section, `## Generated TypeScript`, added after the `## Messages` table and before
`## HTTP endpoints`. Four facts, no more:

- The committed output is `web-client/src/protocol/protocol.gen.ts`.
- `./gradlew :poker-server:generateProtocolTypes` writes it; run it after any protocol change.
- `./gradlew :poker-server:verifyProtocolTypes` runs as part of `check`, so forgetting to
  regenerate fails the build.
- It is types only, and a merge conflict in it is resolved by regenerating, never by hand-merging
  (`ADR-0020`).

Change no other line of the document. Do not restate the mapping table from `ADR-0020` here: two
copies of a mapping is a second thing that can be wrong.

**The trap.** `ProtocolDocumentationTest.theDocumentNamesNoMessageThatDoesNotExist` reads every
line matching ``^\| `Name` \|`` as the name of a protocol message and fails if no such message
exists. Write the new section as prose or a bullet list — no table whose first cell is a single
backticked identifier. The last `verify:` command counts those lines and requires the number to
stay at 12, which is exactly today's twelve messages.

## Out of scope

- `docs/architecture.md`, `README.md`, `CONTRIBUTING.md`.
- Documenting the emitter's internals; `ADR-0020` is where that lives, and the section may link
  to it.

## Acceptance criteria

- [ ] `ProtocolDocumentationTest` passes with all five of its tests green and the test file
      unchanged — this ticket adds prose the test does not read as a message row
- [ ] `docs/protocol.md` names `web-client/src/protocol/protocol.gen.ts`
- [ ] `docs/protocol.md` names `:poker-server:generateProtocolTypes` and
      `:poker-server:verifyProtocolTypes`
- [ ] The count of message table rows in `docs/protocol.md` is still 12
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
