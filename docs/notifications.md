# Status notifications

An unattended run tells you what it is doing without being asked. Four reports, over Telegram:

| Report | When | Says |
| --- | --- | --- |
| **heartbeat** | every 2 hours while working | what landed, what is in flight, what is blocked |
| **stop** | the run ended | why, plus the same context |
| **blocked** | the run parked on a decision | which `DEC-NNN`, and the question |
| **budget** | tokens are running short | **whether the resume cron was armed** |

The fourth is why this exists. A usage limit terminates the turn and every turn after it, so the
report most likely to be owed is the one the agent is least able to write — which is why reports
are composed from repository state by a script rather than from an agent's memory. See
[`ADR-0042`](adr/ADR-0042-the-run-reports-itself-every-two-hours.md).

---

## Setting it up on a fresh machine

### 1. Make a bot

Message [@BotFather](https://t.me/BotFather) on Telegram:

```
/newbot
```

Give it a display name and a username ending in `bot`. BotFather replies with a token that looks
like `123456789:AAH…`.

### 2. Message your bot, once

Open your new bot's chat and send it anything — `hi` will do.

**This step is mandatory and easy to miss.** A bot cannot open a conversation with you, so until
you have written to it, Telegram will not tell anyone what your chat id is.

### 3. Find your chat id

```sh
curl -s "https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['result'][-1]['message']['chat']['id'])"
```

An empty `result` means step 2 did not happen.

### 4. Write the credential file

Outside the repository, readable only by you:

```sh
mkdir -p ~/.claude/poker-duels
cat > ~/.claude/poker-duels/telegram.env <<'EOF'
TELEGRAM_BOT_TOKEN=123456789:AAH…
TELEGRAM_CHAT_ID=987654321
EOF
chmod 600 ~/.claude/poker-duels/telegram.env
```

`TELEGRAM_BOT_TOKEN` and `TELEGRAM_CHAT_ID` in the environment override the file, which is how
CI or a second machine would be given different values. **Both halves are required** — a token
with no chat id is treated as no configuration at all.

### 5. Prove it

```sh
python3 scripts/notify/notify.py doctor
```

`doctor` is the only command here that exits non-zero, and it sends a real message rather than
merely checking that the file parses. Everything else exits 0 whether or not it could send —
a notification that breaks a build is worse than one that is missed — so `doctor` is what you
run after any credential change.

### 6. Register the stop hook

The stop report is a `Stop` hook, executed by the harness, because the case worth reporting is
the one where the agent has nothing left to say. Paste this into
`.claude/settings.local.json` (create the file if it is absent, or merge the `hooks` key into
what is already there):

```json
{
  "hooks": {
    "Stop": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "$CLAUDE_PROJECT_DIR/scripts/notify/hooks/stop_hook.sh"
          }
        ]
      }
    ]
  }
}
```

**Why `settings.local.json` and not `settings.json`:** `.claude/settings.json` is deny-listed to
the agent in this repository's own permissions, and that rule is not worth weakening for a
notifier. The hook's *logic* is versioned under `scripts/notify/hooks/`, so the only thing living
outside git is the single line that wires it up.

The hook is silent unless a run is in flight — it sends nothing unless `.claude/run-state.json`
exists and names a `current_epic`. Without that guard you would get a message every time any turn
ended in this repository.

---

## Commands

```sh
python3 scripts/notify/notify.py report                       # print, do not send
python3 scripts/notify/notify.py send "text"                  # or pipe on stdin
python3 scripts/notify/notify.py heartbeat [--force]          # send if a window has passed
python3 scripts/notify/notify.py stop --reason "…"
python3 scripts/notify/notify.py blocked --decision DEC-NNN --question "…"
python3 scripts/notify/notify.py budget --cron-armed armed|not-armed|unknown
python3 scripts/notify/notify.py state --epic EPIC-11 --epics EPIC-11,EPIC-03 --cron-armed armed
python3 scripts/notify/notify.py doctor                       # the loud one
```

`--dry-run` prints the message instead of sending it, on every command that sends.

`budget` **requires** `--cron-armed`. It has no default, deliberately: a budget report that does
not say what happened to the resume cron cannot tell you whether the run restarts itself, which
is the only reason that report exists. The three values are distinct — `unknown` is never
rendered as `not-armed`.

## The run-state breadcrumb

`.claude/run-state.json` is gitignored and holds only what the repository cannot know: the epic
list being worked, the last-sent stamp, and the armed-cron flag. Every field is optional, and a
missing or corrupt file degrades a report rather than preventing one.

The driver writes it — `notify.py state --epic … ` at the start of a run, `--clear` at the end so
the stop hook falls silent again.

## The heartbeat clock, and its two expiry dates

The two-hourly heartbeat is an in-session cron job armed at the start of a run:

```
CronCreate(cron: "17 */2 * * *", recurring: true,
           prompt: "run: python3 scripts/notify/notify.py heartbeat")
```

Two things end it, and neither announces itself:

- **Quitting Claude Code.** Cron jobs are session-only — nothing is written to disk. They survive
  a usage limit, which leaves the process running, but not a quit and not a reboot.
- **Seven days.** Recurring jobs auto-expire after a week; the job fires one last time and is
  deleted.

So the heartbeat is re-armed at the start of every run rather than assumed, and jobs fire only
while the REPL is idle — which is why the dedup window lives in the script rather than in the
schedule. An off-minute (`:17`, not `:00`) is deliberate.

## What is deliberately not built

- **A heartbeat that survives Claude Code being quit.** The cron is session-only. It survives a
  usage limit, which leaves the process running, but not a quit or a reboot.
  [`STORY-1104`](../tasks/stories/STORY-1104-a-heartbeat-that-outlives-the-session.md) holds the
  launchd design, written and unstarted.
- **Two-way control.** The bot sends; it does not take commands. A chat that can start a run is a
  remote-execution surface.
- **A second channel.** Only `telegram.py` is Telegram-shaped, so Slack or email is one file —
  but no second channel is built until one is asked for.

## If it goes quiet

Because notifiers never fail loudly, a misconfigured channel looks exactly like a quiet run.
Run `doctor`. If the token has leaked, send `/revoke` to @BotFather — a leaked token lets a
stranger post into your chat, and nothing else; it cannot read the repository.
