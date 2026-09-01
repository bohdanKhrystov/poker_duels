---
schema: 2
id: TASK-121402
title: The duel table's column fits the phone it is nested in
type: task
status: done
parent: STORY-1214
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [bug, audit, manual-verify, R2]
depends_on: []
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - awk '/<main/ { if ($0 ~ /class[Nn]ame=.*[" ]p[trblxy]?-/) bad=1 } END { exit bad }' web-client/src/App.tsx
  - awk '/className=.*min-h-\[100dvh\]/ { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.tsx
  - awk '/className=.*max-w-\[380px\]/ { n++ } END { exit (n != 6) }' web-client/src/lobby/Lobby.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

At 390 × 664 the duel table measures `scrollHeight ≤ clientHeight` on the running client, which is
what [`ADR-0103`](../../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md)
requires and what the shipped client does not do.

## This is a new ticket, not a reopening

[`TASK-121302`](TASK-121302-the-decision-fits-a-390-by-664-screen.md) merged on 2026-09-01 in
`#1272` and is `done`. Its `manual-verify` obligation was **never discharged** — recorded in
[`STORY-1213`](../stories/STORY-1213-round-1-audit-three-criteria-unmet-and-a-table-that-names-the-wrong-winner.md)
— and measuring it afterwards shows the criterion unmet. `EPIC-12` §Termination rule 1 keeps that
round's set frozen, so this is a **new** ticket in the ordinary backlog and `TASK-121302` is not
touched.

**The gap is in the client, not the card.** `TASK-121305` amended
`design/screens/duel-table.html` and it was measured to fit at **664 / 664**. The client does not.
Nothing here asks the card to change, and the card is not even read.

## The defect, measured

On the running client at `e1a37a80`, at the preflop decision beat, on both browsers:

| viewport | `scrollHeight` | `clientHeight` | over by |
| --- | --- | --- | --- |
| 390 × 664 | 712 | 664 | **48** |
| 780 × 792 | 840 | 792 | **48** |
| 720 × 900 | 948 | 900 | **48** |

`ADR-0103` states the bar as *"the whole column fits 390 × 664 at every beat (`scrollHeight ≤
clientHeight`, no scroll to act)"*. It does not.

**The overflow is 48 px at every shape**, which is what identifies the cause rather than a symptom:

- `web-client/src/App.tsx:5` wraps every screen in `<main className="min-h-screen bg-bg p-6 …">`.
  `p-6` is 24 px top and 24 px bottom — **48 px**.
- `web-client/src/lobby/Lobby.tsx:181` gives the duel-table column `min-h-[100dvh]`, the floor
  `ADR-0103` §5 requires and `TASK-121302` correctly landed.

The column asks for the whole viewport height *inside* an ancestor that adds 48 px around it, so
the document is always `100dvh + 48px`. It is not a content problem: the column's own content
already fits. `TASK-121302` conformed to `ADR-0103` §5 faithfully; nothing in that ticket, and
nothing in the card, accounts for an ancestor the card does not have.

**The cure was measured, not reasoned.** Setting `main`'s padding to `0` in the live page and
re-reading the document took `scrollHeight` from **712 to 664** against `clientHeight` **664** — an
exact fit, on both browsers. The style was reverted immediately; no file was changed.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.tsx` | modify |
| `web-client/src/lobby/Lobby.tsx` | modify |

**Both rows are load-bearing, and the second is why this is two files rather than one.** `p-6` on
`main` is the *only* outer padding the front door, the account screen, the history screen, the
sign-in screen and the result screen have — `Lobby.tsx` renders each of them inside
`<section className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4">`, which
carries none of its own. At 390 wide their gutters would fall from 24 px to 5 px. So the padding
does not disappear; it **moves** to the six sections that were relying on it, and the duel-table
column, which already carries `p-[var(--wgap)]`, stops paying for it twice.

**Measured, not copied.** The change was stubbed in `App.tsx` and the pull-request gate set from
`.github/workflows/build.yml` was run in full and separately — `npm run typecheck`, `npm run lint`,
`npm run format:check`, `npm run test` (117 files, **985 tests**), `npm run build` — and every one
exited 0, naming no third path. No test asserts `min-h-screen`, `p-6`, `100dvh`, `max-w-[560px]` or
`max-w-[380px]`, so no merged test moves and none is in the budget. **The gate set cannot see
layout**, though, so its silence bounds the *compile-and-test* blast radius and nothing more; the
second row above comes from reading which screens have no padding of their own. If a third file is
genuinely required, say so in the PR and raise `files_touched` (`ADR-0068`, `ADR-0069`).

## Scope

