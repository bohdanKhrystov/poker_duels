"""One HTTPS call to the Telegram Bot API.

Raises nothing and reveals nothing. Plain text with no ``parse_mode``, per ADR-0042: MarkdownV2
needs sixteen characters escaped and a single miss fails the send with an opaque 400 — and ticket
ids, file paths and diff summaries contain several of them.
"""

from __future__ import annotations

import json
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass

import credentials as credentials_module

API_ROOT = "https://api.telegram.org"

#: Telegram rejects anything longer than this, so truncation happens here rather than at the API.
LIMIT = 4096
TRUNCATION_MARKER = "\n… [truncated]"


@dataclass(frozen=True)
class Result:
    ok: bool
    detail: str


def clamp(text, limit=LIMIT) -> str:
    """Cut to ``limit`` *including* the marker, so the result can never exceed the limit."""
    text = str(text)
    if len(text) <= limit:
        return text
    return text[: limit - len(TRUNCATION_MARKER)] + TRUNCATION_MARKER


def send(creds, text, timeout=10, opener=None) -> Result:
    """POST one message. Every failure becomes a ``Result``; nothing propagates to the caller."""
    opener = urllib.request.urlopen if opener is None else opener

    url = f"{API_ROOT}/bot{creds.token}/sendMessage"
    body = urllib.parse.urlencode({"chat_id": creds.chat_id, "text": clamp(text)}).encode("utf-8")
    request = urllib.request.Request(url, data=body, method="POST")
    request.add_header("Content-Type", "application/x-www-form-urlencoded")

    try:
        with opener(request, timeout=timeout) as response:
            payload = response.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as error:
        # The URL carries the token and HTTPError quotes it, so this path must be redacted too.
        try:
            error_body = error.read().decode("utf-8", "replace")
        except Exception:  # noqa: BLE001 — a already-failing send may not fail again
            error_body = ""
        detail = f"HTTP {error.code} {getattr(error, 'url', '')} {error_body}".strip()
        return Result(False, credentials_module.redact(detail, creds.token))
    except Exception as error:  # noqa: BLE001 — a status message may never raise
        detail = f"{type(error).__name__}: {error}"
        return Result(False, credentials_module.redact(detail, creds.token))

    try:
        parsed = json.loads(payload)
    except ValueError:
        return Result(False, credentials_module.redact(f"unparseable response: {payload}", creds.token))

    if parsed.get("ok"):
        return Result(True, "sent")
    described = parsed.get("description") or payload
    return Result(False, credentials_module.redact(str(described), creds.token))
