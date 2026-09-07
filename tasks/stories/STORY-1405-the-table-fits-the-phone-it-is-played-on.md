---
id: STORY-1405
title: The table fits the phone it is played on
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, table, bug, manual-verify]
depends_on: []
---

## Goal

At 390 × 664 the duel table's document fits on **both** axes at every beat —
`scrollHeight ≤ clientHeight` **and** `scrollWidth ≤ clientWidth`
([`ADR-0121`](../../docs/adr/ADR-0121-the-table-reflows-never-scales-and-the-shape-is-measured-on-the-device.md)
§3) — so neither seat plate is cut off at the screen edge, the reading that says so is written down
where a future round can find it, and the one utility that silently generated nothing is spelled so
that it generates.

## Why

**It is `EPIC-14` item 3d, and it is the first photograph of this product on a real phone.** An
iPhone in Safari over the local network, 2026-09-06: the table **scrolled** and **both seat plates
were clipped at the screen edge**. The human's four annotations are *"opponent shoud fit screen"*,
*"info bar should fit screen"*, *"conroller shoud fit screen"* and *"table area shoud be scaled to
fit the screen; scroll is nececerry only if table area reach some reasonable min heigt/width"*.

**`ADR-0121` answers `DEC-136` and went the other way on the mechanism, deliberately.** The table
**keeps reflowing and nothing scales it** — a uniform scale is refused on `R3` and on `ADR-0103`
§3's give order, because the numbers give last and a transform shrinks them first, and `ADR-0106` §1
already rules that where the instrument and the property disagree the property governs. What the
ADR changes is **what the fit is measured against**: the smallest viewport the browser presents
(§2), on **both** axes (§3), at a shape (§4) that stays 390 × 664 until a **device** reading
corrects it downward and that nobody may raise. It adds **no criterion** — the clipped plates are
`R2` and `R3` `not met` under merged text, and three of the human's four annotations were never
blocked by an unanswered question at all.

**`ADR-0121` §5 deliberately did not diagnose the cause.** *"A shorter viewport than 664, a content
box that is wider on the device than headless, a font metric — the decision is robust to which,
because §§2–4 fix what the fit is measured against, and a cause is findable once the measurement is
the player's."* This split went and found it; *Design notes* below is that measurement, and the
repair it sizes is ordinary work under the merged give order, exactly as §5 predicts.

## Design notes

Everything below was **measured in this worktree on `develop` at `7ac3d59f`** against probes that
were reverted (`git clean`); nothing here is arithmetic and no ticket re-litigates it.

### The method, and why it is not a running stack

Headless Chrome 152, `Emulation.setDeviceMetricsOverride` at 390 × 664, `deviceScaleFactor: 0`,
`mobile: false` — the audit's own shape and the one `ADR-0089` §3 permits. Two subjects:

- **The whole shipped client**, reconstructed exactly: `driveScriptedDuel` (`src/e2e/drive-duel.tsx`)
  was run over the committed `scripted-duel.gen.json` and the rendered `container.innerHTML` was
  captured at **all 57 beats**, then each beat was laid into a page carrying `npm run build`'s own
  `dist/assets/*.css`. The markup and the stylesheet are the product's; only the socket was a
  double.
- **Single components** — `SeatPlate`, `PotStrip`, `ActionBar` — rendered the same way inside the
  exact column `Lobby.tsx:333` draws, so a shape the recorded script never reaches could still be
  read off the real component.

`ADR-0089` bars a browser from any `verify:` block, so **every number below is an acceptance
criterion with a named runner, never a gate** — which is why `TASK-140501` exists at all.

### All 57 recorded beats fit, and that is the finding, not a clean bill

At 390 × 664, every one of the 57 beats reads `scrollHeight` **664** / `clientHeight` **664** and
`scrollWidth` **390** / `clientWidth` **390**. The one exception is beat 56, the **result** screen,
at **692 / 664** — and that is the post-win account offer, which
[`ADR-0125`](../../docs/adr/ADR-0125-the-account-screen-names-the-anonymous-profile-and-owns-the-door.md)
§2 **retires whole** in `STORY-1408`. It is named here so nobody files it twice; it is not this
story's, and it needs no ticket of its own.

