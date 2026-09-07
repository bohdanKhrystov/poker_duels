---
schema: 2
id: TASK-141102
title: The card draws the showdown the viewer lost
type: task
status: done
parent: STORY-1411
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table]
depends_on: [TASK-141101]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "Showdown — ImKate shows") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"tbl\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"opp\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"hero\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"board\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"oppcards\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"hole\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"meta\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"bet-line\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"note\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"last-act\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"dealer\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"who\"") { n++ } END { exit (n != 8) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"status\"") { n++ } END { exit (n != 7) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"timebank\"") { n++ } END { exit (n != 8) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"chips\"") { n++ } END { exit (n != 8) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"bar off\"") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pile") { n++ } END { exit (n != 9) }' design/screens/duel-table-states.html
  - awk 'index($0, "pile flying") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc") { n++ } END { exit (n != 29) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc\"") { n++ } END { exit (n != 16) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc red\"") { n++ } END { exit (n != 13) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"back") { n++ } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk 'index($0, "back mucked") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:40px\"") { n++ } END { exit (n != 8) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:96px\"") { n++ } END { exit (n != 8) }' design/screens/duel-table-states.html
  - awk 'index($0, "style=\"--w:var(--bw)\"") { n++ } END { exit (n != 20) }' design/screens/duel-table-states.html
  - awk 'index($0, "aria-label=\"ImKate") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"amount win\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "Your rival wins 4,850") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "Blinds 75/150 · Hand 14 · Hand complete") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "Dealing hand 15") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "7,800") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "12,200") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0120") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0126") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
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

`design/screens/duel-table-states.html` gains a **fourth** frame — the showdown the viewer **lost**,
with ImKate's two cards **face up in her own seat** and the banner reading `Your rival wins 4,850`.
It is the state this card has never had, the one `ADR-0120` §3's 2,000 ms beat exists for, and the
one the human was looking at when they wrote *"if we went to showdown villan card shoud be show"*.
No card in it is marked and nothing in it moves.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |

