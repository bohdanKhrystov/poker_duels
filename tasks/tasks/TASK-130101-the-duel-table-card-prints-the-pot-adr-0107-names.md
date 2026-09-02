---
schema: 2
id: TASK-130101
title: The duel-table card prints the pot ADR-0107 names
type: task
status: done
parent: STORY-1301
module: design
estimate: XS
tier: haiku
review: light
files_touched: 1
labels: [design, table]
depends_on: []
verify:
  - awk 'index($0, "Pot&nbsp;2,850") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "2,450") { n++ } END { exit (n != 0) }' design/screens/duel-table.html
  - awk 'index($0, "3,650") { n++ } END { exit (n != 4) }' design/screens/duel-table.html
  - awk 'index($0, "committed <span class=\"amt\">400</span>") { n++ } END { exit (n != 2) }' design/screens/duel-table.html
  - awk 'index($0, "Pot&nbsp;3,250") { n++ } END { exit (n != 1) }' design/screens/duel-table-states.html
  - ./design/check-drift.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`design/screens/duel-table.html`'s two pot nodes read **`Pot 2,850`** — the quantity
[`ADR-0107`](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) §1 makes the
word `Pot` name — so the frame every later `EPIC-13` card composes carries a figure a merged ADR
agrees with, and the card stops disagreeing with its own sizing row.

## The card is in arrears, and by how much

Both frames draw the same hero decision: the rival `committed 400`, the hero offered `Call 400` and
therefore committed nothing this street, `Pot 2,450`.

```
Pot = 2,450 + 400 + 0 = 2,850        (ADR-0107 §1)
```

**The card's own sizing row already prices against 2,850.**
[`ADR-0101`](../../docs/adr/ADR-0101-pot-means-a-pot-sized-raise-and-the-fractions-share-its-base.md)
§4 works this exact frame at `P = 2,850`, `base = 3,250`, `pot chip = 3,650`, and `TASK-120914`
merged `3,650` into both frames on 2026-08-31. The pot node is the last number on this card still
quoting the collected pot, which is why `ADR-0107` §6 measures the correction rather than debating
it.

**Measured in the repository on 2026-09-02**, so the coder replaces literals and counts nothing:

| Literal | File | Count today |
| --- | --- | --- |
| `Pot&nbsp;2,450` | `design/screens/duel-table.html` | **2** — lines 189 and 248, one per frame |
| `2,450` | `design/screens/duel-table.html` | **2** — the same two nodes; there is no third occurrence, so replacing both is the whole change |
| `2,850` | `design/screens/duel-table.html` | **0** |
| `3,650` | `design/screens/duel-table.html` | **4** — two frames × (stepper readout, *Raise to* button); must not move |
| `Pot&nbsp;3,250` | `design/screens/duel-table-states.html` | **1** — its bet-line is empty, so it already agrees (`ADR-0107` §6) and must not move |

**Three other cards draw `2,450` and none of them moves**, checked rather than assumed:
`design/components/seat-and-pot.html`, `design/tokens/colors.html` and `design/tokens/type.html`
each show the pot strip as a *specimen*, with no bet-line beside it — so their collected pot and
their total are the same number and `ADR-0107` §1 leaves them exactly as they are. `ADR-0107` §6
names only this file for the same reason.

## Files

| File | Action |
| --- | --- |
| `design/screens/duel-table.html` | modify |
| `docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md` | read |

## Scope

- Change `Pot&nbsp;2,450` to `Pot&nbsp;2,850` in both `<span class="amount">` nodes — lines 189 and
  248 today.
- Same markup, same `class="amount"`, same `&nbsp;`, same comma at the thousand. Two literals, two
  lines.
- Nothing else in the file.

## Out of scope

- **Every other number on the card.** `Call 400`, the rival's `committed 400`, the stacks 4,150 and
  13,400, the blinds 75/150, the hand number, and the four `3,650`s: all already correct under
  `ADR-0101` §4 and `ADR-0107` §1, and three `verify:` commands pin them where they are.
- **`design/screens/duel-table-states.html`.** `ADR-0107` §6: its bet-line is empty, so its
  `Pot 3,250` already states the total. A `verify:` command pins it unchanged.
- **`seat-and-pot.html`, `colors.html`, `type.html`.** See the table above — no bet-line, no
  arrears, and `ADR-0107` §6 names none of them.
- **The bet-line itself.** `ADR-0107` §4 leaves whether the rival's `committed 400` keeps standing
  to `STORY-1306`'s card. The rival's chips being drawn twice is a cost the ADR's *Consequences*
  accepts by name; do not "fix" it here.
- **`docs/adr/README.md`'s `DEC-101` row**, which still reads *"`design/screens/duel-table.html` is
  in arrears by one number in two places"*. That sentence is about the `3,250` arrears
  `TASK-120914` closed on 2026-08-31 and is already stale; correcting a merged register entry is
  neither this ticket's nor this story's, and is not yet ticketed.

## Tests

**No test file, and none is possible.** A design card is HTML nobody imports; `ADR-0089` §2b
forbids a browser measurement being a gate, and this change is not a geometry anyway. The gates are
the `verify:` block's five content assertions plus `design/check-drift.sh`, and they are exhaustive
for a two-literal edit: two say what must now be there, three say what must not have moved.

Every count in them was measured on 2026-09-02 (see the table above), not computed.

## Acceptance criteria

- [ ] `Pot&nbsp;2,850` appears exactly twice in `design/screens/duel-table.html`
- [ ] `2,450` appears zero times in `design/screens/duel-table.html`
- [ ] `3,650` still appears exactly four times in `design/screens/duel-table.html`
- [ ] `committed <span class="amt">400</span>` still appears exactly twice in
      `design/screens/duel-table.html`
- [ ] `Pot&nbsp;3,250` still appears exactly once in `design/screens/duel-table-states.html`
- [ ] `./design/check-drift.sh` exits 0
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket:
`verify` green, review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into
`develop`. Not done until the PR is merged.
