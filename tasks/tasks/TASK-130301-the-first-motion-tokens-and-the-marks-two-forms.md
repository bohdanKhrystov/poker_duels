---
schema: 2
id: TASK-130301
title: The first motion tokens, the sheet's one still-block, and the seat's mark in both forms
type: task
status: done
parent: STORY-1303
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [design, table, client, minting]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "--pd-motion-turn-period") { n++ } END { exit (n < 1) }' design/tokens/tokens.css
  - awk 'index($0, "--pd-motion-turn-ease") { n++ } END { exit (n < 1) }' design/tokens/tokens.css
  - awk 'index($0, "@media (prefers-reduced-motion: reduce)") { n++ } END { exit (n != 1) }' design/tokens/tokens.css
  - awk 'index($0, "@media (prefers-reduced-motion: reduce)") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "@keyframes pd-acting-seat") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "--pd-motion-") { n++ } END { exit (n < 3) }' design/components/seat-and-pot.html
  - awk 'index($0, "acting — moving") { n++ } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk 'index($0, "acting — at rest") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "waiting — no mark") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "presence outranks the turn") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"seat on-turn") { n++ } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk 'index($0, "stilled") { n++ } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk 'index($0, "Your turn") { n++ } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk 'index($0, "Their turn") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - sh -c 'grep -q "class=\"seat on-turn" design/components/seat-and-pot.html && ! grep -qE "aria-|role=" design/components/seat-and-pot.html'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/tokens.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The product's motion vocabulary exists: `design/tokens/tokens.css` declares the first
