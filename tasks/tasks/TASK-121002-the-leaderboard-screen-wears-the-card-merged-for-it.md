---
schema: 2
id: TASK-121002
title: The `leaderboard` screen wears the card merged for it, and a row reads as rank, name and coins
type: task
status: ready
parent: STORY-1210
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, high]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "Show more is dressed, not bare"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "the self line is the highlighted box the card draws"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/ladder/LadderScreen.test.tsx 2>&1 | grep -qF "a ladder row states its rank, its name and its coins in three elements"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/ladder/LadderScreen.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The `leaderboard` screen carries the vocabulary `design/screens/leaderboard.html` draws for it, so
the reader's own standing is the highlighted thing on the screen and a row is three columns rather
than a string a player has to parse.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/leaderboard.html` — **merged the day before, by `TASK-120903`, and the newest merged
source for this screen.** Read from the running client, not from the report.

**A row is one flat string, and it is genuinely ambiguous.** Shipped, verbatim:

```
<li class="border-t border-hairline py-3 first:border-t-0"><p class="text-small">1 x 1</p></li>
```

That is rank 1, a player named `x`, on 1 duel coin. Nothing on the screen says which number is
which. Card, `design/screens/leaderboard.html:96`:

```
<li class="row"><span class="rank">1</span><span class="who">ImKate</span><span class="coins">14</span></li>
```

**The reader's own line is unhighlighted plain text.** Shipped: `<p>You are rank 1 this season, on 1
duel coin.</p>` — no class, no coin. Card, line 94:

```
<p class="self"><span class="coin" aria-hidden="true"></span><span>You are rank 42 this season, on −6 duel coins.</span></p>
```

with `.self { background: var(--pd-accent-subtle); border: 1px solid var(--pd-accent); … }`. This is
`UAT-Q1` — *is the main info properly highlighted?* — failing against a merged card: the one line
about the reader is the least prominent thing on the screen.

**And *Show more* is an unclassed native button:**

```
BUTTON "Show more"  getAttribute("class") → null  bg rgba(0,0,0,0)  pad 0px  radius 0px  border 0px
```

`LadderScreen.tsx:116` renders it with no `className` prop at all; the card draws it
`class="btn fill"`, and `.btn.fill { background: var(--pd-accent-fill); color: var(--pd-on-accent); }`.
This is an empty class list, not a class present and overridden — `<ul className="w-full">` and each
`<li>` on the same page compute correctly.

**The client is not short of vocabulary.** `bg-accent-subtle`, `border-accent`, `bg-accent-fill` and
`text-on-accent` are all live utilities in `web-client/src/styles/app.css`, mapped from
`--pd-accent-subtle`, `--pd-accent`, `--pd-accent-fill` and `--pd-on-accent`.

## Files

| File | Action |
| --- | --- |
| `web-client/src/ladder/LadderScreen.tsx` | modify |
| `web-client/src/ladder/LadderScreen.test.tsx` | modify |

## Scope

- **Split a row into the card's three parts** — rank, name, coins — each in its own element, so a
  reader can tell `1 x 1` apart without counting words. `ladder-text.ts`'s `rowLine` keeps its
  contract and its tests; this ticket does not have to delete it, and must not change what it
  returns.
- **Dress the self line as the card's `.self`**: `bg-accent-subtle` and `border-accent`, with the
  coin mark the card puts before it. `CoinMark` already exists (`web-client/src/result/CoinMark.tsx`)
  and is what `DuelResult` uses.
- **Dress *Show more* as the card's `.btn.fill`**: `bg-accent-fill` and `text-on-accent`, with the
  padding and radius the client's other primary buttons use. It must stay the **same node** across
  the loading transition — it is `hidden`, never unmounted, deliberately (see its comment), and a
  port that swaps it for a conditional render breaks a merged test for the wrong reason.
- No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).

## Out of scope

- **Every string on this screen.** `LADDER_HEADING`, `MORE`, `seasonName`, `rowLine`, `selfLine` and
  `NO_PLACE_THIS_SEASON` are `ladder-text.ts`'s. **Change no literal.**
- **The order tied rows sit in.** `ADR-0064` §4 settles it: *"arbitrary, invisible, and not a measure
  of play"*, and which deterministic key the query uses is the **architect's**, already part of
  `DEC-061`. Do not add a sort, a secondary column or a tie marker — §5 forbids the last of those by
  name.
- **Any tie marker.** `ADR-0064` §5: *"A tied row prints its rank exactly as an untied row does. No
  `=`, no `T1`… The repetition is what says it."*
- **`Back`.** Rendered by `Lobby.tsx`'s swap, not by this screen (`ADR-0060` §4), and **no card
  draws it** — deliberately not filed (`STORY-1210` §*What was not filed*).
- **The duels and account screens.** Same cause, different files: `TASK-121001`, `TASK-121003`.

## Tests

`LadderScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `Show more is dressed, not bare` | the *Show more* button's class list contains **both** `bg-accent-fill` and `text-on-accent` — named tokens, not merely non-empty, so the gate cannot be satisfied by any class at all |
| `the self line is the highlighted box the card draws` | the element carrying `selfLine`'s sentence has a class list containing **both** `bg-accent-subtle` and `border-accent`; a border alone can sit flush with the body colour |
| `a ladder row states its rank, its name and its coins in three elements` | for a row with rank `5`, name `P4` and coins `5`, three **distinct** elements each hold exactly one of `5`, `P4`, `5` — and the fixture deliberately repeats the value `5`, so a test that found one element containing both numbers would fail |

**Two inputs, not one.** The row assertion uses a fixture whose rank and coin count are the same
integer, because a row rendered as one string and a row rendered as three elements are
indistinguishable to a test that only checks the text is present.

**Why the `verify:` block runs the file twice.** `--reporter=verbose` prints a test's name whether it
passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The three greps
prove the named tests **exist**; the fourth command runs the same file with **no pipe**, so its exit
code is the suite's and proves they **pass**. `NO_COLOR=1` is set because ANSI escapes break a
fixed-string grep.

## Acceptance criteria

- [ ] `LadderScreen.test.tsx > Show more is dressed, not bare` passes
- [ ] `LadderScreen.test.tsx > the self line is the highlighted box the card draws` passes
- [ ] `LadderScreen.test.tsx > a ladder row states its rank, its name and its coins in three elements`
      passes
- [ ] Reverting `LadderScreen.tsx` alone reddens all three; the reviewer runs that rather than
      reading it
- [ ] Every merged `LadderScreen.test.tsx` test still passes — in particular the ones that press
      *Show more* twice across the loading transition
- [ ] **By hand, on a live stack** — press *Leaderboard* and see the reader's own line inside an
      accented box with a coin, a filled *Show more*, and rows whose rank, name and coins are
      visibly three columns
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
