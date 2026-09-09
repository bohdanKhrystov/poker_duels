---
schema: 2
id: TASK-141508
title: The app mounts the panel beside the lobby, and it follows onto every chosen screen
type: task
status: backlog
parent: STORY-1415
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, rematch, notice]
depends_on: [TASK-141507]
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.screens.test.tsx 2>&1 | grep -qE '^ *Tests +2 passed \(2\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchNotice.test.tsx 2>&1 | grep -qE '^ *Tests +16 passed \(16\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/App.test.tsx 2>&1 | grep -qE '^ *Tests +36 passed \(36\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +113 passed \(113\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/result/RematchControl.test.tsx 2>&1 | grep -qE '^ *Tests +12 passed \(12\)$'
  - awk 'index($0,"<RematchNotice />") { n++ } END { exit (n != 1) }' web-client/src/App.tsx
  - awk 'index($0,"<Lobby />") { n++ } END { exit (n != 1) }' web-client/src/App.tsx
  - awk 'index($0,"</main>") { n++ } END { exit (n != 1) }' web-client/src/App.tsx
  - sh -c 'test "$(grep -n "<Lobby />" web-client/src/App.tsx | cut -d: -f1)" -lt "$(grep -n "<RematchNotice />" web-client/src/App.tsx | cut -d: -f1)"'
  - sh -c 'test "$(grep -n "<RematchNotice />" web-client/src/App.tsx | cut -d: -f1)" -lt "$(grep -n "</main>" web-client/src/App.tsx | cut -d: -f1)"'
  - git diff --quiet develop -- web-client/src/lobby/Lobby.tsx
  - git diff --quiet develop -- web-client/src/result/RematchControl.tsx
  - git diff --quiet develop -- web-client/src/App.test.tsx
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`App` renders `<RematchNotice />` as the **last child of `<main>`, after `<Lobby />`**, and the panel
therefore stands on every one of the six chosen screens — with no carve-out, which is `ADR-0123`
§1's whole claim — while `/` keeps exactly one surface for the same fact.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.tsx` | modify |
| `web-client/src/result/RematchNotice.screens.test.tsx` | create |

Read [`ADR-0138`](../../docs/adr/ADR-0138-the-panel-mounts-beside-the-lobby-and-the-dismissal-lives-in-the-mount.md)
§§1, 2, 8 and 9, [`ADR-0123`](../../docs/adr/ADR-0123-a-standing-rematch-offer-follows-the-rival.md)
§§1 and 3, `web-client/src/App.test.tsx` (whose `vi.mock("./main")` factory and provider stack this
new file copies), and `web-client/src/routing/screen.ts`. **Nothing outside the table above is
changed**; three `verify:` lines diff `Lobby.tsx`, `RematchControl.tsx` and `App.test.tsx` against
`develop`.

## Scope

- **`App.tsx` gains one import and one element, and nothing else:**

  ```tsx
  <main className="min-h-screen bg-bg font-ui text-text">
    <Lobby />
    <RematchNotice />
  </main>
  ```

  `ADR-0138` §1: *"That is the whole of `App.tsx`'s change."* Three gates count `<Lobby />`,
  `<RematchNotice />` and `</main>` at **1** each, and two more assert the line order — `<Lobby />`
  before `<RematchNotice />` before `</main>`. Order is not cosmetic: it is the tab order
  (`ADR-0138` §6, and its named cost), and it is what puts the panel after everything on the screen
  beneath.
- **`Lobby.tsx` is not edited** — not its cascade, not its branch order, not either of its effects,
  not the block that computes `standing`, `ruling` and `shown`.
- **A new test file, not `App.test.tsx`.** `App.test.tsx` is 1261 lines and 36 tests and this story
  opens none of them; a gate diffs it against `develop`. The new file copies that file's
  `vi.mock("./main")` **factory** — mounting the real `HistoryProvider` throws, because Node's own
  `localStorage` shadows jsdom's and reads `undefined` inside `HistoryScreen`'s mount effect.
- **Every chosen branch's guard must be satisfied**, which means the provider stack is
  `AccountProvider` → `HistoryProvider` → `LadderProvider` → `DuelProvider`, exactly as `App.test.tsx`
  builds it and as `main.tsx` ships it. This is not incidental: with a provider missing,
  `ADR-0114` §2's fall-through fires, `shown` names a chosen screen while `Lobby` renders the room's
  own screen, and the panel stands beside `RematchControl` — the two-surface case `ADR-0138` §9
  fences. **No test here asserts anything about that path in either direction**; the fence is prose
  in `ADR-0138` §9 and stays there.

## Out of scope

- **`App.test.tsx`, `Lobby.test.tsx`, `RematchControl.test.tsx`.** None is opened; all three are
  gated at their measured counts.
- **The dismissal across screens, the agreement, and the prohibitions** — `TASK-141509` and
  `TASK-141510` extend this same file.
- **`ADR-0138` §9's fall-through repair.** It becomes owed on the day a chosen screen's guard can go
  false at runtime, and no such screen exists.

## Tests

`RematchNotice.screens.test.tsx` — **2** tests. The room fixture is `RoomJoined`, then
`DuelFinished`, then `RematchOffered` from the rival's seat, in that order.

**The address is set before `render()`, never after.** Measured at `f20d07ed`: assigning
`window.location.hash` inside `act()` does **not** settle a `useScreen` consumer — jsdom queues
`hashchange` and `act`'s synchronous flush does not run it, so the component still reports the old
screen. Setting the hash before the render sidesteps it entirely, and it is what
`App.test.tsx:391`'s *"opens the screen the address already names"* already does. (`TASK-141509`
needs a mid-test move and carries the two forms that do work.)

