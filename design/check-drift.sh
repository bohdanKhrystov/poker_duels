#!/bin/sh
# Poker Duels — token-name drift check (TASK-060106, ADR-0024 §2).
# Every `--pd-*` name any design card mentions — in CSS or printed on the card —
# must be declared in the canonical sheet. A rename that forgets a card fails here
# instead of drifting silently. Stock macOS/Linux tools only (grep -o is a BSD/GNU
# extension both platforms ship; strict POSIX omits it).
set -eu
DIR=$(dirname "$0")
SHEET="$DIR/tokens/tokens.css"
[ -f "$SHEET" ] || { echo "check-drift: missing $SHEET" >&2; exit 1; }

declared=$(grep -o -- '--pd-[a-z0-9]*\(-[a-z0-9]*\)*' "$SHEET" | sort -u)
fail=0
mentions=0
files=0
for f in $(find "$DIR" -name '*.html' | sort); do
  files=$((files + 1))
  for name in $(grep -o -- '--pd-[a-z0-9]*\(-[a-z0-9]*\)*' "$f" | sort -u); do
    mentions=$((mentions + 1))
    if ! printf '%s\n' "$declared" | grep -qx -- "$name"; then
      echo "drift: $f mentions $name, which tokens.css does not declare" >&2
      fail=1
    fi
  done
done

# a suit glyph without U+FE0E lets OEM emoji fallbacks repaint it — sweep every card
for f in $(find "$DIR" -name '*.html' | sort); do
  if grep -nE '[♠♥♦♣]([^︎]|$)' "$f" >&2; then
    echo "check-drift: bare suit glyph (no U+FE0E) in $f" >&2
    fail=1
  fi
done

# an empty tree must fail as loudly as a missing sheet — a vacuous pass guards nothing
[ "$files" -gt 0 ] || { echo "check-drift: no cards found under $DIR" >&2; exit 1; }
if [ "$fail" -ne 0 ]; then exit 1; fi
echo "check-drift: every mentioned token name resolves ($mentions distinct mentions across $files cards)"
