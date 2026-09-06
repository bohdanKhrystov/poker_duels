---
schema: 2
id: TASK-140401
title: The card redraws the waiting table — three frames, no You, and the sentence
type: task
status: ready
parent: STORY-1404
module: design
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [design, table, lobby]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 6) }' design/screens/duel-table.html
  - awk 'index($0, "Host alone") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - sh -c '! grep -qF "Host alone — no clipboard API" design/screens/duel-table.html'
  - awk 'index($0, "<span class=\"name\">You</span>") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "Waiting for your rival") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "Copy the link") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "Link copied.") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "Copy it from the box above.") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "The room stays open. That link still works for your rival, and it brings you back.") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "Back to the lobby") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"linkbox") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"linkbox sel\"") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, ".linkbox.sel {") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "<p class=\"autostart\">") { n++ } END { exit (n != 3) }' design/screens/duel-table.html
  - awk 'index($0, ".autostart {") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "ADR-0128") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "ADR-0129") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - perl -ne 'print "$1\n" while m{<p class="autostart">([^<]*)</p>}g' design/screens/duel-table.html | sort -u > "${TMPDIR:-/tmp}/pd-autostart.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-autostart.txt")" -eq 1
  - grep -qi rival "${TMPDIR:-/tmp}/pd-autostart.txt"
  - grep -qi duel "${TMPDIR:-/tmp}/pd-autostart.txt"
  - sh -c '! grep -qiE "[0-9]|opponent|forever|second|minute|hour|expire|countdown|remaining|timer|timeout|button|click|tap|press|game|&" "${TMPDIR:-/tmp}/pd-autostart.txt"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html` draws **three** host-alone variants instead of four, none of them
puts `You` on the host's plate, each carries the one sentence `ADR-0129` requires, and the frame
reached by a press that could not copy shows the link **selected** in its box. The card fixes that
sentence's exact glyphs — the two merged string-set tests and `TASK-140403` all take their literal
from this file, so nothing downstream can be written until this merges.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |

Read [`ADR-0128`](../../docs/adr/ADR-0128-the-copy-control-is-never-absent-and-a-press-that-cannot-copy-hands-over-the-selection.md)
§§1–6 and [`ADR-0129`](../../docs/adr/ADR-0129-the-host-is-told-the-duel-starts-by-itself.md) §§1–4.
Those two sections of `ADR-0129` are the whole specification of the sentence you write: two facts it
must convey (§2) and six things it may not carry (§3). **Nothing else is opened, and no file outside
the table above is changed** — in particular no file under `web-client/`.

## Scope

The four host-alone frames are at lines 491, 520, 548 and 576 today. After this ticket there are
three.

- **The fourth frame is deleted whole.** `<h2>Host alone — no clipboard API</h2>` and the entire
  `<div class="frame">` around it (lines 576–599, up to and including its closing `</div>`) go.
  `ADR-0128` §6: with the control always present, that state at rest is byte-identical to the
  at-rest frame and its pressed form is the third frame, so the variant is **struck**, not deferred.
- **`You` leaves the host's plate in all three surviving frames.** The plate itself stays — it is
  `ADR-0110` §2's seat, and only the *name* is optional (§§2–3). In each frame,

  ```html
          <span class="who"><span class="name">You</span></span>
  ```

  becomes

  ```html
          <span class="who"></span>
  ```

  The three `<span class="name">You</span>` nodes in the **live-table** frames do not move: the
  human crossed `You` out on the waiting table and nowhere else. A `verify:` gate pins the survivors
  at exactly **3**, so removing a fourth fails.
- **One sentence, drawn once per frame, directly beneath the rival's empty seat plate.** Add, in
  each of the three frames, immediately after the `<div class="seat seat-empty">…</div>` block and
  before `<div class="code">`:

  ```html
        <p class="autostart">«the sentence you write»</p>
  ```

  The three copies are **byte-identical**; a `verify:` gate extracts them and fails unless exactly
  one distinct spelling exists. `ADR-0129` §4 puts the placement here too: the human's annotation
  sat on the `Waiting for your rival` bar, so directly beneath that plate is the report followed and
  owes no reason on the card. Anywhere else does, and you would have to write that reason into the
  frame's note.
