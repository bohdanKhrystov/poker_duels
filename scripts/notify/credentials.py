"""Where the Telegram credentials come from, and how the token stays out of everything else.

Environment first, then a file outside the repository. Both halves or nothing: half a credential
set is a misconfiguration for ``doctor`` to name, not something to discover mid-send.

Standard library only, per ADR-0042 — the same constraint ``.github/scripts/lint_tickets.py``
runs under, so CI needs nothing new to run this.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

TOKEN_KEY = "TELEGRAM_BOT_TOKEN"
CHAT_KEY = "TELEGRAM_CHAT_ID"

DEFAULT_PATH = Path.home() / ".claude" / "poker-duels" / "telegram.env"

MASK = "***"


@dataclass(frozen=True)
class Credentials:
    """A complete pair. A half-filled one is never constructed — see :func:`resolve`."""

    token: str
    chat_id: str


def read_env_file(path) -> dict:
    """Parse ``KEY=value`` lines.

    An unreadable file is an empty result rather than an error: a machine that has not been
    set up yet must still be able to run everything that calls this.
    """
    values: dict = {}
    try:
        text = Path(path).read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return values

    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if not separator:
            continue
        key = key.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        if key:
            values[key] = value
    return values


def resolve(env=None, path=None):
    """Return :class:`Credentials`, or ``None`` when either half is missing."""
    env = os.environ if env is None else env
    path = DEFAULT_PATH if path is None else path

    token = (env.get(TOKEN_KEY) or "").strip()
    chat_id = (env.get(CHAT_KEY) or "").strip()

    if not (token and chat_id):
        from_file = read_env_file(path)
        token = token or (from_file.get(TOKEN_KEY) or "").strip()
        chat_id = chat_id or (from_file.get(CHAT_KEY) or "").strip()

    if token and chat_id:
        return Credentials(token=token, chat_id=chat_id)
    return None


def redact(text, token) -> str:
    """Mask the token, and the bot-id prefix Telegram echoes back in its own error bodies.

    A falsy token is a no-op: replacing the empty string would insert the mask between every
    character of the message.
    """
    if not token:
        return str(text)

    out = str(text).replace(token, MASK)
    bot_id, separator, _ = token.partition(":")
    if separator and bot_id:
        out = out.replace(f"{bot_id}:", f"{MASK}:").replace(bot_id, MASK)
    return out
