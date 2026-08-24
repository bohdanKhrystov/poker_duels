---
schema: 2
id: TASK-031403
title: Two strings are the whole addition, and nothing stands between the press and the lobby
type: task
status: backlog
parent: STORY-0314
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 1
labels: [client, ui, rooms]
depends_on: [TASK-031402]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test 2>&1 | grep -qE 'Tests +[0-9]+ passed \([0-9]+\)'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'adds exactly two strings to the waiting screen and no third'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'puts no confirmation between the press and the lobby'
  - cd web-client && grep -qF 'Back to the lobby' src/lobby/Lobby.tsx
  - cd web-client && grep -qF 'The room stays open. That link still works for your rival, and it brings you back.' src/lobby/Lobby.tsx
  - cd web-client && npm run check
---

## Goal

The waiting screen's whole text is pinned to five strings in order, so a sixth fails; and one press
reaches the browser's navigation with nothing in between.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.test.tsx` | modify — two tests added, none changed |
| `web-client/src/lobby/Lobby.tsx` | read — `WaitingForRival`'s five elements, and that `CopyLink` returns `null` with no clipboard |

## Scope

- Two tests. **No source file changes at all** — if either test fails, the ticket is a report, not
  an edit: say which string is wrong and stop.
- Neither test may call `withClipboard`. jsdom defines no `navigator.clipboard`, so `CopyLink`
  returns `null` and the section holds exactly five elements with text. That is the point of the
  fixture; a clipboard would add `Copy the link` and make the enumeration a different claim.

## Out of scope

- **The words `ADR-0073` §5 refuses by name, and the ban on a duration** — `TASK-031404`. This
  ticket pins what *is* there; that one pins what may not be.
- Changing `Lobby.tsx`. It is a `read` row.

## Tests

`Lobby.test.tsx`, inside `describe("the lobby")`.

| Test | Proves |
| --- | --- |
| `adds exactly two strings to the waiting screen and no third` | the waiting section's text is exactly the five strings below, in that order |
| `puts no confirmation between the press and the lobby` | one press calls `forgetRoom` once, is not cancelled, and puts nothing on screen |

**`adds exactly two strings to the waiting screen and no third`** — after
`store.apply(ROOM_JOINED)` and `renderLobby(store)`:

```ts
const waiting = screen.getByText("Waiting for your rival").closest("section");
expect(waiting).not.toBeNull();
const texts = Array.from(
  waiting?.querySelectorAll("h2, p, a, button, label") ?? [],
).map((element) => element.textContent?.trim());

expect(texts).toEqual([
  "Waiting for your rival",
  "ABCDEFGH",
  "Invite link",
  "Back to the lobby",
  "The room stays open. That link still works for your rival, and it brings you back.",
]);
```

`toEqual` on the whole array, never `toContain` on the two new ones: *"these two strings are the
whole of the addition"* (`ADR-0073` §3) is a claim about the **set**, and an assertion that only
names the strings it wants cannot see a sixth. The first three entries are the positive control —
they are what the screen carried before this story, so a run that lost the invite box fails here
rather than passing a narrowed check.

**`puts no confirmation between the press and the lobby`** — from the same fixture, take
`const back = screen.getByRole("link", { name: "Back to the lobby" })`, then
`const clickReturn = fireEvent.click(back)`, and assert all five:

- `expect(forgetRoom).toHaveBeenCalledOnce()` — the press landed, and once (the positive control for
  everything below it)
- `expect(clickReturn).toBe(true)` — nothing called `preventDefault`, so the browser's navigation is
  still the navigation (`ADR-0072` §5)
- `expect(screen.queryByRole("dialog")).toBeNull()`
- `expect(screen.queryByRole("alertdialog")).toBeNull()`
- `expect(screen.queryByRole("button", { name: /sure|confirm|really|yes/i })).toBeNull()`

Four negative assertions under one positive control: the click demonstrably ran a handler, so an
empty screen after it is a fact about the screen rather than about a press that never happened.

jsdom does not navigate, so the waiting panel is still mounted after the click and `state.roomCode`
is unchanged — `forgetRoom` clears storage, not the store. Assert nothing about a screen change
here; `TASK-031405` owns what the next socket does.

## Acceptance criteria

- [ ] `the lobby > adds exactly two strings to the waiting screen and no third` passes, and its
      assertion is a single `toEqual` over the five-string array
- [ ] `the lobby > puts no confirmation between the press and the lobby` passes with all five
      assertions above present
- [ ] The PR's diff touches `web-client/src/lobby/Lobby.test.tsx` and nothing else
      (`git diff --name-only` names one file); both `grep -qF` commands in `verify:` exit 0
- [ ] No existing test in `Lobby.test.tsx` is edited or deleted
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
