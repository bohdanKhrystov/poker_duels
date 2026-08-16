---
schema: 2
id: TASK-031015
title: The virtual-time guard exempts itself on purpose, not by accident
type: task
status: done
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, tests, guard]
depends_on: [TASK-031014]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +320 passed \(320\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'flags its own source once the exemption is lifted'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'every test file that reaches for a timer installs fake ones first'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sees the timer in a file that installs none'
  - cd web-client && npm run check
---

## Goal

The one file the timer guard cannot judge is skipped **by name**, with a test that proves the skip
is doing the work — instead of the file passing because one of its own fixture strings happens to
spell the thing the guard looks for.

## Why this exists

`virtual-time.test.ts` scans every client test file for a timer used without fake ones installed.
Its own source names `setTimeout` (inside `REACHES_FOR_A_TIMER`), so it is a candidate offender, and
it escapes only because the fixture in `sees the timer in a file that installs none` contains a
literal `vi.useFakeTimers(`. Delete that one fixture and the guard starts reporting its own file.

A guard whose self-exemption is a coincidence of spelling is a guard that changes behaviour when
somebody edits a string. Make it explicit, and prove the explicit one is load-bearing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/virtual-time.test.ts` | modify |

## Scope

- A named constant beside the two patterns, and a filter in the walk:

  ```ts
  /**
   * The guard's own file: its patterns name every timer API it looks for, so
   * the walk cannot judge it. Skipped by name — `flags its own source once the
   * exemption is lifted` proves the skip is what keeps it out, and not luck.
   */
  const THE_GUARD_ITSELF = "virtual-time.test.ts";
  ```

  `testSources()` drops that entry. The key to compare against is the entry the walk yields
  (`readdirSync(srcDir, { recursive: true })` yields it relative to `src/`, so it is exactly
  `"virtual-time.test.ts"`).
- The fixture in `sees the timer in a file that installs none` is **assembled** rather than written
  out, so this file's own bytes no longer contain the sequence the guard looks for:

  ```ts
  const installsFakeOnes = `vi.useFake${"Timers"}();`;
  ```

  Same runtime value, same assertions, same test name — the test still proves the detector accepts a
  file that installs fake ones. Without this change the new test below cannot pass, because the file
  would still exempt itself the old way.
- **No other spelling of that call may appear anywhere in the file**, including in comments and
  doc comments. The regex literal `/vi\.useFakeTimers\s*\(/` is safe: the backslash between `vi` and
  the dot means it does not match itself.

## Out of scope

- Widening or narrowing what the guard detects. The patterns, the walk and both existing test names
  stay exactly as they are.
- Exempting any other file. This is the only file that names a timer API it does not call, and a
  general opt-out — a pragma, a comment marker — would be a way for any file to leave the guard.
- `reconnecting.ts`, which calls `setTimeout` and must. It is production code and the guard has
  never read production code.

## Tests

`web-client/src/virtual-time.test.ts`, describe block `"the client's tests"`.

| Test | Proves |
| --- | --- |
| `flags its own source once the exemption is lifted` | the guard applied to **its own bytes** reports them, and the walk it actually runs does not contain that entry — so the skip, not the spelling, is what keeps this file quiet |

```ts
it("flags its own source once the exemption is lifted", () => {
  const own = readFileSync(fileURLToPath(import.meta.url), "utf-8");

  expect(sleepingTestFiles(new Map([[THE_GUARD_ITSELF, own]]))).toEqual([
    THE_GUARD_ITSELF,
  ]);
  expect([...testSources().keys()]).not.toContain(THE_GUARD_ITSELF);
});
```

Both existing tests keep their names and every assertion they have. `sources.size` is one smaller
than before and the `toBeGreaterThan(40)` assertion is unchanged — 48 files remain.

One test added. Three hundred and nineteen exist after `TASK-031014`, so the suite reports **320**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 320 passed (320)` | one ran and nothing else moved |
| the three `--reporter=verbose` greps | the new name exists and neither old one was renamed |
| `npm run check` | typechecks, lints and is formatted |

**Name the edit that makes each assertion red** — run each, quote the failure in the PR, revert:

1. Remove the `THE_GUARD_ITSELF` filter from `testSources()` → `every test file that reaches for a
   timer installs fake ones first` fails, naming `virtual-time.test.ts`. That failure **is** the
   proof the exemption is load-bearing; it does not happen on `develop` today.
2. Write the fixture back out as a plain literal → `flags its own source once the exemption is
   lifted` fails on its first assertion, because the file exempts itself again.

## Acceptance criteria

- [ ] `the client's tests > flags its own source once the exemption is lifted` passes
- [ ] `the client's tests > every test file that reaches for a timer installs fake ones first` still
      passes, with its assertions unchanged
- [ ] `the client's tests > sees the timer in a file that installs none` still passes, with its
      assertions unchanged and the same two fixtures
- [ ] The file contains no literal `vi.useFakeTimers(` outside the `INSTALLS_FAKE_ONES` regex
- [ ] No file other than `web-client/src/virtual-time.test.ts` differs
- [ ] `npm run --silent test` reports `Tests  320 passed (320)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
