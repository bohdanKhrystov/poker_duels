---
schema: 2
id: TASK-121302
title: The decision fits a 390 by 664 screen, because the column is budgeted to the viewport
type: task
status: ready
parent: STORY-1213
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, audit, bug, R2, manual-verify]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

At **390 × 664** the acting player can see, without scrolling, the action they are being asked to
take and every number that decision depends on — their stack, the rival's stack, the pot and the
amount to call. `R2` (`ADR-0096` §2) is met at every beat and at both shapes.

## The defect

Round 1 of `/qa-cycle audit smoke` answered `R2` **`not met`**, at phone shape, at every beat that
presents a betting decision. Measured on the running client:

| beat | what was asked | measurement at 390 × 664 |
| --- | --- | --- |
| 2/3 | preflop, facing a call | `{"scrollHeight":885,"clientHeight":664}` — the Fold / Call 100 / Raise to 200 / All in 10,000 row and the sizing chips appear only after scrolling to the bottom |
| 4 | facing a raise | `getBoundingClientRect()` on Fold and All-in returned `bottom: 820.578` against `viewport: 664` — neither button is on screen |
| 5 | facing an all-in call | `{"scrollHeight":868,"clientHeight":664}` — the screen shows `You / YOUR TURN / 10,000` and no Fold or Call |
| 6 | flop, check-facing | `{"scrollHeight":866,"clientHeight":664}` |

At **720 × 900** the same four screens each measured `scrollHeight` equal to `clientHeight`
(900/900) with every control visible. Beats 1, 7 and 8 hide nothing at either shape — the front
door sits in the top half of the viewport, and the Victory / Defeat / rematch-offer screens' last
buttons measured `bottom` of 634.5, 653.75 and 653.75 against 664.

**One bar, checked twice, and the phone answer decides.** `ADR-0096` §2: *"A criterion is `met`
only if it is met at every shape it was answered at"*, and *"a product that must scroll to show the
amount to call is `R2` `not met`, whether that happens at 390 px or at 720."* The laptop pass is not
partial credit and there is no relaxed phone bar to invent.

## The cause, read from source rather than from the browser

**The client took the card's width and left its height behind.**

- `design/screens/duel-table.html` draws the table as
  `.table { max-width: 560px; min-height: 100vh; min-height: 100dvh; margin: 0 auto; padding: var(--pd-space-5); display: flex; flex-direction: column; gap: var(--pd-space-5); }`
  and gives the pot-and-board block the leftover with `.center { flex: 1; … justify-content: center }`.
  The card's whole mechanism for fitting a short screen is those two rules: the column is exactly
  the viewport, and the middle absorbs whatever is left.
- `web-client/src/lobby/Lobby.tsx:166` renders the same column as
  `<div className="mx-auto flex max-w-[560px] flex-col gap-5">` — the width, the centring and the
  gap, and **not** the height.
- `web-client/src/table/DuelTable.tsx` renders the pot-and-board block as
  `<div className="flex flex-col items-center gap-4">` — **no `flex-1`**.
- `grep -n "dvh\|100vh\|sticky\|fixed bottom\|overflow"` over `Lobby.tsx`, `App.tsx` and every
  `web-client/src/table/*.tsx` returns **nothing**.

So the column has no viewport-height budget anywhere and nothing in it absorbs slack. Its height is
the sum of its content, and the action bar is whatever falls off the end — which is exactly the
885-against-664 that was measured, and exactly why the same markup passes at 720 × 900, where the
sum happens to fit.

**A third fact the repair has to account for: there are two nested columns where the card has one.**
`Lobby.tsx`'s `<div className="mx-auto flex max-w-[560px] flex-col gap-5">` holds `DuelTable`,
`PresenceNotice` and `ActionBar`; `DuelTable.tsx` opens a *second*
`mx-auto flex max-w-[560px] flex-col gap-5` inside it. A `flex-1` on the centre block only absorbs
slack if the block is a flex child of the element that carries the viewport height, so the nesting
is part of the problem and not incidental to it.

**That the bar is meetable at both shapes is a merged fact, not an aspiration** (`ADR-0096` §2):
the card already draws a layout that is the full width of a phone and a centred column on a laptop.
**No `DEC` is needed here** — this is conformance to a merged card, not a new decision.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/table/DuelTable.tsx` | modify |
| `design/screens/duel-table.html` | read |

## Scope

- The duel-table column is budgeted to the viewport the way the card budgets it: the column is at
  least the viewport's height, and the pot-and-board block absorbs the slack so the action bar
  stays on screen.
- At **390 × 664**, at each of beats 2/3, 4, 5 and 6, every one of `R2`'s enumerated numbers — the
  viewer's stack, the rival's stack, the pot, and the amount to call — plus the action controls and
  the sizing row are visible **without scrolling**.
- At **720 × 900** nothing regresses: the same four screens still show everything, and the column
  still centres with gutters.

## Out of scope

- **Changing what is on the table.** No element is removed, no number is dropped, and no control is
  hidden behind a disclosure to make the sum fit. `R2` asks that the decision fit the screen, not
  that the screen say less.
- **Naming a made hand or adding any text.** `ADR-0095` §3, and
  `web-client/src/table/no-derivation.test.tsx` is a merged gate.
- **Landscape, tablets, or any third shape.** `ADR-0097` §5 records the human's *"we are ok to
  support only one orientation for mobile form factor"*. Two shapes, portrait only.
- **The card.** `design/screens/duel-table.html` is right and is read, never edited. It is in the
  *Files* table as `read` for exactly that reason.
- **`R1`'s runout and `R4`'s spacing.** `TASK-121301` and `TASK-121303`.
- **The front door, the result screen and the rematch offer.** They pass `R2` at both shapes today;
  this ticket touches the table.

## Tests

**None can be written, and the reason is a merged rule rather than a difficulty.** The failure is a
measured geometry in a real browser, and `ADR-0089` §2b — *"No dependency. **No gate.** No coverage
claim"*, specifically *"No pull request, `verify:` block or ticket waits on a QA case"* — is one of
the three conditions that license the QA harness to exist at all. Putting a `scripts/qa/`
measurement in a `verify:` line breaks that condition rather than bending it. jsdom computes no
layout, so `web-client`'s existing test runner cannot see this defect either.

**`npm run check` is in `verify:` and it cannot fail on this defect.** It is there so a repair
cannot merge a client that does not typecheck, lint or pass its existing suite — it gates the diff,
not the bug. That is stated rather than left to be inferred, because a gate presented as proving
something it cannot prove is the failure mode this repository has already been bitten by.

**Do not add a `grep` for `dvh` or for any class name.** A coder can satisfy it by typing the
string while the buttons stay off screen, which is a gate that cannot fail.

## Acceptance criteria

Manual, at the two shapes `ADR-0096` §4 names, on both browsers, with `record` or `eval` as the
instrument (`manual-verify`):

- [ ] At **390 × 664**, at the preflop decision (beat 2/3): `document.documentElement.scrollHeight`
      is **less than or equal to** `clientHeight`, and Fold, Call, Raise and All-in are all visible
      in the unscrolled screenshot along with the sizing row
- [ ] At **390 × 664**, facing a raise (beat 4): `getBoundingClientRect().bottom` on the Fold button
      and on the All-in button are both **≤ 664**
- [ ] At **390 × 664**, facing an all-in call (beat 5) and at the flop check (beat 6): the same two
      checks hold
- [ ] At **390 × 664**, at all four beats, the viewer's stack, the rival's stack, the pot and the
      amount to call are readable in the unscrolled screenshot
- [ ] At **720 × 900**, the same four beats still show everything and the column is still centred
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