**The reason the duel beats pass is that the committed script carries no `TurnClock`.** No recorded
frame puts `Timebank` on a plate — grepped over all 57 dumps, zero hits — so no recorded beat has
ever drawn a seat plate at the width `ADR-0108` and `ADR-0113` gave it in `EPIC-13`. This is
`ADR-0121` §2's own sentence about headless passes, arriving one level down: *"A pass taken there
carries no information about the device by construction."*

### The seat plate is the overflow, and it is reproduced

The real `SeatPlate` rendered inside the real column at 390. `overhang` is the last child's
`getBoundingClientRect().right` minus `document.documentElement.clientWidth` — how far past the
screen edge the **stack figure** is painted:

| # | what the plate carries | plate `scrollWidth`/`clientWidth` | overhang | height |
| --- | --- | --- | --- | --- |
| 1 | on turn · `Raise to 10,000` · `24` · `Timebank 3:00` · pile · `10,000` | **434** / 379 | **+50.25** | 83.5 |
| 2 | off turn · `Raise to 10,000` · `D` · `Timebank 3:00` · pile · `10,000` | **437** / 379 | **+53.19** | 67 |
| 3 | on turn · `24` · `D` · `Timebank 3:00` · pile · `10,000` | 379 / 379 | −21.00 | 67 |
| 4 | off turn · `All in 9,350` · `D` · `Timebank 3:00` · pile · `9,350` | 379 / 379 | −14.56 | 67 |
| 5 | on turn · `Call` · `24` · `Timebank 3:00` · pile · `9,350` | 379 / 379 | −21.00 | 67 |
| 6 | on turn · `Raise to 200` · `24` · `Timebank 3:00` · pile · `1,500` | **405** / 379 | **+21.36** | 83.5 |
| 7 | off turn · `Raise to 10,000` · `D` · pile · `10,000` — **no clock** | 379 / 379 | −21.00 | 67 |
| 8 | on turn · `24` · `Timebank 3:00` · pile · `10,000` — **no mark** | 379 / 379 | −21.00 | 67 |

A page holding rows 1, 2 and 6 reads `document.documentElement.scrollWidth` **443** against
`clientWidth` **390**.

Rows 7 and 8 are the point: **the plate as `ADR-0106` measured it fits, and the plate as `ADR-0113`
measured it fits.** The overflow needs the last-act mark (`ADR-0109`) and the timebank (`ADR-0108`,
`ADR-0113`) **together** — two additions that each landed in `EPIC-13` after the fit was last
measured, on an instrument that watched one axis. Row 6 says the magnitudes are not the cause
either: `Raise to 200` against a 1,500 stack overflows too.

**Only the plate.** `PotStrip` and `ActionBar` were rendered at the widest figures the format can
produce — pot 20,200, `Blinds 200/400 · Hand 47`, `Fold` / `Call 200` / `Raise to 800` /
`All in 13,000`, the sizing row and the typed-total input — and the document read **390 / 390**.
The human's *"info bar should fit screen"* and *"conroller shoud fit screen"* have no width
overflow of their own in the shipped client; the most likely reading is that a document 443 px wide
scrolls sideways and takes everything with it, and that is one of the things `TASK-140501`'s
reading settles.

### The cause is one utility that generates no CSS

`SeatPlate.tsx:50` is `<span className="min-w-0 flex-1">` — the block holding the name and the
status, and the only child of the plate's flex row that is meant to give ground. **`min-w-0`
produces no rule in this build.** `src/styles/app.css:55` sets `--spacing: initial;`, so Tailwind's
bare spacing steps are switched off by design; `min-w-0` compiles to `min-width: calc(var(--spacing)
* 0)` and is dropped. Measured on the built stylesheet: the only `min-w` rule in
`dist/assets/*.css` is `.min-w-\[2ch\]{min-width:2ch}` — an **arbitrary** value, which needs no
theme entry. `.flex-1{flex:1}`, `.truncate{…}` and `.last-act{…}` are all present. Named steps
still work (`.min-h-7{min-height:var(--spacing-7)}`); the bare `0` does not.

