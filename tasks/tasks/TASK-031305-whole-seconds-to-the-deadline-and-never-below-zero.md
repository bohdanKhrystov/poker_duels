---
schema: 2
id: TASK-031305
title: Whole seconds to the deadline, and never below zero
type: task
status: done
parent: STORY-0313
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, duel, presence]
depends_on: [TASK-031304]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'counts whole seconds up to the deadline'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reaches zero and stays there'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads both of its arguments'
  - cd web-client && npm run check
---

## Goal

One pure function turns a local deadline and a local instant into the whole number of seconds a
countdown shows, clamped at zero.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/presence-countdown.ts` | create |
| `web-client/src/table/presence-countdown.test.ts` | create |
| `web-client/src/table/chips.ts` | read — the shape a small pure module takes here |

## Scope

- One exported function, and nothing else in the file:

  ```ts
  /**
   * Whole seconds left of a window that ends at `deadlineMillis`, never below zero.
   *
   * `Math.ceil` so that the last whole second is shown for the whole of it and zero is reached
   * at the deadline rather than a second before it. Zero is not an event (`ADR-0028` §3): the
   * number stops there and nothing the client does changes.
   */
  export function secondsRemaining(
    deadlineMillis: number,
    nowMillis: number,
  ): number {
    return Math.max(0, Math.ceil((deadlineMillis - nowMillis) / 1000));
  }
  ```

- No clock is read here and none is imported. Both instants are arguments, which is what makes the
  function testable without a timer at all.
- No formatting. The function answers a number; whether a screen prints `45`, `45s` or `0:45` is
  `EPIC-06`'s and is not decided in this module (`ADR-0046` §3).

## Out of scope

- Where the deadline comes from. `TASK-031306` anchors it once, from the frame's own
  `graceRemainingMillis`.
- Any timer. This file installs none and the component that does is `TASK-031306`.
- The numeral's shape. `ADR-0046` §3 leaves it to the design.

## Tests

`web-client/src/table/presence-countdown.test.ts`, one describe block: `"the countdown"`.

**Every case uses a non-zero `now`** — `const NOW = 1_700_000_000_000` — and states the deadline as
`NOW + something`. With `now` at zero, `Math.ceil(deadline / 1000)` gives the right answer for every
input, so a function that ignored its second argument would pass a whole file of zero-based cases.
Two inputs, or the test is vacuous.

`the countdown`

| Test | Proves |
| --- | --- |
| `counts whole seconds up to the deadline` | `NOW + 47_000` → `47`; `NOW + 46_001` → `47`; `NOW + 46_000` → `46`. The middle case is the one that pins `Math.ceil`: `Math.floor` answers `46` there |
| `reaches zero and stays there` | `NOW` → `0`; `NOW - 1` → `0`; `NOW - 600_000` → `0`. Asserted with `toBe(0)`, which is `Object.is` — `Math.max(0, -0)` is `+0` by specification, so the clamp is what makes this pass and not a coincidence of formatting |
| `reads both of its arguments` | `secondsRemaining(NOW + 47_000, NOW)` is `47` while `secondsRemaining(NOW + 47_000, NOW + 20_000)` is `27` — same deadline, different instant, different answer |

Three tests. The suite grows by that many on top of whatever `TASK-031304` left, and every one of them passes.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | three ran and every test before them still does |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | the module typechecks and exports one name |

**Name the edit that makes each assertion red:**

1. Swap `Math.ceil` for `Math.floor` → `counts whole seconds up to the deadline` fails on its second
   case, `46` against `47`. Revert.
2. Drop the `Math.max(0, …)` → `reaches zero and stays there` fails on its third case, `-600`
   against `0`. Revert.

## Acceptance criteria

- [ ] `the countdown > counts whole seconds up to the deadline` passes
- [ ] `the countdown > reaches zero and stays there` passes
- [ ] `the countdown > reads both of its arguments` passes
- [ ] `presence-countdown.ts` exports exactly one name, `secondsRemaining`
- [ ] `presence-countdown.ts` mentions neither `Date` nor `performance` nor any timer API
- [ ] `presence-countdown.test.ts` uses a non-zero `now` in every case
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
