---
schema: 2
id: TASK-121301
title: The runout arrives street by street on the screen, not only in the log
type: task
status: ready
parent: STORY-1213
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [qa, audit, bug, R1, manual-verify]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When a hand ends in an all-in and the engine runs the board out, a player can tell that the flop,
the turn and the river happened — `R1` (`ADR-0096` §2) is met at beat 5, as it already is at the
other seven.

## Register the decision first — no diff exists until it merges

**Do not write code before this is done.** `ADR-0096` §2 routes this ticket by name:

> `R1` requires that a runout be perceivable; it fixes no duration, no animation and no transition.
> How a beat is paced is settled by the ticket that repairs it — with a card where a still can hold
> it (`ADR-0091` §3), with the architect where it cannot.

**A still cannot hold it, so it is the architect's.** The frames need no drawing: a board with
three cards and a board with four are `design/screens/duel-table.html`'s existing anatomy at two
fills, and no new element appears on the screen at any point in a runout. What is undecided is
temporal and structural, and no card can carry either half:

- **Where the pacing lives.** The server sending one `Snapshot` per street during a runout, or the
  client holding the snapshot it has and revealing the board in steps. The first changes
  `poker-server` and possibly the frame contract; the second changes only `web-client` but makes
  the client hold back a fact the server has already sent, which is a rule this repository takes
  seriously (`CLAUDE.md`: *the server is authoritative*).
- **What a step costs.** How long a street is held, and whether a returning or reconnecting client
  replays the pacing or jumps to the end.

**First act: register `DEC-NNN` — the architect's — and route it, before any diff exists.**
State it as one question with the two halves above named as the choice, cite `ADR-0096` §2 as the
routing authority, add the row to `tasks/BOARD.md` §*Open decisions* and to this story, and come
back `blocked`. This ticket is then **re-cut** against the merged answer, the way `TASK-120907` was
re-cut against `ADR-0094` and `TASK-121101` against `ADR-0095`. That is the merged pattern for this
shape and it is not a failure of the ticket.

## The defect

Round 1 of `/qa-cycle audit smoke` answered `R1` **`not met` at beat 5** — an all-in call — and
`met` at the other seven beats. With `record` armed on both tabs, the acting player's next two
frames were:

- frame *N* — `THEIR TURN | 10,000 | committed 100 | Pot 0 | Blinds 50/100 · Hand 3 · Preflop`,
  **no community cards**;
- frame *N+1* — `Your rival wins 19,800 | Blinds 50/100 · Hand 3 · Hand complete`, **five community
  cards**.

Nothing came between them, on either browser. The board went from zero cards to five and the street
label from `Preflop` to `Hand complete` in one paint.

**It reproduces from source, without a browser, and the source reading is stronger than the frame
log because it shows no instrument could have caught an intermediate frame:**

1. **One snapshot per transition.** `poker-server/…/duel/Addressed.kt`'s `broadcast` emits, per
   seat, an `Events` frame of the new events and then — its own comment — *"Always emit Snapshot
   frame, which is the authoritative last word on state"*. `DuelTurn.kt`'s `framesFor` is the only
   caller. An all-in call is **one** transition: `poker-engine/…/game/StreetProgression.kt`'s
   `runOutBoard` deals flop, turn and river inside it, so all five cards and the award are in the
   one post-transition state.
2. **No path from a street event to the screen.** `web-client/src/table/DuelTable.tsx` renders
   `<BoardCards cards={view.board.cards} />` — the board is the latest `PlayerView` and nothing
   else. `grep -rn "StreetDealt" web-client/src/table web-client/src/store web-client/src/lobby`
   returns **nothing**: no component, no reducer branch, no selector reads the event.

So the engine emits each street as its own event *"so the log reads like the deal it was"*
(`ADR-0008`, which `R1` cites), and the two layers above it collapse the sequence into one paint.

## Files

**Provisional.** The `DEC`'s answer decides the module and may replace this table wholesale — a
server-paced answer moves it to `poker-server` and leaves `web-client` untouched. Re-cut the ticket
when the ADR merges rather than forcing the answer into the list below.

| File | Action |
| --- | --- |
| `web-client/src/store/duel-state.ts` | modify |
| `web-client/src/store/duel-state.test.ts` | modify |
| `docs/adr/ADR-0096-the-audit-judges-a-whole-duel-against-a-frozen-rubric.md` | read |
| `poker-server/src/main/kotlin/duels/poker/server/duel/Addressed.kt` | read |

## Scope

- Register and route the `DEC` above. Nothing else happens until it merges.
- Then: the flop, the turn and the river each leave a trace a player can perceive at beat 5, by
  whatever mechanism the answer settles.
- The criterion is met at **every** beat afterwards, not only at beat 5 — a fix that paces the
  all-in runout must not break the ordinary street-by-street case, which is `met` today.

## Out of scope

- **Naming a made hand, anywhere.** `ADR-0095` §3 closed that permanently, and
  `web-client/src/table/no-derivation.test.tsx`'s `HAND_TALK` matcher is a merged gate on it. A
  runout that pauses to explain what beat what is a different product.
- **The award line itself.** `ADR-0095` settled it and `TASK-121101`/`TASK-121109` shipped it;
  whether it names the right hand is `TASK-121304`, filed under this same story.
- **`R2`'s overflow and `R4`'s spacing.** `TASK-121302` and `TASK-121303`.
- **Deriving the board from `StreetDealt` in the client as a shortcut.** If the answer is
  client-side pacing, it paces snapshots the server sent; it never reconstructs a board from
  events. A client may not assert a game fact (`CLAUDE.md`, `ADR-0002`).

## Tests

**Named after the `DEC` merges, not before.** A test written now would pin a mechanism nobody has
chosen; a test name pinned in a `verify:` today is satisfied by writing that test, which gates
nothing.

## Acceptance criteria

- [ ] **`DEC-NNN` is registered and routed to the architect before any production file is opened** —
      the row exists in `tasks/BOARD.md` §*Open decisions* and in `STORY-1213`
- [ ] The merged ADR is cited in this ticket, and this ticket has been re-cut against it — module,
      *Files*, *Tests* and `verify:` all replaced by what the answer supports
- [ ] **Manual reproduction, at phone shape 390 × 664, both browsers** (`manual-verify`): play a
      hand to an all-in call before the river. On both screens the board reaches five cards through
      at least one intermediate state in which three or four community cards are visible, and the
      street label passes through the streets it deals
- [ ] The same walk at the other seven beats shows no regression: `R1` still `met` at 1, 2, 3, 4, 6,
      7 and 8
- [ ] Every command in `verify:` exits 0

## Why `verify:` carries only the linter

Two reasons, both stated so no one reads it as laziness:

1. **Half of what this must build is undecided.** Any command written today pins a mechanism the
   architect has not chosen, and a gate that presumes its own answer is worse than no gate.
2. **The failure is a browser fact and a browser fact may never be a gate here.** `ADR-0089` §2b —
   *"No pull request, `verify:` block or ticket waits on a QA case"* — is one of the three
   conditions that license the QA harness at all, and reaching for `scripts/qa/` in a `verify:`
   line breaks it rather than bending it.

**Do not invent a `grep` that passes either way.** A gate that cannot fail is worse than an honest
manual step, and this repository has been bitten by exactly that.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
