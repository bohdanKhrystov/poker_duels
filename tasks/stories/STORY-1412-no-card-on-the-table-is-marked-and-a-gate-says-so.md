---
id: STORY-1412
title: No card on the table is marked, and a gate says so
type: story
status: done
parent: EPIC-14
module: web-client
labels: [client, design, table, gate]
depends_on: []
---

## Goal

The property `ADR-0126` §1 states — **the drawing of a card does not depend on whether it played** —
becomes something the repository can fail on, and the two frames of the showdown card that do not
yet say it say it in their margins.

## Why

**It is `EPIC-14` item 2b, and the answer is no.** The human asked for the winning five to be
marked; [`ADR-0126`](../../docs/adr/ADR-0126-the-table-shows-the-cards-and-marks-none-of-them.md)
declines it for v0.1 and dates the reversal at v0.4, applying `ADR-0095` §3's refusal — *a hand's
name is not stated* — to a statement made without words. So `poker-server` leaves this item, no wire
field carries the five, `PROTOCOL_VERSION` does not move, `BestHand` stays inside `poker-engine`,
and there is **no `atomic:` ticket here**.

**What is left is exactly what `ADR-0126` §4 says is left**, and the reason it is worth keeping is
in the same sentence: *"nothing in the repository today would notice a mark appearing."*
`no-derivation.test.tsx` proves the client states no hand **name**; `PlayingCard.test.tsx` proves
one card draws its own rank and suit. Neither would fail if a ring, a `data-*` or a dimmed class
appeared on the five cards that made the hand — which is the exact defect this story's gate is
written against, and the exact reason a *decision to build nothing* still owes a test.

`STORY-1411` is **done**, so the showdown card exists and its frames are there to annotate.
`STORY-1413` no longer waits on this story — what it was waiting for was the version lock, and
`ADR-0126` §2 released it.

## Design notes

Measured in this worktree on `develop` at **`c6a41e6d`, 2026-09-08**. Every number below was read,
not computed.

### Where the gate lives, and why it is not a shell gate under `design/`

Two homes were considered and one is closed by a merged ADR.

`design/check-frame-cards.sh` reads **one** card, `design/screens/duel-table-states.html`, and runs
in CI — `.github/workflows/tickets.yml` invokes it (`TASK-140719` wired it; `ADR-0142` §1's remark
that no workflow does is true of the tree it was written against and false of this one). It is a
gate over *the card*, and this story's property is about *the client's DOM*, which no shell gate
under `design/` may reach:
[`ADR-0142`](../../docs/adr/ADR-0142-a-text-module-is-checked-against-the-rendered-card.md) §1
restates `ADR-0091` §4 as a constraint rather than a preference — *"`check-drift.sh` does not reach
into `web-client/`"*, and *"a consumer's conformance is checked in the consumer's contract tests"*.

So the gate is **a vitest file in the client's own suite**,
`web-client/src/table/cards-are-drawn-alike.test.tsx`, beside `no-derivation.test.tsx`, which is the
same placement and the same reasoning `ADR-0142` §1 chose for its own gate: `build.yml`'s `client`
job already runs `npm run check` → `vitest run` on every pull request, so `vitest run` globs the
file into existence as a gate and **no workflow file changes**.

`ADR-0142` is otherwise not this story's business: it governs a *text module against its card*, and
this gate compares *one rendered card element against another*. Neither `PAIRS` nor `NO_CARD` gains
a row, and the file it describes does not exist at `c6a41e6d`.

### The property, and the matcher

`ADR-0126` §4 states the property and deliberately leaves the matcher to the ticket, *"because §1's
subject is pictures"*:

> any two face-up cards rendered at a completed showdown differ only in rank, suit and suit colour —
> no other attribute, class or label differs between them.

The matcher is a **drawing signature**: an element's tag name, its attributes, its class tokens and
the recursive shape of its children, with exactly three things normalised away — the rank glyph, the
suit glyph and the suit-colour class token. Two cards whose signatures are equal differ only in what
they *are*. A mark from `ADR-0126` §1's list — *ring, border, glow, tint, shadow, lift, scale,
opacity, reordering, badge, connector, a keyable class, a `data-*`, a `title`* — is expressible in
the DOM only as an extra attribute, an extra class token or an extra child, and the signature sees
all three.

The `aria-label` is the one hole the signature cannot close, because two cards legitimately carry
different labels. `ADR-0126` §1 closes it by naming its own bound — *"anything in an `aria-label`
beyond the rank and suit `card-text.ts` already produces"* — so the gate carries a **second**
assertion: each face-up card's `aria-label` equals, character for character, `cardText(card)!.label`
for the card that seat or place was dealt.

### The gate must be red against the defect it names, permanently and not once

The deliverable is the gate, so a green run over a table that draws no cards, or a comparator that
returns a constant, is worth nothing. Three defences, all inside the one file:

