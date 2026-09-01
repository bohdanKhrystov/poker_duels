---
schema: 2
id: TASK-121302
title: The decision fits a 390 by 664 screen, because the client draws the card's phone
type: task
status: backlog
parent: STORY-1213
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, audit, bug, R2, manual-verify]
depends_on: [TASK-121305]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 npm run --silent build
  - sh -c 'grep -c "max-w-\[560px\]" web-client/src/lobby/Lobby.tsx web-client/src/table/DuelTable.tsx | grep -c ":1$" | grep -q "^1$"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

At **390 × 664** the acting player can see, without scrolling, the action they are being asked to
take and every number that decision depends on — their stack, the rival's stack, the pot and the
amount to call. `R2` (`ADR-0096` §2) is met at every beat and at both shapes, and it is met **by
conformance to `design/screens/duel-table.html`**, which by then draws the phone.

## This ticket was rewritten, not amended, and here is what was false

[`ADR-0103`](../../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md)
§5, merged, answering `DEC-106`. Two of the previous cut's sentences are now false and are gone:

- its *Files* table listed `design/screens/duel-table.html` as `read`, never edited;
- its cause section said *"No `DEC` is needed here — this is conformance to a merged card."*

**The merged card described no phone.** Measured headless at 390 × 664 it is `scrollHeight` **732**
against `clientHeight` **664**, with `.bar` ending at 715.7 — so a client transcribing it *perfectly*
still failed `R2` by 68 px. The first attempt reproduced the card's `.table { min-height: 100dvh }`
and `.center { flex: 1 }` faithfully and got no closer, and the review established why: **`min-height`
is a floor and `flex-grow` only distributes slack**, and at 885 px of content in a 664 px viewport
there is no slack to distribute. The card holds one rule that narrows with the column — `--bw`, read
by the board alone — while the hero's hole cards and the rival's mini hand are hardcoded at `--w:96px`
and `--w:40px`.

**So the design work goes first.** `TASK-121305` amends the card; this ticket follows it, and that
ordering is the whole of `ADR-0103`'s sequencing claim. Do not start this before that has merged: a
coder cannot conform to a shape the card does not draw, which is the wall the first attempt hit and
the wall `CLAUDE.md` rule 5 puts there on purpose.

## The defect

Round 1 of `/qa-cycle audit smoke` answered `R2` **`not met`**, at phone shape, at every beat that
presents a betting decision. Measured on the running client:

| beat | what was asked | measurement at 390 × 664 |
| --- | --- | --- |
| 2/3 | preflop, facing a call | `{"scrollHeight":885,"clientHeight":664}` — the Fold / Call 100 / Raise to 200 / All in 10,000 row and the sizing chips appear only after scrolling to the bottom |
| 4 | facing a raise | `getBoundingClientRect()` on Fold and All-in returned `bottom: 820.578` against `viewport: 664` — neither button is on screen |
| 5 | facing an all-in call | `{"scrollHeight":868,"clientHeight":664}` — the screen shows `You / YOUR TURN / 10,000` and no Fold or Call |
| 6 | flop, check-facing | `{"scrollHeight":866,"clientHeight":664}` |

At **720 × 900** the same four screens each measured `scrollHeight` equal to `clientHeight` (900/900)
with every control visible. **One bar, checked twice** (`ADR-0096` §2): *"a product that must scroll
to show the amount to call is `R2` `not met`, whether that happens at 390 px or at 720."* The laptop
pass is not partial credit and there is no relaxed phone bar to invent.

## The two halves of the repair

### 1. The height budget — necessary, and **not sufficient**

`ADR-0103` §5 says so in as many words, and it is recorded here so nobody re-litigates it in either
direction. **It is still owed:**

- the column still owes the card's `min-height: 100dvh`;
- the centre block still owes `flex: 1`;
- `Lobby.tsx:166`'s `<div className="mx-auto flex max-w-[560px] flex-col gap-5">` and
  `DuelTable.tsx`'s *second* `mx-auto flex max-w-[560px] flex-col gap-5` inside it must become **one**
  column. A `flex-1` on the centre block only absorbs slack if the block is a flex child of the
  element that carries the viewport height, so the nesting is part of the problem and not incidental
  to it.

Without those, the column's height is the sum of its content, nothing absorbs slack, and the 900/900
laptop pass is an arithmetic accident rather than a property. **But the sum has to come down first**;
the height budget is what keeps it down afterwards.

