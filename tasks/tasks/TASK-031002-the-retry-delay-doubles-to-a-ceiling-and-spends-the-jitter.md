---
schema: 2
id: TASK-031002
title: The retry delay doubles to a ceiling and spends the jitter it is handed
type: task
status: backlog
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, protocol, reconnect]
depends_on: [TASK-031001]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +284 passed \(284\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'doubles its ceiling for every attempt until it caps'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'is never shorter than half its ceiling, at every attempt'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'spends the jitter it is handed, differently at every attempt'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stays inside its bounds however far the attempts run'
  - cd web-client && npm run check
---

## Goal

One pure function says how long to wait before the next reconnect attempt: bounded, doubling, and
jittered from a number the caller supplies.

## Files

| File | Action |
| --- | --- |
| `web-client/src/protocol/retry-delay.ts` | create |
| `web-client/src/protocol/retry-delay.test.ts` | create |

## Scope

- The whole module, and no more than this:

  ```ts
  /** The ceiling on the first retry, doubling from there. */
  export const FIRST_RETRY_MILLIS = 500;

  /** The ceiling no retry ever exceeds, however long the server stays away. */
  export const LONGEST_RETRY_MILLIS = 10_000;

  /**
   * How long to wait before reconnect attempt number [attempt] (0 for the first
   * retry after a socket closes).
   *
   * Equal jitter: half the ceiling, plus [jitter] of the other half. Half a
   * window of spread is enough to keep two tabs that dropped together from
   * retrying in lockstep, and keeping the lower half fixed means the first
   * retry is still fast.
   *
   * @param attempt How many retries have already been made, 0-based.
   * @param jitter A number in [0, 1). The caller owns the source, so a test
   *   hands it a value rather than a distribution.
   */
  export function retryDelayMillis(attempt: number, jitter: number): number {
    const ceiling = Math.min(FIRST_RETRY_MILLIS * 2 ** attempt, LONGEST_RETRY_MILLIS);
    return Math.floor(ceiling / 2 + jitter * (ceiling / 2));
  }
  ```

- No `Math.random` anywhere in this file. The randomness is the caller's, which is what makes every
  test below an exact equality rather than a range check.
- No clock, no timer, no socket. This module computes a number.

## Out of scope

- Calling it. `TASK-031003` owns the loop that schedules on this number.
- Choosing where the jitter comes from in production. `TASK-031003`'s option defaults to
  `Math.random`.
- A per-attempt cap on the *number* of retries. There is none: a client that has given up on a
  server that is merely slow to come back is the failure this story exists to remove.

## Tests

`web-client/src/protocol/retry-delay.test.ts`, describe block `"the retry delay"`. Every test below
**enumerates** its attempts rather than sampling one — a claim about "every attempt" that checks one
attempt is a claim the next reader will believe and the code will not honour.

| Test | Proves |
| --- | --- |
| `doubles its ceiling for every attempt until it caps` | `[0,1,2,3,4,5,6].map((a) => retryDelayMillis(a, 1))` equals `[500, 1000, 2000, 4000, 8000, 10000, 10000]` — the doubling, and the cap landing between attempt 4 and attempt 5 |
| `is never shorter than half its ceiling, at every attempt` | the same seven attempts at `jitter = 0` equal `[250, 500, 1000, 2000, 4000, 5000, 5000]` |
| `spends the jitter it is handed, differently at every attempt` | the same seven at `jitter = 0.5` equal `[375, 750, 1500, 3000, 6000, 7500, 7500]`, **and** for each of the seven attempts the three values at `jitter` 0, 0.5 and 1 are three distinct numbers. A function that ignored its jitter would pass the first assertion of the previous test and fail this one |
| `stays inside its bounds however far the attempts run` | for every attempt 0 through 30, the delay is at least 250 and at most `LONGEST_RETRY_MILLIS`, at `jitter = 0` and at `jitter = 0.999` |

```ts
it("spends the jitter it is handed, differently at every attempt", () => {
  const attempts = [0, 1, 2, 3, 4, 5, 6];

  expect(attempts.map((a) => retryDelayMillis(a, 0.5))).toEqual([
    375, 750, 1500, 3000, 6000, 7500, 7500,
  ]);

  for (const attempt of attempts) {
    const spread = [0, 0.5, 1].map((j) => retryDelayMillis(attempt, j));
    expect(new Set(spread).size).toBe(3);
  }
});
```

Four tests added. Two hundred and eighty exist, so the suite reports **284**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 284 passed (284)` | four ran and nothing else moved |
| the four `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks, lints and is formatted |

**Name the edit that makes each assertion red:**

1. Drop the `Math.min` → `doubles its ceiling for every attempt until it caps` fails at attempt 5,
   and `stays inside its bounds however far the attempts run` fails outright. Revert.
2. Return `ceiling` and ignore `jitter` → `spends the jitter it is handed, differently at every
   attempt` fails on its `Set` size. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the retry delay > doubles its ceiling for every attempt until it caps` passes
- [ ] `the retry delay > is never shorter than half its ceiling, at every attempt` passes
- [ ] `the retry delay > spends the jitter it is handed, differently at every attempt` passes
- [ ] `the retry delay > stays inside its bounds however far the attempts run` passes
- [ ] `retry-delay.ts` contains no `Math.random`, no `setTimeout` and no `Date`
- [ ] `npm run --silent test` reports `Tests  284 passed (284)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
