---
schema: 2
id: TASK-031315
title: The duel screen names the server as the actor
type: task
status: ready
parent: STORY-0313
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, duel, ui, presence, copy]
depends_on: [TASK-031314]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the server as the actor, for a check as well as a fold'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the most recent mark, whichever order the frames arrived in'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rival who is back leaves no sentence about the server behind'
  - cd web-client && npm run check
---

## Goal

The honesty `ADR-0028` paid a wire break for reaches a person: the most recent action the server
took for an absent seat is on the duel screen, with the server as its subject — and it leaves the
screen with the absence that produced it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one line in the duel branch |
| `web-client/src/lobby/Lobby.test.tsx` | modify — three tests added, none changed |
| `web-client/src/table/absent-action-text.ts` | read — `absentActionText` |
| `docs/adr/ADR-0075-the-mark-lives-as-long-as-the-absence-that-produced-it.md` | read — §2, and the failure it makes impossible |

## Scope

- The duel branch renders one more reserved-height line, beside the presence notice:

  ```tsx
        {state.serverAction !== null && (
          <p className="min-h-[calc(var(--pd-fs-small)*var(--pd-lh-body))] text-center text-small text-text-muted">
            {absentActionText(state.serverAction, state.mySeat)}
          </p>
        )}
  ```

- `mySeat` comes off the store and nowhere else — never a literal, never `view.viewerSeat`. The two
  can disagree: `duel-state.ts` keeps `mySeat` from `RoomJoined` even when a snapshot's `viewerSeat`
  says otherwise, and there is a merged test saying so.
- **One line, not a log.** `ADR-0046` §4: showing the most recent mark satisfies this, and no
  scrollback, replay or action list is designed or built here.
- **No lifetime logic in this component.** `ADR-0075` §2 puts the whole rule in the reducer, which
  `TASK-031314` has already written; this branch renders `null` as nothing and that is the entire
  mechanism. No `useState`, no `useEffect`, no local copy of the last mark.
- No new string. Every word comes out of `absentActionText`.

## Out of scope

- Attaching the mark to a rendered event. There is no rendered event log; `narration` reaches no
  component.
- Placement and order on the screen. `EPIC-06`'s, and no test asserts either.
- Anything that would make the line survive the reducer clearing it. `ADR-0075` alternative 5
  refuses a timer, a fade and a dismiss control by name.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Three added.

All three seat this client at **seat 1**. A screen that read the actor off a literal `0`, or
compared `view.viewerSeat` instead of `mySeat`, agrees with the fixture at seat 0 and disagrees
here.

`the lobby`

| Test | Proves |
| --- | --- |
| `names the server as the actor, for a check as well as a fold` | with `RoomJoined(seat 1)` and `SNAPSHOT`: `ActedForAbsent(seat 0, hand 3, sequence 7, FOLD)` puts `The server folded for your rival.` on screen; then `ActedForAbsent(seat 1, hand 3, sequence 9, CHECK)` puts `The server checked for you.` on screen. **Both verbs and both subjects**, because a screen that handled only `FOLD`, or only the rival, passes half of this. And `Your rival folded` is nowhere on screen in either |
| `shows the most recent mark, whichever order the frames arrived in` | `ActedForAbsent(seat 0, hand 3, sequence 7, FOLD)`, then an `Events` frame carrying `PlayerFolded` at sequence 7, leaves `The server folded for your rival.` on screen; and the same two frames applied in the other order leave the same sentence on screen. Ordering is a courtesy (`ADR-0028` §4) and the screen does not depend on it |
| `a rival who is back leaves no sentence about the server behind` | `RoomJoined(seat 1)`, `SNAPSHOT`, `OpponentPresence(ABSENT)`, `ActedForAbsent(seat 0, hand 3, sequence 7, FOLD)`, `OpponentPresence(PRESENT)`: the screen shows `Your rival is back.` and `The server folded for your rival.` is **nowhere on it**. This is `DEC-070` at the level a player sees it — the store test asserts two field values, and only this one asserts that the two *sentences* are never co-present. No `Snapshot` between the mark and the `PRESENT`, or `rivalReturned` would be back to `false` and the return line would not render |

Three tests. Six hundred and thirty-one exist after `TASK-031314`, so the suite reports **634**.

## Proof

| Command | Proves |
| --- | --- |
| a green `Tests N passed (N)` line | three ran and every test before them still does |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | `absentActionText(state.serverAction, state.mySeat)` typechecks with `mySeat` as `number \| null` |

**Name the edit that makes each assertion red:**

1. Pass `state.view?.viewerSeat ?? null` instead of `state.mySeat` → **nothing fails**, because in
   this fixture the two agree. That is the finding to record, not a reason to relax the rule: the
   guard against them disagreeing is `duel-state.test.ts`'s `keeps mySeat from RoomJoined when a
   snapshot's viewerSeat disagrees`, and this ticket does not duplicate it. Say so in the PR and
   keep `state.mySeat`.
2. Pass a literal `0` as the seat → `names the server as the actor, for a check as well as a fold`
   fails on its first half, `The server folded for you.` against `The server folded for your
   rival.`. Revert.
3. The edit that reddens the third test is **not in this ticket's files**: delete the `serverAction`
   key from `duel-state.ts`'s `OpponentPresence` case — a file this ticket reads and does not
   modify — and `a rival who is back leaves no sentence about the server behind` fails with
   `The server folded for your rival.` still on screen beside `Your rival is back.`, which is
   `DEC-070`'s named failure reproduced on demand. Revert, and leave `duel-state.ts` untouched in
   the diff.

Quote all three in the PR, including that mutation 1 was green.

## Acceptance criteria

- [ ] `the lobby > names the server as the actor, for a check as well as a fold` passes
- [ ] `the lobby > shows the most recent mark, whichever order the frames arrived in` passes
- [ ] `the lobby > a rival who is back leaves no sentence about the server behind` passes
- [ ] The first test asserts a `FOLD` **and** a `CHECK`, and a mark about seat 0 **and** one about
      seat 1
- [ ] The third test asserts `Your rival is back.` is on screen **and** that the mark's sentence is
      not, in the same rendered state
- [ ] `Lobby.tsx` reads `state.mySeat` and contains no seat literal
- [ ] `Lobby.tsx` renders no string this story added — every word comes from `absentActionText`
- [ ] `Lobby.tsx` holds no state of its own for the mark: no `useState`, no `useEffect`, no `useRef`
- [ ] `duel-state.ts` is not in this ticket's diff
- [ ] Every other test in `Lobby.test.tsx` is byte-identical to what `TASK-031311` merged
- [ ] `npm run --silent test` reports `Tests  N passed (N)` with no failures
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
