---
schema: 2
id: TASK-121401
title: The catalogue sees a present player marked away
type: task
status: ready
parent: STORY-1214
estimate: S
tier: haiku
review: light
files_touched: 1
labels: [qa, harness, process, catalogue]
depends_on: []
verify:
  - awk -F'|' '/^\| `CORE-06` \|/ { if ($4 !~ /seated and present/) bad=1 } END { exit bad }' docs/test-plan.md
  - awk '/A navigation is not a disconnect/ { bad=1 } END { exit bad }' docs/test-plan.md
  - awk -F'|' '/^\| `CORE-21` \|/ { n++; if ($4 !~ /neither screen ever marks/) bad=1 } END { exit (bad || n != 1) }' docs/test-plan.md
  - awk -F'|' '/^\| `CORE-22` \|/ { n++; if ($4 !~ /still playable/) bad=1 } END { exit (bad || n != 1) }' docs/test-plan.md
  - awk -F'|' '/^\| `CORE-23` \|/ { n++; if ($4 !~ /refused/) bad=1 } END { exit (bad || n != 1) }' docs/test-plan.md
  - python3 .github/scripts/lint_tickets.py
---

## Goal

`docs/test-plan.md` contains a case that is **red on today's product**, so that the next round
cannot pass a duel in which both seats are told the other one left.

## The defect in the catalogue

Three rounds ended `PASS` on a product that marks both connected players absent on the first hand
of a fresh room ([`STORY-1214`](../stories/STORY-1214-a-duel-played-by-hand-deadlocked-on-presence.md)).
That is a property of the catalogue, not luck:

- **`CORE-06`** expects *"the two screens never disagree about the board, the pot or either
  stack"*. The two screens agreed on all three and disagreed about **presence**. The sentence
  sounds universal and is backed by a closed list of three; the case passes on the broken product.
- **`CORE-18`** asserts the away marking **appears** when A closes. Nothing anywhere asserts it
  **stays away when nobody has left**. A product that marks everyone away always, immediately and
  unconditionally passes `CORE-18`.
- **The Reconnect preamble** says *"A navigation is not a disconnect on this browser; only `close`
  ends a session."* **Measured false on 2026-09-01**: `drive.mjs open` on A produced
  `OpponentPresence AWAY` on B within milliseconds, then `PRESENT` 25 ms later when A resumed. The
  whole section rests on that sentence, and so did the reasoning that dropped `TASK-120502`.

## Files

| File | Action |
| --- | --- |
| `docs/test-plan.md` | modify |

Read nothing else. Everything this ticket needs is quoted here.

## Scope

Four amendments, all inside `docs/test-plan.md`.

**1. `CORE-06`'s enumeration gains presence.** The row becomes exactly:

```
| `CORE-06` | read both screens each turn | the two screens never disagree about the board, the pot, either stack, or who is seated and present | any of the four differ at the same moment |
```

**2. The Reconnect preamble states what was measured.** Replace the single sentence
*"A navigation is not a disconnect on this browser; only `close` ends a session."* with a
paragraph saying that a navigation **is** a disconnect — `open` closes the socket and the server
starts the other seat's grace window within milliseconds, measured 2026-09-01 — and that it
differs from `close` only in that the client resumes immediately and the window clears again. Keep
it to two or three sentences. `CORE-17`, `CORE-18` and `CORE-19` are **not** edited: each still
does what it says and still expects what it expects.

**3. A new `### Presence` section**, placed between `### Reconnect` and `### Lobby`, with a short
lead-in saying that `CORE-18` and `CORE-19` check the mark appears and clears, that nothing checked
it stays away when nobody has left, and that a case which only asserts a thing appears is passed by
a product that shows it always. Then a four-column table with the same header as every other CORE
table, carrying exactly these three rows:

```
| `CORE-21` | both seated, nobody closes and nobody navigates; `absent "is away"` and `absent "Timed out"` on **both** screens for 75s | neither screen ever marks the other away or timed out | either screen marks a rival who never left |
| `CORE-22` | neither seat acts for 75s — longer than `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS` — then the seat to act acts | the action is accepted and the hand advances; the duel is still playable after an idle grace window | the action is refused, or a seat was folded while its player stayed connected |
| `CORE-23` | while a screen carries `The duel is paused.`, that seat clicks its own action | the action is **refused** — `ADR-0028` §6's `DUEL_PAUSED` — because a duel the table calls paused is paused on the server | the action is accepted, proving the screen said paused while the server was not |
```

