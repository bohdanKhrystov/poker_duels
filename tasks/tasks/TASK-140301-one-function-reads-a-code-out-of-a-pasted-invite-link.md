---
schema: 2
id: TASK-140301
title: One function reads a room code out of whatever was pasted into the field
type: task
status: ready
parent: STORY-1403
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, lobby]
depends_on: []
verify:
  - cd web-client && npm ci && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/room-link.test.ts > "${TMPDIR:-/tmp}/room-link-after.txt" 2>&1; grep -qF 'Tests  10 passed (10)' "${TMPDIR:-/tmp}/room-link-after.txt"
  - cd web-client && cp src/lobby/room-link.ts "${TMPDIR:-/tmp}/room-link.fixed.ts" && perl -0pi -e 's/\n    if \(carried !== null\) return carried;//' src/lobby/room-link.ts && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/room-link.test.ts > "${TMPDIR:-/tmp}/room-link-before.txt" 2>&1; cp "${TMPDIR:-/tmp}/room-link.fixed.ts" src/lobby/room-link.ts; cmp -s "${TMPDIR:-/tmp}/room-link.fixed.ts" src/lobby/room-link.ts && grep -qF "expected 'HTTP://192.168.0.142:5173/?ROOM=918RH" "${TMPDIR:-/tmp}/room-link-before.txt" && grep -qF 'Tests  2 failed | 8 passed (10)' "${TMPDIR:-/tmp}/room-link-before.txt"
  - cd web-client && test "$(grep -rlF 'get("room")' src)" = "src/lobby/room-link.ts"
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/lobby/room-link.ts` exports one new function, `roomCodeFromField(text)`, which
yields the room code for anything a player types or pastes into the front door's field — the `room`
a whole invite link carries, and otherwise exactly the string the field yields today.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/room-link.ts` | modify |
| `web-client/src/lobby/room-link.test.ts` | modify |

Read [`STORY-1403`](../stories/STORY-1403-the-room-code-field-takes-the-link-as-well-as-the-code.md)'s
*Design notes*. Both files are short (24 and 28 lines). **Nothing else is opened, and no other file
in the repository is changed** — the call site is `TASK-140302`.

## Scope

- **Add exactly one exported function to `room-link.ts`**, after `roomLink`. Write it verbatim as
  below: the third `verify:` gate re-creates today's behaviour by deleting the line
  `    if (carried !== null) return carried;` character for character, and a differently spelled
  equivalent makes that gate red.

  ```ts
  /**
   * The code the front door's field yields for what was typed or pasted into it:
   * the `room` a pasted invite link carries, and otherwise the text normalised
   * exactly as before. `""` for an empty field, which the caller refuses.
   *
   * One parse, not two: the code a paste yields and the code the same link yields
   * when a tab navigates to it both come from `roomCodeFromSearch`. Text that is
   * neither is passed on untouched but for case — `ADR-0022` leaves the server the
   * only judge of whether a code names a room.
   */
  export function roomCodeFromField(text: string): string {
    try {
      const carried = roomCodeFromSearch(new URL(text.trim()).search);
      if (carried !== null) return carried;
    } catch {
      // Not a URL: the ordinary paste, a bare code.
    }
    return normalizeRoomCode(text);
  }
  ```

- **`normalizeRoomCode` and `roomCodeFromSearch` are not touched** — not their signatures, bodies,
  KDoc or comments. `roomCodeFromSearch` is *not* generalised to take arbitrary text, and this is the
  ticket's one design decision, taken here so nobody re-opens it: its `null` is load-bearing at
  `store/boot.ts:84,99`, where `options.joinRoomCode ?? remembered` prefers the URL's code over the
  remembered one. A generalised version would answer `"HTTPS://DUELS.EXAMPLE/LOBBY"` instead of
  `null` for a URL carrying no code, and that non-null would win the `??` and send a remembered
  player to nowhere. Two callers need two different answers to *"there is no `room` here"* — `null`
  for the boot path, the typed text for the field — so they get two functions over **one** parse.
- **The link is never parsed a second time.** Everything about *which* parameter carries the code
  and *how* it is normalised stays inside `roomCodeFromSearch`; `roomCodeFromField` only decides
  whether the text was a URL at all. The fourth `verify:` gate pins this: `get("room")` may appear
  in exactly one file in `web-client/src`, and that file is `room-link.ts`.
- **Add five tests** to `room-link.test.ts` in a second `describe("the code the field yields")`
  block, exactly as the *Tests* section specifies. Add the import of `roomCodeFromField` to the
  existing import; add no helper.
- Run `npm run format` before `npm run check` — prettier owns the wrapping of the new assertions.

## Out of scope

- **The call site.** `Lobby.tsx` still calls `normalizeRoomCode`, and nothing outside
  `room-link.ts`/`room-link.test.ts` changes in this ticket — `TASK-140302` moves the field onto the
  new function and asserts the message the socket is sent. A green build here means the function
  exists and is proven, not that the field uses it.
- **`main.tsx`.** It keeps calling `roomCodeFromSearch(window.location.search)`; a search string is
  not a URL and that call site is correct as written.
- **Validating the alphabet, the length or the origin.** `ADR-0022`'s no-oracle rule forbids the
  client from judging a code's shape, and a link from any origin carrying a `room` is still a code
  the server can answer. `roomCodeFromField` refuses nothing and reports nothing: the only value it
  treats specially is the empty one, and only because the caller already refuses `""`.
