---
schema: 2
id: TASK-131107
title: A mailed token is not spent before the server has answered
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 1
labels: [client, routing, account]
depends_on: [TASK-131106]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 84) }'
  - grep -qF 'bootDuelClient' web-client/src/lobby/Lobby.test.tsx
  - grep -qF 'writeRoomCode' web-client/src/lobby/Lobby.test.tsx
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/protocol/one-module-owns-each-storage-key.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 4) }'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The one path `ADR-0112` §5 set as a hard requirement is closed by a gate that counts: a mailed link
opened by a tab that remembers a room sends **nothing** until the frames say whether it may.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §§5 and 7's
*Not spent* bullet, `web-client/src/store/boot.ts`, and `web-client/src/store/boot.test.ts` for the
fixture shape. Nothing else — **no production file is opened or changed by this ticket.**

## Scope

- Add one fixture to `Lobby.test.tsx` that reaches `roomAwaited` **through the tab's memory**, not
  through the provider prop, because that is what `ADR-0114` §7 requires and it is the only thing
  that proves memory → `boot.ts` → provider → `Lobby` is one wire rather than three:
  - an `inMemoryStorage()` (already in this file) with `writeRoomCode(storage, "ABCDEFGH")` called
    **before** the boot;
  - `bootDuelClient({ connect, joinRoomCode: null, storage })`, where `connect` returns a stub
    `Connection` — `{ status: { kind: "connecting" }, send: vi.fn(), close: vi.fn() }` — that
    delivers nothing, so the unknown window stays open for as long as the test wants it;
  - `<AccountProvider calls={accountCallsFixture({ verifyEmail })}>` over
    `<DuelProvider store={client.store} send={client.send} roomAwaited={client.roomAwaited}>`.
  - Frames are applied with `act(() => client.store.apply(frame))`, the way the rest of this file
    applies them.
- `verifyEmail` is a **counting spy**. Every assertion below is a count. `ADR-0114` §7 is explicit:
  never an assertion about the argument of the last call — zero and one are the whole requirement,
  and only a count tells them apart.

## Out of scope

- **Any production change.** If a test here goes red, the defect belongs to `TASK-131105` or
  `TASK-131106` and is a new ticket, not a widening of this one.
- **`ADR-0114` §6's residual window** — a mailed link arriving in the single render between
  `RoomJoined` and `Snapshot` is honoured and its token spent. That is the shape of an asynchronous
  authoritative server, not a defect of this mechanism, and closing it is a wire change and the
  architect's. Do not write a test asserting it is closed; do not write one asserting it is open
  either, since nothing in the client decides it.
- **The `reset` screen's own count.** One mailed screen proves the predicate; `spendsOnArrival`
  already carries `reset` and `TASK-131102` tests it as a pure function.

## Tests

Three more `it` blocks in `Lobby.test.tsx`:

| Test | Proves |
| --- | --- |
| `sends nothing from a mailed link while the room this tab remembers is unknown` | booted at `#/verify/zqx-verify-token-zqx` with the code in storage and **no frame applied** → `verifyEmail` called **zero** times, and `window.location.hash` still reads `"#/verify/zqx-verify-token-zqx"`. The token is neither read nor dropped |
| `still sends nothing once the frames say the duel is running` | the same tree, then `RoomJoined` + `Snapshot` → `verifyEmail` still **zero**, and `window.location.hash` now reads `""`. The hold became a refusal and the address went whole, token with it |
| `sends the token once the frames say the duel is over` | a fresh tree of the same shape, then `RoomJoined` + `DuelFinished` → `verifyEmail` called exactly **once**. A `FINISHED` room is entitled to open a mailed link (`ADR-0112` §§3, 5) |

The first and third are the pair that matters: **zero** and **one** on inputs that differ only in
which frame arrived. Either one alone is satisfied by a client that never sends, or by one that
always does.

## Acceptance criteria

- [ ] All three tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at
      least 84 tests
- [ ] `Lobby.test.tsx` names `bootDuelClient` and `writeRoomCode` — the fixture goes through the
      tab's memory
- [ ] `one-module-owns-each-storage-key.test.ts` still passes: the key literal stays in
      `room-memory.ts` and this test writes it through `writeRoomCode`
- [ ] Every assertion about `verifyEmail` is a call **count**; none reads the argument of a call
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
