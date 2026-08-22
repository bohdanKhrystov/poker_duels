---
schema: 2
id: TASK-050310
title: The self line has two sentences, and the one for no place prints no number at all
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, leaderboard, copy, self-standing]
depends_on: [TASK-050309]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states the rank and the standing the response carried, for two different responses'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says a player with no place has none, and prints no number at all'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names one duel coin in the singular'
  - cd web-client && npm run check
---

## Goal

The ladder's text module can turn a `SelfStanding` into the one sentence the reader gets about
themselves — and the *no place* sentence contains no digit anywhere.

## The words, and where they come from

`ADR-0065` §1 delegates the wording to this module and constrains it twice: the line is **a
statement about this season and nothing evaluative** — no encouragement, no *doing well*, no
*closing on the leader*, no comparison to a named player — and it uses the vocabulary the vision
fixes: *duel*, *season*, *rival*, never *buy-in* or *bankroll*. The two strings below satisfy both
and are **golden**: a coder rewording either is changing a decision, not polishing copy.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-text.ts` | modify — one constant and one function |
| `web-client/src/ladder/ladder-text.test.ts` | modify — **adds tests only**; no assertion written by `TASK-050303` changes |

## Scope

- Add:

  ```ts
  export const NO_PLACE_THIS_SEASON = "You have no place on this season's leaderboard.";
  export function selfLine(self: SelfStanding): string;
  ```

- `selfLine` is **the only place that branches** on which of the two answers a `SelfStanding` is,
  exactly as `emptyLine` in `history-text.ts` is the only place that branches on which empty state
  the record is in. A component choosing between the sentences inline would be a second place able
  to get it wrong.
  - `rank` and `coins` both numbers → `` `You are rank ${rank} this season, on ${coinBalanceText(coins)} duel ${coins === 1 ? "coin" : "coins"}.` ``
  - either `null` → `NO_PLACE_THIS_SEASON`.
- **`0` is a real standing and *no place* is not one** (`ADR-0065` §4). `selfLine({ rank: 195, coins: 0 })`
  states rank `195` and `0 duel coins`; `selfLine({ rank: null, coins: null })` states neither and
  prints no digit.
- `coinBalanceText` is imported, so a negative standing reads `−1` with U+2212 — the same character
  the rows use.
- `import type { SelfStanding } from "./ladder-page";` — type-only, or `verbatimModuleSyntax` fails
  the typecheck with TS1484.

## Out of scope

- **The third state — a response carrying no self standing at all.** That is `self === null`, and
  `selfLine` is never called with it: the screen renders no element (`TASK-050311`). This function
  takes a `SelfStanding`, not a `SelfStanding | null`, so the case cannot reach it.
- **A ladder total (*5th of 404*), a movement line, a streak, or a count of the players sharing the
  rank** — `ADR-0065` §7 names each and refuses each. `TASK-050313` asserts their absence.
- **Anything evaluative, or any comparison to another player** — `ADR-0065` §1.
- **A second-person variant of `No name`** — `ADR-0058` §2. This sentence names no player.

## Tests

`web-client/src/ladder/ladder-text.test.ts`, same `describe`, three new tests.

| Test | Proves |
| --- | --- |
| `states the rank and the standing the response carried, for two different responses` | `selfLine({ rank: 5, coins: 3 })` is `"You are rank 5 this season, on 3 duel coins."` and `selfLine({ rank: 215, coins: -1 })` is `"You are rank 215 this season, on −1 duel coins."`. **Two fixtures, because one cannot tell a rendered field from a hardcoded string** (`ADR-0065` §1) |
| `says a player with no place has none, and prints no number at all` | `selfLine({ rank: null, coins: null })` is exactly `NO_PLACE_THIS_SEASON`, and `/\d/.test(...)` on it is `false`. Asserted beside `selfLine({ rank: 195, coins: 0 })`, which **does** state a rank and a `0`: the two are different strings, and a function that printed `0` for both reddens |
| `names one duel coin in the singular` | `selfLine({ rank: 1, coins: 1 })` ends `"on 1 duel coin."`. One character, and it is the one a template literal gets wrong |

## Acceptance criteria

- [ ] `states the rank and the standing the response carried, for two different responses` passes for
      both — hardcoding either number, or swapping the rank for the standing, reddens it
- [ ] `says a player with no place has none, and prints no number at all` passes, including the
      `/\d/` assertion — returning `"You are rank 0 this season, on 0 duel coins."` for two nulls
      reddens it
- [ ] `names one duel coin in the singular` passes — dropping the plural branch reddens it
- [ ] `NO_PLACE_THIS_SEASON` and the placed sentence are different strings, and neither contains
      *doing well*, *keep going*, *leader*, or any other evaluation
- [ ] Every test `TASK-050303` wrote still passes, with no assertion in it edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
