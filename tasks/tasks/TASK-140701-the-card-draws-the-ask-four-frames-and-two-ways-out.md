---
schema: 2
id: TASK-140701
title: The card draws the ask — four frames, two ways out, and the words
type: task
status: ready
parent: STORY-1407
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, profile, lobby]
depends_on: []
verify:
  - ./design/check-drift.sh
  - test "$(head -1 design/screens/name-ask.html)" = '<!-- @dsCard group="Screens" -->'
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/name-ask.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 4) }' design/screens/name-ask.html
  - awk 'index($0, "class=\"btn ") { n++ } END { exit (n != 7) }' design/screens/name-ask.html
  - awk 'index($0, "class=\"field\"") { n++ } END { exit (n != 3) }' design/screens/name-ask.html
  - awk 'index($0, "class=\"link\"") { n++ } END { exit (n != 0) }' design/screens/name-ask.html
  - awk 'index($0, "class=\"mark\"") { n++ } END { exit (n != 0) }' design/screens/name-ask.html
  - awk 'index($0, "That name is not available. Try another.") { n++ } END { exit (n != 1) }' design/screens/name-ask.html
  - awk 'index($0, "That did not reach the server. Reload the page to see whether the name was set.") { n++ } END { exit (n != 1) }' design/screens/name-ask.html
  - sh -c '! grep -qi "permanent" design/screens/name-ask.html'
  - sh -c '! grep -qiE "cancel|dismiss|no thanks|maybe later|not now|skip for now|close this|http" design/screens/name-ask.html'
  - perl -ne 'print "$1\n" while m{<div class="hero-h">([^<]*)</div>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-heading.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-heading.txt")" -eq 1
  - test "$(grep -c 'class="hero-h"' design/screens/name-ask.html)" -eq 4
  - perl -ne 'print "$1\n" while m{<p class="what">([^<]*)</p>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-what.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-what.txt")" -eq 1
  - test "$(grep -c 'class="what"' design/screens/name-ask.html)" -eq 3
  - grep -qi rival "${TMPDIR:-/tmp}/pd-ask-what.txt"
  - grep -qi ladder "${TMPDIR:-/tmp}/pd-ask-what.txt"
  - perl -ne 'print "$1\n" while m{<p class="keeps">([^<]*)</p>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-keeps.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-keeps.txt")" -eq 1
  - test "$(grep -c 'class="keeps"' design/screens/name-ask.html)" -eq 3
  - grep -qi "chang" "${TMPDIR:-/tmp}/pd-ask-keeps.txt"
  - sh -c '! grep -qiE "[0-9]|forever|never|available|free|taken|account|password|email|coin" "${TMPDIR:-/tmp}/pd-ask-keeps.txt"'
  - perl -ne 'print "$1\n" while m{<button class="btn fill">([^<]*)</button>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-take.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-take.txt")" -eq 1
  - perl -ne 'print "$1\n" while m{<button class="btn ghost">([^<]*)</button>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-skip.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-skip.txt")" -eq 1
  - grep -qi duel "${TMPDIR:-/tmp}/pd-ask-skip.txt"
  - perl -ne 'print "$1\n" while m{<label>([^<]*)</label>}g' design/screens/name-ask.html | sort -u > "${TMPDIR:-/tmp}/pd-ask-label.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-label.txt")" -eq 1
  - perl -ne 'print "$1\n" while m{<div class="box">([^<]*)</div>}g' design/screens/name-ask.html > "${TMPDIR:-/tmp}/pd-ask-boxes.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-ask-boxes.txt")" -eq 3
  - test "$(grep -cE '^[^ ]+ [^ ]+ [^ ]+$' "${TMPDIR:-/tmp}/pd-ask-boxes.txt")" -eq 2
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/name-ask.html` exists and draws the product's **first dialog** in four frames: the
suggestion offered, a name typed, a name refused as taken, and a refusal the player cannot retry
with the skip still standing. It fixes every string the six tickets after it transcribe — the
heading, the two sentences the ask owes, the field's label and the two control labels — so nothing
downstream can be written until this merges.

## Files

| File | Action |
| --- | --- |
| `design/screens/name-ask.html` | create |

Read [`ADR-0119`](../../docs/adr/ADR-0119-the-name-is-asked-at-the-first-press-and-skipping-plays.md)
§§1–4, [`ADR-0130`](../../docs/adr/ADR-0130-a-name-can-be-changed-and-the-name-it-leaves-is-spent.md)
§5, [`ADR-0137`](../../docs/adr/ADR-0137-a-name-suggestion-is-drawn-in-the-browser.md) §§4–5, and
**`design/screens/sign-in.html`**, which is this card's sibling: a form, a field, a refusal line and
one submit, in the treatments this card reuses. **Nothing else is opened, and no file outside the
table above is changed** — in particular nothing under `web-client/` and nothing in
`design/tokens/`.

## Scope

- **One new card, self-contained, conventions per `design/README.md`.** Line 1 is exactly
  `<!-- @dsCard group="Screens" -->`; a `<title>` names it; the `:root` token block is a **faithful
  copy** of `design/screens/sign-in.html`'s, because `check-drift.sh` compares every inlined
  `--pd-NAME: VALUE` against the canonical sheet and fails on a single re-typed value. Copy the
  block; do not re-derive it.
- **Four frames, in this order, each with its own `<h2>` naming the state:**

  1. **the suggestion offered** — the field holds a drawn name, both controls stand;
  2. **a name typed** — the same frame with the player's own string in the field instead;
  3. **the name is not available** — `<p class="status">That name is not available. Try
     another.</p>`, the field holding a **fresh** suggestion (`ADR-0137` §5 rerolls on `conflict`
     alone), both controls still standing;
  4. **nothing reached the server** — `<p class="status">That did not reach the server. Reload the
     page to see whether the name was set.</p>`, **no field and no submit** (`mayTryAgain` answers
     `false` to this one), and the skip still there and still working. This frame is the drawing of
     `ADR-0119` §2's *"no state of this screen ever leaves a player unable to duel"*; a card without
     it draws a promise nobody can see.

  Both refusal sentences are the shipped golden strings from `web-client/src/profile/name-text.ts`
  (`ADR-0052`'s, applied by `ADR-0119` §3's obligation 3). They are quoted here so the card cannot
  invent a second vocabulary for the same failures, and two `verify:` gates pin them.
- **The screen's own heading is `<div class="hero-h">…</div>`, byte-identical in all four frames.**
  A gate extracts every copy and fails unless exactly one distinct spelling exists. It is the string
  `NAME_ASK_HEADING` will hold in `TASK-140709` and the `<h2>` the client renders. The frames' own
  `<h2>` elements name the states and are **not** product copy.
- **Two sentences, in frames 1–3 only, one distinct spelling each:**
  - `<p class="what">` — **what a name is for**: that it is what a rival and the ladder see
    (`ADR-0119` §3, obligation 1). Gates require the words *rival* and *ladder*.
  - `<p class="keeps">` — **`ADR-0130` §5's pair**: that the name can be changed later, **and** that
    a name given up is gone for good, for the player and for everybody. One sentence may carry both,
    and for a player taking a first name the second half is forward-looking — it is the string they
    would leave behind on a later change that is spent. Gates require *chang*, and refuse digits,
    *forever*, *never*, *available*, *free*, *taken*, and every account word: this surface says
    nothing about accounts, passwords, email or coins (`ADR-0119` §6), and nothing about whether a
    name is available (`ADR-0029` §5).
  - **`PERMANENCE_LINE` is not on this card and neither is the word *permanent*.** `ADR-0130` §5
    supersedes `ADR-0119` §3's obligation 2 and takes that sentence out of the product; a gate fails
    on the word.
- **Two controls, and no third.** `<button class="btn fill">` — take the name — in frames 1–3;
  `<button class="btn ghost">` — the skip — in **all four**. Both are buttons of the same family:
  gates pin `class="btn ` at **7** and `class="link"` at **0**, which is what *"two equally
  reachable ways out"* (`ADR-0119` §2) means mechanically. **No *suggest another* control** — the
  field is editable and pre-filled, and `conflict` already redraws by itself.
