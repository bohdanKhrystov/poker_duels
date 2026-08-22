---
schema: 2
id: TASK-050314
title: The ladder is a screen a player can reach — one control in, one control back
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, leaderboard, ui, wiring]
depends_on: [TASK-050313]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the first screen for the ladder, and comes back to it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'binds the ladder read to the browser fetch and the browser storage'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders no Back button when the ladder screen is mounted on its own'
  - cd web-client && npm run check
---

## Goal

The ladder stops being a component nothing imports: a player opens it from the first screen with one
control, comes back with one control, and it reads through `window.fetch` and the browser's
`Storage`.

## The shape, and where it comes from

`ADR-0060`, with `HistoryScreen` and `Lobby` as the worked example rather than a coincidence to be
re-derived: the ladder is **its own screen**, never rendered beside the lobby, in by one control on
the branch that offers *Create a duel room*, out by one control. **The way back is rendered by the
swap, never by the screen itself**, so `LadderScreen` knows nothing about navigation and the
affordance is assertable with no transport at all.

This door is the **fifth** control on the first screen, after *Create a duel room*, *Join the duel*,
the name surface and *Your duels*. `ADR-0060` predicted exactly that — *"the first screen becomes the
only door and will crowd"* — and this ticket does not reopen it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify — one module-scope binding, a context, a provider, a hook |
| `web-client/src/lobby/Lobby.tsx` | modify — the door, the swap, and the way back |
| `web-client/src/App.test.tsx` | modify — the mock of `./main`, `renderApp`, and the three tests below |

## The blast radius, named

`App.test.tsx` mocks `./main` with a factory that returns `HistoryProvider` and `useHistory` only,
and `renderApp` wraps the tree in `<HistoryProvider>`. `Lobby.tsx` gaining `useLadder` from `../main`
makes that mock insufficient for **every** test in the file, not just the new ones. So:

- the mock factory gains `LadderProvider` and `useLadder`, answering a fake read the same way the
  history one does;
- `renderApp` wraps `<LadderProvider>` as well.

**No assertion in any existing test in `App.test.tsx` changes.** The scaffolding moves; the
expectations do not. If a merged assertion has to be weakened to make this pass, stop and report it
rather than editing it.

Two other files render `Lobby` and are **not** opened by this ticket: `lobby/Lobby.test.tsx`, whose
heading guard counts `0` headings on the first screen — a door is a button and adds none — and
`e2e/duel-secrecy.test.tsx`, which renders `Lobby` outside any provider, so `useLadder()` answers
`null` and the swap branch is simply never taken. `npm run check` runs both; if either goes red, the
cause is in `Lobby.tsx` and the fix belongs here.

## Scope

- In `main.tsx`, at **module scope** beside `readProfile`, `setName` and `readHistory`:

  ```ts
  const readLadder = (after: string | null): Promise<LadderRead> =>
    readLadderPage({
      fetch: (path, init) => window.fetch(path, init),
      storage: localStorage,
      after,
    });
  ```

  Module scope for the reason the three beside it give: a reference created inside the JSX would be
  a new function on every render, and this one is an effect dependency.
- `LadderContext`, `LadderProvider` and `useLadder` mirror `HistoryContext`, `HistoryProvider` and
  `useHistory` exactly, and `<LadderProvider>` joins the render tree at the bottom of `main.tsx`.
- `localStorage` is named **here and passed down**, never inside the screen or the read. Node's own
  inert `localStorage` shadows jsdom's under Vitest, so a component reaching for the global would be
  untestable and would behave differently in the browser.
- In `Lobby.tsx`, on the branch where `state.view === null` and `state.roomCode === null`: a
  `showLadder` piece of state, a swap that renders `<LadderScreen read={readLadder} />` with a
  *Back* button beside it, and a door button labelled `{LADDER_HEADING}`, imported from
  `ladder-text.ts`.
- The *Back* button reuses the **same literal the history swap already uses**, in the same place.
  One word, one spelling, and no `BACK` constant is invented in `ladder-text.ts` to sit beside a
  literal that means the same thing.
- The door is rendered **after** the *Your duels* door, on the same branch, and gates on nothing.

## Out of scope

- **The two conditions on the door** — that it survives a failed profile read and stands down for a
  duel in progress — are `TASK-050315`, in this same file.
- **A URL, an address, or a working browser *Back*** — `DEC-054`, the architect's, and
  `EPIC-04`'s `STORY-0412`. This story uses whatever exists when it starts, which is nothing.
- **Remembering which screen a player was on across a reload**, and prefetching the ladder before a
  player asks for it.
- **Touching `ProfileStrip.tsx`** — `ADR-0065` §2 leaves it printing the all-time counter, and this
  story does not open it.
- **A protocol version bump.** Nothing here is a socket fact.

## Tests

`web-client/src/App.test.tsx`, three new tests.

| Test | Proves |
| --- | --- |
| `leaves the first screen for the ladder, and comes back to it` | One test that goes both ways, as `TASK-041313` does for the record: the lobby is showing, the `Leaderboard` control is clicked, *Create a duel room* is gone and the section labelled `leaderboard` is on screen; *Back* is clicked, *Create a duel room* is back and the ladder is gone |
| `binds the ladder read to the browser fetch and the browser storage` | A source assertion, like the history one beside it: `main.tsx` names `readLadderPage`, `window.fetch` and `localStorage`, and neither `LadderScreen.tsx` nor `ladder-read.ts` names `localStorage` or `window.fetch`. It is a source assertion because `main.tsx` mounts the real DOM and is rendered by no test |
| `renders no Back button when the ladder screen is mounted on its own` | `<LadderScreen read={fake} />` rendered directly holds no button named *Back*. `ADR-0060`: the way back belongs to the swap, and a screen that grew one would be a screen that knows about navigation |

## Acceptance criteria

- [ ] `leaves the first screen for the ladder, and comes back to it` passes in both directions
- [ ] `binds the ladder read to the browser fetch and the browser storage` passes — moving the
      binding into `LadderScreen.tsx` reddens it
- [ ] `renders no Back button when the ladder screen is mounted on its own` passes — rendering the
      *Back* button inside `LadderScreen` reddens it
- [ ] Every test already in `App.test.tsx` passes, with **no assertion in any of them edited** — only
      the `vi.mock("./main", …)` factory and `renderApp` differ
- [ ] Every test already in `Lobby.test.tsx` passes unchanged, and that file is not opened
- [ ] `grep -cE 'localStorage|window\.fetch' web-client/src/ladder/LadderScreen.tsx` returns `0`
- [ ] `grep -c 'readLadderPage' web-client/src/main.tsx` returns at least `1`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
