---
schema: 2
id: TASK-110101
title: Resolve credentials, and redact the token from everything
type: task
status: done
parent: STORY-1101
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: []
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_credentials.py
  - grep -c "    def test_" scripts/notify/test_credentials.py
---

## Goal

`scripts/notify/credentials.py` finds the bot token and chat id, and no other module in this
tree ever has to know where they came from or how to hide them.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/credentials.py` | create |
| `scripts/notify/test_credentials.py` | create |

## Scope

- `resolve(env, path)` returns a `Credentials(token, chat_id)` or `None`. Environment first —
  `TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` — then the file at `path`, defaulting to
  `~/.claude/poker-duels/telegram.env`.
- The file format is `KEY=value` lines. Blank lines and `#` comments are skipped; surrounding
  single or double quotes on a value are stripped; an unparseable line is skipped, not fatal.
- A partial configuration is **not** a configuration: a token with no chat id returns `None`.
  Both halves are needed to send, and half a credential set is a misconfiguration to be reported
  by `doctor`, not a thing to fail on mid-send.
- `redact(text, token)` replaces every occurrence of the token with `***`, and also replaces the
  token's colon-separated bot-id prefix — Telegram error bodies echo it back on their own.
- `redact` is a no-op on an empty or `None` token rather than replacing every empty string in
  the input, which would corrupt the message.

## Out of scope

- Reading the Keychain. The human chose the file on 2026-08-15; a second source is a new ticket.
- Writing the credential file. `docs/notifications.md` tells the human to create it —
  `TASK-110103`.
- Any network call — `TASK-110102`.

## Tests

`test_credentials.py`

| Test | Proves |
| --- | --- |
| `test_environment_wins_over_file` | both present, the environment's values are returned |
| `test_falls_back_to_the_file` | environment empty, the file's values are returned |
| `test_missing_file_is_not_an_error` | a path that does not exist returns `None`, raises nothing |
| `test_comments_and_blank_lines_are_skipped` | a file with both still parses |
| `test_quoted_values_are_unquoted` | `TELEGRAM_CHAT_ID="123"` yields `123`, not `"123"` |
| `test_token_without_chat_id_is_not_a_configuration` | returns `None` rather than a half-filled record |
| `test_redact_removes_the_token` | the token does not appear in the output |
| `test_redact_removes_the_bot_id_prefix` | `123456789:` alone is masked, as Telegram echoes it |
| `test_redact_of_empty_token_leaves_text_alone` | an empty token does not turn every gap into `***` |

## Acceptance criteria

- [ ] `test_environment_wins_over_file` passes
- [ ] `test_falls_back_to_the_file` passes
- [ ] `test_missing_file_is_not_an_error` passes
- [ ] `test_comments_and_blank_lines_are_skipped` passes
- [ ] `test_quoted_values_are_unquoted` passes
- [ ] `test_token_without_chat_id_is_not_a_configuration` passes
- [ ] `test_redact_removes_the_token` passes
- [ ] `test_redact_removes_the_bot_id_prefix` passes
- [ ] `test_redact_of_empty_token_leaves_text_alone` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
