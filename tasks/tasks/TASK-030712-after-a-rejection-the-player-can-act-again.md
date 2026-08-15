---
schema: 2
id: TASK-030712
title: After a rejection the player can act again
type: task
status: blocked
parent: STORY-0307
module: web-client
estimate: S
tier: sonnet
review: deep
files_touched: 3
labels: [client, duel, store, blocked]
depends_on: [TASK-030711]
verify:
  - cd web-client && npm ci
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejected action leaves the decision point open'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a second action can be sent after a rejection'
  - cd web-client && NO_COLOR=1 npm run --silent test -- --reporter=verbose 2>&1 | grep -qF 'a rejection stops being shown when the next turn opens'
  - cd web-client && npm run check
---

## Goal

`STORY-0307`'s fifth acceptance criterion: a `Rejected` shows the server's numbers **and leaves the
bar usable**, so the player can act again on the same decision point — and the rejection stops being
shown once the server opens the next one.

## Blocked on `DEC-037`

**Nothing in this ticket may be implemented before the ADR that answers `DEC-037` is merged.** The
question is technical and the `architect` agent's:

> After a `Rejected`, is the decision point still open on the client — and when does a rejection
> stop being shown?

The facts that make it a real decision rather than a detail:

- `DuelAction.act` returns **only** the `Rejected` frame. The server sends no fresh `YourTurn` after
  a rejection, so nothing re-opens the turn from the outside.
- `duel-state.ts` clears `pendingTurn` on `Rejected`, pinned by `TASK-030404`'s
  `a rejected action clears the pending turn`. With both of those true, a rejected action ends the
  player's hand: the bar goes off, no frame can re-open it, and a duel with both players connected
  stalls.
- `duel-state.ts` never clears `rejection`, so the line `TASK-030709` renders would still be on
  screen forty hands later.

Three shapes answer it, and they are not equivalent: the reducer keeps `pendingTurn` through a
`Rejected` and clears `rejection` on the next `YourTurn` or `Snapshot`; or the bar keeps its own
copy of the turn and restores it, which puts "it is still your turn" inside a component where no
test of the store can see it; or the **server** re-prompts after a rejection, which is a server and
possibly protocol change that `EPIC-03` explicitly forbids itself and which would need its own
epic's ticket. Guessing here would settle the client's reading of `ADR-0002` in a component diff.

## Files

The answer decides which pair moves; `files_touched` is 3 either way, and the planner re-scopes this
ticket in one pass when the ADR merges.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify — *if* the answer is the reducer's |
| `web-client/src/store/duel-state.test.ts` | modify — **this ticket owns `a rejected action clears the pending turn`**, which states today's behaviour and cannot survive an answer that keeps the turn |
| `web-client/src/table/ActionBar.test.tsx` | modify — the bar-side assertion that a second action can be sent |

## Scope

- Whatever `ADR-00NN` (answering `DEC-037`) says, and nothing else.
- Whichever merged test states the behaviour the ADR reverses is rewritten **in this ticket**, with
  the reason named in the diff — not left for a later ticket to discover.

## Out of scope

- Any change to `poker-server` or to the protocol. If the ADR's answer is a server re-prompt, this
  ticket is dropped and the work is filed against the epic the ADR names.
- Retrying an action for the player. The bar becomes usable again; it never re-sends by itself.

## Tests

Named now because they are observable whichever shape the ADR picks. The file each lands in follows
the answer.

| Test | Proves |
| --- | --- |
| `a rejected action leaves the decision point open` | after `YourTurn` then `Rejected`, the client still holds the turn the server opened, with the same `handNumber` and `actionSequence` |
| `a second action can be sent after a rejection` | the bar is live after a `Rejected`, and a click sends one more `Act` carrying that same identity |
| `a rejection stops being shown when the next turn opens` | the sentence `TASK-030709` renders is gone once the next `YourTurn` or `Snapshot` arrives |

## Acceptance criteria

- [ ] The ADR answering `DEC-037` is **merged**, and this ticket names it
- [ ] `a rejected action leaves the decision point open` passes
- [ ] `a second action can be sent after a rejection` passes
- [ ] `a rejection stops being shown when the next turn opens` passes
- [ ] Any merged test whose behaviour the ADR reverses was rewritten here, and the PR says which and
      why
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
