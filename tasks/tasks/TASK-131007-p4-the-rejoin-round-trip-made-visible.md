---
schema: 2
id: TASK-131007
title: P4 — the rejoin round trip made visible
type: task
status: ready
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131005]
verify:
  - awk '/^\| `P4`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 2) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The reload path `EPIC-13` measured as surviving is re-measured with the rejoin round trip **wide
enough to see**, and `STORY-1310`'s `P4` row says what stands on screen while the browser waits for
its first frame.

## The one path the whole story was built around

`EPIC-13` recorded the honest limit of its own measurement: ***"No lobby flash was observable at the
sampling resolution `drive.mjs` allows."*** `ADR-0112` §6 turned that into a path — *"**real
latency**, where the rejoin round-trip is visible — a lobby flash localhost sampling could not
see"*. `TASK-131001` built the instrument for it. This is where the instrument gets used on the
question it was built for.

**What the first paint is a paint of.** `boot.ts` writes the room code to storage on `RoomJoined` and
re-sends `JoinRoom` on the next socket's `Welcome`, so a reloading browser holds a room in memory but
has been told nothing by the server yet. `Lobby.tsx`'s three store branches all read null in that
window — `outcome`, `view` and `roomCode` are set by frames, not by storage — so the first paint is
whatever the tree renders with no room. `ADR-0114` §5 names that state and calls it `unknown`,
introducing `roomAwaited` to *"distinguish *no room* from *not told yet*"* — which is evidence that
today nothing distinguishes them, and therefore that the flash is expected rather than surprising.

**So a finding here is likely, and it must not be pre-written.** Drive it, read what is on the
screen, and write that. Do not write the row from the paragraph above.

## The stack

The **delayed layout**, driven at three delays so the reading is a function of the delay rather than
one sample: `delayed 0ms`, `delayed 300ms`, `delayed 1000ms`, with Vite on `5273` and
`node scripts/qa/delay.mjs 5173 5273 <ms> 6173` on `5173`. Otherwise as `TASK-131003` sets it out,
with fresh Chrome profiles from `mktemp -d` (`ADR-0089` §3).

**Three delays, because one is not a measurement.** A flash that grows with the delay is the rejoin
round trip; one that does not is something else, and only two more readings can tell them apart.

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §§1 and 6,
`web-client/src/store/boot.ts`, and `scripts/qa/drive.mjs`. No source is changed.

## Scope

- Two fresh profiles through the relay, a room, a duel under way. Drive the **host** on a bare `/`,
  which is the exact browser `EPIC-13`'s table recorded as *Survives*.
- At each of the three delays, in order:
  1. `X text` — the table before the reload.
  2. `X open` — the reload. **Keep its output verbatim**; it is the first paint.
  3. `X record` immediately, then `X wait` for something only the table has, then `X frames`.
  4. `X text` and `X eval "location.hash"`.
- **Also drive the rival on `?room=CODE`** at the largest delay, because that browser reaches its
  room by a different route (`ADR-0076` §4's instruction consumed at boot) and could paint something
  different.
- **Write the `P4` row**: what the first paint showed at each delay, whether it grew with the delay,
  what the frames transcript contained, and the short commit. The row is one sentence; the three
  readings go in the PR body.

## Out of scope

- **Any repair.** If a lobby is on screen while a rejoin is in flight, the fix is `STORY-1311`'s
  ground and `ADR-0114` §5 is the merged mechanism nearest to it. Note in the row that the finding is
  handed to `STORY-1311`; do not open `Lobby.tsx`, `boot.ts` or `room-standing.ts`.
- **Deciding whether it is a defect.** `ADR-0112` §1 says the reload *"lands on `/` and the frames
  put the player back"*, which a flash does not contradict on its face. Whether a flash the player
  can see is acceptable is a judgement this ticket does not make and does not need to make.
- **The `waiting`-for-one-render residual.** `ADR-0114` §6 names it and says a drive that observes it
  opens a new `DEC` — that is `TASK-131009`'s to route once every row is in, not this ticket's to
  chase.
- **`/qa-cycle`, `docs/test-plan.md`, `A(N)`/`B(N)`, and any coverage claim** (`ADR-0089` §§2b, 2c).

**If the reading needs a decision no merged source settles**, write the row, register the next free
`DEC` in `docs/adr/README.md`'s `## Open decisions`, and leave this ticket `blocked`.

## Tests

No test can be written, for `TASK-131003`'s merged reason. The four gates are document-shape checks
and one conformance check: the `P4` row is filled, at most two placeholders remain, the table still
has its seven rows — the row gate already fails on a **missing** row (`exit (bad || !ok)`,
probed), so this one guards the other six, and no `verify:` block in
this story runs a browser.

**What the gates cannot see, stated so the reviewer looks for it.** A `P4` row that reads *no lobby
appeared* from a single reading at one delay is the exact claim `EPIC-13` already retired as a
statement about the sampler. Three delays, and the growth or non-growth between them, are what make
the sentence mean anything.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text**, unedited.

- [ ] `open`'s first-paint output after the reload is in the PR body at **each of** 0 ms, 300 ms and
      1000 ms
- [ ] The `frames` transcript after each reload is in the PR body
- [ ] The PR body states, for each delay, what stood on screen between the first paint and the first
      frame, and how long that lasted
- [ ] The rival on `?room=CODE` was driven at the largest delay and its readings are in the PR body
- [ ] The `P4` row says whether a lobby is shown while a rejoin is in flight, whether what is shown
      grows with the delay, and names the delays and the short commit
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
