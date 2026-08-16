---
schema: 2
id: TASK-031001
title: The room code lives under one storage key this module owns
type: task
status: done
parent: STORY-0310
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 3
labels: [client, protocol, storage, reconnect]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +280 passed \(280\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'reads back each room code it was told to remember'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers null when this browser has never been in a room'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'answers null when the stored code is blank'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room it was remembering'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the room code under a key of its own'
  - cd web-client && npm run check
---

## Goal

The browser can remember which room it is seated in, under one key `src/protocol/` owns, read
through an injected `Storage` — so a socket that reopens has something to rejoin.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/protocol/room-memory.ts` | create |
| `web-client/src/protocol/room-memory.test.ts` | create |
| `web-client/src/protocol/index.ts` | modify — one `export` line |
| `web-client/src/protocol/device-id.ts` | read — copy its shape exactly |
| `web-client/src/protocol/device-id.test.ts` | read — copy its `inMemoryStorage` helper |

## Scope

- `room-memory.ts` exports exactly four things and imports nothing:

  ```ts
  export const ROOM_CODE_STORAGE_KEY = "pd.roomCode";
  export function readRoomCode(storage: Storage): string | null
  export function writeRoomCode(storage: Storage, code: string): void
  export function forgetRoomCode(storage: Storage): void
  ```

- `readRoomCode` mirrors `readDeviceId` line for line: `null` when the key is absent, `null` when
  the stored value trims to empty, and otherwise the stored value **verbatim** — no upper-casing,
  no trimming of what is returned. Normalising is `room-link.ts`'s job and doing it twice would put
  two spellings of one code in the system.
- `forgetRoomCode` calls `storage.removeItem`. It removes the key rather than writing `""`, so a
  browser that has left a duel is indistinguishable from one that has never been in one.
- `index.ts` gains one line beside the device-id export:
  `export { ROOM_CODE_STORAGE_KEY, readRoomCode, writeRoomCode, forgetRoomCode } from "./room-memory";`
  in the order `npm run format` leaves it.
- The `Storage` is a parameter, never the `localStorage` global. `DEC-032` records why: under Vitest
  on Node 24+, the global is present and inert, and `TASK-030304` already took this way out.

## Out of scope

- Anything that *decides* when to remember or forget a room. `TASK-031007` (remember on
  `RoomJoined`), `TASK-031009` (forget on `DuelFinished`) and `TASK-031010` (forget on a refused
  rejoin) each own one of those, in `src/store/boot.ts`.
- Persisting anything else. The device id and the room code are the only two values this client
  keeps; a persisted `PlayerView` would be a stale game fact held by a client.
- Touching `device-id.ts`. This is a sibling of it, not a refactor of it.

## Tests

`web-client/src/protocol/room-memory.test.ts`, describe block `"the remembered room code"`. Copy
`device-id.test.ts`'s `inMemoryStorage()` helper verbatim — that duplication is deliberate and
already three files deep, and unpicking it is not this ticket.

| Test | Proves |
| --- | --- |
| `reads back each room code it was told to remember` | writes `ABCDEFGH`, reads `ABCDEFGH`; then writes `ZYXWVUTS`, reads `ZYXWVUTS`. **Two distinct codes in one test**, because a reader asserted only against a single fixture value cannot tell a stored value from a hardcoded constant |
| `answers null when this browser has never been in a room` | an empty `Storage` reads `null` |
| `answers null when the stored code is blank` | `setItem(ROOM_CODE_STORAGE_KEY, "   ")` reads `null` |
| `forgets the room it was remembering` | after `writeRoomCode` then `forgetRoomCode`, `readRoomCode` is `null` **and** `storage.getItem(ROOM_CODE_STORAGE_KEY)` is `null` — removed, not blanked |
| `keeps the room code under a key of its own` | `ROOM_CODE_STORAGE_KEY === "pd.roomCode"`, it is not `DEVICE_ID_STORAGE_KEY`, and writing a room code into a storage that already holds `pd.deviceId = "d-9"` leaves `readDeviceId` answering `"d-9"` |

```ts
it("reads back each room code it was told to remember", () => {
  const storage = inMemoryStorage();

  writeRoomCode(storage, "ABCDEFGH");
  expect(readRoomCode(storage)).toBe("ABCDEFGH");

  writeRoomCode(storage, "ZYXWVUTS");
  expect(readRoomCode(storage)).toBe("ZYXWVUTS");
});
```

Five tests added. Two hundred and seventy-five exist, so the suite reports **280**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 280 passed (280)` | five ran and nothing else moved |
| the five `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks, lints, and is formatted — including the new export line |

**Name the edit that makes each assertion red:**

1. Make `readRoomCode` return the literal `"ABCDEFGH"` → `reads back each room code it was told to
   remember` fails on its second half. Revert.
2. Make `forgetRoomCode` call `setItem(key, "")` → `forgets the room it was remembering` fails on
   its `getItem` half while its `readRoomCode` half still passes. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `the remembered room code > reads back each room code it was told to remember` passes
- [ ] `the remembered room code > answers null when this browser has never been in a room` passes
- [ ] `the remembered room code > answers null when the stored code is blank` passes
- [ ] `the remembered room code > forgets the room it was remembering` passes
- [ ] `the remembered room code > keeps the room code under a key of its own` passes
- [ ] `room-memory.ts` names `localStorage` nowhere, and neither does its test
- [ ] `device-id.ts` is byte-identical to what it was
- [ ] `npm run --silent test` reports `Tests  280 passed (280)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
