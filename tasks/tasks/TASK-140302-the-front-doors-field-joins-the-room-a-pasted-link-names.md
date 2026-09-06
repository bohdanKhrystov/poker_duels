---
schema: 2
id: TASK-140302
title: The front door's field joins the room a pasted link names
type: task
status: done
parent: STORY-1403
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby]
depends_on: [TASK-140301]
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lobby-after.txt" 2>&1; grep -qF 'Tests  102 passed (102)' "${TMPDIR:-/tmp}/lobby-after.txt"
  - cd web-client && cp src/lobby/Lobby.tsx "${TMPDIR:-/tmp}/Lobby.fixed.tsx" && perl -0pi -e 's/const code = roomCodeFromField\(typedCode\);/const code = typedCode.trim().toUpperCase();/' src/lobby/Lobby.tsx && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx > "${TMPDIR:-/tmp}/lobby-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/Lobby.fixed.tsx" src/lobby/Lobby.tsx; cmp -s "${TMPDIR:-/tmp}/Lobby.fixed.tsx" src/lobby/Lobby.tsx && grep -qF '"HTTP://192.168.0.142:5173/?ROOM=918RHERX"' "${TMPDIR:-/tmp}/lobby-before.txt" && grep -qF 'Tests  2 failed | 100 passed (102)' "${TMPDIR:-/tmp}/lobby-before.txt"
  - cd web-client && test "$(grep -cE 'new URL\(|URLSearchParams' src/lobby/Lobby.tsx || true)" = "0"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A rival who pastes the whole invite link into the front door's `Room code` field and presses
`Join the duel` sends the same `JoinRoom` message as one who typed the bare code.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

Read [`STORY-1403`](../stories/STORY-1403-the-room-code-field-takes-the-link-as-well-as-the-code.md)'s
*Design notes* and `web-client/src/lobby/room-link.ts` (44 lines after `TASK-140301` — it is the
function being called). In `Lobby.tsx`, only lines 43 and 73 and their surroundings are of interest;
in `Lobby.test.tsx`, only the block of join tests around line 485. **Nothing else is opened, and no
other file in the repository is changed.**

## Scope

- **Two lines in `Lobby.tsx`, and nothing else.** The import at line 43 becomes
  `import { roomCodeFromField } from "./room-link";`, and line 73 becomes:

  ```ts
    const code = roomCodeFromField(typedCode);
  ```

  Write that line verbatim: the third `verify:` gate re-creates today's behaviour by matching
  exactly this text, and a differently spelled equivalent makes that gate red. `normalizeRoomCode`
  has no other use in this file, so its import goes with it.
- **The submit handler, the label, the input and the button do not change.** The empty-code refusal
  (`if (code === "") return;`) and its comment stay exactly as they are — `roomCodeFromField` returns
  `""` for an empty or whitespace-only field, so the shipped refusal keeps seeing the value it
  refuses today.
- **No parsing in the component.** `Lobby.tsx` must not mention `new URL(`, `URLSearchParams` or
  `room=`; the derivation lives in `room-link.ts` (`TASK-140301`), and the fourth `verify:` gate
  pins that. The field's `value` stays `typedCode` — what the player pasted stays on screen
  untouched, and nothing is rewritten under their cursor.
- **Nothing new is said.** No hint, no confirmation, no *"link recognised"*, no error: `ADR-0022`
  keeps the server the only judge of whether a code names a room, so the front door may not report
  what it made of what was typed. What a paste that is neither a code nor a link does is send that
  text, normalised, and let the server answer it — the same failed join it spends today.
- **Add three tests** to `Lobby.test.tsx`, immediately after
  `it("sends a pasted code trimmed and upper-cased", ...)` (line 493), exactly as the *Tests*
  section specifies. Use the existing `renderLobby()` and `typeCode()` helpers; add no helper.
- Run `npm run format` before `npm run check`.

## Out of scope

- **Every merged assertion in `Lobby.test.tsx`.** None is deleted, weakened, renamed or rewritten.
  This ticket changes what the field does with a **URL** only; for every other input
  `roomCodeFromField` returns exactly what `normalizeRoomCode` returned, so
  `sends a pasted code trimmed and upper-cased`, `sends nothing when the code box holds only
  whitespace` and `sends nothing after a refusal until a fresh click` observe unchanged behaviour and
  must pass untouched. If one of them goes red, the change did something this ticket did not ask
  for — stop and report it rather than editing the assertion.
- **`room-link.ts`** — `TASK-140301`'s, merged before this starts, and byte-identical afterwards.
- **`main.tsx` and `store/boot.ts`.** The navigation path already works and is not re-plumbed.
- **The `Room code` label.** Whether it should advertise that it takes a link is `STORY-1403`'s
  *Out of scope* — one string, one card frame, and nobody has asked for it.
- **The front door's other seams** — the wordmark overlap and the `Play duel` rename (`STORY-1402`),
  the profile strip (`STORY-1408`).