1. **A count before the comparison.** The showdown fixture deals five board cards and two to each
   seat, and the gate asserts it found **nine** face-up cards before comparing any of them. A
   component that rendered none passes a *no two differ* assertion vacuously; it fails this.
2. **Four mutation controls, one per DOM-expressible form of a mark.** The gate clones a real
   rendered card, asserts the **unmutated** clone's signature equals the original's — otherwise a
   difference could come from cloning rather than from the mark — and then asserts the signature
   changes when the clone gains an extra class token, a `data-*` attribute, a `title` attribute and
   an inline `style`. A universal claim is a promise to enumerate; this is the enumeration.
3. **Two fixtures that disagree.** The whole property is checked twice, once with `viewerSeat: 0`
   and once with `viewerSeat: 1`. One seat cannot tell a general rule from a constant, and this
   epic has already had a hard-coded seat survive eight of nine tests.

### What is already true, and what the client actually draws

Measured: `PlayingCard.tsx` has **one** `SHELL` constant and `CardFace` differs between cards only
in `text.rank`, `text.suit`, `text.label` and the `text-suit-red` / `text-suit-black` class. `Hand`
draws a place with a card as `CardFace` and a place without one as `CardBack`; `BoardCards` draws
five places the same way. So **the gate is expected to be green on the day it lands**, and that is
the point — `ADR-0126` §Consequences names it: *"a gate must be written to hold a nothing."*

The gate reads **face-up** cards only. A face-down card is what a card *is* under `ADR-0120` §1's
table, not what it did, and the card's own `class="back mucked"` treatment is `ADR-0008`'s statement
about a hand that never reached showdown — gated already, frame by frame, by
`design/check-frame-cards.sh`. `ADR-0126` §1's *"dimming the cards that did not play is the same
statement"* is about the cards that **did** reach a showdown face up, and those are the nine this
gate reads.

### The card, and what is already on it

`design/screens/duel-table-states.html` at `c6a41e6d`: **5** `class="frame"` blocks, **38**
occurrences of `class="pc`, **4** of `back mucked`, and **2** mentions of `ADR-0126`. Both existing
mentions are in frame margins — *Showdown — ImKate shows* and *Showdown — a split pot*. The two
frames that draw face-up cards and do **not** say it are *Showdown — you win, the loser mucks* and
*Fold — you win on the river, nobody shows*, and `ADR-0126` §1 names both moments by name (*"at a
showdown, at a fold"*). `TASK-141201` is that one line, twice, and it is checked frame by frame:
`awk` scoped to each frame's own span currently returns **1** for both and **0** for the two frames
that already carry it, which is the gate red exactly where the defect is and green exactly where it
is not.

`check-drift.sh` and `check-frame-cards.sh` both exit `0` today and must still exit `0`: the added
prose must carry no bare suit glyph and no `--pd-` name.

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-141201](../tasks/TASK-141201-two-more-frames-say-no-card-is-picked-out.md) | Two more frames say in their margin that no card is picked out | backlog |
| [TASK-141202](../tasks/TASK-141202-a-gate-says-the-drawing-of-a-card-does-not-depend-on-whether-it-played.md) | A gate says the drawing of a card does not depend on whether it played | backlog |

## Acceptance criteria

- [ ] All four frames of `duel-table-states.html` that draw a face-up card say in their own margin
      that no card is picked out, checked frame by frame rather than file-wide.
- [ ] `web-client/src/table/cards-are-drawn-alike.test.tsx` exists, runs under `npm run check` with
      no workflow file changed, and fails when a card is given an extra class token, a `data-*`, a
      `title` or an inline `style`.
- [ ] The gate asserts it found nine face-up cards before it compares any of them, and runs the
      whole property from both viewer seats.
- [ ] No face-up card's `aria-label` says anything beyond what `card-text.ts` produces for its own
      card.
- [ ] `poker-engine`, `poker-server`, `protocol.gen.ts`, `PROTOCOL_VERSION` and
      `web-client/src/table/no-derivation.test.tsx` are untouched.

## Out of scope

- **Repairing `BestHand.cards`' KDoc.** `ADR-0126` §3 keeps the field, calls the sentence stale
  rather than dangerous, and says the one-line repair is *"not `EPIC-14`'s"* — that epic's *Out of
  scope* is *"Nothing here opens `poker-engine`"*. Not yet ticketed.
- **Naming the hand.** `ADR-0095` §3 stands unamended; `ADR-0126` §5 applies it and reopens nothing.
- **Marking or naming the five afterwards**, in the replay viewer or `EPIC-08`'s analysis board over
  stored hands. Neither licensed nor foreclosed (`ADR-0126` §5).
- **Extending the gate to face-down cards.** The mucked and folded treatments are `ADR-0008`'s and
  `ADR-0120` §1's, and `design/check-frame-cards.sh` already gates them per frame.
- **`ADR-0142`'s `card-text.test.ts` register.** A different gate about a different pair; its
  implementing ticket does not exist at `c6a41e6d` and this story adds no text module.
