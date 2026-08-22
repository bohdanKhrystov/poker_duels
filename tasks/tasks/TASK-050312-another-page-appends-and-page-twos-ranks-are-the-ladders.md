---
schema: 2
id: TASK-050312
title: Another page appends, page two's ranks are the ladder's, and the self line does not move
type: task
status: done
parent: STORY-0503
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, leaderboard, ui, paging]
depends_on: [TASK-050311]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'appends the next page under the first, and repeats the rank the boundary repeated'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stops offering another page at the end of the ladder'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the self line where it was when a second page arrives'
  - cd web-client && npm run check
---

## Goal

A reader can walk the ladder a page at a time: rows accumulate, the ranks stay the server's across
the boundary, and the walk stops offering more when there is no more.

## The trap this ticket owns

`ADR-0064` §2: *"A page may begin with the rank the previous page ended on. A block of tied players
spanning a page boundary is ordinary. The same rank number appearing on two pages is **not** a
duplicate row."* On the second day of a season this is the normal case, not an edge one — page one
and page two of a 190-way tie both read `5` all the way down. A client that de-duplicates on rank,
or renumbers from the accumulated row count, is wrong exactly here and nowhere a single page can
show.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | modify — the *Show more* control and the request behind it |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify — **adds tests only**; no assertion written by `TASK-050307` through `TASK-050311` changes |

## Scope

- The control renders when `nextPageAfter(state)` is not `null` **and** `state.phase !== "loading"`,
  and its label is `MORE` from `ladder-text.ts`.
- `nextPageAfter(state)` is called **once** per render and the value is used both to decide whether
  to render the control and as the argument to `ask`. Rendering from one rule and asking from
  another is how the two end up disagreeing.
- The loading guard is the reason a second click cannot re-issue an outstanding request:
  `nextPageAfter` is pure over `nextCursor`, which a request in flight does not move.
- Rows are appended by the reducer, which already owns that rule (`TASK-050305`). Nothing new here
  sorts, de-duplicates, re-keys or renumbers.
- Fixtures in this ticket's tests **do** carry cursors; every fixture written by an earlier ticket
  keeps `nextCursor: null` and therefore keeps rendering no control.

## The guard this ticket must carry

`TASK-050305` demonstrated, with real output, that `ladderReducer`'s `askedWith` is a **single slot
with no per-request identity**: two reads in flight, and a late response for the superseded one is
misclassified as a first page — the held rows vanish and `phase` reads `"ready"` while a request is
still outstanding. The same exposure sits in the merged `history-state.ts`.

`TASK-050307` shipped the screen with **one** call site, so today the only way to reach it is to
break the documented *stable `read` reference* contract. **This ticket adds the second entry point.**
A *show more* control that can be pressed while a page is loading makes the gap live and reachable by
an ordinary player double-pressing a button.

So the control must not start a read while one is outstanding — a phase guard, as
`HistoryScreen`'s *show more* already carries (`state.phase !== "loading"`), or an equivalent. Assert
it: a second press during a pending read must issue **no** second request, and only a request count
can show that.

## Out of scope

- **Restarting a walk the server refused** — `TASK-050304` maps a `400` to `unavailable` and
  `ADR-0066` §7's *drop the cursor and start again* is not built by this story.
- **Loading the next page automatically**, on scroll, on a timer, or on anything but the control.
- **A page-size control, or a `limit` parameter** — the server's default is the page size.
- **A jump-to-me control, or any parameter naming a page that contains the reader** —
  `ADR-0065` §5.

## Tests

`web-client/src/ladder/LadderScreen.test.tsx`, same `describe`, three new tests. A `read` that
answers page one on its first call and page two on its second.

Page one: ranks `[1, 1, 1, 5]`, `season: "2026-08"`, `self: { rank: 5, coins: 1 }`,
`nextCursor: "c1"`. Page two: ranks `[5, 5, 9, 9]`, `season: "2026-08"`,
`self: { rank: 9, coins: -2 }`, `nextCursor: null`.

| Test | Proves |
| --- | --- |
| `appends the next page under the first, and repeats the rank the boundary repeated` | After clicking `MORE`: the list holds **eight** `<li>`, and their ranks read `[1, 1, 1, 5, 5, 5, 9, 9]` in that order. `read` was called a second time with `"c1"` exactly. Page one's four rows are still on screen above page two's, and the `5` that ends page one and the `5` that begins page two are both printed |
| `stops offering another page at the end of the ladder` | The control is on screen before the click and gone after it, because page two named no next cursor. Two states, so the assertion cannot pass on a control that never renders |
| `leaves the self line where it was when a second page arrives` | Page two carries a **different** self standing. After the click the screen still reads `"You are rank 5 this season, on 1 duel coin."` and never `rank 9`. `ADR-0065` §1: the line does not change as the player walks pages |

## Acceptance criteria

- [ ] `appends the next page under the first, and repeats the rank the boundary repeated` passes with
      eight rows and ranks `[1, 1, 1, 5, 5, 5, 9, 9]` — de-duplicating on rank reddens it at six
      rows, and numbering rows from the accumulated count reddens it at `[1..8]`
- [ ] `stops offering another page at the end of the ladder` passes in both states — rendering the
      control unconditionally reddens it
- [ ] `leaves the self line where it was when a second page arrives` passes — taking `self` from
      every page reddens it
- [ ] Every test `TASK-050307` through `TASK-050311` wrote still passes, with no assertion in any of
      them edited
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
