"""The status report: what landed, what is in flight, what is blocked, and the budget line.

Composed from repository state rather than from the agent's memory, per ADR-0042. That is the
load-bearing property: it means a report can be produced by anything able to run a script, and
specifically by something that is *not* the agent whose budget just ran out.

Every section degrades alone. No ``gh`` on the box, a git repository with no commits, a missing
board — each replaces its own section with one honest line and leaves the others intact.
"""

from __future__ import annotations

import subprocess
from datetime import datetime, timedelta, timezone

import board as board_module

LIMIT = 4096
DEFAULT_WINDOW_HOURS = 2
MAX_LINES_PER_SECTION = 12


def default_runner(args, cwd=None, timeout=15):
    """Run a command, return ``(ok, output)``. Never raises — a missing binary is ``ok=False``."""
    try:
        finished = subprocess.run(
            args, capture_output=True, text=True, timeout=timeout, check=False, cwd=cwd
        )
    except (OSError, subprocess.SubprocessError) as error:
        return False, f"{type(error).__name__}: {error}"
    if finished.returncode != 0:
        return False, (finished.stderr or finished.stdout or "").strip()
    return True, finished.stdout


def parse_time(raw):
    if not isinstance(raw, str) or not raw:
        return None
    try:
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
    except ValueError:
        return None
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def window_start(state, now, hours=DEFAULT_WINDOW_HOURS):
    """Where the DONE section starts looking.

    A malformed or future-dated stamp falls back to the fixed window: a clock that has gone
    backwards must not produce a report covering nothing.
    """
    stamped = parse_time(getattr(state, "last_report_at", None))
    if stamped is not None and stamped <= now:
        return stamped
    return now - timedelta(hours=hours)


def _bullets(lines, empty):
    lines = [line for line in lines if line.strip()]
    if not lines:
        return f"  (none) {empty}".rstrip()
    shown = [f"  • {line.strip()}" for line in lines[:MAX_LINES_PER_SECTION]]
    if len(lines) > MAX_LINES_PER_SECTION:
        shown.append(f"  … and {len(lines) - MAX_LINES_PER_SECTION} more")
    return "\n".join(shown)


def done_section(repo, since, runner):
    ok, out = runner(
        ["git", "-C", str(repo), "log", "--no-merges", "--oneline", f"--since={since.isoformat()}", "develop"]
    )
    if not ok:
        return "  (unavailable) git could not list commits"
    return _bullets(out.splitlines(), "nothing landed in this window")


def in_progress_section(repo, backlog, runner):
    lines = []
    # `gh` is optional tooling; its absence costs this half of the section and nothing else.
    ok, out = runner(["gh", "pr", "list", "--state", "open", "--limit", "20"], str(repo))
    if ok:
        lines.extend(out.splitlines())
    else:
        lines.append("(unavailable) gh could not list open pull requests")

    counts = backlog.counts_by_status
    working = counts.get("in-progress", 0) + counts.get("in-review", 0)
    lines.append(f"{working} ticket(s) in-progress or in-review")
    return _bullets(lines, "")


def blocked_section(backlog):
    lines = []
    for item in backlog.blocked:
        decision = f" — {item.decision}" if item.decision else " — no decision named"
        lines.append(f"{item.id} {item.title}{decision}")
    return _bullets(lines, "nothing is blocked")


def cron_line(state):
    """Three states, never two. Absent is *unknown* and is never rendered as *not armed*."""
    armed = getattr(state, "cron_armed", None)
    if armed is True:
        return "  • resume cron: armed"
    if armed is False:
        return "  • resume cron: NOT ARMED — this run will not restart itself"
    return "  • resume cron: unknown"


def budget_section(state):
    lines = [cron_line(state)]
    note = getattr(state, "note", None)
    if note:
        lines.append(f"  • {note}")
    return "\n".join(lines)


def header(repo, state, now, backlog):
    epic = getattr(state, "current_epic", None) or "—"
    story = getattr(state, "current_story", None)
    counts = backlog.counts_by_status
    done = counts.get("done", 0)
    total = sum(counts.values()) or 0
    where = f"{epic}" + (f" / {story}" if story else "")
    stamp = now.strftime("%Y-%m-%d %H:%M UTC")
    skipped = f"  ({backlog.skipped} unreadable)" if backlog.skipped else ""
    return (
        f"Poker Duels — run status\n"
        f"{stamp}\n"
        f"working: {where}\n"
        f"backlog: {done}/{total} tasks done{skipped}"
    )


def assemble(head, sections, limit=LIMIT):
    """Drop whole trailing sections until it fits, and say which were dropped."""
    kept = list(sections)
    dropped: list = []
    while True:
        parts = [head] + [f"{title}\n{body}" for title, body in kept]
        if dropped:
            parts.append(f"[dropped for length: {', '.join(dropped)}]")
        text = "\n\n".join(parts)
        if len(text) <= limit or not kept:
            return text
        dropped.insert(0, kept.pop()[0])


def compose(repo, state, now=None, runner=None, limit=LIMIT, backlog=None):
    now = datetime.now(timezone.utc) if now is None else now
    runner = default_runner if runner is None else runner
    backlog = board_module.scan(f"{repo}/tasks") if backlog is None else backlog

    since = window_start(state, now)
    sections = [
        (f"DONE (since {since.strftime('%H:%M UTC')})", done_section(repo, since, runner)),
        ("IN PROGRESS", in_progress_section(repo, backlog, runner)),
        ("BLOCKED", blocked_section(backlog)),
        ("BUDGET", budget_section(state)),
    ]
    return assemble(header(repo, state, now, backlog), sections, limit)
