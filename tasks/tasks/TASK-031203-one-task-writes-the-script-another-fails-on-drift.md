---
schema: 2
id: TASK-031203
title: One Gradle task writes the duel script, another fails the build on drift
type: task
status: ready
parent: STORY-0312
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [server, client, build, fixture]
depends_on: [TASK-031202]
verify:
  - ./gradlew :poker-server:verifyDuelScript
  - ./gradlew :poker-server:check
  - cd web-client && npm ci
  - cd web-client && npm run check
---

## Goal

`web-client/src/e2e/scripted-duel.gen.json` exists, was written by the server's own encoder, and
cannot silently disagree with it: `:poker-server:check` fails the moment the two differ.

## Files

| File | Action |
| --- | --- |
| `poker-server/src/test/kotlin/duels/poker/server/duel/GenerateDuelScript.kt` | create |
| `poker-server/build.gradle.kts` | modify |
| `web-client/.prettierignore` | modify |

Read, do not modify: `duel/ScriptedDuel.kt` (`scriptedDuel`, `SCRIPT_ROOM_CODE`),
`protocol/typescript/GenerateProtocolTypes.kt` (the `main` to copy),
`poker-server/build.gradle.kts`'s existing `generateProtocolTypes`/`verifyProtocolTypes` pair (the
tasks to copy).

**Generated output, not authored:** running the new task writes
`web-client/src/e2e/scripted-duel.gen.json`, and this ticket commits it. It is the task's output in
exactly the sense `protocol.gen.ts` is `generateProtocolTypes`'s (`ADR-0020`), so it is not part of
the three-file budget above — but it *is* part of the diff, and the PR says how large it came out.

## Scope

- `GenerateDuelScript.kt`: one `public fun main(args: Array<String>)`, modelled line for line on
  `GenerateProtocolTypes.kt` — require exactly one argument, `File(args[0])`, `parentFile?.mkdirs()`,
  `writeText`. The text is `Json { prettyPrint = false }.encodeToString(scriptedDuel())`, its own
  `Json` rather than `protocolJson`, because a `ScriptedDuel` is a fixture and not a wire message.
  No trailing newline is added: byte-for-byte is byte-for-byte.
- Two tasks in `poker-server/build.gradle.kts`, copied from the pair already there:
  - `generateDuelScript` — `JavaExec`, `group = "protocol"`, `classpath = sourceSets["test"].runtimeClasspath`,
    `mainClass = "duels.poker.server.duel.GenerateDuelScriptKt"`,
    `args(rootProject.file("web-client/src/e2e/scripted-duel.gen.json").absolutePath)`.
  - `verifyDuelScript` — the same, but writing to
    `layout.buildDirectory.file("e2e/scripted-duel.gen.json")` and comparing the two files' bytes in
    a `doLast` that throws a `GradleException` naming the regeneration command. Both `File`s are
    resolved into **plain local vals at configuration time**, exactly as `verifyProtocolTypes` does,
    so the action reaches neither `Project` nor the task and the configuration cache holds.
  - `tasks.named("check")` gains `verifyDuelScript` beside `verifyProtocolTypes`.
- The classpath is the **test** runtime, not the main one: `playDuel` and `scriptedDuel` are test
  scaffolding and belong nowhere near the production jar. Say so in the task's `description`.
- `web-client/.prettierignore` gains `src/e2e/scripted-duel.gen.json`. It is minified machine output;
  Prettier would reformat it and the byte comparison above would then fail forever. This is the same
  bargain `src/protocol/protocol.gen.ts` already has three lines above it.
- Run `./gradlew :poker-server:generateDuelScript` and commit what it wrote.

## Out of scope

- Any change to `eslint.config.js`: ESLint's `files` glob is `**/*.{js,ts,tsx}` and never sees JSON.
- Any client code reading the file — `TASK-031204`.
- A CI job change. `:poker-server:check` already runs in the server job and `npm run check` in the
  client job; both stay as they are.
- Regenerating `protocol.gen.ts`, or touching `verifyProtocolTypes`.

## Tests

No new test method. The gates are commands, and each one fails for a different reason:

| Command | Proves |
| --- | --- |
| `./gradlew :poker-server:verifyDuelScript` | the committed file is byte-identical to what this build produces |
| `./gradlew :poker-server:check` | `verifyDuelScript` actually runs as part of `check`, and `ScriptedDuelTest` still passes |
| `cd web-client && npm run check` | the new file survives `format:check`, `lint` and `tsc` |

**Name the edit that makes each gate red** — run each, quote the failure in the PR, revert:

1. Change one character inside the committed JSON → `verifyDuelScript` fails with the message naming
   `generateDuelScript`.
2. Remove `src/e2e/scripted-duel.gen.json` from `.prettierignore` → `npm run check` fails on
   `format:check`, which is the reason the line is there.
3. Change `SCRIPT_SEED` in `ScriptedDuel.kt` without regenerating → `./gradlew :poker-server:check`
   fails, which is the drift this pair exists to catch.

## Acceptance criteria

- [ ] `./gradlew :poker-server:verifyDuelScript` exits 0
- [ ] `./gradlew :poker-server:check` exits 0 and runs `verifyDuelScript`
- [ ] `cd web-client && npm run check` exits 0
- [ ] `web-client/src/e2e/scripted-duel.gen.json` is committed and was produced by
      `./gradlew :poker-server:generateDuelScript`, not by hand
- [ ] `verifyDuelScript` resolves both file paths into local vals at configuration time and its
      `doLast` names neither `project` nor `task`
- [ ] `.prettierignore` names the generated file
- [ ] The PR quotes the three red runs above, and states the committed file's size in bytes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
