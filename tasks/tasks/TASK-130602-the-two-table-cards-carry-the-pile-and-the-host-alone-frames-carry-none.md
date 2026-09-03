---
schema: 2
id: TASK-130602
title: The two table cards carry the pile, and the host-alone frames carry none
type: task
status: backlog
parent: STORY-1306
module: design
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [design, table]
depends_on: [TASK-130601]
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/@keyframes pd-chip-flight/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@keyframes pd-chip-flight/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@media \(prefers-reduced-motion: reduce\)/, "&") } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 8) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 24) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 7) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 21) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pile flying/, "&") } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pile flying/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "Host alone") { seen = 1 } seen { n += gsub(/class="pile/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/no chips at all — ADR-0110 §3 states no game fact here/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="frame"/, "&") } END { exit (n != 6) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="frame"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 4) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/class="bet-line"/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="bet-line"/, "&") } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/--pd-chip-/, "&") } END { exit (n < 4) }' design/screens/duel-table.html
  - awk '{ n += gsub(/--pd-chip-/, "&") } END { exit (n < 4) }' design/screens/duel-table-states.html
  - awk '{ n += gsub(/--pd-motion-chip-/, "&") } END { exit (n < 4) }' design/screens/duel-table.html
  - awk '{ n += gsub(/--pd-motion-chip-/, "&") } END { exit (n < 4) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two composed table cards draw the chips in place: a pile at each seat's stack, a pile at the
bet line, a pile at the pot, and a **flying** pile at the stack that just took a pot — while the
four host-alone frames draw none at all, because `ADR-0110` §3 states no game fact there.

## Why this is a transcription and not a design

`TASK-130601` merged the chip: the tokens, `@keyframes pd-chip-flight`, `.pile`, `.disc`,
`.pile.flying` and `.pile.stilled`, all judged at the pane. This ticket copies those rules
character for character into the two screen cards and puts the piles where the composed table
already puts the figures they belong to. Nothing new is designed. `check-drift.sh`'s value clause
compares every `--pd-` a card inlines against the sheet, so a mistyped value fails rather than
drifting.

## What is already true, measured on `develop` 2026-09-03

| Marker | `duel-table.html` | `duel-table-states.html` |
| --- | --- | --- |
| `class="frame"` | 6 — the laptop, the phone, and **four** `Host alone` | 3 |
| `class="chips"` | 4 — two seats × two live frames | 6 — two seats × three frames |
| `class="pot"` | 2 | 3 |
| `class="bet-line"` | 2 | 3 |
| `@keyframes` / `prefers-reduced-motion` | 1 / 1 | 1 / 1 |
| `class="pile` / `class="disc"` | 0 / 0 | 0 / 0 |
| `transition:` | 0 | 0 |

**The host-alone frames already carry no stack numeral at all** — that is why `class="chips"`
reads 4 on a file with six frames. `ADR-0110` §3 forbids a game fact there and the merged frames
honour it; this ticket must not smuggle one in as a drawing.

**`class="chip"` on both cards is the sizing button** — 10 and 15 occurrences. The drawn chip is
`.disc` inside `.pile`, never `.chip`, so no gate here can conflate them.

`duel-table-states.html`'s frames 2 and 3 put `You win 4,850` where the pot figure stands
(`ADR-0095`). **The pot has already gone to the stack there, so those frames carry no pot pile** —
they carry a `pile flying` at the hero's stack instead. That is the story's *stack receiving an
award*, drawn.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | modify |
| `design/components/seat-and-pot.html` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **Copy from the merged component card into each screen card's `<style>`**, unchanged: the six
  `--pd-chip-*` / `--pd-motion-chip-*` declarations inlined in the card's own `:root`, the one
  `@keyframes pd-chip-flight` block, and the `.pile`, `.disc`, `.pile.flying` and `.pile.stilled`
  rules. Each card keeps its **one** existing `@media (prefers-reduced-motion: reduce)` block —
  none is added.
- **`duel-table.html` — eight piles, all inside the two live frames.** In the laptop frame and in
  the 390 × 664 phone frame alike, one `<span class="pile">` beside each of:
  1. the rival's `<span class="chips">4,150</span>`,
  2. the rival's `<div class="bet-line">committed …</div>` figure,
  3. the `<span class="amount">Pot&nbsp;2,850</span>`,
  4. the hero's `<span class="chips">13,400</span>`.
- **`duel-table.html` — the four `Host alone` frames gain nothing**, and say so once, in the
  existing `.note` idiom, verbatim: `no chips at all — ADR-0110 §3 states no game fact here`. A
  gate counts `class="pile` from the first `Host alone` heading to the end of the file and
  requires **zero**.
- **`duel-table-states.html` — seven piles.**
  - Frame 1 (*Waiting — their turn on the turn card*): `pile` at the rival's `3,750`, `pile` at
    `Pot&nbsp;3,250`, `pile` at the hero's `13,000`. Its `<div class="bet-line"></div>` is empty
    and stays empty — nothing is committed, so nothing is drawn.
  - Frame 2 (*Showdown — you win, the loser mucks*): `pile` at the rival's `2,950`, and
    **`pile flying`** at the hero's `12,200`. **No pot pile** — the banner reads `You win 4,850`
    and the chips are what has just arrived at the stack.
  - Frame 3 (*Won without a showdown*): the same two, `pile` at the rival's stack and
    **`pile flying`** at the hero's.
  - The bet lines in frames 2 and 3 carry `ImKate mucks` and `ImKate folds` — words, not
    commitments — and gain no pile.
- **Every pile holds exactly three `<span class="disc"></span>`**, the same three the component
  card drew at `13,400` and at `150`. Gates pin 24 discs on one card and 21 on the other.
- **No frame is added and no figure moves.** `class="frame"` stays 6 and 3, `class="chips"` 4 and
  6, `class="pot"` 2 and 3, `class="bet-line"` 2 and 3 — all pinned. If a pile forces a figure to
  move to fit, stop and say so in the PR rather than editing the number.
- **Neither card gains `transition:` anywhere, comments included** — same reason and same blind
  spot as `TASK-130601`: the gate counts the string and cannot tell a comment from a declaration.

## Out of scope

- **`ADR-0107` §4's bet-line question.** Whether the `committed` line keeps standing once chips are
  drawn is `TASK-130603`'s frame, and this ticket draws the additive half only: the bet-line and
  its figure stay exactly as merged, with a pile added beside them.
- **The token sheet and the component card.** Merged in `TASK-130601`. A wrong value is a repair
  ticket against the sheet, not an edit here.
- **Any client code.** `TASK-130604`–`TASK-130607`.
- **The 390 × 664 fit as a gate.** `ADR-0089` §2b forbids a pull request waiting on a browser. The
  PR states in prose that the pile rides inside the row that already holds the figure, so no box
  grows; if a transcribed drawing does change a box, the PR says which and the human's pane
  verdict decides (`ADR-0103` §1, `ADR-0024` §3).
- **`design/components/action-bar.html` and `playing-card.html`.** No chip reaches them.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports (`ADR-0089` §2b).
The gates are the `verify:` block: twelve say what must now be on each card, eleven refuse what
must not have moved, and `check-drift.sh` says every inlined `--pd-` value still equals the
sheet's.

The refusal that matters most is the region gate. `awk` sets a flag at the first `Host alone`
heading and counts `class="pile` from there to the end of the file; a chip drawn on a table that
is not a duel fails it, which is `ADR-0110` §3 made mechanical.

| Marker | `duel-table.html` today → after | `duel-table-states.html` today → after |
| --- | --- | --- |
| `class="pile` | 0 → **8** | 0 → **7** |
| `class="disc"` | 0 → **24** | 0 → **21** |
| `class="pile flying` | 0 → **0** — the composed live table is drawn at rest | 0 → **2** |
| `class="pile` after `Host alone` | 0 → **0** | n/a |
| `@keyframes` | 1 → **2** | 1 → **2** |
| `class="frame"` / `class="chips"` / `class="pot"` / `class="bet-line"` | 6/4/2/2 → **6/4/2/2** | 3/6/3/3 → **3/6/3/3** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] Each card contains exactly one `@keyframes pd-chip-flight`, two `@keyframes` in all, and
      exactly one `@media (prefers-reduced-motion: reduce)`
- [ ] `duel-table.html` contains `class="pile` exactly 8 times, `class="disc"` exactly 24 times and
      `class="pile flying` **zero** times — the composed live table is drawn at rest
- [ ] `duel-table-states.html` contains `class="pile` exactly 7 times, `class="disc"` exactly 21
      times and `class="pile flying` exactly twice
- [ ] `duel-table.html` contains **zero** `class="pile` at or after its first `Host alone` heading
- [ ] `duel-table.html` contains `no chips at all — ADR-0110 §3 states no game fact here` exactly once
- [ ] `class="frame"`, `class="chips"`, `class="pot"` and `class="bet-line"` are unchanged on both
      cards — 6/4/2/2 and 3/6/3/3
- [ ] Neither card contains `transition:` at all
- [ ] Each card mentions `--pd-chip-` and `--pd-motion-chip-` at least four times
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
