---
schema: 2
id: TASK-140719
title: The frame-card gate runs in CI
type: task
status: backlog
parent: STORY-1407
module: repo
estimate: XS
tier: haiku
review: standard
files_touched: 1
labels: [ci, design, process]
depends_on: []
verify:
  - grep -qF 'check-frame-cards.sh' .github/workflows/tickets.yml
  - ./design/check-frame-cards.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/check-frame-cards.sh` runs in **no workflow**. Three tickets name it in their `verify:`
blocks and CI names it nowhere, so it has guarded exactly the three commits whose authors ran it by
hand.

This wires it into `tickets.yml`, beside the linter that already runs there.

## Why this is its own ticket

Found by the architect while answering `DEC-155`, and it is the reason that decision did **not**
choose a new shell gate: the failure mode `DEC-155` was registered against — *a check that lives only
in ticket `verify:` blocks and does not outlive the merge* — **has already happened here once, to a
design gate, unnoticed**.

The gate was added earlier in `EPIC-14` to catch a specific defect: whole-file `awk` counts cannot
tell you which frame a thing landed in, so a card can show a folded hand face-up with 48 gates green.
It was written, it works, and nothing runs it.

`ADR-0142` §1 records the gap and explicitly leaves the wiring to a ticket rather than widening a
decision into a workflow edit.

## Files

| File | Action |
| --- | --- |
| `.github/workflows/tickets.yml` | modify |

Read `.github/workflows/tickets.yml` and `design/check-frame-cards.sh`. **Nothing outside the table
above is changed** — in particular the script itself is not edited, and `build.yml` keeps its two
jobs (`ADR-0088` §1).

## Scope

One step in the existing `lint backlog` job that runs `./design/check-frame-cards.sh`.

It runs on every PR, like the ticket linter beside it. No new job, no new runner, no new dependency.

## Out of scope

- **Editing `check-frame-cards.sh`.** If it fails on `develop` today, stop and report — that is a
  defect the wiring would expose, and it becomes its own ticket rather than being fixed here under
  cover of a workflow change.
- **`check-drift.sh`**, which is a separate question and may already run elsewhere; check, and say
  what you found, but do not wire it here.
- **`ADR-0142`'s own test**, which is the client suite's and is a different ticket.

## Tests

No unit test — this is a workflow edit, and the gate is its own proof.

The acceptance criterion is that the step is **shown to run and to be capable of failing**, not
merely present.

## Acceptance criteria

1. `.github/workflows/tickets.yml` names `check-frame-cards.sh` in the `lint backlog` job.
2. The PR's own CI run shows the step executing, and the run is green.
3. **The step is shown able to fail**: break one frame's `<h2>` anchor locally, confirm
   `./design/check-frame-cards.sh` exits non-zero, and restore the file byte-for-byte with `cp`.
   Report both exit codes. A step that cannot fail is not a gate.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] The failing probe in criterion 3 was run and reverted.
- [ ] PR merged into `develop`.
