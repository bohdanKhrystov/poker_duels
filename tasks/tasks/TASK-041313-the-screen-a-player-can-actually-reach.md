---
schema: 2
id: TASK-041313
title: The screen a player can actually reach, reading through the real transport
type: task
status: done
parent: STORY-0413
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [client, history, ui, wiring, blocked]
depends_on: [TASK-041312]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'binds the history read to the browser fetch and the browser storage'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the lobby exactly as it was for a player who never opens the record'
  - cd web-client && npm run check
---

## Goal

The history screen stops being a component nothing imports: a player can get to their whole duel
record from the running client, and it reads through `window.fetch` and the browser's `Storage`.

## Blocked on `DEC-053`

**The product owner's.** The client has **no navigation of any kind**: `App.tsx` renders the lobby,
and the lobby swaps itself for the duel table and the result screen off store state. Nothing merged
says how a player reaches a second screen, and `STORY-0413` does not say either — it specifies the
screen and stops.

Open: **is the whole duel record its own screen reached from the lobby — and what is the affordance
called, and how does a player come back — or is it a section of the lobby beneath the strip?**
`STORY-0412`'s account screens, `STORY-0415`'s offer and `EPIC-05`'s leaderboard all inherit
whichever answer this gets, which is why it is not settled inside a ticket.

Whether the client grows URL-addressable routes and a working browser *Back* is the **architect's**
and is expected to be raised by the ADR that answers this — it only arises if the answer is a
distinct screen.

**The half below is settled under either answer** and does not move: the read is bound once, at
module scope, from `window.fetch` and `localStorage`. The PR that unblocks this adds the navigation
Scope bullets, the tests for the affordance, and their `verify:` lines.

## Files

| File | Action |
| --- | --- |
| `web-client/src/main.tsx` | modify — one module-scope binding of the history read |
| `web-client/src/App.tsx` **or** `web-client/src/lobby/Lobby.tsx` | modify — whichever `DEC-053`'s answer names; exactly one of the two |
| `web-client/src/App.test.tsx` | modify — the tests below |

Read, not edited: `web-client/src/profile/duel-page.ts`,
`web-client/src/profile/profile-provider.tsx` (the stable-reference pattern to copy).

## Scope

- In `main.tsx`, at **module scope** beside `readProfile` and `setName`:

  ```ts
  const readHistory = (query: HistoryQuery): Promise<DuelPageRead> =>
    readDuelPage({
      fetch: (path, init) => window.fetch(path, init),
      storage: localStorage,
      query,
    });
  ```

  Module scope for the reason the two beside it give: a reference created inside the JSX would be a
  new function on every render, and this one is an effect dependency.
- `localStorage` is read **here and passed down**, never inside the screen or the read. Under Vitest,
  Node's own inert `localStorage` shadows jsdom's, so a component that reached for the global would
  be untestable and would behave differently in the browser; `main.tsx` is not under test and is the
  one place the real one is named.
- Nothing about the lobby, the duel table or the result screen changes for a player who never opens
  the record. `ADR-0036` — every screen reachable anonymously stays reachable anonymously — and this
  screen gates on nothing.


**Carried from `TASK-041308`.** `HistoryScreen` renders one heading, which `ADR-0060` names — but a
**second** one is currently uncatchable: measured, adding an `<h3>` to it leaves all 455 tests green,
because no test mounts `HistoryScreen` inside the lobby tree and `Lobby.test.tsx`'s guard only
inspects that tree. This ticket makes the screen reachable, so it is where the guard can finally
reach it: **assert the mounted history screen carries exactly one heading**, and confirm a second
fails it. This is the same shape as the gap `TASK-041108` recorded and `TASK-041113` closed — a
no-heading rule guarded only where something happened to render.

## Out of scope

- Anything the answer to `DEC-053` does not require. If it is a section of the lobby, no navigation
  state is added at all.
- Prefetching the record, or reading it before a player asks for it. The lobby's strip already costs
  two requests on load, and a third for a screen nobody opened is a cost with no reader.
- A protocol version bump. Nothing here is a socket fact, and `ADR-0057` §4's cursor is negotiated by
  no version.
- Remembering which screen a player was on across a reload.

## Tests

`web-client/src/App.test.tsx`.

| Test | Proves |
| --- | --- |
| `binds the history read to the browser fetch and the browser storage` | The binding in `main.tsx` is asserted by reading its source: it names `window.fetch` and `localStorage`, and `HistoryScreen.tsx` and `duel-page.ts` name neither. Fails against a component that reaches for a global — the failure that works in a browser and cannot be tested at all — and it is a source assertion because `main.tsx` mounts the real DOM and is not rendered by any test |
| `leaves the lobby exactly as it was for a player who never opens the record` | The three merged tests in this file still find the heading, its class and the *Create a duel room* button, and one new assertion pins that nothing from the history screen is on the first screen a player sees. Fails against a change that renders the record unconditionally |
| *(named by the ADR that answers `DEC-053`)* | The affordance: that a player can reach the record and get back |

Two tests written now, plus the one the answer names.

## Acceptance criteria

- [ ] `binds the history read to the browser fetch and the browser storage` passes
- [ ] `leaves the lobby exactly as it was for a player who never opens the record` passes
- [ ] The criterion `DEC-053`'s ADR names passes
- [ ] The three merged tests in `App.test.tsx` pass unchanged
- [ ] `grep -cE 'localStorage\.|window\.fetch\(' web-client/src/history/HistoryScreen.tsx` returns `0`
- [ ] `grep -c 'readDuelPage' web-client/src/main.tsx` returns at least `1`
- [ ] Exactly one of `App.tsx` and `Lobby.tsx` differs, not both
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
