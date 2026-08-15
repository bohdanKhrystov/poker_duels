---
name: plan-story
description: Split one story into schema-2 micro-tickets with executable verify blocks, using the Opus planner. Run once per story, before its tickets are worked. Converts legacy tickets in place.
---

# Plan a story

Takes a story ID. Runs the **planner** subagent (Opus, high effort) once, then checks its output.

This is the only expensive step in the workflow, and it runs about once per story. Everything
downstream is cheap because this was done properly — so do not economise here.

---

## 1. Dispatch the planner

Give it the story ID and nothing else. It reads what it needs.

> Split `tasks/stories/<STORY-ID>-<slug>.md` into schema-2 micro-tickets, following your
> instructions and `tasks/templates/task.md`. Existing tickets under this story are legacy —
> re-split them to XS/S, add `verify` blocks, and delete any that no longer make sense.

## 2. Check the output

Do not accept it blindly. Verify mechanically:

```sh
python3 .github/scripts/lint_tickets.py
```

Then confirm by inspection of the frontmatter only — not the prose:

- [ ] every new ticket has `schema: 2`, `tier`, `review`, `files_touched`, and a non-empty `verify`
- [ ] every `estimate` is `XS` or `S` — **no `M`**
- [ ] `files_touched` ≤ 3 everywhere
- [ ] exactly one ticket is `status: ready`
- [ ] `depends_on` forms a chain, and no two startable tickets touch the same file

If a `verify` command references a Gradle module or task that does not exist yet, that is fine
when an earlier ticket creates it — but the dependency must be declared in `depends_on`.

## 3. Dry-run the verify commands

For the one `ready` ticket, run its `verify` commands now. They **should fail** — the work is not
done. What matters is *how*:

- a test failure or "no tests found" → correct, the command is runnable
- "task not found", "unknown option", a shell syntax error → the command is **wrong**, and the
  coder would be chasing a broken gate rather than writing code

Send anything in the second category back to the planner. A wrong `verify` command is the single
most expensive defect in this system: it makes a correct implementation look like a failure, and
the retry policy will burn three dispatches and a promotion before giving up.

## 4. Update the board and commit

Update `tasks/BOARD.md` with the new ticket list, then commit on a branch and open a PR the same
way any other change ships:

```
docs(tasks): split <STORY-ID> into micro-tickets (<STORY-ID>)
```

Planning is work, and work reaches `develop` through a reviewed PR like everything else.

## 5. Report

```
STORY: <id>
TICKETS: <n> created (<n> XS, <n> S) — <n> haiku, <n> sonnet
READY: <ticket id>
DECISIONS: <DEC-NNN raised, or none — say which are the architect's, which the
           product owner's, and which are the human's>
```

A raised `DEC-NNN` does not end the run. Route it: technical to the `architect` agent, product to
the `product-owner` agent (which derives its answer from `docs/vision.md`), and only a decision that
would *change* the vision — money, what the product is or is not, the roadmap's shape, risk with
consequences outside the software — to the human. The answering PR is merged without asking, like
every PR except one changing `docs/vision.md`; the tickets it unblocks start once it is **merged**,
not once it is written.
