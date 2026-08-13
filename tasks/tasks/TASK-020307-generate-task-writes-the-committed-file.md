---
schema: 2
id: TASK-020307
title: generateProtocolTypes writes the committed TypeScript file
type: task
status: done
parent: STORY-0203
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, protocol, typescript, build]
depends_on: [TASK-020306]
verify:
  - ./gradlew :poker-server:generateProtocolTypes
  - grep -qxF 'export type ProtocolVersion = 2;' web-client/src/protocol/protocol.gen.ts
  - grep -c 'export interface SeatView {' web-client/src/protocol/protocol.gen.ts | grep -qx 1
  - grep -c 'holeCards: readonly string\[\];' web-client/src/protocol/protocol.gen.ts | grep -qx 1
  - grep -c 'export interface FOLD' web-client/src/protocol/protocol.gen.ts | grep -qx 0
  - npx --yes --package=typescript@5.6.3 tsc --noEmit --strict web-client/src/protocol/protocol.gen.ts
  - ./gradlew :poker-server:ktlintCheck
---

## Goal

`./gradlew :poker-server:generateProtocolTypes` writes
`web-client/src/protocol/protocol.gen.ts`, and that file is committed — the command
`ADR-0020` names, producing the artefact `ADR-0020` names.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/main/kotlin/duels/poker/server/protocol/typescript/GenerateProtocolTypes.kt` | create |
| `poker-server/build.gradle.kts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | create (generated — run the task, commit its output, never type into it) |

The generated file is machine output and does not count against this ticket's estimate; the
hand-written diff is about thirty lines.

## Scope

**The entry point.** A top-level `public fun main(args: Array<String>)` in
`duels.poker.server.protocol.typescript` — public, not `internal`, because Gradle launches it by
class name. It takes exactly one argument, the target path; `require` a size of 1 with a message
naming the task. It creates the parent directories and writes `protocolTypeScript()` with
`writeText`, which preserves the string's LF newlines verbatim. Nothing else: no logging, no
default path, no directory scanning.

**The Gradle task**, in `poker-server/build.gradle.kts`:

```kotlin
tasks.register<JavaExec>("generateProtocolTypes") {
    group = "protocol"
    description = "Emits web-client/src/protocol/protocol.gen.ts from the protocol serial descriptors (ADR-0020)."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("duels.poker.server.protocol.typescript.GenerateProtocolTypesKt")
    args(rootProject.file("web-client/src/protocol/protocol.gen.ts").absolutePath)
}
```

The main class name is the file name plus `Kt`; renaming the file breaks the task.

**The output.** Run the task and commit the file it produced. Do not edit it afterwards — if
something in it looks wrong, the emitter is wrong, and that is a new ticket.

## Out of scope

- The drift check and the `check` wiring — `TASK-020308`.
- The CI typecheck step — `TASK-020309`.
- Any other file under `web-client/`: no `package.json`, no `tsconfig.json`, no `index.ts`, no
  README. `EPIC-03` scaffolds the module around this one file, and a stub left here is a thing it
  would have to undo.

## Verifying, and why these commands

The `verify:` block is the test for this ticket; no JUnit test is added.

- `grep ... 'export type ProtocolVersion = 2;'` — the version alias reached the file.
- `SeatView` and `holeCards: readonly string[];` — both are reachable only past a repeated LIST
  serial name, and `holeCards` is a `List<Card>`, so this one line proves in the committed artefact
  what `ADR-0020` was decided on: the custom `CardSerializer` puts a **string** on the wire, and the
  emitter agrees.
- `export interface FOLD` counted at zero — an enum entry must not have become a type. `ActionType`
  is in the file, so the absence is a real absence.
- `tsc --noEmit --strict` — exits non-zero on any dangling reference, which is what a dropped
  declaration looks like. Run it exactly as written: `npx --yes typescript@... tsc` fails with
  "could not determine executable to run"; the package flag is required.

## Acceptance criteria

- [ ] `./gradlew :poker-server:generateProtocolTypes` exits 0 and `web-client/src/protocol/protocol.gen.ts` exists
- [ ] Running the task twice leaves the file byte-identical (no timestamp, no run counter, nothing
      that varies between runs is in the output)
- [ ] `tsc --noEmit --strict` accepts the file
- [ ] The committed file is exactly the task's output, with no hand edit
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
