---
schema: 2
id: TASK-141107
title: The showdown card is gated frame by frame, not file by file
type: task
status: done
parent: STORY-1411
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table, gate]
depends_on: [TASK-141102]
verify:
  - ./design/check-frame-cards.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && C="$T/design/screens/duel-table-states.html" && perl -0777 -pi -e "s{(<h2>Showdown — ImKate shows.*?<div class=\"oppcards\">)(.*?)(</div>.*?<h2>Fold —.*?<div class=\"oppcards\">)(.*?)(</div>)}{\$1\$4\$3\$2\$5}s" "$C" && ! cmp -s design/screens/duel-table-states.html "$C" || exit 2; "$T/design/check-frame-cards.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && C="$T/design/screens/duel-table-states.html" && perl -0777 -pi -e "s{(<h2>Showdown — ImKate shows.*?<div class=\"oppcards\">)(.*?)(</div>.*?<h2>Fold —.*?<div class=\"oppcards\">)(.*?)(</div>)}{\$1\$4\$3\$2\$5}s" "$C" && ! cmp -s design/screens/duel-table-states.html "$C" || exit 2; [ "$(grep -c "class=\"pc" "$C")" -eq 29 ] && [ "$(grep -c "back mucked" "$C")" -eq 4 ] && [ "$(grep -c "class=\"frame\"" "$C")" -eq 4 ] && "$T/design/check-drift.sh" >/dev/null 2>&1'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && C="$T/design/screens/duel-table-states.html" && perl -0777 -pi -e "s{<h2>Fold — you win on the river, nobody shows</h2>}{<h2>Nobody shows on the river</h2>}" "$C" && ! cmp -s design/screens/duel-table-states.html "$C" || exit 2; "$T/design/check-frame-cards.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && C="$T/design/screens/duel-table-states.html" && perl -0777 -pi -e "s{    <div class=\"frame\">}{    <div class=\"frame\">\n      <h2>Probe</h2>\n          <div class=\"oppcards\">\n            <span class=\"pc\"></span>\n          </div>\n    </div>\n\n    <div class=\"frame\">}" "$C" && [ "$(grep -c "class=\"frame\"" "$C")" -eq 5 ] || exit 2; "$T/design/check-frame-cards.sh" >/dev/null 2>&1'
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc") { n++ } END { exit (n != 29) }' design/screens/duel-table-states.html
  - awk 'index($0, "back mucked") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - ./design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/check-frame-cards.sh` exists and says, **inside the named frame** rather than across the
file, that the fold frame's rival slot holds two dimmed backs and no face, and that the
showdown-lost frame's holds two faces and no back. `TASK-141102` landed correct card frames behind
gates that cannot hold them, and this is the gate that can.

## Files

| File | Action |
| --- | --- |
| `design/check-frame-cards.sh` | create |

Read `design/check-drift.sh` for the house shape of a design gate — `set -eu`, a `check-…:` prefix
on every message, a loud refusal rather than a quiet pass — and
`design/screens/duel-table-states.html` for the markup being gated. **No other file is opened and
no other file is changed**: this ticket adds one script and edits no frame markup, no stylesheet,
no card and nothing under `web-client/`.

## Why this ticket exists

Every gate in `TASK-141102` is a **whole-file aggregate count** over
`design/screens/duel-table-states.html` — `class="pc"` 29, `class="pc red"` 13, `back mucked` 4,
the width counts, the `aria-label` counts — with **no anchor to which `<div class="frame">` the
markup sits in**.

**That hole was confirmed by construction, not suspected.** Swapping the `oppcards` markup between
the new showdown-lost frame and the untouched fold frame, leaving every other line alone, leaves all
48 of that ticket's `verify` commands exiting 0, `./design/check-drift.sh` included — and it was
re-confirmed on the merged card at `96b9a278` while writing this ticket: **41 of 41 count gates, all
5 absence gates and `check-drift.sh` still pass on the swapped file**, and the 48th command,
`lint_tickets.py`, never reads the card at all. The fold frame then shows a **folded hand face up**,
which `CLAUDE.md`'s non-negotiable — *"folded and mucked cards appear in no event, anywhere"* —
forbids outright.

A whole-file count can never tell *"the right frame reveals"* from *"some frame reveals and another
silently does not"*. A swap moves nothing in or out of the file, so every total is invariant under
exactly the mutation that matters.

## Scope

Create `design/check-frame-cards.sh`, executable, POSIX `sh` plus `awk` and stock macOS/Linux tools
— roughly 110 lines. It takes no argument: it resolves its own directory the way `check-drift.sh`
does and reads `screens/duel-table-states.html` beside itself, so the mutation gates below can copy
the whole `design` tree to a scratch directory and run the copy.

**1. Cut the card into frame blocks.** A block opens on a line containing `<div class="frame">` and
closes when `<div>` nesting returns to zero, counting `<div` opens and `</div>` closes per line. A
block's **name** is the text of the first `<h2>…</h2>` inside it.

**2. Find each gated frame by that name, never by anything else.** Matching is a substring test
against the heading. **No line number, no ordinal, no "the third frame"** — `TASK-141101` and
`TASK-141102` both moved lines in this file in one session, and `TASK-141103` will move them again.
A `verify:` gate inserts an extra frame *before* every existing one and requires the script still
exits 0, so a position-anchored reader cannot pass.

**3. Count inside that frame's own `<div class="oppcards">` slot** — the rival's two card places,
which is exactly what the swap exchanged. The slot opens on a line containing
`<div class="oppcards">` and closes when its own nesting returns to zero.

**4. The expectation table, verbatim.** Both rows were **measured** against the merged card at
`96b9a278`, not computed:

```sh
EXPECT="Fold —|0|0|2|2
Showdown — ImKate shows|1|1|0|0"
```

The five fields are the `<h2>` anchor, then the counts that frame's own rival slot must hold of
`class="pc"`, `class="pc red"`, `class="back` (any back) and `class="back mucked"`. So:

- **Fold** — `back mucked` **2**, `class="back` **2**, `class="pc"` **0**, `class="pc red"` **0**.
  `ADR-0008` and `CLAUDE.md`: a folded hand appears in no event, anywhere.
- **Showdown — ImKate shows** — `class="pc"` **1**, `class="pc red"` **1**, `class="back` **0**,
  `back mucked` **0**. `ADR-0120` §1: the hands the rules showed are face up. The 1 + 1 split is
  measured — `J♠` is a plain `.pc` and `J♦` is `.pc red` — and is a stronger statement than
  "two faces".

**The reviewer's brief said `pc` / `pc red` == 0 *in the fold frame*, and that is false at
whole-frame scope: the fold frame holds seven face-up cards — five board and the hero's two — so its
frame-level counts are `class="pc"` 4 and `class="pc red"` 3.** All four of the briefed numbers are
exact one level in, at the rival's `oppcards` slot, which is also the locus the swap acted on. The
measured numbers win, and this is where they are true. For the record, the frame-level counts on the
merged card are: waiting 3/3/2/0, showdown-won 4/3/2/2, showdown-lost 5/4/0/0, fold 4/3/2/2
(`pc` / `pc red` / `back*` / `back mucked`).

**5. Be loud, never quiet.** The script exits **1**, naming the frame, when any of these holds — a
vacuous pass guards nothing, and each of these was probed and fires:

| Refusal | Why it must be one |
| --- | --- |
| a heading anchor matches **0** frames, or **2** | a renamed heading would otherwise silently gate nothing |
| a named frame holds other than **1** rival slot | there is no single home to count in |
| a rival slot contains a nested `<div>` | its own `</div>` is missing and the count is already running past the slot it names |
| a `<div class="frame">` never closes | the block reader stopped short |
| the card holds **0** frames | the gate would pass across nothing |
| the card file is missing | same |

**6. Report every frame, gated or not**, one line each on success — heading and the four slot counts
— so a frame added later shows up in the gate's own output instead of quietly falling outside it.
On success the script exits 0 after a one-line summary.

**Run the last three `verify:` commands before changing anything.** They are `TASK-141102`'s own
whole-file numbers on the untouched card (`class="frame"` 4, `class="pc` 29, `back mucked` 4). They
must already be green on arrival; if one is red, the card moved under this ticket — **stop and
report, do not edit the card.**

## Out of scope

- **Every frame the table does not name.** The waiting frame and the won-showdown frame are not
  gated here; `TASK-141102`'s whole-file counts still cover the file as a whole, and this gate adds
  the frame-scoped statement only where a shown-versus-hidden hand is the point.
- **The split frame.** `TASK-141103` has not drawn it, so there is nothing to measure and a computed
  row would be a guess. It is one `EXPECT` row when that frame exists, and the gate's per-frame
  report prints it as `ungated` until then, in the reviewer's face rather than in silence.
- **Changing any frame markup.** `design/screens/duel-table-states.html` is **not edited** — three
  `verify:` commands pin its counts unchanged, and the diff is one new file.
- **Replacing `TASK-141102`'s gates.** They stay exactly as merged. A whole-file count is a true
  statement about the file; it is simply not a statement about a frame. This ticket adds the second
  kind and removes none of the first.
- **Wiring either gate into CI.** `.github/workflows/build.yml` runs Gradle and the client suite and
  has never run `check-drift.sh`; design gates are `verify:`-block gates by convention
  (`ADR-0024` §2). Changing that is a workflow question nobody has asked and is not ticketed.
- **Generalising the gate to other cards under `design/screens/`.** It names one card, because one
  card is what has frames drawing hands.
- **`design/check-drift.sh`.** Untouched. It answers token, value, suit, graphics, symbol and lockup
  questions and has no opinion about which frame a card sits in.

## Tests

None to name: a gate is not a test suite, and the `verify:` block is the whole assertion. Its arms:

| Gate | Proves |
| --- | --- |
| `./design/check-frame-cards.sh` | the gate is green on the card as merged |
| the swap, then `[ $? -eq 1 ]` | **the exact mutation that motivated this ticket goes red** — the reviewer's swap of the showdown-lost and fold `oppcards` slots, anchored on the two headings so it survives a fifth frame being inserted between them |
| the same swap, then `class="pc` 29, `back mucked` 4, `class="frame"` 4 and `check-drift.sh` | the hole is real and stays recorded: every whole-file gate is blind to that mutation |
| the fold heading renamed, then `[ $? -eq 1 ]` | the heading anchor is load-bearing — a gate that found the frame some other way would stay green here |
| an extra frame inserted **before** every existing one, then exit 0 | the gate is position-independent: ordinals shift by one and nothing moves |
| `class="frame"` 4, `class="pc` 29, `back mucked` 4 on the real card | no frame markup changed |
| `./design/check-drift.sh` | the design tree's own invariants still hold with a new file beside them |

Both mutation arms were falsified as well as run: with `check-frame-cards.sh` replaced by
`#!/bin/sh` + `exit 0`, the swap arm and the heading-rename arm both go **red**, so neither can be
satisfied by a script that does nothing.

## Acceptance criteria

- [ ] `design/check-frame-cards.sh` exists, is executable, takes no argument, and exits **0** on the
      card as merged
- [ ] Its output names **4** frames, marking `Showdown — ImKate shows…` and `Fold —…` as gated and
      the other two as ungated, with each frame's four slot counts
- [ ] Swapping the two frames' `<div class="oppcards">` slots makes it exit **1**, naming both
      frames — the second `verify:` command
- [ ] On that same swapped copy, `class="pc` is still 29, `back mucked` still 4, `class="frame"`
      still 4 and `check-drift.sh` still exits 0 — the third `verify:` command
- [ ] Renaming the fold frame's `<h2>` makes it exit **1** — the fourth `verify:` command
- [ ] Inserting an extra `<div class="frame">` before every existing frame leaves it exiting **0** —
      the fifth `verify:` command
- [ ] `design/screens/duel-table-states.html` is byte-unchanged: `class="frame"` 4, `class="pc` 29
      and `back mucked` 4 all still hold
- [ ] The diff touches exactly one file, `design/check-frame-cards.sh`, and it is a create
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
