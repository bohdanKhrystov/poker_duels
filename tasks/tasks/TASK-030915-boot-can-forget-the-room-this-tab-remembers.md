---
schema: 2
id: TASK-030915
title: Boot can forget the room this tab remembers
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030914]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +569 passed \(569\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'forgets the room when the tab is told to'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'sends no JoinRoom on the Welcome after the tab forgot the room'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'takes the forget where there is nothing to forget in'
  - cd web-client && grep -qF 'DuelFinished' src/store/boot.ts
  - cd web-client && npm run check
---

## Goal

`DuelClient` has a third member: a tab can be told to forget the room it remembers, so no socket
opened after that one rejoins it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — one interface member, one member on the returned object |
| `web-client/src/store/boot.test.ts` | modify — three added, none removed |
| `docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md` | read — §4, the member and its KDoc verbatim |

## Scope

- `DuelClient` gains `forgetRoom`, with `ADR-0072` §4's KDoc, written as the ADR writes it:

  ```ts
  export interface DuelClient {
    readonly store: DuelStore;
    readonly send: (message: ClientMessage) => void;
    /**
     * Forgets the room this tab remembers, so no socket opened after this one
     * rejoins it. It tells the server nothing — there is no leave on the wire —
     * and the socket that is open keeps its seat: the memory is about the next
     * socket, never the current one.
     */
    readonly forgetRoom: () => void;
  }
  ```

- `bootDuelClient` returns it, beside `store` and `send`:

  ```ts
    forgetRoom: () => {
      if (options.storage) {
        forgetRoomCode(options.storage);
      }
    },
  ```

  The `if` is the same optionality `BootOptions.storage` already carries, for the reason its own
  comment gives: a client that cannot remember is still a working client. `forgetRoomCode` is
  already imported.
- **Nothing else in `boot.ts` moves.** The `Welcome`, `RoomJoined`, `Failure` and `DuelFinished`
  reactions are untouched, including their comments.

## Out of scope

- **Deleting the `DuelFinished` branch.** That is `TASK-030919`, and it must come *after* the screen
  is wired (`TASK-030918`) or the lobby is unreachable from a finished duel in between. A `verify`
  command asserts the branch is **still there** when this ticket merges.
- The provider (`TASK-030916`), `DuelResult` (`TASK-030917`), the wiring (`TASK-030918`).
- Sending anything. There is no leave on the wire (`ADR-0044`), and this sends nothing: the open
  socket keeps its seat.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`. **Three added, none
removed or edited.** They reuse the file's `bootOverFakeSocket`, `sentJoinRooms` and `readRoomCode`.

| Test | Proves |
| --- | --- |
| `forgets the room when the tab is told to` | `RoomJoined(ABCDEFGH)` ⇒ `readRoomCode(storage)` is `"ABCDEFGH"`; then `client.forgetRoom()` ⇒ it is `null` |
| `sends no JoinRoom on the Welcome after the tab forgot the room` | `Welcome`, `RoomJoined(ABCDEFGH)`, `Welcome` ⇒ `sentJoinRooms` is exactly `[{ type: "JoinRoom", code: "ABCDEFGH" }]`; then `client.forgetRoom()` and **two** more `Welcome`s ⇒ still exactly that one |
| `takes the forget where there is nothing to forget in` | a client booted with **no** `storage` (the shape `remembers nothing when it was given nowhere to remember it` uses): after a `RoomJoined`, `client.forgetRoom()` throws nothing and the store still holds `roomCode` — the memory is storage's, not the store's |

The first test asserts the code **before** and after: a `forgetRoom` that removed nothing would pass
the second assertion of a test that only looked afterwards if the write had never happened.

**No test here drives `DuelFinished`.** The branch that forgets on that frame is still in `boot.ts`
until `TASK-030919`, so a test that finished a duel before forgetting would pass with `forgetRoom`
implemented as `() => {}` — it would be asserting the branch, not this member. `TASK-030919` owns
the `DuelFinished` forms, once the branch is gone and they mean something.

The second test's two trailing `Welcome`s stand for two more sockets the retry loop would open
(`TASK-031003`–`TASK-031005`), the same reason the merged `sends no JoinRoom on the Welcome after
the room is gone` gives for its three.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 569 passed (569)` | three added to 566, none lost |
| the three `--reporter=verbose` greps | all three names exist |
| `grep -qF 'DuelFinished' src/store/boot.ts` | the branch this ticket does not touch is still there |

**Name the edit that makes each assertion red:**

1. Make `forgetRoom` a no-op (`() => {}`) → `forgets the room when the tab is told to` fails on the
   second assertion. Revert.
2. Have the `Welcome` reaction read the code from a variable captured at `RoomJoined` instead of
   from `readRoomCode(options.storage)` → `sends no JoinRoom on the Welcome after the tab forgot the
   room` fails, because the rejoin has to read storage on every `Welcome` for a forget to reach it.
   Revert.
3. Drop the `if (options.storage)` guard → `takes the forget where there is nothing to forget in`
   fails on the `TypeError`. Revert.

Quote 1 and 3 in the PR.

## Acceptance criteria

- [ ] `booting the duel client > forgets the room when the tab is told to` passes
- [ ] `booting the duel client > sends no JoinRoom on the Welcome after the tab forgot the room` passes
- [ ] `booting the duel client > takes the forget where there is nothing to forget in` passes
- [ ] `boot.ts` still contains its `DuelFinished` branch, unedited — this ticket does not reverse it
- [ ] Every pre-existing `it` block in `boot.test.ts` is unchanged from `develop`, and none is removed
- [ ] No file outside the two in the table differs from `develop`
- [ ] `npm run --silent test` reports `Tests  569 passed (569)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
