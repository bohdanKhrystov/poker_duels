---
schema: 2
id: TASK-121007
title: Two cards and a margin note catch up with the client that overtook them
type: task
status: backlog
parent: STORY-1210
module: design
estimate: XS
tier: sonnet
review: light
files_touched: 3
labels: [qa, uat, bug, medium, design]
depends_on: []
verify:
  - grep -qF 'committed' design/screens/duel-table.html
  - test "$(grep -c 'bets <span class="amt">' design/screens/duel-table.html)" -eq 0
  - grep -qF '−1 duel coin' design/screens/duel-end.html
  - test "$(grep -c 'The coin goes to' design/screens/duel-end.html)" -eq 0
  - test "$(grep -c 'SignInForm.tsx does not render it yet' design/screens/sign-in.html)" -eq 0
  - grep -qF 'SignInScreenBody' design/screens/sign-in.html
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Three merged design cards stop asserting things about the client that are no longer — or never
were — true, so the next UAT round spends its findings on the product rather than on the drawings.

## The defect

Round 2 of `/qa-cycle uat regression`, 2026-08-30, commit `07df9e7f`. Three findings, one shape:
**the card is the artefact in arrears**, not the client. `STORY-1209` established the adjudication
and `STORY-1210` §*Which artefact is the defect* applies it; the repair is the card, and none of
these is a product defect, so none counts toward `B(2)`.

**1. `duel-table.html`'s bet line says *bets* where the client says *committed*.** Card, line 145:
`<div class="bet-line">bets <span class="amt">400</span></div>`. Client, `DuelTable.tsx:72–83`:
`committed <span>100</span>`, and its own KDoc states the rule:

> The word is the field's, not an action's: the view says how much is committed and never says
> whether it got there by a blind, a call, a bet or a raise.

The projection carries a committed total for the street. Printing *bets* would be the client
inferring which action produced it, which is a client asserting a game fact — `ADR-0002`. The
component (2026-08-15) also postdates the card (2026-08-14).

**2. `duel-end.html`'s Defeat coin line names the rival.** Card, line 96:
`<span class="delta loss">The coin goes to ImKate</span>`. Client, `outcome-text.ts:52`:
`"−1 duel coin"`. Two reasons the client governs:

- `ADR-0089` §5 makes the module that owns a player-facing literal its source, and `coinLine`
  (2026-08-16, `TASK-030803`) postdates the card (2026-08-14) by two days.
- **The card's phrasing needs a fact the wire does not carry.** `DuelOutcome` is
  `{ winner, handsPlayed, finalStacks }` — no display name. The Victory frame's `+1 duel coin`
  already matches the client exactly; only the Defeat frame drifted.

**3. `sign-in.html`'s margin note claims a control does not ship.** It reads:

> Forgot your password? is FORGOT_PASSWORD_LABEL (recovery-text.ts) — **SignInForm.tsx does not
> render it yet**, so this card draws the gap the next UAT round should catch.

The control **does** render, and always did — from `Lobby.tsx:387–389`'s `SignInScreenBody`, not from
`SignInForm.tsx`. `grep -rn 'FORGOT_PASSWORD_LABEL' web-client/src` returns `Lobby.tsx` and no other
component. **The drawing was right and the note was wrong about where the control lives**, so the
note has to be corrected rather than deleted: the card's prediction succeeded, and the gap round 2
found is a treatment gap, not an absence — it renders unclassed where the card draws `class="link"`.
That repair is `TASK-121005`, in the client.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `design/screens/duel-end.html` | modify |
| `design/screens/sign-in.html` | modify |

## Scope

- **`duel-table.html`**: the bet line reads `committed <amt>`, and the margin note beside it names
  `DuelTable.tsx`'s `BetLine` as the owner and `ADR-0002` as the reason the word is the field's.
- **`duel-end.html`**: the Defeat frame's `.delta.loss` reads `−1 duel coin` (U+2212 MINUS SIGN, as
  `coinDeltaText` emits and as the Victory frame's `+1` already pairs with), and the margin note
  names `outcome-text.ts`'s `coinLine` as the owner and states that `DuelOutcome` carries no display
  name.
- **`sign-in.html`**: the margin note says the control is rendered by `Lobby.tsx`'s
  `SignInScreenBody` — not `SignInForm.tsx` — and that what the next round should check is its
  **treatment**, which as shipped carries no class where this card draws `.link`.
- Nothing else on any of the three cards changes: same frames, same tokens, same geometry.

## Out of scope

- **The `duel-end` meta line.** The card asks for `17 hands · 12 minutes · she took the whole
  stack`; the wire carries no duration at all, and `DuelResult.tsx`'s merged `metaLine` KDoc explains
  why the line states stacks. **What that line should say is a product decision, not a card
  correction**, and it is named in `TASK-120911`'s *Out of scope* for the second round running. Do
  not guess it here.
- **The account offer's own card.** `duel-end.html` draws nothing after *Rematch*; the offer section
  is `STORY-0415`'s, merged thirteen days after the card, and `ADR-0091` §5 registers *"carded-screen
  accretions … the account offer first among them"* as debt for the retrofit story.
- **`duel-table-states.html`'s missing away and back frames.** Card composition, owed to the same
  retrofit story, named in `TASK-120911`'s *Out of scope*.
- **`TASK-120911`'s three corrections.** Still open, still backlog, and this ticket does not absorb
  them — a second round's findings do not silently rewrite a first round's ticket.
- **Changing any client file.** These are drawings. `TASK-121005` is the client half of finding 3.

## Tests

No test file: a card is a rendered artefact, and `ADR-0024` §3 puts its taste judgment with the human
at the pane. The `verify:` block gates what a command honestly can, and **each line fails today**:

| Command | Proves |
| --- | --- |
| `grep -qF 'committed' duel-table.html` | the corrected word is present |
| `grep -c 'bets <span class="amt">' … -eq 0` | the old one is gone — the *pair* matters, since a card could gain the new word and keep the old line |
| `grep -qF '−1 duel coin' duel-end.html` | the corrected Defeat delta is present, with the U+2212 sign the client emits |
| `grep -c 'The coin goes to' … -eq 0` | the old phrasing is gone from both the frame and any note quoting it |
| `grep -c 'SignInForm.tsx does not render it yet' … -eq 0` | the false claim is gone |
| `grep -qF 'SignInScreenBody' sign-in.html` | it was replaced by the component that actually renders the control, not merely deleted |
| `sh design/check-drift.sh` | every token name each card mentions is still declared in the sheet and every inlined value still equals it (`ADR-0024` §2) |

## Acceptance criteria

- [ ] `design/screens/duel-table.html`'s bet line reads `committed <amt>` and its note names
      `BetLine` and `ADR-0002`
- [ ] `design/screens/duel-end.html`'s Defeat delta reads `−1 duel coin` and its note names
      `coinLine` and the fact that `DuelOutcome` carries no display name
- [ ] `design/screens/sign-in.html`'s note names `Lobby.tsx`'s `SignInScreenBody` and describes the
      real gap as a treatment gap
- [ ] `sh design/check-drift.sh` exits 0 with all three cards in the tree
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged. The human's visual verdict may trail it
(`ADR-0091` §3).