**75 s, not 60**, in both `CORE-21` and `CORE-22`: `RoomTimeouts.DEFAULT_DISCONNECT_GRACE_MILLIS`
is `60_000`, so a window of exactly the grace period proves nothing about what happens when it
expires. The margin is the point of the case.

**4. Ids are 21, 22 and 23** — the next free ones. `CORE-20` keeps its number and its place under
`### Lobby`; *"stable, referenced by bug tickets forever — never renumbered"* is the catalogue's
own rule, so nothing is resequenced to make the new section contiguous with Reconnect.

## The fourth amendment, and why it is not the one that was asked for

The obvious fourth case is *a paused notice and enabled action controls never appear together* —
both were on screen at once, and that contradiction is what made a human stop playing. **Do not
write that case.** [`ADR-0046`](../../docs/adr/ADR-0046-the-table-says-away-timed-out-and-back.md)
§6 declines the question **by name** — *"Whether the action bar's controls look disabled
while the duel is paused"* — and records that `ADR-0028` §6 keeps `YourTurn` standing with
`DUEL_PAUSED` as the refusal, and that `STORY-0313`'s criteria *"already assume a live bar and an
explained refusal"*. A live bar therefore contradicts **no merged source**, and a case with no
merged source is a `DEC` for the product owner rather than a case. It is registered as `DEC-108`;
the case that checks its answer is written when the answer merges.

`CORE-23` asks the answerable half instead, and it has a merged source: **the client may never
assert a game fact** (`CLAUDE.md`). Under *The duel is paused.* the acting player's `Call 100` was
accepted and the stack moved 9,950 → 9,900 — the screen said paused while the server was not. That
is checkable today, it is red today, and it pre-empts nothing.

## Out of scope

- **Repairing the product.** No file outside `docs/test-plan.md` is touched. The repair is
  `TASK-121403` and it is blocked on `DEC-107`.
- **Running any case, or any round.** A catalogue entry is a plan, not a run. `CORE-21`–`CORE-23`
  are expected to be **red on today's product**, and that is the reason they are being written.
- **Renumbering, moving or rewording `CORE-01`–`CORE-20`**, apart from `CORE-06`'s single row.
- **The UAT sections, the screen inventory and the standing questions.** Untouched.
- **Adding a `source` column to the CORE tables.** They have four columns; the new rows have four.
- **Any case about the action bar's enabled state.** See §The fourth amendment above.

## Tests

There is no test class. The deliverable is the catalogue text, and the `verify:` block reads it —
the `awk -F'|'` idiom `TASK-120503` established for exactly this.

| Gate | Proves | Today |
| --- | --- | --- |
| `CORE-06` row names *seated and present* | amendment 1 landed in the `expect` column, not in prose beside it | **red** |
| no line matches *A navigation is not a disconnect* | amendment 2 removed the false sentence | **red** |
| `CORE-21` appears **exactly once** and its `expect` says *neither screen ever marks* | the negative case exists as a row | **red** |
| `CORE-22` appears **exactly once** and its `expect` says *still playable* | the idle case exists as a row | **red** |
| `CORE-23` appears **exactly once** and its `expect` says *refused* | the paused-is-paused case exists as a row | **red** |
| `lint_tickets.py` | the backlog is still well formed | green |

Each of the five was run against a stubbed amendment and **exited 0**, and run against `develop`
and **exited 1**. They fail for the absence of the work and pass for its presence; neither
direction was assumed.

The count check (`n != 1`) is deliberate: a bare `grep` for a case id is satisfied by mentioning it
in a sentence, and a second copy of a row is how a catalogue quietly grows two answers to one
question.

## Acceptance criteria

- [ ] `CORE-06`'s `expect` column reads *…the board, the pot, either stack, or who is seated and
      present*, and its `fails if` column says *any of the four*
- [ ] The string *A navigation is not a disconnect* appears nowhere in `docs/test-plan.md`
- [ ] `CORE-21` is one row, in a `### Presence` section between `### Reconnect` and `### Lobby`,
      and its `expect` column says neither screen ever marks the other away or timed out
- [ ] `CORE-22` is one row and its `expect` column says the duel is still playable
- [ ] `CORE-23` is one row and its `expect` column says the action is refused
- [ ] `CORE-01`–`CORE-20` keep their ids, and no row other than `CORE-06` is reworded
- [ ] Every command in `verify:` exits 0

## Definition of done

Standard, per [`tasks/README.md`](../README.md) — do not restate it in the ticket: `verify` green,
review passed, CI green, status `done`, `BOARD.md` updated, squash-merged into `develop`.