With no `min-width: 0`, the block keeps flexbox's automatic minimum size, cannot shrink below its
own min-content, and the row overflows the plate instead. Measured, the block sits at a floor of
**64.94 px** in every one of the eight rows above.

**The merged card already specifies the opposite behaviour, in words.**
`design/components/seat-and-pot.html` — the canonical plate, copied faithfully into
`design/screens/duel-table.html` and `design/screens/duel-table-states.html` — declares
`.seat .who { flex: 1; min-width: 0; }` and comments that the mark, the clock and the timebank
*"ride as children of `.seat`'s own flex row … `.who` gives ground by truncating the name first and
the plate never grows a pixel taller."* **So this is drift from a merged card, not missing design.**

### The card is right, and no design ticket is owed

`design/screens/duel-table.html` was measured at 390 × 664 with its `.last-act` and `.chips` text
replaced by the widest strings `DuelFormat.DEFAULT` can produce (`Raise to 10,000`, `10,000`):
`.viewport.phone` reads `scrollWidth` **390** / `clientWidth` **390**, the plate reads **379 / 379**
and the tail sits **11.75 px inside** the edge, because the card's `.who` shrinks to **0** exactly
as its comment promises. Unmodified, the card's phone frames read `scrollHeight` 748 and 730 against
664 — **and that is a `.note` paragraph the card places inside its own `.viewport` box, not the
table**: `.table` itself measures a flat **664** at 390 and its three blocks sum to 648 + 16 of
`--wgap`. The stray note is named in *Out of scope*.

`ADR-0091` §1 makes the card the carrier of design into implementation. Here the card carries it
already, so `ADR-0103` §5's *"the design work precedes the client work"* is satisfied by a card that
merged in `EPIC-12`, and **no ticket in this story opens a file under `design/`.**

### The fix is verified, and its cost is what the card already accepts

`min-w-0` → `min-w-[0px]` on `SeatPlate.tsx:50`, built and re-measured end to end:

| | before | after |
| --- | --- | --- |
| `document.documentElement.scrollWidth` / `clientWidth` | **443** / 390 | **390** / 390 |
| row 1 overhang | +50.25 | **−14.69** |
| row 2 overhang | +53.19 | **−11.75** |
| row 6 overhang | +21.36 | **−21.00** |
| plate heights (rows 1 / 2 / 6) | 83.5 / 67 / 83.5 | 83.5 / 67 / 83.5 |

The **height axis does not move** — the fix buys width and spends none. `npm run --silent check`
with the change applied is **124 files, 1166 tests**, all green: **no merged test observes this**,
which is why `TASK-140502` must bring its own, and why one of them has to read the *built
stylesheet* rather than the class string.

**The spelling is forced by a merged gate, not chosen.** The alternative — teaching the theme a
zero step — cannot be taken: `src/styles/theme.test.ts` admits only `--x-*: initial;`,
`--x: initial;` and `--x: var(--pd-…);` inside `@theme static`, so `--spacing-0: 0px` fails it, and
`--spacing-0: var(--pd-space-0)` would need a token `tokens.css` does not have — **minting**, which
`ADR-0103` §4 reserves for the human. `min-w-[0px]` is the arbitrary-value idiom the same tree
already uses at `min-w-[2ch]`.

### The name gives ground to nothing, and that verdict is the human's

After the fix the name block is drawn at **0 px** in rows 1 and 2 — `Your rival` is not visible at
the widest row — and at **22.58 px** in row 6. The merged card does the same thing at the same
strings (`.who` measured 0), so the client is conforming rather than inventing, and `R3` is about
**amounts**, which are what the fix puts back on screen. If a name shrunk to nothing is the wrong
drawing, that is a **card** change first and a client change second, and `ADR-0024` §3 and
`ADR-0091` §3 both let the human's visual verdict **trail the merge**. No ticket here decides it and
none registers a `DEC` for it — there is no question a merged source leaves open, only a drawing a
human may dislike.

