---
schema: 2
id: TASK-130402
title: The two table cards carry the last act in place, and the host-alone frames carry none
type: task
status: done
parent: STORY-1304
module: design
estimate: S
tier: sonnet
review: light
files_touched: 2
labels: [design, table]
depends_on: [TASK-130401]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"last-act") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"last-act") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, ".last-act") { n++ } END { exit (n < 1) }' design/screens/duel-table.html
  - awk 'index($0, ".last-act") { n++ } END { exit (n < 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"seat") { n++ } END { exit (n != 12) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"seat") { n++ } END { exit (n != 6) }' design/screens/duel-table-states.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 6) }' design/screens/duel-table.html
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 3) }' design/screens/duel-table-states.html
  - awk 'index($0, "viewport phone") { n++ } END { exit (n != 5) }' design/screens/duel-table.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 16) }' design/screens/duel-table.html
  - awk 'index($0, "role=\"img\"") { n++ } END { exit (n != 24) }' design/screens/duel-table-states.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' design/screens/duel-table.html
  - awk 'index($0, "@keyframes") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - awk 'index($0, "animation") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "animation") { n++ } END { exit (n != 2) }' design/screens/duel-table-states.html
  - awk 'index($0, "transition") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "transition") { n++ } END { exit (n != 0) }' design/screens/duel-table-states.html
  - awk 'index($0, "<h2>") { host = index($0, "Host alone") } host && index($0, "class=\"last-act") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "<h2>") { host = index($0, "Host alone") } host && index($0, "Waiting for your rival") { n++ } END { exit (n < 1) }' design/screens/duel-table.html
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two cards that draw the duel table show the last-act mark **in place**: at the phone's own
390 × 664 box, so `ADR-0103` §1's fit is a drawing and not a claim; standing beside `ADR-0095` §4's
award line on the fold frame, which is `ADR-0109` §3's own worked example; and **absent from all
four host-alone frames**, because a mark is a game fact and no game fact is stated before the
opening `Snapshot` (`ADR-0110` §3).

## Why both files, and why no frame is added

`grep -rl "class=\"seat" design/` finds three files. `components/seat-and-pot.html` is canonical and
`TASK-130401` drew it; these two declare the seat plate as *"a faithful copy"* of it, so leaving them
behind would draw one class two ways.

**No frame is added and none is needed.** `duel-table.html`'s two live frames already put the hero
on turn facing a bet — the rival's `committed 400` and the bar's `Call 400` — and its four
host-alone frames are exactly the `view === null` case. `duel-table-states.html`'s three frames
already cover a mid-hand wait, a showdown and a fold. Adding frames would copy sixty lines of table
markup to say something already on screen, and would move `role="img"` and `class="frame"` off the
numbers three earlier tickets pinned them at.

## The act each frame gets, and why it is the only coherent one

**Do not invent numbers.** Each mark below is the only act consistent with the figures the frame
already prints; a mark that contradicts them is the *false pair* `ADR-0109` rejects two alternatives
on.

