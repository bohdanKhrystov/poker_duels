---
schema: 2
id: TASK-130603
title: The card asks whether the bet-line survives the chips, and answers half of it already
type: task
status: done
parent: STORY-1306
module: design
estimate: S
tier: sonnet
review: light
files_touched: 1
labels: [design, table]
depends_on: [TASK-130602]
verify:
  - ./design/check-drift.sh
  - awk '{ n += gsub(/class="frame"/, "&") } END { exit (n != 7) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pile/, "&") } END { exit (n != 12) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="disc"/, "&") } END { exit (n != 36) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="bet-line"/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="chips"/, "&") } END { exit (n != 6) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pot"/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/Pot&nbsp;2,850/, "&") } END { exit (n != 3) }' design/screens/duel-table.html
  - awk '{ n += gsub(/Phone — the bet-line retired/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk '{ n += gsub(/ADR-0107 §4: the stack numerals stand in both — a stack stated only as a drawing would break ADR-0115 §1/, "&") } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Host alone") { seen = 1 } seen { n += gsub(/class="pile/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/class="pile flying/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/transition:/, "&") } END { exit (n != 0) }' design/screens/duel-table.html
  - awk '{ n += gsub(/@keyframes/, "&") } END { exit (n != 2) }' design/screens/duel-table.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html` carries one more 390 × 664 frame: the same table with the
rival's `committed` bet-line **retired**, its chips standing at the bet line with no figure of
their own. The human's eye chooses between that frame and the merged one beside it
(`ADR-0024` §3), and `ADR-0107` §4's open question finally has two drawings to be answered from.

## The question, and the half of it that is already settled

`ADR-0107` §4, verbatim: *"Whether the bet-lines keep standing once `EPIC-13` item 6 makes stacks
and bets drawn chips is that item's card question and the human's eye, not this ADR's; nothing
here forbids or requires it."* §Consequences owns the duplication that makes it a question at all:
the rival's street chips now appear **twice** — on their bet line and inside the pot figure.

**The stack numerals are not part of the question.** `ADR-0115` §1 settles that half:
*"Every fact a surface states… is stated by its still form."* A stack drawn only as a pile would
put its value in a drawing and nowhere else, and §6 names the stack numerals as one of the three
things that state amounts still. So both frames keep every `class="chips"` numeral, and the only
thing this card proposes retiring is the `committed 400` line — whose amount is **already inside
`Pot 2,850`** by `ADR-0107` §1, so retiring it removes no fact from the screen.

That asymmetry is the frame's caption, written verbatim into the card so nobody re-derives it.

## Why the client ships the additive half regardless

`TASK-130607` adds a pile to the bet line and removes nothing, because a verdict may trail the
merge (`ADR-0024` §3, `ADR-0091` §3) and an unattended run must not stall on a pane. **Retiring
the bet-line in the client is a separate ticket that exists only if the verdict asks for it** —
not yet ticketed. No ticket in this story decides it silently, which is exactly what the story's
design notes require.

## What is already true, after `TASK-130602`

`duel-table.html` carries `class="frame"` 6, `class="pile` 8, `class="disc"` 24,
`class="bet-line"` 2, `class="chips"` 4, `class="pot"` 2, `Pot&nbsp;2,850` 2, `@keyframes` 2,
`class="pile flying` 0, `transition:` 0, and **zero** `class="pile` at or after the first
`Host alone` heading.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md` | read |
| `docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md` | read |

## Scope

- **One new `class="frame"`, inserted immediately after the existing `Phone — 390 × 664` frame
  and before the first `Host alone` frame.** Its heading is verbatim
  `Phone — the bet-line retired`. Placing it there keeps the merged region gate true: nothing at
  or after `Host alone` gains a pile.
- **The new frame is the phone frame, copied, with exactly two differences:**
  1. its `<div class="bet-line">committed <span class="amt">400</span></div>` becomes a
     `<span class="pile">` with three `<span class="disc"></span>` and **no `bet-line` div and no
     figure** — so `class="bet-line"` stays at 2 across the file, which is the gate that proves
     the line is genuinely gone rather than merely restyled;
  2. everything else is identical, `Pot&nbsp;2,850` included — the pot figure is where the
     retired 400 already lives.
- **Both stack numerals stay** in the new frame, so `class="chips"` goes 4 → 6.
- **One caption, in the file's `.note` idiom, verbatim**, on the new frame:
  `ADR-0107 §4: the stack numerals stand in both — a stack stated only as a drawing would break ADR-0115 §1`
- **Nothing else on the card moves.** The two live frames and the four host-alone frames are not
  touched; `class="pile flying` stays 0, `@keyframes` stays 2, `transition:` stays 0.

## Out of scope

- **Deciding the answer.** This ticket draws the alternative; the human's eye at the pane decides
  (`ADR-0024` §3), and the verdict may trail the merge. Do not delete the merged bet-line.
- **`duel-table-states.html`.** Its three bet lines carry `ImKate mucks`, `ImKate folds` and an
  empty string — words, never a commitment — so the question does not reach them.
- **Retiring the bet-line in the client.** Not yet ticketed, and conditional on the verdict.
- **The stack numeral.** Settled by `ADR-0115` §1, not re-asked. A frame that removed a stack
  numeral would be drawing a question that is already answered.
- **Any client code, the sheet, or the component card.**

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports (`ADR-0089` §2b).
The `verify:` block is the gate set: four say what the new frame must contain, seven refuse what
must not have moved with it.

The load-bearing gate is `class="bet-line"` staying at **2**. A frame that kept the div and merely
hid its figure would look right in the pane and answer nothing; the count is what makes *retired*
mean retired.

| Marker | Today | After |
| --- | --- | --- |
| `class="frame"` | 6 | **7** |
| `class="pile` / `class="disc"` | 8 / 24 | **12** / **36** |
| `class="chips"` | 4 | **6** |
| `class="pot"` / `Pot&nbsp;2,850` | 2 / 2 | **3** / **3** |
| `class="bet-line"` | 2 | **2** |
| `class="pile` after `Host alone` | 0 | **0** |
| `class="pile flying` / `transition:` / `@keyframes` | 0 / 0 / 2 | **0** / **0** / **2** |

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `duel-table.html` contains `class="frame"` exactly 7 times
- [ ] It contains `Phone — the bet-line retired` exactly once
- [ ] It contains `class="pile` exactly 12 times and `class="disc"` exactly 36 times
- [ ] It contains `class="bet-line"` exactly 2 times — unchanged
- [ ] It contains `class="chips"` exactly 6 times, `class="pot"` exactly 3 times and
      `Pot&nbsp;2,850` exactly 3 times
- [ ] It contains the caption
      `ADR-0107 §4: the stack numerals stand in both — a stack stated only as a drawing would break ADR-0115 §1`
      exactly once
- [ ] It contains **zero** `class="pile` at or after its first `Host alone` heading, zero
      `class="pile flying`, zero `transition:`, and exactly 2 `@keyframes`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
