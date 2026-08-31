# ADR-0100 — The driver reaches an amount by pressing what a player presses

- **Status:** Accepted
- **Date:** 2026-08-31
- **Resolves:** `DEC-100` — **the architect's** — how does a scripted duel reach a bet amount once
  the table's sizing control is the card's discrete presets rather than a range slider? Registered
  and answered in the same PR (the `DEC-039` path); it never appeared in an open table.
- **Registers:** `DEC-101` — **the product owner's** — what amount does each named sizing preset
  set: what does a chip labelled `pot` (and `⅓`, `½`) promise a player in a heads-up duel? Open
  below, and it gates the rewritten `TASK-120908`.
- **Applies, and does not touch:**
  [`ADR-0002`](ADR-0002-server-authoritative.md) — the client asserts nothing; this ADR reads it
  one level finer (a *proposal* is the player's, a *label naming a game quantity* is the client's
  assertion) and changes none of its text.
  [`ADR-0033`](ADR-0033-component-anatomy-is-born-in-its-canonical-card.md) — the card is still
  where anatomy is born; §4 below says what a card's **one drawn state** does and does not settle,
  which is a reading, not an amendment.
  [`ADR-0068`](ADR-0068-an-atomic-ticket-names-the-gate-that-forbids-splitting-it.md) — §7 hands
  the planner a file set over the cap and names the merged gate that forbids splitting it, which is
  the exemption `ADR-0068` §3 already provides.
- **Touches no module but `web-client`.** `poker-engine` is read here and not changed;
  `poker-server`'s script generator is read here and **not** re-run. No wire message, no
  `PROTOCOL_VERSION`, no persisted shape moves.
- **Where the numbers came from.** Every count and amount quoted below was read on 2026-08-31 off
  the committed `web-client/src/e2e/scripted-duel.gen.json` at `b7fa5a25`, by decoding each step's
  frame with the fixture's own JSON, and off
  `poker-server/src/test/kotlin/duels/poker/server/duel/PlayedDuel.kt` and
  `poker-engine/src/main/kotlin/duels/poker/engine/game/BettingRules.kt` as merged. One of them
  contradicted the premise this decision was raised on, and the premise changed rather than the
  numbers.

## Context

`TASK-120908` replaces the action bar's native range input with the sizing row
`design/screens/duel-table.html` draws — five chips (`min`, `⅓`, `½`, `pot`, `all-in`) and a
stepper. One of its own three tests asserts that no `type="range"` input remains. The coder got all
three green and the whole of `ActionBar.test.tsx` green at 25 tests, then `npm run check` failed
with **24 failures across four files** it was not budgeted to open, and routed instead of widening
scope (`CLAUDE.md` rules 4 and 5).

**The mechanism.** `web-client/src/e2e/drive-duel.tsx` is the one driver behind every scripted e2e
duel. Its `actThroughTheBar` answers each recorded `"client"` step through the real bar, and for a
`Bet` or a `Raise` it reaches the amount in one line:

```tsx
fireEvent.change(within(container).getByRole("slider"), {
  target: { value: String(action.to) },
});
```

Delete the input and that query throws `Unable to find an accessible element with the role "slider"`
in `whole-duel.test.tsx`, `duel-secrecy.test.tsx`, `claimed-here-recovered-there.test.tsx` and
`drive-duel.test.tsx`. `drive-arc.tsx` never touches the bar and is not affected.

**What the e2e actually proves, which decides how much freedom there is here.**
`whole-duel.test.tsx`'s *sends one `Act` for each `YourTurn`, and the frame the server recorded*
ends in

```ts
expect(actsSent).toEqual(recordedActs);
```

The oracle is the **server's own recording**, compared field for field and position for position.
So the client must build `{"type":"Raise","seat":0,"to":1400}` where the server recorded that, and
`{"type":"AllIn","seat":1}` where the server recorded *that* — the same chips, two different
frames. `actThroughTheBar`'s KDoc states the other half of the contract: *"Never `send`, `actFrame`
or `socket.send` — the frame that reaches the double is whatever the real bar built from a real
click, not one handed to it."* A driver that reached the amount by a door a player does not have
would keep the assertion green while deleting what it asserts.

