---
id: STORY-1305
title: A bet amount can be typed, and an illegal one is refused in the server's own numbers
type: story
status: ready
parent: EPIC-13
module: web-client
labels: [client, design, table, action-bar]
depends_on: [STORY-1304]
---

## Goal

The action bar carries a text field a player types a total into; a legal total is dialled and sent
like any chip's, and an illegal one sends nothing, rewrites nothing, and says why in the server's own
numbers.

## Why

`EPIC-13` item 7, and the human's last table sentence: *"player shoud be able to make bet using raw
text input."* `ActionBar.tsx` renders `<button>` alone — the sizing row has never been able to say
something illegal, so the server's two amount rejections have been unreachable from this UI since the
slider left. The field is the first control that can express an amount the rules refuse, and
[`ADR-0111`](../../docs/adr/ADR-0111-an-illegal-typed-amount-is-refused-in-the-servers-own-numbers.md)
has settled what happens when it does.

It sits after the three table-surface stories because it is the one item that touches only
`ActionBar` and the bar's own card frames, so it neither spends nor is spent by `ADR-0103`'s seat
budget — and it is the last unblocked item before the epic's two large ones, the chips and the clock.

## Design notes

Everything below is `ADR-0111`, merged, and is not re-litigated by a ticket.

- **That the field exists is the human's instruction and is not re-litigated** (§header). This story
  builds it; it does not weigh whether to.
- **A press with an illegal entry sends nothing, locks nothing, rewrites nothing** (§1). No sent-lock
  is taken, the entry **stands exactly as typed** — no reset, no substitution, no nudge — and
  pressing again does the same thing. The refusal is safely repeatable.
- **The refusal says why, in the server's own numbers, and the control stays live** (§2). The
  sentences are `rejection-text.ts`'s already-merged ones — `500 is under the minimum of 800.`,
  `5,000 is over the maximum of 4,000.` — with the bound read off **this turn's** `LegalActions` and
  formatted by `formatChips`. **Whether the module is literally shared is the ticket's.** The
  sentence stands **at latest when the press happens** and may stand earlier; a press is never
  answered by silence, and there is **no silent dead button**.
- **Two kinds of illegal, one rule** (§3). Outside the interval is one case, with the violated bound
  quoted. A non-number is a different kind: `That is not an amount.` — never coerced. **The empty
  field is this kind; a negative is this kind; a plain `0` is a number and takes §2's minimum
  sentence.** The one rule over both: *nothing the player typed is ever rewritten, and nothing the
  server's standing word already refused is ever sent.*
- **What counts as spelling a number is the ticket's, inside two bounds** (§3): the field may *read*
  `formatChips`' grouping, because reading is not rewriting; and **when the reading is in doubt it
  refuses as not an amount**, because a wrong refusal costs a retype and a wrong reading costs chips.
- **The check is a reading and may never grow finer than the statement it reads** (§4). Sendable iff
  the entry spells a whole number in `[floor, allInTo]`, where `floor` is `minRaiseTo` when the
  server allowed `RAISE` and `minBetTo` when it allowed `BET` — the bar's merged `amountFloor`,
  unchanged. Those are literal fields of `YourTurn`, so the bar's law — *"works out no amount the
  server did not send"* — is intact. `rejection-text.ts` is **not retired**: it stays load-bearing
  for `NotYourTurn`, `HandComplete`, a race, a finer rule and a bug.
- **No act conversion, ever** (§5). A typed `callTo` is not a `Call`; an amount at or above the stack
  is not an `AllIn`; a refused raise is not downgraded.
- **`ADR-0100` §5 stands in full** (§6). `actThroughTheBar` gains **no typing branch and sets no
  field's value**; no ticket here touches `web-client/src/e2e/drive-duel.tsx`'s amount path.
  Scripted duels keep pressing the sizing row and reading the action button before it clicks. The
  field's own unit tests type into it the way a player would — that is testing the control, not a
  driver reaching past the UI.
- **The sharp cost, named so a test aims at it** (§Consequences): if the bar's reading is ever
  *stricter* than the server's rule, a legal amount is wrongly refused and **no server message will
  ever say so**, because nothing is sent. Nothing in the type system polices this. A named test
  drives the interval's **two endpoints** — exactly `floor` and exactly `allInTo` — and proves both
  send.

**The card is the first ticket and merges before the implementing ticket is startable** (`EPIC-13`
*Design first*, `ADR-0091` §2, and `ADR-0111` §7 by name). It draws the field's states, named — at
least **outside the interval** and **not an amount**, alongside the ordinary legal entry. One
constraint carries from `ADR-0100` §2: while the entry is illegal **the action button may print the
player's proposal or print no amount, but never a different amount** — a corrected total on the
button is the clamp coming back through the paint. On every press-reached state the button's printed
total stays exact, so the driver's read-before-click contract is untouched. The card lands the field
inside `ADR-0103`'s fit; taste is the human's (`ADR-0024` §3).

## Tasks

