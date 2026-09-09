---
schema: 2
id: TASK-141509
title: The dismissal outlives a walk through the lobby, and the agreement takes the screen with no panel over it
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, rematch, notice, test]
depends_on: [TASK-141508]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.screens.test.tsx 2>&1 | grep -qE '^ *Tests +4 passed \(4\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +16 passed \(16\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +113 passed \(113\)$'
  - git diff --quiet develop -- web-client/src/App.tsx
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/result/RematchNotice.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Two claims that only a mounted `App` can prove: **the dismissal survives a screen change and a trip
through `/`** — `ADR-0138` §8's own *"the assertion that discriminates the mount point"* — and **the
agreement takes the screen with no panel over it**.

This ticket writes no source. Everything it asserts is already implemented; what is missing is the
evidence that the mount point is the one `ADR-0138` §1 chose rather than one that merely looks like
it.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchNotice.screens.test.tsx` | modify |

Read [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§1, 3 and 8 and its Alternatives 1,
[`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md) §§6 and 7,
[`ADR-0112`](../../docs/adr/ADR-0112-only-a-running-duel-refuses-another-screen.md) §4,
`web-client/src/lobby/Lobby.tsx`'s layout-effect restore, and
`web-client/src/routing/use-screen.ts`. **No file outside the table above is changed** — four
`verify:` lines diff `App.tsx`, `Lobby.tsx`, `RematchNotice.tsx` and `RematchControl.tsx` against
`develop`, because a test ticket that repairs the thing it is testing has proved nothing.

## Scope

- **Two tests appended to the file `TASK-141508` created**, taking it from **2** to **4**. Its
  fixture builder, its `vi.mock("./main")` factory and its provider stack are reused, not rewritten.
- **Moving the address mid-test needs one of two forms, and a bare assignment is neither.**
  Measured at `899d81f7`: assigning `window.location.hash` inside `act()` leaves a `useScreen`
  consumer showing the **old** screen, because jsdom queues `hashchange` and `act`'s synchronous
  flush does not run it. Two forms were probed and both work:

  ```tsx
  window.location.hash = "#/account";
  await waitFor(() => expect(screen.getByText(ACCOUNT_HEADING)).toBeDefined());
  ```

  or, synchronously,

  ```tsx
  act(() => {
    window.location.hash = "#/account";
    fireEvent(window, new HashChangeEvent("hashchange"));
  });
  ```

  **Use the first.** The `await` on the destination screen's own heading is not ceremony — it is the
  positive control that the address actually moved, and without it *"the panel is still hidden"*
  passes on a test where nothing navigated at all. That is the single most likely way this ticket
  goes green while proving nothing.
- **Nothing is added to `RematchNotice.tsx`.** If a test here fails, the finding is the failure;
  repairing the component is a different ticket.

## Out of scope

- **The prohibitions** — `TASK-141510` extends the same file again.
- **`Lobby.tsx`'s restore.** It is merged, it is `ADR-0114` §3's, and it is observed here rather
  than touched.

## Tests

| Test | Proves |
| --- | --- |
| `the dismissal survives a screen change and a trip through the lobby` | render at `#/duels` with the rival's offer standing; press `NOT_NOW`; await `ACCOUNT_HEADING` at `#/account` and assert `queryByText(RIVAL_OFFERS)` is null; await the result screen at `/` and assert `getAllByText(RIVAL_OFFERS)` has length **1** with no `[role="status"]` ancestor — the control, not the panel; await `ACCOUNT_HEADING` at `#/account` again and assert `queryByText(RIVAL_OFFERS)` is **still** null |
| `the agreement takes the screen with no panel over it` | offer standing at `#/account`; apply the agreeing `Snapshot` in its own `act()`; then `getByText("You")` finds the duel table, `queryByRole("status")` finds no panel, and `window.location.hash` is `""`. All three literals are the merged ones: `App.test.tsx:967`'s *"shows the duel to a player reading the account screen when a frame seats them"* asserts the same table node and the same empty hash for the same restore, and `hashForScreen("first")` is `"/"`, which `history.replaceState` writes as an empty fragment |

## What would still pass if this were built wrong

**The trip through `/` is the whole test.** `ADR-0138` §8 says so in as many words: a panel mounted
inside `Lobby`'s cascade passes the first half — dismiss on `#/duels`, still hidden on `#/account` —
and fails the second, because walking back to `/` unmounts every branch and takes the dismissal with
it. A version of this test that stopped after `#/account` would grade the rejected design as
correct. The `/` leg must be a real render of the room's own screen, which is why it asserts the
control's line is there rather than asserting nothing.

**The three `await`s are positive controls, not waits.** Each one names the destination screen's own
heading, so a navigation that silently did not happen fails on the heading rather than passing on
the absence of a panel that was never going to be there.

**`the agreement takes the screen…` asserts three things and needs all three.** The table proves the
`Snapshot` landed; `queryByRole("status")` proves no panel is over it; the address proves
`ADR-0114` §3's layout restore ran in the same commit. Any two of the three pass against a version
where the panel flashed over the table for one frame, because none of the two alone says *in the
same commit*.

This ticket is `review: deep` for one reason: the assertion that discriminates the mount point is a
sequence of four navigations, and a reviewer has to check the **order**, not just the presence.

## Acceptance criteria

- [ ] `npx vitest run src/result/RematchNotice.screens.test.tsx` reports **4 passed (4)**, the two
      new ones being the two named above
- [ ] The dismissal test navigates `#/duels` → `#/account` → `/` → `#/account`, in that order, and
      awaits a heading or the result screen's own line at each of the last three
- [ ] `src/result/RematchNotice.test.tsx` reports **16 passed (16)** and `src/lobby/Lobby.test.tsx`
      **113 passed (113)**
- [ ] `git diff --quiet develop` is clean for `App.tsx`, `Lobby.tsx`, `RematchNotice.tsx` and
      `RematchControl.tsx` — this ticket writes tests and nothing else
- [ ] **Shown red, then reverted:** deleting the `/` leg from the dismissal test and re-running
      leaves it green, which is the point being made — record that, restore it, then move
      `const [dismissed, setDismissed] = useState(false)` and the panel's markup into a child
      component that `Lobby` would remount, or more cheaply add `key={screen}` to `<RematchNotice />`
      in `App.tsx`, and confirm the **full** test fails on the leg after `/`. Revert both
- [ ] `npm run check` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
