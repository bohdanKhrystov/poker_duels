---
schema: 2
id: TASK-031008
title: With no code in hand, boot rejoins the room it remembers
type: task
status: backlog
parent: STORY-0310
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, storage, reconnect]
depends_on: [TASK-031007]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +304 passed \(304\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'rejoins the remembered room when the tab carried no code'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prefers the code the tab was opened with over the one it remembers'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends one JoinRoom on every Welcome, at the code it now holds'
  - cd web-client && npm run check
---

## Goal

Every `Welcome` — the first one and every one after a reconnect — is answered with a `JoinRoom` for
the room this tab belongs in, so a dropped socket and a reloaded tab both land back in the seat.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — the `Welcome` branch reads two sources |
| `web-client/src/store/boot.test.ts` | modify — three tests added, none changed |
| `web-client/src/protocol/room-memory.ts` | read — `readRoomCode` |

## Scope

- The `Welcome` branch becomes:

  ```ts
  if (message.type === "Welcome") {
    // The invite wins over the memory: a player who has just followed a link to
    // a new room means that room, whatever this browser was in last. The memory
    // is what answers for the host, whose URL never carried a code, and for any
    // tab whose socket has been reopened under it.
    const remembered = options.storage ? readRoomCode(options.storage) : null;
    const code = options.joinRoomCode ?? remembered;
    if (code !== null) connection.send({ type: "JoinRoom", code });
  }
  ```

- The reaction stays exactly-once-per-`Welcome` by construction, which is what `ADR-0032` bought:
  no ref, no guard, no cleanup, and one `JoinRoom` per socket rather than per mount.
- The order on the wire is `Hello` then `JoinRoom` and cannot be otherwise: `openConnection` sends
  `Hello` from `onopen`, and this fires on the `Welcome` that answers it.
- `joinRoomCode` keeps its type and its meaning — the code this tab's **URL** carried. Nothing in
  `main.tsx` changes.

## Out of scope

- Forgetting a room — `TASK-031009` and `TASK-031010`.
- What happens when the rejoin is refused. `RoomRegistry.resume` answers only for a player already
  seated in a room carrying a duel; anything else falls through to an ordinary join and is refused,
  which the store already records as `refusal` and the lobby already renders. That is a lobby
  return, not a retry, and this ticket adds no retry.
- Any change to the server or the wire. The recipe — reopen, `Hello`, `JoinRoom` — is the one
  `TASK-020810`, `TASK-020811` and `TASK-020814` already built and proved. Nothing new is asked of
  it.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`. `bootOverFakeSocket`
already returns the storage; seed it with `writeRoomCode` before the `Welcome` where a test needs a
memory.

| Test | Proves |
| --- | --- |
| `rejoins the remembered room when the tab carried no code` | storage seeded with `ZYXWVUTS`, `joinRoomCode: null`, `Welcome` → exactly one `JoinRoom`, carrying `ZYXWVUTS` |
| `prefers the code the tab was opened with over the one it remembers` | storage seeded with `ZYXWVUTS`, `joinRoomCode: "ABCDEFGH"`, `Welcome` → exactly one `JoinRoom`, carrying `ABCDEFGH`. **Two distinct codes in one test**: a branch that read only one of the two sources passes the other tests and fails this one |
| `sends one JoinRoom on every Welcome, at the code it now holds` | `joinRoomCode: null`, storage empty; `Welcome` → no `JoinRoom`; then `RoomJoined` with `ABCDEFGH`; then a second `Welcome` → exactly one `JoinRoom` in total, carrying `ABCDEFGH`. This is the host's reconnect, end to end through the reaction |

```ts
it("prefers the code the tab was opened with over the one it remembers", () => {
  const { socket, storage } = bootOverFakeSocket("ABCDEFGH");
  writeRoomCode(storage, "ZYXWVUTS");

  socket.open();
  socket.receive(WELCOME);

  expect(sentJoinRooms(socket)).toEqual([
    { type: "JoinRoom", code: "ABCDEFGH" },
  ]);
});
```

The two existing tests that pin this branch keep working and keep meaning what they said:
`sends exactly one JoinRoom when Welcome arrives with a code in hand` still passes its code as
`joinRoomCode` with an empty storage, and `sends no JoinRoom when the tab opened without a code`
now also proves that an empty memory sends nothing.

Three tests added, the ten before them unchanged. Three hundred and one exist, so the suite reports
**304**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 304 passed (304)` | three ran, the ten before them still do, and nothing else moved |
| the three `--reporter=verbose` greps | every name above exists |
| `npm run check` | typechecks — `code` narrows to `string` before the send |

**Name the edit that makes each assertion red:**

1. Write `const code = remembered ?? options.joinRoomCode;` → `prefers the code the tab was opened
   with over the one it remembers` fails with `ZYXWVUTS`. Revert.
2. Guard the branch with a `let welcomed` so it fires once → `sends one JoinRoom on every Welcome,
   at the code it now holds` fails with zero `JoinRoom`s. Revert.

Quote both in the PR.

## Acceptance criteria

- [ ] `booting the duel client > rejoins the remembered room when the tab carried no code` passes
- [ ] `booting the duel client > prefers the code the tab was opened with over the one it remembers` passes
- [ ] `booting the duel client > sends one JoinRoom on every Welcome, at the code it now holds` passes
- [ ] The ten tests already in `boot.test.ts` keep their names and every assertion
- [ ] `boot.ts` contains no `useEffect`, no `useRef` and no import from React
- [ ] `boot-strict-mode.test.tsx` is byte-identical to what it was, and its three tests still pass
- [ ] `npm run --silent test` reports `Tests  304 passed (304)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
