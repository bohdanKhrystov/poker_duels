---
schema: 2
id: TASK-121004
title: The front door finishes the card `TASK-120901` started — the fill, the code well, the wordmark
type: task
status: done
parent: STORY-1210
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the primary call to action is filled, not ghosted"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the room code is the card's code well"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The three things `TASK-120901` did not promise — the primary button's accent fill, the room code's
type treatment, and the wordmark — match `design/screens/create-duel.html`, so the front door reads
as the card draws it rather than as an outline of it.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`. **This is not a regression**,
and the check was run rather than assumed: `TASK-120901`'s three controls are all still dressed at
`07df9e7f`, and so is the whole waiting frame. What round 2 found is what that ticket never
promised — its *Scope* was *"dress … with the client's existing token classes"* and its gate asserted
a non-empty class from that vocabulary.

**1. The primary call to action is a ghost where the card draws a fill.**

```
BUTTON "Create a duel room"
cls "rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text"
backgroundColor "rgba(0, 0, 0, 0)"
```

Card: `<button class="btn fill">Create a duel</button>`, and
`.btn.fill { background: var(--pd-accent-fill); color: var(--pd-on-accent); }`. The fill treatment
exists and works elsewhere in this client — the duel table's *Raise to* renders
`border-transparent bg-accent-fill text-on-accent` — so the door's most important control is the
only ghost among equals. *Copy the link* and *Join the duel* are the same.

**2. The room code is boxed but not the card's code well.** One particular of the round-2 report is
corrected here: the code does **not** render as plain body text. It carries
`rounded-medium border border-hairline bg-surface px-5 py-4 text-text` and computes
`background-color: rgb(28, 26, 24)`. What it lacks is the type treatment:

```
shipped : -apple-system … / 15px / letter-spacing normal / text-align start
card    : var(--pd-font-mono) / 1.875rem / var(--pd-track-code) / centered   (create-duel.html:72)
```

The card's own lede says why it matters: the code is *"big enough to read across a room"*.

**3. The wordmark is a plain title.** `<h1 class="text-title">Poker Duels</h1>`, weight 400, one text
node. The card's front-door frame draws
`<span class="mark"><span class="coin"></span>Poker<span class="duels">Duels</span></span>` —
a coin, a bold *Poker*, a muted *Duels* (`create-duel.html:46, 115`).

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- **Give the front door's primary control the card's fill**: `bg-accent-fill` and `text-on-accent`,
  keeping the padding and radius `TASK-120901` already applied. *Create a duel room* is the primary;
  the waiting frame's *Copy the link* is the primary there.
- **Give the room code the card's type treatment**: the mono family, the card's size, the code
  tracking token and centring. The box `TASK-120901` gave it stays.
- ~~**Give the header the card's wordmark**: the coin, a bold *Poker*, a muted *Duels*.~~
  **Struck on 2026-08-31 and disposed by
  [`ADR-0098`](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md)
  (`DEC-099`).** The markup this item quotes is not in `Lobby.tsx` and never was: the
  unconditional `<h1 className="text-title">Poker Duels</h1>` lives in `web-client/src/App.tsx`
  and renders on **every** screen, so dressing it as the card draws it would put the front door's
  lockup on all eleven — a change to what a player sees everywhere, not a dressing change on one
  screen, and outside this ticket's two-file table. The coder refused to guess and routed it
  (`CLAUDE.md` rule 5). The product owner answered from the merged cards, which hold exactly one
  `.mark` between them: the lockup is the **front door's alone** and the `h1` leaves `App.tsx`.
  The client work is its own ticket under `STORY-1210`, written by the planner from `ADR-0098`'s
  file set — `App.tsx`, `App.test.tsx`, `Lobby.tsx`, `Lobby.test.tsx` — because four merged
  `App.test.tsx` assertions that have gated since `EPIC-03` are rewritten deliberately by it.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes). If the card's `1.875rem` has no client utility, it is
  a token question and it is worked with the human, not minted here.

## Out of scope

- **Every string on this screen.** *Copy the link* vs the card's *Copy link* is `TASK-120911`'s, in
  the card's direction. *Back to the lobby* and *The room stays open…* are `ADR-0073`'s own words and
  are correct as shipped. **Change no literal.**
- **The card's front-door structure** — the *Challenge someone* lede, the *Create a duel* /
  *I have a code* pair, and the separate code screen behind the second. That needs a product
  decision; it is `TASK-120907`, which is not startable until its `DEC` is answered.
- **The waiting frame's seat plates.** Composition, not dressing, and it needs the same decision
  `TASK-120907` names.
- **The three lobby door buttons** — *Your duels*, *Leaderboard*, *Account*. They are unclassed, and
  **no card draws them**: `create-duel.html`'s front-door frame carries the note *"nothing else on
  the door — no lobby noise"*. They contradict nothing and are deliberately not filed
  (`STORY-1210` §*What was not filed*).
- **`Forgot your password?`.** Same file, different card: `TASK-121010`, which depends on this one
  so two coders never hold `Lobby.tsx` at once. The sign-in submit is a second ticket in a second
  file, `TASK-121005`; the two were one ticket until the 2026-08-31 split.

## Tests

`Lobby.test.tsx`

| Test | Proves |
| --- | --- |
| `the primary call to action is filled, not ghosted` | *Create a duel room*'s class list contains **both** `bg-accent-fill` and `text-on-accent`; and, in the waiting state, so does *Copy the link*. Named tokens, not merely non-empty — a non-empty check is what let the ghost treatment ship |
| `the room code is the card's code well` | the element holding the room code carries a mono-family utility **and** the code tracking utility. Two, because a mono code at body size and normal tracking is still not the well the card draws |

**Why the `verify:` block runs the file twice.** `--reporter=verbose` prints a test's name whether it
passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The greps prove the
named tests **exist**; the third command runs the file with **no pipe**, so its exit code is the
suite's and proves they **pass**, and it re-runs `TASK-120901`'s three merged dressing tests so this
change cannot undo them. `NO_COLOR=1` is set because ANSI escapes break a fixed-string grep.

**No test for the wordmark's appearance.** A two-tone lockup with a coin is a rendering judgment
`ADR-0024` §3 puts with the human at the pane; what a test can honestly assert is that the header
renders the coin mark and two separate text elements, and that is the third assertion inside the
first test rather than a gate pretending to grade a lockup.

## Acceptance criteria

- [ ] `Lobby.test.tsx > the primary call to action is filled, not ghosted` passes
- [ ] `Lobby.test.tsx > the room code is the card's code well` passes
- [ ] `TASK-120901`'s three merged tests still pass unchanged
- [ ] Reverting `Lobby.tsx` alone reddens both new tests
- [ ] **By hand, on a live stack** — open `/` and see a filled primary button; create a room and read
      the code as large, tracked, centred monospace
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