| ID | Title | Status |
| --- | --- | --- |
| [TASK-130501](../tasks/TASK-130501-the-action-bar-card-draws-the-typed-total-and-its-three-refusals.md) | The action-bar card draws the typed total, and the three refusals it can express | ready |
| [TASK-130502](../tasks/TASK-130502-the-two-table-cards-carry-the-typed-total-in-place.md) | The two table cards carry the typed total in place, at the phone as well as the laptop | backlog |
| [TASK-130503](../tasks/TASK-130503-what-the-bar-makes-of-what-the-player-typed.md) | What the bar makes of what the player typed, in the server's own numbers | backlog |
| [TASK-130504](../tasks/TASK-130504-the-bar-carries-a-typed-total-and-a-refused-one-sends-nothing.md) | The bar carries a typed total, and a refused one sends nothing and says why | backlog |
| [TASK-130505](../tasks/TASK-130505-both-ends-of-the-interval-the-repeated-refusal-and-the-button-that-never-lies.md) | Both ends of the interval, the repeated refusal, and the button that never prints a different amount | backlog |
| [TASK-130506](../tasks/TASK-130506-there-is-no-typed-total-before-the-server-has-named-a-turn.md) | There is no typed total before the server has named a turn | backlog |

**What the split settled, measured on `develop` `be02155b` rather than reasoned about.**

- **Exactly one merged assertion is in the blast radius**, and `TASK-130504` owns it:
  `ActionBar.test.tsx:141`'s `expect(container.querySelector("input")).toBeNull()`, `ADR-0100`'s
  *no slider* guard. The field was planted and the whole client suite run — 1 failure, 1033 passes.
  Nothing in `Lobby.test.tsx` (80), `whole-duel.test.tsx` (8), `duel-secrecy.test.tsx`,
  `no-derivation.test.tsx` (7) or `null-view.test.tsx` (6) moves.
- **The field is `type="text"` with no `min` and no `max`.** Planting `max={allInTo}` reddens
  `bar-no-derivation.test.tsx` — measured, `expected [ 175, 175, 13400, 175 ] to not include 13400`
  — so that file's count of 3 is a live gate against a bound-carrying control. `min={floor}` does
  **not** redden it, which is why the field's own test asserts `min`'s absence directly.
- **The refusal stands on the bar's existing notice line, and the local sentence wins while it
  stands.** No fourth row is added, so `ADR-0103` §1's `390 × 664` fit is untouched by construction.
  Moving `<Notice>` inside `Live` was probed on this branch: **1034 of 1034 tests stayed green**.
- **The typed field does not exist when `view === null`** — three independent reasons, written into
  `TASK-130504` and asserted in `TASK-130506` rather than left for a coder to discover against the
  contract: `Lobby.tsx` mounts `WaitingTable` and no bar at all; the field lives in `Live`, which
  needs a turn; and inside `Live` it needs `amountFloor(actions) !== null`. A `Snapshot` alone is
  **not** enough, which is the boundary `TASK-130506`'s middle assertion pins.
- **The card keeps the stepper's ± buttons.** `DEC-102` is open and stays the product owner's, so
  the field takes the readout's slot and nothing is deleted. The client ships the field **alone**,
  so the row it must fit is narrower than the row the card draws — the fit judgement is
  conservative in the right direction.

## Acceptance criteria

- [ ] A card under `design/` draws the typed field with at least the *outside the interval* and
      *not an amount* states named and drawn, plus the ordinary legal entry, with
      `design/check-drift.sh` exiting 0 — merged before any implementing ticket is startable
- [ ] Typing a legal total and pressing sends that exact total — asserted at **both** interval
      endpoints, `floor` and `allInTo`, and at one interior value
- [ ] Pressing with an under-floor entry sends **no frame**, takes no sent-lock, leaves the entry
      byte-identical, and renders `rejection-text.ts`'s minimum sentence quoting this turn's bound
- [ ] Pressing with an over-`allInTo` entry does the same with the maximum sentence
- [ ] An empty field, a negative and a non-numeric entry each render `That is not an amount.` and
      send nothing; a plain `0` renders the **minimum** sentence instead
- [ ] Pressing twice with the same illegal entry produces the same result both times and sends
      nothing either time
- [ ] While the entry is illegal the action button prints the player's proposal or no amount, and
      **never** a different amount
- [ ] `web-client/src/e2e/drive-duel.tsx` is unchanged, and a grep proves the driver has no typing
      branch and sets no field value
- [ ] `bar-no-derivation.test.tsx` stays green

## Out of scope

- **`DEC-102`** — the stepper's step. `ADR-0111` §Consequences records that the field moves its
  ground (every legal total is now typeable) and says the question stays open and stays the product
  owner's. **No stepper is built here.**
- **Clamping, keystroke masking, auto-correction on blur, and every act-conversion courtesy.**
  `ADR-0111` §§1, 3, 5 and §Consequences foreclose them; reaching them again needs a superseding ADR.
- **A settable value for the e2e driver** — `ADR-0100` §5 and `EPIC-13` *Out of scope*, by name.
- **Removing or narrowing `rejection-text.ts`** (§4).
- **The engine.** Nothing here opens `poker-engine`, and `ActionValidation.kt` is not touched.
