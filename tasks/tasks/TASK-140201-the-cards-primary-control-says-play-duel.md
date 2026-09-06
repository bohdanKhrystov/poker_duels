---
schema: 2
id: TASK-140201
title: The cards' primary control says Play duel
type: task
status: ready
parent: STORY-1402
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [design, lobby]
depends_on: []
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "<button class=\"btn fill\">Play duel</button>") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - awk 'index($0, "<button class=\"btn fill\">Play duel</button>") { n++ } END { exit (n != 1) }' design/components/flow-actions.html
  - awk 'index($0, "Create a duel") { n++ } END { exit (n != 2) }' design/screens/create-duel.html
  - awk 'index($0, "Create a duel") { n++ } END { exit (n != 0) }' design/components/flow-actions.html
  - awk 'index($0, "Create a duel") { n++ } END { exit (n != 0) }' design/screens/enter-code.html
  - awk 'index($0, "beside Play duel (ADR-0060") { n++ } END { exit (n != 1) }' design/screens/enter-code.html
  - sh -c '! grep -rqF "Create a duel room" design'
  - awk 'index($0, "<button class=\"btn ghost\">I have a code</button>") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - awk 'index($0, "<button class=\"btn ghost\">I have a code</button>") { n++ } END { exit (n != 1) }' design/components/flow-actions.html
  - awk 'index($0, "column; gap") { n++ } END { exit (n != 1) }' design/screens/create-duel.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every card in `design/` that names the front door's primary control names it `Play duel`. No card's
own title moves, no frame gains or loses a node, and the column the front-door frame already draws
is byte-unchanged.

## This is the story's first ticket, and the client waits on it

`ADR-0091` §2: the card is what a coder transcribes, so it merges before anything transcribes it.
`TASK-140202` and `TASK-140204` both depend on this ticket for that reason and for no other — they
touch no file it touches.

This card is **composing**, not minting: the word replaces a word, no token is created and no rule
changes, so it is an ordinary dispatched ticket at `review: light` (`ADR-0091` §3). The human's
visual verdict on the rendered card may trail the merge (`ADR-0024` §3, `ADR-0091` §3).

## Files

| File | Action |
| --- | --- |
| `design/screens/create-duel.html` | modify |
| `design/components/flow-actions.html` | modify |
| `design/screens/enter-code.html` | modify |

**Nothing else is opened.** The word is the human's own, given on 2026-09-06 when they were asked
what the annotation *"play duel"* on `edits1.png` meant. It is not this ticket's to improve on;
a different wording is a question for the human, not a rewrite.

## Scope

- **`design/screens/create-duel.html:86`** — the *Before — the front door* frame's filled control.
  The node becomes exactly:

  ```html
        <button class="btn fill">Play duel</button>
  ```

- **`design/components/flow-actions.html:103`** — the same control in *The standalone action*, the
  card that says of itself *"this card is where a retune starts: change it here, then the copies."*
  The node becomes exactly:

  ```html
      <button class="btn fill">Play duel</button>
  ```

  Its caption two lines below — *"fill is the one bright thing on a lobby screen…"* — does not
  change; it describes the treatment, not the word.
- **`design/screens/enter-code.html:79`** — one phrase inside the lede. `beside Create a duel room
  (ADR-0060 §§1, 4; ADR-0094 §2)` becomes `beside Play duel (ADR-0060 §§1, 4; ADR-0094 §2)`. The
  sentence's claim — that the code field lives *beside* that control — is untouched, and so are both
  citations. Only the control's name moves.

## Out of scope

- **`design/screens/create-duel.html`'s `<title>` (`:7`) and `<h1>` (`:74`), both reading `Create a
  duel`.** `design/README.md` says *"A `<title>` names the card"* — these two name the **card**, and
  the card is about creating a duel, which is still what the flow does. A `verify:` gate pins
  `Create a duel` in that file at exactly **2** occurrences, so removing either fails this ticket.
  Renaming the card, and the file with it, is a larger move than renaming a control and nothing here
  asks for it.
- **The `.frame` rule at `:37-38`.** It already draws the column this story is about — `display:
  flex; flex-direction: column; gap: var(--pd-space-5)`. The card was never wrong about the layout;
  the client is what never transcribed it. A gate pins `column; gap` at exactly **1** occurrence.
- **The `I have a code` ghost button**, on either card. Two gates pin it at 1 occurrence each.
- **Every other card**, and every SVG. A sweep across `design/` was run at planning time and found
  the string in exactly these three files.
- **`docs/adr/`.** `ADR-0060`, `ADR-0094`, `ADR-0105`, `ADR-0108`, `ADR-0110`, `ADR-0118` and
  `ADR-0124` name *Create a duel room* while recording what was true when they were written. An ADR
  is a record and a change supersedes it rather than quietly editing it (`ADR-0098` §3). A ticket
  that greps the ADR tree for this string has widened its scope.
- **The front-door frame's other arrears** — the hero heading, the sub-line and the `I have a code`
  ghost button that the client has never shipped. That drift is older than this epic and is about
  what the door *offers*, not what it *says*. `STORY-1402`'s Out of scope owns it; reconciling it
  here would widen a rename into a redesign.
- **The client.** `Lobby.tsx` still says `Create a duel room` after this merges, and `TASK-140202` is
  what changes it. A card ahead of its client is `ADR-0091` §2 working as designed.

## Tests

None: a card is not executable, and the `verify:` block is the whole assertion. It is written as
**exact counts** in the style `TASK-141401` established, so that a node added, a node removed and a
node edited are three different failures.

**Every gate below was run against `develop` at `77a09060` and against the finished edit.** Six are
**red today**; four are green today and are regression guards.

| Gate | Proves | Today |
| --- | --- | --- |
| `<button class="btn fill">Play duel</button>` is 1 in each of the two cards | the control is spelled exactly, on both, once | **red** |
| `Create a duel` is 2 in `create-duel.html` | the button's occurrence went and the card's own title and `<h1>` stayed | **red** (3) |
| `Create a duel` is 0 in `flow-actions.html` and in `enter-code.html` | neither file keeps a stale name | **red** |
| `beside Play duel (ADR-0060` is 1 in `enter-code.html` | the lede's phrase was edited in place, keeping its citations | **red** |
| `grep -rF "Create a duel room" design` finds nothing | no card anywhere under `design/` still carries the old string | **red** |
| `<button class="btn ghost">I have a code</button>` is 1 in each card | the ghost button beside it did not move | green — a regression guard |
| `column; gap` is 1 in `create-duel.html` | the frame's column rule was not touched while editing a button four lines below it | green — a regression guard |
| `./design/check-drift.sh` | tokens, values, suits, symbols and the lockup still hold (`ADR-0024` §2) | green — a regression guard |

## Acceptance criteria

- [ ] `design/screens/create-duel.html` contains `<button class="btn fill">Play duel</button>`
      exactly once and `Create a duel` exactly twice — the `<title>` and the `<h1>`
- [ ] `design/components/flow-actions.html` contains `<button class="btn fill">Play duel</button>`
      exactly once and `Create a duel` zero times
- [ ] `design/screens/enter-code.html` contains `beside Play duel (ADR-0060` exactly once and
      `Create a duel` zero times
- [ ] No file under `design/` contains `Create a duel room`
- [ ] `I have a code` is still one ghost-button node in each of the two cards, and `column; gap` is
      still one occurrence in `design/screens/create-duel.html`
- [ ] `./design/check-drift.sh` exits 0
- [ ] The diff touches exactly three files, all under `design/`, and no file under `docs/adr/`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
