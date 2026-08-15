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
#   4. Graphics mirrors (TASK-060112): SVGs cannot var(), so each names the tokens it
#      mirrors as `pd-NAME (#hex)` in its head comment. The sheet must declare the
#      name with exactly that value, the cited hex must still paint an attribute in
#      the file, an SVG with no pairs fails, and so does finding no SVGs at all —
#      the class must never go invisible again, vacuously or otherwise.
#   5. Inlined symbols (TASK-060113): a card that inlines a `<symbol id="pd-…">` block
#      carries a copy of canonical geometry; the copy must equal the same-id symbol in
#      a graphics SVG, compared with XML comments stripped and whitespace runs
#      collapsed, so an asset retune can never leave a card silently stale.
#   6. Lockup anatomy (TASK-060114, per ADR-0033): the wordmark's em constants —
#      gap and letter-spacing on `.mark`; width, height and the inset ring on
#      `.mark .coin` (the 0.92em coin constant pins both axes) — are born in
#      graphics/wordmark.html; any other card whose rules target `.coin` under
#      `.mark` must carry the same values (values, deliberately not declaration
#      blocks — a conformant copy folds shared-`.coin` properties in). A card
#      copying only `.mark` stays outside the clause: the recorded residual gap.
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
# and both must land on the same normalized form. A declaration ends at `;` or at
# its block's `}`: a final declaration legally omits the semicolon, and requiring
# one made the sheet's last token escape the set while a card's garbled forward
# into a false report (TASK-060121). The `--pd-` name charset below is the same
# language as the name gate's grep in the loop — a change to what a token name may
# contain must change both.
EXTRACT='
  { buf = buf $0 "\n" }
  END {
    while (match(buf, /--pd-[a-z0-9-]*[ \t]*:[^;{}]*[;}]/)) {
      d = substr(buf, RSTART, RLENGTH); buf = substr(buf, RSTART + RLENGTH)
      out = ""; inq = 0; q = ""
      for (i = 1; i <= length(d); i++) {
        c = substr(d, i, 1)
        if (inq) { out = out c; if (c == q) inq = 0; continue }
        if (c == "\"" || c == "\047") { q = c; inq = 1; out = out c; continue }
        if (c == " " || c == "\t" || c == "\n" || c == "\r") continue
        out = out c
      }
      sub(/:/, "=", out); sub(/[;}]$/, "", out)
      print out
    }
  }
'
# the one sheet-set loader kernel, spliced into both consumers below so the HTML
# value gate and the SVG pair gate can never disagree about how the sheet parses
LOAD='eq = index($0, "="); if (eq) val[substr($0, 1, eq - 1)] = substr($0, eq + 1)'
# flags card declarations whose name the sheet knows but whose value differs; names
# the sheet lacks stay the name gate speaking alone, not a double report. The sheet
# set arrives as the first input file — awk -v would reprocess backslash escapes and
# silently mangle a value that ever gains one, so the set never travels through -v.
COMPARE='
  NR == FNR {
    if ($0 == "") next
    ok[$0] = 1
    '"$LOAD"'
    next
  }
  $0 != "" && !ok[$0] {
    eq = index($0, "="); nm = substr($0, 1, eq - 1)
    if (nm in val) printf "drift: %s declares %s as %s, but the sheet says %s\n", f, nm, substr($0, eq + 1), val[nm]
  }
'
# the SVG pair judge, same spliced loader
PAIRCHECK='
  NR == FNR { if ($0 != "") { '"$LOAD"' } next }
  {
    nm = "--" $1
    hex = $2; gsub(/[()]/, "", hex)
    if (!(nm in val)) printf "check-drift: %s cites %s, which tokens.css does not declare\n", g, $1
    else if (val[nm] != hex) printf "check-drift: %s cites %s as %s, but the sheet says %s\n", g, $1, hex, val[nm]
  }
