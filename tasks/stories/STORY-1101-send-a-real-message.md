---
id: STORY-1101
title: Send a real message
type: story
status: done
parent: EPIC-11
labels: [process, notifications]
depends_on: []
---

## Goal

`scripts/notify/` can put arbitrary text on the human's phone, and can prove it did. Credentials
come from the environment or from a file outside the repository, the token never appears in any
output, and nothing that calls a notifier can be broken by one.

## Why

Everything else in the epic composes text. This is the only story that delivers it, so it goes
first and the rest is untestable until it lands.

## Design notes

Settled by [`ADR-0042`](../../docs/adr/ADR-0042-the-run-reports-itself-every-two-hours.md), and
the tasks may not revisit any of it:

- **Python 3, standard library only.** `urllib.request`, no `requests`, no `curl` subprocess.
  The same constraint `.github/scripts/lint_tickets.py` runs under, so CI needs nothing new.
- **Plain text, no `parse_mode`.** MarkdownV2 needs sixteen characters escaped and a single miss
  fails the send with an opaque 400. Ticket ids and file paths contain several of them.
- **Never fail the caller.** Every entry point exits 0 whether or not it sent. `doctor` is the
  single exception and exists to be loud.
- **Credential order is environment, then `~/.claude/poker-duels/telegram.env`.** Never the
  repository, in either direction.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-110101](../tasks/TASK-110101-credentials-resolve-and-redact.md) | Resolve credentials, and redact the token from everything | ready |
| [TASK-110102](../tasks/TASK-110102-send-one-telegram-message.md) | Send one message to the Telegram Bot API | ready |
| [TASK-110103](../tasks/TASK-110103-notify-cli-and-doctor.md) | The `notify` CLI, and a `doctor` that proves the channel | ready |
| [TASK-110104](../tasks/TASK-110104-the-suite-runs-in-ci.md) | Run the notifier suite in CI | ready |

## Acceptance criteria

- [ ] A message sent by `notify.py send` arrives in the human's Telegram chat.
- [ ] `notify.py doctor` exits non-zero with a named cause when the channel is not configured,
      and 0 when it is.
- [ ] No output path — success, failure, or doctor — contains the bot token.
- [ ] `notify.py send` exits 0 with no credentials present.

## Out of scope

- Composing any actual report — `STORY-1102`.
- Scheduling anything — `STORY-1103`.
- A second channel. The transport module is the only Telegram-shaped file in the tree, which is
  what keeps a second channel to one file, but no second channel is built.
