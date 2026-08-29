---
id: STORY-1203
title: The qa-cases skill — the authoring half, whose last act is a printed command
type: story
status: ready
parent: EPIC-12
labels: [process, qa, meta]
depends_on: []
---

## Goal

`.claude/skills/qa-cases/SKILL.md` exists, so `/qa-cases EPIC-04 EPIC-05` plans and lands the
missing catalogue suites through ordinary reviewed PRs and then **stops** — printing, verbatim and
with the scope filled in, the `/qa-cycle` command the human types next. This is the authoring half
of the request the human made on 2026-08-29, delivered as its own command because
[`ADR-0090`](../../docs/adr/ADR-0090-a-skill-may-write-the-catalogue-or-run-it-never-both.md) §3
licensed exactly that shape and refused the composite.

## This is not a round story

`EPIC-12`'s Stories table says *"one story per QA round; the round number lives in the story, not
the id"*. **This story breaks that convention**, the way `STORY-1201` breaks it by being a
retrospective record. It runs no round, brings no stack up and reports no `B(N)`: it builds the
tool that fills the catalogue a round runs. The epic's table is amended in this story's PR to say
so, and the round stories resume at `STORY-1204` — openly, in the table, rather than by quietly
shifting a number.

## Why

`docs/test-plan.md` §*Not yet written* lists `EPIC-04`, `EPIC-05` and `EPIC-06` with **no cases at
all**, which is the largest piece of work left in this epic — `ADR-0090` §Consequences counts 24
Definition-of-done promises across the three, and §Template rule 1 is one case per promise. Nothing
can author them today: `qa` has no `Write`, `qa-manager` writes only bug tickets from a round it
was handed, and `qa-cycle` is the runtime half.

It is also the story that makes `ADR-0090` §2's check meaningful. That ADR declares **exactly three
files** under `.claude/` may name `qa-cycle`, and names `.claude/skills/qa-cases/SKILL.md` as the
third — a slot that does not exist yet. Until it does, the allow-list is a command in an ADR with
one of its three members missing, and no ticket carries it. This story creates the file and puts
the check in a `verify:` block in the same PR, so the list is enforced from the moment it is
complete rather than from the first time somebody remembers to run it.

## Design notes

Everything below is settled by `ADR-0090`, merged 2026-08-29 as `8dfebbc1`. Nothing here is open.

**§3 is a list of verbs and it is binding.** The skill **may** read the epics, the ADRs,
`docs/duel-rules.md`, `docs/vision.md` and the client's own literals; plan a story and its tickets;
run them through `build-epic`; and update `docs/test-plan.md` including its §*Not yet written*
table. It **may not** bring the stack up, start a browser, dispatch `qa` or `qa-manager`, or invoke
`/qa-cycle` by any route. `ADR-0090` §2 explains why it is a verb list rather than a principle:
*"so that one reviewer can check it against one file."* That sentence is the review instruction for
this story's ticket.

**The terminal act is a report, and the distinction a grep cannot make lives here.** The skill
prints the next command; it does not run one. `ADR-0090` §2 says so in as many words — *"what no
grep can catch is whether one of the three declared files runs the cycle rather than naming it,
because print this command and run this command are the same string."* The ticket's gates say which
half of that they see and which they do not.

**§4 gives every authored case a fifth `source` column**, already documented in
`docs/test-plan.md` §*How a case is written* and §*Per-epic suites* → *Template* rule 3 by
`ADR-0090`'s own PR. Player-facing text cites the module holding the literal; anything else cites an
ADR section or a `docs/duel-rules.md` heading. **A case whose expectation has no merged source is
not written** — it becomes a `DEC-NNN` for the **product owner** and the case waits.

**§5 makes an authored suite provisional.** It carries one line, and the round record that first
runs it deletes that line:

> **Provisional** — authored YYYY-MM-DD from merged sources, not yet run (`ADR-0090` §5).

That string is already merged **verbatim** in two places — `ADR-0090` §5 and `docs/test-plan.md`
§*Per-epic suites*. The skill file is the third copy and the one an author actually copies from, so
the ticket pins it as a literal. See the ticket's *Tests* table for what that gate can and cannot
see; in short, it can see that the marker is present in the template, and it cannot see that any
particular suite's marker is *true*.

**Nothing else under `.claude/` changes, and neither does `docs/test-plan.md`.** `ADR-0090`'s PR
already amended the test plan and `qa-cycle`'s condition-**b** bullet. Re-editing either here would
be churn against a merged file.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-120301](../tasks/TASK-120301-the-qa-cases-skill-file.md) | Create the qa-cases skill, whose terminal act is a printed command | ready |

**One ticket, because it is one file.** The deliverable is a single markdown document under 120
lines; splitting it would produce a merged intermediate state in which a licensed skill exists
without the prohibitions that license it — which is worse than a slightly larger ticket, and is not
what a split is for.

## Acceptance criteria

- [ ] `.claude/skills/qa-cases/SKILL.md` exists, and it is the only file this story adds under
      `.claude/`.
- [ ] `ADR-0090` §2's command exits 0, and the set of files under `.claude/skills` and
      `.claude/agents` naming `qa-cycle` is **exactly** the three that ADR declares.
- [ ] The four §3 prohibitions are written in the file, in a section of their own, in the words a
      later reader would have to delete rather than merely fail to add.
- [ ] The file states §4's rule that a case with no merged source is not written and becomes a
      `DEC` for the product owner, and carries §5's `Provisional` line verbatim.
- [ ] `python3 .github/scripts/lint_tickets.py` exits 0.

## Out of scope

- **Running `/qa-cases`.** This story builds the skill. The first authoring pass —
  `EPIC-04`, `EPIC-05`, `EPIC-06` — is the human typing the command afterwards, and it will produce
  its own story under whichever epic owns the suite.
- **Writing any catalogue case.** Not one row of `docs/test-plan.md` is touched here. A case
  written by this story would be a case written without §4's sourcing pass.
- **Editing `docs/test-plan.md` or `.claude/skills/qa-cycle/SKILL.md`.** Both were amended by
  `ADR-0090`'s PR (`8dfebbc1`) and are already correct.
- **Any change to `.claude/agents/qa-manager.md`.** `ADR-0090` §2: it names the cycle **nowhere
  today and gains no licence to**. Adding a mention would fail this story's own allow-list gate.
- **A `verify:` block anywhere that waits on a QA case.** `ADR-0089` §2b forbids it; every gate in
  this story is static and reads committed files.
- **Ticking any `EPIC-12` Definition-of-done box.** None of them asks for this skill, and the one
  about telling a harness defect from a product defect still needs a round.