### The height budget is tight, and this story does not spend it

At the preflop decision beat the centre block's `flex-1` holds **17.891 px** of slack (beat 4;
beat 10 holds 35.141). An on-turn plate carrying a last-act mark is **83.5 px** tall against the
67 px the card draws, because the squeezed status line wraps — and it is 83.5 both before and after
the fix. Only one seat can be on turn and only one can hold the last act, so the worst case is one
83.5 plate and one 67, which the slack absorbs with about 1.4 px to spare. **Nothing in this story
changes a height**, and `TASK-140501`'s reading is what would catch it if the device says otherwise.

### `ADR-0103` §3's stop rule is armed and is not pre-empted

`ADR-0121` §5: *"If the give list runs out, `ADR-0103` §3's stop rule fires … it does not take the
next thing it sees."* Nothing here reaches that boundary — the repair spends **no** give at all, it
restores a behaviour the merged card already specifies. If `TASK-140501`'s reading comes back at a
shape smaller than 390 × 664, or if `TASK-140502`'s after-reading still overflows, that ticket
**stops and registers its own `DEC`**. This split pre-registers none, which is `ADR-0121`'s own
instruction.

### The device reading is the human's, and no ticket pretends otherwise

`ADR-0121` §4: the first reading is a **written hand-check** in `ADR-0088`'s form, *"because no CI
job owns an iPhone"* — the phone in the photograph, the browser in the photograph, the beat in the
photograph. No agent can hold the phone, so **no ticket in this story asks one to**. What
`TASK-140501` writes is the receipt block and the agent's own headless reading beside it, with the
device row left for the human to fill; `EPIC-14`'s Definition of done already carries the unchecked
line that closes on it. The number may only move **down** (`ADR-0121` §4), and if it does, three
things move with it — the round's shape, the headless override and
`design/screens/duel-table.html`'s `.viewport.phone` box — none of which is ticketed here, because
none is owed until a reading exists.

## Tasks

Split on **2026-09-07**. One linear chain: the reading establishes the shape, the repair is measured
against it, and the sweep can only gate the tree once the first site is clean.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-140501`](../tasks/TASK-140501-both-axes-are-read-at-the-phone-and-the-reading-is-written-down.md) | XS | The reading: both axes at 390 × 664 on the **running** client, at three named beats, written into `EPIC-14`'s Definition of done as an `ADR-0088` §3 receipt with the device row left for the human |
| [`TASK-140502`](../tasks/TASK-140502-the-seat-plates-name-block-gives-ground-again.md) | S | `min-w-0` → `min-w-[0px]` at `SeatPlate.tsx:50`, plus the two tests that would have caught it: one over the class, one over the **built stylesheet**, because the class alone passes for a utility that generates nothing |
| [`TASK-140503`](../tasks/TASK-140503-the-second-inert-min-width-is-spelled-so-it-generates.md) | XS | The other `min-w-0` — `ActionBar.tsx:149`, equally inert — and the tree-wide gate that closes the class: the string appears nowhere under `web-client/src/` |

**A design ticket was considered and refused**, with the measurement above as the reason: the card
already draws what the client fails to. **A card correction for the stray `.note` was considered and
refused** as this story's — it is a card-authoring artifact with no product consequence, named in
*Out of scope* and deliberately not ticketed.

## Acceptance criteria

- [ ] `EPIC-14`'s Definition of done carries a `### The 390 × 664 reading` block naming the commit
      it was taken at, and at least three beats, each with a `scrollHeight / clientHeight` pair and
      a `scrollWidth / clientWidth` pair
- [ ] At 390 × 664 on the running client, at a beat where a seat plate carries a last-act mark with
      a figure **and** a timebank, the **before** reading is `scrollWidth > clientWidth` — the
      reproduction. A before reading of `scrollWidth ≤ clientWidth` means the defect did not
      reproduce: the ticket **stops**, records it, and changes nothing
