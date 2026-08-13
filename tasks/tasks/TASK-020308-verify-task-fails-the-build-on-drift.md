---
schema: 2
id: TASK-020308
title: verifyProtocolTypes byte-compares on every check
type: task
status: done
parent: STORY-0203
module: poker-server
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [server, protocol, typescript, build, ci]
depends_on: [TASK-020307]
verify:
  - ./gradlew :poker-server:verifyProtocolTypes
  - ./gradlew :poker-server:check --dry-run | grep -q ':poker-server:verifyProtocolTypes'
  - bash .github/scripts/prove_protocol_drift_fails.sh
  - git diff --exit-code -- web-client/src/protocol/protocol.gen.ts
---

## Goal

A Kotlin protocol change that skips regeneration fails `./gradlew check`, and that claim is proven
by a script rather than asserted in prose.

## Files

| File | Action |
| --- | --- |
| `poker-server/build.gradle.kts` | modify |
| `.github/scripts/prove_protocol_drift_fails.sh` | create |

`.github/workflows/build.yml` already runs `./gradlew check -PrequireDocker=true`, so nothing in
the workflow changes here — that is the point of wiring into `check`.

## Scope

**The task.** Register `verifyProtocolTypes` next to `generateProtocolTypes`: the same `JavaExec`,
the same main class, but writing into `layout.buildDirectory.file("protocol/protocol.gen.ts")`, with
a `doLast` that reads both files as bytes and throws `GradleException` unless they are equal. The
message must name `./gradlew :poker-server:generateProtocolTypes`, because that message is the only
instruction the person who broke the build will read.

Capture both paths as plain `File` locals *outside* the `doLast` block and use them inside it, so
the action does not reach back into the project at execution time.

Byte comparison inside the task, not `git diff` — the check is then indifferent to working-tree
state, and the two are equivalent on a clean CI checkout.

**The wiring.** `tasks.named("check") { dependsOn("verifyProtocolTypes") }`.

**The proof.** `.github/scripts/prove_protocol_drift_fails.sh`, run from the repository root,
`set -euo pipefail`:

1. Run `:poker-server:verifyProtocolTypes` — expect success (a green baseline; without it, step 3
   proves nothing, because a task that always fails would also "detect drift").
2. Append a line to `web-client/src/protocol/protocol.gen.ts`.
3. Run the task again and expect a **non-zero** exit; if it succeeds, print why and exit 1.
4. Restore the file with `git checkout --` from a `trap ... EXIT`, so an interrupted run does not
   leave the repository dirty.

The script is committed because it is the story's fourth acceptance criterion made executable — it
is what "adding a field and not regenerating fails CI" means, demonstrated rather than believed.

## Out of scope

- The `tsc` CI step — `TASK-020309`.
- Any change to the emitter, the generated file, or `generateProtocolTypes`. If the byte comparison
  fails before you have touched anything, the committed file and the emitter already disagree: stop
  and report it, do not regenerate to make the check pass.

## Acceptance criteria

- [ ] `./gradlew :poker-server:verifyProtocolTypes` exits 0 against the committed file
- [ ] `./gradlew :poker-server:check --dry-run` lists `:poker-server:verifyProtocolTypes`
- [ ] `bash .github/scripts/prove_protocol_drift_fails.sh` exits 0, and its output shows the
      perturbed run failing
- [ ] `git diff --exit-code -- web-client/src/protocol/protocol.gen.ts` is clean after the script
      runs — the trap restored the file it deliberately broke
- [ ] The failure message contains `./gradlew :poker-server:generateProtocolTypes`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
