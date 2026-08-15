import contextlib
import io
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import credentials
import notify
import telegram


class SendTest(unittest.TestCase):
    def test_send_exits_zero_with_no_credentials(self):
        # The contract of ADR-0042: a notifier never fails its caller.
        with mock.patch.object(notify.credentials_module, "resolve", return_value=None):
            with contextlib.redirect_stdout(io.StringIO()):
                self.assertEqual(0, notify.main(["send", "anything"]))

    def test_send_reads_stdin_when_no_arguments(self):
        with mock.patch.object(notify, "deliver", return_value=(True, "sent")) as deliver:
            with mock.patch("sys.stdin", io.StringIO("piped report body")):
                with contextlib.redirect_stdout(io.StringIO()):
                    notify.main(["send"])
        self.assertEqual("piped report body", deliver.call_args[0][0])

    def test_send_joins_argument_words(self):
        with mock.patch.object(notify, "deliver", return_value=(True, "sent")) as deliver:
            with contextlib.redirect_stdout(io.StringIO()):
                notify.main(["send", "three", "separate", "words"])
        self.assertEqual("three separate words", deliver.call_args[0][0])


class DoctorTest(unittest.TestCase):
    def test_doctor_exits_non_zero_when_unconfigured(self):
        with mock.patch.dict(os.environ, {}, clear=True):
            with mock.patch.object(credentials, "DEFAULT_PATH", Path("/nowhere/at/all/telegram.env")):
                out = io.StringIO()
                with contextlib.redirect_stdout(out):
                    code = notify.main(["doctor"])
        self.assertEqual(1, code)
        self.assertIn("no credential file", out.getvalue())

    def test_doctor_names_the_missing_piece(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "telegram.env"
            path.write_text("TELEGRAM_BOT_TOKEN=only-the-token\n", encoding="utf-8")
            with mock.patch.dict(os.environ, {}, clear=True):
                with mock.patch.object(credentials, "DEFAULT_PATH", path):
                    out = io.StringIO()
                    with contextlib.redirect_stdout(out):
                        code = notify.main(["doctor"])
        self.assertEqual(1, code)
        self.assertIn("half a configuration", out.getvalue())
        self.assertIn(credentials.CHAT_KEY, out.getvalue())

    def test_doctor_exits_zero_when_the_send_succeeds(self):
        creds = credentials.Credentials(token="t:okon", chat_id="1")
        with mock.patch.object(notify.credentials_module, "resolve", return_value=creds):
            with mock.patch.object(notify.telegram_module, "send", return_value=telegram.Result(True, "sent")):
                out = io.StringIO()
                with contextlib.redirect_stdout(out):
                    code = notify.main(["doctor"])
        self.assertEqual(0, code)
        self.assertIn("ok", out.getvalue())


class UsageTest(unittest.TestCase):
    def test_unknown_subcommand_exits_two(self):
        with contextlib.redirect_stderr(io.StringIO()):
            with self.assertRaises(SystemExit) as raised:
                notify.main(["nonsense"])
        self.assertEqual(2, raised.exception.code)

    def test_no_subcommand_exits_two(self):
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            self.assertEqual(2, notify.main([]))


if __name__ == "__main__":
    unittest.main()
