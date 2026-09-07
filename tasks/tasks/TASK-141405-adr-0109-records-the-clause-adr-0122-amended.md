---
schema: 2
id: TASK-141405
title: ADR-0109 records the clause ADR-0122 amended
type: task
status: done
parent: STORY-1414
module: docs
estimate: XS
tier: haiku
review: light
files_touched: 2
labels: [docs, adr]
depends_on: [TASK-141404]
verify:
  - awk 'index($0, "ADR-0122-call-names-the-price-and-raise-to-names-the-total.md") { n++ } END { exit (n != 1) }' docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md
  - awk 'index($0, "**Status:** Accepted — ") { n++ } END { exit (n != 1) }' docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md
  - awk 'index($0, "**Status:** Accepted") { n++ } END { exit (n != 1) }' docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md
  - awk 'END { exit (NR != 251) }' docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md
  - awk 'index($0, "The table marks the last act, and the next deal clears it | Accepted — resolves") && index($0, "Amended by 0122") { n++ } END { exit (n != 1) }' docs/adr/README.md
  - awk 'index($0, "Amended by 0122") { n++ } END { exit (n != 1) }' docs/adr/README.md
  - awk 'index($0, "The table marks the last act, and the next deal clears it | Accepted — resolves") { n++ } END { exit (n != 1) }' docs/adr/README.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0109`'s own header and its row in the ADR index say that `ADR-0122` amended §2, so a reader who
opens either one is not told that the last-act mark prints a call's total.

## Files

| File | Action |
| --- | --- |
| `docs/adr/ADR-0109-the-table-marks-the-last-act-and-the-next-deal-clears-it.md` | modify |
| `docs/adr/README.md` | modify |

Read [`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md)'s
*Amends* bullet (the fifth in its header) and `docs/adr/ADR-0027`'s Status line, which is the house
form this ticket copies. **Nothing else is opened.**

## Scope

- **This is bookkeeping `ADR-0122`'s own PR owed and did not write.** That PR annotated `ADR-0122`'s
  index row — *"amends `ADR-0109` §2's `Call` clause"* — but left `ADR-0109` itself reading
  `- **Status:** Accepted` and left its index row summarising a rule the product no longer follows:
  *"with the act event's own `to` total on `Call`/`Bet`/`Raise to`/`All in`"*. Measured on `develop`:
  `Amended by 0122` appears nowhere in `docs/adr/`.
- **Line 3 of `ADR-0109`** becomes one line, in `ADR-0027`'s and `ADR-0061`'s form:

  ```markdown
  - **Status:** Accepted — §2's `Call` figure amended by [`ADR-0122`](ADR-0122-call-names-the-price-and-raise-to-names-the-total.md)
  ```

- **`ADR-0109`'s row in `docs/adr/README.md`** keeps every word it has and gains a sentence at the
  **end** of its status cell, before the closing `|`:

  ```markdown
  **Amended by 0122** — §2's figure list loses `Call`: the mark is bare on a call, because `PlayerCalled` carries only `to` and the price the button now names is unrecoverable a tick later
  ```

- **The body of `ADR-0109` is not edited, and neither is the summary already in its row.** *An ADR
  records what was decided when it was decided* — `ADR-0068`'s own precedent, where the stale
  paragraph was left standing under an amendment note rather than rewritten. The note is how a
  reader is warned; a rewrite would erase the fact that the rule changed.
- **Both files change by edit, never by insertion.** For `ADR-0109` that is enforced by a line-count
  gate at **251**. For the index it is enforced differently and deliberately: the annotation must
  land on a line that already carries `The table marks the last act, and the next deal clears it |
  Accepted — resolves`, which is the index row's own opening and appears exactly once. A line count
  is not used there, because `docs/adr/README.md` legitimately gains a row every time an ADR merges
  and this story is not the only thing in flight.

## Out of scope

- **`DEC-117`'s row** in `docs/adr/README.md`'s decision register — it links `ADR-0109` too, which
  is why the gates anchor on the **index row's** own opening rather than on the filename, and why
  `Amended by 0122` must appear exactly once in the whole file. That row records what `DEC-117`
  decided on 2026-09-02 and is answered; `DEC-137`'s row in the same register already states the
  amendment in full. Annotating both would put the same correction in two registers and make the
  second one the stale copy.
- **`ADR-0101`, `ADR-0107` and `ADR-0111`.** `ADR-0122` applies them and touches none, and says so
  clause by clause in its header.
- **`tasks/BOARD.md`'s prose copies** of either summary. The board is a status register, not the
  decision of record, and nothing there is corrected by this ticket beyond its own status cell.
- **Any client or design file.** By the time this merges they are already correct.

## Tests

None: this is a documentation ticket, and the `verify:` block is the whole assertion. It is written
as exact counts so that an edit in the wrong row, an edit in both rows, and an inserted line are
three different failures:

| Gate | Proves |
| --- | --- |
| `ADR-0122-….md` appears once in `ADR-0109` | the Status line links the amending ADR, once |
| `**Status:** Accepted — ` and `**Status:** Accepted` each appear once | the Status line gained the clause and did not gain a second Status line |
| `ADR-0109`'s file is 251 lines | the body was annotated, not rewritten |
| a line carries the index row's opening (`The table marks the last act, and the next deal clears it \| Accepted — resolves`) **and** `Amended by 0122`, exactly one | the annotation landed on the existing index row — not on `DEC-117`'s, and not on a line the coder added |
| `Amended by 0122` appears once in the whole index | it did not land twice |
| that same row opening appears exactly once | the row was edited, not duplicated |

## Acceptance criteria

- [ ] `docs/adr/ADR-0109-….md` line 3 reads
      `- **Status:** Accepted — §2's \`Call\` figure amended by [\`ADR-0122\`](…)` and the file is
      still 251 lines
- [ ] `docs/adr/README.md`'s `ADR-0109` row ends with the `**Amended by 0122**` sentence, on the row
      it already had, and `DEC-117`'s row is unchanged
- [ ] No other line of either file differs from the commit this ticket branches from
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
