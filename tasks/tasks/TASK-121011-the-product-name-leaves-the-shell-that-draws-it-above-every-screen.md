---
schema: 2
id: TASK-121011
title: The product's name leaves the shell that draws it above every screen
type: task
status: done
parent: STORY-1210
module: web-client
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: [TASK-121004]
verify:
  - cd web-client && ! grep -qF 'Poker Duels' src/App.tsx
  - cd web-client && ! grep -qF 'renders the application heading' src/App.test.tsx
  - cd web-client && ! grep -qF 'gives the heading a token-derived class' src/App.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/App.test.tsx 2>&1 | grep -qF "adds no heading of its own above the screens it composes"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/App.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`App` renders no heading of its own, so the only heading above any screen is that screen's —
[`ADR-0098`](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) §2's
*"the unconditional `<h1>Poker Duels</h1>` leaves `App.tsx`, and no product-name chrome replaces
it anywhere"*.

## Why this ticket exists, and why it is one of two

**Where it comes from.** `TASK-121004` carried three scope items. Its coder shipped two of them —
the front door's accent fill and the room code's type treatment, PR #1234 — and refused to guess
the third, because the `<h1 className="text-title">Poker Duels</h1>` the ticket quoted does not live
in `Lobby.tsx` where the ticket looked: it lives in `App.tsx`, above every screen the product has.
Dressing it in place would make the front door's wordmark every screen's wordmark; moving it would
take the only on-page product naming ten surfaces have. That is a product question, not a dressing
change, so it was routed as `DEC-099` (`CLAUDE.md` rule 5) rather than decided in a ticket.
`ADR-0098` answers it: the wordmark renders on the front door alone. This ticket and
`TASK-121012` are what that struck third item becomes — the same shape as
`TASK-121005`/`TASK-121010` and `TASK-120910`/`TASK-120913` before them.

**Why two tickets and not one.** `ADR-0098` §4 names a four-file set. Four exceeds `ADR-0068`'s
cap of three, and `atomic:` is bought only by naming a **merged gate that fails on the smaller
commit**. The planner probed for one on 2026-08-31 rather than assuming it (`ADR-0069`), by
applying this ticket's half alone to `develop` at `6c4965dd` and running the client job's whole
command — `npm run check`, which is typecheck, lint, format and the full suite:

```
117 test files, 956 tests, exit 0
```

Green. No merged gate refuses this half on its own, so under `ADR-0068` §4 it is two tickets —
the same reading, and the same probe, that split `TASK-121109` out of `TASK-121101` the day
before. The other half is `TASK-121012`, and it is what puts the lockup on the front door.

**What that costs, said plainly.** Between this ticket merging and `TASK-121012` merging, the
product names itself on no screen at all — only in the browser tab's `<title>`. That is a
transient this chain owns and closes, not a state `ADR-0098` blesses: §1 puts the wordmark on the
front door, and `TASK-121012` is where it lands. The order is forced the other way round —
applying the lockup first leaves two headings on the front door and `screen.getByRole("heading")`
throws on *"found multiple elements"* in three merged `App.test.tsx` tests, which the probe also
measured.

## Files

| File | Action |
| --- | --- |
| `web-client/src/App.tsx` | modify |
| `web-client/src/App.test.tsx` | modify |

## Scope

- **Delete the heading.** `App.tsx`'s one line, `<h1 className="text-title">Poker Duels</h1>`.
  The `<main className="min-h-screen bg-bg p-6 font-ui text-text">` shell and every class on it
  stay exactly as they are; `<Lobby />` stays its only child.
- **Retire the two merged tests whose subject just left**, named below, and replace them with the
  one test named below. They are not kept, not renamed, and not mechanically adjusted: nothing
  will render a heading whose text is `Poker Duels` with class `text-title` after this lands, and
  a test edited until it goes green against the new markup would pin whatever the coder happened
  to write (`ADR-0098` §4).
- **Remove the two repeated heading assertions inside
  `leaves the lobby exactly as it was for a player who never opens the record`**, and correct that
  test's leading comment, which says *"the three merged tests still find the heading, its class and
  the Create a duel room button"*. Its other two assertions — the create button, and the history
  screen's absence — stay untouched and are what the test now rests on.
- **Assert only what stays true after `TASK-121012`.** The new test names two screens that will
  never carry the lockup. It must not assert anything about the *front door's* headings: the front
  door gains one in the next ticket, and a test that pinned its emptiness here would have to be
  rewritten there — which is how a two-file ticket quietly becomes a three-file one.

## Out of scope

- **The lockup itself.** The coin, the bold *Poker* and the muted *Duels* land on the front door
  in `TASK-121012`, in `Lobby.tsx`. Do not open `Lobby.tsx` or `Lobby.test.tsx`; the front door's
  wordmark is asserted there, not here.
