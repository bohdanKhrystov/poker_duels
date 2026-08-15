"""The three event reports: the run stopped, the run parked, the run is short on tokens.

The budget report is the one with a hard rule. It must always name what happened to the resume
cron, because without that line the human cannot tell *"it will pick itself up"* from *"it is
over until I touch it"* — which is the entire reason the report exists.
"""

from __future__ import annotations

from datetime import datetime, timezone

import report as report_module

CRON_STATES = ("armed", "not-armed", "unknown")

_CRON_FLAG = {"armed": True, "not-armed": False, "unknown": None}


def cron_flag(value):
    """Map the CLI's three words onto the run state's three-valued flag."""
    return _CRON_FLAG[value]


def _context(repo, state, now, runner, backlog):
    """DONE and BLOCKED, so every event report is self-contained."""
    since = report_module.window_start(state, now)
    backlog = report_module.board_module.scan(f"{repo}/tasks") if backlog is None else backlog
    return [
        (f"DONE (since {since.strftime('%H:%M UTC')})", report_module.done_section(repo, since, runner)),
        ("BLOCKED", report_module.blocked_section(backlog)),
    ], backlog


def stop_report(repo, state, now=None, reason=None, runner=None, backlog=None) -> str:
    now = datetime.now(timezone.utc) if now is None else now
    runner = report_module.default_runner if runner is None else runner
    sections, backlog = _context(repo, state, now, runner, backlog)

    headline = "Poker Duels — the run STOPPED"
    lead = f"reason: {reason}" if reason else "reason: not given (the session ended)"
    head = f"{headline}\n{now.strftime('%Y-%m-%d %H:%M UTC')}\n{lead}"
    sections.append(("BUDGET", report_module.budget_section(state)))
    return report_module.assemble(head, sections)


def blocked_report(repo, state, decision, question=None, now=None, runner=None, backlog=None) -> str:
    now = datetime.now(timezone.utc) if now is None else now
    runner = report_module.default_runner if runner is None else runner
    sections, backlog = _context(repo, state, now, runner, backlog)

    epic = getattr(state, "current_epic", None) or "—"
    head = (
        f"Poker Duels — PARKED on {decision}\n"
        f"{now.strftime('%Y-%m-%d %H:%M UTC')}\n"
        f"epic: {epic}\n"
        f"{('question: ' + question) if question else 'this one needs you.'}"
    )
    return report_module.assemble(head, sections)


def budget_report(repo, state, cron_armed, reset_at=None, now=None, runner=None, backlog=None) -> str:
    """``cron_armed`` is one of :data:`CRON_STATES` and is never optional."""
    if cron_armed not in CRON_STATES:
        raise ValueError(f"cron_armed must be one of {CRON_STATES}, got {cron_armed!r}")

    now = datetime.now(timezone.utc) if now is None else now
    runner = report_module.default_runner if runner is None else runner
    sections, backlog = _context(repo, state, now, runner, backlog)

    if cron_armed == "armed":
        verdict = "resume cron: armed — the run should restart itself after the reset"
    elif cron_armed == "not-armed":
        verdict = "resume cron: NOT ARMED — the run will not restart itself; you must re-issue it"
    else:
        verdict = "resume cron: unknown — assume nothing; check before you go to sleep"

    head = (
        f"Poker Duels — SHORT ON TOKENS\n"
        f"{now.strftime('%Y-%m-%d %H:%M UTC')}\n"
        f"{verdict}"
    )
    if reset_at:
        head += f"\nlimit resets: {reset_at}"

    epics = getattr(state, "epics", None) or []
    if epics:
        head += f"\nstill queued: {', '.join(str(e) for e in epics)}"

    return report_module.assemble(head, sections)