- `main` in `App.tsx` carries **no padding utility**.
- The six `max-w-[380px]` sections in `Lobby.tsx` carry their own outer padding, so every screen
  other than the duel table looks exactly as it does today at both shapes.
- The duel-table column keeps `min-h-[100dvh]`, its `max-w-[560px]` cap, its container-query
  context and its `p-[var(--wgap)]`. `ADR-0103` §5 still owes all of them.

## Out of scope

- **Reaching the fit by hardcoding the ancestor's padding into the column** — a
  `min-h-[calc(100dvh-3rem)]` or similar. It fits by arithmetic that silently re-breaks the day the
  ancestor's padding changes, and `ADR-0103` §5 asks the column for `min-height: 100dvh`, not for a
  number derived from something above it.
- **Removing or weakening `min-h-[100dvh]`.** Guarded in `verify:`.
- **Anything on `ADR-0103` §3's give list.** Whitespace is §3.1, the first give, and this is
  whitespace: duplicated outer padding. The rival's mini hand, the hole cards and the board are
  **not** touched, no clamp changes, and no card measurement moves. If 48 px turns out not to be
  enough, stop — do not take the next thing on the list — and say so.
- **A breakpoint, a media query, a `sm:`/`md:` phone switch, a sticky action bar.** `ADR-0103`
  §6 and its rejected alternative B, unchanged.
- **`design/screens/duel-table.html`.** Not read, not edited. It fits at 664 / 664 already.
- **`DEC-103` and `DEC-104`.** Still open, still the product owner's, still not answered here.
- **Any presence behaviour.** `TASK-121403`.

## Tests

**No test can be written, and the reason is a merged rule.**
[`ADR-0089`](../../docs/adr/ADR-0089-a-browser-drives-this-client-for-a-qa-round-never-for-a-gate.md)
§2b — *"No pull request, `verify:` block or ticket waits on a QA case"* — is one of the three
standing conditions that license the QA harness to exist at all, so a `scripts/qa/` measurement may
not appear in `verify:`. jsdom computes no layout, so the client runner cannot see this defect
either. **This is the mistake `TASK-121302` made and this ticket must not repeat: the measurement
below is an acceptance criterion with a named runner and a named number, not a gate.**

| Gate | Proves | Today |
| --- | --- | --- |
| `npm run check`, `npm run build` | the diff typechecks, lints, is formatted, and leaves 985 tests and the production build green | green — **they cannot fail on this defect** |
| `main` carries no padding utility | the 48 px is gone from the ancestor, and cannot be re-added as `px-6`, `pt-6` or `p-[24px]` | **red** |
| `className` carrying `min-h-[100dvh]` appears exactly once in `Lobby.tsx` | `ADR-0103` §5's floor was not deleted to make the sum fit | green — a regression guard |
| `className` carrying `max-w-[380px]` appears exactly 6 times in `Lobby.tsx` | no other screen's section was removed while moving padding into it | green — a regression guard |

The padding gate was run against `develop` and **exited 1**, and against a stub with `p-6` removed
and **exited 0**. It matches the utility *shape*, not the literal `p-6`, so it cannot be satisfied
by renaming the class. The two count gates are scoped to lines containing `className=` on purpose:
`Lobby.tsx`'s own comment quotes `min-h-[100dvh]`, and an unscoped count matches the prose and
inverts.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on the running stack, with
`node scripts/qa/drive.mjs <port> size <w> <h>` and `… eval`. **The numbers they must see** are
below. Paste each reading into the PR body as text — a criterion nobody records is a criterion
nobody discharged, which is exactly how `TASK-121302` closed unmet.

- [ ] At **390 × 664**, at the preflop decision beat, `document.documentElement.scrollHeight` is
      **≤** `clientHeight` — the number to beat is today's **712 against 664**, and the expected
      reading is **664 / 664**
- [ ] At **390 × 664**, `getBoundingClientRect().bottom` on the `Fold` button and on the `All in`
      button are both **≤ 664**
- [ ] At **720 × 900** the same beat reads `scrollHeight ≤ clientHeight` — today **948 / 900**
- [ ] On the **front door** at 390 × 664, the visible gutter either side of the `max-w-[380px]`
      section is **≥ 16 px** — it is 24 px today and must not collapse to 5 px
- [ ] On the **account** screen at 390 × 664, the same gutter check holds
- [ ] The hero's hole card still measures **≥** a board card, and the rival's mini card **<** the
      hero's — `ADR-0103` §3's floor and ordering, unchanged by this ticket and re-checked because
      the column's height budget moved
- [ ] Every element on the table at 720 is on the table at 390, in the same order, same words
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