Read [`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§§1–3, [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md) §1
and §4, and
[`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
§6. **Nothing else is opened and nothing else is changed** — no other card under `design/`, no file
under `web-client/`, and no line of this file's stylesheet.

## Scope

**Build the new frame by copying the existing showdown frame whole** (`Showdown — you win, the loser
mucks`, `class="frame"` at `:293` after `TASK-141101`) and changing only what is listed below. Copy
rather than type: every glyph in this card is a variation selector away from failing
`check-drift.sh`, and the ranks and suits you need are already in the file.

Insert the new frame **between** the existing showdown frame and the fold frame, so the card reads
wait → showdown won → showdown lost → fold.

- **The heading** becomes, exactly:

  ```html
      <h2>Showdown — ImKate shows, and the beat stands two seconds</h2>
  ```

- **ImKate's hand is face up.** The two `<span class="back mucked" style="--w:40px" …>` nodes become
  two **face-up** cards, `J♠` and `J♦`, drawn with the same markup as every other face-up card in
  this file and at the same 40 px the backs they replace used:

  ```html
            <span class="pc" style="--w:40px" role="img" aria-label="jack of spades">…</span>
            <span class="pc red" style="--w:40px" role="img" aria-label="jack of diamonds">…</span>
  ```

  Take the inner `<span class="ix">…</span><span class="pip">…</span>` structure from an existing
  face-up card and change only the rank character, the suit glyph and the `aria-label`. Both cards
  are free: the board is `A♠ 7♦ 2♣ J♥ 7♣` and the hero holds `A♥ K♠`.

  **Not `class="pc mini"`.** That rule is inlined at `:182` and used by nothing, and
  `design/components/playing-card.html:81` draws its 40 px reference with it — but
  `web-client/src/table/PlayingCard.tsx`'s `CardFace` has **no** mini variant: one `SHELL` constant,
  with the corner index and the pip both sized as fractions of `--w` at every width. The client has
  never drawn a face-up card at the rival's width because until this story none was ever shown
  there. This card draws what the client draws; a card that specified the mini would be specifying a
  client change nobody has asked for. Say so in the note, and a `verify:` gate keeps `pc mini` out of
  the file.

  **Neither card carries `ImKate` in its label.** A back says whose hidden hand it is because that is
  all it can say; a face says its rank and its suit, exactly as the hero's two and the board's five
  do. `ADR-0126` §1 forbids *"anything in an `aria-label` beyond the rank and suit"*, and a gate pins
  `aria-label="ImKate` at **3** — the three backs in the other frames.

- **The bet line stays empty.** `<div class="bet-line"></div>`, as `TASK-141101` left the frame you
  copied. No line says who showed, who mucked or who won; the banner says who won and the cards say
  the rest.

- **The banner names the rival** — `ADR-0095` §2's second line, the client's own literal from
  `PotStrip.tsx`'s `awardLineFor`:

  ```html
              <span class="amount win">Your rival wins 4,850</span>
  ```

  The `.meta` beneath it keeps the facts line `TASK-141101` put there, byte-identical.

- **The stacks and the chip pile change hands.** The wait frame's 3,750 + 13,000 + 3,250 = 20,000
  still has to hold. Both seats put 800 in on the river, so the pot is 4,850 and the stacks before
  the award are ImKate 2,950 and You 12,200. ImKate takes it:

  - ImKate's `<span class="chips">` reads `7,800` (2,950 + 4,850), and her `<span class="pile">`
    becomes `<span class="pile flying">` — the chips arrive at the winner, which is her.
  - Your `<span class="chips">` reads `12,200`, and your pile is the ordinary
    `<span class="pile">` — you receive nothing.

  7,800 + 12,200 = 20,000, and gates pin both figures.

- **Everything else in the frame is the copy, unaltered:** the board's five cards, the hero's `A♥ K♠`
  at 96 px, both seat plates' structure, ImKate's `Call` mark and dealer button, both timebanks, and
  the disabled bar with `Dealing hand 15`.

- **The note is one `<p class="note">`** and it carries four things, citing `ADR-0120` **once**,
  `ADR-0126` **once** and `ADR-0136` **once** (the `ADR-0120` gate expects **2** in the file: one
  here and the one `TASK-141101` left in the frame above). It must say, in the card's own voice:

  1. **The faces are already on the table.** They come from the snapshot's `SeatView.holeCards`,
     which the engine's projection layer populated for a revealed seat — `ADR-0120` §2. Nothing new
     is drawn here; what changed is how long it stands.
  2. **This is the beat that stands 2,000 ms** (`ADR-0120` §3), and the hold is **time, not
     entrance**: the hand is already on screen the instant the beat begins, nothing fades in and
     nothing scales, and the beat ends when the next hand's frames are applied — a fact arriving,
     never a timer taking something away (`ADR-0095` §4, `ADR-0136` §6).
  3. **No card is picked out** (`ADR-0126` §1): the cards that made the winning hand are drawn
     exactly as the ones that did not, in either seat and on the board.
  4. **The rival's hand is drawn at 40 px as a plain `.pc`**, matching `PlayingCard.tsx`, and the
     unused `.pc.mini` rule is deliberately not reached.

  **The note names no hand.** `ADR-0095` §3 holds everywhere on this table, and a margin note that
  spelled out what beat what would put the string one copy-paste from the screen. A `verify:` gate
  refuses the vocabulary.

- **The lede gains the new ending.** It currently describes *"the river's two endings"*; there are
  three now. One clause, no more.

## Out of scope

- **The split.** `TASK-141103` adds the fifth frame; a gate pins `class="frame"` at **4** here.
- **Marking the winning five.** `ADR-0126` answered `DEC-134` **no**. No ring, border, glow, tint,
  shadow, lift, scale, opacity, reordering, badge, connector, extra class, `data-*` or `title`
  appears on any card — gates pin `class="pc"` at **16** and `class="pc red"` at **13**, summing to
  the **29** total, so a third class on any card fails; and `data-` and `title=` are pinned absent
  across the file, where they are absent today.
- **Any motion.** `animation:` stays at **3**, `--pd-motion` at **12**, `transition` absent, and
  `design/tokens/tokens.css` is not opened. `ADR-0136` §6: the hold has **no CSS**, so
  `prefers-reduced-motion` has nothing here to still and this frame owes no reduced-motion twin.
- **The `pile flying` treatment itself.** It is copied from the frame beside it and moved to the seat
  that won. Whether and how the pot travels is `EPIC-14` item 2c and `STORY-1413`'s, under
  `ADR-0115`; nothing about the flight is decided, added or changed here.
- **The other three frames.** The wait, the won showdown and the fold are untouched — gates pin
  `class="back"` at **6**, `back mucked` at **4** and `Blinds 75/150 · Hand 14 · Hand complete` at
  **3**.
- **Every client source file.** `DuelTable.tsx` already draws `cards={rival.holeCards}` and `Hand.tsx`
  already draws a place the view carries a card for face up. There is nothing in `web-client/` for
  this ticket to do, and `CardFace` growing a mini variant is not ticketed anywhere.
- **The all-in-and-called reveal and a voluntary show.** `ADR-0120` §5 names both and answers
  neither; no frame here draws either.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. Every count below
was measured on this file on `develop` at `c54b4a1f` and is stated as the number **after**
`TASK-141101` and this ticket have both landed.

| Gate | Proves |
| --- | --- |
| `class="frame"` **4**, `<h2>` **4**, `Showdown — ImKate shows` **1** | one frame added, added rather than renamed, and only one |
| `class="tbl"`, `class="opp"`, `class="hero"`, `class="board"`, `class="oppcards"`, `class="hole"`, `class="meta"`, `class="bet-line"`, `class="note"`, `class="last-act"`, `class="dealer"`, `class="bar off"` each **4** | the frame is the whole table, with every slot the others have and none twice |
| `class="who"`, `class="timebank"`, `class="chips"` each **8**; `class="status"` **7** | two seat plates, structurally identical to their neighbours' |
| `class="pc` **29**, of which `class="pc"` **16** and `class="pc red"` **13** | nine face-up cards added — five board, two hero, **two rival** — and **no card carries a third class** |
| `class="back"` **6** and `back mucked` **4** | the rival's two backs became faces; nobody else's did |
| `style="--w:40px"` **8**, `style="--w:96px"` **8**, `style="--w:var(--bw)"` **20** | the shown hand is at the rival's own width, the hero's at the hero's, the board's at the board's |
| `aria-label="ImKate` **3** | a face says rank and suit and nothing else (`ADR-0126` §1) |
| `class="amount win"` **3**, `Your rival wins 4,850` **1**, `Blinds 75/150 · Hand 14 · Hand complete` **3** | `ADR-0095` §2's second line, once, over the untouched facts line |
| `7,800` **1**, `12,200` **2**, `Dealing hand 15` **3** | the chips went to the winner and the table still holds 20,000 |
| `class="pile` **9**, `pile flying` **3** | one ordinary pile and one flying pile added, and the flying one is the winner's |
| `ADR-0120` **2**, `ADR-0126` **1**, `ADR-0136` **1** | the note cites the decision it is drawn from, once each |
| `animation:` **3**, `--pd-motion` **12**, `transition` absent | nothing here moves, and reduced motion has nothing to reach |
| `pc mini`, `data-`, `title=` absent | no mini variant the client cannot draw, and no keyable attribute on a card |
| no `two pair`/`full house`/`full of`/`flush`/`straight`/`trips`/`quads`/`three of a kind`/`high card`, case-insensitive | `ADR-0095` §3 — no hand is named, in the frame or in its margin |
| `./design/check-drift.sh` exits 0 | tokens, values, suits (U+FE0E) and the lockup hold across the new cards |

## Acceptance criteria

- [ ] `design/screens/duel-table-states.html` holds **4** `class="frame"` nodes and **4** `<h2>`
      headings, exactly one of which contains `Showdown — ImKate shows`
- [ ] The new frame draws ImKate's two cards **face up** at `--w:40px` as `class="pc"` /
      `class="pc red"`, and `class="back` still appears exactly **6** times with `back mucked`
      exactly **4**
- [ ] `class="pc` appears exactly **29** times, `class="pc"` exactly **16** and `class="pc red"`
      exactly **13**
- [ ] `aria-label="ImKate` appears exactly **3** times — neither new face-up card names her
- [ ] The frame's banner reads `Your rival wins 4,850` exactly once, over the same facts line as its
      two neighbours, which now appears exactly **3** times
- [ ] ImKate's stack reads `7,800` and carries `pile flying`; the hero's reads `12,200` and carries a
      plain `class="pile"`; `class="pile` is **9** and `pile flying` is **3**
- [ ] `class="bet-line"` is **4** and the new frame's is empty
- [ ] The note cites `ADR-0120`, `ADR-0126` and `ADR-0136` and states all four points in *Scope*;
      file totals are `ADR-0120` **2**, `ADR-0126` **1**, `ADR-0136` **1**
- [ ] `pc mini`, `data-`, `title=` and `transition` appear nowhere; `animation:` is **3** and
      `--pd-motion` is **12**
- [ ] No hand is named anywhere in the file, by the refused vocabulary the `verify:` block lists
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly one file, `design/screens/duel-table-states.html`
- [ ] Every command in `verify:` exits 0

> The human's visual verdict on the rendered card is the design review (`ADR-0024` §3) and it may
> **trail the merge** (`ADR-0091` §3). A rejection is a repair ticket against this card.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
