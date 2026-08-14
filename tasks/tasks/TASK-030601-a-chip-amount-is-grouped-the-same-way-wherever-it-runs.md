---
schema: 2
id: TASK-030601
title: A chip amount is grouped the same way wherever it runs
type: task
status: done
parent: STORY-0306
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, duel, ui]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +136 passed \(136\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'groups a four-figure stack'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'writes zero as zero'
  - cd web-client && npm run check
---

## Goal

The table's first shared piece: one function that writes `13400` as `13,400`, identically on every
machine that runs the suite.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/table/chips.ts` | create |
| `web-client/src/table/chips.test.ts` | create |

`web-client/src/table/` is new and is where this story's screen lives. Nothing else is read.

## Scope

- The whole module, verbatim:

  ```ts
  /**
   * A chip amount as the table writes it: digits grouped in threes.
   *
   * Deliberately not `toLocaleString` — the grouping must not change with the
   * browser's locale, because the design's mono, tabular-figure numbers are drawn
   * one way and a test asserting `"13,400"` must not depend on where it runs.
   */
  export function formatChips(amount: number): string {
    return String(amount).replace(/\B(?=(\d{3})+$)/g, ",");
  }
  ```

- The `$` anchor is load-bearing: without it the lookahead matches at every position that has *some*
  multiple of three digits after it, and `13400` comes out `1,3,400`. That is red edit 2 below.
- No `Intl`, no `toLocaleString`, no options object, no currency, no rounding.

## Out of scope

- Any component. This ticket ships one exported function and its test, nothing that renders.
- Negative amounts as a product concern. `String(-1500)` groups correctly by accident and no test
  pins it, because no chip count on the wire is negative.

## Tests

`web-client/src/table/chips.test.ts`, describe block `"a chip amount"`.

| Test | Proves |
| --- | --- |
| `leaves three digits ungrouped` | `formatChips(150)` is `"150"` |
| `groups a four-figure stack` | `formatChips(4150)` is `"4,150"` — the design's rival stack |
| `groups a five-figure stack` | `formatChips(13400)` is `"13,400"` — the design's hero stack |
| `writes zero as zero` | `formatChips(0)` is `"0"`, not `""` |
| `groups the same way wherever it runs` | `formatChips(1234567)` is `"1,234,567"` |

Five tests. One hundred and thirty-one exist, so the suite reports **136**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 136 passed (136)` | the five ran and the hundred-and-thirty-one before them still do |
| the two `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

`NO_COLOR=1` is on every grep because this environment sets `FORCE_COLOR=3`, and Vitest then emits
ANSI even through a pipe. `FORCE_COLOR=0` does not help — Node prints a warning and keeps the
colour.

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. `return String(amount);` → `groups a four-figure stack` fails with `expected '4150' to be
   '4,150' // Object.is equality`, and two more with it (three failed, two passed). Revert.
2. Drop the `$` from the lookahead — `/\B(?=(\d{3})+)/g` → `groups a five-figure stack` fails with
   `expected '1,3,400' to be '13,400' // Object.is equality` and `groups the same way wherever it
   runs` with `expected '1,2,3,4,567' to be '1,234,567'`. Revert.
3. Add `if (amount === 0) return "";` above the return → `writes zero as zero` fails with
   `expected '' to be '0' // Object.is equality`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `a chip amount > leaves three digits ungrouped` passes
- [ ] `a chip amount > groups a four-figure stack` passes
- [ ] `a chip amount > groups a five-figure stack` passes
- [ ] `a chip amount > writes zero as zero` passes
- [ ] `a chip amount > groups the same way wherever it runs` passes
- [ ] `npm run --silent test` reports `Tests  136 passed (136)`
- [ ] `chips.ts` contains neither `Intl` nor `toLocaleString`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
