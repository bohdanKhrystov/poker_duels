---
id: STORY-1301
title: Pot names every chip committed to the hand
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, design, table]
depends_on: []
---

## Goal

The word `Pot` on the duel table names the same quantity the sizing row prices against —
`view.pot` plus both seats' `committedThisStreet` — so no hand ever opens at `Pot 0`, and the two
merged design cards print that number too.

## Why

**It is the first story of `EPIC-13` because it is the one that changes a figure every later card
copies.** `design/screens/duel-table.html` draws `Pot 2,450` in two nodes and
`duel-table-states.html` draws `Pot 3,250`; under
[`ADR-0107`](../../docs/adr/ADR-0107-pot-names-every-chip-committed-to-the-hand.md) §6 the first
card's two nodes become **2,850** and the second is already right. Every card this epic goes on to
draw — the host-alone table, the acting-seat mark, the last-act mark, the clock, the chips —
composes a table frame with a pot figure in it. Landing the correction first means none of them
inherits a number a merged ADR contradicts.

It is also the smallest item in the epic and the only one that ships a behaviour change with no
new surface, which makes it the cheapest place to start a sequential run.

## Design notes

Everything below is merged and is not re-litigated by a ticket.

- **The quantity is fixed** (`ADR-0107` §1): `Pot = view.pot + seats[0].committedThisStreet +
  seats[1].committedThisStreet`. That is identically `Lobby.tsx:154`'s `potIncludingStreet` and
  `ADR-0100` §6's `P`. **The same screen already computes it** and hands it to the action bar; the
  strip prints `view.pot` instead. This story does not add arithmetic to the client, it stops one
  component printing the smaller of two numbers the screen already holds.
- **One line, one figure, one word** (`ADR-0107` §2). The label stays `Pot`. No *Total pot*, no
  second figure, no parenthetical. Blinds, hand number, street name and the award line at
  `COMPLETE` are untouched, and `ADR-0095`'s award line still replaces the figure.
- **Committed, not netted** (`ADR-0107` §3). An uncalled raise is inside the number even though a
  fold would hand it back. That is deliberate; a ticket that "corrects" it is wrong.
- **The bet-lines are not resettled here** (`ADR-0107` §4). The rival's `committed` line at
  `DuelTable.tsx:58` stands, so the rival's street chips are on screen twice. Whether that survives
  once chips are drawn is `STORY-1306`'s card question, not this story's.
- **The never-derives guard narrows by exactly one named quantity, and no further**
  (`ADR-0107` §5). `no-derivation.test.tsx` must go on failing for any *second* derived figure. Its
  fixture-independence sweep — no two fixture numbers may sum to a third — has to carve out this
  one sum and nothing else. **This is a real weakening of a test whose value was having no
  exceptions**, and the ticket that does it says so in its own words.
- **Whether the strip takes a prop or sums the view itself is the ticket's shape**, per
  `ADR-0107` §5 and `ADR-0101` §7's precedent. Either is admissible; neither is a decision.
- **The card correction is the first ticket, and it merges before the client ticket is startable.**
  `EPIC-13` *Design first* and `ADR-0091` §2. This item adds **no new surface and no new state** —
  it is a two-node correction to `design/screens/duel-table.html`, not a new card — so the card
  ticket owes no state enumeration; `duel-table-states.html`'s `Pot 3,250` already agrees and does
  not move. `design/check-drift.sh` must stay green.

**Merged pins that move, named so the split budgets for them** (`ADR-0107` *Consequences*). Each is
a test file whose assertions this story invalidates, so whichever ticket changes the behaviour owns
them in its own `Files` table:

- `PotStrip.test.tsx` — *"takes the pot from the view and not from what the seats put in"* inverts
  in intent. Its successor guards the same *wrong* sum (`view.pot + committedThisHand`, which
  double-counts swept streets) under a truthful name.
- `DuelTable.test.tsx` — `Pot 5,675` beside commitments of 125 and 825 becomes **6,625**.
- `Lobby.test.tsx` and `reconnect.test.tsx` — every `Pot 0` / `Pot 30` pin moves wherever its own
  fixture holds street commitments.
- **Each new expected value is read off its own fixture, never computed from a rule in a comment.**

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| — | *not yet split — run `/plan-story STORY-1301`* | — |

## Acceptance criteria

- [ ] `design/screens/duel-table.html`'s two `Pot 2,450` nodes read `Pot 2,850`, and
      `duel-table-states.html` is unchanged; `design/check-drift.sh` exits 0
- [ ] The duel table prints `Pot 150` at 50/100 on the first decision of a hand, where it printed
      `Pot 0` — asserted by a named test, not by a screenshot
- [ ] `no-derivation.test.tsx` still fails when a *second* derived figure is rendered, and its
      fixture-independence sweep admits exactly one named sum
- [ ] The label is still the single word `Pot` and the award line still replaces the figure at
      `COMPLETE`
- [ ] Every moved pin in `PotStrip.test.tsx`, `DuelTable.test.tsx`, `Lobby.test.tsx` and
      `reconnect.test.tsx` is read off its own fixture, and no assertion is deleted or weakened
      except the one narrowing `ADR-0107` §5 names

## Out of scope

- **`GameState.potTotal` on the wire.** It exists in the engine and is not on the view.
  `ADR-0107` §5 neither asks for it nor forbids it; putting it there is a `PROTOCOL_VERSION` step
  and an architect's question, and this epic **opens no engine** (`EPIC-13` *Out of scope*).
- **The rival's bet-line, and a hero bet-line.** `ADR-0107` §4 leaves both to `STORY-1306`'s card.
- **The sizing row.** `ADR-0101`'s presets and their base are unchanged; `DEC-102` (the stepper's
  step) stays open and is not touched.
- **Any second figure on the strip.** `ADR-0107` §2 forecloses it; a ticket that wants one owes a
  new ADR.
