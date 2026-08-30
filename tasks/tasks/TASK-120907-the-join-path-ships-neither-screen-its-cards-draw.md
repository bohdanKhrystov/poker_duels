---
schema: 2
id: TASK-120907
title: The join path ships neither of the two screens its cards draw
type: task
status: backlog
parent: STORY-1209
module: web-client
estimate: S
tier: opus
review: standard
files_touched: 1
labels: [qa, uat, bug, medium, manual-verify]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
---

## Goal

The two screens `design/screens/join-duel.html` and `design/screens/enter-code.html` draw either
exist in the client, or the cards are corrected to the product that was decided instead — and which
of those it is has been **decided** rather than guessed.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`. Two findings, one cause: the
join path was built as two lobby panel swaps, and its two cards draw two screens.

**1. The offered seat never renders** (`design/screens/join-duel.html`). The card's whole frame —
*ImKate challenges you*, the stakes line, the room-code chip, the *Playing as* namerow, the *Take the
seat* button — has no counterpart in the client: `grep -rn "Take the seat\|challenges you\|Playing as"
web-client/src` returns nothing. Opening an invite link seats the player directly into an
already-dealt hand, blinds posted, with no intervening screen and no decision at all — not even the
one the card's lede promises: *"One decision on this screen and only one — take the seat."*

**2. There is no enter-a-code screen** (`design/screens/enter-code.html`). The card's tracked code
well, hint text, *Open the duel* / *Back* buttons and `.refusal` box do not exist. A code is typed
into an inline field on the lobby itself, and `design/screens/create-duel.html`'s own front door
disagrees with the shipped one too — the card offers *Create a duel* and *I have a code*, where the
client offers *Create a duel room* and an inline *Room code* field.

## Why this is a decision before it is a repair

**The card asks for a fact the wire does not carry.** *"ImKate challenges you"* needs the host's
display name and the room's stakes **before** the player joins, and an invite link carries only
`?room=CODE`. Nothing in `docs/protocol.md` answers a client that holds a code and no seat. So a
coder handed this as a repair would have to invent a protocol step, which is exactly what
`CLAUDE.md` rule 5 forbids.

**And the product may be right.** `docs/vision.md`'s success sentence — *"Send a link. She opens it
in a browser. We play a full heads-up match."* — is what the client does literally, and the card's
own margin note cites that same sentence as the reason its frame exists (*"No form stands between
her and the table"*). Two merged sources, pointing different ways, with no ADR between them.

**So this ticket is not startable as written.** Whoever picks it up **registers a `DEC` first**
(`CLAUDE.md` rule 5) and routes it: *should the join path have a consent screen at all* is the
**product owner's**, deriving from the vision; *what the wire tells a client that holds a code and
no seat* is the **architect's**, and only arises if the first is answered yes. The same pattern
`TASK-120601`'s *Out of scope* used for its own second half. It is **not** `STOP_BLOCKED`: neither
decision is human-only, and neither gates round 1's fix set.

## Why `medium` and not `high`

`uat` reported the first finding `high` and the second `high`. **Severity lowered on both, and the
reason is not the arithmetic** — the round's verdict is `PROCEED` at either grade, because
`TASK-120901` is `high` on its own reasons.

- **No promise in `EPIC-12`'s `high` row is broken**, and the product is on `docs/vision.md`'s side
  of the disagreement — it satisfies the very sentence the card cites as its own justification.
- **Not a regression**; the join path has behaved this way since `EPIC-03`.
- A player following a link reaches a duel and plays it. What they lose is a moment of consent and a
  look at who challenged them: a real gap, not a broken promise.
- Grading it `high` would put a coder on a screen the protocol cannot feed, which is how
  `build-epic` ends up changing production code to satisfy a 2026-08-14 drawing.

## Files

| File | Action |
| --- | --- |
| `web-client/src/lobby/Lobby.tsx` | modify |

The table names the file the *client-side* branch would touch. The card-side branch touches
`design/screens/join-duel.html` and `design/screens/enter-code.html` instead. **Which branch this
ticket becomes is the `DEC`'s to settle**, and the Files table is rewritten when it lands.

## Scope

- **Register and route the `DEC` before writing any code.**
- Once answered, rewrite this ticket's *Files*, *Scope*, *Tests* and `verify:` block to the branch
  the answer chose, and only then start it.

## Out of scope

- **Dressing the lobby's controls.** `TASK-120901`, and it lands independently of this answer.
- **The waiting frame's seat plates** (*Open seat*, *You / host / 10,000*), which
  `design/screens/create-duel.html` draws and the client does not render. Same cause, same decision;
  named here so it is not lost, and not a second ticket until the answer arrives.
- **Guessing.** Do not build the offered-seat screen from the card alone, and do not delete the
  card's frame to make the product conform. Both are the decision being taken by whoever writes the
  diff.

## Tests

**None yet, and that is stated rather than faked.** `verify:` carries only the linter, because the
one command that could gate this ticket is a command about a behaviour nobody has decided on. A
`grep` that passed either way would be worse than an honest gap — the acceptance criterion below is
the manual reproduction, and the label is `manual-verify` until the `DEC` lands and this block is
rewritten.

## Acceptance criteria

- [ ] A `DEC` is registered in both registers and routed, before any diff exists
- [ ] **The manual reproduction, for whoever picks this up**: host a room on one browser; open the
      invite link on a second; observe that the second browser is seated at a dealt hand with no
      intervening screen. Then type a made-up code into the lobby's field and press *Join the duel*;
      observe the refusal printed inline above the still-visible lobby rather than in the card's
      refusal box
- [ ] When the answer lands, this ticket carries a real `verify:` block and this criterion is
      replaced by it
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged. **Not startable until its `DEC` is answered.**
