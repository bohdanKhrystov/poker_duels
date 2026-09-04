---
schema: 2
id: TASK-131101
title: roomStanding reads the room off the frames the server sent
type: task
status: done
parent: STORY-1311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, routing]
depends_on: []
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/routing/room-standing.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 7) }'
  - sh -c 'test -f web-client/src/routing/room-standing.ts && ! grep -qF "window." web-client/src/routing/room-standing.ts'
  - sh -c 'test -f web-client/src/routing/room-standing.ts && ! grep -qF "from \"react\"" web-client/src/routing/room-standing.ts'
  - awk '/^import / && !/^import type / { bad = 1 } END { exit bad ? 1 : 0 }' web-client/src/routing/room-standing.ts
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A pure function answers what the room this tab holds is doing, using only facts the server sent,
so that nothing after this ticket has to re-derive the ladder from `outcome`, `view` and `roomCode`
by hand.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/room-standing.ts` | create |
| `web-client/src/routing/room-standing.test.ts` | create |
| `web-client/src/store/duel-state.ts` | read |
| `web-client/src/table/view-fixture.ts` | read |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §1 and §7's
first bullet. Nothing else — `Lobby.tsx` is not opened by this ticket and is not changed by it.

## Scope

- Create `web-client/src/routing/room-standing.ts` in the `screen.ts` tradition: **no `window`, no
  React, type-only imports.** `ADR-0114` §1 gives the body verbatim and it is not to be improved on:

  ```ts
  export type RoomStanding = "unknown" | "none" | "waiting" | "running" | "finished";

  export function roomStanding(state: DuelState, roomAwaited: boolean): RoomStanding {
    if (state.outcome !== null) return "finished";
    if (state.view !== null) return "running";
    if (state.roomCode !== null) return "waiting";
    return roomAwaited && state.refusal === null ? "unknown" : "none";
  }
  ```

- **The order is the decision, not a style.** The reducer clears nothing a frame established, so
  `view` and `roomCode` both outlive the duel: testing `finished` after `running` makes `finished`
  unreachable. Say so in a comment, citing `ADR-0114` §1, and say *why* rather than *what*.
- KDoc the two exports: `RoomStanding` is the room read off frames, never off the wire (`RoomState`
  is a server-side enum and no frame carries it), and `running` is `ADR-0105` §2's *running means
  `PLAYING`* — the grace window and a mid-paint runout are both inside it.
- Build every test state from `initialState()` with an object spread. No store, no provider, no
  render: this file has no DOM in it.

## Out of scope

- **`rulingOn`, `Ruling` and `spendsOnArrival`** — `TASK-131102`, in this same file.
- **Any use of this module.** `Lobby.tsx` is untouched until `TASK-131105`.
- **An `abandoned` standing.** `ABANDONED` is a server-side `RoomState` and appears in no frame and
  in no client type; a client holding an abandoned room reads `waiting` or `finished` like any
  other. `ADR-0114` §1 names five standings and this ticket adds no sixth.
- **Anything on the wire.** `PROTOCOL_VERSION` does not move (`ADR-0112` §7).

## Tests

`room-standing.test.ts` — `describe("the room's standing")`, seven `it` blocks with these exact
names:

| Test | Proves |
| --- | --- |
| `answers unknown before any frame while this tab is awaiting a room` | `initialState()` with `roomAwaited` **true** → `"unknown"` |
| `answers none when this tab is awaiting no room` | the same state with `roomAwaited` **false** → `"none"`. The pair is the point: one input cannot tell a rule from a constant |
| `answers none once the server refused the room this tab awaited` | `{ ...initialState(), refusal: "UNKNOWN_ROOM" }` with `roomAwaited` true → `"none"`, so a reaped room does not hold a tab in the unknown window forever |
| `answers waiting on a RoomJoined alone` | `{ ...initialState(), roomCode: "ABCDEFGH" }` → `"waiting"`, `roomAwaited` either way |
| `answers running once a Snapshot stands` | `{ ...initialState(), roomCode: "ABCDEFGH", view: aView() }` → `"running"` |
| `answers finished while the view the duel ended on is still standing` | `{ ...initialState(), roomCode: "ABCDEFGH", view: aView(), outcome: { winner: 0, handsPlayed: 3, finalStacks: [1000, 0] } }` → `"finished"`. **Not a tautology**: `view` is non-null and the answer is still `finished`, which is the whole reason `outcome` is tested first |
| `answers running while a reveal is still painting` | `view` non-null, `outcome` **null**, and `reveal` holding a queued `DuelFinished` → `"running"`. **Not a tautology**: the duel's end is already in hand and the standing must not move until the frame is applied (`ADR-0102`'s paint) |

The last test's `reveal` is `{ steps: [{ board: [], street: "PREFLOP" }], queued: [{ message: { type: "DuelFinished", outcome: { winner: 0, handsPlayed: 1, finalStacks: [1000, 0] } }, arrivedAt: 0 }] }` — read `duel-state.ts` for the exact `Reveal`, `RevealStep` and `QueuedMessage` fields rather than guessing them.

## Acceptance criteria

- [ ] All seven tests above exist under those exact names and pass
- [ ] `room-standing.ts` contains no `window`, no `react` import, and every `import` line in it is
      an `import type` line — the three gates in `verify:` check exactly this
- [ ] `roomStanding` tests `outcome`, then `view`, then `roomCode`, then the `roomAwaited`/`refusal`
      pair, in that order
- [ ] `RoomStanding` has exactly five members and no `"abandoned"`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
