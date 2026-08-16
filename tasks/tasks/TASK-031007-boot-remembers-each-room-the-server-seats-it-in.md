---
schema: 2
id: TASK-031007
title: Boot remembers each room the server seats it in
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [client, store, storage, reconnect]
depends_on: [TASK-031006]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +301 passed \(301\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'remembers each room the server seats it in'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'remembers no room until the server names one'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'remembers nothing when it was given nowhere to remember it'
  - cd web-client && grep -qF 'storage: localStorage' src/main.tsx
  - cd web-client && npm run check
---

## Goal

Every `RoomJoined` the server sends is written to the room-code key, so the tab knows which room to
ask for when its next socket opens — including the host's, whose URL never carried a code.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — one option, one branch |
| `web-client/src/store/boot.test.ts` | modify — the helper gains a storage, three tests added |
| `web-client/src/main.tsx` | modify — one line |
| `web-client/src/protocol/room-memory.ts` | read — `writeRoomCode` |
| `docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md` | read — why this lives in boot |

## Scope

- `BootOptions` gains one **optional** field:

  ```ts
  /**
   * Where this tab remembers the room it is seated in, so a reopened socket
   * knows what to ask for. Optional because a client that cannot remember is
   * still a working client — a test that is about something else need not
   * invent a Storage — but `main.tsx` always passes one.
   */
  readonly storage?: Storage;
  ```

- The reaction gains one branch, beside the `Welcome` one that is already there:

  ```ts
  if (message.type === "RoomJoined" && options.storage) {
    writeRoomCode(options.storage, message.code);
  }
  ```

  `message.code` verbatim — the server's spelling of the code, never `typedCode` and never a
  re-normalised copy.
- This is a **boot reaction**, which is why it is here and not in `reconnecting.ts`: `ADR-0032`
  puts every message-triggered reaction in the boot wiring, outside the tree and outside transport.
- `main.tsx` gains `storage: localStorage,` to the `bootDuelClient` call and changes nothing else.
- `boot.test.ts`'s `bootOverFakeSocket` hoists its inline `inMemoryStorage()` into a named `const
  storage`, hands the *same* object to `openConnection` and to `bootDuelClient`, and returns it
  alongside `socket`, `client` and `connect`. One storage per tab, holding both the device id and
  the room code, exactly as `localStorage` does in a browser.

## This ticket owns nothing it unsettles

The seven tests already in `boot.test.ts` keep every assertion. `folds every frame the server sends
into the store` applies a `RoomJoined` and asserts the *store*; this ticket adds a write to storage
beside it and takes nothing away. The helper change is a hoist: the storage the connection is given
is the same object it was given before, now also reachable from the test.

`boot-strict-mode.test.tsx` is **not** touched and must not be: `storage` is optional, so its
options literal still typechecks, and its three tests are about StrictMode rather than persistence.

## Out of scope

- Reading the remembered code back. `TASK-031008` owns the rejoin.
- Forgetting it — `TASK-031009` (`DuelFinished`) and `TASK-031010` (a room that is gone).
- Making `storage` required. That would drag `boot-strict-mode.test.tsx` into this diff and put it
  over three files; there is no behaviour in it.
- The reducer. `DuelState.roomCode` already holds the code for the screens; this is a second,
  narrower copy that survives the page, and the reducer stays pure.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`.

| Test | Proves |
| --- | --- |
| `remembers each room the server seats it in` | after `RoomJoined` with code `ABCDEFGH`, `readRoomCode(storage)` is `ABCDEFGH`; after a second `RoomJoined` with `ZYXWVUTS`, it is `ZYXWVUTS`. **Two distinct codes**, because a write asserted only against the fixture's one code cannot be told from a hardcoded constant |
| `remembers no room until the server names one` | after `Welcome` and nothing else, `readRoomCode(storage)` is `null` |
| `remembers nothing when it was given nowhere to remember it` | booted without `storage`, a `RoomJoined` still reaches the store (`mySeat`, `roomCode`) and throws nothing |

```ts
it("remembers each room the server seats it in", () => {
  const { socket, storage } = bootOverFakeSocket();

  socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":1}');
  expect(readRoomCode(storage)).toBe("ABCDEFGH");

  socket.receive('{"type":"RoomJoined","code":"ZYXWVUTS","seat":0}');
  expect(readRoomCode(storage)).toBe("ZYXWVUTS");
});
```

Three tests added, the seven before them unchanged. Two hundred and ninety-eight exist, so the suite
reports **301**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 301 passed (301)` | three ran, the seven before them still do, and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `grep -F 'storage: localStorage' src/main.tsx` | the browser's boot has somewhere to remember |
| `npm run check` | typechecks — `storage` is optional and `message.code` narrows |

**Name the edit that makes each assertion red:**

1. Write `writeRoomCode(options.storage, "ABCDEFGH")` → `remembers each room the server seats it in`
   fails on its second half. Revert.
2. Move the write into the `Welcome` branch → `remembers no room until the server names one` still
   passes (there is nothing to write) but `remembers each room the server seats it in` fails with
   `null`. Revert.

Quote both in the PR, and say in the PR body that `boot-strict-mode.test.tsx` is unchanged.

## Acceptance criteria

- [ ] `booting the duel client > remembers each room the server seats it in` passes
- [ ] `booting the duel client > remembers no room until the server names one` passes
- [ ] `booting the duel client > remembers nothing when it was given nowhere to remember it` passes
- [ ] The seven tests already in `boot.test.ts` keep their names and every assertion
- [ ] `boot-strict-mode.test.tsx` is byte-identical to what it was
- [ ] `boot.ts` still imports nothing from React — `store/framework-free.test.ts` still passes
- [ ] `main.tsx` contains `storage: localStorage`
- [ ] `npm run --silent test` reports `Tests  301 passed (301)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
