---
schema: 2
id: TASK-141504
title: The notice stands where an incoming offer stands, and nowhere else
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, rematch, notice]
depends_on: [TASK-141503]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +5 passed \(5\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +113 passed \(113\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qE '^ *Tests +36 passed \(36\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"role=\"status\"") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"useEffect") || index($0,"useLayoutEffect") || index($0,"setTimeout") || index($0,"setInterval") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"role=\"alert\"") || index($0,"role=\"dialog\"") || index($0,"alertdialog") || index($0,"aria-live") || index($0,"aria-hidden") || index($0,"autoFocus") || index($0,"tabIndex") || index($0,".focus()") || index($0,"inert") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"createPortal") || index($0,"data-testid") || index($0,"localStorage") || index($0,"sessionStorage") || index($0,"window.location") || index($0,"props") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"fixed") { n++ } END { exit (n < 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^import / && index($0,"routing/room-standing") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^import / && index($0,"./rematch-stand") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^import / && index($0,"./rematch-text") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - sh -c '! grep -qE "[[:space:]\":](inset-0|inset-x-0|inset-y-0|top-0|bottom-0|left-0|right-0)[[:space:]\":]" web-client/src/result/RematchNotice.tsx'
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/App.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/result/RematchNotice.tsx` exists: a props-less component that says
`Your rival offers a rematch` on a chosen screen when the rival's seat has offered, and returns
`null` everywhere else. It is mounted by nothing yet — `TASK-141508` does that, deliberately last,
so `develop` never carries a mounted panel a player cannot dismiss.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.tsx` | create |
| `web-client/src/result/RematchNotice.test.tsx` | create |

Read [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§1, 2, 5 and 6, `web-client/src/routing/room-standing.ts`, `web-client/src/result/rematch-stand.ts`,
`web-client/src/result/RematchControl.test.tsx` (the shape a test of this kind takes here), and
`design/screens/rematch-panel.html` — the panel's shell, its colours and its place are that card's
and are transcribed, not chosen. **Nothing outside the table above is changed**; two `verify:` lines
diff `Lobby.tsx` and `App.tsx` against `develop` and fail on one byte.

## Scope

- **Props-less.** It reads `useDuelState()`, `useRoomAwaited()` and `useScreen()` for itself
  (`ADR-0138` §1: props would have to be handed down by `Lobby`, and being rendered by `Lobby` is
  the one thing this component cannot be). A gate refuses the word `props` on any non-comment line.
- **The gate is `ADR-0114`'s predicate, called a second time, and the fact:**

  ```tsx
  const standing = roomStanding(state, roomAwaited);
  const shown = rulingOn(screen, standing) === "honour" ? screen : "first";
  const { theirs } = rematchStand(state.rematchOffers, state.mySeat);
  if (shown === "first" || !theirs) return null;
  ```

  Both are the **same pure functions `Lobby` already calls**, imported from
  `routing/room-standing.ts` and `result/rematch-stand.ts`. `ADR-0138` §2: this is a second *call*,
  not a second *predicate* — a bespoke test of `state.outcome` or of the address would be the
  violation, and there is none. Nothing is derived and nothing is recomputed by hand. Three gates
  require exactly one `import` line each from `routing/room-standing`, `./rematch-stand` and
  `./rematch-text`, so a hand-rolled copy of any of the three fails.
- **No effect of any kind.** No `useEffect`, no `useLayoutEffect`, no `setTimeout`, no
  `setInterval`, no subscription beyond the three hooks. This is what makes `ADR-0123` §5's *never
  retires itself on a timer* structural and what keeps this component out of `ADR-0114` §3's
  layout-effect restore.
- **`role="status"` on the root, exactly once, and nothing else in the assistive register.**
  `ADR-0138` §6 and `ADR-0143` §1 agree: polite, never `alert`, never `dialog`, never
  `alertdialog`, no `aria-live` of its own, no `aria-hidden` on anything, no `autoFocus`, no
  `tabIndex`, no `.focus()`, no `inert`.
- **Out of flow.** The root carries `fixed`, transcribed from the card. `ADR-0138` §5: an in-flow
  panel would move the screen beneath it at the instant it arrived, and out-of-flow is what makes
  `ADR-0123` §2's *"the player may go on doing what they were doing"* literally true rather than
  nearly true.
- **Every spacing utility uses a named step `1`–`9`.** Measured at `899d81f7`: `app.css:55` sets
  `--spacing: initial`, so `inset-0`, `inset-x-0`, `inset-y-0`, `top-0`, `bottom-0`, `left-0` and
  `right-0` generate **no CSS at all** — verified absent from `dist/assets/*.css` after
  `npx vite build`, while `bottom-5`, `left-5`, `right-5`, `mx-auto`, `fixed`, `z-10`,
  `pointer-events-none`, `pointer-events-auto`, `shadow-pop` and `bg-surface-raised` all generate.
  This is `TASK-140502`'s `min-w-0` defect in a new place and it is **silent**: the class sits in
  the DOM and does nothing.
- **No portal, no `data-testid`, no test-only prop, no storage, no `window.location` write**
  (`ADR-0138` §§1–2 and Alternatives, `ADR-0100` §5, `ADR-0086` §2).
- **Every gate on this file skips comment lines**, because the KDoc this component owes says *"no
  `useEffect`, no `setTimeout`"* and *"takes no props"* in as many words. Measured: the naive
  substring form fails on a correct file. Do not delete the comment to satisfy a gate.

## Out of scope

- **`App.tsx`.** The mount is `TASK-141508`'s.
- **The accept button and the press** (`TASK-141505`), **the gone room** (`TASK-141506`) and
  **`Not now`** (`TASK-141507`). This component renders one sentence and nothing else.
- **`Lobby.tsx`, `use-screen.ts`, `screen.ts`, `room-standing.ts`, `rematch-stand.ts`,
  `duel-state.ts`.** None is opened; the two pure modules are imported as they are.

## Tests

`RematchNotice.test.tsx` — **5** tests. Every one renders
`<DuelProvider store={store} send={vi.fn()}><RematchNotice /></DuelProvider>` after setting
`window.location.hash`, which is what a player's own navigation does; `beforeEach` resets the hash.
The room fixture is three frames **in this order**, and the order is load-bearing because
`DuelFinished` clears `rematchOffers` (`duel-state.ts:350-361`): `RoomJoined` with the seat, then
`DuelFinished`, then `RematchOffered` with the offering seat.

| Test | Proves |
| --- | --- |
| `stands over a chosen screen when the rival has offered` | at `#/account`, `mySeat: 1` and seat `0` offering: `RIVAL_OFFERS` is on screen and its element has a `[role="status"]` ancestor |
| `says nothing on the room's own screen` | the same store at `/` renders no `RIVAL_OFFERS` — `ADR-0123` §3's *one fact never has two live surfaces* |
| `follows only an incoming offer` | **the same offering seat `0` in both halves**: `mySeat: 1` shows the sentence, `mySeat: 0` shows nothing. Two `render`s in one test with `unmount()` between them and `within(result.container)` for each half — `screen` is document-wide and would see both |
| `says nothing to a store that has been told nothing` | a fresh `createDuelStore()` at `#/account` renders nothing — `ADR-0118` §1 by construction |
| `says nothing to a client that holds no seat` | `DuelFinished` and `RematchOffered` with **no** `RoomJoined`: `mySeat` is `null`, `rematchStand` answers `theirs: false`, nothing renders |

## What would still pass if this were built wrong

Four of the five are negatives, and a negative passes trivially against a component that renders
nothing at all. `stands over a chosen screen…` is their positive control and uses the same fixture
builder, so *renders nothing, ever* fails one test instead of passing five.

`follows only an incoming offer` is the one no constant satisfies: both halves pass the same
`RematchOffered` seat `0`, so a component that hard-codes a seat, or reads `rematchOffers.length`
instead of asking whose seat it is, shows the panel twice and fails.

The two class-list gates are a pair: `fixed` must be **present** on a non-comment line and the seven
`-0` utilities must be **absent**, because a root that is fixed with an inert offset is exactly the
shape that reads right in the source and paints in the wrong place. Both were run against a
prototype and both discriminate: the `-0` grep finds nothing on the correct form and fires on a
`bottom-5` → `bottom-0` mutation.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.test.tsx` reports **5 passed (5)**, and the five
      tests are the five named above
- [ ] `src/lobby/Lobby.test.tsx` reports **113 passed (113)**, `src/App.test.tsx` **36 passed (36)**
      and `src/result/RematchControl.test.tsx` **12 passed (12)** — all measured on `develop` at
      `899d81f7`, and none of the three files is opened here
- [ ] `git diff --quiet develop -- web-client/src/lobby/Lobby.tsx` and the same for `App.tsx` both
      exit 0. A failure here means either this ticket touched them or `develop` moved them; rebase
      and re-check before changing anything
- [ ] `RematchNotice.tsx` has `role="status"` exactly once and `fixed` at least once on non-comment
      lines, and **0** non-comment occurrences of `useEffect`, `useLayoutEffect`, `setTimeout`,
      `setInterval`, `role="alert"`, `role="dialog"`, `alertdialog`, `aria-live`, `aria-hidden`,
      `autoFocus`, `tabIndex`, `.focus()`, `inert`, `createPortal`, `data-testid`, `localStorage`,
      `sessionStorage`, `window.location`, `props`
- [ ] `RematchNotice.tsx` carries none of `inset-0`, `inset-x-0`, `inset-y-0`, `top-0`, `bottom-0`,
      `left-0`, `right-0`
- [ ] **Shown red, then reverted — the counts are measured, not predicted.** Replacing
      `shown === "first"` with `false` fails **exactly 1** test, `says nothing on the room's own
      screen`. Replacing `!theirs` with `false` fails **exactly 3**: `follows only an incoming
      offer`, `says nothing to a store that has been told nothing`, and `says nothing to a client
      that holds no seat`. Both mutations were run, observed red with those names, and reverted
- [ ] `npm run check` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
