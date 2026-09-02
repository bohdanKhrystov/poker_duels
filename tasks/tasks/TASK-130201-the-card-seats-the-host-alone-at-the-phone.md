---
schema: 2
id: TASK-130201
title: The card seats the host alone at the phone, at rest
type: task
status: done
parent: STORY-1302
module: design
estimate: S
tier: sonnet
review: light
files_touched: 1
labels: [design, table, client]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "Host alone") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Waiting for your rival") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Invite link") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Copy the link") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Back to the lobby") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "The room stays open. That link still works for your rival, and it brings you back.") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "viewport phone") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"pot\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"bar\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"board\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"dealer\"") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "Pot&nbsp;") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 16) }' design/screens/duel-table.html
  - awk 'index($0, "10,000") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "Open seat") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "Link copied.") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "minutes") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - sh -c '! grep -qiE "@keyframes|animation:|transition:" design/screens/duel-table.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html` draws a third frame — **the host alone at the table, at rest, in
the 390 × 664 box** — so that
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §8's fit
claim stops being an argument and becomes a drawing, and the client tickets under this story have
a merged card to conform to.

## Why the frames go in this file

`ADR-0110` §8 owes five states. §8.2 settles one of them by pointing: *"**The moment the rival
arrives** — which is the live table `design/screens/duel-table.html` already draws; the card owes
the transition nothing more than showing that the waiting furniture is gone from it."* Three facts
follow, and together they pick the file:

1. **The arrival frame already exists, here.** Drawing the host-alone states in the same file makes
   the transition an *adjacency* a reader can see, instead of a claim about another file.
2. **This is the only card with a 390 × 664 box.** `.viewport.phone { width: 390px; height: 664px }`
   is declared here and nowhere else; `duel-table-states.html`'s frames are `flex: 1 1 400px`,
   which cannot prove a fit `ADR-0110` §8 puts on the card.
3. **A new file would copy about ninety lines of this one** — the token preamble, `.frames` /
   `.frame` / `.viewport` glue, the `.table` column with its `--wgap` ramp, and the whole seat
   plate — before drawing anything, and would owe a `_ds_manifest.json` entry on the cloud side
   (`design/README.md`) that this ticket cannot make.

The story grants the choice: *"The card ticket names the file it lands in — frames appended to an
existing screens card or a new card file."*

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/create-duel.html` | read |
| `design/tokens/tokens.css` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **One `:root` addition.** This file declares no `--pd-track-*` name today (measured). Add
  `--pd-track-code: 0.14em;` — `design/tokens/tokens.css:81`'s exact value, which
  `check-drift.sh`'s value clause compares against the sheet. Confirm it against the sheet before
  writing it; do not take this ticket's copy as canonical.
- **CSS, copied not invented.** The dashed empty seat is `create-duel.html`'s `.seat.seat-empty`
  and its `.seat.seat-empty .name` — copy them verbatim, including the reason its comment gives for
  `border-left-width: 1px; padding-left: 17px` (1 px border + 17 px padding preserves the plate's
  2 px + 16 px left metric, so a rival claiming the seat changes pixels and never layout). The code
  well is `create-duel.html`'s `.code`. Add a `.linkbox`, an `.ilabel` and a `.promise` rule for the
  invite's box, its label and §4's line. `.btn`, `.btn.fill` and `.btn.ghost` already exist in this
  file — reuse them.
- **One frame**, appended after the existing `Phone — 390 × 664` frame, headed exactly
  `<h2>Host alone — at rest</h2>`, wrapping `<div class="viewport phone">` around a `<div
  class="table">` that carries, top to bottom:
  - the rival's seat: one `.seat.seat-empty` plate whose `.name` reads `Waiting for your rival`
    — capital *W*, no full stop, and **no `.status`, no `.chips`, no `.dealer`**;
  - the invite, in `ADR-0110` §5's three parts: the bare code in the `.code` well (use
    `7Q4M9K2T`, the code `create-duel.html` already draws), the `Invite link` label, the
    read-only link box showing `…/?room=7Q4M9K2T`, and a `Copy the link` `.btn.fill`;
  - the host's seat: one `.seat` plate whose `.name` reads `You`, with **no `.chips`**;
  - `Back to the lobby` as a `.btn.ghost`, and §4's line in a `.promise` paragraph, verbatim:
    `The room stays open. That link still works for your rival, and it brings you back.`
