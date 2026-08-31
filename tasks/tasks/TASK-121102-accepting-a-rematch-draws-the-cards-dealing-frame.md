---
schema: 2
id: TASK-121102
title: Accepting a rematch draws the card's dealing frame
type: task
status: done
parent: STORY-1211
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 3
labels: [qa, uat, bug, low, manual-verify]
depends_on: []
verify:
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Accepting a pending rematch shows the card's *"it begins"* frame before the new hand's table, rather
than jumping straight to it.

## The defect

`design/screens/rematch-states.html` draws a third frame, *Both — it begins*:

```
<div class="dealing">Rematch. The button changes sides —<br>
  dealing hand 1…</div>
```

Neither string exists anywhere under `web-client/src` — `grep -rn "it begins\|dealing hand\|changes
sides" web-client/src` is empty — so the frame cannot render. Round 3 confirmed it from the browser
too: the accepting device's frame array goes from the offer screen straight to
`"...Hand 1 · Preflop...You | YOUR TURN | D | 9,950..."` with nothing between.

**Why this is `low` and not `high`.** `EPIC-12`'s `high` row names *rematch dead*. Rematch is not
dead: the click is taken, hand 1 is dealt, the table renders and the dealer button has changed sides
— the `D` in that same evidence line is the card's own claim, already on screen. What is missing is
a momentary interstitial whose information the destination already carries. Nothing is lost but the
beat.

## Files

| File | Action |
| --- | --- |
| `web-client/src/result/RematchControl.tsx` | edit |
| `web-client/src/store/duel-state.ts` | edit |
| `web-client/src/result/RematchControl.test.tsx` | edit |

## Scope

- **Find or add the store state the frame names**: both sides accepted, the new hand not yet dealt.
  The frame is a *state*, not a timer — a fixed-duration splash is a different thing and is not what
  the card draws.
- **Render the card's two lines in that state**, composed from the card, not reworded.

## Out of scope

- **Inventing a delay.** If the state is genuinely instantaneous on the wire, say so in the ticket
  and route it rather than sleeping to make a frame visible. A client that pauses to show itself a
  screen is a client asserting a fact about the game's pace.
- **The rematch offer frames themselves.** Round 2 confirmed both match the card well.

## Tests

**A command cannot gate this one honestly, and inventing one would be worse than saying so.** The
frame's existence depends on a store state that does not exist yet, so any `grep` written today is
satisfied by adding the literal anywhere, and any test name pinned today is satisfied by writing the
test. When the state exists, this block becomes a real `RematchControl.test.tsx` case and `verify:`
grows the two-command pattern the round-2 tickets use.

## Acceptance criteria

- [ ] **Manual reproduction, before:** with a rematch offered by A, arm `record` on B, click
      *Rematch*, read `frames` — the array goes from the offer screen straight to the live table with
      no frame between
- [ ] **Manual acceptance, after:** the same walk shows a frame carrying the card's two lines
      between the offer and the table
- [ ] Every command in `verify:` exits 0
