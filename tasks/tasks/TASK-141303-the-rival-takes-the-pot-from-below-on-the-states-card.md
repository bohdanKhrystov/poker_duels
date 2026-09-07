---
schema: 2
id: TASK-141303
title: The rival takes the pot from below on the states card
type: task
status: done
parent: STORY-1413
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, table, bug]
depends_on: [TASK-141302, TASK-141103]
verify:
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - sh -c '! grep -qF "pile flying\"" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "@keyframes pd-chip-flight " design/screens/duel-table-states.html'
  - awk 'index($0, "--pd-motion") { n++ } END { exit (n != 15) }' design/screens/duel-table-states.html
  - awk 'index($0, "pd-chip-flight") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "animation:") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - sh -c '! grep -q "transition" design/screens/duel-table-states.html'
  - grep -qF '.pile.flying-down { animation-name: pd-chip-flight-down; animation-duration: var(--pd-motion-chip-flight);' design/screens/duel-table-states.html
  - grep -qF '.pile.flying-up { animation-name: pd-chip-flight-up; animation-duration: var(--pd-motion-chip-flight);' design/screens/duel-table-states.html
  - sh -c 'a=$(sed -n "/@keyframes pd-chip-flight-up/,/^  }$/p" design/components/seat-and-pot.html); b=$(sed -n "/@keyframes pd-chip-flight-up/,/^  }$/p" design/screens/duel-table-states.html); [ -n "$a" ] && [ "$a" = "$b" ]'
  - awk 'index($0,"class=\"pile ")>0 && index($0,"flying-down")==0 && index($0,"flying-up")==0 && index($0,"stilled")==0 { bad++ } END { exit (bad+0 != 0) }' design/screens/duel-table-states.html
  - awk '/<div class="opp">/ { side="opp" } /<div class="hero">/ { side="hero" } index($0,"pile flying-up") { if (side!="opp") bad++; else up++ } index($0,"pile flying-down") { if (side!="hero") bad++; else down++ } END { exit (bad+0 != 0 || up+0 < 1 || down+0 < 1) }' design/screens/duel-table-states.html
  - sh -c 'cp design/screens/duel-table-states.html "${TMPDIR:-/tmp}/st.m1.html"; perl -0pi -e "s/pile flying-up/pile flying-XX/g; s/pile flying-down/pile flying-up/g; s/pile flying-XX/pile flying-down/g" design/screens/duel-table-states.html; awk "/<div class=\"opp\">/ { side=\"opp\" } /<div class=\"hero\">/ { side=\"hero\" } index(\$0,\"pile flying-up\") { if (side!=\"opp\") bad++; else up++ } index(\$0,\"pile flying-down\") { if (side!=\"hero\") bad++; else down++ } END { exit (bad+0 != 0 || up+0 < 1 || down+0 < 1) }" design/screens/duel-table-states.html; r=$?; cp "${TMPDIR:-/tmp}/st.m1.html" design/screens/duel-table-states.html; cmp -s "${TMPDIR:-/tmp}/st.m1.html" design/screens/duel-table-states.html && [ "$r" -ne 0 ]'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every winner's pile on the table-states card is aimed at the middle: the pile in a
`<div class="opp">` block flies **up**, because the pot is below the rival's seat, and the pile in a
`<div class="hero">` block flies **down**, because the pot is above the viewer's. The card's copy of
the flight CSS follows the component card in the same diff. This is the frame the human annotated.

## Why this is a ticket

`design/screens/duel-table-states.html:374` — the *Showdown — ImKate shows* frame — draws
`<span class="pile flying">` at ImKate's seat, which under the merged `pd-chip-flight` means *falls
in from above*. Her seat is at the **top** of the table and the pot is **below** her, so the chips
she just won arrive from off the top of the screen: the opposite of the arrow the human drew from
the pot's pile to that exact plate. The viewer's two winning frames already fly the right way and
only change spelling.

This card also holds a **faithful copy** of the component card's flight CSS (its own comment says
so), so the block follows `TASK-141301` here as it did in `TASK-141302`.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |

**Nothing else is opened.** Both other cards are already right when this starts.

## Scope

- **The CSS block.** The same replacement `TASK-141301` merged into
  `design/components/seat-and-pot.html`, copied character for character: two rules, two keyframes,
  the rewritten comment, and *in either direction* on the comment above `.pile.stilled`. The card's
  own `@media (prefers-reduced-motion: reduce)` block is byte-unchanged.

