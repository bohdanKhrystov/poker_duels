---
schema: 2
id: TASK-031013
title: No client test sleeps on a real clock
type: task
status: backlog
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [client, tests, guard, reconnect]
depends_on: [TASK-031012]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +316 passed \(316\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'every test file that reaches for a timer installs fake ones first'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sees the timer in a file that installs none'
  - cd web-client && npm run check
---

## Goal

*"No test sleeps on a real clock"* stops being a promise in a story and becomes a check that fails
the build — the same rule `EPIC-02` held itself to, made executable on this side.

## Files

| File | Action |
| --- | --- |
| `web-client/src/virtual-time.test.ts` | create |
| `web-client/src/protocol/boundary.ts` | read — `guardedFiles`, the walk to copy |
| `web-client/src/store/one-connection.test.ts` | read — a guard that keeps its helpers inside itself |

## Scope

- One test file, in the shape `one-connection.test.ts` already set: helpers declared in the file, no
  new shipped module, no export.
- It walks `web-client/src/` for `*.test.ts` and `*.test.tsx` — including the ones under
  `src/protocol/`, which `guardedFiles()` skips and which is exactly where the reconnect tests live,
  so reuse the `readdirSync(srcDir, { recursive: true })` walk rather than `guardedFiles()` itself.
- The rule, stated once as a named predicate:

  ```ts
  const REACHES_FOR_A_TIMER = /\b(?:setTimeout|setInterval|requestAnimationFrame)\b/;
  const INSTALLS_FAKE_ONES = /vi\.useFakeTimers\s*\(/;

  /** Test files that touch a timer without first making it virtual. */
  function sleepingTestFiles(sources: Map<string, string>): string[] { … }
  ```

- A file that names a timer **and** installs fake ones is fine; a file that names neither is fine; a
  file that names a timer and installs none is the offence.
- No production file is read by this guard. `reconnecting.ts` calls `setTimeout` and must — that is
  the timer the tests make virtual.

## Out of scope

- Awaiting a real promise. `await` is not a sleep, `findBy*` queries are ordinary testing-library
  usage, and a guard that banned them would ban the library.
- The Kotlin side. `EPIC-02` already holds itself to injected time and has its own tests for it.
- Enforcing anything about `vi.useRealTimers()` in an `afterEach`. Vitest restores between files;
  making that a rule here would be a second, weaker claim in the same file.

## Tests

`web-client/src/virtual-time.test.ts`, describe block `"the client's tests"`.

| Test | Proves |
| --- | --- |
| `every test file that reaches for a timer installs fake ones first` | `sleepingTestFiles` over every real test file under `src/` is `[]`, and — in the same test — the map it walked is **non-empty and contains at least one file that names a timer**, so a walk that found nothing cannot pass by vacuum |
| `sees the timer in a file that installs none` | the predicate applied to two literal sources: `"it('x', () => { setTimeout(done, 10); });"` is reported, and the same source with `vi.useFakeTimers();` above it is not. The detector is shown to reject something before it is trusted to accept everything |

```ts
it("every test file that reaches for a timer installs fake ones first", () => {
  const sources = testSources();

  expect(sleepingTestFiles(sources)).toEqual([]);

  // A walk that found no files, or only files with no timer in them, would
  // satisfy the line above by saying nothing. Both are named here.
  expect(sources.size).toBeGreaterThan(40);
  expect(
    [...sources.values()].filter((source) => REACHES_FOR_A_TIMER.test(source)),
  ).not.toHaveLength(0);
});
```

Two tests added. Three hundred and fourteen exist, so the suite reports **316**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 316 passed (316)` | two ran and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks and lints |

**Name the edit that makes each assertion red:**

1. Delete `vi.useFakeTimers()` from `protocol/reconnecting.test.ts` → `every test file that reaches
   for a timer installs fake ones first` names that file. Revert.
2. Narrow the walk to `src/store/` → the same test fails on `sources.size`, because a guard that
   stops looking must not pass quietly. Revert.

Quote both in the PR, and say in the PR body how many test files the walk found.

## Acceptance criteria

- [ ] `the client's tests > every test file that reaches for a timer installs fake ones first` passes
- [ ] `the client's tests > sees the timer in a file that installs none` passes
- [ ] The guard walks `src/protocol/` as well as the rest of `src/`
- [ ] No other file differs from what it was
- [ ] `npm run --silent test` reports `Tests  316 passed (316)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
