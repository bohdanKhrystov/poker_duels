---
schema: 2
id: TASK-121305
title: The duel-table card draws the phone too, and the cards give before the numbers
type: task
status: backlog
parent: STORY-1213
module: design
estimate: S
tier: sonnet
review: light
files_touched: 1
labels: [qa, audit, design, R2, manual-verify]
depends_on: []
verify:
  - ./design/check-drift.sh
  - sh -c '! grep -q -- "--w:96px" design/screens/duel-table.html'
  - sh -c '! grep -q -- "--w:40px" design/screens/duel-table.html'
  - sh -c 'grep -c "aria-label=\"ace of hearts\"" design/screens/duel-table.html | grep -q "^2$"'
  - sh -c 'grep -c "aria-label=\"ImKate.s hidden hand\"" design/screens/duel-table.html | grep -q "^2$"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html` draws the duel table at **both** shapes the product is judged at —
720 × 900 exactly as it does today, and 390 × 664 fitting the box — so `TASK-121302` can go back to
asking *does the client match the card*, which is the only question `ADR-0091` gives a coder the
tools to answer.

## Why this exists

[`ADR-0103`](../../docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md)
(merged, answers `DEC-106`) §4. **The merged card is in arrears**: it draws one shape while the
product claims two, and at the shape the product claims it is 68 px too tall. Measured headless with
`Emulation.setDeviceMetricsOverride`, on `develop` at `27d6ba76`, and confirmed again for this
ticket:

