---
schema: 2
id: TASK-041401
title: The duel driver writes into the storage it is handed, and one module owns the double
type: task
status: done
parent: STORY-0414
module: web-client
estimate: S
tier: haiku
review: light
files_touched: 2
labels: [client, e2e, test, storage]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-duel.test.tsx 2>&1 | grep -qE 'Tests +3 passed \(3\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-duel.test.tsx --reporter=verbose 2>&1 | grep -qF 'writes the seat own device id into the storage it was handed'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-duel.test.tsx --reporter=verbose 2>&1 | grep -qF 'two seats driven into two storages hold two different device ids'
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/e2e/drive-duel.test.tsx --reporter=verbose 2>&1 | grep -qF 'a run given no storage still plays the whole script'
  - cd web-client && npm run check
---

## Goal

`driveScriptedDuel` can be handed the `Storage` it writes into, and `inMemoryStorage` is exported,
so a later ticket can play a duel and then keep the device id that duel minted.

## Why this exists

`STORY-0414` needs two browsers holding two *different* device ids that neither invented. The
committed script already carries them, from the server's own encoder — **measured**, not assumed:

```
seat 0 → {"type":"Welcome","playerId":"player-seat-0","deviceId":"device-seat-0","protocolVersion":5}
seat 1 → {"type":"Welcome","playerId":"player-seat-1","deviceId":"device-seat-1","protocolVersion":5}
```

`connection.ts:68` writes that id into the connection's storage. But `drive-duel.tsx:161` builds its
storage privately (`const storage = inMemoryStorage()`) and returns no handle on it, so the id the
duel minted is unreachable the moment the run ends. This ticket opens that seam and nothing else.

## Files

| File | Action |
| --- | --- |
| `web-client/src/e2e/drive-duel.tsx` | modify |
| `web-client/src/e2e/drive-duel.test.tsx` | create |

Read, and do not edit: `web-client/src/e2e/scripted-duel.ts`; `web-client/src/protocol/device-id.ts`;
`web-client/src/protocol/connection.ts`.

## Scope

- `driveScriptedDuel`'s options gain **`readonly storage?: Storage`**. When absent it builds one with
  `inMemoryStorage()` exactly as today, so every merged caller is byte-unaffected.
- `inMemoryStorage` becomes **exported** from `drive-duel.tsx`. Its body does not change — it stays
  the one in-memory `Storage` double this directory owns, for the `DEC-032` reason its existing
  doc comment already gives.
- `DuelRun` gains **`readonly storage: Storage`** — the storage the run actually used, whether handed
  in or built. A caller that passed one already has it; a caller that did not needs it to read the
  device id back.
- Nothing else in `drive-duel.tsx` changes: not `actThroughTheBar`, not `CountingSocket`, not the
  replay loop, not the tree it renders.

## Out of scope

- Wrapping the rendered tree in any provider — that is `TASK-041406`'s `drive-arc.tsx`, a **separate**
  module. This driver keeps rendering `<DuelProvider><Lobby /></DuelProvider>` and knows nothing
  about accounts.
- Any HTTP double — `TASK-041402`.
- Exporting `actThroughTheBar`. `TASK-041406` replays no duel of its own; it reuses this driver.

## Tests

`drive-duel.test.tsx`

| Test | Proves |
| --- | --- |
| `writes the seat own device id into the storage it was handed` | Driving seat 0 into a caller's storage leaves `pd.deviceId` === `"device-seat-0"` in **that** object, readable after the run returns. |
| `two seats driven into two storages hold two different device ids` | Seat 0 and seat 1 into two storages give `"device-seat-0"` and `"device-seat-1"` — asserted **not equal** as well as equal to their own values, so one hard-coded id cannot pass both. |
| `a run given no storage still plays the whole script` | The default path is unchanged: no `storage` option, and `run.storage.getItem("pd.deviceId")` is still the seat's id, with `run.receivedCount` equal to the seat's server-step count. |

Use `DEVICE_ID_STORAGE_KEY` from `../protocol/device-id`, never the literal `"pd.deviceId"`.

## Acceptance criteria

- [ ] `drive-duel.test.tsx` `writes the seat own device id into the storage it was handed` passes
- [ ] `drive-duel.test.tsx` `two seats driven into two storages hold two different device ids` passes
- [ ] `drive-duel.test.tsx` `a run given no storage still plays the whole script` passes
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/drive-duel.test.tsx 2>&1 | grep -qE 'Tests +3 passed \(3\)'` exits 0
- [ ] `NO_COLOR=1 npm run --silent test -- src/e2e/whole-duel.test.tsx src/e2e/duel-secrecy.test.tsx src/e2e/scripted-duel.test.ts 2>&1 | grep -qE 'Tests +18 passed \(18\)'`
      exits 0 — the three merged `src/e2e/` files are pinned by **count over their own files**, never
      by a test name and never by a directory total that later tickets in this story will move
- [ ] `npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Proof

Measured on `develop` @ `a99222f4`, in this worktree, before the ticket was written:

1. `NO_COLOR=1 npx vitest run src/e2e/` prints `Test Files  3 passed (3)` and `Tests  18 passed (18)`;
   the same three files named explicitly print `Tests  18 passed (18)` too. The whole client suite is
   `Test Files  99 passed (99)`, `Tests  777 passed (777)`. If any of these differs on your base,
   stop and say so — do not adjust the criterion.
2. `python3 -c "import json; d=json.load(open('web-client/src/e2e/scripted-duel.gen.json')); print([s['steps'][0]['frame'] for s in d['seats']])"`
   prints the two `Welcome` frames quoted above. The two device ids are the fixture's, not this
   ticket's.
3. Deleting the `storage` option's use — that is, leaving `driveScriptedDuel` building its own —
   must redden `writes the seat own device id into the storage it was handed` and
   `two seats driven into two storages hold two different device ids`, and must leave
   `a run given no storage still plays the whole script` green. Run it. A mutation that reddens all
   three means the third test is not exercising the default path it claims to.

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