- **The sentence itself is this ticket's one judgement, and `ADR-0129` §§2–4 bound it.** It must
  convey, in the product's voice, both that **the rival's arrival is what starts the duel** and that
  **nothing is required of the host**; the review test is whether a host reading this surface for the
  first time can answer *"is there anything I still have to do?"* with **no**, from this string
  alone. A line that only restates that the host is waiting fails — `Waiting for your rival` already
  says that, and saying it is what produced the question. It calls the second player **rival** and
  the thing that starts a **duel**; never *opponent*, never *game*, never a gambling noun, and never
  the human's `dual`. It may not carry a duration, a countdown or *forever*; anything about the code;
  any game fact; any named control, present or absent; anything about what the rival sees; or any
  instruction. Four `verify:` gates hold the mechanical half of that — one distinct spelling, the
  word *rival*, the word *duel*, and no digit and none of a list of refused words — and the rest is
  the review's. It is one sentence, plain text, **no HTML entity and no markup inside the `<p>`**:
  the client transcribes this literal byte-for-byte, and an entity would not survive the trip.
- **The third frame becomes the frame both no-copy routes reach.** Its heading
  `<h2>Host alone — the copy was refused</h2>` becomes `<h2>Host alone — the press could not
  copy</h2>`, and it gains a note naming both routes and citing the decision, for example:

  ```html
        <p class="note">reached two ways — the browser exposes no Clipboard API, or the write was refused: ADR-0128 §3 makes them one outcome, and the box takes focus with the whole link selected</p>
  ```

  That note is where the `ADR-0128` citation the `verify:` block counts lives. Put the single
  `ADR-0129` citation on a note in the at-rest frame, beside the sentence it explains.
- **The selection is drawn in that frame only.** Its `<div class="linkbox">` becomes
  `<div class="linkbox sel">`, and the stylesheet gains one rule beside `.promise`:

  ```css
    /* the hand-over: a press that could not copy leaves the whole link selected */
    .linkbox.sel { background: var(--pd-accent-subtle); color: var(--pd-text); }
  ```

  The comment names no ADR: the `verify:` block pins `ADR-0128` at **exactly one** mention in this
  file, and that one is the third frame's note above. Both citations are today at **zero**.

  `--pd-accent-subtle` and `--pd-text` are already declared in this card's own `:root` (lines 39 and
  22) and in the sheet, so **no token is minted** and `check-drift.sh` has nothing to say. *How* a
  selection is drawn is taste and taste is the human's (`ADR-0024` §3) — this is the cheapest
  spelling that uses the settled vocabulary, and the human may replace it at the pane.
- **The `.autostart` rule, beside `.promise`:**

  ```css
    .autostart { font-size: var(--pd-fs-small); color: var(--pd-text-muted); }
  ```

  Both tokens are already inlined in this card. A separate class rather than reusing `.promise` is
  what makes the sentence extractable by the gates in this ticket and in `TASK-140403`.

## Out of scope

- **Every client source file.** `InvitePanel.tsx` still hides the control without a clipboard after
  this merges, and `WaitingTable.tsx` still prints `You`. `TASK-140402` and `TASK-140403` are what
  change them. A card ahead of its client is `ADR-0091` §2's *"the card comes first"*, and it is
  deliberate here.
- **The live-table frames.** Their three `You` plates, their `Call 400` buttons and everything else
  in this file above line 491 are untouched. Three `verify:` gates pin that.
- **The bare code, the `Invite link` label and the link box.** They move nowhere and lose nothing
  (`ADR-0110` §5, unamended in that part).
