---
schema: 2
id: TASK-030508
title: The framework-free store modules import nothing from react
type: task
status: backlog
parent: STORY-0305
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, store, test]
depends_on: [TASK-030507]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +109 passed \(109\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'duel-state.ts imports nothing from react'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'duel-store.ts imports nothing from react'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'boot.ts imports nothing from react'
  - cd web-client && npm run check
---

## Goal

`ADR-0032` §4's structural test: the reducer, the store and the boot wiring stay testable without
a DOM, and a future edit that reaches for a hook in one of them fails the suite by name.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/framework-free.test.ts` | create |
| `web-client/src/protocol/boundary.ts` | read — the house pattern for a source-text guard |

## Scope

- Create `web-client/src/store/framework-free.test.ts` with exactly this content:

  ```ts
  import { readFileSync } from "node:fs";
  import { dirname, join } from "node:path";
  import { fileURLToPath } from "node:url";
  import { describe, expect, it } from "vitest";

  // The three modules ADR-0032 keeps free of React: the reducer, the store around
  // it, and the boot wiring. `duel-provider.tsx` is the one React-aware file here,
  // and it is deliberately absent from this list.
  const FRAMEWORK_FREE = ["duel-state.ts", "duel-store.ts", "boot.ts"];

  function sourceOf(name: string): string {
    const here = dirname(fileURLToPath(import.meta.url));
    return readFileSync(join(here, name), "utf-8");
  }

  describe("the store's framework-free modules", () => {
    it.each(FRAMEWORK_FREE)("%s imports nothing from react", (name) => {
      expect(sourceOf(name)).not.toMatch(/from "react/);
    });
  });
  ```

- The path is resolved from `import.meta.url`, not `process.cwd()`, so the test finds the same
  files regardless of where the runner started — the same reason `boundary.ts` does it.
- `/from "react/` with no closing quote catches `react` and `react-dom` alike.
- `readFileSync` throwing is the point: a module that is renamed or deleted fails here loudly
  rather than passing by absence.

## Out of scope

- Adding `duel-provider.tsx` to the list — it is the one file in `src/store/` that must import
  React.
- Guarding *other* directories. `src/lobby/` is React by definition.
- An eslint rule doing this instead. `ADR-0032` names a custom rule as possible later and not
  bought now; a nineteen-line test is what is bought here.

## Tests

`web-client/src/store/framework-free.test.ts`, one `describe("the store's framework-free
modules")` containing a single `it.each`, which Vitest reports as three named tests:

| Test | Proves |
| --- | --- |
| `duel-state.ts imports nothing from react` | the reducer is still pure TypeScript |
| `duel-store.ts imports nothing from react` | the subscription shell is React's contract, not React's code |
| `boot.ts imports nothing from react` | the wiring that owns the connection is outside the tree |

Three tests. One hundred and six exist, so the suite reports **109**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 109 passed (109)` | the three ran and the hundred-and-six before them still do |
| the three `--reporter=verbose` greps | `it.each` interpolated `%s` into the three names |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Add `"duel-provider.tsx"` to `FRAMEWORK_FREE` → a fourth case appears and fails with
   ``expected 'import {\n  createContext,\n  useCont…' not to match /from "react/``. Revert.
2. Change `"boot.ts"` in the list to `"boot-typo.ts"` → that case fails with `Error: ENOENT: no
   such file or directory, open '…/src/store/boot-typo.ts'`. Revert.
3. Add `import type { ReactNode } from "react";` to the top of `duel-store.ts` → `duel-store.ts
   imports nothing from react` fails with ``expected 'import type { ReactNode } from "react…' not
   to match /from "react/``, and `tsc` additionally reports `error TS6133: 'ReactNode' is declared
   but its value is never read`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the store's framework-free modules > duel-state.ts imports nothing from react` passes
- [ ] `the store's framework-free modules > duel-store.ts imports nothing from react` passes
- [ ] `the store's framework-free modules > boot.ts imports nothing from react` passes
- [ ] `npm run --silent test` reports `Tests  109 passed (109)`
- [ ] `git diff --name-only` for the PR lists exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