- **Every `class="pile flying"` becomes a directed spelling**, chosen by which block it sits in and
  by nothing else:

  | Block | Spelling | Why |
  | --- | --- | --- |
  | `<div class="opp">` — the rival's seat, above the middle | `pile flying-up` | the pot is below it |
  | `<div class="hero">` — the viewer's seat, below the middle | `pile flying-down` | the pot is above it |

  When this starts, `TASK-141103` has merged, so the file holds **five** flying piles: the hero's in
  *Showdown — you win*, ImKate's in *Showdown — ImKate shows*, **both** in the split frame, and the
  hero's in the fold frame. Every one of them is a seat's stack pile, and the rule above decides
  each without reading the frame's heading.

- **One margin note.** The *Showdown — ImKate shows* frame's `<p class="note">` gains one sentence:
  the chips arrive from the pot below her seat, and the flight states nothing the banner and the
  numeral do not already state (`ADR-0115` §1).

## Out of scope

- **The pot row's pile and the bet lines.** No pile outside a `<div class="seat">` flies in this
  card, before or after; the pot receives from both seats and has no side.
- **Every frame's cards, figures, headings and every other note.** `ADR-0126` and `ADR-0095` §3 are
  `STORY-1411`'s and `STORY-1412`'s ground: no card is marked, dimmed, reordered or named here, and
  `check-frame-cards.sh` is in `verify:` to prove the rival's card slots did not move.
- **Adding or removing a frame.** The count stays whatever `TASK-141103` left, which is why no gate
  here counts frames or piles — the rule is gated instead of the total, on `TASK-141107`'s lesson.
- **The other two cards, and every client file.** `TASK-141301`, `TASK-141302`, `TASK-141304`.

## Tests

A card has no test runner; the gates are the `verify:` block plus the human's pane verdict, which
may trail the merge (`ADR-0091` §3).

**Measured at planning time** on the card as it stands **before** `TASK-141103` (three flying piles:
one in an `opp` block, two in `hero` blocks), change applied then reverted:

| Gate | Proves | Reading |
| --- | --- | --- |
| the side rule — every `pile flying-up` in an `opp` block, every `pile flying-down` in a `hero` block, at least one of each | the drawing, not a total: it survives `TASK-141103` adding two more piles, and it is what the human's arrow actually asks for | `up=1 down=2 bad=0`, exit **0** |
| **Mutation** — swap the two directions throughout | the gate depends on which side each pile is on | `up=0 down=0 bad=3`, exit **1**; the file is restored and `cmp`d before the gate reports |
| every `class="pile ` line names `flying-down`, `flying-up` or `stilled` | no pile kept an undirected modifier | green |
| `pile flying"` and `@keyframes pd-chip-flight ` absent | no undirected spelling survives | measured |
| `--pd-motion` **15**, `pd-chip-flight` **5**, `animation:` **3** | the CSS copy landed whole, and nothing gained a bare `animation:` shorthand — `TASK-141103` adds no motion CSS, so these three do not depend on it | 12 → 15, 3 → 5, 3 unchanged |
| the `@keyframes pd-chip-flight-up` block equals the component card's, and is non-empty | the copy is faithful rather than merely present | green |
| `./design/check-drift.sh`, `./design/check-frame-cards.sh` | tokens still resolve, and every gated frame's rival card slot is untouched | both green |

## Acceptance criteria

- [ ] Every `pile flying-up` in the file sits inside a `<div class="opp">` block and every
      `pile flying-down` inside a `<div class="hero">` block, with at least one of each
- [ ] Swapping the two directions makes that gate exit non-zero, and the file is restored
      byte-for-byte afterwards
- [ ] Every line carrying `class="pile ` names `flying-down`, `flying-up` or `stilled`
- [ ] The strings `pile flying"` and `@keyframes pd-chip-flight ` appear nowhere in the file
- [ ] `--pd-motion` is **15**, `pd-chip-flight` is **5**, `animation:` is **3**, and `transition`
      appears nowhere
- [ ] The `@keyframes pd-chip-flight-up` block is byte-identical to
      `design/components/seat-and-pot.html`'s, and the extraction comparing them is non-empty
- [ ] The *Showdown — ImKate shows* note says the chips arrive from the pot below her seat and that
      the flight states nothing the banner and the numeral do not
- [ ] `./design/check-drift.sh`, `./design/check-frame-cards.sh` and
      `python3 .github/scripts/lint_tickets.py` exit 0
- [ ] The diff touches exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
