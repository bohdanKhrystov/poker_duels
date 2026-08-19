---
schema: 2
id: TASK-041308
title: The screen renders the page in the order it arrived, and derives no fact
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, history, ui]
depends_on: [TASK-041307]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders the page it was handed, in the order it was handed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints each row from the server, taking the outcome from the outcome and not the coin'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names an opponent who has a name, and prints No name for one who has not'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'asks for the first page once, with no cursor and no filter'
  - cd web-client && npm run check
---

## Goal

`HistoryScreen` exists: it asks for the first page of the whole record through the read it is handed,
and renders what came back in the order it came back.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | create |
| `web-client/src/history/HistoryScreen.test.tsx` | create |

Read, not edited: `web-client/src/profile/ProfileStrip.tsx` (the duel line already shipped, whose
words this reuses), `web-client/src/profile/profile-text.ts`, `web-client/src/profile/name-text.ts`.

## Scope

- ```tsx
  export function HistoryScreen(props: {
    readonly read: (query: HistoryQuery) => Promise<DuelPageRead>;
  }): ReactElement;
  ```

  `read` is injected, exactly as `ProfileProvider`'s is, and must be a stable reference — it is the
  effect's only dependency. The component holds `useReducer(historyReducer, NO_FILTER,
  initialHistory)` and calls `read`; it constructs no `fetch` and touches no `Storage`, so nothing
  here can reach Node's inert `localStorage` global under Vitest.
- One `ask(query)` helper: dispatch `asked` with `query.after`, await `read(query)`, then dispatch
  `page` on a `page` answer and `failed` on `unavailable`. On mount it asks
  `firstPageQuery(NO_FILTER)`.
- **`no-profile` is dispatched as an empty page**, not as a failure: a browser holding no profile has
  played no duels, so *"No duels yet."* is true of it and *"Your duels did not load."* is not. This
  keeps the screen at the four states `STORY-0413` names rather than inventing a fifth.
- A `<section aria-label="your duels">` with `HISTORY_HEADING` as its heading, and the rows in a
  `<ul>` of `<li>`, keyed by `duelId` — the shape `ProfileStrip` already uses.
- Each row prints, in this order and with these functions: `outcomeWord(outcome)`,
  `coinDeltaText(coinDelta)`, `handsPlayed` with `hand`/`hands`, `vs`, `nameOrNone(
  opponentDisplayName)`, `finishedAtText(finishedAt)`. **The words are reused, never re-authored** —
  `STORY-0413`'s design note, and the reason `outcomeWord` exists.
- Classes compose `design/tokens/tokens.css` through the Tailwind theme, as `ProfileStrip` does. No
  colour, size or weight is authored here.

## Out of scope

- The loading, empty and failed states — `TASK-041309`. Until it lands the screen may render an
  empty `<ul>`, and no test here asserts anything about an empty page in either direction.
- Asking for another page, or any filter control — `TASK-041310` and `TASK-041311`.
- Mounting the screen anywhere. Nothing imports it yet; `TASK-041313` owns that and is blocked on
  `DEC-053`.
- Extracting a duel line shared with `ProfileStrip`. **A refusal, not an omission:** it would edit
  `ProfileStrip.tsx`, a lobby file this story has no reason to disturb, and the four functions that
  produce the *words* are already shared, so what is duplicated is arrangement — which `EPIC-06`
  owns for both surfaces anyway.
- Anything about React StrictMode's double effect. `ADR-0032` puts one-per-tab concerns in the boot
  module; a `GET` is idempotent, and the tests render without StrictMode as every other component
  test in this client does.

## Tests

`web-client/src/history/HistoryScreen.test.tsx`, describe block `"the history screen"`. `read` is a
`vi.fn()` created once per test, so the reference is stable across renders. Every multi-row fixture
is **monotone in no field**.

| Test | Proves |
| --- | --- |
| `renders the page it was handed, in the order it was handed` | Three rows monotone in no field, distinguished by opponent name; `screen.getAllByRole("listitem")` mapped to `textContent` and `toEqual`-compared against the three lines in fixture order. The section's `aria-label` and `HISTORY_HEADING` are asserted in the same test. Fails against any client-side `sort` or `reverse` — the defect `TASK-031112` pinned for the strip — and a fixture monotone in nothing means no sort can pass by luck |
| `prints each row from the server, taking the outcome from the outcome and not the coin` | One row with `outcome: "WON"` and `coinDelta: -1` — a combination the server never sends, and the only input that separates the two sources. The line reads `Won`, `−1` (U+2212), `23 hands`, and the text `finishedAtText` returns for the fixture's instant, computed by the test rather than hardcoded so the assertion is not a property of the runner's locale. Fails against a row that derives the word from the delta's sign, and against a re-authored `Win`/`Loss` |
| `names an opponent who has a name, and prints No name for one who has not` | **One render, two rows**: `opponentDisplayName: "Ada"` and `null`, both from bodies carrying `opponentPlayerId`. The screen shows `Ada`, shows `No name`, and `container.innerHTML` contains neither id. Two rows in one render is what makes it non-vacuous — a single row cannot tell a copied field from a constant — and it fails against the `Player-3F2A` fallback `ADR-0029` §6 forbids |
| `asks for the first page once, with no cursor and no filter` | After the first render settles, `read` has been called exactly once, with `{ outcome: null, opponent: "", after: null }`. Fails against a component that asks twice, and against one that invents a cursor or a page size for its first request |

Four tests, in a new file.

## Acceptance criteria

- [ ] All four tests above pass, under describe block `the history screen`
- [ ] The three-row fixture is monotone in none of `finishedAt`, `duelId`, `handsPlayed` or
      `coinDelta`
- [ ] The `finishedAt` assertion calls `finishedAtText` rather than hardcoding a formatted date
- [ ] `grep -cE 'coinDelta *[<>]|Math\.sign' web-client/src/history/HistoryScreen.tsx` returns `0` —
      no outcome is worked out from a delta
- [ ] `grep -cE 'localStorage\.|window\.fetch\(' web-client/src/history/HistoryScreen.tsx` returns `0`
- [ ] `grep -cE '#[0-9a-fA-F]{3,8}|rgb\(' web-client/src/history/HistoryScreen.tsx` returns `0` — no
      colour is authored
- [ ] No file outside the two listed differs
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
