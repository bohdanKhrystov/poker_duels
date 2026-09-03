---
schema: 2
id: TASK-130701
title: Draw the turn clock, its two allowance states, and the timebank at both seats
type: task
status: ready
parent: STORY-1307
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [design, table, clock, minting]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation:/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/animation-/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/--pd-warn/, "&") } END { exit (n < 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock/, "&") } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock"/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="clock running-out"/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/\.clock\.running-out/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="timebank"/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;3:00/, "&") } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/Timebank&nbsp;1:12/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat/, "&") } END { exit (n != 21) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat on-turn"/, "&") } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="seat on-turn stilled"/, "&") } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 21) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="l"/, "&") } END { exit (n != 24) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="dealer"/, "&") } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="last-act"/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 6) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/forfeit/, "&") } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/the clock carries no animation of any kind — a numeral changing each second is a step, not motion, so the clock at rest is the clock/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/regular — the fresh allowance, counting down once a second/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/running out — the same allowance, urgent; the figure is still the whole fact/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/at rest — identical, because a numeral changing each second is a step and not motion/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/not on turn — no countdown at all, and the timebank is still a public fact of the table/, "&") } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/tokens/tokens.css
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/styles/tokens.test.ts 2>&1 | grep -qE '^ *Tests +1 passed \(1\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/components/seat-and-pot.html` draws the turn clock: the countdown at the seat on turn in
its two **allowance** states — *regular* and *running out* — the same plate **at rest**, and the
seat's **timebank** figure at a seat that is not on turn. The clock carries **no animation of any
kind**, and the card says why.

## The rule this card exists to settle — read it before drawing

