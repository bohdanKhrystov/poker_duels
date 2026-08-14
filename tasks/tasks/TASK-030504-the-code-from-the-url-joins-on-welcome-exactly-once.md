---
schema: 2
id: TASK-030504
title: The code the URL carried joins on Welcome, exactly once
type: task
status: done
parent: STORY-0305
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030503]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +98 passed \(98\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends exactly one JoinRoom when Welcome arrives with a code in hand'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no JoinRoom when the tab opened without a code'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends nothing but Hello before Welcome'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'leaves the socket open and quiet after a refusal'
  - cd web-client && npm run check
---

## Goal

The story's hardest criterion, proved without React: a tab booted with a room code sends exactly
one `JoinRoom`, after `Welcome` and not before — as a boot reaction, with no guard and no ref.

## Files

`files_touched` counts the create/modify rows only.

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify |
| `web-client/src/store/boot.test.ts` | modify |
| `web-client/src/protocol/protocol.gen.ts` | read — `JoinRoom`, `Welcome`, `Failure` |
| `docs/adr/ADR-0032-react-subscribes-to-a-store-it-does-not-own.md` | read — §2, and the last of the alternatives considered |

## Scope

- `BootOptions` gains one **required** field:

  ```ts
  export interface BootOptions {
    readonly connect: (onMessage: (message: ServerMessage) => void) => Connection;
    /** The code this tab's URL carried, or `null` when it carried none. */
    readonly joinRoomCode: string | null;
  }
  ```

  Required, not optional: with `?`, "this tab carried no code" and "the caller forgot the field"
  are the same value, and `TASK-030515` passes a `string | null` straight from the URL parser.
- The `onMessage` callback in `bootDuelClient` becomes exactly this:

  ```ts
  const connection = options.connect((message) => {
    store.apply(message);
    // A message-triggered send is a boot reaction, never a screen effect: one
    // boot per tab and one Welcome per socket is the whole of "exactly once",
    // with no ref, no guard and no cleanup anywhere (ADR-0032).
    if (message.type === "Welcome" && options.joinRoomCode !== null) {
      connection.send({ type: "JoinRoom", code: options.joinRoomCode });
    }
  });
  ```

- **Fold first, react second** — the store holds the frame before anything is sent, so no listener
  ever observes a send that outran its cause.
- **No counter, no boolean, no `Set` of frames already reacted to.** Exactly-once is structural:
  one boot per tab, one `Welcome` per socket. A guard here would be a guard that has to be right,
  and `ADR-0032` rejects that shape by name.
- The code is sent **verbatim as given**. Trimming and upper-casing belong to the parser that
  produced it (`TASK-030509`), so this module holds no opinion about the alphabet — `ADR-0022` has
  the server answer an unparseable code and an unknown room identically on purpose.
- Nothing reacts to `Failure`, and nothing calls `connection.close()` — ever. `Connection.send`
  already refuses to speak to a server that answered `outdated`, so a version mismatch needs no
  handling here.

## Out of scope

- Reading the code out of the URL — `TASK-030509`, and `main.tsx` composes them in `TASK-030515`.
- Rendering the refusal — `TASK-030513`.
- Re-`Welcome` on a reconnect firing the reaction again. `ADR-0032` names it and leaves it to
  `STORY-0310` to decide on purpose; there is no reconnect today.
- Any React at all: the double-mount proof is `TASK-030507`, and it changes no production code.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`. Four `it` blocks
appended after `TASK-030503`'s three.

Three edits to what `TASK-030503` wrote, and no others:

- `bootOverFakeSocket` takes `(joinRoomCode: string | null = null)` and passes it through:
  `bootDuelClient({ connect, joinRoomCode })`.
- A `WELCOME` fixture above it:

  ```ts
  const WELCOME = JSON.stringify({
    type: "Welcome",
    deviceId: "d-1",
    protocolVersion: PROTOCOL_VERSION,
  });
  ```

  `PROTOCOL_VERSION` comes from the barrel and **must not** be a literal: a mismatched version
  makes the connection `outdated`, `Connection.send` then silently refuses everything, and these
  tests fail for a reason that has nothing to do with the reaction.
- One more helper: `sentJoinRooms(socket)` — `sentFrames(socket).filter((frame) => frame.type ===
  "JoinRoom")`.

The three existing `it` blocks are not edited.

| Test | Proves |
| --- | --- |
| `sends exactly one JoinRoom when Welcome arrives with a code in hand` | boot with `"ABCDEFGH"`, `socket.open()`, `socket.receive(WELCOME)` leaves `sentJoinRooms(socket)` deep-equal to `[{ type: "JoinRoom", code: "ABCDEFGH" }]` — one frame, carrying the code |
| `sends no JoinRoom when the tab opened without a code` | the same sequence booted with `null` leaves `sentJoinRooms(socket)` `[]` |
| `sends nothing but Hello before Welcome` | boot with `"ABCDEFGH"` then `socket.open()` alone leaves the frame types deep-equal to `["Hello"]` |
| `leaves the socket open and quiet after a refusal` | after `Welcome` then `socket.receive('{"type":"Failure","error":"UNKNOWN_ROOM"}')`, the frame types are still `["Hello", "JoinRoom"]`, `socket.closed` is `false`, and `client.store.getState().refusal` is `"UNKNOWN_ROOM"` |

Four tests. Ninety-four exist, so the suite reports **98**.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 98 passed (98)` | the four tests ran and the ninety-four before them still do |
| the four `--reporter=verbose` greps | each exists by name |
| `npm run check` | typechecks, lints, formats |

**Name the edit that makes each assertion red** — all three were run against this exact test file:

1. Weaken the condition to `if (options.joinRoomCode !== null)` (react to every frame, not just
   `Welcome`) → `leaves the socket open and quiet after a refusal` fails with `expected [ 'Hello',
   'JoinRoom', 'JoinRoom' ] to deeply equal [ 'Hello', 'JoinRoom' ]`. Revert.
2. Move the send out of the callback and put it straight after `options.connect(...)`, guarded
   only by `joinRoomCode !== null` → `sends nothing but Hello before Welcome` fails with
   `expected [ 'JoinRoom', 'Hello' ] to deeply equal [ 'Hello' ]`. Revert.
3. Drop the null check and send `code: options.joinRoomCode ?? ""` → `sends no JoinRoom when the
   tab opened without a code` fails with `expected [ { type: 'JoinRoom', code: '' } ] to deeply
   equal []`. Revert.

Quote all three in the PR. The second is the whole point: the send is triggered by a *message*,
not by boot, and not by a screen.

## Acceptance criteria

- [ ] `booting the duel client > sends exactly one JoinRoom when Welcome arrives with a code in hand` passes
- [ ] `booting the duel client > sends no JoinRoom when the tab opened without a code` passes
- [ ] `booting the duel client > sends nothing but Hello before Welcome` passes
- [ ] `booting the duel client > leaves the socket open and quiet after a refusal` passes
- [ ] `npm run --silent test` reports `Tests  98 passed (98)`
- [ ] `boot.ts` contains no `useRef`, no `useEffect`, no boolean or counter guarding the send, and
      no call to `close()`
- [ ] The three `it` blocks from `TASK-030503` are unedited apart from the helper's new parameter
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