**The premise the decision was raised on is false, and measuring it is what made this cheap.**
The ticket accepts that *"the set of reachable sizes shrinks to the presets"*, on the understanding
that the script's amounts were chosen against a control that could reach anything. They were not.
The script is **generated, not authored** — `poker-server`'s `playDuel` answers every `YourTurn`
with an action drawn from `legalActions`, and for `BET` and `RAISE` it draws the amount from
exactly two values:

```kotlin
val to = if (amountDraw.value == 0) legal.minRaiseTo else legal.allInTo
```

Read off the committed fixture, every amount-carrying step in the whole duel:

| Seat | Hand | Seq | Recorded | `minRaiseTo` | `allInTo` | Lands on |
| --- | --- | --- | --- | --- | --- | --- |
| 0 | 3 | 5 | `Raise to 1400` | 200 | 1400 | `allInTo` |
| 0 | 7 | 5 | `Raise to 400` | 400 | 1600 | `minRaiseTo` |
| 1 | 2 | 5 | `Raise to 1600` | 200 | 1600 | `allInTo` |
| 1 | 7 | 7 | `Raise to 1400` | 600 | 1400 | `allInTo` |

Four amount-carrying steps, no `Bet` at all, and every one of them on a boundary the card draws as
a chip. The script has never reached an interior amount and, under the merged policy, cannot.

**A second breakage sits in the same two files and is not about amounts at all.** The ticket also
cuts the actions row to the card's three (*Fold*, *Call*, *Raise to*) and moves all-in into the
sizing row. The script records one `AllIn` (seat 1, hand 1, sequence 7) in a state whose `allowed`
is `[CHECK, RAISE, ALL_IN]`; with no `ALL_IN` button `actThroughTheBar` finds nothing starting with
*"All in"* and throws by design. It cannot be substituted by a chip: `BettingRules.kt` adds
`ALL_IN` whenever the pot is contestable but adds `RAISE` only when `allInTo > betToMatch`, so a
short stack facing a bet is offered `ALL_IN` with **no** `RAISE` button for a chip to size. And
three of the four recorded raises are `Raise to allInTo`, so the sizing row must also be able to
set `to = allInTo` **without** sending. Both routes to the same number are load-bearing, for two
different frames.

**The pull the other way.** The driver is a test file. Every cheap repair — a hidden input, a
`data-testid`, a test-only prop, a range slider kept behind the presets — costs one line and turns
the four merged e2e files green immediately. Each of them also puts a door in the bar that no
player has, and one of them puts a control in the product for the tests' sake.

**And the second half.** The presets are named for fractions of the pot, but no pot reaches
`ActionBar`: it is handed `LegalActions { callTo, minBetTo, minRaiseTo, allInTo }` and nothing else,
so the coder sized the fractions off the server's own minimum — `floor × 4/3`, `× 1.5`, `× 2` — and
a chip reading *½* does not size half of anything. The pot is not withheld by the wire:
`PlayerView` carries `pot` (*"chips already swept from finished betting rounds"*) and both seats'
`committedThisStreet`, `PotStrip` already prints `view.pot`, and `Lobby.tsx` already holds
`state.view` and passes it to `DuelTable` a few lines above the `<ActionBar />` it renders. So
threading it costs no wire change and no new state. What the fractions *mean*, though, is not
derivable: the card's one worked example — `Pot 2,450`, opponent `committed 400`, `Call 400`, the
`pot` chip selected, `Raise to 3,250` — is satisfied by `view.pot + 2 × callTo` **and** by
`(view.pot + both committed) + callTo`, which agree on that frame and disagree the moment the hero
has committed anything this street; and it excludes the textbook pot-sized raise a poker player
expects, which is 3,650 there. Two readings of a merged card, a third from the domain, all
defensible, differing by real chips at the table. That is what a player is promised by a label.

