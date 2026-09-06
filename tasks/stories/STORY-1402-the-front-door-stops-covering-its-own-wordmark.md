---
id: STORY-1402
title: The front door stops covering its own wordmark, and the door says Play duel
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, design, lobby, bug]
depends_on: [STORY-1401]
---

## Goal

The first screen of the product draws its wordmark with nothing painted over it, and its primary
control reads `Play duel` — on the card, in the client, and in every merged source that drives the
old string.

## Why

**It is the screen the player meets first and the screen the human photographed first.** Both
halves are decision-free: the overlap is a layout defect, and the rename is an answer the human
gave on 2026-09-06 when asked what the annotation *"play duel"* meant.

**Both halves land in the same frame, so the card is redrawn once.** The word is on the card and the
column is the card's; splitting them would draw `design/screens/create-duel.html`'s front-door frame
twice and ask the human for two verdicts on one screen.

The dependency on `STORY-1401` is the epic's stated order — *"item 3's defects go first"* — and not
a code coupling: the two touch disjoint files. It is declared because the run is sequential and
because `STORY-1403` and `STORY-1404` then queue behind this one on files they genuinely share.

## Design notes

Measured on `develop` at `7c39fd3d`, or merged. Nothing here is re-litigated by a ticket.

### What the card draws, and what the client ships

`design/screens/create-duel.html`'s *Before — the front door* frame is a flex **column** with
`gap: var(--pd-space-5)` (`.frame`, lines 37–38), holding: the wordmark lockup at `1.875rem`, a hero
heading, a sub-line, a filled button and a ghost button, and the note *"nothing else on the door —
no lobby noise, no tables list"*.

`web-client/src/lobby/Lobby.tsx`'s front-door branch is `<section className="p-6">` with **no
column, no gap and no width**, holding an `inline-flex` `<h1>` lockup, an inline-block button, a
`<form>`, the profile strip, the name surface and a `flex flex-col gap-2` div of three doors. Every
other branch of that file carries `mx-auto flex w-full max-w-[380px] flex-col items-center gap-4`.

**The overlap sits in that difference: this is the one branch that never got the column its card
draws.** It is the same neighbourhood `TASK-121303` measured one row lower, where three doors abutted
because *"the parent supplies no layout — no flex, no grid, no gap, nothing"*.

**That reading is where to look, not what to write.** The implementing ticket reproduces the overlap
in a browser at the front door before it changes anything and shows it gone afterwards; jsdom cannot
see two boxes overlap, so the executable gate is structural (the section carries the card's column)
and the visual verdict is the human's at the rendered card (`ADR-0024` §3), which may trail the
merge (`ADR-0091` §3).

### The word

`Play duel` — the human's own annotation on `edits1.png`, confirmed by them the same day to mean
*rename the button*, and transcribed in `EPIC-14`'s *The feedback* and its `STORY-1402` seam. It is
not the planner's choice and no ticket may improve on it; a different wording is a question for the
human, not a rewrite.

It replaces **two** strings that already disagree with each other: the client's `Create a duel room`
and the card's `Create a duel`.

### The blast radius of the string, counted

| Where | Occurrences |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | 1 |
| `web-client/src/App.test.tsx` | 18 |
| `web-client/src/lobby/Lobby.test.tsx` | 10 |
| `web-client/src/e2e/whole-duel.test.tsx` | 1 |
| `docs/test-plan.md` | 3 rows — `SMK-02`, `SMK-04`, `05-01`, each a drive a browser performs |
| `design/screens/create-duel.html` | 1 (as `Create a duel`) |

**Merged ADR prose that quotes the old string is not edited.** `ADR-0060`, `ADR-0094`, `ADR-0105`,
`ADR-0108`, `ADR-0110` and `ADR-0118` name *Create a duel room* while describing what was true when
they were written; an ADR is a record, and a change supersedes it rather than quietly editing it
(`ADR-0098` §3). A ticket that greps the ADR tree for the string has widened its scope.

### What does not change

- **`ADR-0098` §§1–2 stand.** The lockup renders on the front door and on no other screen, as the
  coin and two text elements, with the `aria-label` that pins the accessible name to `Poker Duels`.
  This story makes the mark **visible**; it moves it nowhere and puts it on nothing new.
- **`ADR-0118` §1's empty `/`.** The front door renders only once the server has spoken; the branch
  above it stays exactly as it is, and the column added here belongs to the front-door section
  alone.
- **The three doors, the name surface and the profile strip** keep their present markup. This story
  gives the section a column; it does not re-dress what stands in it.

### Design first

**This story owes a card ticket, and it is the first ticket** (`ADR-0091` §2, applied by the
planner's judgement to a surface whose card exists and would otherwise be left drawing a control
that no longer exists). Two things a coder must transcribe rather than invent are on that frame: the
word on the primary control, and the column that separates it from the wordmark. The card is
**composing** from the settled vocabulary rather than minting — both endpoints are declared tokens —
so it is an ordinary dispatched ticket, `module: design`, `review: light` (`ADR-0091` §3), and it
merges before the client ticket is startable.

## Tasks

Split into four on 2026-09-07. The counts below were **re-measured on `develop` at `77a09060`**,
after `TASK-140302` landed in `Lobby.test.tsx`, rather than inherited from the table above: the
client files still carry 1 / 18 / 10 / 1, `docs/test-plan.md` carries three rows but **four**
occurrences (`05-01` names the control twice on one line), and `design/` carries the string in
**three** files, not one — `create-duel.html`'s button, the same button on the component card
`flow-actions.html`, and one phrase in `enter-code.html`'s lede.

| Task | What it settles |
| --- | --- |
| [TASK-140201](../tasks/TASK-140201-the-cards-primary-control-says-play-duel.md) | The card, first (`ADR-0091` §2). Three design files, and **`create-duel.html`'s `.frame` is not touched** — it already draws the column, so the card was never wrong about the layout and the client is what failed to transcribe it. The card's own `<title>` and `<h1>` keep reading `Create a duel`: `design/README.md` says a `<title>` names the *card*, and the flow is still creating a duel |
| [TASK-140202](../tasks/TASK-140202-the-front-doors-primary-control-says-play-duel.md) | The rename where a merged gate holds it. Renaming `Lobby.tsx` alone was **run**: `1159 passed` became `17 failed \| 1142 passed`, all 17 inside `App.test.tsx` (9) and `Lobby.test.tsx` (8). `npm run check` therefore refuses every proper subset of those three files, and three is the ordinary cap — so this ticket declares **no `atomic:`** |
| [TASK-140203](../tasks/TASK-140203-the-two-names-no-gate-holds-say-play-duel.md) | The two references the same probe left **green**: `e2e/whole-duel.test.tsx`, whose reference is a *negative* observation that turns into a tautology if left stale, and `docs/test-plan.md`, which no gate reads at all. A second ticket because no gate binds them to the first, not because they felt separate |
| [TASK-140204](../tasks/TASK-140204-the-front-door-stands-in-the-cards-column.md) | The layout defect. One attribute on `Lobby.tsx:388` and two new tests, with a browser reading as the criterion |

**The overlap was reproduced before the split, so no ticket has to guess its mechanism.** The
front-door markup was rendered against the client's own built stylesheet in headless Chrome at both
shapes, with the section's element children joined without whitespace (JSX elides it —
`Lobby.tsx:437`). Reading `clearance = max(gapX, gapY)` between the wordmark's rect and the primary
control's:

