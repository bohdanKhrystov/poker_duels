# ADR-0006 — Every task ends in a reviewed, merged pull request

- **Status:** Accepted
- **Date:** 2026-08-10

## Context

[`ADR-0004`](ADR-0004-branching-and-ticket-workflow.md) established that nothing reaches an
integration branch except through a pull request. It did not say what happens *inside* that
pull request, and on a project where most code is written by agents that gap matters more than
it would on a team of people.

Two failure modes were visible almost immediately.

**The first is that "done" drifts.** An agent finishes writing code, reports the task complete,
and moves on — leaving a branch that was never pushed, or a PR that was never merged. The
ticket says `done`, `develop` has never seen the work, and the discrepancy is invisible until
someone tries to build on it. This happened on the very first task in this repository.

**The second is that self-review is nearly worthless.** The agent that wrote a diff is the
worst available reviewer of it. It already believes the approach is correct, it has lost track
of which parts it was unsure about, and it reads what it intended rather than what it wrote.
Asking it "does this look right?" reliably returns yes.

There is a third, quieter risk: a permissive permission model (see
[`docs/workflow.md`](../workflow.md#permissions)) means an agent can do a great deal without
being interrupted. That is a deliberate trade, and it is only defensible if there is a real
gate somewhere further down.

## Decision

**A task is finished when its pull request is merged into `develop`. Not when the code is
written.**

There is no state in which a task is "done except for the PR". While the PR is open, the ticket
is `in-review`, and the next task does not begin.

**Every pull request is reviewed by `/code-review` before it merges. No exceptions** — not for
documentation, not for one-line changes, not when the author is confident.

The reviewer works from the diff, not from the intent. It did not write the code and holds no
belief about what the code was supposed to do.

Findings are handled by a fixed rule:

| Finding | Action |
| --- | --- |
| Real defect in this diff | Fix in this PR, push, review again |
| Real problem outside the ticket's scope | New ticket in `backlog`. Do **not** fix here |
| Disagreement | State the reason in the PR. Never silently ignore |

**A person presses merge.** An agent may open the PR, run the review, fix findings and push
again, but the squash-merge button stays with the human. It is one decision per ticket, taken
on a diff that is already reviewed and already green — the cheapest checkpoint available that
is still a real one.

A clean review is a normal outcome. A *skipped* review is a process failure and is recorded as
one in the metrics, because a rule that is quietly dropped when inconvenient was never a rule.

## Consequences

**Gained**

- "Done" means one thing, and it is externally observable: the commit is on `develop`.
- Every line that reaches an integration branch has been read by something with no stake in it.
- A permissive permission model becomes defensible, because the gate it relies on is real.
- The metrics in `tasks/BOARD.md` — first-pass acceptance rate, review iterations — become
  measurable rather than self-reported, which is what makes them worth anything to the case
  study.

**Cost**

- Every ticket, however small, carries branch, PR, review and merge overhead. On a
  three-line documentation fix that overhead exceeds the work itself.
  Accepted deliberately: the alternative is a judgement call about which changes are "small
  enough to skip", and that judgement is exactly what erodes. A rule with exceptions is a
  preference.
- Review costs tokens on every task. It is the highest-value place to spend them.
- The human is on the critical path once per ticket. This is a feature; if it becomes a
  bottleneck the correct response is smaller tickets, not fewer merges.

## Alternatives considered

- **Self-review by the implementing agent** — rejected. It is the cheapest option and the one
  that produces the least information, for the reasons above.
- **Review only above a size threshold** — rejected. Defect density does not track diff size,
  and any threshold turns into an argument at the boundary.
- **Batch several tickets into one reviewed PR** — rejected. It breaks the one-ticket-one-
  revertable-commit property from ADR-0004, and it makes the review diff large enough that
  attention thins exactly where it is needed.
- **A second implementing agent reviews instead of `/code-review`** — kept in reserve. Worth
  trying on the engine's correctness-critical stories (the evaluator, betting rules), where a
  poker-specific reviewer may catch what a general one cannot. Not the default.