## Decision

### 1. The driver reaches an amount by pressing the sizing controls and reading the action button

`actThroughTheBar`'s `Bet`/`Raise` branch stops setting a control's value and starts doing what a
player does:

1. Find the action button whose accessible name starts with the recorded action's verb — unchanged,
   and it stays the first thing that happens, so a missing button still throws the message it throws
   today.
2. **Read the amount that button prints.** The bar's own `actionText` puts the dialled total on the
   `BET`/`RAISE` button, so `Raise to 400` is the bar stating what it is about to send. If it
   already equals the recorded `to` — which it does whenever the recording is the server's minimum,
   because the bar opens at the floor — click it and stop.
3. Otherwise press the sizing row's controls one at a time in **document order**, re-querying the
   action button after each press because React has replaced it, and click as soon as the printed
   amount equals the recorded `to`.
4. If no press makes them equal, **throw**, naming the step index, the hand, the recorded amount and
   every amount the sizing row reached.

The click is on the real action button after a real press. Nothing is set, injected, stubbed or
synthesised; the driver touches only controls a player touches and reads only text a player reads.

### 2. Reading the button before clicking is an assertion the slider path never made

`fireEvent.change` set a value and trusted the bar to have taken it. The new path **proves** the bar
reached the amount before the frame is built, and the frame equality in `whole-duel.test.tsx` then
proves the bar encoded it. That is strictly more than the deleted line proved, and it is the reason
this is not a workaround: the e2e gets stronger where it was expected to get weaker.

### 3. The committed script is not re-recorded, because it already fits

Measured, not assumed — the table in *Context*. Every amount-carrying step lands on `minRaiseTo`
(once) or `allInTo` (three times), both of which the card draws as chips, and `playDuel`'s merged
amount policy can produce nothing else. **No frame is regenerated, no e2e file is edited, and the
step counts `ScriptedDuel.kt` pins (57 for seat 0, 55 for seat 1) do not move.** That the four
failing files need no edit is the evidence that nothing they prove was traded away; if a later
change makes one of them need an edit to accommodate the bar, that evidence is gone and the change
is not this decision.

The property is **self-policing rather than separately guarded**: §1.4's throw is what fails, loudly
and by name, the day the fixture or `playDuel`'s policy produces an amount no press reaches. No new
test, no new fixture check, no assertion for someone to keep in step.

### 4. The sizing row must set `to = allInTo`; the actions row keeps every action the server named

Both, and for two different recorded frames.

- **The actions row renders exactly the actions in `legalActions.allowed`, in the order the server
  sent them.** This is not new: it is `ActionBar`'s own merged law — *"It offers exactly the actions
  the server named in `YourTurn` and no others — it hides none it thinks bad, adds none it thinks
  legal"* — which is `ADR-0002` at the bar. So an `ALL_IN` button appears whenever the server names
  `ALL_IN`, and the recorded `AllIn` step is pressed the way it is pressed today.
- **The sizing row must be able to set the dialled total to `allInTo` without sending**, or three of
  the four recorded raises are unreachable.
- **The card's three-button `.actions` row is one drawn state, not a law over all states.** The
  card draws a hero with 13,400 behind facing 400; `BettingRules.kt` reaches states the card does
  not draw, including `allowed = [FOLD, CALL, ALL_IN]`, where an all-in chip would have no button to
  size. `ADR-0033` makes a card the birthplace of anatomy, not a claim that every state was drawn.
  **No card is in arrears and none moves**: the sizing row is the card's, and the actions row obeys
  a merged law the card had no occasion to contradict.

The visible consequence is that all-in can appear twice on one bar — as a chip that sizes and as a
button that sends. That is a real cost, named in *Consequences*; how it is presented is a dressing
question for a card, not an architecture one, and the two **capabilities** are settled here.

