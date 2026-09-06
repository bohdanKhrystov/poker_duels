---
id: STORY-1414
title: Call says what it costs
type: story
status: ready
parent: EPIC-14
module: web-client
labels: [client, table, design, bug]
depends_on: []
---

## Goal

The `Call` button prints the chips the press takes from the stack —
`legalActions.callTo − committedThisStreet`, `ADR-0101` §1's `toCall` — while `Bet`, `Raise to`
and `All in` keep their totals; the last-act mark goes **bare** on a call; and the two cards that
draw a call mark with a figure stop drawing one. At the human's own frame — 100 in, the rival
raises to 300 — the button reads `Call 200`.

## Why

**It is `EPIC-14` item 4, and the one item that is a decision rather than a defect.** Nothing
miscalculates on this screen because nothing calculates: `actionText` returns
`{ verb: "Call", amount: actions.callTo }` under a KDoc that refuses the netting by name — *"Nothing
is priced, netted or worked out."* The human read the shipped convention as a bug on the first
evening they played the product on two devices, and
[`ADR-0122`](../../docs/adr/ADR-0122-call-names-the-price-and-raise-to-names-the-total.md) answered
`DEC-137` in their favour: the button names the price. That ADR is merged, so this story waits on
nothing and registers nothing.

**Its whole difficulty is the test, and that was measured before this story was written.**
`ActionBar.test.tsx` writes `committedThisStreet={0}` in **seventeen** places — the count `ADR-0122`
measured — plus `committedThisStreet: 0` in three more and a `?? 0` default in its own `bar(...)`
helper; and all **three** of its merged assertions that name a `Call` label — lines 57, 479 and
658 — sit at that zero, where `callTo − 0` and `callTo` are the same number. Its one non-zero
fixture (200 against `callTo` 600, at line 599) presses the sizing row and never asserts the `Call`
label at all. **So the change lands green against today's suite either way**, and a ticket whose
fixture commits zero proves nothing: zero is the value at which the bug and the fix agree. Every
ticket below states what its fixture's two commitments are, and every ticket that changes behaviour
proves its tests were red **by mutation** rather than by assertion — the shape `TASK-140101`
established.

## Design notes

Everything below is measured on `develop` at `e7061e7b` or is merged, and no ticket re-litigates it.

- **The subtraction already ships, and its home is the shape question.** `ActionBar.tsx:255`
  computes `actions.callTo - committedThisStreet` for the sizing base under `ADR-0101` §1, so
  nothing moves the wire, no `PROTOCOL_VERSION` steps, and `ADR-0122` registers no `DEC`.
  `ADR-0122` §5 leaves one thing to the implementing ticket: *"whether `actionText` gains an
  argument or is handed the price by its caller"*. **This story chooses the argument**, because one
  function then owns what every button says, the choice is provable by a pure unit test, and the
  alternative would leave the price decided in the component and the figure decided in
  `action-text.ts` — two places for one sentence. The parameter is **required, never defaulted**: a
  `?? 0` default is exactly the mechanism that hid this from the suite in the first place.
- **The type checker fixes the smallest first ticket, and it is three files.** A required fourth
  parameter drags `ActionBar.tsx:163` and every one of `action-text.test.ts`'s ten `actionText`
  call sites into the same diff — `npm run check` refuses the intermediate state. That set is
  irreducible and is exactly three files, so `TASK-141402` is a normal ticket and **nothing here is
  `atomic:`**.
- **The wiring needs its own assertion, and that is why there is a fourth ticket.** With the price
  computed inside `actionText`, a caller that passed `0` would satisfy every test in
  `action-text.test.ts`. `TASK-141403` is the test-only ticket that closes that hole: it renders the
  real bar at `callTo` 600 / committed 200 and at `callTo` 925 / committed 75 — **two frames whose
  commitments differ from each other and from zero**, so neither a constant nor a dropped prop nor a
  hard-coded seat can pass — and its `verify:` block mutates `ActionBar.tsx`'s argument back to `0`
  and requires both files to go red.
- **The never-derives guard is admitted into, not weakened.** `ADR-0122` §5 lets
  `bar-no-derivation.test.tsx` admit **one** further named quantity: the acting seat's call price.
  Measured: all three of that file's merged tests render at `committedThisStreet={0}`, where the
  price *is* `callTo`, so the guard as it stands never sees the new quantity and would pass either
  implementation. `TASK-141403` adds the frame where it can see it, and asserts the exact set — the
  price is on screen and **`callTo` is not**.
