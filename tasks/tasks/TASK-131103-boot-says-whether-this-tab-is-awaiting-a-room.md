---
schema: 2
id: TASK-131103
title: Boot says whether this tab is awaiting a room
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [client, store]
depends_on: [TASK-131102]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/boot.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 26) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/store/one-connection.test.ts 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 2) }'
  - awk '{ n += gsub(/roomAwaited/, "&") } END { exit (n < 3) }' web-client/src/store/boot.ts
  - sh -c '! grep -qF "localStorage" web-client/src/store/boot.ts'
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`bootDuelClient` returns one boolean — *this tab is asking the server about a room and has not been
answered* — computed once at construction, so the tree can withhold rather than guess.

## Files

| File | Action |
| --- | --- |
| `web-client/src/store/boot.ts` | modify |
| `web-client/src/store/boot.test.ts` | modify |

Read `docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md` §5. Nothing
else.

## Scope

- Add `readonly roomAwaited: boolean` to the `DuelClient` interface, KDoc'd in `ADR-0114` §5's own
  words: **it is used only to withhold, never to assert.** A remembered room code is a code the
  server minted; the client does not claim to be seated on the strength of it. It says only *do not
  act yet, the server has not answered.*
- Compute it **once, at construction** — not inside the `Welcome` handler, and not on every read:
  `(options.joinRoomCode ?? (options.storage ? readRoomCode(options.storage) : null)) !== null`.
  Return it on the client object.
- **Leave the `Welcome` handler exactly as it is.** It re-reads the memory for its own reason (a
  socket reopened under a tab that was seated later), and that read is not this one. Two reads of
  the same key here is correct and the comment should say so.
- The reach for the key stays where it already is: `readRoomCode` from `../protocol`, never
  `localStorage` (`ADR-0086` §2 — the global is `main.tsx`'s alone). The `verify:` gate checks it.

## Out of scope

- **Carrying it into the tree.** `duel-provider.tsx` and `main.tsx` are `TASK-131104`'s.
- **Keeping it fresh.** It is a snapshot and `ADR-0114`'s Consequences already accepts that a tab
  which forgets its room mid-document keeps a stale `true` — harmless because `refusal` and the
  standing dominate the predicate wherever it would matter. Do not add a subscription, a setter or a
  second read to fix this; that would be answering a question `ADR-0114` closed.
- **Any wire or storage change.** No key is added, renamed or written.

## Tests

Four more `it` blocks in `boot.test.ts`:

| Test | Proves |
| --- | --- |
| `awaits a room when the address carried a code` | `joinRoomCode: "ABCDEFGH"`, empty storage → `roomAwaited` is `true` |
| `awaits a room when the tab remembers one` | `joinRoomCode: null`, storage holding a code via `writeRoomCode` **before** the boot → `true` |
| `awaits no room when the address carried none and the tab remembers none` | both absent → `false`. This is the input that stops the flag being a constant |
| `is decided at construction and not by a later RoomJoined` | boot with neither, then drive `Welcome` and a `RoomJoined` through the fake socket → `roomAwaited` still reads `false`, because it is a snapshot of what this tab **asked**, not of what it holds |

The last one is the load-bearing test: it is what a later reader needs in order to not "fix"
`roomAwaited` into a live value.

## Acceptance criteria

- [ ] All four tests above exist under those exact names and pass, and `boot.test.ts` reports at
      least 26 tests in total
- [ ] `DuelClient.roomAwaited` exists and is `readonly boolean`
- [ ] `boot.ts` names `roomAwaited` at least three times (the field, the computation, the return) and
      still never mentions `localStorage`
- [ ] `one-connection.test.ts` still passes — nothing here opens a second connection
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