### 5. Refused by name: any door the driver has and a player does not

None of these ships, and each is refused explicitly so that nobody adds it later as an obvious
convenience:

- **A range input kept behind the presets for the driver.** A control in the product for the tests'
  sake. It also defeats the ticket's own test that no `type="range"` remains, so it would have to
  hide from that test too — a control hidden from the assertion that forbids it.
- **A test-only prop, `data-testid`, exported setter or window hook** that sets the amount. The
  driver's KDoc forbids the same move one level up; a door one level down is the same door.
- **Reaching into React state, or building the `Act` with `actFrame`.** This is the bypass
  `whole-duel.test.tsx` already defends against with its independent bar-state witness, and it is
  the one this decision's constraint names: a test must not synthesise state the server did not
  send.

### 6. A named preset computes the quantity it is named for

`floor × 4/3` labelled *⅓ pot* does not ship. A chip whose name states a game quantity is the client
telling the player what the pot is; when the arithmetic is not the pot, that statement is false, and
`ADR-0002` is exactly the rule that a client does not state game facts of its own manufacture. The
line this ADR draws inside `ADR-0002`, because the bar now sits on both sides of it:

- **A dialled total is a proposal, and it is the player's.** The slider always let a player reach a
  number the turn did not carry; a chip does the same in one press. The server validates it. This is
  not a derived fact and needs no permission.
- **A label naming a game quantity is a statement, and it is the server's.** So the pot must reach
  the control, or the labels must not name it.

**The pot reaches the control.** `Lobby.tsx` passes it to `<ActionBar />` from the `state.view` it
already holds, as `view.pot + seats[0].committedThisStreet + seats[1].committedThisStreet` — every
term already on the wire, so no protocol change, no `PROTOCOL_VERSION` bump, no new store field.
`ActionBar` takes it as an input beside `turn`; it does **not** reach for the store itself, keeping
the bar a function of what it is handed.

**What the chips compute from it is `DEC-101`, and it is the product owner's.** The card admits two
formulas that agree on the one frame it draws and disagree in play, and the poker convention a
player brings matches neither; no technical fact chooses between them, and the choice decides what a
player believes when they press. Registered open by this ADR. Until it is answered the fraction
chips are not written — the ticket that writes them is blocked on it (§7), and shipping the
mislabelled arithmetic in the meantime is refused rather than deferred.

### 7. `TASK-120908` is rewritten, not amended, and it stays blocked

The ticket as written cannot go green: its two-file budget excludes the driver, and its three-button
actions row breaks a merged gate independently of the amounts. It is **rewritten by the planner**
over this file set, and stays blocked on `DEC-101` until the product owner answers.

| File | Why it is in the set |
| --- | --- |
| `web-client/src/table/ActionBar.tsx` | the sizing row replaces the range input; the bar takes the pot (§6); the actions row keeps every allowed action (§4) |
| `web-client/src/table/ActionBar.test.tsx` | the ticket's three new tests, plus the eleven `getByRole("slider")` sites already in the file |
| `web-client/src/e2e/drive-duel.tsx` | `actThroughTheBar`'s amount path (§1) — the only slider query outside the bar's own two test files |
| `web-client/src/lobby/Lobby.tsx` | passes the pot to `<ActionBar />` from the `state.view` it already holds |
| `web-client/src/table/bar-no-derivation.test.tsx` | its second test is named *"counts the ceiling that reaches the player only as a slider bound"* and turns on `max={allInTo}`; the bound is what goes, so the test is re-stated for the control that replaces it — **its guard is not weakened**, and the ticket says in words what each surviving assertion still forbids |
| `web-client/src/table/turn-fixture.ts` | only if the bar's new input needs a default; `aLegalActions`'s four mutually independent amounts stay as they are |

