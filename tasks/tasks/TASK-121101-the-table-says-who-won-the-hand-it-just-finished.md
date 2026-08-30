---
schema: 2
id: TASK-121101
title: The table says who won the hand it just finished
type: task
status: backlog
parent: STORY-1211
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, medium, manual-verify]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
---

## Goal

When a hand ends — at a showdown or on a fold — the table states the pot's winner and its amount
where the pot line stands, as `design/screens/duel-table-states.html` draws it.

## The defect, and the half of it nobody has decided

`design/screens/duel-table-states.html` draws the banner as **this state's own structure**, and its
own margin note says the pot row *becomes* the banner without changing shape:

```
<span class="amount win">You win 4,850</span>
<span class="meta">Two pair, aces and sevens</span>
```

`web-client/src/table/PotStrip.tsx` has exactly one `return`. It renders `Pot {view.pot}` and
`Blinds N/N · Hand N · {street}` on every street including `COMPLETE`, with **no branch** that could
ever swap in a banner, and `DuelTable.tsx` renders no other pot-adjacent element. So no banner
renders at any tick — this is true by construction, not by observation, which is how round 3's
hand-check reproduced it without inheriting the `record`/`frames` blind spot.

**The two halves are not equally buildable, and that is the whole of this ticket's difficulty.**

- **The amount is on the wire.** `PotAwarded` carries `seat` and `amount`
  (`web-client/src/protocol/protocol.gen.ts`), so *You win 4,850* is transcribable today.
- **The made hand's name is on no wire field.** No `GameEvent` in `protocol.gen.ts` names a made
  hand. A client that printed *Two pair, aces and sevens* would be **asserting a game fact**, which
  `CLAUDE.md`'s non-negotiables and `ADR-0002` forbid — and `web-client/src/table/no-derivation.test.tsx`
  gates exactly that vocabulary (`HAND_TALK` matches `pair`, `wins?`, `winner`, `beats`).

So the card draws one renderable half and one half that needs a product decision before any diff
exists. **Do not guess it** (`CLAUDE.md` rule 5). Register the `DEC` first, exactly as `TASK-120907`
did, and let the answer rewrite this block.

## Files

| File | Action |
| --- | --- |
| `tasks/BOARD.md` | edit — register the `DEC` |
| `docs/adr/` | create — the answering ADR |
| `web-client/src/table/PotStrip.tsx` | edit — only after the answer |

## Scope

- **Register `DEC-NNN` in both registers and route it to the `product-owner`**, before any diff
  exists: *when a hand ends, should the table state the pot's winner and amount in place of the pot
  line, as `duel-table-states.html` draws — and does the product name the made hand at showdown at
  all, given no wire field carries one?*
- **Then, and only then, rewrite this ticket** — its `Scope`, its `Tests` and its `verify:` — to the
  answer. A *yes* on the second half raises a conditional architect question (what wire field carries
  a made-hand description); it does not arise on a *no*.

## Out of scope

- **Deriving the hand name in the client.** Forbidden by `CLAUDE.md`, `ADR-0002` and a merged test.
  It is a server change or it is nothing.
- **`no-derivation.test.tsx`'s fixture.** Its `names no hand and declares no winner` case renders a
  `street: "TURN"` view, so a `COMPLETE`-street banner does not break it. Do not weaken it to make
  room; if it ever needs to change, that is its own ticket with its own reasoning.
- **The meta line's street segment.** `TASK-121108` — the card is the outlier there, not the client.

## Tests

**None yet, and that is stated rather than faked.** `verify:` carries only the linter, because the
one command that could gate this ticket is a command about a behaviour nobody has decided on. A
`grep` that passed either way would be worse than an honest gap — `ADR-0084` and this repository's
own history both say so — and the label is `manual-verify` until the `DEC` lands and this block is
rewritten.

## Acceptance criteria

- [ ] A `DEC` is registered in both registers and routed to the `product-owner`, before any diff
      exists
- [ ] **Manual reproduction, before:** play a hand to a checked-down showdown on two browsers; when
      the hand ends, the pot line reads `Pot 0` and `Blinds N/N · Hand N · Hand complete` and no
      element states a winner or an amount. Repeat with a fold ending; same result.
- [ ] **Manual acceptance, after:** the same two walks show the winner and the pot amount where the
      pot line stood, in the words the answering ADR settles
- [ ] Every command in `verify:` exits 0