- **A design card.** No new string and no new visible state (`ADR-0091` §2).

## Tests

Three new `it(...)` blocks in the existing top-level `describe("the lobby", ...)` of
`web-client/src/lobby/Lobby.test.tsx`, placed directly after the merged
`sends a pasted code trimmed and upper-cased`, whose `ABCDEFGH` is the **second** code these tests
lean on: the new ones carry `918RHERX`, the code in the human's own screenshot of the invite box, so
no single fixture value spans the file's join assertions.

`Lobby.test.tsx` → `describe("the lobby")`

| Test | Proves |
| --- | --- |
| `sends the code a pasted invite link carries` | With `typeCode("http://192.168.0.142:5173/?room=918RHERX")` and a click on `Join the duel`: `send` is called **once**, with `{ type: "JoinRoom", code: "918RHERX" }` — the exact string `roomLink` builds and `ADR-0094` names, written as a literal. It then asserts the input's `value` is still the whole pasted link, so an implementation that rewrites the field under the player fails |
| `sends the code a pasted link carries among other parameters` | `typeCode("  https://duels.example/?utm=mail&room=abcdefgh#seat ")` sends `{ type: "JoinRoom", code: "ABCDEFGH" }` once — a second, different code, reached past other parameters, a fragment and the whitespace a paste brings |
| `sends text carrying no room code as it always did, and says nothing about it` | `typeCode("https://duels.example/lobby")` sends `{ type: "JoinRoom", code: "HTTPS://DUELS.EXAMPLE/LOBBY" }` once — a URL with no `room` is not an error and not a silent no-op; it spends the same failed join it spends today. `screen.queryByText(/link/i)` is `null`: the front door says nothing about what it made of the text (`ADR-0022`) |

The merged `sends a pasted code trimmed and upper-cased` (bare code) and `sends nothing when the code
box holds only whitespace` (the refused input) are the other two of the three inputs the story
requires, and they are already in this file — this ticket keeps them green rather than restating
them. Together with the link tests they tell the two implementations apart in both directions: the
link tests fail on today's `normalizeRoomCode`, and the bare-code and whitespace tests fail on a
link-only implementation that parses every paste as a URL.

The second `verify:` command pins the file's absolute test count at **102** — 99 merged plus these
3 — so a collection error or a renamed test cannot pass as a green run. The third replaces line 73
with `typedCode.trim().toUpperCase()`, which is today's behaviour written without the import,
re-runs the file, restores it byte-for-byte (`cmp -s`), and requires the captured output to hold the
received `"HTTP://192.168.0.142:5173/?ROOM=918RHERX"` and `Tests  2 failed | 100 passed (102)`.
**These strings and counts were measured on a prototype of this exact change**, not computed.

**If `Lobby.test.tsx` has gained tests on `develop` since this ticket was written** — `STORY-1402`
edits the same file — the two numbers in the second and third `verify:` commands are the only text
in this ticket you may change, and only like this: run
`npx vitest run src/lobby/Lobby.test.tsx` on untouched `develop`, take the count it prints as *N*,
and use `Tests  N+3 passed (N+3)` and `Tests  2 failed | N+1 passed (N+3)`. Put both *N* and the
old 99 in the PR body. Nothing else about the gates moves; if the *received-value* string does not
appear, the tests are wrong and no number fixes that.

## Acceptance criteria

- [ ] `sends the code a pasted invite link carries` passes, asserting one call with
      `{ type: "JoinRoom", code: "918RHERX" }` and that the field still holds the pasted link
- [ ] `sends the code a pasted link carries among other parameters` passes, asserting one call with
      `{ type: "JoinRoom", code: "ABCDEFGH" }`
- [ ] `sends text carrying no room code as it always did, and says nothing about it` passes,
      asserting one call with `{ type: "JoinRoom", code: "HTTPS://DUELS.EXAMPLE/LOBBY" }` and that
      no text matching `/link/i` is on the front door
- [ ] The merged `sends a pasted code trimmed and upper-cased`, `sends nothing when the code box
      holds only whitespace` and `sends nothing after a refusal until a fresh click` pass unchanged,
      and no assertion anywhere in `Lobby.test.tsx` is deleted or weakened
- [ ] `web-client/src/lobby/Lobby.test.tsx` holds exactly 102 tests and all 102 pass
- [ ] With line 73 returned to today's behaviour, exactly the first two new tests fail and the other
      100 pass — asserted by the third `verify:` command, which restores the file
- [ ] `Lobby.tsx` contains no `new URL(` and no `URLSearchParams` — asserted by the fourth command
- [ ] The diff touches exactly two files, and in `Lobby.tsx` exactly two lines
- [ ] Every command in `verify:` exits 0

The PR body quotes the received value the before-run printed, from
`"${TMPDIR:-/tmp}/lobby-before.txt"`, not retyped from this ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
