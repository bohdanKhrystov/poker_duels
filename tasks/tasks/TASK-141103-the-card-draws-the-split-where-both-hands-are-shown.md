---
schema: 2
id: TASK-141103
title: The card draws the split where both hands are shown
type: task
status: done
parent: STORY-1411
module: design
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [design, table]
depends_on: [TASK-141102]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "Showdown — a split pot") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"tbl\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"opp\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"hero\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"board\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"oppcards\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"hole\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"meta\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"bet-line\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"note\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"last-act\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"dealer\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"bar off\"") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"who\"") { n++ } END { exit (n != 10) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"status\"") { n++ } END { exit (n != 9) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"timebank\"") { n++ } END { exit (n != 10) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"chips\"") { n++ } END { exit (n != 10) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pile") { n++ } END { exit (n != 11) }' design/screens/duel-table-states.html
  - awk 'index($0, "pile flying") { n++ } END { exit (n != 5) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc") { n++ } END { exit (n != 38) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc\"") { n++ } END { exit (n != 21) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc red\"") { n++ } END { exit (n != 17) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"back") { n++ } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk 'index($0, "back mucked") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:40px\"") { n++ } END { exit (n != 10) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:96px\"") { n++ } END { exit (n != 10) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:var(--bw)\"") { n++ } END { exit (n != 25) }' design/screens/duel-table-states.html
  - awk 'index($0, "aria-label=\"ImKate") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"amount win\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "Split pot — you win 2,425") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "Your rival wins 4,850") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "Blinds 75/150 · Hand 14 · Hand complete") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "Dealing hand 15") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "5,375") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "14,625") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "12,200") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0095") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0120") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0126") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0136") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "animation:") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "--pd-motion") { n++ } END { exit (n != 12) }' design/screens/duel-table-states.html
  - sh -c '! grep -qF "transition" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "pc mini" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "data-" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "title=" design/screens/duel-table-states.html'
  - sh -c '! grep -qiE "two pair|full house|jacks full|full of|flush|straight|trips|quads|three of a kind|high card" design/screens/duel-table-states.html'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table-states.html` gains a **fifth** frame and finishes `ADR-0120` §1's table:
a split pot, **both** hands face up, and the banner stating the **viewer's own share** — `Split pot —
you win 2,425`. With this the card draws every ending the rules produce: a hand shown, both hands
shown, and a fold where none is.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |

Read [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§§1–3, [`ADR-0095`](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
§§2–3, and [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md)
§1. **Nothing else is opened and nothing else is changed** — no other card under `design/`, no file
under `web-client/`, and no line of this file's stylesheet.

## Scope

**Copy the frame `TASK-141102` added** — `Showdown — ImKate shows, and the beat stands two seconds` —
whole, place the copy directly after it, and change only what is listed below. Copy rather than type:
the ranks and suits you need are already in the file, and every glyph carries a variation selector
`check-drift.sh` checks.

- **The heading** becomes, exactly:

  ```html
      <h2>Showdown — a split pot, both hands shown</h2>
  ```

- **ImKate's two shown cards become `A♣` and `K♦`**, so that both players play the same five. The
  board is `A♠ 7♦ 2♣ J♥ 7♣` and the hero holds `A♥ K♠`; `A♣` and `K♦` are free, and with them the
  two hands are indistinguishable rank for rank. Change only the rank character, the suit glyph, the
  suit colour class and the `aria-label` — `A♣` is `class="pc"` and `K♦` is `class="pc red"`, which
  is why the black/red gates move by one each. Neither label names ImKate (`ADR-0126` §1); a gate
  pins `aria-label="ImKate` at **3**.

- **The banner states the viewer's own share** — `ADR-0095` §2's third line, the client's own literal
  from `PotStrip.tsx`'s `awardLineFor`:

  ```html
              <span class="amount win">Split pot — you win 2,425</span>
  ```

  The `.meta` beneath it is unchanged.

- **Both stacks take a share, and both piles fly.** Before the award the stacks are ImKate 2,950 and
  You 12,200; 4,850 splits into two shares of 2,425 with **no odd chip**, so:

  - ImKate's `<span class="chips">` reads `5,375` and her pile stays `<span class="pile flying">`.
  - Your `<span class="chips">` reads `14,625` and your `<span class="pile">` becomes
    `<span class="pile flying">` too — both seats are paid.

  5,375 + 14,625 = 20,000, and gates pin both figures and the `pile flying` count at **5**.

- **Everything else in the frame is the copy, unaltered:** the board's five cards, the hero's hand at
  96 px, both plates' structure, ImKate's `Call` mark and dealer button, the empty bet line, both
  timebanks, and the disabled bar with `Dealing hand 15`.

- **The note is one `<p class="note">`** citing `ADR-0120` **once**, `ADR-0095` **once** and
  `ADR-0126` **once** — the file-wide gates then read `ADR-0120` **3**, `ADR-0095` **3**,
  `ADR-0126` **2** and `ADR-0136` **1** (`ADR-0136` is cited only in the frame above; do not repeat
  it). It must say:

  1. **Both hands are shown because the rules showed both** — `ADR-0120` §1's third row, *in reveal
     order*, which on this table is simply each hand in its own seat. Nothing is disclosed that the
     rules withheld, here or anywhere.
  2. **The banner states the reader's own share and never the rival's** (`ADR-0095` §2). It is
     2,425 here because 4,850 halves evenly; `docs/duel-rules.md` sends an odd chip to the player out
     of position, so the two shares can differ by one and stating the reader's own is what keeps the
     line true.
  3. **This ending holds too.** The viewer had not seen the rival's hand before, so `ADR-0120` §3's
     predicate holds and this is a beat that stands rather than a step — the same still two seconds
     the frame above draws, with nothing entering and nothing fading.
  4. **No card is picked out** (`ADR-0126` §1) — and a split is the case that makes the point
     hardest to argue with: **both** winners' cards are drawn exactly as every other card on the
     table.

  **The note names no hand**, and on a split it will be especially tempting to explain why the pot
  divided. `ADR-0095` §3 holds in the margin as on the screen; a `verify:` gate refuses the
  vocabulary.

