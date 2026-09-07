---
schema: 2
id: TASK-141301
title: The component card draws both directions and the still frame they share
type: task
status: done
parent: STORY-1413
module: design
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [design, table]
depends_on: []
verify:
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - sh -c '! grep -qF "pile flying\"" design/components/seat-and-pot.html'
  - sh -c '! grep -qF "@keyframes pd-chip-flight " design/components/seat-and-pot.html'
  - awk 'index($0, "pile flying-down") { n++ } END { exit (n != 2) }' design/components/seat-and-pot.html
  - awk 'index($0, "pile flying-up") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "pile stilled") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"pile") { n++ } END { exit (n != 7) }' design/components/seat-and-pot.html
  - awk 'index($0, "--pd-motion") { n++ } END { exit (n != 15) }' design/components/seat-and-pot.html
  - awk 'index($0, "pd-chip-flight") { n++ } END { exit (n != 5) }' design/components/seat-and-pot.html
  - awk 'index($0, "animation:") { n++ } END { exit (n != 4) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"disc\"") { n++ } END { exit (n != 7) }' design/components/seat-and-pot.html
  - grep -qF 'translateY(calc(var(--pd-motion-chip-travel) * -1))' design/components/seat-and-pot.html
  - grep -qF 'translateY(var(--pd-motion-chip-travel))' design/components/seat-and-pot.html
  - grep -qF '@media (prefers-reduced-motion: reduce)' design/components/seat-and-pot.html
  - sh -c '! grep -q "transition" design/components/seat-and-pot.html'
  - sh -c '! grep -q "getBoundingClientRect" design/components/seat-and-pot.html'
  - awk 'index($0,"class=\"pile ")>0 && index($0,"flying-down")==0 && index($0,"flying-up")==0 && index($0,"stilled")==0 { bad++ } END { exit (bad+0 != 0) }' design/components/seat-and-pot.html
  - sh -c 'cp design/components/seat-and-pot.html "${TMPDIR:-/tmp}/sp.m1.html"; perl -0pi -e "s/pile flying-up/pile flying-down/" design/components/seat-and-pot.html; awk "index(\$0, \"pile flying-up\") { n++ } END { exit (n != 1) }" design/components/seat-and-pot.html; r=$?; cp "${TMPDIR:-/tmp}/sp.m1.html" design/components/seat-and-pot.html; cmp -s "${TMPDIR:-/tmp}/sp.m1.html" design/components/seat-and-pot.html && [ "$r" -ne 0 ]'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The pile's canonical card draws the flight in **both** directions — chips arriving from above at a
seat below the middle of the table, and from below at a seat above it — and the `stilled` row says
in words that it is the frame **both** of them end on. Nothing else about the pile changes: three
discs, the same tokens, the same 420 ms.

## Why this is a ticket

`ADR-0091` §2 puts the card first, and `ADR-0115` §4 says what a card that gives a surface motion
owes: it names what moves and the tokens that move it, and it **draws the surface at rest as a named
state**. Under `ADR-0115` §1 the still form is the thing that carries the fact, so a card that drew
only the flight would be drawing the garnish and not the statement.

`design/components/seat-and-pot.html` is where the pile's states are drawn once and named — its
*The chip pile* section already holds *chips at rest*, *the same pile at a hundredth of the stack*,
*chips in flight*, *stilled* and *a busted seat draws no chips*, and both screen cards say in their
own comments that their copy of this CSS is faithful to it. So the second direction is born here and
the two screen cards follow in `TASK-141302` and `TASK-141303`.

**No token is minted** (`ADR-0091` §3): `--pd-motion-chip-travel`, `--pd-motion-chip-flight` and
`--pd-motion-chip-ease` are the merged three and all three are reused, so this is composing and an
ordinary dispatched ticket rather than work done at the pane.

## Files

| File | Action |
| --- | --- |
| `design/components/seat-and-pot.html` | modify |

**Nothing else is opened.** `design/tokens/tokens.css` is not edited — no `--pd-` name is added, and
`check-drift.sh` would fail if one were. The two screen cards are the next two tickets'.

## Scope

- **The CSS block.** `.pile.flying` becomes **two** rules and `@keyframes pd-chip-flight` becomes
  **two** keyframes, with the comment rewritten to say what the pair is for. The whole replacement,
  in place of the four commented lines and the rule and keyframe under them:

  ```css
  /* pd-chip-flight-down and pd-chip-flight-up each run once, from an offset of
   * --pd-motion-chip-travel to transform: none — the frame the pile already
   * has at rest — so a flight always ends where a still pile sits and nothing
   * is ever animated out (ADR-0115 §1, §3, §6). They differ only in the side
   * the chips come from: a pile below the middle of the table takes the pot's
   * chips from above, a pile above it takes them from below. Neither states an
   * amount — the numeral beside the pile does. */
  .pile.flying-down { animation-name: pd-chip-flight-down; animation-duration: var(--pd-motion-chip-flight);
    animation-timing-function: var(--pd-motion-chip-ease); animation-fill-mode: both; }
  .pile.flying-up { animation-name: pd-chip-flight-up; animation-duration: var(--pd-motion-chip-flight);
    animation-timing-function: var(--pd-motion-chip-ease); animation-fill-mode: both; }
  @keyframes pd-chip-flight-down {
    from { transform: translateY(calc(var(--pd-motion-chip-travel) * -1)); }
    to { transform: none; }
  }
  @keyframes pd-chip-flight-up {
    from { transform: translateY(var(--pd-motion-chip-travel)); }
    to { transform: none; }
  }
  ```

  The `.pile.stilled { animation: none; }` rule and the card's own
  `@media (prefers-reduced-motion: reduce)` block are **byte-unchanged**; only the first line of the
  comment above `.pile.stilled` gains the words *in either direction*.