- **A design card.** No new string and no new visible state (`ADR-0091` §2, and `STORY-1403`'s
  *Design notes*).

## Tests

Five new `it(...)` blocks in a new `describe("the code the field yields", ...)` at the end of
`web-client/src/lobby/room-link.test.ts`. The link shape is the one `roomLink` emits and `ADR-0094`
names — `origin/?room=CODE` — written as a **literal**, never built by calling `roomLink`.

**Two different codes appear across these tests on purpose**: `918RHERX` (the code in the human's own
screenshot of the invite box) and `ABCDEFGH` (the file's merged fixture). A single fixture value
cannot tell a parse from a constant — an implementation that returned `"ABCDEFGH"` unconditionally
passes every test this file holds today.

`room-link.test.ts` → `describe("the code the field yields")`

| Test | Proves |
| --- | --- |
| `reads the code out of a whole invite link` | `roomCodeFromField("http://192.168.0.142:5173/?room=918RHERX")` is `"918RHERX"` — the paste path reaches the same answer the navigation path already gives for that link. Red today: the field would yield `HTTP://192.168.0.142:5173/?ROOM=918RHERX` |
| `still yields the bare code the field has always taken` | `roomCodeFromField("  abcdefgh  ")` is `"ABCDEFGH"` and `roomCodeFromField("918rherx")` is `"918RHERX"` — trimming and upper-casing survive, at two different codes. This is the test a link-only implementation fails, so the pair of it and the one above can tell the two implementations apart in both directions |
| `reads the code among other parameters, whatever surrounds the link` | `roomCodeFromField("  https://duels.example/?utm=mail&room=abcdefgh#seat ")` is `"ABCDEFGH"` — the `room` need not be the only parameter, a fragment does not hide it, and paste whitespace does not stop the URL parsing |
| `hands back text carrying no room code exactly as the field always did` | `roomCodeFromField("https://duels.example/lobby")` is `"HTTPS://DUELS.EXAMPLE/LOBBY"` and `roomCodeFromField("https://duels.example/?room=")` is `"HTTPS://DUELS.EXAMPLE/?ROOM="` — a URL with no code, and one with a blank code, are **not** errors and **not** empty: they fall through to today's behaviour and the server answers them (`ADR-0022`) |
| `yields nothing for an empty or whitespace-only field` | `roomCodeFromField("")` and `roomCodeFromField("   ")` are both `""` — the refused input. The caller's `if (code === "") return` still sees the empty string it refuses today, so no paste can turn the empty field into a join attempt |

**The three inputs that tell the implementations apart**, and why each is needed:

- a whole link, which today's `normalizeRoomCode` gets wrong and only the new parse gets right;
- a bare code, which the new parse must still get right — it is what rejects a *link-only*
  implementation that hands `new URL(...)` the text and returns `""` or throws on `ABCDEFGH`;
- an input that must be refused (whitespace only), which rejects an implementation that loses the
  empty answer and lets the field spend a failed join on nothing.

The second `verify:` command pins the file's absolute test count at **10** — 5 merged plus these
5 — so a collection error or a renamed test cannot pass as a green run. The third deletes the
`if (carried !== null) return carried;` line, which is exactly today's behaviour, re-runs the file,
restores it byte-for-byte (`cmp -s`), and then requires the captured output to hold
`expected 'HTTP://192.168.0.142:5173/?ROOM=918RH…' to be '918RHERX'` and
`Tests  2 failed | 8 passed (10)`. **These strings were measured on this exact prototype**, not
computed from it: the two link tests are the two that go red, and the bare-code, no-code and empty
tests must stay green under the mutation, which is what proves they are not merely restating the
new implementation.

## Acceptance criteria

- [ ] `reads the code out of a whole invite link` passes, asserting `"918RHERX"`
- [ ] `still yields the bare code the field has always taken` passes, asserting `"ABCDEFGH"` and
      `"918RHERX"` from the two inputs named above
- [ ] `reads the code among other parameters, whatever surrounds the link` passes, asserting
      `"ABCDEFGH"`
- [ ] `hands back text carrying no room code exactly as the field always did` passes, asserting both
      `"HTTPS://DUELS.EXAMPLE/LOBBY"` and `"HTTPS://DUELS.EXAMPLE/?ROOM="`
- [ ] `yields nothing for an empty or whitespace-only field` passes, asserting `""` twice
- [ ] `web-client/src/lobby/room-link.test.ts` holds exactly 10 tests and all 10 pass
- [ ] With `if (carried !== null) return carried;` deleted, exactly those first and third tests fail
      and the other 8 pass — asserted by the third `verify:` command, which restores the file
- [ ] `get("room")` appears in exactly one file under `web-client/src`, and it is
      `src/lobby/room-link.ts` — asserted by the fourth `verify:` command
- [ ] The five merged tests in `describe("the room link")` are byte-identical to `develop`, and
      `normalizeRoomCode`, `roomCodeFromSearch` and `roomLink` are byte-identical to `develop`
- [ ] Every command in `verify:` exits 0

The PR body quotes the before-run's two failure lines from `"${TMPDIR:-/tmp}/room-link-before.txt"`,
not retyped from this ticket.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
