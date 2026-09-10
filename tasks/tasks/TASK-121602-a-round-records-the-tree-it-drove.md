---
schema: 2
id: TASK-121602
title: A round records the tree it drove, not its own HEAD
type: task
status: backlog
parent: STORY-1216
estimate: XS
tier: sonnet
review: standard
files_touched: 2
labels: [process, qa, harness, manual-verify]
depends_on: [TASK-121601]
verify:
  - test "$(grep -c 'COMMIT: <git rev-parse --short HEAD>' .claude/agents/qa.md)" = "0"
  - grep -qF 'SERVED:' .claude/agents/qa.md
  - grep -qF 'stack.sh served' .claude/skills/qa-cycle/SKILL.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

A `qa` report states the commit **the browser loaded**, and the `qa-cycle` skill does not open a
round whose served commit is not the commit under test.

## Why this exists

`.claude/agents/qa.md`'s report shape says `COMMIT: <git rev-parse --short HEAD>`. That is a fact
about where the agent is standing and says nothing about what the dev server is serving. In
`STORY-1216` round 1 the two differed by three days and 92 commits, and every finding in the report
was about code the browser never loaded. The line is not merely uninformative — it is the line that
made a void round look like a valid one.

`ADR-0089` §4's harness class: **no production code may be changed here.**

## Files

| File | Action |
| --- | --- |
| `.claude/agents/qa.md` | modify |
| `.claude/skills/qa-cycle/SKILL.md` | modify |

## Scope

- In `.claude/agents/qa.md`'s `## Report` block, **replace** the `COMMIT:` line with a `SERVED:`
  line carrying what `scripts/qa/stack.sh served` printed — both ports, each with its checkout root
  and short commit. The old line is removed, not kept beside the new one: two commit lines in one
  report is how a reader picks the wrong one.
- In `.claude/skills/qa-cycle/SKILL.md`'s *Bringing it up*, add `scripts/qa/stack.sh served` as the
  step **after** `wait-server` / `wait-web` and **before** any browser starts, with the rule stated
  in the imperative: if either line does not name the commit under test, **the round does not
  open** — report `STOP_INFRA`, say which tree was answering, and stop. Do not repair it by
  guessing; a stale listener is a human's to clear, because every verb that would end it is denied.
- State in the skill, in one sentence each, the two things that made the mismatch invisible, so the
  next reader does not rediscover them: `wait-server` and `wait-web` ask only whether *something*
  answers; and `web-client/vite.config.ts` sets no `strictPort`, so a second `npm run dev` moves to
  another port and prints it to a background log.

## Out of scope

- **The `served` command itself** — `TASK-121601`, which this depends on.
- **Adding a `STOP_INFRA` row to `.claude/agents/qa-manager.md`'s Step 6 table.** Recorded as an
  observation in `STORY-1216`; not ticketed here, and this ticket must not open that file.
- **`.claude/agents/uat.md` and `.claude/agents/audit.md`.** Their report shapes carry the same
  `COMMIT:` line and owe the same repair. Not yet ticketed — one focus at a time, and this round
  ran under `qa`.

## Tests

There is no runnable behaviour here: both files are prose that agents read. The gate is therefore
three text assertions plus a manual reproduction, and the ticket says so rather than inventing a
command that passes either way.

| Gate | Proves |
| --- | --- |
| `grep -c 'COMMIT: <git rev-parse --short HEAD>' … = 0` | the **removal** happened. Measured at `ea4bd05c`: the count is **1** today, so this is red before the diff and green after, and it cannot be satisfied by adding text |
| `grep -qF 'SERVED:'` | the replacement line exists |
| `grep -qF 'stack.sh served'` | the skill's *Bringing it up* calls it. Measured at `ea4bd05c`: **0** occurrences today |

## Acceptance criteria

- [ ] `.claude/agents/qa.md` contains no occurrence of `COMMIT: <git rev-parse --short HEAD>`.
- [ ] `.claude/agents/qa.md`'s `## Report` block contains a `SERVED:` line whose description names
      both ports and both commits.
- [ ] `.claude/skills/qa-cycle/SKILL.md` calls `scripts/qa/stack.sh served` in *Bringing it up*,
      after the two `wait-*` steps and before any `chrome-up`, and states the refusal in the
      imperative.
- [ ] **Manual reproduction, as the acceptance criterion for the refusal itself** (`manual-verify`,
      because the rule governs an agent's behaviour and no command can gate it): with a stale
      listener on `:5173`, `scripts/qa/stack.sh served` names that listener's checkout and commit,
      and the skill's text as written instructs the round to stop rather than proceed. Paste the
      `served` output in the PR body.
- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket.
