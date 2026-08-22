---
schema: 2
id: TASK-050307
title: The screen asks for the first page and prints it in the order it arrived, ranks and all
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, leaderboard, ui, rank]
depends_on: [TASK-050306]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for the first page once, with no cursor'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints the rows in the order the server sent them, and not in coin order'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints the rank the response carried, on a page that does not start at one'
  - cd web-client && npm run check
---

## Goal

There is a ladder screen. It asks for the first page when it mounts and prints exactly the rows it
was handed, in the order it was handed them, carrying the ranks it was given.

## The trap this ticket owns

A screen that computes a rank from a row's position looks right on page one of a ladder whose ranks
happen to be `1..n`, and is wrong everywhere after — `EPIC-05`'s named non-negotiable, and
`ADR-0064` §2. **Every fixture in this file carries ranks that are not `1..n`**, so a client that
renumbers has nowhere to hide.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | create |
| `web-client/src/ladder/LadderScreen.test.tsx` | create |

Read, not edited: `web-client/src/history/HistoryScreen.tsx` — the component this copies in shape:
`read` injected as a stable reference, a `useReducer` beside it, one `useEffect` that asks, and no
`fetch` and no `Storage` anywhere inside.

## Scope

- `LadderScreen` takes one prop:

  ```ts
  props: { readonly read: (after: string | null) => Promise<LadderRead> }
  ```

  `read` is injected exactly as `HistoryScreen`'s is and must be a stable reference — it is the
  effect's only dependency. The component constructs no `fetch` and touches no `Storage`, so nothing
  here can reach Node's inert `localStorage` global under Vitest.
- Holds `useReducer(ladderReducer, initialLadder())`. One `ask(after)` callback dispatches `asked`,
  awaits `read(after)`, and dispatches `page` or `failed`. One `useEffect` calls `ask(null)`.
- Renders, in this order and nothing else yet:

  ```tsx
  <section aria-label="leaderboard" className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4">
    <h2>{LADDER_HEADING}</h2>
    <ul className="w-full">
      {state.rows.map((row) => (
        <li key={row.playerId} className="border-t border-hairline py-3 first:border-t-0">
          <p className="text-small">{rowLine(row)}</p>
        </li>
      ))}
    </ul>
  </section>
  ```

- **Exactly one heading**, and the `<ul>` is **always rendered**, empty or not. Both are load-bearing
  for later tickets: a conditional list would move under `TASK-050309`, and a second heading is what
  `TASK-050314`'s guard counts.
- **No string literal is rendered.** Every visible character comes from `ladder-text.ts`, and a row
  is one interpolation of `rowLine(row)` — no `{" "}`, no separator, no unit word typed in the JSX.
- Only token classes already used by `HistoryScreen` are composed. No colour is authored and
  `design/tokens/tokens.css` is not touched (`EPIC-06` owns the visual language).
- **The `localStorage` grep below reads the whole file, comments included.** `HistoryScreen`'s KDoc
  names the global while explaining why it avoids it; this component's must not. Say *the browser's
  storage* and *the browser's fetch*, or a criterion fails on a comment.
- **Every fixture in `LadderScreen.test.tsx` carries `nextCursor: null`** until `TASK-050312`, which
  is the ticket that adds the *Show more* control. A fixture with a cursor here would sprout a
  button under that ticket and break assertions written now.

## Out of scope

- **The season name, and the loading, empty and failed states** — `TASK-050309`.
- **The self line** — `TASK-050311`.
- **Walking to a second page, and any *Show more* control** — `TASK-050312`.
- **A way back to the first screen.** `ADR-0060`: the way back is rendered by the swap, never by the
  screen, so this component knows nothing about navigation and renders no *Back* button.
  `TASK-050314` asserts that.
- **Sorting or filtering the rows.** The client never sorts (`ADR-0002`), and nothing gates a place
  (`ADR-0063`).

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, `describe("the ladder screen")`. A local helper
builds a `LadderPage`; a `vi.fn()` read answers it. Read the rows with
`within(screen.getByRole("list")).getAllByRole("listitem")` so later tickets may add elements
**outside** the list without touching these assertions.

| Test | Proves |
| --- | --- |
| `asks for the first page once, with no cursor` | `read` is called exactly once on mount, with `null`. Fails against a component that asks twice, or that asks with a cursor it invented |
| `prints the rows in the order the server sent them, and not in coin order` | A page whose four rows carry coins `[3, 1, 5, 0]` — deliberately **not** descending, which no real ladder would be — renders four `<li>` whose text is `rowLine` of each row in the wire order. A client that sorts by coins reddens |
| `prints the rank the response carried, on a page that does not start at one` | A page whose ranks read `[3, 3, 5, 9]` with names `["Ada", null, "Bo", "Cy"]`: the four `<li>` texts are exactly `["3 Ada 3", "3 No name 1", "5 Bo 5", "9 Cy 0"]`. Every row shows its own number, the repeat is printed twice, the skip from `5` to `9` is printed as sent, and nothing is blanked, grouped or de-duplicated |

The expected strings are written **literally** in the test, not built by calling `rowLine` — a test
that calls the function under test asserts nothing about what it produces.

## Acceptance criteria

- [ ] `asks for the first page once, with no cursor` passes
- [ ] `prints the rows in the order the server sent them, and not in coin order` passes — inserting a
      `.sort((a, b) => b.coins - a.coins)` before the map reddens it
- [ ] `prints the rank the response carried, on a page that does not start at one` passes against
      ranks `[3, 3, 5, 9]` — rendering the row's array index plus one instead of `row.rank` reddens
      it, and so does de-duplicating the repeated `3`
- [ ] `grep -cE 'localStorage|window\.fetch|fetch\(' web-client/src/ladder/LadderScreen.tsx` returns `0`
- [ ] `grep -c 'Back' web-client/src/ladder/LadderScreen.tsx` returns `0`
- [ ] `grep -c '<h' web-client/src/ladder/LadderScreen.tsx` returns `1` — one heading, which
      `TASK-050315` guards again once the screen is reachable from the tree
- [ ] Every fixture in `LadderScreen.test.tsx` carries `nextCursor: null`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
