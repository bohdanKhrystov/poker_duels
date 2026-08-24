---
schema: 2
id: TASK-031404
title: The waiting screen offers none of the words ADR-0073 refuses, and names no deadline
type: task
status: ready
parent: STORY-0314
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, ui, rooms]
depends_on: [TASK-031403]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'offers none of the words ADR-0073 refuses'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'prints no duration, countdown or expiry'
  - cd web-client && grep -qF 'Back to the lobby' src/lobby/Lobby.tsx
  - cd web-client && grep -qF 'The room stays open. That link still works for your rival, and it brings you back.' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Cancel' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Close the room' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Delete the room' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'End the room' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Leave the room' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Leave the duel' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Give up' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Forfeit' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Cash out' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Exit table' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Stand up' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qiF 'Sit out' src/lobby/Lobby.tsx
  - cd web-client && ! grep -qE 'setTimeout|setInterval|Date[.]now|new Date' src/lobby/Lobby.tsx
  - cd web-client && npm run check
---

## Goal

Every word `ADR-0073` §5 refuses is absent from the waiting screen as text and as a control, and
nothing on it names a duration.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `web-client/src/lobby/Lobby.tsx` | read — the strings `WaitingForRival` actually renders |

## Scope

- Two tests. **No source file changes** — a failure here is a report naming the offending string,
  not an edit.
- The refusal is about **this screen**. `Back` on its own is a legitimate control on the lobby's
  roomless branch (the history and ladder swaps) and stays there: query the waiting section, never
  the whole document, and assert nothing about the other branch.

## Out of scope

- The two strings that *are* there and the order they sit in — `TASK-031403`.
- `design/screens/create-duel.html`, whose frame is `EPIC-06`'s (`ADR-0073` §6).

## Tests

`Lobby.test.tsx`, inside `describe("the lobby")`. Both start from `store.apply(ROOM_JOINED)` and
`renderLobby(store)`, with no `withClipboard`.

| Test | Proves |
| --- | --- |
| `offers none of the words ADR-0073 refuses` | none of the eighteen refused strings is on the waiting screen as text, as a button or as a link |
| `prints no duration, countdown or expiry` | the waiting section's text names no unit of time and no clock face |

**`offers none of the words ADR-0073 refuses`.** Assert the positive control first, so an
all-null sweep cannot be a render that never happened:

```ts
expect(screen.getByText("Waiting for your rival")).toBeDefined();
expect(screen.getByRole("link", { name: "Back to the lobby" })).toBeDefined();
```

Then loop over **all eighteen** of `ADR-0073` §5's strings, written out in the test as a `const`
array — the name promises the whole table, so the whole table is enumerated:

```
"Cancel", "Cancel the room", "Cancel the duel", "Close the room", "Delete the room",
"End the room", "Leave", "Leave the room", "Leave the duel", "Give up", "Abandon",
"Withdraw", "Forfeit", "Back", "Cash out", "Exit table", "Stand up", "Sit out"
```

For each one, all three of `screen.queryByText(word)`, `screen.queryByRole("button", { name: word })`
and `screen.queryByRole("link", { name: word })` are `null`. Testing Library matches the **whole**
text and the **whole** accessible name exactly, so `Back` does not match `Back to the lobby` and
`Leave` does not match anything — that is why the sweep can include both.

**`prints no duration, countdown or expiry`.** Take the section's own text and assert the positive
control before the negatives:

```ts
const waiting = screen.getByText("Waiting for your rival").closest("section");
const text = waiting?.textContent ?? "";

expect(text).toContain("Back to the lobby");
expect(text).toContain(
  "The room stays open. That link still works for your rival, and it brings you back.",
);
expect(text).not.toMatch(
  /\b(second|seconds|minute|minutes|hour|hours|day|days|expire|expires|expired|expiry|countdown|remaining|timer|timeout|until)\b/i,
);
expect(text).not.toMatch(/\d{1,2}:\d{2}/);
```

**Do not add `expect(text).not.toMatch(/\d/)`.** A room code is Crockford base32
(`RoomCode.ALPHABET`) and carries digits, so a no-digits assertion would pass only because the
fixture's code happens to be `ABCDEFGH` — it would pin the fixture, not the screen, and would fail
the day someone changes the constant.

The `! grep` commands in `verify:` are the other half of this ticket, and they catch what no
rendering test can: a refused word or a timer that reaches `Lobby.tsx` in a branch, a comment or a
prop the waiting fixture never renders.

## Acceptance criteria

- [ ] `the lobby > offers none of the words ADR-0073 refuses` passes, and its array holds all
      eighteen strings listed above
- [ ] `the lobby > prints no duration, countdown or expiry` passes, with both `toContain` positive
      controls present
- [ ] Every `! grep` command in `verify:` exits 0
- [ ] The PR's diff touches `web-client/src/lobby/Lobby.test.tsx` and nothing else
      (`git diff --name-only` names one file); both `grep -qF` commands in `verify:` exit 0
- [ ] No existing test in `Lobby.test.tsx` is edited or deleted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
