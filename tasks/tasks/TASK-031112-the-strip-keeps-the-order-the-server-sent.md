---
schema: 2
id: TASK-031112
title: The strip lists recent duels in the order the server sent them
type: task
status: ready
parent: STORY-0311
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [client, profile, tests]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +359 passed \(359\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'lists the duels in the order they arrived, not one it chose'
  - cd web-client && npm run check
---

## Goal

The order of the recent-duel list is pinned to the order the server sent, by a fixture that a
client-side `sort` or `reverse` cannot survive.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.test.tsx` | modify |
| `web-client/src/profile/ProfileStrip.tsx` | read — the `state.duels.map` whose order is the claim |
| `web-client/src/profile/profile-text.ts` | read — `outcomeWord`, `coinDeltaText` |

## Scope

- One test added to the existing `describe("the profile strip")` block. Nothing else in the file
  moves: the five tests already there keep their bodies, their names and their fixtures.
- No production file changes. `ProfileStrip.tsx` already renders `state.duels` in order; this ticket
  is the assertion that says so, which nothing currently makes.
- **Why it is missing.** `TASK-031108`'s fixture is two rows already in descending `finishedAt`
  order, and ascending `handsPlayed` order, and ascending `duelId` order — so a `sort` on any of the
  three would ship green. `TASK-031111`'s guard reads the surface for leaks and takes no position on
  order either. `TASK-021107` already proved the server returns the rows newest first and capped, so
  the client's whole job here is to not touch them.

## Tests

`web-client/src/profile/ProfileStrip.test.tsx`, added to `describe("the profile strip")`.

| Test | Proves |
| --- | --- |
| `lists the duels in the order they arrived, not one it chose` | three rows, given in an order that is **monotone in no field at all**, render top to bottom in exactly that order |

The fixture, in the order it is passed:

| # | `duelId` | `outcome` | `coinDelta` | `handsPlayed` | `finishedAt` |
| --- | --- | --- | --- | --- | --- |
| 1 | `duel-b` | `DREW` | `0` | `12` | `2026-03-02T09:00:00Z` |
| 2 | `duel-a` | `WON` | `1` | `41` | `2026-05-14T18:20:00Z` |
| 3 | `duel-c` | `LOST` | `-1` | `7` | `2026-01-09T22:05:00Z` |

Every column rises and then falls, so **no** single-field `sort` — ascending or descending, on the
id, the outcome word, the coin delta, the hand count or the instant — reproduces this order, and
`reverse()` does not either. Three rows rather than two is what makes that possible: with two rows,
every field is trivially sorted one way or the other.

The assertion is the whole rendered sequence, not a spot check:

- `screen.getAllByRole("listitem")` has length 3;
- mapping each item's `textContent`, the hand counts appear as `12 hands`, `41 hands`, `7 hands`
  **in that order** — assert the mapped array with `toEqual`, so a swapped pair fails rather than a
  `toContain` that would pass on any permutation;
- the outcome words appear as `Drew`, `Won`, `Lost` in that order, asserted the same way.

Two independent columns are asserted so that a reorder cannot be hidden by one of them coinciding.

One test added. Three hundred and fifty-eight exist, so the suite reports **359**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 359 passed (359)` | one ran and nothing else moved |
| the `--reporter=verbose` grep | the name exists |
| `npm run check` | typechecks, lints, is formatted |

**Name the edit that makes the assertion red** — run it, quote the failure in the PR, revert:

1. In `ProfileStrip.tsx`, render
   `[...state.duels].sort((a, b) => b.finishedAt.localeCompare(a.finishedAt))` instead of
   `state.duels` → the new test fails **and** `shows one line per duel, with its outcome, coin,
   hands and time` still passes, which is the gap this ticket closes. Say in the PR that the older
   test stayed green under the planted sort.

## Acceptance criteria

- [ ] `the profile strip > lists the duels in the order they arrived, not one it chose` passes
- [ ] The fixture has three or more rows and is monotone in none of `duelId`, `outcome`,
      `coinDelta`, `handsPlayed`, `finishedAt`
- [ ] The order is asserted with `toEqual` over a mapped array, in two independent columns
- [ ] The five tests already in `ProfileStrip.test.tsx` keep their bodies, names and fixtures
      unchanged, and no assertion in them is weakened
- [ ] No production file differs
- [ ] `npm run --silent test` reports `Tests  359 passed (359)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
