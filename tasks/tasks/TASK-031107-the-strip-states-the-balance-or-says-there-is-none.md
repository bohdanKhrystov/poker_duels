---
schema: 2
id: TASK-031107
title: The strip states the balance, or says there is no profile yet
type: task
status: ready
parent: STORY-0311
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, profile, ui]
depends_on: [TASK-031106]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +348 passed \(348\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'states the balance the server sent'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'says there is no profile yet, and raises no alarm'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'renders nothing at all when the read did not land'
  - cd web-client && npm run check
---

## Goal

The strip exists as a component: it prints the coin balance the server sent — `−1` included — says
*no profile yet* when there is none, and renders nothing at all when the read did not land.

## Files

| File | Action |
| --- | --- |
| `web-client/src/profile/ProfileStrip.tsx` | create |
| `web-client/src/profile/ProfileStrip.test.tsx` | create |
| `web-client/src/profile/profile-strip.ts` | read — `ProfileStripState` |
| `web-client/src/profile/profile-text.ts` | read — `coinBalanceText` |
| `web-client/src/result/DuelResult.tsx` | read — the token classes a panel is built from |

## Scope

- **Props only.** `ProfileStrip(props: { state: ProfileStripState }): ReactElement | null`. It calls
  no hook, holds no state, and fetches nothing; the read that fills it runs outside the tree
  (`TASK-031109`).
- `kind: "unavailable"` renders `null`. A lobby that announced every failed background read would
  spend the player's attention on something they cannot act on, and `STORY-0311` asks for an error
  alert on **no** path.
- `kind: "no-profile"` renders the sentence *No profile yet.* — the ordinary state of a first visit,
  not an error. No `role="alert"`, no word *error*, nothing red.
- `kind: "profile"` renders the balance through `coinBalanceText`, labelled *Duel coins*, in the
  same `<section aria-label="your profile">` wrapper as the other rendered state.
- **No `h1`–`h6` anywhere in this component.** `App.test.tsx` calls `screen.getByRole("heading")`,
  which throws when a second heading exists, and this strip is about to mount inside that tree
  (`TASK-031110`). A labelled `<section>` gives the region a name without adding a heading.
- Classes come from the theme only — `styles/color-literals.test.ts` fails `npm run check` on a
  colour literal outside the token layer. Compose the classes `DuelResult` already uses.

## Out of scope

- The duel rows and their empty state — `TASK-031108`, in this same file.
- Mounting it anywhere — `TASK-031110`.
- Designing it. `EPIC-06` authored no screen for this strip; inventing a visual language here would
  pre-empt that epic. Plain, token-composed, quiet.
- A coin mark or any graphic. `result/CoinMark.tsx` exists and is the result screen's.

## Tests

`web-client/src/profile/ProfileStrip.test.tsx`, describe block `"the profile strip"`, rendered with
testing-library exactly as `DuelResult.test.tsx` does.

| Test | Proves |
| --- | --- |
| `states the balance the server sent` | a balance of `7` puts `7` on screen; a balance of `-1` puts `−1` on screen, asserted as the escape `"−"`. **Two balances**, one negative — one value could not tell a rendered field from a constant, and `−1` is the answer `ADR-0014` says must survive |
| `says there is no profile yet, and raises no alarm` | `no-profile` puts the sentence on screen, and in the same test `queryAllByRole("alert")` is empty and the rendered text contains no *error* in any case |
| `renders nothing at all when the read did not land` | `unavailable` leaves `container.innerHTML` empty |

Three tests added. Three hundred and forty-five exist, so the suite reports **348**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 348 passed (348)` | three ran and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks, lints, is formatted, and holds the colour-literal rule |

**Name the edit that makes each assertion red** — run each, quote two in the PR, revert:

1. Print `Math.max(0, balance)` → `states the balance the server sent` fails on `-1` and passes on
   `7`.
2. Render the no-profile sentence inside `<p role="alert">` → `says there is no profile yet, and
   raises no alarm` fails on its second half while its first half still passes.
3. Render the wrapper `<section>` for `unavailable` too → `renders nothing at all when the read did
   not land` fails.

## Acceptance criteria

- [ ] `the profile strip > states the balance the server sent` passes, with two balances
- [ ] `the profile strip > says there is no profile yet, and raises no alarm` passes
- [ ] `the profile strip > renders nothing at all when the read did not land` passes
- [ ] `ProfileStrip.tsx` calls no React hook and imports no fetch
- [ ] `ProfileStrip.tsx` contains no `<h1>`…`<h6>`
- [ ] `npm run check` passes, so no colour literal entered the client
- [ ] No file outside `web-client/src/profile/` differs
- [ ] `npm run --silent test` reports `Tests  348 passed (348)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
