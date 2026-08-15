# ADR-0042 — The run reports itself every two hours, over Telegram

- **Status:** Accepted
- **Date:** 2026-08-15

## Context

`/build-epic` is designed to run unattended for hours. Everything about it optimises for *not*
stopping: a blocked epic moves to the next epic, a product decision goes to an agent rather than
to the human, and the skill explicitly forbids pausing for approval already granted. That is the
right design, and it has one consequence nobody chose — **the human has no idea what is
happening.** The run is silent from the moment it starts until the moment it prints a final
report, and the two states *working through ticket 40 of 60* and *dead since 11pm* look
identical from outside.

The failure that motivated this is specific. A usage limit does not warn you and let you tidy
up: it terminates the turn, and every turn after it, until the reset. `build-epic` already
answers half of that with an armed resume cron — but the human only learns the run stalled by
opening the terminal, and even then cannot tell whether the resume was armed *before* the
budget went, which is the difference between "it will pick itself up" and "it is over until I
touch it".

So the run owes the human four reports, and they are not the same report:

- **the heartbeat** — every two hours while working: what landed, what is in flight, what is
  blocked
- **the stop** — the run ended, and why
- **the block** — something needs the human, and the run is parked on it
- **the budget** — tokens are running short, **and whether the resume cron was armed**

The fourth is the one with a hard constraint. It is the report most likely to be owed at exactly
the moment the agent is least able to write one, so its mechanism cannot be *"the agent
remembers to send it"*.

Three things are genuinely undecided: what carries a message, what composes one, and what fires
the clock.

## Decision

**A message is one HTTPS call to the Telegram Bot API**, made by `scripts/notify/`, written in
Python 3 with the standard library only — the same tooling constraint `.github/scripts/lint_tickets.py`
already lives under, so CI needs nothing new to run it.

**A notifier never fails its caller.** Every entry point exits 0 whether or not it could send —
unconfigured, offline, rate-limited, all the same. A notification that breaks a build is a worse
outcome than a notification that is missed, and this code will run from git hooks and CI where
a non-zero exit has consequences far out of proportion to a status message. `notify doctor` is
the one exception: it exists to be loud, and exits non-zero when the channel is broken.

**Credentials resolve from the environment first, then from a machine-local file** —
`~/.claude/poker-duels/telegram.env`, mode 600, outside the repository. The token is never
committed, never logged, and never echoed: every code path that could print it goes through a
redactor, and the redactor is tested against the report body, the error path and the doctor
output rather than trusted.

**A report is composed from repository state, not from the agent's memory.** `tasks/BOARD.md`
supplies statuses, `git log` supplies what landed, `gh pr list` supplies what is in flight, and
the ticket files supply what is blocked and on which `DEC-NNN`. The agent contributes only a
gitignored breadcrumb — `.claude/run-state.json` — holding the few facts the repository cannot
know: which epic list is being worked, when the last report went out, and **whether the resume
cron was armed**.

That split is the load-bearing part of this decision. It means a status report can be produced
by anything able to run a script, and specifically by something that is *not* the agent whose
budget just ran out.

**The clock is an in-session recurring cron**, armed at the start of a run alongside the resume
job that `build-epic` already schedules, firing every two hours. The heartbeat deduplicates on
the last-sent stamp in the run state, so an agent that has just reported by hand does not
produce a second message when the job fires ten minutes later.

**Lifecycle reports are hooks, not intentions.** The stop report is a `Stop` hook, executed by
the harness rather than by the agent, because the case worth reporting is exactly the case where
the agent has nothing left to say. The hook *registration* lives in `.claude/settings.local.json`
— `.claude/settings.json` is deny-listed to the agent by this repository's own permissions, and
that rule is not worth weakening for a notifier. The hook *logic* lives in a versioned script
under `scripts/notify/`, so the process trail keeps everything except the one line that wires it
up. `docs/notifications.md` carries that line, so a fresh machine is one paste away from working.

## Consequences

**The heartbeat dies with the session.** A cron job here is session-only: nothing is written to
disk, and the job dies when Claude Code is quit or the machine reboots. It survives a usage
limit, which leaves the process running — the case this ADR was written for — but it does not
survive the human closing the terminal. A launchd agent would have survived all of it. The human
chose the session-only mechanism on 2026-08-15, weighing a daemon installed on the workstation
against a class of missed report, and this is the cost side of that choice, recorded rather than
discovered later. `STORY-1104` is the un-taken option, left written down.

**A run now has an outbound network dependency it did not have.** Every report is a call to
`api.telegram.org` from the workstation. It is failure-tolerant by construction, but the project
went from *"builds with no network"* to *"builds with no network, and quietly tells you nothing"*.

**Silence is now ambiguous in a new way.** Because notifiers never fail loudly, a misconfigured
channel looks exactly like a quiet run. `notify doctor` is the mitigation and the reason it exits
non-zero; the setup document tells the human to run it once and after any credential change.

**The richest inputs to a report are invisible to CI.** `.claude/run-state.json` is gitignored,
so the armed-cron flag and the epic list exist on one machine only. Any second reporter — a
scheduled GitHub workflow, say — can report what was pushed and nothing more.

**The token is now a thing this project has.** A leaked bot token lets a stranger post into the
human's chat. It cannot read the repository, cannot read other chats, and is revoked by one
`/revoke` to @BotFather, so the blast radius is bounded and recoverable — but it is a real
secret in a project that previously had none, and `.gitignore`, the redactor and the 600 file
mode are all load-bearing rather than tidy.

## Alternatives considered

**A launchd agent on the workstation.** Survives everything the cron does not: usage limits,
crashes, quitting Claude Code, reboots. Rejected by the human on 2026-08-15 in favour of
installing nothing on the machine. It is also not available to the agent unilaterally —
`launchctl` is on this repository's deny list — so it could only ever have been a plist the
human loads by hand. Kept as `STORY-1104`, unstarted, so the option is one command away rather
than a rediscovery.

**A scheduled GitHub Actions workflow.** Survives the workstation being asleep or offline, and
needs no local install at all. Rejected as the *primary* mechanism because it can only see
pushed state: it cannot know whether the agent is alive, whether the resume cron was armed, or
what is sitting uncommitted in a worktree — which is most of what the human actually wants to
know. Viable later as a coarse backstop, and cheap to add, because the report composer already
takes its inputs from the repository.

**Agent-composed reports only, with no script.** Richer prose, no new code, and it is what an
agent does naturally. Rejected on the one requirement that matters: it cannot produce the budget
report, because the agent that would write it is the thing that has stopped.

**Slack, email or SMS.** Telegram was named by the human. Nothing in `scripts/notify/` is
Telegram-shaped above the transport module, so a second channel is one file, but no second
channel is built until one is asked for.

**Markdown-formatted messages.** Telegram's MarkdownV2 requires escaping sixteen characters,
several of which appear constantly in ticket ids, file paths and diff summaries, and a single
missed escape fails the whole send with an opaque 400. Messages are sent as plain text with no
`parse_mode`, so nothing in a report can ever break its own delivery.
