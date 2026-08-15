---
schema: 2
id: TASK-110102
title: Send one message to the Telegram Bot API
type: task
status: done
parent: STORY-1101
estimate: S
tier: haiku
review: standard
files_touched: 2
labels: [process, notifications]
depends_on: [TASK-110101]
verify:
  - python3 -m unittest discover -s scripts/notify -t scripts/notify -p test_telegram.py
  - grep -c "    def test_" scripts/notify/test_telegram.py
---

## Goal

`scripts/notify/telegram.py` puts one piece of text in a Telegram chat, and reports what
happened without ever revealing the token.

## Files

| File | Action |
| --- | --- |
| `scripts/notify/telegram.py` | create |
| `scripts/notify/test_telegram.py` | create |

## Scope

- `send(creds, text, timeout=10, opener=None) -> Result(ok: bool, detail: str)`. One POST to
  `https://api.telegram.org/bot<token>/sendMessage`, form-encoded, `chat_id` and `text` only.
- **No `parse_mode`.** Plain text, per `ADR-0042`: MarkdownV2 needs sixteen characters escaped
  and a single miss fails the send with an opaque 400. Ticket ids and paths contain several.
- Text longer than 4096 characters is truncated to 4096 **including** the marker that says it
  was truncated, so the request can never be rejected for length.
- Every exception from the transport — `HTTPError`, `URLError`, socket timeout, anything else —
  becomes `Result(False, detail)`. `send` raises nothing.
- `detail` is passed through `credentials.redact` on **both** paths, because a `HTTPError` body
  from Telegram quotes the request URL, and the token is in the URL.
- The `opener` parameter is how the tests reach it: default `None` uses `urllib.request.urlopen`.

## Out of scope

- Retries. A missed status message is not worth a backoff loop, and the next heartbeat is two
  hours away rather than lost.
- Reading credentials — `TASK-110101` owns that; `send` is given them.
- Any CLI — `TASK-110103`.

## Tests

`test_telegram.py`

| Test | Proves |
| --- | --- |
| `test_posts_to_the_send_message_endpoint` | the URL contains `/sendMessage` and the token |
| `test_body_carries_chat_id_and_text` | the form body round-trips both fields |
| `test_no_parse_mode_is_sent` | `parse_mode` appears nowhere in the request body |
| `test_long_text_is_truncated_to_the_limit` | a 9000-character message produces a body at or under 4096 characters of text |
| `test_truncation_says_it_truncated` | the sent text ends with the truncation marker |
| `test_http_error_becomes_a_failed_result` | a raised `HTTPError` returns `ok=False`, raises nothing |
| `test_network_error_becomes_a_failed_result` | a raised `URLError` returns `ok=False`, raises nothing |
| `test_error_detail_never_contains_the_token` | the token string is absent from `detail` after an error whose body quotes the URL |

## Acceptance criteria

- [ ] `test_posts_to_the_send_message_endpoint` passes
- [ ] `test_body_carries_chat_id_and_text` passes
- [ ] `test_no_parse_mode_is_sent` passes
- [ ] `test_long_text_is_truncated_to_the_limit` passes
- [ ] `test_truncation_says_it_truncated` passes
- [ ] `test_http_error_becomes_a_failed_result` passes
- [ ] `test_network_error_becomes_a_failed_result` passes
- [ ] `test_error_detail_never_contains_the_token` passes
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md).