- [ ] After `TASK-140502`, the same beat reads `scrollWidth ≤ clientWidth` **and**
      `scrollHeight ≤ clientHeight`, and no seat plate's last child is painted past
      `document.documentElement.clientWidth`
- [ ] `web-client/dist/assets/*.css` contains `.min-w-\[0px\]{min-width:0}` after a build — the
      utility generates, which a class-name assertion alone cannot show
- [ ] The string `min-w-0` appears nowhere under `web-client/src/`
- [ ] Every changed behaviour is proved red by a `verify:` command that restores the old spelling,
      captures the failure, restores the file and checks the restore with `cmp -s`
- [ ] `cd web-client && npm run check` exits 0 and `python3 .github/scripts/lint_tickets.py` exits 0
- [ ] No file under `design/`, `poker-engine/` or `poker-server/` is opened by any ticket here, and
      no file under `web-client/src/e2e/` is edited

## Out of scope

- **A returning profile's front door overflows the height axis — `scrollHeight` 696 against
  `clientHeight` 664 at 390 × 664**, observed on 2026-09-07 while taking `TASK-140501`'s reading and
  recorded here so it does not die in a transcript. It is **not** one of the three beats that ticket
  measures, and the canonical B1 recipe — a first-time device — never reaches the state: the overflow
  needs the name-prompt block, `No name`, `0 Duel coins` and `Set my name` on screen together. It is
  a **height**-axis finding on the front door, where this story's repair is a **width**-axis fix on
  the seat plate, so neither repair ticket addresses it and neither should be assumed to. Worth its
  own ticket against `EPIC-14` item 3d or the front door, measured before it is repaired.

- **A uniform scale of the table.** `ADR-0121` §1 refuses it on `R3` and on `ADR-0103` §3's give
  order, and §1 names the one sentence from the human that would reverse it. No ticket here adds a
  `transform`, a `zoom` or a whole-column font multiplier, and none removes one.
- **Correcting the judged shape.** 390 × 664 stays until a **device** reading under `ADR-0121` §2
  comes back smaller, and only the human can take that reading. `TASK-140501` leaves the row for it;
  the three things that move with it — the round's shape, the headless override and
  `design/screens/duel-table.html`'s `.viewport.phone` — are not ticketed until a number exists.
- **A `visualViewport` listener.** `ADR-0121` §2: a document that fits with the bars expanded fits
  with them retracted, so *"the client owes no response to the chrome moving"* and alternative E is
  foreclosed.
- **The result screen's 692 / 664 at beat 56.** Measured here, owned by `STORY-1408`: it is the
  post-win account offer, which `ADR-0125` §2 retires whole. Not re-filed.
- **The stray `.note` inside `design/screens/duel-table.html`'s `.viewport` boxes**, which is what
  takes three phone frames to 748, 730 and 808 against 664 while `.table` itself measures a flat
  664. A card-authoring artifact, no product consequence, **not ticketed** — recorded here so the
  next reader of those numbers does not file a defect against the table.
- **`design/`.** The card is right (see *Design notes*); nothing here opens it, and the human's
  visual verdict on the truncated name may trail the merge (`ADR-0024` §3, `ADR-0091` §3).
- **`docs/test-plan.md` and the QA catalogue.** `EPIC-14`'s per-epic suite is the `qa-cases` skill's
  to write from the epic's Definition of done (`ADR-0090` §5), not the planner's and not a ticket's.
- **The engine and the server.** `EPIC-14`'s *Out of scope* forbids opening `poker-engine`; no
  Kotlin file is opened here and nothing crosses the socket, so no ticket in this story is
  `atomic:`.
- **`ADR-0103` §3's give order.** It is not spent here — the repair restores a behaviour the merged
  card already specifies. A ticket that reaches the end of the list registers its own `DEC`
  (`ADR-0121` §5); this split pre-registers none.