'
# every suit sweep, HTML and SVG alike, through one function: grep exits 0 on match,
# 1 on clean no-match; anything else means the file was never actually read, which
# must fail as loudly as a bad file
sweep_suits() {
  st=0; grep -nE -- "$BARE" "$1" >&2 || st=$?
  if [ "$st" -eq 0 ]; then
    echo "check-drift: bare suit glyph (no U+FE0E) in $1" >&2; fail=1
  elif [ "$st" -ne 1 ]; then
    echo "check-drift: grep error $st sweeping $1 for bare suits" >&2; fail=1
  fi
  st=0; grep -nE -- "$ENTITY" "$1" >&2 || st=$?
  if [ "$st" -eq 0 ]; then
    echo "check-drift: entity-form suit or selector in $1 — write the literal glyph plus literal U+FE0E" >&2; fail=1
  elif [ "$st" -ne 1 ]; then
    echo "check-drift: grep error $st sweeping $1 for entity suits" >&2; fail=1
  fi
}
# self-test both halves before trusting their silence, like the suit sweep above; the
# || true keeps a hard awk death inside the comparison below, where the message says
# what broke, instead of dying with awk usage noise under set -e. Scratch files are
# left for the OS to purge, the trade-off this repo records in its gate tickets.
probe=$(printf -- '--pd-probe :  rgba(0, 0, 0, 0.4) ;\n' | awk "$EXTRACT" 2>/dev/null || true)
[ "$probe" = "--pd-probe=rgba(0,0,0,0.4)" ] \
  || { echo "check-drift: self-test failed — value extractor broke (awk?)" >&2; exit 1; }
probe2=$(printf -- ':root { --pd-a: 1px; --pd-b: 2px }\n' | awk "$EXTRACT" 2>/dev/null || true)
[ "$probe2" = "$(printf -- '--pd-a=1px\n--pd-b=2px')" ] \
  || { echo "check-drift: self-test failed — a semicolon-less final declaration escaped (awk?)" >&2; exit 1; }
probe_sheet=$(mktemp)
printf -- '--pd-probe=1px\n' > "$probe_sheet"
mism=$(printf -- '--pd-probe=2px\n' | awk -v f=self "$COMPARE" "$probe_sheet" - 2>/dev/null || true)
[ -n "$mism" ] \
  || { echo "check-drift: self-test failed — a drifted value went undetected" >&2; exit 1; }

sheet_file=$(mktemp)
awk "$EXTRACT" "$SHEET" > "$sheet_file"
[ -s "$sheet_file" ] || { echo "check-drift: no declarations extracted from $SHEET" >&2; exit 1; }

# pulls every <symbol id="pd-…">…</symbol> block, comments stripped and whitespace
# collapsed, as one `id<TAB>block` line, and a `!opens<TAB>N` trailer counting every
# <symbol open tag — the caller compares N to the extracted lines, so a copy the
# strict pattern cannot read (single quotes, a non-pd id, a nested symbol truncating
# the walk) fails loudly instead of silently leaving enforcement. The lookbehind
# keeps data-id= from mis-keying a block.
SYMEXTRACT='
  my $src = do { local $/; <STDIN> };
  $src =~ s/<!--.*?-->//gs;
  my $opens = () = $src =~ /<symbol\b/g;
  while ($src =~ /(<symbol\b[^>]*(?<![-\w])id="(pd-[a-z0-9-]+)"[^>]*>.*?<\/symbol>)/gs) {
    my ($blk, $id) = ($1, $2);
    $blk =~ s/\s+/ /g;
    print "$id\t$blk\n";
  }
  print "!opens\t$opens\n";
'
# runs the extractor on one file and refuses any open-tag the strict walk missed;
# emits only the id lines on stdout
extract_syms() {
  _out=$(perl -e "$SYMEXTRACT" < "$1" 2>/dev/null) || {
    echo "check-drift: symbol extraction failed on $1 (perl error)" >&2; return 1
  }
  _opens=$(printf '%s\n' "$_out" | awk -F '\t' '$1 == "!opens" { print $2 }')
  _strict=$(printf '%s\n' "$_out" | grep -c '^pd-' || true)
  if [ "${_opens:-0}" -ne "$_strict" ]; then
    echo "check-drift: $1 has ${_opens:-0} <symbol> tags but $_strict readable pd- blocks — a symbol copy is malformed (quoting? id prefix? nesting?)" >&2
    return 1
  fi
  printf '%s\n' "$_out" | grep '^pd-' || true
}
symprobe=$(printf '<symbol id="pd-x" a="1">\n  <p/> <!-- c -->\n</symbol>\n' | perl -e "$SYMEXTRACT" 2>/dev/null || true)
[ "$symprobe" = "$(printf 'pd-x\t<symbol id="pd-x" a="1"> <p/> </symbol>\n!opens\t1')" ] \
  || { echo "check-drift: self-test failed — symbol extractor broke (perl?)" >&2; exit 1; }
