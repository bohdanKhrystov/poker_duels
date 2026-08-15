---
schema: 2
id: TASK-030713
title: The bar comes back after a rejection, at the same decision point
type: task
status: backlog
parent: STORY-0307
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, duel, ui]
depends_on: [TASK-030712]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'comes back to life after a rejection at the same decision point'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'stays locked when nothing was rejected'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'comes back a second time when the second attempt is refused too'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'returns the amount control to the minimum the server sent after a rejection'
  - grep -qF 'rejectionCount={state.rejectionCount}' web-client/src/lobby/Lobby.tsx
  - cd web-client && npm run check
---

## Goal

After the server refuses an action, the player can act again at the same decision point: the bar's
in-flight lock lifts because React unmounts the old `Live`, and the `Act` the next click sends
carries the very `handNumber` and `actionSequence` the first one did.

This is the **bar half** of [`ADR-0043`](../../docs/adr/ADR-0043-a-rejection-closes-no-decision-point.md),
which answers `DEC-037`. `TASK-030712` is the store half and is already merged. Together they close
`STORY-0307`'s fifth acceptance criterion; neither closes it alone.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | modify — one optional prop, one key string |
| `web-client/src/table/ActionBar.test.tsx` | modify — four tests added; **no existing assertion moves** |
| `web-client/src/lobby/Lobby.tsx` | modify — one prop passed to `ActionBar` |

## Scope

Exactly `ADR-0043`'s decision point 4, and nothing else:

- `ActionBar` gains `rejectionCount?: number` in its props type, documented in one line as **client
  bookkeeping the store keeps, not a game fact** — the count of refusals, whose only job is to
  change.
- The remount key becomes `` `${turn.handNumber}:${turn.actionSequence}:${props.rejectionCount ?? 0}` ``.
  `TASK-030707`'s mechanism is preserved exactly: the lock lifts because React unmounts `Live`,
  never because anything clears `sent`. `ActionBar.tsx` keeps its **no `useEffect`, no `useRef`**
  rule — if either appears in the diff, the ticket is wrong.
- `Lobby.tsx` passes `rejectionCount={state.rejectionCount}` beside the three props it already
  passes. The `verify` block greps for that exact text, because no file in this ticket's budget can
  render `Lobby`.

### Why the prop is optional

`bar-no-derivation.test.tsx` renders `<ActionBar>` twice without it, so a **required** prop would
drag that file into the diff and put this ticket at four files — one over `lint_tickets.py`'s cap.
`?? 0` makes an unpassed count behave exactly as today. The compiler therefore does not force the
one caller that matters, so the `verify` grep on `Lobby.tsx` does instead; keep both.

## Out of scope

- **Anything in `web-client/src/store/`.** The reducer is `TASK-030712`, merged. This ticket adds no
  field and reads no frame.
- Clearing `rejection` from the component. The store clears it on the next `YourTurn`, `Snapshot` or
  `DuelFinished`; a component that hid the sentence itself would be deriving.
- Branching on the `Rejection` variant to decide whether the bar comes back. `ADR-0043` decision
  point 5: all five variants reduce identically, and a bar that treated `NotYourTurn` as final would
  switch itself off during the player's own turn.
- Preserving the amount the player had dialled. `ADR-0043` accepts losing it as the price of the
  remount, and test four pins that loss so nobody "fixes" it by hand later.
- `refusal`'s lifetime — `TASK-030714`.

## Tests

`web-client/src/table/ActionBar.test.tsx`, in the existing `describe("the action bar")`. Four are
added. The existing `bar()` helper does not pass `rejectionCount`; these four call `render` directly,
as the file's other rerender tests already do.

| Test | Proves |
| --- | --- |
| `comes back to life after a rejection at the same decision point` | render `aTurn({handNumber: 61, actionSequence: 103})` with `rejectionCount={0}`, click `Fold`, then rerender the **same turn object** with a `rejection` and `rejectionCount={1}`: a second `Fold` click sends a second `Act`, and `toHaveBeenNthCalledWith(2, …)` shows `handNumber: 61, actionSequence: 103` — the identity survived the refusal |
| `stays locked when nothing was rejected` | the same first half, then a rerender with `rejectionCount` still `0`: the second click sends nothing and `send` stays at one call. Without this, a bar that remounted on every rerender would pass the test above |
| `comes back a second time when the second attempt is refused too` | `0 → 1 → 2` with a click after each: three `Act` frames, all bearing the one identity. The case a boolean gets wrong, and `ADR-0043` names it — two identical refusals set a deep-equal `rejection`, so the count is the only thing that moves |
| `returns the amount control to the minimum the server sent after a rejection` | with `RAISE` allowed, `fireEvent.change` the slider to `3250`, rerender with `rejectionCount={1}`: the slider reads `1200` again — `minRaiseTo` from the fixture, never a figure the bar kept |

## Proof

| Command | Proves |
| --- | --- |
| the four `--reporter=verbose` greps | each new test exists by name and ran |
| the `Lobby.tsx` grep | the count is actually wired from the store, which no test in this budget can see |
| `npm run check` | typechecks, lints, formats, and every existing test still passes. No command here greps a total, because the tickets around this one land in any order and each moves the total |

**Name the edit that makes each assertion red**, and quote all three in the PR:

1. Drop `:${props.rejectionCount ?? 0}` from the key → `comes back to life after a rejection at the
   same decision point` fails on `send` being called once. Revert.
2. Replace the key's count with the literal `1` → `comes back a second time when the second attempt
   is refused too` fails, and `stays locked when nothing was rejected` fails too. Revert.
3. Remove `rejectionCount={state.rejectionCount}` from `Lobby.tsx` → the grep exits 1 while every
   test still passes, which is the whole reason that grep is in `verify`. Revert.

## Acceptance criteria

- [ ] This ticket names `ADR-0043` and implements its decision point 4 — no more
- [ ] `comes back to life after a rejection at the same decision point` passes
- [ ] `stays locked when nothing was rejected` passes
- [ ] `comes back a second time when the second attempt is refused too` passes
- [ ] `returns the amount control to the minimum the server sent after a rejection` passes
- [ ] `ActionBar.tsx` contains no `useEffect` and no `useRef`
- [ ] No existing test in `ActionBar.test.tsx` is renamed, deleted or weakened — the four are
      additions, and `comes back to life on the next turn, at the new minimum` still passes unchanged
- [ ] `Lobby.tsx` passes `rejectionCount={state.rejectionCount}`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