**Six files against `ADR-0068`'s cap of three, and it is genuinely atomic.** The merged gate that
forbids splitting it is `web-client/src/e2e/whole-duel.test.tsx` — *sends one `Act` for each
`YourTurn`, and the frame the server recorded*. Remove the slider without the driver and it is red;
move the driver to chips that do not exist yet and it is red. There is no ordering of two smaller
tickets that leaves `develop` green, and the only shape that would — the bar carrying both controls
for one PR — ships a player a bar with two amount controls and contradicts the ticket's own test.
So the rewrite declares `atomic:` naming that file, per `ADR-0068` §3, or the planner splits it in a
way that keeps every intermediate commit green; that judgement is the planner's and this ADR names
the constraint rather than the shape.

**The four e2e files are not in the set** — `whole-duel.test.tsx`, `duel-secrecy.test.tsx`,
`claimed-here-recovered-there.test.tsx` and `drive-duel.test.tsx` are untouched, and that is the
check on the whole decision: if the rewrite finds itself opening one of them, it is weakening a
proof and should stop.

### 8. Reversing this is one function in one file

§1 lives entirely in `actThroughTheBar`. If a scripted duel ever needs an amount no press reaches,
the repair is local and graduated: teach the same function to press the stepper after the nearest
chip below the target, and nothing outside `drive-duel.tsx` moves. That is why §1 throws instead of
stepping today — with four data points all on boundaries, the evidence for a stepping search is
thin, and the cheapest thing to be wrong about is the one that fails loudly and is repaired in one
test file. §6's pot thread is reversed by deleting one prop.

## Consequences

**The cost, named plainly: the driver's reach is now the sizing row's, and the e2e can no longer
prove the bar builds a correct frame for an interior amount.** Today nothing is lost — `playDuel`
draws only `minRaiseTo` and `allInTo`, so the proof never had that reach — but the day someone
widens that policy precisely *to* get it, they hit a wall this ADR built. There is a real class of
bug the e2e now cannot see: an off-by-one or a rounding fault in the amount path that is correct at
both boundaries and wrong in between. It is covered only by `ActionBar.test.tsx`'s unit tests over a
rendered bar, which is a weaker witness than a whole duel, and the repair when it is wanted is §8's
stepping search plus a policy change in `poker-server`.

**A second cost: the bar can show all-in twice.** A chip that sizes to `allInTo` and a button that
sends `AllIn` are both required by merged gates (§4), so in the common state where the server allows
`RAISE` and `ALL_IN` together a player reads the same words in two places doing two different
things. This is accepted here as a capability question and left to a card as a presentation one; it
is the kind of thing a UAT round may legitimately file against a card, and it now has a merged
source to be read against.

**A third: `TASK-120908` grew from a two-file `S` into a six-file atomic ticket that is blocked.**
A `medium` finding now costs a large ticket, a product decision and two PRs. Nothing is on fire —
every action remains available and correct behind the shipped slider, which is why the severity was
lowered in the first place — but the trail should show honestly that the small ticket was never
small.

**A fourth, quieter: the driver's failure mode moved from "the query throws" to "no press reached
the amount".** The old failure was unmistakable. The new one names the recorded amount and the
amounts reached, which is more informative but reads as a *script* problem when it may be a *bar*
problem — a chip that computes wrongly presents exactly as a script that recorded oddly. §1.4's
message must therefore print both sides, and the ticket says so.

**What it buys.** The four merged e2e proofs go green with **no edit to any of them** and one
strictly stronger assertion added to the driver. No test-only door enters the client, no control
enters the product for the tests, no frame is regenerated, no wire moves, and the `ADR-0002`
constraint is honoured in the strong form: the driver presses what a player presses and reads what a
player reads.

**What it forecloses.** A scripted duel can no longer be the place an arbitrary amount is exercised,
until §8 is taken. And it forecloses the reading that a design card's single drawn state settles
what a control offers in every state — §4 says the opposite out loud, which will be quoted the next
time a card and `legalActions` disagree.

