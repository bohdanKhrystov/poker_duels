---
schema: 2
id: TASK-050303
title: The words the ladder says — a row line, and a season named without a clock
type: task
status: backlog
parent: STORY-0503
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, leaderboard, copy, season]
depends_on: [TASK-050302]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states every sentence exactly, character for character'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'spells a season as a month and a year, and hands back a wire string it cannot spell'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the season the response carried while the browser clock says another month'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'builds a row line from the rank, the name and the standing the server sent'
  - cd web-client && ! grep -qE 'Date|Intl|toLocale|getMonth|getFullYear' src/ladder/ladder-text.ts
  - cd web-client && npm run check
---

## Goal

The ladder has a text module, the way `history-text.ts` is the history screen's, and it can turn
`"2026-08"` into `August 2026` without ever asking the browser what month it is.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/ladder-text.ts` | create |
| `web-client/src/ladder/ladder-text.test.ts` | create |

Read, not edited: `web-client/src/history/history-text.ts` (the module this copies in shape),
`web-client/src/profile/profile-text.ts` (`coinBalanceText`),
`web-client/src/profile/name-text.ts` (`nameOrNone`).

## Scope

- Exports:

  ```ts
  export const LADDER_HEADING = "Leaderboard";
  export const MORE = "Show more";
  export function seasonName(season: string): string;
  export function rowLine(row: LadderRow): string;
  ```

- `LADDER_HEADING` is **one constant used twice** — the door on the first screen and the heading on
  the ladder screen, exactly as `HISTORY_HEADING` is. The word is the vision's own
  (*"**A leaderboard.** Ranked results over a season."*), not a new coinage.
- `seasonName("2026-08")` is `"August 2026"`. Month names come from a **twelve-entry array in this
  module**. `ADR-0061` §6 and `ADR-0002`: the wire form `2026-08` is never shown, and the client
  never works the season out from the browser's clock — *"it is wrong for two hours of every month
  in half the world"*.
- **No `Date`, no `Intl`, no `toLocale…`, no `getMonth`.** A `new Date("2026-08")` is midnight UTC
  and formatting it in a negative-offset zone prints July. The `verify:` block greps for all four.
- **The grep reads the whole file, comments included.** The KDoc explaining why this module does not
  consult the clock must therefore say *the browser's clock* and *a formatter that knows about time
  zones* rather than naming `Date`, `Intl` or `toLocaleString`. A comment that spells one of them out
  fails the `verify:` block, and the failure will look like a bug in the guard.
- A `season` whose month is not `01`–`12`, or which is not `YYYY-MM` at all, is **handed back
  unchanged**. `seasonName` never throws and never renders `undefined 2026`. (`parseLadderPage`
  already refuses such a body, so this is a total function rather than a second gate.)
- `rowLine(row)` is `` `${row.rank} ${nameOrNone(row.displayName)} ${coinBalanceText(row.coins)}` `` —
  three parts, one space between each. It is imported by the screen and rendered as one
  interpolation, which is how *"no string literal is rendered from the component"* becomes checkable.
- `nameOrNone` is imported from `profile/name-text.ts` and is the **only** thing that decides what
  stands where a name is missing (`ADR-0058`). `coinBalanceText` is imported from
  `profile/profile-text.ts`, so a negative standing prints `−1` with U+2212, the same character the
  strip uses.
- `import type { LadderRow } from "./ladder-page";` — a **type-only** import. `verbatimModuleSyntax`
  turns a value import of a type into TS1484 at `npm run typecheck`.

## Out of scope

- **The self line and its two sentences** — `TASK-050310` adds them to this file.
- **The loading, empty and failed sentences** — `TASK-050309` adds them to this file, with the
  states that use them.
- **Re-implementing `No name` or the minus sign.** Both already exist and both are imported.
- **Any colour, spacing token or edit to `tokens.css`** — `EPIC-06` owns the visual language, and
  nothing in this story edits a token sheet.

## Tests

`web-client/src/ladder/ladder-text.test.ts`, `describe("the words the ladder says")`.

| Test | Proves |
| --- | --- |
| `states every sentence exactly, character for character` | `LADDER_HEADING` is `"Leaderboard"` and `MORE` is `"Show more"`. Golden strings, asserted literally — the test does not reference the constants to build its expectation |
| `spells a season as a month and a year, and hands back a wire string it cannot spell` | `seasonName("2026-08")` is `"August 2026"`; `seasonName("2026-01")` is `"January 2026"`; `seasonName("2026-12")` is `"December 2026"`; and `seasonName("2026-13")` is `"2026-13"`. The first three catch an off-by-one in the month array from both ends |
| `names the season the response carried while the browser clock says another month` | With `vi.useFakeTimers()` and `vi.setSystemTime(new Date("2026-12-15T00:00:00Z"))`, `seasonName("2026-08")` is still `"August 2026"`. `vi.useRealTimers()` in an `afterEach`. This test is synchronous — no React, no promise — so fake timers cannot stall it |
| `builds a row line from the rank, the name and the standing the server sent` | `rowLine({ rank: 4, playerId: "p", displayName: "Ada", coins: 3 })` is `"4 Ada 3"`, and `rowLine({ rank: 215, playerId: "q", displayName: null, coins: -1 })` is `"215 No name −1"`. **Copy the minus from `profile-text.ts`** — it is U+2212, not a hyphen, and a hyphen typed here fails without saying why |

## Acceptance criteria

- [ ] `states every sentence exactly, character for character` passes
- [ ] `spells a season as a month and a year, and hands back a wire string it cannot spell` passes —
      shifting the month array by one in either direction reddens it
- [ ] `names the season the response carried while the browser clock says another month` passes —
      implementing `seasonName` with `new Date(...).toLocaleString(...)` reddens it in any zone west
      of UTC, and the `verify:` grep refuses it outright
- [ ] `builds a row line from the rank, the name and the standing the server sent` passes — replacing
      `nameOrNone(...)` with an inline `?? "No name"` leaves it green but reddens the criterion below
- [ ] `grep -c 'No name' web-client/src/ladder/ladder-text.ts` returns `0` (`ADR-0058`: one branch,
      and it is `nameOrNone`)
- [ ] `! grep -qE 'Date|Intl|toLocale|getMonth|getFullYear' web-client/src/ladder/ladder-text.ts`
      exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