- **`Copy the link`, `Link copied.` and `Copy it from the box above.`** — shipped strings, drawn
  byte-identically, and `ADR-0128` §5 adds none. `ADR-0110` §6's enumeration gains **exactly one**
  member, `ADR-0129`'s sentence, and nothing else.
- **Every other card under `design/`.** `duel-table.html` is the only file in `design/` that draws
  this state — measured, not assumed.
- **Any mechanism for copying without the Clipboard API.** That is `DEC-150`, the architect's, and
  `ADR-0128` §7 says outright that it blocks nothing here.
- **The `_ds_manifest.json` push.** No card is added, so no manifest step is owed, and
  `DesignSync` is invoked by the human alone.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. It is written as
**exact counts** in the style `TASK-141401` established, so that a node added, a node removed and a
node edited are three different failures. Every count below was measured on a prototype built in
this worktree and reverted; the number after the arrow is what this ticket must produce.

| Gate | Proves |
| --- | --- |
| `class="frame"` 7 → **6**, `Host alone` 4 → **3**, and `Host alone — no clipboard API` absent | one frame retired, and retired rather than renamed |
| `<span class="name">You</span>` 7 → **3** | the host's plate is bare in all three, and the live table kept all three of its own |
| `Waiting for your rival`, `Copy the link`, `Back to the lobby` and the room-stays-open promise each 4 → **3** | the frame went whole, and no surviving frame lost furniture |
| `Link copied.` **1** and `Copy it from the box above.` **1** | the two feedback frames are still one each |
| `<p class="autostart">` **3** and `.autostart {` **1** | the sentence is drawn once per host-alone frame, off one rule |
| exactly **one** distinct `autostart` spelling | the three copies are byte-identical, so the client has one literal to transcribe |
| that spelling contains `rival` and `duel` | `ADR-0129` §4's vocabulary |
| that spelling contains no digit and none of *opponent, forever, second, minute, hour, expire, countdown, remaining, timer, timeout, button, click, tap, press, game* or `&` | the mechanical half of `ADR-0129` §3's six refusals, plus the no-entity rule the client's transcription needs |
| `class="linkbox` 4 → **3**, of which `class="linkbox sel"` is **1**, and `.linkbox.sel {` **1** | the selection is drawn in the hand-over frame and nowhere else |
| `ADR-0128` **1** and `ADR-0129` **1** | each amending decision is cited exactly once |
| `./design/check-drift.sh` exits 0 | tokens, values, suits, symbols and the lockup still hold (`ADR-0024` §2) — the two new rules mint nothing |

## Acceptance criteria

- [ ] `design/screens/duel-table.html` holds **6** `class="frame"` nodes and **3** headings
      containing `Host alone`, and the string `Host alone — no clipboard API` appears nowhere in it
- [ ] It holds exactly **3** `<span class="name">You</span>` nodes, all three in live-table frames
- [ ] `Waiting for your rival`, `Copy the link`, `Back to the lobby` and
      `The room stays open. That link still works for your rival, and it brings you back.` each
      appear exactly **3** times; `Link copied.` and `Copy it from the box above.` exactly **1**
- [ ] It holds exactly **3** `<p class="autostart">` nodes and one `.autostart {` rule, and the
      three nodes' text is **one** distinct string
- [ ] That string contains `rival` and `duel`, contains no digit, and contains none of the refused
      words the `verify:` block lists
- [ ] It holds **3** `class="linkbox` nodes, exactly one of them `class="linkbox sel"`, and one
      `.linkbox.sel {` rule
- [ ] `ADR-0128` and `ADR-0129` are each cited exactly once
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly one file, `design/screens/duel-table.html`
- [ ] Every command in `verify:` exits 0

> The human's visual verdict on the rendered card is the design review (`ADR-0024` §3) and it may
> **trail the merge** (`ADR-0091` §3). A rejection is a repair ticket against this card and against
> whatever transcribed it — which is why `TASK-140403` transcribes the literal from this file rather
> than repeating it.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
