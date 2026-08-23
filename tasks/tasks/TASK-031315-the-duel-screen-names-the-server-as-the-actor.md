---
schema: 2
id: TASK-031315
title: The duel screen names the server as the actor
type: task
status: blocked
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
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +629 passed \(629\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'names the server as the actor, for a check as well as a fold'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'shows the most recent mark, whichever order the frames arrived in'
  - cd web-client && npm run check
---

## Blocked on `DEC-070`

**The product owner's** — how long does the most recent mark stay on screen, and what takes it off?
See `TASK-031314`, which carries the question in full. This ticket renders whatever the store holds,
so the answer reaches it through `TASK-031314` and adds nothing here — but it is `blocked` rather
than `backlog` because a rendering ticket that ships before the lifetime is settled is what makes
the stale sentence hard to take back: the tests written here would then be pinning it.

## Goal

The honesty `ADR-0028` paid a wire break for reaches a person: the most recent action the server
took for an absent seat is on the duel screen, with the server as its subject.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify — one import, one line in the duel branch |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `web-client/src/table/absent-action-text.ts` | read — `absentActionText` |

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
- No new string. Every word comes out of `absentActionText`.

## Out of scope

- **When the line goes away.** `DEC-070`; `TASK-031314`'s reducer is what will clear the field, and
  this component renders `null` as nothing already.
- Attaching the mark to a rendered event. There is no rendered event log; `narration` reaches no
  component today.
- Placement and order on the screen. `EPIC-06`'s, and no test asserts either.

## Tests

`web-client/src/lobby/Lobby.test.tsx`, describe block `"the lobby"`. Two added.

Both seat this client at **seat 1**. A screen that read the actor off a literal `0`, or compared
`view.viewerSeat` instead of `mySeat`, agrees with the fixture at seat 0 and disagrees here.

`the lobby`

| Test | Proves |
| --- | --- |
| `names the server as the actor, for a check as well as a fold` | with `RoomJoined(seat 1)` and `SNAPSHOT`: `ActedForAbsent(seat 0, hand 3, sequence 7, FOLD)` puts `The server folded for your rival.` on screen; then `ActedForAbsent(seat 1, hand 3, sequence 9, CHECK)` puts `The server checked for you.` on screen. **Both verbs and both subjects**, because a screen that handled only `FOLD`, or only the rival, passes half of this. And `Your rival folded` is nowhere on screen in either |
| `shows the most recent mark, whichever order the frames arrived in` | `ActedForAbsent(seat 0, hand 3, sequence 7, FOLD)`, then an `Events` frame carrying `PlayerFolded` at sequence 7, leaves `The server folded for your rival.` on screen; and the same two frames applied in the other order leave the same sentence on screen. Ordering is a courtesy (`ADR-0028` §4) and the screen does not depend on it |

Neither test applies a `Snapshot`, a `YourTurn` or a `DuelFinished` **after** a mark, so whatever
`DEC-070` answers is additive here: no assertion written now has to be taken back.

Two tests. Six hundred and twenty-seven exist after `TASK-031314`, so the suite reports **629** —
before whatever test `DEC-070`'s answer added to the store's file.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 629 passed (629)` | two ran and the six hundred and twenty-seven before them still do |
| the two `--reporter=verbose` greps | each exists by name |
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

Quote both in the PR, including that mutation 1 was green.

## Acceptance criteria

- [ ] `DEC-070` is answered by a merged ADR before this ticket is started
- [ ] `the lobby > names the server as the actor, for a check as well as a fold` passes
- [ ] `the lobby > shows the most recent mark, whichever order the frames arrived in` passes
- [ ] The first test asserts a `FOLD` **and** a `CHECK`, and a mark about seat 0 **and** one about
      seat 1
- [ ] `Lobby.tsx` reads `state.mySeat` and contains no seat literal
- [ ] `Lobby.tsx` renders no string this story added — every word comes from `absentActionText`
- [ ] Every other test in `Lobby.test.tsx` is byte-identical to what `TASK-031311` merged
- [ ] `npm run --silent test` reports `Tests  629 passed (629)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
