---
schema: 2
id: TASK-120504
title: A round allocates the third profile CORE-03 needs
type: task
status: done
parent: STORY-1205
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [process, qa, harness]
depends_on: [TASK-120503]
verify:
  - grep -qF 'chrome-up 9234' .claude/skills/qa-cycle/SKILL.md
  - grep -qF 'chrome-down 9232 9233 9234' .claude/skills/qa-cycle/SKILL.md
  - grep -qF '9234' docs/test-plan.md
  - grep -qF '`CORE-03`' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A round brings up the third browser profile `CORE-03` has always required, so the case that guards
*"Two players. Never three."* can actually be run.

## This is a harness defect, and what that means here

Filed under `ADR-0089` §4 and `EPIC-12` §Termination rule 6. It is **excluded from `B(1)`**, and
**no production code may be changed by this ticket** — the two files are the skill and the
catalogue. A `## Files` table naming anything under `poker-engine/`, `poker-server/` or
`web-client/` is grounds to reject the diff on sight.

## The defect

Round 1 of `/qa-cycle regression`, 2026-08-29, commit `fe4bbf2a`. `CORE-03` was **blocked** — not
failed. Blocked is not a failure and it is not counted in `B(1)`; it is a statement that the
harness could not present the case's precondition.

`CORE-03` reads *"fresh profile C, `open /?room=<code>` on a **full** room"*. The skill starts
exactly two:

```
scripts/qa/stack.sh chrome-up 9232 "$A"
scripts/qa/stack.sh chrome-up 9233 "$B"
```

and tears exactly two down: `scripts/qa/stack.sh chrome-down 9232 9233`. There is no third, the
`qa` agent may not start Chrome itself, and a second tab on an existing port is not a third
device — every `drive.mjs` verb calls `attach({fresh:false})` and a tab shares its port's
`localStorage`, so it shares `pd.deviceId` and the server re-seats it as the same player
(`ADR-0018`).

`stack.sh chrome-up` already takes a port and a profile directory, so nothing in the script needs
to change: the round simply never asks for a third.

**Fix the harness, not the case.** `CORE-03` guards a sentence the vision states outright —
*"Two players. Never three."* Rewriting it to fit two profiles would delete the only check that a
third player is refused a seat.

## Files

| File | Action |
| --- | --- |
| `.claude/skills/qa-cycle/SKILL.md` | modify |
| `docs/test-plan.md` | modify |

## Scope

- In the skill's stack-up block, add a third profile directory from `mktemp -d` and
  `scripts/qa/stack.sh chrome-up 9234 "$C"`, alongside the existing two.
- In the skill's stack-down block, make the teardown `scripts/qa/stack.sh chrome-down 9232 9233
  9234`. A profile started and never closed is a browser nobody can stop — `kill` is denied.
- The third profile is **fresh per round**, exactly as the other two are, and for the same reason
  the skill already states: a reused profile rejoins its old room by `pd.deviceId`.
- In `docs/test-plan.md`, extend the driver line — *"`A` is port 9232 and `B` is 9233"* — to name
  `C` as 9234, and make `CORE-03`'s `do` cell say `C open /?room=<code>` in the driver's own verbs
  rather than in prose.

## Out of scope

- **`scripts/qa/stack.sh`.** `chrome-up` and `chrome-down` already take ports; nothing there is
  missing.
- **Weakening or deleting `CORE-03`.** The `verify:` block fails if the id disappears.
- **Bringing up a third profile only for some scopes.** One rule is cheaper to keep true than two,
  and an unused headless tab costs nothing next to a scope condition nobody remembers.
- **The five history-dependent cases.** That is `TASK-120503`, which this ticket depends on
  because both edit `docs/test-plan.md` and a stale base would regress the other's rows. This
  ticket is `backlog` for exactly that reason and goes `ready` when `TASK-120503` is `done`.

## Tests

None — the deliverables are two documents, so the gates are structural checks over their text.

| Gate | Proves | Today |
| --- | --- | --- |
| `chrome-up 9234` in the skill | the round starts a third profile | **exits 1** |
| `chrome-down 9232 9233 9234` in the skill | it also stops it | **exits 1** |
| `9234` in the catalogue | the case knows which port C is | **exits 1** |
| `` `CORE-03` `` still in the catalogue | the case was not deleted to satisfy the three above | exits 0 — it must keep doing so |

All four were run at commit `fe4bbf2a`; the first three exit `1` and the fourth exits `0`.

## Acceptance criteria

- [ ] The skill starts three browser profiles and stops all three.
- [ ] The third profile comes from `mktemp -d`, like the other two.
- [ ] `docs/test-plan.md` names `C` as port 9234, and `CORE-03`'s `do` cell uses driver verbs.
- [ ] `CORE-03`'s `expect` and `fails if` cells are byte-identical to what they are now.
- [ ] The diff touches exactly two files, and they are the skill and the catalogue.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
