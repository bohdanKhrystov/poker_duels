---
schema: 2
id: TASK-131003
title: P1 — a refresh on the result screen
type: task
status: ready
parent: STORY-1310
module: web-client
estimate: XS
tier: sonnet
review: standard
files_touched: 1
labels: [qa, refresh, manual-verify]
depends_on: [TASK-131010]
verify:
  - awk '/^\| `P1`/ { if (index($0, "NOT-YET-DRIVEN")) bad = 1; else ok = 1 } END { exit (bad || !ok) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '{ n += gsub(/NOT-YET-DRIVEN/, "&") } END { exit (n > 6) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^\| `P[1-6]/ { n++ } END { exit (n != 7) }' tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1310*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0112` §6's first undriven path — a reload on the **result screen**, a held `FINISHED` room with
`outcome` standing — is driven against a running stack and its `P1` row in `STORY-1310` says what
happened.

## What the gates can and cannot check

They check that the `P1` row exists, that it is no longer the placeholder, that the table still has
its seven rows, and that no ticket in this story put a browser in a `verify:` block. **They cannot
check that the sentence in the row is true** — `ADR-0089` §2b makes a browser drive a PR statement
and a human's verdict, never a merge condition. The readings pasted into the PR body are the
evidence; the reviewer's judgement is the acceptance.

## The stack, and the two layouts

Bare, which is the product as it ships:

```
scripts/qa/stack.sh db-up
CP=$(scripts/qa/stack.sh cp)
java -cp "$CP" duels.poker.server.ApplicationKt     # a background task
cd web-client && npm run dev                        # a background task
scripts/qa/stack.sh wait-server && scripts/qa/stack.sh wait-web
A=$(mktemp -d); B=$(mktemp -d)
scripts/qa/stack.sh chrome-up 9232 "$A"
scripts/qa/stack.sh chrome-up 9233 "$B"
```

Delayed, which is the same with Vite moved and `TASK-131001`'s relay in front of it:
`npm run dev -- --port 5273`, plus `node scripts/qa/delay.mjs 5173 5273 300 6173` as a third
background task. `wait-web` then answers through the relay, and every port `drive.mjs` knows stays
what it was.

**Fresh Chrome profiles from `mktemp -d`, never reused** (`ADR-0089` §3): the server re-seats a
returning device by `pd.deviceId`, so a reused profile rejoins its old room with storage cleared.
Tear down with `chrome-down 9232 9233`, `db-down`, and stopping the background tasks.

## Files

| File | Action |
| --- | --- |
| `tasks/stories/STORY-1310-the-refresh-paths-nobody-drove.md` | modify |

Read: `docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md` §§1 and 6, and
`scripts/qa/drive.mjs` for the verbs. Nothing else — no client source is opened by this ticket, and
none is changed by it.

## Scope

- **Reach a finished duel.** `A` creates a room, `B` opens the invite link, and the duel is played to
  a winner by pressing what a player presses. The fastest route is `All in` and then `Call`; a match
  ends when one seat is out of chips, and `CORE-12`'s sequence is the model.
- **Drive the reload on `A`**, which sits on a bare `/`. In order, and the order is the measurement:
  1. `A text` — the result screen before the reload, so the after has a before to be compared with.
  2. `A eval "location.hash"` and `A eval "localStorage['pd.roomCode']"`.
  3. `A open` — the reload. **Keep what it prints**: `open` returns the moment `#root` first has
     content, so its output is the first paint and the only pre-frame observation a navigation
     allows.
  4. `A record` immediately, then `A wait` for the winner line, then `A frames` — the whole
     transition from first paint to settled screen.
  5. `A text`, `A eval "location.hash"` again.
- **Do it on both layouts.** Bare first. Then, whatever the bare reading was, repeat it at
  `delayed 300ms` — a negative finding on a bare stack is a statement about a 250 ms sampler, not
  about the product, which is the whole reason `TASK-131001` merged first.
- **Write the `P1` row**: one sentence of result, the `git rev-parse --short HEAD` of the commit
  driven, and the layout the reading came from. If the two layouts disagree, the row says both and
  the delayed one is the finding.

## What `ADR-0112` predicts, so the drive can contradict it

§1: *"A reload while seated lands on `/` and the frames put the player back — measured, on every
path tried."* The result screen is the path that measurement did **not** cover. `boot.ts` re-sends
`JoinRoom` on the next `Welcome`, and a `FINISHED` room's resume restates the outcome, so the
expectation is that the result screen returns and `location.hash` reads empty.

**Three ways it could be false, and each is a different row.** The result is lost and the lobby
stands. The result returns but a lobby is painted on the way. The result returns and something on it
does not — the winner, the hand count, the rematch control, the account offer. Record which.

## Out of scope

- **Any repair.** `STORY-1311` owns what the client does about a held room and a chosen screen. A
  ticket that finds something writes the row and stops.
- **`P2`–`P6b`.** One row per ticket. If another path happens to be observed while driving this one,
  it may be written down — the placeholder-count gate is a lower bound on rows filled, deliberately,
  so a real observation is never forbidden by a number.
- **`/qa-cycle`, `docs/test-plan.md`, and any `A(N)`/`B(N)`.** This is a targeted reproduction an ADR
  asked for by name, not a round (`ADR-0089` §2b: a cycle is started by the human's own message and
  nothing else).
- **Any coverage claim.** `ADR-0089` §2c: a statement about one run, on one machine, at one commit.
- **Re-measuring `EPIC-13`'s five.** A host in a live duel on a bare `/`, a rival on `?room=CODE`, a
  host on the waiting screen and `#/leaderboard` on a room-free browser all stand as measured.

**If the reading is a defect no merged source settles what to do about**, write the `P1` row anyway
— the observation *is* the result — then register the next free `DEC` in `docs/adr/README.md`'s
`## Open decisions` (`DEC-125` at the time of writing; check the register for the highest before
claiming one), say what was measured, and leave this ticket `blocked`. That is a legitimate ending,
not a failure, and it is the conduct `TASK-121501` was given. Do not decide it here.

## Tests

**No test can be written, and the reason is merged.** `ADR-0089` §2b: no `verify:` block waits on a
QA case. jsdom computes no navigation, boots no socket and has no second browser, so the client
runner cannot see this either. The four gates above are document-shape checks and one conformance
check, and the ticket says so rather than dressing them up.

| Gate | Proves | Today |
| --- | --- | --- |
| the `P1` row is present and is not the placeholder | a result was written down | **red** |
| at most six placeholders remain in the story | at least one path is now owed less; more than one is allowed | **red** |
| the table still has seven `P` rows | no **other** row was lost — a reformat, or a rebase dropping one | green — a regression guard |
| no `verify:` block under `tasks/tasks/TASK-1310*.md` names `drive.mjs` or `stack.sh` | `ADR-0089` §2b holds across the whole story, checked at the point a violation would enter | green — a regression guard |

**What the third gate is and is not for, measured rather than assumed.** The first gate is
`exit (bad || !ok)`, so a **deleted** `P1` row leaves `ok` unset and it already exits 1 — probed on a
scratch copy of the story before this ticket was written. So the third gate is not a backstop for
this row; it is the guard on the other six, which nothing else in this ticket looks at.

## Acceptance criteria

**Who runs the measurement:** the implementer, before opening the PR, on a running stack.
**Paste every reading into the PR body as text** — the `text` outputs, the `open` first paint, the
`frames` transcript and both `location.hash` reads, for both layouts, unedited.

- [ ] A duel was played to a winner and both browsers reached the result screen, and the PR body
      shows it
- [ ] `A open` on the result screen was driven at `bare` and at `delayed 300ms`, and both first
      paints are in the PR body verbatim
- [ ] The `frames` transcript after each reload is in the PR body, and the row says whether a lobby
      appeared in it
- [ ] `location.hash` after each reload is in the PR body
- [ ] The `P1` row in `STORY-1310` names the result in one sentence, the short commit driven, and
      the layout
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
