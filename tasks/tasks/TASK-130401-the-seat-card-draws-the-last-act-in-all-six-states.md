---
schema: 2
id: TASK-130401
title: The seat card draws the last act, in all six of its states, at both seats
type: task
status: done
parent: STORY-1304
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"last-act") { n++ } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"seat") { n++ } END { exit (n != 12) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"name\">ImKate<") { n++ } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"name\">You<") { n++ } END { exit (n != 5) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — fold, bare") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — check, bare") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — call, the total the server sent") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — bet, the total the server sent") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — raise to, the total the server sent") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — all in, the total the server sent") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Raise to") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "All in") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Fold") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Check") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Call") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Bet") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "1,700") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "950") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "2,300") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "4,150") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "animation") { n++ } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk 'index($0, "transition") { n++ } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk 'index($0, "aria-") { n++ } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk 'index($0, "role=") { n++ } END { exit (n != 0) }' design/components/seat-and-pot.html
  - sh -c 'grep -q "class=\"last-act" design/components/seat-and-pot.html && ! grep -q "data-" design/components/seat-and-pot.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/components/seat-and-pot.html` draws the last-act mark in **all six** states `ADR-0109` §5
names — *Fold* and *Check* bare, *Call*, *Bet*, *Raise to* and *All in* each with a figure — three
of them at the rival's seat and three at the hero's, so the human can judge the drawing, the
placement and the crowding in one pane before any client code exists.

## Why the card is first, and what stays the human's

`EPIC-13` *Design first* and `ADR-0091` §2: the card merges before the ticket that implements it is
startable. `ADR-0109` §5 fixes only **how many states the card owes — six** — and hands the rest to
the human's eye (`ADR-0024` §3): **icon versus word versus both, where inside the plate the mark
sits, and its colour are this card's to offer and the human's to accept.** The human's visual
verdict may trail the merge (`ADR-0091` §3), so an unattended run never stalls here; a trailing
rejection is a repair ticket against this card.

What the card may **not** move, because `ADR-0109` §2 settled it: the six words are
`action-text.ts`'s — *Fold, Check, Call, Bet, Raise to, All in* — and the figure rides with exactly
four of them, as a **total**. No increment (*Raise by 140*) anywhere: `ADR-0109` §Alternative 7
refuses it by name because the server never sent that number.

## The three constraints that are not taste

- **The mark rides in the plate's existing row and the plate's height does not change.**
  `ADR-0103` §1 froze the phone fit and listed exhaustively what may give; this card adds a standing
  element to a screen that fit by inches. The plate is already
  `display: flex; align-items: center` with `.who` at `flex: 1; min-width: 0` — a mark placed in
  that row lets the name truncate (the give the card already draws) and grows no box. **If the
  drawing cannot work without a taller plate, that re-opens `ADR-0103`'s give list** (`ADR-0109` §5)
  and is a `DEC`, not a quiet spend.
