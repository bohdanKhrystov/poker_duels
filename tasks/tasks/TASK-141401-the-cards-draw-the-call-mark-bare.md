---
schema: 2
id: TASK-141401
title: The cards draw the call mark bare
type: task
status: ready
parent: STORY-1414
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [design, table]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"last-act\">Call</span>") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"last-act\">Call</span>") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "Call&nbsp;") { n++ } END { exit (n != 0) }' design/screens/duel-table-states.html
  - awk 'index($0, "Call&nbsp;") { n++ } END { exit (n != 0) }' design/components/seat-and-pot.html
  - awk 'index($0, "last act — call, bare") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "the total the server sent") { n++ } END { exit (n != 3) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"last-act\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"last-act\"") { n++ } END { exit (n != 8) }' design/components/seat-and-pot.html
  - awk 'index($0, "class=\"last-act\"") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "bare for three of the six acts") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "bare for three of the six acts") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "bare for three of the six acts") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - sh -c '! grep -rqF "bare for two of the six acts" design'
  - awk 'index($0, "ADR-0122") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0122") { n++ } END { exit (n != 1) }' design/components/seat-and-pot.html
  - awk 'index($0, "ADR-0122") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Call <span class=\"amt\">400</span>") { n++ } END { exit (n != 5) }' design/components/action-bar.html
  - awk 'index($0, "Call <span class=\"amt\">400</span>") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every card that draws the last-act mark for a **call** draws it bare — `Call`, no figure — and the
three copies of the mark's own comment say that three of the six acts are bare, not two. No card
node that draws a `Call` **button** moves.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |
| `design/components/seat-and-pot.html` | modify |
| `design/screens/duel-table.html` | modify |

Read [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) §§4
and 7 — §4 is why the mark loses its figure, §7 is the measurement this ticket extends. **Nothing
else is opened**, and no file outside the three above is changed.

## Scope

- **`design/screens/duel-table-states.html:298`** — the showdown frame's rival plate. The node
  becomes exactly:

  ```html
            <span class="last-act">Call</span>
  ```

  The `&nbsp;800` goes with the figure. The frame's other two marks (`Check` at `:271`, `Fold` at
  `:358`) are untouched, and so is every other line of that frame — the mucked backs, `You win
  4,850`, `Two pair, aces and sevens`, the board and the plate's `2,950`.
- **`design/components/seat-and-pot.html:252`** — the same node in the mark's own inventory:

  ```html
        <span class="last-act">Call</span>
  ```

  and its caption at `:256` becomes `<span class="l">last act — call, bare</span>`, matching the
  fold's and the check's captions two rows above it. The bet, raise and all-in rows keep both their
  figures and their *the total the server sent* captions — the `verify:` block pins that at **three**
  remaining captions, so removing a fourth fails.
- **The `.last-act` CSS comment, in all three files.** It is byte-identical in
  `design/screens/duel-table-states.html:81-86`, `design/components/seat-and-pot.html:65-70` and
  `design/screens/duel-table.html:105-110` today, and must be byte-identical in all three after this
  ticket. Replace the whole comment with:

  ```css
  /* the last act: the plate's mark for the most recent act of the hand, at the seat that
   * made it — one mark, never one per seat (ADR-0109 §1). It says the actor's own button
   * word, bare for three of the six acts and with the event's own total for the other
   * three (§2 as ADR-0122 §4 amends it: the call joined the bare three when its button
   * began naming a price PlayerCalled cannot carry); nothing here ever times out, fades
   * or otherwise dismisses it (§4). It rides as another child of .seat's own flex row, so
   * .who gives ground by truncating the name first and the plate never grows a pixel
   * taller. */
  ```

  Keep each file's existing leading indentation for the block (two spaces before `/*` in all three).
  `duel-table.html` gets this comment change and **nothing else**.
- **The `.last-act` CSS rule itself does not change** in any of the three files — same font, same
  pill, same `white-space: nowrap`. A bare mark is a shorter pill, not a different one.

## Out of scope

- **Every `Call` button node.** `design/screens/duel-table.html` draws `Call 400` in three frames
  and `design/components/action-bar.html` in five, all in `ADR-0101` §4's worked frame where the hero
  has committed nothing — so 400 is the price as well and `ADR-0122` §7 says outright that no node
  moves. Two `verify:` commands pin those counts at **3** and **5**; if you change one, this ticket
  goes red.
- **`design/components/action-bar.html` entirely.** It is not in the Files table and it draws no
  last-act mark.
- **Any client source file.** `SeatPlate.tsx` still prints the figure after this ticket merges, and
  `TASK-141404` is what removes it. A card ahead of its client is `ADR-0091` §2's *"before or with"*,
  and it is deliberate here.
- **Adding a new state or frame to any card.** `ADR-0122` §7: no new surface and no new state is
  drawn, so `ADR-0091` §2 owes no new card. This is four existing nodes corrected, not a card.
- **`docs/adr/ADR-0109`'s own text** — `TASK-141405`.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. It is written as
**exact counts** in the style `TASK-130207` established, so that a node added, a node removed and a
node edited are three different failures:

| Gate | Proves |
| --- | --- |
| `class="last-act">Call</span>` is 1 in each of the two cards | the bare node exists, spelled exactly |
| `Call&nbsp;` is 0 in each of the two cards | no call mark anywhere keeps a figure |
| `class="last-act"` is 3, 8 and 3 | no mark node was added or deleted while editing one |
| `last act — call, bare` is 1 and `the total the server sent` is 3 | the caption followed the node, and the other three captions did not |
| `bare for three of the six acts` is 1 in each of the three cards, and `bare for two` is absent from all of `design/` | the comment was corrected in all three copies, not one |
| `ADR-0122` is 1 in each of the three cards | the comment cites the amending decision, once |
| `Call <span class="amt">400</span>` is 5 and 3 | no button node moved |
| `./design/check-drift.sh` | tokens, values, suits, symbols and the lockup still hold (`ADR-0024` §2) |

## Acceptance criteria

- [ ] `design/screens/duel-table-states.html` contains `<span class="last-act">Call</span>` exactly
      once and `Call&nbsp;` zero times
- [ ] `design/components/seat-and-pot.html` contains `<span class="last-act">Call</span>` exactly
      once, `Call&nbsp;` zero times, and the caption `last act — call, bare` exactly once
- [ ] All three cards contain `bare for three of the six acts` exactly once and cite `ADR-0122`
      exactly once, and no file under `design/` contains `bare for two of the six acts`
- [ ] The `.last-act` node counts are 3, 8 and 3, and the `Call` button node counts are 5 and 3 —
      unchanged from `develop`
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly three files, all under `design/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
