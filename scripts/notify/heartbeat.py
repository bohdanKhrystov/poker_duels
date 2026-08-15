"""One message per window, whoever fires it.

Two things can fire a heartbeat — the agent by hand and the cron on its clock — and the human
must receive exactly one message per window.
"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

import report as report_module
import run_state as run_state_module

WINDOW = timedelta(hours=2)


def due(state, now, window=WINDOW) -> bool:
    """A malformed or future-dated stamp is *due*: a bad clock must not silence the channel."""
    stamped = report_module.parse_time(getattr(state, "last_report_at", None))
    if stamped is None or stamped > now:
        return True
    return (now - stamped) >= window


def beat(repo, state_path, sender, now=None, window=WINDOW, force=False, composer=None, stamp=True):
    """Compose, send, and stamp — stamping **only** on a successful send.

    A failed send that stamped anyway would suppress the next window too, turning one lost
    message into two.

    ``stamp=False`` is for a dry run. Printing a report is not delivering one, and letting a
    preview consume the window would silently suppress the next real heartbeat.
    """
    now = datetime.now(timezone.utc) if now is None else now
    state = run_state_module.load(state_path)

    if not force and not due(state, now, window):
        return False, "not due"

    composer = report_module.compose if composer is None else composer
    text = composer(repo, state, now)

    if sender(text):
        if stamp:
            run_state_module.stamp_report(state_path, now)
        return True, text
    return False, text