| shape | today | with the column |
| --- | --- | --- |
| 390 × 664 | **0.000** — the button's top border sits on the wordmark's bottom edge | **12.000** |
| 720 × 900 | **0.000** — the button's left border sits on the wordmark's right edge, inside 37.5 px of its 45 px band | **12.000** |

**It is not occlusion.** `document.elementFromPoint`, sampled across both glyph boxes at both shapes,
returns a node inside the `<h1>` in every reading, before and after — so an "is anything drawn over
the mark" assertion is a gate that cannot fail, and `TASK-140204` says so rather than letting a coder
re-derive it. The gate that *can* fail is structural and runs in jsdom; the number is the
implementer's browser reading, `ADR-0089` forbidding a browser in any `verify:` block.

## Acceptance criteria

- [ ] The merged front-door card frame draws the primary control reading `Play duel` and the column
      that keeps it clear of the wordmark, with `./design/check-drift.sh` exiting 0 — merged before
      any implementing ticket is startable
- [ ] On the shipped front door, at 390 × 664 and at a laptop width, every glyph of the wordmark is
      visible with no control drawn over it — evidenced by a before-and-after capture in the
      implementing ticket's report, not by a jsdom assertion
- [ ] The front door's `<section>` carries the card's column, asserted structurally by a named test
- [ ] No source, test, plan or card in the repository outside `docs/adr/` names `Create a duel room`
      or `Create a duel` as a control; `Play duel` is what each names instead
- [ ] `docs/test-plan.md`'s `SMK-02`, `SMK-04` and `05-01` name the new string, so each still
      describes a drive a browser can perform
- [ ] `ADR-0098` §§1–2 still hold: the lockup renders on the front door and on no other screen, and
      its accessible name is still `Poker Duels`
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0 and `./design/check-drift.sh`
      exits 0

## Out of scope

- **The `Account` door's font size.** `edits1.png`'s *bigger font* annotation lands inside the
  **open `DEC-094`** — may a control that no card draws wear the client's control vocabulary? — which
  is already the product owner's and already has two QA rounds' findings spent on it. The three doors
  render with `className=""` today; growing one of them here would answer `DEC-094` by increment,
  which is the one thing a planner may not do. **No new `DEC` is registered: the question is already
  open and correctly asked**, and the human's annotation is a third demand on it. It lands in
  whatever story that answer yields.
- **The profile strip's removal** (*remove*, crossed out on `edits1.png`) — answered by
  [`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
  §5: the strip's `no-profile` branch renders nothing, so `No profile yet.` goes and the `profile`
  branch stays. It lands in `STORY-1408`, not here.
- **The room-code field's behaviour** — `STORY-1403`. This story does not touch the form.
- **The card's other arrears.** The front-door frame also draws a hero heading, a sub-line and an
  `I have a code` ghost button that the client has never shipped, and `design/screens/enter-code.html`
  already records that the code field lives inline *"beside Create a duel room"* instead. That drift
  is older than this epic and is a separate question — about what the door offers, not what it says —
  and reconciling it inside this card ticket would widen a rename into a redesign.
- **The phone fit.** `DEC-136` and `STORY-1405` own what happens to any screen below 390 px.
