---
schema: 2
id: TASK-141101
title: The card strikes the muck line and both banners' second lines
type: task
status: done
parent: STORY-1411
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, table]
depends_on: []
verify:
  - ./design/check-drift.sh
  - sh -c '! grep -qF "ImKate mucks" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "Two pair, aces and sevens" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "Nobody shows" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "stated in words in the reserved line" design/screens/duel-table-states.html'
  - sh -c '! grep -qF "bet/muck line" design/screens/duel-table-states.html'
  - awk 'index($0, "ImKate folds") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "Blinds 75/150 · Hand 14 · Hand complete") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "Blinds 75/150 · Hand 14 · Turn") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"meta\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"bet-line\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"note\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"pc") { n++ } END { exit (n != 20) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"back") { n++ } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk 'index($0, "back mucked") { n++ } END { exit (n != 4) }' design/screens/duel-table-states.html
  - awk 'index($0, "You win 4,850") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "You win 3,250") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0095") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0120") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "ADR-0008") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table-states.html` stops drawing three strings that merged ADRs already
deleted: the words that say a hand was mucked, and the second line under each of the two award
banners. In their place the two ending frames carry `ADR-0095` §1's untouched facts line, the same
one the wait frame beside them already carries. Nothing is added, no frame moves, and no card
changes.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table-states.html` | modify |

Read [`ADR-0095`](../../docs/adr/ADR-0095-the-table-states-who-took-the-pot-and-never-names-a-hand.md)
§§1–3 and
[`ADR-0120`](../../docs/adr/ADR-0120-a-showdown-shows-the-hands-the-rules-showed-and-the-beat-that-shows-them-stands.md)
§1. **Nothing else is opened and nothing else is changed** — in particular no file under
`web-client/`, no other card under `design/`, and no line in this file above the frames.

## Scope

Six edits, in three places. Line numbers are as this file stands on `develop` at `c54b4a1f`.

- **The muck line goes (`:302`).** In the showdown frame,

  ```html
          <div class="bet-line">ImKate mucks</div>
  ```

  becomes

  ```html
          <div class="bet-line"></div>
  ```

  — the shape the wait frame's own bet line already has at `:249`. `ADR-0120` §1: *"**No line is
  added to say a hand was mucked.** The cards that stay face down say it … a *Your rival mucks*
  string is not licensed by it."* The reserved line keeps its height and its class, so nothing on the
  table moves; only the words go.

- **The showdown banner's second line goes (`:315`).**

  ```html
              <span class="meta">Two pair, aces and sevens</span>
  ```

  becomes

  ```html
              <span class="meta">Blinds 75/150 · Hand 14 · Hand complete</span>
  ```

  `ADR-0095` §3 struck this string by name — *"the card's showdown frame loses that line"* — and §1
  says what stands in its place: *"The facts line beside it is untouched on this street as on every
  other: `Blinds N/N · Hand N · Hand complete` goes on saying what it says today."* The literal is
  the client's own (`PotStrip.tsx:14,119-120`). **Copy the wait frame's own meta at `:255` and change
  only the last word**, rather than typing this line: the separator is `·` (U+00B7) between plain
  spaces, and `Blinds 75/150` carries **no** `&nbsp;` — only the `.amount` above it does. A gate
  fails on any other spelling.

- **The fold banner's second line goes (`:375`).**

  ```html
              <span class="meta">Nobody shows — your river bet of 800 goes uncalled and returns</span>
  ```

  becomes the **same** string as above. `ADR-0095` §3 struck it *"for its own reason rather than by
  association: **the numbers already agree without it.**"*

- **The showdown frame's note loses the clause that described the deleted line (`:348-350`).** It
  reads today: *ADR-0008: the loser's cards stay down, dimmed, stated in words in the reserved line;
  the pot row becomes the banner without changing shape — the amount is mono like every chip number*.
  The middle clause is now false. Rewrite it so it still cites `ADR-0008` **once**, adds `ADR-0120`
  **once**, and says that the cards staying face down are the whole statement — for example:

  ```html
        <p class="note">ADR-0008: the loser's cards stay down, dimmed, and ADR-0120 §1 says that is
        the whole statement — no line says a hand was mucked; the pot row becomes the banner without
        changing shape, one line replacing one line, and the amount is mono like every chip
        number</p>
  ```

- **The fold frame's note gains the `ADR-0095` citation (`:408-412`).** Everything it says is still
  true; add one clause naming `ADR-0095` §§1 and 3 as the reason the second line went, and keep the
  arithmetic sentence exactly as it is. The `ADR-0095` gate counts **two** mentions in the file: one
  here and one in the showdown frame's note, so put the other one there too.

- **The lede stops naming the muck (`:227-230`).** *"Every slot — the opponent's hidden hand, the
  bet/muck line, the pot row, the bar — exists in every state"* becomes *"… the bet line, the pot
  row, the bar …"*. The slot still exists in every state; it is the muck half of its name that has
  gone.

## Out of scope

- **`ImKate folds` at `:307`.** It stays, and a `verify:` gate pins it at exactly one. `ADR-0120` §1
  strikes the **muck** line by name and says nothing about a fold line, and whether `ADR-0109` §1's
  `Fold` mark on the plate above makes it redundant is a question nobody has asked. Deleting it here
  would be one deletion taken as a licence for another.
- **The `.mucked` dimming on the four card backs.** `ADR-0126` §1 keeps what a card **is**
  *"exactly as it is drawn today"*, and `back mucked` is gated at **4** — unchanged.
- **Adding a frame.** `TASK-141102` and `TASK-141103` add the two this card does not have; a gate
  pins `class="frame"` at **3** here, so a coder who starts early fails.
- **Every card face, every board, every seat plate, every stack figure, every `.last-act` mark and
  the whole stylesheet.** Gates pin `class="pc"` at **20**, `class="back"` at **6**, `You win 4,850`
  at **1** and `You win 3,250` at **1**.
- **Every other card under `design/`.** `design/tokens/type.html:94` also carries the string
  *Two pair, aces and sevens* — as a **type specimen**, not as a table state, so `ADR-0095` §3 does
  not reach it. It is not opened.
- **Every client source file.** `PotStrip.tsx` already builds this facts line and already refuses to
  name a hand; there is nothing in `web-client/` for this ticket to do.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. Every count below
was measured on this file on `develop` at `c54b4a1f`; the number after the arrow is what this ticket
must produce.

| Gate | Proves |
| --- | --- |
| `ImKate mucks` 1 → **absent** | `ADR-0120` §1's refusal is drawn |
| `Two pair, aces and sevens` 1 → **absent**, `Nobody shows` 1 → **absent** | `ADR-0095` §3's two named strikes |
| `Blinds 75/150 · Hand 14 · Hand complete` 0 → **2** | both endings carry the facts line rather than a gap, and both carry the *same* one |
| `Blinds 75/150 · Hand 14 · Turn` **1** | the wait frame's own meta did not drift |
| `class="meta"` **3**, `class="bet-line"` **3**, `class="note"` **3**, `class="frame"` **3** | three slots removed nothing and three frames stayed three |
| `ImKate folds` **1** | the fold line was not taken with the muck line |
| `class="pc"` **20**, `class="back"` **6**, `back mucked` **4** | not one card moved |
| `You win 4,850` **1**, `You win 3,250` **1** | both banners' **first** lines are untouched |
| `stated in words in the reserved line` **absent** | the note stopped describing a drawing that is gone |
| `bet/muck line` **absent** | the lede followed |
| `ADR-0095` **2**, `ADR-0120` **1**, `ADR-0008` **2** | each decision cited where it applies, and `ADR-0008` neither gained nor lost a mention |
| `./design/check-drift.sh` exits 0 | tokens, values, suits and the lockup still hold (`ADR-0024` §2) — nothing is minted |

## Acceptance criteria

- [ ] `ImKate mucks`, `Two pair, aces and sevens` and `Nobody shows` appear nowhere in
      `design/screens/duel-table-states.html`
- [ ] `Blinds 75/150 · Hand 14 · Hand complete` appears exactly **2** times, and
      `Blinds 75/150 · Hand 14 · Turn` exactly **1**
- [ ] `ImKate folds` still appears exactly **1** time
- [ ] `class="meta"`, `class="bet-line"`, `class="note"` and `class="frame"` each appear exactly
      **3** times
- [ ] `class="pc` appears exactly **20** times, `class="back` exactly **6**, and `back mucked`
      exactly **4**
- [ ] `You win 4,850` and `You win 3,250` each appear exactly **1** time
- [ ] `stated in words in the reserved line` and `bet/muck line` appear nowhere
- [ ] `ADR-0095` is cited exactly **2** times, `ADR-0120` exactly **1**, `ADR-0008` exactly **2**
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly one file, `design/screens/duel-table-states.html`
- [ ] Every command in `verify:` exits 0

> The human's visual verdict on the rendered card is the design review (`ADR-0024` §3) and it may
> **trail the merge** (`ADR-0091` §3).

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
