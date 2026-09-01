---
schema: 2
id: TASK-121303
title: The front door's three doors read as three doors, not as one word
type: task
status: done
parent: STORY-1213
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, audit, bug, R4, manual-verify]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

On the front door at phone width, *Your duels*, *Leaderboard* and *Account* render as three
separate labels. `R4` (`ADR-0096` §2) is met at beat 1, as it already is at the other seven.

## The defect

Round 1 of `/qa-cycle audit smoke` answered `R4` **`not met` at beat 1 only**. A 4× zoom crop of
the front door's nav row, cropped from the 390 × 664 screenshot, renders as:

```
Your duelsLeaderboardAccount
```

— the three items with no space and no separator of any kind between them.

**It is a rendered fact, not a text-dump artefact, and the observer ruled that out itself.** The
same screen's `Room code` field and `Join the duel` button also run together in a text dump —
`Room codeJoin the duel` — and the same screenshot shows them as two clearly separated controls,
because the `<form>` around them is block-level. The nav row has no such separation at any zoom.

## The cause, read from the markup

`web-client/src/lobby/Lobby.tsx` renders the front door as a bare `<section>` — the **only** branch
of that file whose `<section>` carries no class at all. Its six siblings (the `duels`,
`leaderboard`, `account`, `sign-in`, `verify` and `reset` branches) each carry
`className="mx-auto flex w-full max-w-[380px] flex-col items-center gap-4"`. Inside the bare one,
the three doors are three adjacent buttons:

```tsx
<button type="button" onClick={() => open("duels")}>{HISTORY_HEADING}</button>
<button type="button" onClick={() => open("leaderboard")}>{LADDER_HEADING}</button>
<button type="button" onClick={() => open("account")}>{ACCOUNT_HEADING}</button>
```

Three facts compose into the defect:

1. **JSX emits no text node between them.** Whitespace between elements is elided when it contains
   a newline, so the rendered DOM has the three buttons as immediate siblings with nothing between.
2. **A `<button>` is inline-level** by the user-agent default, and Tailwind's preflight does not
   change its `display`.
3. **The parent supplies no layout** — no flex, no grid, no gap, nothing.

Three inline boxes, no whitespace, no gap: they abut. This is `ADR-0098`'s defect one register
over — its settlement records that *"the card's markup has no text node between the two spans, so a
screen reader's own concatenation reads `PokerDuels`"* — decided in the product's favour on
2026-08-31.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `design/screens/create-duel.html` | read |
| `web-client/src/lobby/Lobby.test.tsx` | read |

`Lobby.test.tsx` is read and not edited: its only reference to these three controls is
`screen.getByRole("button", { name: ACCOUNT_HEADING })`, a name query that is indifferent to
layout. If a layout change reddens it, the change did something this ticket did not ask for.

## Scope

- The three doors render as three distinct labels at **390 × 664**: separated, or on their own
  lines, or both.
- Their words, their element and their placement are untouched: `ADR-0060` §§2–4 fix each of the
  three, `HISTORY_HEADING`, `LADDER_HEADING` and `ACCOUNT_HEADING` stay the single owned literals,
  and the doors stay `<button>` elements beneath the profile strip and outside it.
- Nothing else on the front door moves.

## Out of scope

- **Dressing the controls.** Whether a control no card draws should wear the client's control
  vocabulary is **`DEC-094`, open, the product owner's**, and it is about these exact three
  buttons. This ticket makes them **separate**; it must not make them **dressed**. A door can be
  bare and legible or dressed and illegible — the two are independent, and a repair that answers
  `DEC-094` by hand pre-empts a decision that has been costing rounds a finding since round 2.
  Add no border, no fill, no padding recipe and no `text-*`/`font-*` class to any of the three.
- **The rest of the front door's bare `<section>`.** Whether it should carry the same recipe its
  six siblings carry is not a criterion answer and is not filed as one; `STORY-1213` §*Owed to a
  later round* records it. Repair only the consequence `R4` names.
- **`create-duel.html`.** The card draws neither the doors nor a nav row and notes *"nothing else
  on the door — no lobby noise, no tables list"*. It is read so the repair does not contradict it,
  and it is not edited: this is an `R4` finding against a merged criterion, not a conformance
  finding against a card.
- **`R1`'s runout and `R2`'s overflow.** `TASK-121301` and `TASK-121302`.

## Tests

**No automated test can express this failure, and the reason is mechanical.** The defect is visual
adjacency, not text content: `textContent` concatenates the three labels *today and after any
correct repair*, so a `textContent` assertion fails on the fix as loudly as on the bug. jsdom
computes no layout, so `getComputedStyle` sees no gap either. And the browser that can see it may
not be a gate — `ADR-0089` §2b: *"No pull request, `verify:` block or ticket waits on a QA case."*

**A structural or class-name assertion is refused by name.** *"The three buttons share a wrapper"*
is satisfied by a bare `<div>` with no gap, and *"the wrapper's class list is non-empty"* is the
vacuous assertion `STORY-1211` already ruled out for this repository. Both would pass while the
words still abut.

**`npm run check` is in `verify:` and it cannot fail on this defect.** It gates that the diff
typechecks, lints and leaves the existing suite green — including the `ACCOUNT_HEADING` name query
above. It does not gate the bug, and it is not offered as if it did.

## Acceptance criteria

Manual, at phone shape, with the same instrument the finding used (`manual-verify`):

- [ ] At **390 × 664**, a screenshot of the front door, cropped and zoomed on the nav row, shows
      *Your duels*, *Leaderboard* and *Account* as three visually separate labels — the string
      `Your duelsLeaderboardAccount` does not render as one run at any zoom
- [ ] At **720 × 900**, the same three labels are still three, and nothing else on the front door
      has moved
- [ ] The three accessible names are unchanged: `getByRole("button", { name: … })` still finds each
      of `HISTORY_HEADING`, `LADDER_HEADING` and `ACCOUNT_HEADING`
- [ ] The diff adds **no** border, fill, padding recipe, `text-*` or `font-*` class to any of the
      three buttons — `DEC-094` is still open and unanswered after this merges
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
