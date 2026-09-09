---
schema: 2
id: TASK-141203
title: The card-text register ADR-0142 owes
type: task
status: done
parent: STORY-1412
module: web-client
estimate: S
tier: sonnet
review: standard
files_touched: 2
labels: [client, design, process]
depends_on: []
verify:
  - cd web-client && npm ci
  - cd web-client && FORCE_COLOR=0 NO_COLOR=1 npm run --silent check
  - cd web-client && test -f src/design/card-text.test.ts
  - sh -c '! grep -qF "ANON-BLOCK" design/screens/account.html'
  - ./design/check-drift.sh
  - ./design/check-frame-cards.sh
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`ADR-0142` is merged and **has no implementing ticket anywhere**. This is it.

A vitest file in the client's own suite reads each paired card's rendered text, comments stripped,
and matches every carded export's **imported value** against it — with the `carded`/`notCarded`
partition checked **both ways**. The `ANON-BLOCK` markers are **deleted**, not extended.

## Why this is its own ticket

Found by the planner splitting `STORY-1410` and `STORY-1412`, which went looking for the ticket that
owed `ADR-0142`'s register and found none. The architect's report said the planner would write it;
the planner I dispatched was briefed on two other stories, so it fell between them.

That is exactly the failure `ADR-0142` itself is about — an obligation recorded in a document that
nothing executable enforces — so leaving it unwritten would be the joke telling itself.

## Files

| File | Action |
| --- | --- |
| `web-client/src/design/card-text.test.ts` | create |
| `design/screens/account.html` | modify |
| `web-client/src/account/account-text.ts` | **not modified** — listed here when I wrote the ticket, on the assumption it would need classification comments. It does not: the gate **imports** its values rather than editing the source, and `SIGN_OUT_WARNING`'s hand-concatenated form is handled by that import. The coder flagged the discrepancy instead of inventing an edit to match the count, which was right |

Read [`ADR-0142`](../../docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) in
full — it specifies the mechanism, and this ticket implements exactly what it says.
**Nothing outside the table above is changed.**

## Scope

Per `ADR-0142`:

- the register naming which module pairs with which card, and which exports are `carded` vs
  `notCarded`;
- the test reading each card's **rendered** text with comments stripped, on a **NUL** sentinel rather
  than a newline — a newline separator cuts wrapped `<p class="line">` sentences in half and reports
  strings absent from a card that plainly carries them, which is the bug that produced the
  `ANON-BLOCK` markers in the first place;
- the partition checked **both ways**, so a new export must be classified rather than silently
  ignored;
- `ANON-BLOCK` comments removed from `account.html`, since the reason for them is gone.

The two `account-text.ts` exports `STORY-1410` adds must be classified by whichever of the two
tickets lands second — `STORY-1410`'s *Out of scope* records the same obligation from its side.

## Out of scope

- **Widening `check-frame-cards.sh`.** It guards one card by design (`TASK-140720`).
- **Choosing any word.** Which strings a card carries stays the card's decision under `ADR-0091` §2;
  `ADR-0142` §5 chooses no word and neither does this.
- **`ADR-0126` §3's `BestHand.cards` KDoc repair**, which `ADR-0126` says is not this epic's.

## Tests

| Test | What it refuses |
| --- | --- |
| every carded export appears in its card's rendered text | a module string that drifted from the words the design gate holds |
| every export is classified `carded` or `notCarded` | a new export slipping in unclassified and unchecked |

## Acceptance criteria

1. The gate is **shown red** against the defect it names: change *ladder* to *leaderboard* on
   `name-ask.html`, confirm the run fails naming `NAME_IS_FOR`, restore with `cp` and `cmp -s`.
2. Adding an unclassified export to a paired module fails the partition check; revert it.
3. `grep -F ANON-BLOCK design/screens/account.html` finds nothing.
4. `SIGN_OUT_WARNING`'s hand-concatenated value is handled — the gate **imports** values rather than
   grepping sources, since that string occurs zero times as a literal in its own file.

## Definition of done

- [ ] Every command in `verify:` exits 0.
- [ ] Both probes in criteria 1 and 2 were run, observed red, and reverted.
- [ ] PR merged into `develop`.
