import tempfile
import unittest
from pathlib import Path

import credentials


def write(text):
    handle = tempfile.NamedTemporaryFile("w", suffix=".env", delete=False, encoding="utf-8")
    handle.write(text)
    handle.close()
    return Path(handle.name)


class ResolveTest(unittest.TestCase):
    def test_environment_wins_over_file(self):
        path = write("TELEGRAM_BOT_TOKEN=from-file\nTELEGRAM_CHAT_ID=222\n")
        env = {"TELEGRAM_BOT_TOKEN": "from-env", "TELEGRAM_CHAT_ID": "111"}
        found = credentials.resolve(env, path)
        self.assertEqual("from-env", found.token)
        self.assertEqual("111", found.chat_id)

    def test_falls_back_to_the_file(self):
        path = write("TELEGRAM_BOT_TOKEN=abc:def\nTELEGRAM_CHAT_ID=222\n")
        found = credentials.resolve({}, path)
        self.assertEqual("abc:def", found.token)
        self.assertEqual("222", found.chat_id)

    def test_missing_file_is_not_an_error(self):
        self.assertIsNone(credentials.resolve({}, Path("/nowhere/at/all/telegram.env")))

    def test_comments_and_blank_lines_are_skipped(self):
        path = write("# a comment\n\nTELEGRAM_BOT_TOKEN=t\n\n# another\nTELEGRAM_CHAT_ID=9\n")
        found = credentials.resolve({}, path)
        self.assertEqual("t", found.token)
        self.assertEqual("9", found.chat_id)

    def test_quoted_values_are_unquoted(self):
        path = write('TELEGRAM_BOT_TOKEN="tok"\nTELEGRAM_CHAT_ID=\'123\'\n')
        found = credentials.resolve({}, path)
        self.assertEqual("tok", found.token)
        self.assertEqual("123", found.chat_id)

    def test_token_without_chat_id_is_not_a_configuration(self):
        path = write("TELEGRAM_BOT_TOKEN=only-the-token\n")
        self.assertIsNone(credentials.resolve({}, path))


class RedactTest(unittest.TestCase):
    TOKEN = "8609897557:AAGabcdefghijklmnop"

    def test_redact_removes_the_token(self):
        text = f"POST https://api.telegram.org/bot{self.TOKEN}/sendMessage failed"
        self.assertNotIn(self.TOKEN, credentials.redact(text, self.TOKEN))

    def test_redact_removes_the_bot_id_prefix(self):
        # Telegram error bodies echo the bot id back on its own, without the secret half.
        self.assertNotIn("8609897557", credentials.redact("bot 8609897557: rejected", self.TOKEN))

    def test_redact_of_empty_token_leaves_text_alone(self):
        self.assertEqual("untouched", credentials.redact("untouched", ""))
        self.assertEqual("untouched", credentials.redact("untouched", None))


if __name__ == "__main__":
    unittest.main()
