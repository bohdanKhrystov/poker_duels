---
id: STORY-1413
title: The pot travels to the winner
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, table, design, bug]
depends_on: []
---

## Goal

The chips a hand pays out arrive at the winner's seat **from the middle of the table**. Today they
arrive from above at every seat, which is right for the seat below the pot and backwards for the
seat above it — the rival's, which is the seat the human drew the arrow to. The flight gains a
**direction** and nothing else: the same three discs, the same 420 ms, the same token distance,
still `aria-hidden`, still stating nothing. Every amount goes on being stated by the pot figure and
the stack numerals, which is why the whole story is allowed to be garnish (`ADR-0115` §1).

## Why

**It is `EPIC-14` item 2c, and it is the human's own annotation** — an arrow drawn from the pot's
chip pile to the rival's seat plate, captioned *"chips from center shoud go to winner"* — after
playing the product on a laptop and an iPhone against a local-network stack on 2026-09-06.

**The epic booked it as *"a card and a client story"* and both halves are governed by merged ADRs**,
so it waits on nothing and registers nothing:
[`ADR-0115`](../../docs/adr/ADR-0115-motion-never-carries-a-fact-and-reduced-motion-stills-every-surface.md)
(motion never carries a fact; reduced motion stills every surface, automatically, from one block in
the sheet) and
[`ADR-0102`](../../docs/adr/ADR-0102-a-hand-ends-in-steps-and-the-client-owns-the-clock.md) with
[`ADR-0136`](../../docs/adr/ADR-0136-a-beat-declares-its-own-length-and-zero-silences-every-beat.md)
(the pacing). `ADR-0126` took the version lock out of item 2 altogether, so nothing here is
`atomic:` — **no Kotlin file is opened, nothing crosses the socket.**

## Design notes

Everything below was **measured in this worktree at `cb1cd0cc`** against a prototype that was
reverted, unless it names a merged document. No ticket re-derives any of it.

### What is already true, and what the one defect is

`ChipPile` is **three discs, always** — `aria-hidden="true"`, no text, no `aria-label`, no `title`
— that arrive on mount with the `chip-flight` animation and then stand still. It is mounted in
three places, each keyed on the number beside it: the seat's stack (`SeatPlate.tsx:91`,
`key={props.seat.stack}`), the rival's bet line (`DuelTable.tsx:137`, `key={props.committed}`) and
the pot (`PotStrip.tsx:114`, `key={total}`).

So an award already moves piles, and it already moves **only the winner's**: the winner's `stack`
changes, its key changes, its pile remounts and flies; the loser's stack does not change, so the
loser's pile never re-enters. The pot's pile is not animated away — `awardLine !== null` drops it in
the same tick the banner replaces `Pot N`, and *nothing is ever animated out* is written into
`app.css:118-126` and into all three cards as a consequence of `ADR-0115` §§1, 3, 6.

**The single defect is the direction.** `pd-chip-flight` runs
`translateY(calc(var(--pd-motion-chip-travel) * -1))` → `none`, i.e. every pile in this product falls
in **from above**. The viewer's plate sits *below* the centre block (`DuelTable.tsx`: rival above,
board between, you below), so its chips already arrive from the pot. The rival's plate sits *above*
it, so her chips arrive from off the top of the table — the opposite of the arrow the human drew.
`design/screens/duel-table-states.html:374` draws exactly that frame today,
`<span class="pile flying">` at ImKate's seat, and it is the frame the human was looking at.

### The mechanism: one direction, two token-distance flights, no measurement

The flight stays CSS, stays a token distance, and gains a second spelling:

```css
@keyframes pd-chip-flight-down { from { transform: translateY(calc(var(--pd-motion-chip-travel) * -1)); } to { transform: none; } }
@keyframes pd-chip-flight-up   { from { transform: translateY(var(--pd-motion-chip-travel));            } to { transform: none; } }
```