# pulls the lockup anatomy — gap and letter-spacing from every .mark rule, width,
# height and the inset ring from every rule whose subject is .coin under .mark —
# as one `anat-NAME=value` line each. Rules are walked block-wise (combinators,
# compound classes and rules inside @media all count), every declaration of a
# property is collected, and a property whose declarations disagree is reported as
# a conflict rather than half-compared. CSS comments are stripped quote-aware so a
# stray /* inside content cannot swallow the lockup. The ring is the fourth length
# token of the inset layer, wherever that layer sits in a shadow list.
ANATOMY='
  my $src = do { local $/; <STDIN> };
  $src =~ s/<!--.*?-->//gs;
  # only style blocks are CSS — prose apostrophes must never flip the string tracker
  my $css = join "", $src =~ /<style[^>]*>(.*?)<\/style>/gis;
  $css = $src unless $src =~ /<style/i;
  my ($out, $q, $i) = ("", "", 0);
  while ($i < length $css) {
    my $c = substr($css, $i, 1);
    if ($q) { $out .= $c; $q = "" if $c eq $q; $i++; next }
    if ($c eq "\"" || $c eq "\x27") { $q = $c; $out .= $c; $i++; next }
    if ($c eq "/" && substr($css, $i, 2) eq "/*") {
      my $e = index($css, "*/", $i + 2); last if $e < 0; $i = $e + 2; next;
    }
    $out .= $c; $i++;
  }
  $css = $out;
  my ($markbody, $coinbody) = ("", "");
  while ($css =~ /([^{}]+)\{([^{}]*)\}/gs) {
    my ($sels, $body) = ($1, $2);
    for my $s (split /,/, $sels) {
      $s =~ s/^\s+|\s+$//g;
      next if $s =~ /\@/;
      $markbody .= ";" . $body if $s =~ /(^|[\s>+~])\.mark(\.[-\w]+)*$/;
      $coinbody .= ";" . $body if $s =~ /(^|[\s>+~])\.mark(?![-\w])/ && $s =~ /\.coin(\.[-\w]+)*$/;
    }
  }
  exit 0 if $coinbody eq "";
  # quoted strings are opaque — a content string naming a property is not a declaration
  for ($markbody, $coinbody) { s/"[^"]*"|\x27[^\x27]*\x27/__str__/g }
  my $PAREN = qr/\((?:[^()]|\([^()]*\))*\)/;
  sub decls {
    my ($body, $prop, $paren) = @_;
    my %seen;
    while ($body =~ /(?<![-\w])$prop\s*:\s*((?:[^;{}()]|$paren)*)/gi) {
      my $v = $1; $v =~ s/^\s+|\s+$//g; $v =~ s/\s+/ /g;
      $seen{$v} = 1 if length $v;
    }
    return keys %seen;
  }
  my %v;
  my %want = (gap => [$markbody, "gap"], track => [$markbody, "letter-spacing"],
              width => [$coinbody, "width"], height => [$coinbody, "height"]);
  for my $k (sort keys %want) {
    my @d = decls(@{$want{$k}}, $PAREN);
    if (@d > 1) { print "conflict=$k\n"; exit 0 }
    $v{$k} = $d[0] if @d;
  }
  my @sh = decls($coinbody, "box-shadow", $PAREN);
  if (@sh > 1) { print "conflict=ring\n"; exit 0 }
  if (@sh) {
    # any inset layer may carry the ring — take the first that yields a 4th length
    for my $layer (split /,(?![^(]*\))/, $sh[0]) {
      next unless $layer =~ /\binset\b/i;
      my @tok = $layer =~ /((?:[^\s()]|$PAREN)+)/g;
      my @len = grep { !/^inset$/i && !/^(#|rgba?\(|hsla?\(|var\()/i } @tok;
      if (@len >= 4) { $v{ring} = $len[3]; last }
    }
  }
  my @missing = grep { !defined $v{$_} } qw(gap track width height ring);
  if (@missing) { print "missing=@missing\n"; exit 0 }
  print "anat-$_=$v{$_}\n" for qw(gap track width height ring);
'
anat_probe=$(printf '%s\n' \
  '/* c */ .other { gap: 9em; }' \
  '.mark { row-gap: 0; gap: 1em; letter-spacing: 2em /* tail */ }' \
  '@media (x) { .mark > .coin.big { WIDTH: 3em } }' \
  '.mark .coin { min-width: 9em; width: 3em; height: 5em;' \
  '  box-shadow: 0 1px 2px rgba(0,0,0,.5), inset 0 0 0 4em rgba(255,255,255,.25); }' \
  | perl -e "$ANATOMY" 2>/dev/null || true)
[ "$anat_probe" = "$(printf 'anat-gap=1em\nanat-track=2em\nanat-width=3em\nanat-height=5em\nanat-ring=4em')" ] \
  || { echo "check-drift: self-test failed — anatomy extractor broke (perl?)" >&2; exit 1; }
# the error arms are load-bearing too: prove conflict and missing still report
cprobe=$(printf '.mark { gap: 1em; letter-spacing: 1em; }\n.mark .coin { width: 1em; width: 2em; }\n' | perl -e "$ANATOMY" 2>/dev/null || true)
[ "$cprobe" = "conflict=width" ] \
  || { echo "check-drift: self-test failed — a conflicting lockup copy went unreported" >&2; exit 1; }
mprobe=$(printf '.mark .coin { width: 1em; }\n' | perl -e "$ANATOMY" 2>/dev/null || true)
case "$mprobe" in
  missing=*ring*) : ;;
  *) echo "check-drift: self-test failed — an incomplete lockup copy went unreported" >&2; exit 1 ;;
