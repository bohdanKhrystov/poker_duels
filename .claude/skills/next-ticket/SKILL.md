---
name: next-ticket
description: Run exactly one ticket end to end — dispatch a coder, run its verify commands, review the diff, open a PR and merge it. Use when the user wants a single ticket done, or when driving tickets one per session. For a whole epic unattended, use build-epic instead.
---

# Run one ticket

Takes a ticket ID as argument. With no argument, picks the first **startable** ticket:
`type: task`, `status: ready`, and every `depends_on` already `done`.

**Your own context must stay small.** You are a driver, not a participant. Do not read source
files, do not read the ticket's linked documents, do not review the code yourself. Dispatch,
check exit codes, move on.

---

## 1. Select

```sh
python3 .github/scripts/lint_tickets.py --startable
```

Read **only** the chosen ticket file. If it has no `schema: 2`, stop: its story has not been
planned yet. Tell the user to run `/plan-story <STORY-ID>` first.

If nothing is startable, report that and stop.

## 2. Branch

```sh
git switch develop && git pull --ff-only
git switch -c <type>/<TICKET-ID>-<slug>
```

`<type>` is `feature`, `fix`, `chore`, `docs`, `test` or `refactor`.

Set the ticket's `status: in-progress`.

## 3. Code

Dispatch the **coder** subagent with `model` set from the ticket's `tier`. Pass it the ticket
path and nothing else — it reads the ticket itself.

> Implement the ticket at `tasks/tasks/<TICKET-ID>-<slug>.md`. Read that ticket and only the
> files it names. Run its `verify` commands. Report in the format your instructions specify.

Do not paste the ticket's contents into the prompt. The subagent reads the file; sending it
twice pays for it twice.

## 4. Verify — the objective gate

Run the ticket's `verify` commands yourself. **Do not trust the coder's report.**

```sh
# each command from the ticket's verify: block
```

All exit 0 → continue.

**Any failure → retry policy:**

| Attempt | Action |
| --- | --- |
| 1st failure | Re-dispatch the coder at the same tier with the failing output |
| 2nd failure | **Promote**: set the ticket's `tier: sonnet`, re-dispatch at Sonnet |
| 3rd failure | Stop. Set `status: blocked`, record why in the ticket, move on |

Commit the `tier` change with the work, so the PR shows which tickets Haiku could not handle.
That record is the point — it is data for the case study, not an embarrassment to hide.

## 5. Review

Dispatch the **reviewer** subagent, scaled by the ticket's `review:` field:

| `review:` | Action |
| --- | --- |
| `light` | reviewer subagent (Haiku) |
| `standard` | reviewer subagent (Sonnet — pass `model: sonnet`) |
| `deep` | reviewer subagent (Sonnet), **and** `/code-review low` |

Never run `/code-review high` from here. It is a multi-agent fan-out that has cost 132k tokens
on a single documentation PR; it belongs to deliberate human-initiated review, not to a loop.

`VERDICT: fail` → hand the findings back to the coder, then re-verify and re-review. Two failed
review cycles → `status: blocked`, and move on.

## 6. Ship

Update the ticket to `status: done` and update `tasks/BOARD.md`. Commit everything together:

```
<type>(<scope>): <description> (<TICKET-ID>)
```

Then:

```sh
git push -u origin <branch>
gh pr create --base develop --fill
```

Wait for CI. When the `lint backlog` check is green **and** verify passed **and** the review
passed:

```sh
gh pr merge --squash --delete-branch
```

Auto-merge is deliberate and its reasoning is in
[`ADR-0007`](../../../docs/adr/ADR-0007-token-lean-agent-workflow.md). The gates that replace the
human are objective: verify exits 0, CI green, reviewer clean, one squashed commit per ticket so
any merge is one `git revert` away.

**Never merge with a red check, a failing verify command, or a failing review.** If any gate is
red, leave the PR open, set the ticket `blocked`, and report.

## 7. Promote dependents

Any `backlog` task whose `depends_on` are now all `done` becomes `ready`. Commit that with the
next ticket's work, not as a separate PR.

## 8. Report

Four lines, no more:

```
TICKET: <id> — <title>
RESULT: merged | blocked
PR: <url>
NEXT: <next startable ticket id, or "none">
```

---

## Stop and ask the human when

- The ticket needs a decision no ADR covers → register `DEC-NNN` in `docs/adr/README.md`, set
  the ticket `blocked`, report it. Do not guess.
- `verify` cannot pass without doing something the ticket puts out of scope.
- The work needs a new dependency.
- Three verify failures or two review failures.

In every case: **record it, block the ticket, and stop this run.** Never guess at a decision to
keep the loop moving — a wrong decision propagates into every ticket built on top of it.