`ChipPile` gains `from?: "above" | "below"`, mapped through a literal side table to
`chip-flight-down` / `chip-flight-up`, defaulting to `"above"` — the direction every existing call
site already had. `SeatPlate` is the only site that passes anything:
`from={props.isViewer ? "above" : "below"}`, because `DuelTable`'s own KDoc fixes the arrangement —
*"one column, rival above, board between, you below"* — and this table has no other shape.

**No new token is minted.** `--pd-motion-chip-travel`, `--pd-motion-chip-flight` and
`--pd-motion-chip-ease` are the merged three and all three are reused, so this is *composing*
rather than *minting* under `ADR-0091` §3 and the design tickets are ordinary dispatched tickets.
`design/tokens/tokens.css` and `web-client/src/styles/tokens.css` are **not edited by any ticket in
this story**.

### A real journey across the table was measured against and refused — named, not registered

The honest alternative is the arrow drawn literally: the pot's pile travels the *actual* distance to
the winner's plate. It is refused here on merged grounds, not on taste, and the refusal is named so
no ticket re-opens it:

- The distance is not knowable without `getBoundingClientRect` on two elements in two subtrees, and
  `ADR-0115` §4 names **travel distance** as a design value that is born in
  `design/tokens/tokens.css` *"and nowhere else"*.
- A JS-driven animation (`Element.animate`) is **not** stilled by the sheet's one
  `prefers-reduced-motion` block — `animation: none !important` reaches CSS animations only — so it
  would need a `matchMedia` inside a component, which is precisely the per-surface re-decision
  `ADR-0115` §4 refuses (*"honouring the signal is a property of the vocabulary, not a re-decision
  per surface"*).
- It buys a measurement layer for **garnish**: `ADR-0115` §1 already puts the fact in the still form,
  so the journey can never state anything the numerals do not.

`ADR-0105` §6's route: **named, deliberately not registered.** If the pane says the arrival is too
small a gesture, the repair is one value — `--pd-motion-chip-travel` in the token sheet — and it is
the human's (`ADR-0024` §5, and `ADR-0115`'s own *retuning any duration (one token)*). Only a
request for the measured journey itself would owe a `DEC`, and nobody has made one.

### The award beat needs no declared length, and `ADR-0136` is why

Stated here rather than left to a coder, because `ADR-0136` merged the day before this split and a
reader could reasonably ask. **`ADR-0102` §4's existing step is enough, and `RevealStep.hold` gains
no third value:**

1. **The flight already fits.** `--pd-motion-chip-flight` is **420 ms** and the shortest a hand's
   ending ever stands is `REVEAL_STEP_MS` = **600 ms**; a showdown the viewer had not seen stands
   2,000 ms (`ADR-0136` §2). Both merged figures, so the flight completes inside the beat that
   carries it with nothing to lengthen.
2. **A hold added *for* an animation would invert `ADR-0136` §6.** That clause makes reduced motion
   unable to reach a hold *by construction* — *"the hold has no CSS"*. A beat lengthened to make
   room for a flight is time spent on a **how**, and under `prefers-reduced-motion: reduce`, where
   the flight does not run at all, the screen would stand still waiting for a garnish that was
   skipped. That is `ADR-0115` §3's line, crossed.
3. **Nothing is owed reading time.** The award line and the stack numeral state the fact the instant
   the beat begins (`ADR-0115` §1).

So **no store file is opened by this story**: `duel-store.ts`, `duel-state.ts` and `boot.ts` are
untouched, `REVEAL_STEP_MS` and `REVEAL_READ_MS` keep their values, and no new constant is named.

### The reduced-motion path is gated on the built stylesheet, because a class name cannot tell

This epic has been bitten twice by a gate that could not fail — `min-w-0` compiling away
(`TASK-140502`) and the class-name assertion that could not see it (`TASK-140503`) — so the gate
here reads generated CSS, and the mutation that proves it bites was **run**:

