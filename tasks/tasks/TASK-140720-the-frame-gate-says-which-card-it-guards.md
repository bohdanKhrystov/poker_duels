---
schema: 2
id: TASK-140720
title: The frame gate says which card it guards
type: task
status: backlog
parent: STORY-1407
module: repo
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [design, process]
depends_on: [TASK-140719]
verify:
  - ./design/check-frame-cards.sh
  - sh -c 'grep -qF "duel-table-states.html" design/check-frame-cards.sh'
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/check-frame-cards.sh` guards **one** card — `design/screens/duel-table-states.html` — and one
thing inside it, the rival's `<div class="oppcards">` slot per frame. There are **eleven** cards under
`design/screens/`.

That is correct for what it was built to do, and its name does not say so. This ticket makes the
script's own output and its header state the scope, so nobody reads a green run as coverage it never
had.

## Why this is its own ticket

Found while wiring the gate into CI (`TASK-140719`), by asking what happens when a card with no
`<h2>` frames is added: the answer is that the gate does not look at it at all.

**Two PRs have already overstated this.** `TASK-140810`'s and `TASK-140811`'s landings both cited
`check-frame-cards.sh` alongside the claim that a block had landed in the right frame of
`account.html`. The claim was true — a reviewer read the frames and confirmed it — but the script
took no part in it, and the sentences read as though it had. The gate exited 0 on those PRs because
it never opened that file.

The script is not wrong. It was written for `TASK-141107` against a specific defect: whole-file counts
cannot tell *the right frame reveals* from *some frame reveals and another silently does not*, and it
reads `duel-table-states.html` frame by frame to catch exactly that. Widening it to eleven cards with
different structures is a different job and is **not** this ticket.

## Files

| File | Action |
| --- | --- |
| `design/check-frame-cards.sh` | modify |

Read `design/check-frame-cards.sh` and `design/check-drift.sh` for house shape. **Nothing outside the
table above is changed** — no workflow, no card, no other script.

## Scope

Two edits, both to text the script already prints or carries:

1. The header comment says, in its first sentence, which single card it reads and that every other
   card under `design/screens/` is outside it.
2. On success it prints one line naming the card it checked and the frame count, so a green CI step
   is legible as *this card, these frames* rather than as *the cards are fine*.

## Out of scope

- **Widening the gate to other cards.** Different cards have different structures and no rival-hand
  slot; a general frame gate is a separate design question, and `ADR-0142` has just answered the
  adjacent one for text modules.
- **Renaming the script.** Three tickets and a workflow name it; a rename is churn for no gate.
- **Changing what it checks** inside `duel-table-states.html`. The logic is not touched.

## Tests

No unit test — the script is its own proof, and the acceptance criteria below are the check.

## Acceptance criteria

1. `./design/check-frame-cards.sh` still exits 0 and its checking logic is byte-unchanged.
2. Its success output names `duel-table-states.html` and a frame count.
3. Breaking one frame's `<h2>` still makes it exit non-zero; the file is restored byte-for-byte
   afterwards and both exit codes are reported.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] The failing probe in criterion 3 was run and reverted.
- [ ] PR merged into `develop`.
