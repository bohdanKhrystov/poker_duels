---
schema: 2
id: TASK-131112
title: The address / renders nothing until the server has spoken
type: task
status: backlog
parent: STORY-1311
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, routing, lobby, recovery]
depends_on: [TASK-131111]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 95) }'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | awk '/^ *Tests +[0-9]+ passed \([0-9]+\)$/ { n = $2 } END { exit !(n >= 36) }'
  - grep -qF 'standing === "unknown"' web-client/src/lobby/Lobby.tsx
  - sh -c '! grep -qiE "Restoring|Reconnecting|Loading|spinner" web-client/src/lobby/Lobby.tsx'
  - awk '/^export type Screen =/ { on = 1 } on && /^  \| "/ { n++ } on && /;$/ { on = 0 } END { exit (n != 7) }' web-client/src/routing/screen.ts
  - grep -qF 'export const PROTOCOL_VERSION: ProtocolVersion = 6;' web-client/src/protocol/version.ts
  - awk '/^---$/ { fm++; next } fm == 1 && /^verify:/ { inv = 1; next } fm == 1 && inv && /^ / { if (index($0, "drive" ".mjs") || index($0, "stack" ".sh") || index($0, "vite" " preview")) { print FILENAME ": " $0; bad = 1 } next } fm == 1 { inv = 0 } END { exit bad ? 1 : 0 }' tasks/tasks/TASK-1311*.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A browser recovering a room paints **no element at all** at `/` until the server answers, so the
front door — and the two controls on it that can take a player out of the duel they are in — stops
being shown to a client that was told nothing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read `docs/adr/ADR-0118-a-recovering-browser-shows-nothing-it-was-not-told.md` §§1–2 and §6. Nothing
else.

## Scope

- Condition the fall-through. `ADR-0118` §2 reads it as `ADR-0114` §2's third row becoming
  `standing === "none"` rather than the bare fall-through, and says in as many words that the
  reading is **offered, not designed** — how the branch is written is this ticket's. Either shape
  satisfies it: an early `if (standing === "unknown") return <></>;` above the final `return`, or the
  final `return` guarded on `standing === "none"`. `<></>` renders no element and keeps the
  component's `ReactElement` return type; do not widen the signature to `ReactElement | null` for
  this.
- **Nothing is added to draw this; something is withheld.** No wordmark, no *Create a duel room*, no
  *Join the duel*, no profile strip, no doors, no room code, no invite panel, no spinner, no mark,
  no sentence. `App.tsx`'s dark full-height shell stands and is empty, and `App.tsx` is not opened.
- Comment it with `ADR-0118` §1's one sentence, because it is the whole rule and a reader will
  otherwise read the branch as a loading state: *the client shows what it was told, and before it is
  told it shows nothing.* Name what ends the silence — the first frame that answers the ask,
  `RoomJoined` or `Failure` — so nobody adds a timeout to "help".

## Out of scope

- **A client that cannot reach the server at all**, leaving the rectangle empty indefinitely.
  `ADR-0118` §5 names this, gives it no threshold and no reopening condition, and states that it is
  **deliberately not registered** as a `DEC` and does **not** block this story (`ADR-0105` §6's
  route: *a `DEC` nobody is working is noise in the open table*). So: no timeout, no fallback to the
  front door, no *"still trying"* line, and no `DEC` raised from inside this ticket. The product
  accepts looking briefly dead in exchange for never briefly lying, and accepts looking dead
  permanently against a server that answers neither frame. Both costs are on the record in
  `ADR-0118` §Consequences.
- **An interstitial of any kind** — not *Restoring your duel*, not *Reconnecting*, not a spinner,
  not a progress mark (`ADR-0118` §4). `ADR-0105` §4's merged discipline is the operative rule: a
  screen that needs a new sentence for this has outgrown its decision and owes a new ADR. A
  `verify:` gate refuses those four words in this file.
- **The chosen screens.** `rulingOn` already honours the record, the ladder, the account and sign-in
  on `unknown` and holds `verify` and `reset`. This decision is about `/` alone, and the branches
  above the fall-through are untouched.
- **Measuring how long the silence lasts.** `ADR-0118` §4's one-second reopening condition is read
  off a **built** bundle by a browser drive, and `ADR-0089` §2b forbids any of that reaching a
  `verify:` block. It is not this story's; see the story's *Out of scope*.
- **`DEC-110`'s missing seat check.** That the flashed front door could create a second room is
  evidence *for* this change and is cited as a force; the server's absent guard stays `DEC-110`'s.

## Tests

`renderLobby` gains an optional second argument for `roomAwaited` (defaulting to `false`, so the
other 91 tests in the file are untouched) and returns `container` alongside `send` and `forgetRoom`.
Four more `it` blocks:

| Test | Proves |
| --- | --- |
| `renders no element at all while the room this tab holds is unknown` | `roomAwaited` **true**, no frame applied, address `/` → `container.firstChild` is `null` and `container.textContent` is `""`. One assertion for *no element*, one for *no text* |
| `renders the front door at once for a browser holding no room` | the identical tree with `roomAwaited` **false** → *Create a duel room*, *Join the duel*, the `Poker Duels` wordmark and all three doors are on screen. **This is the input that stops the fix being "never render the front door"** — one fixture cannot tell a rule from a deletion (`ADR-0118` §6) |
| `ends the silence on the frame that names the room` | `roomAwaited` true, then `act(() => store.apply(ROOM_JOINED))` → *"Waiting for your rival"* is on screen |
| `ends the silence on a refusal, carrying the refusal` | `roomAwaited` true, then `act(() => store.apply({ type: "Failure", error: "UNKNOWN_ROOM" }))` → both *"No duel room has that code."* and *Create a duel room* are on screen. A reaped room lands on the sentence, not on silence |

The first two are a pair and must be written as one delta: same store, same address, same providers,
one boolean different.

**These merged tests must still pass unchanged, and none of their assertions moves:** every test in
this file that renders the front door — `asks the server for a room when the host clicks create`,
`the front door wears the card's wordmark`, `no state but the front door wears the wordmark`,
`says an unknown room is unknown`, `says a full room is full` and the rest. All of them leave
`roomAwaited` at its default `false`, so `standing` is `"none"` and the front door renders exactly
as it does today.

## Acceptance criteria

- [ ] All four tests above exist under those exact names and pass, and `Lobby.test.tsx` reports at
      least 95 tests
- [ ] `Lobby.tsx` tests `standing === "unknown"` and contains none of the words *Restoring*,
      *Reconnecting*, *Loading* or *spinner*
- [ ] `screen.ts`'s `Screen` union still has exactly seven members and `PROTOCOL_VERSION` is still 6
- [ ] `App.test.tsx` still reports at least 36 tests
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