- **Three lines of the head comment** say the file now draws the table *before* the duel too, and
  name `ADR-0110` §§1–5 as the reason.
- **The fit is the point.** The frame's content must sit inside the 664 px box without the
  `.viewport`'s scrollbar engaging. If all three invite parts genuinely will not fit, **stop and
  say so in the PR** — `ADR-0110`'s *Consequences* says that finding reopens its §5 and does not
  license dropping a part.

## Out of scope

- **The other three host-alone variants** — after `Link copied.`, after
  `Copy it from the box above.`, and with no clipboard API — and the arrival note. All four are
  `TASK-130202`. `verify:` pins `Link copied.` at zero here so this ticket cannot drift into them.
- **The two existing frames.** Six count gates pin `class="pot"`, `class="bar"`, `class="board"`,
  `class="dealer"`, `Pot&nbsp;` and `role="img"` exactly where they are today (2, 2, 2, 2, 2, 16 —
  measured 2026-09-02). Nothing about the laptop or the live phone frame moves.
- **Any game fact on the new frame.** `ADR-0110` §3 is `ADR-0002` applied: no stack, blind, card,
  pot, dealer button or action bar. `create-duel.html`'s retired frame drew `10,000` on the host's
  seat and *"the room lives 10 minutes unclaimed"* in a note — a gate pins `10,000` and `minutes`
  at zero, because copying that frame wholesale is the obvious wrong move.
- **`Open seat` and `send the link or read the code aloud`.** `create-duel.html`'s labels for this
  seat predate `ADR-0110` §2, which fixes the words at `Waiting for your rival`. Gated at zero.
- **Motion.** [`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
  §6 names only `STORY-1303` and `STORY-1306`; nothing in this state moves, so no still form is
  owed. A gate refuses `@keyframes`, `animation:` and `transition:` in the file, which today
  contains none of the three (measured).
- **Retiring `create-duel.html`'s frame.** `TASK-130207`.
- **Taste** — how the empty seat reads, where the invite sits, what weight each element carries.
  `ADR-0024` §3 and `ADR-0110` §8 put that with the human, given by looking at the rendered card,
  and it may trail the merge.

## Tests

**No test file, and none is possible.** A design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block: six say what must
now be on the card, eight say what must not have moved or appeared, and `check-drift.sh` says the
tokens, values and suit glyphs still hold.

Every count in the refusal gates was **measured in `design/screens/duel-table.html` on
2026-09-02**, not computed:

| Marker | Count today | Count after this ticket |
| --- | --- | --- |
| `class="frame"` | 2 | **3** |
| `viewport phone` | 1 | **2** |
| `Host alone` | 0 | **1** |
| `class="pot"` / `class="bar"` / `class="board"` / `class="dealer"` | 2 each | 2 each — unmoved |
| `Pot&nbsp;` | 2 | 2 — unmoved |
| `role="img"` | 16 | 16 — unmoved |
| `10,000`, `Open seat`, `Link copied.`, `minutes` | 0 each | 0 each |

## Acceptance criteria

- [ ] `Host alone` appears on exactly one line of `design/screens/duel-table.html`
- [ ] `Waiting for your rival`, `Invite link`, `Copy the link`, `Back to the lobby` and
      `The room stays open. That link still works for your rival, and it brings you back.` each
      appear exactly once
- [ ] `viewport phone` appears twice and `class="frame"` three times
- [ ] `class="pot"`, `class="bar"`, `class="board"`, `class="dealer"` and `Pot&nbsp;` still appear
      twice each, and `role="img"` still sixteen times
- [ ] `10,000`, `Open seat`, `Link copied.` and `minutes` appear zero times
- [ ] `@keyframes`, `animation:` and `transition:` appear zero times
- [ ] `./design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
