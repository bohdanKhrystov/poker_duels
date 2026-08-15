#!/usr/bin/env bash
# Stop hook — report that the run stopped.
#
# Exits 0 on every path, deliberately. A non-zero Stop hook interferes with the session it is
# reporting on, which is a worse outcome than a missed message.
#
# Registered from .claude/settings.local.json, not .claude/settings.json: that file is deny-listed
# to the agent by this repository's own permissions, and the rule is not worth weakening for a
# notifier. See docs/notifications.md.
set -u

# Drain the hook payload so the harness never blocks writing into an unread pipe.
cat >/dev/null 2>&1 || true

here="$(cd "$(dirname "${BASH_SOURCE[0]:-$0}")" 2>/dev/null && pwd)" || exit 0
repo="$(cd "$here/../../.." 2>/dev/null && pwd)" || exit 0

state="$repo/.claude/run-state.json"

# Only report while a run is in flight. Without this the human gets a Telegram message every
# time any turn ends in this repository, and the channel becomes noise inside a day.
[ -f "$state" ] || exit 0
grep -q '"current_epic"' "$state" 2>/dev/null || exit 0

command -v python3 >/dev/null 2>&1 || exit 0
[ -f "$repo/scripts/notify/notify.py" ] || exit 0

python3 "$repo/scripts/notify/notify.py" stop \
  --reason "the Claude Code session stopped" >/dev/null 2>&1 || true

exit 0
