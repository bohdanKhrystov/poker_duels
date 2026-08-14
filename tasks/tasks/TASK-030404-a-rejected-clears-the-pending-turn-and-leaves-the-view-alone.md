---
schema: 2
id: TASK-030404
title: A Rejected clears the pending turn and leaves the view untouched
type: task
status: backlog
parent: STORY-0304
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030403]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +75 passed \(75\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejected action clears the pending turn'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'surfaces the rejection exactly as the server sent it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the view untouched'
  - cd web-client && npm run check
---

## Goal

A `Rejected` clears the pending turn, records the `Rejection` exactly as sent, and changes no
field of `state.view`.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

`Rejection`'s five variants (`ActionNotAllowed`, `AmountTooLarge`, `AmountTooSmall`,
`HandComplete`, `NotYourTurn`) are already visible in `TASK-030401`'s import; no new file needs
reading for their shape.

## Scope

- Add one case to the `switch` in `applyServerMessage`, after `case "Snapshot":`:

  ```ts
  case "Rejected":
    return { ...state, pendingTurn: null, rejection: message.rejection };
  ```

- `message.rejection` is stored whole, not matched on its `type`. There is no `switch` or
  `if`/`else` over which rejection it is — same discipline `TASK-030310` used for
  `ProtocolError`, for the same reason: a `Rejection` variant this file has never seen still
  survives the trip unchanged.
- `pendingTurn: null` unconditionally, the same rule `TASK-030403` gave `Snapshot`: a refused
  action means there is nothing left to act on until the next `YourTurn`.
- `view` is not part of this object literal, so it is not touched. A `Rejected` answers "no" to an
  attempted action; the table itself has not moved.

## Out of scope

- Interpreting a rejection into player-facing copy — a screen's job, `STORY-0307`.
- `DuelFinished` also clearing a pending turn — `TASK-030406`.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Three `it` blocks,
appended after `TASK-030403`'s five. **Those eleven existing tests are not edited.** The third new
test reuses `samplePlayerView` from `TASK-030403` unchanged.

| Test | Proves |
| --- | --- |
| `a rejected action clears the pending turn` | `YourTurn` then `Rejected` (two real `applyServerMessage` calls) leaves `pendingTurn` `null` |
| `surfaces the rejection exactly as the server sent it` | `Rejected` with `rejection: {type:"AmountTooSmall", attempted:5, minimum:10}` leaves `state.rejection` equal to that object exactly |
| `leaves the view untouched` | `Snapshot` then `Rejected` leaves `state.view` equal to the same `PlayerView` the `Snapshot` carried |

Three tests. Seventy-two exist, so the suite reports **75**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 75 passed (75)` | the three tests ran and the eleven before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red:**

1. Drop `pendingTurn: null` from the case → `a rejected action clears the pending turn` fails, the
   object is non-null. Revert.
2. Change the case to `rejection: { ...message.rejection, handled: true }` → `surfaces the
   rejection exactly as the server sent it` fails, the actual object carries an extra `handled`
   key `toEqual` does not expect. Revert.
3. Add `view: null` to the case → `leaves the view untouched` fails, `expected null to equal
   {...}`. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel state > a rejected action clears the pending turn` passes
- [ ] `the duel state > surfaces the rejection exactly as the server sent it` passes
- [ ] `the duel state > leaves the view untouched` passes
- [ ] `npm run --silent test` reports `Tests  75 passed (75)`
- [ ] The eleven `it` blocks from `TASK-030401` through `TASK-030403` are unedited, and their
      assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
