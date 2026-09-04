---
schema: 2
id: TASK-131005
title: P2 — a refresh during a runout
type: task
status: backlog
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131004]
verify:
  - awk '/^\| `P2`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 4) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A reload issued **while a runout is being painted** is driven, and `STORY-1310`'s `P2` row says what
the reloading browser saw on the way back — in particular whether a lobby was among it.

## What is already merged about this path

`ADR-0102` §5 is titled *"A returning or reconnecting client **jumps to the end**, and the server is
why"*, and it explains the mechanism rather than promising a screen: `resumeFrames` sends no
`StreetDealt` at all, so the paced street steps are empty by construction and *"a reconnect
mid-runout, and a reload, both land on the finished board at once"* — after one final 600 ms step
that is left uniform on purpose.

So the claim under test is not *does it jump to the end* — that is a property of frames the server
does or does not send. It is `ADR-0112` §6's own words: ***"confirm no lobby shows on the way."***
That is a claim about the client's first paint, and it is the half no merged source has observed.

**A runout is a narrow window and the reload has to land inside it.** `ADR-0102` §2 paces a
hand-ending snapshot at one step per preceding `StreetDealt` plus a final step, 600 ms each, so an
all-in on the flop gives roughly 1.2–1.8 s of runout. On a bare stack that is enough time to issue
one `open`; getting the reload inside it reliably is what the delayed layout is for, and the row
must say which layout the reading came from.

## The stack

As `TASK-131003` sets it out — bare, and `delayed 300ms` with Vite on `5273` and
`node scripts/qa/delay.mjs 5173 5273 300 6173` in front of it. Fresh Chrome profiles from
`mktemp -d` every time (`ADR-0089` §3).

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md` §5,
`docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §6, and `scripts/qa/drive.mjs`.
No client source is opened, and none is changed.

## Scope

- **Manufacture a runout.** Two fresh profiles, a room, a duel. Get all the chips in before the
  river — `All in` on one seat and `Call` on the other is the shortest route — so the client has
  several board cards to paint in steps rather than one.
- **Reload inside the window, on the seat that is *not* driving the action** if that makes the
  timing easier. In order:
  1. `X record` **before** the call that starts the runout, then the call, then `X frames` — this is
     the runout as the product paints it, and it is the *before* the reload has to be compared with.
  2. Start the next runout and issue `X open` while the board is still filling in. Keep what `open`
     prints: it is the first paint after the navigation, and the only pre-frame observation a
     navigation allows, because a `MutationObserver` armed by `record` dies with the document.
  3. `X record` immediately after `open`, then `X frames` once the screen settles, then `X text`.
- **Both layouts.** Bare, then `delayed 300ms`. On the delayed layout the reload lands with a far
  wider gap between first paint and the first frame, which is the reading that decides the negative.
- **Write the `P2` row**: whether a lobby appeared in the first paint or in the frames, whether the
  board came back at the finished state as `ADR-0102` §5 says, the layout, and the short commit.

**Two failures are distinct and the row must not blur them.** *A lobby appeared* is `ADR-0112` §6's
question. *The board came back mid-runout rather than finished* would contradict `ADR-0102` §5 and is
a different, larger finding. Record both observations even when one of them is uneventful.

## Out of scope

- **The 600 ms step, the queue, and anything about pacing.** `ADR-0102` is merged and `ADR-0113` §6
  already amended its §6; this ticket measures a reload, not a timing.
- **The turn clock.** `STORY-1308` and `STORY-1309` are merged, so a countdown is on screen; it is
  not this ticket's subject and no assertion is made about it. If the reload visibly restarts a
  clock at its full allowance, that is worth a sentence in the PR body and belongs to `STORY-1311`'s
  successors, not here.
- **Any repair**, `/qa-cycle`, `docs/test-plan.md`, `A(N)`/`B(N)`, and any coverage claim
  (`ADR-0089` §§2b, 2c).

**If the reading needs a decision no merged source settles**, write the row, register the next free
`DEC` in `docs/adr/README.md`'s `## Open decisions`, and leave this ticket `blocked`.

## Tests

No test can be written, for `TASK-131003`'s merged reason. The four gates are document-shape checks
and one conformance check: the `P2` row is filled, at most four placeholders remain, the table still
has its seven rows — the row gate already fails on a **missing** row (`exit (bad || !ok)`,
probed), so this one guards the other six, and no `verify:` block in
this story runs a browser.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text**, unedited, for both layouts.

- [ ] A runout was reached and its **undisturbed** `frames` transcript is in the PR body, so the
      reloaded one has a baseline
- [ ] The reload was issued while the board was still filling in, and the PR body says how that was
      confirmed rather than assumed
- [ ] `open`'s first-paint output after the reload is in the PR body for both layouts
- [ ] The `frames` transcript after the reload is in the PR body for both layouts
- [ ] The `P2` row answers `ADR-0112` §6's question — *did a lobby show on the way* — separately from
      whether the board returned finished, and names the layout and the short commit
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
