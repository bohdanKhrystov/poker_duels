---
schema: 2
id: TASK-141302
title: The duel table card's copy of the flight follows the component card
type: task
status: backlog
parent: STORY-1413
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, table]
depends_on: [TASK-141301]
verify:
  - ./design/check-drift.sh
  - sh -c '! grep -qF "pile flying" design/screens/duel-table.html'
  - sh -c '! grep -qF "@keyframes pd-chip-flight " design/screens/duel-table.html'
  - awk 'index($0, "--pd-motion") { n++ } END { exit (n != 15) }' design/screens/duel-table.html
  - awk 'index($0, "pd-chip-flight") { n++ } END { exit (n != 5) }' design/screens/duel-table.html
  - awk 'index($0, "animation:") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"pile") { n++ } END { exit (n != 12) }' design/screens/duel-table.html
  - grep -qF '.pile.flying-down { animation-name: pd-chip-flight-down; animation-duration: var(--pd-motion-chip-flight);' design/screens/duel-table.html
  - grep -qF '.pile.flying-up { animation-name: pd-chip-flight-up; animation-duration: var(--pd-motion-chip-flight);' design/screens/duel-table.html
  - grep -qF 'translateY(calc(var(--pd-motion-chip-travel) * -1))' design/screens/duel-table.html
  - grep -qF 'translateY(var(--pd-motion-chip-travel))' design/screens/duel-table.html
  - sh -c '! grep -q "transition" design/screens/duel-table.html'
  - sh -c 'a=$(sed -n "/@keyframes pd-chip-flight-down/,/^  }$/p" design/components/seat-and-pot.html); b=$(sed -n "/@keyframes pd-chip-flight-down/,/^  }$/p" design/screens/duel-table.html); [ -n "$a" ] && [ "$a" = "$b" ]'
  - sh -c 'a=$(sed -n "/@keyframes pd-chip-flight-up/,/^  }$/p" design/components/seat-and-pot.html); b=$(sed -n "/@keyframes pd-chip-flight-up/,/^  }$/p" design/screens/duel-table.html); [ -n "$a" ] && [ "$a" = "$b" ]'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html`'s copy of the chip-flight CSS says the same thing the component
card now says: two directions, two keyframes, one still form. The card draws no flying pile and
gains none — only its copy of the block moves.

## Why this is a ticket

The card's own comment calls this block a **faithful copy of `components/seat-and-pot.html`**, and
it is: the same rule, the same keyframe, the same comment, character for character. `TASK-141301`
changed the original, so the copy is stale the moment that merges — a card holding vocabulary the
component card no longer has is exactly the drift `check-drift.sh` exists to catch in the token
dimension and cannot see in this one.

It is its own ticket because it is its own file, and `ADR-0091` §3 sizes a design ticket as one
card, one file.

**The card uses none of it.** Measured: `pile flying` appears **0** times in this card and
`pile stilled` **0** — all twelve of its piles are drawn at rest. The copy is of the block, not of
the used subset, which is why the file still moves.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |

**Nothing else is opened.** `design/components/seat-and-pot.html` is already right when this starts;
`design/screens/duel-table-states.html` is `TASK-141303`'s.

## Scope

- **The CSS block only.** Replace the commented `.pile.flying` rule and the `@keyframes pd-chip-flight`
  block with the two rules, two keyframes and rewritten comment that `TASK-141301` merged into
  `design/components/seat-and-pot.html`, copied **character for character**, including the added
  words *in either direction* on the comment above `.pile.stilled`.
- **`.pile.stilled { animation: none; }` and the card's `@media (prefers-reduced-motion: reduce)`
  block are byte-unchanged.**

## Out of scope

- **Every `<span class="pile">` in the card.** All twelve stay exactly as they are: no pile in this
  card flies, before or after.
- **Any other CSS block, any markup, any copy.** The seat plates, the board, the bar, the bet lines
  and every figure are untouched.
- **`design/screens/duel-table-states.html`.** `TASK-141303`.
- **Any client file.** `TASK-141304`.

## Tests

A card has no test runner; the gates are the `verify:` block, and two of them compare this card's
keyframes to the component card's **text** rather than to a remembered spelling — so a copy that
drifts by a character fails.

**Measured at planning time**, change applied then reverted:

| Gate | Proves | Reading |
| --- | --- | --- |
| `--pd-motion` **15**, `pd-chip-flight` **5** | both rules and both keyframes landed, reading the tokens | 12 → 15, 3 → 5 |
| `animation:` **3**, `class="pile` **12** | no bare `animation:` shorthand was added, and no pile was added, removed or set flying | both unchanged |
| `pile flying` and `@keyframes pd-chip-flight ` absent | no undirected spelling survives | measured |
| the two `sed`-extracted keyframe blocks equal the component card's, and are non-empty | the copy is faithful, not merely present — an empty extraction would otherwise compare equal to an empty extraction | green with the change, and the non-empty test is what makes it non-vacuous |
| `./design/check-drift.sh` | every `--pd-*` name and value still matches the sheet | green |

## Acceptance criteria

- [ ] `design/screens/duel-table.html` contains neither `pile flying` nor `@keyframes pd-chip-flight `
- [ ] `--pd-motion` is **15** and `pd-chip-flight` is **5**
- [ ] `animation:` is **3** and `class="pile` is **12** — both unchanged
- [ ] Both `.pile.flying-down` and `.pile.flying-up` opening lines are present as literals
- [ ] Both keyframe blocks are byte-identical to `design/components/seat-and-pot.html`'s, and the
      extraction that compares them is non-empty
- [ ] `./design/check-drift.sh` and `python3 .github/scripts/lint_tickets.py` exit 0
- [ ] The diff touches exactly one file, and every changed line is inside the chip-pile CSS block
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
