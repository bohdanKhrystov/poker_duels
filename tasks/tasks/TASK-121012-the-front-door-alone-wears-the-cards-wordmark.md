---
schema: 2
id: TASK-121012
title: The front door alone wears the card's wordmark, and it says the product's name
type: task
status: backlog
parent: STORY-1210
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, medium]
depends_on: [TASK-121010, TASK-121011]
verify:
  - cd web-client && grep -qF 'aria-label="Poker Duels"' src/lobby/Lobby.tsx
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the front door wears the card's wordmark"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "no state but the front door wears the wordmark"
  - cd web-client && ! grep -qF 'expect(headings.length).toBe(0)' src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The first screen's pre-create branch renders the coin-and-two-tone lockup
`design/screens/create-duel.html`'s front-door frame draws, no other screen or state renders it,
and a screen reader is given `Poker Duels` rather than `PokerDuels` —
[`ADR-0098`](../../docs/adr/ADR-0098-the-wordmark-belongs-to-the-front-door-alone.md) §§1–2.

## Why this ticket exists, and why it is one of two

The second half of what `TASK-121004`'s struck third scope item became; `TASK-121011` holds the
first half and the full account of the routing. In short: that ticket's coder shipped the fill and
the code well (PR #1234) and refused to guess the wordmark, because the markup it quoted sat in
`App.tsx` above every screen rather than in `Lobby.tsx`. `DEC-099` asked where the wordmark
belongs; `ADR-0098` answered *the front door alone*, reading it off the merged cards — across
eleven card files exactly one frame draws `.mark`, and it is this one.

`ADR-0098` §4 names four files. `ADR-0068` caps a ticket at three and sells the exemption only for
a **merged gate that fails on the smaller commit**, so the planner probed for one on 2026-08-31
instead of assuming it (`ADR-0069`): `TASK-121011`'s half alone runs `npm run check` green —
117 files, 956 tests, exit 0 — so no gate forbids the split and it is two tickets
(`ADR-0068` §4, the same reading that split `TASK-121109` out of `TASK-121101`).

**The order is forced, and this is the second half.** With the shell's `<h1>` still in place, this
ticket's lockup would be the *second* heading on the front door and `screen.getByRole("heading")`
throws on *"found multiple elements"* inside three merged `App.test.tsx` tests — so
`TASK-121011` lands first. The `depends_on` onto `TASK-121010` is the ordinary one this story
has used throughout: it stops two coders holding `Lobby.tsx` at once.

**The plumbing PR #1234's report worried about is not needed.** The lockup lives *in* the branch
that is the front door — the final `return` of `Lobby()`, the pre-create branch `ADR-0060` §5
calls *"the branch with the create button"*. Nothing has to be told which screen is showing; every
other state returns earlier and never renders it. `ADR-0098` §4 says so explicitly, to spare this
ticket the detour.

## The one thing `ADR-0098` left here, and how it is settled

`ADR-0098` closes with: *"The lockup's accessible name is now a thing the ticket must get right.
The card's markup concatenates to `PokerDuels` for a screen reader."* It routes that to this
ticket by name — *"ordinary implementation under merged guidance … decided in the follow-up ticket
under review, not in a register"* — so it is settled here, and no `DEC` is raised.

**The settlement: the element carrying the lockup is an `<h1>`, and it carries
`aria-label="Poker Duels"`.**

- **`<h1>`, because `ADR-0098` already spent that consequence.** Its *Consequences* reads *"the
  client keeps a level-one heading **only where the front door provides one**"* — the front door
  providing one is the accepted state of affairs, and a non-heading lockup would quietly make the
  client's level-one heading count zero, which is not what was accepted.
- **`aria-label`, because the name is otherwise whatever the reader's algorithm happens to do.**
  The card writes the two words as adjacent inline elements with no text node between them —
  `<span class="coin"></span>Poker<span class="duels">Duels</span>` — and the visual space is a
  flex `gap`, not a character. So the DOM's own concatenation is `PokerDuels`. **This is measured,
  not assumed.** On 2026-08-31 the planner rendered both forms under this repo's own runner and
  found that `dom-accessibility-api` — what `@testing-library`'s `getByRole` computes names with —
  joins element children with a space, so:

  | Markup | `getByRole("heading", { name: "Poker Duels" })` | `.textContent` |
  | --- | --- | --- |
  | no `aria-label` | **found** | `"PokerDuels"` |
  | `aria-label="Poker Duels"` | found | `"PokerDuels"` |

  A name query therefore **cannot gate the label** — it is satisfied either way, which would leave
  a green suite over a product that says its own name wrong. So the requirement is pinned on the
  attribute, in the test and in `verify:`, and the explicit label also removes the disagreement
  between whatever implementations do with that whitespace.
- **Not a visible space instead.** Adding one drifts the rendered lockup from the card, which
  `ADR-0033`'s anatomy gate refuses; the label fixes the name and moves no pixel.
- **The coin stays silent.** `CoinMark` (`web-client/src/result/CoinMark.tsx`) already carries
  `aria-hidden="true"` and its KDoc says why. Whether it is reused or the coin is redrawn at the
  lockup's own em sizes is the coder's call (`ADR-0098` §4) — either way it must be
  `aria-hidden`, and `aria-label` on the `<h1>` overrides its subtree regardless.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- **Render the lockup in the pre-create branch only** — the final `return` of `Lobby()`, the one
  holding the *Create a duel room* button — as the card's front-door frame draws it: the coin, a
  bold *Poker*, a muted *Duels*, in two separate text elements.
- **Carry it on an `<h1>` with `aria-label="Poker Duels"`**, per the settlement above.
- **Size it from the card's own value.** The card sets the mark at `1.875rem`
  (`create-duel.html:115`); `--pd-fs-display` is `1.875rem`, so `text-display` is that value and no
  new token or value is minted.
- **No new token, no new value, no arbitrary length literal.** `ADR-0091` §4's fourth client guard
  refuses a raw length inside a Tailwind arbitrary value (`-[380px]` fails, `-[var(--pd-…)]`
  passes). The card's `gap: 0.42em` has no client utility: use the nearest existing spacing
  utility and leave the exact figure to the human's eye at the pane (`ADR-0024` §3) — do **not**
  write `gap-[0.42em]`.

## Out of scope

- **Every other screen and state.** The waiting room's top is the room code, sized by the card's
  lede to be read across a room, and `ADR-0098` forecloses the wordmark there **by name**; the
  table, the result, duels, leaderboard, account, sign-in, verify and reset render no product-name
  chrome either. This is asserted, not merely refrained from — see the second test.
- **The shell.** `App.tsx` and `App.test.tsx` are `TASK-121011`'s and are already done when this
  starts. Do not open them.
- **The heading hierarchy.** Every other screen still tops out at its own `h2`.
  `ADR-0098`'s *Consequences* names that wart and **accepts** it — *"an accessibility wart this ADR
  accepts and names rather than hides"* — so **promote no screen's heading**. Those files are in
  neither ticket's budget and none is opened.
- **The card's front-door structure** — the *Challenge someone* lede, the *Create a duel* /
  *I have a code* pair. Composition, not the wordmark; it needs the product decision
  `TASK-120907` is blocked on.
- **Every string on this screen.** Change no literal but the wordmark's own two words.

## Tests

`Lobby.test.tsx`

| Test | Proves |
| --- | --- |
| `the front door wears the card's wordmark` | on the bare front door, the level-1 heading's `getAttribute("aria-label")` is exactly `"Poker Duels"` and its `textContent` is `"PokerDuels"`; it contains an `aria-hidden` element (the coin); `Poker` and `Duels` are two separate `SPAN`s; `Duels` carries `text-text-muted` and the heading carries `text-display` |
| `no state but the front door wears the wordmark` | with `ROOM_JOINED` applied — the waiting room — `queryByRole("heading", { level: 1 })` is `null`; and after `cleanup()`, so is it for `renderFinishedDuel(0)`, the result screen |

**Modified — one merged test.** `renders the lobby with no headings from the name surface` asserts
`expect(headings.length).toBe(0)` on a front door that today has none. The front door now has
exactly one, its own wordmark, so that number becomes **1** and the assertion gains the identity
of the one heading it found: `headings[0]` is the level-1 heading and its `aria-label` is
`Poker Duels`. **The test's intent is preserved, not weakened** — a heading contributed by
`NameSurface` still makes the count 2 and still reddens it, which is the whole reason the test
exists. Its `TestPlayer` assertions and its `findByLabelText("your profile")` are untouched.

**Both polarities, and two states in the negative.** The first test says where the lockup is; the
second says where it is not, on two different screens — one screen could be satisfied by a lockup
that happens to render only on the front door for an unrelated reason, and the waiting room is the
one `ADR-0098` forecloses by name.

**Why the `aria-label` is asserted as an attribute.** Because a name query cannot see it: the
planner measured that `getByRole("heading", { name: "Poker Duels" })` finds the unlabelled markup
too (table above). An assertion written that way would be green over a lockup that says
`PokerDuels`.

**Why `verify:` runs the file three times.** `--reporter=verbose` prints a test's name whether it
passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The two greps
prove the named tests **exist**; the unpiped run's exit code is the suite's and proves they
**pass**, and re-runs every merged test in this file — including the ones `TASK-121004` and
`TASK-121010` add ahead of it — so this change cannot undo them. **No count is pinned here on
purpose**: the file holds 75 tests on `develop` at `6c4965dd` and both tickets ahead of this one
add to it, so any number written today would be wrong by the time this is worked. The unpiped
run's exit code is the assertion.

**The planner ran this whole change on 2026-08-31** against `develop` at `6c4965dd`:
`npm run check` gave 117 files, 959 tests, exit 0. Deleting the `aria-label` from `Lobby.tsx`
alone reddened two tests — `the front door wears the card's wordmark` and
`renders the lobby with no headings from the name surface` — so the gate bites.

## Acceptance criteria

- [ ] `Lobby.test.tsx > the front door wears the card's wordmark` passes
- [ ] `Lobby.test.tsx > no state but the front door wears the wordmark` passes
- [ ] `renders the lobby with no headings from the name surface` passes with its count at 1 and
      the identity of that one heading asserted; its `TestPlayer` and `your profile` assertions
      are unchanged
- [ ] `Lobby.tsx` contains the literal `aria-label="Poker Duels"` on the element carrying the
      lockup, and contains no `gap-[` arbitrary length
- [ ] `src/lobby/Lobby.test.tsx` exits 0 with every merged test in it still passing
- [ ] Deleting the `aria-label` from `Lobby.tsx` alone reddens
      `the front door wears the card's wordmark`
- [ ] **By hand, on a live stack** — open `/` and read the coin, a bold *Poker* and a muted
      *Duels*; create a room and see no wordmark above the code; finish a duel and see none on the
      result
- [ ] Every command in `verify:` exits 0

**What each gate was measured at on 2026-08-31, before the work**, against `develop` at
`6c4965dd`. A gate that already passes today does not gate this change and is here as a
regression check; the four that fail are the ones that gate it.

| `verify:` command | Exit today | Gates |
| --- | --- | --- |
| `grep -qF 'aria-label="Poker Duels"' src/lobby/Lobby.tsx` | **1** | the settled accessible name, in the one place a name query provably cannot see it |
| `… --reporter=verbose src/lobby/Lobby.test.tsx │ grep -qF "the front door wears the card's wordmark"` | **1** | the positive test **exists** |
| `… --reporter=verbose src/lobby/Lobby.test.tsx │ grep -qF "no state but the front door wears the wordmark"` | **1** | the negative test **exists** |
| `! grep -qF 'expect(headings.length).toBe(0)' src/lobby/Lobby.test.tsx` | **1** | the merged zero-heading expectation was rewritten, not left beside a copy |
| `npm run --silent test -- src/lobby/Lobby.test.tsx` | 0 | the file's exit code is the suite's, so both named tests **pass** and the merged tests still do |
| `npm run --silent check` | 0 | typecheck, lint, format and all 117 files |
| `python3 .github/scripts/lint_tickets.py` | 0 | the registers stay consistent |

`NO_COLOR=1` is set because ANSI escapes break a fixed-string grep.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