| | at 390 × 664 | at 720 × 900 |
| --- | --- | --- |
| `document.documentElement` | `scrollHeight` **732** / `clientHeight` **664** | **900 / 900** |
| `.bar` bottom | **715.7** — 51.7 px below the fold | on screen |
| `.hole` (the hero's two cards) | 134.4 tall, at the hardcoded 96 px | 134.4 |
| `.sizing` / `.actions` | 59 / 61.5 — both wrapped | 32 / 44.3 |

So a client transcribing this card **perfectly** still fails `R2` by 68 px. The card holds exactly
one rule that narrows with the column — `--bw: clamp(48px, calc((100cqi - 64px) / 5), 72px)`, read by
the five board cards alone — while the hero's hole cards and the rival's mini hand are hardcoded at
`--w:96px` and `--w:40px`. The missing rule is design, `ADR-0091` §1 makes the card the carrier of
design into implementation, and a coder inventing a clamp is inventing design.

**This is the composing half of `ADR-0091` §3's split, not the minting half**, and that was checked
rather than assumed: a fit was probed for this ticket using only names `design/tokens/tokens.css`
already declares — `--pd-space-3`, `--pd-space-4`, `--pd-space-5`, `--pd-space-6` — plus raw pixels
inside `clamp()`, which is the board's own idiom. **No new token, so no interactive minting session
and no `tokens/tokens.css` edit.** If the treatment you reach for needs a size step the sheet does not
have, stop: that is minting, it is worked with the human, and it is not this ticket.

## Files

The set was **measured, not remembered** (`ADR-0069`). The amendment was stubbed against a copy of
the whole `design/` tree and `.github/workflows/tickets.yml`'s gate — `./design/check-drift.sh` —
was run on it: green, *"tokens resolve, values match the sheet, suits carry U+FE0E, 6 graphics pairs
and 5 inlined symbols mirror truly, and the lockup anatomy holds across 1 copy (503 distinct mentions
across 19 cards)"*. No second path was named. `design/README.md` indexes directories, not cards, so it
gains no row.

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | read |
| `design/tokens/tokens.css` | read |
| `docs/adr/ADR-0103-the-table-fits-the-phone-and-the-cards-give-before-the-numbers.md` | read |

`duel-table-states.html` is read for two things and edited for neither: the `.frames` / `.frame` form
this card is to adopt, and the sentence that makes one phone frame answer every beat — *"Every slot —
the opponent's hidden hand, the bet/muck line, the pot row, the bar — exists in every state, so
nothing appears, disappears, or moves; only text and card faces change."* `tokens.css` is read to
confirm a name exists before it is used, never edited.

## Scope

Three changes to one file, all of them `ADR-0103` §4's.

- **The two hardcoded widths narrow with the column**, the way `--bw` already does: `--w:96px` on the
  hero's hole cards and `--w:40px` on the rival's mini hand become measurements that are a
  **continuous function of the column's own width**, in the `100cqi` idiom already in the file.
- **The give order is `ADR-0103` §3's, in order and exhaustively**: whitespace first — the column's
  outer padding and the gaps between its three blocks; then the rival's face-down hand, which narrows
  furthest of anything on the table; then the player's own hole cards, **never smaller than a board
  card**; then the board, last of the three card groups. **Nothing else gives**: both seat plates'
  names and stacks, the pot and its line, the bet lines, the amount to call, the sizing row and the
  action buttons keep their type size, their labels and their place (`R3`). The action bar may
  **grow** — wrapping is fitting, not giving.
- **The file gains a second frame at 390 × 664**, beside the existing one, in `duel-table-states.html`'s
  `.frames` / `.frame` form. The two frames are the **same markup**; the only difference between them
  is the width and height of the box they are drawn in, and **the phone box is 664 tall as well as
  390 wide**. A `min-height: 100dvh` column inside a tall page proves nothing about a short viewport.

**A fit exists and was probed for this ticket**, so the target below is known to be reachable: with
the mini hand and the hole cards narrowing on the board's idiom and the vertical rhythm stepping down
with the column, the phone box came to `scrollHeight` **664** against `clientHeight` **664** with the
`.bar` ending at **652**, nothing removed, while the laptop box stayed at every number the merged
card has today. That is an existence proof, not a prescription — the numbers are yours, under the
human's eye (`ADR-0024` §3).

## Out of scope

- **A second file.** `design/screens/duel-table-phone.html` is refused by name (`ADR-0103` §4,
  alternative C): a second file is free to diverge, and the line drawn is **markup identity, not file
  count**.
- **A breakpoint.** No `@media`, no rule that fires only below some width. `ADR-0103` §2's property is
  that measurements narrow **continuously**, so 390 is not a threshold and a player dragging a window
  narrower watches one table get tighter rather than a second table replace the first.
- **Removing, collapsing or hiding anything at 390.** Nothing appears only on a phone and nothing
  disappears on one. Dropping the rival's card backs, the board's undealt slots or the pot's meta line
  is `ADR-0103`'s alternative D, refused: that is a reduced feature set, `ADR-0096` §4's second
  second-surface test, and the human's call.
- **Shrinking a number.** `ADR-0103`'s alternative E, refused outright by `R3`.
- **Minting a token, or editing `design/tokens/tokens.css`** — see above.
- **Any other card, and the client.** `duel-table-states.html` is read, never edited; its own frames
  keep their hardcoded widths and this ticket does not chase them. The client is `TASK-121302`, which
  depends on this.
- **Landscape.** `ADR-0097` §5: portrait only.
- **`DEC-103`.** Whether `Raise to 3,650` may break between the words and the number is open and this
  ticket does not answer it. §3 makes wrapping legal in general; a *particular* wrap is still that
  decision's.

## If the give list runs out

`ADR-0103` §3, in as many words: *"If the list runs out before the column fits, that is a decision to
re-open, not a scroll to accept. A ticket that reaches the end of this list and is still over budget
stops and registers a `DEC`; it does not take the next thing it sees."* The list is exhaustive
precisely so that running out is a visible event. A fit was probed to exist at 664/664 with nothing
removed, so running out would be surprising — but if it happens, register the `DEC`, mark this
ticket `blocked`, and do not take a fifth thing.

## Tests

**None, and the reason is a merged rule rather than a difficulty.** The subject is a rendered geometry
in a real browser; `ADR-0089` §2b — *"No pull request, `verify:` block or ticket waits on a QA
case"* — is one of the three conditions that license the browser harness at all, so the measurement
below is an acceptance criterion a human or agent runs, never a `verify:` line. The `manual-verify`
label carries it.

**What `verify:` does and does not gate**, stated so nothing is offered as proving what it cannot:

- `./design/check-drift.sh` gates that every `--pd-*` name still resolves, that every inlined value
  still equals the sheet's, and that duplicating the markup did not lose a `U+FE0E` after a suit
  glyph. It cannot see a pixel.
- The two `! grep` lines gate that `--w:96px` and `--w:40px` are **gone** from the file. Both are red
  today and cannot be satisfied by typing something else beside them.
- The two `grep -c … 2` lines gate that the hero's hand and the rival's hand each appear **exactly
  twice** — the markup really is duplicated into two frames, and there are two frames rather than
  three. Both are red today (each string appears once).

None of the five gates the fit. That is what the criteria below are for.

## Acceptance criteria

Measured in a headless browser on the amended file — `file://…/design/screens/duel-table.html`, a
viewport wide enough to lay both frames side by side (1400 × 1000 was used for the probe), and
`getBoundingClientRect` / `clientHeight` / `scrollHeight` read off the two boxes:

- [ ] The element boxing the **phone** frame measures `clientWidth` **390** and `clientHeight`
      **664**, and its `scrollHeight` is **less than or equal to 664** — the number to beat, from
      **732** today
- [ ] Inside the phone box, the action bar's bottom edge is **on screen**: its `getBoundingClientRect().bottom`
      is at or above the box's own bottom, and every one of `R2`'s five things — the viewer's stack,
      the rival's stack, the pot, the amount to call, and the action buttons — plus the sizing row
      is visible without scrolling
- [ ] Inside the phone box, the hero's hole card is **wider than or equal to** a board card, and the
      rival's mini card is **narrower than** the hero's — `ADR-0103` §3's floor and its ordering, both
      checked by measurement rather than by reading the CSS
- [ ] The element boxing the **laptop** frame measures 720 × 900 with `scrollHeight` ≤ 900, and its
      parts still measure what the merged card measures today at 720 × 900: `.opp` **150.5**,
      `.oppcards` **56**, `.board` **100.8**, `.hole` **134.4**, `.bar` **110.3**, `.sizing` **32**,
      `.actions` **44.3**, a hero card **96** wide, a mini card **40**, a board card **72**
- [ ] The two frames' table markup is **identical**: extracting each frame's table block and diffing
      them produces no output. Only the boxes differ
- [ ] No `@media` query and no width-conditional rule was added — `grep -c "@media" design/screens/duel-table.html`
      still returns **1**, the `forced-colors` rule that is already there
- [ ] Every element the table shows at 720 it shows at 390, in the same order, with the same words —
      checked by the diff above, which is what makes *one layout, two widths* verifiable rather than
      claimed
- [ ] The human's visual verdict on the rendered card (`ADR-0024` §3). It may trail the merge
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
