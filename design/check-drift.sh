#!/bin/sh
# Poker Duels — design-card invariant gate (ADR-0024 §2).
#   1. Token drift (TASK-060106): every `--pd-*` name any card mentions — in CSS or
#      printed on the card — must be declared in the canonical sheet, so a rename
#      that forgets a card fails here instead of drifting silently.
#   2. Suit presentation (TASK-060110): every suit glyph is a literal ♠♥♦♣ followed
#      by a literal U+FE0E text-presentation selector. A bare glyph lets OEM emoji
#      fonts repaint it, and entity spellings (&spades; &#x2660; &#xFE0E;) hide from
#      a source-level sweep — so the literal form is the convention, enforced here.
# Stock macOS/Linux tools only (grep -o is a BSD/GNU extension both platforms ship;
# strict POSIX omits it).
set -eu
# the suit regex is multibyte; a C/POSIX locale degrades grep to byte-set matching,
# so pin a UTF-8 locale — the self-test below catches a system that lacks this one
LC_ALL=C.UTF-8
export LC_ALL

DIR=$(dirname "$0")
SHEET="$DIR/tokens/tokens.css"
[ -f "$SHEET" ] || { echo "check-drift: missing $SHEET" >&2; exit 1; }

BARE='[♠♥♦♣]([^︎]|$)'
ENTITY='&(spades|hearts|diams|clubs);|&#0*(9824|9829|9830|9827|65038);|&#[xX]0*(2660|2665|2666|2663|[fF][eE]0[eE]);'

# self-test: prove the sweep regex works here before trusting its silence — a wrong
# locale or a byte-oriented grep must fail this loudly, not distort verdicts below
printf 'A♠ x\n' | grep -qE -- "$BARE" \
  || { echo "check-drift: self-test failed — a bare suit went undetected (grep/locale?)" >&2; exit 1; }
if printf 'A♠︎ x\n' | grep -qE -- "$BARE"; then
  echo "check-drift: self-test failed — a suited glyph read as bare (locale?)" >&2; exit 1
fi

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

  # grep exits 0 on match, 1 on clean no-match; anything else means the card was
  # never actually read, which must fail as loudly as a bad card
  st=0; grep -nE -- "$BARE" "$f" >&2 || st=$?
  if [ "$st" -eq 0 ]; then
    echo "check-drift: bare suit glyph (no U+FE0E) in $f" >&2; fail=1
  elif [ "$st" -ne 1 ]; then
    echo "check-drift: grep error $st sweeping $f for bare suits" >&2; fail=1
  fi

  st=0; grep -nE -- "$ENTITY" "$f" >&2 || st=$?
  if [ "$st" -eq 0 ]; then
    echo "check-drift: entity-form suit or selector in $f — write the literal glyph plus literal U+FE0E" >&2; fail=1
  elif [ "$st" -ne 1 ]; then
    echo "check-drift: grep error $st sweeping $f for entity suits" >&2; fail=1
  fi
done

# an empty tree must fail as loudly as a missing sheet — a vacuous pass guards nothing
[ "$files" -gt 0 ] || { echo "check-drift: no cards found under $DIR" >&2; exit 1; }
if [ "$fail" -ne 0 ]; then exit 1; fi
echo "check-drift: tokens resolve and suits carry U+FE0E ($mentions distinct mentions across $files cards)"
