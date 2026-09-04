---
schema: 2
id: TASK-131006
title: P3 — a genuinely dropped socket
type: task
status: backlog
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131005]
verify:
  - awk '/^\| `P3`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 3) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A socket is dropped **underneath a page that keeps running**, so `reconnecting.ts` opens the next one
by itself, and `STORY-1310`'s `P3` row says what the player saw while it did.

## Why this is not any reconnect the catalogue already has

`ADR-0112` §6 asks for *"a **genuinely dropped socket** — a reconnect through `reconnecting.ts`,
**not** a reload"*, and the emphasis is the whole path. Everything the harness could do before
`TASK-131002` destroys the document and takes `reconnecting.ts` with it:

- `drive.mjs close` closes the **tab** — that is `CORE-18`, a player closing a window.
- `open` is a navigation, which `docs/test-plan.md` already records as *"a disconnect on this
  browser… it differs from `close` only in that the client resumes immediately"* — that is `CORE-17`
  and `CORE-19`.

The event nobody has driven is the network going away while the page lives: the socket closes, the
store keeps its state, `retryDelayMillis` waits (500 ms, doubling, equal jitter, capped at 10 s), a
fresh socket sends `Hello`, `boot.ts` re-sends `JoinRoom` on the `Welcome`, and the frames come back
into a tree that never unmounted. **What the player is shown during that gap is the measurement.**

## The stack

The **delayed layout only**, because the cut lives in the relay: Vite on `5273`,
`node scripts/qa/delay.mjs 5173 5273 <delayMs> 6173` on `5173`, and the cut issued with
`node scripts/qa/delay.mjs cut 6173`. Drive it twice, at `delayed 0ms` and at `delayed 300ms` — the
zero-delay run is the closest this instrument gets to a bare stack, and the row names both.

Otherwise as `TASK-131003` sets it out, with fresh Chrome profiles from `mktemp -d` (`ADR-0089` §3).

## What the relay's self-test does *not* prove, and this drive depends on

`TASK-131002`'s review found one substantive gap: **`--selftest-cut` never demonstrates that the
relay still accepts new connections after a cut.** It closes its servers immediately after asserting,
so a relay that severed its pairs *and stopped listening* would pass every gate.

That is precisely what this path needs. `P3` cuts a live socket and then expects the client to
**reconnect through the relay** — if the relay were deaf afterwards, the reconnect would fail and the
reading would look like a product defect in `reconnecting.ts` rather than an instrument fault.

The code never calls `server.close()` on the normal path, so it should hold. **Establish that it
does before trusting a negative reading**: cut, then confirm a fresh connection is accepted, and say
in the PR body which you observed. A `P3` row reporting "did not reconnect" is only about the product
if the relay was still listening.

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §6,
`web-client/src/protocol/reconnecting.ts`, and `scripts/qa/drive.mjs`. No source is changed.

## Scope

- Two fresh profiles through the relay, a room, a duel under way — a `PLAYING` room with a board and
  two stacks on screen.
- **Arm the observer first.** `X record` on the browser about to lose its socket, then the cut, then
  `X frames` once the screen has settled. This is the one path in the story where `record` genuinely
  works, because no navigation happens and the document survives — use it, and do not substitute
  `absent`, whose 250 ms sampling is exactly what this instrument exists to get past.
- `node scripts/qa/delay.mjs cut 6173`, then in order: `X text` immediately, `X frames` once the
  table is back, `X text` again, and `X eval "location.hash"`.
- **Watch the other seat too.** `Y record` before the cut and `Y frames` after: `ADR-0108` retired
  the pause and `ADR-0113` §7 deleted `isPaused`, so the rival's screen should keep running with a
  clock. Whether it marks the cut seat away — `CORE-18`'s marking — is part of this row.
- **Write the `P3` row**: what the dropped browser showed between the cut and the frames returning
  (a lobby, the stale table, a notice, a blank), how long it took to return, what the rival showed,
  the delay the relay was running at, and the short commit.

**The failure this path is looking for is a lobby, or a lost screen, in a tree that never
unmounted.** `roomCode`, `view` and `outcome` all live in the store and nothing clears them on a
socket close, so the expectation is that the table simply stands. A reading that shows otherwise is
a finding larger than anything `STORY-1311` is scoped for, and the row must say so plainly.

## Out of scope

- **Reconnect timing as a claim.** `retry-delay.ts` and `reconnecting.test.ts` already pin the
  schedule; the seconds observed here go in the PR body as context, never as an assertion about the
  backoff.
- **Presence and the away marking as a subject.** `CORE-18`/`CORE-19` own that. It is recorded here
  because it is on screen, not tested here.
- **The turn clock's behaviour across a drop.** `ADR-0108` decided it (*"the clock is indifferent to
  the socket"*) and `STORY-1308` shipped it. Note what the countdown did in the PR body; assert
  nothing.
- **Any repair**, `/qa-cycle`, `docs/test-plan.md`, `A(N)`/`B(N)`, and any coverage claim
  (`ADR-0089` §§2b, 2c).

**If the reading needs a decision no merged source settles**, write the row, register the next free
`DEC` in `docs/adr/README.md`'s `## Open decisions`, and leave this ticket `blocked`.

## Tests

No test can be written, for `TASK-131003`'s merged reason. The four gates are document-shape checks
and one conformance check: the `P3` row is filled, at most three placeholders remain, the table still
has its seven rows — the row gate already fails on a **missing** row (`exit (bad || !ok)`,
probed), so this one guards the other six, and no `verify:` block in
this story runs a browser.

**One thing the gates cannot see and a reviewer must.** A `P3` row written from a `close`-and-`open`
sequence rather than from a cut would read identically and be a measurement of a different event.
The PR body must show the `cut` command and its `cut 1` answer, and the reviewer should refuse the
row without them.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text**, unedited, at both relay delays.

- [ ] The PR body shows `node scripts/qa/delay.mjs cut 6173` and the `cut <n>` line it answered with,
      proving a live pair was severed rather than an already-dead one
- [ ] The dropped browser's `frames` transcript spans the cut and the return, and is in the PR body
- [ ] The rival browser's `frames` transcript across the same window is in the PR body
- [ ] The PR body records how long the dropped browser took to show the table again
- [ ] The `P3` row says what the dropped browser showed in the gap, what the rival showed, the relay
      delay, and the short commit
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
