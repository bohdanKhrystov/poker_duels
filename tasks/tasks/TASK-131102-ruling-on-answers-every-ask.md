---
schema: 2
id: TASK-131102
title: rulingOn answers every ask, and the two mailed screens wait
type: task
status: done
parent: STORY-1311
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, routing]
depends_on: [TASK-131101]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/routing/room-standing.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 13) }'
  - awk '/^import / && !/^import type / { bad = 1 } END { exit bad ? 1 : 0 }' web-client/src/routing/room-standing.ts
  - awk '{ n += gsub(/export function spendsOnArrival/, "&") } END { exit (n != 1) }' web-client/src/routing/room-standing.ts
  - awk '/^export type Screen =/ { on = 1 } on && /^  \| "/ { n++ } on && /;$/ { on = 0 } END { exit (n != 7) }' web-client/src/routing/screen.ts
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The same module answers what happens to an ask — honoured, refused, or held until the server has
spoken — so that one function, and not a branch order, is where the rule lives.

## Files

| File | Action |
| --- | --- |
| `web-client/src/routing/room-standing.ts` | modify |
| `web-client/src/routing/room-standing.test.ts` | modify |
| `web-client/src/routing/screen.ts` | read |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §1 and §5.
Nothing else.

## Scope

- Add to `room-standing.ts`, beside `roomStanding` and in the same file **because they are one rule**
  (`ADR-0114` §1: a reader who has to open two files to know what the client does is back to prose):

  ```ts
  export type Ruling = "honour" | "refuse" | "hold";

  export function rulingOn(asked: Screen, standing: RoomStanding): Ruling {
    if (asked === "first") return "honour";
    if (standing === "running") return "refuse";
    if (standing === "unknown") return spendsOnArrival(asked) ? "hold" : "honour";
    return "honour";
  }
  ```

- Add `spendsOnArrival(asked: Screen): boolean` as an **exported, tested predicate** — true for
  `"verify"` and `"reset"`, false for every other `Screen`. `ADR-0114` §5 is explicit that this is
  not a literal inside a condition: the question it asks — *does mounting this screen send something
  the server cannot be asked twice?* — must be one a future screen has to answer rather than one it
  can walk past. KDoc it in those words.
- `asked === "first"` is tested **before** `running`, and the comment says why: a player already at
  `/` must never have the address rewritten on every frame that arrives.

## Out of scope

- **Any caller.** `Lobby.tsx` is untouched until `TASK-131105`.
- **A `Screen` member.** None is added here or anywhere in this story — the table, the wait and the
  result have no address and get none (`ADR-0076` §§1–2, `ADR-0112` §1). The `verify:` gate that
  counts the union at seven is the check.
- **Deciding what a `hold` renders.** That is `Lobby.tsx`'s and is `TASK-131105`'s.

## Tests

Six more `it` blocks in `room-standing.test.ts`, under `describe("the ruling on an ask")`:

| Test | Proves |
| --- | --- |
| `honours the first screen whatever the room is doing` | `rulingOn("first", s)` is `"honour"` for all five standings — the guard that keeps `replaceState` off a player already at `/` |
| `refuses every chosen screen while the duel is running` | `"duels"`, `"leaderboard"`, `"account"`, `"sign-in"`, `"verify"` and `"reset"` against `"running"` all answer `"refuse"` |
| `holds only the two mailed screens while the room is unknown` | `"verify"` and `"reset"` against `"unknown"` answer `"hold"` |
| `honours the other four chosen screens while the room is unknown` | `"duels"`, `"leaderboard"`, `"account"` and `"sign-in"` against `"unknown"` answer `"honour"` — the pair with the row above is what stops `hold` becoming *hold everything* |
| `honours every chosen screen over a waiting or a finished room` | all six against `"waiting"` and against `"finished"` answer `"honour"` — `ADR-0112` §3, the half this story actually changes |
| `names the two screens that spend a secret on arrival, and no others` | `spendsOnArrival` is true for exactly `"verify"` and `"reset"` across all seven `Screen` members |

Enumerate the members literally in the last test rather than iterating a list built from the
predicate itself: a loop over a list the function produced would pass whatever the function says.

## Acceptance criteria

- [ ] All six tests above exist under those exact names and pass, and `room-standing.test.ts` reports
      at least 13 tests in total
- [ ] `spendsOnArrival` is exported exactly once from `room-standing.ts`
- [ ] `screen.ts`'s `Screen` union still has exactly seven members
- [ ] `room-standing.ts` still has type-only imports
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
