---
schema: 2
id: TASK-130204
title: The host-alone table is a component, drawn as the card draws it
type: task
status: backlog
parent: STORY-1302
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [client, table]
depends_on: [TASK-130203]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/WaitingTable.test.tsx 2>&1 | grep -qE '^ *Tests +6 passed \(6\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/InvitePanel.test.tsx 2>&1 | grep -qE '^ *Tests +5 passed \(5\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - sh -c 'grep -c "<section" web-client/src/table/WaitingTable.tsx | grep -q "^1$"'
  - sh -c 'grep -q "InvitePanel" web-client/src/table/WaitingTable.tsx && ! grep -qE "PotStrip|BoardCards|ActionBar|SeatPlate|formatChips" web-client/src/table/WaitingTable.tsx'
  - sh -c 'grep -c "max-w-\[560px\]" web-client/src/lobby/Lobby.tsx | grep -q "^1$"'
  - sh -c 'grep -q "WaitingForRival" web-client/src/lobby/Lobby.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`WaitingTable` exists and renders exactly what
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md) §§2–5 put on
the host-alone table, in the column `design/screens/duel-table.html`'s `Host alone` frames draw.
**It is not wired to a screen yet** — `TASK-130205` does that — so this ticket's whole diff is one
new component and its test.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/WaitingTable.tsx` | create |
| `web-client/src/table/WaitingTable.test.tsx` | create |
| `design/screens/duel-table.html` | read |
| `web-client/src/lobby/Lobby.tsx` | read |

## Scope

- **`WaitingTable(props: { code: string; onLeave: () => void })`.** Its root is a single
  `<section>` carrying the **same column class list `Lobby.tsx`'s `state.view !== null` branch
  carries, copied verbatim** — `[container-type:inline-size] mx-auto flex min-h-[100dvh]
  max-w-[560px] flex-col gap-[var(--wgap)] p-[var(--wgap)]` and its `--wgap` clamp. Copy it with a
  comment saying why the literal is repeated rather than shared: the two branches never render at
  the same time, and `ADR-0103` §5's rule is against a column **nested** inside a column, which
  this is not. A `verify:` gate keeps `Lobby.tsx` at exactly one `max-w-[560px]`.
- **The root is the only `<section>` in this tree**, and it carries **no `aria-label`**. Three
  tests in `Lobby.test.tsx` scope with `.closest("section")` and must keep finding the screen's own
  element; and an `aria-label` would be an eleventh string, which `ADR-0110` §6 forbids without a
  new ADR.
- **Four blocks, in the card's order:**
  1. the rival's seat — one plate, drawn as the card's dashed twin (`border-dashed`), carrying
     the single string `Waiting for your rival`: capital *W*, no full stop, **no status line, no
     stack, no button**;
  2. `<InvitePanel code={props.code} />`;
  3. the host's seat — the same plate, solid, carrying the single string `You` and **no stack**;
  4. `Back to the lobby` as `<a href="/" onClick={props.onLeave}>` with today's class list
     (`border-hairline` among them), and beside it, verbatim:
     `The room stays open. That link still works for your rival, and it brings you back.`
- **The seat plates are private to this file**, in the shape `DuelTable.tsx` already uses for its
  private `BetLine`. `SeatPlate` is not reusable here: it takes a `SeatView` and always prints
  `formatChips(seat.stack)`, and `ADR-0110` §3 forbids a stack. A gate refuses `SeatPlate` and
  `formatChips` in this file.
- **No new string of any kind**, in text, `aria-label`, `title` or visually-hidden markup.
  `ADR-0110` §6's ten strings are exhaustive, and a state that seems to need an eleventh is a stop
  and a new ADR.

## Out of scope

- **Wiring.** `Lobby.tsx` is `read` here and a gate asserts `WaitingForRival` is still in it.
  `TASK-130205` replaces the branch and deletes that function; doing it here would put four files
  in one ticket.
- **`Lobby.test.tsx`.** Unchanged, and gated at **80**.
- **The clipboard's three states.** `InvitePanel.test.tsx` already pins them at the component
  level and `TASK-130206` pins them at the screen level. Re-asserting them here would be a third
  copy of the same claim.
- **`ADR-0110` §3's refusals as a sweep.** *No stack, blind, card, pot, dealer button or action
  bar* is `TASK-130206`'s standing guard, run against the real branch. The two structural gates
  here (`SeatPlate`/`formatChips` absent) are cheap sanity, not that contract.
- **Presence, narration, the reveal step, `serverAction`.** None of them exists before the first
  `Snapshot`; this component takes no such prop.

## Tests

`WaitingTable.test.tsx` — six tests. Render with `code="7Q4M9K2T"` and a `vi.fn()` for `onLeave`,
and install a clipboard where a test needs one, deleting it in `afterEach` as `Lobby.test.tsx`
does.

| Test | Proves |
| --- | --- |
| `names the empty seat and the host's seat exactly once each` | `getAllByText("Waiting for your rival")` and `getAllByText("You")` both have length 1 — `ADR-0110` §2's *renders once* |
| `draws the invite whole` | the bare code, the `Invite link` box holding `http://localhost:3000/?room=7Q4M9K2T`, and `Copy the link` all render — §5's three parts |
| `keeps the way back a link to the lobby that forgets the room` | the `Back to the lobby` link's `href` is `/`, its classes include `border-hairline`, clicking it calls `onLeave` once and `fireEvent.click` returns `true` |
| `keeps saying that the room stays open` | the promise sentence renders, byte-identical |
| `renders exactly one section, and it is its own root` | `container.querySelectorAll("section")` has length 1 and `container.firstElementChild.tagName` is `SECTION` |
| `carries the card's column and never a second one` | the root's `className.split(" ")` contains `mx-auto`, `max-w-[560px]`, `min-h-[100dvh]` and `flex-col`, and it is the **only** element in the tree whose class list contains `max-w-[560px]` |

## Acceptance criteria

- [ ] `WaitingTable.test.tsx` reports `Tests  6 passed (6)`
- [ ] `InvitePanel.test.tsx` still reports `Tests  5 passed (5)` and `Lobby.test.tsx` still reports
      `Tests  80 passed (80)`
- [ ] `web-client/src/table/WaitingTable.tsx` contains exactly one `<section`, contains
      `InvitePanel`, and contains none of `PotStrip`, `BoardCards`, `ActionBar`, `SeatPlate`,
      `formatChips`
- [ ] `web-client/src/lobby/Lobby.tsx` still contains `WaitingForRival` and exactly one
      `max-w-[560px]`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
