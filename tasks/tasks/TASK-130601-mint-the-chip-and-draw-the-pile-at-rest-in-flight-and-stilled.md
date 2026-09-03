---
schema: 2
id: TASK-130601
title: Mint the chip, its flight values, and the pile at rest, in flight and stilled
type: task
status: ready
parent: STORY-1306
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [design, table, minting]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/--pd-chip-face/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/--pd-chip-edge/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/--pd-chip-size/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/--pd-motion-chip-flight/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/--pd-motion-chip-ease/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/--pd-motion-chip-travel/, "&") } END { exit (n < 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/tokens/tokens.css
  - awk '{ n += gsub(/@keyframes pd-chip-flight/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 18) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 17) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>13,400</, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/>150</, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/--pd-chip-face/, "&") } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/--pd-chip-size/, "&") } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/--pd-motion-chip-flight/, "&") } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/--pd-motion-chip-travel/, "&") } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/chips at rest — the numeral is the fact/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/the same pile at a hundredth of the stack — a pile is never a count/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/chips in flight — the numeral has already moved/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/stilled — the flight is skipped and the chips are where they land/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/a busted seat draws no chips/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/the pot at rest/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/the pot receiving/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/tokens.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The chip exists as design vocabulary: `design/tokens/tokens.css` declares the first `--pd-chip-*`
values and the flight's `--pd-motion-chip-*` values, and `design/components/seat-and-pot.html`
draws the pile in every state it has — **at rest**, **in flight**, **stilled**, and **absent** —
beside the numerals that state the amounts.

## Why minting and this card are one ticket

`ADR-0091` §3 splits authorship on what the ticket creates, and this creates **new visual
language**: the product has no chip. `design/graphics/` holds the coin, the suits and the
wordmark; `web-client/src/table/chips.ts` is digit grouping. So this is worked **interactively
with the human** — *"taste does not survive a verify block"* — exactly as `TASK-130301` minted the
first motion tokens beside the drawing they animate. Splitting the token from the drawing would
mean choosing a chip size and a flight duration before anything had been drawn at them, and the
card ticket that then found them wrong could not fix them: the sheet would not be in its budget.

The human's visual verdict **may trail the merge** (`ADR-0024` §3, `ADR-0091` §3's closing
sentence), so an unattended run never stalls here. A trailing rejection is a repair ticket.

## The rule this card exists to make visible — read it before drawing

[`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
§1: **no fact lives only in motion.** §6 says what that means here by name — *"the flight is
garnish… every amount is stated still — the pot figure, the stack numerals, the bet lines — so no
count, destination or timing is knowable only from choreography."* Three consequences the drawing
must obey, and they are not negotiable by taste:

1. **The numeral stays.** Every pile is drawn beside the figure it belongs to, never instead of
   it. A pile is the only thing on this card that could state an amount, and it must not.
2. **A pile is never a count.** The same pile is drawn at `13,400` and at `150`. A pile whose
   size read off the amount would invent a denomination — *what a chip is worth* — which is a
   fact no server ever sent and a product question nobody has asked (`ADR-0002`, `docs/vision.md`
   — *no money*). Rows 1 and 2 below are that statement, drawn.
3. **The flight ends at rest, and nothing is ever animated out.** `@keyframes pd-chip-flight`
   runs **from** an offset **to** the resting position; the `to` frame is the frame the surface
   already had. That is what makes the stilled row identical to the at-rest row with no second
   design (`ADR-0115` §2), and it is why an animation stopped at any instant — by the sheet's
   reduced-motion block, by a screenshot, or by a re-render — leaves the chips where they belong.
   An exit animation is the one shape whose stilled form is wrong, and there is none here.

## What is already true, measured on `develop` 2026-09-03

- `design/tokens/tokens.css` declares `--pd-motion-turn-period` and `--pd-motion-turn-ease` and
  **exactly one** `@media (prefers-reduced-motion: reduce)` block, which stills **both**
  `animation` and `transition` product-wide with `!important` on `*`. **The chip flight is
  therefore stilled by what is already merged** — this ticket adds no second block anywhere.
- `seat-and-pot.html` carries: `@keyframes` 1, `prefers-reduced-motion` 1, `class="chips"` 12,
  `class="pot"` 1, `class="pile` 0, `class="disc"` 0, `transition:` 0, `13,400` 0, `>150<` 0.
- **`class="chip"` on the screen cards is the sizing button, not a chip.** It reads 10 on
  `duel-table.html` and 15 on `duel-table-states.html`. That is why the drawn chip is `.disc`
  inside `.pile` and never `.chip` — a gate over `class="chip` would conflate the two.

## Files

| File | Action |
| --- | --- |
| `design/tokens/tokens.css` | modify |
| `web-client/src/styles/tokens.css` | modify |
| `design/components/seat-and-pot.html` | modify |
| `docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md` | read |
| `design/README.md` | read |

## Scope

- **Mint in `design/tokens/tokens.css`**, in a new commented `Chips` group and in the existing
  `Motion` group respectively:
  - `--pd-chip-face`, `--pd-chip-edge`, `--pd-chip-size` — the disc's face, its rim and its
    diameter. The face composes tokens the sheet already declares wherever it can, as
    `--pd-coin-face` does; a genuinely new hue is minted only if the drawing needs one, and its
    comment says what it derives from.
  - `--pd-motion-chip-flight`, `--pd-motion-chip-ease`, `--pd-motion-chip-travel` — the flight's
    duration, its easing, and how far a pile travels before it lands.
  - Further `--pd-chip-*` names may be minted if the drawing below needs them; none may be minted
    that this card does not use. Every comment cites `ADR-0115` §§1, 4.
  - **No second reduced-motion block.** The sheet's one block already stills this. A gate pins it
    at exactly one.
- **Copy the sheet byte-for-byte to `web-client/src/styles/tokens.css`.** `tokens.test.ts`
  compares buffers, not strings alone; a missed newline fails the client job.
- **Draw on `design/components/seat-and-pot.html`:**
  - inline the six new values in the card's own `:root` — `check-drift.sh`'s value clause compares
    each against the sheet, so the two can never drift;
  - one `@keyframes` block named exactly **`pd-chip-flight`**, whose `to` frame is the resting
    state and whose `from` frame carries `var(--pd-motion-chip-travel)`;
  - `.pile` (the container) and `.disc` (one drawn chip), plus `.pile.flying` carrying the
    animation and `.pile.stilled` switching it off exactly as the sheet's block does;
  - **every pile holds exactly three `<span class="disc"></span>`** — three at every amount, on
    every row, which is what rows 1 and 2 exist to show. A gate pins 6 piles and 18 discs.
- **Five seat rows and two pot rows**, each caption in the file's existing `.l` idiom, lower case,
  written **verbatim** including the em dash. The stack numeral on each row is the existing
  `<span class="chips">` node and the pile sits beside it:

  | Row | `class="chips"` | Pile | Caption, verbatim |
  | --- | --- | --- | --- |
  | 1 | `13,400` | `pile` | `chips at rest — the numeral is the fact` |
  | 2 | `150` | `pile` | `the same pile at a hundredth of the stack — a pile is never a count` |
  | 3 | `4,550` | `pile flying` | `chips in flight — the numeral has already moved` |
  | 4 | `4,550` | `pile stilled` | `stilled — the flight is skipped and the chips are where they land` |
  | 5 | `0` | none | `a busted seat draws no chips` |
  | 6 | — | `pile` on the **existing** `.pot` | `the pot at rest` |
  | 7 | — | `pile flying` on a **new** `.pot` | `the pot receiving` |

  Row 3's caption is the load-bearing one: at the mid-flight frame **the numerals are already the
  new ones**. The client owns the schedule and states no fact the server did not send
  (`ADR-0102` §6) — the motion is decoration over a change that has already happened, never a
  withheld quantity.
- **The pile mints no string and speaks nothing.** No `aria-*`, no `role`, no text node inside a
  `.disc`, no new word. The numerals beside it are its whole voice.
- **The card carries no `transition:` anywhere, comments included.** A CSS transition on a value
  the store replaces is `ADR-0102`'s own recorded failure — *"the store replaces the array a
  millisecond later and kills the animation mid-flight"* — and the flight here is an `animation`
  precisely so it cannot take that shape. The gate counts the string and cannot tell a comment
  from a declaration, so do not write it in prose either.

## Out of scope

- **The two screen cards.** `duel-table.html` and `duel-table-states.html` copy these rules in
  `TASK-130602`. Do not open them here.
- **`ADR-0107` §4's bet-line question.** `TASK-130603` draws it. This card carries no bet-line at
  all today and gains none.
- **Any client code.** `app.css` and the components are `TASK-130604`–`TASK-130607`. Only the
  vendored sheet moves, and only because `tokens.test.ts` forces it to.
- **A chip under `design/graphics/`.** The coin is drawn there because an SVG travels;
  `--pd-coin-face` is how the *client* draws the same coin, and the chip follows that half of the
  precedent. A `.svg` would also owe `check-drift.sh` clause 4's `pd-NAME (#hex)` mirror pairs and
  a `_ds_manifest.json` entry no ticket can make. Not yet ticketed, and needed by nothing here.
- **Choosing the drawing for anyone else.** Face, rim, size, stagger, duration and travel are the
  human's eye (`ADR-0024` §3); the verdict may trail the merge.
- **A growing pile.** A pile whose size depends on the amount needs a denomination, which is a
  product decision nobody has asked for. If the human's verdict at the pane wants one, that is a
  repair ticket **and** a `DEC` for the product owner — not a change made inside a ticket.
- **An in-product motion setting.** `ADR-0115` §2 refuses one by name.

## Tests

**No test file, and none is possible** for the card: a design card is HTML nobody imports, and
`ADR-0089` §2b forbids a browser measurement being a gate. The gates are the `verify:` block —
twenty say what must now be on the card and in the sheet, four refuse what must not have appeared,
and `check-drift.sh` says the tokens, values and suit glyphs still hold.

The one **executable** test this ticket owes is the merged one it must not break:
`web-client/src/styles/tokens.test.ts`, which asserts the vendored sheet is byte-identical to the
canonical. It is pinned at `Tests 1 passed (1)`, and the whole `npm run check` runs beside it
because `theme.test.ts` reads the sheet's declared names too.

| Marker | Count today | Count after |
| --- | --- | --- |
| `--pd-chip-*` in the sheet | 0 | ≥ 3 named |
| `--pd-motion-chip-*` in the sheet | 0 | ≥ 3 named |
| `@media (prefers-reduced-motion: reduce)` in the sheet | 1 | **1** |
| `@keyframes pd-chip-flight` on the card | 0 | exactly 1 |
| `@keyframes` on the card | 1 | **2** |
| `class="pile` / `class="disc"` on the card | 0 / 0 | **6** / **18** |
| `class="chips"` / `class="pot"` on the card | 12 / 1 | **17** / **2** |
| `transition:` on the card | 0 | **0** |
| `>13,400<` / `>150<` on the card | 0 / 0 | **1** / **1** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `design/tokens/tokens.css` declares `--pd-chip-face`, `--pd-chip-edge`, `--pd-chip-size`,
      `--pd-motion-chip-flight`, `--pd-motion-chip-ease` and `--pd-motion-chip-travel`
- [ ] `design/tokens/tokens.css` still contains exactly one `@media (prefers-reduced-motion: reduce)`
- [ ] `design/components/seat-and-pot.html` contains exactly one `@keyframes pd-chip-flight`, two
      `@keyframes` in all, and exactly one `@media (prefers-reduced-motion: reduce)`
- [ ] The card contains `class="pile` exactly 6 times and `class="disc"` exactly 18 times
- [ ] The card contains `class="chips"` exactly 17 times and `class="pot"` exactly 2 times
- [ ] The card contains `>13,400<` exactly once and `>150<` exactly once
- [ ] The card contains no `transition:` at all
- [ ] Each of the seven captions in the table above appears exactly once, verbatim
- [ ] `src/styles/tokens.test.ts` reports `Tests  1 passed (1)`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