`--pd-motion-*` values beside the one `@media (prefers-reduced-motion: reduce)` block that stills
them, and `design/components/seat-and-pot.html` draws the acting seat's mark in all three states
[`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
§6 names — *waiting*, *acting — moving*, *acting — at rest* — at **both** seats.

## Why minting and the card are one ticket

`ADR-0091` §3 splits authorship on what the ticket creates, and this creates **new visual
language**: the first motion value the product has ever had. `ADR-0115` §4 hands the sheet's block
to *"the minting ticket's"* judgment. Splitting the token from the drawing would mean choosing a
period before anything had been drawn at it, and the card ticket that then found the period wrong
could not fix it — the sheet would not be in its budget. One eye, one pane, one diff.

The human's visual verdict **may trail the merge** (`ADR-0024` §3, and `ADR-0091` §3's closing
sentence, which covers minting and composing alike), so an unattended run never stalls here. A
trailing rejection is a repair ticket against this card.

## What is already true, measured on `develop` 2026-09-02

- **Zero motion anywhere.** No `@keyframes`, `animation`, `transition` or motion utility in
  `design/`, `web-client/src` or the sheet; no `prefers-reduced-motion` in the repository.
- **The still form already ships.** `.seat.on-turn { border-left-color: var(--pd-accent) }` plus
  `.seat .status.turn` in micro caps, and `SeatPlate.tsx`'s `border-l-accent` — the plate's 2 px
  left border is transparent off-turn and always reserved, so gaining the turn shifts no pixel.
  `ADR-0115` §1 is therefore satisfied by what is merged; this ticket adds motion **beside** the
  still mark and never in place of it.
- `seat-and-pot.html` carries `class="seat on-turn` twice (rival `Their turn`, hero `Your turn`),
  `Your turn` once, `Their turn` once, `class="l"` six times, and **no** `aria-` or `role=`
  attribute at all.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | modify |
| `web-client/src/styles/tokens.css` | modify |
| `design/components/seat-and-pot.html` | modify |
| `docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md` | read |
| `design/README.md` | read |

## Scope

- **Mint in `design/tokens/tokens.css`**, in a new commented group after the focus/elevation
  block: `--pd-motion-turn-period` and `--pd-motion-turn-ease`, the cycle length and the easing of
  the acting seat's mark. Further `--pd-motion-*` names may be minted if the drawing below needs
  geometry; none may be minted that this card does not use. The comment says what they are for and
  cites `ADR-0115` §4.
- **One `@media (prefers-reduced-motion: reduce)` block in the sheet**, and only ever one — the
  product's whole answer to the signal. **Its exact CSS is this ticket's to choose** (`ADR-0115`
  §4 says so by name): zeroing the `--pd-motion-*` values leaves a mid-flight frame frozen where
  it stands, while `animation: none` on a universal selector snaps every surface to its base
  frame. The PR states which was chosen and what artefact the other would have left. Useful fact,
  measured: `web-client/src` declares **no** CSS transition of any kind today, so a universal rule
  costs nothing that exists.
- **Copy the sheet byte-for-byte to `web-client/src/styles/tokens.css`.** `tokens.test.ts`
  compares buffers, not strings alone; a missed newline fails the client job.
- **Draw the mark on `design/components/seat-and-pot.html`**, whose lede already calls the turn
  marker *"an accent edge that is always reserved"*:
  - inline the new `--pd-motion-*` values in the card's own `:root` — `check-drift.sh`'s value
    clause compares each against the sheet, so the two can never drift;
  - one `@keyframes` block named exactly **`pd-acting-seat`**;
  - the motion added to `.seat.on-turn`, so **both** existing on-turn rows carry it — the rival
    with `Their turn`, the hero with `Your turn`. That is the story's *both seats*;
  - one new row: the hero's on-turn plate again, carrying a **`stilled`** modifier whose rule
    switches the motion off exactly as the sheet's block does. This is `ADR-0115` §2's *same
    surface at rest* — nothing hidden, nothing added, no second design — drawn as a named state so
    the human sees both forms in one pane without changing their system settings;
  - the card carries its **own** single `@media (prefers-reduced-motion: reduce)` block too, so it
    genuinely stills on a machine that asks and the pane verdict covers the real behaviour.
- **Four captions**, in the file's existing `.l` idiom, lower case, each appearing exactly the
  number of times the gates pin. The four substrings are load-bearing and must be written
  verbatim, em dash included:
  - `acting — moving` on **both** on-turn rows;
  - `acting — at rest` on the stilled row;
  - `waiting — no mark` on the existing idle row;
  - `presence outranks the turn` on the existing `.away` row — a seat that is on turn *and* away is
    drawn by that row unchanged, because `ADR-0046` §1 makes it look exactly like a seat that is
    away and not on turn. The caption is the whole of what this state owes.
- **The mark mints no string and speaks nothing.** No `aria-*`, no `role`, no new word. `Your turn`
  and `Their turn` already ship in `seat-status.ts` and are the mark's still voice; the story
  forbids inventing a sentence, and `ADR-0046`'s register is where a new one would have to be born.

## Out of scope

- **The two screen cards.** `design/screens/duel-table.html` and `duel-table-states.html` copy this
  rule in `TASK-130302`. Do not open them here.
- **Any client code.** `app.css` and `SeatPlate.tsx` are `TASK-130303`. Only the vendored sheet
  moves, and only because `tokens.test.ts` forces it to.
- **Choosing the drawing for anyone else.** Pulsing, a running circle, a travelling edge — the card
  may draw one and the human's eye decides (`ADR-0024` §3); the verdict may trail the merge.
- **Motion tokens for anything but this mark.** `STORY-1306`'s chip flight mints its own; a general
  `--pd-motion-fast`/`slow` ladder invented here would be a vocabulary nobody has drawn.
- **The countdown** (`STORY-1307`–`STORY-1309`), **the last-act mark** (`STORY-1304`) and **chips**
  (`STORY-1306`).
- **An in-product motion setting.** `ADR-0115` §2 refuses one by name.

## Tests

**No test file, and none is possible** for the card: a design card is HTML nobody imports, and
`ADR-0089` §2b forbids a browser measurement being a gate. The gates are the `verify:` block —
eleven say what must now be on the card and in the sheet, four refuse what must not have appeared,
and `check-drift.sh` says the tokens, values and suit glyphs still hold.

The one **executable** test this ticket owes is the merged one it must not break:
`web-client/src/styles/tokens.test.ts`, which asserts the vendored sheet is byte-identical to the
canonical. It is pinned at `Tests 1 passed (1)`, and the whole `npm run check` runs beside it
because `theme.test.ts` reads the sheet's declared names too.

| Marker | Count today | Count after |
| --- | --- | --- |
| `--pd-motion-*` in the sheet | 0 | ≥ 2 named |
| `@media (prefers-reduced-motion: reduce)` in the sheet | 0 | exactly 1 |
| `@keyframes pd-acting-seat` on the card | 0 | exactly 1 |
| `class="seat on-turn` on the card | 2 | **3** |
| `Your turn` / `Their turn` on the card | 1 / 1 | **2** / 1 |
| `aria-` and `role=` on the card | 0 / 0 | 0 / 0 |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `design/tokens/tokens.css` declares `--pd-motion-turn-period` and `--pd-motion-turn-ease`
- [ ] `design/tokens/tokens.css` contains exactly one `@media (prefers-reduced-motion: reduce)`
- [ ] `design/components/seat-and-pot.html` contains exactly one `@media (prefers-reduced-motion:
      reduce)` and exactly one `@keyframes pd-acting-seat`
- [ ] The card mentions `--pd-motion-` on at least three lines
- [ ] `acting — moving` appears exactly twice, `acting — at rest` exactly once, `waiting — no mark`
      exactly once and `presence outranks the turn` exactly once
- [ ] `class="seat on-turn` appears exactly three times and `stilled` at least twice
- [ ] `Your turn` appears exactly twice and `Their turn` exactly once
- [ ] The card contains no `aria-` and no `role=`
- [ ] `src/styles/tokens.test.ts` reports `Tests  1 passed (1)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
