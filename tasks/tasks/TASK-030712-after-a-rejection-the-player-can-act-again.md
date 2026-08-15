---
schema: 2
id: TASK-030712
title: A rejection leaves the decision point open
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 2
labels: [client, duel, store]
depends_on: [TASK-030711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejected action leaves the decision point open'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejection stops being shown when the next turn opens'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejection stops being shown when the next snapshot arrives'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejection stops being shown when the duel finishes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'two rejections at one decision point are two attempts'
  - cd web-client && ! NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejected action clears the pending turn'
  - cd web-client && npm run check
---

## Goal

The store stops treating a `Rejected` as the end of the player's turn. After one, the client still
holds the decision point the server opened; the refusal's sentence lives only until the server next
speaks about the game; and every refusal moves a value the action bar can see.

This is the **store half** of [`ADR-0043`](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md),
which answers `DEC-037`. The ADR is merged — implement exactly what it says and nothing else.

## Why the ticket changed shape

It was written blocked, assuming one ticket and possibly a component fix. `ADR-0043` settles it in
the reducer and shows the fix is five files — two more than a schema-2 ticket may touch. So the
work splits and **this ticket no longer touches `ActionBar.test.tsx`**, which its earlier `Files`
table named. Re-enabling the bar's controls is the sibling ticket's (`ActionBar.tsx`,
`ActionBar.test.tsx`, `Lobby.tsx`); `STORY-0307`'s fifth acceptance criterion closes when both land.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — one field, four reducer cases |
| `web-client/src/store/duel-state.test.ts` | modify — **this ticket owns `a rejected action clears the pending turn`**, which states the behaviour `ADR-0043` reverses; it is rewritten, not deleted quietly |

## Scope

Exactly `ADR-0043`'s decision points 1, 2, 3 and 5:

- `DuelState` gains `readonly rejectionCount: number`, and `initialState()` returns `0` for it. It
  carries a comment saying what it is — **client bookkeeping, not a game fact**: the store is the
  only layer that sees frames as events, so it is the only layer that can turn "a rejection
  happened" into a value a component can key off.
- `Rejected` becomes
  `{ ...state, rejection: message.rejection, rejectionCount: state.rejectionCount + 1 }`.
  `pendingTurn` and `view` are **untouched** — a `Rejected` reports on an attempt, not on state.
- `YourTurn`, `Snapshot` and `DuelFinished` each additionally set `rejection: null`. `Events` does
  not: it is narration, and every `Events` frame the server sends is accompanied by a `Snapshot`.
- The reducer **never reads the `Rejection` variant**. All five reduce identically. `guard` answers
  `NOT_YOUR_TURN` both when the sender is not on turn and when the sender *is* on turn but named the
  opponent in `action.seat`, and in that second case the rejection carries the recipient's own seat —
  so a reducer that closed the turn on `NotYourTurn` would switch the bar off on the player's own
  turn.
- `starts with nothing the server has not sent` gains `rejectionCount: 0` in its `toEqual`.

## Out of scope

- **Anything in `web-client/src/table/` or `web-client/src/lobby/`.** The bar's remount key
  (`` `${turn.handNumber}:${turn.actionSequence}:${rejectionCount}` ``) and `Lobby.tsx`'s new prop
  are `ADR-0043`'s decision point 4 and belong to the sibling ticket. Nothing here renders.
- Any change to `poker-server` or to the protocol. `ADR-0043` decided against a server re-prompt;
  the wire is untouched and `PROTOCOL_VERSION` does not move.
- Adding a second field the server did not send. `rejectionCount` is the one the ADR licenses, and
  the ADR says any second one is argued in an ADR rather than added.
- `refusal`. `Failure{DUEL_PAUSED}` says *do not re-send*, so the bar staying locked there is
  correct; that its sentence also never clears is a separate ticket the ADR names.
- Retrying an action for the player. The store holds the turn; it never re-sends.

## Tests

`web-client/src/store/duel-state.test.ts`, in the existing `describe("the duel state")`. Four are
added, one is rewritten, one is extended.

| Test | Proves |
| --- | --- |
| `a rejected action leaves the decision point open` | **the rewrite of `a rejected action clears the pending turn`** — after `YourTurn{handNumber: 1, actionSequence: 1}` then `Rejected`, `pendingTurn` still carries that same `handNumber` and `actionSequence`, and `rejection` is the frame's |
| `a rejection stops being shown when the next turn opens` | after `Rejected` then `YourTurn`, `rejection` is `null` |
| `a rejection stops being shown when the next snapshot arrives` | after `Rejected` then `Snapshot`, `rejection` is `null` and `pendingTurn` is `null` |
| `a rejection stops being shown when the duel finishes` | after `Rejected` then `DuelFinished`, `rejection` is `null` and the outcome is set |
| `two rejections at one decision point are two attempts` | two `Rejected` frames on one `YourTurn` leave `rejectionCount` at `2` and `pendingTurn` still holding that turn — the case a single boolean would get wrong |
| `starts with nothing the server has not sent` | extended: `rejectionCount: 0` joins the `toEqual` |

The rewrite is the point of the ticket, not a casualty of it: `TASK-030404` asserted today's
behaviour honestly, and `ADR-0043` reverses it. **Name it in the PR body, with the ADR, and say
why** — a merged test that quietly disappears is the trail breaking.

## Proof

| Command | Proves |
| --- | --- |
| the five `--reporter=verbose` greps | each new test exists by name and ran |
| the negated grep | `a rejected action clears the pending turn` is gone, not renamed alongside a survivor |
| `npm run check` | typechecks, lints, formats, and the whole suite passes |

**Name the edit that makes each assertion red**, and quote all three in the PR:

1. Restore `pendingTurn: null` to the `Rejected` case → `a rejected action leaves the decision point
   open` and `two rejections at one decision point are two attempts` both fail. Revert.
2. Drop `rejection: null` from the `Snapshot` case → `a rejection stops being shown when the next
   snapshot arrives` fails. Revert.
3. Replace `rejectionCount: state.rejectionCount + 1` with `rejectionCount: 1` → `two rejections at
   one decision point are two attempts` fails on the count. Revert.

## Acceptance criteria

- [ ] This ticket names `ADR-0043`, and implements its decision points 1, 2, 3 and 5 — no more
- [ ] `a rejected action leaves the decision point open` passes
- [ ] `a rejection stops being shown when the next turn opens` passes
- [ ] `a rejection stops being shown when the next snapshot arrives` passes
- [ ] `a rejection stops being shown when the duel finishes` passes
- [ ] `two rejections at one decision point are two attempts` passes
- [ ] `a rejected action clears the pending turn` no longer exists, and the PR body says which ADR
      reversed it and why
- [ ] The `Rejected` case reads no field of `message.rejection` and branches on no variant
- [ ] No file outside `web-client/src/store/` is touched
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
