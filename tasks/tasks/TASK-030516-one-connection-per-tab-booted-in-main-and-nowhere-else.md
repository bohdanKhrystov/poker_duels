---
schema: 2
id: TASK-030516
title: One connection per tab, booted in main.tsx and nowhere else
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [client, store, test]
depends_on: [TASK-030515]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +131 passed \(131\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is named by main.tsx and by no other shipped file'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is booted once, in main.tsx'
  - cd web-client && npm run check
---

## Goal

`ADR-0032`'s ownership rule stops being a convention: no shipped file but `main.tsx` may name a
connection opener, and the client is booted exactly once — so `STORY-0306`–`STORY-0312` cannot
quietly copy a connect-in-a-component.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/one-connection.test.ts` | create |
| `web-client/src/protocol/boundary.ts` | read — `guardedFiles()`, which this reuses |
| `web-client/src/protocol/boundary.test.ts` | read — the house shape for this kind of test |

## Scope

- **No production code changes.** One new test file. If it goes red, the fix is in the file that
  broke the rule, not here.
- Create `web-client/src/store/one-connection.test.ts` with exactly this content:

  ```ts
  import { readFileSync } from "node:fs";
  import { basename } from "node:path";
  import { describe, expect, it } from "vitest";
  import { guardedFiles } from "../protocol/boundary";

  const OPENS_A_CONNECTION = /\b(?:connectToDuelServer|openConnection)\b/;
  const BOOTS_A_CLIENT = /\bbootDuelClient\s*\(/;
  const EVERY_BOOT = /\bbootDuelClient\s*\(/g;

  // Tests drive both by design — the boot tests open a connection over a
  // FakeSocket — and `boot.ts` is where `bootDuelClient` is declared. What
  // ADR-0032 forbids is a second one in shipped code: one boot per tab is where
  // "exactly once" lives, and a screen may never hold a connection of its own.
  function shippedFiles(): string[] {
    return guardedFiles().filter(
      (file) =>
        !/\.test\.tsx?$/.test(file) &&
        basename(file) !== "boot.ts" &&
        basename(file) !== "boundary.ts",
    );
  }

  function shippedFilesMatching(pattern: RegExp): string[] {
    return shippedFiles()
      .filter((file) => pattern.test(readFileSync(file, "utf-8")))
      .map((file) => basename(file));
  }

  function sourceOf(name: string): string {
    const file = shippedFiles().find((path) => basename(path) === name);
    if (file === undefined) throw new Error(`no shipped file named ${name}`);
    return readFileSync(file, "utf-8");
  }

  describe("the tab's one connection", () => {
    it("is named by main.tsx and by no other shipped file", () => {
      expect(shippedFilesMatching(OPENS_A_CONNECTION)).toEqual(["main.tsx"]);
    });

    it("is booted once, in main.tsx", () => {
      expect(shippedFilesMatching(BOOTS_A_CLIENT)).toEqual(["main.tsx"]);
      expect(sourceOf("main.tsx").match(EVERY_BOOT)).toHaveLength(1);
    });
  });
  ```

- **`OPENS_A_CONNECTION` has no `\(`**: `main.tsx` passes `connectToDuelServer` by reference and
  never calls it, so a call-shaped pattern matches nothing and the first test passes vacuously —
  observed, not guessed.
- **`BOOTS_A_CLIENT` and `EVERY_BOOT` are two objects with the same source.** `RegExp.test` on a
  `/g` regex is stateful through `lastIndex`, so one shared global regex would silently skip
  files. The non-global one filters; the global one counts.
- `guardedFiles()` already excludes `src/protocol/`, which is where `openConnection` is declared
  and tested. The filter here drops test files (they drive both on purpose), `boot.ts` (the
  declaration), and `boundary.ts`.
- The guard reads text, so a comment naming `connectToDuelServer` in a shipped file counts as a
  violation. That bluntness is deliberate and cheap to work around by not writing the name.

## Out of scope

- Enforcing *"no send from an effect"*. `ADR-0032` says no lint rule does that today and reviewers
  must; this test does not pretend to.
- Extending `boundary.ts` itself. That file is `src/protocol/`'s and `STORY-0303` owns it.
- Whether `main.tsx` is correct beyond these two facts — it is untestable by import and stays so.

## Tests

`web-client/src/store/one-connection.test.ts`, one `describe("the tab's one connection")`.

| Test | Proves |
| --- | --- |
| `is named by main.tsx and by no other shipped file` | across every shipped `.ts`/`.tsx` outside `src/protocol/`, `connectToDuelServer` and `openConnection` appear in `main.tsx` alone |
| `is booted once, in main.tsx` | `bootDuelClient(` appears in `main.tsx` alone, and exactly once in it |

Two tests. One hundred and twenty-nine exist, so the suite reports **131**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 131 passed (131)` | the two ran and the hundred-and-twenty-nine before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — both were run against this exact test file:

1. Have `App.tsx` open its own connection: add `import { connectToDuelServer } from "./protocol";`
   and a `connectToDuelServer(() => {});` call in the component body → `is named by main.tsx and by
   no other shipped file` fails with `expected [ 'App.tsx', 'main.tsx' ] to deeply equal [
   'main.tsx' ]`. Revert.
2. Add a second `bootDuelClient({ connect: connectToDuelServer, joinRoomCode: null });` to
   `main.tsx` → `is booted once, in main.tsx` fails with `expected [ Array(2) ] to have a length of
   1 but got 2`. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the tab's one connection > is named by main.tsx and by no other shipped file` passes
- [ ] `the tab's one connection > is booted once, in main.tsx` passes
- [ ] `npm run --silent test` reports `Tests  131 passed (131)`
- [ ] `git diff --name-only` for the PR lists exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