| File | Frame | Mark sits at | It prints | Why that one |
| --- | --- | --- | --- | --- |
| `duel-table.html` | `Laptop — 720 × 900` | `ImKate`'s plate | `Bet` `400` | Her committed row says `400` and the hero's bar says `Call 400`: she bet 400 on the flop |
| `duel-table.html` | `Phone — 390 × 664` | `ImKate`'s plate | `Bet` `400` | Same table markup at the other shape — the pair is what makes the fit checkable by diff |
| `duel-table.html` | the four `Host alone` frames | — | **nothing** | No `Snapshot` yet, so no act exists to mark (`ADR-0110` §3) |
| `duel-table-states.html` | `Waiting — their turn on the turn card` | `You`'s plate | `Check` | Both bet lines are empty on a fresh turn card and `ImKate` has the button, so the non-button hero opened the street by checking and it is now her turn. Bare, and at the **hero's** seat — `ADR-0109` §1's *a player's own act is marked the same way* |
| `duel-table-states.html` | `Showdown — you win, the loser mucks` | `ImKate`'s plate | `Call` `800` | The card's own arithmetic: `3,250` + 800 + 800 = the `4,850` banner, and `2,950` + `12,200` + `4,850` = 20,000. She called the hero's river bet of 800 |
| `duel-table-states.html` | `Fold — you win on the river, nobody shows` | `ImKate`'s plate | `Fold` | Its note already says *"your river bet of 800 goes uncalled"*. Bare, and standing beside the award banner — `ADR-0109` §3's *a fold's mark stands through the award window, telling the why beside the award line's who* |

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-table-states.html` | modify |
| `design/components/seat-and-pot.html` | read |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | read |

## Scope

- **Copy `.last-act` character for character** from the merged `components/seat-and-pot.html` into
  each card's seat block, beside its `.seat` rules, exactly as `.seat.on-turn`'s rule was copied.
  Never re-derived: two spellings of one class is the drift `check-drift.sh` exists to catch and
  cannot see.
- **One `<span class="last-act">` per row in the table above**, inside the plate, in the same
  position the canonical card puts it.
- **The four host-alone frames get nothing** — no element, no class, no caption about a mark. That
  absence is `ADR-0110` §3 drawn, and the client half of it is `TASK-130408`.
- **Nothing else on either card moves.** Not a stack, not a pot, not a card, not the acting seat's
  pulse. The gates pin `class="seat` at 12 and 6, `class="frame"` at 6 and 3, `viewport phone` at 5,
  `role="img"` at 16 and 24, `@keyframes` at 1 each and `animation` at 2 lines each — all measured
  on `develop` 2026-09-02.
- **The mark does not move and mints no token** (`ADR-0109` §4, `ADR-0115`): no new `@keyframes`, no
  `animation`, no `transition`, and every `--pd-*` it names is already declared — `check-drift.sh`
  clause 1 fails otherwise, and clause 3 fails if an inlined value drifts from the sheet.

## Out of scope

- **The canonical card.** `TASK-130401` merged it; if the drawing is wrong, that is a repair ticket
  against that card, not a second spelling here.
- **Any client code.** `app.css`, `SeatPlate.tsx`, `DuelTable.tsx` and `Lobby.tsx` are
  `TASK-130406`–`TASK-130408`.
- **A new frame, a new state, or a *stale mark* treatment.** `TASK-130401` names why the seventh
  state is not drawn in this story.
- **`create-duel.html`, `duel-end.html` and the other screens.** None of them draws a seat plate in
  a live hand.

## Tests

**No test file, and none is possible** — a design card is HTML nobody imports, and `ADR-0089` §2b
forbids a browser measurement being a gate. The gates are the `verify:` block: two say how many
marks each card carries, twelve refuse any movement in what was already there, and
`check-drift.sh` says the tokens, values, suit glyphs, symbols and lockup still hold.

| Marker | duel-table.html today → after | duel-table-states.html today → after |
| --- | --- | --- |
| `class="last-act` | 0 → **2** | 0 → **3** |
| `class="seat` | 12 → **12** | 6 → **6** |
| `class="frame"` | 6 → **6** | 3 → **3** |
| `role="img"` | 16 → **16** | 24 → **24** |
| `viewport phone` | 5 → **5** | — |
| `@keyframes` / `animation` / `transition` | 1 / 2 / 0 → **unchanged** | 1 / 2 / 0 → **unchanged** |

**The host-alone refusal is gated in two halves, and the second is the guard on the guard.** One
`awk` walks `<h2>` headings, latches inside every `Host alone` section and counts `class="last-act`
there — it must be **0**. A section-walk that latched onto nothing would report 0 forever, so a
second `awk` with the identical walk counts `Waiting for your rival` in the same sections and must
find at least one; measured on `develop` 2026-09-02 it finds **4**.

## Acceptance criteria

- [ ] `./design/check-drift.sh` exits 0
- [ ] `class="last-act` appears exactly **2** times in `duel-table.html` and exactly **3** in
      `duel-table-states.html`, and each file mentions the `.last-act` rule at least once
- [ ] `class="seat` is still 12 and 6, `class="frame"` still 6 and 3, `viewport phone` still 5, and
      `role="img"` still 16 and 24
- [ ] `@keyframes` is still exactly 1 in each file, `animation` still exactly 2 lines in each, and
      `transition` still 0 in each
- [ ] No `class="last-act` appears inside any `Host alone` section of `duel-table.html`, and the
      same section walk still finds `Waiting for your rival` there
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
