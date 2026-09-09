---
schema: 2
id: TASK-141507
title: Not now hides the surface, sends nothing, and the next offer brings it back
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, rematch, notice]
depends_on: [TASK-141506]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +16 passed \(16\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"{NOT_NOW}") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"type=\"button\"") { n++ } END { exit (n != 2) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"setDismissed(false)") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"OfferRematch") { n++ } END { exit (n != 1) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"useEffect") || index($0,"useLayoutEffect") || index($0,"setTimeout") || index($0,"setInterval") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - awk '$0 ~ /^[[:space:]]*(\/\/|\/?\*)/ { next } index($0,"localStorage") || index($0,"sessionStorage") || index($0,"eslint-disable") { n++ } END { exit (n != 0) }' web-client/src/result/RematchNotice.tsx
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/App.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The panel carries `Not now`. Pressing it takes the surface off the screen and sends nothing; a
reload brings it back; and the **next** offer is not swallowed by the last dismissal.
`ADR-0123` §7 and `ADR-0138` §3, whole.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.tsx` | modify |
| `web-client/src/result/RematchNotice.test.tsx` | modify |

Read [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §7,
[`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§3 and 4, [`ADR-0044`](../../docs/adr/ADR-0044-a-rematch-is-one-intent-and-one-room-fact.md) §6,
and `design/screens/rematch-panel.html`, whose three panel frames each carry the control.
**Nothing outside the table above is changed.**

## Scope

- **One `useState` boolean, and its lifetime is the mount.**

  ```tsx
  const [dismissed, setDismissed] = useState(false);
  // ADR-0123 §7: a dismissal lasts as long as the offer it was about, and the
  // offer ends when the duel that answers it begins (Snapshot) or when the room
  // hands down another result (DuelFinished). Cleared here, in the render that
  // first sees the offer gone, so the next offer is not swallowed by the last
  // dismissal.
  if (dismissed && !theirs) setDismissed(false);
  ```

  Because the component is mounted above `Lobby`'s cascade (`ADR-0138` §1) this state survives every
  screen change **including a walk back to `/` and out again**, and dies with the document — which
  is `ADR-0123` §7's *"a reload brings the panel back"*, with **no storage key, no context, no
  provider, no store field and no module-scope variable**. A gate holds `localStorage` and
  `sessionStorage` at **0**.
- **No `eslint-disable`, and a gate refuses one.** Probed at `899d81f7`:
  `react-hooks/set-state-in-render` is at `error` in `eslint-plugin-react-hooks@^7`'s
  `recommended-latest` and fires on an unguarded `setDismissed(x)` in a render body, while the
  guarded form above passes with exit 0. `Lobby.tsx:170-181` carries a documented suppression at `:179` for a
  **different** rule (`react-hooks/set-state-in-effect`, for a `setState` in an effect body) and is
  not the precedent for this one. Keep the explanatory comment; add no suppression.
- **The dismiss sends nothing.** `onClick` calls `setDismissed(true)` and returns — no `send`, no
  fetch, no storage write, no callback out of the component. `ADR-0044` §6's *silence is a decline*
  is preserved by there being no code that could break it, and a gate holds `OfferRematch` at
  **1** — the accept's, still the only frame this story sends.
- **`Not now` stands in all three states**, including after `UNKNOWN_ROOM`: it asserts nothing about
  the game, and `ADR-0123` §5 says it is the only way off a panel that never leaves by itself. So it
  sits **outside** the branch that swaps the accept for the dealing sentence or for `ROOM_GONE`. A
  gate holds `{NOT_NOW}` at **1** and `type="button"` at **2**.
- **The gate line grows to three terms**: `if (shown === "first" || !theirs || dismissed) return
  null;` — after the clear, never before it, or a dismissal would survive its own offer by one
  render.

## Out of scope

- **Any storage, any key, any persistence.** `ADR-0123` §7 mints none and this ticket adds none.
- **A decline frame.** `Room` records no decline and no retraction, and the offerer is told nothing
  at all, before or after (`ADR-0044` §6, `ADR-0123` §7).