- **The skip's label says what it does: it takes the player into the duel with no name.** It may not
  read as a close, a cancel or a dismissal (`ADR-0119` §3), and a gate refuses *cancel*, *dismiss*,
  *not now*, *no thanks*, *maybe later*, *skip for now* and *close this* **anywhere on the card —
  inside a `<p class="note">` too**, so write the reason without those words. It must contain the
  word *duel*: a player pressing it has to know they are about to duel, not that they are backing
  out of one.
- **The field is `<div class="field"><label>…</label><div class="box">…</div></div>`**, three times
  (frames 1–3), one distinct label spelling. Two of the three boxes hold a **three-word** string —
  the drawn suggestion in frames 1 and 3, `Quiet Iron Raven` in shape — and the third holds the
  player's own typed name, which is not three words. Gates pin exactly three boxes and exactly two
  three-word ones.
- **No wordmark.** `ADR-0098` §1's lockup belongs to the front door's pre-create branch; this screen
  stands in place of that branch and is not it, and the account and sign-in screens carry none
  either. A gate pins `class="mark"` at 0.
- **`<p class="note">` blocks carry the reasons**, as every other card does: which ADR fixes which
  string, that the two refusal sentences are `name-text.ts`'s and not this card's, and that the
  suggestion in the field promises nothing about availability.

