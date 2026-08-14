#!/bin/sh
# Poker Duels — design-card invariant gate (ADR-0024 §2).
#   1. Token drift (TASK-060106): every `--pd-*` name any card mentions — in CSS or
#      printed on the card — must be declared in the canonical sheet, so a rename
#      that forgets a card fails here instead of drifting silently.
#   2. Suit presentation (TASK-060110): every suit glyph is a literal ♠♥♦♣ followed
#      by a literal U+FE0E text-presentation selector. A bare glyph lets OEM emoji
#      fonts repaint it, and entity spellings (&spades; &#x2660; &#xFE0E;) hide from
#      a source-level sweep — so the literal form is the convention, enforced here.
#   3. Value drift (TASK-060111): every `--pd-NAME: VALUE` a card inlines must equal
#      the sheet's declaration for that name — whitespace is stripped outside quoted
#      strings so `rgba(0, 0, 0, 0.4)` and `rgba(0,0,0,0.4)` compare equal, and
#      declarations join to their `;` across the sheet's wrapped lines.
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

# joins every `--pd-NAME: … ;` declaration to one line and strips whitespace outside
# quoted strings, emitting `name=value` — the sheet wraps long values, cards do not,
# and both must land on the same normalized form. The `--pd-` name charset below is
# the same language as the name gate's grep in the loop — a change to what a token
# name may contain must change both.
EXTRACT='
  { buf = buf $0 "\n" }
  END {
    while (match(buf, /--pd-[a-z0-9-]*[ \t]*:[^;]*;/)) {
      d = substr(buf, RSTART, RLENGTH); buf = substr(buf, RSTART + RLENGTH)
      out = ""; inq = 0; q = ""
      for (i = 1; i <= length(d); i++) {
        c = substr(d, i, 1)
        if (inq) { out = out c; if (c == q) inq = 0; continue }
        if (c == "\"" || c == "\047") { q = c; inq = 1; out = out c; continue }
        if (c == " " || c == "\t" || c == "\n" || c == "\r") continue
        out = out c
      }
      sub(/:/, "=", out); sub(/;$/, "", out)
      print out
    }
  }
'
# flags card declarations whose name the sheet knows but whose value differs; names
# the sheet lacks stay the name gate speaking alone, not a double report. The sheet
# set arrives as the first input file — awk -v would reprocess backslash escapes and
# silently mangle a value that ever gains one, so the set never travels through -v.
COMPARE='
  NR == FNR {
    if ($0 == "") next
    ok[$0] = 1
    eq = index($0, "="); if (eq) val[substr($0, 1, eq - 1)] = substr($0, eq + 1)
    next
  }
  $0 != "" && !ok[$0] {
    eq = index($0, "="); nm = substr($0, 1, eq - 1)
    if (nm in val) printf "drift: %s declares %s as %s, but the sheet says %s\n", f, nm, substr($0, eq + 1), val[nm]
  }
'
# self-test both halves before trusting their silence, like the suit sweep above; the
# || true keeps a hard awk death inside the comparison below, where the message says
# what broke, instead of dying with awk usage noise under set -e. Scratch files are
# left for the OS to purge, the trade-off this repo records in its gate tickets.
probe=$(printf -- '--pd-probe :  rgba(0, 0, 0, 0.4) ;\n' | awk "$EXTRACT" 2>/dev/null || true)
[ "$probe" = "--pd-probe=rgba(0,0,0,0.4)" ] \
  || { echo "check-drift: self-test failed — value extractor broke (awk?)" >&2; exit 1; }
probe_sheet=$(mktemp)
printf -- '--pd-probe=1px\n' > "$probe_sheet"
mism=$(printf -- '--pd-probe=2px\n' | awk -v f=self "$COMPARE" "$probe_sheet" - 2>/dev/null || true)
[ -n "$mism" ] \
  || { echo "check-drift: self-test failed — a drifted value went undetected" >&2; exit 1; }

sheet_file=$(mktemp)
awk "$EXTRACT" "$SHEET" > "$sheet_file"
[ -s "$sheet_file" ] || { echo "check-drift: no declarations extracted from $SHEET" >&2; exit 1; }
# this BRE and EXTRACT's awk ERE describe the same name language — change both or neither
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

  # extraction failures fail as loudly as the st= sweeps below — an unreadable card
  # must never read as a value-clean card (the || true softening is for the
  # self-tests only, where the assert right after catches a dead awk)
  st=0; card_vals=$(awk "$EXTRACT" "$f" 2>/dev/null) || st=$?
  if [ "$st" -ne 0 ]; then
    echo "check-drift: awk error $st extracting values from $f" >&2; fail=1; card_vals=""
  fi
  if [ -n "$card_vals" ]; then
    bad=$(printf '%s\n' "$card_vals" | awk -v f="$f" "$COMPARE" "$sheet_file" -) || {
      echo "check-drift: value comparison failed on $f (awk error)" >&2; fail=1; bad=""
    }
    if [ -n "$bad" ]; then printf '%s\n' "$bad" >&2; fail=1; fi
  fi

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
echo "check-drift: tokens resolve, values match the sheet, and suits carry U+FE0E ($mentions distinct mentions across $files cards)"
