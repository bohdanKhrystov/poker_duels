---
schema: 2
id: TASK-130205
title: Creating a duel lands the host at the table, and the waiting screen is gone
type: task
status: backlog
parent: STORY-1302
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, table, lobby]
depends_on: [TASK-130204]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent build
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/WaitingTable.test.tsx 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - awk 'index($0, "heading\", { name: \"Waiting for your rival\" }") { n++ } END { exit (n != 0) }' web-client/src/lobby/Lobby.test.tsx
  - awk 'index($0, "queryByText(\"Waiting for your rival\")") { n++ } END { exit (n != 3) }' web-client/src/lobby/Lobby.test.tsx
  - awk 'index($0, "adds exactly two strings to the waiting screen and no third") { n++ } END { exit (n != 0) }' web-client/src/lobby/Lobby.test.tsx
  - awk 'index($0, "and no seventh") { n++ } END { exit (n != 1) }' web-client/src/lobby/Lobby.test.tsx
  - sh -c 'grep -q "WaitingTable" web-client/src/lobby/Lobby.tsx && ! grep -q "WaitingForRival" web-client/src/lobby/Lobby.tsx'
  - sh -c 'grep -c "max-w-\[560px\]" web-client/src/lobby/Lobby.tsx | grep -q "^1$"'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`state.roomCode !== null && state.view === null` renders the duel table with the rival's seat
empty. The dedicated waiting screen is deleted, and the first `Snapshot` still ends the wait with
no navigation —
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §§1 and 7.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |
| `web-client/src/table/WaitingTable.tsx` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **`Lobby.tsx`:** the `state.roomCode !== null` branch returns
  `<WaitingTable code={state.roomCode} onLeave={forgetRoom} />` and nothing else. Delete the
  `WaitingForRival` function and the `InvitePanel` import it was the only user of. **The branch
  order does not change** — `outcome`, then `view`, then `roomCode` — which is what makes the
  opening `Snapshot` end the wait with no navigation (`ADR-0110` §7).
- **`Lobby.test.tsx`:** exactly three tests are edited, none added and none deleted. The count
  gate at **80** is what proves that.

### The three edits, named

**1 & 2 — three assertions become vacuous and must move.** `Waiting for your rival` stops being a
heading and becomes a seat line, so `queryByRole("heading", { name: "Waiting for your rival" })` is
null in *every* state and asserts nothing. Measured on `develop` 2026-09-02: exactly three lines
carry it — **556**, **888** and **908**, in

- `leaves the waiting panel when the first Snapshot arrives` (one), and
- `shows the result when the duel finishes` (two, one per case).

Each becomes `queryByText("Waiting for your rival")`. **Nothing else in either test changes, and no
assertion is weakened**: both still say the waiting state is gone, now by the words the player
reads rather than by a role that no longer exists.

> **The trap, measured this run so you do not have to find it.** The live table *does* render the
> string `Waiting for your rival…` — `ActionBar.tsx:228`'s waiting line. It does **not** collide:
> the ellipsis makes it a different string and Testing Library's default `exact: true` compares the
> whole normalised text, so `queryByText("Waiting for your rival")` returns `null` after the
> `Snapshot`. Verified by rendering both states. Do not reach for a regex or a container scope.

**3 — the string-set test moves from `ADR-0073` §3's rule to `ADR-0110` §6's.**
`adds exactly two strings to the waiting screen and no third` becomes
`states the six strings the host-alone table renders with no clipboard, and no seventh`.
Its fixture does not change (`ROOM_JOINED`, no clipboard installed). Its assertion becomes:
collect every text node under the section, trim, drop the empties, **sort**, and compare to

```
Waiting for your rival | ABCDEFGH | Invite link | You |
Back to the lobby | The room stays open. That link still works for your rival, and it brings you back.
```

Sorted, so the assertion says nothing about layout — that is the card's and the human's
(`ADR-0024` §3) and must not be pinned by a test. Measured on `develop` 2026-09-02: the five nodes
this state renders today are exactly the list above minus `You`, and adding `You` is the only
change `ADR-0110` §§2–6 make to it.

Two facts that decide how you write it:

- **`ADR-0110` §6 lists *the link itself*, and it will not appear.** The link lives in
  `<input value>`, a property, not a text node — `textContent` of an `<input>` is empty. The
  existing `shows an invite link carrying that code` already pins the value, and keeps doing so.
- **Every expected string is a plain literal in the markup**, so each is one text node. If your
  walk splits one, the markup interpolated where it should not have — fix the markup, not the
  assertion.

## Out of scope

- **The other seventy-seven tests in `Lobby.test.tsx`.** They pass unchanged, including the nine
  invite tests, `offers none of the words ADR-0073 refuses` and `prints no duration, countdown or
  expiry` — the last two scope with `.closest("section")` and keep working because `WaitingTable`'s
  root is the tree's only `<section>` (`TASK-130204` pins that).
- **`ADR-0110` §3's refusals as a sweep**, and the three clipboard variants of §6's enumeration.
  Both are `TASK-130206`'s `null-view.test.tsx`, deliberately the ticket after this one so the
  contract lands as its own reviewable diff.
- **The one-render `waiting` window on a resume into a `PLAYING` room.**
  [`ADR-0114`](../../docs/adr/ADR-0114-one-predicate-answers-every-ask-and-a-mailed-screen-waits.md)
  §6 measured it off merged source — `RoomJoined` and `Snapshot` arrive as two frames, so the
  standing reads `waiting` for one render — and says in as many words that it **cannot be closed
  from the client** and that observing it is §7's drive, which is `STORY-1311`'s. This ticket
  changes what that one render *draws* and nothing about its width. Do not add a guard, a delay or
  a spinner for it; if the drive later observes a token spent that way, `ADR-0114` §6 makes that a
  new `DEC`.
- **`PresenceNotice`, `serverAction`, the action bar.** None of them has an occasion before the
  first `Snapshot`; the branch renders `WaitingTable` alone.
- **The wire.** `RoomJoined(code, seat)` already carries everything (`ADR-0110` *Constrains*). No
  protocol type, no `PROTOCOL_VERSION`, no stored data, and nothing in `poker-engine`.

## Tests

No new test file. The three edited tests keep their fixtures; the file's count is the gate.

| Test | Proves after this ticket |
| --- | --- |
| `leaves the waiting panel when the first Snapshot arrives` | `Waiting for your rival` is on screen before the `Snapshot` and gone after it, with `Pot 30` in its place — the arrival is silent and needs no navigation |
| `shows the result when the duel finishes` | a finished duel replaces the host-alone table in both the victory and defeat cases |
| `states the six strings the host-alone table renders with no clipboard, and no seventh` | `ADR-0110` §6's enumeration is exhaustive for this state — an invented sentence, a stray label or a duplicated seat line all fail it |

## Acceptance criteria

- [ ] `Lobby.test.tsx` reports `Tests  80 passed (80)` — the same eighty, three of them edited
- [ ] `WaitingTable.test.tsx` still reports `Tests  6 passed (6)`
- [ ] `heading", { name: "Waiting for your rival" }` appears zero times in `Lobby.test.tsx`, and
      `queryByText("Waiting for your rival")` appears exactly three times
- [ ] `adds exactly two strings to the waiting screen and no third` appears zero times, and
      `and no seventh` exactly once
- [ ] `Lobby.tsx` contains `WaitingTable`, does not contain `WaitingForRival`, and still contains
      exactly one `max-w-[560px]`
- [ ] `cd web-client && npm run check` and `npm run build` both exit 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
