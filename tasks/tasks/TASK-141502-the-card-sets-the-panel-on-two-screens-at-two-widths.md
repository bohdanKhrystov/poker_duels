---
schema: 2
id: TASK-141502
title: The card sets the panel on two screens, at the phone and on the laptop
type: task
status: done
parent: STORY-1415
module: design
estimate: S
tier: sonnet
review: light
files_touched: 1
labels: [design, rematch, notice]
depends_on: [TASK-141501]
verify:
  - ./design/check-drift.sh
  - awk 'index($0, "class=\"frame\"") { n++ } END { exit (n != 4) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"frame over phone\"") { n++ } END { exit (n != 2) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"frame over laptop\"") { n++ } END { exit (n != 2) }' design/screens/rematch-panel.html
  - awk 'index($0, "<h2>") { n++ } END { exit (n != 8) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"panel\"") { n++ } END { exit (n != 7) }' design/screens/rematch-panel.html
  - awk 'index($0, "<h3>Leaderboard</h3>") { n++ } END { exit (n != 2) }' design/screens/rematch-panel.html
  - awk 'index($0, "<h3>Account</h3>") { n++ } END { exit (n != 2) }' design/screens/rematch-panel.html
  - awk 'index($0, "390px") { n++ } END { exit (n < 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "Your rival offers a rematch") { n++ } END { exit (n != 5) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"btn fill\"") { n++ } END { exit (n != 5) }' design/screens/rematch-panel.html
  - awk 'index($0, "class=\"btn ghost\"") { n++ } END { exit (n != 7) }' design/screens/rematch-panel.html
  - awk 'index($0, "That duel room is gone.") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "dealing hand 1…") { n++ } END { exit (n != 1) }' design/screens/rematch-panel.html
  - awk 'index($0, "ADR-0121") { n++ } END { exit (n < 1) }' design/screens/rematch-panel.html
  - awk 'tolower($0) ~ /@keyframes|animation|transition|z-index/ { n++ } END { exit (n != 0) }' design/screens/rematch-panel.html
  - perl -ne 'print "$1\n" while m{<button class="btn ghost">([^<]*)</button>}g' design/screens/rematch-panel.html | sort -u > "${TMPDIR:-/tmp}/pd-panel-dismiss2.txt"
  - test "$(wc -l < "${TMPDIR:-/tmp}/pd-panel-dismiss2.txt")" -eq 1
  - test "$(cat "${TMPDIR:-/tmp}/pd-panel-dismiss2.txt")" = "Not now"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The panel card shows the panel **over something**: the ladder and the account screen, each at the
phone width `ADR-0103` §1 fixes and on the laptop. `ADR-0123` §8's last bullet, in four frames —
*"what the panel covers is the thing most likely to be wrong"*.

## Files

| File | Action |
| --- | --- |
| `design/screens/rematch-panel.html` | modify |

Read [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §8,
[`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
§1, and the two cards these frames stand in for — `design/screens/leaderboard.html` and
`design/screens/account.html`. **Nothing outside the table above is changed**, and neither of those
two cards is edited.

## Scope

- **Four frames appended after `TASK-141501`'s four**, each with its own `<h2>`:
  1. `<div class="frame over phone">` — the panel over the ladder at **390 px**;
  2. `<div class="frame over phone">` — the panel over the account screen at 390 px;
  3. `<div class="frame over laptop">` — the panel over the ladder at the card's own wide column;
  4. `<div class="frame over laptop">` — the panel over the account screen, the same.
- **The screens beneath are stand-ins, not copies.** Each is a `<div class="screen">` holding an
  `<h3>` naming the screen — exactly `Leaderboard` and `Account`, the shipped `LADDER_HEADING` and
  `ACCOUNT_HEADING` — plus enough shape to show what the panel covers. A copy of either card's
  markup would go stale the moment that card moves; a stand-in that names the screen cannot.
- **Only the first state is placed.** All four frames carry the offer standing, because the question
  these frames answer is *what does it sit on and what does it cover*, and drawing three states over
  two screens at two widths is twelve frames of the same answer. Gates hold the other two states at
  **1** each, so a coder who copies the wrong frame fails.
- **The 390 px frame is a real 390 px box** — `width: 390px` on the viewport stand-in, not a
  description of one. `ADR-0121`'s fit axis is `scrollWidth ≤ clientWidth`, so a panel that overruns
  is visible here or nowhere, and a note cites `ADR-0121` for it.
- **No motion, no `z-index`, and no new token**, exactly as `TASK-141501` fixed them. The gates are
  repeated here because this ticket reopens the file.

## Out of scope

- **The panel's own shell.** Its box, colours, width and place are `TASK-141501`'s and are not
  re-decided; these frames reuse the `.panel` rule that ticket wrote.
- **`design/screens/leaderboard.html` and `design/screens/account.html`.** Neither is opened.
- **Any client file.**

## Tests

None: a design card's structural gates are its `verify:` block, and its visual verdict is the
human's (`ADR-0024` §3).

## What would still pass if this were drawn wrong

The four counts that move — `<h2>` 4 → 8, `class="panel"` 3 → 7, `btn fill` 1 → 5, `btn ghost`
3 → 7 — are what stop a coder satisfying this with four empty frames or four frames that draw no
panel. The two states held at **1** are what stop a copy-paste of the wrong frame. And
`class="frame"` stays at **4**: the new frames carry `class="frame over phone"` and
`class="frame over laptop"`, so a coder who drops the size class fails the first gate rather than
passing a looser one.

**The `4`, `3`, `1`, `1` these numbers start from are `TASK-141501`'s own gates, not a measurement
of `develop`** — that ticket cannot merge with any other count, so they cannot be stale here.
`class="frames"`, the container, shares a prefix with `class="frame"`; every gate below closes the
quote for that reason.

## Acceptance criteria

- [ ] `class="frame"` is still **4**; `class="frame over phone"` is **2** and
      `class="frame over laptop"` is **2**
- [ ] `<h2>` is **8** and `class="panel"` is **7**
- [ ] `<h3>Leaderboard</h3>` and `<h3>Account</h3>` each appear **twice**
- [ ] `390px` appears at least once
- [ ] `Your rival offers a rematch` is **5**, `class="btn fill"` is **5**, `class="btn ghost"` is
      **7** with the single label `Not now`
- [ ] `That duel room is gone.` and `dealing hand 1…` are still **1** each
- [ ] `ADR-0121` is cited by name in a note
- [ ] None of `@keyframes`, `animation`, `transition`, `z-index` appears anywhere
- [ ] `./design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