### 2. What gives, in order and exhaustively — `ADR-0103` §3

The client narrows what the amended card narrows, and nothing else:

1. **Whitespace.** The column's outer padding and the gaps between its three blocks tighten first —
   the only give that costs no information at all.
2. **The rival's face-down hand.** The `[--w:40px]` mini hand. It narrows furthest of anything on the
   table: its entire content is *she still holds cards*, and her name, her stack, her button and
   whose turn it is are on the plate directly above.
3. **The player's own hole cards.** The `[--w:96px]` block — 134 px, a fifth of a 664 px phone. They
   narrow, **with a floor: never smaller than a board card.** A table that draws the shared five
   larger than the private two inverts the game's own emphasis.
4. **The board.** It already narrows with the column, and it gives **last** of the three card groups,
   because it is what the decision is read off, at every beat, repeatedly.
5. **Nothing else gives.** Both seat plates' names and stacks, the pot and its line, the bet lines,
   the amount to call, the sizing row and the action buttons keep their **type size**, their
   **labels** and their **place**. `R3` is merged and is the reason: every amount at or above body
   size, not clipped, not truncated, and saying what it is.

**The action bar does not give; it may grow.** At 390 the sizing row wraps to two rows and the action
buttons wrap their labels to two lines — measured **59** and **61.5** at 390, against **32** and
**44.3** at 720. Wrapping is *fitting*, not *giving*, and it is allowed. Truncating a label, hiding a
chip or dropping a row is not.

### It is one table at two widths

Every element the table shows at 720 it shows at 390, **in the same order, with the same words**.
Nothing is removed, nothing is collapsed behind a disclosure, nothing appears only on a phone, nothing
moves to a different place in the column. What changes is how much room an element takes, and it
changes **continuously with the column's own width** — the `100cqi` idiom already on `DuelTable.tsx`'s
`[container-type:inline-size]`. Continuity is the property: 390 is not a threshold, so none of
`ADR-0096` §4's three second-surface tests is met — not a second layout (one markup), not a reduced
feature set (nothing dropped), not a separate application.

### If the give list runs out

`ADR-0103` §3: *"If the list runs out before the column fits, that is a decision to re-open, not a
scroll to accept. A ticket that reaches the end of this list and is still over budget stops and
registers a `DEC`; it does not take the next thing it sees."* A fit was probed to exist at **664/664**
with nothing removed, so running out would be surprising — but if it happens, register the `DEC`, mark
this ticket `blocked`, and do not take a fifth thing.

## Files