`STORY-1307`'s hardest constraint is
[`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
§1: **no fact may live only in motion.** A countdown looks like the one surface that breaks it —
its whole content is *how long is left*, and it changes every second.

It does not break it, and `ADR-0115` §3 already says so in the clock's own name:

> The turn clock's once-a-second change (`EPIC-13` item 4) — **each second's numeral is a step**. A
> smooth sub-second depletion drawn between them is a *how*, and is what a reduced form skips.

So the settled answer, and the thing this card must make visible:

**The still form of a ticking clock is the clock.** A numeral that changes each second is
information arriving, not motion, so a player with `prefers-reduced-motion: reduce` reads the same
figure at the same second as everyone else. Nothing is stilled away, because nothing was moving.

`STORY-1306` proved `ADR-0115` is best satisfied **structurally** rather than by a second drawing —
`pd-chip-flight` ends at `transform: none`, so the final frame *is* the no-animation frame. The
structural satisfaction here is stronger and simpler: **the clock animates nothing at all.** Three
gates hold it — `@keyframes` pinned at 2, `animation:` at 4 and `animation-` at 4, all unchanged —
so no keyframe, shorthand or longhand can reach the clock, and the reduced-motion form is
byte-identical by construction rather than by care.

The **at rest** row draws it anyway, because a reader who does not know this will assume a clock
must tick visually. That row is `.seat.on-turn.stilled` — the merged spelling, the merged rule, no
new CSS — and it is identical to the *regular* row in every fact. Its caption is the proof.

## What is already true, measured on `develop` 2026-09-03

- `design/tokens/tokens.css` **already declares the clock's one distinctive colour**, and reserved
  it for this and nothing else: `--pd-warn: #c99a4a;`, under the comment *"Amber exists only for
  the turn clock running down."* The sheet also already says *"Numbers that move (pot, stacks,
  **clock**) are mono with tabular figures so nothing jitters."* So the vocabulary this card needs
  was minted before the clock existed, and the card **composes** unless the chosen drawing needs a
  value the sheet does not have.
- `--pd-warn` appears in **no** card and in **no** client file. This card is its first use, which
  is why a gate requires it here.
- `seat-and-pot.html` carries: `class="seat` 17, `class="seat on-turn"` 2,
  `class="seat on-turn stilled"` 1, `class="seat away"` 1, `class="l"` 20, `class="chips"` 17,
  `class="dealer"` 7, `class="last-act"` 6, `class="pile` 6, `@keyframes` 2, `animation:` 4,
  `animation-` 4, `transition:` 0, `@media (prefers-reduced-motion: reduce)` 1, `--pd-warn` 0,
  `class="clock` 0, `class="timebank"` 0, `forfeit` 0.
- The card inlines **raw values** for type sizes (`0.6875rem`) and `var(--pd-…)` only for the
  tokens it actually uses, so `--pd-warn` must be added to its `:root` before `var(--pd-warn)`
  will resolve. `check-drift.sh` clause 3 then compares the inlined value against the sheet.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | modify |
| `design/tokens/tokens.css` | modify |
| `web-client/src/styles/tokens.css` | modify |
| `docs/adr/ADR-0108-expiry-plays-the-seat-and-the-timebank-replaces-the-grace-window.md` | read |
| `docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md` | read |

## Scope

- **Add to `seat-and-pot.html`'s `:root`**, beside the values already inlined there:
  `--pd-warn: #c99a4a;`, with a comment citing `ADR-0108` §5 and the sheet's own reservation.
- **Add two CSS rules and one comment**, in the file's existing idiom:

  ```
  .clock              the countdown at the seat on turn — mono, tabular figures,
                      larger than the status line, coloured --pd-text
  .clock.running-out  the same figure in var(--pd-warn)
  .timebank           the seat's remaining bank — mono, tabular, quieter than .clock
  ```

  The comment above `.clock` carries this sentence **verbatim**, and a gate counts it:

  `the clock carries no animation of any kind — a numeral changing each second is a step, not motion, so the clock at rest is the clock`

  Cite `ADR-0115` §§1, 3 beside it. Both elements ride as children of `.seat`'s own flex row — the
  `.last-act` idiom the file already documents, *"so `.who` gives ground by truncating the name
  first and the plate never grows a pixel taller."* Their **order within the row** is the drawing's
  and the pane's; no gate reads it.
- **Four rows**, appended to the existing *The seat, in its states* stack, each with its caption in
  the file's `.l` idiom, **verbatim including the em dash**:

  | Row | Plate | Name / status | `.clock` | `.timebank` | `.chips` | Caption, verbatim |
  | --- | --- | --- | --- | --- | --- | --- |
  | A | `seat on-turn` | ImKate / `Their turn` | `<span class="clock">24</span>` | `Timebank&nbsp;3:00` | `4,550` | `regular — the fresh allowance, counting down once a second` |
  | B | `seat on-turn` | ImKate / `Their turn` | `<span class="clock running-out">6</span>` | `Timebank&nbsp;3:00` | `4,550` | `running out — the same allowance, urgent; the figure is still the whole fact` |
  | C | `seat on-turn stilled` | ImKate / `Their turn` | `<span class="clock">24</span>` | `Timebank&nbsp;3:00` | `4,550` | `at rest — identical, because a numeral changing each second is a step and not motion` |
  | D | `seat` | You / empty status | **none** | `Timebank&nbsp;1:12` | `7,450` | `not on turn — no countdown at all, and the timebank is still a public fact of the table` |

  Row C is row A with one class added and nothing else different — that identity is the point.
  Row D carries a **different** bank figure from rows A–C on purpose: two seats, two values, so a
  drawing that hard-codes one bank cannot pass. No row gains a `.dealer` or a `.pile`; those counts
  are pinned unchanged.
- **The figures are the values as they are today, never a rule.** 30 s and 3 m are configuration
  (`ADR-0108` §1) — the card draws `24`, `6`, `3:00` and `1:12` as *this* table's numbers and
  writes no arithmetic anywhere.
- **The shape of the figures**: bare whole seconds under a minute (`24`, `6`), `m:ss` at a minute
  or more (`3:00`, `1:12`). `ADR-0046` §3 leaves the numeral's shape to the design, and this is it;
  the human's pane verdict overrules it (`ADR-0024` §3).
- **Mint only if the drawing needs it.** If the chosen treatment needs a value the sheet does not
  declare, mint it as `--pd-clock-*` in `design/tokens/tokens.css` with a comment saying what it
  derives from, and **copy the sheet byte-for-byte** to `web-client/src/styles/tokens.css`
  (`tokens.test.ts` compares buffers). If nothing is minted, both sheets are untouched and that is
  a correct outcome — the gates pass either way. **No second reduced-motion block** in the sheet;
  a gate pins it at exactly one.
- **`forfeit` appears nowhere on the card**, in copy, caption or comment. `ADR-0046` §5 forbids the
  word because it is false and `ADR-0108` §3 keeps it false. The gate counts the string and cannot
  tell prose from copy, so do not write it even to deny it.

## Why the bank figure says `Timebank`

`ADR-0108` §5 makes **both** banks public facts of the table, so a bare `3:00` beside a stack
needs a label or it states nothing. The word is derived, not invented, and the derivation is
recorded here so the pane can overrule it in one line:

- **`Timebank`** is the human's own word, verbatim from the feedback that opened `EPIC-13` —
  *"30seconds for move + 3m timebank"*.
- The **shape** `Timebank&nbsp;3:00` is the shipped label-and-figure idiom this same table already
  uses for `Pot&nbsp;2,850`, non-breaking space included.
- Bare **`Bank`** is refused: `docs/vision.md` *Positioning* refuses *bankroll* by name, and a lone
  `Bank` beside a chip stack reads as money in a product that has none.

If the pane wants the wordless form instead — `+3:00`, a reserve behind the countdown — that is a
repair ticket against this card and against nothing else, because no client code has been written
against it yet.

## Out of scope

- **The states after the allowance is spent.** *on timebank*, *expired*, and the plate after the
  server acted are `TASK-130702`'s three rows. Do not draw them here, and do not add
  `.clock.on-timebank` or `.clock.expired` — the CSS arrives with the rows that use it.
- **`AWAY` and `ABSENT`.** `TASK-130703` draws `ADR-0108` §4's table and retires the merged
  `reconnecting…` row. Leave that row exactly as it stands; `class="seat away"` is pinned at 1.
- **The two screen cards.** `TASK-130704`. `duel-table.html` and `duel-table-states.html` are not
  in this budget.
- **The presence line under the table.** *"The duel is paused."* and its replacement are derived by
  `STORY-1309`, against this card (`ADR-0108` §5). No sentence is chosen here.
- **Any client or server code.** This story draws. If drawing requires opening one, stop and
  register a `DEC`.
- **A pulse, ring, sweep, bar or any other continuous motion on the clock.** Refused by
  construction above. If the human's eye wants one at the pane, that is a repair ticket that must
  bring its still form with it under `ADR-0115` §4 — never a keyframe added inside this one.
- **Choosing the drawing for anyone else.** Size, weight, placement in the row and the exact
  urgent treatment are the human's eye (`ADR-0024` §3); the verdict may trail the merge
  (`ADR-0091` §3).

## Tests

**No test file, and none is possible** for a design card: it is HTML nobody imports, and
`ADR-0089` §2b forbids a browser measurement being a pull request's gate. The gates are the
`verify:` block — thirteen say what must now be on the card, ten refuse what must not have moved
or appeared, and `check-drift.sh` says every inlined `--pd-` value still equals the sheet's.

The three that carry the story's argument are `@keyframes` = 2, `animation:` = 4 and `animation-`
= 4. Together they say *the clock added no motion of any kind*, which is what makes its still form
identical without a second drawing.

The one **executable** test in the budget is the merged `web-client/src/styles/tokens.test.ts`,
pinned at `Tests 1 passed (1)`, with `npm run check` beside it because `theme.test.ts` reads the
sheet's declared names too. Both pass unchanged when nothing is minted.

| Marker | Today | After |
| --- | --- | --- |
| `class="clock` / `class="clock"` / `class="clock running-out"` | 0 / 0 / 0 | **3 / 2 / 1** |
| `.clock.running-out` (the CSS rule) | 0 | **1** |
| `class="timebank"` | 0 | **4** |
| `Timebank&nbsp;3:00` / `Timebank&nbsp;1:12` | 0 / 0 | **3 / 1** |
| `class="seat` / `class="seat on-turn"` / `class="seat on-turn stilled"` | 17 / 2 / 1 | **21 / 4 / 2** |
| `class="chips"` / `class="l"` | 17 / 20 | **21 / 24** |
| `class="dealer"` / `class="last-act"` / `class="pile` | 7 / 6 / 6 | **7 / 6 / 6** |
| `@keyframes` / `animation:` / `animation-` / `transition:` | 2 / 4 / 4 / 0 | **2 / 4 / 4 / 0** |
| `@media (prefers-reduced-motion: reduce)` | 1 | **1** |
| `--pd-warn` on the card | 0 | **≥ 2** |
| `forfeit` | 0 | **0** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `seat-and-pot.html` contains `class="clock` exactly 3 times, `class="clock"` exactly twice and
      `class="clock running-out"` exactly once, and the CSS rule `.clock.running-out` exactly once
- [ ] `seat-and-pot.html` contains `class="timebank"` exactly 4 times, `Timebank&nbsp;3:00` exactly
      3 times and `Timebank&nbsp;1:12` exactly once
- [ ] `seat-and-pot.html` contains `class="seat` exactly 21 times, `class="seat on-turn"` exactly 4
      times and `class="seat on-turn stilled"` exactly twice
- [ ] `seat-and-pot.html` still contains `@keyframes` exactly twice, `animation:` exactly 4 times,
      `animation-` exactly 4 times and `transition:` not at all — the clock added no motion
- [ ] `seat-and-pot.html` still contains exactly one `@media (prefers-reduced-motion: reduce)`, and
      so does `design/tokens/tokens.css`
- [ ] `seat-and-pot.html` mentions `--pd-warn` at least twice — once inlined in `:root`, once used
- [ ] `class="dealer"`, `class="last-act"` and `class="pile` are unchanged at 7, 6 and 6
- [ ] Each of the four captions in the table above appears exactly once, verbatim
- [ ] The comment sentence *the clock carries no animation of any kind …* appears exactly once,
      verbatim
- [ ] `forfeit` appears nowhere on the card
- [ ] `src/styles/tokens.test.ts` reports `Tests  1 passed (1)`, and `cd web-client && npm run check`
      exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