esac
WORDMARK="$DIR/graphics/wordmark.html"
canon_anat_file=$(mktemp)
perl -e "$ANATOMY" < "$WORDMARK" > "$canon_anat_file" 2>/dev/null \
  || { echo "check-drift: anatomy extraction failed on $WORDMARK" >&2; exit 1; }
grep -q '^anat-gap=' "$canon_anat_file" \
  || { echo "check-drift: could not read the lockup anatomy from $WORDMARK — the canonical broke ($(cat "$canon_anat_file"))" >&2; exit 1; }

canon_syms=$(mktemp)
canon_fail=0
for g in $(find "$DIR/graphics" -name '*.svg' 2>/dev/null | sort); do
  extract_syms "$g" >> "$canon_syms" || canon_fail=1
done
[ "$canon_fail" -eq 0 ] || exit 1
# an empty canonical set would make the NR==FNR comparison below swallow every card
# block as a canonical — the same vacuous-invisibility clause 4 forbids
[ -s "$canon_syms" ] || { echo "check-drift: no pd- symbols extracted from any graphics SVG — the canonical set went invisible" >&2; exit 1; }
dups=$(awk -F '\t' 'seen[$1]++ { print $1 }' "$canon_syms")
[ -z "$dups" ] || { printf 'check-drift: two canonicals claim the same symbol id: %s\n' "$dups" >&2; exit 1; }
# this BRE and EXTRACT's awk ERE describe the same name language — change both or neither
declared=$(grep -o -- '--pd-[a-z0-9]*\(-[a-z0-9]*\)*' "$SHEET" | sort -u)
fail=0
mentions=0
files=0
syms_total=0
anat_copies=0
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

  # a card whose rules target .coin under .mark copies the lockup — its anatomy
  # values must equal the canonical's, constant by constant
  if [ "$f" != "$WORDMARK" ]; then
    st=0; anat=$(perl -e "$ANATOMY" < "$f" 2>/dev/null) || st=$?
    if [ "$st" -ne 0 ]; then
      echo "check-drift: anatomy extraction failed ($st) on $f" >&2; fail=1; anat=""
    fi
    case "$anat" in
      "") : ;;
      missing=*) echo "check-drift: $f copies the lockup but lacks: ${anat#missing=}" >&2; fail=1 ;;
      conflict=*) echo "check-drift: $f declares ${anat#conflict=} more than once with disagreeing values in its lockup copy" >&2; fail=1 ;;
      *)
        bad=$(printf '%s\n' "$anat" | awk -v f="$f" '
          NR == FNR { eq = index($0, "="); if (eq) c[substr($0, 1, eq - 1)] = substr($0, eq + 1); next }
          { eq = index($0, "="); nm = substr($0, 1, eq - 1); v = substr($0, eq + 1)
            if (nm in c && c[nm] != v)
              printf "check-drift: %s copies the lockup %s as %s, but the canonical carries %s\n", f, substr(nm, 6), v, c[nm] }
        ' "$canon_anat_file" -) || { echo "check-drift: anatomy comparison failed on $f (awk error)" >&2; fail=1; bad=""; }
        if [ -n "$bad" ]; then printf '%s\n' "$bad" >&2; fail=1
        else anat_copies=$((anat_copies + 1)); fi
      ;;
    esac
  fi

  # inlined pd- symbol blocks must equal their same-id canonicals
  syms=$(extract_syms "$f") || { fail=1; syms=""; }
  if [ -n "$syms" ]; then
    bad=$(printf '%s\n' "$syms" | awk -F '\t' -v f="$f" '
      NR == FNR { canon[$1] = $2; next }
      $1 != "" {
        if (!($1 in canon)) printf "check-drift: %s inlines symbol %s with no same-id canonical under graphics/\n", f, $1
        else if (canon[$1] != $2) printf "check-drift: %s inlines symbol %s, which differs from its canonical\n", f, $1
      }
    ' "$canon_syms" -) || { echo "check-drift: symbol comparison failed on $f (awk error)" >&2; fail=1; bad=""; }
    if [ -n "$bad" ]; then printf '%s\n' "$bad" >&2; fail=1; fi
    syms_total=$((syms_total + $(printf '%s\n' "$syms" | grep -c .)))
  fi

  sweep_suits "$f"
done

svgs=0
pairs_total=0
for g in $(find "$DIR/graphics" -name '*.svg' 2>/dev/null | sort); do
  svgs=$((svgs + 1))
  # read errors are read errors, never convention verdicts — the st= discipline again
  st=0; pairs=$(grep -oE -- 'pd-[a-z0-9-]* \(#[0-9a-f]{6}\)' "$g") || st=$?
  if [ "$st" -gt 1 ]; then
    echo "check-drift: grep error $st reading $g for mirror pairs" >&2; fail=1; continue
  fi
  # every parenthesized hex must belong to a well-formed pair — a wrapped, uppercased,
  # alpha-lengthened or otherwise malformed pair would silently leave enforcement
  # while its siblings keep the SVG looking covered (id refs like url(#pd-coin-steel)
  # start with a non-hex letter and stay outside this count)
  cited=$(grep -oE -- '\(#[0-9a-fA-F]+\)' "$g" | grep -c . || true)
  strict=$([ -n "$pairs" ] && printf '%s\n' "$pairs" | grep -c . || echo 0)
  if [ "$cited" -ne "$strict" ]; then
    echo "check-drift: $g has $cited parenthesized hex citations but $strict well-formed 'pd-NAME (#hex)' pairs — a pair is malformed, wrapped, or uppercased" >&2
    fail=1
  fi
  if [ -z "$pairs" ]; then
    echo "check-drift: $g carries no pd-NAME (#hex) mirror pair — every graphics SVG names what it mirrors" >&2
    fail=1; continue
  fi
  pairs_total=$((pairs_total + strict))
  bad=$(printf '%s\n' "$pairs" | awk -v g="$g" "$PAIRCHECK" "$sheet_file" -) \
    || { echo "check-drift: pair comparison failed on $g (awk error)" >&2; fail=1; bad=""; }
  if [ -n "$bad" ]; then printf '%s\n' "$bad" >&2; fail=1; fi
  # a pair whose hex paints nothing is stale — the artwork moved on. XML comments are
  # stripped first (the citations live there, and comment prose must not vouch for
  # paint); any remaining occurrence is real — an attribute, style=, or a style block.
  for hx in $(printf '%s\n' "$pairs" | grep -oE -- '#[0-9a-f]{6}'); do
    if ! perl -0777 -pe 's/<!--.*?-->//gs' "$g" | grep -qi -- "$hx"; then
      echo "check-drift: $g cites $hx in a mirror pair but nothing outside comments carries it" >&2; fail=1
    fi
  done
  sweep_suits "$g"
done
[ "$svgs" -gt 0 ] || { echo "check-drift: no SVGs under $DIR/graphics — the graphics class went invisible" >&2; fail=1; }

# an empty tree must fail as loudly as a missing sheet — a vacuous pass guards nothing
[ "$files" -gt 0 ] || { echo "check-drift: no cards found under $DIR" >&2; exit 1; }
if [ "$fail" -ne 0 ]; then exit 1; fi
[ "$anat_copies" -eq 1 ] && copyword=copy || copyword=copies
echo "check-drift: tokens resolve, values match the sheet, suits carry U+FE0E, $pairs_total graphics pairs and $syms_total inlined symbols mirror truly, and the lockup anatomy holds across $anat_copies $copyword ($mentions distinct mentions across $files cards)"
