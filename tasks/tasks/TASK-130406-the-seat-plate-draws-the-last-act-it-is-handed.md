---
schema: 2
id: TASK-130406
title: The seat plate draws the last act it is handed, and speaks nothing
type: task
status: backlog
parent: STORY-1304
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, table]
depends_on: [TASK-130405]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/SeatPlate.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/theme.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/no-derivation.test.tsx 2>&1 | grep -qE '^ *Tests +7 passed \(7\)$'
  - awk 'index($0, "@utility last-act") { n++ } END { exit (n != 1) }' web-client/src/styles/app.css
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' web-client/src/styles/app.css
  - awk 'index($0, "animation") { n++ } END { exit (n != 4) }' web-client/src/styles/app.css
  - awk 'index($0, "transition") { n++ } END { exit (n != 0) }' web-client/src/styles/app.css
  - awk 'index($0, "aria-label") { n++ } END { exit (n != 1) }' web-client/src/table/SeatPlate.tsx
  - awk 'index($0, "title=") { n++ } END { exit (n != 0) }' web-client/src/table/SeatPlate.tsx
  - awk 'index($0, "lastActText") { n++ } END { exit (n < 2) }' web-client/src/table/SeatPlate.tsx
  - awk 'index($0, "formatChips") { n++ } END { exit (n < 2) }' web-client/src/table/SeatPlate.tsx
  - sh -c 'grep -q "last-act" web-client/src/table/SeatPlate.tsx && ! grep -q "data-testid" web-client/src/table/SeatPlate.tsx'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent build
  - sh -c 'grep -q "last-act" web-client/dist/assets/*.css'
  - sh -c 'grep -q "acting-mark" web-client/dist/assets/*.css'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`SeatPlate` draws the last-act mark the merged cards draw: handed an act event it prints that act's
verb and, on the four acts that carry one, the event's own total; handed nothing it prints nothing;
and either way it speaks no accessible name at all.

## Why the plate, and why it is its own diff

The plate is the one element **both** seats have — `ADR-0109` §1's *the mark is the same mark at
either seat* has nowhere else to live, and `TASK-130401` drew it there. This ticket is only about
what a plate does with the prop it is handed; **which** plate gets the prop is `TASK-130407`, and it
opens two files this ticket never does.

The three files here are the same three `TASK-130303` spent on the acting seat's mark, for the same
reason: the rule is minted in the sheet the card can be diffed against, the component applies it,
and the component's own test file moves with it.

## Two traps in these files, read them before writing

- **`web-client/src/styles/theme.test.ts` asserts that every line inside `app.css`'s
  `@theme static { … }` block is either a reset (`--x-*: initial;`) or a reference
  (`--x: var(--pd-y);`).** A `@utility` block written inside it reddens a merged test. `last-act`
  goes **outside** it, beside the merged `@utility acting-mark`.
- **`no-derivation.test.tsx` sweeps every printed and spoken number on the table against the
  view's own.** The mark's figure is the act event's `to`, which is **not** a `PlayerView` field, so
  a mark rendered into one of that file's fixtures would redden it. It cannot be: every one of that
  file's seven tests renders `<DuelTable view={…} />` and passes no act, so no plate in that file is
  ever handed one. The gate pins it at 7 to prove this ticket did not reach it. **Widening that
  guard to admit the act's total is not this story's work** — if a later ticket renders a mark
  there, it owes the guard a named carve-out the way `ADR-0107` §5's `potTotal` got one.

## What is already true, measured on `develop` 2026-09-02

- `SeatPlate.test.tsx` reports **8**; `no-derivation.test.tsx` **7**; `theme.test.ts` **1**.
- `SeatPlate.tsx` carries exactly **one** `aria-label` (the button) and **zero** `title`.
- `app.css` carries **1** `@keyframes`, **1** `@utility`, **4** lines mentioning `animation` and
  **0** mentioning `transition`. Those four `animation` lines are `acting-mark`'s; the last-act
  mark adds none, so the count does not move (`ADR-0109` §4, `ADR-0115`).
- The plate is `flex items-center gap-4` with `.who` at `min-w-0 flex-1`, so a mark placed in that
  row lets the name truncate and grows no box — which is how `ADR-0103` §1's fit survives.
- **Tailwind 4.3.3's `@utility` form does emit** into the production bundle (`STORY-1303`'s
  settlement), so no plain-rule fallback is needed; the bundle grep proves it anyway.

## Files

| File | Action |
| --- | --- |
| `web-client/src/styles/app.css` | modify |
| `web-client/src/table/SeatPlate.tsx` | modify |
| `web-client/src/table/SeatPlate.test.tsx` | modify |
| `design/components/seat-and-pot.html` | read |
| `web-client/src/table/action-text.ts` | read |

## Scope

- **`app.css`, outside the `@theme static` block:** one `@utility last-act { … }` whose
  declarations are the merged card's `.last-act` rule, transcribed and not re-derived, with every
  colour and length reaching for a `var(--pd-*)` the sheet declares. **No `animation`, no
  `transition`, no `@keyframes`** — `ADR-0109` §4 refuses a timer and a fade, `ADR-0115` refuses a
  fact that lives only in motion, and the gates pin all three counts where they already are.
