"""Counts and lists from the backlog, read for a message rather than for correctness.

Deliberately not ``.github/scripts/lint_tickets.py``: that one exits the process on malformed
input, which is right for CI and fatal for a status message. A ticket this cannot read is
skipped, counted, and reported — never silently dropped, because a silent drop makes the numbers
wrong in a message nobody can check.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path

DECISION_RE = re.compile(r"DEC-\d{3}")


@dataclass(frozen=True)
class Blocked:
    id: str
    title: str
    decision: object  # str, or None when the ticket names no DEC


@dataclass
class Backlog:
    counts_by_status: dict = field(default_factory=dict)
    blocked: list = field(default_factory=list)
    epics: dict = field(default_factory=dict)
    skipped: int = 0

    def epic_status(self, epic_id):
        return self.epics.get(epic_id)


def _split_frontmatter(text):
    """Return ``(fields, body)`` or ``None``. The same flat YAML subset the linter reads."""
    if not text.startswith("---\n"):
        return None
    end = text.find("\n---\n", 4)
    if end == -1:
        return None

    data: dict = {}
    for line in text[4:end].splitlines():
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if line[0].isspace() or line.lstrip().startswith("-"):
            continue  # a nested list item, e.g. the entries under `verify:`
        key, separator, value = line.partition(":")
        if separator:
            data[key.strip()] = value.strip()
    return data, text[end + 5 :]


def scan(tasks_dir) -> Backlog:
    """Walk the backlog. A missing directory is an empty result, not an error."""
    backlog = Backlog()
    root = Path(tasks_dir)
    if not root.is_dir():
        return backlog

    for path in sorted(root.rglob("*.md")):
        relative = path.relative_to(root)
        if relative.parts[0] == "templates" or len(relative.parts) == 1:
            continue  # templates, and README.md / BOARD.md at the root

        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            backlog.skipped += 1
            continue

        parsed = _split_frontmatter(text)
        if parsed is None:
            backlog.skipped += 1
            continue

        data, body = parsed
        ticket_id, status, kind = data.get("id"), data.get("status"), data.get("type")
        if not ticket_id or not status or not kind:
            backlog.skipped += 1
            continue

        if kind == "epic":
            backlog.epics[ticket_id] = status
        elif kind == "task":
            backlog.counts_by_status[status] = backlog.counts_by_status.get(status, 0) + 1
            if status == "blocked":
                found = DECISION_RE.search(body)
                backlog.blocked.append(
                    Blocked(ticket_id, data.get("title", ""), found.group(0) if found else None)
                )

    return backlog
