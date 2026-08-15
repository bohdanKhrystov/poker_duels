---
schema: 2
id: TASK-110103
title: The notify CLI, and a doctor that proves the channel
type: task
status: done
parent: STORY-1101
estimate: S
tier: haiku
review: standard
files_touched: 3
labels: [process, notifications]
depends_on: [TASK-110102]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_cli.py
  - python3 scripts/notify/notify.py send "verify probe"
  - grep -c "    def test_" scripts/notify/test_cli.py
---

## Goal

One command sends a message, and one command says whether sending would work — loudly enough to
be worth running.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/notify.py` | create |
| `scripts/notify/test_cli.py` | create |
| `docs/notifications.md` | create |

## Scope

- `notify.py send <text…>` sends its arguments, or stdin when none are given. **Exits 0 always**,
  per `ADR-0042` — unconfigured, offline, rejected, all the same. It prints one line saying what
  happened, so a human running it by hand is not left guessing.
- `notify.py doctor` is the one entry point that exits non-zero, and it names the cause it found:
  no credential file, file present but unreadable, half a credential set, `getMe` rejected the
  token, or the send itself failed. It sends a real message on success — a doctor that only
  checks configuration proves nothing about the wire.
- An unknown subcommand prints usage and exits 2. No subcommand does the same.
- `docs/notifications.md` takes a fresh machine from nothing to a delivered message: the
  @BotFather steps, the "message the bot first, it cannot open a chat with you" trap, the
  `getUpdates` command that reveals the chat id, the credential file's path, contents and
  600 mode, and `doctor` as the check.

## Out of scope

- The `Stop` hook registration line — `TASK-110303` adds it to this same document.
- Composing a status report — `STORY-1102`. `send` takes text and asks no questions.
- Any scheduling — `STORY-1103`.

## Tests

`test_cli.py`

| Test | Proves |
| --- | --- |
| `test_send_exits_zero_with_no_credentials` | the documented never-fail-the-caller contract |
| `test_send_reads_stdin_when_no_arguments` | piping a report into it works |
| `test_send_joins_argument_words` | `send a b c` sends `a b c`, not `a` |
| `test_doctor_exits_non_zero_when_unconfigured` | doctor is the loud one |
| `test_doctor_names_the_missing_piece` | its output distinguishes no-file from half-a-config |
| `test_doctor_exits_zero_when_the_send_succeeds` | success is a real send, not a config check |
| `test_unknown_subcommand_exits_two` | typos do not silently do nothing |

## Acceptance criteria

- [ ] `test_send_exits_zero_with_no_credentials` passes
- [ ] `test_send_reads_stdin_when_no_arguments` passes
- [ ] `test_send_joins_argument_words` passes
- [ ] `test_doctor_exits_non_zero_when_unconfigured` passes
- [ ] `test_doctor_names_the_missing_piece` passes
- [ ] `test_doctor_exits_zero_when_the_send_succeeds` passes
- [ ] `test_unknown_subcommand_exits_two` passes
- [ ] `docs/notifications.md` contains the credential file path, its 600 mode, and the
      `getUpdates` command
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