| Test | Proves |
| --- | --- |
| `follows onto every chosen screen` | a table of **six** `[hash, heading]` pairs — `#/duels`/`HISTORY_HEADING`, `#/leaderboard`/`LADDER_HEADING`, `#/account`/`ACCOUNT_HEADING`, `#/sign-in`/`SIGN_IN_HEADING`, `#/verify`/`VERIFY_HEADING`, `#/reset`/`RESET_HEADING` — each rendered and unmounted in turn. For every one: the heading is on screen **and** `RIVAL_OFFERS` is, and the element holding `RIVAL_OFFERS` has a `[role="status"]` ancestor |
| `one fact never has two live surfaces` | the same store rendered at `/`: `getAllByText(RIVAL_OFFERS)` has length **exactly 1**, and that one element has **no** `[role="status"]` ancestor — it is `RematchControl`'s line on the result screen, not the panel |

## What would still pass if this were built wrong

**The heading half of the enumeration is the positive control and it is not decoration.** Without
it, a test that only looks for `RIVAL_OFFERS` passes on every hash a component happens to render
for — including hashes where the chosen screen did not render at all — so it would prove *the panel
shows* rather than *the panel follows onto that screen*. One screen also cannot tell *follows every
screen* from *is on the account screen*, which is why `ADR-0123` §1's *no carve-out* is written here
as an enumeration of six rather than a sample of one.

**The `[role="status"]` ancestor is what distinguishes the two surfaces**, and it is the reason
`one fact never has two live surfaces` can be a count instead of a presence check.
`RematchControl`'s line sits in a plain `<div>`; the panel's root carries the role. A test that
merely asserted *the sentence is on screen at `/`* would pass with both surfaces up, which is the
exact defect `ADR-0123` §3 forbids.

`getAllByText(...).length === 1` rather than `getByText(...)`: `getByText` **throws** on multiple
matches, so it does catch two surfaces — but it reports *"found multiple elements"* rather than a
count, and it cannot distinguish two from three. The count is the assertion.

## Acceptance criteria

- [ ] `App.tsx` contains `<Lobby />` once, `<RematchNotice />` once and `</main>` once, in that line
      order
- [ ] `npx vitest run src/result/RematchNotice.screens.test.tsx` reports **2 passed (2)**, the two
      tests being the two named above, and the enumeration covers all six chosen screens
- [ ] `src/result/RematchNotice.test.tsx` reports **16 passed (16)**; `src/App.test.tsx` reports
      **36 passed (36)**; `src/lobby/Lobby.test.tsx` reports **113 passed (113)**;
      `src/result/RematchControl.test.tsx` reports **12 passed (12)** — the last three measured on
      `develop` at `f20d07ed` and none of them opened here
- [ ] `git diff --quiet develop` is clean for `Lobby.tsx`, `RematchControl.tsx` and `App.test.tsx`
- [ ] **Shown red, then reverted:** removing `<RematchNotice />` from `App.tsx` fails **exactly
      one** test — `follows onto every chosen screen` — and **no test in `App.test.tsx`**. Run it,
      record the failing name and the unchanged `36 passed (36)`, and revert. `one fact never has
      two live surfaces` stays green under that mutation on purpose: it is a test of the gate, not
      of the mount, and a criterion claiming otherwise would be wrong. If `App.test.tsx` also goes
      red, this ticket has changed something it was not supposed to
- [ ] `npm run check` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
