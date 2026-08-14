---
schema: 2
id: TASK-030106
title: Vitest renders the app in jsdom and asserts what it shows
type: task
status: done
parent: STORY-0301
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 3
labels: [client, test, toolchain]
depends_on: [TASK-030105]
verify:
  - cd web-client && npm test
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +1 passed \(1\)'
  - cd web-client && npm run typecheck
  - cd web-client && npm run format:check
  - ./gradlew :poker-server:verifyProtocolTypes
---

## Goal

`npm test` runs Vitest once in `jsdom`, renders `App` with `@testing-library/react`, and asserts the
heading text — and the run reports one passing test rather than merely exiting 0.

## Files

| File | Action |
| --- | --- |
| `web-client/vite.config.ts` | modify |
| `web-client/package.json` | modify |
| `web-client/src/App.test.tsx` | create |

## Scope

- `vite.config.ts` gains a `test` block. Change the import to `vitest/config` so the property
  typechecks — that is the documented way, not a triple-slash reference:

  ```ts
  import { defineConfig } from 'vitest/config';
  ...
    test: {
      environment: 'jsdom',
      globals: true,
    },
  ```

  `globals: true` is there for one reason worth a comment in the file: it is how
  `@testing-library/react` registers its automatic DOM cleanup between tests. The tests still import
  `describe`, `it` and `expect` explicitly, so `tsconfig.json` needs no `vitest/globals` types entry
  and stays out of this ticket's budget.
- One script: `"test": "vitest run"`. **`run` is not optional** — bare `vitest` is watch mode and
  would hang CI until the job times out.
- `src/App.test.tsx` — one file, one test, named exactly `renders the application heading`:

  ```tsx
  render(<App />);
  expect(screen.getByRole('heading').textContent).toBe('Poker Duels');
  ```

## Out of scope

- `@testing-library/jest-dom` and custom matchers. `textContent` plus `toBe` needs no setup file,
  and a setup file would be a fourth file.
- Coverage, reporters, `setupFiles`, `pool` and threading options.
- Any test that opens a socket, calls `fetch`, or binds a port. No test in `EPIC-03` may reach the
  network, and this is the file that sets that precedent.
- The proxy test — `TASK-030108`.

## Tests

`src/App.test.tsx`

| Test | Proves |
| --- | --- |
| `renders the application heading` | Vitest transforms TSX, `jsdom` provides a DOM, `@testing-library/react` renders into it, and the mounted component shows `Poker Duels` |

## Proof the runner really ran

A test command that matches nothing has reported success in this repository before. Two things stop
it here:

- `vitest run` with no pattern argument — nothing to mistype, nothing to match the wrong file — and
  `passWithNoTests` left at its default `false`, so an empty run is red.
- The verify block asserts the **case count** from the summary line, not the exit code:
  `grep -qE 'Tests +1 passed \(1\)'`. A run that discovered no test file, or a different one, cannot
  satisfy it.

Later tickets that add tests update that number to their own total; `TASK-030108` expects three.

### Amended by the driver: the assertion had to survive a colourising terminal

As first written the command was `npm run --silent test 2>&1 | grep -qE ...`, and it failed for the
driver while passing for the coder. Vitest emits ANSI colour when `FORCE_COLOR` is set in the
environment — Claude Code sets `FORCE_COLOR=3` — so the summary line arrives as

```
^[[2m      Tests ^[[22m ^[[1m^[[32m1 passed^[[39m^[[22m^[[90m (1)^[[39m
```

and no plain-text regex can match it. Setting `FORCE_COLOR=0` does **not** help: the colour library
keys off the variable's presence, not its value. `NO_COLOR=1` does, so the verify command now carries
that prefix.

This is the *count* assertion, the one thing standing between a green run and a run that executed
nothing, so an assertion that passes or fails on the terminal rather than on the tests is worse than
no assertion at all. `TASK-030108` and `TASK-030109` carry the same pattern and were given the same
prefix at the same time, rather than rediscovering this twice more.

## Acceptance criteria

- [ ] `cd web-client && npm test` exits 0
- [ ] The run prints `Tests  1 passed (1)`
- [ ] `renders the application heading` fails if `App` renders different text — check once by
      editing the heading locally, then revert
- [ ] `"test"` is `vitest run`, never bare `vitest`
- [ ] `cd web-client && npm run typecheck` and `npm run format:check` both exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