- **The heading hierarchy this leaves behind.** With the shell's `<h1>` gone, every screen but the
  reset screen tops out at its own `h2` — `Your duels`, `Leaderboard`, `Account`, `Sign in`,
  `Verify`, the result's verdict — and the duel table has no heading at all. `ADR-0098`'s
  *Consequences* names that exact wart and **accepts** it: *"an accessibility wart this ADR
  accepts and names rather than hides"*. **Promote no screen's heading**, in this ticket or the
  next; those files are not in either budget and none of them is opened.
- **`index.html`'s `<title>Poker Duels</title>`.** Untouched. It is the naming `ADR-0098` counts
  on for the invited player and the one this decision deliberately leaves standing.
- **`renders the lobby beneath the heading`.** Left byte-for-byte unchanged. Its body asserts only
  that *Create a duel room* is on screen, so it passes as written both now and after
  `TASK-121012`, at which point its name is true again.

## Tests

`App.test.tsx`

**Retired — delete both.** Their subject is the `<h1>` this ticket removes.

| Retired test | What it asserted, and where that goes |
| --- | --- |
| `renders the application heading` | `getByRole("heading").textContent === "Poker Duels"`. The front door's heading is asserted in `TASK-121012`'s `Lobby.test.tsx`, against the lockup |
| `gives the heading a token-derived class` | `text-title` on that heading. The lockup's own type class is asserted in `TASK-121012`, where the lockup lives |

**Added — one test, replacing both.**

| Test | Proves |
| --- | --- |
| `adds no heading of its own above the screens it composes` | On the record and on the account screen, `getAllByRole("heading")` has length **1** and that heading's `textContent` is the screen's own — `"Your duels"`, then `ACCOUNT_HEADING`. A shell that kept product-name chrome makes both counts 2 |

**Two screens, and a count rather than an absence.** One screen could be satisfied by a shell that
renders no heading only on that route; and `queryByRole("heading", { name: "Poker Duels" })` being
null would pass just as happily for a shell that swapped in some other chrome. The exact count
plus the identity of the one heading rejects both.

**The mechanism, so it need not be discovered.** `renderApp()` already exists in this file. From
the front door, `fireEvent.click(screen.getByRole("button", { name: "Your duels" }))` opens the
record — `await screen.findByRole("heading", { name: "Your duels" })` before counting, since the
record settles asynchronously. `fireEvent.click(screen.getByRole("button", { name: "Back" }))`
returns to the front door, and `fireEvent.click(screen.getByRole("button", { name: ACCOUNT_HEADING }))`
opens the account screen; `ACCOUNT_HEADING` is already imported at the top of the file. The
planner ran this exact sequence on 2026-08-31: `36 passed (36)`, exit 0.

**Nothing else in this file changes.** No other assertion moves, none is weakened, and no test
outside the three named above is edited. The file holds 37 tests today and 36 after this lands —
two retired, one added.

## Acceptance criteria

- [ ] `App.tsx` contains no `<h1>` and no occurrence of the string `Poker Duels`
- [ ] `App.test.tsx > adds no heading of its own above the screens it composes` passes
- [ ] `App.test.tsx` contains neither `renders the application heading` nor
      `gives the heading a token-derived class`
- [ ] `leaves the lobby exactly as it was for a player who never opens the record` passes, with its
      create-button and absent-history-screen assertions unchanged and its two heading assertions
      removed
- [ ] `src/App.test.tsx` reports **36 passed** and exits 0
- [ ] Restoring the `<h1>` to `App.tsx` alone reddens
      `adds no heading of its own above the screens it composes`
- [ ] Every command in `verify:` exits 0

**What each gate was measured at on 2026-08-31, before the work**, against `develop` at
`6c4965dd`. A gate that already passes today does not gate this change and is here as a
regression check; the four that fail are the ones that gate it.

| `verify:` command | Exit today | Gates |
| --- | --- | --- |
| `! grep -qF 'Poker Duels' src/App.tsx` | **1** | the heading actually leaves — cannot be faked by writing a test |
| `! grep -qF 'renders the application heading' src/App.test.tsx` | **1** | the first merged test is retired, not renamed beside a copy |
| `! grep -qF 'gives the heading a token-derived class' src/App.test.tsx` | **1** | the second merged test is retired |
| `… --reporter=verbose src/App.test.tsx │ grep -qF "adds no heading of its own above the screens it composes"` | **1** | the replacement test **exists**. The exit code of a piped run is `grep`'s, not the suite's, which is why the next command re-runs the file unpiped |
| `npm run --silent test -- src/App.test.tsx` | 0 | the file's exit code is the suite's, so the named test **passes** and the 34 untouched tests still do |
| `npm run --silent check` | 0 | typecheck, lint, format and all 117 files — nothing else in the client read that heading |
| `python3 .github/scripts/lint_tickets.py` | 0 | the registers stay consistent |

`NO_COLOR=1` is set because ANSI escapes break a fixed-string grep.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
