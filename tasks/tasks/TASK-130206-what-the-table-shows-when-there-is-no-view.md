---
schema: 2
id: TASK-130206
title: What the table shows when there is no view, written down as a gate
type: task
status: backlog
parent: STORY-1302
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 1
labels: [client, table, guard]
depends_on: [TASK-130205]
verify:
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/table/null-view.test.tsx 2>&1 | grep -qE '^ *Tests +4 passed \(4\)$'
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npx vitest run src/lobby/Lobby.test.tsx 2>&1 | grep -qE '^ *Tests +80 passed \(80\)$'
  - awk 'index($0, "../lobby/Lobby") { n++ } END { exit (n != 1) }' web-client/src/table/null-view.test.tsx
  - sh -c 'grep -q "queryAllByRole" web-client/src/table/null-view.test.tsx && ! grep -q "data-testid" web-client/src/table/null-view.test.tsx'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`web-client/src/table/null-view.test.tsx` is the standing answer to the question
[`ADR-0110`](../../docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md)'s
*Consequences* hands to the rest of `EPIC-13`: *"The table acquires a state in which it is not a
duel. Every future table surface — the turn clock, the chips, the act-just-made line, all of them
this same epic — must now say what it shows when `view === null`, or be absent from that state on
purpose."* After this ticket the answer is a file that fails, rather than a sentence each later
story has to rediscover.

## Why this is its own ticket, and why it is `deep`

`ADR-0110` §3 is not a styling rule — it is `ADR-0002` applied: *"a starting stack read from
configuration would be the client's guess wearing the server's voice."* The failure it forbids is
a client asserting a game fact, which is the one class of client defect this project treats as
correctness. So the refusal gets its own reviewable diff, and the review that reads it is the
one that asks whether each probe could pass while the thing it names is on screen.

**It is the epic's tax, paid once.** A turn clock drawn at the empty seat prints digits and fails
test 1. A stack of chips prints a figure and fails test 1. A dealer button fails test 2. An action
bar fails test 2. A last-act line that says *Raise to 400* fails test 1. Every one of those is a
story already on `BOARD.md`, and each of them will either move a surface out of this state on
purpose or amend this file with an argument.

## Files

| File | Action |
| --- | --- |
| `web-client/src/table/null-view.test.tsx` | create |
| `web-client/src/table/no-derivation.test.tsx` | read |
| `web-client/src/lobby/Lobby.test.tsx` | read |
| `docs/adr/ADR-0110-creating-a-duel-seats-the-host-at-the-table.md` | read |

## Scope

- **Render the screen, not the component.** `render(<DuelProvider store={store} send={() => {}}
  forgetRoom={() => {}}><Lobby /></DuelProvider>)` with the store at `RoomJoined` — the shape
  `presence-copy.test.tsx` already uses, which needs no `vi.mock("../main")` and no other provider.
  The contract is about what a player sees in this state, and only the screen can say whether
  something is rendered *beside* the table.
- **Two helpers, adapted from `no-derivation.test.tsx`** (read it; do not import from it —
  that file's helpers are shaped around a `PlayerView` that does not exist here):
  - `textNodes(root)` — a `TreeWalker` over `SHOW_TEXT`, each node trimmed, empties dropped.
  - `spoken(root)` — every `aria-label` and every `title` attribute value in the tree. A figure
    the client worked out for itself reaches the player from either of those just as surely as
    from print; `no-derivation.test.tsx` records that it shipped `186 passed` against a text-only
    scan before this was added.
- **A file docstring that states the contract in one paragraph**, addressed to the next story:
  before the first `Snapshot` the client holds a room code and a seat number and nothing else, so
  the table states no stack, blind, card, pot, dealer button or action bar; a surface this epic
  adds either renders nothing here or says here what it renders.
- **`ADR-0100` §5 holds**: no `data-testid`, no test-only prop, no exported setter. Everything
  below is reachable by pressing what a player presses. A gate refuses `data-testid` in the file.

## Out of scope

- **`ADR-0110` §6's no-clipboard enumeration.** `Lobby.test.tsx`'s `states the six strings the
  host-alone table renders with no clipboard, and no seventh` owns that state (`TASK-130205`);
  this file owns the three clipboard states. Between them the four variants are covered once each,
  and neither file restates the other's.
- **`WaitingTable`'s positive rendering** — the seats, the way back, the promise. `TASK-130204`'s
  `WaitingTable.test.tsx`.
- **The live table's own numbers.** Test 3 asserts each probe finds *something* there, never how
  much: pinning 7 images and 6 figures would redden this guard every time a later `EPIC-13` story
  changes the live table, for a reason that has nothing to do with the contract.
- **Widening `no-derivation.test.tsx`.** It guards the table that *has* a view and stays exactly as
  it is; `TASK-130102` narrowed it earlier in this epic and `TASK-130103` pinned its count at 7.

## Tests

`null-view.test.tsx` — four tests.

| Test | Proves |
| --- | --- |
| `states no figure the server never stated` | **two inputs.** With `code="7Q4M9K2T"`, every text node and every `aria-label`/`title` that contains a digit is exactly `7Q4M9K2T` — one entry, no other. With `code="ABCDEFGH"`, that same filter is **empty**. The second input is what tells a code echoed from the store apart from a literal `7Q4M9K2T` compiled into a component |
| `deals no card, draws no button, offers no bar and names no pot` | `queryAllByRole("img")` has length 0; `queryByLabelText("the button")`, `queryByLabelText("your move")` and `queryByText(/^Pot/)` are each `null` |
| `finds all four of those on the live table` | after applying a `Snapshot` to the same store: `queryAllByRole("img").length` is greater than 0, the other three are non-null, and the digit filter of test 1 is non-empty. **This is what stops test 2 being four assertions about probes that never work** — measured on `develop` 2026-09-02, the live table carries 7 images, the button, the bar, one `Pot` node and six digit-bearing text nodes |
| `the copy control's feedback adds one named string and no other` | with a clipboard installed, the sorted text nodes are the six of the no-clipboard state plus `Copy the link`; after a resolved `writeText`, plus `Link copied.`; after a rejected one, plus `Copy it from the box above.` — and nothing else in any of the three |

Notes the fourth test needs:

- The six baseline strings are `Waiting for your rival`, the code, `Invite link`, `You`,
  `Back to the lobby` and `The room stays open. That link still works for your rival, and it
  brings you back.` **The link itself never appears**: it lives in `<input value>`, a property, and
  an `<input>` has no text content.
- Compare **sorted**, so nothing here pins layout — that is the card's and the human's
  (`ADR-0024` §3).
- `Reflect.deleteProperty(navigator, "clipboard")` in `afterEach`, or a clipboard installed by one
  test leaks into the next and test 2's `Copy the link` assumptions silently change.

## Acceptance criteria

- [ ] `null-view.test.tsx` reports `Tests  4 passed (4)`
- [ ] `Lobby.test.tsx` still reports `Tests  80 passed (80)`
- [ ] `null-view.test.tsx` imports `Lobby` from `../lobby/Lobby` on exactly one line
- [ ] `null-view.test.tsx` contains `queryAllByRole` and contains no `data-testid`
- [ ] `cd web-client && npm run check` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
