---
id: STORY-1104
title: A heartbeat that outlives the session
type: story
status: backlog
parent: EPIC-11
labels: [process, notifications]
depends_on: [STORY-1103]
---

## Goal

Reports keep arriving after Claude Code is quit, crashes, or the machine reboots — a launchd
agent running the same composer on the same two-hour clock, deduplicated against the in-session
heartbeat so the human never receives two.

## Why

Not now. This story exists **written and unstarted on purpose.**

[`ADR-0042`](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md) records the
choice: on 2026-08-15 the human weighed a daemon installed on the workstation against a class of
missed report, and chose to install nothing. The in-session cron survives a usage limit — the
case the epic was written for, because a limit leaves the process running — but not a quit, a
crash or a reboot.

Keeping the design written down means reversing that judgement costs one command instead of one
rediscovery. Deleting it would lose the reasoning along with the plist.

## Design notes

- `~/Library/LaunchAgents/com.pokerduels.status.plist`, `StartInterval` 7200, running
  `notify.py heartbeat` with the repository path baked in.
- **The agent installs nothing.** `launchctl` is on this repository's deny list, deliberately.
  The tasks produce a plist and an installer the *human* runs; anything that loads it on their
  behalf is out of scope and would be a reason to reject the PR.
- launchd does not read `~/.zshrc`, so the credential file — not an exported variable — is what
  makes this work at all. That is already the shape `STORY-1101` chose.
- Deduplication is the existing last-sent stamp, unchanged. Two clocks, one window, one message.
- A launchd agent that cannot reach the repository must log and exit 0, never accumulate failed
  starts.

## Tasks

Not yet written. Run `/plan-story STORY-1104` if this is ever started — writing the tickets now
would be planning work that the story exists specifically not to do.

## Acceptance criteria

- [ ] A report arrives on the two-hour clock with Claude Code fully quit.
- [ ] The in-session heartbeat and the launchd heartbeat never both send inside one window.
- [ ] Uninstalling is one documented command, and leaves no file behind.

## Out of scope

- Replacing the in-session heartbeat. This is a floor beneath it, not a substitute: the
  in-session report is richer because the agent is alive to write it.