- **The mark reddens two tests in two files, and that was counted rather than guessed.**
  `lastActText` is `action-text.ts:69` and its only consumer is `SeatPlate.tsx:43`. The merged
  assertions that name a call's figure as a mark are `action-text.test.ts:90`
  (*says Call with the call's own total*, two assertions) and `SeatPlate.test.tsx:180`
  (*prints the act's own total on a call, a bet, a raise and an all-in*, one row of four).
  `DuelTable.test.tsx:289`'s `heroCall` fixture asserts **placement** and `not.toMatch(/950/)`, never
  the mark's text, so it is green before and after — it is not in any ticket's budget. Two tests,
  not twenty.
- **`ADR-0109` §2 is amended in exactly one clause** — the figure list loses `Call` — and §§1, 3, 4
  and 6 stand byte-unchanged. The mark cannot print the price: `PlayerCalled` carries `sequence`,
  `seat` and `to`, and after the act the caller's `committedThisStreet` **is** `to`, so the term the
  subtraction needs is gone by the time the mark exists (`ADR-0122` §4).
- **Four card nodes are in arrears, and the ADR named one of them.** `ADR-0122` §7 measured
  `design/screens/duel-table-states.html:298` (`Call&nbsp;800`). Measured here in addition:
  `design/components/seat-and-pot.html:252` draws the same mark as `Call&nbsp;1,700` with the
  caption *last act — call, the total the server sent* at `:256`, and **three** cards carry the
  identical `.last-act` CSS comment saying the mark is *"bare for two of the six acts and with the
  event's own total for the other four"* — `screens/duel-table-states.html:81`,
  `components/seat-and-pot.html:65` and `screens/duel-table.html:105`. It is three and three now.
  This is a drawing corrected to a merged decision, not a decision, so it needs no `DEC`.
- **No card node that draws a `Call` *button* moves, and that is measured too.**
  `screens/duel-table.html` draws `Call 400` in three frames and `components/action-bar.html` in
  five, all beside `Raise to 1,200`/`3,250` in `ADR-0101` §4's worked frame, where the hero has
  committed nothing — so 400 is the price as well. `TASK-141401` gates those counts as **unchanged**,
  so a coder correcting the mark cannot drift the button.
- **No merged test outside the two named files moves, measured end to end.** `develop` runs
  **124 files, 1151 tests**, green. With all four client-side changes applied it runs **124 files,
  1153 tests**, green — the two added by `TASK-141403`. The recorded-frame suites are unaffected by
  construction as well as by measurement: `ADR-0100` §3's driver finds a control by the text it
  *starts with* (`scripts/qa/drive.mjs:77-84`, `drive-duel.tsx`), precisely so that a changing figure
  cannot break it, so **no drive is re-recorded**.
- **The card comes first.** `ADR-0091` §2 and `ADR-0107` §6's shape: the card is corrected before or
  with the client. It is its own ticket rather than a step in the mark's because the mark's own
  irreducible set is already three files.

## Tasks

Split on **2026-09-07**. One linear chain — every ticket touches a file the next one reads, and
exactly one is startable at a time.

| Ticket | Est | What it is |
| --- | --- | --- |
| [`TASK-141401`](../tasks/TASK-141401-the-cards-draw-the-call-mark-bare.md) | XS | The two card nodes that draw a call mark with a figure lose it, one caption follows, and the three copies of the `.last-act` comment say three and three. Gates pin the eight `Call` **button** nodes as unchanged |
| [`TASK-141402`](../tasks/TASK-141402-the-call-button-prints-the-price-the-press-takes.md) | S | `actionText` gains a required `committedThisStreet` and returns `callTo − committedThisStreet` for `CALL`; `ActionBar.tsx` hands it the prop it already holds; `action-text.test.ts` moves to fixtures where the commitment is **not** zero, including for the three verbs whose figure must **not** move |
| [`TASK-141403`](../tasks/TASK-141403-the-bar-is-pinned-where-the-two-commitments-differ.md) | S | Test-only. The real bar is rendered at two frames with differing non-zero commitments and its button text pinned; the never-derives guard admits the price and is made to refuse `callTo`. The gate mutates the wiring back to `0` and requires both files red |
| [`TASK-141404`](../tasks/TASK-141404-the-last-act-mark-goes-bare-on-a-call.md) | S | `lastActText` returns `amount: null` for `PlayerCalled`; the two merged tests that pin the old figure move, and the seat plate's bare-mark test gains the call at two different `to` values |
| [`TASK-141405`](../tasks/TASK-141405-adr-0109-records-the-clause-adr-0122-amended.md) | XS | `ADR-0109`'s Status line and its index row record the amendment `ADR-0122` made and its own merging PR did not write down |

**A sixth ticket was considered and refused.** `docs/test-plan.md` has no case naming the `Call`
button, and `EPIC-14`'s per-epic suite is the `qa-cases` skill's to write from the epic's Definition
of done — *"one case per promise the epic made… not one per ticket"* (`docs/test-plan.md`
§Per-epic suites, rule 1). A story that writes its own catalogue row writes it from a ticket title,
which that section forbids in as many words.

## Acceptance criteria

- [ ] At a frame where the acting seat has committed 200 against `callTo` 600, the button reads
      `Call 400`, and at 75 against 925 it reads `Call 850` — asserted through the real `ActionBar`,
      not through `actionText` alone
- [ ] At those same frames, the total (600, 925) appears **nowhere** in the bar
- [ ] At a frame where the acting seat has committed **nothing**, the button still reads
      `Call 400` — the shipped assertions at lines 57, 479 and 658 of `ActionBar.test.tsx` are green
      and unedited
- [ ] `Bet`, `Raise to` and `All in` print the same figures at a non-zero commitment as at zero
- [ ] `bar-no-derivation.test.tsx` shows the price on screen, does **not** show `callTo`, and admits
      no third derived figure
- [ ] A `PlayerCalled` mark renders as the string `Call` and matches no digit, at two different `to`
      values; `Bet`, `Raise to` and `All in` marks keep their totals
- [ ] Every changed behaviour is proved red by a `verify:` command that reintroduces the old
      behaviour, captures the failure, restores the file and checks the restore with `cmp -s`
- [ ] `design/screens/duel-table-states.html` and `design/components/seat-and-pot.html` draw the call
      mark bare, and the eight `Call` **button** nodes across `design/` are byte-unchanged
- [ ] `ADR-0109`'s Status line names `ADR-0122` as amending §2, and its `docs/adr/README.md` row says
      so
- [ ] `cd web-client && NO_COLOR=1 npm run --silent check` exits 0, `./design/check-drift.sh` exits
      0, and `python3 .github/scripts/lint_tickets.py` exits 0

## Out of scope

- **The wire.** No field is added to `PlayerCalled` or `LegalActions`, no `PROTOCOL_VERSION` step,
  no server or engine file. `ADR-0122` §Consequences names the field that would let the *mark* print
  a price as the **reversal path**, deliberately not registered (`ADR-0105` §6).
- **`All in`'s figure.** It is bare and prints a total, and `ADR-0122` §3 says outright that it is
  left where it is because nobody has asked. Moving it is one line in `actionText` and a different
  decision.
- **The hero's own bet line.** `DuelTable.tsx:81` mounts exactly one `BetLine` and it is the rival's;
  the hero's street commitment is drawn nowhere. That is `ADR-0122`'s *Alternative 1* — considered
  and not taken — and it is nobody's ticket today.
- **The sizing row, the typed field and the frame.** `ADR-0122` §6: chips still set street totals,
  the field still takes a street total, `actFrame` still sends `{ type: "Call", seat }` with no
  amount. `DEC-102` (what one stepper press moves) is untouched.
- **`absentActionText`** (`web-client/src/table/absent-action-text.ts`) — it says what the server did
  for an absent seat, carries no figure of any kind, and is `ADR-0046` §4's.
- **The bar's `off` state** — `ADR-0127` answered `DEC-135` *no* and retired `STORY-1406`; the
  sentence stays and no verb or figure is drawn outside a turn.
- **`docs/test-plan.md` and the QA catalogue** — see the refused sixth ticket above.
- **Any recorded frame under `web-client/src/e2e/`** — the driver matches a control by its leading
  verb, so no recording moves. If one does, the change did something this story did not ask for.