**No deadline, and one thing that gets harder with time.** Nothing here is free today and impossible
later. But the property §3 rests on is a fact about the *current* fixture: the day a second script is
recorded, or `playDuel`'s policy widens, the throw fires and someone re-decides. That is the design,
not a gap — the alternative was a separate guard test asserting the same thing, which would be a
second copy of a rule, and two copies of a rule drift (`ADR-0092` §8).

## Alternatives considered

**Give the driver a non-visual path to the amount — an exported setter, a test-only prop, a
`data-testid` on a hidden input.** *Its strongest case:* it is one line, it keeps every recorded
amount reachable forever, it survives any future change to the sizing control, and the thing it
bypasses — dialling an amount — is not the thing the e2e is about; the e2e is about the frame the
bar builds and the store it drives, and those stay fully exercised. **Why it lost:** the driver's own
KDoc already refuses this move at the frame level, for the reason that a driver which can reach past
the UI eventually does, and then the assertions pass over a bar nobody drove. It is also the exact
shape this decision's constraint forbids — a path that lets a test put the client into a state no
player could reach. The line between "not the thing the e2e is about" and "the thing the e2e is
about" is not stable enough to hang the client's only whole-duel proof on.

**Keep a range input behind the presets, driver-only.** *Its strongest case:* it is the least work,
it changes no test outside the two-file budget, and a visually hidden but accessible slider is a
defensible affordance in its own right — some players prefer dragging, and hiding it is a styling
decision that could be revisited. **Why it lost:** it is a control in the product for the tests'
sake, and the ticket's own test forbids `type="range"`, so it would have to be hidden from the
assertion that exists to forbid it — a test and a control arranged not to see each other. If the
product ever wants a continuous sizing control, that is a card and a product decision, arrived at on
its merits and not smuggled in as a test fixture.

**Restrict the recorded scripts to amounts the presets reach.** *Its strongest case:* it is the
principled version of what this ADR does — state the constraint on the fixture instead of leaving it
to a runtime throw — and it would make the coupling explicit at the point of generation. **Why it
lost:** there is nothing to restrict. `playDuel` already draws every amount from `minRaiseTo` or
`allInTo`, both of them chips, so the restriction is a rule with no work to do; and enforcing it
would mean teaching a **server** test fixture the **client's** preset arithmetic, a dependency
pointing the wrong way through the module graph. The version that re-records to *avoid*
`Raise to allInTo` is worse still: it changes the server's action policy, rewrites every frame in a
hundred-thousand-character committed fixture, and moves the 57/55 step counts `ScriptedDuel.kt`
pins — a large, risky change made to keep a test file small.

**Click the nearest preset, then step to the exact amount.** *Its strongest case:* it is what the
card itself draws — chips to get close, the stepper to get exact — so the driver would be using the
player's full vocabulary rather than a subset, and no recorded amount could ever become unreachable;
it also removes the dependency on the fixture's boundary property entirely. **Why it lost:** the
step size is undecided and belongs with `DEC-101`'s control design, so this alternative cannot be
implemented today without deciding a second open question inside a driver; a one-chip step turns a
1,200-chip gap into 1,200 `fireEvent.click`s inside an `act()`, and a coarse step makes some legal
amounts unreachable, which is a capability question, not a driver question. And it would never
execute: with all four amounts on chips, the stepping branch is dead code guarded by nothing. §8
adds it in one function on the day a script needs it, which is the cheaper order.

**Answer the preset arithmetic here as well, and unblock the whole ticket in one PR.** *Its
strongest case:* the card gives a worked example, one formula (`view.pot + 2 × callTo`) fits it
using numbers already in `LegalActions` plus `view.pot`, and the ticket would ship this week instead
of waiting on a second agent run. **Why it lost:** two formulas fit that example and disagree in
play, and the textbook pot-sized raise — what a poker player pressing *pot* expects — matches
neither. Two competent engineers would land in different places, which is the test for whose
decision it is. A confidently-argued product decision dressed as an architecture one reads as
settled and never gets revisited; routing it costs one agent run.
