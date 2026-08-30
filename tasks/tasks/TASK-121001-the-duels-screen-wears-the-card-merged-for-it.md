---
schema: 2
id: TASK-121001
title: The `duels` screen wears the card merged for it, and its search field can be seen
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
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "the filter and search controls are dressed, not bare"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "the opponent-search field is drawn, not invisible"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/history/HistoryScreen.test.tsx 2>&1 | grep -qF "a history row states its outcome in its own element"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/history/HistoryScreen.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The `duels` screen carries the vocabulary `design/screens/duels.html` draws for it, so a player can
see the field they type an opponent's name into and can read a history row as outcome, opponent and
date rather than as one run-on sentence.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`, against
`design/screens/duels.html` — **merged the day before, by `TASK-120902`, and the newest merged
source for this screen.** Read from the running client, not from the report.

**Every control on the screen is an unclassed native element.**

```
FIELDSET  "OutcomeAllWonLostDrew"  getAttribute("class") → null   bg rgba(0,0,0,0)  pad 0px  border 0px
LABEL×4   "All" "Won" "Lost" "Drew" getAttribute("class") → null
INPUT×4   (radios)                  getAttribute("class") → null
LABEL     "Opponent name"           getAttribute("class") → null
INPUT     (the search field)        getAttribute("class") → null
BUTTON    "Search"                  getAttribute("class") → null
```

`HistoryScreen.tsx:153–201` renders the `<fieldset>`, its four `<label>`/`<input>` pairs, the search
`<label>`/`<input>` and the *Search* `<button>` with **no `className` prop at all**. This is an
empty class list, not a class present and overridden — classes that *are* present on the same page
(`border-t border-hairline py-3 first:border-t-0` on each `<li>`) compute correctly.

**The sharpest consequence is a control a player cannot see.** The opponent-search field:

```
{"cls":null,"rect":{"x":326,"y":149,"w":167,"h":23},"bg":"rgba(0, 0, 0, 0)",
 "border":"0px solid rgb(236, 233, 227)","outline":"rgb(236, 233, 227) none 3px",
 "placeholder":null,"bodyBg":"rgb(19, 18, 17)"}
```

A 167 × 23 px region with a transparent background, a zero-width border, no outline and no
placeholder, on a `rgb(19,18,17)` body. There is nothing on the screen to see. The card draws that
field explicitly — `design/screens/duels.html:60`:

```
.search input[type="text"] { background: var(--pd-surface); border: 1px solid var(--pd-hairline); … }
```

This is the same measurement round 1 took on the room-code field (`167 × 22.5`, same transparency,
same zero border, same missing placeholder) and graded `high` in `TASK-120901`.

**And a row is one flat string where the card draws three parts.** Shipped:

```
<li class="border-t border-hairline py-3 first:border-t-0">
  <p class="text-small">Lost −1 3 hands vs No name 30 серп. 2026 р., 18:46</p>
</li>
```

Card, `design/screens/duels.html:116–117`:

```
<li class="row"><span class="outcome-word lost">Lost</span> −1 14 hands vs
  <span class="opp">ImKate</span> <span class="when">Aug 29, 2026, 9:14 PM</span></li>
```

**The client is not short of vocabulary.** `bg-surface`, `border-hairline`, `bg-accent-fill`,
`text-on-accent`, `text-win`, `text-loss` and `text-text-muted` are all live utilities in
`web-client/src/styles/app.css`, already consumed by `DuelResult.tsx`, `AccountOffer.tsx` and — since
round 1 — `Lobby.tsx`. This is transcription that was skipped, not a vocabulary to invent.

## Files

| File | Action |
| --- | --- |
| `web-client/src/history/HistoryScreen.tsx` | modify |
| `web-client/src/history/HistoryScreen.test.tsx` | modify |

## Scope

- **Dress the filter fieldset, its four radio labels, the opponent-search field and *Search*** with
  the client's existing token utilities, the vocabulary `Lobby.tsx` and `DuelResult.tsx` already use.
  No new token, no new value, no arbitrary length literal (`ADR-0091` §4's fourth client guard
  refuses `-[380px]`; `-[var(--pd-…)]` passes).
- **The opponent-search field must be visibly a field**: a border drawn from `border-hairline` and a
  surface from `bg-surface`, so it reads as an input against `rgb(19,18,17)`. That pair is the
  minimum — a border alone can sit flush with the body colour.
- **Split the row into the card's parts**: the outcome word in its own element, the opponent in its
  own, the date in its own. The outcome word carries the card's win/loss colour where there is one.
- Structure and behaviour otherwise stay exactly as they are: same elements, same order, same
  handlers, same strings.

## Out of scope

- **Every string on this screen.** The row's words are `outcomeWord`, `coinDeltaText`,
  `nameOrNone` and `finishedAtText`, each owned by its own module. **Change no literal.**
- **The date's locale.** `finishedAtText` renders *"in the reader's locale"* by design, and
  `ADR-0061` §Costs accepts that behaviour by name. Two rounds have now ruled it not a defect
  (`STORY-1205`, `STORY-1210`). Do not touch it, and do not force a locale.
- **`Back`.** It is rendered by `Lobby.tsx`'s swap, not by this screen (`ADR-0060` §4), and **no
  card draws it** — it contradicts nothing and is deliberately not filed (`STORY-1210` §*What was
  not filed*).
- **The leaderboard and account screens.** Same cause, different files, their own tickets:
  `TASK-121002` and `TASK-121003`.
- **Paging, filtering and search behaviour.** This ticket changes how the screen looks, never what
  it fetches.

## Tests

`HistoryScreen.test.tsx`

| Test | Proves |
| --- | --- |
| `the filter and search controls are dressed, not bare` | the `<fieldset>`, each of the four radio labels, the search input **and** *Search* each carry a non-empty `class` — asserted one control at a time, because one dressed control standing in for six is exactly the failure `TASK-120901` shipped |
| `the opponent-search field is drawn, not invisible` | the search input's class list contains **both** `border-hairline` and `bg-surface` — named tokens, not merely non-empty, so the gate cannot be satisfied by any class at all |
| `a history row states its outcome in its own element` | a rendered row contains an element whose text content is exactly the outcome word, and separate elements for the opponent and the date — so the row is three parts, not one string |

**Why the `verify:` block runs the file twice.** `--reporter=verbose` prints a test's name whether it
passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The three greps
prove the named tests **exist**; the fourth command runs the same file with **no pipe**, so its exit
code is the suite's and proves they **pass**. Neither half is sufficient alone, and dropping either
makes this gate unable to fail. `NO_COLOR=1` is set because ANSI escapes break a fixed-string grep.

## Acceptance criteria

- [ ] `HistoryScreen.test.tsx > the filter and search controls are dressed, not bare` passes
- [ ] `HistoryScreen.test.tsx > the opponent-search field is drawn, not invisible` passes
- [ ] `HistoryScreen.test.tsx > a history row states its outcome in its own element` passes
- [ ] Reverting `HistoryScreen.tsx` alone reddens all three; the reviewer runs that rather than
      reading it, because an assertion that passes against the pre-fix component gates nothing
- [ ] **By hand, on a live stack** — the half no jsdom test reaches: open `/`, press *Your duels*,
      and see a bordered field beside *Opponent name*, a button where *Search* is, and a row whose
      outcome, opponent and date are visibly three things
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
