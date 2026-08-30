---
schema: 2
id: TASK-120901
title: The front door and the waiting frame wear the client's tokens, and the room-code field can be seen
type: task
status: done
parent: STORY-1209
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, uat, bug, high]
depends_on: []
verify:
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the front door's controls are dressed, not bare"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the room-code field is drawn, not invisible"
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose src/lobby/Lobby.test.tsx 2>&1 | grep -qF "the waiting frame's controls are dressed, not bare"
  - cd web-client && NO_COLOR=1 npm run --silent test -- src/lobby/Lobby.test.tsx
  - cd web-client && NO_COLOR=1 npm run --silent check
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Every control the lobby offers — on the front door and in the waiting frame — carries the client's
token vocabulary, so a player looking at the first screen of the product can see a button where a
button is and a field where a field is.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`, against
`design/screens/create-duel.html`. Read from the running client, not from the report.

**Every primary control on the lobby is an unclassed native element.**

```
[{"tag":"BUTTON","cls":null,"txt":"Create a duel room","bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"},
 {"tag":"INPUT", "cls":null,"txt":"",                  "bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"},
 {"tag":"BUTTON","cls":null,"txt":"Join the duel",     "bg":"rgba(0, 0, 0, 0)","pad":"0px","radius":"0px","border":"0px"}, …]
```

**The sharpest consequence is a control a player cannot see.** The room-code field:

```
{"rect":{"x":101.5,"y":79.5,"w":167,"h":22.5},"bg":"rgba(0, 0, 0, 0)","border":"0px solid rgb(236, 233, 227)",
 "outline":"rgb(236, 233, 227) none 3px","placeholder":null,"bodyBg":"rgb(19, 18, 17)"}
```

A 167 × 22.5 px region with a transparent background, a zero-width border, no outline and no
placeholder, on a `rgb(19,18,17)` body. There is nothing on the screen to see. The driver finds it
because it has a rect; a player does not, which is what `ADR-0092` §3's reachability check is for.

**The waiting frame is the same.** `<p>8F8CRDTT</p>`, `<input id="invite-link" readonly …>` and
`<button type="button">Copy the link</button>` all ship without a `class`, where the card draws
`.code`, `.linkline` and `.btn.fill`.

**The client is not short of vocabulary; this file never got any.** `Lobby.tsx` holds eleven
`className` attributes in total, and the ones it has arrived with later work — the `ADR-0073` way-out
link is dressed `rounded-medium border border-hairline px-5 py-4 leading-tight font-medium text-text`
while the controls the card draws are bare. The same token classes are already in use on
`AccountOffer.tsx` and `DuelResult.tsx`, so this is transcription that was skipped, not a vocabulary
that has to be invented.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |
| `web-client/src/lobby/Lobby.test.tsx` | modify |

## Scope

- **Dress the front door's three controls** — *Create a duel room*, the room-code input and *Join
  the duel* — with the client's existing token classes, the same vocabulary `AccountOffer.tsx` and
  `DuelResult.tsx` already use. No new token, no new value, no arbitrary length literal
  (`ADR-0091` §4's fourth client guard refuses `-[380px]`; `-[var(--pd-…)]` passes).
- **The room-code input must be visibly a field**: a border drawn from `border-hairline` and a
  surface from `bg-surface`, so it reads as an input against `rgb(19,18,17)`.
- **Dress the waiting frame's controls** — the room code, the invite-link box and *Copy the link* —
  to the same standard.
- Structure and behaviour stay exactly as they are: same elements, same order, same handlers, same
  strings.

## Out of scope

- **Every string on this screen.** *Copy the link* vs the card's *Copy link* is a copy question with
  two merged sources pointing different ways, resolved in `TASK-120911` in the card's direction.
  *Back to the lobby* and *The room stays open…* are `ADR-0073`'s own words and are correct as
  shipped. **Change no literal in this ticket.**
- **The card's front-door structure** — its *Challenge someone* lede, and its *Create a duel* /
  *I have a code* control pair with a separate code screen behind the second. That is a structural
  change that needs a product decision; it is `TASK-120907`.
- **The seat plates** the card draws in the waiting frame (*Open seat*, *You / host / 10,000*).
  Composition, not dressing, and it needs the same decision `TASK-120907` names.
- **`web-client/src/protocol/reconnecting.ts`.** The lobby's other defect — a press before the
  socket opens is silently lost — is `TASK-120906`, and it is not this file's.

## Tests

`Lobby.test.tsx`

| Test | Proves |
| --- | --- |
| `the front door's controls are dressed, not bare` | *Create a duel room*, the room-code input **and** *Join the duel* each carry a non-empty `class` drawn from the client's token vocabulary — all three, so one dressed control cannot carry the assertion |
| `the room-code field is drawn, not invisible` | the input's class list carries both a border token and a surface token, which is the pair that makes it visible against the body |
| `the waiting frame's controls are dressed, not bare` | in the waiting state, the room code, the invite-link input and *Copy the link* each carry a non-empty `class` |

**Why the `verify:` block runs the file twice.** `--reporter=verbose` prints a test's name whether
it passed or failed, and the exit code of a piped run is `grep`'s, not the suite's. The three greps
prove the named tests **exist**; the fourth command runs the same file with **no pipe**, so its exit
code is the suite's and proves they **pass**. Neither half is sufficient alone, and dropping either
one makes this gate unable to fail.

## Acceptance criteria

- [ ] `Lobby.test.tsx > the front door's controls are dressed, not bare` passes
- [ ] `Lobby.test.tsx > the room-code field is drawn, not invisible` passes
- [ ] `Lobby.test.tsx > the waiting frame's controls are dressed, not bare` passes
- [ ] Reverting `Lobby.tsx` alone reddens all three; the reviewer runs that rather than reading it,
      because an assertion that passes against the pre-fix component gates nothing
- [ ] **By hand, on a live stack** — the half no jsdom test reaches: open `/`, and see a button and
      a bordered field on the front door; create a room, and see the code, the link box and *Copy
      the link* as drawn controls rather than as running text
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