| Probe | Reading |
| --- | --- |
| `npm run build`, then `grep -F` over `dist/assets/*.css` for `.chip-flight-up{animation-name:pd-chip-flight-up;…}`, `.chip-flight-down{…}`, `@keyframes pd-chip-flight-up{0%{transform:translateY(var(--pd-motion-chip-travel))}to{transform:none}}` and `@media (prefers-reduced-motion:reduce){*,:before,:after{transition:none!important;animation:none!important}}` | all five present, exact strings |
| **Mutation D** — rename `@utility chip-flight-up` away in `app.css`, rebuild | the built rule is **gone** (the `grep -F` exits non-zero) while `ChipPile.test.tsx` still reports **`5 passed (5)`** — the class-name test cannot see it, which is the whole point |
| **Mutation E** — turn `@media (prefers-reduced-motion: reduce)` into `no-preference` in `web-client/src/styles/tokens.css`, rebuild | the reduce grep **fails**, `src/table` stays **`253 passed (253)`** green at `TASK-141304`'s state, and `src/styles/tokens.test.ts`'s byte-identity check reddens as a side effect (recorded so the coder is not surprised) |

A **third** trap was found by probing and is written into `TASK-141304` so nobody rediscovers it:
building the class name dynamically — `` `chip-flight-${…}` `` — did **not** drop the rule, because
Tailwind's content scan reads `ChipPile.test.tsx` too and the test's own literals keep it alive. A
mutation that spells the class dynamically therefore proves nothing; deleting the utility is the one
that bites.

### The driver's zero cannot be reached from here, and the same gate says so

`ADR-0136` §3 makes `stepMillis === 0` silence every beat, and `drive-duel.tsx:229` boots at zero for
four recorded-frame suites that may not be edited. **Nothing in this story consults that clock**: the
flight is a CSS animation on a mounted element, `ChipPile` gains no `useEffect`, no `setTimeout`, no
`requestAnimationFrame` and no `matchMedia`, and jsdom runs no CSS animation at all. So the gate is
`STORY-1411`'s, minus the mutation there is no gate to delete: **no file under `web-client/src/e2e/`
is edited**, `scripted-duel.gen.json` is byte-unchanged, `drive-duel.tsx` is not opened, and
`npx vitest run src/e2e` stays at **7 files, 55 tests** — measured green with the whole change
applied. A source gate on `ChipPile.tsx` pins the *reason* rather than the symptom.

### The card comes first, it draws the still form, and it is three tickets because it is three files

`ADR-0091` §2, and `ADR-0115` §4's second half: *a card that gives a surface motion names what moves
and **draws the surface at rest as a named state***. The at-rest drawing is not a formality here —
under `ADR-0115` §1 the still form is the thing that carries the fact, so a card that drew only the
flight would be drawing the garnish and not the statement.

The pile's canonical home is `design/components/seat-and-pot.html`, whose *The chip pile* section
already draws *chips at rest*, *chips in flight* and *stilled*. It gains the second direction beside
the first and one relabelled `stilled` row that says it is the still frame **both** flights end on —
which is exactly why two directions are safe: they land on the same frame, and the numeral beside
them has already moved. Both screen cards carry a **faithful copy** of that CSS block (they say so in
their own comments), so all three files move, one ticket each, and `design/screens/duel-table.html`
moves even though it draws no flying pile at all — the copy is of the block, not of the used subset.

### The screen card's gate is frame-relative, not a whole-file count

`TASK-141107` merged the day before this split after showing that whole-file aggregate counts on
`duel-table-states.html` cannot tell *the right frame* from *some frame*. The same lesson applies
here and is sharper, because `TASK-141103` — `STORY-1411`'s split frame, still `backlog` — adds two
more flying piles to the same file. So `TASK-141303` **depends on `TASK-141103`** and gates the
**rule** rather than a total: tracking the last `<div class="opp">` / `<div class="hero">` marker
seen, every `pile flying-up` must sit in an `opp` block and every `pile flying-down` in a `hero`
block, with at least one of each. Measured on the prototype: `up=1 down=2 bad=0`, exit 0; with the
two directions swapped, `up=0 down=0 bad=3`, exit 1.

