---
schema: 2
id: TASK-030919
title: A finished duel forgets nothing, and the next socket rejoins that room
type: task
status: ready
parent: STORY-0309
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, store, rooms]
depends_on: [TASK-030918]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +575 passed \(575\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'keeps the room the finished duel was played in'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'rejoins that room on the Welcome a reopened socket sends'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'rejoins nothing once the player has left the finished room'
  - cd web-client && ! grep -qF 'DuelFinished' src/store/boot.ts
  - cd web-client && ! grep -qF 'forgets the room once the duel has finished' src/store/boot.test.ts
  - cd web-client && ! grep -qF 'sends no JoinRoom on the Welcome after a duel has finished' src/store/boot.test.ts
  - cd web-client && npm run check
---

## Goal

`pd.roomCode` means *the room this tab is seated in*: a finished duel leaves it alone, and the next
socket this tab opens rejoins that room and is handed the offer it is still holding.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify — one branch and its comment deleted |
| `web-client/src/store/boot.test.ts` | modify — two deleted, three added |
| `docs/adr/ADR-0072-a-tab-remembers-its-room-until-the-player-leaves-it.md` | read — §2, §3 and **§9's table**, which this ticket implements exactly |

## Scope

- Delete this branch from the message reaction in `boot.ts`, comment and all:

  ```ts
  if (message.type === "DuelFinished" && options.storage) {
    forgetRoomCode(options.storage);
  }
  ```

  Nothing replaces it. After this, `boot.ts` does not name `DuelFinished` at all, and a `verify`
  command says so.
- **Nothing else in `boot.ts` moves.** The `Failure(UNKNOWN_ROOM)` branch and its `rejoining` guard
  stay **exactly as written** (`ADR-0072` §3): the same refusal reaches a player who mistyped a code
  in the lobby, and that must not throw away a room this tab is seated in. `room-memory.ts` is not
  opened — the key, the `Storage` and all three functions are unchanged (§8).
- **This ticket owns the two merged tests it invalidates.** `TASK-031009`'s
  `forgets the room once the duel has finished` and
  `sends no JoinRoom on the Welcome after a duel has finished` assert the behaviour this reverses.
  They are **deleted here and replaced here**, per `ADR-0072` §9. `TASK-031009` itself stays `done`
  and is not rewritten.

Where §9's four replacement assertions live, so that none is dropped:

| `ADR-0072` §9 asks for | Ticket, test |
| --- | --- |
| `RoomJoined` + `DuelFinished` leaves the code in storage | here — `keeps the room the finished duel was played in` |
| `forgetRoom()` removes it | `TASK-030915` — `forgets the room when the tab is told to` |
| the three frames plus a second `Welcome` send exactly one `JoinRoom`, at that code | here — `rejoins that room on the Welcome a reopened socket sends` |
| the same run with `forgetRoom()` before the second `Welcome` sends none | here — `rejoins nothing once the player has left the finished room`; `TASK-030915` has the same claim without a finish in it |

## Out of scope

- Anything on the wire, in the store, or in `poker-server`. `ADR-0044` §5's server half is merged
  (`TASK-021307`); this is what makes it reachable (`ADR-0072` §8).
- A timer, an expiry, or any clock against the rematch window. §6: the only authority on a dead room
  is `Failure(UNKNOWN_ROOM)`, and a stale code costs one budgeted failed join per boot.
- Forgetting on the `UNKNOWN_ROOM` that answers a **rematch press**. Boot cannot see which frame a
  `Failure` answers, and §7 says why that is enough: `TASK-030909`'s screen retires the control and
  `TASK-030918`'s way back forgets.

## Tests

`web-client/src/store/boot.test.ts`, describe block `"booting the duel client"`. **Two deleted,
three added.** They reuse `bootOverFakeSocket`, `sentJoinRooms` and `readRoomCode`, and the
`DuelFinished` frame string the deleted tests used.

| Test | Proves |
| --- | --- |
| `keeps the room the finished duel was played in` | `RoomJoined(ABCDEFGH)` ⇒ storage holds `"ABCDEFGH"`; a `DuelFinished` ⇒ it **still** holds `"ABCDEFGH"` |
| `rejoins that room on the Welcome a reopened socket sends` | `Welcome`, `RoomJoined(ABCDEFGH)`, `DuelFinished`, a second `Welcome` ⇒ `sentJoinRooms` is exactly `[{ type: "JoinRoom", code: "ABCDEFGH" }]` — one, and at that code |
| `rejoins nothing once the player has left the finished room` | the same four frames with `client.forgetRoom()` between the `DuelFinished` and the second `Welcome` ⇒ `sentJoinRooms` is `[]` |

The first asserts the code **before** the finish as well as after: a client that had stopped
remembering anything at all would fail there, which is the failure §9 exists to make impossible.

The second is the pair's other half, and it is needed: the storage assertion alone cannot tell
*remembered* from *rejoined*, and the frame assertion alone cannot tell *forgotten* from *never
written*.

## Proof

| Command | Proves |
| --- | --- |
| `Tests 575 passed (575)` | two deleted and three added, against 574 |
| the three `--reporter=verbose` greps | all three names exist |
| the two `! grep` commands on `boot.test.ts` | the two reversed assertions are gone rather than left failing or skipped |
| `! grep -qF 'DuelFinished' src/store/boot.ts` | the branch is gone, not disabled |

**Name the edit that makes each assertion red** — this was run before the ticket was written, and
the result is the reason the third test is described the way it is:

1. Put the `DuelFinished` branch back → `keeps the room the finished duel was played in` and
   `rejoins that room on the Welcome a reopened socket sends` both fail. Revert.
2. **The third test passes with the branch back**, because the branch has already forgotten the code
   by the time `forgetRoom()` is called. It is not this ticket's guard against the reversal being
   undone — the two above are; it is the guard that the forget still reaches a room whose duel has
   ended. Do not treat its green as evidence that the branch is gone; the `! grep` does that.
3. Make `forgetRoom` a no-op → the third test fails. Revert.

Quote 1 and 3 in the PR, and say in the PR body that the count moved by **+1** because two tests
were deleted and three added.

## Acceptance criteria

- [ ] `booting the duel client > keeps the room the finished duel was played in` passes
- [ ] `booting the duel client > rejoins that room on the Welcome a reopened socket sends` passes
- [ ] `booting the duel client > rejoins nothing once the player has left the finished room` passes
- [ ] `boot.test.ts` no longer contains `forgets the room once the duel has finished` or `sends no JoinRoom on the Welcome after a duel has finished`
- [ ] `boot.ts` does not contain the string `DuelFinished`
- [ ] These three merged tests are unchanged from `develop` and still pass: `forgets the room the server says is gone`, `sends no JoinRoom on the Welcome after the room is gone`, `keeps the room when the refusal answered no rejoin of its own`
- [ ] `room-memory.ts` is unchanged from `develop`
- [ ] `npm run --silent test` reports `Tests  575 passed (575)`
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