- **The dismissal across a screen change and through `/`.** That is `TASK-141509`'s, because it
  needs `App` — with this component alone there is nothing on screen to prove the address actually
  moved, and a test that asserts *still hidden* after a move that did not happen passes for the
  wrong reason.
- **`App.tsx`, `Lobby.tsx`, `RematchControl.tsx`.** None is opened.

## Tests

`RematchNotice.test.tsx` grows from **12** to **16**.

| Test | Proves |
| --- | --- |
| `Not now takes the surface off the screen` | with the offer standing at `#/account`, pressing `NOT_NOW` leaves `queryByRole("status")` null and `RIVAL_OFFERS` absent |
| `Not now sends nothing` | the `send` spy's call count is **0** before the press and **0** after. The count, not a `not.toHaveBeenCalledWith` — the only assertion that catches a decline frame being added later |
| `a fresh render of the same tree shows the panel again` | dismiss, `unmount()`, then render the **same store** again: the panel is back. This is `ADR-0123` §7's reload, and it is the whole proof that nothing was stored |
| `the next offer is not swallowed by the last dismissal` | dismiss, then apply a `Snapshot`, a `DuelFinished` and a fresh `RematchOffered` from seat `0` **each in its own `act()`**: the panel is back |

The count is **16**, not 17: `Not now` also has to be on screen in the dealing span and after
`UNKNOWN_ROOM`, and those two assertions are added to `the press opens the dealing span in place of
the button` and `states that the room is gone in place of the accept`, which already render exactly
those states. Adding two tests that re-render the same states to look at one more node would be
duplication; the two existing tests grow by one line each and neither loses an assertion.

**The separate-`act()` rule is measured, not stylistic.** With the three `store.apply` calls inside
one `act()`, React batches them, the render in which `theirs` is false never happens, the clear
never fires, and `the next offer is not swallowed…` fails against a **correct** component. With one
`act()` each it passes. In production the frames arrive on separate socket messages in separate
tasks, so React does not batch them.

## What would still pass if this were built wrong

`Not now takes the surface off the screen` passes against a component that returns `null`
unconditionally after any press — which is why `the next offer is not swallowed by the last
dismissal` exists and why it is the one test that fails on the version of this component a careful
reader would write first, the one without the render-phase clear. Every other test here dismisses
at most once inside its own render and stays green without it.

`Not now sends nothing` asserts a **count of 0 both sides** rather than *was not called with a
decline*: a component that sent `OfferRematch` on dismiss — the single most plausible mistake here,
since both buttons sit in the same handler region — passes the second form and fails the first.

`a fresh render of the same tree shows the panel again` deliberately does not inspect any `Storage`
object. Node's own `localStorage` shadows jsdom's under vitest and reads `undefined`, so an
assertion over its keys is a test of the runtime rather than of this component; the source gate that
holds `localStorage` and `sessionStorage` at **0** is what covers *nothing is stored*, and the fresh
render is what covers *the dismissal does not survive*.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.test.tsx` reports **16 passed (16)**
- [ ] `src/result/RematchControl.test.tsx` reports **12 passed (12)** — measured on `develop` at
      `899d81f7`, and that file is not opened
- [ ] `RematchNotice.tsx` has, on non-comment lines, `{NOT_NOW}` **1**, `type="button"` **2**,
      `setDismissed(false)` **1**, `OfferRematch` **1**, and **0** of `useEffect`,
      `useLayoutEffect`, `setTimeout`, `setInterval`, `localStorage`, `sessionStorage`,
      `eslint-disable`
- [ ] `git diff --quiet develop` is clean for `Lobby.tsx`, `App.tsx` and `RematchControl.tsx`
- [ ] **Shown red, then reverted:** deleting `if (dismissed && !theirs) setDismissed(false);` makes
      `the next offer is not swallowed by the last dismissal` fail and leaves the other fifteen
      green. The mutation was run, observed red by that name, and reverted
- [ ] `npm run check` exits 0, with no `eslint-disable` added anywhere in this diff

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