- **The lede gains the split.** One clause, no more.

## Out of scope

- **Marking the winning five.** `ADR-0126` answered `DEC-134` **no**, and §1 names a split explicitly
  — *"for **both** winners of a split pot, the five cards that made the winning hand are drawn
  exactly as the two that did not."* Gates pin `class="pc"` at **21** and `class="pc red"` at **17**,
  summing to the **38** total, so a third class on any card fails; `data-` and `title=` stay absent.
- **Any motion.** `animation:` stays **3**, `--pd-motion` **12**, `transition` absent, and
  `design/tokens/tokens.css` is not opened.
- **The odd chip.** This frame is deliberately built on an **even** pot so no odd chip arises; the
  note says the rule and the frame does not draw the case. Drawing a one-chip difference is a
  different frame nobody has asked for.
- **The pot's travel.** Both piles carry the `flying` treatment copied from their neighbours;
  `EPIC-14` item 2c and `STORY-1413` own the flight itself, under `ADR-0115`.
- **The other four frames.** Untouched — gates pin `Your rival wins 4,850` at **1**, `12,200` at
  **2**, `class="back"` at **6** and `back mucked` at **4**.
- **Every client source file.** `awardLineFor` already builds this exact string for a two-award hand
  and already refuses to state the rival's share. There is nothing in `web-client/` for this ticket
  to do.
- **The all-in-and-called reveal and a voluntary show.** `ADR-0120` §5 names both and answers
  neither.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. Every count is stated
as the number **after** `TASK-141101`, `TASK-141102` and this ticket have all landed.

| Gate | Proves |
| --- | --- |
| `class="frame"` **5**, `<h2>` **5**, `Showdown — a split pot` **1** | one frame added, and only one |
| the twelve per-frame slot classes each **5**; `class="who"`, `class="timebank"`, `class="chips"` each **10**; `class="status"` **9** | the frame is the whole table, with every slot its neighbours have |
| `class="pc` **38**, of which `class="pc"` **21** and `class="pc red"` **17** | nine face-up cards added, one of the rival's black and one red, and **no card carries a third class** |
| `class="back"` **6**, `back mucked` **4** | no back was added or taken away by this ticket |
| `style="--w:40px"` **10**, `style="--w:96px"` **10**, `style="--w:var(--bw)"` **25** | both shown hands at their own seats' widths |
| `aria-label="ImKate` **3** | a face says rank and suit and nothing else |
| `class="amount win"` **4**, `Split pot — you win 2,425` **1**, `Your rival wins 4,850` **1** | `ADR-0095` §2's third line, once, and the second line untouched |
| `Blinds 75/150 · Hand 14 · Hand complete` **4**, `Dealing hand 15` **4** | the facts line and the bar came with the copy |
| `5,375` **1**, `14,625` **1**, `12,200` **2** | both shares were paid and the table still holds 20,000 |
| `class="pile` **11**, `pile flying` **5** | both seats are paid, and the frame above kept its one |
| `ADR-0095` **3**, `ADR-0120` **3**, `ADR-0126` **2**, `ADR-0136` **1** | the note cites what it is drawn from and does not repeat the mechanism ADR |
| `animation:` **3**, `--pd-motion` **12**, `transition` absent | nothing here moves |
| `pc mini`, `data-`, `title=` absent | no mini variant, no keyable attribute on a card |
| no `two pair`/`full house`/`full of`/`flush`/`straight`/`trips`/`quads`/`three of a kind`/`high card`, case-insensitive | `ADR-0095` §3, in the frame and in its margin |
| `./design/check-drift.sh` exits 0 | tokens, values, suits (U+FE0E) and the lockup hold across the new cards |

## Acceptance criteria

- [ ] `design/screens/duel-table-states.html` holds **5** `class="frame"` nodes and **5** `<h2>`
      headings, exactly one containing `Showdown — a split pot`
- [ ] The new frame draws **both** hands face up: ImKate's two at `--w:40px` and the hero's two at
      `--w:96px`, with `class="back` still exactly **6** and `back mucked` exactly **4**
- [ ] `class="pc` is **38**, `class="pc"` is **21** and `class="pc red"` is **17**
- [ ] `aria-label="ImKate` is **3**
- [ ] The banner reads `Split pot — you win 2,425` exactly once, and `Your rival wins 4,850` still
      appears exactly once
- [ ] ImKate's stack reads `5,375`, the hero's reads `14,625`, both carry `pile flying`, and
      `pile flying` totals **5** with `class="pile` at **11**
- [ ] The note states all four points in *Scope*; file totals are `ADR-0095` **3**, `ADR-0120` **3**,
      `ADR-0126` **2**, `ADR-0136` **1**
- [ ] `pc mini`, `data-`, `title=` and `transition` appear nowhere; `animation:` is **3** and
      `--pd-motion` is **12**
- [ ] No hand is named anywhere in the file, by the refused vocabulary the `verify:` block lists
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly one file, `design/screens/duel-table-states.html`
- [ ] Every command in `verify:` exits 0

> The human's visual verdict on the rendered card is the design review (`ADR-0024` §3) and it may
> **trail the merge** (`ADR-0091` §3).

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