### The per-file counts are measured, never computed

Baselines at `cb1cd0cc`, `npm ci` fresh: `ChipPile.test.tsx` **3**, `SeatPlate.test.tsx` **19**,
`src/e2e` **7 files, 55 tests**, whole client suite **124 files, 1166 tests**. With the change
applied: `ChipPile.test.tsx` **5**, `SeatPlate.test.tsx` **20**, `src/e2e` unchanged, whole suite
**1168** after `TASK-141304` and **1169** after `TASK-141305` — each of those run, not added up.
Card counts, all line-based in this project's `awk index(...)` idiom, after the change:
`seat-and-pot.html` `--pd-motion` 12 → **15**, `pd-chip-flight` 3 → **5**, `animation:` **4**
unchanged, `class="pile` 6 → **7**; both screen cards `--pd-motion` **15** and `pd-chip-flight`
**5**, `animation:` **3** unchanged.

### Exactly one merged assertion moves, and it was counted

`ChipPile.test.tsx:29` — `carries the flight class beside the pile class` — asserts
`toContain("chip-flight")`, and the rename to `chip-flight-down` reddens it and nothing else:
measured `1 failed | 1165 passed (1166)` across the whole suite, naming that test alone.
`TASK-141304` owns it, in the file it edits anyway. `PotStrip.tsx`, `DuelTable.tsx`,
`PotStrip.test.tsx`, `DuelTable.test.tsx` and `null-view.test.tsx` are **not opened by any ticket
here**: the prop is optional and its default is the direction they already had.

## Tasks