- **`SeatPlate.tsx` gains one optional prop, `lastAct?: ActEvent | null`**, typed from
  `../store/duel-state`:
  - absent or `null` → the component renders **no element at all** for the mark;
  - otherwise one `<span className="last-act …">` carrying `lastActText(props.lastAct).verb` and,
    when that `ActionText`'s `amount` is not `null`, `formatChips(amount)` beside it in the mono
    tabular treatment every chip figure on this screen already wears.
- **The mark speaks nothing.** No `aria-label`, no `title`, no `role`, no `data-testid`. Its printed
  words are the whole of what it says. `SeatPlate.tsx` keeps exactly **one** `aria-label` — the
  button's — and **zero** `title`, and gates pin both. This is what keeps `null-view.test.tsx`'s
  `spoken()` closure closed when `TASK-130408` amends it.
- **Nothing else in the component moves.** Not `onTurn`, not `seatStatus`, not `acting-mark`, not
  `border-l-accent`/`border-l-transparent`, not the button, not the stack. The eight merged tests
  assert exactly those and none of their assertions moves.
- **Run `npm run format` before `format:check`** — `prettier-plugin-tailwindcss` sorts class lists
  and will reorder the string you write.
- **The 390 × 664 fit must be untouched, and say why in the PR.** The mark rides in the plate's
  existing flex row against `.who`'s `min-w-0 flex-1`, so no box grows taller — write that sentence
  in the PR, or, if the transcribed drawing does change a box, paste the measured `scrollHeight` and
  `clientHeight` at 390 × 664 instead. It cannot be a `verify:` gate: `ADR-0089` §2b forbids a pull
  request waiting on a browser.

## Out of scope

- **Which plate gets the mark.** `TASK-130407` reads `lastAct.seat` in `DuelTable.tsx`. This ticket
  never decides a seat, and its tests hand the prop in directly.
- **The screen and the store.** `Lobby.tsx` feeds the field in `TASK-130408`; `duel-state.ts` is
  merged and read-only here.
- **`no-derivation.test.tsx` and `null-view.test.tsx`.** Neither is opened; the first is pinned at
  7 to prove it, and the second is `TASK-130408`'s.
- **A second mark, or a stale-mark treatment.** `ADR-0109` §1 and `TASK-130401`'s *Out of scope*.
- **Any motion.** Not a fade-in, not a transition on the class, not a keyframe. Three gates.

## Tests

`SeatPlate.test.tsx` — four added to the eight it has (measured 2026-09-02), so the file reports
**12**. Each queries `container.querySelectorAll(".last-act")`; `ADR-0100` §5 holds, so no
`data-testid`, no test-only prop and no exported setter. The `plate()` helper gains one optional
key, `lastAct`, defaulting to absent.

| Test | Proves |
| --- | --- |
| `draws no mark when it is handed none` | with the prop absent, zero `.last-act`; with the prop explicitly `null`, zero. **Both spellings**, because `DuelTable` passes `undefined` to one plate and a value to the other, and a `!== null` check that forgot `undefined` would ship a mark reading `undefined` |
| `prints a fold and a check bare` | handed `PlayerFolded` the plate has exactly one `.last-act` whose text is `Fold`; handed `PlayerChecked`, `Check`. Neither contains a digit — asserted with `/\d/` over the mark's own `textContent`, so a stray total cannot hide |
| `prints the act's own total on a call, a bet, a raise and an all-in` | four acts and **four different totals** — `PlayerCalled` `to: 400` reads `Call 400`, `PlayerBet` `to: 950` reads `Bet 950`, `PlayerRaised` `to: 2300` reads `Raise to 2,300`, `PlayerAllIn` `to: 13400` reads `All in 13,400`. Four inputs, so neither a hard-coded figure nor a hard-coded verb survives, and the grouping is `formatChips`'s |
| `speaks the mark to nobody` | with a mark standing, the plate still has exactly one element carrying an `aria-label` — the button — and **no** element carrying a `title`. `ADR-0110` §3's contract in miniature: the mark reaches a player as print and by no other route |

The eight merged tests assert names, stacks, statuses, presence and the acting mark's classes —
measured, not one of them reads the mark's element or the plate's `textContent` as a whole, so no
assertion moves and none is weakened.

## Acceptance criteria

- [ ] `SeatPlate.test.tsx` reports `Tests  12 passed (12)`
- [ ] `a seat plate.draws no mark when it is handed none` passes
- [ ] `a seat plate.prints a fold and a check bare` passes
- [ ] `a seat plate.prints the act's own total on a call, a bet, a raise and an all-in` passes
- [ ] `a seat plate.speaks the mark to nobody` passes
- [ ] `src/styles/theme.test.ts` still reports `Tests  1 passed (1)` and
      `src/table/no-derivation.test.tsx` still reports `Tests  7 passed (7)`
- [ ] `app.css` contains exactly one `@utility last-act`, still exactly one `@keyframes`, still
      exactly 4 lines mentioning `animation` and still zero mentioning `transition`
- [ ] `SeatPlate.tsx` still carries exactly one `aria-label` and zero `title`, mentions
      `lastActText` and `formatChips`, and contains no `data-testid`
- [ ] `cd web-client && npm run build` exits 0 and the built CSS under `dist/assets/` contains both
      `last-act` and `acting-mark`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
