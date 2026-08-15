"""The few facts a report needs that the repository cannot know.

Which epics are being worked, when the last report went out, and whether the resume cron was
armed. Gitignored, optional in every field, and never a reason a report fails to exist: a corrupt
breadcrumb degrades the report rather than preventing it.
"""

from __future__ import annotations

import json
import os
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

DEFAULT_PATH = Path(".claude/run-state.json")

FIELDS = (
    "epics",
    "current_epic",
    "current_story",
    "last_report_at",
    "cron_armed",
    "started_at",
    "note",
)


@dataclass
class RunState:
    epics: list = field(default_factory=list)
    current_epic: object = None
    current_story: object = None
    last_report_at: object = None
    #: True, False, or None meaning *unknown* — three states, never two. A report that renders
    #: an unknown cron as "not armed" tells the human the run is over when it may not be.
    cron_armed: object = None
    started_at: object = None
    note: object = None


def load(path=DEFAULT_PATH) -> RunState:
    """Never raises. A missing, empty, malformed or wrong-shaped file is an empty state."""
    try:
        raw = Path(path).read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return RunState()

    try:
        data = json.loads(raw)
    except ValueError:
        return RunState()

    if not isinstance(data, dict):
        return RunState()

    state = RunState()
    for name in FIELDS:
        if name in data:
            setattr(state, name, data[name])
    if not isinstance(state.epics, list):
        state.epics = []
    return state


def save(path, state) -> None:
    """Write atomically — a report reading a half-written file is the race this lives in."""
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)

    payload = {name: getattr(state, name) for name in FIELDS if getattr(state, name) is not None}

    handle = tempfile.NamedTemporaryFile(
        "w", encoding="utf-8", dir=str(path.parent), prefix=".run-state-", suffix=".tmp", delete=False
    )
    try:
        with handle as out:
            json.dump(payload, out, indent=2, sort_keys=True)
            out.write("\n")
        os.replace(handle.name, path)
    except Exception:
        try:
            os.unlink(handle.name)
        except OSError:
            pass
        raise


def stamp_report(path, when) -> RunState:
    """Update only ``last_report_at``, so a heartbeat cannot erase the armed-cron flag."""
    state = load(path)
    state.last_report_at = when if isinstance(when, str) else when.isoformat()
    save(path, state)
    return state