Split on **2026-09-07**. One linear chain — the three cards share a vocabulary and the two client
tickets share a declaration — so exactly one ticket is startable at a time.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-141301`](../tasks/TASK-141301-the-component-card-draws-both-directions-and-the-frame-they-share.md) | S | The pile's canonical card gains the second direction, its own row, and a `stilled` row relabelled as the frame **both** flights end on — the still form `ADR-0115` §1 makes load-bearing |
| [`TASK-141302`](../tasks/TASK-141302-the-duel-table-cards-copy-of-the-flight-follows.md) | XS | `design/screens/duel-table.html`'s faithful copy of the flight CSS follows the component card. It draws no flying pile; the copy is of the block, not of the used subset |
| [`TASK-141303`](../tasks/TASK-141303-the-rival-takes-the-pot-from-below-on-the-states-card.md) | XS | The states card's copy follows, and every winner's pile is aimed at the middle — ImKate's flies **up**, the hero's **down**, gated by a frame-relative rule rather than a total |
| [`TASK-141304`](../tasks/TASK-141304-the-flight-has-a-side-and-the-built-sheet-proves-it.md) | S | Two keyframes, two utilities, `ChipPile`'s `from` prop, and the gates that read `dist/assets/*.css` rather than a class list |
| [`TASK-141305`](../tasks/TASK-141305-the-rivals-seat-takes-its-chips-from-below.md) | XS | `SeatPlate` aims its pile at the middle: `from={props.isViewer ? "above" : "below"}`, proved at both seats and reddened by either hard-coding |

**A sixth ticket was considered and refused.** `docs/test-plan.md` gains nothing: `EPIC-14`'s
per-epic suite is the `qa-cases` skill's to write from the epic's *Definition of done* — *"one case
per promise the epic made… not one per ticket"* — and a case for a motion that carries no fact would
be a case for something no player can be wrong about.

## Acceptance criteria

- [ ] `ChipPile` renders `chip-flight-up` for `from="below"` and `chip-flight-down` for
      `from="above"` and for no prop at all, and each of the three is asserted with the other
      spelling **absent**
- [ ] `SeatPlate` gives the viewer's plate `chip-flight-down` and the rival's `chip-flight-up`, from
      one expression over `isViewer`; hard-coding either direction reddens exactly one test
- [ ] After `npm run build`, `web-client/dist/assets/*.css` holds the `.chip-flight-up`,
      `.chip-flight-down` and `@keyframes pd-chip-flight-up` rules **and** the universal
      `prefers-reduced-motion: reduce` block, each matched as an exact string
- [ ] Renaming `@utility chip-flight-up` away in `app.css` makes that gate fail while
      `ChipPile.test.tsx` stays **`5 passed (5)`**, and the file is restored and `cmp`d afterwards
- [ ] `--pd-motion-chip-travel`, `--pd-motion-chip-flight` and `--pd-motion-chip-ease` are the only
      motion values used; `design/tokens/tokens.css` and `web-client/src/styles/tokens.css` are not
      edited by any ticket, and no `--pd-` name is added anywhere
- [ ] `ChipPile.tsx` contains no `matchMedia`, `setTimeout`, `requestAnimationFrame`, `useEffect` or
      `useState`, and stays `aria-hidden="true"` with empty `textContent`
- [ ] No file under `web-client/src/e2e/` is edited, `scripted-duel.gen.json` is byte-unchanged,
      `drive-duel.tsx` is not opened, and `npx vitest run src/e2e` stays at **7 files, 55 tests**
- [ ] `duel-store.ts`, `duel-state.ts`, `boot.ts`, `PotStrip.tsx` and `DuelTable.tsx` are untouched,
      and `RevealStep.hold` gains no third value
- [ ] `pile flying"` appears in **no** card; in `duel-table-states.html` every `pile flying-up` sits
      inside a `<div class="opp">` block and every `pile flying-down` inside a `<div class="hero">`
      block, with at least one of each, and swapping the two makes that gate exit 1
- [ ] `design/components/seat-and-pot.html` draws both directions **and** a `stilled` row, and its
      label says the still frame is the one both flights end on
- [ ] `cd web-client && npm run check` exits 0, `./design/check-drift.sh` exits 0,
      `./design/check-frame-cards.sh` exits 0, and `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **A measured journey from the pot to the plate.** Named and refused in *Design notes* on
  `ADR-0115` §4 and `ADR-0091` §3 grounds, on `ADR-0105` §6's route: **not registered**, not
  ticketed. The retune knob, if the pane wants a bigger gesture, is one token value and is the
  human's.
- **The pot's own pile, and the bet line's.** The pot receives from both seats, so it has no single
  side, and the rival's bet line already takes its chips from her stack above it. Both keep the
  flight they have. Not ticketed.
- **Animating the pot's pile out.** *Nothing is ever animated out* is written into `app.css` and all
  three cards as a consequence of `ADR-0115` §§1, 3, 6; the pot's pile leaves with the banner that
  replaces `Pot N`, exactly as it does today.
- **A flight that depends on whether the stack rose or fell.** It would make the component remember
  a previous value, which `DuelTable`'s own *"nothing is worked out here"* refuses, and it would put
  a fact in the motion. Not ticketed.
- **A declared length for the award beat.** Answered *no* in *Design notes*: `ADR-0102` §4's step
  stands, `ADR-0136` is not amended, no store file is opened.
- **Any in-product motion setting.** `ADR-0115` §2 forbids it outright: the system's signal is the
  whole interface.
- **Marking, naming or re-drawing any card at a showdown.** `ADR-0126` and `ADR-0095` §3 —
  `STORY-1411` and `STORY-1412`'s ground. No frame's cards move here; only a pile's class does.
- **The engine and the server.** `EPIC-14`'s *Out of scope* forbids opening `poker-engine`, and item
  2 is client and design end to end (`ADR-0126`). No Kotlin file is opened, nothing crosses the
  socket, so nothing here is `atomic:`.