**Measure the set; do not copy it from this ticket's previous cut** — `files_touched: 2` there was a
pre-decision guess and is not evidence, and the two rows below are the *known starting points*
(`ADR-0103` §5's `TASK-120908` precedent), not a finished list. Before writing code, stub the
narrowing in `DuelTable.tsx` alone and run `.github/workflows/build.yml`'s pull-request gate set —
`npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run test`, `npm run build`, each on
its own so no failing prefix hides a later gate — and **let the run name the paths**. If it names a
third, say so in the PR and raise `files_touched` to the true count; if that count reaches four,
declare `atomic:` with the failing gate (`ADR-0068`, `ADR-0069`).

**The client's 885 exceeds the card's 732 by 153 px**, and that difference lives in components neither
the card nor a two-file budget accounts for. Whichever they are, they enter the set by measurement.

| File | Action |
| --- | --- |
| `web-client/src/table/DuelTable.tsx` | modify |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `design/screens/duel-table.html` | read |
| `docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md` | read |

`design/screens/duel-table.html` is **read here and edited by `TASK-121305`** — that is the whole
reason this ticket has a predecessor. Read it *as amended*: the two frames, the clamps that replaced
`--w:96px` and `--w:40px`, and the give order they express. `DuelTable.tsx`'s two hardcoded widths are
at lines 36 and 51 of the file as it stands on `develop`.

## Out of scope

- **Changing what is on the table.** No element is removed, no number is dropped, no control is hidden
  behind a disclosure to make the sum fit, and nothing is truncated. `R2` asks that the decision fit
  the screen, not that the screen say less. `ADR-0103`'s alternatives D and E are both refused, and
  both are the human's to reopen.
- **A breakpoint.** No `@media`, no class that applies only below some width, no `sm:`/`md:` variant
  used as a phone switch. Measurements narrow continuously with the column's own width.
- **A sticky or fixed action bar.** `ADR-0103`'s alternative B, rejected: it guarantees the buttons
  and guarantees nothing about the rival's stack or the pot, so the player still scrolls mid-decision,
  and it introduces an overlay the card does not draw.
- **Inventing a clamp the card does not draw.** After `TASK-121305` merges, this is conformance again.
  If the amended card leaves a rule ambiguous, ask it there — do not settle it here.
- **Editing the card.** It is `read` in the table above, and `TASK-121305` owns it.
- **Naming a made hand or adding any text.** `ADR-0095` §3, and `web-client/src/table/no-derivation.test.tsx`
  is a merged gate.
- **Landscape, tablets, or any third shape.** `ADR-0097` §5 records the human's *"we are ok to support
  only one orientation for mobile form factor"*. Two shapes, portrait only. No viewport below 390 ×
  664 is promised.
- **`DEC-103` and `DEC-104`.** Both open, both the product owner's, both observed at phone width, and
  neither is answered here. §3 makes wrapping legal in general; whether a *particular* wrap is
  acceptable is `DEC-103`'s.
- **`R1`'s runout and `R4`'s spacing.** `TASK-121301` and `TASK-121303`. `ADR-0103` §6: a paced runout
  changes nothing about the table's height, because every slot is reserved, so the two repairs do not
  collide.
- **The front door, the result screen and the rematch offer.** They pass `R2` at both shapes today
  (`bottom` 634.5, 653.75 and 653.75 against 664); this ticket touches the table.

## Tests

**None can be written, and the reason is a merged rule rather than a difficulty.** The failure is a
measured geometry in a real browser, and `ADR-0089` §2b — *"No pull request, `verify:` block or ticket
waits on a QA case"* — is one of the three conditions that license the QA harness to exist at all.
Putting a `scripts/qa/` measurement in a `verify:` line breaks that condition rather than bending it.
jsdom computes no layout, so `web-client`'s existing runner cannot see this defect either. The
`manual-verify` label carries it.

**What `verify:` gates, and what it cannot.**

- `npm run check` and `npm run build` gate that the diff typechecks, lints, is formatted and leaves
  the existing suite and the production build green. **They cannot fail on this defect.** That is
  stated rather than left to be inferred, because a gate presented as proving something it cannot
  prove is the failure mode this repository has already been bitten by.
- The one `grep` line gates the **nesting**, not the fit: exactly one of `Lobby.tsx` and
  `DuelTable.tsx` still carries a `max-w-[560px]`. It is red today — both carry one — and it cannot be
  satisfied by adding a class, only by removing the duplicate column. It says nothing about whether
  anything is on screen.

**Do not add a `grep` for `dvh` or for any other class name.** A coder can satisfy it by typing the
string while the buttons stay off screen, which is a gate that cannot fail.

## Acceptance criteria

Manual, at the two shapes `ADR-0096` §4 names, on both browsers, with `record` or `eval` as the
instrument (`manual-verify`):

- [ ] At **390 × 664**, at the preflop decision (beat 2/3): `document.documentElement.scrollHeight` is
      **less than or equal to** `clientHeight`, and Fold, Call, Raise and All-in are all visible in
      the unscrolled screenshot along with the sizing row
- [ ] At **390 × 664**, facing a raise (beat 4): `getBoundingClientRect().bottom` on the Fold button
      and on the All-in button are both **≤ 664**
- [ ] At **390 × 664**, facing an all-in call (beat 5) and at the flop check (beat 6): the same two
      checks hold
- [ ] At **390 × 664**, at all four beats, the viewer's stack, the rival's stack, the pot and the
      amount to call are readable in the unscrolled screenshot, at their full type size, unclipped and
      untruncated (`R3`)
- [ ] At **390 × 664**, the hero's hole card measures **wider than or equal to** a board card, and the
      rival's mini card measures **narrower than** the hero's — `ADR-0103` §3's floor and its
      ordering, by measurement rather than by reading the CSS
- [ ] At **720 × 900**, the same four beats still show everything, the column is still centred with
      gutters, and the hero's hole card still measures **96** px wide — the wide shape is unchanged
- [ ] At an intermediate width (560 is the obvious one), the same measurements fall **between** the
      390 and 720 values rather than jumping — the continuity `ADR-0103` §2 requires, and the check
      that no breakpoint was introduced
- [ ] Every element on the table at 720 is on the table at 390, in the same order, with the same
      words — nothing removed, nothing collapsed, nothing moved
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
