#!/usr/bin/env python3
"""The one command the run reports through.

Every entry point exits 0 whether or not it sent, per ADR-0042 — unconfigured, offline, rejected,
all the same. A notification that breaks a build is a worse outcome than one that is missed, and
this runs from hooks and CI where a non-zero exit has consequences out of all proportion to a
status message.

``doctor`` is the single exception. It exists to be loud.
"""

from __future__ import annotations

import argparse
import sys
from datetime import datetime, timezone
from pathlib import Path

import credentials as credentials_module
import heartbeat as heartbeat_module
import lifecycle as lifecycle_module
import report as report_module
import run_state as run_state_module
import telegram as telegram_module

REPO = Path(__file__).resolve().parents[2]
STATE_PATH = REPO / ".claude" / "run-state.json"

USAGE = "notify.py {send|report|doctor|heartbeat|stop|blocked|budget|state} …"


def deliver(text, dry_run=False):
    """Resolve credentials and send. Returns ``(ok, detail)`` and never raises."""
    if dry_run:
        print(text)
        return True, "dry-run"

    creds = credentials_module.resolve()
    if creds is None:
        return False, "not configured — see docs/notifications.md"

    result = telegram_module.send(creds, text)
    return result.ok, result.detail


def _now():
    return datetime.now(timezone.utc)


def _state():
    return run_state_module.load(STATE_PATH)


def cmd_send(args) -> int:
    text = " ".join(args.text) if args.text else sys.stdin.read()
    ok, detail = deliver(text, args.dry_run)
    print(f"send: {'ok' if ok else 'not sent'} — {detail}")
    return 0


def cmd_report(args) -> int:
    print(report_module.compose(REPO, _state(), _now()))
    return 0


def cmd_doctor(args) -> int:
    path = credentials_module.DEFAULT_PATH
    creds = credentials_module.resolve()

    if creds is None:
        if not Path(path).exists():
            print(f"doctor: FAIL — no credential file at {path}")
        else:
            parsed = credentials_module.read_env_file(path)
            missing = [k for k in (credentials_module.TOKEN_KEY, credentials_module.CHAT_KEY) if not parsed.get(k)]
            if not parsed:
                print(f"doctor: FAIL — {path} exists but no KEY=value line parsed")
            else:
                print(f"doctor: FAIL — half a configuration, missing {', '.join(missing)}")
        return 1

    # A doctor that only checks configuration proves nothing about the wire.
    result = telegram_module.send(creds, "notify doctor: the Poker Duels status channel works.")
    if not result.ok:
        print(f"doctor: FAIL — send rejected: {result.detail}")
        return 1

    print("doctor: ok — a real message was delivered")
    return 0


def cmd_heartbeat(args) -> int:
    sent, text = heartbeat_module.beat(
        REPO,
        STATE_PATH,
        sender=lambda body: deliver(body, args.dry_run)[0],
        now=_now(),
        force=args.force,
        stamp=not args.dry_run,
    )
    print(f"heartbeat: {'sent' if sent else 'skipped'}")
    return 0


def cmd_stop(args) -> int:
    text = lifecycle_module.stop_report(REPO, _state(), _now(), reason=args.reason)
    ok, detail = deliver(text, args.dry_run)
    print(f"stop: {'sent' if ok else 'not sent'} — {detail}")
    return 0


def cmd_blocked(args) -> int:
    text = lifecycle_module.blocked_report(REPO, _state(), args.decision, args.question, _now())
    ok, detail = deliver(text, args.dry_run)
    print(f"blocked: {'sent' if ok else 'not sent'} — {detail}")
    return 0


def cmd_budget(args) -> int:
    state = _state()
    # Record it before sending: a later heartbeat must repeat the same answer even if this
    # send is the one that fails.
    state.cron_armed = lifecycle_module.cron_flag(args.cron_armed)
    run_state_module.save(STATE_PATH, state)

    text = lifecycle_module.budget_report(REPO, state, args.cron_armed, args.reset_at, _now())
    ok, detail = deliver(text, args.dry_run)
    print(f"budget: {'sent' if ok else 'not sent'} — {detail}")
    return 0


def cmd_state(args) -> int:
    state = _state()
    if args.epic is not None:
        state.current_epic = args.epic or None
    if args.story is not None:
        state.current_story = args.story or None
    if args.epics is not None:
        state.epics = [part.strip() for part in args.epics.split(",") if part.strip()]
    if args.cron_armed is not None:
        state.cron_armed = lifecycle_module.cron_flag(args.cron_armed)
    if args.note is not None:
        state.note = args.note or None
    if args.clear:
        # Every field is a fact about the run that just ended, except last_report_at, the
        # heartbeat's dedupe stamp — clearing that would let the next run's first heartbeat fire
        # as though none had ever been sent. Derived from FIELDS so a field added later is
        # cleared by default rather than surviving as a new stale breadcrumb.
        for name in run_state_module.FIELDS:
            if name != "last_report_at":
                setattr(state, name, None)
    run_state_module.save(STATE_PATH, state)
    print(f"state: written to {STATE_PATH}")
    return 0


def build_parser():
    parser = argparse.ArgumentParser(prog="notify.py", usage=USAGE)
    subparsers = parser.add_subparsers(dest="command")

    send = subparsers.add_parser("send", help="send text, or stdin when no text is given")
    send.add_argument("text", nargs="*")
    send.add_argument("--dry-run", action="store_true")
    send.set_defaults(handler=cmd_send)

    report = subparsers.add_parser("report", help="print the status report without sending it")
    report.set_defaults(handler=cmd_report)

    doctor = subparsers.add_parser("doctor", help="prove the channel; the one command that exits non-zero")
    doctor.set_defaults(handler=cmd_doctor)

    beat = subparsers.add_parser("heartbeat", help="send the periodic report if one is due")
    beat.add_argument("--force", action="store_true", help="send even inside the window")
    beat.add_argument("--dry-run", action="store_true")
    beat.set_defaults(handler=cmd_heartbeat)

    stop = subparsers.add_parser("stop", help="report that the run stopped")
    stop.add_argument("--reason")
    stop.add_argument("--dry-run", action="store_true")
    stop.set_defaults(handler=cmd_stop)

    blocked = subparsers.add_parser("blocked", help="report that the run parked on a decision")
    blocked.add_argument("--decision", required=True)
    blocked.add_argument("--question")
    blocked.add_argument("--dry-run", action="store_true")
    blocked.set_defaults(handler=cmd_blocked)

    # --cron-armed is required rather than defaulted: a budget report that does not say what
    # happened to the resume cron cannot do the one job it exists for.
    budget = subparsers.add_parser("budget", help="report a token shortage; names the resume cron")
    budget.add_argument("--cron-armed", required=True, choices=lifecycle_module.CRON_STATES)
    budget.add_argument("--reset-at")
    budget.add_argument("--dry-run", action="store_true")
    budget.set_defaults(handler=cmd_budget)

    state = subparsers.add_parser("state", help="stamp the run-state breadcrumb")
    state.add_argument("--epic")
    state.add_argument("--story")
    state.add_argument("--epics", help="comma-separated list still queued")
    state.add_argument("--cron-armed", choices=lifecycle_module.CRON_STATES)
    state.add_argument("--note")
    state.add_argument("--clear", action="store_true", help="the run ended; stop the Stop hook firing")
    state.set_defaults(handler=cmd_state)

    return parser


def main(argv=None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    if not getattr(args, "handler", None):
        parser.print_usage()
        return 2
    return args.handler(args)


if __name__ == "__main__":
    sys.exit(main())
