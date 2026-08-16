---
schema: 2
id: TASK-031010
title: A room that is gone is forgotten, and no socket resumes into it
type: task
status: ready
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, storage, reconnect]
depends_on: [TASK-031009]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +309 passed \(309\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room the server says is gone'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no JoinRoom on the Welcome after the room is gone'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the room when the refusal answered no rejoin of its own'
  - cd web-client && npm run check
---

## Goal

`UNKNOWN_ROOM` answering a rejoin means that room has been reaped. The tab forgets it, so the next
socket does not ask again — and every later socket comes up at the lobby, not in a retry loop.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — one flag, one branch |
| `web-client/src/store/boot.test.ts` | modify — three tests added, none changed |
| `web-client/src/protocol/room-memory.ts` | read — `forgetRoomCode` |

## Scope

- One flag, set where the rejoin is sent and cleared by whatever answers it:

  ```ts
  // Whether the JoinRoom this reaction sent is still unanswered. A refusal is
  // only about our room when it is answering our rejoin — the same UNKNOWN_ROOM
  // reaches a player who mistyped a code in the lobby, and that must not throw
  // away a room this tab is seated in.
  let rejoining = false;
  ```

  set to `true` immediately before the `Welcome` branch's `connection.send({ type: "JoinRoom" … })`,
  and set to `false` in the `RoomJoined` branch.
- The new branch:

  ```ts
  if (
    message.type === "Failure" &&
    message.error === "UNKNOWN_ROOM" &&
    rejoining &&
    options.storage
  ) {
    rejoining = false;
    forgetRoomCode(options.storage);
  }
  ```

- What ends here is the *resume*, not the connection. `VERSION_MISMATCH` ends the retry loop because
  the server will refuse this client identically forever (`TASK-031005`); `UNKNOWN_ROOM` says only
  that one room is gone, and a socket that keeps coming back is what lets the player open the next
  one. The story's two clauses have different reasons and therefore different reach.
- The reducer is untouched — it already records the `Failure` as `refusal`, and the lobby already
  renders *No duel room has that code.*

## Out of scope

- Closing the socket, or stopping the reconnect loop. See above.
- `ROOM_FULL`. It means somebody else holds the seat, which for a rejoin can only happen if this
  browser is no longer the player it was; forgetting on it is not obviously right and nothing in
  this story needs it.
- Telling the player their duel is over. The lobby's refusal line is `TASK-030513`'s and says
  enough.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`.

| Test | Proves |
| --- | --- |
| `forgets the room the server says is gone` | storage seeded with `ZYXWVUTS`, `joinRoomCode: null`; `Welcome` → a `JoinRoom`; `Failure UNKNOWN_ROOM` → `readRoomCode(storage)` is `null` |
| `sends no JoinRoom on the Welcome after the room is gone` | the same, then a second `Welcome` → exactly **one** `JoinRoom` in the whole run |
| `keeps the room when the refusal answered no rejoin of its own` | `RoomJoined` with `ABCDEFGH` (which clears the flag), then a `Failure UNKNOWN_ROOM` → `readRoomCode(storage)` is still `ABCDEFGH`. A branch that forgot on every `UNKNOWN_ROOM` passes the two above and fails this one |

```ts
it("keeps the room when the refusal answered no rejoin of its own", () => {
  const { socket, storage } = bootOverFakeSocket(null);

  socket.open();
  socket.receive(WELCOME);
  socket.receive('{"type":"RoomJoined","code":"ABCDEFGH","seat":0}');
  socket.receive('{"type":"Failure","error":"UNKNOWN_ROOM"}');

  expect(readRoomCode(storage)).toBe("ABCDEFGH");
});
```

`leaves the socket open and quiet after a refusal` — already in this file, and the closest thing to
this ticket's subject — keeps every assertion: it inspects the frames sent, the socket's
`closed` flag and `state.refusal`, none of which this ticket touches. It boots with
`joinRoomCode: "ABCDEFGH"` and an empty storage, so the forget it now triggers removes a key that
was never written.

Three tests added, the fifteen before them unchanged. Three hundred and six exist, so the suite
reports **309**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 309 passed (309)` | three ran, the fifteen before them still do, and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks — `message.error` narrows on the `Failure` variant |

**Name the edit that makes each assertion red:**

1. Drop the `rejoining` condition → `keeps the room when the refusal answered no rejoin of its own`
   fails with `null`. Revert.
2. Never set `rejoining = false` on `RoomJoined` → the same test fails the same way, which is why
   the clear is named in the scope and not left to be inferred. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `booting the duel client > forgets the room the server says is gone` passes
- [ ] `booting the duel client > sends no JoinRoom on the Welcome after the room is gone` passes
- [ ] `booting the duel client > keeps the room when the refusal answered no rejoin of its own` passes
- [ ] The fifteen tests already in `boot.test.ts` keep their names and every assertion, including
      `leaves the socket open and quiet after a refusal`
- [ ] `boot.ts` closes no socket and calls `connection.close()` nowhere
- [ ] `npm run --silent test` reports `Tests  309 passed (309)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
