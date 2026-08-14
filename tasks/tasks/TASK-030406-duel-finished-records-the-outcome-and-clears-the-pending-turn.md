---
schema: 2
id: TASK-030406
title: DuelFinished records the outcome verbatim and clears the pending turn
type: task
status: done
parent: STORY-0304
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store]
depends_on: [TASK-030405]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +81 passed \(81\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'records the outcome exactly as the server sent it'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'clears any pending turn once the duel finishes'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the view and narration untouched'
  - cd web-client && npm run check
---

## Goal

A `DuelFinished` records its `DuelOutcome` exactly as sent, clears any pending turn, and changes
neither `view` nor `narration` — the last message this story's reducer folds in.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |

`DuelOutcome`'s three fields (`winner`, `handsPlayed`, `finalStacks`) are already visible in
`TASK-030401`'s import; no new file needs reading for its shape.

## Scope

- Add one case to the `switch` in `applyServerMessage`, after `case "Events":`:

  ```ts
  case "DuelFinished":
    return { ...state, outcome: message.outcome, pendingTurn: null };
  ```

- `message.outcome` is stored whole. `winner`, `handsPlayed` and `finalStacks` are not read
  individually, not compared, not used to decide anything — `DuelOutcome.winner` already says who
  won; this store does not re-derive it.
- `pendingTurn: null` unconditionally, for the same reason `Snapshot` and `Rejected` clear it: a
  finished duel has no action left to offer a button for.
- `view` and `narration` are not part of this object literal, so both are left exactly as they
  were — the last `Snapshot` and the last `Events` remain in state for a result screen to read
  later.

## Out of scope

- Rematch, or anything that follows a finished duel — `STORY-0309`.
- Rendering the result — `STORY-0308`.
- The forward-compatibility question the whole *"story-specific things"* list raises for
  `ServerMessage` itself growing a ninth variant: this reducer's `default` branch already no-ops
  on anything it does not name, so nothing here needs to change for that to keep working.

## Tests

`web-client/src/store/duel-state.test.ts`, describe block `"the duel state"`. Three `it` blocks,
appended after `TASK-030405`'s three — the last tests this story adds. **The seventeen existing
tests are not edited.** The third new test reuses `samplePlayerView` from `TASK-030403` unchanged.

| Test | Proves |
| --- | --- |
| `records the outcome exactly as the server sent it` | `DuelFinished` with `outcome: {winner:1, handsPlayed:12, finalStacks:[0,2000]}` leaves `state.outcome` equal to that object exactly |
| `clears any pending turn once the duel finishes` | `YourTurn` then `DuelFinished` (two real `applyServerMessage` calls) leaves `pendingTurn` `null` |
| `leaves the view and narration untouched` | `Snapshot` then `Events` then `DuelFinished` leaves `state.view` and `state.narration` exactly as `Events` left them |

Three tests. Seventy-eight exist, so the suite reports **81**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 81 passed (81)` | the three tests ran and the seventeen before them still do |
| the three `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red:**

1. Change the case to `outcome: { winner: message.outcome.winner }` (drop the other two fields) →
   `records the outcome exactly as the server sent it` fails, `handsPlayed` and `finalStacks` are
   `undefined`. Revert.
2. Drop `pendingTurn: null` from the case → `clears any pending turn once the duel finishes`
   fails, the object is non-null. Revert.
3. Add `narration: []` to the case → `leaves the view and narration untouched` fails, the actual
   narration is empty where the events from the prior step were expected. Revert.

Quote all three in the PR.

## Acceptance criteria

- [ ] `the duel state > records the outcome exactly as the server sent it` passes
- [ ] `the duel state > clears any pending turn once the duel finishes` passes
- [ ] `the duel state > leaves the view and narration untouched` passes
- [ ] `npm run --silent test` reports `Tests  81 passed (81)`
- [ ] The seventeen `it` blocks from `TASK-030401` through `TASK-030405` are unedited, and their
      assertions are byte-identical
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
