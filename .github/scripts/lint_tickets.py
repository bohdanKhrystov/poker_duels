#!/usr/bin/env python3
"""Validate the ticket backlog under tasks/.

Checks structure, not prose: IDs match filenames, parents exist and are one level up,
statuses are real, dependencies resolve, and nothing is orphaned. Run with no arguments
from the repository root.

Exits 0 when the backlog is well formed, 1 otherwise.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

TASKS = Path("tasks")
SKIP_DIRS = {"templates"}

STATUSES = {"backlog", "ready", "in-progress", "in-review", "blocked", "done"}
ESTIMATES = {"S", "M"}

ID_PATTERNS = {
    "epic": re.compile(r"^EPIC-\d{2}$"),
    "story": re.compile(r"^STORY-\d{4}$"),
    "task": re.compile(r"^TASK-\d{6}$"),
}
DIR_FOR_TYPE = {"epic": "epics", "story": "stories", "task": "tasks"}
LIST_RE = re.compile(r"^\[(.*)\]$")

errors: list[str] = []


def fail(where: str, message: str) -> None:
    errors.append(f"{where}: {message}")


def parse_frontmatter(path: Path) -> dict[str, object] | None:
    """Parse the flat YAML frontmatter our tickets use.

    Deliberately not a YAML parser: the schema is a fixed set of scalars and simple
    inline lists, and depending on PyYAML in CI to read six keys is not worth it.
    """
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        fail(str(path), "missing frontmatter")
        return None
    end = text.find("\n---\n", 4)
    if end == -1:
        fail(str(path), "unterminated frontmatter")
        return None

    data: dict[str, object] = {}
    for lineno, line in enumerate(text[4:end].splitlines(), start=2):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        if ":" not in line:
            fail(str(path), f"line {lineno}: not a key/value pair: {line!r}")
            continue
        key, _, raw = line.partition(":")
        key, raw = key.strip(), raw.strip()
        match = LIST_RE.match(raw)
        if match:
            inner = match.group(1).strip()
            data[key] = [v.strip() for v in inner.split(",") if v.strip()] if inner else []
        else:
            data[key] = raw
    return data


def collect() -> dict[str, dict]:
    tickets: dict[str, dict] = {}
    for path in sorted(TASKS.rglob("*.md")):
        rel = path.relative_to(TASKS)
        if rel.parts[0] in SKIP_DIRS or len(rel.parts) == 1:
            continue  # templates, and README.md / BOARD.md at the root

        where = str(path)
        data = parse_frontmatter(path)
        if data is None:
            continue

        for field in ("id", "title", "type", "status"):
            if not data.get(field):
                fail(where, f"missing required field '{field}'")
        ticket_type = data.get("type")
        ticket_id = data.get("id")
        if not isinstance(ticket_type, str) or ticket_type not in ID_PATTERNS:
            fail(where, f"type must be one of {sorted(ID_PATTERNS)}, got {ticket_type!r}")
            continue
        if not isinstance(ticket_id, str) or not ID_PATTERNS[ticket_type].match(ticket_id):
            fail(where, f"id {ticket_id!r} does not match the {ticket_type} format")
            continue

        if ticket_id in tickets:
            fail(where, f"duplicate id, already used by {tickets[ticket_id]['path']}")
            continue
        if not path.name.startswith(f"{ticket_id}-"):
            fail(where, f"filename does not start with its id {ticket_id!r}")
        expected_dir = DIR_FOR_TYPE[ticket_type]
        if rel.parts[0] != expected_dir:
            fail(where, f"a {ticket_type} belongs in tasks/{expected_dir}/, found in {rel.parts[0]}/")

        status = data.get("status")
        if status not in STATUSES:
            fail(where, f"status {status!r} is not one of {sorted(STATUSES)}")

        if ticket_type == "task":
            estimate = data.get("estimate")
            if estimate not in ESTIMATES:
                fail(where, f"estimate must be S or M (L means split the task), got {estimate!r}")

        parent = data.get("parent")
        if ticket_type == "epic" and parent:
            fail(where, "an epic must not declare a parent")
        if ticket_type in ("story", "task") and not parent:
            fail(where, f"a {ticket_type} must declare a parent")

        for field in ("labels", "depends_on"):
            if field in data and not isinstance(data[field], list):
                fail(where, f"'{field}' must be a list, e.g. [a, b]")

        data["path"] = where
        tickets[ticket_id] = data
    return tickets


def check_links(tickets: dict[str, dict]) -> None:
    parent_type = {"story": "epic", "task": "story"}
    for ticket_id, data in tickets.items():
        where = data["path"]
        parent = data.get("parent")
        if parent:
            if parent not in tickets:
                fail(where, f"parent {parent} does not exist")
            else:
                expected = parent_type[data["type"]]
                if tickets[parent]["type"] != expected:
                    fail(where, f"parent {parent} must be an {expected}")
                elif data["type"] == "task" and not ticket_id.startswith(parent.replace("STORY-", "TASK-")):
                    fail(where, f"id {ticket_id} does not encode its parent {parent}")
                elif data["type"] == "story" and not ticket_id.startswith(parent.replace("EPIC-", "STORY-")):
                    fail(where, f"id {ticket_id} does not encode its parent {parent}")

        for dep in data.get("depends_on", []):
            if dep not in tickets:
                fail(where, f"depends_on references unknown ticket {dep}")
            elif dep == ticket_id:
                fail(where, "a ticket cannot depend on itself")

        # 'ready' on a task means an agent can start it right now, so its dependencies must
        # already be done. On an epic or a story it only means "specified", which says
        # nothing about whether the work can start yet.
        if data["type"] == "task" and data["status"] == "ready":
            unmet = [
                dep for dep in data.get("depends_on", [])
                if dep in tickets and tickets[dep]["status"] != "done"
            ]
            if unmet:
                fail(where, f"status is 'ready' but these are not done: {', '.join(unmet)}")


def main() -> int:
    if not TASKS.is_dir():
        print("tasks/ not found — run from the repository root", file=sys.stderr)
        return 1

    tickets = collect()
    check_links(tickets)

    counts = {t: sum(1 for d in tickets.values() if d["type"] == t) for t in ID_PATTERNS}
    if errors:
        print(f"{len(errors)} problem(s) in the backlog:\n", file=sys.stderr)
        for error in errors:
            print(f"  {error}", file=sys.stderr)
        return 1

    print(
        f"backlog ok — {counts['epic']} epics, {counts['story']} stories, {counts['task']} tasks"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
