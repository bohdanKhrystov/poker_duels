import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

import heartbeat
import run_state

NOW = datetime(2026, 8, 15, 22, 0, tzinfo=timezone.utc)


def stamped(minutes_ago):
    return (NOW - timedelta(minutes=minutes_ago)).isoformat()


class Sender:
    def __init__(self, ok=True):
        self.ok = ok
        self.sent = []

    def __call__(self, text):
        self.sent.append(text)
        return self.ok


def composer(repo, state, now):
    return "a report"


class DueTest(unittest.TestCase):
    def test_first_ever_beat_is_due(self):
        self.assertTrue(heartbeat.due(run_state.RunState(), NOW))

    def test_second_beat_inside_the_window_is_not_due(self):
        self.assertFalse(heartbeat.due(run_state.RunState(last_report_at=stamped(30)), NOW))

    def test_beat_after_the_window_is_due(self):
        self.assertTrue(heartbeat.due(run_state.RunState(last_report_at=stamped(121)), NOW))

    def test_malformed_timestamp_is_due(self):
        self.assertTrue(heartbeat.due(run_state.RunState(last_report_at="not a time"), NOW))

    def test_future_timestamp_is_due(self):
        # A clock that has gone backwards must not silence the channel indefinitely.
        self.assertTrue(heartbeat.due(run_state.RunState(last_report_at=stamped(-600)), NOW))


class BeatTest(unittest.TestCase):
    def beat(self, state, sender, force=False):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "run-state.json"
            run_state.save(path, state)
            sent, _ = heartbeat.beat(".", path, sender, now=NOW, force=force, composer=composer)
            return sent, run_state.load(path)

    def test_force_sends_inside_the_window(self):
        sender = Sender()
        sent, _ = self.beat(run_state.RunState(last_report_at=stamped(5)), sender, force=True)
        self.assertTrue(sent)
        self.assertEqual(["a report"], sender.sent)

    def test_successful_send_stamps_the_state(self):
        sent, state = self.beat(run_state.RunState(), Sender(ok=True))
        self.assertTrue(sent)
        self.assertTrue(state.last_report_at.startswith("2026-08-15T22:00"))

    def test_failed_send_does_not_stamp(self):
        # Stamping a failed send would suppress the next window too, turning one lost message
        # into two.
        sent, state = self.beat(run_state.RunState(), Sender(ok=False))
        self.assertFalse(sent)
        self.assertIsNone(state.last_report_at)


if __name__ == "__main__":
    unittest.main()
