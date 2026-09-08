#!/bin/sh
# Poker Duels — per-frame rival-hand gate for duel-table-states.html only; all other cards
# under design/screens/ are outside scope (TASK-141107).
#
# TASK-141102's gates are whole-file aggregate counts over
# design/screens/duel-table-states.html: 29 `class="pc"`, 4 `back mucked`, and so on.
# A whole-file count cannot tell "the right frame reveals" from "some frame reveals
# and another silently does not" — swapping the rival's <div class="oppcards"> slot
# between the fold frame and the showdown-lost frame moves nothing in or out of the
# file, so every one of those totals stays exactly the same while the fold frame
# shows a folded hand face up. This gate reads the card frame by frame instead, and
# checks the rival's own card slot inside each named frame.
#
# Takes no argument: resolves its own directory, like check-drift.sh, and reads
# screens/duel-table-states.html beside itself.
set -eu

DIR=$(dirname "$0")
CARD="$DIR/screens/duel-table-states.html"
[ -f "$CARD" ] || { echo "check-frame-cards: missing $CARD" >&2; exit 1; }

# Cuts the card into <div class="frame"> blocks (nesting counted per line by
# literal "<div" opens and "</div>" closes — no self-closing divs occur in this
# card), names each block by the text of its first <h2>, then finds that block's
# own single <div class="oppcards"> slot the same way and counts, inside that slot
# only, class="pc", class="pc red", class="back (either back variant) and
# class="back mucked". Emits one heading\tpc\tpcred\tback\tbackmucked line per
# frame on success; refuses loudly, naming what broke, otherwise.
FRAMEPROG='
function opens(line,    t) { t = line; return gsub(/<div/, "&", t) }
function closes(line,    t) { t = line; return gsub(/<\/div>/, "&", t) }
{
  line = $0
  if (!in_frame) {
    if (index(line, "<div class=\"frame\">") == 0) next
    in_frame = 1; depth = 0; nlines = 0
  }
  nlines++
  blines[nlines] = line
  depth += opens(line) - closes(line)
  if (depth == 0) { process_frame(); in_frame = 0 }
}
function process_frame(    i, j, ln, o, c, heading, slots, slot_start, slot_end,
                            sdepth, nested, pc, pcr, bk, bkm, t) {
  frame_count++
  heading = ""
  for (i = 1; i <= nlines; i++) {
    if (match(blines[i], /<h2>[^<]*<\/h2>/)) {
      heading = substr(blines[i], RSTART + 4, RLENGTH - 9)
      break
    }
  }
  if (heading == "") {
    printf "check-frame-cards: a <div class=\"frame\"> has no readable <h2> heading\n" > "/dev/stderr"
    fail = 1; return
  }
  slots = 0
  for (i = 1; i <= nlines; i++) {
    if (index(blines[i], "<div class=\"oppcards\">") > 0) { slots++; if (slots == 1) slot_start = i }
  }
  if (slots != 1) {
    printf "check-frame-cards: %s holds %d <div class=\"oppcards\"> slots, not 1 — there is no single home to count in\n", heading, slots > "/dev/stderr"
    fail = 1; return
  }
  sdepth = 0; nested = 0; slot_end = 0
  for (j = slot_start; j <= nlines; j++) {
    ln = blines[j]
    o = opens(ln); c = closes(ln)
    if (j > slot_start && o > 0) nested = 1
    sdepth += o - c
    if (sdepth == 0) { slot_end = j; break }
  }
  if (slot_end == 0) {
    printf "check-frame-cards: %s — the oppcards slot never closes\n", heading > "/dev/stderr"
    fail = 1; return
  }
  if (nested) {
    printf "check-frame-cards: %s — the oppcards slot holds a nested <div>, so its own </div> is missing and the count already ran past the slot it names\n", heading > "/dev/stderr"
    fail = 1; return
  }
  pc = 0; pcr = 0; bk = 0; bkm = 0
  for (j = slot_start; j <= slot_end; j++) {
    ln = blines[j]
    t = ln; pc += gsub(/class="pc"/, "&", t)
    t = ln; pcr += gsub(/class="pc red"/, "&", t)
    t = ln; bk += gsub(/class="back/, "&", t)
    t = ln; bkm += gsub(/class="back mucked"/, "&", t)
  }
  headings[frame_count] = heading
  pcv[frame_count] = pc; pcrv[frame_count] = pcr
  bkv[frame_count] = bk; bkmv[frame_count] = bkm
}
END {
  if (in_frame) {
    printf "check-frame-cards: a <div class=\"frame\"> never closes\n" > "/dev/stderr"
    exit 1
  }
  if (frame_count == 0) {
    printf "check-frame-cards: the card holds 0 frames\n" > "/dev/stderr"
    exit 1
  }
  if (fail) exit 1
  for (i = 1; i <= frame_count; i++)
    printf "%s\t%d\t%d\t%d\t%d\n", headings[i], pcv[i], pcrv[i], bkv[i], bkmv[i]
}
'

data=$(awk "$FRAMEPROG" "$CARD") || exit 1

# The expectation table (§4): measured against the merged card at 96b9a278, not
# computed. Fields: heading anchor (a substring test, never a line number or an
# ordinal), then the rival oppcards slot's class="pc", class="pc red",
# class="back (either variant) and class="back mucked" counts it must hold.
EXPECT='Fold —|0|0|2|2
Showdown — ImKate shows|1|1|0|0'

fail=0
gated=""
IFS='
'
for row in $EXPECT; do
  anchor=$(printf '%s' "$row" | awk -F'|' '{print $1}')
  epc=$(printf '%s' "$row" | awk -F'|' '{print $2}')
  epr=$(printf '%s' "$row" | awk -F'|' '{print $3}')
  ebk=$(printf '%s' "$row" | awk -F'|' '{print $4}')
  ebkm=$(printf '%s' "$row" | awk -F'|' '{print $5}')

  matches=$(printf '%s\n' "$data" | awk -F'\t' -v a="$anchor" 'index($1, a) > 0')
  cnt=0
  [ -n "$matches" ] && cnt=$(printf '%s\n' "$matches" | grep -c .)
  if [ "$cnt" -ne 1 ]; then
    echo "check-frame-cards: heading anchor '$anchor' matches $cnt frames, not 1 — a renamed heading would otherwise silently gate nothing" >&2
    fail=1
    continue
  fi

  aheading=$(printf '%s\n' "$matches" | awk -F'\t' '{print $1}')
  apc=$(printf '%s\n' "$matches" | awk -F'\t' '{print $2}')
  apr=$(printf '%s\n' "$matches" | awk -F'\t' '{print $3}')
  abk=$(printf '%s\n' "$matches" | awk -F'\t' '{print $4}')
  abkm=$(printf '%s\n' "$matches" | awk -F'\t' '{print $5}')
  gated="$gated
$aheading"

  if [ "$apc" -ne "$epc" ] || [ "$apr" -ne "$epr" ] || [ "$abk" -ne "$ebk" ] || [ "$abkm" -ne "$ebkm" ]; then
    echo "check-frame-cards: $aheading — expected pc=$epc pcred=$epr back=$ebk backmucked=$ebkm in its oppcards slot, found pc=$apc pcred=$apr back=$abk backmucked=$abkm" >&2
    fail=1
  fi
done
unset IFS

[ "$fail" -eq 0 ] || exit 1

# §6: report every frame, gated or not, one line each — so a frame added later
# shows up in the gate's own output instead of quietly falling outside it. The
# gated-heading list travels as the first input file — -v mangles a multi-line
# value in some awks — the same discipline check-drift.sh uses for its sheet set.
gated_file=$(mktemp)
printf '%s\n' "$gated" > "$gated_file"
frame_count=$(printf '%s\n' "$data" | grep -c .)
printf '%s\n' "$data" | awk -F'\t' '
  NR == FNR { if ($0 != "") g[$0] = 1; next }
  {
    state = ($1 in g) ? "gated" : "ungated"
    printf "check-frame-cards: %s — %s: pc=%s pcred=%s back=%s backmucked=%s\n", $1, state, $2, $3, $4, $5
  }
' "$gated_file" -
echo "check-frame-cards: duel-table-states.html — $frame_count frames: the rival oppcards slot holds the right cards in every gated frame"
