---
schema: 2
id: TASK-060121
title: The value gate reads CSS values correctly
type: task
status: done
parent: STORY-0601
module: design
estimate: S
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s|\#9fb2c4;|\#9fb2c4; --pd-probe:url(\"data:x;GOOD\");|" "$T/design/tokens/tokens.css" && perl -0777 -pi -e "s|\#9fb2c4;|\#9fb2c4; --pd-probe:url(\"data:x;DRIFTED\");|" "$T/design/screens/duel-end.html" && grep -q DRIFTED "$T/design/screens/duel-end.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s|--pd-shadow-card:[^;]*;|--pd-shadow-card:0 1px 9px rgba(0,0,0,0.4)|" "$T/design/tokens/tokens.css" && grep -q "1px 9px" "$T/design/tokens/tokens.css" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

The value gate's reader is a regex, `--pd-NAME:[^;]*;`, that knows nothing about
strings. Three shapes follow from that one fact, and they are the same bug:

- **A declaration written without its terminating semicolon.** With nothing after it
  the reader never sees it; with text after it the walk runs to the next `;` and
  swallows that text into the value. Counterfactual today — every declaration in the
  tree carries its semicolon — but the sheet's last declaration is one edit away from
  it.
- **A semicolon inside a quoted value.** `url("data:image/svg+xml;utf8,…")` truncates
  at the semicolon in the string, identically on both sides, so the sheet and a
  drifted card compare equal and the drift passes green (#511 review, reproduced).
- **A quoted string wrapped across sheet lines** — the *loud* one, and the live one.
  The reader preserves whitespace inside quotes and strips it outside, so a sheet
  wrapping `"Segoe UI"` mid-string yields a **false** drift against a card that does
  not. `tokens.css` already wraps `--pd-font-ui` and `--pd-font-mono`, both holding
  quoted names with spaces, and all 14 cards declare them: one reflow that moves a wrap
  point inside a quoted name reddens the gate across all of them.

All three need the reader to know where a string starts and ends. `DEC-035` asked what
that costs; [`ADR-0034`](../../docs/adr/ADR-0034-the-value-gate-reads-css-string-aware.md)
answered it, and this ticket implements that answer.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — `EXTRACT` becomes `VALUES`, the string-aware reader `ADR-0034` specifies |

## Scope

- `VALUES` replaces `EXTRACT` to `ADR-0034`'s seven-point contract: CSS regions, not
  files; comments stripped after the region and string-aware; strings opaque and
  escape-aware; a value ending at the first `;` at paren depth zero or its block's
  `}`; whitespace normalized outside strings and collapsed inside them so a wrapped
  string equals its one-line spelling; and total-or-loud — an unterminated string or
  comment, an unclosed `<style>`, or a value opening a `{` names the file and fails.
- The emitted normal form does not change, so `LOAD`, `COMPARE` and `PAIRCHECK` are
  untouched.

## Out of scope

- The lockup clause's missing count floor — `TASK-060123`.

## Tests

None — the proof is the ADR's obligation, discharged four ways: the readable language
is written into the script header; fourteen inline probes cover each shape and each
refusal; the verify carries a mutation fixture per silence, both red on the tree before
this change; and the swap is proved inert — `VALUES`' output is byte-identical to
`EXTRACT`'s across all seventeen files under `design/`.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

- Three approaches were built and each was withdrawn under review, all reproduced end
  to end (#511, rounds 1–3). Extending the terminator to `[;}]` made a braced value
  leave the gate silently and turned comments and code samples into live CSS.
  Rewriting the reader as a character walker was worse: a `<!--` inside CSS blanked a
  card's whole value gate, one escaped quote wedged the walker so commented-out
  declarations read as live, and string content forged declarations. Enforcing the
  semicolon by counting candidates against records neither reached the quoted-`;`
  silence nor survived a legally wrapped quoted value, which hard-failed the sheet.
- The pattern is the finding: a shell gate cannot parse CSS by accretion, and each
  attempt cost a review round. That is what `DEC-035` exists to settle, and why this
  ticket is `blocked` rather than attempted a fourth time.
- The bug is latent today — no token holds a quoted `;`, `{` or `}`, and every
  declaration in the tree carries its semicolon — so blocking costs nothing now.
