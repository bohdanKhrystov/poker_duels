---
schema: 2
id: TASK-141501
title: The card mints the panel, and draws its three states and the screen without it
type: task
status: backlog
parent: STORY-1415
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, minting, rematch, notice]
depends_on: []
verify:
  - ./design/check-drift.sh
  - test "$(head -1 design/screens/rematch-panel.html)" = '<!-- @dsCard group="Screens" -->'
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/rematch-panel.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 4) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"panel\"") { n++ } END { exit (n != 3) }' design/screens/rematch-panel.html
  - awk 'index($0, "Your rival offers a rematch") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "That duel room is gone.") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "Rematch. The button changes sides —") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "dealing hand 1…") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"btn fill\"") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"btn ghost\"") { n++ } END { exit (n != 3) }' design/screens/rematch-panel.html
  - awk 'index($0, "--pd-shadow-pop") { n++ } END { exit (n < 2) }' design/screens/rematch-panel.html
  - awk 'index($0, "ADR-0138") { n++ } END { exit (n < 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "ADR-0143") { n++ } END { exit (n < 1) }' design/screens/rematch-panel.html
  - awk 'tolower($0) ~ /@keyframes|animation|transition|z-index/ { n++ } END { exit (n != 0) }' design/screens/rematch-panel.html
  - sh -c '! grep -qiE "role=\"alert\"|aria-live|alertdialog|role=\"dialog\"|⚠" design/screens/rematch-panel.html'
  - sh -c '! grep -qiE "decline|reject|no thanks|maybe later|skip for now|close this" design/screens/rematch-panel.html'
  - sh -c '! grep -qiE "countdown|expires|expiring|time left|seconds left|remaining|[0-9]+ seconds" design/screens/rematch-panel.html'
  - perl -ne 'print "$1\n" while m{<button class="btn ghost">([^<]*)</button>}g' design/screens/rematch-panel.html | sort -u > "${TMPDIR:-/tmp}/pd-panel-dismiss.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-panel-dismiss.txt")" -eq 1
  - test "$(cat "${TMPDIR:-/tmp}/pd-panel-dismiss.txt")" = "Not now"
  - perl -ne 'print "$1\n" while m{<button class="btn fill">([^<]*)</button>}g' design/screens/rematch-panel.html | sort -u > "${TMPDIR:-/tmp}/pd-panel-accept.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-panel-accept.txt")" -eq 1
  - test "$(cat "${TMPDIR:-/tmp}/pd-panel-accept.txt")" = "Rematch"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/rematch-panel.html` exists and draws the product's **first surface that sits above
another screen**, in four frames: the offer standing, the accepted span, the room gone, and the
screen with the panel set aside — which is the screen, unchanged. It fixes every string the client
tickets after it transcribe and every treatment they wear, so nothing downstream can be written
until this merges.

## Why this is the first ticket, and why it is minting

`ADR-0091` §2: a story that puts a new player-facing surface in front of a player owes the card
first. `ADR-0123` §8 makes this one **minting** rather than composing — a surface above another
screen is visual language this product does not have — so `ADR-0091` §3 puts it in the human's
hands: it is worked interactively, and the visual verdict may trail the merge. The gates below are
structure, never taste.

## Files

| File | Action |
| --- | --- |
| `design/screens/rematch-panel.html` | create |

Read [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§2, 4, 5,
7 and 8, [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§4–6, [`ADR-0143`](../../docs/adr/ADR-0143-irreversibility-is-said-last-and-never-coloured.md) §1,
and **`design/screens/account.html`**, whose `:root` block this card copies. **Nothing outside the
table above is changed** — in particular nothing under `web-client/`, nothing in `design/tokens/`,
and not `design/screens/rematch-states.html`, which is `TASK-141509`'s.

## Scope

- **One new card, self-contained, conventions per `design/README.md`.** Line 1 is exactly
  `<!-- @dsCard group="Screens" -->`; a `<title>` names it; the `:root` token block is a **faithful
  copy** of `design/screens/account.html`'s, plus `--pd-accent`, `--pd-accent-subtle` and
  `--pd-shadow-pop`. `check-drift.sh` compares every inlined `--pd-NAME: VALUE` against the
  canonical sheet and fails on a single re-typed value — copy the lines, do not re-derive them.
- **`--pd-shadow-pop` gets its first use in the product.** Measured at `899d81f7`: it is declared
  (`design/tokens/tokens.css:117`, *"for what floats (menus, dialogs)"*), mapped
  (`web-client/src/styles/app.css:73`) and used by **no component and no screen card**. This card is
  what floats. A gate requires it at least twice — the inlined declaration and at least one rule.
- **Four frames, in this order, each with its own `<h2>` naming the state:**

  1. **the offer standing** — `<div class="theirs">Your rival offers a rematch</div>`, then
     `<button class="btn fill">Rematch</button>`, then `<button class="btn ghost">Not now</button>`;
  2. **the accepted span** — the dealing sentence in place of the accept, on two lines split by a
     `<br>` exactly as `RematchControl` splits it: `Rematch. The button changes sides —` then
     `dealing hand 1…`. `Not now` still stands;
  3. **the room is gone** — `That duel room is gone.` in place of the accept (`ADR-0044` §6's frame
     that ends a rematch). `Not now` still stands;
  4. **set aside** — a stand-in screen with **no panel on it at all**. This is the drawing of
     `ADR-0123` §7's *"a dismissal hides the surface, never the offer"*: what a player gets back is
     their screen, with nothing taken and nothing added. A card without it draws three states and
     hides the fourth.

  All four product sentences are the **shipped golden strings** from
  `web-client/src/result/RematchControl.tsx` (`ADR-0123` §4: *"No new words are minted"*). `Not now`
  is `ADR-0123` §4's word for an offer that may be set aside. Four gates pin them, and two more pin
  that each control label has exactly **one** distinct spelling.
- **The panel's shell is this card's to mint**: its box, its width, its place on the viewport, its
  colours and its type. Two constraints it is written inside, both from merged decisions —
  `ADR-0121`'s fit axis (`scrollWidth ≤ clientWidth`) bounds it to the viewport width, and
  `ADR-0103`'s height budget is untouched because the panel never renders over the table.
- **No motion of any kind, and a `<p class="note">` saying so.** A gate refuses `@keyframes`,
  `animation`, `transition` and `z-index` anywhere on the card. `ADR-0123` §2 permits an entrance and
  does not require one; `ADR-0115` §1 says the still form must say everything, and here it does. The
  consequence is that `ADR-0123` §8's *"each of the first three at rest under
  `prefers-reduced-motion`"* is satisfied by there being **nothing to still** — say that in the
  note, because a reader checking §8 against this card must be able to see that the requirement was
  met rather than skipped. **If the human wants an entrance at the pane, it is a follow-up ticket**
  that mints a `--pd-motion-panel-*` pair in `design/tokens/tokens.css` **and** in the vendored
  `web-client/src/styles/tokens.css` (they are byte-compared by `tokens.test.ts`), adds a
  `@media (prefers-reduced-motion: reduce)` block, and adds the three stilled frames. It is not this
  ticket and it is not a coder's to add.
- **`z-index` is refused, and the reason is measured, not stylistic.** There is **no `z-index`
  anywhere** in `web-client/src` or `design/` at `899d81f7`, and the live panel is the last child of
  `<main>` with `PlayingCard`'s absolute glyphs its only positioned competition — earlier in the
  document, so painted first. A card that mints a stacking order mints a vocabulary the product does
  not need.
- **`ADR-0143` §1 binds this card, and permits its accent.** Nothing here marks an act by how it is
  drawn, and nothing enters the assistive register: gates refuse `role="alert"`, `role="dialog"`,
  `alertdialog`, any `aria-live`, and `⚠`. The accent **is** permitted — §1's own measurement lists
  *a standing offer* among the things colour in this client already names, so `--pd-accent` on the
  rival's line says what the thing **is**. Put that in a note, so the flatness is graded at the pane
  as a decision rather than re-litigated as an oversight.
- **Nothing that states what the wire does not carry.** Gates refuse *decline*, *reject*, *no
  thanks*, *maybe later*, *skip for now*, *close this*, and every countdown word. `ADR-0044` §6
  records no decline and no deadline; `ADR-0123` §5 says the client has nothing truthful to count.
  The `<h2>` and `<p class="note">` blocks are card furniture and may use the word *dismiss* — the
  ban is on control labels and product sentences, and the label gates are what enforce that.
- **`<p class="note">` blocks carry the reasons**, as every other card does: that the panel is
  `position: fixed` and out of flow in the live client (`ADR-0138` §5) while this card draws it in a
  frame; that its root carries `role="status"` and takes no focus (`ADR-0138` §6); that the words are
  `RematchControl`'s and not this card's; and the two above. Gates require `ADR-0138` and `ADR-0143`
  to be cited by name.

## Out of scope

- **The panel over a screen, at either width.** `TASK-141502` adds those four frames to this file.
- **Anything under `web-client/`.** The transcription is `TASK-141503`'s (the words) and
  `TASK-141504`'s (the surface).
- **`design/screens/rematch-states.html`.** The result screen's card, stale in three places, is
  `TASK-141509`'s.
- **`design/tokens/tokens.css` and its vendored copy.** No token is minted here.
- **The `DesignSync` push and the `_ds_manifest.json` entry a new card needs.** Human-invoked
  (`design/README.md`), and the visual verdict may trail this merge (`ADR-0091` §3).

## Tests

None: a design card's structural gates are its `verify:` block, and its visual verdict is the
human's, given on the rendered card (`ADR-0024` §3).

## What would still pass if this were drawn wrong

Every count gate here is exact rather than `>= 1`, because a presence check passes on a card that
draws one state four times. The two label gates extract **every** `btn fill` and `btn ghost` label,
`sort -u` them, and require exactly one line each — a card that spells the dismiss *Not now* in one
frame and *Not Now* in another passes a `grep -q` and fails this. The `panel` count of 3 against a
`frame` count of 4 is what makes frame 4 real: without it, a card that drew a fourth panel frame and
called it *set aside* would pass every other gate.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0 with the new card present
- [ ] Line 1 is exactly `<!-- @dsCard group="Screens" -->`, and the card requests nothing from
      anywhere
- [ ] `class="frame"` appears **4** times, `<h2>` **4** times, and `class="panel"` **3** times
- [ ] Each of `Your rival offers a rematch`, `That duel room is gone.`,
      `Rematch. The button changes sides —` and `dealing hand 1…` appears exactly **once**
- [ ] `class="btn fill"` appears **1** time with the single label `Rematch`; `class="btn ghost"`
      appears **3** times with the single label `Not now`
- [ ] `--pd-shadow-pop` appears at least **twice**
- [ ] None of `@keyframes`, `animation`, `transition`, `z-index` appears anywhere on the card
- [ ] None of `role="alert"`, `role="dialog"`, `alertdialog`, `aria-live`, `⚠` appears anywhere
- [ ] None of *decline*, *reject*, *no thanks*, *maybe later*, *skip for now*, *close this* or any
      countdown word appears anywhere
- [ ] `ADR-0138` and `ADR-0143` are each cited by name in a note
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