- **The specimen rows**, in *The chip pile*. The existing *chips in flight* row (ImKate, `4,550`)
  keeps its seat and its numeral, becomes `class="pile flying-up"`, and its label becomes:

  > the same flight to the seat above the middle — the pot is below it, so they
  > arrive from below; nothing else about the pile differs

  A **new** row is inserted immediately above it, drawn on `You` with `<span class="chips">14,625</span>`
  and `class="pile flying-down"`, labelled:

  > chips in flight to the seat below the middle — the pot is above it, so they
  > arrive from above; the numeral has already moved

  It is a copy of its neighbour with the name, the numeral and the direction changed — same
  `<div class="seat">`, same three `<span class="disc">`.

- **The `stilled` row's label** becomes:

  > stilled — either flight is skipped and the chips are where they land, which
  > is the same frame both of them end on

- **The pot strip's *the pot receiving* row** becomes `class="pile flying-down"` — the direction it
  already had — with its label extended to say why it keeps it:

  > the pot receiving — chips reach it from either seat, so it keeps the one
  > flight it has always had

## Out of scope

- **Both screen cards.** `TASK-141302` and `TASK-141303`, in that order, each one file.
- **Any client file.** `TASK-141304` transcribes this vocabulary into `app.css`; nothing under
  `web-client/` is opened here.
- **A new token, or a bigger travel.** The three merged `--pd-motion-chip-*` values are reused as
  they stand. Retuning `--pd-motion-chip-travel` is one value in the token sheet and is the human's
  (`ADR-0024` §5) — not this ticket's, and not any ticket's in `STORY-1413`.
- **A travel that crosses the table.** Refused in `STORY-1413`'s *Design notes* on `ADR-0115` §4
  grounds and named there rather than registered. Nothing here measures a distance.
- **Every other section of this card** — the seat plate, the clock, the marks, the pot amounts. Not
  a line moves outside *The chip pile*, its CSS block and the one pot row named above.

## Tests

A card has no test runner; its gates are the `verify:` block above and the human's eye at the pane
(`ADR-0024` §3), which may trail the merge (`ADR-0091` §3).

**Measured at planning time**, against the change applied and then reverted:

| Gate | Proves | Reading |
| --- | --- | --- |
| `pile flying-down` **2**, `pile flying-up` **1**, `pile stilled` **1** | both directions are drawn, and the still form with them | measured |
| `class="pile` **7**, `class="disc"` **7** | one row was added and every pile still holds three discs — 6 → 7 piles, 21 discs on 7 lines | measured |
| `--pd-motion` **15**, `pd-chip-flight` **5** | the pair of rules and the pair of keyframes are both there, and both read the tokens | 12 → 15, 3 → 5 |
| `animation:` **4** | nothing gained a bare `animation:` shorthand — the two new rules use `animation-name:` and the stills are the three that were already there | unchanged |
| `pile flying"` and `@keyframes pd-chip-flight ` absent | no undirected spelling survives anywhere | measured |
| `! grep transition`, `! grep getBoundingClientRect` | the card proposes no second motion mechanism and no measured travel | both absent today |
| `./design/check-drift.sh` | every `--pd-*` the card names is declared in the sheet with the same value | green |
| **Mutation** — `pile flying-up` → `pile flying-down` throughout | the direction gate depends on the drawing rather than on the file existing | the `flying-up` count gate must exit **non-zero**; the file is restored and `cmp`d before the gate reports |

## Acceptance criteria

- [ ] `design/components/seat-and-pot.html` holds `pile flying-down` **2** times, `pile flying-up`
      **1** time and `pile stilled` **1** time, and the string `pile flying"` nowhere
- [ ] `class="pile` is **7** and `class="disc"` is **7** — one specimen row added, three discs each
- [ ] `--pd-motion` is **15**, `pd-chip-flight` is **5** and `animation:` is **4**
- [ ] Both keyframe offsets are present as literals:
      `translateY(calc(var(--pd-motion-chip-travel) * -1))` and `translateY(var(--pd-motion-chip-travel))`
- [ ] The card still carries its own `@media (prefers-reduced-motion: reduce)` block, and contains
      no `transition` and no `getBoundingClientRect`
- [ ] Every line carrying `class="pile ` names `flying-down`, `flying-up` or `stilled`
- [ ] The `stilled` row's label says the still frame is the one **both** flights end on
- [ ] The mutation gate exits 0: swapping `flying-up` to `flying-down` makes the direction count
      fail, and the file is restored byte-for-byte
- [ ] `./design/check-drift.sh`, `./design/check-frame-cards.sh` and
      `python3 .github/scripts/lint_tickets.py` exit 0
- [ ] The diff touches exactly one file
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
