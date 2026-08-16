---
schema: 2
id: TASK-031009
title: A finished duel is forgotten, so the way back to the lobby stays open
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: standard
files_touched: 2
labels: [client, store, storage, reconnect]
depends_on: [TASK-031008]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +306 passed \(306\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room once the duel has finished'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no JoinRoom on the Welcome after a duel has finished'
  - cd web-client && npm run check
---

## Goal

Once the duel is over, the tab stops remembering where it was — so `TASK-030807`'s *Back to the
lobby* link, which is a page reload, reaches the lobby instead of resuming into the result again.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — one branch |
| `web-client/src/store/boot.test.ts` | modify — two tests added, none changed |
| `web-client/src/result/DuelResult.tsx` | read — the `<a href="/">` this exists for |

## Scope

- One branch beside the others:

  ```ts
  if (message.type === "DuelFinished" && options.storage) {
    // The way on from the result is a reload (TASK-030807). A tab that still
    // remembered this room would rejoin it and be handed the same DuelFinished
    // back, and the lobby would be unreachable.
    forgetRoomCode(options.storage);
  }
  ```

- It forgets on the frame, not on the click, so no component ever touches storage and `DuelResult`
  stays a function of its props.
- The store is left alone: `outcome` stays set, so a socket that drops *after* the duel ended still
  shows the result. What is dropped is only the intent to rejoin.

## Out of scope

- Resetting the store. The reducer clears nothing a frame established; starting from an empty store
  is what the reload is for.
- A duel that ends while this client is away. That case works through the same path and needs no
  code here: `resumeFrames` hands a finished room's returning seat a `DuelFinished`, the rejoin has
  already happened by then, and this branch forgets the room on the way in. `TASK-031012` renders
  it.
- Rematch. `STORY-0309` owns it and is blocked on `DEC-023`; nothing here forecloses it, since a
  rematch that the wire could carry would arrive as its own frame and could remember the room again.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`.

| Test | Proves |
| --- | --- |
| `forgets the room once the duel has finished` | `RoomJoined` with `ABCDEFGH` → storage holds it; a `DuelFinished` → `readRoomCode(storage)` is `null` |
| `sends no JoinRoom on the Welcome after a duel has finished` | booted with `joinRoomCode: null`; `RoomJoined`, `DuelFinished`, then a second `Welcome` → **no** `JoinRoom` was ever sent. The behaviour the previous test's storage assertion is only a proxy for |

```ts
it("sends no JoinRoom on the Welcome after a duel has finished", () => {
  const { socket } = bootOverFakeSocket(null);

  socket.open();
  socket.receive(WELCOME);
  socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
  socket.receive(
    '{"type":"DuelFinished","outcome":{"winner":0,"handsPlayed":3,"finalStacks":[2000,0]}}',
  );
  socket.receive(WELCOME);

  expect(sentJoinRooms(socket)).toEqual([]);
});
```

Two tests added, the thirteen before them unchanged. Three hundred and four exist, so the suite
reports **306**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 306 passed (306)` | two ran, the thirteen before them still do, and nothing else moved |
| the two `--reporter=verbose` greps | both names exist |
| `npm run check` | typechecks and lints |

**Name the edit that makes each assertion red:**

1. Delete the branch → both tests fail, the second with one `JoinRoom` for `ABCDEFGH`. Revert.
2. Put the branch on `Snapshot` instead → `forgets the room once the duel has finished` fails with
   `ABCDEFGH`, and a resume mid-duel would have stopped working. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `booting the duel client > forgets the room once the duel has finished` passes
- [ ] `booting the duel client > sends no JoinRoom on the Welcome after a duel has finished` passes
- [ ] The thirteen tests already in `boot.test.ts` keep their names and every assertion
- [ ] `DuelResult.tsx` is byte-identical to what it was, and names no `Storage` and no `localStorage`
- [ ] `npm run --silent test` reports `Tests  306 passed (306)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
