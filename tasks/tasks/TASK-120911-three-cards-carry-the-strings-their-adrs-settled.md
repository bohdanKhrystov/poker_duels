---
schema: 2
id: TASK-120911
title: Three cards carry the strings the ADRs that superseded them settled
type: task
status: done
parent: STORY-1209
module: design
estimate: S
tier: sonnet
review: light
files_touched: 3
labels: [qa, uat, bug, medium, design]
depends_on: []
verify:
  - grep -qF 'Back to the lobby' design/screens/create-duel.html
  - grep -qF 'The room stays open. That link still works for your rival, and it brings you back.' design/screens/create-duel.html
  - grep -qF 'No duel room has that code.' design/screens/enter-code.html
  - grep -qF 'Back to the lobby' design/screens/duel-end.html
  - sh -c '! grep -qF "This code doesn" design/screens/enter-code.html'
  - sh design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

Three merged cards state the strings the ADRs merged **after** them decided, so a coder transcribing
one is not transcribing a decision that was overturned.

## The defect

Round 1 of `/qa-cycle uat regression`, 2026-08-30, commit `c05ee695`. Three conformance findings
that inverted at triage: **the client is right and the card has drifted.** `design/screens/` was
drawn on 2026-08-14/15; each of these decisions merged later.

| card | the card says | the client says | what governs |
| --- | --- | --- | --- |
| `create-duel.html` waiting frame | no way out, and no line about the room | *Back to the lobby*, and *The room stays open. That link still works for your rival, and it brings you back.* | **the client.** [`ADR-0073`](../../docs/adr/ADR-0073-the-waiting-screen-says-back-to-the-lobby-and-the-room-stays-open.md) decided both strings and says in as many words that *"`design/screens/create-duel.html`'s waiting frame **gains** the control and the line verbatim"*, as `EPIC-06`'s work. That work never happened |
| `enter-code.html` refusal | *This code doesn't open a duel.* | *No duel room has that code.* | **the client.** `Lobby.tsx:347` owns the literal; `ADR-0072` and `ADR-0073` both name it as the shipped correction; `docs/test-plan.md` `CORE-04` transcribes it as the expected wire text |
| `duel-end.html` way back | *Back to lobby* | *Back to the lobby* | **the client.** `ADR-0073` fixes the string as *"byte-identical to the string `DuelResult.tsx` already renders"* — one phrase for one action |

**And one more, resolved in the same direction for the same reason.** The card's waiting frame says
*Copy link* where the client says *Copy the link*. No ADR owns either string, so the newest merged
signal on that frame's phrasing decides it: `ADR-0073` chose the definite article for the control
beside it and refused the terser alternatives by name. The card takes the client's string.

## Why this is not a product defect, and counts in no `B(N)`

`uat` reported these under check **a** as `high` divergences from a card. At triage the direction
inverted: the product contradicts nothing, so there is no product defect to count. This is **not** a
fourth exclusion from `B(N)` — the round invents none — it is that `B(N)` counts product defects and
a card in arrears is not one. Severity `medium`: a real defect in the design trail, with the
governing ADR merged and findable by anyone who looks, which is the workaround.

## Files

| File | Action |
| --- | --- |
| `design/screens/create-duel.html` | modify |
| `design/screens/enter-code.html` | modify |
| `design/screens/duel-end.html` | modify |

## Scope

- **`create-duel.html`**: the waiting frame gains the *Back to the lobby* control and the
  room-stays-open line **verbatim**, as `ADR-0073` assigns; *Copy link* becomes *Copy the link*.
- **`enter-code.html`**: the refusal reads *No duel room has that code.*
- **`duel-end.html`**: *Back to lobby* becomes *Back to the lobby*, in both frames.
- Each change carries a margin note naming the ADR that settled it, the way the cards already cite
  their sources — so the next reader sees why the card says what it says.

## Out of scope

- **`duel-end.html`'s meta line.** The card asks for *"17 hands · 12 minutes · you took the whole
  stack"*; the wire carries no duration at all and `DuelResult.tsx`'s merged `metaLine` KDoc explains
  why the line states stacks. **Deciding what that line should say is a product question**, not a
  card correction. Not ticketed; registered in `STORY-1209`.
- **`duel-end.html`'s account-offer section.** `STORY-0415` merged the offer thirteen days after the
  card was drawn, and `ADR-0091` §5 already registers *"carded-screen accretions … the account offer
  first among them"* as debt for the `EPIC-06` retrofit story. That story's, not this one's.
- **`duel-table-states.html`'s missing away and back frames.** Composition, and the same retrofit
  story owns it. `ADR-0046` settled the copy, so it is well-sourced when someone draws it.
- **Every other divergence.** Where the client is the side that drifted, the ticket is against the
  client — `TASK-120901`, `TASK-120908`, `TASK-120912`.

## Tests

No test file: cards are rendered artefacts and `ADR-0024` §3 puts their taste judgment with the
human at the pane. The `verify:` block gates what a command honestly can, and **each of the first
five lines fails today**: four assert a string the card does not yet carry, and the fifth asserts the
absence of the string it must lose — without that negative line, a card that added the new refusal
beside the old one would pass.

## Acceptance criteria

- [ ] All three cards render, and each changed string is quoted from the ADR that settled it
- [ ] `design/screens/enter-code.html` no longer contains *This code doesn't open a duel.*
- [ ] `sh design/check-drift.sh` exits 0 with the edits in the tree
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged. The human's visual verdict may trail it
(`ADR-0091` §3).
