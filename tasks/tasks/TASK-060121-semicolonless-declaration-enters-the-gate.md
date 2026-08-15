---
schema: 2
id: TASK-060121
title: The value gate reads CSS values correctly
type: task
status: blocked
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
---

## Goal

The value gate's reader is a regex, `--pd-NAME:[^;]*;`, that knows nothing about
strings. Two silences follow from that one fact, and they are the same bug:

- **A declaration written without its terminating semicolon.** With nothing after it
  the reader never sees it — the sheet's own last declaration, today
  `--pd-shadow-card` — so drifting it passes every clause green. With text after it
  the walk runs to the next `;` and swallows that text into the value.
- **A semicolon inside a quoted value.** `url("data:image/svg+xml;utf8,…")` truncates
  at the semicolon in the string, identically on both sides, so the sheet and a
  drifted card compare equal and the drift passes green (#511 review, reproduced).

Both need the reader to know where a string starts and ends. **`DEC-035` blocks this
ticket**: what that costs is a real decision, and three attempts to guess it have each
shipped a worse defect than the one they fixed.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — per the ADR that answers `DEC-035` |

## Scope

- Nothing until `DEC-035` is answered. The answer decides the shape: a real parser
  (which costs the gate its stock-tools property, `ADR-0024`'s standing constraint),
  a convention lint that refuses shapes the reader cannot read, or an accepted and
  recorded limit with the tree kept inside it.

## Out of scope

- The reader's comment- and prose-blindness — `TASK-060123`.

## Tests

None yet; the answering ADR sets the proof obligation.

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