- **Nothing about the mark moves.** No `@keyframes`, no `animation`, no `transition` on `.last-act`
  — `ADR-0109` §4 refuses a timer and a fade, and `ADR-0115` refuses a fact that lives only in
  motion. The gates pin `@keyframes` at exactly 1 (the acting seat's, merged) and `animation` at
  exactly 3 lines (measured on `develop` 2026-09-02: `.seat.on-turn`'s two-line declaration and
  `.stilled`'s `animation: none`), so a mark that pulses or fades reddens the ticket.
- **Mint no new token.** Every `--pd-*` the drawing uses must already be declared in
  `design/tokens/tokens.css` — `check-drift.sh` clause 1 fails otherwise, and a new token drags the
  vendored `web-client/src/styles/tokens.css` in with it (`tokens.test.ts` compares buffers), which
  is a second file this ticket does not have. The declared set is ample: `--pd-text-muted`,
  `--pd-hairline`, `--pd-radius-pill`, `--pd-fs-micro`, `--pd-font-mono`, `--pd-space-2/3/4`,
  `--pd-surface-raised`, `--pd-accent-subtle`.

## What is already true, measured on `develop` 2026-09-02

- `class="seat` appears **6** times, `class="name">ImKate<` **3**, `class="name">You<` **2**;
  `@keyframes` **1**, `animation` **3** lines, `transition` **0**, `aria-` **0**, `role=` **0**.
- **None of the six verbs appears anywhere on this card** — `Fold`, `Check`, `Call`, `Bet`,
  `Raise to` and `All in` are all at 0 — which is why the gates can pin each at exactly 1. Write the
  captions in **lower case** (`last act — raise to, …`), or the verb gates count two.
- **The four figures are all at 0 today** — `1,700`, `950`, `2,300`, `4,150` — deliberately chosen
  that way. `400` is *not* free: it is inside `--pd-motion-turn-period: 2400ms`, which is why the
  call's figure is not 400.
- The `.l` caption idiom is `<span class="l">lower case — em dash</span>`, one per row.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | modify |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |
| `design/README.md` | read |

## Scope

- **One CSS rule, `.last-act`**, in the seat block, beside `.dealer`. Its declarations are the
  card's to choose within the three constraints above; it is the rule
  `web-client/src/styles/app.css` copies verbatim in `TASK-130406`, so write it as a
  self-contained block.
- **Six new rows in the `The seat, in its states` stack**, each a `.seat` plate carrying one
  `.last-act`, in this order, with these exact printed strings and these exact captions:

  | Row | Seat | The mark prints | Caption (verbatim, em dash included) |
  | --- | --- | --- | --- |
  | 1 | `ImKate` | `Fold` | `last act — fold, bare` |
  | 2 | `You` | `Check` | `last act — check, bare` |
  | 3 | `ImKate` | `Call` and `1,700` | `last act — call, the total the server sent` |
  | 4 | `You` | `Bet` and `950` | `last act — bet, the total the server sent` |
  | 5 | `ImKate` | `Raise to` and `2,300` | `last act — raise to, the total the server sent` |
  | 6 | `You` | `All in` and `4,150` | `last act — all in, the total the server sent` |

  Three at each seat is the story's *both seats*, and it is the whole of what the second seat owes
  — `ADR-0109` §5: *"the mark is the same mark at either seat, so the second seat multiplies
  nothing."*
- **The figures are mono and tabular**, as every chip figure on this card already is
  (`.seat .chips`), and grouped in threes the way `formatChips` groups them — `2,300`, not `2300`.
- **The lede gains one sentence** saying what the mark is: the most recent act of the hand, at the
  seat that made it, in that seat's own button's words. One mark, never one per seat.
- **The mark speaks nothing.** No `aria-*`, no `role`, no `title`, no `data-*` attribute. The words
  it prints are the whole of what it says, and gates pin `aria-` and `role=` at 0.

## Out of scope

- **The two screen cards.** `design/screens/duel-table.html` and `duel-table-states.html` carry the
  mark in place in `TASK-130402`. Do not open them here.
- **A seventh, *stale* state.** Whether a mark left standing from an earlier street looks different
  is offered by `ADR-0109` §5 and **is not drawn here**: it would need the client to remember a
  street boundary, which no ticket in this story adds. If the human wants it, it is a new card and a
  new store field.
- **Any client code.** `app.css` and `SeatPlate.tsx` are `TASK-130406`.
- **The acting seat's mark** (merged, `TASK-130301`) and the pot strip. Neither moves; the gates on
  `@keyframes`, `animation` and `class="seat` prove it.
- **`ADR-0046` §4's server-action line.** It coexists with this mark (`ADR-0109` §6) and is drawn
  nowhere on this card today.

## Tests

**No test file, and none is possible**: a design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block — eleven say what must
now be on the card, six refuse what must not have appeared, and `check-drift.sh` says the tokens,
values, suit glyphs and lockup still hold.

| Marker | Count today | Count after |
| --- | --- | --- |
| `class="last-act` | 0 | **6** |
| `class="seat` | 6 | **12** |
| `class="name">ImKate<` / `class="name">You<` | 3 / 2 | **6** / **5** |
| `Fold` / `Check` / `Call` / `Bet` / `Raise to` / `All in` | 0 each | **1** each |
| `1,700` / `950` / `2,300` / `4,150` | 0 each | **1** each |
| `@keyframes` / `animation` / `transition` | 1 / 3 / 0 | **1 / 3 / 0** |
| `aria-` / `role=` / `data-` | 0 / 0 / 0 | **0 / 0 / 0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `class="last-act` appears exactly 6 times and `class="seat` exactly 12
- [ ] `class="name">ImKate<` appears exactly 6 times and `class="name">You<` exactly 5 — three new
      rows at each seat
- [ ] Each of the six captions in the table above appears exactly once, verbatim
- [ ] `Fold`, `Check`, `Call`, `Bet`, `Raise to` and `All in` each appear exactly once, and so do
      `1,700`, `950`, `2,300` and `4,150`
- [ ] `@keyframes` is still exactly 1, `animation` still exactly 3 lines, `transition` still 0 — the
      mark does not move (`ADR-0109` §4, `ADR-0115`)
- [ ] The card still contains no `aria-`, no `role=` and no `data-`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