## Out of scope

- **Anything under `web-client/`.** The transcription is `TASK-140709`'s (the words) and
  `TASK-140710`/`TASK-140711`'s (the screen).
- **A *suggest another* control**, a labelled suggestion, or any hint that a name is free — refused
  in `STORY-1407`'s *Design notes* and *Out of scope*.
- **The vocabulary.** The example name on this card is an illustration, not a seed; the words are
  `TASK-140702`–`TASK-140704`'s.
- **The front door, the account screen and `NameSurface`.** `design/screens/create-duel.html` and
  `design/screens/account.html` are not opened; `ADR-0130` §6's move is `STORY-1409`'s.
- **The `DesignSync` push and the `_ds_manifest.json` entry a new card needs.** Human-invoked
  (`design/README.md`), and the visual verdict may trail this merge (`ADR-0091` §3).

## Tests

None: a design card's structural gates are its `verify:` block, and its visual verdict is the
human's, given on the rendered card (`ADR-0024` §3).

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0 with the new card present, so every `--pd-` name it mentions
      is declared in `design/tokens/tokens.css` and every value it inlines equals the sheet's
- [ ] Line 1 is exactly `<!-- @dsCard group="Screens" -->`, and the card requests nothing from
      anywhere
- [ ] The card holds exactly **4** `class="frame"` blocks and **4** `<h2>` headings
- [ ] `class="hero-h"` appears **4** times with exactly **one** distinct spelling
- [ ] `class="what"` appears **3** times with one distinct spelling, containing *rival* and *ladder*
- [ ] `class="keeps"` appears **3** times with one distinct spelling, containing *chang*, and no
      digit and none of *forever*, *never*, *available*, *free*, *taken*, *account*, *password*,
      *email*, *coin*
- [ ] The word *permanent* appears nowhere on the card
- [ ] `class="btn ` appears **7** times and `class="link"` **0** times; the `btn fill` label has one
      distinct spelling and the `btn ghost` label has one, and the ghost label contains *duel*
- [ ] None of *cancel*, *dismiss*, *not now*, *no thanks*, *maybe later*, *skip for now*, *close
      this* appears anywhere on the card
- [ ] `class="field"` appears **3** times, `<div class="box">` **3** times, exactly **2** of the
      boxes hold a three-word string, and `<label>` has one distinct spelling
- [ ] `That name is not available. Try another.` and `That did not reach the server. Reload the page
      to see whether the name was set.` each appear exactly once
- [ ] `class="mark"` appears **0** times
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
