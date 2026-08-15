---
schema: 2
id: TASK-060121
title: Every token declaration ends in a semicolon, and the gate says so
type: task
status: done
parent: STORY-0601
module: design
estimate: XS
tier: opus
review: light
files_touched: 1
labels: [design]
depends_on: []
verify:
  - ./design/check-drift.sh
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-shadow-card:[^;]*;/--pd-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.4)/" "$T/design/tokens/tokens.css" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/--pd-focus-offset: 2px;/--pd-focus-offset: 2px/" "$T/design/screens/create-duel.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
  - sh -c 'T=$(mktemp -d) && cp -R design "$T/design" && perl -0777 -pi -e "s/0 1px 3px/0 1px 4px/" "$T/design/components/playing-card.html" || exit 2; "$T/design/check-drift.sh" >/dev/null 2>&1; [ $? -eq 1 ]'
---

## Goal

The value gate's reader stops at `;`, so a declaration written without its terminating
semicolon — legal CSS — is read wrongly in one of two silent ways: with nothing after
it the reader never sees it (the sheet's own last declaration, today
`--pd-shadow-card`), and with text after it the walk runs on to the next `;` and
swallows that text into the value. Either way a real drift can pass green.

The fix is to enforce the convention, not to parse around it.

## Files

| File | Action |
| --- | --- |
| `design/check-drift.sh` | edit — a terminator check beside the value gate, on every card and the sheet |

## Scope

- Two counts, no new parsing: every `--pd-NAME:` candidate must yield a record (one
  that yields none vanished for want of a following `;`), and no emitted value may
  carry a brace (a value that does ran past its own end into the next rule).
- The reader itself is untouched — byte-identical to the merged version — so no
  extraction behaviour changes and no existing comparison moves.
- Three self-tests: an unterminated final declaration must be refused, a declaration
  that swallowed its block must be refused, and a properly terminated pair must pass.
- Conservative by design: an unterminated `--pd-NAME:` written in prose or a code
  sample also fails, with a message naming the missing semicolon. A loud refusal of a
  legal-but-unread shape is the trade this gate makes everywhere else.

## Out of scope

- Every other clause of the gate; the reader's comment- and string-blindness is
  `TASK-060123`'s.

## Tests

None — the verify runs the gate on the aligned tree and proves both silences on
mutated scratch copies, plus that ordinary value drift is still caught.

## Acceptance criteria

- [ ] Every command in `verify:` exits 0.

## Definition of done

Standard, with `EPIC-06`'s recorded deviation: the review is visual, in claude.ai/design.

## Deviations

- The ticket's title and approach changed under review, twice. Extending the reader's
  terminator class to `[;}]` introduced five reproduced regressions: a braced value
  left the gate silently, comments and code samples read as live, a brace inside a
  quoted value truncated it. Rewriting the reader as a character walker — comment
  stripping, `<style>` scoping, quote tracking — introduced worse ones still: a `<!--`
  inside CSS blanked a card's whole value gate, an escaped quote wedged the walker so
  commented-out declarations read as live, and string content forged declarations
  (#511 review, rounds 1 and 2, each reproduced end to end).
- The lesson is recorded rather than re-learned: a shell gate should not parse CSS.
  Where a shape is hard to read, refuse it and name the convention. This version adds
  no parsing at all, which is why it cannot reintroduce that class.
